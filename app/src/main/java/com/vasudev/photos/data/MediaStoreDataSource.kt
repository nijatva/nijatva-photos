package com.vasudev.photos.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads all photos and videos from the device MediaStore.
 * Zoho Photos is LOCAL-FIRST: media is never copied, moved or uploaded —
 * it is read in place, exactly like Google Photos does.
 */
class MediaStoreDataSource(private val context: Context) {

    suspend fun loadMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val photos = query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false)
        val videos = query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
        (photos + videos).sortedByDescending { it.dateTakenMillis }
    }

    private fun query(collection: Uri, isVideo: Boolean): List<MediaItem> {
        val useRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )
        if (isVideo) projection.add(MediaStore.Video.Media.DURATION)
        // RELATIVE_PATH only exists on Android 10+; on older devices derive it from DATA.
        projection.add(
            if (useRelativePath) MediaStore.MediaColumns.RELATIVE_PATH
            else MediaStore.MediaColumns.DATA
        )

        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(collection, projection.toTypedArray(), null, null, null)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                val durationCol =
                    if (isVideo) cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION) else -1
                val pathCol = cursor.getColumnIndexOrThrow(projection.last())

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawPath = cursor.getString(pathCol)
                    val relativePath = when {
                        useRelativePath -> rawPath
                        rawPath != null -> rawPath.substringBeforeLast('/')
                            .removePrefix("/storage/emulated/0/") + "/"
                        else -> null
                    }
                    items += MediaItem(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                        displayName = cursor.getString(nameCol) ?: "Unknown",
                        dateTakenMillis = cursor.getLong(dateCol) * 1000L,
                        mimeType = cursor.getString(mimeCol)
                            ?: (if (isVideo) "video/mp4" else "image/jpeg"),
                        sizeBytes = cursor.getLong(sizeCol),
                        bucketName = cursor.getString(bucketCol),
                        durationMs = if (isVideo && !cursor.isNull(durationCol))
                            cursor.getLong(durationCol) else null,
                        width = if (cursor.isNull(widthCol)) 0 else cursor.getInt(widthCol),
                        height = if (cursor.isNull(heightCol)) 0 else cursor.getInt(heightCol),
                        relativePath = relativePath
                    )
                }
            }
        return items
    }
}
