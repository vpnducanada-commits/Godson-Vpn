package com.godson.vpn.vpn

import com.godson.vpn.api.VpnSessionResponse
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork

/**
 * Construit la configuration du tunnel WireGuard (types de la bibliothèque officielle)
 * à partir de la session éphémère livrée par le backend. Aucune cryptographie maison :
 * uniquement l'assemblage documenté des paramètres.
 */
object TunnelConfigMapper {

    fun build(keyPair: KeyPair, session: VpnSessionResponse): Config {
        val iface = Interface.Builder()
            .parsePrivateKey(keyPair.privateKey.toBase64())   // identité du client (jamais loggée)
            .addAddress(InetNetwork.parse(session.address))   // CIDR du client dans le tunnel
            .parseDnsServers(session.dns)                     // DNS poussé DANS le tunnel (anti-fuite)
            .build()

        val peer = Peer.Builder()
            .setPublicKey(Key.fromBase64(session.serverPublicKey))
            .parseEndpoint("${session.endpointHost}:${session.endpointPort}")
            .apply {
                val ips = session.allowedIps.ifEmpty { listOf("0.0.0.0/0", "::/0") }
                ips.forEach { addAllowedIp(InetNetwork.parse(it)) }
            }
            .setPersistentKeepalive(session.persistentKeepaliveSeconds)
            .build()

        return Config.Builder()
            .setInterface(iface)
            .addPeer(peer)
            .build()
    }
}
