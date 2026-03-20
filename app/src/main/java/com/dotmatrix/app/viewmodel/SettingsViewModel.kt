package com.dotmatrix.app.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotmatrix.app.update.AppUpdateInfo
import com.dotmatrix.app.update.AppUpdateManager
import com.dotmatrix.app.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK, PITCH_DARK }
enum class FontSizeOption { SMALL, MEDIUM, LARGE }
enum class FontFamilyOption { DEFAULT, SERIF, MONOSPACE, ROUNDED }
enum class AppUpdateStatus { CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore
    private val appUpdateManager = AppUpdateManager(application.applicationContext)
    private val notificationHelper = NotificationHelper(application)

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        private val KEY_FONT_STYLE = stringPreferencesKey("font_family")
        private val KEY_LAST_NOTIFIED_APP_VERSION = longPreferencesKey("last_notified_app_version")
    }

    val themeMode: StateFlow<ThemeMode> = dataStore.data
        .map { prefs ->
            ThemeMode.values().firstOrNull { it.name == prefs[KEY_THEME] } ?: ThemeMode.SYSTEM
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val fontSize: StateFlow<FontSizeOption> = dataStore.data
        .map { prefs ->
            FontSizeOption.values().firstOrNull { it.name == prefs[KEY_FONT_SIZE] } ?: FontSizeOption.MEDIUM
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FontSizeOption.MEDIUM)

    val fontFamily: StateFlow<FontFamilyOption> = dataStore.data
        .map { prefs ->
            FontFamilyOption.values().firstOrNull { it.name == prefs[KEY_FONT_STYLE] } ?: FontFamilyOption.DEFAULT
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FontFamilyOption.DEFAULT)

    private val _installedVersionName = MutableStateFlow(appUpdateManager.getInstalledVersionName())
    val installedVersionName: StateFlow<String> = _installedVersionName.asStateFlow()

    private val _appUpdateStatus = MutableStateFlow(AppUpdateStatus.CHECKING)
    val appUpdateStatus: StateFlow<AppUpdateStatus> = _appUpdateStatus.asStateFlow()

    private val _availableAppUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableAppUpdate: StateFlow<AppUpdateInfo?> = _availableAppUpdate.asStateFlow()

    private val _appUpdateError = MutableStateFlow<String?>(null)
    val appUpdateError: StateFlow<String?> = _appUpdateError.asStateFlow()

    init {
        refreshAppUpdateStatus()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_THEME] = mode.name }
        }
    }

    fun setFontSize(size: FontSizeOption) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_FONT_SIZE] = size.name }
        }
    }

    fun setFontFamily(family: FontFamilyOption) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_FONT_STYLE] = family.name }
        }
    }

    fun refreshAppUpdateStatus() {
        viewModelScope.launch {
            _installedVersionName.value = appUpdateManager.getInstalledVersionName()
            _appUpdateStatus.value = AppUpdateStatus.CHECKING
            _appUpdateError.value = null

            val result = appUpdateManager.fetchUpdateInfo()
            val update = result.value

            if (update == null) {
                _availableAppUpdate.value = null
                _appUpdateStatus.value = AppUpdateStatus.ERROR
                _appUpdateError.value = result.errorMessage ?: "Unable to check app updates"
                return@launch
            }

            _availableAppUpdate.value = update
            val installedVersionCode = appUpdateManager.getInstalledVersionCode()

            if (update.versionCode > installedVersionCode) {
                _appUpdateStatus.value = AppUpdateStatus.UPDATE_AVAILABLE
                maybeNotifyForNewAppVersion(update.versionCode, update.versionName)
            } else {
                _appUpdateStatus.value = AppUpdateStatus.UP_TO_DATE
            }
        }
    }

    private suspend fun maybeNotifyForNewAppVersion(versionCode: Long, versionName: String) {
        val lastNotifiedVersion = dataStore.data.firstOrNull()?.get(KEY_LAST_NOTIFIED_APP_VERSION) ?: 0L
        if (versionCode <= lastNotifiedVersion) return

        notificationHelper.showAppUpdateNotification(versionName)
        dataStore.edit { prefs ->
            prefs[KEY_LAST_NOTIFIED_APP_VERSION] = versionCode
        }
    }
}
