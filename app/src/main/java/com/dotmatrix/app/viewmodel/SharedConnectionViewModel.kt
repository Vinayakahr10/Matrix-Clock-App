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
import androidx.lifecycle.viewModelScope
import com.dotmatrix.app.ble.BleError
import com.dotmatrix.app.ble.BleManager
import com.dotmatrix.app.ble.FirmwareAlarmState
import com.dotmatrix.app.ble.ScannedDevice
import com.dotmatrix.app.service.PhoneMicVisualizerRuntime
import com.dotmatrix.app.service.PhoneMicVisualizerService
import com.dotmatrix.app.utils.NotificationHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Alarm(val id: String, val time: String, val label: String, val active: Boolean)

// DataStore for connection and alarms
private val Application.connectionDataStore: DataStore<Preferences> by preferencesDataStore(name = "connection_data")

class SharedConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    val bleManager = BleManager.shared(appContext)
    private val dataStore = application.connectionDataStore
    private val gson = Gson()
    private val notificationHelper = NotificationHelper(application)
    private val _deviceEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)

    companion object {
        private val KEY_ALARMS = stringPreferencesKey("saved_alarms")
        private val KEY_LAST_DEVICE = stringPreferencesKey("last_device_address")
        private val KEY_TIME_FORMAT_24 = booleanPreferencesKey("time_format_24")
        private val KEY_BRIGHTNESS = intPreferencesKey("brightness")
        private val KEY_ANIMATION_STYLE = stringPreferencesKey("animation_style")
        private val KEY_SCROLL_TEXT = booleanPreferencesKey("scroll_text")
        private val KEY_VISUALIZER_SOURCE = stringPreferencesKey("visualizer_source")
        private val KEY_VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        private val KEY_MESSAGE_ANIMATION_STYLE = stringPreferencesKey("message_animation_style")
        private val KEY_VISUALIZER_SENSITIVITY = intPreferencesKey("visualizer_sensitivity")
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
    val currentMode: StateFlow<String> = bleManager.currentMode
    val currentClockTool: StateFlow<String> = bleManager.currentClockTool
    val deviceEvents: SharedFlow<String> = _deviceEvents.asSharedFlow()
    val timeFormatStatusMessage: StateFlow<String?> = bleManager.timeFormatStatusMessage
    private val connectionReadyEvents: SharedFlow<Unit> = bleManager.connectionReadyEvents

    fun clearBleError() = bleManager.clearError()

    private val _is24HourFormat = MutableStateFlow(false)
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _brightness = MutableStateFlow(8)
    val brightness: StateFlow<Int> = _brightness.asStateFlow()

    private val _animationStyle = MutableStateFlow("None")
    val animationStyle: StateFlow<String> = _animationStyle.asStateFlow()

    private val _scrollText = MutableStateFlow(true)
    val scrollText: StateFlow<Boolean> = _scrollText.asStateFlow()

    private val _visualizerSource = MutableStateFlow("DEVICE")
    val visualizerSource: StateFlow<String> = _visualizerSource.asStateFlow()

    private val _visualizerStyle = MutableStateFlow("BARS")
    val visualizerStyle: StateFlow<String> = _visualizerStyle.asStateFlow()

    private val _messageAnimationStyle = MutableStateFlow("NONE")
    val messageAnimationStyle: StateFlow<String> = _messageAnimationStyle.asStateFlow()

    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    private val _phoneMicPermissionGranted = MutableStateFlow(hasRecordAudioPermission())
    val phoneMicPermissionGranted: StateFlow<Boolean> = _phoneMicPermissionGranted.asStateFlow()

    val isPhoneMicStreaming: StateFlow<Boolean> = PhoneMicVisualizerRuntime.isRunning.asStateFlow()

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
            _is24HourFormat.value = prefs[KEY_TIME_FORMAT_24] ?: false
            _brightness.value = prefs[KEY_BRIGHTNESS] ?: 8
            _animationStyle.value = prefs[KEY_ANIMATION_STYLE] ?: "None"
            _scrollText.value = prefs[KEY_SCROLL_TEXT] ?: true
            _visualizerSource.value = prefs[KEY_VISUALIZER_SOURCE] ?: "DEVICE"
            _visualizerStyle.value = prefs[KEY_VISUALIZER_STYLE] ?: "BARS"
            _messageAnimationStyle.value = prefs[KEY_MESSAGE_ANIMATION_STYLE] ?: "NONE"
            _sensitivity.value = prefs[KEY_VISUALIZER_SENSITIVITY] ?: 50
            _visualizerMode.value = when (_visualizerStyle.value) {
                "WAVE" -> "waveform"
                "RADIAL" -> "radial"
                else -> "bars"
            }
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

    private fun persistTimeFormat(is24H: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_TIME_FORMAT_24] = is24H
            }
        }
    }

    private fun persistUiPreferences() {
        val brightness = _brightness.value
        val animationStyle = _animationStyle.value
        val scrollText = _scrollText.value
        val visualizerSource = _visualizerSource.value
        val visualizerStyle = _visualizerStyle.value
        val messageAnimationStyle = _messageAnimationStyle.value
        val sensitivity = _sensitivity.value
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_BRIGHTNESS] = brightness
                prefs[KEY_ANIMATION_STYLE] = animationStyle
                prefs[KEY_SCROLL_TEXT] = scrollText
                prefs[KEY_VISUALIZER_SOURCE] = visualizerSource
                prefs[KEY_VISUALIZER_STYLE] = visualizerStyle
                prefs[KEY_MESSAGE_ANIMATION_STYLE] = messageAnimationStyle
                prefs[KEY_VISUALIZER_SENSITIVITY] = sensitivity
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

        viewModelScope.launch {
            connectionReadyEvents.collect {
                requestInfo()
                delay(700)
                sendCurrentTimeSync()
                requestCurrentClockTool()
                requestAlarmState()
                requestTimerState()
                requestStopwatchState()
                requestBrightness()
                requestTimeFormat()
                delay(200)
                bleManager.writeData("TIMEFMT:${if (_is24HourFormat.value) 24 else 12}")
                bleManager.writeData("BRIGHTNESS:${_brightness.value.coerceIn(0, 15)}")
                bleManager.writeData("ANIM:${_animationStyle.value}")
                bleManager.writeData("SCROLL:${if (_scrollText.value) 1 else 0}")
                bleManager.writeData("VIZSRC:${_visualizerSource.value}")
                bleManager.writeData("VIZSTYLE:${_visualizerStyle.value}")
                bleManager.writeData("MSGANIM:${_messageAnimationStyle.value}")
                bleManager.writeData("VIS_SENSE:${_sensitivity.value.coerceIn(0, 100)}")
            }
        }

        viewModelScope.launch {
            bleManager.deviceTimeFormat24.collectLatest { is24Hour ->
                if (is24Hour != null) {
                    _is24HourFormat.value = is24Hour
                }
            }
        }

        viewModelScope.launch {
            bleManager.brightnessValue.collectLatest { value ->
                if (value != null) {
                    _brightness.value = value.coerceIn(0, 15)
                }
            }
        }

        viewModelScope.launch {
            bleManager.deviceEvents.collect { event ->
                _deviceEvents.emit(event)
            }
        }

        viewModelScope.launch {
            bleManager.visualizerSource.collectLatest { source ->
                _visualizerSource.value = source
                persistUiPreferences()
            }
        }

        viewModelScope.launch {
            bleManager.visualizerStyle.collectLatest { style ->
                _visualizerStyle.value = style
                _visualizerMode.value = when (style) {
                    "WAVE" -> "waveform"
                    "RADIAL" -> "radial"
                    else -> "bars"
                }
                persistUiPreferences()
            }
        }

        viewModelScope.launch {
            bleManager.messageAnimationStyle.collectLatest { style ->
                _messageAnimationStyle.value = style
                persistUiPreferences()
            }
        }

        viewModelScope.launch {
            bleManager.alarmState.collectLatest { state ->
                syncAlarmStateFromFirmware(state)
            }
        }

        viewModelScope.launch {
            bleManager.timerState.collectLatest { state ->
                timerJob?.cancel()
                _timerSeconds.value = state.totalSeconds
                _initialTimerSeconds.value = when {
                    state.status == "DONE" -> 0
                    state.totalSeconds > 0 -> maxOf(_initialTimerSeconds.value, state.totalSeconds)
                    else -> _initialTimerSeconds.value
                }
                _isTimerRunning.value = state.status == "RUNNING"
                if (state.status == "RUNNING") {
                    startTimerDisplayCountdown()
                } else if (state.status == "DONE") {
                    _initialTimerSeconds.value = 0
                }
            }
        }

        viewModelScope.launch {
            bleManager.stopwatchState.collectLatest { state ->
                _stopwatchMillis.value = state.totalSeconds * 1000L
                _isStopwatchRunning.value = state.status == "RUNNING"
                stopwatchJob?.cancel()
                if (state.status == "RUNNING") {
                    startStopwatchDisplayTicker(_stopwatchMillis.value)
                }
            }
        }

        viewModelScope.launch {
            combine(
                isConnected,
                visualizerSource,
                isVisualizerPlaying,
                phoneMicPermissionGranted
            ) { connected, source, playing, micPermissionGranted ->
                connected && source == "PHONE" && playing && micPermissionGranted
            }.collectLatest { shouldStream ->
                if (shouldStream) {
                    startPhoneMicVisualizer()
                } else {
                    stopPhoneMicVisualizer()
                }
            }
        }
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun connect(device: BluetoothDevice) {
        saveLastDevice(device.address)
        bleManager.connect(device)
    }

    fun disconnect() {
        bleManager.disconnect()
        stopPhoneMicVisualizer()
    }

    fun setTimeFormat(is24H: Boolean) {
        _is24HourFormat.value = is24H
        persistTimeFormat(is24H)
        bleManager.writeData("TIMEFMT:${if (is24H) 24 else 12}")
    }

    fun requestTimeFormat() {
        bleManager.writeData("TIMEFMT?")
    }

    fun requestBrightness() {
        bleManager.writeData("BRIGHTNESS?")
    }

    fun syncTime() {
        bleManager.writeData(buildSyncCommandFromCurrentTime())
    }

    fun sendCurrentTimeSync() {
        syncTime()
    }

    fun buildSyncCommandFromCurrentTime(): String {
        val now = LocalDateTime.now()
        val timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val dateStr = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        return "SYNC:$timeStr,$dateStr"
    }

    fun sendVirtualButtonCommand(button: String) {
        val command = when (button.uppercase()) {
            "MODE" -> "BTN:MODE"
            "NEXT" -> "BTN:NEXT"
            "BACK" -> "BTN:BACK"
            "SELECT", "OK" -> "BTN:SELECT"
            else -> return
        }
        bleManager.writeData(command)
        viewModelScope.launch {
            delay(250)
            requestInfo()
        }
    }

    fun sendMessageToDisplay(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            bleManager.writeData("MSG:$trimmed")
        }
    }

    fun sendMessageAnimationNone() {
        _messageAnimationStyle.value = "NONE"
        persistUiPreferences()
        bleManager.writeData("MSGANIM:NONE")
    }

    fun sendMessageAnimationWave() {
        _messageAnimationStyle.value = "WAVE"
        persistUiPreferences()
        bleManager.writeData("MSGANIM:WAVE")
    }

    fun sendMessageAnimationScroll() {
        _messageAnimationStyle.value = "SCROLL"
        persistUiPreferences()
        bleManager.writeData("MSGANIM:SCROLL")
    }

    fun sendMessageAnimationRain() {
        _messageAnimationStyle.value = "RAIN"
        persistUiPreferences()
        bleManager.writeData("MSGANIM:RAIN")
    }

    fun requestMessageAnimationStyle() {
        bleManager.writeData("MSGANIM?")
    }

    fun requestCurrentMode() {
        bleManager.writeData("MODE?")
    }

    fun sendBrightness(value: Int) {
        val clamped = value.coerceIn(0, 15)
        _brightness.value = clamped
        persistUiPreferences()
        bleManager.writeData("BRIGHTNESS:$clamped")
    }

    fun setBrightness(brightness: Int) {
        sendBrightness(brightness)
    }

    fun setAnimationStyle(style: String) {
        _animationStyle.value = style
        persistUiPreferences()
        bleManager.writeData("ANIM:$style")
    }

    fun setScrollText(enabled: Boolean) {
        _scrollText.value = enabled
        persistUiPreferences()
        bleManager.writeData("SCROLL:${if (enabled) 1 else 0}")
    }

    fun addAlarm(time: String, label: String) {
        val normalizedTime = normalizeAlarmTimeForFirmware(time)
        val firmwareAlarm = Alarm("firmware-alarm", normalizedTime, label.ifBlank { "Alarm" }, true)
        _alarms.value = listOf(firmwareAlarm)
        persistAlarms(_alarms.value)
        bleManager.writeData("ALARM_SET:$normalizedTime")
        bleManager.writeData("ALARM_ON")
    }

    fun toggleAlarm(alarm: Alarm) {
        val enabled = !alarm.active
        _alarms.value = _alarms.value.map {
            if (it.id == alarm.id) it.copy(active = enabled) else it
        }
        persistAlarms(_alarms.value)
        bleManager.writeData(if (enabled) "ALARM_ON" else "ALARM_OFF")
    }

    fun deleteAlarm(id: String) {
        _alarms.value = emptyList()
        persistAlarms(emptyList())
        bleManager.writeData("ALARM_CLEAR")
    }

    fun setTimerDuration(seconds: Int) {
        val normalized = seconds.coerceAtLeast(0)
        timerJob?.cancel()
        _timerSeconds.value = normalized
        _initialTimerSeconds.value = normalized
        _isTimerRunning.value = false
        bleManager.writeData("TIMER_SET:${formatMinutesSeconds(normalized)}")
        requestTimerStateSoon()
    }

    // Timer functions
    fun startTimer(seconds: Int) {
        if (seconds <= 0) return
        _initialTimerSeconds.value = seconds
        _timerSeconds.value = seconds
        _isTimerRunning.value = true
        startTimerDisplayCountdown()
        bleManager.writeData("TIMER_SET:${formatMinutesSeconds(seconds)}")
        bleManager.writeData("TIMER_START")
        requestTimerStateSoon()
    }

    fun addTimerSeconds(extraSeconds: Int) {
        if (_timerSeconds.value + extraSeconds >= 0) {
            _timerSeconds.value = (_timerSeconds.value + extraSeconds).coerceAtLeast(0)
            _initialTimerSeconds.value = (_initialTimerSeconds.value + extraSeconds).coerceAtLeast(_timerSeconds.value)
        }
        bleManager.writeData("TIMER_ADD:$extraSeconds")
        requestTimerStateSoon()
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        bleManager.writeData("TIMER_PAUSE")
        requestTimerStateSoon()
    }
    
    fun resetTimer() {
        timerJob?.cancel()
        _timerSeconds.value = 0
        _initialTimerSeconds.value = 0
        _isTimerRunning.value = false
        bleManager.writeData("TIMER_RESET")
        requestTimerStateSoon()
    }

    // Stopwatch functions
    fun startStopwatch() {
        if (_isStopwatchRunning.value) return
        _isStopwatchRunning.value = true
        startStopwatchDisplayTicker(_stopwatchMillis.value)
        bleManager.writeData("SW_START")
        requestStopwatchStateSoon()
    }

    fun pauseStopwatch() {
        stopwatchJob?.cancel()
        _isStopwatchRunning.value = false
        bleManager.writeData("SW_PAUSE")
        requestStopwatchStateSoon()
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        _stopwatchMillis.value = 0L
        _isStopwatchRunning.value = false
        bleManager.writeData("SW_RESET")
        requestStopwatchStateSoon()
    }

    fun lapStopwatch() {
        bleManager.writeData("SW_LAP")
        requestStopwatchStateSoon()
    }

    fun requestCurrentClockTool() {
        bleManager.writeData("CTOOL?")
    }

    fun requestAlarmState() {
        bleManager.writeData("ALARM?")
    }

    fun requestTimerState() {
        bleManager.writeData("TIMER?")
    }

    fun requestStopwatchState() {
        bleManager.writeData("SW?")
    }

    fun toggleVisualizer() {
        val newState = !_isVisualizerPlaying.value
        _isVisualizerPlaying.value = newState
        bleManager.writeData("VIS_ENABLE:${if (newState) 1 else 0}")
        if (!newState) {
            stopPhoneMicVisualizer()
        }
    }

    fun setVisualizerMode(mode: String) {
        _visualizerMode.value = mode
        _visualizerStyle.value = when (mode.lowercase()) {
            "waveform" -> "WAVE"
            "radial" -> "RADIAL"
            else -> "BARS"
        }
        persistUiPreferences()
        bleManager.writeData("VIZSTYLE:${_visualizerStyle.value}")
    }

    fun setSensitivity(value: Int) {
        _sensitivity.value = value
        persistUiPreferences()
        bleManager.writeData("VIS_SENSE:$value")
        if (PhoneMicVisualizerRuntime.isRunning.value) {
            PhoneMicVisualizerService.start(appContext, value)
        }
    }

    fun sendVisualizerSourceDevice() {
        _visualizerSource.value = "DEVICE"
        persistUiPreferences()
        bleManager.writeData("VIZSRC:DEVICE")
        stopPhoneMicVisualizer()
    }

    fun sendVisualizerSourcePhone() {
        _visualizerSource.value = "PHONE"
        persistUiPreferences()
        bleManager.writeData("VIZSRC:PHONE")
    }

    fun requestVisualizerSource() {
        bleManager.writeData("VIZSRC?")
    }

    fun sendVisualizerStyleBars() {
        _visualizerMode.value = "bars"
        _visualizerStyle.value = "BARS"
        persistUiPreferences()
        bleManager.writeData("VIZSTYLE:BARS")
    }

    fun sendVisualizerStyleWave() {
        _visualizerMode.value = "waveform"
        _visualizerStyle.value = "WAVE"
        persistUiPreferences()
        bleManager.writeData("VIZSTYLE:WAVE")
    }

    fun sendVisualizerStyleRadial() {
        _visualizerMode.value = "radial"
        _visualizerStyle.value = "RADIAL"
        persistUiPreferences()
        bleManager.writeData("VIZSTYLE:RADIAL")
    }

    fun requestVisualizerStyle() {
        bleManager.writeData("VIZSTYLE?")
    }

    fun setVisualizerScreenVisible(visible: Boolean) {
        // Keep the phone mic visualizer running across app navigation.
    }

    fun refreshPhoneMicPermission() {
        _phoneMicPermissionGranted.value = hasRecordAudioPermission()
    }

    fun onPhoneMicPermissionResult(granted: Boolean) {
        _phoneMicPermissionGranted.value = granted
        if (!granted) {
            _deviceEvents.tryEmit("Phone microphone permission denied")
            stopPhoneMicVisualizer()
        }
    }

    fun sendVisualizerFrame(levels32: List<Int>) {
        if (levels32.size != 32) return
        val payload = levels32.joinToString(",") { it.coerceIn(0, 8).toString() }
        bleManager.writeData("VIZFRAME:$payload")
    }

    fun startPhoneMicVisualizer() {
        PhoneMicVisualizerService.start(appContext, _sensitivity.value)
    }

    fun stopPhoneMicVisualizer() {
        PhoneMicVisualizerService.stop(appContext)
    }

    override fun onCleared() {
        super.onCleared()
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication<Application>().applicationContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun syncAlarmStateFromFirmware(state: FirmwareAlarmState?) {
        if (state == null || state.time.isBlank()) {
            _alarms.value = emptyList()
            persistAlarms(emptyList())
            return
        }
        val syncedAlarm = Alarm(
            id = "firmware-alarm",
            time = state.time,
            label = "Alarm",
            active = state.enabled
        )
        _alarms.value = listOf(syncedAlarm)
        persistAlarms(_alarms.value)
    }

    private fun normalizeAlarmTimeForFirmware(time: String): String {
        val trimmed = time.trim()
        if (!trimmed.contains("AM", ignoreCase = true) && !trimmed.contains("PM", ignoreCase = true)) {
            return trimmed
        }

        val normalized = trimmed.replace(Regex("\\s+"), " ").uppercase(Locale.US)
        val patterns = listOf("h:mm a", "hh:mm a")
        for (pattern in patterns) {
            runCatching {
                val parsed = DateTimeFormatter.ofPattern(pattern, Locale.US)
                LocalTime.parse(normalized, parsed)
            }.getOrNull()?.let { localTime ->
                return "%02d:%02d".format(localTime.hour, localTime.minute)
            }
        }

        return trimmed
    }

    private fun formatMinutesSeconds(totalSeconds: Int): String {
        val minutes = (totalSeconds.coerceAtLeast(0) / 60).coerceAtMost(99)
        val seconds = totalSeconds.coerceAtLeast(0) % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun requestTimerStateSoon() {
        viewModelScope.launch {
            delay(250)
            requestTimerState()
        }
    }

    private fun requestStopwatchStateSoon() {
        viewModelScope.launch {
            delay(250)
            requestStopwatchState()
        }
    }

    private fun startTimerDisplayCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value && _timerSeconds.value > 0) {
                delay(1000)
                if (_isTimerRunning.value && _timerSeconds.value > 0) {
                    _timerSeconds.value -= 1
                }
            }
        }
    }

    private fun startStopwatchDisplayTicker(startMillis: Long) {
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis() - startMillis
            while (_isStopwatchRunning.value) {
                _stopwatchMillis.value = System.currentTimeMillis() - startedAt
                delay(50)
            }
        }
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
