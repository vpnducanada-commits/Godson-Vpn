package com.godson.vpn.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.ui.country.CountryPickerViewModel
import com.godson.vpn.ui.dashboard.DashboardViewModel
import com.godson.vpn.ui.servers.ServersViewModel
import com.godson.vpn.ui.settings.SettingsViewModel
import com.godson.vpn.ui.stats.StatsViewModel

/** Factory unique : tous les ViewModels reçoivent le conteneur applicatif. */
class GodsonViewModelFactory(private val container: GodsonContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container) as T
        modelClass.isAssignableFrom(ServersViewModel::class.java) -> ServersViewModel(container) as T
        modelClass.isAssignableFrom(CountryPickerViewModel::class.java) -> CountryPickerViewModel(container) as T
        modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(container) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container) as T
        else -> throw IllegalArgumentException("ViewModel inconnu : $modelClass")
    }
}
