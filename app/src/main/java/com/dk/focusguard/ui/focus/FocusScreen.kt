package com.dk.focusguard.ui.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.domain.model.FocusSession
import com.dk.focusguard.ui.components.DkCard
import com.dk.focusguard.ui.components.DkEmptyState
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
import com.dk.focusguard.ui.theme.TextGray
import com.dk.focusguard.ui.theme.TextMuted
import com.dk.focusguard.ui.theme.TextWhite
import com.dk.focusguard.utils.DateTimeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingStartTime by remember { mutableStateOf(false) }
    var editingEndTime by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                FocusHeader()
            }

            // Save feedback banner
            item {
                AnimatedVisibility(
                    visible = uiState.scheduleSaveFeedback != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    DkCard(
                        backgroundColor = CyberGreen.copy(alpha = 0.15f),
                        borderColor = CyberGreen,
                        contentPadding = 12.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CyberGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.scheduleSaveFeedback ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberGreen
                            )
                        }
                    }
                }
            }

            // SECTION 1: SCHEDULED AUTOMATIC FOCUS DEFENSE
            item {
                DkSectionHeader(
                    title = "Scheduled Defense Protocol",
                    subtitle = "Automate focus defense during routine deep work hours"
                )
            }

            item {
                DkCard(
                    isGlowActive = uiState.isScheduleEnabled,
                    borderBrush = if (uiState.isScheduleEnabled) CyberBorderGradient else null,
                    contentPadding = 18.dp
                ) {
                    // Focus Mode Master Toggle
                    DkSwitchRow(
                        title = "Focus Defense Mode",
                        description = "Enforce scheduled shielding across target applications.",
                        icon = Icons.Default.Schedule,
                        checked = uiState.isScheduleEnabled,
                        onCheckedChange = { viewModel.toggleScheduleEnabled(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CyberCardBorder)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Start & End Time Selectors
                    Text(
                        text = "ACTIVE WINDOW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimeSelectorBox(
                            modifier = Modifier.weight(1f),
                            label = "Start Time",
                            time = uiState.scheduledStartTime,
                            onClick = {
                                val next = if (uiState.scheduledStartTime == "09:00") "08:30" else "09:00"
                                viewModel.updateStartTime(next)
                            }
                        )
                        TimeSelectorBox(
                            modifier = Modifier.weight(1f),
                            label = "End Time",
                            time = uiState.scheduledEndTime,
                            onClick = {
                                val next = if (uiState.scheduledEndTime == "17:00") "18:00" else "17:00"
                                viewModel.updateEndTime(next)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weekly Schedule Days
                    Text(
                        text = "RECURRENCE (ACTIVE DAYS)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            val isSelected = uiState.selectedDays.contains(day)
                            DaySelectorChip(
                                day = day,
                                isSelected = isSelected,
                                onClick = { viewModel.toggleDay(day) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Distracting Applications Selection
                    Text(
                        text = "RESTRICTED TARGET APPS (${uiState.selectedAppsForFocus.size} selected)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        uiState.availableApps.forEach { (appName, pkg) ->
                            val isChecked = uiState.selectedAppsForFocus.contains(pkg)
                            AppCheckRow(
                                appName = appName,
                                packageName = pkg,
                                isChecked = isChecked,
                                onToggle = { viewModel.toggleAppSelection(pkg) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Schedule Button
                    DkPrimaryButton(
                        text = "Save Defense Schedule",
                        onClick = { viewModel.saveSchedule() },
                        useGradient = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = CyberBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            // SECTION 2: ON-DEMAND FOCUS SESSION (COUNTDOWN TIMER)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                DkSectionHeader(
                    title = "On-Demand Focus Chamber",
                    subtitle = "Trigger immediate distraction lockdown"
                )
            }

            item {
                CyberTimerCard(
                    remainingSeconds = uiState.remainingSeconds,
                    totalSeconds = uiState.totalSeconds,
                    timerState = uiState.timerState
                )
            }

            if (uiState.timerState == FocusTimerState.IDLE) {
                item {
                    DkSectionHeader(title = "Preset Session Length")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(15, 25, 45, 60).forEach { mins ->
                            DurationSelectorPill(
                                minutes = mins,
                                isSelected = uiState.selectedDurationMinutes == mins,
                                onClick = { viewModel.selectDuration(mins) }
                            )
                        }
                    }
                }
            }

            item {
                TimerActionControls(
                    timerState = uiState.timerState,
                    onStart = { viewModel.startTimer() },
                    onPause = { viewModel.pauseTimer() },
                    onResume = { viewModel.resumeTimer() },
                    onCancel = { viewModel.cancelTimer() }
                )
            }

            item {
                DkSectionHeader(
                    title = "Logged Focus History",
                    subtitle = "${uiState.recentSessions.size} sessions recorded"
                )
            }

            if (uiState.recentSessions.isEmpty()) {
                item {
                    DkCard {
                        DkEmptyState(
                            icon = Icons.Default.Timer,
                            title = "No Focus History",
                            message = "Completed focus sessions will be cataloged here."
                        )
                    }
                }
            } else {
                items(uiState.recentSessions, key = { it.id }) { session ->
                    FocusSessionItem(session = session)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FocusHeader() {
    Column {
        Text(
            text = "DISCIPLINE ENGINE",
            style = MaterialTheme.typography.labelSmall,
            color = CyberGreen,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Focus Mode Control",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = "Configure automated schedules and initiate deep work sessions",
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )
    }
}

@Composable
private fun TimeSelectorBox(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(CyberCardVariant)
            .border(1.dp, CyberCardBorder, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        }
    }
}

@Composable
private fun DaySelectorChip(
    day: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isSelected) CyberGreen else CyberCardVariant)
            .border(
                1.dp,
                if (isSelected) CyberGreen else CyberCardBorder,
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.take(2),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CyberBackground else TextGray
        )
    }
}

@Composable
private fun AppCheckRow(
    appName: String,
    packageName: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = CyberGreen,
                checkmarkColor = CyberBackground,
                uncheckedColor = CyberCardBorder
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun CyberTimerCard(
    remainingSeconds: Long,
    totalSeconds: Long,
    timerState: FocusTimerState
) {
    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    DkCard(
        modifier = Modifier.fillMaxWidth(),
        isGlowActive = timerState == FocusTimerState.RUNNING,
        contentPadding = 24.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(190.dp),
                color = CyberCardVariant,
                strokeWidth = 10.dp
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(190.dp),
                color = if (timerState == FocusTimerState.RUNNING) CyberGreen else CyberCyan,
                strokeWidth = 10.dp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = DateTimeUtils.formatDuration(remainingSeconds),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp),
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                DkStatusBadge(
                    isActive = timerState == FocusTimerState.RUNNING,
                    activeText = "LOCKED IN",
                    inactiveText = if (timerState == FocusTimerState.PAUSED) "PAUSED" else "STANDBY",
                    customActiveColor = CyberGreen,
                    customInactiveColor = CyberCyan
                )
            }
        }
    }
}

@Composable
private fun DurationSelectorPill(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (isSelected) CyberGreen else CyberCardVariant)
            .border(
                1.dp,
                if (isSelected) CyberGreen else CyberCardBorder,
                MaterialTheme.shapes.small
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${minutes}m",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CyberBackground else TextWhite
        )
    }
}

@Composable
private fun TimerActionControls(
    timerState: FocusTimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    when (timerState) {
        FocusTimerState.IDLE -> {
            DkPrimaryButton(
                text = "Initiate Focus Lockdown",
                onClick = onStart,
                useGradient = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = CyberBackground
                    )
                }
            )
        }
        FocusTimerState.RUNNING -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DkPrimaryButton(
                    modifier = Modifier.weight(1f),
                    text = "Pause",
                    onClick = onPause,
                    containerColor = CyberCardVariant,
                    contentColor = TextWhite,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                    }
                )
                DkPrimaryButton(
                    modifier = Modifier.weight(1f),
                    text = "Abandon",
                    onClick = onCancel,
                    containerColor = CyberRed.copy(alpha = 0.2f),
                    contentColor = CyberRed,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                )
            }
        }
        FocusTimerState.PAUSED -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DkPrimaryButton(
                    modifier = Modifier.weight(1f),
                    text = "Resume",
                    onClick = onResume,
                    useGradient = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = CyberBackground)
                    }
                )
                DkPrimaryButton(
                    modifier = Modifier.weight(1f),
                    text = "Reset",
                    onClick = onCancel,
                    containerColor = CyberCardVariant,
                    contentColor = TextWhite,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    }
                )
            }
        }
        FocusTimerState.COMPLETED -> {
            DkPrimaryButton(
                text = "Session Completed — Start New",
                onClick = onCancel,
                useGradient = true
            )
        }
    }
}

@Composable
private fun FocusSessionItem(session: FocusSession) {
    DkCard(contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CyberGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = DateTimeUtils.formatTimestamp(session.startTimeMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
            Text(
                text = "${session.plannedDurationMinutes}m",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
        }
    }
}
