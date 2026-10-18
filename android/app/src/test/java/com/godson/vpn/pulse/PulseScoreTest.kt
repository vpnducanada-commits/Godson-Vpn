package com.godson.vpn.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests unitaires du score GODSON PULSE — logique pure, déterministe.
 * (Exécution : `./gradlew :app:testDebugUnitTest` — NON exécutés dans Arena AI,
 * faute de JDK 17 / Gradle ; voir docs/06_AUDIT_TECHNIQUE.md §D.)
 */
class PulseScoreTest {

    private fun result(
        latency: Double? = 20.0,
        jitter: Double? = 5.0,
        loss: Double? = 0.0,
        reachable: Boolean = true,
        source: PulseSource = PulseSource.REAL,
        id: String = "srv-test"
    ): ProbeResult = ProbeResult(
        serverId = id,
        source = source,
        latencyMs = latency,
        jitterMs = jitter,
        lossRate = loss,
        reachable = reachable,
        timestampMillis = 1_700_000_000_000
    )

    // ── Scores nominaux ─────────────────────────────────────────────────

    @Test
    fun `mesure quasi parfaite — score maximal attendu`() {
        // latence 20 ms → 93,33 ; perte 0 → 100 ; gigue 5 ms → 93,75
        // stabilité = 0,6×100 + 0,4×93,75 = 97,5 ; total = 0,6×93,33 + 0,4×97,5 = 95,0
        val score = PulseScore.compute(result(latency = 20.0, jitter = 5.0, loss = 0.0))
        assertEquals(95, score)
        assertEquals(QualityLevel.EXCELLENT, PulseScore.levelOf(score!!))
    }

    @Test
    fun `mesure très dégradée — score nul`() {
        // latence 350 ms (≥ 300 → 0), perte 0,5 (≥ 0,30 → 0), gigue 100 ms (≥ 80 → 0)
        val score = PulseScore.compute(result(latency = 350.0, jitter = 100.0, loss = 0.5))
        assertEquals(0, score)
        assertEquals(QualityLevel.POOR, PulseScore.levelOf(score!!))
    }

    @Test
    fun `score toujours compris entre 0 et 100 sur une gamme de mesures`() {
        val latencies = listOf(0.0, 5.0, 30.0, 60.0, 90.0, 150.0, 250.0, 299.0, 300.0, 1000.0)
        latencies.forEach { lat ->
            val score = PulseScore.compute(result(latency = lat))!!
            assertTrue("score hors bornes pour $lat ms : $score", score in 0..100)
        }
    }

    // ── Seuils et niveaux (table officielle) ────────────────────────────

    @Test
    fun `seuils des niveaux de qualité exacts`() {
        assertEquals(QualityLevel.EXCELLENT, PulseScore.levelOf(85))
        assertEquals(QualityLevel.EXCELLENT, PulseScore.levelOf(100))
        assertEquals(QualityLevel.GOOD, PulseScore.levelOf(84))
        assertEquals(QualityLevel.GOOD, PulseScore.levelOf(65))
        assertEquals(QualityLevel.FAIR, PulseScore.levelOf(64))
        assertEquals(QualityLevel.FAIR, PulseScore.levelOf(45))
        assertEquals(QualityLevel.POOR, PulseScore.levelOf(44))
        assertEquals(QualityLevel.POOR, PulseScore.levelOf(0))
    }

    // ── Données manquantes (renormalisation documentée) ─────────────────

    @Test
    fun `latence seule — pas de pénalité ni de bonus inventés`() {
        // 20 ms → latScore 93,33 ; aucune composante de stabilité → renormalisé = 93
        val score = PulseScore.compute(result(latency = 20.0, jitter = null, loss = null))
        assertEquals(93, score)
    }

    @Test
    fun `perte seule sans gigue — seule la perte pèse dans la stabilité`() {
        // lat 60 ms → latScore 80 ; perte 0,10 → lossScore 66,67
        // stabilité = 66,67 (poids interne 0,6 seul) ;
        // total = (0,6×80 + 0,4×0,6×66,67) / (0,6 + 0,4×0,6) = (48 + 16,0) / 0,84 = 76,19 → 76
        val score = PulseScore.compute(result(latency = 60.0, jitter = null, loss = 0.10))
        assertEquals(76, score)
    }

    @Test
    fun `aucune latence — aucun score produit`() {
        assertNull(PulseScore.compute(result(latency = null, reachable = false)))
    }

    // ── Données invalides ou contradictoires ────────────────────────────

    @Test
    fun `taux de perte hors bornes — construction refusée`() {
        try {
            result(loss = 1.5)
            fail("Une perte > 1.0 doit être refusée à la construction")
        } catch (expected: IllegalArgumentException) {
            // comportement attendu : validation à l'entrée
        }
    }

    @Test
    fun `latence négative — construction refusée`() {
        try {
            result(latency = -5.0)
            fail("Une latence négative doit être refusée à la construction")
        } catch (expected: IllegalArgumentException) {
            // comportement attendu
        }
    }

    @Test
    fun `mesure contradictoire — incohérente et non scorée`() {
        val incoherent = result(latency = 50.0, reachable = false)
        assertTrue(!incoherent.isCoherent)
        assertNull(PulseScore.compute(incoherent))
    }

    @Test
    fun `latence aberrante au-delà de la borne de plausibilité — construction refusée`() {
        try {
            result(latency = ProbeResult.MAX_LATENCY_MS + 1)
            fail("Une latence au-delà de la borne de plausibilité doit être refusée")
        } catch (expected: IllegalArgumentException) {
            // comportement attendu
        }
    }

    // ── Bornes exactes des constantes documentées ───────────────────────

    @Test
    fun `bornes exactes — 300 ms et 30 pour cent de perte valent zero`() {
        val atLimit = PulseScore.compute(result(latency = 300.0, jitter = 80.0, loss = 0.30))
        assertEquals(0, atLimit)
        val justUnder = PulseScore.compute(result(latency = 299.0, jitter = 79.0, loss = 0.29))
        assertTrue("score juste sous les bornes doit rester faible", justUnder in 0..5)
    }

    @Test
    fun `sources REAL et DEMO ne se mélangent pas dans la compatibilité`() {
        val realResult = result(source = PulseSource.REAL)
        val demoResult = result(source = PulseSource.DEMO, id = "srv-demo")
        assertTrue(realResult.isCompatibleWith(realResult.copy()))
        assertTrue(!realResult.isCompatibleWith(demoResult))
    }
}
