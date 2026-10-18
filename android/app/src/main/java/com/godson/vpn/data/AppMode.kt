package com.godson.vpn.data

import com.godson.vpn.BuildConfig

/** Mode applicatif : démonstration (aucun backend) ou VPN réel. */
enum class VpnMode { DEMO, REAL }

/**
 * Séparation stricte démo / réel (exigence §4-5 des consignes de développement).
 *
 * Le mode est dérivé de la configuration de build : tant que [BuildConfig.API_BASE_URL]
 * pointe vers un domaine `.invalid` (RFC 2606 — jamais résolvable), l'application est
 * en mode DÉMO : aucune requête réseau réelle, aucune tentative de tunnel, erreur
 * explicite affichée à l'utilisateur.
 *
 * Pour passer en mode RÉEL : renseigner l'URL du backend réel dans
 * `app/build.gradle.kts` (champ API_BASE_URL) — voir docs/04_WIREGUARD_INFRA.md
 * et docs/05_BUILD_AND_TEST.md.
 */
object AppMode {

    val vpnMode: VpnMode
        get() = if (BuildConfig.API_BASE_URL.contains(".invalid")) VpnMode.DEMO else VpnMode.REAL

    val isDemo: Boolean
        get() = vpnMode == VpnMode.DEMO
}
