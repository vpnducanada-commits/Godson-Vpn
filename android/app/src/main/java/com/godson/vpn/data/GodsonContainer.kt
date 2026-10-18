package com.godson.vpn.data

import com.godson.vpn.api.ApiClient
import com.godson.vpn.vpn.VpnController
import com.godson.vpn.vpn.VpnKeyManager
import com.godson.vpn.vpn.VpnStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Conteneur d'injection manuel (décision d'architecture §13).
 *
 * - Mode DÉMO (URL backend `.invalid`) : catalogue étiqueté, aucune requête réseau,
 *   aucune tentative de tunnel — l'UI affiche l'état honnête.
 * - Mode RÉEL : l'API et le contrôleur WireGuard sont actifs ; le tunnel ne démarre
 *   qu'après consentement VpnService, et « Connecté » n'apparaît qu'après vérification.
 */
class GodsonContainer(appContext: android.content.Context) {
    val appContext = appContext.applicationContext
    private val appScope = CoroutineScope(SupervisorJob())

    val settingsRepository = SettingsRepository(appContext)
    val vpnStateMachine = VpnStateMachine()
    val sessionStats = SessionStatsRepository(appScope, vpnStateMachine)

    // Couche réseau (active en mode RÉEL ; la construction reste inoffensive en démo)
    val api: com.godson.vpn.api.GodsonApi by lazy { ApiClient.create() }
    val keyManager = VpnKeyManager(appContext)
    val vpnController = VpnController(appContext, vpnStateMachine, api, keyManager)

    val serverRepository = ServerRepository(
        isRealMode = !AppMode.isDemo,
        api = api
    )

    // Sonde GODSON PULSE : déterministe et étiquetée DEMO. Une sonde réelle
    // (HttpProber, Phase backend) s'injectera ici sans changer l'UI.
    val pulseProber: com.godson.vpn.pulse.PulseProber = com.godson.vpn.pulse.DemoProber()

    init {
        vpnController.startMonitoring(appScope)
    }
}
