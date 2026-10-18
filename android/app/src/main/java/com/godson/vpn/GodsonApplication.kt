package com.godson.vpn

import android.app.Application
import com.godson.vpn.data.AppMode
import com.godson.vpn.data.GodsonContainer
import com.wireguard.android.backend.GoBackend

/**
 * Point d'entrée applicatif. L'injection est manuelle et légère (décision d'architecture §13) :
 * un seul conteneur, créé paresseusement, partagé par toute l'application.
 *
 * Mode RÉEL uniquement : le backend Go de WireGuard reçoit le contexte applicatif
 * (il pilotera le VpnService déclaré au manifeste). En mode DÉMO, aucune initialisation
 * VPN n'a lieu — aucune connexion n'est possible, et l'interface l'affiche.
 */
class GodsonApplication : Application() {

    val container: GodsonContainer by lazy {
        GodsonContainer(this)
    }
}
