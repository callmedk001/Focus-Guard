package com.dk.focusguard.ui.protection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dk.focusguard.data.local.entity.MonitoredAppEntity
import com.dk.focusguard.data.preferences.AppPreferencesManager
import com.dk.focusguard.domain.model.AppCategory
import com.dk.focusguard.domain.model.BlockedApp
import com.dk.focusguard.domain.model.ProtectionRule
import com.dk.focusguard.domain.repository.AccessibilityStatusRepository
import com.dk.focusguard.domain.repository.BlockEventRepository
import com.dk.focusguard.domain.repository.DomainBlocklistRepository
import com.dk.focusguard.domain.repository.MonitoredAppsRepository
import com.dk.focusguard.domain.repository.ProtectionRepository
import com.dk.focusguard.domain.repository.ProtectionSettingsRepository
import com.dk.focusguard.domain.repository.SecurityRepository
import com.dk.focusguard.domain.repository.UserSettingsRepository
import com.dk.focusguard.domain.repository.VpnStatusRepository
import com.dk.focusguard.services.accessibility.TargetAppRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProtectionViewModel(
    private val protectionSettingsRepository: ProtectionSettingsRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val legacyProtectionRepository: ProtectionRepository? = null,
    private val blockEventRepository: BlockEventRepository? = null,
    private val securityRepository: SecurityRepository? = null,
    private val accessibilityStatusRepository: AccessibilityStatusRepository? = null,
    private val vpnStatusRepository: VpnStatusRepository? = null,
    private val domainBlocklistRepository: DomainBlocklistRepository? = null,
    private val preferencesManager: AppPreferencesManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProtectionUiState(isLoading = true))
    val uiState: StateFlow<ProtectionUiState> = _uiState.asStateFlow()

    init {
        loadProtectionData()
        observeTelemetry()
    }

    private fun loadProtectionData() {
        viewModelScope.launch {
            combine(
                protectionSettingsRepository.getProtectionSettings(),
                monitoredAppsRepository.getAllMonitoredApps(),
                userSettingsRepository.getUserSettings()
            ) { settings, monitoredApps, userSettings ->
                val rulesList = listOf(
                    ProtectionRule(
                        id = "rule_yt_shorts",
                        title = "YouTube Shorts Shield",
                        description = "Intercepts the addictive vertical infinite feed inside YouTube.",
                        isEnabled = settings.blockYouTubeShorts,
                        category = AppCategory.ENTERTAINMENT
                    ),
                    ProtectionRule(
                        id = "rule_ig_reels",
                        title = "Instagram Reels Guard",
                        description = "Suppresses the algorithmic Reels consumption tab while preserving messages.",
                        isEnabled = settings.blockInstagramReels,
                        category = AppCategory.SOCIAL
                    ),
                    ProtectionRule(
                        id = "rule_adult_filter",
                        title = "Adult (18+) Web Filter",
                        description = "Enforces safe browsing boundaries across web browsers.",
                        isEnabled = settings.blockAdultWebsites,
                        category = AppCategory.ADULT_CONTENT
                    )
                )

                _uiState.update { current ->
                    current.copy(
                        rules = rulesList,
                        blockedApps = monitoredApps.map { it.toBlockedApp() },
                        isMasterProtectionEnabled = settings.isMasterProtectionEnabled,
                        isStrictModeEnabled = settings.isStrictModeEnabled,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    private fun observeTelemetry() {
        // Observe Real YouTube & Instagram Block Events
        blockEventRepository?.let { repo ->
            viewModelScope.launch {
                repo.getAllRealEvents().collect { events ->
                    val youtubeEvents = events.filter { it.packageName == TargetAppRegistry.PACKAGE_YOUTUBE }
                    val interceptedCount = youtubeEvents.count { it.eventStatus == "INTERCEPTED" }
                    val lastTimestamp = youtubeEvents.firstOrNull()?.timestamp ?: 0L

                    val instagramEvents = events.filter { it.packageName == TargetAppRegistry.PACKAGE_INSTAGRAM }
                    val instagramInterceptedCount = instagramEvents.count { it.eventStatus == "INTERCEPTED" }
                    val lastInstagramTimestamp = instagramEvents.firstOrNull()?.timestamp ?: 0L

                    _uiState.update { current ->
                        current.copy(
                            youtubeInterruptionCount = interceptedCount,
                            lastYoutubeDetectionTimestamp = lastTimestamp,
                            instagramInterruptionCount = instagramInterceptedCount,
                            lastInstagramDetectionTimestamp = lastInstagramTimestamp
                        )
                    }
                }
            }
        }

        // Observe Security Emergency Override
        securityRepository?.let { repo ->
            viewModelScope.launch {
                repo.isEmergencyOverrideActive.collect { overrideActive ->
                    _uiState.update { current ->
                        current.copy(isEmergencyOverrideActive = overrideActive)
                    }
                }
            }
        }

        // Observe Current Foreground Application
        accessibilityStatusRepository?.let { repo ->
            viewModelScope.launch {
                repo.currentForegroundPackage.collect { foregroundPkg ->
                    _uiState.update { current ->
                        current.copy(
                            isYouTubeForegroundActive = foregroundPkg == TargetAppRegistry.PACKAGE_YOUTUBE,
                            isInstagramForegroundActive = foregroundPkg == TargetAppRegistry.PACKAGE_INSTAGRAM
                        )
                    }
                }
            }
        }

        // Observe Phase 8 VPN Health & Telemetry
        vpnStatusRepository?.let { repo ->
            viewModelScope.launch {
                repo.connectionState.collect { state ->
                    _uiState.update { it.copy(vpnConnectionState = state) }
                }
            }
            viewModelScope.launch {
                repo.blockedDnsCount.collect { count ->
                    _uiState.update { it.copy(adultBlockedDnsCount = count) }
                }
            }
            viewModelScope.launch {
                repo.lastBlockedTimestamp.collect { ts ->
                    _uiState.update { it.copy(lastAdultBlockedTimestamp = ts) }
                }
            }
        }

        // Observe Domain Blocklist
        domainBlocklistRepository?.let { repo ->
            viewModelScope.launch {
                repo.allBlockedDomains.collect { allDomains ->
                    _uiState.update { it.copy(adultBlocklistCount = allDomains.size) }
                }
            }
            viewModelScope.launch {
                repo.customBlockedDomains.collect { custom ->
                    _uiState.update { it.copy(customBlockedDomains = custom) }
                }
            }
        }

        // Observe Safe Search Preference
        preferencesManager?.let { pm ->
            viewModelScope.launch {
                pm.isSafeSearchEnabled.collect { enabled ->
                    _uiState.update { it.copy(isSafeSearchEnabled = enabled) }
                }
            }
        }
    }

    fun toggleRule(ruleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            when (ruleId) {
                "rule_yt_shorts" -> protectionSettingsRepository.setYouTubeShortsProtection(isEnabled)
                "rule_ig_reels" -> protectionSettingsRepository.setInstagramReelsProtection(isEnabled)
                "rule_adult_filter" -> protectionSettingsRepository.setAdultWebsiteProtection(isEnabled)
            }
            legacyProtectionRepository?.updateProtectionRule(ruleId, isEnabled)
        }
    }

    fun toggleSafeSearch(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager?.setSafeSearchEnabled(enabled)
            _uiState.update { it.copy(isSafeSearchEnabled = enabled) }
        }
    }

    fun addCustomDomain(domain: String) {
        viewModelScope.launch {
            domainBlocklistRepository?.addCustomBlockedDomain(domain)
        }
    }

    fun removeCustomDomain(domain: String) {
        viewModelScope.launch {
            domainBlocklistRepository?.removeCustomBlockedDomain(domain)
        }
    }

    fun importDomains(domains: List<String>) {
        viewModelScope.launch {
            domainBlocklistRepository?.importDomains(domains)
        }
    }

    fun toggleAppBlock(packageName: String, isBlocked: Boolean) {
        viewModelScope.launch {
            monitoredAppsRepository.updateAppEnabled(packageName, isBlocked)
            legacyProtectionRepository?.toggleAppBlock(packageName, isBlocked)
        }
    }

    fun addCustomBlockedApp(packageName: String, appName: String, category: AppCategory) {
        viewModelScope.launch {
            val entity = MonitoredAppEntity(
                packageName = packageName.trim(),
                appName = appName.trim(),
                isEnabled = true,
                dateAdded = System.currentTimeMillis(),
                category = category.name,
                blockShortsOrReelsOnly = false
            )
            monitoredAppsRepository.addMonitoredApp(entity)
            legacyProtectionRepository?.setAppBlocked(
                BlockedApp(
                    packageName = packageName.trim(),
                    appName = appName.trim(),
                    isBlocked = true,
                    category = category
                )
            )
        }
    }

    fun removeBlockedApp(packageName: String) {
        viewModelScope.launch {
            monitoredAppsRepository.removeMonitoredApp(packageName)
            legacyProtectionRepository?.removeBlockedApp(packageName)
        }
    }

    companion object {
        fun provideFactory(
            protectionSettingsRepository: ProtectionSettingsRepository,
            monitoredAppsRepository: MonitoredAppsRepository,
            userSettingsRepository: UserSettingsRepository,
            legacyProtectionRepository: ProtectionRepository? = null,
            blockEventRepository: BlockEventRepository? = null,
            securityRepository: SecurityRepository? = null,
            accessibilityStatusRepository: AccessibilityStatusRepository? = null,
            vpnStatusRepository: VpnStatusRepository? = null,
            domainBlocklistRepository: DomainBlocklistRepository? = null,
            preferencesManager: AppPreferencesManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProtectionViewModel(
                    protectionSettingsRepository = protectionSettingsRepository,
                    monitoredAppsRepository = monitoredAppsRepository,
                    userSettingsRepository = userSettingsRepository,
                    legacyProtectionRepository = legacyProtectionRepository,
                    blockEventRepository = blockEventRepository,
                    securityRepository = securityRepository,
                    accessibilityStatusRepository = accessibilityStatusRepository,
                    vpnStatusRepository = vpnStatusRepository,
                    domainBlocklistRepository = domainBlocklistRepository,
                    preferencesManager = preferencesManager
                ) as T
            }
        }
    }
}
