package com.dk.focusguard

import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.data.repository.FocusScheduleRepositoryImpl
import com.dk.focusguard.fakes.FakeFocusScheduleDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FocusSchedulePersistenceTest {

    private lateinit var fakeDao: FakeFocusScheduleDao
    private lateinit var repository: FocusScheduleRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeFocusScheduleDao()
        repository = FocusScheduleRepositoryImpl(dao = fakeDao)
    }

    @Test
    fun saveSchedule_insertsAndGeneratesId() = runBlocking {
        val schedule = FocusScheduleEntity(
            name = "Deep Work Block",
            startTime = "09:00",
            endTime = "12:00",
            selectedWeekdays = "Mon,Tue,Wed,Thu,Fri",
            isEnabled = true,
            selectedAppPackages = "com.google.android.youtube,com.instagram.android"
        )

        val result = repository.saveSchedule(schedule)
        assertTrue(result.isSuccess)
        val generatedId = result.getOrThrow()
        assertTrue(generatedId > 0)

        val schedules = repository.getAllSchedules().first()
        assertEquals(1, schedules.size)
        assertEquals("Deep Work Block", schedules[0].name)
        assertEquals("09:00", schedules[0].startTime)
        assertEquals("12:00", schedules[0].endTime)
        assertEquals("Mon,Tue,Wed,Thu,Fri", schedules[0].selectedWeekdays)
        assertTrue(schedules[0].isEnabled)
    }

    @Test
    fun getEnabledSchedules_returnsOnlyActiveSchedules() = runBlocking {
        val activeSchedule = FocusScheduleEntity(
            name = "Morning Protocol",
            startTime = "08:00",
            endTime = "10:00",
            selectedWeekdays = "Mon,Wed",
            isEnabled = true
        )
        val disabledSchedule = FocusScheduleEntity(
            name = "Weekend Study",
            startTime = "14:00",
            endTime = "18:00",
            selectedWeekdays = "Sat,Sun",
            isEnabled = false
        )

        repository.saveSchedule(activeSchedule)
        repository.saveSchedule(disabledSchedule)

        val all = repository.getAllSchedules().first()
        assertEquals(2, all.size)

        val enabled = repository.getEnabledSchedules().first()
        assertEquals(1, enabled.size)
        assertEquals("Morning Protocol", enabled[0].name)
    }

    @Test
    fun setScheduleEnabled_togglesStateSuccessfully() = runBlocking {
        val schedule = FocusScheduleEntity(
            name = "Night Focus",
            startTime = "21:00",
            endTime = "23:00",
            selectedWeekdays = "Mon,Tue,Wed",
            isEnabled = true
        )
        val id = repository.saveSchedule(schedule).getOrThrow()

        val toggleResult = repository.setScheduleEnabled(id, false)
        assertTrue(toggleResult.isSuccess)

        val schedules = repository.getAllSchedules().first()
        assertFalse(schedules[0].isEnabled)

        val enabledSchedules = repository.getEnabledSchedules().first()
        assertTrue(enabledSchedules.isEmpty())
    }

    @Test
    fun deleteSchedule_removesSpecificSchedule() = runBlocking {
        val id1 = repository.saveSchedule(FocusScheduleEntity(name = "Block 1", startTime = "10:00", endTime = "11:00", selectedWeekdays = "Mon", isEnabled = true)).getOrThrow()
        val id2 = repository.saveSchedule(FocusScheduleEntity(name = "Block 2", startTime = "14:00", endTime = "15:00", selectedWeekdays = "Tue", isEnabled = true)).getOrThrow()

        val deleteResult = repository.deleteSchedule(id1)
        assertTrue(deleteResult.isSuccess)

        val remaining = repository.getAllSchedules().first()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining[0].id)
    }

    @Test
    fun clearAllSchedules_removesAllEntries() = runBlocking {
        repository.saveSchedule(FocusScheduleEntity(name = "Block 1", startTime = "10:00", endTime = "11:00", selectedWeekdays = "Mon", isEnabled = true))
        repository.saveSchedule(FocusScheduleEntity(name = "Block 2", startTime = "14:00", endTime = "15:00", selectedWeekdays = "Tue", isEnabled = true))

        val clearResult = repository.clearAllSchedules()
        assertTrue(clearResult.isSuccess)

        val remaining = repository.getAllSchedules().first()
        assertTrue(remaining.isEmpty())
    }
}

