package com.dk.focusguard.services.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.dk.focusguard.FocusGuardApplication
import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.domain.repository.VpnConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Local Android VpnService implementation for privacy-first adult website filtering.
 *
 * GUARANTEES:
 * 1. LOCAL ONLY: Filters DNS packets on device; never transmits browsing activity to third parties.
 * 2. ZERO CONTENT INSPECTION: Inspects only DNS query domains; never inspects HTTPS web traffic.
 * 3. NO ROOT / NO MITM: Never installs custom root certificates or performs MITM.
 * 4. SYSTEM-WIDE ADULT BLOCKING: Backed by Cloudflare 1.1.1.1 for Families (1.1.1.3 / 1.0.0.3)
 *    and CleanBrowsing Adult Filter to automatically sinkhole millions of adult sites upstream.
 * 5. SAFE SEARCH ENFORCEMENT: Enforces strict SafeSearch for Google, Bing, and YouTube Restricted Mode.
 * 6. DOH BYPASS SHIELD: Intercepts DoH endpoints so Chrome / Edge (normal & Incognito) fall back to system DNS.
 */
class FocusGuardVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.dk.focusguard.vpn.START"
        const val ACTION_STOP = "com.dk.focusguard.vpn.STOP"

        const val VPN_ADDRESS = "10.0.0.2"
        const val VPN_ROUTE_DNS = "10.0.0.2"

        // Upstream Family DNS Resolvers (Blocks malware & adult content automatically upstream)
        const val UPSTREAM_DNS_PRIMARY = "1.1.1.3" // Cloudflare Family (Adult Content Blocker)
        const val UPSTREAM_DNS_SECONDARY = "1.0.0.3" // Cloudflare Family Secondary
        const val UPSTREAM_DNS_FALLBACK = "185.228.168.168" // CleanBrowsing Adult Filter

        const val UPSTREAM_DNS_IPV6_PRIMARY = "2606:4700:4700::1113"
        const val UPSTREAM_DNS_IPV6_SECONDARY = "2606:4700:4700::1003"

        const val BUFFER_SIZE = 4096

        // Public DNS IPs to route through VPN filter to prevent DNS leaks and bypasses
        val INTERCEPTED_PUBLIC_DNS_IPS = listOf(
            "1.1.1.3",
            "1.0.0.3",
            "1.1.1.1",
            "1.0.0.1",
            "8.8.8.8",
            "8.8.4.4",
            "9.9.9.9",
            "149.112.112.112",
            "208.67.222.222",
            "208.67.220.220",
            "185.228.168.168",
            "185.228.169.168"
        )
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dnsProcessor = DnsPacketProcessor()

    private var lastBlockedDomain: String? = null
    private var lastBlockedTimestamp: Long = 0L

    @Volatile
    private var isAdultProtectionEnabled: Boolean = true

    @Volatile
    private var isSafeSearchEnabled: Boolean = true

    @Volatile
    private var isEmergencyOverrideActive: Boolean = false

    override fun onCreate() {
        super.onCreate()
        val app = application as? FocusGuardApplication

        app?.protectionStateRepository?.let { stateRepo ->
            serviceScope.launch {
                stateRepo.protectionState.collect { state ->
                    isAdultProtectionEnabled = state.adultWebsitesState == com.dk.focusguard.domain.model.ProtectionModuleState.ACTIVE ||
                            state.adultWebsitesState == com.dk.focusguard.domain.model.ProtectionModuleState.SCHEDULED
                    isEmergencyOverrideActive = state.isEmergencyOverrideActive
                }
            }
        } ?: run {
            app?.protectionSettingsRepository?.let { repo ->
                serviceScope.launch {
                    repo.getProtectionSettings().collect { settings ->
                        isAdultProtectionEnabled = settings.blockAdultWebsites && settings.isMasterProtectionEnabled
                    }
                }
            }

            app?.securityRepository?.let { repo ->
                serviceScope.launch {
                    repo.isEmergencyOverrideActive.collect { overrideActive ->
                        isEmergencyOverrideActive = overrideActive
                    }
                }
            }
        }

        app?.preferencesManager?.let { pm ->
            serviceScope.launch {
                pm.isSafeSearchEnabled.collect { enabled ->
                    isSafeSearchEnabled = enabled
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startVpn()
                return START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    private fun startVpn() {
        val app = application as? FocusGuardApplication
        val statusRepo = app?.vpnStatusRepository

        if (vpnInterface != null) {
            statusRepo?.setConnectionState(VpnConnectionState.VPN_CONNECTED)
            return
        }

        statusRepo?.setConnectionState(VpnConnectionState.VPN_CONNECTING)

        try {
            val builder = Builder()
                .setSession("DK Focus Guard Defense Engine")
                .addAddress(VPN_ADDRESS, 32)
                .addDnsServer(UPSTREAM_DNS_PRIMARY)
                .addDnsServer(UPSTREAM_DNS_SECONDARY)
                .addRoute(VPN_ROUTE_DNS, 32)
                .setMtu(1500)

            // Gracefully attempt IPv6 DNS loop configuration
            runCatching {
                builder.addAddress("fd00::2", 128)
                    .addDnsServer(UPSTREAM_DNS_IPV6_PRIMARY)
                    .addRoute("fd00::2", 128)
                    .addRoute(UPSTREAM_DNS_IPV6_PRIMARY, 128)
                    .addRoute(UPSTREAM_DNS_IPV6_SECONDARY, 128)
            }

            // Route standard public DNS IPs into the filter to prevent DNS leaks and bypasses
            for (dnsIp in INTERCEPTED_PUBLIC_DNS_IPS) {
                runCatching { builder.addRoute(dnsIp, 32) }
            }

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                statusRepo?.setConnectionState(VpnConnectionState.VPN_ERROR)
                stopSelf()
                return
            }

            statusRepo?.setConnectionState(VpnConnectionState.VPN_CONNECTED)
            startPacketProcessingLoop()

        } catch (e: Exception) {
            statusRepo?.setConnectionState(VpnConnectionState.VPN_ERROR)
            stopVpn()
        }
    }

    private fun startPacketProcessingLoop() {
        val pfd = vpnInterface ?: return
        val app = application as? FocusGuardApplication ?: return

        serviceScope.launch {
            val inStream = FileInputStream(pfd.fileDescriptor)
            val outStream = FileOutputStream(pfd.fileDescriptor)
            val packet = ByteArray(BUFFER_SIZE)

            val filterEngine = DomainFilterEngine(
                blocklistRepository = app.domainBlocklistRepository,
                isAdultProtectionEnabled = { isAdultProtectionEnabled },
                isSafeSearchEnabled = { isSafeSearchEnabled },
                isEmergencyOverrideActive = { isEmergencyOverrideActive }
            )

            while (isActive && vpnInterface != null) {
                try {
                    val length = inStream.read(packet)
                    if (length <= 0) continue

                    if (dnsProcessor.isUdpDnsPacket(packet, length)) {
                        handleUdpDnsPacket(packet, length, outStream, filterEngine, app)
                    }
                } catch (_: Exception) {
                    // Safe error handling; prevent service crash on malformed streams
                }
            }
        }
    }

    private fun handleUdpDnsPacket(
        packet: ByteArray,
        length: Int,
        outStream: FileOutputStream,
        filterEngine: DomainFilterEngine,
        app: FocusGuardApplication
    ) {
        val ihl = (packet[0].toInt() and 0x0F) * 4
        val udpHeaderOffset = ihl
        val dnsOffset = udpHeaderOffset + 8
        val dnsLength = length - dnsOffset

        if (dnsLength < 12) return

        val dnsQuery = dnsProcessor.parseDnsQuery(packet, dnsOffset, dnsLength) ?: return
        val decision = filterEngine.evaluateDomain(dnsQuery.domain)

        when (decision) {
            is DomainFilterDecision.Block -> {
                // Synthesize sinkhole response
                val dnsResponse = dnsProcessor.buildBlockedDnsResponse(dnsQuery, DnsPacketProcessor.DEFAULT_SINKHOLE_IPV4)
                val responsePacket = buildUdpIpResponse(packet, ihl, dnsResponse)
                outStream.write(responsePacket)

                // Record genuine blocked telemetry with debouncing
                val now = System.currentTimeMillis()
                if (dnsQuery.domain != lastBlockedDomain || (now - lastBlockedTimestamp) > 5000L) {
                    lastBlockedDomain = dnsQuery.domain
                    lastBlockedTimestamp = now

                    app.vpnStatusRepository.recordBlockedDns(dnsQuery.domain, now)
                    serviceScope.launch {
                        app.blockEventRepository.recordRealBlockEvent(
                            BlockEventEntity(
                                packageName = "android.net.dns",
                                category = "ADULT_WEB",
                                timestamp = now,
                                detectionSource = "VPN_DNS_FILTER",
                                eventStatus = "BLOCKED"
                            )
                        )
                    }
                }
            }

            is DomainFilterDecision.SafeSearchRedirect -> {
                // Synthesize Safe Search VIP response
                val dnsResponse = dnsProcessor.buildSafeSearchDnsResponse(dnsQuery, decision.redirectIp)
                val responsePacket = buildUdpIpResponse(packet, ihl, dnsResponse)
                outStream.write(responsePacket)
            }

            is DomainFilterDecision.Allow -> {
                // Forward permitted query to protected family-safe upstream resolver
                forwardPermittedDns(packet, dnsOffset, dnsLength, ihl, outStream)
            }
        }
    }

    private fun forwardPermittedDns(
        packet: ByteArray,
        dnsOffset: Int,
        dnsLength: Int,
        ihl: Int,
        outStream: FileOutputStream
    ) {
        // Attempt Primary Family DNS (Cloudflare Family Adult Blocker)
        val response = queryUpstream(packet, dnsOffset, dnsLength, UPSTREAM_DNS_PRIMARY)
            ?: queryUpstream(packet, dnsOffset, dnsLength, UPSTREAM_DNS_SECONDARY)
            ?: queryUpstream(packet, dnsOffset, dnsLength, UPSTREAM_DNS_FALLBACK)

        if (response != null) {
            val responseIpPacket = buildUdpIpResponse(packet, ihl, response)
            outStream.write(responseIpPacket)
        }
    }

    private fun queryUpstream(
        packet: ByteArray,
        dnsOffset: Int,
        dnsLength: Int,
        upstreamIpStr: String
    ): ByteArray? {
        return try {
            DatagramSocket().use { socket ->
                protect(socket) // CRITICAL: Protect upstream socket to avoid infinite VPN routing loops!
                socket.soTimeout = 1500

                val upstreamIp = InetAddress.getByName(upstreamIpStr)
                val sendPacket = DatagramPacket(packet, dnsOffset, dnsLength, upstreamIp, DnsPacketProcessor.DNS_PORT)
                socket.send(sendPacket)

                val receiveBuffer = ByteArray(BUFFER_SIZE)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)

                val dnsResponseBytes = ByteArray(receivePacket.length)
                System.arraycopy(receiveBuffer, 0, dnsResponseBytes, 0, receivePacket.length)
                dnsResponseBytes
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildUdpIpResponse(
        queryIpPacket: ByteArray,
        ihl: Int,
        dnsResponsePayload: ByteArray
    ): ByteArray {
        val totalLength = ihl + 8 + dnsResponsePayload.size
        val response = ByteArray(totalLength)

        // Copy IP Header
        System.arraycopy(queryIpPacket, 0, response, 0, ihl)

        // Swap Source and Destination IP
        System.arraycopy(queryIpPacket, 16, response, 12, 4) // Src = old Dst
        System.arraycopy(queryIpPacket, 12, response, 16, 4) // Dst = old Src

        // Update Total Length in IP Header
        response[2] = ((totalLength shr 8) and 0xFF).toByte()
        response[3] = (totalLength and 0xFF).toByte()

        // Set standard response TTL
        response[8] = 64.toByte()

        // UDP Header: Swap Ports
        val srcPortOffset = ihl
        val dstPortOffset = ihl + 2
        response[srcPortOffset] = queryIpPacket[dstPortOffset]
        response[srcPortOffset + 1] = queryIpPacket[dstPortOffset + 1]
        response[dstPortOffset] = queryIpPacket[srcPortOffset]
        response[dstPortOffset + 1] = queryIpPacket[srcPortOffset + 1]

        // UDP Length
        val udpLength = 8 + dnsResponsePayload.size
        response[ihl + 4] = ((udpLength shr 8) and 0xFF).toByte()
        response[ihl + 5] = (udpLength and 0xFF).toByte()

        // UDP Checksum = 0 (optional in IPv4 UDP)
        response[ihl + 6] = 0
        response[ihl + 7] = 0

        // Compute valid IPv4 Header Checksum (RFC 791)
        response[10] = 0
        response[11] = 0
        val ipChecksum = DnsPacketProcessor.calculateIpChecksum(response, 0, ihl)
        response[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        response[11] = (ipChecksum and 0xFF).toByte()

        // Copy DNS Payload
        System.arraycopy(dnsResponsePayload, 0, response, ihl + 8, dnsResponsePayload.size)

        return response
    }

    private fun stopVpn() {
        val app = application as? FocusGuardApplication
        app?.vpnStatusRepository?.setConnectionState(VpnConnectionState.VPN_DISCONNECTED)

        runCatching { vpnInterface?.close() }
        vpnInterface = null
        stopSelf()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }
}
