package com.vayora.extension.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vayora.extension.VayoraApp
import com.vayora.extension.cloud.DriveClient
import com.vayora.extension.security.VerifyResult
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RecoveryStep { GOOGLE, PHRASE }

data class RecoveryUi(
    val step: RecoveryStep = RecoveryStep.GOOGLE,
    val busy: Boolean = false,
    val message: String? = null,
    val lockSeconds: Long = 0,
    val email: String? = null
)

class RecoveryViewModel(private val app: VayoraApp) : ViewModel() {

    private val manager = app.recoveryManager
    private val store = app.driveState

    val hasPhrase: StateFlow<Boolean> = manager.hasPhrase

    val driveEmail: StateFlow<String?> = store.state
        .map { it.email }
        .stateIn(viewModelScope, SharingStarted.Eagerly, store.state.value.email)

    private val _ui = MutableStateFlow(RecoveryUi(email = store.state.value.email))
    val ui: StateFlow<RecoveryUi> = _ui.asStateFlow()

    private var ticker: Job? = null

    fun reset() {
        ticker?.cancel()
        _ui.value = RecoveryUi(email = store.state.value.email)
    }

    /** Explains why recovery cannot work, or null when it is ready. */
    fun unavailableReason(): String? {
        if (store.state.value.email == null) {
            return "No Google account is registered in this app, so recovery cannot be " +
                "verified. Reinstalling the app is the only option. Evidence already " +
                "uploaded stays in your Google Drive."
        }
        if (!manager.hasPhrase.value) {
            return "No recovery phrase was set up before the passcode was forgotten. " +
                "Reinstalling the app is the only option. Evidence already uploaded " +
                "stays in your Google Drive."
        }
        return null
    }

    fun report(text: String) {
        _ui.update { it.copy(busy = false, message = text) }
    }

    fun onGoogleToken(token: String) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, message = null) }
            try {
                val email = withContext(Dispatchers.IO) { DriveClient(token).aboutEmail() }
                val registered = store.state.value.email
                if (registered != null && registered.equals(email, ignoreCase = true)) {
                    _ui.update { it.copy(step = RecoveryStep.PHRASE, busy = false) }
                } else {
                    _ui.update {
                        it.copy(
                            busy = false,
                            message = "That Google account is not the one registered in this app."
                        )
                    }
                }
            } catch (e: IOException) {
                _ui.update {
                    it.copy(busy = false, message = "Could not reach Google. Check your connection.")
                }
            } catch (e: Exception) {
                _ui.update { it.copy(busy = false, message = "Verification failed.") }
            }
        }
    }

    fun verifyPhrase(phrase: String, onVerified: () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, message = null) }
            val result = withContext(Dispatchers.Default) { manager.verify(phrase) }
            when (result) {
                is VerifyResult.Success -> {
                    _ui.update { it.copy(busy = false) }
                    onVerified()
                }
                is VerifyResult.Wrong -> {
                    _ui.update { it.copy(busy = false, message = "Incorrect recovery phrase") }
                    startTicker()
                }
                is VerifyResult.Locked -> {
                    _ui.update { it.copy(busy = false) }
                    startTicker()
                }
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                val remaining = manager.remainingLockMs()
                if (remaining <= 0) break
                _ui.update { it.copy(lockSeconds = (remaining + 999) / 1000) }
                delay(500)
            }
            _ui.update { it.copy(lockSeconds = 0) }
        }
    }

    /** Sets or changes the recovery phrase. Returns an error text, or null on success. */
    suspend fun changePhrase(current: String, newPhrase: String, confirm: String): String? {
        val trimmed = newPhrase.trim()
        if (trimmed.length < 8) return "Use at least 8 characters."
        if (trimmed != confirm.trim()) return "The two phrases do not match."
        if (trimmed.toSet().size < 4) return "Choose a less repetitive phrase."
        if (manager.hasPhrase.value) {
            val result = withContext(Dispatchers.Default) { manager.verify(current) }
            if (result is VerifyResult.Locked) return "Too many wrong attempts. Try again later."
            if (result !is VerifyResult.Success) return "The current phrase is incorrect."
        }
        withContext(Dispatchers.Default) { manager.setPhrase(trimmed) }
        return null
    }

    companion object {
        fun factory(app: VayoraApp): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { RecoveryViewModel(app) }
            }
    }
}
