package com.nijatva.photos.ui

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.nijatva.photos.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

/**
 * Heavyweight media actions used by the photo viewer:
 * rename, PDF conversion, wallpaper and contact avatar.
 */

sealed interface DeleteResult {
    data object Success : DeleteResult
    data class NeedsPermission(val sender: IntentSender) : DeleteResult
    data class Error(val message: String) : DeleteResult
}

sealed interface RenameResult {
    data object Success : RenameResult
    data class NeedsPermission(val sender: IntentSender) : RenameResult
    data class Error(val message: String) : RenameResult
}

/** Reads a downscaled bitmap from a content URI, or null on failure. */
private suspend fun loadScaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
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
    }

/** Converts an image to a single-page A4 PDF saved into Downloads. Returns the file name. */
suspend fun convertToPdf(context: Context, source: Uri, displayName: String): String? =
    withContext(Dispatchers.IO) {
        val bitmap = loadScaledBitmap(context, source, 1600) ?: return@withContext null
        val document = PdfDocument()
        try {
            // A4 at 72 dpi
            val pageWidth = 595
            val pageHeight = 842
            val margin = 24f
            val scale = minOf(
                (pageWidth - 2 * margin) / bitmap.width,
                (pageHeight - 2 * margin) / bitmap.height
            )
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale
            val left = (pageWidth - drawWidth) / 2f
            val top = (pageHeight - drawHeight) / 2f

            val page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            )
            page.canvas.drawBitmap(
                bitmap, null,
                RectF(left, top, left + drawWidth, top + drawHeight), null
            )
            document.finishPage(page)

            val fileName = displayName.substringBeforeLast('.') + ".pdf"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val outUri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: return@withContext null
                context.contentResolver.openOutputStream(outUri)?.use { document.writeTo(it) }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                FileOutputStream(File(dir, fileName)).use { document.writeTo(it) }
            }
            fileName
        } catch (e: Exception) {
            null
        } finally {
            document.close()
        }
    }

/** Sets an image as the device wallpaper. */
suspend fun setWallpaperFrom(context: Context, source: Uri): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val bitmap = loadScaledBitmap(context, source, 1440) ?: return@withContext false
            val manager = android.app.WallpaperManager.getInstance(context)
            runCatching { manager.setBitmap(bitmap) }.isSuccess
        } catch (e: Exception) {
            false
        }
    }

/** Writes an image as the photo of the picked contact. */
suspend fun setContactAvatar(context: Context, source: Uri, contactUri: Uri): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val contactId = ContentUris.parseId(contactUri)
            val rawId = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID}=?",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            } ?: return@withContext false

            val photo = loadScaledBitmap(context, source, 720) ?: return@withContext false
            val bytes = ByteArrayOutputStream().also {
                photo.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }.toByteArray()

            val ops = arrayListOf(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND " +
                            "${ContactsContract.Data.MIMETYPE}=?",
                        arrayOf(
                            rawId.toString(),
                            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                        )
                    )
                    .build(),
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes)
                    .build()
            )
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            false
        }
    }

/** Permanently deletes a media item from the MediaStore (with consent flow). */
suspend fun deleteMediaForever(context: Context, item: MediaItem): DeleteResult =
    withContext(Dispatchers.IO) {
        try {
            val deleted = context.contentResolver.delete(Uri.parse(item.uri), null, null)
            if (deleted > 0) DeleteResult.Success
            else DeleteResult.Error("Could not delete this file")
        } catch (e: Exception) {
            val sender = recoverableSender(e)
            if (sender != null) DeleteResult.NeedsPermission(sender)
            else DeleteResult.Error(e.message ?: "Could not delete this file")
        }
    }

/**
 * Copies an image into app cache WITHOUT EXIF metadata (decode/re-encode drops
 * all tags), so the shared file carries no location or camera information.
 */
suspend fun prepareScrubbedShare(context: Context, item: MediaItem): File? =
    withContext(Dispatchers.IO) {
        try {
            val bitmap = loadScaledBitmap(context, Uri.parse(item.uri), 4096)
                ?: return@withContext null
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val safe = item.displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(
                dir,
                "shared_" + System.currentTimeMillis() + "_" +
                    safe.substringBeforeLast('.') + ".jpg"
            )
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            file
        } catch (e: Exception) {
            null
        }
    }

private const val GOOGLE_PHOTOS_PACKAGE = "com.google.android.apps.photos"

/**
 * Opens the Google Photos app so the user can manage its Backup & sync.
 * Falls back to the Play Store page when the app is not installed.
 */
fun openGooglePhotosApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(GOOGLE_PHOTOS_PACKAGE)
    if (launch != null) {
        context.startActivity(launch)
        return
    }
    try {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$GOOGLE_PHOTOS_PACKAGE")
            )
        )
    } catch (e: Exception) {
        Toast.makeText(context, "Google Photos app not found", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Hands one photo/video to the Google Photos app for upload.
 * No Google account access is needed by Nijatva Photos — the upload
 * happens inside Google's own app.
 */
fun sendToGooglePhotos(context: Context, item: MediaItem) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage(GOOGLE_PHOTOS_PACKAGE)
    }
    try {
        context.startActivity(Intent.createChooser(send, "Upload to Google Photos"))
    } catch (e: Exception) {
        Toast.makeText(
            context, "Google Photos app not found on this device", Toast.LENGTH_SHORT
        ).show()
    }
}

/** Reads GPS coordinates from an image's EXIF, or null if absent. */
suspend fun readExifLocation(context: Context, item: MediaItem): String? =
    withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext null
        try {
            context.contentResolver.openInputStream(Uri.parse(item.uri))?.use { input ->
                val exif = ExifInterface(input)
                exif.latLong?.let { latLong -> "%.4f, %.4f".format(latLong[0], latLong[1]) }
            }
        } catch (e: Exception) {
            null
        }
    }

/**
 * Android Q+ throws RecoverableSecurityException when an app renames media it does
 * not own; the user can then approve a one-time write via a system dialog.
 * It is read via reflection so the code builds against any platform jar.
 */
private fun recoverableSender(e: Exception): IntentSender? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    if (e.javaClass.name != "android.os.RecoverableSecurityException") return null
    return try {
        val userAction = e.javaClass.getMethod("getUserAction").invoke(e)
        userAction.javaClass.getMethod("getActionIntent").invoke(userAction) as? IntentSender
    } catch (reflect: Exception) {
        null
    }
}

/** Renames a media item in the MediaStore, surfacing consent flows where required. */
suspend fun renameMedia(context: Context, item: MediaItem, newName: String): RenameResult {
    val extension = item.displayName.substringAfterLast('.', "").ifBlank { "jpg" }
    val finalName = if (newName.contains('.')) newName else "$newName.$extension"
    return withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
        }
        try {
            val updated = context.contentResolver.update(Uri.parse(item.uri), values, null, null)
            if (updated > 0) RenameResult.Success
            else RenameResult.Error("Could not rename this file")
        } catch (e: Exception) {
            val sender = recoverableSender(e)
            if (sender != null) {
                RenameResult.NeedsPermission(sender)
            } else {
                RenameResult.Error(e.message ?: "Could not rename this file")
            }
        }
    }
}
