package com.godson.vpn.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.SplitTunnelPolicy
import com.godson.vpn.ui.common.GodsonViewModelFactory
import com.godson.vpn.ui.components.GodsonCard
import com.godson.vpn.ui.components.SubHeader
import com.godson.vpn.ui.theme.RadiusPill

/**
 * Écran « Applications » du tunnelage fractionné : interrupteur principal,
 * choix du mode (bloquer / autoriser), recherche et sélection réelle des
 * applications installées. En mode démo, les réglages sont persistés mais
 * l'effet n'a lieu qu'à la création d'un tunnel réel — c'est affiché.
 */
@Composable
fun SplitTunnelScreen(
    container: GodsonContainer,
    onBack: () -> Unit
) {
    val vm: SplitTunnelViewModel = viewModel(factory = GodsonViewModelFactory(container))
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val rows by vm.rows.collectAsStateWithLifecycle()
    val query by vm.queryState.collectAsStateWithLifecycle()
    val warn by vm.warnEmptyAllowlist.collectAsStateWithLifecycle()
    val selectedCount by vm.selectedCount.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SubHeader(
            title = stringResource(R.string.split_apps_title),
            subtitle = stringResource(R.string.split_pick_subtitle, selectedCount),
            onBack = onBack
        )

        // ── Interrupteur principal ───────────────────────────────────────
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.split_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.split_demo_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enabled, onCheckedChange = vm::setEnabled)
            }
        }

        // ── Choix du mode ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeChip(
                label = stringResource(R.string.split_mode_block),
                selected = mode == SplitTunnelPolicy.Mode.BLOCK,
                onClick = { vm.setMode(SplitTunnelPolicy.Mode.BLOCK) },
                modifier = Modifier.weight(1f)
            )
            ModeChip(
                label = stringResource(R.string.split_mode_allow),
                selected = mode == SplitTunnelPolicy.Mode.ALLOW,
                onClick = { vm.setMode(SplitTunnelPolicy.Mode.ALLOW) },
                modifier = Modifier.weight(1f)
            )
        }

        // ── Avertissement liste ALLOW vide ───────────────────────────────
        if (warn) {
            GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.split_allow_empty_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }

        // ── Recherche ────────────────────────────────────────────────────
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.split_apps_search)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // ── Liste réelle des applications ────────────────────────────────
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.split_apps_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp, vertical = 6.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows, key = { it.pkg }) { row ->
                    GodsonCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.toggle(row.pkg) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = row.label,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = row.pkg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(checked = row.selected, onCheckedChange = { vm.toggle(row.pkg) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surface
    val borderColor =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(RadiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
