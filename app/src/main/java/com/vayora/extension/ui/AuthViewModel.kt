package com.vayora.extension.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vayora.extension.security.PasscodeManager
import com.vayora.extension.security.VerifyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Stage {
    SETUP, SETUP_CONFIRM, LOCKED, UNLOCKED, CHANGE_CURRENT, CHANGE_NEW, CHANGE_CONFIRM
}

data class AuthUi(
    val stage: Stage,
    val filled: Int = 0,
    val message: String? = null,
    val lockSeconds: Long = 0,
    val attemptsLeft: Int? = null,
    val busy: Boolean = false
)

class AuthViewModel(
    private val manager: PasscodeManager,
    private val onWrongPasscode: (Int) -> Unit = {}
) : ViewModel() {

    private val _ui = MutableStateFlow(
        AuthUi(if (manager.isSet()) Stage.LOCKED else Stage.SETUP)
    )
    val ui: StateFlow<AuthUi> = _ui.asStateFlow()

    private var entry = ""
    private var firstEntry = ""
    private var ticker: Job? = null
    private var generation = 0

    init {
        refreshLock()
    }

    fun onDigit(digit: Char) {
        val s = _ui.value
        if (s.busy || s.lockSeconds > 0 || s.stage == Stage.UNLOCKED || entry.length >= 6) return
        entry += digit
        _ui.update { it.copy(filled = entry.length, message = null) }
        if (entry.length == 6) submit()
    }

    fun onDelete() {
        val s = _ui.value
        if (s.busy || entry.isEmpty()) return
        entry = entry.dropLast(1)
        _ui.update { it.copy(filled = entry.length) }
    }

    fun startChange() {
        entry = ""
        firstEntry = ""
        generation++
        _ui.value = AuthUi(Stage.CHANGE_CURRENT)
        refreshLock()
    }

    fun cancelChange() {
        entry = ""
        firstEntry = ""
        generation++
        ticker?.cancel()
        _ui.value = AuthUi(Stage.UNLOCKED)
    }

    fun lock() {
        entry = ""
        firstEntry = ""
        generation++
        when (_ui.value.stage) {
            Stage.UNLOCKED,
            Stage.CHANGE_CURRENT,
            Stage.CHANGE_NEW,
            Stage.CHANGE_CONFIRM,
            Stage.LOCKED -> {
                _ui.value = AuthUi(Stage.LOCKED)
                refreshLock()
            }
            Stage.SETUP_CONFIRM,
            Stage.SETUP -> {
                _ui.value = AuthUi(Stage.SETUP)
            }
        }
    }

    private fun submit() {
        val code = entry
        when (_ui.value.stage) {
            Stage.SETUP -> acceptNew(code, Stage.SETUP_CONFIRM)
            Stage.SETUP_CONFIRM -> confirmNew(code, Stage.SETUP)
            Stage.CHANGE_NEW -> acceptNew(code, Stage.CHANGE_CONFIRM)
            Stage.CHANGE_CONFIRM -> confirmNew(code, Stage.CHANGE_NEW)
            Stage.LOCKED -> verify(code)
            Stage.CHANGE_CURRENT -> verify(code)
            Stage.UNLOCKED -> Unit
        }
    }

    private fun acceptNew(code: String, next: Stage) {
        entry = ""
        if (isWeak(code)) {
            _ui.update { it.copy(filled = 0, message = "Choose a less predictable passcode") }
            return
        }
        firstEntry = code
        _ui.update { it.copy(stage = next, filled = 0, message = null) }
    }

    private fun confirmNew(code: String, backStage: Stage) {
        entry = ""
        if (code != firstEntry) {
            firstEntry = ""
            _ui.update {
                it.copy(
                    stage = backStage,
                    filled = 0,
                    message = "Passcodes did not match. Start again."
                )
            }
            return
        }
        val gen = generation
        _ui.update { it.copy(busy = true, filled = 0) }
        viewModelScope.launch {
            withContext(Dispatchers.Default) { manager.setPasscode(code) }
            if (gen != generation) return@launch
            firstEntry = ""
            _ui.value = AuthUi(Stage.UNLOCKED)
        }
    }

    private fun verify(code: String) {
        val stage = _ui.value.stage
        val gen = generation
        _ui.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { manager.verify(code) }
            entry = ""
            if (gen != generation) return@launch
            when (result) {
                is VerifyResult.Success -> {
                    _ui.value = AuthUi(
                        if (stage == Stage.LOCKED) Stage.UNLOCKED else Stage.CHANGE_NEW
                    )
                }
                is VerifyResult.Wrong -> {
                    _ui.update {
                        it.copy(busy = false, filled = 0, message = "Incorrect passcode")
                    }
                    refreshLock()
                    onWrongPasscode(result.failures)
                }
                is VerifyResult.Locked -> {
                    _ui.update { it.copy(busy = false, filled = 0, message = null) }
                    refreshLock()
                }
            }
        }
    }

    private fun refreshLock() {
        ticker?.cancel()
        if (manager.remainingLockMs() <= 0) {
            _ui.update { it.copy(lockSeconds = 0, attemptsLeft = attemptsLeft()) }
            return
        }
        ticker = viewModelScope.launch {
            while (true) {
                val remaining = manager.remainingLockMs()
                if (remaining <= 0) break
                _ui.update { it.copy(lockSeconds = (remaining + 999) / 1000) }
                delay(500)
            }
            _ui.update {
                it.copy(lockSeconds = 0, message = null, attemptsLeft = attemptsLeft())
            }
        }
    }

    private fun attemptsLeft(): Int? {
        val fails = manager.failureCount()
        return if (fails in 1..4) 5 - fails else null
    }

    private fun isWeak(code: String): Boolean {
        if (code.all { it == code[0] }) return true
        if ("0123456789".contains(code) || "9876543210".contains(code)) return true
        return code in setOf("123123", "121212", "112233", "696969", "159753")
    }

    companion object {
        fun factory(
            manager: PasscodeManager,
            onWrongPasscode: (Int) -> Unit = {}
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AuthViewModel(manager, onWrongPasscode) }
            }
    }
}
