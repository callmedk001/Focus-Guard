package com.dk.focusguard.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Futuristic Cybersecurity Palette
val CyberBackground = Color(0xFF080B14)
val CyberCard = Color(0xFF111827)
val CyberCardVariant = Color(0xFF162032)
val CyberCardBorder = Color(0xFF1F293D)

// Primary & Secondary Accents
val CyberGreen = Color(0xFF00FF9C)
val CyberGreenGlow = Color(0x3300FF9C)
val CyberGreenDark = Color(0xFF008F56)

val CyberCyan = Color(0xFF00B8FF)
val CyberCyanGlow = Color(0x3300B8FF)
val CyberCyanDark = Color(0xFF006FA3)

// Alert & Warning
val CyberRed = Color(0xFFFF3366)
val CyberRedGlow = Color(0x33FF3366)
val CyberYellow = Color(0xFFFFB800)

// Text & Neutral Content
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)

// Gradients
val CyberNeonGradient = Brush.horizontalGradient(
    colors = listOf(CyberGreen, CyberCyan)
)

val CyberBorderGradient = Brush.horizontalGradient(
    colors = listOf(CyberGreen.copy(alpha = 0.5f), CyberCyan.copy(alpha = 0.5f))
)

val CyberActiveGlowGradient = Brush.radialGradient(
    colors = listOf(CyberGreen.copy(alpha = 0.25f), Color.Transparent)
)

// Legacy alias mappings for seamless backwards compatibility
val GuardBackground = CyberBackground
val GuardSurface = CyberCard
val GuardSurfaceVariant = CyberCardVariant
val GuardCardBorder = CyberCardBorder
val GuardBlue = CyberCyan
val GuardShieldGreen = CyberGreen
val GuardAlertRed = CyberRed
val TextPrimary = TextWhite
val TextSecondary = TextGray
