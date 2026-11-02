package com.vayora.extension.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vayora.extension.VayoraApp
import com.vayora.extension.data.SecurityEvent
import com.vayora.extension.monitor.MonitorConfig
import com.vayora.extension.monitor.MonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MonitorViewModel(private val app: VayoraApp) : ViewModel() {

    val settings: StateFlow<MonitorConfig> = app.monitorSettings.state

    val events: StateFlow<List<SecurityEvent>> = app.repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setMonitoring(enabled: Boolean) {
        app.monitorSettings.setEnabled(enabled)
        if (enabled) {
            MonitorService.start(app)
        } else {
            MonitorService.stop(app)
        }
    }

    fun setRecordUnlocks(value: Boolean) {
        app.monitorSettings.setRecordUnlocks(value)
    }

    companion object {
        fun factory(app: VayoraApp): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { MonitorViewModel(app) }
            }
    }
}
