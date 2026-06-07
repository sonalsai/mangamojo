package com.mangamojo.app.ui.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangamojo.app.core.SOURCE_MANGAKAKALOT
import com.mangamojo.app.core.SOURCE_MANGADEX
import com.mangamojo.app.domain.model.Manga
import com.mangamojo.app.domain.model.SearchSort
import com.mangamojo.app.ui.components.EmptyState
import com.mangamojo.app.ui.components.ErrorState
import com.mangamojo.app.ui.components.LoadingState
import com.mangamojo.app.ui.components.MangaCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMangaClick: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, state.items.size, state.hasMore) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= state.items.size - 4 && state.hasMore) {
                    viewModel.loadMore()
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "MANGAMOJO",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 16.dp),
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search titles, authors, genres...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        SortChips(selected = state.sort, onSelect = viewModel::onSortChange)

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> LoadingState()

                state.error != null && state.items.isEmpty() ->
                    ErrorState(state.error!!.userMessage, onRetry = viewModel::retry)

                !state.hasSearched ->
                    EmptyState(
                        title = "Find your next binge",
                        message = "Search and browse results as poster cards.",
                        icon = Icons.Rounded.Search,
                    )

                state.items.isEmpty() ->
                    EmptyState(title = "No results", message = "Try another title or sort mode.")

                else -> SearchResultsGrid(
                    items = state.items,
                    loadingMore = state.loadingMore,
                    onLoadMore = viewModel::loadMore,
                    onMangaClick = onMangaClick,
                    gridState = gridState,
                )
            }
        }
    }
}

@Composable
private fun SortChips(
    selected: SearchSort,
    onSelect: (SearchSort) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        lazyRowItems(SearchSort.entries.toList(), key = { it.name }) { sort ->
            FilterChip(
                selected = selected == sort,
                onClick = { onSelect(sort) },
                label = { Text(sort.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun SearchResultsGrid(
    items: List<Manga>,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onMangaClick: (String) -> Unit,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Search Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${items.size} titles found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        items(items, key = { it.id }) { manga ->
            MangaCard(
                title = manga.title,
                coverUrl = manga.coverUrl,
                subtitle = manga.metadata,
                rating = manga.year?.toString(),
                badge = if (manga.contentRating.equals("safe", ignoreCase = true)) null else manga.contentRating.prettyLabel(),
                onClick = { onMangaClick(manga.id) },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (loadingMore) {
                    CircularProgressIndicator()
                } else {
                    TextButton(onClick = onLoadMore) { Text("Load More") }
                }
            }
        }
    }
}

private val SearchSort.label: String
    get() = when (this) {
        SearchSort.RELEVANCE -> "Relevance"
        SearchSort.POPULAR -> "Trending"
        SearchSort.LATEST -> "Latest"
        SearchSort.NEWEST -> "Newest"
        SearchSort.RATING -> "Rating"
    }

private val Manga.metadata: String
    get() = listOf(sourceLabel, status.label, contentRating.prettyLabel())
        .filter { it.isNotBlank() }
        .joinToString(" - ")

private val Manga.sourceLabel: String
    get() = when (sourceId) {
        SOURCE_MANGADEX -> "MangaDex"
        SOURCE_MANGAKAKALOT -> "MangaKakalot"
        else -> sourceId.replaceFirstChar { it.uppercase() }
    }

private fun String.prettyLabel(): String =
    replaceFirstChar { it.uppercase() }
