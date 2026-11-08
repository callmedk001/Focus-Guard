package com.dk.focusguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dk.focusguard.FocusGuardApplication
import com.dk.focusguard.ui.dashboard.DashboardScreen
import com.dk.focusguard.ui.dashboard.DashboardViewModel
import com.dk.focusguard.ui.focus.FocusScreen
import com.dk.focusguard.ui.focus.FocusViewModel
import com.dk.focusguard.ui.protection.ProtectionScreen
import com.dk.focusguard.ui.protection.ProtectionViewModel
import com.dk.focusguard.ui.security.AuthMode
import com.dk.focusguard.ui.security.AuthenticationScreen
import com.dk.focusguard.ui.security.AuthenticationViewModel
import com.dk.focusguard.ui.settings.SettingsScreen
import com.dk.focusguard.ui.settings.SettingsViewModel
import com.dk.focusguard.ui.statistics.StatisticsScreen
import com.dk.focusguard.ui.statistics.StatisticsViewModel

@Composable
fun DkNavGraph(
    navController: NavHostController,
    app: FocusGuardApplication,
    modifier: Modifier = Modifier
) {
    // Stores callback for actions that required unlock authentication before executing
    var pendingAuthorizedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.provideFactory(
                    protectionSettingsRepository = app.protectionSettingsRepository,
                    monitoredAppsRepository = app.monitoredAppsRepository,
                    blockEventRepository = app.blockEventRepository,
                    focusSessionRepository = app.focusSessionRepository,
                    userSettingsRepository = app.userSettingsRepository,
                    protectionStateRepository = app.protectionStateRepository,
                    unifiedProtectionManager = app.unifiedProtectionManager
                )
            )
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToFocus = {
                    navController.navigate(Screen.Focus.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToProtection = {
                    navController.navigate(Screen.Protection.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.Protection.route) {
            val protectionViewModel: ProtectionViewModel = viewModel(
                factory = ProtectionViewModel.provideFactory(
                    protectionSettingsRepository = app.protectionSettingsRepository,
                    monitoredAppsRepository = app.monitoredAppsRepository,
                    userSettingsRepository = app.userSettingsRepository,
                    legacyProtectionRepository = app.protectionRepository,
                    blockEventRepository = app.blockEventRepository,
                    securityRepository = app.securityRepository,
                    accessibilityStatusRepository = app.accessibilityStatusRepository,
                    vpnStatusRepository = app.vpnStatusRepository,
                    domainBlocklistRepository = app.domainBlocklistRepository,
                    preferencesManager = app.preferencesManager
                )
            )
            val accessibilityViewModel: com.dk.focusguard.ui.accessibility.AccessibilityViewModel = viewModel(
                factory = com.dk.focusguard.ui.accessibility.AccessibilityViewModel.provideFactory(
                    accessibilityStatusRepository = app.accessibilityStatusRepository
                )
            )
            ProtectionScreen(
                viewModel = protectionViewModel,
                accessibilityViewModel = accessibilityViewModel,
                vpnConnectionManager = app.vpnConnectionManager,
                onNavigateBack = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
                onNavigateToAccessibilitySetup = {
                    navController.navigate(Screen.AccessibilitySetup.route)
                }
            )
        }

        composable(Screen.Focus.route) {
            val focusViewModel: FocusViewModel = viewModel(
                factory = FocusViewModel.provideFactory(
                    focusSessionRepository = app.focusSessionRepository,
                    focusScheduleRepository = app.focusScheduleRepository,
                    legacyFocusRepository = app.focusRepository,
                    unifiedProtectionManager = app.unifiedProtectionManager
                )
            )
            FocusScreen(viewModel = focusViewModel)
        }

        composable(Screen.Statistics.route) {
            val statisticsViewModel: StatisticsViewModel = viewModel(
                factory = StatisticsViewModel.provideFactory(
                    blockEventRepository = app.blockEventRepository,
                    focusSessionRepository = app.focusSessionRepository
                )
            )
            StatisticsScreen(viewModel = statisticsViewModel)
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(
                    userSettingsRepository = app.userSettingsRepository,
                    focusSessionRepository = app.focusSessionRepository,
                    blockEventRepository = app.blockEventRepository,
                    monitoredAppsRepository = app.monitoredAppsRepository,
                    protectionSettingsRepository = app.protectionSettingsRepository,
                    preferencesManager = app.preferencesManager,
                    securityRepository = app.securityRepository,
                    securityLockManager = app.securityLockManager,
                    biometricAuthManager = app.biometricAuthManager,
                    emergencyOverrideManager = app.emergencyOverrideManager
                )
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToPinSetup = {
                    navController.navigate(Screen.PinSetup.route)
                },
                onNavigateToChangePin = {
                    navController.navigate(Screen.ChangePin.route)
                },
                onNavigateToPinUnlock = { actionDescription, onAuthorized ->
                    pendingAuthorizedAction = onAuthorized
                    navController.navigate(Screen.PinUnlock.createRoute(actionDescription))
                }
            )
        }

        // Phase 4 Authentication Destinations
        composable(Screen.PinSetup.route) {
            val authViewModel: AuthenticationViewModel = viewModel(
                factory = AuthenticationViewModel.provideFactory(
                    securityRepository = app.securityRepository,
                    securityLockManager = app.securityLockManager,
                    biometricAuthManager = app.biometricAuthManager,
                    initialMode = AuthMode.SETUP,
                    actionDescription = "Configure Master PIN"
                )
            )
            AuthenticationScreen(
                viewModel = authViewModel,
                onAuthenticationSuccess = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.PinUnlock.route,
            arguments = listOf(
                navArgument("action") {
                    type = NavType.StringType
                    defaultValue = "Authorize Action"
                }
            )
        ) { backStackEntry ->
            val action = backStackEntry.arguments?.getString("action") ?: "Authorize Action"
            val authViewModel: AuthenticationViewModel = viewModel(
                factory = AuthenticationViewModel.provideFactory(
                    securityRepository = app.securityRepository,
                    securityLockManager = app.securityLockManager,
                    biometricAuthManager = app.biometricAuthManager,
                    initialMode = AuthMode.UNLOCK,
                    actionDescription = action
                )
            )
            AuthenticationScreen(
                viewModel = authViewModel,
                onAuthenticationSuccess = {
                    val actionToRun = pendingAuthorizedAction
                    pendingAuthorizedAction = null
                    navController.popBackStack()
                    actionToRun?.invoke()
                },
                onNavigateBack = {
                    pendingAuthorizedAction = null
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ChangePin.route) {
            val authViewModel: AuthenticationViewModel = viewModel(
                factory = AuthenticationViewModel.provideFactory(
                    securityRepository = app.securityRepository,
                    securityLockManager = app.securityLockManager,
                    biometricAuthManager = app.biometricAuthManager,
                    initialMode = AuthMode.CHANGE_CURRENT,
                    actionDescription = "Change Master PIN"
                )
            )
            AuthenticationScreen(
                viewModel = authViewModel,
                onAuthenticationSuccess = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Phase 5 Accessibility Setup Route
        composable(Screen.AccessibilitySetup.route) {
            val accessibilityViewModel: com.dk.focusguard.ui.accessibility.AccessibilityViewModel = viewModel(
                factory = com.dk.focusguard.ui.accessibility.AccessibilityViewModel.provideFactory(
                    accessibilityStatusRepository = app.accessibilityStatusRepository
                )
            )
            com.dk.focusguard.ui.accessibility.AccessibilitySetupScreen(
                viewModel = accessibilityViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
