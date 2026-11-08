package com.dk.focusguard.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dk.focusguard.ui.theme.CyberBackground
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberGreenGlow
import com.dk.focusguard.ui.theme.TextGray

@Composable
fun DkBottomNavBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Subtle top hairline cyber border
                drawLine(
                    color = CyberCardBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            },
        containerColor = CyberCard,
        tonalElevation = 0.dp
    ) {
        Screen.bottomNavItems.forEach { screen ->
            val isSelected = when (screen) {
                Screen.Dashboard -> currentRoute == Screen.Dashboard.route
                Screen.Protection -> currentRoute == Screen.Protection.route || currentRoute == Screen.AccessibilitySetup.route
                Screen.Focus -> currentRoute == Screen.Focus.route
                Screen.Statistics -> currentRoute == Screen.Statistics.route
                Screen.Settings -> currentRoute == Screen.Settings.route || currentRoute == Screen.PinSetup.route || currentRoute == Screen.ChangePin.route || currentRoute?.startsWith("pin_unlock") == true
                else -> currentRoute == screen.route
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    onNavigateToRoute(screen.route)
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberBackground,
                    selectedTextColor = CyberGreen,
                    indicatorColor = CyberGreen,
                    unselectedIconColor = TextGray,
                    unselectedTextColor = TextGray
                )
            )
        }
    }
}
