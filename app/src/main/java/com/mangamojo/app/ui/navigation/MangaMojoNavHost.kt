package com.mangamojo.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mangamojo.app.ui.categories.CategoryMangaScreen
import com.mangamojo.app.reader.ReaderScreen
import com.mangamojo.app.ui.categories.CategoriesScreen
import com.mangamojo.app.ui.details.DetailsScreen
import com.mangamojo.app.ui.favorites.FavoritesScreen
import com.mangamojo.app.ui.history.HistoryScreen
import com.mangamojo.app.ui.home.HomeScreen
import com.mangamojo.app.ui.search.SearchScreen
import com.mangamojo.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MangaMojoNavHost(navController: NavHostController = rememberNavController(), isAdultMode: Boolean = false) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showDrawer = TopLevelDestination.entries.any { it.route == currentRoute }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
        Unit
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        scrimColor = Color.Black.copy(alpha = 0.64f),
        drawerContent = {
            MangaMojoDrawer(
                currentRoute = currentRoute,
                isAdultMode = isAdultMode,
                onDestinationClick = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigateToTab(route)
                },
            )
        },
    ) {
        Scaffold { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onMangaClick = { navController.navigate(Routes.details(it)) },
                        onResume = { mangaId, chapterId ->
                            navController.navigate(Routes.reader(mangaId, chapterId))
                        },
                        onSearch = { navController.navigateToTab(Routes.SEARCH) },
                        onSeeFavorites = { navController.navigateToTab(Routes.FAVORITES) },
                        onSeeHistory = { navController.navigateToTab(Routes.HISTORY) },
                        onSeeCategories = { navController.navigateToTab(Routes.CATEGORIES) },
                        onOpenDrawer = openDrawer,
                    )
                }

                composable(Routes.SEARCH) {
                    SearchScreen(
                        onMangaClick = { navController.navigate(Routes.details(it)) },
                        onOpenDrawer = openDrawer,
                    )
                }

                composable(Routes.CATEGORIES) {
                    CategoriesScreen(
                        onCategoryClick = { category ->
                            navController.navigate(
                                Routes.category(
                                    id = category.id,
                                    name = category.title,
                                    group = category.group,
                                    tagIds = category.tagIds,
                                    ratings = category.contentRatings,
                                )
                            )
                        },
                        onOpenDrawer = openDrawer,
                    )
                }

                composable(Routes.FAVORITES) {
                    FavoritesScreen(
                        onMangaClick = { navController.navigate(Routes.details(it)) },
                        onOpenDrawer = openDrawer,
                    )
                }

                composable(Routes.HISTORY) {
                    HistoryScreen(
                        onResume = { mangaId, chapterId ->
                            navController.navigate(Routes.reader(mangaId, chapterId))
                        },
                        onOpenDrawer = openDrawer,
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(onOpenDrawer = openDrawer)
                }

                composable(
                    route = Routes.DETAILS,
                    arguments = listOf(navArgument(Routes.ARG_MANGA_ID) { type = NavType.StringType }),
                ) { entry ->
                    val mangaId = entry.arguments?.getString(Routes.ARG_MANGA_ID).orEmpty()
                    DetailsScreen(
                        onBack = { navController.popBackStack() },
                        onChapterClick = { chapterId ->
                            navController.navigate(Routes.reader(mangaId, chapterId))
                        },
                    )
                }

                composable(
                    route = Routes.READER,
                    arguments = listOf(
                        navArgument(Routes.ARG_MANGA_ID) { type = NavType.StringType },
                        navArgument(Routes.ARG_CHAPTER_ID) { type = NavType.StringType },
                    ),
                ) {
                    ReaderScreen(onBack = { navController.popBackStack() })
                }

                composable(
                    route = Routes.CATEGORY_RESULTS,
                    arguments = listOf(
                        navArgument(Routes.ARG_CATEGORY_ID) { type = NavType.StringType },
                        navArgument(Routes.ARG_CATEGORY_NAME) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument(Routes.ARG_CATEGORY_GROUP) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument(Routes.ARG_CATEGORY_TAG_IDS) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument(Routes.ARG_CATEGORY_RATINGS) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
                ) {
                    CategoryMangaScreen(
                        onBack = { navController.popBackStack() },
                        onMangaClick = { navController.navigate(Routes.details(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaMojoDrawer(
    currentRoute: String?,
    isAdultMode: Boolean = false,
    onDestinationClick: (String) -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 18.dp, vertical = 24.dp),
        ) {
            DrawerHeader(isAdultMode = isAdultMode)
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f))
            Spacer(Modifier.height(18.dp))

            DrawerSectionLabel("Browse")
            Spacer(Modifier.height(8.dp))
            TopLevelDestination.entries
                .filterNot { it == TopLevelDestination.SETTINGS }
                .forEach { destination ->
                    DrawerNavItem(
                        destination = destination,
                        selected = currentRoute == destination.route,
                        onClick = { onDestinationClick(destination.route) },
                    )
                }

            Spacer(Modifier.weight(1f))

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
            Spacer(Modifier.height(10.dp))
            DrawerSectionLabel("Manage")
            Spacer(Modifier.height(8.dp))
            DrawerNavItem(
                destination = TopLevelDestination.SETTINGS,
                selected = currentRoute == TopLevelDestination.SETTINGS.route,
                onClick = { onDestinationClick(TopLevelDestination.SETTINGS.route) },
            )
            Text(
                text = "MangaDex source",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun DrawerHeader(isAdultMode: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "M",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.size(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "MANGAMOJO",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isAdultMode) {
                Spacer(Modifier.size(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp)
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
    }
}

@Composable
private fun DrawerSectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun DrawerNavItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val iconColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Spacer(
                modifier = Modifier
                    .height(24.dp)
                    .size(width = 3.dp, height = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.size(12.dp))
        } else {
            Spacer(Modifier.size(15.dp))
        }
        Icon(
            destination.icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Switch drawer tabs while preserving each tab's state. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
