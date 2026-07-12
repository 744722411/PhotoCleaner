package com.photocleaner.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.photocleaner.R

class ScanForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.scan_notification_channel), NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_scan)
                .setContentTitle(getString(R.string.scan_notification_title))
                .setContentText(getString(R.string.scan_notification_body))
                .setOngoing(true)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf(startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "photo_scan"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) = context.startForegroundService(Intent(context, ScanForegroundService::class.java))
        fun stop(context: Context) = context.stopService(Intent(context, ScanForegroundService::class.java))
    }
}
