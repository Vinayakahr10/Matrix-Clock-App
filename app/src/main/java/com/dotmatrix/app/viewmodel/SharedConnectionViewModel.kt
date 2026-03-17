package com.dotmatrix.app.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import com.dotmatrix.app.ble.BleManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Alarm(val id: String, val time: String, val label: String, val active: Boolean)

class SharedConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val bleManager = BleManager(application.applicationContext)

    val isConnected: StateFlow<Boolean> = bleManager.isConnected
    val scannedDevices: StateFlow<List<BluetoothDevice>> = bleManager.scannedDevices
    val deviceName: StateFlow<String> = bleManager.deviceName

    private val _is24HourFormat = MutableStateFlow(false)
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    // Timer state
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()
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
    private val _isVisualizerActive = MutableStateFlow(false)
    val isVisualizerActive: StateFlow<Boolean> = _isVisualizerActive.asStateFlow()

    private val _visualizerMode = MutableStateFlow("bars")
    val visualizerMode: StateFlow<String> = _visualizerMode.asStateFlow()

    fun startScan() {
        bleManager.startScan()
    }

    fun connect(device: BluetoothDevice) {
        bleManager.connect(device)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun set24HourFormat(enabled: Boolean) {
        _is24HourFormat.value = enabled
        bleManager.writeData("FORMAT:${if (enabled) 24 else 12}")
    }

    fun syncTime() {
        val now = LocalDateTime.now()
        val timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val dateStr = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        bleManager.writeData("SYNC:$timeStr,$dateStr")
    }

    fun setBrightness(brightness: Int) {
        bleManager.writeData("BRIGHT:$brightness")
    }

    fun addAlarm(time: String, label: String) {
        val id = System.currentTimeMillis().toString()
        val newAlarm = Alarm(id, time, label, true)
        _alarms.value = _alarms.value + newAlarm
        bleManager.writeData("ALARM_ADD:$id,$time,1")
    }

    fun toggleAlarm(alarm: Alarm) {
        val updatedAlarms = _alarms.value.map {
            if (it.id == alarm.id) it.copy(active = !it.active) else it
        }
        _alarms.value = updatedAlarms
        val newState = if (!alarm.active) "1" else "0"
        bleManager.writeData("ALARM_TOGGLE:${alarm.id},$newState")
    }

    fun deleteAlarm(id: String) {
        _alarms.value = _alarms.value.filter { it.id != id }
        bleManager.writeData("ALARM_REMOVE:$id")
    }

    // Timer functions
    fun startTimer(seconds: Int) {
        _timerSeconds.value = seconds
        _isTimerRunning.value = true
        bleManager.writeData("TIMER_START:$seconds")
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0) {
                delay(1000)
                _timerSeconds.value -= 1
            }
            _isTimerRunning.value = false
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        bleManager.writeData("TIMER_STOP")
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
        val newState = !_isVisualizerActive.value
        _isVisualizerActive.value = newState
        bleManager.writeData("VIS_ENABLE:${if (newState) 1 else 0}")
    }

    fun setVisualizerMode(mode: String) {
        _visualizerMode.value = mode
        bleManager.writeData("VIS_MODE:$mode")
    }

    fun setVisualizerSensitivity(value: Int) {
        bleManager.writeData("VIS_SENSE:$value")
    }
}
