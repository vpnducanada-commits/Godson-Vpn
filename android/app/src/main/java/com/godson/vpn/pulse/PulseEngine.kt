package com.godson.vpn.pulse

/**
 * GODSON PULSE — moteur d'orchestration et de recommandation.
 *
 * Responsabilités :
 * - exécuter une sonde à la demande, cible par cible, SANS boucle en arrière-plan
 *   (aucun timer, aucun service : [probeAndEvaluate] ne fait que séquencer les
 *   appels demandés par l'interface) ;
 * - ne comparer que des résultats COMPATIBLES (même [PulseSource]) : si des
 *   mesures réelles existent, les résultats DEMO sont écartés du classement ;
 * - exclure les mesures incohérentes ou invalides (jamais interprétées) ;
 * - recommander le meilleur serveur à partir du score, avec départage
 *   déterministe (latence, puis identifiant) ;
 * - retourner un état EXPLICITE lorsque l'évaluation est impossible, ou lorsque
 *   seules des données de démonstration sont disponibles ;
 * - ne jamais présenter un serveur fictif comme opérationnel : une recommandation
 *   DEMO porte toujours [PulseRecommendation.source] = DEMO et un rapport
 *   [PulseStatus.DEMO_ONLY].
 *
 * Le moteur ne touche ni aux clés WireGuard, ni au trafic utilisateur, et ne
 * modifie jamais l'état du VPN.
 */

/** État du rapport d'évaluation. */
enum class PulseStatus {
    /** Évaluation réelle disponible (au moins une mesure REAL valide). */
    OK,

    /** Aucune mesure réelle — recommandation basée sur des données DEMO étiquetées. */
    DEMO_ONLY,

    /** Évaluation impossible : aucun résultat valide, ou toutes les sondes ont échoué. */
    UNAVAILABLE
}

/** Recommandation issue du classement (toujours accompagnée de sa source). */
data class PulseRecommendation(
    val serverId: String,
    val score: Int,
    val level: QualityLevel,
    val source: PulseSource
)

/** Rapport complet d'une évaluation GODSON PULSE. */
data class PulseReport(
    val status: PulseStatus,
    val recommendation: PulseRecommendation?,
    /** Classement décroissant des identifiants évalués (au plus [PulseEngine.MAX_RANKED]). */
    val rankedServerIds: List<String>,
    val realResultCount: Int,
    val demoResultCount: Int,
    val evaluatedAtMillis: Long
)

class PulseEngine {

    /**
     * Exécute [prober] sur chaque cible, séquentiellement et à la demande,
     * puis produit le rapport. Une exception de sonde devient un résultat
     * d'échec explicite (jamais propagée, jamais interprétée comme une mesure).
     *
     * Aucune boucle en arrière-plan : cette fonction s'arrête après les cibles fournies.
     *
     * @param nowMillis horodatage passé explicitement (testabilité, déterminisme).
     */
    suspend fun probeAndEvaluate(
        targets: List<PulseTarget>,
        prober: PulseProber,
        nowMillis: Long
    ): PulseReport {
        val results = targets.map { target ->
            runCatching { prober.probe(target) }.getOrElse {
                ProbeResult.unavailable(
                    serverId = target.serverId,
                    source = prober.source,
                    timestampMillis = nowMillis,
                    detail = "Exception de sonde : ${it.javaClass.simpleName}"
                )
            }
        }
        return evaluate(results, nowMillis)
    }

    /**
     * Évaluation pure (sans I/O) d'un lot de résultats déjà collectés.
     * Déterministe : mêmes entrées ⇒ même rapport.
     */
    fun evaluate(results: List<ProbeResult>, nowMillis: Long): PulseReport {
        val valid = results.filter { it.isCoherent }
        val real = valid.filter { it.source == PulseSource.REAL }
        val demo = valid.filter { it.source == PulseSource.DEMO }

        // Comparaison uniquement à source égale : le réel prime s'il existe.
        val primary = if (real.isNotEmpty()) real else demo
        val primarySource = if (real.isNotEmpty()) PulseSource.REAL else PulseSource.DEMO

        val ranked = rank(primary)
        val report = buildReport(
            ranked = ranked,
            source = primarySource,
            realCount = real.size,
            demoCount = demo.size,
            nowMillis = nowMillis
        )
        return report
    }

    /**
     * Classement déterministe : score décroissant, puis latence croissante,
     * puis identifiant alphabétique. Ne retient que les cibles joignables
     * avec une latence valide.
     */
    private fun rank(results: List<ProbeResult>): List<Pair<ProbeResult, Int>> =
        results
            .filter { it.hasValidLatency }
            .map { it to (PulseScore.compute(it) ?: 0) }
            .sortedWith(
                compareByDescending<Pair<ProbeResult, Int>> { it.second }
                    .thenBy { it.first.latencyMs ?: Double.MAX_VALUE }
                    .thenBy { it.first.serverId }
            )
            .take(MAX_RANKED)

    private fun buildReport(
        ranked: List<Pair<ProbeResult, Int>>,
        source: PulseSource,
        realCount: Int,
        demoCount: Int,
        nowMillis: Long
    ): PulseReport {
        val best = ranked.firstOrNull()
        val status = when {
            best == null -> PulseStatus.UNAVAILABLE
            source == PulseSource.REAL -> PulseStatus.OK
            else -> PulseStatus.DEMO_ONLY
        }
        val recommendation = best?.let { (result, score) ->
            PulseRecommendation(
                serverId = result.serverId,
                score = score,
                level = PulseScore.levelOf(score),
                source = result.source
            )
        }
        return PulseReport(
            status = status,
            recommendation = recommendation,
            rankedServerIds = ranked.map { it.first.serverId },
            realResultCount = realCount,
            demoResultCount = demoCount,
            evaluatedAtMillis = nowMillis
        )
    }

    companion object {
        /** Taille maximale du classement exposé (limite d'interface, pas de logique). */
        const val MAX_RANKED = 10
    }
}
