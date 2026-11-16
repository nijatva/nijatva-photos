package com.nijatva.photos

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.nijatva.photos.data.AppContainer

class ZohoPhotosApplication : Application(), coil.ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /**
     * Single shared image loader for the whole app. VideoFrameDecoder lets
     * AsyncImage render video thumbnails straight from MediaStore URIs.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}

fun Context.findApp(): ZohoPhotosApplication = applicationContext as ZohoPhotosApplication
