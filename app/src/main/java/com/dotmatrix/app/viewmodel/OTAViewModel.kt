package com.dotmatrix.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotmatrix.app.ota.OTAManager
import com.dotmatrix.app.ota.OTAReleaseInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OTAState { 
    Idle, 
    Checking, 
    UpdateAvailable, 
    NoUpdate,
    Downloading, 
    ReadyToInstall, 
    Installing, 
    Success, 
    Error 
}

class OTAViewModel(application: Application) : AndroidViewModel(application) {
    private val otaManager = OTAManager(application.applicationContext)

    private val _otaState = MutableStateFlow(OTAState.Idle)
    val otaState: StateFlow<OTAState> = _otaState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _releaseInfo = MutableStateFlow<OTAReleaseInfo?>(null)
    val releaseInfo: StateFlow<OTAReleaseInfo?> = _releaseInfo.asStateFlow()
    
    // We mock the current version check for now
    val currentVersion = "v2.4.1"

    fun checkForUpdates() {
        _otaState.value = OTAState.Checking
        viewModelScope.launch {
            val latest = otaManager.fetchLatestRelease()
            if (latest != null) {
                _releaseInfo.value = latest
                if (latest.version != currentVersion) {
                    _otaState.value = OTAState.UpdateAvailable
                } else {
                    _otaState.value = OTAState.NoUpdate
                }
            } else {
                _otaState.value = OTAState.Error
            }
        }
    }

    fun downloadUpdate() {
        val info = _releaseInfo.value ?: return
        _otaState.value = OTAState.Downloading
        _downloadProgress.value = 0f

        viewModelScope.launch {
            val file = otaManager.downloadFirmware(info.downloadUrl) { progress ->
                _downloadProgress.value = progress
            }
            if (file != null && file.exists()) {
                _otaState.value = OTAState.ReadyToInstall
            } else {
                _otaState.value = OTAState.Error
            }
        }
    }

    fun startInstall() {
        _otaState.value = OTAState.Installing
        // Mock installation process as real BLE chunking requires device-side support
        viewModelScope.launch {
            _downloadProgress.value = 0f
            while (_downloadProgress.value < 1f) {
                kotlinx.coroutines.delay(200)
                _downloadProgress.value += 0.05f
            }
            _otaState.value = OTAState.Success
        }
    }

    fun reset() {
        _otaState.value = OTAState.Idle
        _downloadProgress.value = 0f
    }
}
