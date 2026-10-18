package com.godson.vpn.api

import kotlinx.serialization.Serializable

/**
 * Contrat API v1 — miroir de docs/04_WIREGUARD_INFRA.md (§ contrat backend).
 * Tous les champs sont tolérants aux extensions futures (ignoreUnknownKeys).
 */

@Serializable
data class HealthResponse(
    val status: String,
    val version: String? = null
)

@Serializable
data class ServerDto(
    val id: String,
    val countryCode: String,
    val city: String,
    val hostname: String,
    val port: Int? = null,
    val protocol: String = "WireGuard",
    /** AVAILABLE | BUSY | UNAVAILABLE | PLANNED — parsé avec tolérance. */
    val status: String,
    val pingMs: Int? = null,
    val loadPercent: Int? = null,
    val recommended: Boolean = false
)

@Serializable
data class ServersResponse(
    val servers: List<ServerDto>
)

/** Requête de session : seule la clé PUBLIQUE du client quitte l'appareil. */
@Serializable
data class VpnSessionRequest(
    val serverId: String,
    val clientPublicKey: String
)

/**
 * Configuration éphémère du tunnel. La clé privée du client ne transite JAMAIS :
 * elle est générée et conservée sur l'appareil (VpnKeyManager).
 */
@Serializable
data class VpnSessionResponse(
    val sessionId: String,
    /** Adresse du client dans le tunnel, ex. « 10.7.0.2/32 ». */
    val address: String,
    /** DNS annoncé dans le tunnel, ex. « 10.8.0.1 » ou liste séparée par des virgules. */
    val dns: String,
    /** Clé publique WireGuard du serveur (base64). */
    val serverPublicKey: String,
    val endpointHost: String,
    val endpointPort: Int,
    val allowedIps: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepaliveSeconds: Int = 25
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String? = null
)

@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorBody
)
