package com.dotmatrix.app.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dotmatrix.app.ble.BleManager
import com.dotmatrix.app.ota.OTAManager
import com.dotmatrix.app.ota.OTAReleaseInfo
import com.dotmatrix.app.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

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

class OTAViewModel(application: Application, private val bleManager: BleManager) : AndroidViewModel(application) {
    private val otaManager = OTAManager(application.applicationContext)
    private val notificationHelper = NotificationHelper(application)

    private val _otaState = MutableStateFlow(OTAState.Idle)
    val otaState: StateFlow<OTAState> = _otaState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _releaseInfo = MutableStateFlow<OTAReleaseInfo?>(null)
    val releaseInfo: StateFlow<OTAReleaseInfo?> = _releaseInfo.asStateFlow()
    
    private val _currentVersion = MutableStateFlow("Unknown")
    val currentVersion: StateFlow<String> = _currentVersion.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Optionally set a token if required by firmware
    private val otaToken: String? = "mytoken123" 

    init {
        viewModelScope.launch {
            bleManager.firmwareVersion.collectLatest { version ->
                if (version != null) {
                    _currentVersion.value = version
                    compareVersions()
                }
            }
        }
    }

    private fun compareVersions() {
        val current = _currentVersion.value
        val latest = _releaseInfo.value?.version
        
        if (latest == null) return

        // If current version is unknown, we should still allow update if we found release info
        if (current == "Unknown") {
            _otaState.value = OTAState.UpdateAvailable
            return
        }

        // Standard version comparison (clean versions of any 'v' prefix for comparison)
        val cleanCurrent = current.replace("v", "").trim()
        val cleanLatest = latest.replace("v", "").trim()

        if (cleanLatest != cleanCurrent) {
            _otaState.value = OTAState.UpdateAvailable
        } else {
            _otaState.value = OTAState.NoUpdate
        }
    }

    fun checkForUpdates() {
        _otaState.value = OTAState.Checking
        _errorMessage.value = null
        
        // Ask clock for version using new profile command
        bleManager.writeData("INFO")

        // Fetch from GitHub
        viewModelScope.launch {
            val result = otaManager.fetchUpdateMetadata()
            val latest = result.value
            if (latest != null) {
                _releaseInfo.value = latest
                compareVersions()
                if (_otaState.value == OTAState.UpdateAvailable) {
                    notificationHelper.showUpdateNotification(latest.version)
                }
            } else {
                _errorMessage.value = result.errorMessage ?: "Could not check for updates"
                _otaState.value = OTAState.Error
            }
        }
    }

    fun downloadUpdate() {
        val info = _releaseInfo.value ?: run {
            _errorMessage.value = "Update details are missing. Check for updates again."
            _otaState.value = OTAState.Error
            return
        }
        _otaState.value = OTAState.Downloading
        _downloadProgress.value = 0f
        _errorMessage.value = null

        viewModelScope.launch {
            val result = otaManager.downloadFirmware(info.bin_url, info.md5) { progress ->
                _downloadProgress.value = progress
            }
            val file = result.value
            if (file != null && file.exists()) {
                _otaState.value = OTAState.ReadyToInstall
            } else {
                _errorMessage.value = result.errorMessage ?: "Firmware download failed"
                _otaState.value = OTAState.Error
            }
        }
    }

    fun uploadLocalFirmware(uri: Uri) {
        _otaState.value = OTAState.Installing
        _downloadProgress.value = 0f
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val inputStream = app.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Could not read file")
                inputStream.close()

                bleManager.streamFile(bytes, otaToken) { progress ->
                    _downloadProgress.value = progress
                }
                _otaState.value = OTAState.Success
            } catch (e: Exception) {
                Log.e("OTAViewModel", "Local upload failed", e)
                _errorMessage.value = e.message ?: "Local firmware upload failed"
                _otaState.value = OTAState.Error
            }
        }
    }

    fun startInstall() {
        val app = getApplication<Application>()
        val file = File(app.cacheDir, "firmware_update.bin")
        if (!file.exists()) {
            _errorMessage.value = "No downloaded firmware file was found"
            _otaState.value = OTAState.Error
            return
        }

        _otaState.value = OTAState.Installing
        _downloadProgress.value = 0f
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                val bytes = file.readBytes()
                bleManager.streamFile(bytes, otaToken) { progress ->
                    _downloadProgress.value = progress
                }
                _otaState.value = OTAState.Success
                // Cleanup
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.e("OTAViewModel", "Installation failed", e)
                _errorMessage.value = e.message ?: "Firmware installation failed"
                _otaState.value = OTAState.Error
            }
        }
    }

    fun reset() {
        _otaState.value = OTAState.Idle
        _downloadProgress.value = 0f
        _errorMessage.value = null
    }

    fun retry() {
        when {
            _otaState.value == OTAState.ReadyToInstall -> startInstall()
            _releaseInfo.value == null -> checkForUpdates()
            else -> downloadUpdate()
        }
    }

    class Factory(private val application: Application, private val bleManager: BleManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OTAViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return OTAViewModel(application, bleManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
