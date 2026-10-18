package com.godson.vpn.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.SessionRecord
import com.godson.vpn.data.model.ServerInfo
import com.godson.vpn.vpn.VpnConnectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Statistiques de session RÉELLES (durées mesurées localement ; trafic réel en Phase 5). */
class StatsViewModel(private val container: GodsonContainer) : ViewModel() {

    val history: StateFlow<List<SessionRecord>> = container.sessionStats.history

    val currentSeconds: StateFlow<Int> = container.sessionStats.currentSessionSeconds

    val vpnState: StateFlow<VpnConnectionState> = container.vpnStateMachine.state

    val serverById: StateFlow<Map<String, ServerInfo>> = container.serverRepository.catalog
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun clearHistory() {
        container.sessionStats.clearHistory()
    }
}
