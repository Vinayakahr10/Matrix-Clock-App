package com.dotmatrix.app.viewmodel

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import com.dotmatrix.app.ble.BleError
import com.dotmatrix.app.ble.BleManager
import com.dotmatrix.app.ble.ScannedDevice
import com.dotmatrix.app.utils.NotificationHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Alarm(val id: String, val time: String, val label: String, val active: Boolean)

// DataStore for connection and alarms
private val Application.connectionDataStore: DataStore<Preferences> by preferencesDataStore(name = "connection_data")

class SharedConnectionViewModel(application: Application) : AndroidViewModel(application) {
    val bleManager = BleManager(application.applicationContext)
    private val dataStore = application.connectionDataStore
    private val gson = Gson()
    private val notificationHelper = NotificationHelper(application)

    companion object {
        private val KEY_ALARMS = stringPreferencesKey("saved_alarms")
        private val KEY_LAST_DEVICE = stringPreferencesKey("last_device_address")
        private const val TARGET_DEVICE_NAME = "DotMatrix Clock"
    }

    val isConnected: StateFlow<Boolean> = bleManager.isConnected
    val scannedDevices: StateFlow<List<ScannedDevice>> = bleManager.scannedDevices
    val deviceName: StateFlow<String> = bleManager.deviceName
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val bleError: StateFlow<BleError?> = bleManager.error
    
    // Device Data
    val batteryLevel: StateFlow<Int?> = bleManager.batteryLevel
    val firmwareVersion: StateFlow<String?> = bleManager.firmwareVersion
    
    // Environment Data
    val temperature: StateFlow<Float?> = bleManager.temperature
    val humidity: StateFlow<Float?> = bleManager.humidity

    fun clearBleError() = bleManager.clearError()

    private val _is24HourFormat = MutableStateFlow(false)
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _brightness = MutableStateFlow(0.8f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _animationStyle = MutableStateFlow("None")
    val animationStyle: StateFlow<String> = _animationStyle.asStateFlow()

    private val _scrollText = MutableStateFlow(true)
    val scrollText: StateFlow<Boolean> = _scrollText.asStateFlow()

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    init {
        // Load persisted data on startup
        viewModelScope.launch {
            loadPersistedStateAndReconnect()
        }

        // Watch for connection to show/hide persistent notification
        viewModelScope.launch {
            combine(isConnected, deviceName, batteryLevel) { connected, name, battery ->
                Triple(connected, name, battery)
            }.collectLatest { (connected, name, battery) ->
                if (connected) {
                    notificationHelper.showConnectionNotification(name, battery)
                } else {
                    notificationHelper.dismissConnectionNotification()
                }
            }
        }
    }

    private fun autoConnect(lastAddress: String?) {
        if (isConnected.value) return

        val adapter = bleManager.getAdapter() ?: return
        
        // Strategy A: Try last connected device first
        if (lastAddress != null) {
            Log.d("SharedVM", "Attempting auto-connect to last device: $lastAddress")
            try {
                val device = adapter.getRemoteDevice(lastAddress)
                if (device != null) {
                    bleManager.connect(device)
                    return // Found and attempting connection
                }
            } catch (e: Exception) {
                Log.e("SharedVM", "Auto-connect to last device failed", e)
            }
        }

        // Strategy B: Try paired (bonded) devices matching the name
        val context = getApplication<Application>().applicationContext
        val hasConnectPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

        if (hasConnectPermission) {
            val bondedDevices = adapter.bondedDevices
            val targetDevice = bondedDevices?.find { it.name == TARGET_DEVICE_NAME }
            if (targetDevice != null) {
                Log.d("SharedVM", "Found bonded device: ${targetDevice.name} (${targetDevice.address}), auto-connecting")
                bleManager.connect(targetDevice)
            }
        }
    }

    fun retryAutoConnect() {
        viewModelScope.launch {
            loadPersistedStateAndReconnect()
        }
    }

    private suspend fun loadPersistedStateAndReconnect() {
        dataStore.data.firstOrNull()?.let { prefs ->
            val json = prefs[KEY_ALARMS] ?: "[]"
            val type = object : TypeToken<List<Alarm>>() {}.type
            _alarms.value = gson.fromJson<List<Alarm>>(json, type) ?: emptyList()
            autoConnect(prefs[KEY_LAST_DEVICE])
        }
    }

    private fun persistAlarms(alarms: List<Alarm>) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_ALARMS] = gson.toJson(alarms)
            }
        }
    }

    private fun saveLastDevice(address: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_LAST_DEVICE] = address
            }
        }
    }

    // Timer state
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()
    
    private val _initialTimerSeconds = MutableStateFlow(0)
    val initialTimerSeconds: StateFlow<Int> = _initialTimerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()
    private var timerJob: Job? = null

    // Stopwatch state
    private val _stopwatchMillis = MutableStateFlow(0L)
    val stopwatchMillis: StateFlow<Long> = _stopwatchMillis.asStateFlow()
    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning.asStateFlow()
    private var stopwatchJob: Job? = null

    // Visualizer state
    private val _isVisualizerPlaying = MutableStateFlow(false)
    val isVisualizerPlaying: StateFlow<Boolean> = _isVisualizerPlaying.asStateFlow()

    private val _visualizerMode = MutableStateFlow("bars")
    val visualizerMode: StateFlow<String> = _visualizerMode.asStateFlow()

    private val _sensitivity = MutableStateFlow(50)
    val sensitivity: StateFlow<Int> = _sensitivity.asStateFlow()

    fun startScan() {
        bleManager.startScan()
    }

    fun connect(device: BluetoothDevice) {
        saveLastDevice(device.address)
        bleManager.connect(device)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun setTimeFormat(is24H: Boolean) {
        _is24HourFormat.value = is24H
        bleManager.writeData("FORMAT:${if (is24H) 24 else 12}")
    }

    fun syncTime() {
        val now = LocalDateTime.now()
        val timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val dateStr = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        bleManager.writeData("SYNC:$timeStr,$dateStr")
    }

    fun setBrightness(brightness: Float) {
        _brightness.value = brightness
        val level = (brightness * 100).toInt()
        bleManager.writeData("BRIGHT:$level")
    }

    fun setAnimationStyle(style: String) {
        _animationStyle.value = style
        bleManager.writeData("ANIM:$style")
    }

    fun setScrollText(enabled: Boolean) {
        _scrollText.value = enabled
        bleManager.writeData("SCROLL:${if (enabled) 1 else 0}")
    }

    fun addAlarm(time: String, label: String) {
        val id = System.currentTimeMillis().toString()
        val newAlarm = Alarm(id, time, label, true)
        val updated = _alarms.value + newAlarm
        _alarms.value = updated
        persistAlarms(updated)
        bleManager.writeData("ALARM_ADD:$id,$time,1")
    }

    fun toggleAlarm(alarm: Alarm) {
        val updated = _alarms.value.map {
            if (it.id == alarm.id) it.copy(active = !it.active) else it
        }
        _alarms.value = updated
        persistAlarms(updated)
        val newState = if (!alarm.active) "1" else "0"
        bleManager.writeData("ALARM_TOGGLE:${alarm.id},$newState")
    }

    fun deleteAlarm(id: String) {
        val updated = _alarms.value.filter { it.id != id }
        _alarms.value = updated
        persistAlarms(updated)
        bleManager.writeData("ALARM_REMOVE:$id")
    }

    // Timer functions
    fun startTimer(seconds: Int) {
        if (seconds <= 0) return
        _initialTimerSeconds.value = seconds
        _timerSeconds.value = seconds
        _isTimerRunning.value = true
        bleManager.writeData("TIMER_START:$seconds")
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _timerSeconds.value -= 1
            }
            _isTimerRunning.value = false
        }
    }

    fun addTimerSeconds(extraSeconds: Int) {
        if (_isTimerRunning.value) {
            _timerSeconds.value += extraSeconds
            _initialTimerSeconds.value += extraSeconds
            bleManager.writeData("TIMER_ADD:$extraSeconds")
        } else {
            _timerSeconds.value += extraSeconds
            _initialTimerSeconds.value = _timerSeconds.value
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        bleManager.writeData("TIMER_STOP")
    }
    
    fun resetTimer() {
        stopTimer()
        _timerSeconds.value = 0
        _initialTimerSeconds.value = 0
    }

    // Stopwatch functions
    fun startStopwatch() {
        if (_isStopwatchRunning.value) return
        _isStopwatchRunning.value = true
        bleManager.writeData("SW_START")
        
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis() - _stopwatchMillis.value
            while (_isStopwatchRunning.value) {
                _stopwatchMillis.value = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
    }

    fun pauseStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        bleManager.writeData("SW_PAUSE")
    }

    fun resetStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        _stopwatchMillis.value = 0
        bleManager.writeData("SW_RESET")
    }

    fun toggleVisualizer() {
        val newState = !_isVisualizerPlaying.value
        _isVisualizerPlaying.value = newState
        bleManager.writeData("VIS_ENABLE:${if (newState) 1 else 0}")
    }

    fun setVisualizerMode(mode: String) {
        _visualizerMode.value = mode
        bleManager.writeData("VIS_MODE:$mode")
    }

    fun setSensitivity(value: Int) {
        _sensitivity.value = value
        bleManager.writeData("VIS_SENSE:$value")
    }
    
    // Commands for new profile
    fun requestInfo() {
        bleManager.writeData("INFO")
    }
    
    fun requestBattery() {
        bleManager.writeData("BAT?")
    }
    
    fun sendBatteryLevel(level: Int) {
        bleManager.writeData("BAT:$level")
    }
}
