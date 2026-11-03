package com.vayora.extension.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("vayora_theme", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(load())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private fun load(): ThemeMode {
        val saved = prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(saved)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setMode(value: ThemeMode) {
        prefs.edit().putString(KEY_MODE, value.name).commit()
        _mode.value = value
    }

    companion object {
        private const val KEY_MODE = "mode"
    }
}
