package com.vayora.extension.security

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stores the recovery phrase as a salted hash, never in plain text. */
class RecoveryManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("vayora_recovery", Context.MODE_PRIVATE)

    private val _hasPhrase = MutableStateFlow(prefs.contains(KEY_BLOB))
    val hasPhrase: StateFlow<Boolean> = _hasPhrase.asStateFlow()

    private data class LockState(
        val fails: Int,
        val startElapsed: Long,
        val startWall: Long,
        val durationMs: Long
    )

    fun setPhrase(phrase: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hashed = hash(normalize(phrase), salt)
        val blob = Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(hashed, Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_BLOB, KeystoreCrypto.encrypt(blob.toByteArray(Charsets.UTF_8)))
            .commit()
        writeLock(LockState(0, 0, 0, 0))
        _hasPhrase.value = true
    }

    fun remainingLockMs(): Long {
        val state = readLock()
        if (state.durationMs <= 0) return 0
        val elapsedNow = SystemClock.elapsedRealtime()
        val passed = if (elapsedNow >= state.startElapsed) {
            elapsedNow - state.startElapsed
        } else {
            System.currentTimeMillis() - state.startWall
        }
        return (state.durationMs - passed).coerceIn(0, state.durationMs)
    }

    fun verify(phrase: String): VerifyResult {
        val remaining = remainingLockMs()
        if (remaining > 0) return VerifyResult.Locked(remaining)
        if (matches(normalize(phrase))) {
            writeLock(LockState(0, 0, 0, 0))
            return VerifyResult.Success
        }
        val fails = readLock().fails + 1
        val duration = durationFor(fails)
        writeLock(
            LockState(
                fails,
                SystemClock.elapsedRealtime(),
                System.currentTimeMillis(),
                duration
            )
        )
        return VerifyResult.Wrong(fails, duration)
    }

    private fun normalize(phrase: String): String = phrase.trim()

    private fun durationFor(fails: Int): Long = when {
        fails < 3 -> 0L
        fails == 3 -> 60_000L
        fails == 4 -> 300_000L
        fails == 5 -> 900_000L
        fails == 6 -> 3_600_000L
        else -> 86_400_000L
    }

    private fun matches(phrase: String): Boolean {
        val raw = prefs.getString(KEY_BLOB, null) ?: return false
        val text = KeystoreCrypto.decrypt(raw)?.toString(Charsets.UTF_8) ?: return false
        val parts = text.split(":")
        if (parts.size != 2) return false
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expected = Base64.decode(parts[1], Base64.NO_WRAP)
        return MessageDigest.isEqual(expected, hash(phrase, salt))
    }

    private fun hash(phrase: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(phrase.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    private fun readLock(): LockState {
        val raw = prefs.getString(KEY_LOCK, null)
            ?.let { KeystoreCrypto.decrypt(it) }
            ?.toString(Charsets.UTF_8)
        val parts = raw?.split("|")
        if (parts == null || parts.size != 4) return LockState(0, 0, 0, 0)
        return LockState(
            parts[0].toIntOrNull() ?: 0,
            parts[1].toLongOrNull() ?: 0L,
            parts[2].toLongOrNull() ?: 0L,
            parts[3].toLongOrNull() ?: 0L
        )
    }

    private fun writeLock(state: LockState) {
        val text = "${state.fails}|${state.startElapsed}|${state.startWall}|${state.durationMs}"
        prefs.edit()
            .putString(KEY_LOCK, KeystoreCrypto.encrypt(text.toByteArray(Charsets.UTF_8)))
            .commit()
    }

    companion object {
        private const val KEY_BLOB = "rp_blob"
        private const val KEY_LOCK = "rp_lock"
        private const val ITERATIONS = 150_000
    }
}
