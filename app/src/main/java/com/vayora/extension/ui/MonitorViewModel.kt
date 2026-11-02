package com.vayora.extension.ui

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vayora.extension.VayoraApp
import com.vayora.extension.data.SecurityEvent
import com.vayora.extension.evidence.CaptureResult
import com.vayora.extension.evidence.EvidenceConfig
import com.vayora.extension.evidence.LockScreenTestWorker
import com.vayora.extension.monitor.DeviceStateReader
import com.vayora.extension.monitor.EventType
import com.vayora.extension.monitor.MonitorConfig
import com.vayora.extension.monitor.MonitorService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MonitorViewModel(private val app: VayoraApp) : ViewModel() {

    val settings: StateFlow<MonitorConfig> = app.monitorSettings.state

    val evidenceSettings: StateFlow<EvidenceConfig> = app.evidenceCoordinator.settings.state

    val events: StateFlow<List<SecurityEvent>> = app.repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setMonitoring(enabled: Boolean) {
        app.monitorSettings.setEnabled(enabled)
        if (enabled) {
            MonitorService.start(app, true)
        } else {
            MonitorService.stop(app)
        }
    }

    /** Restarts the service so it can pick up newly granted camera access. */
    fun restartMonitoring() {
        if (app.monitorSettings.state.value.enabled) {
            MonitorService.stop(app)
            MonitorService.start(app, true)
        }
    }

    fun setRecordUnlocks(value: Boolean) {
        app.monitorSettings.setRecordUnlocks(value)
    }

    fun setCaptureOnAppFail(value: Boolean) {
        app.evidenceCoordinator.settings.setCaptureOnAppFail(value)
    }

    fun setCaptureOnSystemFail(value: Boolean) {
        app.evidenceCoordinator.settings.setCaptureOnSystemFail(value)
        restartMonitoring()
    }

    fun setCaptureOnUnlock(value: Boolean) {
        app.evidenceCoordinator.settings.setCaptureOnUnlock(value)
        restartMonitoring()
    }

    fun setUnlockGapMinutes(value: Int) {
        app.evidenceCoordinator.settings.setUnlockGapMinutes(value)
    }

    suspend fun testPhoto(context: Context, owner: LifecycleOwner): CaptureResult {
        val eventId = app.repository.recordEvent(
            EventType.CAPTURE_TEST,
            "Manual test photo from Device Security",
            DeviceStateReader.describe(app)
        )
        return app.evidenceCoordinator.capture(context, owner, eventId)
    }

    fun scheduleLockScreenTest() {
        val request = OneTimeWorkRequestBuilder<LockScreenTestWorker>()
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(app).enqueue(request)
    }

    companion object {
        fun factory(app: VayoraApp): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { MonitorViewModel(app) }
            }
    }
}
