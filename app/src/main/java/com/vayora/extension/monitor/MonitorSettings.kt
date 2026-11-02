package com.vayora.extension.monitor

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MonitorConfig(
    val enabled: Boolean = false,
    val recordUnlocks: Boolean = true
)

class MonitorSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("vayora_monitor", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        MonitorConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            recordUnlocks = prefs.getBoolean(KEY_UNLOCKS, true)
        )
    )
    val state: StateFlow<MonitorConfig> = _state.asStateFlow()

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, value).commit()
        _state.value = _state.value.copy(enabled = value)
    }

    fun setRecordUnlocks(value: Boolean) {
        prefs.edit().putBoolean(KEY_UNLOCKS, value).commit()
        _state.value = _state.value.copy(recordUnlocks = value)
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_UNLOCKS = "record_unlocks"
    }
}
