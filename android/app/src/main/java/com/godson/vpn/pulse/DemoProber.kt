package com.godson.vpn.pulse

/**
 * Sonde de DÉMONSTRATION — ne mesure RIEN.
 *
 * Produit des valeurs déterministes dérivées de l'identifiant du serveur
 * (même entrée ⇒ même valeur, à chaque exécution, sur tous les appareils).
 * Aucune valeur aléatoire, aucune opération réseau, aucune horloge système
 * (l'horodatage est passé en paramètre).
 *
 * ⚠️ Chaque résultat porte [PulseSource.DEMO] : il ne doit JAMAIS être présenté
 * comme une mesure réelle, et ne prouve jamais qu'un serveur est opérationnel.
 * Une cible non explicitement marquée `isDemoTarget` est refusée (échec explicite).
 */
class DemoProber : PulseProber {

    override val source: PulseSource = PulseSource.DEMO

    override suspend fun probe(target: PulseTarget): ProbeResult =
        demoResultFor(target, System.currentTimeMillis())

    companion object {

        /**
         * Résultat de démonstration déterministe pour une cible.
         * La valeur dérivée du `serverId` reste stable (pas de hasard).
         */
        fun demoResultFor(target: PulseTarget, timestampMillis: Long): ProbeResult {
            if (!target.isDemoProbeable) {
                return ProbeResult.unavailable(
                    serverId = target.serverId,
                    source = PulseSource.DEMO,
                    timestampMillis = timestampMillis,
                    detail = "Cible non marquée pour la démonstration — sonde refusée"
                )
            }
            val seed = stableSeed(target.serverId)
            // Bandes de latence plausibles 15..214 ms, déterministes par identifiant.
            val latency = 15.0 + (seed % 200)
            // Gigue 2..21 ms, perte 0 ou 2 % — également déterministes.
            val jitter = 2.0 + (seed / 200 % 20)
            val loss = if (seed % 7L == 0L) 0.02 else 0.0

            return ProbeResult(
                serverId = target.serverId,
                source = PulseSource.DEMO,
                latencyMs = latency,
                jitterMs = jitter,
                lossRate = loss,
                reachable = true,
                timestampMillis = timestampMillis,
                detail = "Valeur de démonstration (déterministe) — aucune mesure réelle"
            )
        }

        /** Graine stable : somme pondérée des caractères (pas de Random). */
        internal fun stableSeed(serverId: String): Long =
            serverId.lowercase().fold(0L) { acc, c -> acc * 31 + c.code }
    }
}
