package com.dk.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dk.focusguard.data.local.entity.ProtectionRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectionRuleDao {

    @Query("SELECT * FROM protection_rules ORDER BY id ASC")
    fun getAllRules(): Flow<List<ProtectionRuleEntity>>

    @Query("SELECT * FROM protection_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: String): ProtectionRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: ProtectionRuleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultRules(rules: List<ProtectionRuleEntity>)

    @Query("UPDATE protection_rules SET isEnabled = :isEnabled WHERE id = :ruleId")
    suspend fun updateRuleStatus(ruleId: String, isEnabled: Boolean)
}

