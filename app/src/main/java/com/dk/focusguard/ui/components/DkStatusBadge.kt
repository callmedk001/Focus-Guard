package com.dk.focusguard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed

@Composable
fun DkStatusBadge(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeText: String = "ACTIVE",
    inactiveText: String = "INACTIVE",
    customActiveColor: Color = CyberGreen,
    customInactiveColor: Color = CyberRed
) {
    val targetColor = if (isActive) customActiveColor else customInactiveColor
    val animatedColor by animateColorAsState(targetValue = targetColor, label = "badgeColor")

    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = animatedColor.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.small
            )
            .background(
                color = animatedColor.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color = animatedColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isActive) activeText else inactiveText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            )
        }
    }
}
