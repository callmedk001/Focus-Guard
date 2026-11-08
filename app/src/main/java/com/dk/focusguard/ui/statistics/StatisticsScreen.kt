package com.dk.focusguard.ui.statistics

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.ui.components.DkCard
import com.dk.focusguard.ui.components.DkSectionHeader
import com.dk.focusguard.ui.components.DkStatusBadge
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

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    StatisticsHeader()
                }

                // Sample Data Disclaimer Banner
                item {
                    DkCard(
                        backgroundColor = CyberCardVariant,
                        borderColor = CyberYellow.copy(alpha = 0.4f),
                        contentPadding = 12.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = CyberYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SAMPLE DATA / PREVIEW: Displays projected telemetry and analytics. Real device usage stats will populate as permissions are activated.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }
                }

                // 1. DAILY USAGE OVERVIEW
                item {
                    DkSectionHeader(
                        title = "Daily Usage Surveillance",
                        subtitle = "Today's attention allocation"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Screen Time",
                            value = uiState.dailyScreenTimeFormatted,
                            subtitle = "-24% vs average",
                            icon = Icons.Default.StayCurrentPortrait,
                            tint = CyberCyan
                        )
                        StatMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Time Defended",
                            value = uiState.timeSavedFormatted,
                            subtitle = "Doomscroll saved",
                            icon = Icons.Default.Shield,
                            tint = CyberGreen
                        )
                        StatMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Intercepts",
                            value = "${uiState.totalInterceptedToday}",
                            subtitle = "Loops halted",
                            icon = Icons.Default.Block,
                            tint = CyberRed
                        )
                    }
                }

                // 2. WEEKLY SCREEN TIME & FOCUS CHART
                item {
                    DkSectionHeader(
                        title = "Weekly Screen & Focus Trends",
                        subtitle = "Cyan = Device Screen Time | Mint = Deep Focus Time"
                    )

                    WeeklyChartCard(weeklyData = uiState.weeklyChart)
                }

                // 3. FOCUS-TIME STATISTICS
                item {
                    DkSectionHeader(
                        title = "Discipline Achievements",
                        subtitle = "Continuous focus defense performance"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FocusStatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.HourglassBottom,
                            title = "Deep Work",
                            value = "${uiState.totalFocusMinutesAchieved}m",
                            subtitle = "This week",
                            color = CyberGreen
                        )
                        FocusStatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CheckCircle,
                            title = "Sessions",
                            value = "${uiState.completedSessionsCount}",
                            subtitle = "Completed",
                            color = CyberCyan
                        )
                        FocusStatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.LocalFireDepartment,
                            title = "Streak",
                            value = "${uiState.disciplineStreakDays} Days",
                            subtitle = "Discipline unbroken",
                            color = CyberYellow
                        )
                    }
                }

                // 4. BLOCKED ATTEMPTS BREAKDOWN
                item {
                    DkSectionHeader(
                        title = "Intercepted Distractions",
                        subtitle = "High-risk doomscroll vectors suppressed today"
                    )

                    DkCard(contentPadding = 16.dp) {
                        DistractionBarRow(
                            name = "YouTube Shorts Loops",
                            count = 18,
                            maxCount = 20,
                            color = CyberRed
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DistractionBarRow(
                            name = "Instagram Reels Feed",
                            count = 14,
                            maxCount = 20,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DistractionBarRow(
                            name = "Adult (18+) Web Requests",
                            count = 6,
                            maxCount = 20,
                            color = CyberYellow
                        )
                    }
                }

                // 5. MOST-USED APPLICATIONS
                item {
                    DkSectionHeader(
                        title = "Application Usage Profile",
                        subtitle = "Audited device foreground activity"
                    )
                }

                items(uiState.mostUsedApps) { app ->
                    AppUsageItem(app = app)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatisticsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "TELEMETRY AUDIT",
                style = MaterialTheme.typography.labelSmall,
                color = CyberGreen,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Discipline Analytics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }
        DkStatusBadge(
            isActive = true,
            activeText = "SAMPLE AUDIT",
            customActiveColor = CyberCyan
        )
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    DkCard(
        modifier = modifier,
        contentPadding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
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
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun WeeklyChartCard(weeklyData: List<WeeklyDayStat>) {
    DkCard(contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-Day Usage Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendPill(color = CyberCyan, label = "Screen")
                LegendPill(color = CyberGreen, label = "Focus")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Custom Cyber Canvas Bar Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = 14.dp.toPx()
                val maxHours = 5f
                val stepX = canvasWidth / (weeklyData.size.coerceAtLeast(1))

                // Baseline Grid Line
                drawLine(
                    color = CyberCardBorder,
                    start = Offset(0f, canvasHeight - 20.dp.toPx()),
                    end = Offset(canvasWidth, canvasHeight - 20.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )

                weeklyData.forEachIndexed { index, dayStat ->
                    val centerX = (index * stepX) + (stepX / 2f)

                    val screenBarHeight = ((dayStat.screenHours / maxHours) * (canvasHeight - 40.dp.toPx())).coerceAtLeast(4f)
                    val focusBarHeight = ((dayStat.focusHours / maxHours) * (canvasHeight - 40.dp.toPx())).coerceAtLeast(4f)

                    val baseY = canvasHeight - 22.dp.toPx()

                    // Screen Time Bar (Cyan)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(CyberCyan, CyberCyan.copy(alpha = 0.5f))
                        ),
                        topLeft = Offset(centerX - barWidth - 2.dp.toPx(), baseY - screenBarHeight),
                        size = Size(barWidth, screenBarHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Focus Time Bar (Green)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(CyberGreen, CyberGreen.copy(alpha = 0.5f))
                        ),
                        topLeft = Offset(centerX + 2.dp.toPx(), baseY - focusBarHeight),
                        size = Size(barWidth, focusBarHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        // Day Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weeklyData.forEach { dayStat ->
                Text(
                    text = dayStat.day,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (dayStat.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (dayStat.isToday) CyberGreen else TextGray
                )
            }
        }
    }
}

@Composable
private fun LegendPill(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextGray
        )
    }
}

@Composable
private fun FocusStatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    DkCard(
        modifier = modifier,
        contentPadding = 12.dp
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun DistractionBarRow(
    name: String,
    count: Int,
    maxCount: Int,
    color: Color
) {
    val progress = (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = "$count intercepted",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = CyberCardVariant
        )
    }
}

@Composable
private fun AppUsageItem(app: AppUsageStat) {
    DkCard(contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = app.statusTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (app.isThreat) CyberRed else CyberGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Text(
                text = app.usageTimeFormatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { app.usagePercentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (app.isThreat) CyberRed else CyberCyan,
            trackColor = CyberCardVariant
        )
    }
}
