package com.godson.vpn.ui.support

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.QuestionAnswer
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.godson.vpn.BuildConfig
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.ui.components.GodsonCard
import com.godson.vpn.ui.components.SectionLabel
import com.godson.vpn.ui.components.SubHeader
import com.godson.vpn.ui.components.statusTextRes
import com.godson.vpn.vpn.VpnConnectionState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Assistance : FAQ dépliable, diagnostic réel (appareil, réseau, état du tunnel), signalement. */
@Composable
fun SupportScreen(container: GodsonContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val state by container.vpnStateMachine.state.collectAsStateWithLifecycle()

    var showReport by remember { mutableStateOf(false) }

    val diagnostic = remember(state) { buildDiagnostic(context, state) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SubHeader(
            title = stringResource(R.string.support_title),
            subtitle = stringResource(R.string.support_subtitle),
            onBack = onBack
        )

        // ── FAQ ──────────────────────────────────────────────────────────
        SectionLabel(
            stringResource(R.string.faq_title),
            Modifier.padding(horizontal = 20.dp)
        )
        val faqItems = listOf(
            R.string.faq1_q to R.string.faq1_a,
            R.string.faq2_q to R.string.faq2_a,
            R.string.faq3_q to R.string.faq3_a,
            R.string.faq4_q to R.string.faq4_a,
            R.string.faq5_q to R.string.faq5_a
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            faqItems.forEachIndexed { index, (questionRes, answerRes) ->
                FaqCard(
                    question = stringResource(questionRes),
                    answer = stringResource(answerRes),
                    initialOpen = index == 0
                )
            }
        }

        // ── Diagnostic ───────────────────────────────────────────────────
        SectionLabel(
            stringResource(R.string.diag_title),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp)) {
                DiagRow(stringResource(R.string.diag_app), BuildConfig.VERSION_NAME)
                DiagRow(stringResource(R.string.diag_device), Build.MODEL ?: "?")
                DiagRow(stringResource(R.string.diag_android), "API ${Build.VERSION.SDK_INT}")
                DiagRow(
                    stringResource(R.string.diag_network),
                    stringResource(if (isOnline(context)) R.string.diag_online else R.string.diag_offline)
                )
                DiagRow(
                    stringResource(R.string.diag_vpn_state),
                    stringResource(statusTextRes(state))
                )
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(diagnostic))
                        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.MonitorHeart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.diag_copy),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // ── Signalement ──────────────────────────────────────────────────
        SectionLabel(
            stringResource(R.string.report_title),
            Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)
        )
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.report_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showReport = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Rounded.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.report_title),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            icon = { Icon(Icons.Rounded.QuestionAnswer, contentDescription = null) },
            title = { Text(stringResource(R.string.report_title)) },
            text = { Text(stringResource(R.string.report_body)) },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(diagnostic))
                    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                    showReport = false
                }) { Text(stringResource(R.string.diag_copy)) }
            },
            dismissButton = {
                TextButton(onClick = { showReport = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun FaqCard(question: String, answer: String, initialOpen: Boolean) {
    var open by rememberSaveable { mutableStateOf(initialOpen) }
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(220),
        label = "faqArrow"
    )
    GodsonCard {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = question, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation)
                )
            }
            AnimatedVisibility(
                visible = open,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Vérification réseau RÉELLE via ConnectivityManager. */
private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun buildDiagnostic(context: Context, state: VpnConnectionState): String =
    buildString {
        appendLine("Godson VPN — diagnostic (données non sensibles)")
        appendLine("Version : ${BuildConfig.VERSION_NAME}")
        appendLine("Appareil : ${Build.MODEL} — Android API ${Build.VERSION.SDK_INT}")
        appendLine("État du tunnel : $state")
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        appendLine(
            "Réseau : " + when {
                caps == null -> "inconnu"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Données mobiles"
                else -> "autre"
            }
        )
    }
