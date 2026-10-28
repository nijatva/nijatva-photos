package com.vasudev.photos.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1_000_000f)
    bytes >= 1_000L -> "%d KB".format(bytes / 1_000)
    else -> "$bytes B"
}

fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm"))
