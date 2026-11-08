package com.dk.focusguard

import com.dk.focusguard.services.vpn.DnsPacketProcessor
import com.dk.focusguard.services.vpn.ParsedDnsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Hardening unit tests for DNS packet parsing and response synthesis:
 * - RFC 791 IPv4 header checksum verification
 * - RFC 1035 QNAME compression pointer parsing and loop prevention
 * - RFC 3596 AAAA 16-byte RDATA compliance
 * - Malformed, oversized, and truncated packet resilience
 * - IPv4 and IPv6 UDP DNS packet recognition
 */
class DnsPacketProcessorHardeningTest {

    private lateinit var processor: DnsPacketProcessor

    @Before
    fun setUp() {
        processor = DnsPacketProcessor()
    }

    @Test
    fun testIpChecksum_standardHeader_calculatesValidChecksum() {
        // Sample standard 20-byte IPv4 header with checksum set to 0 at bytes 10-11
        val ipHeader = byteArrayOf(
            0x45.toByte(), 0x00.toByte(), // Version 4, IHL 5, DSCP/ECN
            0x00.toByte(), 0x3C.toByte(), // Total Length 60
            0x1C.toByte(), 0x46.toByte(), // Identification
            0x40.toByte(), 0x00.toByte(), // Flags (DF), Fragment Offset
            0x40.toByte(), 0x11.toByte(), // TTL 64, Protocol 17 (UDP)
            0x00.toByte(), 0x00.toByte(), // Checksum (0 for calculation)
            0x0A.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte(), // Src: 10.0.0.2
            0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte()  // Dst: 1.1.1.1
        )

        val checksum = DnsPacketProcessor.calculateIpChecksum(ipHeader, 0, 20)
        assertTrue("Checksum must be non-zero", checksum > 0)
        assertTrue("Checksum must be within 16-bit range", checksum <= 0xFFFF)

        // Write checksum into header and verify that re-summing produces 0 (all ones complement verification)
        ipHeader[10] = ((checksum shr 8) and 0xFF).toByte()
        ipHeader[11] = (checksum and 0xFF).toByte()

        val verifySum = DnsPacketProcessor.calculateIpChecksum(ipHeader, 0, 20)
        assertEquals("Verifying calculated checksum over header must yield 0", 0, verifySum)
    }

    @Test
    fun testAaaaResponse_rdataMustBe16Bytes_rfc3596Compliance() {
        // Query for example.com with TYPE_AAAA (28)
        val query = ParsedDnsQuery(
            transactionId = 0xABCD,
            domain = "example.com",
            queryType = DnsPacketProcessor.TYPE_AAAA,
            queryClass = DnsPacketProcessor.CLASS_IN,
            isQuery = true,
            rawQuestionBytes = byteArrayOf(
                0x07, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
                0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
                0x00,
                0x00, 0x1C, // TYPE_AAAA (28)
                0x00, 0x01  // CLASS_IN (1)
            )
        )

        val responseBytes = processor.buildBlockedDnsResponse(query)
        assertNotNull(responseBytes)

        val buffer = ByteBuffer.wrap(responseBytes)
        val txId = buffer.short.toInt() and 0xFFFF
        assertEquals(0xABCD, txId)

        val flags = buffer.short.toInt() and 0xFFFF
        assertTrue("QR bit must be set (response)", (flags and 0x8000) != 0)

        val qdCount = buffer.short.toInt() and 0xFFFF
        val anCount = buffer.short.toInt() and 0xFFFF
        assertEquals(1, qdCount)
        assertEquals(1, anCount)

        // Skip NS and AR counts
        buffer.short
        buffer.short

        // Skip Question Section
        buffer.position(12 + query.rawQuestionBytes.size)

        // Answer Section: Name pointer (2) + Type (2) + Class (2) + TTL (4) + RDLENGTH (2)
        val namePointer = buffer.short.toInt() and 0xFFFF
        assertEquals("Answer name should be pointer 0xC00C", 0xC00C, namePointer)

        val type = buffer.short.toInt() and 0xFFFF
        val qclass = buffer.short.toInt() and 0xFFFF
        val ttl = buffer.int
        val rdLength = buffer.short.toInt() and 0xFFFF

        assertEquals("Type must be AAAA", DnsPacketProcessor.TYPE_AAAA, type)
        assertEquals(DnsPacketProcessor.CLASS_IN, qclass)
        assertEquals(DnsPacketProcessor.DEFAULT_TTL_SECONDS, ttl)
        assertEquals("RFC 3596: RDLENGTH for AAAA record MUST be 16 bytes", 16, rdLength)

        val rdata = ByteArray(rdLength)
        buffer.get(rdata)
        assertEquals(16, rdata.size)
        // All zeros for sinkhole IPv6 (::)
        assertTrue("IPv6 sinkhole RDATA must be 16 zero bytes", rdata.all { it == 0.toByte() })
    }

    @Test
    fun testAResponse_rdataMustBe4Bytes_rfc1035Compliance() {
        val query = ParsedDnsQuery(
            transactionId = 0x1234,
            domain = "badsite.com",
            queryType = DnsPacketProcessor.TYPE_A,
            queryClass = DnsPacketProcessor.CLASS_IN,
            isQuery = true,
            rawQuestionBytes = byteArrayOf(
                0x07, 'b'.code.toByte(), 'a'.code.toByte(), 'd'.code.toByte(), 's'.code.toByte(), 'i'.code.toByte(), 't'.code.toByte(), 'e'.code.toByte(),
                0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
                0x00,
                0x00, 0x01, // TYPE_A (1)
                0x00, 0x01  // CLASS_IN (1)
            )
        )

        val responseBytes = processor.buildBlockedDnsResponse(query, sinkholeIp = "0.0.0.0")
        assertNotNull(responseBytes)

        val buffer = ByteBuffer.wrap(responseBytes)
        buffer.position(12 + query.rawQuestionBytes.size + 10) // Jump directly to RDLENGTH
        val rdLength = buffer.short.toInt() and 0xFFFF
        assertEquals("RFC 1035: RDLENGTH for A record MUST be 4 bytes", 4, rdLength)

        val rdata = ByteArray(4)
        buffer.get(rdata)
        assertEquals(0, rdata[0].toInt())
        assertEquals(0, rdata[1].toInt())
        assertEquals(0, rdata[2].toInt())
        assertEquals(0, rdata[3].toInt())
    }

    @Test
    fun testParseDnsQuery_withCompressionPointer_resolvesCorrectly() {
        // Construct DNS packet with a question using compression pointer (0xC00C)
        // Header: 12 bytes
        // Offset 12: \x03com\x00 (suffix at offset 12)
        // Offset 17: \x07example\xC0\x0C (example + pointer to offset 12)
        val packet = byteArrayOf(
            // Header (12 bytes)
            0x55.toByte(), 0xAA.toByte(), // ID
            0x01.toByte(), 0x00.toByte(), // Flags: standard query, RD=1
            0x00.toByte(), 0x01.toByte(), // QDCOUNT = 1
            0x00.toByte(), 0x00.toByte(), // ANCOUNT = 0
            0x00.toByte(), 0x00.toByte(), // NSCOUNT = 0
            0x00.toByte(), 0x00.toByte(), // ARCOUNT = 0
            // Offset 12: target suffix ".com"
            0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(), 0x00,
            // Offset 17: "example" + pointer to offset 12
            0x07, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            0xC0.toByte(), 0x0C.toByte(),
            // QTYPE (1) + QCLASS (1)
            0x00, 0x01,
            0x00, 0x01
        )

        // Parse starting at offset 17 (where the question actually begins)
        // Adjust packet to put question at offset 12:
        val stdPacket = byteArrayOf(
            0x55.toByte(), 0xAA.toByte(),
            0x01.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x01.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(),
            // Offset 12: "example" + pointer to offset 26 (0x1A)
            0x07, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(), 'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
            0xC0.toByte(), 0x1A.toByte(),
            // QTYPE (1) + QCLASS (1) (offsets 22..25)
            0x00, 0x01,
            0x00, 0x01,
            // Offset 26: "org\0"
            0x03, 'o'.code.toByte(), 'r'.code.toByte(), 'g'.code.toByte(), 0x00
        )

        val parsed = processor.parseDnsQuery(stdPacket, 0, stdPacket.size)
        assertNotNull(parsed)
        assertEquals("example.org", parsed?.domain)
        assertEquals(DnsPacketProcessor.TYPE_A, parsed?.queryType)
        assertEquals(0x55AA, parsed?.transactionId)
    }

    @Test
    fun testParseDnsQuery_pointerCycle_preventInfiniteLoop() {
        // Header (12 bytes)
        // Offset 12: Pointer to offset 14 (0xC00E)
        // Offset 14: Pointer to offset 12 (0xC00C) -> Cycle!
        val cyclingPacket = byteArrayOf(
            0x11, 0x22,
            0x01, 0x00,
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,
            0xC0.toByte(), 0x0E.toByte(),
            0xC0.toByte(), 0x0C.toByte(),
            0x00, 0x01,
            0x00, 0x01
        )

        // Must safely return null without throwing StackOverflowError
        val parsed = processor.parseDnsQuery(cyclingPacket, 0, cyclingPacket.size)
        assertNull("Cycling pointer must be safely rejected", parsed)
    }

    @Test
    fun testParseDnsQuery_truncatedAndMalformedPackets_safeNull() {
        // Less than 12 bytes
        assertNull(processor.parseDnsQuery(byteArrayOf(0x01, 0x02, 0x03)))

        // Zero length
        assertNull(processor.parseDnsQuery(byteArrayOf(), 0, 0))

        // QDCOUNT is 0
        val zeroQdPacket = ByteArray(12).apply {
            this[4] = 0 // QDCOUNT high
            this[5] = 0 // QDCOUNT low
        }
        assertNull(processor.parseDnsQuery(zeroQdPacket))

        // Truncated question label (claims length 10 but only 3 bytes follow)
        val truncatedLabelPacket = byteArrayOf(
            0x01, 0x02, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x0A, 'a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()
        )
        assertNull(processor.parseDnsQuery(truncatedLabelPacket))
    }

    @Test
    fun testIsIpv6UdpDnsPacket_distinguishesIpv6Dns() {
        // Valid 48-byte IPv6 UDP DNS packet directed to port 53
        val ipv6DnsPacket = ByteArray(48).apply {
            this[0] = 0x60.toByte() // Version 6
            this[6] = 17.toByte()   // Next Header = UDP
            this[40] = 0xC0.toByte() // Src Port high (49152)
            this[41] = 0x00.toByte() // Src Port low
            this[42] = 0x00.toByte() // Dst Port high (53)
            this[43] = 0x35.toByte() // Dst Port low (53)
        }

        assertTrue(processor.isIpv6UdpDnsPacket(ipv6DnsPacket, 48))
        assertFalse("IPv4 packet must not match IPv6 UDP DNS", processor.isIpv6UdpDnsPacket(ByteArray(28), 28))
    }
}
