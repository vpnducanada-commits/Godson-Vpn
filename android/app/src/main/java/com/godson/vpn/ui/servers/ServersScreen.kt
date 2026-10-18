package com.godson.vpn.ui.servers

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.ServerCategory
import com.godson.vpn.data.ServerFilter
import com.godson.vpn.data.UiSettings
import com.godson.vpn.data.model.ServerInfo
import com.godson.vpn.pulse.PulseReport
import com.godson.vpn.pulse.PulseStatus
import com.godson.vpn.pulse.QualityLevel
import com.godson.vpn.ui.common.GodsonViewModelFactory
import com.godson.vpn.ui.components.DemoBadge
import com.godson.vpn.ui.components.EmptyState
import com.godson.vpn.ui.components.FlagEmojiBadge
import com.godson.vpn.ui.components.GodsonCard
import com.godson.vpn.ui.components.SignalBars
import com.godson.vpn.ui.components.SubHeader
import com.godson.vpn.ui.components.motionMillis
import com.godson.vpn.ui.components.rememberShimmerAlpha
import com.godson.vpn.ui.theme.CyberCyan
import com.godson.vpn.ui.theme.ErrorRed
import com.godson.vpn.ui.theme.RadiusMedium
import com.godson.vpn.ui.theme.RadiusPill
import com.godson.vpn.ui.theme.RadiusSmall
import com.godson.vpn.ui.theme.SuccessGreen
import com.godson.vpn.ui.theme.WarningAmber
import kotlinx.coroutines.delay

/**
 * Écran « Emplacements » : recherche instantanée, catégories, tri par latence,
 * skeletons de chargement, états vides illustrés, favoris persistants.
 */
@Composable
fun ServersScreen(
    container: GodsonContainer,
    onBack: () -> Unit
) {
    val vm: ServersViewModel = viewModel(factory = GodsonViewModelFactory(container))
    val controls by vm.controlsState.collectAsStateWithLifecycle()
    val servers by vm.visibleServers.collectAsStateWithLifecycle()
    val pulse by vm.pulseState.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val settings by container.settingsRepository.uiSettings
        .collectAsStateWithLifecycle(initialValue = UiSettings())
    val haptics = LocalHapticFeedback.current

    val shimmer = rememberShimmerAlpha()

    Column(Modifier.fillMaxSize()) {
        SubHeader(
            title = stringResource(R.string.servers_title),
            subtitle = stringResource(R.string.servers_subtitle),
            onBack = onBack,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DemoBadge()
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.servers_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        // Recherche instantanée
        SearchField(
            value = controls.query,
            onValueChange = vm::setQuery,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // Catégories + tri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(
                label = stringResource(R.string.cat_recommended),
                selected = controls.category == ServerCategory.RECOMMENDED,
                onClick = { vm.setCategory(ServerCategory.RECOMMENDED) }
            )
            CategoryChip(
                label = stringResource(R.string.cat_all),
                selected = controls.category == ServerCategory.ALL,
                onClick = { vm.setCategory(ServerCategory.ALL) }
            )
            CategoryChip(
                label = stringResource(R.string.cat_favorites),
                selected = controls.category == ServerCategory.FAVORITES,
                onClick = { vm.setCategory(ServerCategory.FAVORITES) }
            )
            CategoryChip(
                label = stringResource(R.string.cat_recent),
                selected = controls.category == ServerCategory.RECENT,
                onClick = { vm.setCategory(ServerCategory.RECENT) }
            )
            SortButton(active = controls.sortByLatency, onClick = vm::toggleSortByLatency)
        }

        when {
            controls.loading -> {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.servers_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(0.8f)
                    )
                    repeat(5) {
                        com.godson.vpn.ui.components.SkeletonCard(height = 76.dp, shimmer = shimmer)
                    }
                }
            }
            servers.isEmpty() -> EmptyServersState(controls)
            else -> {
                Text(
                    text = servers.size.toString() + " · " + stringResource(R.string.demo_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // Synthèse GODSON PULSE — étiquetée « démo » tant qu'aucune sonde
                // réelle n'existe ; « Indisponible » sans valeur inventée sinon.
                pulse.report?.let { report ->
                    PulseSummaryLine(
                        report = report,
                        recommendedCountry = report.recommendation
                            ?.let { reco -> servers.firstOrNull { it.id == reco.serverId }?.country }
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp, vertical = 10.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(servers, key = { _, s -> s.id }) { index, server ->
                        StaggeredAppearance(index = index) { modifier ->
                            ServerCard(
                                server = server,
                                isFavorite = server.id in favorites,
                                hapticEnabled = settings.hapticFeedback,
                                pulseScore = pulse.scoreByServerId[server.id],
                                onToggleFavorite = {
                                    if (settings.hapticFeedback) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    vm.toggleFavorite(server.id)
                                },
                                onClick = {
                                    vm.select(server)
                                    onBack()
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

/** Apparition progressive en cascade (désactivée avec « Réduire les animations »). */
@Composable
private fun StaggeredAppearance(
    index: Int,
    content: @Composable (Modifier) -> Unit
) {
    val reduced = com.godson.vpn.ui.theme.LocalReducedMotion.current
    var appeared by remember { mutableStateOf(reduced) }
    LaunchedEffect(Unit) {
        if (!appeared) {
            delay(minOf(index, 8) * 35L)
            appeared = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(motionMillis(240)),
        label = "appear"
    )
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 22f
        }
    ) {
        content(Modifier)
    }
}

@Composable
private fun SearchField(
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
    val background =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surface
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(RadiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SortButton(active: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(RadiusPill),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Icon(
            Icons.Rounded.Sort,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.servers_sort_latency),
            style = MaterialTheme.typography.labelLarge,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun ServerCard(
    server: ServerInfo,
    isFavorite: Boolean,
    hapticEnabled: Boolean,
    pulseScore: Int? = null,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    GodsonCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagEmojiBadge(emoji = server.flagEmoji, size = 44.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(text = server.country, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = server.city + " · " + server.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    SignalBars(level = ServerFilter.signalLevel(server.pingMs))
                    Text(
                        text = stringResource(
                            if (server.available) R.string.server_available
                            else R.string.server_unavailable
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (server.available) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    pulseScore?.let { score ->
                        PulseScoreChip(score = score, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = server.pingMs?.let { "$it ms" }
                        ?: stringResource(R.string.value_unavailable),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (server.available) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = server.protocol,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.server_favorite_remove
                        else R.string.server_favorite_add
                    ),
                    tint = if (isFavorite) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyServersState(controls: ServersViewModel.Controls) {
    val (icon, titleRes, bodyRes) = when {
        controls.query.isNotBlank() -> Triple(
            Icons.Rounded.SearchOff,
            R.string.servers_empty_search_title,
            R.string.servers_empty_search_body
        )
        controls.category == ServerCategory.FAVORITES -> Triple(
            Icons.Rounded.FavoriteBorder,
            R.string.servers_empty_favorites_title,
            R.string.servers_empty_favorites_body
        )
        controls.category == ServerCategory.RECENT -> Triple(
            Icons.Rounded.History,
            R.string.servers_empty_recent_title,
            R.string.servers_empty_recent_body
        )
        else -> Triple(
            Icons.Rounded.Dns,
            R.string.servers_empty_search_title,
            R.string.servers_empty_search_body
        )
    }
    EmptyState(
        icon = icon,
        title = stringResource(titleRes),
        body = stringResource(bodyRes)
    )
}

/** Couleur officielle d'un niveau de qualité PULSE (toujours accompagnée du libellé texte). */
private fun pulseLevelColor(level: QualityLevel): Color = when (level) {
    QualityLevel.EXCELLENT -> SuccessGreen
    QualityLevel.GOOD -> CyberCyan
    QualityLevel.FAIR -> WarningAmber
    QualityLevel.POOR -> ErrorRed
}

/** Libellé localisé d'un niveau de qualité PULSE. */
@Composable
private fun qualityLabel(level: QualityLevel): String = stringResource(
    when (level) {
        QualityLevel.EXCELLENT -> R.string.pulse_quality_excellent
        QualityLevel.GOOD -> R.string.pulse_quality_good
        QualityLevel.FAIR -> R.string.pulse_quality_fair
        QualityLevel.POOR -> R.string.pulse_quality_poor
    }
)

/** Pastille de score PULSE d'un serveur (ex. « 95 · Excellente ») ; l'étiquette démo est portée par la ligne de synthèse. */
@Composable
private fun PulseScoreChip(score: Int, modifier: Modifier = Modifier) {
    val level = QualityLevel.ofScore(score)
    val color = pulseLevelColor(level)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$score · " + qualityLabel(level),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/**
 * Ligne de synthèse PULSE sous le compteur de serveurs. Statut DEMO_ONLY ⇒
 * mention explicite « démo » ; UNAVAILABLE ⇒ « indisponible », sans valeur inventée.
 */
@Composable
private fun PulseSummaryLine(
    report: PulseReport,
    recommendedCountry: String?,
    modifier: Modifier = Modifier
) {
    val reco = report.recommendation
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (reco != null) {
            Box(
                Modifier
                    .padding(end = 6.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(pulseLevelColor(reco.level))
            )
        }
        Text(
            text = when (report.status) {
                PulseStatus.UNAVAILABLE -> stringResource(R.string.pulse_summary_unavailable)
                PulseStatus.OK -> stringResource(
                    R.string.pulse_summary_real,
                    recommendedCountry ?: reco?.serverId ?: "",
                    reco?.score ?: 0,
                    reco?.let { qualityLabel(it.level) } ?: ""
                )
                PulseStatus.DEMO_ONLY -> stringResource(
                    R.string.pulse_summary_demo,
                    recommendedCountry ?: reco?.serverId ?: "",
                    reco?.score ?: 0,
                    reco?.let { qualityLabel(it.level) } ?: ""
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
