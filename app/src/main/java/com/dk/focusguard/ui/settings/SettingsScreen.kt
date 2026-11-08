package com.dk.focusguard.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.ui.components.DkCard
import com.dk.focusguard.ui.components.DkPrimaryButton
import com.dk.focusguard.ui.components.DkSectionHeader
import com.dk.focusguard.ui.components.DkStatusBadge
import com.dk.focusguard.ui.components.DkSwitchRow
import com.dk.focusguard.ui.security.EmergencyOverrideDialog
import com.dk.focusguard.ui.theme.CyberBackground
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberCardVariant
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed
import com.dk.focusguard.ui.theme.CyberYellow
import com.dk.focusguard.ui.theme.TextGray
import com.dk.focusguard.ui.theme.TextMuted
import com.dk.focusguard.ui.theme.TextWhite
import com.dk.focusguard.utils.PermissionUtils

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToPinSetup: () -> Unit = {},
    onNavigateToChangePin: () -> Unit = {},
    onNavigateToPinUnlock: (actionDescription: String, onAuthorized: () -> Unit) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showResetDialog by remember { mutableStateOf(false) }
    var showClearSampleDialog by remember { mutableStateOf(false) }
    var showEmergencyOverrideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsHeader()
                }

                // Feedback banner for data operations
                if (uiState.dataActionFeedback != null) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberGreen.copy(alpha = 0.15f))
                                    .border(1.dp, CyberGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = uiState.dataActionFeedback ?: "",
                                    color = CyberGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Active Emergency Override Alert Banner
                if (uiState.isEmergencyOverrideActive) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberRed.copy(alpha = 0.15f))
                                .border(1.dp, CyberRed, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Alert",
                                        tint = CyberRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "EMERGENCY DEFENSE OVERRIDE ACTIVE",
                                        color = CyberRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                val minutes = uiState.emergencyOverrideRemainingSeconds / 60
                                val seconds = uiState.emergencyOverrideRemainingSeconds % 60
                                Text(
                                    text = "Protection filters suspended for ${minutes}m ${seconds}s remaining.",
                                    color = TextWhite,
                                    fontSize = 12.sp
                                )
                                Button(
                                    onClick = { viewModel.cancelEmergencyOverride() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Terminate Override & Resume Protection", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 1. CYBERSECURITY & ACCESS DEFENSE (PHASE 4 ACTIVE)
                item {
                    DkSectionHeader(
                        title = "Cybersecurity & Access Defense",
                        subtitle = "Cryptographic Master PIN & Hardware Biometrics"
                    )

                    DkCard(contentPadding = 16.dp) {
                        // Master PIN Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = if (uiState.isPinSet) CyberGreen else CyberYellow,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Master Security PIN",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = if (uiState.isPinSet) "PBKDF2-HMAC-SHA256 Encrypted" else "No security PIN configured",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray
                                    )
                                }
                            }

                            DkStatusBadge(
                                isActive = uiState.isPinSet,
                                activeText = "CONFIGURED",
                                inactiveText = "NOT SET",
                                customActiveColor = CyberGreen,
                                customInactiveColor = CyberYellow
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // PIN Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!uiState.isPinSet) {
                                DkPrimaryButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "Setup Master PIN",
                                    onClick = onNavigateToPinSetup,
                                    containerColor = CyberGreen,
                                    contentColor = Color.Black
                                )
                            } else {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (viewModel.isAuthenticationRequiredFor("CHANGE_PIN")) {
                                            onNavigateToPinUnlock("Change Master PIN") {
                                                onNavigateToChangePin()
                                            }
                                        } else {
                                            onNavigateToChangePin()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                                ) {
                                    Text("Change PIN", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (viewModel.isAuthenticationRequiredFor("CLEAR_PIN")) {
                                            onNavigateToPinUnlock("Remove Master PIN") {
                                                viewModel.clearPin()
                                            }
                                        } else {
                                            viewModel.clearPin()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f))
                                ) {
                                    Text("Remove PIN", color = CyberRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Biometric Authentication Switch
                        DkSwitchRow(
                            title = "Biometric Verification",
                            description = if (uiState.isBiometricAvailable) {
                                "Allow biometric unlock as fallback to Master PIN."
                            } else {
                                "Device biometric hardware or fingerprints unavailable."
                            },
                            icon = Icons.Default.Fingerprint,
                            checked = uiState.isBiometricEnabled && uiState.isBiometricAvailable && uiState.isPinSet,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) },
                            enabled = uiState.isBiometricAvailable && uiState.isPinSet
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto-Lock Inactivity Period
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Auto-Lock Inactivity Timeout",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = "Re-locks sensitive settings when app is idle or in background",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray
                                    )
                                }
                            }
                            Text(
                                text = "${uiState.autoLockSeconds}s",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "Instant", 60 to "1 min", 120 to "2 mins", 300 to "5 mins").forEach { (secs, label) ->
                                val isSelected = uiState.autoLockSeconds == secs
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberCardVariant)
                                        .border(
                                            1.dp,
                                            if (isSelected) CyberCyan else CyberCardBorder,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { viewModel.setAutoLockSeconds(secs) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CyberCyan else TextGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Protection Policies
                        DkSwitchRow(
                            title = "Protect Guard Controls",
                            description = "Require PIN verification before modifying protection switches or rules.",
                            icon = Icons.Default.Shield,
                            checked = uiState.requireAuthForProtection,
                            onCheckedChange = { viewModel.setRequireAuthForProtection(it) },
                            enabled = uiState.isPinSet
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DkSwitchRow(
                            title = "Protect Data Reset",
                            description = "Require PIN verification before resetting application history or telemetry.",
                            icon = Icons.Default.Lock,
                            checked = uiState.requireAuthForDataReset,
                            onCheckedChange = { viewModel.setRequireAuthForDataReset(it) },
                            enabled = uiState.isPinSet
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Emergency Override Trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Emergency Defense Override",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Temporarily suspend guards with mandatory PIN verification & audit record",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    if (viewModel.isAuthenticationRequiredFor("EMERGENCY_OVERRIDE")) {
                                        onNavigateToPinUnlock("Authorize Emergency Override") {
                                            showEmergencyOverrideDialog = true
                                        }
                                    } else {
                                        showEmergencyOverrideDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f))
                            ) {
                                Text("Override", color = CyberRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Note: Android OS allows app uninstallation via system settings; Master PIN secures all in-app controls and defense parameters.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // 2. DATA MANAGEMENT & OFFLINE ENGINE
                item {
                    DkSectionHeader(
                        title = "Local Database & Storage Engine",
                        subtitle = "Offline-first persistent storage telemetry"
                    )

                    DkCard(contentPadding = 16.dp) {
                        DataTelemetryRow(
                            label = "Saved Focus Sessions",
                            value = "${uiState.savedFocusSessionsCount} sessions",
                            description = "Persisted deep work records in Room database",
                            accentColor = CyberGreen
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(10.dp))

                        DataTelemetryRow(
                            label = "Genuine Block Events",
                            value = "${uiState.realBlockEventsCount} events",
                            description = "Authentic security & interception event logs",
                            accentColor = CyberCyan
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(10.dp))

                        DataTelemetryRow(
                            label = "Monitored Applications",
                            value = "${uiState.monitoredAppsCount} apps",
                            description = "Configured application rules in monitored_apps table",
                            accentColor = CyberGreen
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(10.dp))

                        DataTelemetryRow(
                            label = "Database Storage Footprint",
                            value = "${uiState.databaseStorageSizeKb} KB",
                            description = "Room SQLite Database v2 (dk_focus_guard_database)",
                            accentColor = CyberYellow
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DkPrimaryButton(
                                modifier = Modifier.weight(1f),
                                text = "Clear Sample Data",
                                onClick = { showClearSampleDialog = true },
                                containerColor = CyberCardVariant,
                                contentColor = TextWhite,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            DkPrimaryButton(
                                modifier = Modifier.weight(1f),
                                text = "Reset All Data",
                                onClick = {
                                    if (viewModel.isAuthenticationRequiredFor("RESET_DATA")) {
                                        onNavigateToPinUnlock("Reset Application Data") {
                                            showResetDialog = true
                                        }
                                    } else {
                                        showResetDialog = true
                                    }
                                },
                                containerColor = CyberRed.copy(alpha = 0.2f),
                                contentColor = CyberRed,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        tint = CyberRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // 3. PERMISSIONS ARCHITECTURE & HEALTH
                item {
                    DkSectionHeader(
                        title = "System Permissions Telemetry",
                        subtitle = "Android privileges required for background defense operations"
                    )

                    DkCard(contentPadding = 16.dp) {
                        PermissionActionRow(
                            icon = Icons.Default.QueryStats,
                            title = "Usage Access (AppOps)",
                            description = "Required to detect foreground distracting applications.",
                            isGranted = uiState.hasUsageStatsPermission,
                            onClick = {
                                runCatching {
                                    context.startActivity(PermissionUtils.getUsageStatsIntent())
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionActionRow(
                            icon = Icons.Default.Layers,
                            title = "Draw Over Other Apps (Overlay)",
                            description = "Enables attention block screen to shield you instantly.",
                            isGranted = uiState.hasOverlayPermission,
                            onClick = {
                                runCatching {
                                    context.startActivity(PermissionUtils.getOverlayIntent(context))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionActionRow(
                            icon = Icons.Default.Notifications,
                            title = "Foreground Notification Alert",
                            description = "Maintains persistent status banner during active defense.",
                            isGranted = uiState.hasNotificationPermission,
                            onClick = {
                                // Notification settings
                            }
                        )
                    }
                }

                // 4. THEME & STRICTNESS CONFIGURATION
                item {
                    DkSectionHeader(
                        title = "Visual Environment & Strictness",
                        subtitle = "Interface and defense strictness levels"
                    )

                    DkCard(contentPadding = 16.dp) {
                        DkSwitchRow(
                            title = "Strict Enforcement Mode",
                            description = "Disallows disabling protection or modifying rules during an active focus block.",
                            icon = Icons.Default.Lock,
                            checked = uiState.settings.isStrictModeEnabled,
                            onCheckedChange = {
                                if (viewModel.isAuthenticationRequiredFor("STRICT_MODE")) {
                                    onNavigateToPinUnlock("Modify Strict Enforcement Mode") {
                                        viewModel.toggleStrictMode(!uiState.settings.isStrictModeEnabled)
                                    }
                                } else {
                                    viewModel.toggleStrictMode(it)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(8.dp))

                        DkSwitchRow(
                            title = "Cyber Dark Command Center",
                            description = "Futuristic high-contrast cybersecurity color palette.",
                            icon = Icons.Default.DarkMode,
                            checked = uiState.settings.darkThemeEnabled,
                            onCheckedChange = { viewModel.toggleDarkTheme(it) }
                        )
                    }
                }

                // 5. ABOUT APPLICATION & VERSION
                item {
                    DkSectionHeader(
                        title = "Application Information",
                        subtitle = "Architecture & Release Details"
                    )

                    DkCard(contentPadding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "DK FOCUS GUARD",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                Text(
                                    text = "Personal Cyber Discipline & Attention Defense",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Build Architecture",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                            Text(
                                text = "MVVM + PBKDF2 + Keystore + Room v2",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "App Version",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                            Text(
                                text = "v1.0.0-phase4",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberGreen
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Emergency Override Setup Dialog
    if (showEmergencyOverrideDialog) {
        EmergencyOverrideDialog(
            onDismissRequest = { showEmergencyOverrideDialog = false },
            onConfirmOverride = { durationMinutes ->
                showEmergencyOverrideDialog = false
                viewModel.startEmergencyOverride(durationMinutes)
            }
        )
    }

    // Confirmation Dialog: Reset Application Data
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = CyberCard,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = CyberRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Reset Application Data?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "This will clear all recorded focus sessions, reset protection rules to defaults, remove Master PIN, and clear local preferences. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetApplicationData(context)
                        showResetDialog = false
                    }
                ) {
                    Text("Confirm Reset", color = CyberRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }

    // Confirmation Dialog: Clear Sample Data
    if (showClearSampleDialog) {
        AlertDialog(
            onDismissRequest = { showClearSampleDialog = false },
            containerColor = CyberCard,
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Switch to Pure Telemetry?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "This will deactivate sample preview charts and show only real database records. Real block counts will remain at 0 until background blocking services run.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearSampleStatistics()
                        showClearSampleDialog = false
                    }
                ) {
                    Text("Deactivate Sample Preview", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSampleDialog = false }) {
                    Text("Cancel", color = TextGray)
                }
            }
        )
    }
}

@Composable
private fun DataTelemetryRow(
    label: String,
    value: String,
    description: String,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column {
        Text(
            text = "SYSTEM CONFIGURATION",
            style = MaterialTheme.typography.labelSmall,
            color = CyberGreen,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Settings & Guard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = "Permissions status and cybersecurity configuration",
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )
    }
}

@Composable
private fun PermissionActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) CyberGreen else TextGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        DkStatusBadge(
            isActive = isGranted,
            activeText = "ACTIVE",
            inactiveText = "CONFIGURE",
            customActiveColor = CyberGreen,
            customInactiveColor = CyberYellow
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
