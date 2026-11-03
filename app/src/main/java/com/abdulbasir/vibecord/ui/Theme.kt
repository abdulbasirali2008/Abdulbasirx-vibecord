package com.abdulbasir.vibecord.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AccentColors = listOf(
    Color(0xFFFA2D48), // red
    Color(0xFF0A84FF), // blue
    Color(0xFF30D158), // green
    Color(0xFFBF5AF2), // purple
    Color(0xFFFF9F0A), // orange
)

val AccentNames = listOf("Red", "Blue", "Green", "Purple", "Orange")

@Composable
fun VibeTheme(dark: Boolean, accent: Color, content: @Composable () -> Unit) {
    val colors = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = Color(0xFF0B0B0D),
            onBackground = Color(0xFFF2F2F7),
            surface = Color(0xFF0B0B0D),
            onSurface = Color(0xFFF2F2F7),
            surfaceVariant = Color(0xFF232326),
            onSurfaceVariant = Color(0xFF9A9AA2),
            surfaceContainer = Color(0xFF161618),
            secondaryContainer = accent.copy(alpha = 0.25f),
            onSecondaryContainer = Color.White,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF111114),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF111114),
            surfaceVariant = Color(0xFFEFEFF4),
            onSurfaceVariant = Color(0xFF6C6C72),
            surfaceContainer = Color(0xFFF5F5F8),
            secondaryContainer = accent.copy(alpha = 0.18f),
            onSecondaryContainer = Color(0xFF111114),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
