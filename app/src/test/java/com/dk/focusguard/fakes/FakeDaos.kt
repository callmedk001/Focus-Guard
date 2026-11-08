package com.dk.focusguard.fakes

import com.dk.focusguard.data.local.dao.BlockEventDao
import com.dk.focusguard.data.local.dao.FocusScheduleDao
import com.dk.focusguard.data.local.dao.FocusSessionDao
import com.dk.focusguard.data.local.dao.MonitoredAppDao
import com.dk.focusguard.data.local.dao.ProtectionSettingsDao
import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.data.local.entity.FocusSessionEntity
import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import com.dk.focusguard.data.local.entity.ProtectionSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Thread-safe In-Memory Fake DAOs implementing the exact Room DAO interfaces
 * for comprehensive JVM unit testing without requiring an Android device or emulator.
 */

class FakeProtectionSettingsDao(
    initialSettings: ProtectionSettingsEntity? = ProtectionSettingsEntity()
) : ProtectionSettingsDao {
    private val _settingsFlow = MutableStateFlow(initialSettings)
    private var currentSettings: ProtectionSettingsEntity? = initialSettings
    var shouldThrow = false

    override fun getSettings(): Flow<ProtectionSettingsEntity?> {
        return _settingsFlow.map {
            if (shouldThrow) throw RuntimeException("ProtectionSettingsDao read failure")
            it
        }
    }

    override suspend fun getSettingsDirect(): ProtectionSettingsEntity? {
        if (shouldThrow) throw RuntimeException("ProtectionSettingsDao read failure")
        return currentSettings
    }

    override suspend fun insertOrUpdate(settings: ProtectionSettingsEntity) {
        if (shouldThrow) throw RuntimeException("ProtectionSettingsDao insert failure")
        currentSettings = settings
        _settingsFlow.value = settings
    }

    override suspend fun update(settings: ProtectionSettingsEntity) {
        insertOrUpdate(settings)
    }

    override suspend fun updateShortsProtection(enabled: Boolean, timestamp: Long) {
        if (shouldThrow) throw RuntimeException("ProtectionSettingsDao update failure")
        val updated = (currentSettings ?: ProtectionSettingsEntity()).copy(
            blockYouTubeShorts = enabled,
            lastUpdated = timestamp
        )
        currentSettings = updated
        _settingsFlow.value = updated
    }

    override suspend fun updateReelsProtection(enabled: Boolean, timestamp: Long) {
        if (shouldThrow) throw RuntimeException("ProtectionSettingsDao update failure")
        val updated = (currentSettings ?: ProtectionSettingsEntity()).copy(
            blockInstagramReels = enabled,
            lastUpdated = timestamp
        )
        currentSettings = updated
        _settingsFlow.value = updated
    }

    override suspend fun updateAdultProtection(enabled: Boolean, timestamp: Long) {
        if (shouldThrow) throw RuntimeException("ProtectionSettingsDao update failure")
        val updated = (currentSettings ?: ProtectionSettingsEntity()).copy(
            blockAdultWebsites = enabled,
            lastUpdated = timestamp
        )
        currentSettings = updated
        _settingsFlow.value = updated
    }

    override suspend fun updateStrictMode(enabled: Boolean, timestamp: Long) {
        if (shouldThrow) throw RuntimeException("ProtectionSettingsDao update failure")
        val updated = (currentSettings ?: ProtectionSettingsEntity()).copy(
            isStrictModeEnabled = enabled,
            lastUpdated = timestamp
        )
        currentSettings = updated
        _settingsFlow.value = updated
    }

    override suspend fun updateMasterProtection(enabled: Boolean, timestamp: Long) {
        if (shouldThrow) throw RuntimeException("ProtectionSettingsDao update failure")
        val updated = (currentSettings ?: ProtectionSettingsEntity()).copy(
            isMasterProtectionEnabled = enabled,
            lastUpdated = timestamp
        )
        currentSettings = updated
        _settingsFlow.value = updated
    }
}

class FakeMonitoredAppDao : MonitoredAppDao {
    private val appMap = mutableMapOf<String, MonitoredAppEntity>()
    private val _flow = MutableStateFlow<List<MonitoredAppEntity>>(emptyList())
    var shouldThrow = false

    private fun emitState() {
        _flow.value = appMap.values.sortedBy { it.appName }
    }

    override fun getAll(): Flow<List<MonitoredAppEntity>> {
        return _flow.map {
            if (shouldThrow) throw RuntimeException("MonitoredAppDao query failure")
            it
        }
    }

    override suspend fun getAllDirect(): List<MonitoredAppEntity> {
        if (shouldThrow) throw RuntimeException("MonitoredAppDao query failure")
        return appMap.values.sortedBy { it.appName }
    }

    override fun getEnabledApps(): Flow<List<MonitoredAppEntity>> {
        return _flow.map { list ->
            if (shouldThrow) throw RuntimeException("MonitoredAppDao query failure")
            list.filter { it.isEnabled }
        }
    }

    override suspend fun getByPackageName(packageName: String): MonitoredAppEntity? {
        return appMap[packageName]
    }

    override fun getCount(): Flow<Int> {
        return _flow.map {
            if (shouldThrow) throw RuntimeException("MonitoredAppDao count failure")
            it.size
        }
    }

    override suspend fun getCountDirect(): Int {
        if (shouldThrow) throw RuntimeException("MonitoredAppDao count failure")
        return appMap.size
    }

    override suspend fun insert(app: MonitoredAppEntity) {
        if (shouldThrow) throw RuntimeException("MonitoredAppDao insert failure")
        appMap[app.packageName] = app
        emitState()
    }

    override suspend fun insertDefaultApps(apps: List<MonitoredAppEntity>) {
        apps.forEach { if (!appMap.containsKey(it.packageName)) appMap[it.packageName] = it }
        emitState()
    }

    override suspend fun update(app: MonitoredAppEntity) {
        appMap[app.packageName] = app
        emitState()
    }

    override suspend fun updateEnabledStatus(packageName: String, isEnabled: Boolean) {
        if (shouldThrow) throw RuntimeException("MonitoredAppDao update failure")
        appMap[packageName]?.let {
            appMap[packageName] = it.copy(isEnabled = isEnabled)
            emitState()
        }
    }

    override suspend fun delete(packageName: String) {
        if (shouldThrow) throw RuntimeException("MonitoredAppDao delete failure")
        appMap.remove(packageName)
        emitState()
    }

    override suspend fun deleteAll() {
        if (shouldThrow) throw RuntimeException("MonitoredAppDao deleteAll failure")
        appMap.clear()
        emitState()
    }
}

class FakeFocusScheduleDao : FocusScheduleDao {
    private val schedules = mutableMapOf<Long, FocusScheduleEntity>()
    private val _flow = MutableStateFlow<List<FocusScheduleEntity>>(emptyList())
    private var idCounter = 1L
    var shouldThrow = false

    private fun emitState() {
        _flow.value = schedules.values.sortedBy { it.id }
    }

    override fun getAll(): Flow<List<FocusScheduleEntity>> {
        return _flow.map {
            if (shouldThrow) throw RuntimeException("FocusScheduleDao query failure")
            it
        }
    }

    override fun getEnabledSchedules(): Flow<List<FocusScheduleEntity>> {
        return _flow.map { list ->
            if (shouldThrow) throw RuntimeException("FocusScheduleDao query failure")
            list.filter { it.isEnabled }
        }
    }

    override suspend fun getById(id: Long): FocusScheduleEntity? = schedules[id]

    override fun getCount(): Flow<Int> = _flow.map {
        if (shouldThrow) throw RuntimeException("FocusScheduleDao count failure")
        it.size
    }

    override suspend fun insert(schedule: FocusScheduleEntity): Long {
        if (shouldThrow) throw RuntimeException("FocusScheduleDao insert failure")
        val id = if (schedule.id == 0L) idCounter++ else schedule.id
        schedules[id] = schedule.copy(id = id)
        emitState()
        return id
    }

    override suspend fun update(schedule: FocusScheduleEntity) {
        schedules[schedule.id] = schedule
        emitState()
    }

    override suspend fun updateEnabledStatus(id: Long, isEnabled: Boolean) {
        schedules[id]?.let {
            schedules[id] = it.copy(isEnabled = isEnabled)
            emitState()
        }
    }

    override suspend fun delete(id: Long) {
        schedules.remove(id)
        emitState()
    }

    override suspend fun deleteAll() {
        schedules.clear()
        emitState()
    }
}

class FakeFocusSessionDao : FocusSessionDao {
    private val sessions = mutableMapOf<Long, FocusSessionEntity>()
    private val _flow = MutableStateFlow<List<FocusSessionEntity>>(emptyList())
    private var idCounter = 1L
    var shouldThrow = false

    private fun emitState() {
        _flow.value = sessions.values.sortedByDescending { it.startTimeMillis }
    }

    override fun getAll(): Flow<List<FocusSessionEntity>> {
        return _flow.map {
            if (shouldThrow) throw RuntimeException("FocusSessionDao query failure")
            it
        }
    }

    override suspend fun getAllDirect(): List<FocusSessionEntity> {
        if (shouldThrow) throw RuntimeException("FocusSessionDao direct query failure")
        return sessions.values.sortedByDescending { it.startTimeMillis }
    }

    override fun getRecent(limit: Int): Flow<List<FocusSessionEntity>> {
        return _flow.map {
            if (shouldThrow) throw RuntimeException("FocusSessionDao recent failure")
            it.take(limit)
        }
    }

    override suspend fun getById(sessionId: Long): FocusSessionEntity? = sessions[sessionId]

    override fun getSessionsBetween(startTime: Long, endTime: Long): Flow<List<FocusSessionEntity>> {
        return _flow.map { list ->
            if (shouldThrow) throw RuntimeException("FocusSessionDao between failure")
            list.filter { it.startTimeMillis in startTime..endTime }
        }
    }

    override fun getSessionsSince(sinceTimestamp: Long): Flow<List<FocusSessionEntity>> {
        return _flow.map { list ->
            if (shouldThrow) throw RuntimeException("FocusSessionDao since failure")
            list.filter { it.startTimeMillis >= sinceTimestamp }
        }
    }

    override fun getTotalFocusDurationSecondsSince(sinceTimestamp: Long): Flow<Long> {
        return _flow.map { list ->
            if (shouldThrow) throw RuntimeException("FocusSessionDao duration failure")
            list.filter { it.isCompleted && it.startTimeMillis >= sinceTimestamp }
                .sumOf { it.actualDurationSeconds }
        }
    }

    override fun getCompletedSessionCountSince(sinceTimestamp: Long): Flow<Int> {
        return _flow.map { list ->
            if (shouldThrow) throw RuntimeException("FocusSessionDao count failure")
            list.count { it.isCompleted && it.startTimeMillis >= sinceTimestamp }
        }
    }

    override fun getTotalCount(): Flow<Int> = _flow.map {
        if (shouldThrow) throw RuntimeException("FocusSessionDao count failure")
        it.size
    }

    override suspend fun getTotalCountDirect(): Int {
        if (shouldThrow) throw RuntimeException("FocusSessionDao direct count failure")
        return sessions.size
    }

    override suspend fun insert(session: FocusSessionEntity): Long {
        if (shouldThrow) throw RuntimeException("FocusSessionDao insert failure")
        val id = if (session.id == 0L) idCounter++ else session.id
        sessions[id] = session.copy(id = id)
        emitState()
        return id
    }

    override suspend fun update(session: FocusSessionEntity) {
        sessions[session.id] = session
        emitState()
    }

    override suspend fun delete(id: Long) {
        sessions.remove(id)
        emitState()
    }

    override suspend fun deleteAll() {
        sessions.clear()
        emitState()
    }
}

class FakeBlockEventDao : BlockEventDao {
    private val events = mutableMapOf<Long, BlockEventEntity>()
    private val _flow = MutableStateFlow<List<BlockEventEntity>>(emptyList())
    private var idCounter = 1L
    var shouldThrow = false

    private fun emitState() {
        _flow.value = events.values.sortedByDescending { it.timestamp }
    }

    override fun getAll(): Flow<List<BlockEventEntity>> {
        return _flow.map {
            if (shouldThrow) throw RuntimeException("BlockEventDao query failure")
            it
        }
    }

    override suspend fun getAllDirect(): List<BlockEventEntity> {
        if (shouldThrow) throw RuntimeException("BlockEventDao direct failure")
        return events.values.sortedByDescending { it.timestamp }
    }

    override fun getRecent(limit: Int): Flow<List<BlockEventEntity>> =
        _flow.map {
            if (shouldThrow) throw RuntimeException("BlockEventDao recent failure")
            it.take(limit)
        }

    override fun getEventsBetween(startTime: Long, endTime: Long): Flow<List<BlockEventEntity>> =
        _flow.map { list ->
            if (shouldThrow) throw RuntimeException("BlockEventDao between failure")
            list.filter { it.timestamp in startTime..endTime }
        }

    override fun getEventsSince(sinceTimestamp: Long): Flow<List<BlockEventEntity>> =
        _flow.map { list ->
            if (shouldThrow) throw RuntimeException("BlockEventDao since failure")
            list.filter { it.timestamp >= sinceTimestamp }
        }

    override fun getEventCountSince(sinceTimestamp: Long): Flow<Int> =
        _flow.map { list ->
            if (shouldThrow) throw RuntimeException("BlockEventDao count failure")
            list.count { it.timestamp >= sinceTimestamp }
        }

    override fun getTotalCount(): Flow<Int> = _flow.map {
        if (shouldThrow) throw RuntimeException("BlockEventDao count failure")
        it.size
    }

    override suspend fun getTotalCountDirect(): Int {
        if (shouldThrow) throw RuntimeException("BlockEventDao count failure")
        return events.size
    }

    override fun getEventsByPackage(packageName: String): Flow<List<BlockEventEntity>> =
        _flow.map { list ->
            if (shouldThrow) throw RuntimeException("BlockEventDao package failure")
            list.filter { it.packageName == packageName }
        }

    override fun getEventsByCategory(category: String): Flow<List<BlockEventEntity>> =
        _flow.map { list ->
            if (shouldThrow) throw RuntimeException("BlockEventDao category failure")
            list.filter { it.category == category }
        }

    override suspend fun insert(event: BlockEventEntity): Long {
        if (shouldThrow) throw RuntimeException("BlockEventDao insert failure")
        val id = if (event.id == 0L) idCounter++ else event.id
        events[id] = event.copy(id = id)
        emitState()
        return id
    }

    override suspend fun insertAll(eventsList: List<BlockEventEntity>) {
        eventsList.forEach { insert(it) }
    }

    override suspend fun deleteAll() {
        events.clear()
        emitState()
    }
}
