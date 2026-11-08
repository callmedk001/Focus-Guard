package com.dk.focusguard.data.repository

import com.dk.focusguard.data.local.dao.BlockedAppDao
import com.dk.focusguard.data.local.dao.ProtectionRuleDao
import com.dk.focusguard.data.local.entity.BlockedAppEntity
import com.dk.focusguard.domain.model.BlockedApp
import com.dk.focusguard.domain.model.ProtectionRule
import com.dk.focusguard.domain.repository.ProtectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProtectionRepositoryImpl(
    private val blockedAppDao: BlockedAppDao,
    private val protectionRuleDao: ProtectionRuleDao
) : ProtectionRepository {

    override fun getAllBlockedApps(): Flow<List<BlockedApp>> {
        return blockedAppDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveBlockedApps(): Flow<List<BlockedApp>> {
        return blockedAppDao.getActiveBlockedApps().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun setAppBlocked(blockedApp: BlockedApp) {
        blockedAppDao.insertOrUpdate(BlockedAppEntity.fromDomain(blockedApp))
    }

    override suspend fun removeBlockedApp(packageName: String) {
        blockedAppDao.delete(packageName)
    }

    override suspend fun toggleAppBlock(packageName: String, isBlocked: Boolean) {
        blockedAppDao.updateBlockStatus(packageName, isBlocked)
    }

    override fun getProtectionRules(): Flow<List<ProtectionRule>> {
        return protectionRuleDao.getAllRules().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun updateProtectionRule(ruleId: String, isEnabled: Boolean) {
        protectionRuleDao.updateRuleStatus(ruleId, isEnabled)
    }
}

