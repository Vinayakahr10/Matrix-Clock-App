package com.dotmatrix.app.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
enum class AppUpdateStatus { CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR }
enum class FontSizeOption { SMALL, MEDIUM, LARGE }
enum class FontFamilyOption { DEFAULT }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore
    private val appUpdateManager = AppUpdateManager(application.applicationContext)
    private val notificationHelper = NotificationHelper(application)

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        private val KEY_FONT_FAMILY = stringPreferencesKey("font_family")
        private val KEY_LAST_NOTIFIED_APP_VERSION = longPreferencesKey("last_notified_app_version")
        private val KEY_DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
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
            FontFamilyOption.values().firstOrNull { it.name == prefs[KEY_FONT_FAMILY] } ?: FontFamilyOption.DEFAULT
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FontFamilyOption.DEFAULT)

    val isDeveloperModeEnabled: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_DEVELOPER_MODE] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _installedVersionName = MutableStateFlow(appUpdateManager.getInstalledVersionName())
    val installedVersionName: StateFlow<String> = _installedVersionName.asStateFlow()

    private val _appUpdateStatus = MutableStateFlow(AppUpdateStatus.CHECKING)
    val appUpdateStatus: StateFlow<AppUpdateStatus> = _appUpdateStatus.asStateFlow()

    private val _availableAppUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableAppUpdate: StateFlow<AppUpdateInfo?> = _availableAppUpdate.asStateFlow()

    private val _appUpdateError = MutableStateFlow<String?>(null)
    val appUpdateError: StateFlow<String?> = _appUpdateError.asStateFlow()

    private val _versionTapCount = MutableStateFlow(0)
    val versionTapCount: StateFlow<Int> = _versionTapCount.asStateFlow()

    init {
        refreshAppUpdateStatus()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_THEME] = mode.name }
        }
    }

    fun setFontSize(option: FontSizeOption) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_FONT_SIZE] = option.name }
        }
    }

    fun setFontFamily(option: FontFamilyOption) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_FONT_FAMILY] = option.name }
        }
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs -> prefs[KEY_DEVELOPER_MODE] = enabled }
        }
        if (!enabled) _versionTapCount.value = 0
    }

    fun incrementVersionTap() {
        if (isDeveloperModeEnabled.value) return
        _versionTapCount.value++
        if (_versionTapCount.value >= 7) {
            setDeveloperModeEnabled(true)
            _versionTapCount.value = 0
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
