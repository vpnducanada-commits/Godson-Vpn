package com.godson.vpn.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.SessionRecord
import com.godson.vpn.ui.common.Formatting
import com.godson.vpn.ui.common.GodsonViewModelFactory
import com.godson.vpn.ui.components.EmptyState
import com.godson.vpn.ui.components.FlagEmojiBadge
import com.godson.vpn.ui.components.GodsonCard
import com.godson.vpn.ui.components.SubHeader
import com.godson.vpn.ui.theme.AccentGradient
import com.godson.vpn.ui.theme.RadiusMedium
import com.godson.vpn.ui.theme.RadiusSmall
import com.godson.vpn.vpn.VpnConnectionState

/** Statistiques : session en cours, chronologie des durées réelles, historique effaçable. */
@Composable
fun StatsScreen(container: GodsonContainer, onBack: () -> Unit) {
    val vm: StatsViewModel = viewModel(factory = GodsonViewModelFactory(container))
    val history by vm.history.collectAsStateWithLifecycle()
    val currentSeconds by vm.currentSeconds.collectAsStateWithLifecycle()
    val state by vm.vpnState.collectAsStateWithLifecycle()
    val serverById by vm.serverById.collectAsStateWithLifecycle()

    var askClear by remember { mutableStateOf(false) }
    val connected = state == VpnConnectionState.Connected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubHeader(
            title = stringResource(R.string.stats_title),
            subtitle = stringResource(R.string.stats_subtitle),
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stats_current_session),
                value = if (connected) Formatting.duration(currentSeconds)
                else stringResource(R.string.stats_not_connected),
                highlight = connected
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stat_down),
                value = stringResource(R.string.value_unavailable),
                small = true
            )
            StatTile(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stat_up),
                value = stringResource(R.string.value_unavailable),
                small = true
            )
        }

        Text(
            text = stringResource(R.string.stats_traffic_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        // Chronologie des sessions réelles (durées mesurées localement)
        com.godson.vpn.ui.components.SectionLabel(
            text = stringResource(R.string.stats_timeline),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)
        )
        if (history.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Insights,
                title = stringResource(R.string.stats_timeline),
                body = stringResource(R.string.stats_timeline_empty)
            )
        } else {
            DurationChart(
                history = history.takeLast(12),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.forEach { record ->
                    val server = serverById[record.serverId]
                    GodsonCard {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlagEmojiBadge(
                                emoji = server?.flagEmoji ?: "🌐",
                                size = 36.dp
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = server?.let { "${it.country} — ${it.city}" }
                                        ?: record.serverId,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = Formatting.shortDateTime(record.startedAtMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = Formatting.duration(record.durationSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { askClear = true }, enabled = history.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.stats_clear),
                    color = if (history.isNotEmpty()) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = stringResource(R.string.stats_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )
        Spacer(Modifier.height(16.dp))
    }

    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            title = { Text(stringResource(R.string.stats_clear_confirm_title)) },
            text = { Text(stringResource(R.string.stats_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    askClear = false
                    vm.clearHistory()
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { askClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    small: Boolean = false
) {
    val shape = RoundedCornerShape(RadiusMedium)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                brush = if (highlight) {
                    Brush.linearGradient(AccentGradient.map { it.copy(alpha = 0.16f) })
                } else {
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                },
                shape = shape
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (small) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleLarge,
            color = if (highlight) MaterialTheme.colorScheme.tertiary
            else if (small) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Graphique en barres des durées réelles de session (aucune donnée fictive). */
@Composable
private fun DurationChart(history: List<SessionRecord>, modifier: Modifier = Modifier) {
    val maxSeconds = history.maxOfOrNull { it.durationSeconds }?.coerceAtLeast(1) ?: 1
    Column(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val barSpace = size.width / history.size
                val barWidth = barSpace * 0.45f
                history.forEachIndexed { index, record ->
                    val h = (record.durationSeconds.toFloat() / maxSeconds) *
                        (size.height * 0.86f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(AccentGradient),
                        topLeft = Offset(index * barSpace + (barSpace - barWidth) / 2, size.height - h),
                        size = androidx.compose.ui.geometry.Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, size.height - 1f),
                    end = Offset(size.width, size.height - 1f),
                    strokeWidth = 2f
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = history.firstOrNull()?.let { Formatting.shortDateTime(it.startedAtMillis) } ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = history.lastOrNull()?.let { Formatting.shortDateTime(it.startedAtMillis) } ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
