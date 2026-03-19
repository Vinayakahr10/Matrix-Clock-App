package com.dotmatrix.app.ble

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.*

/**
 * Possible BLE error states surfaced to the UI.
 */
enum class BleError {
    PERMISSION_DENIED,
    BLUETOOTH_DISABLED,
    LOCATION_DISABLED,
    SCAN_FAILED,
    CONNECTION_FAILED,
    SERVICE_NOT_FOUND,
    CHARACTERISTIC_NOT_FOUND,
    WRITE_FAILED,
    OTA_FAILED
}

/**
 * Wrapper for a scanned BLE device to hold its resolved display name and signal strength.
 */
data class ScannedDevice(
    val device: BluetoothDevice,
    val displayName: String,
    val rssi: Int = -100
)

class BleManager(private val context: Context) {
    companion object {
        private const val TAG = "BleManager"
        private const val DEFAULT_DEVICE_NAME = "DotMatrix Clock"

        // Standard Services
        val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_CHAR_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        val DEVICE_INFO_SERVICE_UUID: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        val MANUFACTURER_NAME_CHAR_UUID: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        val MODEL_NUMBER_CHAR_UUID: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
        val FIRMWARE_REVISION_CHAR_UUID: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")

        // Custom App Service
        val CUSTOM_SERVICE_UUID: UUID = UUID.fromString("6b5f9001-3d10-4f76-8e22-4b0d6e6a1001")
        val RX_CHAR_UUID: UUID = UUID.fromString("6b5f9002-3d10-4f76-8e22-4b0d6e6a1001") // Write
        val TX_CHAR_UUID: UUID = UUID.fromString("6b5f9003-3d10-4f76-8e22-4b0d6e6a1001") // Notify

        val CLIENT_CONFIG_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    fun getAdapter(): BluetoothAdapter? = bluetoothAdapter

    private var bluetoothGatt: BluetoothGatt? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writeMutex = Mutex()
    private var characteristicWriteDeferred: CompletableDeferred<Int>? = null
    private var mtuDeferred: CompletableDeferred<Int>? = null
    private var negotiatedMtu: Int = 23

    // ── Auto-reconnection ─────────────────────────────────────────────────────
    private var lastConnectedDevice: BluetoothDevice? = null
    private var lastKnownName: String? = null
    private var autoReconnectEnabled = false
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private val maxReconnectAttempts = 50
    private val reconnectScanWindowMs = 10000L

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    // ── Public state flows ────────────────────────────────────────────────────
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _deviceName = MutableStateFlow("Not Connected")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<BleError?>(null)
    val error: StateFlow<BleError?> = _error.asStateFlow()
    
    // ── Device Data State ─────────────────────────────────────────────────────
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _firmwareVersion = MutableStateFlow<String?>("Unknown")
    val firmwareVersion: StateFlow<String?> = _firmwareVersion.asStateFlow()

    private val _manufacturerName = MutableStateFlow<String?>(null)
    val manufacturerName: StateFlow<String?> = _manufacturerName.asStateFlow()

    private val _modelNumber = MutableStateFlow<String?>(null)
    val modelNumber: StateFlow<String?> = _modelNumber.asStateFlow()

    // Environment Data (Parsed via TX)
    private val _temperature = MutableStateFlow<Float?>(null)
    val temperature: StateFlow<Float?> = _temperature.asStateFlow()

    private val _humidity = MutableStateFlow<Float?>(null)
    val humidity: StateFlow<Float?> = _humidity.asStateFlow()

    // ── OTA State ─────────────────────────────────────────────────────────────
    private val _otaStatusMessage = MutableStateFlow("")
    val otaStatusMessage: StateFlow<String> = _otaStatusMessage.asStateFlow()

    private var otaReadyDeferred = CompletableDeferred<Unit>()
    private var otaAckChannel = Channel<Unit>(Channel.CONFLATED)
    private var otaDoneDeferred = CompletableDeferred<Unit>()
    
    private var otaLastAckBytes = 0L
    private var otaError: String? = null

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun clearError() { _error.value = null }

    // ── Scanning ──────────────────────────────────────────────────────────────
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val device = result.device
                val scanRecord = result.scanRecord
                val rssi = result.rssi
                
                val nameFromScanRecord = scanRecord?.deviceName
                val nameFromDevice = try { 
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        device.name 
                    } else null
                } catch (e: SecurityException) { null }
                
                val resolvedName = nameFromDevice ?: nameFromScanRecord ?: "Unknown Device"
                
                val currentList = _scannedDevices.value
                val existingIndex = currentList.indexOfFirst { it.device.address == device.address }
                
                val updatedList = if (existingIndex == -1) {
                    currentList + ScannedDevice(device, resolvedName, rssi)
                } else {
                    val existing = currentList[existingIndex]
                    if (existing.displayName == "Unknown Device" && resolvedName != "Unknown Device" || Math.abs(existing.rssi - rssi) > 5) {
                        val updated = currentList.toMutableList()
                        updated[existingIndex] = ScannedDevice(device, resolvedName, rssi)
                        updated
                    } else {
                        currentList
                    }
                }

                _scannedDevices.value = updatedList.sortedWith(
                    compareByDescending<ScannedDevice> { it.displayName == DEFAULT_DEVICE_NAME }
                        .thenByDescending { it.displayName != "Unknown Device" }
                        .thenByDescending { it.rssi }
                )

                maybeReconnectToMatchedDevice(device, resolvedName)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onScanResult", e)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _error.value = BleError.SCAN_FAILED
        }
    }

    fun startScan() {
        _error.value = null
        if (!hasBluetoothPermissions()) { _error.value = BleError.PERMISSION_DENIED; return }
        if (!isBluetoothEnabled()) { _error.value = BleError.BLUETOOTH_DISABLED; return }
        if (!isLocationEnabled()) { _error.value = BleError.LOCATION_DISABLED; return }

        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            _scannedDevices.value = emptyList()
            _isScanning.value = true
            bluetoothAdapter?.bluetoothLeScanner?.startScan(null, settings, scanCallback)

            scope.launch {
                delay(15000)
                if (_isScanning.value) stopScan()
            }
        } catch (e: SecurityException) {
            _error.value = BleError.PERMISSION_DENIED
        }
    }

    fun stopScan() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
        } finally {
            _isScanning.value = false
        }
    }

    // ── Connection ────────────────────────────────────────────────────────────
    fun connect(device: BluetoothDevice, name: String? = null) {
        Log.d(TAG, "Connecting to ${device.address}, name: $name")
        _error.value = null
        cancelReconnect()

        if (!hasBluetoothPermissions()) {
            _error.value = BleError.PERMISSION_DENIED
            return
        }

        try {
            stopScan()
            try {
                bluetoothGatt?.close()
            } catch (_: SecurityException) {
            }
            bluetoothGatt = null
            lastConnectedDevice = device
            lastKnownName = name ?: _scannedDevices.value.find { it.device.address == device.address }?.displayName
            
            autoReconnectEnabled = true
            reconnectAttempt = 0
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            _error.value = BleError.PERMISSION_DENIED
        }
    }

    fun disconnect() {
        Log.d(TAG, "Manual disconnect requested")
        autoReconnectEnabled = false
        cancelReconnect()
        try {
            bluetoothGatt?.disconnect()
        } catch (e: SecurityException) {}
    }

    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        _isReconnecting.value = false
    }

    private fun scheduleReconnect() {
        val device = lastConnectedDevice ?: return
        if (!autoReconnectEnabled || reconnectAttempt >= maxReconnectAttempts) {
            _isReconnecting.value = false
            return
        }

        _isReconnecting.value = true
        val delayMs = (2000L * (1L shl reconnectAttempt.coerceAtMost(4))).coerceAtMost(30000L)

        Log.d(TAG, "Scheduling reconnect attempt $reconnectAttempt in ${delayMs}ms")
        reconnectJob = scope.launch {
            delay(delayMs)
            reconnectAttempt++
            if (!hasBluetoothPermissions() || !isBluetoothEnabled() || !isLocationEnabled()) {
                _isReconnecting.value = false
                return@launch
            }

            val reconnectName = lastKnownName ?: DEFAULT_DEVICE_NAME
            Log.d(TAG, "Starting reconnect scan for ${device.address} / $reconnectName")

            try {
                stopScan()
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                _isScanning.value = true
                bluetoothAdapter?.bluetoothLeScanner?.startScan(null, settings, scanCallback)
            } catch (e: SecurityException) {
                _isScanning.value = false
                _isReconnecting.value = false
                return@launch
            }

            delay(reconnectScanWindowMs)

            if (!_isConnected.value && autoReconnectEnabled) {
                stopScan()
                scheduleReconnect()
            }
        }
    }

    private fun maybeReconnectToMatchedDevice(device: BluetoothDevice, resolvedName: String) {
        if (!_isReconnecting.value || _isConnected.value || !autoReconnectEnabled) return

        val lastAddress = lastConnectedDevice?.address
        val nameMatches = lastKnownName != null && resolvedName == lastKnownName
        val fallbackNameMatches = resolvedName == DEFAULT_DEVICE_NAME && lastKnownName.isNullOrBlank()
        val addressMatches = lastAddress != null && device.address.equals(lastAddress, ignoreCase = true)

        if (addressMatches || nameMatches || fallbackNameMatches) {
            Log.d(TAG, "Reconnect scan matched ${device.address} ($resolvedName), reconnecting")
            connect(device, resolvedName)
        }
    }

    // ── Data writing ──────────────────────────────────────────────────────────
    suspend fun writeDataSync(data: String) {
        writeBytesSync(data.toByteArray())
    }

    suspend fun writeBytesSync(data: ByteArray) = writeMutex.withLock {
        if (!_isConnected.value) return@withLock
        val gatt = bluetoothGatt ?: return@withLock
        val service = gatt.getService(CUSTOM_SERVICE_UUID) ?: return@withLock
        val characteristic = service.getCharacteristic(RX_CHAR_UUID) ?: return@withLock

        characteristicWriteDeferred = CompletableDeferred()
        
        try {
            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = data
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }

            if (success) {
                withTimeoutOrNull(5000) {
                    characteristicWriteDeferred?.await()
                }
            } else {
                Log.e(TAG, "Failed to initiate writeCharacteristic")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during write", e)
        } finally {
            characteristicWriteDeferred = null
        }
    }

    fun writeData(data: String) {
        scope.launch { writeDataSync(data) }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun calculateMD5(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun streamFile(bytes: ByteArray, token: String? = null, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        otaReadyDeferred = CompletableDeferred()
        otaAckChannel = Channel(Channel.CONFLATED)
        otaDoneDeferred = CompletableDeferred()
        otaLastAckBytes = 0L
        otaError = null
        
        _otaStatusMessage.value = "Negotiating connection..."
        
        mtuDeferred = CompletableDeferred()
        bluetoothGatt?.requestMtu(512)
        val currentMtu = withTimeoutOrNull(2000) { mtuDeferred?.await() } ?: negotiatedMtu
        negotiatedMtu = currentMtu
        
        _otaStatusMessage.value = "Preparing OTA..."
        val md5 = calculateMD5(bytes)
        
        if (token != null) {
            writeDataSync("OTA_BEGIN:${bytes.size},$md5,$token")
        } else {
            writeDataSync("OTA_BEGIN:${bytes.size},$md5")
        }
        
        try {
            withTimeout(15000) {
                otaReadyDeferred.await()
            }
        } catch (e: Exception) {
            throw Exception(otaError ?: "OTA_BEGIN timeout or failed")
        }

        // ATT payload is MTU - 3 bytes. OTA chunks are hex-encoded and prefixed with "OTA_CHUNK:",
        // so the raw firmware chunk must fit inside the negotiated write payload.
        val maxWritePayload = (negotiatedMtu - 3).coerceAtLeast(20)
        val commandPrefixLength = "OTA_CHUNK:".length
        val maxHexChars = (maxWritePayload - commandPrefixLength).coerceAtLeast(32)
        val chunkSize = (maxHexChars / 2).coerceAtMost(240)
        val totalBytes = bytes.size
        
        _otaStatusMessage.value = "Uploading firmware..."
        
        for (i in 0 until totalBytes step chunkSize) {
            if (otaError != null) throw Exception(otaError)
            
            val end = (i + chunkSize).coerceAtMost(totalBytes)
            val hexData = bytesToHex(bytes.copyOfRange(i, end))
            
            while (otaAckChannel.tryReceive().isSuccess) { /* drain */ }
            
            writeDataSync("OTA_CHUNK:$hexData")
            
            try {
                withTimeout(20000) { 
                    otaAckChannel.receive()
                }
            } catch (e: Exception) {
                throw Exception(otaError ?: "OTA_CHUNK ACK timeout at byte $i")
            }
            
            withContext(Dispatchers.Main) {
                onProgress(end.toFloat() / totalBytes.toFloat())
            }
        }
        
        _otaStatusMessage.value = "Finalizing update..."
        writeDataSync("OTA_END")
        
        try {
            withTimeout(60000) { 
                otaDoneDeferred.await()
            }
        } catch (e: Exception) {
            throw Exception(otaError ?: "OTA_END completion timeout")
        }
        
        _otaStatusMessage.value = "Update successful! Rebooting..."
        delay(2000)
    }

    private fun enableNotifications(gatt: BluetoothGatt) {
        try {
            // 1. Enable Battery Level Notifications
            val battService = gatt.getService(BATTERY_SERVICE_UUID)
            val battChar = battService?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
            if (battChar != null) {
                gatt.setCharacteristicNotification(battChar, true)
                val descriptor = battChar.getDescriptor(CLIENT_CONFIG_DESCRIPTOR_UUID)
                if (descriptor != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }

            // 2. Enable Custom TX Notifications
            val customService = gatt.getService(CUSTOM_SERVICE_UUID)
            val txChar = customService?.getCharacteristic(TX_CHAR_UUID)
            if (txChar != null) {
                gatt.setCharacteristicNotification(txChar, true)
                val descriptor = txChar.getDescriptor(CLIENT_CONFIG_DESCRIPTOR_UUID)
                if (descriptor != null) {
                    scope.launch {
                        delay(200) 
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException enabling notifications", e)
        }
    }

    private fun readInitialCharacteristics(gatt: BluetoothGatt) {
        scope.launch {
            try {
                gatt.getService(BATTERY_SERVICE_UUID)?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)?.let {
                    gatt.readCharacteristic(it)
                    delay(250)
                }
                gatt.getService(DEVICE_INFO_SERVICE_UUID)?.getCharacteristic(MANUFACTURER_NAME_CHAR_UUID)?.let {
                    gatt.readCharacteristic(it)
                    delay(250)
                }
                gatt.getService(DEVICE_INFO_SERVICE_UUID)?.getCharacteristic(MODEL_NUMBER_CHAR_UUID)?.let {
                    gatt.readCharacteristic(it)
                    delay(250)
                }
                // Priority 1: Read Device Information characteristic 2A26
                gatt.getService(DEVICE_INFO_SERVICE_UUID)?.getCharacteristic(FIRMWARE_REVISION_CHAR_UUID)?.let {
                    gatt.readCharacteristic(it)
                    delay(250)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException reading characteristics", e)
            }
        }
    }

    private fun updateDeviceName(gatt: BluetoothGatt) {
        try {
            val deviceNameFromGatt = gatt.device.name
            val macRegex = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
            val nameToUse = when {
                deviceNameFromGatt != null && !macRegex.matches(deviceNameFromGatt) -> deviceNameFromGatt
                lastKnownName != null && !macRegex.matches(lastKnownName!!) -> lastKnownName!!
                else -> DEFAULT_DEVICE_NAME
            }
            _deviceName.value = nameToUse
        } catch (e: SecurityException) {
            _deviceName.value = lastKnownName ?: DEFAULT_DEVICE_NAME
        }
    }

    // ── GATT callbacks ────────────────────────────────────────────────────────
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "Connection status: $status, newState: $newState")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                handleDisconnect(gatt)
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _isConnected.value = true
                    _isReconnecting.value = false
                    reconnectAttempt = 0
                    updateDeviceName(gatt)
                    try {
                        gatt.requestMtu(512)
                    } catch (e: SecurityException) {
                        _error.value = BleError.PERMISSION_DENIED
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    handleDisconnect(gatt)
                }
            }
        }

        private fun handleDisconnect(gatt: BluetoothGatt) {
            _isConnected.value = false
            _deviceName.value = "Not Connected"
            _batteryLevel.value = null
            _firmwareVersion.value = "Unknown"
            _manufacturerName.value = null
            _modelNumber.value = null
            _temperature.value = null
            _humidity.value = null
            try { gatt.close() } catch (_: SecurityException) {}
            bluetoothGatt = null
            if (autoReconnectEnabled) scheduleReconnect()
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                negotiatedMtu = mtu
                mtuDeferred?.complete(mtu)
                try {
                    gatt?.discoverServices()
                } catch (e: SecurityException) {}
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateDeviceName(gatt)
                enableNotifications(gatt)
                readInitialCharacteristics(gatt)
                
                // Automatically send startup commands
                scope.launch {
                    delay(1500) // Ensure services and notifications are ready
                    
                    // Priority 2: If 2A26 read didn't work yet or as extra insurance
                    if (_firmwareVersion.value == "Unknown" || _firmwareVersion.value.isNullOrEmpty()) {
                        writeDataSync("VERSION?")
                        delay(300)
                    }
                    
                    writeDataSync("INFO")
                    delay(300)
                    writeDataSync("SENSOR?")
                    delay(300)
                    writeDataSync("OTA_STATUS")
                }
            } else {
                _error.value = BleError.SERVICE_NOT_FOUND
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicUpdate(characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleCharacteristicUpdate(characteristic)
        }

        private fun handleCharacteristicUpdate(characteristic: BluetoothGattCharacteristic) {
            when (characteristic.uuid) {
                BATTERY_LEVEL_CHAR_UUID -> {
                    val level = characteristic.value?.get(0)?.toInt() ?: return
                    _batteryLevel.value = level
                    Log.d(TAG, "Battery Level updated: $level%")
                }
                // Priority 1: Firmware Revision (2A26)
                FIRMWARE_REVISION_CHAR_UUID -> {
                    val fw = characteristic.value?.let { String(it).trim() }
                    if (!fw.isNullOrEmpty()) {
                        Log.d(TAG, "FW Version read from 2A26: $fw")
                        _firmwareVersion.value = fw
                    }
                }
                MANUFACTURER_NAME_CHAR_UUID -> {
                    _manufacturerName.value = characteristic.value?.let { String(it).trim() }
                }
                MODEL_NUMBER_CHAR_UUID -> {
                    _modelNumber.value = characteristic.value?.let { String(it).trim() }
                }
                TX_CHAR_UUID -> {
                    val data = characteristic.value?.let { String(it) } ?: return
                    Log.d(TAG, "TX Notification received: $data")
                    parseTxMessage(data)
                }
            }
        }

        private fun parseTxMessage(message: String) {
            try {
                val msg = message.trim()
                when {
                    msg.startsWith("OTA:") -> {
                        handleOtaNotification(msg.substring(4))
                    }
                    msg.startsWith("STATUS:") -> {
                        val status = msg.removePrefix("STATUS:").trim()
                        Log.d(TAG, "Device Status: $status")
                        if (status == "SECURE") {
                             _otaStatusMessage.value = "Connection Secured"
                        }
                    }
                    // Priority 2: VERSION:v1.0.1-dev
                    msg.startsWith("VERSION:") -> {
                        val fw = msg.removePrefix("VERSION:").trim()
                        if (fw.isNotEmpty()) {
                            Log.d(TAG, "FW Version parsed from VERSION: response: $fw")
                            _firmwareVersion.value = fw
                        }
                    }
                    msg.startsWith("SENSOR:") -> {
                        // SENSOR:T:27.4,H:61.0
                        val parts = msg.removePrefix("SENSOR:").split(",")
                        parts.forEach { part ->
                            val kv = part.trim().split(":")
                            if (kv.size == 2) {
                                when (kv[0].trim()) {
                                    "T" -> _temperature.value = kv[1].trim().toFloatOrNull()
                                    "H" -> _humidity.value = kv[1].trim().toFloatOrNull()
                                }
                            }
                        }
                    }
                    msg.startsWith("INFO:") -> {
                        // INFO:NAME:DotMatrix Clock,MODEL:DM-CLOCK-01,FW:v1.0.1-dev,BAT:100,T:27.4,H:61.0
                        val infoParts = msg.removePrefix("INFO:").split(",")
                        infoParts.forEach { part ->
                            val kv = part.trim().split(":")
                            if (kv.size == 2) {
                                val key = kv[0].trim()
                                val value = kv[1].trim()
                                when (key) {
                                    "NAME" -> _deviceName.value = value
                                    "MODEL" -> _modelNumber.value = value
                                    "FW" -> {
                                         // Priority 3: Fallback parsing from INFO
                                         if (_firmwareVersion.value == "Unknown" || _firmwareVersion.value.isNullOrEmpty()) {
                                             Log.d(TAG, "FW Version parsed from INFO fallback: $value")
                                             _firmwareVersion.value = value
                                         }
                                    }
                                    "BAT" -> _batteryLevel.value = value.toIntOrNull()
                                    "T" -> _temperature.value = value.toFloatOrNull()
                                    "H" -> _humidity.value = value.toFloatOrNull()
                                }
                            }
                        }
                    }
                    msg.startsWith("BATTERY:") -> {
                        _batteryLevel.value = msg.removePrefix("BATTERY:").trim().toIntOrNull()
                    }
                    msg.startsWith("ECHO:") -> {
                        Log.d(TAG, "Echo received: ${msg.removePrefix("ECHO:").trim()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing TX message: $message", e)
            }
        }

        private fun handleOtaNotification(payload: String) {
            val parts = payload.split(",")
            val command = parts[0].trim()
            
            when (command) {
                "READY" -> otaReadyDeferred.complete(Unit)
                "ACK" -> {
                    if (parts.size > 1) otaLastAckBytes = parts[1].trim().toLong()
                    otaAckChannel.trySend(Unit)
                }
                "DONE" -> otaDoneDeferred.complete(Unit)
                "ERROR" -> {
                    otaError = if (parts.size > 1) parts[1].trim() else "unknown error"
                    _otaStatusMessage.value = "Error: $otaError"
                    otaReadyDeferred.completeExceptionally(Exception(otaError))
                    otaDoneDeferred.completeExceptionally(Exception(otaError))
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            characteristicWriteDeferred?.complete(status)
        }
    }
}
