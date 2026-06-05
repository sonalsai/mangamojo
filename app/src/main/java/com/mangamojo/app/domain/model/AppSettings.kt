package com.mangamojo.app.domain.model

import com.mangamojo.app.core.MangaDex

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ThemePalette {
    SHONEN_CRIMSON,
    NEON_CYBERPUNK,
    RETRO_SHONEN,
    MYSTICAL_DARK_SAGE,
    ;

    companion object {
        val Default: ThemePalette = MYSTICAL_DARK_SAGE
    }
}

/**
 * Reading direction. Phase 1 ships VERTICAL (webtoon-style); LTR/RTL are
 * placeholders wired through Settings for Phase 2 paged modes.
 */
enum class ReadingDirection { VERTICAL, LTR, RTL }

enum class AdultContentMode {
    OFF,
    MIXED,
    ADULT_ONLY,
}

/** All user preferences, surfaced as a single immutable snapshot. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val themePalette: ThemePalette = ThemePalette.Default,
    val readingDirection: ReadingDirection = ReadingDirection.VERTICAL,
    val dataSaver: Boolean = false,
    val contentRatings: Set<String> = MangaDex.DEFAULT_CONTENT_RATINGS.toSet(),
    val translatedLanguage: String = MangaDex.DEFAULT_LANGUAGE,
) {
    val adultContentMode: AdultContentMode
        get() {
            val adultRatings = MangaDex.ADULT_CONTENT_RATINGS.toSet()
            val hasAdult = contentRatings.any { it in adultRatings }
            if (!hasAdult) return AdultContentMode.OFF
            val hasGeneral = contentRatings.any { it in MangaDex.DEFAULT_CONTENT_RATINGS }
            return if (hasGeneral) AdultContentMode.MIXED else AdultContentMode.ADULT_ONLY
        }
}
