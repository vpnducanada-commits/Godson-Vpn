package com.godson.vpn.data

import com.godson.vpn.data.model.Region
import com.godson.vpn.data.model.ServerInfo
import com.godson.vpn.data.model.ServerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests du filtrage / tri / niveau de signal de l'écran des serveurs. */
class ServerFilterTest {

    private val servers = listOf(
        ServerInfo("ga-lbv-01", "GA", Region.AFRICA, "🇬🇦", "Gabon", "Libreville", "ga.demo.invalid",
            pingMs = 9, status = ServerStatus.AVAILABLE, recommended = true),
        ServerInfo("fr-cdg-01", "FR", Region.EUROPE, "🇫🇷", "France", "Paris", "fr.demo.invalid",
            pingMs = 38, status = ServerStatus.AVAILABLE, recommended = true),
        ServerInfo("nl-ams-01", "NL", Region.EUROPE, "🇳🇱", "Pays-Bas", "Amsterdam", "nl.demo.invalid",
            pingMs = null, status = ServerStatus.UNAVAILABLE),
        ServerInfo("us-nyc-01", "US", Region.NORTH_AMERICA, "🇺🇸", "États-Unis", "New York", "us.demo.invalid",
            pingMs = 118, status = ServerStatus.AVAILABLE),
        ServerInfo("jp-tyo-01", "JP", Region.ASIA, "🇯🇵", "Japon", "Tokyo", "jp.demo.invalid",
            pingMs = 186, status = ServerStatus.AVAILABLE)
    )

    @Test
    fun `la recherche insensible à la casse porte sur pays, ville et id`() {
        val result = ServerFilter.filter(servers, ServerCategory.ALL, "paris", emptySet(), emptyList())
        assertEquals(listOf("fr-cdg-01"), result.map { it.id })

        val byId = ServerFilter.filter(servers, ServerCategory.ALL, "GA-LBV", emptySet(), emptyList())
        assertEquals(1, byId.size)
    }

    @Test
    fun `la catégorie favoris ne renvoie que les favoris`() {
        val result = ServerFilter.filter(servers, ServerCategory.FAVORITES, "", setOf("jp-tyo-01"), emptyList())
        assertEquals(listOf("jp-tyo-01"), result.map { it.id })
        assertTrue(ServerFilter.filter(servers, ServerCategory.FAVORITES, "", emptySet(), emptyList()).isEmpty())
    }

    @Test
    fun `la catégorie récents conserve l'ordre fourni`() {
        val recents = listOf("us-nyc-01", "ga-lbv-01")
        val result = ServerFilter.filter(servers, ServerCategory.RECENT, "", emptySet(), recents)
        assertEquals(recents, result.map { it.id })
    }

    @Test
    fun `le tri par latence place les indisponibles en dernier`() {
        val sorted = ServerFilter.sort(servers, byLatency = true)
        assertEquals("ga-lbv-01", sorted.first().id)
        assertEquals("nl-ams-01", sorted.last().id)
        assertTrue(sorted.dropLast(1).map { it.pingMs }.zipWithNext().all { (a, b) -> a!! <= b!! })
    }

    @Test
    fun `le tri par défaut privilégie les recommandés puis la latence`() {
        val sorted = ServerFilter.sort(servers, byLatency = false)
        assertEquals("ga-lbv-01", sorted.first().id)
        assertEquals("fr-cdg-01", sorted[1].id)
    }

    @Test
    fun `le niveau de signal reflète la latence réelle`() {
        assertEquals(4, ServerFilter.signalLevel(9))
        assertEquals(3, ServerFilter.signalLevel(45))
        assertEquals(2, ServerFilter.signalLevel(80))
        assertEquals(1, ServerFilter.signalLevel(150))
        assertEquals(0, ServerFilter.signalLevel(null))
    }

    @Test
    fun `un serveur occupé reste connectable, un planifié non`() {
        val busy = ServerInfo("de-fra-01", "DE", Region.EUROPE, "🇩🇪", "Allemagne", "Francfort", "de.demo.invalid",
            pingMs = 49, status = ServerStatus.BUSY)
        val planned = ServerInfo("ma-ra-01", "MA", Region.AFRICA, "🇲🇦", "Maroc", "Casablanca", "ma.demo.invalid",
            status = ServerStatus.PLANNED)
        assertTrue(busy.available)
        assertTrue(!planned.available)
    }
}
