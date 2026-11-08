package com.dk.focusguard.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dk.focusguard.domain.model.UserSettings
import com.dk.focusguard.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

/**
 * DataStore implementation managing persistent user preferences and defense settings.
 * All preference modifications and reads use Kotlin Coroutines and Flows.
 * Sensitive data such as PIN hashes are securely handled without plaintext storage.
 */
class AppPreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val MASTER_PROTECTION = booleanPreferencesKey("key_master_protection")
        val STRICT_MODE = booleanPreferencesKey("key_strict_mode")
        val BLOCK_YT_SHORTS = booleanPreferencesKey("key_block_yt_shorts")
        val BLOCK_IG_REELS = booleanPreferencesKey("key_block_ig_reels")
        val BLOCK_ADULT = booleanPreferencesKey("key_block_adult")
        val DEFAULT_FOCUS_DURATION = intPreferencesKey("key_default_focus_duration")
        val DAILY_FOCUS_GOAL = intPreferencesKey("key_daily_focus_goal")
        val DARK_THEME = booleanPreferencesKey("key_dark_theme")

        // Phase 3 Extended Preferences
        val IS_FIRST_LAUNCH = booleanPreferencesKey("key_is_first_launch")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("key_notifications_enabled")
        val FOCUS_MODE_ACTIVE = booleanPreferencesKey("key_focus_mode_active")
        val IS_PIN_SET = booleanPreferencesKey("key_is_pin_set")
        val PIN_SALT_OR_HASH = stringPreferencesKey("key_pin_salt_hash") // Never stores plaintext PIN

        // Phase 8 Adult Website Filter & Safe Search Preferences
        val SAFE_SEARCH_ENABLED = booleanPreferencesKey("key_safe_search_enabled")
        val CUSTOM_BLOCKED_DOMAINS = stringSetPreferencesKey("key_custom_blocked_domains")
    }

    private val safeData: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val userSettingsFlow: Flow<UserSettings> = safeData.map { preferences ->
        UserSettings(
            isMasterProtectionEnabled = preferences[PreferencesKeys.MASTER_PROTECTION] ?: true,
            isStrictModeEnabled = preferences[PreferencesKeys.STRICT_MODE] ?: false,
            blockYouTubeShorts = preferences[PreferencesKeys.BLOCK_YT_SHORTS] ?: true,
            blockInstagramReels = preferences[PreferencesKeys.BLOCK_IG_REELS] ?: true,
            blockAdultWebsites = preferences[PreferencesKeys.BLOCK_ADULT] ?: true,
            defaultFocusDurationMinutes = preferences[PreferencesKeys.DEFAULT_FOCUS_DURATION]
                ?: Constants.DEFAULT_FOCUS_DURATION_MINUTES,
            dailyFocusGoalMinutes = preferences[PreferencesKeys.DAILY_FOCUS_GOAL]
                ?: Constants.DEFAULT_DAILY_GOAL_MINUTES,
            darkThemeEnabled = preferences[PreferencesKeys.DARK_THEME] ?: true,
            isFirstLaunch = preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true,
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            isPinSet = preferences[PreferencesKeys.IS_PIN_SET] ?: false,
            isFocusModeActive = preferences[PreferencesKeys.FOCUS_MODE_ACTIVE] ?: false
        )
    }.distinctUntilChanged()

    // Specific Granular Observation Flows
    val isMasterProtectionEnabled: Flow<Boolean> = safeData
        .map { it[PreferencesKeys.MASTER_PROTECTION] ?: true }
        .distinctUntilChanged()

    val isStrictModeEnabled: Flow<Boolean> = safeData
        .map { it[PreferencesKeys.STRICT_MODE] ?: false }
        .distinctUntilChanged()

    val isFocusModeActive: Flow<Boolean> = safeData
        .map { it[PreferencesKeys.FOCUS_MODE_ACTIVE] ?: false }
        .distinctUntilChanged()

    val isFirstLaunch: Flow<Boolean> = safeData
        .map { it[PreferencesKeys.IS_FIRST_LAUNCH] ?: true }
        .distinctUntilChanged()

    val isPinSet: Flow<Boolean> = safeData
        .map { it[PreferencesKeys.IS_PIN_SET] ?: false }
        .distinctUntilChanged()

    val notificationsEnabled: Flow<Boolean> = safeData
        .map { it[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }
        .distinctUntilChanged()

    val isSafeSearchEnabled: Flow<Boolean> = safeData
        .map { it[PreferencesKeys.SAFE_SEARCH_ENABLED] ?: true }
        .distinctUntilChanged()

    val customBlockedDomains: Flow<Set<String>> = safeData
        .map { it[PreferencesKeys.CUSTOM_BLOCKED_DOMAINS] ?: emptySet() }
        .distinctUntilChanged()

    suspend fun setSafeSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SAFE_SEARCH_ENABLED] = enabled }
    }

    suspend fun addCustomBlockedDomain(domain: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.CUSTOM_BLOCKED_DOMAINS] ?: emptySet()
            prefs[PreferencesKeys.CUSTOM_BLOCKED_DOMAINS] = current + domain.lowercase().trim()
        }
    }

    suspend fun removeCustomBlockedDomain(domain: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.CUSTOM_BLOCKED_DOMAINS] ?: emptySet()
            prefs[PreferencesKeys.CUSTOM_BLOCKED_DOMAINS] = current - domain.lowercase().trim()
        }
    }

    suspend fun setCustomBlockedDomains(domains: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CUSTOM_BLOCKED_DOMAINS] = domains.map { it.lowercase().trim() }.toSet()
        }
    }

    suspend fun setMasterProtection(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.MASTER_PROTECTION] = enabled }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.STRICT_MODE] = enabled }
    }

    suspend fun setBlockYouTubeShorts(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BLOCK_YT_SHORTS] = enabled }
    }

    suspend fun setBlockInstagramReels(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BLOCK_IG_REELS] = enabled }
    }

    suspend fun setBlockAdultWebsites(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BLOCK_ADULT] = enabled }
    }

    suspend fun setDefaultFocusDuration(minutes: Int) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_FOCUS_DURATION] = minutes }
    }

    suspend fun setDailyFocusGoal(minutes: Int) {
        context.dataStore.edit { it[PreferencesKeys.DAILY_FOCUS_GOAL] = minutes }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DARK_THEME] = enabled }
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { it[PreferencesKeys.IS_FIRST_LAUNCH] = false }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setFocusModeActive(active: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FOCUS_MODE_ACTIVE] = active }
    }

    /**
     * Records PIN setup state with a cryptographic hash representation.
     * The actual user PIN is NEVER stored in plaintext.
     */
    suspend fun setPinHash(hashedPin: String) {
        context.dataStore.edit {
            it[PreferencesKeys.IS_PIN_SET] = true
            it[PreferencesKeys.PIN_SALT_OR_HASH] = hashedPin
        }
    }

    suspend fun clearPin() {
        context.dataStore.edit {
            it[PreferencesKeys.IS_PIN_SET] = false
            it.remove(PreferencesKeys.PIN_SALT_OR_HASH)
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
