package com.godson.vpn.ui.country

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godson.vpn.R
import com.godson.vpn.data.CountryCategory
import com.godson.vpn.data.CountrySort
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.UiSettings
import com.godson.vpn.data.model.Region
import com.godson.vpn.data.model.ServerStatus
import com.godson.vpn.ui.common.GodsonViewModelFactory
import com.godson.vpn.ui.components.DemoBadge
import com.godson.vpn.ui.components.EmptyState
import com.godson.vpn.ui.components.SkeletonCard
import com.godson.vpn.ui.components.SubHeader
import com.godson.vpn.ui.components.motionMillis
import com.godson.vpn.ui.components.pressScale
import com.godson.vpn.ui.components.rememberShimmerAlpha
import com.godson.vpn.ui.theme.RadiusLarge
import com.godson.vpn.ui.theme.RadiusMedium
import com.godson.vpn.ui.theme.RadiusPill
import com.godson.vpn.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * « Choisir un pays » : catalogue par région, recherche instantanée, filtres,
 * favoris/récents persistants, sélection mise en évidence. Les pays sans serveur
 * opérationnel sont affichés « Bientôt disponible » et non sélectionnables.
 */
@Composable
fun CountryPickerScreen(container: GodsonContainer, onBack: () -> Unit) {
    val vm: CountryPickerViewModel = viewModel(factory = GodsonViewModelFactory(container))
    val controls by vm.controlsState.collectAsStateWithLifecycle()
    val rows by vm.visibleRows.collectAsStateWithLifecycle()
    val settings by container.settingsRepository.uiSettings
        .collectAsStateWithLifecycle(initialValue = UiSettings())
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val shimmer = rememberShimmerAlpha()

    Column(Modifier.fillMaxSize()) {
        SubHeader(
            title = stringResource(R.string.country_picker_title),
            subtitle = stringResource(R.string.country_picker_subtitle),
            onBack = onBack,
            trailing = {
                if (vm.isDemo) DemoBadge()
            }
        )

        PickerSearchField(
            value = controls.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // Catégories : Tous / Favoris / Récents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                label = stringResource(R.string.cat_all),
                selected = controls.category == CountryCategory.ALL,
                onClick = { vm.setCategory(CountryCategory.ALL) }
            )
            CategoryChip(
                label = stringResource(R.string.cat_favorites),
                selected = controls.category == CountryCategory.FAVORITES,
                onClick = { vm.setCategory(CountryCategory.FAVORITES) }
            )
            CategoryChip(
                label = stringResource(R.string.cat_recent),
                selected = controls.category == CountryCategory.RECENT,
                onClick = { vm.setCategory(CountryCategory.RECENT) }
            )
        }

        // Filtres par continent
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                label = stringResource(R.string.region_all),
                selected = controls.region == null,
                onClick = { vm.setRegion(null) }
            )
            Region.entries.forEach { region ->
                CategoryChip(
                    label = stringResource(region.labelRes()),
                    selected = controls.region == region,
                    onClick = { vm.setRegion(region) }
                )
            }
        }

        // Disponibilité + tris
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                label = stringResource(R.string.filter_available_only),
                selected = controls.availableOnly,
                onClick = { vm.setAvailableOnly(!controls.availableOnly) }
            )
            CategoryChip(
                label = stringResource(R.string.sort_alpha),
                selected = controls.sort == CountrySort.ALPHA,
                onClick = { vm.setSort(CountrySort.ALPHA) }
            )
            CategoryChip(
                label = stringResource(R.string.servers_sort_latency),
                selected = controls.sort == CountrySort.LATENCY,
                onClick = { vm.setSort(CountrySort.LATENCY) }
            )
        }

        when {
            controls.loading -> {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.countries_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    repeat(6) { SkeletonCard(height = 74.dp, shimmer = shimmer) }
                }
            }
            controls.error -> {
                EmptyState(
                    icon = Icons.Rounded.WifiOff,
                    title = stringResource(R.string.countries_error_title),
                    body = stringResource(R.string.countries_error_body)
                )
                Button(
                    onClick = { vm.refresh() },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                ) { Text(stringResource(R.string.action_retry)) }
            }
            rows.isEmpty() -> {
                EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = stringResource(R.string.countries_empty_title),
                    body = stringResource(R.string.countries_empty_body)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp, vertical = 6.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    itemsIndexed(rows, key = { _, r -> r.entry.code }) { index, row ->
                        StaggeredAppearance(index) { modifier ->
                            CountryRow(
                                row = row,
                                hapticEnabled = settings.hapticFeedback,
                                onToggleFavorite = {
                                    if (settings.hapticFeedback) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    vm.toggleFavorite(row.entry.code)
                                },
                                onSelect = {
                                    if (vm.select(row.entry)) {
                                        if (settings.hapticFeedback) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        Toast.makeText(
                                            context,
                                            context.getString(
                                                R.string.country_selected_toast
                                            ) + " — " + row.entry.flagEmoji + " " + row.entry.nameFr,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        scope.launch {
                                            delay(320) // laisse voir la mise en évidence
                                            onBack()
                                        }
                                    }
                                },
                                modifier = modifier
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Region.labelRes(): Int = when (this) {
    Region.AFRICA -> R.string.region_africa
    Region.EUROPE -> R.string.region_europe
    Region.NORTH_AMERICA -> R.string.region_north_america
    Region.SOUTH_AMERICA -> R.string.region_south_america
    Region.ASIA -> R.string.region_asia
    Region.OCEANIA -> R.string.region_oceania
}

/** Apparition progressive en cascade (désactivée via « Réduire les animations »). */
@Composable
private fun StaggeredAppearance(index: Int, content: @Composable (Modifier) -> Unit) {
    val reduced = com.godson.vpn.ui.theme.LocalReducedMotion.current
    var appeared by remember { mutableStateOf(reduced) }
    LaunchedEffect(Unit) {
        if (!appeared) {
            delay(minOf(index, 10) * 30L)
            appeared = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(motionMillis(240)),
        label = "countryAppear"
    )
    Box(Modifier.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 20f
    }) { content(Modifier) }
}

@Composable
private fun PickerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(RadiusMedium))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.servers_search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.servers_clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(RadiusPill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun CountryRow(
    row: CountryRowUi,
    hapticEnabled: Boolean,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val server = row.server
    val selected = row.isSelected
    val statusLabel = when (server?.status) {
        ServerStatus.AVAILABLE -> stringResource(R.string.server_available)
        ServerStatus.BUSY -> stringResource(R.string.status_busy)
        ServerStatus.UNAVAILABLE -> stringResource(R.string.server_unavailable)
        else -> stringResource(R.string.status_planned)
    }
    val statusColor = when (server?.status) {
        ServerStatus.AVAILABLE -> MaterialTheme.colorScheme.tertiary
        ServerStatus.BUSY -> WarningAmber
        ServerStatus.UNAVAILABLE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, 0.985f)
            .clip(RoundedCornerShape(RadiusLarge))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                when {
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                RoundedCornerShape(RadiusLarge)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = row.selectable,
                onClick = onSelect
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .then(if (!row.selectable) Modifier.alpha(0.62f) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.entry.flagEmoji,
            fontSize = 26.sp,
            modifier = Modifier.width(42.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = row.entry.nameFr,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = server?.city ?: stringResource(R.string.country_soon_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = server?.pingMs?.let { "$it ms" }
                    ?: stringResource(R.string.value_unavailable),
                style = MaterialTheme.typography.titleMedium,
                color = if (row.selectable) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (server?.isDemo == true && server.status != ServerStatus.PLANNED) {
                Text(
                    text = stringResource(R.string.demo_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber,
                    letterSpacing = 1.sp
                )
            }
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (row.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(
                    if (row.isFavorite) R.string.server_favorite_remove
                    else R.string.server_favorite_add
                ),
                tint = if (row.isFavorite) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
