package com.godson.vpn.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.ui.components.GodsonCard
import com.godson.vpn.ui.components.SectionLabel
import com.godson.vpn.ui.components.SubHeader
import kotlinx.coroutines.launch

/**
 * Profil — mode sans compte (décision d'architecture) : identifiant d'appareil
 * anonyme généré localement, affiché masqué, copiable et régénérable.
 */
@Composable
fun ProfileScreen(container: GodsonContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var deviceId by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        deviceId = container.settingsRepository.getOrCreateDeviceId()
    }

    fun maskedId(): String = deviceId?.let { id ->
        if (id.length > 12) id.take(8) + "…" + id.takeLast(4) else id
    } ?: "…"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubHeader(
            title = stringResource(R.string.profile_title),
            onBack = onBack
        )

        SectionLabel(
            stringResource(R.string.profile_no_account_title),
            Modifier.padding(horizontal = 20.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(30.dp)
                )
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        text = stringResource(R.string.profile_no_account_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.profile_no_account_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // Identifiant d'appareil local
        SectionLabel(
            stringResource(R.string.profile_device_id),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = maskedId(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(modifier = Modifier.padding(top = 10.dp)) {
                    TextButton(onClick = {
                        deviceId?.let {
                            clipboard.setText(AnnotatedString(it))
                            Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.action_copy),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    TextButton(onClick = {
                        scope.launch {
                            container.settingsRepository.resetDeviceId()
                            refreshKey++
                            Toast.makeText(
                                context,
                                context.getString(R.string.profile_reset_done),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.profile_reset),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        // Transparence sur l'évolution
        SectionLabel(
            stringResource(R.string.profile_future_title),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.profile_future_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}
