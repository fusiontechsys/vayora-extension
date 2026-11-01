package com.vayora.extension.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vayora.extension.VayoraApp
import com.vayora.extension.cloud.DriveClient
import com.vayora.extension.cloud.DriveException
import com.vayora.extension.cloud.DriveState
import com.vayora.extension.cloud.SyncScheduler
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CloudViewModel(private val app: VayoraApp) : ViewModel() {

    private val store = app.driveState
    private val repository = app.repository

    val drive: StateFlow<DriveState> = store.state
    val pending: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun report(text: String) {
        _message.value = text
    }

    fun clearMessage() {
        _message.value = null
    }

    fun onToken(token: String) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val client = DriveClient(token)
                val email = withContext(Dispatchers.IO) { client.aboutEmail() }
                val registered = store.state.value.email
                if (registered != null && !registered.equals(email, ignoreCase = true)) {
                    _message.value =
                        "$email is not the registered account. Use $registered or disconnect first."
                    return@launch
                }
                if (registered == null) {
                    store.register(email.lowercase())
                }
                val folderId = withContext(Dispatchers.IO) {
                    client.ensureFolder(store.state.value.folderId)
                }
                store.setFolder(folderId)
                store.setError(null)
                SyncScheduler.syncSoon(app)
                _message.value = "Connected as $email. Evidence folder is ready in Google Drive."
            } catch (e: DriveException) {
                _message.value = if (e.code == 403) {
                    "Google Drive refused access (403). Check that the Drive API is enabled " +
                        "for your Google Cloud project."
                } else {
                    "Google Drive error: ${e.message}"
                }
            } catch (e: IOException) {
                _message.value = "No connection to Google. Try again when online."
            } finally {
                _busy.value = false
            }
        }
    }

    fun syncNow() {
        SyncScheduler.syncSoon(app)
        _message.value = "Sync queued. It runs as soon as you are online."
    }

    fun recordTestEvent() {
        viewModelScope.launch {
            repository.recordEvent(
                type = "Test event",
                details = "Created manually from Cloud Evidence",
                deviceState = "App in foreground"
            )
            SyncScheduler.syncSoon(app)
            _message.value = "Test event recorded and queued for upload."
        }
    }

    fun disconnect() {
        store.clear()
        _message.value = "Google account disconnected from this app."
    }

    companion object {
        fun factory(app: VayoraApp): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { CloudViewModel(app) }
            }
    }
}
