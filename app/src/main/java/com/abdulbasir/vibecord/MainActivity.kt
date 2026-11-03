package com.abdulbasir.vibecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdulbasir.vibecord.ui.AccentColors
import com.abdulbasir.vibecord.ui.VibeApp
import com.abdulbasir.vibecord.ui.VibeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val accentIndex by viewModel.accentIndex.collectAsStateWithLifecycle()
            val dark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            VibeTheme(
                dark = dark,
                accent = AccentColors[accentIndex.coerceIn(AccentColors.indices)],
            ) {
                VibeApp(viewModel)
            }
        }
    }
}
