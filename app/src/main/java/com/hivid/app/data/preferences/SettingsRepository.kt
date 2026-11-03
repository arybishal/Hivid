package com.hivid.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hivid_settings")

data class UserSettings(
    val themeMode: String = "DARK", // "DARK" or "LIGHT"
    val defaultProfile: String = "HDR10",
    val defaultExpansion: Float = 70f,
    val fpsCap: Int = 60,
    val manualBitrateOverride: Int = 0,
    val keepScreenOn: Boolean = true,
    val notificationsEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_PROFILE = stringPreferencesKey("default_profile")
        val DEFAULT_EXPANSION = floatPreferencesKey("default_expansion")
        val FPS_CAP = intPreferencesKey("fps_cap")
        val MANUAL_BITRATE = intPreferencesKey("manual_bitrate")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE] ?: "DARK",
            defaultProfile = prefs[Keys.DEFAULT_PROFILE] ?: "HDR10",
            defaultExpansion = prefs[Keys.DEFAULT_EXPANSION] ?: 70f,
            fpsCap = prefs[Keys.FPS_CAP] ?: 60,
            manualBitrateOverride = prefs[Keys.MANUAL_BITRATE] ?: 0,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true
        )
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun updateDefaultProfile(profile: String) {
        context.dataStore.edit { it[Keys.DEFAULT_PROFILE] = profile }
    }

    suspend fun updateDefaultExpansion(expansion: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_EXPANSION] = expansion }
    }

    suspend fun updateFpsCap(fps: Int) {
        context.dataStore.edit { it[Keys.FPS_CAP] = fps }
    }

    suspend fun updateManualBitrate(bitrate: Int) {
        context.dataStore.edit { it[Keys.MANUAL_BITRATE] = bitrate }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun updateNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }
}
