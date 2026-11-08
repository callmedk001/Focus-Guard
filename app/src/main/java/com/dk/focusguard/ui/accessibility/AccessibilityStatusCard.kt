package com.dk.focusguard.ui.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.domain.repository.AccessibilityServiceState
import com.dk.focusguard.ui.components.DkCard
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed
import com.dk.focusguard.ui.theme.CyberYellow
import com.dk.focusguard.ui.theme.TextGray
import com.dk.focusguard.ui.theme.TextWhite

/**
 * Compact, clean accessibility status card for Protection screen.
 */
@Composable
fun AccessibilityStatusCard(
    serviceState: AccessibilityServiceState,
    currentForegroundAppName: String?,
    lastEventTimestamp: Long,
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = serviceState == AccessibilityServiceState.CONNECTED_AND_MONITORING

    if (isConnected) {
        DkCard(
            modifier = modifier.fillMaxWidth(),
            contentPadding = 12.dp,
            borderColor = CyberGreen.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CyberGreen)
                    )
                    Column {
                        Text(
                            text = "Accessibility Engine",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Active & Monitoring",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen
                        )
                    }
                }

                TextButton(
                    onClick = onSetupClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        val (title, color) = if (serviceState == AccessibilityServiceState.PERMISSION_NOT_GRANTED) {
            "Accessibility Permission Required" to CyberRed
        } else {
            "Service Standby" to CyberYellow
        }

        DkCard(
            modifier = modifier.fillMaxWidth(),
            backgroundColor = CyberCard,
            borderColor = color.copy(alpha = 0.5f),
            contentPadding = 14.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = "Required for YouTube Shorts and Instagram Reels shields",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }

                Button(
                    onClick = onSetupClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Enable Accessibility",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}


