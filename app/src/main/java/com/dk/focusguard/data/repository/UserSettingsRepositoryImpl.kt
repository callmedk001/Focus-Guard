package com.dk.focusguard.data.repository

import com.dk.focusguard.data.preferences.AppPreferencesManager
import com.dk.focusguard.domain.model.UserSettings
import com.dk.focusguard.domain.repository.UserSettingsRepository
import kotlinx.coroutines.flow.Flow

class UserSettingsRepositoryImpl(
    private val preferencesManager: AppPreferencesManager
) : UserSettingsRepository {

    override fun getUserSettings(): Flow<UserSettings> {
        return preferencesManager.userSettingsFlow
    }

    override suspend fun setMasterProtection(enabled: Boolean) {
        preferencesManager.setMasterProtection(enabled)
    }

    override suspend fun setStrictMode(enabled: Boolean) {
        preferencesManager.setStrictMode(enabled)
    }

    override suspend fun setBlockYouTubeShorts(enabled: Boolean) {
        preferencesManager.setBlockYouTubeShorts(enabled)
    }

    override suspend fun setBlockInstagramReels(enabled: Boolean) {
        preferencesManager.setBlockInstagramReels(enabled)
    }

    override suspend fun setBlockAdultWebsites(enabled: Boolean) {
        preferencesManager.setBlockAdultWebsites(enabled)
    }

    override suspend fun setDefaultFocusDuration(minutes: Int) {
        preferencesManager.setDefaultFocusDuration(minutes)
    }

    override suspend fun setDailyFocusGoal(minutes: Int) {
        preferencesManager.setDailyFocusGoal(minutes)
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        preferencesManager.setDarkTheme(enabled)
    }
}

