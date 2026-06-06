package com.mangamojo.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangamojo.app.domain.model.AdultContentMode
import com.mangamojo.app.domain.model.ReadingDirection
import com.mangamojo.app.domain.model.ThemeMode
import com.mangamojo.app.domain.model.ThemePalette
import com.mangamojo.app.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cachedCount by viewModel.cachedCount.collectAsStateWithLifecycle()
    val adultMode = settings.adultContentMode
    val adultOnlyMode = adultMode == AdultContentMode.ADULT_ONLY
    var dialog by remember { mutableStateOf<ConfirmTarget?>(null) }
    var showAdultConsent by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsGroup(
                icon = Icons.Rounded.Palette,
                title = "Appearance",
                subtitle = "Display and identity",
            ) {
                SettingBlock(
                    title = "Theme mode",
                    subtitle = if (adultOnlyMode) "MangaMojo18+ uses locked dark mode."
                    else "Use system, light, or dark display.",
                ) {
                    ChoiceRail(
                        options = ThemeMode.entries,
                        selected = if (adultOnlyMode) ThemeMode.DARK else settings.themeMode,
                        label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                        enabled = { !adultOnlyMode },
                        onSelect = viewModel::onThemeModeChange,
                    )
                }

                SettingBlock(
                    title = "Color theme",
                    subtitle = if (adultOnlyMode) "Using MangaMojo18+ red theme."
                    else "Default theme is MangaMojo.",
                ) {
                    PaletteRail(
                        selected = settings.themePalette,
                        lockedPalette = if (adultOnlyMode) ThemePalette.SHONEN_CRIMSON else null,
                        enabled = !adultOnlyMode,
                        onSelect = viewModel::onThemePaletteChange,
                    )
                }
            }

            SettingsGroup(
                icon = Icons.Rounded.Visibility,
                title = "Reader",
                subtitle = "Reading behavior",
            ) {
                SettingBlock(
                    title = "Reading direction",
                    subtitle = "Vertical is active. Other styles are locked for now.",
                ) {
                    ChoiceRail(
                        options = ReadingDirection.entries,
                        selected = ReadingDirection.VERTICAL,
                        label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                        enabled = { false },
                        onSelect = {},
                    )
                }

                ToggleLine(
                    title = "Data saver",
                    subtitle = "Load lower-resolution pages.",
                    checked = settings.dataSaver,
                    onCheckedChange = viewModel::onDataSaverChange,
                )
            }

            SettingsGroup(
                icon = Icons.Rounded.Visibility,
                title = "Content",
                subtitle = "Rating mode",
            ) {
                ToggleLine(
                    title = "Adult content",
                    subtitle = adultMode.description,
                    checked = adultMode != AdultContentMode.OFF,
                    onCheckedChange = { enabled ->
                        if (enabled) showAdultConsent = true
                        else viewModel.onAdultContentModeChange(AdultContentMode.OFF)
                    },
                    checkedColor = if (adultMode == AdultContentMode.ADULT_ONLY) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )

                if (adultMode != AdultContentMode.OFF) {
                    val adultOnlyAccent = MaterialTheme.colorScheme.error
                    val defaultAccent = MaterialTheme.colorScheme.primary
                    SettingBlock(
                        title = "Adult mode",
                        subtitle = "Mixed keeps adult titles in normal browse. Adult only is a separate space.",
                    ) {
                        ChoiceRail(
                            options = adultContentModes,
                            selected = adultMode,
                            label = { it.label },
                            selectedColorFor = { mode ->
                                if (mode == AdultContentMode.ADULT_ONLY) adultOnlyAccent
                                else defaultAccent
                            },
                            onSelect = { mode ->
                                if (mode == AdultContentMode.ADULT_ONLY && adultMode != AdultContentMode.ADULT_ONLY) {
                                    showAdultConsent = true
                                } else {
                                    viewModel.onAdultContentModeChange(mode)
                                }
                            },
                        )
                    }

                    if (adultMode == AdultContentMode.ADULT_ONLY) {
                        ToggleLine(
                            title = "Pause adult history",
                            subtitle = "Do not save history in adult-only space.",
                            checked = settings.pauseHistoryInAdultMode,
                            checkedColor = MaterialTheme.colorScheme.error,
                            onCheckedChange = viewModel::onPauseHistoryInAdultModeChange,
                        )
                    }
                }
            }

            SettingsGroup(
                icon = Icons.Rounded.Storage,
                title = "Storage",
                subtitle = "Local app data",
            ) {
                ActionLine(
                    title = "Clear cache",
                    subtitle = "$cachedCount cached titles. Favorites and history are kept.",
                    label = "Clear",
                    onClick = { dialog = ConfirmTarget.CACHE },
                )
                ActionLine(
                    title = "Clear history",
                    subtitle = "Remove all reading history and progress.",
                    label = "Clear",
                    destructive = true,
                    onClick = { dialog = ConfirmTarget.HISTORY },
                )
                ActionLine(
                    title = "Clear favorites",
                    subtitle = "Remove every title from your library.",
                    label = "Clear",
                    destructive = true,
                    onClick = { dialog = ConfirmTarget.FAVORITES },
                )
            }

            Text(
                text = "MangaMojo - Source: MangaDex",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
    }

    dialog?.let { target ->
        ConfirmDialog(
            title = target.title,
            message = target.message,
            confirmLabel = "Clear",
            onConfirm = {
                when (target) {
                    ConfirmTarget.CACHE -> viewModel.onClearCache()
                    ConfirmTarget.HISTORY -> viewModel.onClearHistory()
                    ConfirmTarget.FAVORITES -> viewModel.onClearFavorites()
                }
            },
            onDismiss = { dialog = null },
        )
    }

    if (showAdultConsent) {
        AdultContentConsentDialog(
            onMixed = {
                viewModel.onAdultContentModeChange(AdultContentMode.MIXED)
                showAdultConsent = false
            },
            onAdultOnly = { pauseHistory ->
                viewModel.onAdultContentModeChange(AdultContentMode.ADULT_ONLY)
                viewModel.onPauseHistoryInAdultModeChange(pauseHistory)
                showAdultConsent = false
            },
            onDismiss = { showAdultConsent = false },
        )
    }
}

private val selectableThemePalettes = listOf(
    ThemePalette.MYSTICAL_DARK_SAGE,
    ThemePalette.NEON_CYBERPUNK,
    ThemePalette.RETRO_SHONEN,
)

private val adultContentModes = listOf(
    AdultContentMode.MIXED,
    AdultContentMode.ADULT_ONLY,
)

private enum class ConfirmTarget(val title: String, val message: String) {
    CACHE("Clear cache?", "Cached metadata will be removed. Favorites and history are kept."),
    HISTORY("Clear history?", "All reading history and progress will be removed."),
    FAVORITES("Clear favorites?", "All titles will be removed from your library."),
}

@Composable
private fun SettingsGroup(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingBlock(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingText(title = title, subtitle = subtitle)
        content()
        GroupDivider()
    }
}

@Composable
private fun ToggleLine(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingText(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        MojoSwitch(
            checked = checked,
            checkedColor = checkedColor,
            onClick = { onCheckedChange(!checked) },
        )
    }
    GroupDivider()
}

@Composable
private fun ActionLine(
    title: String,
    subtitle: String,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingText(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (destructive) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                )
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
    GroupDivider()
}

@Composable
private fun SettingText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
        thickness = 1.dp,
    )
}

@Composable
private fun <T> ChoiceRail(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: (T) -> Boolean = { true },
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    selectedColorFor: ((T) -> Color)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val isEnabled = enabled(option)
            val activeColor = selectedColorFor?.invoke(option) ?: selectedColor
            val itemColor = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (isSelected) activeColor.copy(alpha = 0.12f)
                        else Color.Transparent,
                    )
                    .clickable(enabled = isEnabled) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = itemColor,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isEnabled || isSelected) itemColor else itemColor.copy(alpha = 0.48f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteRail(
    selected: ThemePalette,
    lockedPalette: ThemePalette? = null,
    enabled: Boolean = true,
    onSelect: (ThemePalette) -> Unit,
) {
    val visiblePalettes = if (lockedPalette != null) listOf(lockedPalette) else selectableThemePalettes
    val activePalette = lockedPalette ?: selected
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        visiblePalettes.forEach { palette ->
            val isSelected = activePalette == palette
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) palette.swatchColor.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.background,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) palette.swatchColor
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable(enabled = enabled) { onSelect(palette) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(palette.swatchColor),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = palette.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) palette.swatchColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!enabled) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Locked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MojoSwitch(
    checked: Boolean,
    checkedColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 30.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (checked) checkedColor.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f),
            )
            .border(
                width = 1.dp,
                color = if (checked) checkedColor.copy(alpha = 0.72f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) checkedColor else MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
private fun AdultContentConsentDialog(
    onMixed: () -> Unit,
    onAdultOnly: (pauseHistory: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var pauseHistory by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable adult content?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Adult content may include explicit sexual material. Confirm that you want to include it in results.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Adult only creates a separate space for adult-rated titles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
                ToggleLine(
                    title = "Pause adult history",
                    subtitle = "Do not save history in adult-only space.",
                    checked = pauseHistory,
                    checkedColor = MaterialTheme.colorScheme.error,
                    onCheckedChange = { pauseHistory = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onMixed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Mixed")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAdultOnly(pauseHistory) }) { Text("Adult only") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private val AdultContentMode.label: String
    get() = when (this) {
        AdultContentMode.OFF -> "Off"
        AdultContentMode.MIXED -> "Mixed"
        AdultContentMode.ADULT_ONLY -> "Adult only"
    }

private val AdultContentMode.description: String
    get() = when (this) {
        AdultContentMode.OFF -> "Adult titles are hidden from search and browse."
        AdultContentMode.MIXED -> "Adult and normal titles appear together."
        AdultContentMode.ADULT_ONLY -> "Only adult-rated titles appear in the separate MangaMojo18+ space."
    }

private val ThemePalette.label: String
    get() = when (this) {
        ThemePalette.SHONEN_CRIMSON -> "MangaMojo18+"
        ThemePalette.NEON_CYBERPUNK -> "Cyber"
        ThemePalette.RETRO_SHONEN -> "Retro"
        ThemePalette.MYSTICAL_DARK_SAGE -> "Manga"
    }

private val ThemePalette.swatchColor: Color
    get() = when (this) {
        ThemePalette.SHONEN_CRIMSON -> Color(0xFFE50914)
        ThemePalette.NEON_CYBERPUNK -> Color(0xFFA855F7)
        ThemePalette.RETRO_SHONEN -> Color(0xFFFF6B00)
        ThemePalette.MYSTICAL_DARK_SAGE -> Color(0xFF00C896)
    }
