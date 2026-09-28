package com.rvh.video.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.rvh.video.data.local.AppSettings

private val RvhDarkColorScheme = darkColorScheme(
    background = Surface0, surface = Surface1, surfaceVariant = Surface2,
    primary = AccentTeal, onPrimary = Surface0, secondary = AccentTealDim,
    error = DangerRed, onBackground = TextPrimary, onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
)

private val RvhLightColorScheme = lightColorScheme(
    background = androidx.compose.ui.graphics.Color(0xFFF5F7F8),
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8ECEF),
    primary = androidx.compose.ui.graphics.Color(0xFF087E76),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = androidx.compose.ui.graphics.Color(0xFF176D68),
    error = androidx.compose.ui.graphics.Color(0xFFB3261E),
    onBackground = androidx.compose.ui.graphics.Color(0xFF17191C),
    onSurface = androidx.compose.ui.graphics.Color(0xFF17191C),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF4F565D),
)

@Composable
fun RvhVideoTheme(
    themeMode: String = AppSettings.THEME_SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        AppSettings.THEME_DARK -> true
        AppSettings.THEME_LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) RvhDarkColorScheme else RvhLightColorScheme,
        typography = AppTypography,
        content = content
    )
}
