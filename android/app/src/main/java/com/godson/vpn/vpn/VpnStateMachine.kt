package com.godson.vpn.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Machine à états de la connexion VPN — Kotlin pur, testée unitairement.
 * Toute transition non listée est refusée : impossible d'afficher « Connecté »
 * sans être passé par [Connecting] avec un tunnel vérifié (Phase 5).
 */
class VpnStateMachine(initial: VpnConnectionState = VpnConnectionState.Disconnected) {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    val allowedTransitions: Map<VpnConnectionState, Set<VpnConnectionState>> = mapOf(
        VpnConnectionState.Disconnected to setOf(
            VpnConnectionState.Preparing, VpnConnectionState.Error
        ),
        VpnConnectionState.Preparing to setOf(
            VpnConnectionState.AwaitingPermission,
            VpnConnectionState.Connecting,
            VpnConnectionState.ServerUnavailable,
            VpnConnectionState.Error,
            VpnConnectionState.Disconnected
        ),
        VpnConnectionState.AwaitingPermission to setOf(
            VpnConnectionState.Connecting,
            VpnConnectionState.Disconnected,
            VpnConnectionState.Error
        ),
        VpnConnectionState.Connecting to setOf(
            VpnConnectionState.Connected,
            VpnConnectionState.Reconnecting,
            VpnConnectionState.Disconnecting,
            VpnConnectionState.ServerUnavailable,
            VpnConnectionState.Error
        ),
        VpnConnectionState.Connected to setOf(
            VpnConnectionState.Disconnecting,
            VpnConnectionState.Reconnecting,
            VpnConnectionState.Error
        ),
        VpnConnectionState.Reconnecting to setOf(
            VpnConnectionState.Connected,
            VpnConnectionState.Disconnecting,
            VpnConnectionState.ServerUnavailable,
            VpnConnectionState.Error
        ),
        VpnConnectionState.Disconnecting to setOf(
            VpnConnectionState.Disconnected,
            VpnConnectionState.Error
        ),
        VpnConnectionState.Error to setOf(
            VpnConnectionState.Disconnected,
            VpnConnectionState.Preparing
        ),
        VpnConnectionState.ServerUnavailable to setOf(
            VpnConnectionState.Disconnected,
            VpnConnectionState.Preparing
        )
    )

    fun canTransitionTo(target: VpnConnectionState): Boolean =
        target in (allowedTransitions[_state.value] ?: emptySet())

    /** @return true si la transition a été acceptée. */
    fun transitionTo(target: VpnConnectionState): Boolean {
        if (!canTransitionTo(target)) return false
        _state.value = target
        return true
    }

    fun reset() {
        _state.value = VpnConnectionState.Disconnected
    }
}
