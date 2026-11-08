package com.dk.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing the global protection configuration.
 * Single-row table with ID = 1.
 */
@Entity(tableName = "protection_settings")
data class ProtectionSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val blockYouTubeShorts: Boolean = true,
    val blockInstagramReels: Boolean = true,
    val blockAdultWebsites: Boolean = true,
    val isStrictModeEnabled: Boolean = false,
    val isMasterProtectionEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

