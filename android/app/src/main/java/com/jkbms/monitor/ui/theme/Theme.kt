package com.jkbms.monitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BmsMonitorTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        secondary = Color(0xFF81C784),
        surface = Color(0xFF1E1E1E),
        background = Color(0xFF121212),
        onSurface = Color.White,
        onBackground = Color.White,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
