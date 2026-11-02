package com.vayora.extension.evidence

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.LifecycleOwner
import com.vayora.extension.VayoraApp
import com.vayora.extension.cloud.SyncScheduler
import com.vayora.extension.monitor.DeviceStateReader
import com.vayora.extension.monitor.EventType
import com.vayora.extension.monitor.MonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EvidenceCoordinator(private val app: VayoraApp) {

    val settings = EvidenceSettings(app)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _requests = MutableSharedFlow<Long>(extraBufferCapacity = 4)

    /** Event ids waiting for a photo. The visible lock screen collects these. */
    val requests: SharedFlow<Long> = _requests.asSharedFlow()

    @Volatile
    private var lastAppCapture = 0L

    @Volatile
    private var lastUnlockCapture = 0L

    @Volatile
    private var lastSystemCapture = 0L

    /** Wrong Vayora passcode: record it and ask the visible lock screen for a photo. */
    fun onWrongPasscode(failures: Int) {
        scope.launch {
            val eventId = app.repository.recordEvent(
                EventType.WRONG_PASSCODE,
                "Incorrect Vayora Extension passcode. Consecutive wrong attempts: $failures",
                DeviceStateReader.describe(app)
            )
            SyncScheduler.syncSoon(app)
            val now = System.currentTimeMillis()
            val wantsPhoto = settings.state.value.captureOnAppFail &&
                now - lastAppCapture > MIN_GAP_MS &&
                CameraCapture.hasPermission(app)
            if (wantsPhoto) {
                lastAppCapture = now
                _requests.emit(eventId)
            }
        }
    }

    /** Successful phone unlock that has just been recorded as [eventId]. */
    fun onUnlockEvent(eventId: Long) {
        val config = settings.state.value
        if (!config.captureOnUnlock || !CameraCapture.hasPermission(app)) return
        val now = System.currentTimeMillis()
        val gapMs = config.unlockGapMinutes * 60_000L
        if (now - lastUnlockCapture < gapMs) return
        lastUnlockCapture = now
        dispatch(eventId)
    }

    /** Failed lock-screen attempt that has just been recorded as [eventId]. */
    fun onSystemFailure(eventId: Long) {
        val config = settings.state.value
        if (!config.captureOnSystemFail || !CameraCapture.hasPermission(app)) return
        val now = System.currentTimeMillis()
        if (now - lastSystemCapture < MIN_GAP_MS) return
        lastSystemCapture = now
        dispatch(eventId)
    }

    private fun dispatch(eventId: Long) {
        val service = MonitorService.instance
        if (service != null && MonitorService.cameraActive.value) {
            service.captureFor(eventId)
        } else {
            visibleFallback(eventId)
        }
    }

    /**
     * Shows the visible capture screen. Needs the "display over other apps" permission
     * to open from the background; otherwise a full-screen notification is posted.
     */
    fun visibleFallback(eventId: Long) {
        try {
            if (Settings.canDrawOverlays(app)) {
                val intent = Intent(app, CaptureActivity::class.java).apply {
                    putExtra(CaptureActivity.EXTRA_EVENT_ID, eventId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(intent)
            } else {
                FailedUnlockCapture.trigger(app, eventId)
            }
        } catch (e: Exception) {
            FailedUnlockCapture.trigger(app, eventId)
        }
    }

    suspend fun capture(context: Context, owner: LifecycleOwner, eventId: Long): CaptureResult {
        val result = CameraCapture.capture(context, owner, app.repository, eventId)
        if (result != CaptureResult.SUCCESS && eventId >= 0) {
            app.repository.recordEvent(
                EventType.CAPTURE_FAILED,
                "Reason: ${result.name}",
                DeviceStateReader.describe(app)
            )
        }
        return result
    }

    companion object {
        private const val MIN_GAP_MS = 8_000L
    }
}
