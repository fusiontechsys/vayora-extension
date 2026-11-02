package com.vayora.extension.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vayora.extension.VayoraApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as VayoraApp
        if (app.monitorSettings.state.value.enabled) {
            EventRecorder.record(
                app,
                EventType.BOOT,
                "Phone restarted and was unlocked for the first time.",
                pending = goAsync()
            )
            MonitorService.start(app)
        }
    }
}
