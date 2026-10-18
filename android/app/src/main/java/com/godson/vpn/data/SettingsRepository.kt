package com.godson.vpn.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "godson_settings")

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class UiSettings(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reduceAnimations: Boolean = false,
    val confirmBeforeDisconnect: Boolean = false,
    val hapticFeedback: Boolean = true,
    val autoReconnect: Boolean = true,
    val keepLastServer: Boolean = true,
    val autoConnectOnStart: Boolean = false,
    val localHistoryEnabled: Boolean = false
)

/**
 * Préférences locales (DataStore). Aucune donnée sensible ici :
 * pas de clé privée, pas d'identifiant personnel — conformité §7 de l'architecture.
 * Favoris, récents et dernier serveur sont persistés (requis §7 de la refonte).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val THEME = stringPreferencesKey("theme_mode")
        val REDUCE_ANIM = booleanPreferencesKey("reduce_animations")
        val CONFIRM_DISCONNECT = booleanPreferencesKey("confirm_disconnect")
        val HAPTIC = booleanPreferencesKey("haptic_feedback")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val KEEP_LAST_SERVER = booleanPreferencesKey("keep_last_server")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect_on_start")
        val LOCAL_HISTORY = booleanPreferencesKey("local_history_enabled")
        val LAST_SERVER = stringPreferencesKey("last_server_id")
        val FAVORITES = stringSetPreferencesKey("favorite_servers")
        val RECENTS = stringPreferencesKey("recent_servers_csv")
        val FAVORITE_COUNTRIES = stringSetPreferencesKey("favorite_countries")
        val RECENT_COUNTRIES = stringPreferencesKey("recent_countries_csv")
        val DEVICE_ID = stringPreferencesKey("device_id")
    }

    val uiSettings: Flow<UiSettings> = context.dataStore.data.map { p ->
        UiSettings(
            onboardingCompleted = p[Keys.ONBOARDING] ?: false,
            themeMode = p[Keys.THEME]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            reduceAnimations = p[Keys.REDUCE_ANIM] ?: false,
            confirmBeforeDisconnect = p[Keys.CONFIRM_DISCONNECT] ?: false,
            hapticFeedback = p[Keys.HAPTIC] ?: true,
            autoReconnect = p[Keys.AUTO_RECONNECT] ?: true,
            keepLastServer = p[Keys.KEEP_LAST_SERVER] ?: true,
            autoConnectOnStart = p[Keys.AUTO_CONNECT] ?: false,
            localHistoryEnabled = p[Keys.LOCAL_HISTORY] ?: false
        )
    }

    val favoriteServerIds: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.FAVORITES] ?: emptySet() }

    val recentServerIds: Flow<List<String>> =
        context.dataStore.data.map { p ->
            p[Keys.RECENTS]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
        }

    val lastServerId: Flow<String?> =
        context.dataStore.data.map { it[Keys.LAST_SERVER] }

    /** Pays favoris (codes ISO) — persistés, survivent à la fermeture de l'app. */
    val favoriteCountryIds: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.FAVORITE_COUNTRIES] ?: emptySet() }

    /** Pays récemment utilisés (codes ISO, plus récent d'abord). */
    val recentCountryIds: Flow<List<String>> =
        context.dataStore.data.map { p ->
            p[Keys.RECENT_COUNTRIES]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
        }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[Keys.ONBOARDING] = true }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setReduceAnimations(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_ANIM] = enabled }
    }

    suspend fun setConfirmBeforeDisconnect(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONFIRM_DISCONNECT] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC] = enabled }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
    }

    suspend fun setKeepLastServer(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_LAST_SERVER] = enabled }
    }

    suspend fun setAutoConnectOnStart(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CONNECT] = enabled }
    }

    suspend fun setLastServer(serverId: String) {
        context.dataStore.edit { it[Keys.LAST_SERVER] = serverId }
    }

    suspend fun toggleFavorite(serverId: String) {
        context.dataStore.edit { p ->
            val current = p[Keys.FAVORITES] ?: emptySet()
            p[Keys.FAVORITES] = if (serverId in current) current - serverId else current + serverId
        }
    }

    suspend fun addRecent(serverId: String) {
        context.dataStore.edit { p ->
            val current = p[Keys.RECENTS]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(serverId) + current.filter { it != serverId }).take(5)
            p[Keys.RECENTS] = updated.joinToString(",")
        }
    }

    suspend fun toggleFavoriteCountry(countryCode: String) {
        context.dataStore.edit { p ->
            val current = p[Keys.FAVORITE_COUNTRIES] ?: emptySet()
            p[Keys.FAVORITE_COUNTRIES] =
                if (countryCode in current) current - countryCode else current + countryCode
        }
    }

    suspend fun addRecentCountry(countryCode: String) {
        context.dataStore.edit { p ->
            val current = p[Keys.RECENT_COUNTRIES]?.split(',')?.filter { it.isNotBlank() }
                ?: emptyList()
            val updated = (listOf(countryCode) + current.filter { it != countryCode }).take(5)
            p[Keys.RECENT_COUNTRIES] = updated.joinToString(",")
        }
    }

    /** Identifiant d'appareil anonyme, généré localement au premier usage. */
    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.first()[Keys.DEVICE_ID]
        if (existing != null) return existing
        val created = UUID.randomUUID().toString()
        context.dataStore.edit { it[Keys.DEVICE_ID] = created }
        return created
    }

    suspend fun resetDeviceId(): String {
        val created = UUID.randomUUID().toString()
        context.dataStore.edit { it[Keys.DEVICE_ID] = created }
        return created
    }

    /** Effacement de toutes les préférences locales (droit à l'effacement). */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
