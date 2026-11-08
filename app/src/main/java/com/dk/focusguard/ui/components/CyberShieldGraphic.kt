package com.dk.focusguard.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed

@Composable
fun CyberShieldGraphic(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldPulse"
    )

    val activeColor = if (isActive) CyberGreen else CyberRed
    val secondaryColor = if (isActive) CyberCyan else CyberRed.copy(alpha = 0.6f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height

            // Outer Shield Glow Ring (Only when active)
            if (isActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(activeColor.copy(alpha = pulseAlpha * 0.35f), Color.Transparent),
                        center = Offset(width / 2f, height / 2f),
                        radius = width * 0.55f
                    )
                )
            }

            // Shield Contour Path
            val shieldPath = Path().apply {
                moveTo(width * 0.5f, height * 0.12f)
                cubicTo(
                    width * 0.78f, height * 0.16f,
                    width * 0.90f, height * 0.30f,
                    width * 0.90f, height * 0.54f
                )
                cubicTo(
                    width * 0.90f, height * 0.74f,
                    width * 0.65f, height * 0.88f,
                    width * 0.50f, height * 0.94f
                )
                cubicTo(
                    width * 0.35f, height * 0.88f,
                    width * 0.10f, height * 0.74f,
                    width * 0.10f, height * 0.54f
                )
                cubicTo(
                    width * 0.10f, height * 0.30f,
                    width * 0.22f, height * 0.16f,
                    width * 0.50f, height * 0.12f
                )
                close()
            }

            // Shield Background Fill
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        activeColor.copy(alpha = if (isActive) 0.18f else 0.08f),
                        secondaryColor.copy(alpha = if (isActive) 0.08f else 0.03f)
                    )
                )
            )

            // Shield Outer Border
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(activeColor, secondaryColor)
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Inner Cybernetic Circuit Accents
            drawShieldCircuits(width, height, activeColor, secondaryColor, isActive)
        }
    }
}

private fun DrawScope.drawShieldCircuits(
    width: Float,
    height: Float,
    primaryColor: Color,
    secondaryColor: Color,
    isActive: Boolean
) {
    val strokeWidth = 2.dp.toPx()

    // Center Core (Hexagon/Diamond Node)
    val coreY = height * 0.52f
    val coreRadius = width * 0.14f

    drawCircle(
        color = if (isActive) primaryColor else secondaryColor.copy(alpha = 0.5f),
        radius = coreRadius,
        center = Offset(width * 0.5f, coreY),
        style = Stroke(width = strokeWidth)
    )

    // Inner Core Center Light
    drawCircle(
        color = if (isActive) primaryColor else Color.Gray,
        radius = coreRadius * 0.45f,
        center = Offset(width * 0.5f, coreY)
    )

    // Top vertical feeder line
    drawLine(
        color = primaryColor.copy(alpha = 0.7f),
        start = Offset(width * 0.5f, height * 0.24f),
        end = Offset(width * 0.5f, coreY - coreRadius),
        strokeWidth = strokeWidth
    )

    // Diagonal circuit branches
    drawLine(
        color = secondaryColor.copy(alpha = 0.6f),
        start = Offset(width * 0.30f, height * 0.42f),
        end = Offset(width * 0.5f - coreRadius * 0.7f, coreY - coreRadius * 0.7f),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = secondaryColor.copy(alpha = 0.6f),
        start = Offset(width * 0.70f, height * 0.42f),
        end = Offset(width * 0.5f + coreRadius * 0.7f, coreY - coreRadius * 0.7f),
        strokeWidth = strokeWidth
    )
}

