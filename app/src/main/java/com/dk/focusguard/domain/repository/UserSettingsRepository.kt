package com.dk.focusguard.domain.repository

import com.dk.focusguard.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for reading and persisting user preferences.
 */
interface UserSettingsRepository {
    fun getUserSettings(): Flow<UserSettings>
    suspend fun setMasterProtection(enabled: Boolean)
    suspend fun setStrictMode(enabled: Boolean)
    suspend fun setBlockYouTubeShorts(enabled: Boolean)
    suspend fun setBlockInstagramReels(enabled: Boolean)
    suspend fun setBlockAdultWebsites(enabled: Boolean)
    suspend fun setDefaultFocusDuration(minutes: Int)
    suspend fun setDailyFocusGoal(minutes: Int)
    suspend fun setDarkTheme(enabled: Boolean)
}

