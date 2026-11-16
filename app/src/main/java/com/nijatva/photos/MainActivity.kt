package com.nijatva.photos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nijatva.photos.ui.ZohoPhotosRoot
import com.nijatva.photos.ui.theme.ZohoPhotosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaId = mediaIdFrom(intent)
        setContent {
            ZohoPhotosTheme {
                ZohoPhotosRoot(initialMediaId = mediaId)
            }
        }
    }

    /** MediaStore id from an incoming ACTION_VIEW uri (Nijatva Gallery / Camera hand-off). */
    private fun mediaIdFrom(intent: Intent?): Long? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        return intent.data?.lastPathSegment?.toLongOrNull()
    }
}
