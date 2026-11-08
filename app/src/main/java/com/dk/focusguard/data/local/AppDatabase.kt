package com.dk.focusguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dk.focusguard.data.local.dao.BlockEventDao
import com.dk.focusguard.data.local.dao.BlockedAppDao
import com.dk.focusguard.data.local.dao.DailyUsageStatsDao
import com.dk.focusguard.data.local.dao.FocusScheduleDao
import com.dk.focusguard.data.local.dao.FocusSessionDao
import com.dk.focusguard.data.local.dao.MonitoredAppDao
import com.dk.focusguard.data.local.dao.ProtectionRuleDao
import com.dk.focusguard.data.local.dao.ProtectionSettingsDao
import com.dk.focusguard.data.local.entity.BlockEventEntity
import com.dk.focusguard.data.local.entity.BlockedAppEntity
import com.dk.focusguard.data.local.entity.DailyUsageStatsEntity
import com.dk.focusguard.data.local.entity.FocusScheduleEntity
import com.dk.focusguard.data.local.entity.FocusSessionEntity
import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import com.dk.focusguard.data.local.entity.ProtectionRuleEntity
import com.dk.focusguard.data.local.entity.ProtectionSettingsEntity
import com.dk.focusguard.domain.model.AppCategory
import com.dk.focusguard.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProtectionSettingsEntity::class,
        MonitoredAppEntity::class,
        FocusScheduleEntity::class,
        FocusSessionEntity::class,
        BlockEventEntity::class,
        BlockedAppEntity::class,
        DailyUsageStatsEntity::class,
        ProtectionRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun protectionSettingsDao(): ProtectionSettingsDao
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun focusScheduleDao(): FocusScheduleDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun blockEventDao(): BlockEventDao

    // Legacy DAOs maintained for backwards compatibility
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun dailyUsageStatsDao(): DailyUsageStatsDao
    abstract fun protectionRuleDao(): ProtectionRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create protection_settings table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `protection_settings` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `blockYouTubeShorts` INTEGER NOT NULL,
                        `blockInstagramReels` INTEGER NOT NULL,
                        `blockAdultWebsites` INTEGER NOT NULL,
                        `isStrictModeEnabled` INTEGER NOT NULL,
                        `isMasterProtectionEnabled` INTEGER NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 2. Create monitored_apps table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monitored_apps` (
                        `packageName` TEXT NOT NULL PRIMARY KEY,
                        `appName` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `dateAdded` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `blockShortsOrReelsOnly` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_monitored_apps_dateAdded` ON `monitored_apps` (`dateAdded`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_monitored_apps_isEnabled` ON `monitored_apps` (`isEnabled`)")

                // 3. Create focus_schedules table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_schedules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `selectedWeekdays` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `selectedAppPackages` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // 4. Create block_events table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `block_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `detectionSource` TEXT NOT NULL,
                        `eventStatus` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_block_events_timestamp` ON `block_events` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_block_events_packageName` ON `block_events` (`packageName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_block_events_category` ON `block_events` (`category`)")

                // 5. Update focus_sessions table with status and indices
                try {
                    db.execSQL("ALTER TABLE `focus_sessions` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'COMPLETED'")
                } catch (_: Exception) {
                    // Column already exists in fresh creates
                }
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_startTimeMillis` ON `focus_sessions` (`startTimeMillis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_isCompleted` ON `focus_sessions` (`isCompleted`)")
            }
        }

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        ensureDefaultSettings(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                // Populate default protection settings
                database.protectionSettingsDao().insertOrUpdate(
                    ProtectionSettingsEntity(
                        id = 1,
                        blockYouTubeShorts = true,
                        blockInstagramReels = true,
                        blockAdultWebsites = true,
                        isStrictModeEnabled = false,
                        isMasterProtectionEnabled = true,
                        lastUpdated = System.currentTimeMillis()
                    )
                )

                // Populate default monitored applications
                val defaultApps = listOf(
                    MonitoredAppEntity(
                        packageName = Constants.PACKAGE_YOUTUBE,
                        appName = "YouTube",
                        isEnabled = true,
                        dateAdded = System.currentTimeMillis(),
                        category = AppCategory.ENTERTAINMENT.name,
                        blockShortsOrReelsOnly = true
                    ),
                    MonitoredAppEntity(
                        packageName = Constants.PACKAGE_INSTAGRAM,
                        appName = "Instagram",
                        isEnabled = true,
                        dateAdded = System.currentTimeMillis(),
                        category = AppCategory.SOCIAL.name,
                        blockShortsOrReelsOnly = true
                    )
                )
                database.monitoredAppDao().insertDefaultApps(defaultApps)

                // Populate default focus schedule
                database.focusScheduleDao().insert(
                    FocusScheduleEntity(
                        name = "Daily Work Focus",
                        startTime = "09:00",
                        endTime = "17:00",
                        selectedWeekdays = "Mon,Tue,Wed,Thu,Fri",
                        isEnabled = false,
                        selectedAppPackages = "${Constants.PACKAGE_YOUTUBE},${Constants.PACKAGE_INSTAGRAM}"
                    )
                )
            }

            private suspend fun ensureDefaultSettings(database: AppDatabase) {
                val existing = database.protectionSettingsDao().getSettingsDirect()
                if (existing == null) {
                    database.protectionSettingsDao().insertOrUpdate(
                        ProtectionSettingsEntity(
                            id = 1,
                            blockYouTubeShorts = true,
                            blockInstagramReels = true,
                            blockAdultWebsites = true,
                            isStrictModeEnabled = false,
                            isMasterProtectionEnabled = true,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }

                if (database.monitoredAppDao().getCountDirect() == 0) {
                    val defaultApps = listOf(
                        MonitoredAppEntity(
                            packageName = Constants.PACKAGE_YOUTUBE,
                            appName = "YouTube",
                            isEnabled = true,
                            dateAdded = System.currentTimeMillis(),
                            category = AppCategory.ENTERTAINMENT.name,
                            blockShortsOrReelsOnly = true
                        ),
                        MonitoredAppEntity(
                            packageName = Constants.PACKAGE_INSTAGRAM,
                            appName = "Instagram",
                            isEnabled = true,
                            dateAdded = System.currentTimeMillis(),
                            category = AppCategory.SOCIAL.name,
                            blockShortsOrReelsOnly = true
                        )
                    )
                    database.monitoredAppDao().insertDefaultApps(defaultApps)
                }
            }
        }
    }
}
