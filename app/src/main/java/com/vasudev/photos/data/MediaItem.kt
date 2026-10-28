package com.vasudev.photos.data

data class MediaItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val dateTakenMillis: Long,
    val mimeType: String,
    val sizeBytes: Long,
    val bucketName: String?,
    val durationMs: Long?,
    val width: Int = 0,
    val height: Int = 0,
    val relativePath: String? = null
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

/** A "On this day" memory group shown at the top of the Photos tab. */
data class Memory(
    val yearsAgo: Int,
    val title: String,
    val items: List<MediaItem>
)
