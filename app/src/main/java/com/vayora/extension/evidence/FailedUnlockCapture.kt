package com.vayora.extension.evidence

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vayora.extension.R
import com.vayora.extension.VayoraApp
import com.vayora.extension.monitor.DeviceStateReader
import com.vayora.extension.monitor.EventType

/**
 * Android blocks silent camera use from the background. The supported route is a
 * full-screen notification that opens a visible capture screen over the lock screen.
 * Whether a given phone allows it must be tested on the phone itself.
 */
object FailedUnlockCapture {
    private const val CHANNEL_ID = "vayora_evidence"
    private const val NOTIFICATION_ID = 4202

    fun trigger(context: Context, eventId: Long) {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Security photo",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
        manager.createNotificationChannel(channel)

        val intent = Intent(app, CaptureActivity::class.java).apply {
            putExtra(CaptureActivity.EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            app,
            (eventId % 100000).toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Sentinel")
            .setContentText("Recording a security photo")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setFullScreenIntent(pending, true)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setTimeoutAfter(20_000)
            .build()
        NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, notification)
    }
}

class LockScreenTestWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as VayoraApp
        val eventId = app.repository.recordEvent(
            EventType.LOCKSCREEN_TEST,
            "Lock-screen capture test started from Device Security",
            DeviceStateReader.describe(app)
        )
        FailedUnlockCapture.trigger(app, eventId)
        return Result.success()
    }
}
