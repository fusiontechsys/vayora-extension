package com.vayora.extension.security

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface VerifyResult {
    object Success : VerifyResult
    data class Wrong(val failures: Int, val lockMs: Long) : VerifyResult
    data class Locked(val remainingMs: Long) : VerifyResult
}

class PasscodeManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("vayora_auth", Context.MODE_PRIVATE)

    private data class LockState(
        val fails: Int,
        val startElapsed: Long,
        val startWall: Long,
        val durationMs: Long
    )

    fun isSet(): Boolean = prefs.contains(KEY_BLOB)

    fun setPasscode(code: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hashed = hash(code, salt)
        val blob = Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(hashed, Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_BLOB, KeystoreCrypto.encrypt(blob.toByteArray(Charsets.UTF_8)))
            .commit()
        writeLock(LockState(0, 0, 0, 0))
    }

    fun failureCount(): Int = readLock().fails

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

    fun verify(code: String): VerifyResult {
        val remaining = remainingLockMs()
        if (remaining > 0) return VerifyResult.Locked(remaining)
        if (matches(code)) {
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

    private fun durationFor(fails: Int): Long = when {
        fails < 5 -> 0L
        fails == 5 -> 30_000L
        fails == 6 -> 60_000L
        fails == 7 -> 300_000L
        fails == 8 -> 900_000L
        fails == 9 -> 3_600_000L
        else -> 86_400_000L
    }

    private fun matches(code: String): Boolean {
        val raw = prefs.getString(KEY_BLOB, null) ?: return false
        val text = KeystoreCrypto.decrypt(raw)?.toString(Charsets.UTF_8) ?: return false
        val parts = text.split(":")
        if (parts.size != 2) return false
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expected = Base64.decode(parts[1], Base64.NO_WRAP)
        return MessageDigest.isEqual(expected, hash(code, salt))
    }

    private fun hash(code: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(code.toCharArray(), salt, ITERATIONS, 256)
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
        private const val KEY_BLOB = "pc_blob"
        private const val KEY_LOCK = "pc_lock"
        private const val ITERATIONS = 120_000
    }
}
