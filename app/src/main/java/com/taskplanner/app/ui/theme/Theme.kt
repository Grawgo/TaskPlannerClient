package com.taskplanner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val light = lightColorScheme(
    primary = Color(0xFF6856A8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE8F8),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF1C1B22),
    surface = Color.White,
    onSurface = Color(0xFF1C1B22),
    error = Color(0xFFB72D3A),
)

private val dark = darkColorScheme(
    primary = Color(0xFFB8A7F0),
    onPrimary = Color(0xFF15141B),
    primaryContainer = Color(0xFF393346),
    background = Color(0xFF15141B),
    onBackground = Color(0xFFF4F2F8),
    surface = Color(0xFF24232B),
    onSurface = Color(0xFFF4F2F8),
    error = Color(0xFFFFB4BD),
)

@Composable
fun TaskPlannerTheme(darkMode: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkMode) dark else light, typography = Typography(), content = content)
}
