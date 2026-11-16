package com.nijatva.photos.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Hand-offs between the three Nijatva apps — Photos (this app), Gallery and Camera.
 *
 * Each call prefers the matching Nijatva app and falls back to whatever the system
 * offers, so the actions still work when one of them isn't installed.
 */
object NijatvaApps {

    const val GALLERY = "com.nijatva.gallery"
    const val CAMERA = "com.nijatva.camera"

    fun isInstalled(context: Context, pkg: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(pkg) != null

    /**
     * Opens Nijatva Camera, falling back to the system still-image camera.
     * Returns true when something was launched.
     */
    fun openCamera(context: Context): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(CAMERA)
        if (launch != null) {
            return try {
                context.startActivity(launch)
                true
            } catch (e: ActivityNotFoundException) {
                false
            }
        }
        return try {
            context.startActivity(Intent("android.media.action.STILL_IMAGE_CAMERA"))
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Hands the photo to Nijatva Gallery, falling back to any app that can view images.
     * Returns true when something was launched.
     */
    fun openInGallery(context: Context, uri: String, mimeType: String?): Boolean {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(uri), mimeType ?: "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // 1) Nijatva Gallery, explicitly
        try {
            context.startActivity(Intent(view).setPackage(GALLERY))
            return true
        } catch (e: ActivityNotFoundException) {
            // not installed — fall through
        }

        // 2) anything that can show an image
        return try {
            context.startActivity(Intent.createChooser(view, "Open in"))
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No gallery app found", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
