package com.dk.focusguard

import android.app.Application
import com.dk.focusguard.data.local.AppDatabase
import com.dk.focusguard.data.preferences.AppPreferencesManager
import com.dk.focusguard.data.repository.BlockEventRepositoryImpl
import com.dk.focusguard.data.repository.FocusRepositoryImpl
import com.dk.focusguard.data.repository.FocusScheduleRepositoryImpl
import com.dk.focusguard.data.repository.FocusSessionRepositoryImpl
import com.dk.focusguard.data.repository.MonitoredAppsRepositoryImpl
import com.dk.focusguard.data.repository.ProtectionRepositoryImpl
import com.dk.focusguard.data.repository.ProtectionSettingsRepositoryImpl
import com.dk.focusguard.data.repository.UsageStatsRepositoryImpl
import com.dk.focusguard.data.repository.UserSettingsRepositoryImpl
import com.dk.focusguard.domain.repository.BlockEventRepository
import com.dk.focusguard.domain.repository.FocusRepository
import com.dk.focusguard.domain.repository.FocusScheduleRepository
import com.dk.focusguard.domain.repository.FocusSessionRepository
import com.dk.focusguard.domain.repository.MonitoredAppsRepository
import com.dk.focusguard.domain.repository.ProtectionRepository
import com.dk.focusguard.domain.repository.ProtectionSettingsRepository
import com.dk.focusguard.domain.repository.UsageStatsRepository
import com.dk.focusguard.domain.repository.UserSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class FocusGuardApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this, applicationScope)
    }

    val preferencesManager: AppPreferencesManager by lazy {
        AppPreferencesManager(this)
    }

    // Phase 3 Specialized Repositories
    val protectionSettingsRepository: ProtectionSettingsRepository by lazy {
        ProtectionSettingsRepositoryImpl(
            dao = database.protectionSettingsDao(),
            preferencesManager = preferencesManager
        )
    }

    val monitoredAppsRepository: MonitoredAppsRepository by lazy {
        MonitoredAppsRepositoryImpl(
            monitoredAppDao = database.monitoredAppDao(),
            legacyBlockedAppDao = database.blockedAppDao()
        )
    }

    val focusScheduleRepository: FocusScheduleRepository by lazy {
        FocusScheduleRepositoryImpl(
            dao = database.focusScheduleDao()
        )
    }

    val focusSessionRepository: FocusSessionRepository by lazy {
        FocusSessionRepositoryImpl(
            dao = database.focusSessionDao()
        )
    }

    val blockEventRepository: BlockEventRepository by lazy {
        BlockEventRepositoryImpl(
            dao = database.blockEventDao()
        )
    }

    // Phase 5 Accessibility Detection Engine
    val accessibilityStatusRepository: com.dk.focusguard.domain.repository.AccessibilityStatusRepository by lazy {
        com.dk.focusguard.data.repository.AccessibilityStatusRepositoryImpl()
    }

    // Phase 8 Adult Website Filter & Safe Browsing Engine
    val vpnStatusRepository: com.dk.focusguard.domain.repository.VpnStatusRepository by lazy {
        com.dk.focusguard.data.repository.VpnStatusRepositoryImpl()
    }

    val domainBlocklistRepository: com.dk.focusguard.domain.repository.DomainBlocklistRepository by lazy {
        com.dk.focusguard.data.repository.DomainBlocklistRepositoryImpl(
            preferencesManager = preferencesManager
        )
    }

    val vpnConnectionManager: com.dk.focusguard.services.vpn.VpnConnectionManager by lazy {
        com.dk.focusguard.services.vpn.VpnConnectionManager(
            vpnStatusRepository = vpnStatusRepository
        )
    }

    // Core Repositories
    val protectionRepository: ProtectionRepository by lazy {
        ProtectionRepositoryImpl(
            blockedAppDao = database.blockedAppDao(),
            protectionRuleDao = database.protectionRuleDao()
        )
    }

    val focusRepository: FocusRepository by lazy {
        FocusRepositoryImpl(
            focusSessionDao = database.focusSessionDao()
        )
    }

    val usageStatsRepository: UsageStatsRepository by lazy {
        UsageStatsRepositoryImpl(
            dailyUsageStatsDao = database.dailyUsageStatsDao()
        )
    }

    val userSettingsRepository: UserSettingsRepository by lazy {
        UserSettingsRepositoryImpl(
            preferencesManager = preferencesManager
        )
    }

    // Phase 4 Security Architecture
    val securityRepository: com.dk.focusguard.domain.repository.SecurityRepository by lazy {
        com.dk.focusguard.data.repository.SecurityRepositoryImpl(this)
    }

    val securityLockManager: com.dk.focusguard.security.SecurityLockManager by lazy {
        com.dk.focusguard.security.SecurityLockManager()
    }

    val biometricAuthManager: com.dk.focusguard.security.BiometricAuthManager by lazy {
        com.dk.focusguard.security.BiometricAuthManager(this)
    }

    val emergencyOverrideManager: com.dk.focusguard.security.EmergencyOverrideManager by lazy {
        com.dk.focusguard.security.EmergencyOverrideManager(
            securityRepository = securityRepository,
            blockEventRepository = blockEventRepository,
            isServiceCurrentlyRunningProvider = {
                accessibilityStatusRepository.isCurrentlyConnected || vpnStatusRepository.isCurrentlyConnected
            }
        )
    }

    // Phase 9 Unified Protection Engine
    val protectionStateRepository: com.dk.focusguard.domain.repository.ProtectionStateRepository by lazy {
        com.dk.focusguard.data.repository.ProtectionStateRepositoryImpl(
            protectionSettingsRepository = protectionSettingsRepository,
            focusScheduleRepository = focusScheduleRepository,
            securityRepository = securityRepository,
            accessibilityStatusRepository = accessibilityStatusRepository,
            vpnStatusRepository = vpnStatusRepository,
            hasAccessibilityPermission = { com.dk.focusguard.utils.PermissionUtils.isAccessibilityServiceEnabled(this) },
            hasVpnPermission = { com.dk.focusguard.utils.PermissionUtils.isVpnPermissionGranted(this) },
            coroutineScope = applicationScope
        )
    }

    val protectionNotificationManager: com.dk.focusguard.services.notification.ProtectionNotificationManager by lazy {
        com.dk.focusguard.services.notification.ProtectionNotificationManager(this)
    }

    val unifiedProtectionManager: com.dk.focusguard.domain.manager.UnifiedProtectionManager by lazy {
        com.dk.focusguard.domain.manager.UnifiedProtectionManager(
            context = this,
            protectionStateRepository = protectionStateRepository,
            protectionSettingsRepository = protectionSettingsRepository,
            focusScheduleRepository = focusScheduleRepository,
            securityRepository = securityRepository,
            accessibilityStatusRepository = accessibilityStatusRepository,
            vpnStatusRepository = vpnStatusRepository,
            emergencyOverrideManager = emergencyOverrideManager,
            vpnConnectionManager = vpnConnectionManager,
            blockEventRepository = blockEventRepository,
            notificationManager = protectionNotificationManager,
            coroutineScope = applicationScope
        )
    }

    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()

        // Eagerly initialize Unified Protection Manager
        unifiedProtectionManager.let { /* trigger lazy init */ }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: android.app.Activity) {
                startedActivityCount++
            }

            override fun onActivityStopped(activity: android.app.Activity) {
                startedActivityCount--
                if (startedActivityCount <= 0 && !activity.isChangingConfigurations) {
                    securityLockManager.onAppBackgrounded()
                }
            }

            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }
}
