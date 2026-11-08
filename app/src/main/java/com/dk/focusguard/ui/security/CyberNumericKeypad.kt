package com.dk.focusguard.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen

/**
 * Premium cybersecurity numeric keypad with glowing accents.
 */
@Composable
fun CyberNumericKeypad(
    onDigitClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit,
    onBiometricClick: () -> Unit,
    showBiometricButton: Boolean = false,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KeypadButton(text = "1", isEnabled = isEnabled, onClick = { onDigitClick('1') })
            KeypadButton(text = "2", isEnabled = isEnabled, onClick = { onDigitClick('2') })
            KeypadButton(text = "3", isEnabled = isEnabled, onClick = { onDigitClick('3') })
        }

        // Row 2: 4, 5, 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KeypadButton(text = "4", isEnabled = isEnabled, onClick = { onDigitClick('4') })
            KeypadButton(text = "5", isEnabled = isEnabled, onClick = { onDigitClick('5') })
            KeypadButton(text = "6", isEnabled = isEnabled, onClick = { onDigitClick('6') })
        }

        // Row 3: 7, 8, 9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            KeypadButton(text = "7", isEnabled = isEnabled, onClick = { onDigitClick('7') })
            KeypadButton(text = "8", isEnabled = isEnabled, onClick = { onDigitClick('8') })
            KeypadButton(text = "9", isEnabled = isEnabled, onClick = { onDigitClick('9') })
        }

        // Row 4: Biometric/Clear, 0, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBiometricButton) {
                KeypadIconButton(
                    icon = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Login",
                    tint = CyberCyan,
                    isEnabled = isEnabled,
                    onClick = onBiometricClick
                )
            } else {
                KeypadTextActionButton(
                    text = "CLEAR",
                    isEnabled = isEnabled,
                    onClick = onClearClick
                )
            }

            KeypadButton(text = "0", isEnabled = isEnabled, onClick = { onDigitClick('0') })

            KeypadIconButton(
                icon = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                isEnabled = isEnabled,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (isEnabled) CyberCard else CyberCard.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = if (isEnabled) CyberCardBorder else CyberCardBorder.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = CyberGreen),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (isEnabled) CyberCard else CyberCard.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = if (isEnabled) CyberCardBorder else CyberCardBorder.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = tint),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isEnabled) tint else tint.copy(alpha = 0.3f),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun KeypadTextActionButton(
    text: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = CyberCyan),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (isEnabled) CyberCyan else CyberCyan.copy(alpha = 0.3f)
        )
    }
}
