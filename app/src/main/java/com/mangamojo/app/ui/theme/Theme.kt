package com.mangamojo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mangamojo.app.domain.model.ThemeMode
import com.mangamojo.app.domain.model.ThemePalette

private fun MangaMojoPalette.darkColors() = darkColorScheme(
    primary = accent,
    onPrimary = onAccent,
    secondary = accent,
    onSecondary = onAccent,
    background = background,
    onBackground = primaryText,
    surface = surface,
    onSurface = primaryText,
    surfaceVariant = surface,
    onSurfaceVariant = secondaryText,
    outline = secondaryText,
)

private fun MangaMojoPalette.lightColors() = lightColorScheme(
    primary = accent,
    onPrimary = onAccent,
    secondary = accent,
    onSecondary = onAccent,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1A1A1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1C),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
)

/**
 * App theme driven by the user's [ThemeMode] and [ThemePalette] preferences.
 * The default palette is MangaMojo green rather than dynamic color so the
 * reader keeps a consistent MangaMojo identity across devices.
 */
@Composable
fun MangaMojoTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    themePalette: ThemePalette = ThemePalette.Default,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = themePalette.toMangaMojoPalette()
    MaterialTheme(
        colorScheme = if (dark) palette.darkColors() else palette.lightColors(),
        typography = Typography,
        content = content,
    )
}
