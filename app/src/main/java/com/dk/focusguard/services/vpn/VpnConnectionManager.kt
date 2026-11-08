package com.dk.focusguard.services.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.dk.focusguard.domain.repository.VpnConnectionState
import com.dk.focusguard.domain.repository.VpnStatusRepository

/**
 * Manager handling Android VpnService permission preparation flow,
 * service start/stop triggers, and lifecycle synchronization.
 */
class VpnConnectionManager(
    private val vpnStatusRepository: VpnStatusRepository
) {

    /**
     * Checks whether the user has already granted VPN consent to DK Focus Guard.
     * Returns true if permission is already granted; false if the system dialog is required.
     */
    fun isVpnPermissionGranted(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    /**
     * Returns the system VPN preparation Intent, or null if already granted.
     */
    fun getVpnConsentIntent(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Starts the FocusGuardVpnService.
     * Must only be called after verifying isVpnPermissionGranted.
     */
    fun startVpn(context: Context) {
        val intent = Intent(context, FocusGuardVpnService::class.java).apply {
            action = FocusGuardVpnService.ACTION_START
        }
        runCatching {
            context.startService(intent)
        }
    }

    /**
     * Stops the FocusGuardVpnService and resets VPN connection state.
     */
    fun stopVpn(context: Context) {
        val intent = Intent(context, FocusGuardVpnService::class.java).apply {
            action = FocusGuardVpnService.ACTION_STOP
        }
        context.startService(intent)
        vpnStatusRepository.setConnectionState(VpnConnectionState.VPN_DISCONNECTED)
    }
}

