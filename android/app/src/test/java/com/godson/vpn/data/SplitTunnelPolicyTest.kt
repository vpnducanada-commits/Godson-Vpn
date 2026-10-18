package com.godson.vpn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la logique PURE du tunnelage fractionné : modes, garde-fous
 * (paquet de l'app exclu), calcul des paquets à exclure/inclure, avertissement
 * de liste ALLOW vide, filtrage/tri déterministe de la liste des applications.
 */
class SplitTunnelPolicyTest {

    // ── Mode ─────────────────────────────────────────────────────────────

    @Test
    fun `le mode par défaut est BLOCK même sans valeur ou avec une valeur inconnue`() {
        assertEquals(SplitTunnelPolicy.Mode.BLOCK, SplitTunnelPolicy.Mode.fromStorageKey(null))
        assertEquals(SplitTunnelPolicy.Mode.BLOCK, SplitTunnelPolicy.Mode.fromStorageKey("X"))
        assertEquals(SplitTunnelPolicy.Mode.BLOCK, SplitTunnelPolicy.Mode.fromStorageKey("allow"))
    }

    @Test
    fun `les clés de stockage reconnues donnent le bon mode`() {
        assertEquals(SplitTunnelPolicy.Mode.BLOCK, SplitTunnelPolicy.Mode.fromStorageKey("BLOCK"))
        assertEquals(SplitTunnelPolicy.Mode.ALLOW, SplitTunnelPolicy.Mode.fromStorageKey("ALLOW"))
    }

    // ── Assainissement de la sélection ───────────────────────────────────

    @Test
    fun `la sélection est assainie : paquet de l app et entrées vides retirés`() {
        val input = setOf("com.a", "com.godson.vpn", "", "  ", "com.b")
        assertEquals(setOf("com.a", "com.b"), SplitTunnelPolicy.sanitizeSelection(input))
    }

    // ── Paquets exclus / inclus selon le mode ────────────────────────────

    @Test
    fun `en mode BLOCK on exclut la sélection plus cette application`() {
        val excluded = SplitTunnelPolicy.excludedPackages(
            SplitTunnelPolicy.Mode.BLOCK, setOf("com.a", "com.godson.vpn", "")
        )
        assertEquals(setOf("com.a", "com.godson.vpn"), excluded)
    }

    @Test
    fun `en mode BLOCK rien n est inclus`() {
        assertTrue(SplitTunnelPolicy.includedPackages(SplitTunnelPolicy.Mode.BLOCK, setOf("com.a")).isEmpty())
    }

    @Test
    fun `en mode ALLOW on inclut uniquement la sélection assainie`() {
        val included = SplitTunnelPolicy.includedPackages(
            SplitTunnelPolicy.Mode.ALLOW, setOf("com.a", "com.godson.vpn")
        )
        assertEquals(setOf("com.a"), included)
    }

    @Test
    fun `en mode ALLOW rien n est exclu explicitement`() {
        assertTrue(SplitTunnelPolicy.excludedPackages(SplitTunnelPolicy.Mode.ALLOW, setOf("com.a")).isEmpty())
    }

    // ── Avertissement liste ALLOW vide ───────────────────────────────────

    @Test
    fun `l avertissement ne s'allume qu'en ALLOW sans aucune app sélectionnée`() {
        assertTrue(
            SplitTunnelPolicy.mustWarnEmptyAllowlist(SplitTunnelPolicy.Mode.ALLOW, emptySet())
        )
        assertFalse(
            SplitTunnelPolicy.mustWarnEmptyAllowlist(SplitTunnelPolicy.Mode.ALLOW, setOf("com.a"))
        )
        assertFalse(
            SplitTunnelPolicy.mustWarnEmptyAllowlist(SplitTunnelPolicy.Mode.BLOCK, emptySet())
        )
    }

    // ── Liste visible des applications (filtre + tri purs) ───────────────

    @Test
    fun `la liste visible exclut cette app et trie par libellé puis paquet`() {
        val apps = listOf(
            "com.zeta" to "Zéta", "com.godson.vpn" to "GODSON VPN",
            "com.alpha" to "alpha", "com.beta" to "Beta"
        )
        val visible = SplitTunnelPolicy.visibleApps(apps, "")
        assertEquals(listOf("com.alpha", "com.beta", "com.zeta"), visible.map { it.first })
    }

    @Test
    fun `la recherche filtre sur le libellé ou le paquet sans tenir compte de la casse`() {
        val apps = listOf("com.fire" to "Firefox", "com.notes" to "Notes")
        assertEquals(
            listOf("com.fire"),
            SplitTunnelPolicy.visibleApps(apps, "fIRE").map { it.first }
        )
        assertEquals(
            listOf("com.fire"),
            SplitTunnelPolicy.visibleApps(apps, "com.fi").map { it.first }
        )
        assertEquals(2, SplitTunnelPolicy.visibleApps(apps, "").size)
    }
}
