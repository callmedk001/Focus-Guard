package com.dk.focusguard.ui.interruption

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.MainActivity
import com.dk.focusguard.services.accessibility.TargetAppRegistry
import com.dk.focusguard.ui.components.DkCard
import com.dk.focusguard.ui.theme.CyberBackground
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardVariant
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed
import com.dk.focusguard.ui.theme.CyberRedGlow
import com.dk.focusguard.ui.theme.DKFocusGuardTheme
import com.dk.focusguard.ui.theme.TextGray
import com.dk.focusguard.ui.theme.TextMuted
import com.dk.focusguard.ui.theme.TextWhite

/**
 * Full-screen Cyber Interruption Activity displayed when short-form feeds
 * (YouTube Shorts or Instagram Reels) are intercepted.
 *
 * Guarantees:
 * - SingleTop / SingleTask launch mode prevents multiple stacked instances.
 * - Provides immediate safe escape path back to standard home browsing.
 * - Fully honors the DK Focus Guard cybersecurity visual identity.
 */
class ShortsInterruptionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_TYPE = "extra_target_type"
        const val TYPE_YOUTUBE_SHORTS = "YOUTUBE_SHORTS"
        const val TYPE_INSTAGRAM_REELS = "INSTAGRAM_REELS"

        fun createIntent(context: Context): Intent {
            return createYouTubeIntent(context)
        }

        fun createYouTubeIntent(context: Context): Intent {
            return Intent(context, ShortsInterruptionActivity::class.java).apply {
                putExtra(EXTRA_TARGET_TYPE, TYPE_YOUTUBE_SHORTS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        fun createInstagramIntent(context: Context): Intent {
            return Intent(context, ShortsInterruptionActivity::class.java).apply {
                putExtra(EXTRA_TARGET_TYPE, TYPE_INSTAGRAM_REELS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetType = intent.getStringExtra(EXTRA_TARGET_TYPE) ?: TYPE_YOUTUBE_SHORTS

        setContent {
            DKFocusGuardTheme {
                ShortsInterruptionContent(
                    targetType = targetType,
                    onReturnToSafeApp = {
                        if (targetType == TYPE_INSTAGRAM_REELS) {
                            returnToInstagramHome()
                        } else {
                            returnToYouTubeHome()
                        }
                        finish()
                    },
                    onOpenFocusGuard = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }

    private fun returnToYouTubeHome() {
        val launchIntent = packageManager.getLaunchIntentForPackage(TargetAppRegistry.PACKAGE_YOUTUBE)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
    }

    private fun returnToInstagramHome() {
        val launchIntent = packageManager.getLaunchIntentForPackage(TargetAppRegistry.PACKAGE_INSTAGRAM)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
    }
}

@Composable
fun ShortsInterruptionContent(
    targetType: String,
    onReturnToSafeApp: () -> Unit,
    onOpenFocusGuard: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInstagram = targetType == ShortsInterruptionActivity.TYPE_INSTAGRAM_REELS

    val titleText = if (isInstagram) "REELS SESSION INTERRUPTED" else "SHORTS SESSION INTERRUPTED"
    val descriptionText = if (isInstagram) {
        "Your focus matters. Instagram Reels access has been interrupted by DK FOCUS GUARD."
    } else {
        "Your focus matters. YouTube Shorts access has been interrupted by DK FOCUS GUARD."
    }
    val targetAppLabel = if (isInstagram) "Instagram (com.instagram.android)" else "YouTube (com.google.android.youtube)"
    val restrictedAreaLabel = if (isInstagram) "Short-form Vertical Feed (Reels)" else "Short-form Vertical Feed (Shorts)"
    val safeButtonLabel = if (isInstagram) "RETURN TO INSTAGRAM (SAFE)" else "RETURN TO YOUTUBE (SAFE)"

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CyberBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // Threat Icon Indicator with Glowing Perimeter
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(CyberRedGlow)
                        .border(2.dp, CyberRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Alert",
                        tint = CyberRed,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "SECURITY PROTOCOL ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // HUD Telemetry Card
            DkCard(
                backgroundColor = CyberCard,
                borderColor = CyberRed.copy(alpha = 0.4f),
                contentPadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DEFENSE INTERVENTION TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    InterruptionTelemetryRow(label = "Target Application", value = targetAppLabel)
                    InterruptionTelemetryRow(label = "Restricted Area", value = restrictedAreaLabel)
                    InterruptionTelemetryRow(label = "Enforcement Action", value = "Accessibility Back Navigation")
                    InterruptionTelemetryRow(label = "Guard Status", value = "Interception Verified")
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Safe Return Button
                Button(
                    onClick = onReturnToSafeApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = CyberBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = safeButtonLabel,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Secondary Open Focus Guard Button
                OutlinedButton(
                    onClick = onOpenFocusGuard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyberGreen
                    ),
                    border = BorderStroke(1.dp, CyberGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = CyberGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FOCUS GUARD DASHBOARD",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Dismiss Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextMuted
                    ),
                    border = BorderStroke(1.dp, CyberCardVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "DISMISS",
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )
                }
            }
        }
    }
}

@Composable
private fun InterruptionTelemetryRow(
    label: String,
    value: String
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
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextWhite,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
