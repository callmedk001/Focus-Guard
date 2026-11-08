package com.dk.focusguard.data.repository

import com.dk.focusguard.domain.repository.VpnConnectionState
import com.dk.focusguard.domain.repository.VpnStatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Thread-safe, reactive implementation of VpnStatusRepository.
 */
class VpnStatusRepositoryImpl : VpnStatusRepository {

    private val _connectionState = MutableStateFlow(VpnConnectionState.VPN_DISCONNECTED)
    override val connectionState: Flow<VpnConnectionState> = _connectionState.asStateFlow()

    private val _blockedDnsCount = MutableStateFlow(0)
    override val blockedDnsCount: Flow<Int> = _blockedDnsCount.asStateFlow()

    private val _lastBlockedTimestamp = MutableStateFlow(0L)
    override val lastBlockedTimestamp: Flow<Long> = _lastBlockedTimestamp.asStateFlow()

    override val isVpnConnected: Flow<Boolean> = _connectionState.map {
        it == VpnConnectionState.VPN_CONNECTED
    }

    override val isFilteringActive: Flow<Boolean> = _connectionState.map {
        it == VpnConnectionState.VPN_CONNECTED
    }

    override val isCurrentlyConnected: Boolean
        get() = _connectionState.value == VpnConnectionState.VPN_CONNECTED

    override val currentBlockedDnsCount: Int
        get() = _blockedDnsCount.value

    override val currentLastBlockedTimestamp: Long
        get() = _lastBlockedTimestamp.value

    override fun setConnectionState(state: VpnConnectionState) {
        _connectionState.value = state
    }

    override fun recordBlockedDns(domain: String, timestamp: Long) {
        _blockedDnsCount.value += 1
        _lastBlockedTimestamp.value = timestamp
    }

    override fun resetTelemetry() {
        _blockedDnsCount.value = 0
        _lastBlockedTimestamp.value = 0L
    }
}
