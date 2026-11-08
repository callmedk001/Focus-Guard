package com.dk.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dk.focusguard.domain.model.AppCategory
import com.dk.focusguard.domain.model.ProtectionRule

/**
 * Room entity representing fine-grained guard rules (e.g. Shorts, Reels, Adult Web).
 */
@Entity(tableName = "protection_rules")
data class ProtectionRuleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val isEnabled: Boolean,
    val category: String
) {
    fun toDomain(): ProtectionRule {
        return ProtectionRule(
            id = id,
            title = title,
            description = description,
            isEnabled = isEnabled,
            category = runCatching { AppCategory.valueOf(category) }.getOrDefault(AppCategory.OTHER)
        )
    }

    companion object {
        fun fromDomain(domain: ProtectionRule): ProtectionRuleEntity {
            return ProtectionRuleEntity(
                id = domain.id,
                title = domain.title,
                description = domain.description,
                isEnabled = domain.isEnabled,
                category = domain.category.name
            )
        }
    }
}

