package com.photocleaner

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.photocleaner.service.ScanService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PhotoCleanerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val scanChannel = NotificationChannel(
            ScanService.CHANNEL_ID,
            "照片扫描",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示照片扫描和分类的进度"
            setShowBadge(false)
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(scanChannel)
    }
}
