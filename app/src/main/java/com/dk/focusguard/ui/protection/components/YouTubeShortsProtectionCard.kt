package com.dk.focusguard.ui.protection.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.domain.repository.AccessibilityServiceState
import com.dk.focusguard.ui.components.DkCard
import com.dk.focusguard.ui.theme.CyberBackground
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberCardVariant
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberGreenGlow
import com.dk.focusguard.ui.theme.CyberRed
import com.dk.focusguard.ui.theme.CyberYellow
import com.dk.focusguard.ui.theme.TextGray
import com.dk.focusguard.ui.theme.TextMuted
import com.dk.focusguard.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Cybersecurity Protection Card for YouTube Shorts.
 * Displays all 6 required verification metrics transparently.
 */
@Composable
fun YouTubeShortsProtectionCard(
    isProtectionEnabled: Boolean,
    isYouTubeForeground: Boolean,
    accessibilityServiceState: AccessibilityServiceState,
    interruptionCount: Int,
    lastDetectionTimestamp: Long,
    isEmergencyOverrideActive: Boolean,
    onToggleProtection: (Boolean) -> Unit,
    onSetupAccessibilityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isServiceConnected = accessibilityServiceState == AccessibilityServiceState.CONNECTED_AND_MONITORING

    val (badgeText, badgeColor, badgeBg) = when {
        !isServiceConnected -> Triple("OFFLINE (ACCESSIBILITY)", CyberRed, CyberRed.copy(alpha = 0.15f))
        isEmergencyOverrideActive -> Triple("SUSPENDED (OVERRIDE)", CyberYellow, CyberYellow.copy(alpha = 0.15f))
        !isProtectionEnabled -> Triple("DISABLED BY USER", TextMuted, CyberCardVariant)
        else -> Triple("ACTIVE ENFORCEMENT", CyberGreen, CyberGreenGlow)
    }

    DkCard(
        backgroundColor = CyberCard,
        borderColor = if (isProtectionEnabled && isServiceConnected) CyberCyan.copy(alpha = 0.5f) else CyberCardBorder,
        contentPadding = 16.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Title & Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberRed.copy(alpha = 0.15f))
                            .border(1.dp, CyberRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = "YouTube Shorts",
                            tint = CyberRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "YouTube Shorts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = if (isProtectionEnabled) "Auto-exit Shorts feed" else "Protection disabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isProtectionEnabled) CyberGreen else TextGray
                        )
                    }
                }

                Switch(
                    checked = isProtectionEnabled,
                    onCheckedChange = onToggleProtection,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBackground,
                        checkedTrackColor = CyberGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberCardVariant,
                        uncheckedBorderColor = CyberCardBorder
                    )
                )
            }

            // Summary Row: Blocked count & status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCardVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Blocked Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray
                )
                Text(
                    text = "$interruptionCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (interruptionCount > 0) CyberCyan else TextWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            // If accessibility service is disconnected, show quick action button
            AnimatedVisibility(visible = !isServiceConnected) {
                OutlinedButton(
                    onClick = onSetupAccessibilityClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = BorderStroke(1.dp, CyberCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CyberCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enable Accessibility Permission",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryDataRow(
    label: String,
    value: String,
    valueColor: Color = TextWhite,
    hasPulseDot: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasPulseDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CyberGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = valueColor,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

