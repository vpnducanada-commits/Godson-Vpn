package com.godson.vpn.ui.country

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godson.vpn.data.CountryCategory
import com.godson.vpn.data.CountryFilter
import com.godson.vpn.data.CountrySort
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.model.CountryEntry
import com.godson.vpn.data.model.Region
import com.godson.vpn.data.model.ServerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ligne prête à afficher : géographie + état serveur + états UI (favori, récent, sélection). */
data class CountryRowUi(
    val entry: CountryEntry,
    val server: ServerInfo?,
    val isFavorite: Boolean,
    val isRecent: Boolean,
    val isSelected: Boolean
) {
    val selectable: Boolean get() = server?.available == true
}

/**
 * Écran « Choisir un pays » : recherche instantanée, filtres continent /
 * disponibilité, tris, favoris et récents persistants. La sélection enregistre
 * le serveur du pays (DataStore) ; le tableau de bord s'en sert pour se connecter.
 */
class CountryPickerViewModel(private val container: GodsonContainer) : ViewModel() {

    data class Controls(
        val query: String = "",
        val region: Region? = null,
        val availableOnly: Boolean = false,
        val category: CountryCategory = CountryCategory.ALL,
        val sort: CountrySort = CountrySort.ALPHA,
        val loading: Boolean = true,
        val error: Boolean = false
    )

    private val controls = MutableStateFlow(Controls())
    val controlsState: StateFlow<Controls> = controls.asStateFlow()

    /** Ligne d'état intermédiaire (5 flux max par combine). */
    private data class Base(
        val catalog: List<ServerInfo>,
        val ctrl: Controls,
        val favorites: Set<String>,
        val recents: List<String>
    )

    private val base = combine(
        container.serverRepository.catalog,
        controls,
        container.settingsRepository.favoriteCountryIds,
        container.settingsRepository.recentCountryIds
    ) { catalog, ctrl, favorites, recents -> Base(catalog, ctrl, favorites, recents) }

    val visibleRows: StateFlow<List<CountryRowUi>> = combine(
        base,
        container.settingsRepository.lastServerId
    ) { b, selectedServerId ->
        val serverOf: (CountryEntry) -> ServerInfo? = { entry ->
            b.catalog.firstOrNull { it.countryCode == entry.code }
        }
        val filtered = CountryFilter.filter(
            container.serverRepository.countries,
            b.ctrl.query, b.ctrl.region, b.ctrl.availableOnly,
            b.ctrl.category, b.favorites, b.recents, serverOf
        )
        val ordered =
            if (b.ctrl.category == CountryCategory.RECENT) filtered
            else CountryFilter.sort(filtered, b.ctrl.sort, serverOf)
        // Les récents conservent leur ordre propre (le plus récent d'abord).
        ordered.map { entry ->
            val server = serverOf(entry)
            CountryRowUi(
                entry = entry,
                server = server,
                isFavorite = entry.code in b.favorites,
                isRecent = entry.code in b.recents,
                isSelected = server?.id == selectedServerId
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDemo: Boolean = container.serverRepository.isDemoCatalog

    init {
        refresh(initial = true)
    }

    /** Rechargement (backend réel en Phase 4, avec erreur + Réessayer). */
    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            controls.value = controls.value.copy(loading = true, error = false)
            try {
                container.serverRepository.refreshCatalog()
                if (initial) kotlinx.coroutines.delay(350)
                controls.value = controls.value.copy(loading = false)
            } catch (_: Exception) {
                controls.value = controls.value.copy(loading = false, error = true)
            }
        }
    }

    fun setQuery(query: String) {
        controls.value = controls.value.copy(query = query)
    }

    fun setRegion(region: Region?) {
        controls.value = controls.value.copy(region = region)
    }

    fun setAvailableOnly(enabled: Boolean) {
        controls.value = controls.value.copy(availableOnly = enabled)
    }

    fun setCategory(category: CountryCategory) {
        controls.value = controls.value.copy(category = category)
    }

    fun setSort(sort: CountrySort) {
        controls.value = controls.value.copy(sort = sort)
    }

    fun toggleFavorite(countryCode: String) {
        viewModelScope.launch { container.settingsRepository.toggleFavoriteCountry(countryCode) }
    }

    /**
     * Sélection d'un pays : seul un pays avec serveur opérationnel est sélectionnable
     * (un pays « Bientôt disponible » ne peut jamais déclencher une connexion).
     * @return true si la sélection a été enregistrée.
     */
    fun select(entry: CountryEntry): Boolean {
        val server = container.serverRepository.serverForCountry(entry.code) ?: return false
        if (!server.available) return false
        viewModelScope.launch {
            container.settingsRepository.setLastServer(server.id)
            container.settingsRepository.addRecentCountry(entry.code)
        }
        return true
    }
}
