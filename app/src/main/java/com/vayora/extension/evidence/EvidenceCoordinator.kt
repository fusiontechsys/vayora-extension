package com.vayora.extension.evidence

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.vayora.extension.VayoraApp
import com.vayora.extension.cloud.SyncScheduler
import com.vayora.extension.monitor.DeviceStateReader
import com.vayora.extension.monitor.EventType
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
                failures >= 2 &&
                now - lastAppCapture > 15_000 &&
                CameraCapture.hasPermission(app)
            if (wantsPhoto) {
                lastAppCapture = now
                _requests.emit(eventId)
            }
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
}
