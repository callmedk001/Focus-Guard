package com.dk.focusguard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dk.focusguard.ui.theme.CyberBorderGradient
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberGreen

@Composable
fun DkCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CyberCard,
    borderColor: Color = CyberCardBorder,
    borderBrush: Brush? = null,
    isGlowActive: Boolean = false,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isGlowActive) CyberGreen.copy(alpha = 0.8f) else borderColor,
        label = "cardBorderAnimation"
    )

    val effectiveBorder = when {
        isGlowActive && borderBrush != null -> BorderStroke(borderWidth, borderBrush)
        isGlowActive -> BorderStroke(borderWidth, CyberBorderGradient)
        borderBrush != null -> BorderStroke(borderWidth, borderBrush)
        else -> BorderStroke(borderWidth, animatedBorderColor)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = effectiveBorder
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
