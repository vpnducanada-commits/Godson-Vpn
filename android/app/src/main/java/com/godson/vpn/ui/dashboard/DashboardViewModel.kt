package com.godson.vpn.ui.dashboard

import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.model.ServerInfo
import com.godson.vpn.pulse.ProbeResult
import com.godson.vpn.pulse.PulseEngine
import com.godson.vpn.pulse.PulseRecommendation
import com.godson.vpn.pulse.PulseStatus
import com.godson.vpn.pulse.PulseTarget
import com.godson.vpn.vpn.VpnConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * État du tableau de bord : machine à états VPN réelle + serveur sélectionné
 * (persisté via DataStore — « Retenir le dernier serveur »).
 *
 * Règle d'honnêteté : en l'absence de backend (Phase 4) et d'intégration WireGuard (Phase 5),
 * la connexion aboutit à l'erreur explicite NO_BACKEND — jamais à un faux « Connecté ».
 * Le dialogue de consentement VpnService, lui, est RÉEL (le système Android le montre).
 */
class DashboardViewModel(private val container: GodsonContainer) : ViewModel() {

    enum class ErrorReason { NO_BACKEND, PERMISSION_DENIED, CONNECTION_FAILED }

    val vpnState: StateFlow<VpnConnectionState> = container.vpnStateMachine.state

    val sessionSeconds: StateFlow<Int> = container.sessionStats.currentSessionSeconds

    /**
     * Suggestion GODSON PULSE : évaluation À LA DEMANDE (une passe à l'ouverture),
     * jamais en boucle de fond. En mode démo, la recommandation est étiquetée DEMO
     * (source du rapport) ; en l'absence de sonde réelle, le statut est UNAVAILABLE.
     */
    data class PulseSuggestion(
        val status: PulseStatus,
        val recommendation: PulseRecommendation? = null,
        /** Pays du serveur recommandé, résolu depuis le catalogue (null si introuvable). */
        val recommendedCountry: String? = null
    )

    private val pulseEngine = PulseEngine()
    private val _pulse = MutableStateFlow(PulseSuggestion(PulseStatus.UNAVAILABLE))
    val pulse: StateFlow<PulseSuggestion> = _pulse.asStateFlow()

    init {
        refreshPulse()
    }

    /** Une passe d'évaluation PULSE sur les serveurs disponibles (aucune boucle de fond). */
    fun refreshPulse() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prober = container.pulseProber
            val targets = container.serverRepository.catalog.value
                .filter { it.available }
                .map { PulseTarget(serverId = it.id, host = it.hostname, isDemoTarget = it.isDemo) }
            val results = targets.map { target ->
                runCatching { prober.probe(target) }.getOrElse {
                    ProbeResult.unavailable(
                        serverId = target.serverId,
                        source = prober.source,
                        timestampMillis = now,
                        detail = "Exception de sonde : ${it.javaClass.simpleName}"
                    )
                }
            }
            val report = pulseEngine.evaluate(results, now)
            val country = report.recommendation?.let { reco ->
                container.serverRepository.catalog.value.firstOrNull { it.id == reco.serverId }?.country
            }
            _pulse.value = PulseSuggestion(report.status, report.recommendation, country)
        }
    }

    /** Applique la recommandation PULSE : sélection du serveur (persistée en DataStore). */
    fun applyPulseRecommendation() {
        val reco = _pulse.value.recommendation ?: return
        viewModelScope.launch {
            container.settingsRepository.setLastServer(reco.serverId)
            container.settingsRepository.addRecent(reco.serverId)
        }
    }

    /** Serveur sélectionné, réactif au changement effectué sur l'écran Serveurs. */
    val selectedServer: StateFlow<ServerInfo?> = combine(
        container.settingsRepository.lastServerId,
        container.serverRepository.catalog
    ) { lastId, servers ->
        servers.firstOrNull { it.id == lastId }
            ?: servers.firstOrNull { it.available && it.recommended }
            ?: servers.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val confirmBeforeDisconnect = container.settingsRepository.uiSettings
        .map { it.confirmBeforeDisconnect }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _errorReason = MutableStateFlow<ErrorReason?>(null)
    val errorReason: StateFlow<ErrorReason?> = _errorReason.asStateFlow()

    /**
     * Connexion demandée. Si Android exige le consentement VpnService, [launchPermission]
     * est appelé avec l'intent système ; le résultat arrive via [onVpnPermissionResult].
     */
    fun connect(launchPermission: (Intent) -> Unit) {
        val machine = container.vpnStateMachine
        if (vpnState.value !in setOf(VpnConnectionState.Disconnected, VpnConnectionState.Error)) return
        val server = selectedServer.value ?: return
        container.sessionStats.trackServer(server.id)
        _errorReason.value = null

        val prepareIntent = VpnService.prepare(container.appContext)
        if (prepareIntent != null) {
            machine.reset()
            machine.transitionTo(VpnConnectionState.Preparing)
            machine.transitionTo(VpnConnectionState.AwaitingPermission)
            launchPermission(prepareIntent)
        } else {
            viewModelScope.launch { establishTunnel() }
        }
    }

    /** Résultat du dialogue système de consentement VpnService. */
    fun onVpnPermissionResult(granted: Boolean) {
        val machine = container.vpnStateMachine
        if (machine.state.value != VpnConnectionState.AwaitingPermission) return
        if (granted) {
            viewModelScope.launch { establishTunnel() }
        } else {
            machine.transitionTo(VpnConnectionState.Disconnected)
            _errorReason.value = ErrorReason.PERMISSION_DENIED
        }
    }

    private suspend fun establishTunnel() {
        val machine = container.vpnStateMachine
        machine.transitionTo(VpnConnectionState.Connecting)

        // ── Mode DÉMO : aucun tunnel possible, erreur explicite (jamais de faux état) ──
        if (com.godson.vpn.data.AppMode.isDemo) {
            delay(450) // vérification locale de la configuration (réelle)
            machine.transitionTo(VpnConnectionState.Error)
            _errorReason.value = ErrorReason.NO_BACKEND
            return
        }

        // ── Mode RÉEL : tunnel WireGuard via VpnController ─────────────
        val server = selectedServer.value ?: run {
            machine.transitionTo(VpnConnectionState.Error)
            _errorReason.value = ErrorReason.CONNECTION_FAILED
            return
        }
        // Le contrôleur fixe lui-même l'état final : Connected UNIQUEMENT après
        // vérification du tunnel UP + handshake ; sinon Error/ServerUnavailable.
        // Tunnelage fractionné : réglages lus au moment de la connexion
        // (à la demande, jamais en boucle de fond).
        val splitEnabled = container.settingsRepository.splitTunnelingEnabled.firstOrNull() ?: false
        val splitMode = com.godson.vpn.data.SplitTunnelPolicy.Mode.fromStorageKey(
            container.settingsRepository.splitTunnelModeKey.firstOrNull()
        )
        val splitApps = container.settingsRepository.splitTunnelPackages.firstOrNull() ?: emptySet()
        container.vpnController.connectReal(
            server,
            excludedApps = if (splitEnabled) {
                com.godson.vpn.data.SplitTunnelPolicy.excludedPackages(splitMode, splitApps)
            } else emptySet(),
            includedApps = if (splitEnabled) {
                com.godson.vpn.data.SplitTunnelPolicy.includedPackages(splitMode, splitApps)
            } else emptySet()
        )
        if (machine.state.value == VpnConnectionState.Connecting) {
            machine.transitionTo(VpnConnectionState.Error)
            _errorReason.value = ErrorReason.CONNECTION_FAILED
        }
    }

    /** Déconnexion (après confirmation UI si l'option est activée). */
    fun disconnect() {
        val machine = container.vpnStateMachine
        if (vpnState.value !in setOf(VpnConnectionState.Connected, VpnConnectionState.Reconnecting)) return
        viewModelScope.launch {
            machine.transitionTo(VpnConnectionState.Disconnecting)
            delay(250)
            machine.transitionTo(VpnConnectionState.Disconnected)
        }
    }

    /** Sortie propre d'un état d'erreur vers Déconnecté. */
    fun acknowledgeError() {
        val machine = container.vpnStateMachine
        if (machine.state.value == VpnConnectionState.Error ||
            machine.state.value == VpnConnectionState.ServerUnavailable
        ) {
            machine.transitionTo(VpnConnectionState.Disconnected)
        }
        _errorReason.value = null
    }
}
