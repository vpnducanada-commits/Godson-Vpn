package com.godson.vpn.api

import com.godson.vpn.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Client API : TLS par défaut d'OkHttp (validation de certificats standard,
 * aucun TrustManager permissif), journalisation réseau désactivée en release
 * (aucune donnée sensible dans les logs).
 */
object ApiClient {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun create(baseUrl: String = BuildConfig.API_BASE_URL): GodsonApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            // Phase 4 : ajouter ici l'intercepteur d'authentification par jeton
            // d'appareil (révocable côté serveur). Aucun secret d'administration
            // ne doit jamais être intégré au client.
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GodsonApi::class.java)
    }
}
