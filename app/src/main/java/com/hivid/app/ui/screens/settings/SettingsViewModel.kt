package com.hivid.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hivid.app.data.preferences.SettingsRepository
import com.hivid.app.data.preferences.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val userSettings: StateFlow<UserSettings> = settingsRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun setDefaultProfile(profile: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultProfile(profile)
        }
    }

    fun setDefaultExpansion(expansion: Float) {
        viewModelScope.launch {
            settingsRepository.updateDefaultExpansion(expansion)
        }
    }

    fun setFpsCap(fps: Int) {
        viewModelScope.launch {
            settingsRepository.updateFpsCap(fps)
        }
    }

    fun setManualBitrate(bitrate: Int) {
        viewModelScope.launch {
            settingsRepository.updateManualBitrate(bitrate)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateKeepScreenOn(enabled)
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotifications(enabled)
        }
    }
}
