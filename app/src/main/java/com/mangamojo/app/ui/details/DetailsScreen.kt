package com.mangamojo.app.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangamojo.app.core.UiState
import com.mangamojo.app.domain.model.Chapter
import com.mangamojo.app.domain.model.MangaDetails
import com.mangamojo.app.domain.model.ReadingProgress
import com.mangamojo.app.ui.components.CoverImage
import com.mangamojo.app.ui.components.EmptyState
import com.mangamojo.app.ui.components.ErrorState
import com.mangamojo.app.ui.components.LoadingState
import com.mangamojo.app.ui.components.SectionHeader
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onChapterClick: (chapterId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val details = state.details

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = (details as? UiState.Success)?.data?.title ?: "Details",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (details is UiState.Success) {
                    IconButton(onClick = viewModel::onToggleFavorite) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Rounded.Favorite
                            else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (state.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
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

        when (details) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(details.error.userMessage, onRetry = viewModel::load)
            is UiState.Success -> DetailsContent(
                details = details.data,
                chapters = state.chapters,
                chapterProgress = state.chapterProgress,
                onChapterClick = onChapterClick,
                onSetChapterReadState = viewModel::onSetChapterReadState,
                onOpenExternal = { uriHandler.openUri(it) },
                onRetryChapters = viewModel::load,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailsContent(
    details: MangaDetails,
    chapters: UiState<List<Chapter>>,
    chapterProgress: Map<String, ReadingProgress>,
    onChapterClick: (String) -> Unit,
    onSetChapterReadState: (Chapter, Boolean) -> Unit,
    onOpenExternal: (String) -> Unit,
    onRetryChapters: () -> Unit,
) {
    val chapterList = (chapters as? UiState.Success)?.data.orEmpty()
    val resumeChapter = chapterList.firstOrNull { chapter ->
        !chapter.isExternal && chapterProgress[chapter.id]?.let { !it.completed } == true
    }
    val primaryChapter = resumeChapter
        ?: chapterList.firstOrNull { chapter ->
            !chapter.isExternal && chapterProgress[chapter.id]?.completed != true
        }
        ?: chapterList.firstOrNull { !it.isExternal }
        ?: chapterList.firstOrNull()
    val startButtonLabel = if (resumeChapter != null) resumeChapter.continueLabel() else "Start Reading"
    var chapterQuery by remember(details.id) { mutableStateOf("") }
    var readFilter by remember(details.id) { mutableStateOf(ChapterReadFilter.ALL) }
    var sortOrder by remember(details.id) { mutableStateOf(ChapterSortOrder.NEWEST) }
    val visibleChapters = remember(chapterList, chapterProgress, chapterQuery, readFilter, sortOrder) {
        chapterList
            .filter { it.matchesQuery(chapterQuery) }
            .filter { chapter ->
                val progress = chapterProgress[chapter.id]
                when (readFilter) {
                    ChapterReadFilter.ALL -> true
                    ChapterReadFilter.UNREAD -> progress?.completed != true
                    ChapterReadFilter.READ -> progress?.completed == true
                }
            }
            .sortedByOrder(sortOrder)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            HeaderSection(
                details = details,
                primaryChapter = primaryChapter,
                startButtonLabel = startButtonLabel,
                onStart = { chapter ->
                    if (chapter.isExternal) onOpenExternal(chapter.externalUrl.orEmpty())
                    else onChapterClick(chapter.id)
                },
            )
        }

        stickyHeader {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = 0.dp),
            ) {
                val count = (chapters as? UiState.Success)?.data?.size
                SectionHeader(if (count != null) "Chapters ($count)" else "Chapters")
                if (chapters is UiState.Success && chapters.data.isNotEmpty()) {
                    ChapterListControls(
                        query = chapterQuery,
                        onQueryChange = { chapterQuery = it },
                        readFilter = readFilter,
                        onReadFilterChange = { readFilter = it },
                        sortOrder = sortOrder,
                        onSortOrderChange = { sortOrder = it },
                        totalCount = chapterList.size,
                        visibleCount = visibleChapters.size,
                        readCount = chapterList.count { chapterProgress[it.id]?.completed == true },
                        startedCount = chapterList.count {
                            val progress = chapterProgress[it.id]
                            progress != null && !progress.completed
                        },
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                    thickness = 1.dp,
                )
            }
        }

        when (chapters) {
            is UiState.Loading -> item {
                Box(Modifier.fillMaxWidth().height(160.dp)) { LoadingState() }
            }
            is UiState.Error -> item {
                Box(Modifier.fillMaxWidth().height(160.dp)) {
                    ErrorState(chapters.error.userMessage, onRetry = onRetryChapters)
                }
            }
            is UiState.Success -> {
                if (chapters.data.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(160.dp)) {
                            EmptyState("No chapters", "There are no readable chapters in your language yet.")
                        }
                    }
                } else {
                    if (visibleChapters.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(160.dp)) {
                                EmptyState("No matching chapters", "Adjust search, read status, or sort filters.")
                            }
                        }
                    }

                    items(visibleChapters, key = { it.id }) { chapter ->
                        SwipeableChapterRow(
                            chapter = chapter,
                            progress = chapterProgress[chapter.id],
                            onClick = {
                                if (chapter.isExternal) onOpenExternal(chapter.externalUrl.orEmpty())
                                else onChapterClick(chapter.id)
                            },
                            onSetReadState = { read -> onSetChapterReadState(chapter, read) },
                        )
                    }
                }
            }
        }
    }
}

private enum class ChapterReadFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    READ("Read"),
}

private enum class ChapterSortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
}

@Composable
private fun HeaderSection(
    details: MangaDetails,
    primaryChapter: Chapter?,
    startButtonLabel: String,
    onStart: (Chapter) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(470.dp),
    ) {
        CoverImage(
            url = details.coverUrl,
            contentDescription = details.title,
            cornerRadius = 0,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background.copy(alpha = 0.18f),
                        0.34f to MaterialTheme.colorScheme.background.copy(alpha = 0.54f),
                        0.68f to MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
                        1f to MaterialTheme.colorScheme.background,
                    )
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CoverImage(
                    url = details.coverUrl,
                    contentDescription = details.title,
                    modifier = Modifier.width(118.dp).height(176.dp),
                    cornerRadius = 10,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        details.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val author = (details.authors + details.artists)
                        .distinct()
                        .joinToString(", ")
                        .ifBlank { "Unknown author" }
                    Text(
                        author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailPill(details.status.label)
                        details.year?.let { DetailPill(it.toString()) }
                    }
                }
            }

            Button(
                onClick = { primaryChapter?.let(onStart) },
                enabled = primaryChapter != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(startButtonLabel, fontWeight = FontWeight.Bold)
            }

            if (details.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(details.tags.take(8)) { tag -> DetailPill(tag) }
                }
            }

            if (details.description.isNotBlank()) {
                Text(
                    text = details.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChapterListControls(
    query: String,
    onQueryChange: (String) -> Unit,
    readFilter: ChapterReadFilter,
    onReadFilterChange: (ChapterReadFilter) -> Unit,
    sortOrder: ChapterSortOrder,
    onSortOrderChange: (ChapterSortOrder) -> Unit,
    totalCount: Int,
    visibleCount: Int,
    readCount: Int,
    startedCount: Int,
) {
    val unreadCount = (totalCount - readCount - startedCount).coerceAtLeast(0)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search chapter number or title") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Text(
            text = chapterSummary(
                query = query,
                readFilter = readFilter,
                totalCount = totalCount,
                visibleCount = visibleCount,
                readCount = readCount,
                startedCount = startedCount,
                unreadCount = unreadCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ChapterReadFilter.entries, key = { it.name }) { filter ->
                    FilterChip(
                        selected = readFilter == filter,
                        onClick = { onReadFilterChange(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            FilterChip(
                selected = false,
                onClick = {
                    onSortOrderChange(
                        if (sortOrder == ChapterSortOrder.NEWEST) ChapterSortOrder.OLDEST
                        else ChapterSortOrder.NEWEST,
                    )
                },
                label = { Text(sortOrder.label) },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun DetailPill(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f), RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableChapterRow(
    chapter: Chapter,
    progress: ReadingProgress?,
    onClick: () -> Unit,
    onSetReadState: (Boolean) -> Unit,
) {
    val markRead = progress?.completed != true
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSetReadState(markRead)
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeActionBackground(markRead = markRead) },
    ) {
        ChapterRow(
            chapter = chapter,
            progress = progress,
            onClick = onClick,
        )
    }
}

@Composable
private fun SwipeActionBackground(markRead: Boolean) {
    val color = if (markRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = if (markRead) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(end = 24.dp).size(24.dp),
        )
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    progress: ReadingProgress?,
    onClick: () -> Unit,
) {
    val rowState = chapter.rowState(progress)
    val isCompleted = rowState == ChapterRowState.COMPLETED
    val isUnreadOrStarted = rowState != ChapterRowState.COMPLETED
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        if (isUnreadOrStarted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                        RoundedCornerShape(50),
                    ),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = chapter.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.SemiBold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = chapter.metadataLine(progress),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (chapter.isExternal) {
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Opens externally",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 68.dp, end = 22.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.09f)),
        )
    }
}

private enum class ChapterRowState {
    UNREAD,
    STARTED,
    COMPLETED,
}

private fun Chapter.matchesQuery(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true
    return chapter.orEmpty().contains(normalized, ignoreCase = true) ||
        title.contains(normalized, ignoreCase = true) ||
        label.contains(normalized, ignoreCase = true) ||
        scanlationGroup.contains(normalized, ignoreCase = true)
}

private fun List<Chapter>.sortedByOrder(order: ChapterSortOrder): List<Chapter> {
    return when (order) {
        ChapterSortOrder.NEWEST -> sortedWith(
            compareByDescending<Chapter> { it.chapterNumberSortKey() ?: Double.NEGATIVE_INFINITY }
                .thenByDescending { it.publishedAtMillis() ?: Long.MIN_VALUE },
        )
        ChapterSortOrder.OLDEST -> sortedWith(
            compareBy<Chapter> { it.chapterNumberSortKey() ?: Double.POSITIVE_INFINITY }
                .thenBy { it.publishedAtMillis() ?: Long.MAX_VALUE },
        )
    }
}

private fun Chapter.chapterNumberSortKey(): Double? =
    chapter?.toDoubleOrNull()

private fun Chapter.continueLabel(): String =
    chapter?.takeIf { it.isNotBlank() }?.let { "Continue Ch. $it" } ?: "Continue Reading"

private fun chapterSummary(
    query: String,
    readFilter: ChapterReadFilter,
    totalCount: Int,
    visibleCount: Int,
    readCount: Int,
    startedCount: Int,
    unreadCount: Int,
): String {
    if (query.isNotBlank() || readFilter != ChapterReadFilter.ALL || visibleCount != totalCount) {
        return "$visibleCount of $totalCount chapters"
    }
    return buildList {
        add("$totalCount chapters")
        if (unreadCount > 0) add("$unreadCount unread")
        if (startedCount > 0) add("$startedCount started")
        if (readCount > 0) add("$readCount read")
    }.joinToString(" · ")
}

private fun Chapter.metadataLine(progress: ReadingProgress?): String =
    buildList {
        publishAt?.relativeTimeLabel()?.let { add(it) }
        progress?.viewedPageLabel()?.let { add(it) }
        add(scanlationGroup)
    }.joinToString(" - ")

private fun Chapter.rowState(progress: ReadingProgress?): ChapterRowState =
    when {
        progress?.completed == true -> ChapterRowState.COMPLETED
        progress != null -> ChapterRowState.STARTED
        else -> ChapterRowState.UNREAD
    }

private fun ReadingProgress.viewedPageLabel(): String? {
    if (completed) return null
    val viewedPage = (page + 1).coerceAtLeast(1)
    return if (total > 0) {
        "Page ${viewedPage.coerceAtMost(total)}/$total viewed"
    } else {
        "Page $viewedPage viewed"
    }
}

private fun Chapter.publishedAtMillis(): Long? =
    publishAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

private fun String.relativeTimeLabel(now: Instant = Instant.now()): String? {
    val publishedAt = runCatching { Instant.parse(this) }.getOrNull() ?: return null
    val duration = Duration.between(publishedAt, now)
    val days = duration.toDays().coerceAtLeast(0)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L -> "$days days ago"
        days < 30L -> "${days / 7} weeks ago"
        days < 365L -> "${days / 30} months ago"
        else -> DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
            .format(publishedAt.atZone(ZoneId.systemDefault()))
    }
}
