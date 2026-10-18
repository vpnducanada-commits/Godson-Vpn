package com.godson.vpn.pulse

import kotlin.math.roundToInt

/**
 * Niveaux de qualité GODSON PULSE — seuils documentés et constants.
 * (les valeurs `minScore` forment la table officielle des seuils)
 */
enum class QualityLevel(val labelFr: String, val minScore: Int) {
    EXCELLENT("Excellente", 85),
    GOOD("Bonne", 65),
    FAIR("Correcte", 45),
    POOR("Dégradée", 0);

    companion object {
        /** Niveau associé à un score 0..100 (déterministe). */
        fun ofScore(score: Int): QualityLevel =
            entries.firstOrNull { score >= it.minScore } ?: POOR
    }
}

/**
 * Score GODSON PULSE — calcul DÉTERMINISTE d'une qualité de réseau à partir
 * d'une mesure valide. Aucun état, aucune horloge, aucune valeur aléatoire :
 * le même [ProbeResult] produit toujours le même score.
 *
 * ## Algorithme (documenté, version 1.0)
 *
 * 1. **Composante latence** (poids global 0,60) :
 *    `latencyScore = 100 × (1 − min(latence / LATENCY_MAX_MS, 1))`
 *    → 100 points à 0 ms, 0 point à [LATENCY_MAX_MS] (300 ms) et au-delà.
 *
 * 2. **Composantes de stabilité** (poids global 0,40, réparti en interne) :
 *    - perte   (poids interne 0,60) : `lossScore = 100 × (1 − min(perte / LOSS_MAX, 1))`
 *    - gigue   (poids interne 0,40) : `jitterScore = 100 × (1 − min(gigue / JITTER_MAX_MS, 1))`
 *
 * 3. **Données manquantes** : chaque composante de stabilité absente est EXCLUE
 *    et les poids utilisés sont renormalisés (dénominateur = somme des poids
 *    réellement disponibles). Une mesure ne portant que la latence produit donc
 *    `score = latencyScore` — jamais de pénalité inventée, jamais de bonus inventé.
 *
 * 4. **Données invalides** : refusées à la construction ([ProbeResult] valide ses
 *    bornes) ; le score retourne null si la latence est absente — il n'existe
 *    pas de score sans latence mesurée.
 *
 * Le score est un entier 0..100 (arrondi arithmétique standard).
 */
object PulseScore {

    /** Latence au-delà de laquelle la composante latence vaut 0 (ms). */
    const val LATENCY_MAX_MS = 300.0

    /** Gigue au-delà de laquelle la composante gigue vaut 0 (ms). */
    const val JITTER_MAX_MS = 80.0

    /** Taux de perte au-delà duquel la composante perte vaut 0 (0,30 = 30 %). */
    const val LOSS_MAX = 0.30

    /** Poids global de la latence dans le score final. */
    const val WEIGHT_LATENCY = 0.60

    /** Poids global de la stabilité (perte + gigue) dans le score final. */
    const val WEIGHT_STABILITY = 0.40

    /** Poids de la perte à l'intérieur de la composante stabilité. */
    const val STABILITY_WEIGHT_LOSS = 0.60

    /** Poids de la gigue à l'intérieur de la composante stabilité. */
    const val STABILITY_WEIGHT_JITTER = 0.40

    /**
     * Calcule le score 0..100 d'une mesure, ou null si la latence est absente
     * ou la mesure incohérente (voir [ProbeResult.isCoherent]).
     */
    fun compute(result: ProbeResult): Int? {
        if (!result.hasValidLatency || !result.isCoherent) return null
        val latency = result.latencyMs ?: return null

        val latencyScore = 100.0 * (1.0 - (latency / LATENCY_MAX_MS).coerceAtMost(1.0))

        // Composantes de stabilité disponibles (perte, gigue) — les absentes sont exclues.
        var stabilityWeighted = 0.0
        var stabilityWeightUsed = 0.0

        result.lossRate?.let { loss ->
            stabilityWeighted += STABILITY_WEIGHT_LOSS *
                (100.0 * (1.0 - (loss / LOSS_MAX).coerceAtMost(1.0)))
            stabilityWeightUsed += STABILITY_WEIGHT_LOSS
        }
        result.jitterMs?.let { jitter ->
            stabilityWeighted += STABILITY_WEIGHT_JITTER *
                (100.0 * (1.0 - (jitter / JITTER_MAX_MS).coerceAtMost(1.0)))
            stabilityWeightUsed += STABILITY_WEIGHT_JITTER
        }

        // Renormalisation : seuls les poids réellement disponibles comptent.
        val totalWeight = WEIGHT_LATENCY + WEIGHT_STABILITY * stabilityWeightUsed
        val raw = if (totalWeight <= 0.0) latencyScore
        else (WEIGHT_LATENCY * latencyScore + WEIGHT_STABILITY * stabilityWeighted) / totalWeight

        return raw.roundToInt().coerceIn(0, 100)
    }

    /** Niveau de qualité associé à un score (seuils officiels [QualityLevel]). */
    fun levelOf(score: Int): QualityLevel = QualityLevel.ofScore(score)
}
