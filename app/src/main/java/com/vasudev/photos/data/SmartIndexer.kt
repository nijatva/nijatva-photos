package com.vasudev.photos.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.vasudev.photos.data.db.AppDatabase
import com.vasudev.photos.data.db.MediaIndexEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device intelligence, fully offline (ML Kit bundled models):
 *  - image labels  -> Smart categories (Food, Plant, Building...)
 *  - face counts   -> People (photos that contain faces)
 *
 * Nothing is uploaded; results stay in the local Room database.
 */
class SmartIndexer(
    private val context: Context,
    private val db: AppDatabase
) {

    /** Bridges a gms [Task] into a coroutine. */
    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resumeWithException(it) }
        }

    private fun decodeSmall(uri: Uri, maxDim: Int = 640): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim ||
            bounds.outHeight / (sample * 2) >= maxDim
        ) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Scans every photo, storing labels and face counts.
     * Returns the number of photos successfully indexed.
     */
    suspend fun indexAll(
        media: List<MediaItem>,
        onProgress: (done: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val photos = media.filter { !it.isVideo }
        if (photos.isEmpty()) return@withContext 0

        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder().setConfidenceThreshold(0.7f).build()
        )
        val faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )

        val entities = mutableListOf<MediaIndexEntity>()
        var done = 0
        for (item in photos) {
            try {
                val bitmap = decodeSmall(Uri.parse(item.uri))
                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val labels = labeler.process(image).awaitTask()
                        .mapNotNull { it.text.trim().lowercase() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val faceCount = faceDetector.process(image).awaitTask().size
                    entities += MediaIndexEntity(
                        mediaId = item.id,
                        labels = labels.joinToString(","),
                        faceCount = faceCount
                    )
                }
            } catch (e: Exception) {
                // Skip photos that fail to index; the rest continue.
            }
            done++
            onProgress(done, photos.size)
        }

        runCatching { labeler.close() }
        runCatching { faceDetector.close() }
        db.mediaIndexDao().upsertAll(entities)
        entities.size
    }

    /** label (lowercase) -> set of mediaIds carrying it. */
    suspend fun labelIndex(): Map<String, Set<Long>> = withContext(Dispatchers.IO) {
        db.mediaIndexDao().all().flatMap { entity ->
            entity.labels.split(",").filter { it.isNotBlank() }.map { it.trim() to entity.mediaId }
        }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
    }

    suspend fun faceIds(): Set<Long> = withContext(Dispatchers.IO) {
        db.mediaIndexDao().all().filter { it.faceCount > 0 }.map { it.mediaId }.toSet()
    }

    suspend fun idsForLabel(label: String): Set<Long> = withContext(Dispatchers.IO) {
        db.mediaIndexDao().all()
            .filter { entity -> entity.labels.split(",").map { it.trim() }.contains(label) }
            .map { it.mediaId }
            .toSet()
    }

    suspend fun indexedCount(): Int = withContext(Dispatchers.IO) {
        db.mediaIndexDao().count()
    }
}
