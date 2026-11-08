package com.dk.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity storing genuine intercepted block events.
 *
 * NOTE: Must only contain real events produced by protection services.
 * Never populated with fake or mock data.
 */
@Entity(
    tableName = "block_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["packageName"]),
        Index(value = ["category"])
    ]
)
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val category: String, // e.g. "YOUTUBE_SHORTS", "INSTAGRAM_REELS", "ADULT_WEB", "APP_LOCK"
    val timestamp: Long = System.currentTimeMillis(),
    val detectionSource: String, // e.g. "ACCESSIBILITY", "VPN", "USAGE_STATS", "SYSTEM_OVERLAY"
    val eventStatus: String = "INTERCEPTED" // "INTERCEPTED", "WARNED", "FAILED"
)

