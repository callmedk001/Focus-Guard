package com.dk.focusguard.ui.protection

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.domain.model.AppCategory
import com.dk.focusguard.domain.model.BlockedApp
import com.dk.focusguard.domain.repository.AccessibilityServiceState
import com.dk.focusguard.ui.accessibility.AccessibilityStatusCard
import com.dk.focusguard.ui.accessibility.AccessibilityViewModel
import com.dk.focusguard.ui.components.DkCard
import com.dk.focusguard.ui.components.DkSectionHeader
import com.dk.focusguard.ui.components.DkStatusBadge
import com.dk.focusguard.ui.protection.components.AdultWebsiteProtectionCard
import com.dk.focusguard.ui.protection.components.CustomDomainsDialog
import com.dk.focusguard.ui.protection.components.InstagramReelsProtectionCard
import com.dk.focusguard.ui.protection.components.YouTubeShortsProtectionCard
import com.dk.focusguard.ui.theme.CyberBackground
import com.dk.focusguard.ui.theme.CyberBorderGradient
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberCardVariant
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed
import com.dk.focusguard.ui.theme.TextGray
import com.dk.focusguard.ui.theme.TextMuted
import com.dk.focusguard.ui.theme.TextWhite

@Composable
fun ProtectionScreen(
    viewModel: ProtectionViewModel,
    accessibilityViewModel: AccessibilityViewModel? = null,
    vpnConnectionManager: com.dk.focusguard.services.vpn.VpnConnectionManager? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToAccessibilitySetup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val accessibilityUiState = accessibilityViewModel?.uiState?.collectAsState()?.value
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddAppDialog by remember { mutableStateOf(false) }
    var showCustomDomainsDialog by remember { mutableStateOf(false) }

    val vpnConsentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            vpnConnectionManager?.startVpn(context)
        }
    }

    fun requestVpnOrStart() {
        if (vpnConnectionManager != null) {
            val consentIntent = vpnConnectionManager.getVpnConsentIntent(context)
            if (consentIntent != null) {
                vpnConsentLauncher.launch(consentIntent)
            } else {
                vpnConnectionManager.startVpn(context)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAppDialog = true },
                containerColor = CyberGreen,
                contentColor = CyberBackground
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Application")
            }
        }
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
                    Spacer(modifier = Modifier.height(4.dp))
                    ProtectionHeader(
                        isAnyShieldActive = uiState.rules.any { it.isEnabled },
                        onNavigateBack = onNavigateBack
                    )
                }

                // Phase 5: Live Accessibility Service Status Card
                item {
                    AccessibilityStatusCard(
                        serviceState = accessibilityUiState?.serviceState
                            ?: AccessibilityServiceState.PERMISSION_NOT_GRANTED,
                        currentForegroundAppName = accessibilityUiState?.currentForegroundAppName,
                        lastEventTimestamp = accessibilityUiState?.lastEventTimestamp ?: 0L,
                        onSetupClick = onNavigateToAccessibilitySetup
                    )
                }

                item {
                    DkSectionHeader(
                        title = "Blockers"
                    )
                }

                // 1. YouTube Shorts Shield (Phase 6 Dedicated Card)
                val ytRule = uiState.rules.find { it.id == "rule_yt_shorts" }
                val ytEnabled = ytRule?.isEnabled ?: true
                item {
                    YouTubeShortsProtectionCard(
                        isProtectionEnabled = ytEnabled,
                        isYouTubeForeground = uiState.isYouTubeForegroundActive,
                        accessibilityServiceState = accessibilityUiState?.serviceState
                            ?: AccessibilityServiceState.PERMISSION_NOT_GRANTED,
                        interruptionCount = uiState.youtubeInterruptionCount,
                        lastDetectionTimestamp = uiState.lastYoutubeDetectionTimestamp,
                        isEmergencyOverrideActive = uiState.isEmergencyOverrideActive,
                        onToggleProtection = { viewModel.toggleRule("rule_yt_shorts", it) },
                        onSetupAccessibilityClick = onNavigateToAccessibilitySetup
                    )
                }

                // 2. Instagram Reels Guard (Phase 7 Dedicated Card)
                val igRule = uiState.rules.find { it.id == "rule_ig_reels" }
                val igEnabled = igRule?.isEnabled ?: true
                item {
                    InstagramReelsProtectionCard(
                        isProtectionEnabled = igEnabled,
                        isInstagramForeground = uiState.isInstagramForegroundActive,
                        accessibilityServiceState = accessibilityUiState?.serviceState
                            ?: AccessibilityServiceState.PERMISSION_NOT_GRANTED,
                        interruptionCount = uiState.instagramInterruptionCount,
                        lastDetectionTimestamp = uiState.lastInstagramDetectionTimestamp,
                        isEmergencyOverrideActive = uiState.isEmergencyOverrideActive,
                        onToggleProtection = { viewModel.toggleRule("rule_ig_reels", it) },
                        onSetupAccessibilityClick = onNavigateToAccessibilitySetup
                    )
                }

                // 3. Adult Website Protection Card (Phase 8 Dedicated Card)
                val adultRule = uiState.rules.find { it.id == "rule_adult_filter" }
                val adultEnabled = adultRule?.isEnabled ?: true
                item {
                    AdultWebsiteProtectionCard(
                        isProtectionEnabled = adultEnabled,
                        isSafeSearchEnabled = uiState.isSafeSearchEnabled,
                        vpnState = uiState.vpnConnectionState,
                        totalBlocklistCount = uiState.adultBlocklistCount,
                        blockedDnsCount = uiState.adultBlockedDnsCount,
                        lastBlockedTimestamp = uiState.lastAdultBlockedTimestamp,
                        isEmergencyOverrideActive = uiState.isEmergencyOverrideActive,
                        onToggleProtection = { enabled ->
                            viewModel.toggleRule("rule_adult_filter", enabled)
                            if (enabled) {
                                requestVpnOrStart()
                            } else {
                                vpnConnectionManager?.stopVpn(context)
                            }
                        },
                        onToggleSafeSearch = { viewModel.toggleSafeSearch(it) },
                        onConfigureVpnClick = { requestVpnOrStart() },
                        onManageCustomDomainsClick = { showCustomDomainsDialog = true }
                    )
                }

                // Custom Monitored Applications Section
                item {
                    DkSectionHeader(
                        title = "Monitored Applications",
                        subtitle = "Installed applications subject to discipline limits"
                    )
                }

                items(uiState.blockedApps, key = { it.packageName }) { app ->
                    MonitoredAppItem(
                        app = app,
                        onToggle = { isChecked ->
                            viewModel.toggleAppBlock(app.packageName, isChecked)
                        },
                        onDelete = {
                            viewModel.removeBlockedApp(app.packageName)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }

    if (showAddAppDialog) {
        AddAppDialog(
            onDismiss = { showAddAppDialog = false },
            onAdd = { pkg, name, category ->
                viewModel.addCustomBlockedApp(pkg, name, category)
                showAddAppDialog = false
            }
        )
    }

    if (showCustomDomainsDialog) {
        CustomDomainsDialog(
            customDomains = uiState.customBlockedDomains,
            onAddDomain = { viewModel.addCustomDomain(it) },
            onRemoveDomain = { viewModel.removeCustomDomain(it) },
            onImportDomains = { viewModel.importDomains(it) },
            onDismiss = { showCustomDomainsDialog = false }
        )
    }
}

@Composable
private fun ProtectionHeader(
    isAnyShieldActive: Boolean,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = TextWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Protection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Distraction Shields & Web Filter",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }

        DkStatusBadge(
            isActive = isAnyShieldActive,
            activeText = "ARMED",
            inactiveText = "STANDBY"
        )
    }
}

@Composable
private fun ProtectionModuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    configStatus: String,
    onToggle: (Boolean) -> Unit
) {
    val borderColor = if (isEnabled) CyberGreen.copy(alpha = 0.4f) else CyberCardBorder
    DkCard(
        borderColor = borderColor,
        contentPadding = 16.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isEnabled) CyberGreen.copy(alpha = 0.15f) else CyberCardVariant,
                                shape = MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isEnabled) CyberGreen else TextGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        DkStatusBadge(
                            isActive = isEnabled,
                            activeText = configStatus,
                            inactiveText = "DISABLED",
                            customActiveColor = CyberGreen,
                            customInactiveColor = CyberRed
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBackground,
                        checkedTrackColor = CyberGreen,
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = CyberCardVariant,
                        uncheckedBorderColor = CyberCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun MonitoredAppItem(
    app: BlockedApp,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    DkCard(
        borderColor = if (app.isBlocked) CyberCyan.copy(alpha = 0.3f) else CyberCardBorder,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (app.isBlocked) CyberCyan.copy(alpha = 0.15f) else CyberCardVariant,
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (app.isBlocked) CyberCyan else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = app.isBlocked,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBackground,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = TextGray,
                        uncheckedTrackColor = CyberCardVariant,
                        uncheckedBorderColor = CyberCardBorder
                    )
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove App",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddAppDialog(
    onDismiss: () -> Unit,
    onAdd: (packageName: String, appName: String, category: AppCategory) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCard,
        title = {
            Text(
                text = "Add Monitored Application",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = {
                        appName = it
                        isError = false
                    },
                    label = { Text("Application Name") },
                    placeholder = { Text("e.g. Threads, Reddit") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedLabelColor = CyberGreen,
                        unfocusedLabelColor = TextGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = {
                        packageName = it
                        isError = false
                    },
                    label = { Text("Package Name") },
                    placeholder = { Text("e.g. com.instagram.barcelona") },
                    isError = isError,
                    supportingText = {
                        if (isError) Text("Please enter a valid package name", color = CyberRed)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedLabelColor = CyberGreen,
                        unfocusedLabelColor = TextGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (packageName.isNotBlank() && appName.isNotBlank()) {
                        onAdd(packageName.trim(), appName.trim(), AppCategory.SOCIAL)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Add Guard", color = CyberGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGray)
            }
        }
    )
}
