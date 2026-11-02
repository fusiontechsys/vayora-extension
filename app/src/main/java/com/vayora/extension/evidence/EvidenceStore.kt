package com.vayora.extension.evidence

import android.content.Context
import com.vayora.extension.security.KeystoreCrypto
import java.io.File

/** Evidence photos are encrypted with the Android Keystore key before touching storage. */
object EvidenceStore {

    fun saveEncrypted(context: Context, jpeg: ByteArray, eventId: Long, time: Long): File {
        val dir = File(context.filesDir, "evidence").apply { mkdirs() }
        val file = File(dir, "evidence_${eventId}_$time.enc")
        file.writeBytes(KeystoreCrypto.encryptBytes(jpeg))
        return file
    }

    /** Returns the decrypted JPEG bytes, or null if the file is missing or unreadable. */
    fun read(file: File): ByteArray? {
        if (!file.exists()) return null
        val raw = file.readBytes()
        return if (file.name.endsWith(".enc")) KeystoreCrypto.decryptBytes(raw) else raw
    }
}
