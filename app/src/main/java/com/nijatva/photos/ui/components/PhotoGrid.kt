package com.nijatva.photos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.data.Memory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private sealed interface GridEntry {
    data class Header(val label: String, val count: Int) : GridEntry
    data class Photo(val item: MediaItem) : GridEntry
}

/**
 * The main photo grid, grouped by day with sticky-style day headers.
 * [extraContent] lets screens inject additional full-span rows at the top
 * (e.g. the Memories carousel on the Photos tab).
 */
@Composable
fun PhotoGrid(
    media: List<MediaItem>,
    favorites: Set<Long>,
    onOpenPhoto: (MediaItem) -> Unit,
    extraContent: (LazyGridScope.() -> Unit)? = null
) {
    val entries = remember(media) { groupByDay(media) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(1.dp)
    ) {
        extraContent?.invoke(this)
        entries.forEach { entry ->
            when (entry) {
                is GridEntry.Header -> item(
                    key = "header-${entry.label}",
                    span = { GridItemSpan(maxCurrentLineSpan) }
                ) {
                    DayHeader(entry.label, entry.count)
                }

                is GridEntry.Photo -> item(
                    key = "photo-${entry.item.id}",
                    contentType = "photo"
                ) {
                    PhotoCell(entry.item) { onOpenPhoto(entry.item) }
                }
            }
        }
    }
}

private fun groupByDay(media: List<MediaItem>): List<GridEntry> {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    val zone = ZoneId.systemDefault()
    val entries = mutableListOf<GridEntry>()
    // media is sorted newest-first; groupBy preserves that order.
    media.groupBy { Instant.ofEpochMilli(it.dateTakenMillis).atZone(zone).toLocalDate() }
        .forEach { (_, items) ->
            entries += GridEntry.Header(
                formatter.format(
                    Instant.ofEpochMilli(items.first().dateTakenMillis).atZone(zone)
                ),
                items.size
            )
            items.forEach { entries += GridEntry.Photo(it) }
        }
    return entries
}

@Composable
fun DayHeader(label: String, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, bottom = 4.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            if (count == 1) "1 item" else "$count items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PhotoCell(item: MediaItem, onOpen: () -> Unit) {
    Box(
        Modifier
            .padding(0.5.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onOpen)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (item.isVideo && item.durationMs != null) {
            Text(
                formatDuration(item.durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

/** Card for one "N years ago today" memory. */
@Composable
fun MemoryCard(memory: Memory, onClick: () -> Unit) {
    Box(
        Modifier
            .width(230.dp)
            .height(150.dp)
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = memory.items.first().uri,
            contentDescription = memory.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(memory.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                if (memory.items.size == 1) "1 photo" else "${memory.items.size} photos",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
