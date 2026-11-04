package com.vayora.extension.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.vayora.extension.MainActivity
import com.vayora.extension.R
import com.vayora.extension.VayoraApp
import com.vayora.extension.evidence.CameraCapture
import com.vayora.extension.evidence.CaptureResult
import com.vayora.extension.evidence.TemporaryLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val captureLock = Mutex()
    private var promoted = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_USER_PRESENT) return
            val app = applicationContext as VayoraApp
            val recordUnlocks = app.monitorSettings.state.value.recordUnlocks
            val wantsPhoto = app.evidenceCoordinator.settings.state.value.captureOnUnlock
            if (recordUnlocks || wantsPhoto) {
                EventRecorder.record(
                    app,
                    EventType.UNLOCK,
                    "Phone unlocked. Android does not report which method was used.",
                    onRecorded = { eventId ->
                        if (wantsPhoto) app.evidenceCoordinator.onUnlockEvent(eventId)
                    }
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
        ContextCompat.registerReceiver(
            this,
            unlockReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_EXPORTED
        )
        _running.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!promoted) {
            promoted = true
            val fromUi = intent?.getBooleanExtra(EXTRA_FROM_UI, false) ?: false
            promoteToForeground(fromUi)
        }
        return START_STICKY
    }

    private fun promoteToForeground(fromUi: Boolean) {
        val notification = buildNotification()
        val cameraAllowed = fromUi &&
            Build.VERSION.SDK_INT >= 30 &&
            CameraCapture.hasPermission(this)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                if (cameraAllowed) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                startForeground(NOTIFICATION_ID, notification, type)
            } else if (cameraAllowed) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            _cameraActive.value = cameraAllowed
        } catch (e: Exception) {
            _cameraActive.value = false
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Takes a photo from inside the running foreground service. */
    fun captureFor(eventId: Long) {
        scope.launch {
            if (!captureLock.tryLock()) return@launch
            val owner = TemporaryLifecycleOwner()
            try {
                owner.start()
                val app = applicationContext as VayoraApp
                val result = app.evidenceCoordinator.capture(this@MonitorService, owner, eventId)
                if (result != CaptureResult.SUCCESS) {
                    app.evidenceCoordinator.visibleFallback(eventId)
                }
            } finally {
                owner.destroy()
                captureLock.unlock()
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: IllegalArgumentException) {
            // already unregistered
        }
        instance = null
        _running.value = false
        _cameraActive.value = false
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Security monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Shown while Sentinel monitors device security events"
        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Sentinel")
            .setContentText("Device security monitoring is on")
            .setOngoing(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "vayora_monitor"
        private const val NOTIFICATION_ID = 4101
        private const val EXTRA_FROM_UI = "from_ui"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        private val _cameraActive = MutableStateFlow(false)

        /** True when the service holds background camera access. */
        val cameraActive: StateFlow<Boolean> = _cameraActive.asStateFlow()

        @Volatile
        var instance: MonitorService? = null
            private set

        fun start(context: Context, fromUi: Boolean = true) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, MonitorService::class.java).putExtra(EXTRA_FROM_UI, fromUi)
                )
            } catch (e: Exception) {
                // Android refused a background start; it will start next time the app opens.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
