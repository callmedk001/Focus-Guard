package com.dk.focusguard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dk.focusguard.FocusGuardApplication
import com.dk.focusguard.ui.navigation.DkBottomNavBar
import com.dk.focusguard.ui.navigation.DkNavGraph
import com.dk.focusguard.ui.navigation.Screen

@Composable
fun FocusGuardApp(app: FocusGuardApplication) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigateToRoute: (String) -> Unit = { targetRoute ->
        if (targetRoute == Screen.Dashboard.route) {
            // Popping back to Home/Dashboard cleanly without phantom saved states
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        } else {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = com.dk.focusguard.ui.theme.CyberBackground,
        bottomBar = {
            DkBottomNavBar(
                currentRoute = currentRoute,
                onNavigateToRoute = navigateToRoute
            )
        }
    ) { innerPadding ->
        DkNavGraph(
            navController = navController,
            app = app,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
