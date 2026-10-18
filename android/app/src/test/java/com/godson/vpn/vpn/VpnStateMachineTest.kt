package com.godson.vpn.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaires purs de la machine à états VPN (exécutables via `./gradlew test`).
 * Garantissent notamment : impossible d'afficher « Connecté » sans passer par Connecting.
 */
class VpnStateMachineTest {

    @Test
    fun `refuse le saut direct de Déconnecté vers Connecté`() {
        val machine = VpnStateMachine()
        assertFalse(machine.transitionTo(VpnConnectionState.Connected))
        assertTrue(machine.state.value == VpnConnectionState.Disconnected)
    }

    @Test
    fun `chemin nominal complet`() {
        val machine = VpnStateMachine()
        assertTrue(machine.transitionTo(VpnConnectionState.Preparing))
        assertTrue(machine.transitionTo(VpnConnectionState.Connecting))
        assertTrue(machine.transitionTo(VpnConnectionState.Connected))
        assertTrue(machine.transitionTo(VpnConnectionState.Disconnecting))
        assertTrue(machine.transitionTo(VpnConnectionState.Disconnected))
    }

    @Test
    fun `chemin avec autorisation VpnService`() {
        val machine = VpnStateMachine()
        machine.transitionTo(VpnConnectionState.Preparing)
        assertTrue(machine.transitionTo(VpnConnectionState.AwaitingPermission))
        assertTrue(machine.transitionTo(VpnConnectionState.Connecting))
        assertTrue(machine.transitionTo(VpnConnectionState.Connected))
    }

    @Test
    fun `erreur permet une sortie claire ou une nouvelle tentative`() {
        val machine = VpnStateMachine()
        machine.transitionTo(VpnConnectionState.Preparing)
        machine.transitionTo(VpnConnectionState.Connecting)
        assertTrue(machine.transitionTo(VpnConnectionState.Error))
        assertTrue(machine.transitionTo(VpnConnectionState.Preparing))
    }

    @Test
    fun `connecté peut passer en reconnexion mais pas en préparation`() {
        val machine = VpnStateMachine()
        machine.transitionTo(VpnConnectionState.Preparing)
        machine.transitionTo(VpnConnectionState.Connecting)
        machine.transitionTo(VpnConnectionState.Connected)
        assertFalse(machine.transitionTo(VpnConnectionState.Preparing))
        assertTrue(machine.transitionTo(VpnConnectionState.Reconnecting))
        assertTrue(machine.transitionTo(VpnConnectionState.Connected))
    }

    @Test
    fun `reset revient toujours à Déconnecté`() {
        val machine = VpnStateMachine()
        machine.transitionTo(VpnConnectionState.Preparing)
        machine.reset()
        assertTrue(machine.state.value == VpnConnectionState.Disconnected)
    }
}
