package com.dotmatrix.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dotmatrix.app.ble.BleManager
import com.dotmatrix.app.utils.PhoneMicVisualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object PhoneMicVisualizerRuntime {
    val isRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val sensitivity = kotlinx.coroutines.flow.MutableStateFlow(50)
}

class PhoneMicVisualizerService : Service() {
    companion object {
        private const val CHANNEL_ID = "phone_mic_visualizer"
        private const val CHANNEL_NAME = "Phone Mic Visualizer"
        private const val NOTIFICATION_ID = 1103
        private const val ACTION_START = "com.dotmatrix.app.action.START_PHONE_MIC_VISUALIZER"
        private const val EXTRA_SENSITIVITY = "extra_sensitivity"

        fun start(context: Context, sensitivity: Int) {
            val intent = Intent(context, PhoneMicVisualizerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SENSITIVITY, sensitivity)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PhoneMicVisualizerService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var bleConnectionJob: Job? = null
    private lateinit var bleManager: BleManager
    private lateinit var phoneMicVisualizer: PhoneMicVisualizer

    override fun onCreate() {
        super.onCreate()
        bleManager = BleManager.shared(applicationContext)
        phoneMicVisualizer = PhoneMicVisualizer(
            context = applicationContext,
            sensitivityProvider = { PhoneMicVisualizerRuntime.sensitivity.value },
            onFrame = { levels ->
                val payload = levels.joinToString(",") { it.coerceIn(0, 8).toString() }
                bleManager.writeData("VIZFRAME:$payload")
            },
            onError = {
                PhoneMicVisualizerRuntime.isRunning.value = false
                stopSelf()
            }
        )
        createChannel()
        bleConnectionJob = serviceScope.launch {
            bleManager.isConnected.collectLatest { connected ->
                if (!connected && PhoneMicVisualizerRuntime.isRunning.value) {
                    stopStreaming()
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START, null -> {
                val requestedSensitivity = intent?.getIntExtra(EXTRA_SENSITIVITY, PhoneMicVisualizerRuntime.sensitivity.value)
                    ?: PhoneMicVisualizerRuntime.sensitivity.value
                PhoneMicVisualizerRuntime.sensitivity.value = requestedSensitivity.coerceIn(0, 100)
                startForeground(NOTIFICATION_ID, buildNotification())
                startStreaming()
                return START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        bleConnectionJob?.cancel()
        stopStreaming()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startStreaming() {
        if (!bleManager.isConnected.value) {
            stopSelf()
            return
        }
        phoneMicVisualizer.start()
        PhoneMicVisualizerRuntime.isRunning.value = phoneMicVisualizer.isRunning()
    }

    private fun stopStreaming() {
        phoneMicVisualizer.stop()
        PhoneMicVisualizerRuntime.isRunning.value = false
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the phone microphone visualizer active in the background"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Phone Mic Visualizer Running")
            .setContentText("Streaming phone microphone audio to your DotMatrix clock")
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
