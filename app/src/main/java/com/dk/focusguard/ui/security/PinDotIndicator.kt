package com.dk.focusguard.ui.security

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed

/**
 * Masked visual dot indicator for entered PIN digits.
 * Never displays plaintext characters.
 */
@Composable
fun PinDotIndicator(
    enteredLength: Int,
    maxLength: Int = 6,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxLength) {
            val isFilled = i < enteredLength

            val targetColor = when {
                isError -> CyberRed
                isFilled -> CyberGreen
                else -> Color.Transparent
            }

            val targetBorder = when {
                isError -> CyberRed
                isFilled -> CyberGreen
                else -> CyberCardBorder
            }

            val animatedColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(durationMillis = 200),
                label = "dot_color"
            )

            val animatedBorder by animateColorAsState(
                targetValue = targetBorder,
                animationSpec = tween(durationMillis = 200),
                label = "dot_border"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
                    .border(width = 2.dp, color = animatedBorder, shape = CircleShape)
            )
        }
    }
}

