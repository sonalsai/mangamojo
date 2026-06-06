package com.mangamojo.app.ui.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangamojo.app.core.AppError
import com.mangamojo.app.core.MangaDex
import com.mangamojo.app.core.toAppError
import com.mangamojo.app.domain.model.AdultContentMode
import com.mangamojo.app.domain.model.AppSettings
import com.mangamojo.app.domain.model.Manga
import com.mangamojo.app.domain.model.MangaCategory
import com.mangamojo.app.domain.model.SearchQuery
import com.mangamojo.app.domain.model.SearchSort
import com.mangamojo.app.domain.usecase.GetMangaCategoriesUseCase
import com.mangamojo.app.domain.usecase.ObserveSettingsUseCase
import com.mangamojo.app.domain.usecase.SearchMangaUseCase
import com.mangamojo.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryBlock(
    val id: String,
    val title: String,
    val subtitle: String,
    val group: String,
    val section: CategoryBlockSection = CategoryBlockSection.TAG,
    val tagIds: List<String> = emptyList(),
    val contentRatings: List<String> = emptyList(),
    val adult: Boolean = false,
)

enum class CategoryBlockSection {
    ADULT,
    FEATURED,
    TAG,
}

data class CategoriesUiState(
    val categories: List<CategoryBlock> = emptyList(),
    val loading: Boolean = true,
    val error: AppError? = null,
    val adultContentMode: AdultContentMode = AdultContentMode.OFF,
)

data class CategoryMangaUiState(
    val title: String = "Category",
    val subtitle: String = "",
    val adult: Boolean = false,
    val adultBlocked: Boolean = false,
    val items: List<Manga> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: AppError? = null,
    val hasMore: Boolean = false,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getCategories: GetMangaCategoriesUseCase,
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {

    private val settings: StateFlow<AppSettings> =
        observeSettings().stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _state = MutableStateFlow(CategoriesUiState())
    val state: StateFlow<CategoriesUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var mangaDexCategories: List<MangaCategory> = emptyList()

    init {
        loadCategories()
        viewModelScope.launch {
            settings
                .map { it.adultContentMode }
                .distinctUntilChanged()
                .collect { adultMode ->
                    _state.update {
                        it.copy(
                            adultContentMode = adultMode,
                            categories = buildCategoryBlocks(mangaDexCategories, adultMode),
                        )
                    }
                }
        }
    }

    fun retry() = loadCategories()

    private fun loadCategories() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                mangaDexCategories = getCategories()
                _state.update {
                    it.copy(
                        categories = buildCategoryBlocks(
                            mangaDexCategories,
                            settings.value.adultContentMode,
                        ),
                        loading = false,
                        adultContentMode = settings.value.adultContentMode,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toAppError()) }
            }
        }
    }
}

@HiltViewModel
class CategoryMangaViewModel @Inject constructor(
    private val searchManga: SearchMangaUseCase,
    observeSettings: ObserveSettingsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val title: String = savedStateHandle[Routes.ARG_CATEGORY_NAME] ?: "Category"
    private val group: String = savedStateHandle[Routes.ARG_CATEGORY_GROUP] ?: ""
    private val tagIds: List<String> = (savedStateHandle[Routes.ARG_CATEGORY_TAG_IDS] ?: "")
        .splitCsv()
    private val forcedRatings: List<String> = (savedStateHandle[Routes.ARG_CATEGORY_RATINGS] ?: "")
        .splitCsv()
    private val adultCategory = forcedRatings.any { it in MangaDex.ADULT_CONTENT_RATINGS }

    private val settings: StateFlow<AppSettings> =
        observeSettings().stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _state = MutableStateFlow(
        CategoryMangaUiState(
            title = title,
            subtitle = group.toDisplayGroup(),
            adult = adultCategory,
        )
    )
    val state: StateFlow<CategoryMangaUiState> = _state.asStateFlow()

    private var offset = 0
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            settings
                .map { it.contentRatings to it.adultContentMode }
                .distinctUntilChanged()
                .collect { loadCategory(reset = true) }
        }
    }

    fun retry() = loadCategory(reset = true)

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || !current.hasMore) return
        loadCategory(reset = false)
    }

    private fun loadCategory(reset: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val adultMode = settings.value.adultContentMode
            if (adultCategory && adultMode == AdultContentMode.OFF) {
                _state.update {
                    it.copy(
                        adultBlocked = true,
                        loading = false,
                        loadingMore = false,
                        items = emptyList(),
                        error = null,
                    )
                }
                return@launch
            }

            if (reset) {
                offset = 0
                _state.update {
                    it.copy(
                        adultBlocked = false,
                        items = emptyList(),
                        loading = true,
                        loadingMore = false,
                        error = null,
                    )
                }
            } else {
                _state.update { it.copy(loadingMore = true, error = null) }
            }

            try {
                val ratings = forcedRatings.ifEmpty {
                    settings.value.contentRatings.toList().ifEmpty { MangaDex.DEFAULT_CONTENT_RATINGS }
                }
                val result = searchManga(
                    SearchQuery(
                        title = null,
                        offset = offset,
                        sort = SearchSort.POPULAR,
                        contentRatings = ratings,
                        includedTagIds = tagIds,
                    )
                )
                offset = result.nextOffset
                _state.update {
                    it.copy(
                        items = if (reset) result.items else it.items + result.items,
                        loading = false,
                        loadingMore = false,
                        error = null,
                        hasMore = result.hasMore,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        error = e.toAppError(),
                    )
                }
            }
        }
    }
}

private fun buildCategoryBlocks(
    categories: List<MangaCategory>,
    adultMode: AdultContentMode,
): List<CategoryBlock> {
    val byName = categories.associateBy { it.name.normalizedName() }
    val featured = featuredCategoryNames
        .mapNotNull { name -> byName[name] }
        .map { category ->
            CategoryBlock(
                id = "featured-${category.id}",
                title = category.name,
                subtitle = featuredSubtitle(category),
                group = category.group,
                section = CategoryBlockSection.FEATURED,
                tagIds = listOf(category.id),
            )
        }
    val featuredIds = featured.flatMap { it.tagIds }.toSet()
    val tagBlocks = categories
        .filterNot { it.id in featuredIds }
        .map { category ->
            CategoryBlock(
                id = category.id,
                title = category.name,
                subtitle = category.group.toDisplayGroup(),
                group = category.group,
                section = CategoryBlockSection.TAG,
                tagIds = listOf(category.id),
            )
        }

    val normalBlocks = featured + tagBlocks

    if (adultMode == AdultContentMode.OFF) return normalBlocks

    return buildAdultBlocks(categories) + normalBlocks
}

private fun buildAdultBlocks(categories: List<MangaCategory>): List<CategoryBlock> {
    val byName = categories.associateBy { it.name.normalizedName() }
    val adultRatings = MangaDex.ADULT_CONTENT_RATINGS
    val blocks = mutableListOf(
        CategoryBlock(
            id = "adult-all",
            title = "18+ Library",
            subtitle = "Erotica and pornographic titles",
            group = "adult",
            section = CategoryBlockSection.ADULT,
            contentRatings = adultRatings,
            adult = true,
        ),
        CategoryBlock(
            id = "adult-pornographic",
            title = "Pornographic",
            subtitle = "Explicit adult-rated manga",
            group = "adult",
            section = CategoryBlockSection.ADULT,
            contentRatings = listOf("pornographic"),
            adult = true,
        ),
        CategoryBlock(
            id = "adult-erotica",
            title = "Erotica",
            subtitle = "Adult sensual manga",
            group = "adult",
            section = CategoryBlockSection.ADULT,
            contentRatings = listOf("erotica"),
            adult = true,
        ),
    )

    listOf("doujinshi", "oneshot", "harem", "boys love", "girls love")
        .mapNotNull { name -> byName[name] }
        .forEach { category ->
            blocks += CategoryBlock(
                id = "adult-${category.id}",
                title = "${category.name} 18+",
                subtitle = "Adult-rated ${category.name.lowercase()}",
                group = "adult",
                section = CategoryBlockSection.ADULT,
                tagIds = listOf(category.id),
                contentRatings = adultRatings,
                adult = true,
            )
        }

    return blocks
}

private fun String.splitCsv(): List<String> =
    split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }

private fun String.toDisplayGroup(): String = when (this) {
    "genre" -> "Genre"
    "theme" -> "Theme"
    "content" -> "Content"
    "format" -> "Format"
    "adult" -> "18+"
    else -> replaceFirstChar { it.uppercase() }
}

private fun String.normalizedName(): String =
    lowercase()
        .replace("'", "")
        .replace("-", " ")
        .replace("_", " ")
        .trim()

private fun featuredSubtitle(category: MangaCategory): String = when (category.name.normalizedName()) {
    "action" -> "Fast fights and high stakes"
    "romance" -> "Relationships and emotion"
    "fantasy" -> "Magic, worlds and quests"
    "comedy" -> "Light, funny reads"
    "horror" -> "Dark and unsettling stories"
    "drama" -> "Character-driven stories"
    "doujinshi" -> "Fan-made and independent works"
    "harem" -> "Relationship-heavy setups"
    else -> category.group.toDisplayGroup()
}

private val featuredCategoryNames = listOf(
    "action",
    "romance",
    "fantasy",
    "comedy",
    "horror",
    "drama",
    "doujinshi",
    "harem",
)
