package com.godson.vpn.data

import com.godson.vpn.data.model.CountryCatalog
import com.godson.vpn.data.model.Region
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garde-fous du catalogue de pays (spécification « Catalogue de pays ») :
 * liste exacte des 30 pays, drapeaux valides, séparation géographie × état serveur,
 * et comportement des filtres/tris de l'écran « Choisir un pays ».
 */
class CountryCatalogTest {

    private val expectedCodes = setOf(
        // Afrique
        "GA", "ZA", "MA", "EG", "KE", "NG", "GH",
        // Europe
        "FR", "DE", "BE", "CH", "GB", "ES", "IT", "NL", "PT", "SE",
        // Amérique du Nord
        "US", "CA", "MX",
        // Amérique du Sud
        "BR", "AR", "CL",
        // Asie
        "JP", "SG", "IN", "KR", "AE",
        // Océanie
        "AU", "NZ"
    )

    @Test
    fun `le catalogue contient exactement les 30 pays demandés`() {
        assertEquals(30, CountryCatalog.countries.size)
        assertEquals(expectedCodes, CountryCatalog.countries.map { it.code }.toSet())
    }

    @Test
    fun `chaque pays possède un drapeau emoji valide (indicateurs régionaux)`() {
        CountryCatalog.countries.forEach { c ->
            assertTrue("Drapeau invalide pour ${c.code}", c.flagEmoji.isNotEmpty())
            val first = c.flagEmoji.codePointAt(0)
            assertTrue(
                "Drapeau invalide pour ${c.code}",
                first in 0x1F1E6..0x1F1FF
            )
        }
    }

    @Test
    fun `les noms français sont uniques et non vides`() {
        val names = CountryCatalog.countries.map { it.nameFr }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.none { it.isBlank() })
    }

    @Test
    fun `les 6 régions sont représentées`() {
        val regions = CountryCatalog.countries.map { it.region }.toSet()
        assertEquals(Region.entries.toSet(), regions)
    }

    @Test
    fun `les serveurs de démonstration correspondent à des pays du catalogue`() {
        val servers = DemoServerCatalog.build()
        assertEquals(10, servers.size)
        servers.forEach { s ->
            assertEquals(CountryCatalog.country(s.countryCode)?.code, s.countryCode)
        }
    }

    @Test
    fun `la plupart des pays n'ont pas de serveur — et ne peuvent pas simuler une connexion`() {
        val serverCodes = DemoServerCatalog.build().map { it.countryCode }.toSet()
        val withoutServer = expectedCodes - serverCodes
        assertTrue("Il doit rester des pays « bientôt disponible »", withoutServer.size >= 15)
        assertTrue(withoutServer.contains("MA"))
        assertTrue(withoutServer.contains("BR"))
    }

    // ── CountryFilter ────────────────────────────────────────────────────

    private val countries = CountryCatalog.countries
    private val serverOf: (String) -> com.godson.vpn.data.model.ServerInfo? =
        { code -> DemoServerCatalog.build().firstOrNull { it.countryCode == code } }

    @Test
    fun `la recherche trouve un pays par son nom français, anglais ou son code`() {
        val byFr = CountryFilter.filter(countries, "suisse", null, false, CountryCategory.ALL, emptySet(), emptyList()) {
            serverOf(it.code)
        }
        assertEquals(listOf("CH"), byFr.map { it.code })

        val byCode = CountryFilter.filter(countries, "kr", null, false, CountryCategory.ALL, emptySet(), emptyList()) {
            serverOf(it.code)
        }
        assertEquals(listOf("KR"), byCode.map { it.code })
    }

    @Test
    fun `le filtre par continent renvoie les pays de la région`() {
        val africa = CountryFilter.filter(countries, "", Region.AFRICA, false, CountryCategory.ALL, emptySet(), emptyList()) {
            serverOf(it.code)
        }
        assertEquals(7, africa.size)
        assertTrue(africa.all { it.region == Region.AFRICA })
    }

    @Test
    fun `disponibles uniquement exclut les pays sans serveur opérationnel`() {
        val availableOnly = CountryFilter.filter(countries, "", null, true, CountryCategory.ALL, emptySet(), emptyList()) {
            serverOf(it.code)
        }
        // 10 serveurs dont 1 indisponible (NL) ⇒ 9 connectables
        assertEquals(9, availableOnly.size)
        assertTrue(availableOnly.none { it.code == "NL" })
    }

    @Test
    fun `favoris et récents filtrent par code pays`() {
        val favs = CountryFilter.filter(countries, "", null, false, CountryCategory.FAVORITES, setOf("JP", "FR"), emptyList()) {
            serverOf(it.code)
        }
        assertEquals(setOf("JP", "FR"), favs.map { it.code }.toSet())

        val recents = CountryFilter.filter(countries, "", null, false, CountryCategory.RECENT, emptySet(), listOf("NZ", "GA")) {
            serverOf(it.code)
        }
        assertEquals(listOf("NZ", "GA"), recents.map { it.code })
    }

    @Test
    fun `le tri alphabétique place les pays sans serveur après les connectables`() {
        val sorted = CountryFilter.sort(countries, CountrySort.ALPHA) { serverOf(it.code) }
        val firstNine = sorted.take(9).map { it.code }.toSet()
        assertEquals(setOf("GA", "NG", "FR", "DE", "GB", "ZA", "US", "CA", "JP"), firstNine)
        assertEquals("NL", sorted[9].code) // indisponible juste après
        // tous les sans-serveur en queue
        sorted.drop(10).forEach { entry ->
            assertEquals(null, serverOf(entry.code))
        }
    }

    @Test
    fun `le tri par latence place le serveur le plus rapide d'abord`() {
        val sorted = CountryFilter.sort(countries, CountrySort.LATENCY) { serverOf(it.code) }
        assertEquals("GA", sorted.first().code) // 9 ms
        assertEquals("JP", sorted[8].code)      // 186 ms, dernier des disponibles
    }
}
