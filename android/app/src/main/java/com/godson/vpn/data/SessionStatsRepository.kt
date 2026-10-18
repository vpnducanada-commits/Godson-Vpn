package com.godson.vpn.data

import com.godson.vpn.vpn.VpnConnectionState
import com.godson.vpn.vpn.VpnStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Session terminée, mesurée localement (durée réelle passée en état Connected). */
data class SessionRecord(
    val serverId: String,
    val startedAtMillis: Long,
    val durationSeconds: Int
)

/**
 * Statistiques de session RÉELLES : l'horloge ne tourne que lorsque la machine à états
 * est dans Connected. Les volumes de trafic restent « indisponibles » jusqu'à la Phase 5
 * (ils seront lus depuis les compteurs du tunnel WireGuard — jamais simulés).
 */
class SessionStatsRepository(
    scope: CoroutineScope,
    stateMachine: VpnStateMachine
) {
    private val _history = MutableStateFlow<List<SessionRecord>>(emptyList())
    val history: StateFlow<List<SessionRecord>> = _history.asStateFlow()

    private val _currentSessionSeconds = MutableStateFlow(0)
    val currentSessionSeconds: StateFlow<Int> = _currentSessionSeconds.asStateFlow()

    private var currentStartMillis: Long? = null
    private var currentServerId: String? = null

    init {
        scope.launch {
            stateMachine.state.collect { state ->
                when (state) {
                    VpnConnectionState.Connected -> beginSession()
                    VpnConnectionState.Disconnected,
                    VpnConnectionState.Error,
                    VpnConnectionState.ServerUnavailable -> endSession()
                    else -> Unit
                }
            }
        }
    }

    fun trackServer(serverId: String) {
        currentServerId = serverId
    }

    @Synchronized
    private fun beginSession() {
        if (currentStartMillis != null) return
        currentStartMillis = System.currentTimeMillis()
        _currentSessionSeconds.value = 0
    }

    init {
        scope.launch {
            while (true) {
                delay(1000)
                val start = currentStartMillis
                if (start != null) {
                    _currentSessionSeconds.value =
                        ((System.currentTimeMillis() - start) / 1000).toInt()
                }
            }
        }
    }

    @Synchronized
    private fun endSession() {
        val start = currentStartMillis ?: return
        val duration = ((System.currentTimeMillis() - start) / 1000).toInt()
        val record = SessionRecord(
            serverId = currentServerId ?: "unknown",
            startedAtMillis = start,
            durationSeconds = duration
        )
        _history.value = (listOf(record) + _history.value).take(20)
        currentStartMillis = null
        _currentSessionSeconds.value = 0
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
