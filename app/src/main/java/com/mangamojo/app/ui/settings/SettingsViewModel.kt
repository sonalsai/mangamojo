package com.mangamojo.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangamojo.app.core.MangaDex
import com.mangamojo.app.domain.model.AdultContentMode
import com.mangamojo.app.domain.model.AppSettings
import com.mangamojo.app.domain.model.ReadingDirection
import com.mangamojo.app.domain.model.ThemeMode
import com.mangamojo.app.domain.model.ThemePalette
import com.mangamojo.app.domain.usecase.ClearCacheUseCase
import com.mangamojo.app.domain.usecase.ClearFavoritesUseCase
import com.mangamojo.app.domain.usecase.ClearHistoryUseCase
import com.mangamojo.app.domain.usecase.ObserveCachedCountUseCase
import com.mangamojo.app.domain.usecase.ObserveSettingsUseCase
import com.mangamojo.app.domain.usecase.SetContentRatingsUseCase
import com.mangamojo.app.domain.usecase.SetDataSaverUseCase
import com.mangamojo.app.domain.usecase.SetPauseHistoryInAdultModeUseCase
import com.mangamojo.app.domain.usecase.SetReadingDirectionUseCase
import com.mangamojo.app.domain.usecase.SetThemeModeUseCase
import com.mangamojo.app.domain.usecase.SetThemePaletteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    observeCachedCount: ObserveCachedCountUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setThemePalette: SetThemePaletteUseCase,
    private val setReadingDirection: SetReadingDirectionUseCase,
    private val setDataSaver: SetDataSaverUseCase,
    private val setContentRatings: SetContentRatingsUseCase,
    private val setPauseHistoryInAdultMode: SetPauseHistoryInAdultModeUseCase,
    private val clearCache: ClearCacheUseCase,
    private val clearHistory: ClearHistoryUseCase,
    private val clearFavorites: ClearFavoritesUseCase,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val cachedCount: StateFlow<Int> =
        observeCachedCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onThemeModeChange(mode: ThemeMode) = viewModelScope.launch { setThemeMode(mode) }

    fun onThemePaletteChange(palette: ThemePalette) =
        viewModelScope.launch { setThemePalette(palette) }

    fun onReadingDirectionChange(direction: ReadingDirection) =
        viewModelScope.launch { setReadingDirection(direction) }

    fun onDataSaverChange(enabled: Boolean) = viewModelScope.launch { setDataSaver(enabled) }

    fun onAdultContentModeChange(mode: AdultContentMode) {
        val ratings = when (mode) {
            AdultContentMode.OFF -> MangaDex.DEFAULT_CONTENT_RATINGS
            AdultContentMode.MIXED -> MangaDex.MIXED_CONTENT_RATINGS
            AdultContentMode.ADULT_ONLY -> MangaDex.ADULT_CONTENT_RATINGS
        }.toSet()
        viewModelScope.launch { setContentRatings(ratings) }
    }

    fun onPauseHistoryInAdultModeChange(enabled: Boolean) =
        viewModelScope.launch { setPauseHistoryInAdultMode(enabled) }

    fun onClearCache() = viewModelScope.launch { clearCache() }
    fun onClearHistory() = viewModelScope.launch { clearHistory() }
    fun onClearFavorites() = viewModelScope.launch { clearFavorites() }
}
