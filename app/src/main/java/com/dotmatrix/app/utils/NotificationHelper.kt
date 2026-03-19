package com.dotmatrix.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.dotmatrix.app.MainActivity
import com.dotmatrix.app.R

class NotificationHelper(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "dotmatrix_notifications"
        private const val CHANNEL_NAME = "DotMatrix Notifications"
        private const val CHANNEL_DESC = "Notifications for device connection and updates"
        private const val CONN_NOTIF_ID = 1001
        private const val UPDATE_NOTIF_ID = 1002
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW // Low so it doesn't make sound every time for persistent
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showConnectionNotification(deviceName: String, batteryLevel: Int? = null) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val batteryText = if (batteryLevel != null) " - $batteryLevel% battery" else ""
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("DotMatrix Clock Connected")
            .setContentText("$deviceName is active$batteryText")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Makes it persistent while connected
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        try {
            notificationManager.notify(CONN_NOTIF_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun dismissConnectionNotification() {
        notificationManager.cancel(CONN_NOTIF_ID)
    }

    fun showUpdateNotification(version: String) {
        if (!canPostNotifications()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("New Update Available")
            .setContentText("Version $version is now available for your DotMatrix Clock.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(UPDATE_NOTIF_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
