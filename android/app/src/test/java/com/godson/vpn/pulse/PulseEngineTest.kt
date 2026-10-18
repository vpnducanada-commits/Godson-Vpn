package com.godson.vpn.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaires du moteur GODSON PULSE — évaluation pure (sans I/O,
 * sans coroutines) : classement, compatibilité des sources, données
 * manquantes/invalides, serveurs indisponibles, résultats DEMO, échec total.
 *
 * (NON exécutés dans Arena AI — faute de JDK 17 / Gradle ; exécution prévue
 * via `./gradlew :app:testDebugUnitTest`. Aucun succès n'est revendiqué ici.)
 */
class PulseEngineTest {

    private val engine = PulseEngine()
    private val now = 1_700_000_000_000L

    private fun real(
        id: String,
        latency: Double?,
        jitter: Double? = 5.0,
        loss: Double? = 0.0,
        reachable: Boolean = true
    ): ProbeResult = ProbeResult(
        serverId = id,
        source = PulseSource.REAL,
        latencyMs = latency,
        jitterMs = jitter,
        lossRate = loss,
        reachable = reachable,
        timestampMillis = now
    )

    private fun demo(
        id: String,
        latency: Double,
        jitter: Double? = 5.0,
        loss: Double? = 0.0
    ): ProbeResult = ProbeResult(
        serverId = id,
        source = PulseSource.DEMO,
        latencyMs = latency,
        jitterMs = jitter,
        lossRate = loss,
        reachable = true,
        timestampMillis = now
    )

    // ── États explicites ────────────────────────────────────────────────

    @Test
    fun `aucun résultat — état indisponible, aucune recommandation`() {
        val report = engine.evaluate(emptyList(), now)
        assertEquals(PulseStatus.UNAVAILABLE, report.status)
        assertNull(report.recommendation)
        assertTrue(report.rankedServerIds.isEmpty())
    }

    @Test
    fun `toutes les sondes échouent — état indisponible, jamais de faux serveur recommandé`() {
        val report = engine.evaluate(
            listOf(real("srv-a", latency = null, reachable = false)),
            now
        )
        assertEquals(PulseStatus.UNAVAILABLE, report.status)
        assertNull(report.recommendation)
    }

    @Test
    fun `résultats réels valides — état OK et recommandation REAL`() {
        val report = engine.evaluate(
            listOf(real("srv-fast", latency = 25.0), real("srv-slow", latency = 120.0)),
            now
        )
        assertEquals(PulseStatus.OK, report.status)
        assertEquals(2, report.realResultCount)
        assertEquals(0, report.demoResultCount)
        val rec = report.recommendation
        assertNotNull(rec)
        assertEquals("srv-fast", rec!!.serverId)
        assertEquals(PulseSource.REAL, rec.source)
    }

    // ── Compatibilité des sources : le réel prime, la démo est écartée ──

    @Test
    fun `présence de mesures réelles — les résultats DEMO sont exclus du classement`() {
        val report = engine.evaluate(
            listOf(
                real("srv-reel", latency = 100.0),
                demo("srv-demo-rapide", latency = 5.0) // meilleur score démo, mais ignoré
            ),
            now
        )
        assertEquals(PulseStatus.OK, report.status)
        assertEquals(listOf("srv-reel"), report.rankedServerIds)
        assertEquals("srv-reel", report.recommendation!!.serverId)
        assertEquals(PulseSource.REAL, report.recommendation!!.source)
    }

    @Test
    fun `seules des mesures DEMO — état DEMO_ONLY et recommandation explicitement DEMO`() {
        val report = engine.evaluate(
            listOf(demo("demo-a", latency = 30.0), demo("demo-b", latency = 60.0)),
            now
        )
        assertEquals(PulseStatus.DEMO_ONLY, report.status)
        assertEquals(PulseSource.DEMO, report.recommendation!!.source)
        assertEquals("demo-a", report.recommendation!!.serverId)
        assertEquals(2, report.demoResultCount)
        assertEquals(0, report.realResultCount)
    }

    // ── Classement déterministe ─────────────────────────────────────────

    @Test
    fun `classement par score décroissant`() {
        val report = engine.evaluate(
            listOf(
                real("srv-moyen", latency = 90.0),
                real("srv-meilleur", latency = 20.0),
                real("srv-faible", latency = 200.0)
            ),
            now
        )
        assertEquals(
            listOf("srv-meilleur", "srv-moyen", "srv-faible"),
            report.rankedServerIds
        )
    }

    @Test
    fun `égalité de score — la latence la plus faible départage`() {
        // Mêmes scores : latence identique ET stabilité identique ⇒ ex æquo,
        // départage par latence (identique ici) puis ordre alphabétique.
        val report = engine.evaluate(
            listOf(
                real("srv-z", latency = 50.0),
                real("srv-a", latency = 50.0)
            ),
            now
        )
        assertEquals(listOf("srv-a", "srv-z"), report.rankedServerIds)
    }

    @Test
    fun `égalité de score et de latence — l'identifiant alphabétique départage`() {
        val report = engine.evaluate(
            listOf(
                real("srv-b", latency = 50.0, jitter = null, loss = null),
                real("srv-a", latency = 50.0, jitter = null, loss = null)
            ),
            now
        )
        assertEquals(listOf("srv-a", "srv-b"), report.rankedServerIds)
    }

    @Test
    fun `classement plafonné à MAX_RANKED entrées`() {
        val many = (1..20).map { real("srv-%02d".format(it), latency = 40.0 + it) }
        val report = engine.evaluate(many, now)
        assertEquals(PulseEngine.MAX_RANKED, report.rankedServerIds.size)
        assertEquals("srv-01", report.rankedServerIds.first())
    }

    // ── Données invalides / serveurs indisponibles ──────────────────────

    @Test
    fun `mesure incohérente — exclue du classement sans fausser le rapport`() {
        // srv-incohérent : reachable=false mais latence présente → exclu.
        val incoherent = real("srv-incoherent", latency = 10.0, reachable = false)
        assertTrue(!incoherent.isCoherent)
        val report = engine.evaluate(
            listOf(incoherent, real("srv-valide", latency = 80.0)),
            now
        )
        assertEquals(PulseStatus.OK, report.status)
        assertEquals(listOf("srv-valide"), report.rankedServerIds)
    }

    @Test
    fun `serveur indisponible à côté d'un serveur valide — jamais recommandé`() {
        val report = engine.evaluate(
            listOf(
                real("srv-ko", latency = null, reachable = false),
                real("srv-ok", latency = 70.0)
            ),
            now
        )
        assertEquals("srv-ok", report.recommendation!!.serverId)
        assertTrue(!report.rankedServerIds.contains("srv-ko"))
    }

    @Test
    fun `déterminisme — mêmes entrées, même rapport`() {
        val inputs = listOf(
            real("srv-1", latency = 33.0, jitter = 8.0, loss = 0.01),
            real("srv-2", latency = 55.0, jitter = 3.0, loss = 0.0)
        )
        val first = engine.evaluate(inputs, now)
        val second = engine.evaluate(inputs, now)
        assertEquals(first, second)
    }

    @Test
    fun `niveau de qualité de la recommandation cohérent avec le score`() {
        val report = engine.evaluate(listOf(real("srv-x", latency = 20.0)), now)
        val rec = report.recommendation!!
        assertEquals(PulseScore.levelOf(rec.score), rec.level)
    }
}
