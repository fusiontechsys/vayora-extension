package com.vayora.extension.evidence

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EvidenceConfig(
    val captureOnAppFail: Boolean = true,
    val captureOnSystemFail: Boolean = false
)

class EvidenceSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("vayora_evidence", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        EvidenceConfig(
            captureOnAppFail = prefs.getBoolean(KEY_APP, true),
            captureOnSystemFail = prefs.getBoolean(KEY_SYSTEM, false)
        )
    )
    val state: StateFlow<EvidenceConfig> = _state.asStateFlow()

    fun setCaptureOnAppFail(value: Boolean) {
        prefs.edit().putBoolean(KEY_APP, value).commit()
        _state.value = _state.value.copy(captureOnAppFail = value)
    }

    fun setCaptureOnSystemFail(value: Boolean) {
        prefs.edit().putBoolean(KEY_SYSTEM, value).commit()
        _state.value = _state.value.copy(captureOnSystemFail = value)
    }

    companion object {
        private const val KEY_APP = "capture_on_app_fail"
        private const val KEY_SYSTEM = "capture_on_system_fail"
    }
}
