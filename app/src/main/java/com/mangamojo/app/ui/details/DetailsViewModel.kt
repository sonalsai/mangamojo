package com.mangamojo.app.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangamojo.app.core.UiState
import com.mangamojo.app.core.toAppError
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.ReadingProgress
import com.mangamojo.app.domain.usecase.GetChaptersUseCase
import com.mangamojo.app.domain.usecase.GetMangaDetailsUseCase
import com.mangamojo.app.domain.usecase.ObserveIsFavoriteUseCase
import com.mangamojo.app.domain.usecase.ObserveMangaProgressUseCase
import com.mangamojo.app.domain.usecase.ObserveSettingsUseCase
import com.mangamojo.app.domain.usecase.SetChapterReadStateUseCase
import com.mangamojo.app.domain.usecase.ToggleFavoriteUseCase
import com.mangamojo.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsUiState(
    val details: UiState<MangaDetails> = UiState.Loading,
    val chapters: UiState<List<Chapter>> = UiState.Loading,
    val isFavorite: Boolean = false,
    val chapterProgress: Map<String, ReadingProgress> = emptyMap(),
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMangaDetails: GetMangaDetailsUseCase,
    private val getChapters: GetChaptersUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val setChapterReadState: SetChapterReadStateUseCase,
    observeIsFavorite: ObserveIsFavoriteUseCase,
    observeMangaProgress: ObserveMangaProgressUseCase,
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    private val mangaId: String = checkNotNull(savedStateHandle[Routes.ARG_MANGA_ID])

    private val language: StateFlow<String> = observeSettings()
        .map { it.translatedLanguage }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    private val details = MutableStateFlow<UiState<MangaDetails>>(UiState.Loading)
    private val chapters = MutableStateFlow<UiState<List<Chapter>>>(UiState.Loading)

    val uiState: StateFlow<DetailsUiState> = combine(
        details,
        chapters,
        observeIsFavorite(mangaId),
        observeMangaProgress(mangaId),
    ) { details, chapters, isFavorite, progress ->
        DetailsUiState(
            details = details,
            chapters = chapters,
            isFavorite = isFavorite,
            chapterProgress = progress.associateBy { it.chapterId },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            details.value = UiState.Loading
            details.value = try {
                UiState.Success(getMangaDetails(mangaId))
            } catch (e: Exception) {
                UiState.Error(e.toAppError())
            }
        }
        loadChapters()
    }

    private fun loadChapters() {
        viewModelScope.launch {
            chapters.value = UiState.Loading
            chapters.value = try {
                UiState.Success(getChapters(mangaId, listOf(language.value)))
            } catch (e: Exception) {
                UiState.Error(e.toAppError())
            }
        }
    }

    fun onToggleFavorite() {
        val current = (details.value as? UiState.Success)?.data ?: return
        viewModelScope.launch { toggleFavorite(current) }
    }

    fun onSetChapterReadState(chapter: Chapter, read: Boolean) {
        val current = (details.value as? UiState.Success)?.data ?: return
        viewModelScope.launch { setChapterReadState(current, chapter, read) }
    }
}
