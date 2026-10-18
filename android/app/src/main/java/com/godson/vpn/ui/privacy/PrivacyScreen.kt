package com.godson.vpn.ui.privacy

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.ui.common.GodsonViewModelFactory
import com.godson.vpn.ui.components.GodsonCard
import com.godson.vpn.ui.components.SectionLabel
import com.godson.vpn.ui.components.StatusDot
import com.godson.vpn.ui.components.SubHeader
import com.godson.vpn.ui.components.statusTextRes
import com.godson.vpn.ui.settings.SettingsViewModel
import com.godson.vpn.vpn.VpnConnectionState

/**
 * Confidentialité : état réel du tunnel, protections disponibles (avec renvoi vers les
 * réglages système), données conservées et effacement local fonctionnel.
 */
@Composable
fun PrivacyScreen(container: GodsonContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = GodsonViewModelFactory(container))
    val state by container.vpnStateMachine.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPolicy by remember { mutableStateOf(false) }
    var askClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubHeader(
            title = stringResource(R.string.privacy_title),
            subtitle = stringResource(R.string.privacy_subtitle),
            onBack = onBack
        )

        // État du tunnel — toujours réel, jamais optimiste
        SectionLabel(
            stringResource(R.string.privacy_tunnel_state),
            Modifier.padding(horizontal = 20.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(state = state)
                Text(
                    text = stringResource(statusTextRes(state)),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }

        // Protections
        SectionLabel(
            stringResource(R.string.privacy_protections),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            ProtectionRow(
                icon = Icons.Rounded.Dns,
                title = stringResource(R.string.prot_dns),
                description = stringResource(R.string.prot_dns_desc)
            )
            ProtectionRow(
                icon = Icons.Rounded.AltRoute,
                title = stringResource(R.string.prot_routes),
                description = stringResource(R.string.prot_routes_desc)
            )
            ProtectionRow(
                icon = Icons.Rounded.CheckCircle,
                title = stringResource(R.string.prot_alwayson),
                description = stringResource(R.string.prot_alwayson_desc),
                available = true
            )
            ProtectionRow(
                icon = Icons.Rounded.Block,
                title = stringResource(R.string.prot_killswitch),
                description = stringResource(R.string.prot_killswitch_desc),
                available = true
            )
            OutlinedButton(
                onClick = { openVpnSettings(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.action_open_vpn_settings),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Données conservées
        SectionLabel(
            stringResource(R.string.privacy_data_title),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.privacy_data_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showPolicy = true }) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = stringResource(R.string.privacy_policy_action),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                TextButton(onClick = { askClear = true }) {
                    Text(
                        text = stringResource(R.string.privacy_clear_data),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showPolicy) {
        AlertDialog(
            onDismissRequest = { showPolicy = false },
            title = { Text(stringResource(R.string.privacy_policy_title)) },
            text = { Text(stringResource(R.string.privacy_policy_body)) },
            confirmButton = {
                TextButton(onClick = { showPolicy = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    if (askClear) {
        AlertDialog(
            onDismissRequest = { askClear = false },
            title = { Text(stringResource(R.string.privacy_clear_confirm_title)) },
            text = { Text(stringResource(R.string.privacy_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    askClear = false
                    vm.clearLocalData {
                        Toast.makeText(
                            context,
                            context.getString(R.string.privacy_cleared),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
private fun ProtectionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    available: Boolean = false
) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (available) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (available) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun openVpnSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
