package com.dk.focusguard

import com.dk.focusguard.data.repository.AccessibilityStatusRepositoryImpl
import com.dk.focusguard.domain.repository.AccessibilityServiceState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for:
 * 4. Service status transitions.
 * 5. Permission granted versus service connected distinction.
 * 9. Service disconnection handling.
 * 10. Repository persistence and status restoration.
 */
class AccessibilityStatusManagementTest {

    private lateinit var repository: AccessibilityStatusRepositoryImpl

    @Before
    fun setUp() {
        repository = AccessibilityStatusRepositoryImpl()
    }

    @Test
    fun test4_serviceStatusTransitions_progressesThroughLifecycleAccurately() = runBlocking {
        // Initial state: permission not granted, service not connected
        assertEquals(
            AccessibilityServiceState.PERMISSION_NOT_GRANTED,
            repository.serviceState.first()
        )

        // Step 1: User enables permission in Android Settings, but service not yet bound by OS
        repository.setPermissionGrantedForTesting(true)
        assertEquals(
            AccessibilityServiceState.PERMISSION_GRANTED_DISCONNECTED,
            repository.serviceState.first()
        )

        // Step 2: Android binds and connects the service
        repository.setServiceConnected(true)
        assertEquals(
            AccessibilityServiceState.CONNECTED_AND_MONITORING,
            repository.serviceState.first()
        )

        // Step 3: Service is unbound or system kills the process
        repository.setServiceConnected(false)
        assertEquals(
            AccessibilityServiceState.PERMISSION_GRANTED_DISCONNECTED,
            repository.serviceState.first()
        )

        // Step 4: User revokes permission in Android Settings
        repository.setPermissionGrantedForTesting(false)
        assertEquals(
            AccessibilityServiceState.PERMISSION_NOT_GRANTED,
            repository.serviceState.first()
        )
    }

    @Test
    fun test5_permissionGrantedVersusServiceConnectedDistinction_neverFalselyReportsActive() = runBlocking {
        // Initial state
        assertFalse(repository.isPermissionGranted.first())
        assertFalse(repository.isServiceConnected.first())
        assertEquals(AccessibilityServiceState.PERMISSION_NOT_GRANTED, repository.serviceState.first())

        // Connect service directly
        repository.setServiceConnected(true)
        assertTrue(repository.isServiceConnected.first())

        // However, if permission flag is false, overall state must remain PERMISSION_NOT_GRANTED
        assertEquals(AccessibilityServiceState.PERMISSION_NOT_GRANTED, repository.serviceState.first())

        // Reset
        repository.setServiceConnected(false)
        assertFalse(repository.isServiceConnected.first())
    }

    @Test
    fun test9_serviceDisconnectionHandling_clearsForegroundAppAndRevertsState() = runBlocking {
        repository.setServiceConnected(true)
        repository.updateForegroundApp(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            timestamp = 1000L
        )

        assertEquals("com.google.android.youtube", repository.currentForegroundPackage.first())
        assertEquals("YouTube", repository.currentForegroundAppName.first())
        assertEquals(1000L, repository.lastEventTimestamp.first())

        // Simulate disconnection
        repository.setServiceConnected(false)

        assertFalse("Service must be marked disconnected", repository.isServiceConnected.first())
        assertNull("Foreground package must be cleared on disconnect", repository.currentForegroundPackage.first())
        assertNull("Foreground app name must be cleared on disconnect", repository.currentForegroundAppName.first())
    }

    @Test
    fun test10_repositoryStatusUpdatesAndRestoration_tracksTelemetryAccurately() = runBlocking {
        repository.updateForegroundApp("com.instagram.android", "Instagram", 5000L)

        assertEquals("com.instagram.android", repository.currentForegroundPackage.first())
        assertEquals("Instagram", repository.currentForegroundAppName.first())
        assertEquals(5000L, repository.lastEventTimestamp.first())

        repository.clearForegroundApp()
        assertNull(repository.currentForegroundPackage.first())
        assertNull(repository.currentForegroundAppName.first())
        // Timestamp is preserved for diagnostics
        assertEquals(5000L, repository.lastEventTimestamp.first())
    }
}
