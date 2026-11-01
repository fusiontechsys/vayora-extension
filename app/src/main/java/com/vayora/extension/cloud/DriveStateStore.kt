package com.vayora.extension.cloud

import android.content.Context
import com.vayora.extension.security.KeystoreCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DriveState(
    val email: String? = null,
    val folderId: String? = null,
    val lastSync: Long? = null,
    val lastError: String? = null,
    val syncing: Boolean = false
)

class DriveStateStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("vayora_drive", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<DriveState> = _state.asStateFlow()

    private fun load(): DriveState {
        val email = prefs.getString(KEY_EMAIL, null)
            ?.let { KeystoreCrypto.decrypt(it) }
            ?.toString(Charsets.UTF_8)
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        return DriveState(
            email = email,
            folderId = prefs.getString(KEY_FOLDER, null),
            lastSync = if (lastSync > 0L) lastSync else null,
            lastError = prefs.getString(KEY_ERROR, null)
        )
    }

    fun register(email: String) {
        prefs.edit()
            .putString(KEY_EMAIL, KeystoreCrypto.encrypt(email.toByteArray(Charsets.UTF_8)))
            .commit()
        _state.update { it.copy(email = email, lastError = null) }
    }

    fun setFolder(id: String) {
        prefs.edit().putString(KEY_FOLDER, id).commit()
        _state.update { it.copy(folderId = id) }
    }

    fun markSynced(time: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, time).remove(KEY_ERROR).commit()
        _state.update { it.copy(lastSync = time, lastError = null) }
    }

    fun setError(message: String?) {
        val editor = prefs.edit()
        if (message == null) editor.remove(KEY_ERROR) else editor.putString(KEY_ERROR, message)
        editor.commit()
        _state.update { it.copy(lastError = message) }
    }

    fun setSyncing(value: Boolean) {
        _state.update { it.copy(syncing = value) }
    }

    fun clear() {
        prefs.edit().clear().commit()
        _state.value = DriveState()
    }

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_FOLDER = "folder"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_ERROR = "error"
    }
}
