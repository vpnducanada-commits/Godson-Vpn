package com.godson.vpn.data

import com.godson.vpn.data.model.CountryEntry
import com.godson.vpn.data.model.Region
import com.godson.vpn.data.model.ServerInfo
import com.godson.vpn.data.model.ServerStatus

/** Catégories de l'écran « Choisir un pays ». */
enum class CountryCategory { ALL, FAVORITES, RECENT }

/** Tris disponibles : alphabétique (défaut) ou par latence (mesures réelles/étiquetées). */
enum class CountrySort { ALPHA, LATENCY }

/**
 * Recherche / filtres / tri du catalogue de pays — Kotlin pur, testé unitairement.
 * Séparation stricte : géographie ([CountryEntry]) × état serveur ([ServerInfo]).
 */
object CountryFilter {

    fun filter(
        countries: List<CountryEntry>,
        query: String,
        region: Region?,
        availableOnly: Boolean,
        category: CountryCategory,
        favorites: Set<String>,
        recents: List<String>,
        serverOf: (CountryEntry) -> ServerInfo?
    ): List<CountryEntry> {
        val q = query.trim()
        val filtered = countries.filter { c ->
            val server = serverOf(c)
            val inRegion = region == null || c.region == region
            val inCategory = when (category) {
                CountryCategory.ALL -> true
                CountryCategory.FAVORITES -> c.code in favorites
                CountryCategory.RECENT -> c.code in recents
            }
            val inQuery = q.isEmpty() || listOf(
                c.nameFr, c.nameEn, c.namePt, c.code,
                c.candidateCity.orEmpty(), server?.city.orEmpty()
            ).any { it.contains(q, ignoreCase = true) }
            val availableOk = !availableOnly || (server?.available == true)
            inRegion && inCategory && inQuery && availableOk
        }
        // Contrat « récents » : conserver l'ordre fourni (le plus récent d'abord).
        return if (category == CountryCategory.RECENT) {
            recents.mapNotNull { code -> filtered.firstOrNull { it.code == code } }
        } else {
            filtered
        }
    }

    /**
     * Tri alphabétique (nom français) ou par latence — les pays sans serveur
     * opérationnel passent toujours en dernier, pour mettre en avant le connectable.
     */
    fun sort(
        countries: List<CountryEntry>,
        sort: CountrySort,
        serverOf: (CountryEntry) -> ServerInfo?
    ): List<CountryEntry> {
        val name = { c: CountryEntry -> c.nameFr.lowercase() }
        val rank = { c: CountryEntry ->
            val server = serverOf(c)
            when {
                server == null -> 2                     // bientôt disponible
                server.status == ServerStatus.UNAVAILABLE -> 1
                else -> 0                               // disponible / occupé
            }
        }
        val ping = { c: CountryEntry -> serverOf(c)?.pingMs ?: Int.MAX_VALUE }
        return when (sort) {
            CountrySort.ALPHA ->
                countries.sortedWith(compareBy(rank, name))
            CountrySort.LATENCY ->
                countries.sortedWith(compareBy(rank, ping, name))
        }
    }
}
