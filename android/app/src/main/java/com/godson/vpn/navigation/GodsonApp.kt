package com.godson.vpn.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.godson.vpn.R
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.UiSettings
import com.godson.vpn.ui.components.motionMillis
import com.godson.vpn.ui.country.CountryPickerScreen
import com.godson.vpn.ui.dashboard.DashboardScreen
import com.godson.vpn.ui.privacy.PrivacyScreen
import com.godson.vpn.ui.profile.ProfileScreen
import com.godson.vpn.ui.servers.ServersScreen
import com.godson.vpn.ui.settings.SettingsScreen
import com.godson.vpn.ui.screens.OnboardingScreen
import com.godson.vpn.ui.screens.SplashScreen
import com.godson.vpn.ui.stats.StatsScreen
import com.godson.vpn.ui.support.SupportScreen
import com.godson.vpn.ui.theme.RadiusPill
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SERVERS = "servers"
    const val COUNTRY_PICKER = "country_picker"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val SUPPORT = "support"
    const val PROFILE = "profile"
}

private data class BottomItem(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
fun GodsonApp(container: GodsonContainer, settings: UiSettings) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val bottomItems = listOf(
        BottomItem(Routes.DASHBOARD, R.string.nav_home, Icons.Rounded.Home),
        BottomItem(Routes.SERVERS, R.string.nav_servers, Icons.Rounded.Dns),
        BottomItem(Routes.STATS, R.string.nav_stats, Icons.Rounded.BarChart),
        BottomItem(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings)
    )
    val bottomRoutes = bottomItems.map { it.route }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Transitions partagées : fondu + déplacement léger, 180-280 ms, respect de l'accessibilité.
    val enterAnim = if (!settings.reduceAnimations) {
        fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 24 }
    } else {
        fadeIn(tween(90))
    }
    val exitAnim = fadeOut(tween(180))
    val popExitAnim = if (!settings.reduceAnimations) {
        fadeOut(tween(200)) + slideOutVertically(tween(240)) { it / 24 }
    } else {
        fadeOut(tween(90))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in bottomRoutes) {
                GodsonBottomBar(
                    currentRoute = currentRoute ?: Routes.DASHBOARD,
                    items = bottomItems,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding),
            enterTransition = { enterAnim },
            exitTransition = { exitAnim },
            popEnterTransition = { enterAnim },
            popExitTransition = { popExitAnim }
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    container = container,
                    onFinished = { needsOnboarding ->
                        val target = if (needsOnboarding) Routes.ONBOARDING else Routes.DASHBOARD
                        navController.navigate(target) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onDone = {
                        scope.launch { container.settingsRepository.setOnboardingCompleted() }
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    container = container,
                    onOpenCountryPicker = { navController.navigate(Routes.COUNTRY_PICKER) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.COUNTRY_PICKER) {
                CountryPickerScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(Routes.SERVERS) {
                ServersScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(Routes.STATS) {
                StatsScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onOpenSupport = { navController.navigate(Routes.SUPPORT) },
                    onOpenProfile = { navController.navigate(Routes.PROFILE) }
                )
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(Routes.SUPPORT) {
                SupportScreenImpl(container = container, onBack = { navController.popBackStack() })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(container = container, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun SupportScreenImpl(container: GodsonContainer, onBack: () -> Unit) {
    SupportScreen(container = container, onBack = onBack)
}

/** Barre inférieure redessinée : indicateur discret, couleurs animées, zones tactiles larges. */
@Composable
private fun GodsonBottomBar(
    currentRoute: String,
    items: List<BottomItem>,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
            }
            .padding(vertical = 8.dp)
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            val tint by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(motionMillis(220)),
                label = "navTint"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(item.route) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(width = 22.dp, height = 3.dp)
                        .alpha(if (selected) 1f else 0f)
                        .clip(RoundedCornerShape(RadiusPill))
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Icon(
                    imageVector = item.icon,
                    contentDescription = stringResource(item.labelRes),
                    tint = tint,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = stringResource(item.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}
