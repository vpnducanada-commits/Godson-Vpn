package com.godson.vpn.data.model

/**
 * État RÉEL (ou de démonstration étiqueté) d'un serveur, relié à un pays du
 * [CountryCatalog] via [countryCode]. Les informations géographiques vivent
 * dans [CountryEntry] ; cette classe ne porte que l'état opérationnel.
 */
enum class ServerStatus { AVAILABLE, BUSY, UNAVAILABLE, PLANNED }

/**
 * [pingMs] et [loadPercent] ne sont remplis QUE par des données réelles
 * (backend Phase 4, mesure Phase 4/5). Valeur null = « Indisponible » à l'écran.
 * [isDemo] = true ⇒ l'UI DOIT afficher un badge « démo » à côté de ces valeurs.
 */
data class ServerInfo(
    val id: String,
    val countryCode: String,
    val region: Region,
    val flagEmoji: String,
    val country: String,
    val city: String,
    val hostname: String,
    val protocol: String = "WireGuard",
    val pingMs: Int? = null,
    val loadPercent: Int? = null,
    val status: ServerStatus = ServerStatus.PLANNED,
    val recommended: Boolean = false,
    val isDemo: Boolean = true
) {
    /** Connectable : serveur opérationnel (occupé reste connectable, avec avertissement). */
    val available: Boolean
        get() = status == ServerStatus.AVAILABLE || status == ServerStatus.BUSY
}
