package com.godson.vpn.vpn

import android.content.Context
import android.util.Log
import com.godson.vpn.api.GodsonApi
import com.godson.vpn.api.VpnSessionRequest
import com.godson.vpn.data.model.ServerInfo
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import com.wireguard.crypto.Key

/** Un tunnel est jugé vivant si un handshake date de moins de 3 minutes (API Statistics.peer). */
private const val HANDSHAKE_FRESH_MS = 180_000L

/** Mesures réelles du tunnel (compteurs de la librairie WireGuard — jamais simulées). */
data class TunnelStats(
    val rxBytes: Long,
    val txBytes: Long,
    val tunnelAlive: Boolean
)

/** Tunnel nommé géré par le backend Go. */
private class GodsonTunnel(private val tunnelName: String) : Tunnel {
    override fun getName(): String = tunnelName
    override fun onStateChange(newState: Tunnel.State) {
        Log.i(TAG, "État du tunnel $tunnelName → $newState")
    }

    companion object {
        private const val TAG = "GodsonTunnel"
    }
}

/**
 * Contrôleur du tunnel VPN RÉEL (mode REAL uniquement — voir AppMode).
 *
 * Cycle de connexion :
 *   1. clés de l'appareil (générées localement, clé privée jamais transmise) ;
 *   2. POST /api/v1/vpn/session → configuration éphémère ;
 *   3. assemblage de la config WireGuard officielle ;
 *   4. GoBackend.setState(UP) via VpnService (consentement préalable obligatoire) ;
 *   5. vérification : le tunnel est considéré établi (Connected) uniquement quand
 *      le backend le rapporte UP ET que le tunnel est « alive » (handshake récent).
 *
 * Journalisation : état et erreurs uniquement — jamais de clé, jamais de contenu.
 */
class VpnController(
    private val context: Context,
    private val stateMachine: VpnStateMachine,
    private val api: GodsonApi,
    private val keyManager: VpnKeyManager
) {
    companion object {
        private const val TAG = "VpnController"
        /** Sans handshake pendant cette durée, le tunnel est jugé interrompu. */
        private const val DEAD_TUNNEL_MS = 180_000L
        private const val MONITOR_INTERVAL_MS = 1000L
    }

    private val backend by lazy { GoBackend(context) }

    /** Clé publique du serveur de la session active (pour lire les statistiques par pair). */
    private var activeSessionServerKey: String? = null

    /** Paquet de l'app elle-même (défini par le ViewModel, réel en debug comme en release). */
    var selfPackageForSplit: String? = null
    private var activeTunnel: GodsonTunnel? = null
    private var activeSessionId: String? = null

    private val _stats = MutableStateFlow<TunnelStats?>(null)
    val stats: StateFlow<TunnelStats?> = _stats.asStateFlow()

    /** Démarre le tunnel réel. À appeler UNIQUEMENT après consentement VpnService. */
    suspend fun connectReal(
        server: ServerInfo,
        excludedApps: Set<String> = emptySet(),
        includedApps: Set<String> = emptySet()
    ) {
        if (stateMachine.state.value != VpnConnectionState.Connecting &&
            stateMachine.state.value != VpnConnectionState.Reconnecting
        ) {
            return
        }
        try {
            val keyPair = keyManager.getOrCreateKeyPair()
            val session = api.createVpnSession(
                VpnSessionRequest(
                    serverId = server.id,
                    clientPublicKey = keyPair.publicKey.toBase64()
                )
            )
            activeSessionId = session.sessionId
            activeSessionServerKey = session.serverPublicKey

            val config = TunnelConfigMapper.build(
                keyPair, session,
                excludedApps = excludedApps + listOfNotNull(selfPackageForSplit),
                includedApps = includedApps - listOfNotNull(selfPackageForSplit)
            )
            val tunnel = GodsonTunnel(server.id)
            activeTunnel = tunnel

            backend.setState(tunnel, Tunnel.State.UP, config)

            // Vérification d'établissement : état UP + tunnel « alive » (handshake).
            val established = awaitTunnelEstablished(tunnel, session.serverPublicKey)
            if (established) {
                stateMachine.transitionTo(VpnConnectionState.Connected)
                Log.i(TAG, "Tunnel établi et vérifié (${server.id}).")
            } else {
                backend.setState(tunnel, Tunnel.State.DOWN, null)
                stateMachine.transitionTo(VpnConnectionState.ServerUnavailable)
                Log.w(TAG, "Le tunnel ne s'établit pas (pas de handshake) → indisponible.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Échec réseau/API pendant la connexion : ${e.message}")
            stateMachine.transitionTo(VpnConnectionState.Error)
        } catch (e: Exception) {
            // Permission VpnService refusée, backend indisponible, config invalide…
            Log.e(TAG, "Échec d'établissement du tunnel : ${e.javaClass.simpleName}")
            stateMachine.transitionTo(VpnConnectionState.Error)
        }
    }

    private suspend fun awaitTunnelEstablished(tunnel: Tunnel, serverPublicKey: String): Boolean {
        // État UP immédiat n'est PAS suffisant : on attend un tunnel « alive ».
        repeat(15) {
            val alive = runCatching {
                backend.getStatistics(tunnel)
                    .peer(Key.fromBase64(serverPublicKey))
                    ?.let { System.currentTimeMillis() - it.latestHandshakeEpochMillis() < HANDSHAKE_FRESH_MS }
                    ?: false
            }.getOrDefault(false)
            if (alive) return true
            delay(MONITOR_INTERVAL_MS)
        }
        return false
    }

    /** Fermeture propre : DOWN, révocation de session, purge des références. */
    suspend fun disconnectReal() {
        val tunnel = activeTunnel
        if (tunnel != null) {
            runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
                .onFailure { Log.w(TAG, "Arrêt du tunnel avec avertissement : ${it.message}") }
        }
        activeSessionId?.let { sessionId ->
            runCatching { api.closeVpnSession(sessionId) }
        }
        activeTunnel = null
        activeSessionId = null
        activeSessionServerKey = null
        _stats.value = null
    }

    /**
     * Surveillance du tunnel : alimente les statistiques RÉELLES à 1 Hz et
     * déclare l'interruption (Reconnecting/Disconnected) selon la vérification.
     */
    fun startMonitoring(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                val tunnel = activeTunnel
                if (stateMachine.state.value == VpnConnectionState.Connected && tunnel != null) {
                    runCatching {
                        val s = backend.getStatistics(tunnel)
                        val alive = activeSessionServerKey?.let { key ->
                            s.peer(Key.fromBase64(key))?.let {
                                System.currentTimeMillis() - it.latestHandshakeEpochMillis() < HANDSHAKE_FRESH_MS
                            } ?: false
                        } ?: (backend.getState(tunnel) == Tunnel.State.UP)
                        _stats.value = TunnelStats(
                            rxBytes = s.totalRx(),
                            txBytes = s.totalTx(),
                            tunnelAlive = alive
                        )
                        if (!alive) {
                            stateMachine.transitionTo(VpnConnectionState.Reconnecting)
                        }
                    }
                } else if (stateMachine.state.value != VpnConnectionState.Reconnecting) {
                    _stats.value = null
                }
                delay(MONITOR_INTERVAL_MS)
            }
        }
    }
}
