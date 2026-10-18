package com.godson.vpn.data

import com.godson.vpn.data.model.ServerInfo

/** Catégories de l'écran des serveurs. */
enum class ServerCategory { RECOMMENDED, ALL, FAVORITES, RECENT }

/**
 * Logique de recherche / filtrage / tri — Kotlin pur, testée unitairement.
 * Une catégorie vide renvoie une liste vide : l'UI affiche alors son état vide illustré.
 */
object ServerFilter {

    fun filter(
        servers: List<ServerInfo>,
        category: ServerCategory,
        query: String,
        favorites: Set<String>,
        recents: List<String>
    ): List<ServerInfo> {
        val q = query.trim()
        val filtered = servers.filter { s ->
            val inCategory = when (category) {
                ServerCategory.RECOMMENDED -> s.recommended
                ServerCategory.ALL -> true
                ServerCategory.FAVORITES -> s.id in favorites
                ServerCategory.RECENT -> s.id in recents
            }
            val inQuery = q.isEmpty() ||
                listOf(s.country, s.city, s.id, s.hostname, s.protocol)
                    .any { it.contains(q, ignoreCase = true) }
            inCategory && inQuery
        }
        // Contrat « récents » : conserver l'ordre fourni (le plus récent d'abord).
        return if (category == ServerCategory.RECENT) {
            recents.mapNotNull { id -> filtered.firstOrNull { it.id == id } }
        } else {
            filtered
        }
    }

    /** Tri : par latence (indisponibles en dernier) ou recommandation du système. */
    fun sort(
        servers: List<ServerInfo>,
        byLatency: Boolean,
        pingOf: (ServerInfo) -> Int? = { it.pingMs }
    ): List<ServerInfo> {
        val pingOrMax = { s: ServerInfo -> pingOf(s) ?: Int.MAX_VALUE }
        return if (byLatency) {
            servers.sortedWith(compareBy({ !it.available }, pingOrMax))
        } else {
            servers.sortedWith(compareBy({ !it.recommended }, { !it.available }, pingOrMax))
        }
    }

    /**
     * Niveau de qualité (0 à 4) représenté par les barres de signal.
     * Basé sur la latence réelle lorsqu'elle existe ; 0 si indisponible.
     */
    fun signalLevel(pingMs: Int?): Int = when {
        pingMs == null -> 0
        pingMs < 30 -> 4
        pingMs < 60 -> 3
        pingMs < 100 -> 2
        else -> 1
    }
}
