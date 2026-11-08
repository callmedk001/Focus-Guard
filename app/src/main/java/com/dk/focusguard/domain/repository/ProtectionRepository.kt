package com.dk.focusguard.domain.repository

import com.dk.focusguard.domain.model.BlockedApp
import com.dk.focusguard.domain.model.ProtectionRule
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for managing blocked apps and specific protection rules.
 */
interface ProtectionRepository {
    fun getAllBlockedApps(): Flow<List<BlockedApp>>
    fun getActiveBlockedApps(): Flow<List<BlockedApp>>
    suspend fun setAppBlocked(blockedApp: BlockedApp)
    suspend fun removeBlockedApp(packageName: String)
    suspend fun toggleAppBlock(packageName: String, isBlocked: Boolean)

    fun getProtectionRules(): Flow<List<ProtectionRule>>
    suspend fun updateProtectionRule(ruleId: String, isEnabled: Boolean)
}

