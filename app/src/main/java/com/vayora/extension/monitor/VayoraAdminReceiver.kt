package com.vayora.extension.monitor

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent

class VayoraAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        val attempts = try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.currentFailedPasswordAttempts
        } catch (e: SecurityException) {
            -1
        }
        val details = if (attempts > 0) {
            "Incorrect lock-screen credential. Failed attempts since last success: $attempts"
        } else {
            "Incorrect lock-screen credential."
        }
        EventRecorder.record(
            context,
            EventType.FAILED_AUTH,
            details,
            syncNow = true,
            pending = goAsync()
        )
    }

    override fun onEnabled(context: Context, intent: Intent) {
        EventRecorder.record(
            context,
            EventType.ADMIN_ON,
            "Device Admin approved for failed-attempt detection.",
            pending = goAsync()
        )
    }

    override fun onDisabled(context: Context, intent: Intent) {
        EventRecorder.record(
            context,
            EventType.ADMIN_OFF,
            "Device Admin was turned off. Failed attempts are no longer detected.",
            syncNow = true,
            pending = goAsync()
        )
    }
}
