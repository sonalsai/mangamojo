package com.mangamojo.app.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Central route table. Detail/reader routes expose typed builders. */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CATEGORIES = "categories"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    const val ARG_MANGA_ID = "mangaId"
    const val ARG_CHAPTER_ID = "chapterId"
    const val ARG_CATEGORY_ID = "categoryId"
    const val ARG_CATEGORY_NAME = "categoryName"
    const val ARG_CATEGORY_GROUP = "categoryGroup"
    const val ARG_CATEGORY_TAG_IDS = "categoryTagIds"
    const val ARG_CATEGORY_RATINGS = "categoryRatings"

    const val DETAILS = "details/{$ARG_MANGA_ID}"
    const val READER = "reader/{$ARG_MANGA_ID}/{$ARG_CHAPTER_ID}"
    const val CATEGORY_RESULTS =
        "category/{$ARG_CATEGORY_ID}" +
            "?$ARG_CATEGORY_NAME={$ARG_CATEGORY_NAME}" +
            "&$ARG_CATEGORY_GROUP={$ARG_CATEGORY_GROUP}" +
            "&$ARG_CATEGORY_TAG_IDS={$ARG_CATEGORY_TAG_IDS}" +
            "&$ARG_CATEGORY_RATINGS={$ARG_CATEGORY_RATINGS}"

    fun details(mangaId: String): String = "details/$mangaId"
    fun reader(mangaId: String, chapterId: String): String = "reader/$mangaId/$chapterId"
    fun category(
        id: String,
        name: String,
        group: String,
        tagIds: List<String>,
        ratings: List<String>,
    ): String =
        "category/${Uri.encode(id)}" +
            "?$ARG_CATEGORY_NAME=${Uri.encode(name)}" +
            "&$ARG_CATEGORY_GROUP=${Uri.encode(group)}" +
            "&$ARG_CATEGORY_TAG_IDS=${Uri.encode(tagIds.joinToString(","))}" +
            "&$ARG_CATEGORY_RATINGS=${Uri.encode(ratings.joinToString(","))}"
}

/** Tabs shown in the side navigation drawer. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "Home", Icons.Rounded.Home),
    SEARCH(Routes.SEARCH, "Search", Icons.Rounded.Search),
    CATEGORIES(Routes.CATEGORIES, "Categories", Icons.Rounded.Category),
    FAVORITES(Routes.FAVORITES, "Library", Icons.Rounded.Favorite),
    HISTORY(Routes.HISTORY, "History", Icons.Rounded.History),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Rounded.Settings),
}
