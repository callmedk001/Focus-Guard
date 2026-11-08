package com.dk.focusguard.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * State representing the Android VpnService lifecycle and connection status.
 */
enum class VpnConnectionState {
    /** User has not yet granted Android VpnService system dialog permission */
    VPN_PERMISSION_REQUIRED,

    /** VPN service is currently inactive / stopped */
    VPN_DISCONNECTED,

    /** VPN interface is currently establishing */
    VPN_CONNECTING,

    /** VPN TUN interface is established and filtering DNS queries locally */
    VPN_CONNECTED,

    /** An error occurred while establishing or operating the VPN interface */
    VPN_ERROR
}

/**
 * Contract managing real-time VPN health, DNS interception metrics,
 * and connection states for the Adult Website Filter.
 */
interface VpnStatusRepository {

    val connectionState: Flow<VpnConnectionState>

    val isVpnConnected: Flow<Boolean>

    val isFilteringActive: Flow<Boolean>

    val blockedDnsCount: Flow<Int>

    val lastBlockedTimestamp: Flow<Long>

    val isCurrentlyConnected: Boolean

    val currentBlockedDnsCount: Int

    val currentLastBlockedTimestamp: Long

    fun setConnectionState(state: VpnConnectionState)

    fun recordBlockedDns(domain: String, timestamp: Long = System.currentTimeMillis())

    fun resetTelemetry()
}
