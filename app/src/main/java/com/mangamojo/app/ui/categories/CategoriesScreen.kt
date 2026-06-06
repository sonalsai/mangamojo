package com.mangamojo.app.ui.categories

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangamojo.app.domain.model.Manga
import com.mangamojo.app.ui.components.EmptyState
import com.mangamojo.app.ui.components.ErrorState
import com.mangamojo.app.ui.components.LoadingState
import com.mangamojo.app.ui.components.MangaCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onCategoryClick: (CategoryBlock) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Categories",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        when {
            state.loading -> LoadingState()
            state.error != null -> ErrorState(state.error!!.userMessage, onRetry = viewModel::retry)
            state.categories.isEmpty() -> EmptyState("No categories", "MangaDex did not return category tags.")
            else -> CategoryBlockGrid(
                state = state,
                onCategoryClick = onCategoryClick,
            )
        }
    }
}

@Composable
private fun CategoryBlockGrid(
    state: CategoriesUiState,
    onCategoryClick: (CategoryBlock) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredBlocks = state.categories.filter {
        query.isBlank() || it.matchesQuery(query)
    }
    val adultBlocks = filteredBlocks.filter { it.section == CategoryBlockSection.ADULT }
    val featuredBlocks = filteredBlocks.filter { it.section == CategoryBlockSection.FEATURED }
    val tagBlocks = filteredBlocks.filter { it.section == CategoryBlockSection.TAG }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CategorySearchRow(
                query = query,
                onQueryChange = { query = it },
                count = filteredBlocks.size,
            )
        }

        if (filteredBlocks.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("No matching categories", "Try a shorter category or tag name.")
            }
        }

        if (adultBlocks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategorySectionHeader(
                    title = "Adult Collections",
                    subtitle = "18+ browsing based on MangaDex ratings and tags",
                )
            }
            items(adultBlocks, key = { it.id }) { block ->
                CategoryBlockCard(block = block, onClick = { onCategoryClick(block) })
            }
        }

        if (featuredBlocks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategorySectionHeader(
                    title = "Featured Categories",
                    subtitle = "Quick entry points for common manga browsing",
                )
            }
            items(featuredBlocks, key = { it.id }) { block ->
                CategoryBlockCard(block = block, onClick = { onCategoryClick(block) })
            }
        }

        if (tagBlocks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategorySectionHeader(
                    title = "Tags",
                    subtitle = "Browse every MangaDex tag as a collection",
                )
            }
            items(tagBlocks, key = { it.id }) { block ->
                CategoryBlockCard(block = block, onClick = { onCategoryClick(block) })
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CategorySearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search categories or tags") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CategoryBlockCard(
    block: CategoryBlock,
    onClick: () -> Unit,
) {
    val accent = block.categoryAccent()
    val accentAlpha = if (block.adult) 0.16f else 0.09f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = accentAlpha),
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            text = if (block.adult) "18+" else block.group.displayGroup(),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = block.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = block.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryMangaScreen(
    onBack: () -> Unit,
    onMangaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryMangaViewModel = hiltViewModel(),
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
                Column {
                    Text(
                        text = state.title,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.subtitle.isNotBlank()) {
                        Text(
                            text = state.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (state.adult) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Text(
                            text = "18+",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        when {
            state.adultBlocked -> EmptyState(
                title = "Adult content is disabled",
                message = "Enable adult content in Settings to browse this collection.",
                icon = Icons.Rounded.Category,
            )

            state.loading && state.items.isEmpty() -> LoadingState()

            state.error != null && state.items.isEmpty() ->
                ErrorState(state.error!!.userMessage, onRetry = viewModel::retry)

            state.items.isEmpty() -> EmptyState("No titles", "Try another category.")

            else -> CategoryMangaGrid(
                items = state.items,
                loadingMore = state.loadingMore,
                hasMore = state.hasMore,
                onLoadMore = viewModel::loadMore,
                onMangaClick = onMangaClick,
                gridState = gridState,
            )
        }
    }
}

@Composable
private fun CategoryMangaGrid(
    items: List<Manga>,
    loadingMore: Boolean,
    hasMore: Boolean,
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
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Manga",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${items.size} titles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(items, key = { it.id }) { manga ->
            MangaCard(
                title = manga.title,
                coverUrl = manga.coverUrl,
                subtitle = manga.metadata,
                rating = manga.year?.toString(),
                badge = if (manga.contentRating.equals("safe", ignoreCase = true)) {
                    null
                } else {
                    manga.contentRating.prettyLabel()
                },
                onClick = { onMangaClick(manga.id) },
            )
        }

        if (hasMore || loadingMore) {
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
}

private val Manga.metadata: String
    get() = listOf(status.label, contentRating.prettyLabel())
        .filter { it.isNotBlank() }
        .joinToString(" - ")

private fun String.prettyLabel(): String =
    replaceFirstChar { it.uppercase() }

private fun String.displayGroup(): String = when (this) {
    "genre" -> "Genre"
    "theme" -> "Theme"
    "content" -> "Content"
    "format" -> "Format"
    "adult" -> "18+"
    else -> replaceFirstChar { it.uppercase() }
}

private fun CategoryBlock.matchesQuery(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        subtitle.contains(query, ignoreCase = true) ||
        group.displayGroup().contains(query, ignoreCase = true)

@Composable
private fun CategoryBlock.categoryAccent(): Color = when {
    adult -> MaterialTheme.colorScheme.error
    group == "genre" -> Color(0xFF00C896)
    group == "theme" -> Color(0xFF7DD3FC)
    group == "content" -> Color(0xFFF59E0B)
    group == "format" -> Color(0xFFA78BFA)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
