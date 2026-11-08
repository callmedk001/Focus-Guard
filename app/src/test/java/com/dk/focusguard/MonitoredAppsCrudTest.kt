package com.dk.focusguard

import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import com.dk.focusguard.data.repository.MonitoredAppsRepositoryImpl
import com.dk.focusguard.fakes.FakeMonitoredAppDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MonitoredAppsCrudTest {

    private lateinit var fakeDao: FakeMonitoredAppDao
    private lateinit var repository: MonitoredAppsRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeMonitoredAppDao()
        repository = MonitoredAppsRepositoryImpl(monitoredAppDao = fakeDao)
    }

    @Test
    fun createMonitoredApp_insertsSuccessfullyAndPersists() = runBlocking {
        val app = MonitoredAppEntity(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            isEnabled = true,
            category = "Social",
            blockShortsOrReelsOnly = true
        )

        val result = repository.addMonitoredApp(app)
        assertTrue(result.isSuccess)

        val allApps = repository.getAllMonitoredApps().first()
        assertEquals(1, allApps.size)
        assertEquals("com.google.android.youtube", allApps[0].packageName)
        assertEquals("YouTube", allApps[0].appName)
        assertTrue(allApps[0].isEnabled)
        assertTrue(allApps[0].blockShortsOrReelsOnly)
    }

    @Test
    fun readMonitoredApps_filtersEnabledAppsCorrectly() = runBlocking {
        val app1 = MonitoredAppEntity("com.google.android.youtube", "YouTube", isEnabled = true)
        val app2 = MonitoredAppEntity("com.instagram.android", "Instagram", isEnabled = false)
        val app3 = MonitoredAppEntity("com.twitter.android", "Twitter", isEnabled = true)

        repository.addMonitoredApp(app1)
        repository.addMonitoredApp(app2)
        repository.addMonitoredApp(app3)

        val all = repository.getAllMonitoredApps().first()
        assertEquals(3, all.size)

        val enabled = repository.getEnabledMonitoredApps().first()
        assertEquals(2, enabled.size)
        assertTrue(enabled.none { it.packageName == "com.instagram.android" })
    }

    @Test
    fun updateMonitoredApp_changesEnabledStatus() = runBlocking {
        val app = MonitoredAppEntity("com.instagram.android", "Instagram", isEnabled = true)
        repository.addMonitoredApp(app)

        val updateResult = repository.updateAppEnabled("com.instagram.android", false)
        assertTrue(updateResult.isSuccess)

        val apps = repository.getAllMonitoredApps().first()
        assertEquals(1, apps.size)
        assertFalse(apps[0].isEnabled)
    }

    @Test
    fun deleteMonitoredApp_removesTargetAppOnly() = runBlocking {
        val app1 = MonitoredAppEntity("com.google.android.youtube", "YouTube", isEnabled = true)
        val app2 = MonitoredAppEntity("com.instagram.android", "Instagram", isEnabled = true)

        repository.addMonitoredApp(app1)
        repository.addMonitoredApp(app2)

        val deleteResult = repository.removeMonitoredApp("com.google.android.youtube")
        assertTrue(deleteResult.isSuccess)

        val remaining = repository.getAllMonitoredApps().first()
        assertEquals(1, remaining.size)
        assertEquals("com.instagram.android", remaining[0].packageName)

        val count = repository.getMonitoredAppsCount().first()
        assertEquals(1, count)
    }

    @Test
    fun resetMonitoredApps_clearsAllApps() = runBlocking {
        repository.addMonitoredApp(MonitoredAppEntity("com.google.android.youtube", "YouTube", isEnabled = true))
        repository.addMonitoredApp(MonitoredAppEntity("com.instagram.android", "Instagram", isEnabled = true))

        val resetResult = repository.resetMonitoredApps()
        assertTrue(resetResult.isSuccess)

        val apps = repository.getAllMonitoredApps().first()
        assertTrue(apps.isEmpty())

        val directCount = repository.getMonitoredAppsCountDirect()
        assertEquals(0, directCount)
    }
}

