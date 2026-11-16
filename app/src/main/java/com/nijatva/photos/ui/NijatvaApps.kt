package com.nijatva.photos.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Hand-offs to the other Nijatva apps.
 *
 * Every call here is a **soft** dependency: we try the matching Nijatva app first, then
 * fall back to whatever the system offers, and finally to a plain message. Nothing can
 * crash this app, and no feature is blocked when a sibling app isn't installed — the user
 * simply gets the system chooser (or a short toast) instead.
 *
 * Package ids of the family:
 *   Photos        com.nijatva.photos      (this app)
 *   Gallery       com.nijatva.gallery
 *   Camera        com.nijatva.camera
 *   Photo Editor  com.nijatva.photo.editor
 *   Video Player  com.nijatva.videoplayer
 *   Music Player  com.nijatva.music.player
 *   File Manager  com.nijatva.file
 *   Browser       in.nijatva.browser
 */
object NijatvaApps {

    const val PHOTOS = "com.nijatva.photos"
    const val GALLERY = "com.nijatva.gallery"
    const val CAMERA = "com.nijatva.camera"
    const val PHOTO_EDITOR = "com.nijatva.photo.editor"
    const val VIDEO_PLAYER = "com.nijatva.videoplayer"
    const val MUSIC_PLAYER = "com.nijatva.music.player"
    const val FILE_MANAGER = "com.nijatva.file"
    const val BROWSER = "in.nijatva.browser"

    fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getLaunchIntentForPackage(pkg) != null
    } catch (e: Exception) {
        false
    }

    /* ---------------- internals ---------------- */

    private fun startOn(context: Context, intent: Intent, pkg: String): Boolean = try {
        context.startActivity(Intent(intent).setPackage(pkg))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: Exception) {
        false
    }

    private fun startAny(context: Context, intent: Intent, title: String): Boolean = try {
        context.startActivity(Intent.createChooser(intent, title))
        true
    } catch (e: Exception) {
        false
    }

    private fun viewOf(uri: String, mime: String) = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(uri), mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun nothingFound(context: Context, what: String): Boolean {
        Toast.makeText(context, "No app found to $what", Toast.LENGTH_SHORT).show()
        return false
    }

    /* ---------------- camera ---------------- */

    /** Nijatva Camera if installed, otherwise any still-image camera. */
    fun openCamera(context: Context): Boolean {
        context.packageManager.getLaunchIntentForPackage(CAMERA)?.let {
            if (startOn(context, it, CAMERA)) return true
        }
        return try {
            context.startActivity(Intent("android.media.action.STILL_IMAGE_CAMERA"))
            true
        } catch (e: Exception) {
            nothingFound(context, "open the camera")
        }
    }

    /* ---------------- viewing / editing ---------------- */

    /** Opens the item in Nijatva Gallery, else any gallery, else any viewer. */
    fun openInGallery(context: Context, uri: String, mime: String?): Boolean {
        val intent = viewOf(uri, mime ?: "image/*")
        if (startOn(context, intent, GALLERY)) return true
        return startAny(context, intent, "Open in") || nothingFound(context, "open this item")
    }

    /** ACTION_EDIT in Nijatva Photo Editor, else any editor. Returns false if none. */
    fun editInPhotoEditor(context: Context, uri: String, mime: String?): Boolean {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(Uri.parse(uri), mime ?: "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (startOn(context, intent, PHOTO_EDITOR)) return true
        // no editor at all → caller decides (e.g. use the in-app editor)
        return context.packageManager.resolveActivity(intent, 0) != null &&
            startAny(context, intent, "Edit with")
    }

    /** Plays a video in Nijatva Video Player, else any player. */
    fun playVideo(context: Context, uri: String, mime: String?): Boolean {
        val intent = viewOf(uri, mime ?: "video/*")
        if (startOn(context, intent, VIDEO_PLAYER)) return true
        return startAny(context, intent, "Play with") || nothingFound(context, "play this video")
    }

    /** Plays audio in Nijatva Music Player, else any player. */
    fun playAudio(context: Context, uri: String, mime: String?): Boolean {
        val intent = viewOf(uri, mime ?: "audio/*")
        if (startOn(context, intent, MUSIC_PLAYER)) return true
        return startAny(context, intent, "Play with") || nothingFound(context, "play this audio")
    }

    /* ---------------- other family apps ---------------- */

    /** Opens Nijatva File Manager (no deep link — it's a browsing app). */
    fun openFileManager(context: Context): Boolean {
        context.packageManager.getLaunchIntentForPackage(FILE_MANAGER)?.let {
            if (startOn(context, it, FILE_MANAGER)) return true
        }
        return nothingFound(context, "open the file manager")
    }

    /** Opens a URL in Nijatva Browser, else any browser. */
    fun openInBrowser(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (startOn(context, intent, BROWSER)) return true
        return startAny(context, intent, "Open with") || nothingFound(context, "open this link")
    }
}
