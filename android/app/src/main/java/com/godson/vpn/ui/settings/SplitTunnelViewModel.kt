package com.godson.vpn.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.SplitTunnelPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Écran « Applications » du tunnelage fractionné : liste RÉELLE des applications
 * installées (PackageManager, chargée une fois à l'entrée sur l'écran), recherche
 * instantanée, sélection persistée (DataStore). Aucun inventaire fictif : si le
 * système ne renvoie rien, la liste est vide — jamais complétée par des entrées
 * inventées. L'application GODSON VPN elle-même n'est pas listée (garde-fou
 * [SplitTunnelPolicy.SELF_PACKAGE]).
 */
class SplitTunnelViewModel(private val container: GodsonContainer) : ViewModel() {

    data class AppRow(val pkg: String, val label: String, val selected: Boolean)

    val enabled: StateFlow<Boolean> = container.settingsRepository.splitTunnelingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val mode: StateFlow<SplitTunnelPolicy.Mode> =
        container.settingsRepository.splitTunnelModeKey
            .map { SplitTunnelPolicy.Mode.fromStorageKey(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SplitTunnelPolicy.Mode.BLOCK)

    val selectedCount: StateFlow<Int> = container.settingsRepository.splitTunnelPackages
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val query = MutableStateFlow("")
    val queryState: StateFlow<String> = query.asStateFlow()

    private val installed = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    val rows: StateFlow<List<AppRow>> = combine(
        installed,
        query,
        container.settingsRepository.splitTunnelPackages
    ) { apps, q, selected ->
        SplitTunnelPolicy.visibleApps(apps, q).map { (pkg, label) ->
            AppRow(pkg, label, pkg in selected)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Avertissement actif : mode ALLOW + rien de sélectionné ⇒ rien ne passera par le VPN. */
    val warnEmptyAllowlist: StateFlow<Boolean> = combine(
        enabled,
        mode,
        container.settingsRepository.splitTunnelPackages
    ) { on, m, selected -> on && SplitTunnelPolicy.mustWarnEmptyAllowlist(m, selected) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadInstalledApps()
    }

    /** Inventaire réel, chargé UNE FOIS à la demande (jamais en boucle de fond). */
    private fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = container.appContext.packageManager
            installed.value = withContext(Dispatchers.Default) {
                runCatching {
                    pm.getInstalledApplications(0).mapNotNull { info ->
                        if (pm.getLaunchIntentForPackage(info.packageName) == null) return@mapNotNull null
                        val label = runCatching {
                            pm.getApplicationLabel(info).toString()
                        }.getOrDefault(info.packageName)
                        info.packageName to label
                    }
                }.getOrDefault(emptyList())
            }
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setEnabled(value: Boolean) = viewModelScope.launch {
        container.settingsRepository.setSplitTunnelingEnabled(value)
    }

    fun setMode(value: SplitTunnelPolicy.Mode) = viewModelScope.launch {
        container.settingsRepository.setSplitTunnelMode(value.storageKey)
    }

    fun toggle(pkg: String) = viewModelScope.launch {
        container.settingsRepository.toggleSplitTunnelApp(pkg)
    }
}
