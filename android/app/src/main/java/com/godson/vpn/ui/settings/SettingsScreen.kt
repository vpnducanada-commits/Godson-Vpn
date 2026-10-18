package com.godson.vpn.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.ThemeMode
import com.godson.vpn.ui.common.GodsonViewModelFactory
import com.godson.vpn.ui.components.GodsonCard
import com.godson.vpn.ui.components.SectionLabel
import com.godson.vpn.ui.components.SettingRow
import com.godson.vpn.ui.components.SubHeader
import com.godson.vpn.ui.theme.RadiusPill
import com.godson.vpn.ui.theme.RadiusSmall
import androidx.compose.material3.Icon

/** Paramètres hiérarchisés : interrupteurs fonctionnels, persistance réelle, zéro bouton mort. */
@Composable
fun SettingsScreen(
    container: GodsonContainer,
    onOpenPrivacy: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSplitApps: () -> Unit
) {
    val vm: SettingsViewModel = viewModel(factory = GodsonViewModelFactory(container))
    val s by vm.settings.collectAsStateWithLifecycle()
    val splitCount by vm.splitPackageCount.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubHeader(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.tagline)
        )

        // ── Général ──────────────────────────────────────────────────────
        SectionLabel(stringResource(R.string.s_general), Modifier.padding(horizontal = 20.dp))
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SettingRow(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.set_theme),
                control = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemeOption(stringResource(R.string.theme_system), s.themeMode == ThemeMode.SYSTEM) {
                            vm.setTheme(ThemeMode.SYSTEM)
                        }
                        ThemeOption(stringResource(R.string.theme_dark), s.themeMode == ThemeMode.DARK) {
                            vm.setTheme(ThemeMode.DARK)
                        }
                        ThemeOption(stringResource(R.string.theme_light), s.themeMode == ThemeMode.LIGHT) {
                            vm.setTheme(ThemeMode.LIGHT)
                        }
                    }
                }
            )
            SettingRow(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.set_reduce_anim),
                description = stringResource(R.string.set_reduce_anim_desc),
                control = {
                    GodSwitch(checked = s.reduceAnimations, onCheckedChange = vm::setReduceAnimations)
                }
            )
            SettingRow(
                icon = Icons.Rounded.MusicNote,
                title = stringResource(R.string.set_haptic),
                description = stringResource(R.string.set_haptic_desc),
                control = { GodSwitch(checked = s.hapticFeedback, onCheckedChange = vm::setHaptic) }
            )
        }

        // ── Connexion ────────────────────────────────────────────────────
        SectionLabel(
            stringResource(R.string.s_connection),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SettingRow(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.set_confirm_disc),
                description = stringResource(R.string.set_confirm_disc_desc),
                control = {
                    GodSwitch(checked = s.confirmBeforeDisconnect, onCheckedChange = vm::setConfirmDisconnect)
                }
            )
            SettingRow(
                icon = Icons.Rounded.Memory,
                title = stringResource(R.string.set_keep_last),
                description = stringResource(R.string.set_keep_last_desc),
                control = {
                    GodSwitch(checked = s.keepLastServer, onCheckedChange = vm::setKeepLastServer)
                }
            )
            SettingRow(
                icon = Icons.Rounded.SwapVert,
                title = stringResource(R.string.set_auto_reconnect),
                description = stringResource(R.string.set_auto_reconnect_desc),
                control = {
                    GodSwitch(checked = s.autoReconnect, onCheckedChange = vm::setAutoReconnect)
                }
            )
            SettingRow(
                icon = Icons.Rounded.RestartAlt,
                title = stringResource(R.string.set_auto_connect),
                description = stringResource(R.string.set_auto_connect_desc),
                control = {
                    GodSwitch(checked = s.autoConnectOnStart, onCheckedChange = vm::setAutoConnect)
                }
            )
        }

        // ── Tunnelage fractionné ─────────────────────────────────────────
        SectionLabel(
            stringResource(R.string.s_split),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SettingRow(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.split_title),
                description = if (s.splitTunnelingEnabled) {
                    stringResource(R.string.split_pick_subtitle, splitCount)
                } else {
                    stringResource(R.string.split_desc)
                },
                onClick = onOpenSplitApps,
                control = { Chevron() }
            )
        }

        // ── Données et confidentialité ───────────────────────────────────
        SectionLabel(
            stringResource(R.string.s_data),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SettingRow(
                icon = Icons.Rounded.Fingerprint,
                title = stringResource(R.string.row_privacy),
                description = stringResource(R.string.row_privacy_desc),
                onClick = onOpenPrivacy,
                control = { Chevron() }
            )
        }

        // ── Assistance / Profil ──────────────────────────────────────────
        SectionLabel(
            stringResource(R.string.s_assistance),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SettingRow(
                icon = Icons.Rounded.ConfirmationNumber,
                title = stringResource(R.string.row_support),
                description = stringResource(R.string.row_support_desc),
                onClick = onOpenSupport,
                control = { Chevron() }
            )
            SettingRow(
                icon = Icons.Rounded.Person,
                title = stringResource(R.string.row_profile),
                description = stringResource(R.string.row_profile_desc),
                onClick = onOpenProfile,
                control = { Chevron() }
            )
        }

        // ── À propos ─────────────────────────────────────────────────────
        SectionLabel(
            stringResource(R.string.s_about),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            SettingRow(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.row_version),
                control = {
                    Text(
                        text = vm.versionName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            SettingRow(
                icon = Icons.Rounded.Link,
                title = stringResource(R.string.tagline),
                control = {}
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(RadiusPill)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(shape)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = shape
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun GodSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun Chevron() {
    Icon(
        Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
