package com.vayora.extension.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vayora.extension.data.SecurityEvent
import com.vayora.extension.data.SecurityRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val lastEvent: SecurityEvent? = null,
    val eventCount: Int = 0,
    val photoCount: Int = 0,
    val pendingUploads: Int = 0,
    val driveStatus: String = "Not connected",
    val lastSync: Long? = null
)

class DashboardViewModel(repository: SecurityRepository) : ViewModel() {

    val state: StateFlow<DashboardState> = combine(
        repository.lastEvent,
        repository.eventCount,
        repository.photoCount,
        repository.pendingCount
    ) { last, events, photos, pending ->
        DashboardState(
            lastEvent = last,
            eventCount = events,
            photoCount = photos,
            pendingUploads = pending
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    companion object {
        fun factory(repository: SecurityRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { DashboardViewModel(repository) }
            }
    }
}
