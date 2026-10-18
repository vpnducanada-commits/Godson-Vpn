package com.godson.vpn.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.ui.components.motionMillis
import com.godson.vpn.ui.theme.GlowCyan
import com.godson.vpn.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Écran de lancement — logo GV officiel, halo subtil, anneau.
 * Durée visuelle ~1,4 s MAXIMUM, jamais de délai artificiel supplémentaire.
 */
@Composable
fun SplashScreen(container: GodsonContainer, onFinished: (Boolean) -> Unit) {
    val reducedMotion = LocalReducedMotion.current

    LaunchedEffect(Unit) {
        val needsOnboarding = !container.settingsRepository.uiSettings
            .first().onboardingCompleted
        if (!reducedMotion) delay(1400) else delay(200)
        onFinished(needsOnboarding)
    }

    val breathing by rememberInfiniteTransition(label = "halo").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "haloAlpha"
    )
    val haloAlpha = if (reducedMotion) 0.85f else breathing

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(GlowCyan, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.42f),
                            radius = size.minDimension * 0.55f
                        ),
                        radius = size.minDimension * 0.55f,
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.42f)
                    )
                }
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(150.dp)
                        .alpha(haloAlpha * 0.6f)
                        .background(
                            brush = Brush.radialGradient(listOf(GlowCyan, Color.Transparent)),
                            shape = CircleShape
                        )
                )
                Box(
                    Modifier
                        .size(120.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_logo_gv),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(74.dp)
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(34.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .size(width = 120.dp, height = 3.dp)
                    .alpha(0.8f),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
