package com.godson.vpn.data

import com.godson.vpn.api.GodsonApi
import com.godson.vpn.data.model.CountryCatalog
import com.godson.vpn.data.model.CountryEntry
import com.godson.vpn.data.model.Region
import com.godson.vpn.data.model.ServerInfo
import com.godson.vpn.data.model.ServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Source des pays et des serveurs.
 *
 * - Mode DÉMO : [DemoServerCatalog] (étiqueté « démo » dans toute l'UI).
 * - Mode RÉEL : GET /api/v1/servers, mappé vers le même modèle ; l'erreur réseau
 *   est remontée à l'UI (état d'erreur + bouton Réessayer).
 *
 * La géographie ([CountryCatalog]) reste une source statique locale : l'état
 * opérationnel, lui, vient toujours du backend en mode réel.
 */
class ServerRepository(
    private val isRealMode: Boolean = false,
    private val api: GodsonApi? = null
) {

    /** true tant que le backend n'est pas branché. L'UI doit afficher un badge « démo ». */
    val isDemoCatalog: Boolean
        get() = !isRealMode

    private val _catalog = MutableStateFlow(DemoServerCatalog.build())

    /** Serveurs connus (état réel ou démo étiquetée) — observé par l'UI. */
    val catalog: StateFlow<List<ServerInfo>> = _catalog.asStateFlow()

    /** Catalogue géographique complet (toutes les destinations potentielles). */
    val countries: List<CountryEntry>
        get() = CountryCatalog.countries

    /**
     * Rafraîchissement de l'état des serveurs.
     * @throws Exception en mode réel si le backend est injoignable (UI → erreur + Réessayer).
     */
    suspend fun refreshCatalog(): List<ServerInfo> {
        if (!isRealMode || api == null) {
            kotlinx.coroutines.delay(500) // lecture locale de démonstration
            _catalog.value = DemoServerCatalog.build()
            return _catalog.value
        }
        val response = api.servers()
        _catalog.value = response.servers.mapNotNull { dto ->
            val country = CountryCatalog.country(dto.countryCode.uppercase())
                ?: CountryEntry(
                    code = dto.countryCode.uppercase(),
                    flagEmoji = flagEmojiFor(dto.countryCode),
                    nameFr = dto.countryCode,
                    nameEn = dto.countryCode,
                    namePt = dto.countryCode,
                    region = Region.EUROPE,
                    candidateCity = null
                )
            ServerInfo(
                id = dto.id,
                countryCode = country.code,
                region = country.region,
                flagEmoji = country.flagEmoji,
                country = country.nameFr,
                city = dto.city,
                hostname = dto.hostname,
                protocol = dto.protocol,
                pingMs = dto.pingMs,
                loadPercent = dto.loadPercent,
                status = parseStatus(dto.status),
                recommended = dto.recommended,
                isDemo = false
            )
        }
        return _catalog.value
    }

    private fun parseStatus(raw: String): ServerStatus =
        runCatching { ServerStatus.valueOf(raw.uppercase()) }
            .getOrDefault(ServerStatus.PLANNED)

    /** Drapeau généré depuis le code ISO (indicateurs régionaux Unicode). */
    private fun flagEmojiFor(code: String): String =
        code.uppercase().map { c -> 0x1F1E6 + (c - 'A') }
            .map { Character.toChars(it) }
            .joinToString("") { String(it) }

    suspend fun getServers(): List<ServerInfo> = _catalog.value

    /** Serveur associé à un pays — null si aucun serveur opérationnel n'y est configuré. */
    fun serverForCountry(countryCode: String): ServerInfo? =
        _catalog.value.firstOrNull { it.countryCode == countryCode }

    /**
     * Latence réelle — implémentée en Phase 4/5 (mesure via handshake WireGuard
     * ou sonde dédiée du backend). Aucune valeur fictive n'est renvoyée ici :
     * null = « Indisponible ». (Le catalogue de DÉMONSTRATION porte des valeurs
     * étiquetées « démo ».)
     */
    suspend fun measurePing(server: ServerInfo): Int? = null

    /** Alternative proposée lorsqu'un serveur est indisponible. */
    fun findAlternative(excludingIds: Set<String>): ServerInfo? =
        _catalog.value.firstOrNull { it.available && it.id !in excludingIds }
}
