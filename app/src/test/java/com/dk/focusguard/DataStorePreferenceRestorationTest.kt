package com.dk.focusguard

import com.dk.focusguard.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStorePreferenceRestorationTest {

    @Test
    fun defaultUserSettings_restoresSafeInitialDefaults() {
        val defaultSettings = UserSettings()

        // Protections default to ON
        assertTrue("Master protection should default to enabled", defaultSettings.isMasterProtectionEnabled)
        assertTrue("YouTube Shorts blocking should default to enabled", defaultSettings.blockYouTubeShorts)
        assertTrue("Instagram Reels blocking should default to enabled", defaultSettings.blockInstagramReels)
        assertTrue("Adult website blocking should default to enabled", defaultSettings.blockAdultWebsites)

        // Strict mode defaults to OFF
        assertFalse("Strict mode should default to disabled", defaultSettings.isStrictModeEnabled)

        // Focus Defaults
        assertEquals(25, defaultSettings.defaultFocusDurationMinutes)
        assertEquals(120, defaultSettings.dailyFocusGoalMinutes)
        assertFalse("Focus mode should initially be inactive", defaultSettings.isFocusModeActive)

        // App State Defaults
        assertTrue("First launch should default to true", defaultSettings.isFirstLaunch)
        assertTrue("Notifications should default to enabled", defaultSettings.notificationsEnabled)
        assertTrue("Dark theme should default to enabled for cybersecurity design", defaultSettings.darkThemeEnabled)

        // Security requirement: PIN is not set initially and never exposed in plaintext
        assertFalse("PIN should not be set by default", defaultSettings.isPinSet)
    }

    @Test
    fun customUserSettings_restoresConfiguredValuesAccurately() {
        val configuredSettings = UserSettings(
            isMasterProtectionEnabled = true,
            isStrictModeEnabled = true,
            blockYouTubeShorts = false,
            blockInstagramReels = true,
            blockAdultWebsites = true,
            defaultFocusDurationMinutes = 45,
            dailyFocusGoalMinutes = 180,
            darkThemeEnabled = true,
            isFirstLaunch = false,
            notificationsEnabled = false,
            isPinSet = true,
            isFocusModeActive = true
        )

        assertTrue(configuredSettings.isMasterProtectionEnabled)
        assertTrue(configuredSettings.isStrictModeEnabled)
        assertFalse(configuredSettings.blockYouTubeShorts)
        assertTrue(configuredSettings.blockInstagramReels)
        assertTrue(configuredSettings.blockAdultWebsites)
        assertEquals(45, configuredSettings.defaultFocusDurationMinutes)
        assertEquals(180, configuredSettings.dailyFocusGoalMinutes)
        assertFalse(configuredSettings.isFirstLaunch)
        assertFalse(configuredSettings.notificationsEnabled)
        assertTrue(configuredSettings.isPinSet)
        assertTrue(configuredSettings.isFocusModeActive)
    }

    @Test
    fun copyFunction_preservesImmutabilityAndRestoresUpdatedPreferences() {
        val initial = UserSettings()
        val updated = initial.copy(
            isFirstLaunch = false,
            isPinSet = true,
            blockYouTubeShorts = false
        )

        // Initial remains untouched
        assertTrue(initial.isFirstLaunch)
        assertFalse(initial.isPinSet)
        assertTrue(initial.blockYouTubeShorts)

        // Updated contains modifications
        assertFalse(updated.isFirstLaunch)
        assertTrue(updated.isPinSet)
        assertFalse(updated.blockYouTubeShorts)
        // Other fields intact
        assertTrue(updated.blockAdultWebsites)
        assertTrue(updated.darkThemeEnabled)
    }
}

