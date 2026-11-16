package com.nijatva.photos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nijatva.photos.ui.ZohoPhotosRoot
import com.nijatva.photos.ui.theme.ZohoPhotosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZohoPhotosTheme {
                ZohoPhotosRoot()
            }
        }
    }
}
