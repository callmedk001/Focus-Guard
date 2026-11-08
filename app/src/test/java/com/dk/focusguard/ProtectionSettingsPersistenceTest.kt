package com.dk.focusguard

import com.dk.focusguard.data.local.entity.ProtectionSettingsEntity
import com.dk.focusguard.data.repository.ProtectionSettingsRepositoryImpl
import com.dk.focusguard.fakes.FakeProtectionSettingsDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProtectionSettingsPersistenceTest {

    private lateinit var fakeDao: FakeProtectionSettingsDao
    private lateinit var repository: ProtectionSettingsRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeProtectionSettingsDao(
            ProtectionSettingsEntity(
                id = 1,
                blockYouTubeShorts = true,
                blockInstagramReels = true,
                blockAdultWebsites = true,
                isStrictModeEnabled = false,
                isMasterProtectionEnabled = true,
                lastUpdated = 1000L
            )
        )
        repository = ProtectionSettingsRepositoryImpl(dao = fakeDao)
    }

    @Test
    fun getProtectionSettings_returnsInitialDefaultSettings() = runBlocking {
        val settings = repository.getProtectionSettings().first()
        assertTrue(settings.blockYouTubeShorts)
        assertTrue(settings.blockInstagramReels)
        assertTrue(settings.blockAdultWebsites)
        assertFalse(settings.isStrictModeEnabled)
        assertTrue(settings.isMasterProtectionEnabled)
    }

    @Test
    fun setYouTubeShortsProtection_persistsStateCorrectly() = runBlocking {
        val result = repository.setYouTubeShortsProtection(false)
        assertTrue(result.isSuccess)

        val updated = repository.getProtectionSettings().first()
        assertFalse(updated.blockYouTubeShorts)
        assertTrue(updated.blockInstagramReels) // Untouched
    }

    @Test
    fun setInstagramReelsProtection_persistsStateCorrectly() = runBlocking {
        val result = repository.setInstagramReelsProtection(false)
        assertTrue(result.isSuccess)

        val updated = repository.getProtectionSettings().first()
        assertFalse(updated.blockInstagramReels)
        assertTrue(updated.blockYouTubeShorts) // Untouched
    }

    @Test
    fun setAdultWebsiteProtection_persistsStateCorrectly() = runBlocking {
        val result = repository.setAdultWebsiteProtection(false)
        assertTrue(result.isSuccess)

        val updated = repository.getProtectionSettings().first()
        assertFalse(updated.blockAdultWebsites)
    }

    @Test
    fun setStrictMode_persistsStateCorrectly() = runBlocking {
        val result = repository.setStrictMode(true)
        assertTrue(result.isSuccess)

        val updated = repository.getProtectionSettings().first()
        assertTrue(updated.isStrictModeEnabled)
    }

    @Test
    fun setMasterProtection_persistsStateCorrectly() = runBlocking {
        val result = repository.setMasterProtection(false)
        assertTrue(result.isSuccess)

        val updated = repository.getProtectionSettings().first()
        assertFalse(updated.isMasterProtectionEnabled)
    }

    @Test
    fun resetProtectionSettings_restoresDefaultValues() = runBlocking {
        // First modify everything
        repository.setYouTubeShortsProtection(false)
        repository.setInstagramReelsProtection(false)
        repository.setAdultWebsiteProtection(false)
        repository.setStrictMode(true)
        repository.setMasterProtection(false)

        // Then reset
        val result = repository.resetProtectionSettings()
        assertTrue(result.isSuccess)

        val reset = repository.getProtectionSettings().first()
        assertTrue(reset.blockYouTubeShorts)
        assertTrue(reset.blockInstagramReels)
        assertTrue(reset.blockAdultWebsites)
        assertFalse(reset.isStrictModeEnabled)
        assertTrue(reset.isMasterProtectionEnabled)
    }

    @Test
    fun getProtectionSettingsDirect_returnsSynchronousEntity() = runBlocking {
        val direct = repository.getProtectionSettingsDirect()
        assertNotNull(direct)
        assertEquals(1, direct?.id)
        assertTrue(direct?.blockYouTubeShorts == true)
    }
}

