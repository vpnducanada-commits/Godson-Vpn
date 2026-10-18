package com.godson.vpn.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godson.vpn.data.GodsonContainer
import com.godson.vpn.data.ThemeMode
import com.godson.vpn.data.UiSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Préférences — persistance DataStore réelle ; chaque interrupteur agit immédiatement. */
class SettingsViewModel(private val container: GodsonContainer) : ViewModel() {

    val settings: StateFlow<UiSettings> = container.settingsRepository.uiSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiSettings())

    val versionName: String = com.godson.vpn.BuildConfig.VERSION_NAME

    fun setTheme(mode: ThemeMode) = viewModelScope.launch {
        container.settingsRepository.setThemeMode(mode)
    }

    fun setReduceAnimations(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setReduceAnimations(enabled)
    }

    fun setHaptic(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setHapticFeedback(enabled)
    }

    fun setConfirmDisconnect(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setConfirmBeforeDisconnect(enabled)
    }

    fun setKeepLastServer(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setKeepLastServer(enabled)
    }

    fun setAutoReconnect(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setAutoReconnect(enabled)
    }

    fun setAutoConnect(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.setAutoConnectOnStart(enabled)
    }

    /** Effacement total des données locales (le retour d'info est géré par l'UI). */
    fun clearLocalData(onDone: () -> Unit) = viewModelScope.launch {
        container.settingsRepository.clearAll()
        container.vpnStateMachine.reset()
        onDone()
    }
}
