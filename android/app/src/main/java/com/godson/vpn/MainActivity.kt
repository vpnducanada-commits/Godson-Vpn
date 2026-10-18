package com.godson.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.godson.vpn.data.ThemeMode
import com.godson.vpn.data.UiSettings
import com.godson.vpn.navigation.GodsonApp
import com.godson.vpn.ui.theme.GodsonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GodsonApplication).container

        setContent {
            val settings by container.settingsRepository.uiSettings
                .collectAsState(initial = UiSettings())

            GodsonTheme(
                themeMode = settings.themeMode,
                reduceAnimations = settings.reduceAnimations
            ) {
                GodsonApp(container = container, settings = settings)
            }
        }
    }
}
