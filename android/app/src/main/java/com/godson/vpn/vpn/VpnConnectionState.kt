package com.godson.vpn.vpn

/**
 * États réels du cycle de vie de la connexion VPN.
 * Le libellé « Protégé » n'est jamais affiché hors de l'état Connected (et,
 * en Phase 5, seulement après vérification du handshake et du DNS du tunnel).
 */
enum class VpnConnectionState {
    Disconnected,
    Preparing,
    AwaitingPermission,
    Connecting,
    Connected,
    Reconnecting,
    Disconnecting,
    Error,
    ServerUnavailable
}
