package com.godson.vpn.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.ServerCategory
import com.godson.vpn.data.ServerFilter
import com.godson.vpn.data.model.ServerInfo
import com.godson.vpn.pulse.ProbeResult
import com.godson.vpn.pulse.PulseEngine
import com.godson.vpn.pulse.PulseReport
import com.godson.vpn.pulse.PulseScore
import com.godson.vpn.pulse.PulseTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * État de l'écran des serveurs : recherche instantanée, catégories, tri par latence,
 * favoris et récents persistants (DataStore). Aucun serveur fictif n'est présenté
 * comme opérationnel : les mesures de démo restent étiquetées côté UI.
 */
class ServersViewModel(private val container: GodsonContainer) : ViewModel() {

    data class Controls(
        val category: ServerCategory = ServerCategory.RECOMMENDED,
        val query: String = "",
        val sortByLatency: Boolean = false,
        val loading: Boolean = true
    )

    private val controls = MutableStateFlow(Controls())
    val controlsState: StateFlow<Controls> = controls.asStateFlow()

    /** Liste filtrée + triée, recalculée à chaque changement (combine). */
    val visibleServers: StateFlow<List<ServerInfo>> = combine(
        container.serverRepository.catalog,
        controls,
        container.settingsRepository.favoriteServerIds,
        container.settingsRepository.recentServerIds
    ) { catalog, ctrl, favorites, recents ->
        val filtered = ServerFilter.filter(catalog, ctrl.category, ctrl.query, favorites, recents)
        // Les récents conservent leur ordre propre (le plus récent d'abord).
        if (ctrl.category == ServerCategory.RECENT) filtered
        else ServerFilter.sort(filtered, ctrl.sortByLatency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<Set<String>> = container.settingsRepository.favoriteServerIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val recents: StateFlow<List<String>> = container.settingsRepository.recentServerIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDemo: Boolean = container.serverRepository.isDemoCatalog

    /** Résultat de l'évaluation GODSON PULSE affichée sur l'écran des serveurs. */
    data class PulseEvaluation(
        val report: PulseReport? = null,
        /** Score 0–100 par identifiant — seuls les serveurs réellement évalués y figurent. */
        val scoreByServerId: Map<String, Int> = emptyMap()
    )

    private val pulseEngine = PulseEngine()
    private val _pulse = MutableStateFlow(PulseEvaluation())
    val pulseState: StateFlow<PulseEvaluation> = _pulse.asStateFlow()

    private var loadedOnce = false

    init {
        refresh(initial = true)
    }

    /** Rechargement du catalogue (backend réel en Phase 4). */
    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            controls.value = controls.value.copy(loading = true)
            container.serverRepository.refreshCatalog()
            if (initial && !loadedOnce) {
                // Premier chargement : court temps de lecture locale (skeletons visibles).
                kotlinx.coroutines.delay(350)
                loadedOnce = true
            }
            controls.value = controls.value.copy(loading = false)
            // Évaluation PULSE à la demande (ouverture de l'écran ou bouton de
            // rafraîchissement) : une passe séquentielle, aucune boucle de fond.
            evaluatePulse()
        }
    }

    /**
     * Évaluation GODSON PULSE à la demande : une seule passe séquentielle sur les
     * serveurs disponibles du catalogue. Aucune valeur aléatoire : en mode démo, la
     * sonde DemoProber produit des valeurs déterministes étiquetées (source DEMO) ;
     * une sonde réelle s'injectera via le conteneur (Phase backend) et le moteur
     * écartera automatiquement les données de démonstration.
     */
    private fun evaluatePulse() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val prober = container.pulseProber
            val targets = container.serverRepository.catalog.value
                .filter { it.available }
                .map { PulseTarget(serverId = it.id, host = it.hostname, isDemoTarget = it.isDemo) }
            val results: List<ProbeResult> = targets.map { target ->
                runCatching { prober.probe(target) }.getOrElse {
                    ProbeResult.unavailable(
                        serverId = target.serverId,
                        source = prober.source,
                        timestampMillis = now,
                        detail = "Exception de sonde : ${it.javaClass.simpleName}"
                    )
                }
            }
            _pulse.value = PulseEvaluation(
                report = pulseEngine.evaluate(results, now),
                scoreByServerId = results
                    .mapNotNull { r -> PulseScore.compute(r)?.let { r.serverId to it } }
                    .toMap()
            )
        }
    }

    fun setCategory(category: ServerCategory) {
        controls.value = controls.value.copy(category = category)
    }

    fun setQuery(query: String) {
        controls.value = controls.value.copy(query = query)
    }

    fun toggleSortByLatency() {
        controls.value = controls.value.copy(sortByLatency = !controls.value.sortByLatency)
    }

    fun toggleFavorite(serverId: String) {
        viewModelScope.launch { container.settingsRepository.toggleFavorite(serverId) }
    }

    /** Sélection d'un serveur : persiste dernier serveur + liste des récents. */
    fun select(server: ServerInfo) {
        viewModelScope.launch {
            container.settingsRepository.setLastServer(server.id)
            container.settingsRepository.addRecent(server.id)
        }
    }
}
