package com.dk.focusguard

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dk.focusguard.domain.policy.ProtectionPolicyEngine
import com.dk.focusguard.services.vpn.DnsPacketProcessor
import com.dk.focusguard.services.vpn.ParsedDnsQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/**
 * On-device Android Instrumentation Smoke Test.
 * Validates execution inside Android 14 ART runtime on the physical Oppo A3x 5G.
 */
@RunWith(AndroidJUnit4::class)
class FocusGuardDeviceSmokeTest {

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.dk.focusguard", appContext.packageName)

        val app = appContext.applicationContext as? FocusGuardApplication
        assertNotNull("FocusGuardApplication must initialize on physical device", app)
        assertNotNull("Room database must initialize", app?.database)
    }

    @Test
    fun verifyProtectionPolicyEngineOnDevice() {
        val schedule = com.dk.focusguard.data.local.entity.FocusScheduleEntity(
            id = 1,
            name = "Test Schedule",
            startTime = "09:00",
            endTime = "17:00",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri,Sat,Sun",
            isEnabled = true
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }

        val active = ProtectionPolicyEngine.isScheduleActiveAt(schedule, cal)
        assertTrue("Schedule at 12:00 must be active", active)
    }

    @Test
    fun verifyDnsPacketProcessorOnDevice() {
        val processor = DnsPacketProcessor()
        val query = ParsedDnsQuery(
            transactionId = 0x1234,
            domain = "device-test.com",
            queryType = DnsPacketProcessor.TYPE_A,
            queryClass = DnsPacketProcessor.CLASS_IN,
            isQuery = true,
            rawQuestionBytes = byteArrayOf(
                0x0B, 'd'.code.toByte(), 'e'.code.toByte(), 'v'.code.toByte(), 'i'.code.toByte(), 'c'.code.toByte(), 'e'.code.toByte(),
                '-'.code.toByte(), 't'.code.toByte(), 'e'.code.toByte(), 's'.code.toByte(), 't'.code.toByte(),
                0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
                0x00,
                0x00, 0x01,
                0x00, 0x01
            )
        )
        val response = processor.buildBlockedDnsResponse(query)
        assertNotNull(response)
        assertTrue("Response must have valid length", response.size > 20)
    }
}

