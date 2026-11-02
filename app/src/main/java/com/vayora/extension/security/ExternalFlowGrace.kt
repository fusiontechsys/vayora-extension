package com.vayora.extension.security

import android.os.SystemClock

/**
 * Google's approval screens open on top of this app. Without a short grace window the
 * app would lock itself while they are showing and lose the result.
 */
object ExternalFlowGrace {
    @Volatile
    private var until = 0L

    fun begin(durationMs: Long = 120_000L) {
        until = SystemClock.elapsedRealtime() + durationMs
    }

    fun active(): Boolean = SystemClock.elapsedRealtime() < until

    fun end() {
        until = 0L
    }
}
