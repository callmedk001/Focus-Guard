package com.dk.focusguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.dk.focusguard.ui.FocusGuardApp
import com.dk.focusguard.ui.theme.DKFocusGuardTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as FocusGuardApplication

        setContent {
            val userSettings by app.userSettingsRepository.getUserSettings().collectAsState(initial = null)
            val isDarkTheme = userSettings?.darkThemeEnabled ?: isSystemInDarkTheme()

            DKFocusGuardTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FocusGuardApp(app = app)
                }
            }
        }
    }
}

