package com.mangamojo.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.mangamojo.app.core.MangaDex
import com.mangamojo.app.domain.model.AppSettings
import com.mangamojo.app.domain.model.ReadingDirection
import com.mangamojo.app.domain.model.ThemeMode
import com.mangamojo.app.domain.model.ThemePalette
import com.mangamojo.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val DIRECTION = stringPreferencesKey("reading_direction")
        val DATA_SAVER = booleanPreferencesKey("data_saver")
        val CONTENT_RATINGS = stringSetPreferencesKey("content_ratings")
        val LANGUAGE = stringPreferencesKey("translated_language")
    }

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.THEME].toEnum(ThemeMode.DARK),
                themePalette = prefs[Keys.THEME_PALETTE].toThemePalette(),
                readingDirection = prefs[Keys.DIRECTION].toEnum(ReadingDirection.VERTICAL),
                dataSaver = prefs[Keys.DATA_SAVER] ?: false,
                contentRatings = prefs[Keys.CONTENT_RATINGS] ?: MangaDex.DEFAULT_CONTENT_RATINGS.toSet(),
                translatedLanguage = prefs[Keys.LANGUAGE] ?: MangaDex.DEFAULT_LANGUAGE,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setThemePalette(palette: ThemePalette) {
        dataStore.edit { it[Keys.THEME_PALETTE] = palette.name }
    }

    override suspend fun setReadingDirection(direction: ReadingDirection) {
        dataStore.edit { it[Keys.DIRECTION] = direction.name }
    }

    override suspend fun setDataSaver(enabled: Boolean) {
        dataStore.edit { it[Keys.DATA_SAVER] = enabled }
    }

    override suspend fun setContentRatings(ratings: Set<String>) {
        dataStore.edit { it[Keys.CONTENT_RATINGS] = ratings }
    }

    override suspend fun setTranslatedLanguage(language: String) {
        dataStore.edit { it[Keys.LANGUAGE] = language }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

private fun String?.toThemePalette(): ThemePalette {
    val palette = this?.let { runCatching { enumValueOf<ThemePalette>(it) }.getOrNull() }
    return when (palette) {
        null, ThemePalette.SHONEN_CRIMSON -> ThemePalette.Default
        else -> palette
    }
}
