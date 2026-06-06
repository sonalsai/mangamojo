package com.mangamojo.app.domain.usecase

import com.mangamojo.app.domain.model.AppSettings
import com.mangamojo.app.domain.model.ReadingDirection
import com.mangamojo.app.domain.model.ThemeMode
import com.mangamojo.app.domain.model.ThemePalette
import com.mangamojo.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(private val repo: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = repo.settings
}

class SetThemeModeUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repo.setThemeMode(mode)
}

class SetThemePaletteUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(palette: ThemePalette) = repo.setThemePalette(palette)
}

class SetReadingDirectionUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(direction: ReadingDirection) = repo.setReadingDirection(direction)
}

class SetDataSaverUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repo.setDataSaver(enabled)
}

class SetContentRatingsUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(ratings: Set<String>) = repo.setContentRatings(ratings)
}

class SetTranslatedLanguageUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(language: String) = repo.setTranslatedLanguage(language)
}

class SetPauseHistoryInAdultModeUseCase @Inject constructor(private val repo: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repo.setPauseHistoryInAdultMode(enabled)
}
