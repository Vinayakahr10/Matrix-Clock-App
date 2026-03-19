package com.dotmatrix.app.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── DataStore singleton ───────────────────────────────────────────────────────
private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// ── Enums ─────────────────────────────────────────────────────────────────────
enum class ThemeMode { SYSTEM, LIGHT, DARK, PITCH_DARK }
enum class FontSizeOption { SMALL, MEDIUM, LARGE }
enum class FontFamilyOption { DEFAULT, SERIF, MONOSPACE, ROUNDED }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    // ── Keys ─────────────────────────────────────────────────────────────────
    companion object {
        private val KEY_THEME      = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SIZE  = stringPreferencesKey("font_size")
        private val KEY_FONT_STYLE = stringPreferencesKey("font_family")
    }

    // ── Exposed state ─────────────────────────────────────────────────────────
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

    // ── Setters ───────────────────────────────────────────────────────────────
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
}
