package com.dk.focusguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing scheduled focus windows.
 */
@Entity(tableName = "focus_schedules")
data class FocusScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startTime: String,
    val endTime: String,
    val selectedWeekdays: String,
    val isEnabled: Boolean = true,
    val selectedAppPackages: String = ""
)

