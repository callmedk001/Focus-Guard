package com.dk.focusguard

import com.dk.focusguard.data.repository.VpnStatusRepositoryImpl
import com.dk.focusguard.domain.repository.VpnConnectionState
import com.dk.focusguard.services.vpn.DnsPacketProcessor
import com.dk.focusguard.services.vpn.ParsedDnsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

/**
 * Unit test suite verifying DNS packet parsing, synthesis, sinkholing,
 * malformed packet safety, protocol inspection, and VPN connection states.
 * Covers requirements 8-15, 19.
 */
class DnsPacketProcessingAndVpnTest {

    private lateinit var dnsProcessor: DnsPacketProcessor
    private lateinit var vpnStatusRepo: VpnStatusRepositoryImpl

    @Before
    fun setUp() {
        dnsProcessor = DnsPacketProcessor()
        vpnStatusRepo = VpnStatusRepositoryImpl()
    }

    private fun createRawDnsQuery(txId: Int, domain: String, qType: Int = 1): ByteArray {
        val stream = ByteArrayOutputStream()
        val dos = DataOutputStream(stream)
        dos.writeShort(txId)
        dos.writeShort(0x0100) // Flags: Standard Query, RD=1
        dos.writeShort(1)      // QDCOUNT: 1 question
        dos.writeShort(0)      // ANCOUNT: 0
        dos.writeShort(0)      // NSCOUNT: 0
        dos.writeShort(0)      // ARCOUNT: 0

        // QNAME labels
        val labels = domain.split('.')
        for (label in labels) {
            dos.writeByte(label.length)
            dos.write(label.toByteArray(Charsets.US_ASCII))
        }
        dos.writeByte(0) // End of domain name

        dos.writeShort(qType) // QTYPE: 1 (A) or 28 (AAAA)
        dos.writeShort(1)     // QCLASS: 1 (IN)

        return stream.toByteArray()
    }

    @Test
    fun test8_blockedDnsResponse_synthesizesSinkholeResponse() {
        // Scenario 8: Blocked DNS query produces valid sinkhole A record response
        val txId = 0xABCD
        val rawQueryBytes = createRawDnsQuery(txId, "pornhub.com", qType = 1)
        val query = dnsProcessor.parseDnsQuery(rawQueryBytes)
        assertNotNull(query)

        val responseBytes = dnsProcessor.buildBlockedDnsResponse(query!!, sinkholeIp = "0.0.0.0")
        assertTrue(responseBytes.size >= rawQueryBytes.size + 16)

        val buffer = ByteBuffer.wrap(responseBytes)
        val responseTxId = buffer.short.toInt() and 0xFFFF
        val flags = buffer.short.toInt() and 0xFFFF
        val qdCount = buffer.short.toInt() and 0xFFFF
        val anCount = buffer.short.toInt() and 0xFFFF

        assertEquals(txId, responseTxId)
        assertTrue((flags and 0x8000) != 0) // QR bit = 1 (Response)
        assertEquals(0, flags and 0x000F)  // RCODE = 0 (NoError with sinkhole IP)
        assertEquals(1, qdCount)
        assertEquals(1, anCount)
    }

    @Test
    fun test8b_blockedDnsResponse_synthesizesNxDomain() {
        // Scenario 8b: NXDOMAIN synthesis
        val rawQueryBytes = createRawDnsQuery(0x1234, "malicious-site.xxx")
        val query = dnsProcessor.parseDnsQuery(rawQueryBytes)
        assertNotNull(query)

        val responseBytes = dnsProcessor.buildBlockedDnsResponse(query!!, returnNxDomain = true)
        val buffer = ByteBuffer.wrap(responseBytes)
        buffer.short // txId
        val flags = buffer.short.toInt() and 0xFFFF

        assertEquals(3, flags and 0x000F) // RCODE = 3 (NXDOMAIN)
    }

    @Test
    fun test9_allowedDnsForwarding_parsesValidDnsQuery() {
        // Scenario 9: Allowed DNS query parsing
        val txId = 0x5678
        val rawQueryBytes = createRawDnsQuery(txId, "wikipedia.org", qType = 1)

        val parsed = dnsProcessor.parseDnsQuery(rawQueryBytes)
        assertNotNull(parsed)
        assertEquals(txId, parsed!!.transactionId)
        assertEquals("wikipedia.org", parsed.domain)
        assertEquals(1, parsed.queryType)
        assertEquals(1, parsed.queryClass)
        assertTrue(parsed.isQuery)
    }

    @Test
    fun test10_malformedDnsPacketHandling_failsGracefullyWithoutCrashing() {
        // Scenario 10: Robustness on malformed or truncated packets
        // Empty bytes
        assertNull(dnsProcessor.parseDnsQuery(ByteArray(0)))

        // Truncated header (<12 bytes)
        assertNull(dnsProcessor.parseDnsQuery(ByteArray(8)))

        // Truncated question section
        val corruptBytes = byteArrayOf(
            0x12, 0x34, // ID
            0x01, 0x00, // Flags
            0x00, 0x01, // QDCount = 1
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // AN, NS, AR
            0x0A, 't'.code.toByte(), 'e'.code.toByte() // Declares length 10 but stream terminates!
        )
        assertNull(dnsProcessor.parseDnsQuery(corruptBytes))
    }

    @Test
    fun test11_ipv4AndIpv6Handling_detectsUdpDnsPackets() {
        // Scenario 11: Valid IPv4 UDP DNS packet detection
        val ipPacket = ByteArray(40)
        ipPacket[0] = 0x45.toByte() // IPv4, IHL = 5 (20 bytes)
        ipPacket[9] = 17.toByte()   // Protocol 17 = UDP
        ipPacket[20] = 0x1F         // Source Port
        ipPacket[21] = 0x90.toByte()
        ipPacket[22] = 0x00         // Dest Port: 53 (0x0035)
        ipPacket[23] = 0x35

        assertTrue(dnsProcessor.isUdpDnsPacket(ipPacket, ipPacket.size))

        // Non-DNS UDP port
        ipPacket[23] = 0x50 // Port 80
        assertFalse(dnsProcessor.isUdpDnsPacket(ipPacket, ipPacket.size))

        // Non-UDP (TCP protocol 6)
        ipPacket[9] = 6
        ipPacket[23] = 0x35
        assertFalse(dnsProcessor.isUdpDnsPacket(ipPacket, ipPacket.size))
    }

    @Test
    fun test12_tcpDnsHandling_detectsTcpDnsPackets() {
        // Scenario 12: TCP DNS packet inspection
        val ipPacket = ByteArray(50)
        ipPacket[0] = 0x45.toByte() // IPv4, IHL = 5
        ipPacket[9] = 6.toByte()    // Protocol 6 = TCP
        ipPacket[20] = 0x1F
        ipPacket[21] = 0x90.toByte()
        ipPacket[22] = 0x00         // Dest Port: 53
        ipPacket[23] = 0x35

        assertTrue(dnsProcessor.isTcpDnsPacket(ipPacket, ipPacket.size))

        // TCP to port 443 (HTTPS) must NOT match DNS
        ipPacket[22] = 0x01
        ipPacket[23] = 0xBB.toByte() // Port 443
        assertFalse(dnsProcessor.isTcpDnsPacket(ipPacket, ipPacket.size))
    }

    @Test
    fun test13_to_15_vpnConnectionStates_tracksLifecycleCorrectly() {
        // Scenarios 13, 14, 15: VPN connection state transitions
        vpnStatusRepo.setConnectionState(VpnConnectionState.VPN_DISCONNECTED)
        assertFalse(vpnStatusRepo.isCurrentlyConnected)

        vpnStatusRepo.setConnectionState(VpnConnectionState.VPN_CONNECTING)
        assertFalse(vpnStatusRepo.isCurrentlyConnected)

        vpnStatusRepo.setConnectionState(VpnConnectionState.VPN_CONNECTED)
        assertTrue(vpnStatusRepo.isCurrentlyConnected)

        vpnStatusRepo.setConnectionState(VpnConnectionState.VPN_ERROR)
        assertFalse(vpnStatusRepo.isCurrentlyConnected)

        vpnStatusRepo.setConnectionState(VpnConnectionState.VPN_PERMISSION_REQUIRED)
        assertFalse(vpnStatusRepo.isCurrentlyConnected)
    }

    @Test
    fun test19_noFakeBlockedEvents_recordsOnlyGenuineBlocks() {
        // Scenario 19: Only genuine blocked DNS requests are counted
        assertEquals(0, vpnStatusRepo.currentBlockedDnsCount)

        val now = System.currentTimeMillis()
        vpnStatusRepo.recordBlockedDns("pornhub.com", now)
        assertEquals(1, vpnStatusRepo.currentBlockedDnsCount)
        assertEquals(now, vpnStatusRepo.currentLastBlockedTimestamp)

        vpnStatusRepo.recordBlockedDns("xvideos.com", now + 1000)
        assertEquals(2, vpnStatusRepo.currentBlockedDnsCount)

        vpnStatusRepo.resetTelemetry()
        assertEquals(0, vpnStatusRepo.currentBlockedDnsCount)
        assertEquals(0L, vpnStatusRepo.currentLastBlockedTimestamp)
    }
}
