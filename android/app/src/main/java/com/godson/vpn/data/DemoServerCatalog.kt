package com.godson.vpn.data

import com.godson.vpn.data.model.CountryCatalog
import com.godson.vpn.data.model.ServerInfo
import com.godson.vpn.data.model.ServerStatus

/**
 * État de DÉMONSTRATION des serveurs — associé au catalogue de pays.
 * Aucun de ces serveurs n'existe réellement : les hôtes utilisent le TLD
 * `.invalid` (RFC 2606) et toutes les mesures sont étiquetées « démo » dans l'UI.
 * Remplacé par le backend en Phase 4 (même schéma de données).
 */
object DemoServerCatalog {

    const val NOTICE = "Statuts de serveurs fictifs (démo) — remplacés par le backend en Phase 4"

    private data class DemoServer(
        val code: String,
        val id: String,
        val city: String,
        val pingMs: Int?,
        val status: ServerStatus,
        val recommended: Boolean,
        val loadPercent: Int?
    )

    // Seuls ces pays ont un « serveur » de démonstration : tous les autres
    // pays du catalogue restent « Bientôt disponible » (aucune fausse connexion).
    private val demo = listOf(
        DemoServer("GA", "ga-lbv-01", "Libreville", 9, ServerStatus.AVAILABLE, recommended = true, loadPercent = 42),
        DemoServer("NG", "ng-lag-01", "Lagos", 35, ServerStatus.AVAILABLE, recommended = false, loadPercent = 38),
        DemoServer("FR", "fr-cdg-01", "Paris", 38, ServerStatus.AVAILABLE, recommended = true, loadPercent = 55),
        DemoServer("DE", "de-fra-01", "Francfort", 49, ServerStatus.BUSY, recommended = false, loadPercent = 88),
        DemoServer("GB", "gb-lhr-01", "Londres", 54, ServerStatus.AVAILABLE, recommended = true, loadPercent = 51),
        DemoServer("NL", "nl-ams-01", "Amsterdam", null, ServerStatus.UNAVAILABLE, recommended = false, loadPercent = null),
        DemoServer("ZA", "za-jnb-01", "Johannesburg", 64, ServerStatus.AVAILABLE, recommended = false, loadPercent = 33),
        DemoServer("US", "us-nyc-01", "New York", 118, ServerStatus.AVAILABLE, recommended = false, loadPercent = 47),
        DemoServer("CA", "ca-yyz-01", "Toronto", 126, ServerStatus.AVAILABLE, recommended = false, loadPercent = 29),
        DemoServer("JP", "jp-tyo-01", "Tokyo", 186, ServerStatus.AVAILABLE, recommended = false, loadPercent = 24)
    )

    /** Jointure catalogue géographique × état de démonstration. */
    fun build(): List<ServerInfo> = demo.mapNotNull { d ->
        val country = CountryCatalog.country(d.code) ?: return@mapNotNull null
        ServerInfo(
            id = d.id,
            countryCode = country.code,
            region = country.region,
            flagEmoji = country.flagEmoji,
            country = country.nameFr,
            city = d.city,
            hostname = "${d.id}.demo.invalid",
            pingMs = d.pingMs,
            loadPercent = d.loadPercent,
            status = d.status,
            recommended = d.recommended,
            isDemo = true
        )
    }
}
