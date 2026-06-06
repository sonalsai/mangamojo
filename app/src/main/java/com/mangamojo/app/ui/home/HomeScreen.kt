package com.mangamojo.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangamojo.app.domain.model.AdultContentMode
import com.mangamojo.app.domain.model.Favorite
import com.mangamojo.app.domain.model.HistoryEntry
import com.mangamojo.app.domain.model.Manga
import com.mangamojo.app.ui.components.CoverImage
import com.mangamojo.app.ui.components.EmptyState
import com.mangamojo.app.ui.components.ErrorState
import com.mangamojo.app.ui.components.LoadingState
import com.mangamojo.app.ui.components.ProgressOverlayCard
import com.mangamojo.app.ui.components.RailCard
import com.mangamojo.app.ui.components.SectionHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMangaClick: (String) -> Unit,
    onResume: (mangaId: String, chapterId: String) -> Unit,
    onSearch: () -> Unit,
    onSeeFavorites: () -> Unit,
    onSeeHistory: () -> Unit,
    onSeeCategories: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val discover = state.discover
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            (layout.visibleItemsInfo.lastOrNull()?.index ?: 0) to layout.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                if (total > 0 && lastVisible >= total - 2) viewModel.loadMore()
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MANGAMOJO",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (state.adultContentMode == AdultContentMode.ADULT_ONLY) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "18+",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.adultContentMode != AdultContentMode.OFF) {
                item {
                    AdultModeIndicator(mode = state.adultContentMode)
                }
            }

            item {
                WeeklyPopularSection(
                    weeklyPopular = state.weeklyPopular,
                    onRetry = viewModel::retryWeeklyPopular,
                    onMangaClick = onMangaClick,
                )
            }

            if (state.history.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Continue Reading",
                        actionLabel = "View All",
                        onAction = onSeeHistory,
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.history.take(12), key = { it.mangaId }) { entry ->
                            ProgressOverlayCard(
                                title = entry.title,
                                coverUrl = entry.coverUrl,
                                progress = entry.progressFraction,
                                subtitle = entry.progressLabel,
                                onClick = { onResume(entry.mangaId, entry.chapterId) },
                            )
                        }
                    }
                }
            }

            if (state.favorites.isNotEmpty()) {
                item {
                    LibraryRail(
                        favorites = state.favorites,
                        onMangaClick = onMangaClick,
                        onSeeFavorites = onSeeFavorites,
                    )
                }
            }

            item {
                DiscoverTabs(selected = discover.tab, onSelect = viewModel::selectTab)
            }

            item {
                DiscoverRail(
                    discover = discover,
                    onMangaClick = onMangaClick,
                    onRetry = viewModel::retry,
                    onViewAll = onSeeCategories,
                )
            }

            if (discover.items.size > 10) {
                item {
                    SectionHeader(title = "More to Explore", actionLabel = "View All", onAction = onSeeCategories)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(discover.items.drop(10).take(16), key = { "${it.sourceId}:${it.id}" }) { manga ->
                            RailCard(
                                title = manga.title,
                                coverUrl = manga.coverUrl,
                                subtitle = manga.metadata,
                                onClick = { onMangaClick(manga.id) },
                            )
                        }
                        if (discover.loadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.size(width = 80.dp, height = 180.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator() }
                            }
                        }
                    }
                }
            }

            item {
                CategoryPreviewSection(onViewAll = onSeeCategories)
            }
        }
    }
}

@Composable
private fun AdultModeIndicator(mode: AdultContentMode) {
    val message = when (mode) {
        AdultContentMode.MIXED -> "Mixed: adult and normal titles"
        AdultContentMode.ADULT_ONLY -> "MangaMojo18+ space"
        AdultContentMode.OFF -> return
    }
    val accentColor = if (mode == AdultContentMode.ADULT_ONLY) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    Surface(
        color = accentColor.copy(alpha = 0.12f),
        contentColor = accentColor,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun WeeklyPopularSection(
    weeklyPopular: WeeklyPopularState,
    onRetry: () -> Unit,
    onMangaClick: (String) -> Unit,
) {
    SectionHeader(title = "Popular This Week")
    val items = weeklyPopular.items.take(5)
    when {
        weeklyPopular.loading && weeklyPopular.items.isEmpty() -> Box(Modifier.fillMaxWidth().height(260.dp)) {
            LoadingState()
        }

        weeklyPopular.error != null && weeklyPopular.items.isEmpty() -> Box(Modifier.fillMaxWidth().height(260.dp)) {
            ErrorState(weeklyPopular.error.userMessage, onRetry = onRetry)
        }

        weeklyPopular.items.isEmpty() -> Box(Modifier.fillMaxWidth().height(180.dp)) {
            EmptyState("Nothing popular yet", "No weekly popular titles are available right now.")
        }

        else -> {
            val pagerState = rememberPagerState(pageCount = { items.size })
            LaunchedEffect(items, pagerState) {
                if (items.size > 1) {
                    while (true) {
                        delay(3_500)
                        pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                ) { page ->
                    val manga = items[page]
                    WeeklyPopularSlide(
                        rank = page + 1,
                        manga = manga,
                        onClick = { onMangaClick(manga.id) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, _ ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(width = if (selected) 18.dp else 7.dp, height = 7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyPopularSlide(
    rank: Int,
    manga: Manga,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        CoverImage(
            url = manga.coverUrl,
            contentDescription = manga.title,
            cornerRadius = 12,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                        0.42f to MaterialTheme.colorScheme.background.copy(alpha = 0.18f),
                        1f to MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    )
                ),
        )
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(7.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = manga.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeeklyMetadataChip(manga.status.label)
                WeeklyMetadataChip(manga.contentRating.prettyLabel())
            }
        }
    }
}

@Composable
private fun WeeklyMetadataChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LibraryRail(
    favorites: List<Favorite>,
    onMangaClick: (String) -> Unit,
    onSeeFavorites: () -> Unit,
) {
    SectionHeader(title = "My Library", actionLabel = "View All", onAction = onSeeFavorites)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(favorites.take(12), key = { it.mangaId }) { favorite ->
            RailCard(
                title = favorite.title,
                coverUrl = favorite.coverUrl,
                subtitle = favorite.status.label,
                onClick = { onMangaClick(favorite.mangaId) },
            )
        }
    }
}

@Composable
private fun DiscoverTabs(
    selected: DiscoverTab,
    onSelect: (DiscoverTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DiscoverTab.entries.forEach { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                label = { Text(if (tab == DiscoverTab.POPULAR) "Popular Manga" else "Latest Updates") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun DiscoverRail(
    discover: DiscoverState,
    onMangaClick: (String) -> Unit,
    onRetry: () -> Unit,
    onViewAll: () -> Unit,
) {
    val title = if (discover.tab == DiscoverTab.POPULAR) "Popular Manga" else "Latest Updates"
    SectionHeader(title = title, actionLabel = "View All", onAction = onViewAll)
    when {
        discover.loading && discover.items.isEmpty() -> Box(Modifier.fillMaxWidth().height(220.dp)) {
            LoadingState()
        }

        discover.error != null && discover.items.isEmpty() -> Box(Modifier.fillMaxWidth().height(220.dp)) {
            ErrorState(discover.error.userMessage, onRetry = onRetry)
        }

        discover.items.isEmpty() -> Box(Modifier.fillMaxWidth().height(180.dp)) {
            EmptyState("Nothing here", "No titles to show in this rail right now.")
        }

        else -> LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(discover.items.take(18), key = { "${it.sourceId}:${it.id}" }) { manga ->
                RailCard(
                    title = manga.title,
                    coverUrl = manga.coverUrl,
                    subtitle = manga.metadata,
                    badge = if (discover.tab == DiscoverTab.LATEST) "NEW" else null,
                    onClick = { onMangaClick(manga.id) },
                )
            }
        }
    }
}

@Composable
private fun CategoryPreviewSection(onViewAll: () -> Unit) {
    SectionHeader(title = "Categories", actionLabel = "View All", onAction = onViewAll)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(homeCategories, key = { it.title }) { category ->
            val accent = if (category.adult) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(width = 136.dp, height = 112.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.surface,
                            )
                        )
                    )
                    .clickable(onClick = onViewAll)
                    .padding(12.dp),
            ) {
                Text(
                    text = category.group,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.TopStart),
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = category.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val Manga.metadata: String
    get() = listOf(status.label, contentRating.prettyLabel())
        .filter { it.isNotBlank() }
        .joinToString(" - ")

private val HistoryEntry.progressLabel: String
    get() {
        val remaining = (total - page - 1).coerceAtLeast(0)
        return if (total > 0) "$chapterLabel - $remaining pages left" else chapterLabel
    }

private fun String.prettyLabel(): String =
    replaceFirstChar { it.uppercase() }

private data class HomeCategoryPreview(
    val title: String,
    val subtitle: String,
    val group: String,
    val adult: Boolean = false,
)

private val homeCategories = listOf(
    HomeCategoryPreview("Action", "Popular genre", "Genre"),
    HomeCategoryPreview("Romance", "Relationship stories", "Genre"),
    HomeCategoryPreview("Fantasy", "Magic and worlds", "Genre"),
    HomeCategoryPreview("Doujinshi", "Fan-made works", "Format"),
    HomeCategoryPreview("18+ Library", "Adult collections", "18+", adult = true),
)
