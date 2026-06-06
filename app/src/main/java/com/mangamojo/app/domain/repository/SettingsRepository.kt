package com.mangamojo.app.domain.repository

import com.mangamojo.app.domain.model.AppSettings
import com.mangamojo.app.domain.model.ReadingDirection
import com.mangamojo.app.domain.model.ThemeMode
import com.mangamojo.app.domain.model.ThemePalette
import kotlinx.coroutines.flow.Flow

/** Persisted app preferences, backed by DataStore. */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setThemePalette(palette: ThemePalette)
    suspend fun setReadingDirection(direction: ReadingDirection)
    suspend fun setDataSaver(enabled: Boolean)
    suspend fun setContentRatings(ratings: Set<String>)
    suspend fun setTranslatedLanguage(language: String)
    suspend fun setPauseHistoryInAdultMode(enabled: Boolean)
}
