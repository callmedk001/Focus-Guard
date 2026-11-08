package com.dk.focusguard.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberRed

/**
 * Emergency defense override dialog allowing time-limited suspension of protection.
 * Requires PIN authentication and logs the event to authentic local audit logs.
 */
@Composable
fun EmergencyOverrideDialog(
    onDismissRequest: () -> Unit,
    onConfirmOverride: (durationMinutes: Int) -> Unit
) {
    var selectedDurationMinutes by remember { mutableIntStateOf(15) }
    val durationOptions = listOf(5, 15, 30, 60)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = CyberCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Threat Warning",
                    tint = CyberRed,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "EMERGENCY OVERRIDE",
                    color = CyberRed,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Temporarily suspend all protection filters. This action requires PIN verification and will be recorded in local security audit logs.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Text(
                    text = "SELECT OVERRIDE DURATION",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Duration Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durationOptions.forEach { minutes ->
                        val isSelected = selectedDurationMinutes == minutes
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberRed.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CyberRed else CyberCardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedDurationMinutes = minutes }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${minutes}m",
                                color = if (isSelected) CyberRed else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberRed.copy(alpha = 0.1f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "NOTE: Protection will automatically resume when the timer expires.",
                        color = CyberRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmOverride(selectedDurationMinutes) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Authenticate & Override",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    )
}

