package com.godson.vpn.data

/**
 * Tunnelage fractionné (split tunneling) — logique PURE, testée unitairement.
 *
 * Deux modes exclusifs (l'API Android VpnService n'accepte que l'un des deux) :
 * - BLOCK : tout passe par le VPN SAUF les applications sélectionnées
 *   (→ Interface.excludeApplications, appliqué par GoBackend via
 *   VpnService.Builder.addDisallowedApplication) ;
 * - ALLOW : seules les applications sélectionnées passent par le VPN
 *   (→ Interface.includeApplications, appliqué via addAllowedApplication).
 *
 * Garde-fous :
 * - l'application GODSON VPN elle-même n'est jamais routée dans le tunnel
 *   (ses propres appels API doivent sortir hors du tunnel, sinon boucle) ;
 * - une liste ALLOW vide signifie « aucune application dans le tunnel » :
 *   l'UI doit l'afficher clairement (avertissement explicite, jamais un
 *   comportement trompeur) ;
 * - sans tunnel réel (mode démo), ces réglages sont persistés mais sans effet :
 *   ils seront appliqués à la création du tunnel en mode réel uniquement.
 */
object SplitTunnelPolicy {

    /** Paquet de cette application — jamais sélectionnable, toujours hors tunnel en BLOCK. */
    const val SELF_PACKAGE = "com.godson.vpn"

    enum class Mode(val storageKey: String) {
        BLOCK("BLOCK"),
        ALLOW("ALLOW");

        companion object {
            /** Mode par défaut sûr : BLOCK (tout passe, sauf sélection). */
            fun fromStorageKey(key: String?): Mode =
                entries.firstOrNull { it.storageKey == key } ?: BLOCK
        }
    }

    /** Retire le paquet de l'app elle-même et les entrées vides d'une sélection. */
    fun sanitizeSelection(packages: Set<String>): Set<String> =
        packages.filter { it.isNotBlank() && it != SELF_PACKAGE }.toSet()

    /** Paquets à EXCLURE du tunnel (mode BLOCK : sélection assainie + cette app). */
    fun excludedPackages(mode: Mode, selected: Set<String>): Set<String> =
        if (mode == Mode.BLOCK) sanitizeSelection(selected) + SELF_PACKAGE else emptySet()

    /** Paquets à INCLURE dans le tunnel (mode ALLOW : sélection assainie uniquement). */
    fun includedPackages(mode: Mode, selected: Set<String>): Set<String> =
        if (mode == Mode.ALLOW) sanitizeSelection(selected) else emptySet()

    /** Avertir l'UI : mode ALLOW actif sans aucune app ⇒ rien ne passera par le VPN. */
    fun mustWarnEmptyAllowlist(mode: Mode, selected: Set<String>): Boolean =
        mode == Mode.ALLOW && sanitizeSelection(selected).isEmpty()

    /**
     * Filtre et tri (déterministes) de la liste des applications installées :
     * exclut cette app, filtre par recherche insensible à la casse (paquet ou
     * libellé), tri alphabétique par libellé puis par paquet.
     *
     * @param installed paires (paquet, libellé) fournies par le PackageManager.
     */
    fun visibleApps(
        installed: List<Pair<String, String>>,
        query: String
    ): List<Pair<String, String>> {
        val q = query.trim().lowercase()
        return installed
            .filter { it.first != SELF_PACKAGE }
            .filter {
                q.isEmpty() || it.second.lowercase().contains(q) ||
                    it.first.lowercase().contains(q)
            }
            .sortedWith(compareBy({ it.second.lowercase() }, { it.first }))
    }
}
