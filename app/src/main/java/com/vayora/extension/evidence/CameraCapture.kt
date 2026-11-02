package com.vayora.extension.evidence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import com.vayora.extension.cloud.ImageUtil
import com.vayora.extension.cloud.SyncScheduler
import com.vayora.extension.data.EvidencePhoto
import com.vayora.extension.data.SecurityRepository
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class CaptureResult { SUCCESS, NO_PERMISSION, NO_CAMERA, FAILED }

object CameraCapture {

    private const val MAX_DIM = 1600

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun capture(
        context: Context,
        owner: LifecycleOwner,
        repository: SecurityRepository,
        eventId: Long
    ): CaptureResult {
        val appContext = context.applicationContext
        if (!hasPermission(appContext)) return CaptureResult.NO_PERMISSION

        val provider = try {
            awaitProvider(appContext)
        } catch (e: Exception) {
            return CaptureResult.FAILED
        }
        val selector = CameraSelector.DEFAULT_FRONT_CAMERA
        val hasFront = try {
            provider.hasCamera(selector)
        } catch (e: Exception) {
            false
        }
        if (!hasFront) return CaptureResult.NO_CAMERA

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()

        try {
            withContext(Dispatchers.Main) {
                provider.unbindAll()
                provider.bindToLifecycle(owner, selector, imageCapture)
            }
        } catch (e: Exception) {
            return CaptureResult.FAILED
        }
        delay(700)

        val dir = File(appContext.filesDir, "evidence").apply { mkdirs() }
        val now = System.currentTimeMillis()
        val file = File(dir, "evidence_${eventId}_$now.jpg")
        val saved = withTimeoutOrNull(10_000) { takePicture(appContext, imageCapture, file) } ?: false

        withContext(Dispatchers.Main) {
            try {
                provider.unbind(imageCapture)
            } catch (e: Exception) {
                // camera already released
            }
        }

        if (!saved) {
            file.delete()
            return CaptureResult.FAILED
        }
        withContext(Dispatchers.IO) { normalizeOrientation(file) }
        repository.addPhoto(
            EvidencePhoto(
                eventId = eventId,
                localPath = file.absolutePath,
                capturedAt = now
            )
        )
        SyncScheduler.syncSoon(appContext)
        return CaptureResult.SUCCESS
    }

    private suspend fun awaitProvider(context: Context): ProcessCameraProvider =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    try {
                        cont.resume(future.get())
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }

    private suspend fun takePicture(
        context: Context,
        capture: ImageCapture,
        file: File
    ): Boolean = suspendCancellableCoroutine { cont ->
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) cont.resume(false)
                }
            }
        )
    }

    /** Bakes the EXIF rotation into the pixels so every viewer shows the photo upright. */
    private fun normalizeOrientation(file: File) {
        try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val bitmap = ImageUtil.decodeFile(file.absolutePath, MAX_DIM) ?: return
            val upright = if (degrees != 0f) {
                val matrix = Matrix().apply { postRotate(degrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
            FileOutputStream(file).use { out ->
                upright.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
        } catch (e: Exception) {
            // keep the original file if it cannot be rewritten
        }
    }
}
