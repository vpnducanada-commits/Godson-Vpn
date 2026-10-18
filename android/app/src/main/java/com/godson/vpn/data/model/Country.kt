package com.godson.vpn.data.model

/**
 * Informations GÉOGRAPHIQUES pures — séparées de l'état réel des serveurs
 * (exigence §6 de la spécification « catalogue de pays »).
 * Enrichissement : ajouter une [CountryEntry] ici ; l'état opérationnel vient
 * du backend (Phase 4) ou du catalogue de démonstration.
 */
enum class Region { AFRICA, EUROPE, NORTH_AMERICA, SOUTH_AMERICA, ASIA, OCEANIA }

data class CountryEntry(
    val code: String,        // ISO 3166-1 alpha-2
    val flagEmoji: String,   // drapeau (séquences d'indicateurs régionaux Unicode)
    val nameFr: String,
    val nameEn: String,
    val namePt: String,
    val region: Region,
    val candidateCity: String? = null // ville pressentie pour un futur serveur (info ops)
)

object CountryCatalog {

    /**
     * Catalogue initial des destinations potentielles — 30 pays, 6 régions.
     * ATTENTION : c'est une liste de destinations POTENTIELLES. Un pays n'est
     * « disponible » que si un serveur VPN réellement configuré y est associé
     * (voir [DemoServerCatalog] / backend Phase 4). Les autres affichent
     * « Bientôt disponible » et ne peuvent pas déclencher de connexion.
     */
    val countries: List<CountryEntry> = listOf(
        // ── Afrique ──────────────────────────────────────────────────
        CountryEntry("GA", "🇬🇦", "Gabon", "Gabon", "Gabão", Region.AFRICA, "Libreville"),
        CountryEntry("ZA", "🇿🇦", "Afrique du Sud", "South Africa", "África do Sul", Region.AFRICA, "Johannesburg"),
        CountryEntry("MA", "🇲🇦", "Maroc", "Morocco", "Marrocos", Region.AFRICA, "Casablanca"),
        CountryEntry("EG", "🇪🇬", "Égypte", "Egypt", "Egito", Region.AFRICA, "Le Caire"),
        CountryEntry("KE", "🇰🇪", "Kenya", "Kenya", "Quénia", Region.AFRICA, "Nairobi"),
        CountryEntry("NG", "🇳🇬", "Nigeria", "Nigeria", "Nigéria", Region.AFRICA, "Lagos"),
        CountryEntry("GH", "🇬🇭", "Ghana", "Ghana", "Gana", Region.AFRICA, "Accra"),
        // ── Europe ───────────────────────────────────────────────────
        CountryEntry("FR", "🇫🇷", "France", "France", "França", Region.EUROPE, "Paris"),
        CountryEntry("DE", "🇩🇪", "Allemagne", "Germany", "Alemanha", Region.EUROPE, "Francfort"),
        CountryEntry("BE", "🇧🇪", "Belgique", "Belgium", "Bélgica", Region.EUROPE, "Bruxelles"),
        CountryEntry("CH", "🇨🇭", "Suisse", "Switzerland", "Suíça", Region.EUROPE, "Zurich"),
        CountryEntry("GB", "🇬🇧", "Royaume-Uni", "United Kingdom", "Reino Unido", Region.EUROPE, "Londres"),
        CountryEntry("ES", "🇪🇸", "Espagne", "Spain", "Espanha", Region.EUROPE, "Madrid"),
        CountryEntry("IT", "🇮🇹", "Italie", "Italy", "Itália", Region.EUROPE, "Milan"),
        CountryEntry("NL", "🇳🇱", "Pays-Bas", "Netherlands", "Países Baixos", Region.EUROPE, "Amsterdam"),
        CountryEntry("PT", "🇵🇹", "Portugal", "Portugal", "Portugal", Region.EUROPE, "Lisbonne"),
        CountryEntry("SE", "🇸🇪", "Suède", "Sweden", "Suécia", Region.EUROPE, "Stockholm"),
        // ── Amérique du Nord ─────────────────────────────────────────
        CountryEntry("US", "🇺🇸", "États-Unis", "United States", "Estados Unidos", Region.NORTH_AMERICA, "New York"),
        CountryEntry("CA", "🇨🇦", "Canada", "Canada", "Canadá", Region.NORTH_AMERICA, "Toronto"),
        CountryEntry("MX", "🇲🇽", "Mexique", "Mexico", "México", Region.NORTH_AMERICA, "Mexico"),
        // ── Amérique du Sud ──────────────────────────────────────────
        CountryEntry("BR", "🇧🇷", "Brésil", "Brazil", "Brasil", Region.SOUTH_AMERICA, "São Paulo"),
        CountryEntry("AR", "🇦🇷", "Argentine", "Argentina", "Argentina", Region.SOUTH_AMERICA, "Buenos Aires"),
        CountryEntry("CL", "🇨🇱", "Chili", "Chile", "Chile", Region.SOUTH_AMERICA, "Santiago"),
        // ── Asie ─────────────────────────────────────────────────────
        CountryEntry("JP", "🇯🇵", "Japon", "Japan", "Japão", Region.ASIA, "Tokyo"),
        CountryEntry("SG", "🇸🇬", "Singapour", "Singapore", "Singapura", Region.ASIA, "Singapour"),
        CountryEntry("IN", "🇮🇳", "Inde", "India", "Índia", Region.ASIA, "Mumbai"),
        CountryEntry("KR", "🇰🇷", "Corée du Sud", "South Korea", "Coreia do Sul", Region.ASIA, "Séoul"),
        CountryEntry("AE", "🇦🇪", "Émirats arabes unis", "United Arab Emirates", "Emirados Árabes Unidos", Region.ASIA, "Dubaï"),
        // ── Océanie ──────────────────────────────────────────────────
        CountryEntry("AU", "🇦🇺", "Australie", "Australia", "Austrália", Region.OCEANIA, "Sydney"),
        CountryEntry("NZ", "🇳🇿", "Nouvelle-Zélande", "New Zealand", "Nova Zelândia", Region.OCEANIA, "Auckland")
    )

    private val byCode = countries.associateBy { it.code }

    fun country(code: String): CountryEntry? = byCode[code]
}
