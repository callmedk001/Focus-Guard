package com.dk.focusguard.ui.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.domain.model.OverallProtectionStatus
import com.dk.focusguard.domain.model.ProtectionModuleState
import com.dk.focusguard.ui.components.CyberShieldGraphic
import com.dk.focusguard.ui.components.DkCard
import com.dk.focusguard.ui.components.DkPrimaryButton
import com.dk.focusguard.ui.components.DkSectionHeader
import com.dk.focusguard.ui.components.DkStatusBadge
import com.dk.focusguard.ui.components.DkSwitchRow
import com.dk.focusguard.ui.theme.CyberBackground
import com.dk.focusguard.ui.theme.CyberBorderGradient
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
import com.dk.focusguard.utils.DateTimeUtils

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToFocus: () -> Unit,
    onNavigateToProtection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Control Center Header with Real Aggregated Status
                CyberDashboardHeader(
                    overallStatus = uiState.overallStatus,
                    activeScheduleName = uiState.activeScheduleName
                )

                // Main Cybersecurity Protection Card
                MainCyberProtectionCard(
                    overallStatus = uiState.overallStatus,
                    activeScheduleName = uiState.activeScheduleName,
                    nextTransitionFormatted = uiState.nextScheduleTransitionFormatted,
                    overrideRemainingSeconds = uiState.overrideRemainingSeconds,
                    onToggleProtection = { viewModel.toggleMasterProtection() },
                    onNavigateToProtection = onNavigateToProtection
                )

                // Engine Health & Scheduled Defense Status Banner
                ProtectionPostureBanner(
                    uiState = uiState,
                    onNavigateToProtection = onNavigateToProtection,
                    onNavigateToFocus = onNavigateToFocus
                )

                // Statistics Cards Grid (3 Cards)
                DkSectionHeader(
                    title = "Today's Focus"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Block,
                        title = "Blocked",
                        value = "${uiState.distractionsBlockedToday}",
                        subtitle = "Today",
                        accentColor = CyberRed
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.HourglassTop,
                        title = "Focus Time",
                        value = "${uiState.focusMinutesAchievedToday}m",
                        subtitle = "Saved",
                        accentColor = CyberGreen
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.StayCurrentPortrait,
                        title = "Screen Time",
                        value = DateTimeUtils.formatDurationFromMillis(uiState.dailyScreenTimeMinutes * 60 * 1000L),
                        subtitle = "Total",
                        accentColor = CyberCyan
                    )
                }

                // Quick Controls Section
                DkSectionHeader(
                    title = "Blockers"
                )

                DkCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 16.dp
                ) {
                    DkSwitchRow(
                        title = "YouTube Shorts",
                        icon = Icons.Default.SmartDisplay,
                        checked = uiState.blockYouTubeShorts,
                        onCheckedChange = { viewModel.toggleYouTubeShorts(it) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CyberCardBorder)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    DkSwitchRow(
                        title = "Instagram Reels",
                        icon = Icons.Default.PlayCircleOutline,
                        checked = uiState.blockInstagramReels,
                        onCheckedChange = { viewModel.toggleInstagramReels(it) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CyberCardBorder)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    DkSwitchRow(
                        title = "Adult Websites",
                        icon = Icons.Default.Explicit,
                        checked = uiState.blockAdultWebsites,
                        onCheckedChange = { viewModel.toggleAdultWebsites(it) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

private fun getModuleStatusBadge(state: ProtectionModuleState, isUserEnabled: Boolean): String {
    if (!isUserEnabled) return "OFF"
    return when (state) {
        ProtectionModuleState.ACTIVE -> "ARMED"
        ProtectionModuleState.SCHEDULED -> "SCHEDULED"
        ProtectionModuleState.OFFLINE -> "OFFLINE"
        ProtectionModuleState.PERMISSION_REQUIRED -> "PERM REQ"
        ProtectionModuleState.INACTIVE -> "STANDBY"
    }
}

@Composable
private fun CyberDashboardHeader(
    overallStatus: OverallProtectionStatus,
    activeScheduleName: String?
) {
    val (statusColor, badgeText) = when (overallStatus) {
        OverallProtectionStatus.ACTIVE -> CyberGreen to "ACTIVE"
        OverallProtectionStatus.SCHEDULED -> CyberCyan to "SCHEDULED"
        OverallProtectionStatus.OVERRIDE_ACTIVE -> CyberYellow to "PAUSED"
        OverallProtectionStatus.PERMISSION_REQUIRED -> CyberYellow to "SETUP NEEDED"
        OverallProtectionStatus.OFFLINE -> CyberRed to "OFFLINE"
        OverallProtectionStatus.INACTIVE -> CyberRed to "OFF"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FOCUS GUARD",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (activeScheduleName != null) "Schedule: $activeScheduleName" else "Digital Discipline Center",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }

        DkStatusBadge(
            isActive = overallStatus == OverallProtectionStatus.ACTIVE || overallStatus == OverallProtectionStatus.SCHEDULED,
            activeText = badgeText,
            inactiveText = badgeText,
            customActiveColor = statusColor,
            customInactiveColor = statusColor
        )
    }
}

@Composable
private fun MainCyberProtectionCard(
    overallStatus: OverallProtectionStatus,
    activeScheduleName: String?,
    nextTransitionFormatted: String?,
    overrideRemainingSeconds: Long,
    onToggleProtection: () -> Unit,
    onNavigateToProtection: () -> Unit
) {
    val isGuarding = overallStatus == OverallProtectionStatus.ACTIVE || overallStatus == OverallProtectionStatus.SCHEDULED

    val (titleText, subtitleText, statusColor) = when (overallStatus) {
        OverallProtectionStatus.ACTIVE -> Triple(
            "Protection Active",
            "Shorts, Reels & Adult Content Blocked",
            CyberGreen
        )
        OverallProtectionStatus.SCHEDULED -> Triple(
            "Schedule Active",
            activeScheduleName?.let { "$it in progress" } ?: "Focus schedule in progress",
            CyberCyan
        )
        OverallProtectionStatus.OVERRIDE_ACTIVE -> {
            val mins = (overrideRemainingSeconds / 60).coerceAtLeast(1)
            Triple(
                "Protection Paused",
                "Temporary pause active. Resumes in ${mins}m",
                CyberYellow
            )
        }
        OverallProtectionStatus.PERMISSION_REQUIRED -> Triple(
            "Setup Required",
            "Accessibility or VPN permission required",
            CyberYellow
        )
        OverallProtectionStatus.OFFLINE -> Triple(
            "Protection Offline",
            "Background service is currently stopped",
            CyberRed
        )
        OverallProtectionStatus.INACTIVE -> Triple(
            "Protection Off",
            "Turn on to block distracting feeds",
            CyberRed
        )
    }

    DkCard(
        isGlowActive = isGuarding,
        borderBrush = if (isGuarding) CyberBorderGradient else null,
        contentPadding = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CyberShieldGraphic(
                isActive = isGuarding,
                size = 80.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )

            if (nextTransitionFormatted != null && overallStatus == OverallProtectionStatus.SCHEDULED) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Active until $nextTransitionFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val buttonText = when (overallStatus) {
                OverallProtectionStatus.ACTIVE, OverallProtectionStatus.SCHEDULED -> "Pause Protection"
                OverallProtectionStatus.OVERRIDE_ACTIVE -> "Override Active"
                OverallProtectionStatus.PERMISSION_REQUIRED -> "Grant Permissions"
                OverallProtectionStatus.OFFLINE -> "Start Service"
                OverallProtectionStatus.INACTIVE -> "Turn On Protection"
            }

            val buttonAction = when (overallStatus) {
                OverallProtectionStatus.PERMISSION_REQUIRED, OverallProtectionStatus.OFFLINE -> onNavigateToProtection
                else -> onToggleProtection
            }

            DkPrimaryButton(
                text = buttonText,
                onClick = buttonAction,
                useGradient = !isGuarding,
                containerColor = if (isGuarding) CyberCardVariant else CyberGreen,
                contentColor = if (isGuarding) TextWhite else CyberBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "YouTube Shorts • Instagram Reels • Adult Filter",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun ProtectionPostureBanner(
    uiState: DashboardUiState,
    onNavigateToProtection: () -> Unit,
    onNavigateToFocus: () -> Unit
) {
    when (uiState.overallStatus) {
        OverallProtectionStatus.PERMISSION_REQUIRED -> {
            DkCard(
                backgroundColor = CyberCardVariant,
                borderColor = CyberYellow.copy(alpha = 0.5f),
                contentPadding = 12.dp,
                modifier = Modifier.clickable { onNavigateToProtection() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CyberYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Setup Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyberYellow
                            )
                            Text(
                                text = "Tap to grant Accessibility or VPN permission",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = CyberYellow,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        OverallProtectionStatus.SCHEDULED -> {
            DkCard(
                backgroundColor = CyberCardVariant,
                borderColor = CyberCyan.copy(alpha = 0.4f),
                contentPadding = 12.dp,
                modifier = Modifier.clickable { onNavigateToFocus() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Focus Schedule: ${uiState.activeScheduleName ?: "Active"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = "Automated focus window in progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        else -> {
            // Clean interface: no redundant banners when protection is active or standard
        }
    }
}

@Composable
private fun TelemetryCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    DkCard(
        modifier = modifier,
        backgroundColor = CyberCard,
        contentPadding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1
        )
    }
}
