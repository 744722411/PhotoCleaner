package com.photocleaner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.photocleaner.MainActivity
import com.photocleaner.data.repository.SettingsRepository
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.usecase.ClassifyLogStatus
import com.photocleaner.domain.usecase.ClassifyPhotosUseCase
import com.photocleaner.domain.usecase.ScanLogStatus
import com.photocleaner.domain.usecase.ScanPhotosUseCase
import com.photocleaner.ui.scan.LogStatus
import com.photocleaner.ui.scan.ScanLogEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScanService : Service() {

    @Inject lateinit var scanPhotosUseCase: ScanPhotosUseCase
    @Inject lateinit var classifyPhotosUseCase: ClassifyPhotosUseCase
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var photoRepository: PhotoRepository
    @Inject lateinit var stateHolder: ScanStateHolder

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var scanJob: Job? = null
    private var classifyJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCAN -> startScan()
            ACTION_STOP -> stopAll()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun startScan() {
        // Prevent duplicate starts
        val currentState = stateHolder.uiState.value
        if (currentState.isScanning || currentState.isClassifying) return

        // Start foreground immediately
        val notification = buildNotification("正在准备扫描...", null, 0, 0)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )

        acquireWakeLock()

        // Reset state
        stateHolder.updateState {
            it.copy(
                isScanning = true,
                isPaused = false,
                error = null,
                scanComplete = false,
                classifyComplete = false,
                scanLogs = emptyList()
            )
        }

        stateHolder.addLog(ScanLogEntry(message = "\uD83D\uDD0D 开始扫描照片...", status = LogStatus.INFO))

        scanJob = serviceScope.launch {
            try {
                val batchSize = settingsRepository.getBatchSizeSync()
                val selectedDirectories = settingsRepository.getSelectedDirectoriesSync()

                if (batchSize > 0) {
                    stateHolder.addLog(ScanLogEntry(message = "📦 每次处理数量: $batchSize", status = LogStatus.INFO))
                }

                val isPaused = { stateHolder.uiState.value.isPaused }

                val photos = scanPhotosUseCase(
                    selectedDirectories = selectedDirectories,
                    batchSize = batchSize,
                    isPaused = isPaused,
                    onProgress = { scanned, total ->
                        stateHolder.updateState { state ->
                            state.copy(scannedCount = scanned, totalToScan = total)
                        }
                        updateNotification("📷 正在扫描照片...", scanned, total, isClassifying = false)
                    },
                    onLog = { log ->
                        stateHolder.addLog(ScanLogEntry(
                            photoName = log.photoName,
                            status = when (log.status) {
                                ScanLogStatus.PROCESSING -> LogStatus.PROCESSING
                                ScanLogStatus.LOCAL_HIT -> LogStatus.LOCAL_HIT
                                ScanLogStatus.SUCCESS -> LogStatus.SUCCESS
                                ScanLogStatus.ERROR -> LogStatus.ERROR
                                ScanLogStatus.INFO -> LogStatus.INFO
                            },
                            message = log.message
                        ))
                    }
                )

                val uselessCount = photos.count { it.classification == Classification.USELESS }
                stateHolder.updateState {
                    it.copy(
                        isScanning = false,
                        scanComplete = true,
                        photos = photos,
                        uselessFound = uselessCount
                    )
                }

                updateNotification("扫描完成，准备AI分类...", null, 0, isClassifying = false)

                // Automatically start AI classification
                startClassifyInternal(photos)

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    stateHolder.updateState {
                        it.copy(isScanning = false, error = e.message ?: "扫描失败")
                    }
                    showCompletionNotification("❌ 扫描失败: ${e.message}")
                    stopSelf()
                }
            }
        }
    }

    private fun startClassifyInternal(photos: List<Photo>) {
        classifyJob = serviceScope.launch {
            stateHolder.updateState {
                it.copy(isClassifying = true, isClassifyPaused = false, classifyComplete = false)
            }

            stateHolder.addLog(ScanLogEntry(message = "🤖 开始AI智能分类...", status = LogStatus.INFO))

            try {
                val isClassifyPaused = { stateHolder.uiState.value.isClassifyPaused }

                val results = classifyPhotosUseCase(
                    photos,
                    isPaused = isClassifyPaused,
                    onProgress = { classified, total ->
                        stateHolder.updateState { state ->
                            state.copy(classifiedCount = classified, totalToClassify = total)
                        }
                        updateNotification("🤖 AI正在分类...", classified, total, isClassifying = true)
                    },
                    onLog = { log ->
                        stateHolder.addLog(ScanLogEntry(
                            photoName = log.photoName,
                            status = when (log.status) {
                                ClassifyLogStatus.PROCESSING -> LogStatus.PROCESSING
                                ClassifyLogStatus.SUCCESS -> LogStatus.SUCCESS
                                ClassifyLogStatus.SKIP -> LogStatus.SKIP
                                ClassifyLogStatus.ERROR -> LogStatus.ERROR
                                ClassifyLogStatus.INFO -> LogStatus.INFO
                            },
                            message = log.message,
                            details = log.aiResult
                        ))
                    }
                )

                val uselessCount = results.count { it.classification == Classification.USELESS }
                stateHolder.updateState {
                    it.copy(
                        isClassifying = false,
                        classifyComplete = true,
                        photos = results,
                        uselessFound = uselessCount
                    )
                }

                showCompletionNotification("✅ 扫描完成！发现 $uselessCount 张无用照片")
                stopSelf()

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    stateHolder.updateState {
                        it.copy(isClassifying = false, error = e.message ?: "分类失败")
                    }
                    showCompletionNotification("❌ AI分类失败: ${e.message}")
                    stopSelf()
                }
            }
        }
    }

    private fun stopAll() {
        scanJob?.cancel()
        classifyJob?.cancel()
        stateHolder.updateState {
            it.copy(
                isScanning = false,
                isPaused = false,
                isClassifying = false,
                isClassifyPaused = false
            )
        }
        stateHolder.addLog(ScanLogEntry(message = "⏹️ 已停止", status = LogStatus.INFO))
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --- Notification helpers ---

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "照片扫描",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示照片扫描进度"
            setShowBadge(false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(
        text: String,
        progress: Int?,
        current: Int,
        total: Int,
        isClassifying: Boolean = false
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ScanService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isClassifying) "🤖 AI 智能分类" else "📷 照片扫描")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (progress != null && total > 0) {
            builder.setProgress(total, progress, false)
            builder.setContentText("$text ($current/$total)")
        } else if (total > 0) {
            builder.setProgress(total, 0, true)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun updateNotification(
        text: String,
        current: Int?,
        total: Int,
        isClassifying: Boolean
    ) {
        val notification = buildNotification(
            text = text,
            progress = current,
            current = current ?: 0,
            total = total,
            isClassifying = isClassifying
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(text: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Photo Cleaner")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    // --- Wake lock ---

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PhotoCleaner::ScanWakeLock"
            ).apply {
                acquire(60 * 60 * 1000L) // 1 hour max
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    companion object {
        const val ACTION_START_SCAN = "com.photocleaner.action.START_SCAN"
        const val ACTION_STOP = "com.photocleaner.action.STOP"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "scan_channel"

        fun startScan(context: Context) {
            val intent = Intent(context, ScanService::class.java).apply {
                action = ACTION_START_SCAN
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScanService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
