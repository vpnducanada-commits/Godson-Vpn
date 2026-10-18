package com.godson.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godson.vpn.R
import com.godson.vpn.ui.theme.AccentGradient
import com.godson.vpn.ui.theme.LocalReducedMotion
import com.godson.vpn.ui.theme.RadiusLarge
import com.godson.vpn.ui.theme.RadiusMedium
import com.godson.vpn.ui.theme.RadiusPill
import com.godson.vpn.ui.theme.RadiusSmall
import com.godson.vpn.ui.theme.WarningAmber
import com.godson.vpn.vpn.VpnConnectionState

// ═══ Design system Godson VPN — composants réutilisables ═══════════════════════

/** Durée d'animation adaptée à l'option d'accessibilité « Réduire les animations ». */
@Composable
fun motionMillis(base: Int): Int = if (LocalReducedMotion.current) 0 else base

/** Carte de base : surface différenciée, contour fin, grand rayon. */
@Composable
fun GodsonCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(RadiusLarge)
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Card(
            modifier = modifier
                .pressScale(interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            content = content
        )
    }
}

/** Retour visuel au toucher (légère réduction), conforme §animations. */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(motionMillis(130)),
        label = "pressScale"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Indicateur de statut compact (état vérifié uniquement). */
@Composable
fun StatusDot(state: VpnConnectionState, modifier: Modifier = Modifier) {
    val color = stateColor(state)
    val animated by animateColorAsState(color, tween(motionMillis(300)), label = "dotColor")
    Box(
        modifier = modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(animated)
    )
}

/** Badge « DÉMO » — obligatoire dès qu'une donnée n'est pas réelle. */
@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusPill))
            .border(1.dp, WarningAmber.copy(alpha = 0.55f), RoundedCornerShape(RadiusPill))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.demo_badge),
            style = MaterialTheme.typography.labelSmall,
            color = WarningAmber,
            letterSpacing = 1.sp
        )
    }
}

/** Titre de section (interligne discret, majuscules espacées). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

/** Badge drapeau vectoriel : pastille dégradée avec code pays (jamais d'emoji). */
@Composable
fun FlagBadge(code: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(AccentGradient))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontSize = (size.value * 0.32f).sp
        )
    }
}

/** Barres de qualité du signal (4 niveaux, basées sur la latence réelle). */
@Composable
fun SignalBars(
    level: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.tertiary,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..4) {
            Box(
                Modifier
                    .size(width = 4.dp, height = (5 + i * 3).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= level) activeColor else inactiveColor)
            )
        }
    }
}

/** Alpha partagé des skeletons (un seul générateur par écran — performance). */
@Composable
fun rememberShimmerAlpha(): State<Float> {
    val transition = rememberInfiniteTransition(label = "shimmer")
    return transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
}

/** Carte fantôme de chargement (skeleton — jamais de spinner plein écran). */
@Composable
fun SkeletonCard(height: Dp, shimmer: State<Float>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(RadiusMedium))
            .alpha(shimmer.value)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
    )
}

/** État vide illustré (icône dans un anneau, titre, explication). */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(38.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp)
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** Ligne de paramètres réutilisable (icône + titre + description + contrôle). */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    control: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .pressScale(interaction, 0.985f)
                        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(RadiusSmall))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        control?.invoke()
    }
}

/** En-tête des écrans secondaires (titre, sous-titre, retour, actions). */
@Composable
fun SubHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

/** Couleur sémantique d'un état de connexion. */
@Composable
fun stateColor(state: VpnConnectionState): Color = when (state) {
    VpnConnectionState.Connected -> MaterialTheme.colorScheme.tertiary
    VpnConnectionState.Error -> MaterialTheme.colorScheme.error
    VpnConnectionState.ServerUnavailable -> WarningAmber
    VpnConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.secondary
}

/** Texte de statut principal associé à un état (ressources, jamais codé en dur). */
fun statusTextRes(state: VpnConnectionState): Int = when (state) {
    VpnConnectionState.Disconnected -> R.string.status_disconnected
    VpnConnectionState.Preparing -> R.string.status_preparing
    VpnConnectionState.AwaitingPermission -> R.string.status_awaiting_permission
    VpnConnectionState.Connecting -> R.string.status_connecting
    VpnConnectionState.Connected -> R.string.status_connected
    VpnConnectionState.Reconnecting -> R.string.status_reconnecting
    VpnConnectionState.Disconnecting -> R.string.status_disconnecting
    VpnConnectionState.Error -> R.string.status_error
    VpnConnectionState.ServerUnavailable -> R.string.status_server_unavailable
}

/**
 * Drapeau emoji dans une pastille — repli lisible (surface + contour) sur les
 * appareils qui ne rendent pas les emoji drapeaux. Préféré au bitmap pour
 * couvrir 30 pays sans alourdir l'APK.
 */
@Composable
fun FlagEmojiBadge(emoji: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = (size.value * 0.52f).sp,
            textAlign = TextAlign.Center
        )
    }
}

/** Couleur officielle d'un niveau de qualité PULSE (toujours accompagnée du libellé texte). */
fun pulseLevelColor(level: com.godson.vpn.pulse.QualityLevel): androidx.compose.ui.graphics.Color =
    when (level) {
        com.godson.vpn.pulse.QualityLevel.EXCELLENT -> com.godson.vpn.ui.theme.SuccessGreen
        com.godson.vpn.pulse.QualityLevel.GOOD -> com.godson.vpn.ui.theme.CyberCyan
        com.godson.vpn.pulse.QualityLevel.FAIR -> com.godson.vpn.ui.theme.WarningAmber
        com.godson.vpn.pulse.QualityLevel.POOR -> com.godson.vpn.ui.theme.ErrorRed
    }

/** Ressource du libellé localisé d'un niveau de qualité PULSE (afficher via stringResource). */
fun qualityLabelRes(level: com.godson.vpn.pulse.QualityLevel): Int = when (level) {
    com.godson.vpn.pulse.QualityLevel.EXCELLENT -> com.godson.vpn.R.string.pulse_quality_excellent
    com.godson.vpn.pulse.QualityLevel.GOOD -> com.godson.vpn.R.string.pulse_quality_good
    com.godson.vpn.pulse.QualityLevel.FAIR -> com.godson.vpn.R.string.pulse_quality_fair
    com.godson.vpn.pulse.QualityLevel.POOR -> com.godson.vpn.R.string.pulse_quality_poor
}
