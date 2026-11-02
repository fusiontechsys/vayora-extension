package com.vayora.extension.monitor

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

object DeviceStateReader {

    fun describe(context: Context): String {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1

        val screen = if (power.isInteractive) "Screen on" else "Screen off"
        val lock = if (keyguard.isKeyguardLocked) "lock screen showing" else "lock screen dismissed"
        val batteryText = if (percent >= 0) {
            "battery $percent%" + if (charging) " (charging)" else ""
        } else {
            "battery unknown"
        }
        return "$screen, $lock, $batteryText"
    }
}
