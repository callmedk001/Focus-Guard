package com.dk.focusguard.ui.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dk.focusguard.domain.repository.AccessibilityServiceState
import com.dk.focusguard.ui.components.DkCard
import com.dk.focusguard.ui.components.DkSectionHeader
import com.dk.focusguard.ui.components.DkStatusBadge
import com.dk.focusguard.ui.theme.CyberBackground
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed
import com.dk.focusguard.ui.theme.CyberYellow
import com.dk.focusguard.ui.theme.TextGray
import com.dk.focusguard.ui.theme.TextMuted
import com.dk.focusguard.ui.theme.TextWhite

/**
 * Onboarding and diagnostics walkthrough screen for Android Accessibility Service.
 * Clearly articulates privacy guarantees, target app scope, and system setup steps.
 */
@Composable
fun AccessibilitySetupScreen(
    viewModel: AccessibilityViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Reactively refresh permission state when returning from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "ACCESSIBILITY SETUP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / Status Banner
            item {
                DkCard(contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val iconColor = when (uiState.serviceState) {
                            AccessibilityServiceState.CONNECTED_AND_MONITORING -> CyberGreen
                            AccessibilityServiceState.PERMISSION_GRANTED_DISCONNECTED -> CyberYellow
                            else -> CyberRed
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.15f))
                                .border(1.dp, iconColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.serviceState == AccessibilityServiceState.CONNECTED_AND_MONITORING) {
                                    Icons.Default.Sensors
                                } else {
                                    Icons.Default.Warning
                                },
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DETECTION ENGINE STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = iconColor,
                                letterSpacing = 1.sp
                            )
                            val statusText = when (uiState.serviceState) {
                                AccessibilityServiceState.CONNECTED_AND_MONITORING -> "Connected & Actively Monitoring"
                                AccessibilityServiceState.PERMISSION_GRANTED_DISCONNECTED -> "Permission Granted (Reconnecting...)"
                                AccessibilityServiceState.PERMISSION_NOT_GRANTED -> "Permission Disabled in Android Settings"
                                AccessibilityServiceState.UNAVAILABLE -> "Accessibility Framework Unavailable"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }
                    }
                }
            }

            // Monitored Applications Scope
            item {
                DkSectionHeader(
                    title = "Monitored Target Applications",
                    subtitle = "The engine only observes registered apps on the allowlist"
                )

                DkCard(contentPadding = 16.dp) {
                    uiState.targetApps.forEachIndexed { index, app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = app.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = app.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                        }

                        if (index < uiState.targetApps.size - 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(CyberCardBorder)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            // Strict Privacy Commitments
            item {
                DkSectionHeader(
                    title = "Strict Privacy Guarantees",
                    subtitle = "Accessibility is used exclusively for content boundary detection"
                )

                DkCard(contentPadding = 16.dp) {
                    PrivacyDisclosureRow(
                        isPermitted = true,
                        title = "Structural View ID Inspection",
                        description = "Examines layout identifiers (e.g. reel_container, shorts_player) to identify short-form video sections."
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                    Spacer(modifier = Modifier.height(10.dp))

                    PrivacyDisclosureRow(
                        isPermitted = false,
                        title = "NO Keystroke or Input Recording",
                        description = "The service strictly skips all EditText and password nodes. Never captures typed keys."
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                    Spacer(modifier = Modifier.height(10.dp))

                    PrivacyDisclosureRow(
                        isPermitted = false,
                        title = "NO Screenshots or Visual Captures",
                        description = "Never uses takeScreenshot APIs. UI graphics and camera frames are never viewed."
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                    Spacer(modifier = Modifier.height(10.dp))

                    PrivacyDisclosureRow(
                        isPermitted = false,
                        title = "NO Chat or Message Reading",
                        description = "Never inspects direct messages, emails, contacts, account credentials, or private content."
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CyberCardBorder))
                    Spacer(modifier = Modifier.height(10.dp))

                    PrivacyDisclosureRow(
                        isPermitted = false,
                        title = "100% Offline Local Processing",
                        description = "No accessibility data or telemetry is ever uploaded or transmitted to external servers."
                    )
                }
            }

            // Android Activation Instructions
            item {
                DkSectionHeader(
                    title = "Android Activation Steps",
                    subtitle = "Manual permission consent required by Android OS"
                )

                DkCard(contentPadding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "1. Tap the button below to open Android Accessibility Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite
                        )
                        Text(
                            text = "2. Under 'Downloaded apps' or 'Installed services', tap 'DK Focus Guard Detection Engine'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite
                        )
                        Text(
                            text = "3. Turn ON the toggle and tap Allow to grant detection access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite
                        )
                        Text(
                            text = "4. Return to DK FOCUS GUARD to see the real-time active status.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberYellow.copy(alpha = 0.1f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Notice: Android may pause background accessibility services after prolonged device standby. You can verify connection status here at any time.",
                                color = CyberYellow,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(viewModel.getAccessibilitySettingsIntent())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Launch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Android Accessibility Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.refreshStatus(context) },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Verify Current Connection Status",
                                color = CyberCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PrivacyDisclosureRow(
    isPermitted: Boolean,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val icon = if (isPermitted) Icons.Default.Check else Icons.Default.Close
        val color = if (isPermitted) CyberGreen else CyberRed

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(1.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPermitted) TextWhite else CyberRed
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}

