package com.vayora.extension.cloud

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.vayora.extension.evidence.EvidenceStore
import java.io.File

object ImageUtil {

    private fun sampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / (sample * 2) >= maxDim) {
            sample *= 2
        }
        return sample
    }

    fun decodeBytes(bytes: ByteArray, maxDim: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                null
            } else {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDim)
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /** Decodes a local evidence file. Encrypted .enc files are decrypted in memory only. */
    fun decodeFile(path: String, maxDim: Int): Bitmap? {
        val bytes = EvidenceStore.read(File(path)) ?: return null
        return decodeBytes(bytes, maxDim)
    }
}
