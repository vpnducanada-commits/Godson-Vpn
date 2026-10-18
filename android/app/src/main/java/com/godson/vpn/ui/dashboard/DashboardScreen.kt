package com.godson.vpn.ui.dashboard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.ServerFilter
import com.godson.vpn.ui.common.Formatting
import com.godson.vpn.ui.common.GodsonViewModelFactory
import com.godson.vpn.ui.components.DemoBadge
import com.godson.vpn.ui.components.FlagEmojiBadge
import com.godson.vpn.ui.components.GodsonCard
import com.godson.vpn.pulse.PulseStatus
import com.godson.vpn.ui.components.pulseLevelColor
import com.godson.vpn.ui.components.qualityLabelRes
import com.godson.vpn.ui.components.SignalBars
import com.godson.vpn.ui.components.StatusDot
import com.godson.vpn.ui.components.motionMillis
import com.godson.vpn.ui.components.pressScale
import com.godson.vpn.ui.components.stateColor
import com.godson.vpn.ui.components.statusTextRes
import com.godson.vpn.ui.theme.GlowCyan
import com.godson.vpn.ui.theme.GlowGreen
import com.godson.vpn.ui.theme.LocalReducedMotion
import com.godson.vpn.ui.theme.RadiusMedium
import com.godson.vpn.ui.theme.SlateGray
import com.godson.vpn.vpn.VpnConnectionState
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Tableau de bord premium : en-tête de marque, zone centrale à anneaux concentriques,
 * statut de sécurité réel, carte serveur et indicateurs compacts.
 */
@Composable
fun DashboardScreen(
    container: GodsonContainer,
    onOpenCountryPicker: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val vm: DashboardViewModel = viewModel(factory = GodsonViewModelFactory(container))
    val state by vm.vpnState.collectAsStateWithLifecycle()
    val server by vm.selectedServer.collectAsStateWithLifecycle()
    val sessionSeconds by vm.sessionSeconds.collectAsStateWithLifecycle()
    val confirmDisc by vm.confirmBeforeDisconnect.collectAsStateWithLifecycle()
    val errorReason by vm.errorReason.collectAsStateWithLifecycle()
    val pulse by vm.pulse.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vm.onVpnPermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    var askDisconnect by remember { mutableStateOf(false) }
    val onConnectAction = { vm.connect { intent -> permissionLauncher.launch(intent) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── En-tête de marque ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_gv),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 2.4.sp,
                modifier = Modifier.padding(start = 10.dp)
            )
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                StatusDot(state = state)
                Text(
                    text = if (state == VpnConnectionState.Connected) stringResource(R.string.tunnel_up)
                    else stringResource(R.string.tunnel_down),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onOpenProfile) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = stringResource(R.string.row_profile),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Zone centrale : bouton + statut de sécurité réel ─────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(336.dp),
            contentAlignment = Alignment.Center
        ) {
            NetworkBackdrop()
            ConnectButton(
                state = state,
                onConnect = onConnectAction,
                onDisconnect = {
                    if (confirmDisc) askDisconnect = true else vm.disconnect()
                }
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val statusFadeIn = motionMillis(220)
                val statusFadeOut = motionMillis(150)
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        fadeIn(tween(statusFadeIn)) togetherWith fadeOut(tween(statusFadeOut))
                    },
                    label = "statusText"
                ) { s ->
                    Text(
                        text = stringResource(statusTextRes(s)),
                        style = MaterialTheme.typography.titleMedium,
                        color = stateColor(s),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
                Text(
                    text = stringResource(
                        if (state == VpnConnectionState.Connected) R.string.status_hint_connected
                        else R.string.status_hint_disconnected
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, start = 36.dp, end = 36.dp)
                )
            }
        }

        // ── Message d'erreur explicite + réessai ─────────────────────────
        if (state == VpnConnectionState.Error || state == VpnConnectionState.ServerUnavailable) {
            GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = when (errorReason) {
                                DashboardViewModel.ErrorReason.PERMISSION_DENIED ->
                                    stringResource(R.string.error_permission_denied)
                                else -> stringResource(R.string.error_no_backend_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                    if (errorReason == DashboardViewModel.ErrorReason.NO_BACKEND) {
                        Text(
                            text = stringResource(R.string.error_no_backend_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (errorReason == DashboardViewModel.ErrorReason.CONNECTION_FAILED) {
                        Text(
                            text = stringResource(R.string.error_connection_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(onClick = {
                            vm.acknowledgeError()
                            onConnectAction()
                        }) { Text(stringResource(R.string.action_retry)) }
                        OutlinedButton(onClick = { vm.acknowledgeError() }) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                }
            }
        }

        // ── Connexion rapide ─────────────────────────────────────────────
        if (state == VpnConnectionState.Disconnected || state == VpnConnectionState.Error) {
            QuickConnectButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                onClick = onConnectAction
            )
        }

        // ── Carte du serveur sélectionné ─────────────────────────────────
        server?.let { s ->
            GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.selected_server).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.weight(1f))
                        DemoBadge()
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlagEmojiBadge(emoji = s.flagEmoji, size = 46.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "${s.country} — ${s.city}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${s.id} · ${s.protocol}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = s.pingMs?.let { "$it ms" }
                                    ?: stringResource(R.string.value_unavailable),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                SignalBars(level = ServerFilter.signalLevel(s.pingMs))
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (s.available) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.error
                                        )
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = onOpenCountryPicker,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Public,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(16.dp)
                        )
                        Text(stringResource(R.string.action_choose_country))
                        Icon(
                            Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
        }

        // ── Suggestion GODSON PULSE (démo étiquetée ; une passe à l'ouverture) ──
        GodsonCard(modifier = Modifier.padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GODSON PULSE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.weight(1f))
                    DemoBadge()
                }
                Spacer(Modifier.height(10.dp))
                val reco = pulse.recommendation
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (reco != null) pulseLevelColor(reco.level)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                    )
                    Text(
                        text = when (pulse.status) {
                            PulseStatus.UNAVAILABLE ->
                                stringResource(R.string.pulse_summary_unavailable)
                            PulseStatus.OK -> stringResource(
                                R.string.pulse_summary_real,
                                pulse.recommendedCountry ?: reco?.serverId ?: "",
                                reco?.score ?: 0,
                                reco?.let { stringResource(qualityLabelRes(it.level)) } ?: ""
                            )
                            PulseStatus.DEMO_ONLY -> stringResource(
                                R.string.pulse_summary_demo,
                                pulse.recommendedCountry ?: reco?.serverId ?: "",
                                reco?.score ?: 0,
                                reco?.let { stringResource(qualityLabelRes(it.level)) } ?: ""
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (pulse.status != PulseStatus.UNAVAILABLE && reco != null && reco.serverId != server?.id) {
                    TextButton(
                        onClick = vm::applyPulseRecommendation,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(16.dp)
                        )
                        Text(stringResource(R.string.pulse_action_use))
                    }
                }
            }
        }

        // ── Indicateurs compacts ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickStat(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stat_session),
                value = if (state == VpnConnectionState.Connected) {
                    Formatting.duration(sessionSeconds)
                } else "—"
            )
            QuickStat(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stat_down),
                value = stringResource(R.string.value_unavailable),
                small = true
            )
            QuickStat(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.stat_up),
                value = stringResource(R.string.value_unavailable),
                small = true
            )
        }

        Text(
            text = stringResource(R.string.demo_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(12.dp))
    }

    if (askDisconnect) {
        AlertDialog(
            onDismissRequest = { askDisconnect = false },
            title = { Text(stringResource(R.string.disconnect_confirm_title)) },
            text = { Text(stringResource(R.string.disconnect_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    askDisconnect = false
                    vm.disconnect()
                }) { Text(stringResource(R.string.disconnect_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { askDisconnect = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun QuickStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    small: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(RadiusMedium))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = if (small) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                color = if (small) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun QuickConnectButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .height(52.dp)
            .clip(RoundedCornerShape(RadiusMedium))
            .background(Brush.linearGradient(com.godson.vpn.ui.theme.ActionGradient))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.action_quick_connect),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** Fond technologique discret : nœuds reliés + deux halos (statique, économique). */
@Composable
private fun NetworkBackdrop() {
    val nodes = remember {
        listOf(
            0.10f to 0.16f, 0.26f to 0.36f, 0.06f to 0.58f, 0.20f to 0.84f,
            0.90f to 0.14f, 0.76f to 0.40f, 0.94f to 0.62f, 0.80f to 0.86f
        )
    }
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GlowCyan, Color.Transparent),
                center = Offset(size.width * 0.12f, size.height * 0.10f),
                radius = size.width * 0.42f
            ),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.12f, size.height * 0.10f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GlowGreen, Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.85f),
                radius = size.width * 0.36f
            ),
            radius = size.width * 0.36f,
            center = Offset(size.width * 0.92f, size.height * 0.85f)
        )
        val points = nodes.map { Offset(it.first * size.width, it.second * size.height) }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = SlateGray.copy(alpha = 0.10f),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 1f
            )
        }
        points.forEach { p ->
            drawCircle(color = SlateGray.copy(alpha = 0.22f), radius = 2.dp.toPx(), center = p)
        }
    }
}

private val busyStates = setOf(
    VpnConnectionState.Preparing,
    VpnConnectionState.AwaitingPermission,
    VpnConnectionState.Connecting,
    VpnConnectionState.Reconnecting,
    VpnConnectionState.Disconnecting
)

/** Grand bouton circulaire à anneaux concentriques, avec états animés distincts. */
@Composable
private fun ConnectButton(
    state: VpnConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val color = stateColor(state)
    val connected = state == VpnConnectionState.Connected
    val busy = state in busyStates

    Box(modifier = Modifier.size(252.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(252.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(if (connected) GlowGreen else GlowCyan, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            listOf(0.98f to 0.10f, 0.83f to 0.16f, 0.68f to 0.22f).forEach { (r, a) ->
                drawCircle(
                    color = color.copy(alpha = a),
                    radius = radius * r,
                    style = Stroke(width = 1.1.dp.toPx())
                )
            }
        }
        if (connected) BreathingGlow()
        if (busy && state != VpnConnectionState.AwaitingPermission) {
            RotatingArc(diameter = 212.dp, color = color)
        }

        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = tween(motionMillis(130)),
            label = "btnScale"
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(1.4.dp, color.copy(alpha = 0.55f), CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = !busy,
                    onClick = { if (connected) onDisconnect() else onConnect() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = buttonIcon(state),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = stringResource(buttonLabelRes(state)),
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 18.dp, end = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun BreathingGlow() {
    val reduced = LocalReducedMotion.current
    val alpha by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "breathAlpha"
    )
    Canvas(Modifier.size(252.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GlowGreen.copy(alpha = if (reduced) 0.5f else alpha), Color.Transparent),
                center = center,
                radius = size.minDimension / 2f
            ),
            radius = size.minDimension / 2f,
            center = center
        )
    }
}

@Composable
private fun RotatingArc(diameter: androidx.compose.ui.unit.Dp, color: Color) {
    val angle by rememberInfiniteTransition(label = "arc").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(motionMillis(1500), easing = LinearEasing)),
        label = "angle"
    )
    Canvas(Modifier.size(diameter)) {
        drawCircle(color = color.copy(alpha = 0.15f), style = Stroke(width = 2.6.dp.toPx()))
        rotate(degrees = angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 95f,
                useCenter = false,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

private fun buttonIcon(state: VpnConnectionState): ImageVector = when (state) {
    VpnConnectionState.Connected -> Icons.Rounded.Shield
    VpnConnectionState.Error -> Icons.Rounded.WarningAmber
    VpnConnectionState.ServerUnavailable -> Icons.Rounded.Wifi
    else -> Icons.Rounded.Bolt
}

private fun buttonLabelRes(state: VpnConnectionState): Int = when (state) {
    VpnConnectionState.Connected -> R.string.action_disconnect
    VpnConnectionState.Error -> R.string.action_retry
    VpnConnectionState.ServerUnavailable -> R.string.action_retry
    else -> statusTextRes(state)
}
