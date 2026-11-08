package com.dk.focusguard.ui.security

import android.app.Activity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dk.focusguard.ui.theme.CyberBackground
import com.dk.focusguard.ui.theme.CyberCard
import com.dk.focusguard.ui.theme.CyberCardBorder
import com.dk.focusguard.ui.theme.CyberCyan
import com.dk.focusguard.ui.theme.CyberGreen
import com.dk.focusguard.ui.theme.CyberRed

/**
 * Full-screen cybersecurity authentication interface.
 * Implements PIN setup, confirmation, verification unlock, and change PIN workflows.
 */
@Composable
fun AuthenticationScreen(
    viewModel: AuthenticationViewModel,
    onAuthenticationSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onAuthenticationSuccess()
        }
    }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Shield Icon & Mode Information
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CyberCard)
                        .border(
                            width = 2.dp,
                            color = if (uiState.isLockedOut || uiState.isError) CyberRed else CyberGreen,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isLockedOut) Icons.Default.Lock else Icons.Default.Shield,
                        contentDescription = "Security Shield",
                        tint = if (uiState.isLockedOut || uiState.isError) CyberRed else CyberGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val (title, subtitle) = when (uiState.authMode) {
                    AuthMode.SETUP -> Pair("CREATE MASTER PIN", "Choose a 4–6 digit security PIN to protect guard controls")
                    AuthMode.CONFIRM -> Pair("CONFIRM MASTER PIN", "Re-enter your PIN to verify")
                    AuthMode.UNLOCK -> Pair("SECURITY VERIFICATION", uiState.targetActionDescription)
                    AuthMode.CHANGE_CURRENT -> Pair("VERIFY IDENTITY", "Enter your current PIN to authorize changes")
                    AuthMode.CHANGE_NEW -> Pair("NEW MASTER PIN", "Choose your replacement 4–6 digit PIN")
                    AuthMode.CHANGE_CONFIRM -> Pair("CONFIRM NEW PIN", "Re-enter your new PIN to verify")
                }

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Lockout Banner
                if (uiState.isLockedOut) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberRed.copy(alpha = 0.15f))
                            .border(1.dp, CyberRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Locked Out",
                                tint = CyberRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "LOCKED OUT: Please wait ${uiState.remainingLockoutSeconds}s",
                                color = CyberRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (uiState.isError && uiState.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberRed.copy(alpha = 0.15f))
                            .border(1.dp, CyberRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Authentication failed",
                            color = CyberRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Masked Dot Indicator
                PinDotIndicator(
                    enteredLength = uiState.enteredPin.length,
                    maxLength = 6,
                    isError = uiState.isError || uiState.isLockedOut
                )
            }

            // Numeric Keypad & Submit Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Continue / Confirm Button (enabled for 4-6 digits)
                if (uiState.enteredPin.length in 4..6 && !uiState.isLockedOut) {
                    Button(
                        onClick = { viewModel.onContinueOrConfirmClicked() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        val actionButtonLabel = when (uiState.authMode) {
                            AuthMode.SETUP, AuthMode.CHANGE_NEW -> "Continue"
                            AuthMode.CONFIRM, AuthMode.CHANGE_CONFIRM -> "Confirm PIN"
                            AuthMode.UNLOCK, AuthMode.CHANGE_CURRENT -> "Authorize"
                        }
                        Text(
                            text = actionButtonLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(64.dp))
                }

                CyberNumericKeypad(
                    onDigitClick = { viewModel.onDigitClicked(it) },
                    onDeleteClick = { viewModel.onDeleteClicked() },
                    onClearClick = { viewModel.onClearClicked() },
                    onBiometricClick = {
                        activity?.let { viewModel.triggerBiometricAuthentication(it) }
                    },
                    showBiometricButton = uiState.isBiometricEnabled &&
                            uiState.isBiometricAvailable &&
                            uiState.authMode == AuthMode.UNLOCK,
                    isEnabled = !uiState.isLockedOut
                )
            }
        }
    }
}

