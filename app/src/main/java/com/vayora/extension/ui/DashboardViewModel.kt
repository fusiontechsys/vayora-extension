package com.vayora.extension.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vayora.extension.cloud.DriveStateStore
import com.vayora.extension.data.SecurityEvent
import com.vayora.extension.data.SecurityRepository
import com.vayora.extension.security.RecoveryManager
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
    val lastSync: Long? = null,
    val recoveryReady: Boolean = false
)

class DashboardViewModel(
    repository: SecurityRepository,
    driveStore: DriveStateStore,
    recovery: RecoveryManager
) : ViewModel() {

    private val base = combine(
        repository.lastEvent,
        repository.eventCount,
        repository.photoCount,
        repository.pendingCount,
        driveStore.state
    ) { last, events, photos, pending, drive ->
        val status = if (drive.email == null) {
            "Not connected"
        } else if (drive.lastError != null) {
            "Needs attention"
        } else {
            "Connected"
        }
        DashboardState(
            lastEvent = last,
            eventCount = events,
            photoCount = photos,
            pendingUploads = pending,
            driveStatus = status,
            lastSync = drive.lastSync
        )
    }

    val state: StateFlow<DashboardState> = combine(base, recovery.hasPhrase, driveStore.state) {
        dashboard, hasPhrase, drive ->
        dashboard.copy(recoveryReady = hasPhrase && drive.email != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    companion object {
        fun factory(
            repository: SecurityRepository,
            driveStore: DriveStateStore,
            recovery: RecoveryManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { DashboardViewModel(repository, driveStore, recovery) }
            }
    }
}
