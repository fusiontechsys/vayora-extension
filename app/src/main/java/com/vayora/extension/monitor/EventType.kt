package com.vayora.extension.monitor

object EventType {
    const val UNLOCK = "Screen unlocked"
    const val FAILED_AUTH = "Failed unlock attempt"
    const val BOOT = "Device restarted"
    const val ADMIN_ON = "Failed-attempt detection enabled"
    const val ADMIN_OFF = "Failed-attempt detection disabled"
}
