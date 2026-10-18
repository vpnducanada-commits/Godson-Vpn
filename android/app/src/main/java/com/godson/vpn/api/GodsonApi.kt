package com.godson.vpn.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoints backend (contrat v1 — propositions de l'architecture §8, à aligner
 * avec l'implémentation réelle de la Phase 4). Transport HTTPS uniquement.
 */
interface GodsonApi {

    /** Sonde de vie (monitoring, vérification d'atteignabilité côté client). */
    @GET("api/v1/health")
    suspend fun health(): HealthResponse

    /** Catalogue de serveurs avec état réel (disponibilité, latence si mesurée, charge). */
    @GET("api/v1/servers")
    suspend fun servers(): ServersResponse

    /**
     * Délivre une configuration de tunnel éphémère pour un serveur donné.
     * Auth : jeton d'appareil (à ajouter en Phase 4 — intercepteur dédié).
     */
    @POST("api/v1/vpn/session")
    suspend fun createVpnSession(@Body request: VpnSessionRequest): VpnSessionResponse

    /** Clôture / révocation de la session de tunnel. */
    @DELETE("api/v1/vpn/session/{id}")
    suspend fun closeVpnSession(@Path("id") sessionId: String): Response<Unit>
}
