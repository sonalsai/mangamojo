package com.mangamojo.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangamojo.app.domain.model.AdultContentMode
import com.mangamojo.app.domain.model.ReadingDirection
import com.mangamojo.app.domain.model.ThemeMode
import com.mangamojo.app.domain.model.ThemePalette
import com.mangamojo.app.ui.components.ConfirmDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cachedCount by viewModel.cachedCount.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<ConfirmTarget?>(null) }
    var showAdultConsent by remember { mutableStateOf(false) }
    val adultMode = settings.adultContentMode

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
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
                .padding(bottom = 24.dp),
        ) {
            SettingsSection("Appearance")
            SettingRow(title = "Theme mode", subtitle = "Use system, light, or dark display mode.") {
                SingleChoiceSegmentedButtonRow {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.onThemeModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            colors = mangaMojoSegmentedButtonColors(),
                        ) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
            }
            SettingRow(title = "Color theme", subtitle = "Default is MangaMojo Green.") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectableThemePalettes.forEach { palette ->
                        FilterChip(
                            selected = settings.themePalette == palette,
                            onClick = { viewModel.onThemePaletteChange(palette) },
                            colors = mangaMojoFilterChipColors(),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(palette.swatchColor),
                                )
                            },
                            label = { Text(palette.label) },
                        )
                    }
                }
            }

            HorizontalDivider()
            SettingsSection("Reader")
            SettingRow(
                title = "Reading direction",
                subtitle = "Vertical reader is currently supported. Other styles are coming later.",
            ) {
                SingleChoiceSegmentedButtonRow {
                    ReadingDirection.entries.forEachIndexed { index, dir ->
                        SegmentedButton(
                            selected = dir == ReadingDirection.VERTICAL,
                            enabled = false,
                            onClick = {},
                            shape = SegmentedButtonDefaults.itemShape(index, ReadingDirection.entries.size),
                            colors = mangaMojoSegmentedButtonColors(),
                        ) { Text(dir.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    }
                }
            }
            ToggleRow(
                title = "Data saver",
                subtitle = "Load lower-resolution pages to save bandwidth.",
                checked = settings.dataSaver,
                onCheckedChange = viewModel::onDataSaverChange,
            )

            HorizontalDivider()
            SettingsSection("Content")
            ToggleRow(
                title = "Adult content",
                subtitle = adultMode.description,
                checked = adultMode != AdultContentMode.OFF,
                onCheckedChange = { enabled ->
                    if (enabled) showAdultConsent = true
                    else viewModel.onAdultContentModeChange(AdultContentMode.OFF)
                },
            )
            if (adultMode != AdultContentMode.OFF) {
                SettingRow(
                    title = "Adult mode",
                    subtitle = "Choose whether adult titles appear with general titles or alone.",
                ) {
                    SingleChoiceSegmentedButtonRow {
                        adultContentModes.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = adultMode == mode,
                                onClick = { viewModel.onAdultContentModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, adultContentModes.size),
                                colors = mangaMojoSegmentedButtonColors(),
                            ) { Text(mode.label) }
                        }
                    }
                }
            }

            HorizontalDivider()
            SettingsSection("Storage")
            ClickableRow(
                title = "Clear cache",
                subtitle = "$cachedCount cached titles. Favorites and history are kept.",
                onClick = { dialog = ConfirmTarget.CACHE },
            )
            ClickableRow(
                title = "Clear history",
                subtitle = "Remove all reading history and progress.",
                onClick = { dialog = ConfirmTarget.HISTORY },
            )
            ClickableRow(
                title = "Clear favorites",
                subtitle = "Remove every title from your library.",
                onClick = { dialog = ConfirmTarget.FAVORITES },
            )

            HorizontalDivider()
            Text(
                text = "MangaMojo - Phase 1 - Source: MangaDex",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
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
            onAdultOnly = {
                viewModel.onAdultContentModeChange(AdultContentMode.ADULT_ONLY)
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

private val AdultContentMode.label: String
    get() = when (this) {
        AdultContentMode.OFF -> "Off"
        AdultContentMode.MIXED -> "Mixed"
        AdultContentMode.ADULT_ONLY -> "Adult only"
    }

private val AdultContentMode.description: String
    get() = when (this) {
        AdultContentMode.OFF -> "Adult titles are hidden from search and browse."
        AdultContentMode.MIXED -> "Adult titles can appear with general titles."
        AdultContentMode.ADULT_ONLY -> "Only adult-rated titles appear in search and browse."
    }

private val ThemePalette.label: String
    get() = when (this) {
        ThemePalette.SHONEN_CRIMSON -> "Shonen"
        ThemePalette.NEON_CYBERPUNK -> "Cyberpunk"
        ThemePalette.RETRO_SHONEN -> "Retro"
        ThemePalette.MYSTICAL_DARK_SAGE -> "MangaMojo"
    }

private val ThemePalette.swatchColor: Color
    get() = when (this) {
        ThemePalette.SHONEN_CRIMSON -> Color(0xFFE50914)
        ThemePalette.NEON_CYBERPUNK -> Color(0xFFA855F7)
        ThemePalette.RETRO_SHONEN -> Color(0xFFFF6B00)
        ThemePalette.MYSTICAL_DARK_SAGE -> Color(0xFF00C896)
    }

@Composable
private fun mangaMojoSegmentedButtonColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primary,
    activeContentColor = MaterialTheme.colorScheme.onPrimary,
    activeBorderColor = MaterialTheme.colorScheme.primary,
    inactiveContainerColor = MaterialTheme.colorScheme.surface,
    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
    inactiveBorderColor = MaterialTheme.colorScheme.outline,
    disabledActiveContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
    disabledActiveContentColor = MaterialTheme.colorScheme.primary,
    disabledActiveBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
    disabledInactiveContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
    disabledInactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
    disabledInactiveBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
)

@Composable
private fun mangaMojoFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.padding(top = 8.dp)) { control() }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun ClickableRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick) {
            Text(
                text = "Clear",
                color = if (title == "Clear cache") MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AdultContentConsentDialog(
    onMixed: () -> Unit,
    onAdultOnly: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable adult content?") },
        text = {
            Text(
                "Adult content may include explicit sexual material. Confirm that you want to include it in MangaMojo results.",
            )
        },
        confirmButton = {
            Button(
                onClick = onMixed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Mixed mode")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAdultOnly) { Text("Adult only") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
