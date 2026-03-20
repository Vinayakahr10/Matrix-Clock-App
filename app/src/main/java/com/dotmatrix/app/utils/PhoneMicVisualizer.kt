package com.dotmatrix.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class PhoneMicVisualizer(
    private val context: Context,
    private val sensitivityProvider: () -> Int,
    private val onFrame: (List<Int>) -> Unit,
    private val onError: (String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var audioRecord: AudioRecord? = null
    private var streamJob: Job? = null

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isRunning(): Boolean = streamJob?.isActive == true

    fun start() {
        if (isRunning()) return
        if (!hasPermission()) {
            onError("Phone microphone permission is required")
            return
        }

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) {
            onError("Phone microphone is not available")
            return
        }

        val bufferSize = maxOf(minBufferSize, 4096)
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (_: Exception) {
            null
        }

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            onError("Unable to start phone microphone")
            return
        }

        audioRecord = record
        streamJob = scope.launch {
            val samples = ShortArray(bufferSize / 2)
            var lastFrameAt = 0L

            try {
                record.startRecording()
                while (isActive) {
                    val read = record.read(samples, 0, samples.size)
                    if (read <= 0) {
                        delay(50)
                        continue
                    }

                    val now = SystemClock.elapsedRealtime()
                    if (now - lastFrameAt < 120L) {
                        continue
                    }

                    lastFrameAt = now
                    onFrame(buildLevels(samples, read))
                }
            } catch (_: SecurityException) {
                onError("Phone microphone permission is required")
            } catch (_: IllegalStateException) {
                onError("Phone microphone stopped unexpectedly")
            } finally {
                try {
                    record.stop()
                } catch (_: IllegalStateException) {
                }
                record.release()
                if (audioRecord === record) {
                    audioRecord = null
                }
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        audioRecord?.let { record ->
            try {
                record.stop()
            } catch (_: IllegalStateException) {
            }
            record.release()
        }
        audioRecord = null
    }

    private fun buildLevels(samples: ShortArray, readCount: Int): List<Int> {
        val bucketCount = 32
        val bucketSize = maxOf(1, readCount / bucketCount)
        val gain = 0.65f + (sensitivityProvider().coerceIn(0, 100) / 100f) * 1.85f

        return List(bucketCount) { bucketIndex ->
            val start = bucketIndex * bucketSize
            if (start >= readCount) {
                0
            } else {
                val end = minOf(readCount, start + bucketSize)
                var sum = 0L
                for (i in start until end) {
                    sum += samples[i].toInt().absoluteValue
                }
                val average = if (end > start) sum.toFloat() / (end - start).toFloat() else 0f
                ((average / 32767f) * 8f * gain).roundToInt().coerceIn(0, 8)
            }
        }
    }
}
