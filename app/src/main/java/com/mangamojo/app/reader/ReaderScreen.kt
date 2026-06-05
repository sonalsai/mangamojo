package com.mangamojo.app.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mangamojo.app.domain.model.Page
import com.mangamojo.app.ui.components.EmptyState
import com.mangamojo.app.ui.components.ErrorState
import com.mangamojo.app.ui.components.LoadingState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    // A fresh list state per chapter, positioned at the resume page.
    val listState = remember(state.chapterId) {
        LazyListState(firstVisibleItemIndex = state.initialPage)
    }
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        state.mangaTitle.ifBlank { "Reader" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val pageInfo = if (state.pages.isNotEmpty()) {
                        "${state.chapterLabel}  ·  ${currentPage + 1}/${state.pages.size}"
                    } else {
                        state.chapterLabel
                    }
                    Text(
                        pageInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(),
            windowInsets = WindowInsets(0, 0, 0, 0),
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            when {
                state.error != null && state.pages.isEmpty() ->
                    ErrorState(state.error!!.userMessage, onRetry = viewModel::retry)

                state.isExternal ->
                    ExternalChapterNotice(onOpen = { state.externalUrl?.let { uriHandler.openUri(it) } })

                state.loading && state.pages.isEmpty() -> LoadingState()

                state.pages.isEmpty() ->
                    EmptyState("No pages", "This chapter has no readable pages.")

                else -> PageList(
                    pages = state.pages,
                    listState = listState,
                )
            }
        }

        ReaderControls(
            hasPrevious = state.hasPrevious,
            hasNext = state.hasNext,
            onPrevious = viewModel::goToPreviousChapter,
            onNext = viewModel::goToNextChapter,
        )
    }

    // Persist furthest-read page (debounced) and preload nearby pages.
    val context = LocalContext.current
    LaunchedEffect(state.chapterId, state.pages) {
        if (state.pages.isEmpty()) return@LaunchedEffect
        val imageLoader = SingletonImageLoader.get(context)
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                // Warm the cache for the next few pages.
                for (i in index + 1..index + 3) {
                    state.pages.getOrNull(i)?.let { page ->
                        imageLoader.enqueue(ImageRequest.Builder(context).data(page.imageUrl).build())
                    }
                }
            }
    }
    LaunchedEffect(state.chapterId, state.pages) {
        if (state.pages.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.furthestVisiblePageIndex() }
            .debounce(400)
            .distinctUntilChanged()
            .collect { viewModel.onPageChanged(it) }
    }
}

private fun LazyListState.furthestVisiblePageIndex(): Int =
    layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: firstVisibleItemIndex

@Composable
private fun PageList(pages: List<Page>, listState: LazyListState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
    ) {
        items(pages, key = { it.index }) { page ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(page.imageUrl)
                    .build(),
                contentDescription = "Page ${page.index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReaderControls(
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = hasPrevious,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.AutoMirrored.Rounded.NavigateBefore, contentDescription = null)
                Text("Previous")
            }
            Button(
                onClick = onNext,
                enabled = hasNext,
                modifier = Modifier.weight(1f),
            ) {
                Text("Next")
                Icon(Icons.AutoMirrored.Rounded.NavigateNext, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ExternalChapterNotice(onOpen: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = Color.White,
            )
            Text(
                "This chapter is hosted on an external site.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
            Button(onClick = onOpen) { Text("Open in browser") }
        }
    }
}
