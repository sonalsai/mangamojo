package com.mangamojo.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.mangamojo.app.domain.model.ThemePalette

data class MangaMojoPalette(
    val accent: Color,
    val background: Color,
    val surface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val onAccent: Color,
)

val ShonenCrimsonPalette = MangaMojoPalette(
    accent = Color(0xFFE50914),
    background = Color(0xFF0F0F10),
    surface = Color(0xFF1A1A1C),
    primaryText = Color(0xFFFFFFFF),
    secondaryText = Color(0xFF94A3B8),
    onAccent = Color(0xFFFFFFFF),
)

val NeonCyberpunkPalette = MangaMojoPalette(
    accent = Color(0xFFA855F7),
    background = Color(0xFF0B0F19),
    surface = Color(0xFF1E293B),
    primaryText = Color(0xFFFFFFFF),
    secondaryText = Color(0xFF64748B),
    onAccent = Color(0xFF0B0F19),
)

val RetroShonenPalette = MangaMojoPalette(
    accent = Color(0xFFFF6B00),
    background = Color(0xFF141416),
    surface = Color(0xFF23262F),
    primaryText = Color(0xFFFCFCFD),
    secondaryText = Color(0xFF777E90),
    onAccent = Color(0xFF141416),
)

val MysticalDarkSagePalette = MangaMojoPalette(
    accent = Color(0xFF00C896),
    background = Color(0xFF111612),
    surface = Color(0xFF1C241E),
    primaryText = Color(0xFFF3F4F6),
    secondaryText = Color(0xFF9CA3AF),
    onAccent = Color(0xFF07130F),
)

fun ThemePalette.toMangaMojoPalette(): MangaMojoPalette = when (this) {
    ThemePalette.SHONEN_CRIMSON -> ShonenCrimsonPalette
    ThemePalette.NEON_CYBERPUNK -> NeonCyberpunkPalette
    ThemePalette.RETRO_SHONEN -> RetroShonenPalette
    ThemePalette.MYSTICAL_DARK_SAGE -> MysticalDarkSagePalette
}
