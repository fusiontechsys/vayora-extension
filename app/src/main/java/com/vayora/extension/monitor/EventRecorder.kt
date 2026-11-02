package com.vayora.extension.monitor

import android.content.BroadcastReceiver
import android.content.Context
import com.vayora.extension.VayoraApp
import com.vayora.extension.cloud.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object EventRecorder {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun record(
        context: Context,
        type: String,
        details: String = "",
        syncNow: Boolean = false,
        pending: BroadcastReceiver.PendingResult? = null
    ) {
        val app = context.applicationContext as VayoraApp
        val state = DeviceStateReader.describe(app)
        scope.launch {
            try {
                app.repository.recordEvent(type, details, state)
                if (syncNow) SyncScheduler.syncSoon(app)
            } finally {
                pending?.finish()
            }
        }
    }
}
