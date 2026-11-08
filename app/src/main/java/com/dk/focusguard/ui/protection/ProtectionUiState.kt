package com.dk.focusguard.ui.protection

import com.dk.focusguard.domain.model.BlockedApp
import com.dk.focusguard.domain.model.ProtectionRule
import com.dk.focusguard.domain.repository.VpnConnectionState

/**
 * State representing the Protection configuration screen.
 */
data class ProtectionUiState(
    val rules: List<ProtectionRule> = emptyList(),
    val blockedApps: List<BlockedApp> = emptyList(),
    val isMasterProtectionEnabled: Boolean = true,
    val isStrictModeEnabled: Boolean = false,
    val isLoading: Boolean = false,
    // YouTube Shorts (Phase 6)
    val youtubeInterruptionCount: Int = 0,
    val lastYoutubeDetectionTimestamp: Long = 0L,
    val isEmergencyOverrideActive: Boolean = false,
    val isYouTubeForegroundActive: Boolean = false,
    // Instagram Reels (Phase 7)
    val instagramInterruptionCount: Int = 0,
    val lastInstagramDetectionTimestamp: Long = 0L,
    val isInstagramForegroundActive: Boolean = false,
    // Adult Website Filter & Safe Search (Phase 8)
    val isSafeSearchEnabled: Boolean = true,
    val vpnConnectionState: VpnConnectionState = VpnConnectionState.VPN_DISCONNECTED,
    val adultBlocklistCount: Int = 30,
    val adultBlockedDnsCount: Int = 0,
    val lastAdultBlockedTimestamp: Long = 0L,
    val customBlockedDomains: Set<String> = emptySet()
)
