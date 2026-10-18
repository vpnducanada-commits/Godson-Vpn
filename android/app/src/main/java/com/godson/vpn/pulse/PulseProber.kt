package com.godson.vpn.pulse

/**
 * GODSON PULSE — modèle de données et contrat de sonde réseau.
 *
 * Règles produit (voir docs/06_GODSON_PULSE_SPEC.md) :
 * - chaque mesure porte sa [PulseSource] : REAL (sonde réellement exécutée) ou DEMO ;
 * - une mesure DEMO n'est jamais présentée comme réelle ;
 * - aucune valeur aléatoire : tout résultat est produit par une sonde exécutée
 *   (REAL) ou calculé de façon déterministe (DEMO) ;
 * - une connexion HTTP/TCP réussie ne prouve PAS qu'un serveur VPN est opérationnel :
 *   PULSE évalue la qualité réseau, jamais l'état du tunnel.
 */

/** Origine d'une mesure. Comparaison autorisée uniquement à source égale. */
enum class PulseSource { REAL, DEMO }

/**
 * Cible d'une sonde : un serveur (réel ou candidat de démonstration).
 *
 * [isRealProbeable] protège contre les faux tests : une adresse `.invalid`,
 * `.test` ou `.localhost` (RFC 2606 / 6761) n'est JAMAIS une cible réelle.
 */
data class PulseTarget(
    val serverId: String,
    val host: String,
    val port: Int = 443,
    val isDemoTarget: Boolean = false
) {
    init {
        require(serverId.isNotBlank()) { "serverId requis" }
        require(port in 1..65535) { "port invalide : $port" }
    }

    /** true si une sonde RÉELLE peut viser cette cible (aucun nom réservé, hôte présent). */
    val isRealProbeable: Boolean
        get() = !isDemoTarget &&
            host.isNotBlank() &&
            !host.endsWith(".invalid", ignoreCase = true) &&
            !host.endsWith(".test", ignoreCase = true) &&
            !host.endsWith(".localhost", ignoreCase = true) &&
            !host.endsWith(".example", ignoreCase = true)

    /** true si la cible est explicitement destinée aux démonstrations. */
    val isDemoProbeable: Boolean
        get() = isDemoTarget
}

/**
 * Résultat normalisé d'une sonde. Champs explicites, valeurs bornées :
 * le constructeur refuse toute donnée hors bornes (validation à l'entrée).
 *
 * @param latencyMs  latence mesurée en millisecondes (null = non mesurée) ;
 * @param jitterMs   gigue (variation entre mesures successives) en ms (null = inconnue) ;
 * @param lossRate   taux d'échec des échantillons, entre 0.0 et 1.0 (null = inconnu) ;
 * @param reachable  la cible a répondu à la sonde ;
 * @param source     origine de la mesure (REAL / DEMO) — jamais omise ;
 * @param timestampMillis  horodatage de la mesure (millisecondes epoch) ;
 * @param detail     information technique facultative (jamais de donnée personnelle).
 */
data class ProbeResult(
    val serverId: String,
    val source: PulseSource,
    val latencyMs: Double? = null,
    val jitterMs: Double? = null,
    val lossRate: Double? = null,
    val reachable: Boolean = false,
    val timestampMillis: Long,
    val detail: String? = null
) {
    init {
        require(serverId.isNotBlank()) { "serverId requis" }
        require(timestampMillis >= 0) { "horodatage invalide" }
        require(latencyMs == null || latencyMs >= 0.0) { "latence négative interdite" }
        require(latencyMs == null || latencyMs <= MAX_LATENCY_MS) {
            "latence au-delà de la borne plausible ($MAX_LATENCY_MS ms)"
        }
        require(jitterMs == null || jitterMs in 0.0..MAX_JITTER_MS) {
            "gigue hors bornes"
        }
        require(lossRate == null || lossRate in 0.0..1.0) {
            "taux de perte hors bornes (attendu : 0.0 à 1.0)"
        }
    }

    /** Latence présente, cohérente et dans la borne de plausibilité. */
    val hasValidLatency: Boolean
        get() = reachable && latencyMs != null && latencyMs >= 0.0 && latencyMs <= MAX_LATENCY_MS

    /**
     * Cohérence logique : une cible injoignable ne peut pas porter une latence,
     * une cible joignable doit porter au moins la latence.
     */
    val isCoherent: Boolean
        get() = if (reachable) latencyMs != null else latencyMs == null

    /** Deux résultats ne sont comparables qu'à source égale (REAL avec REAL, DEMO avec DEMO). */
    fun isCompatibleWith(other: ProbeResult): Boolean = source == other.source

    companion object {
        /** Borne de plausibilité : au-delà, la mesure est jugée aberrante. */
        const val MAX_LATENCY_MS = 5_000.0
        const val MAX_JITTER_MS = 2_000.0

        /** Résultat d'échec explicite (sonde injoignable, exception, délai dépassé). */
        fun unavailable(
            serverId: String,
            source: PulseSource,
            timestampMillis: Long,
            detail: String? = null
        ): ProbeResult = ProbeResult(
            serverId = serverId,
            source = source,
            latencyMs = null,
            jitterMs = null,
            lossRate = null,
            reachable = false,
            timestampMillis = timestampMillis,
            detail = detail
        )
    }
}

/**
 * Contrat d'une sonde réseau GODSON PULSE.
 *
 * Contrat d'implémentation :
 * - [probe] s'exécute UNIQUEMENT à la demande (jamais en boucle, jamais en arrière-plan) ;
 * - l'implémentation respecte un délai d'attente court (recommandé : [PROBE_TIMEOUT_MS]) ;
 * - l'implémentation ne lève pas d'exception pour une cible injoignable : elle retourne
 *   [ProbeResult.unavailable] ;
 * - une implémentation réelle ne doit viser que des cibles [PulseTarget.isRealProbeable] ;
 * - aucune donnée personnelle dans [ProbeResult.detail].
 */
interface PulseProber {
    val source: PulseSource

    suspend fun probe(target: PulseTarget): ProbeResult

    companion object {
        /** Délai d'attente recommandé par mesure. */
        const val PROBE_TIMEOUT_MS = 2_000
    }
}
