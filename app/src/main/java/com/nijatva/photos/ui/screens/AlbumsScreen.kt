package com.nijatva.photos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nijatva.photos.data.Prefs
import com.nijatva.photos.data.UserAlbum
import com.nijatva.photos.ui.vm.AlbumsVMFactory
import com.nijatva.photos.ui.vm.AlbumsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumsScreen(
    onOpenUserAlbum: (Long) -> Unit,
    onOpenBucket: (String) -> Unit,
    onOpenTrash: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenCleanup: () -> Unit = {},
    onOpenPhotos: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSmart: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: AlbumsViewModel = viewModel(factory = AlbumsVMFactory)

    val media by vm.media.collectAsStateWithLifecycle()
    val userAlbums by vm.userAlbums.collectAsStateWithLifecycle()
    val trashCount by vm.trashCount.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<UserAlbum?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }
    var sectionState by remember {
        mutableStateOf(
            Prefs.sectionOrder(context).map { it to (it !in Prefs.hiddenSections(context)) }
        )
    }

    val showPrivate = Prefs.showPrivateAlbum(context)
    val cameraItems = media.filter { it.bucketName.equals("Camera", ignoreCase = true) }
    val videoItems = media.filter { it.isVideo }
    val screenshotItems = media.filter {
        it.bucketName?.contains("screenshot", ignoreCase = true) == true
    }
    val favoriteItems = media.filter { it.id in favorites }
    val peopleItems = media.filter { !it.isVideo }
    val gifCount = media.count { it.mimeType == "image/gif" }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------- top bar ----------
        item(span = { GridItemSpan(maxCurrentLineSpan) }) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Albums",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("New album") },
                                onClick = { menuOpen = false; showCreateDialog = true }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Custom order") },
                                onClick = { menuOpen = false; showAdjust = true }
                            )
                        }
                    }
                }
            }
        }

        // ---------- Pin section ----------
        item(span = { GridItemSpan(maxCurrentLineSpan) }) {
            SectionHeader(title = "Pin", actionText = "Adjust") { showAdjust = true }
        }

        val pinned = buildList {
            add(PinEntry("Recent", media.size.toString(), media.firstOrNull()?.uri, null, onOpenPhotos))
            add(PinEntry("Camera", cameraItems.size.toString(), cameraItems.firstOrNull()?.uri, null) { onOpenBucket("Camera") })
            add(PinEntry("Videos", videoItems.size.toString(), videoItems.firstOrNull()?.uri, null) { onOpenBucket("Videos") })
            add(PinEntry("Screenshots", screenshotItems.size.toString(), screenshotItems.firstOrNull()?.uri, null) { onOpenBucket("Screenshots") })
            add(PinEntry("Favorites", favoriteItems.size.toString(), favoriteItems.firstOrNull()?.uri, null) { onOpenBucket("Favorites") })
            add(PinEntry("Recently deleted", trashCount.toString(), null, Icons.Filled.Delete, onOpenTrash))
            if (showPrivate) {
                add(PinEntry("Private", null, null, Icons.Filled.Lock, onOpenVault))
            }
        }
        items(pinned, key = { "pin-${it.label}" }) { entry ->
            SquareCard(
                label = entry.label,
                count = entry.count,
                coverUri = entry.coverUri,
                icon = entry.icon,
                onClick = entry.onClick
            )
        }

        // ---------- dynamic sections ----------
        for ((sectionId, visible) in sectionState) {
            if (!visible) continue
            when (sectionId) {
                "moreAlbums" -> {
                    item(span = { GridItemSpan(maxCurrentLineSpan) }, key = "hdr-moreAlbums") {
                        SectionHeader("More albums")
                    }
                    item(key = "new-album") {
                        SquareCard(
                            label = "New album",
                            count = null,
                            coverUri = null,
                            icon = Icons.Filled.Add,
                            onClick = { showCreateDialog = true }
                        )
                    }
                    items(userAlbums, key = { "user-${it.album.albumId}" }) { album ->
                        val cover = media.firstOrNull { it.id == album.mediaIds.firstOrNull() }
                        SquareCard(
                            label = album.album.name,
                            count = "${album.mediaIds.size}",
                            coverUri = cover?.uri,
                            icon = null,
                            onClick = { onOpenUserAlbum(album.album.albumId) },
                            onLongClick = { deleteTarget = album }
                        )
                    }
                }
                "featured" -> {
                    item(span = { GridItemSpan(maxCurrentLineSpan) }, key = "hdr-featured") {
                        SectionHeader("Featured")
                    }
                    item(key = "feat-memories") {
                        SquareCard(
                            label = "Memories",
                            count = "On this day",
                            coverUri = media.firstOrNull()?.uri,
                            icon = null,
                            onClick = onOpenPhotos
                        )
                    }
                    item(key = "feat-people") {
                        SquareCard(
                            label = "People",
                            count = "Faces \u00B7 on-device",
                            coverUri = peopleItems.firstOrNull()?.uri,
                            icon = Icons.Filled.Face,
                            onClick = onOpenSmart
                        )
                    }
                    item(key = "feat-smart") {
                        SquareCard(
                            label = "Smart categories",
                            count = "ML Kit \u00B7 on-device",
                            coverUri = media.firstOrNull()?.uri,
                            icon = Icons.Filled.Star,
                            onClick = onOpenSmart
                        )
                    }
                }
                "mediaTypes" -> {
                    item(span = { GridItemSpan(maxCurrentLineSpan) }, key = "hdr-mediaTypes") {
                        SectionHeader("Media types")
                    }
                    item(span = { GridItemSpan(maxCurrentLineSpan) }, key = "row-gifs") {
                        ListRow(
                            icon = null,
                            label = "GIF",
                            count = gifCount.toString(),
                            onClick = { onOpenBucket("GIFs") }
                        )
                    }
                    item(span = { GridItemSpan(maxCurrentLineSpan) }, key = "row-videos") {
                        ListRow(
                            icon = null,
                            label = "Videos",
                            count = videoItems.size.toString(),
                            onClick = { onOpenBucket("Videos") }
                        )
                    }
                }
                "more" -> {
                    item(span = { GridItemSpan(maxCurrentLineSpan) }, key = "hdr-more") {
                        SectionHeader("More")
                    }
                    if (showPrivate) {
                        item(span = { GridItemSpan(maxCurrentLineSpan) }, key = "row-private") {
                            ListRow(
                                icon = Icons.Filled.Lock,
                                label = "Private",
                                count = null,
                                onClick = onOpenVault
                            )
                        }
                    }
                    item(span = { GridItemSpan(maxCurrentLineSpan) }, key = "row-cleanup") {
                        ListRow(
                            icon = Icons.Filled.Refresh,
                            label = "Cleanup",
                            count = null,
                            onClick = onOpenCleanup
                        )
                    }
                    item(span = { GridItemSpan(maxCurrentLineSpan) }, key = "row-deleted") {
                        ListRow(
                            icon = Icons.Filled.Delete,
                            label = "Recently deleted",
                            count = trashCount.toString(),
                            onClick = onOpenTrash
                        )
                    }
                }
            }
        }
    }

    // ---------- new album dialog ----------
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    "New album",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Enter an album name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            vm.createAlbum(name.trim())
                            showCreateDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ---------- delete album dialog ----------
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete album?") },
            text = { Text("\u201C${target.album.name}\u201D will be removed. Your photos stay on the device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAlbum(target.album.albumId)
                        deleteTarget = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // ---------- custom order / visibility dialog ----------
    if (showAdjust) {
        AlertDialog(
            onDismissRequest = {
                showAdjust = false
                sectionState = Prefs.sectionOrder(context)
                    .map { it to (it !in Prefs.hiddenSections(context)) }
            },
            title = {
                Text("Custom", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Column {
                    Text(
                        "Sort sections by custom order or hide them",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    sectionState.forEachIndexed { index, (id, visible) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = visible,
                                onCheckedChange = { checked ->
                                    sectionState = sectionState.toMutableList()
                                        .also { list -> list[index] = id to checked }
                                }
                            )
                            Text(
                                Prefs.sectionLabels(id),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(
                                enabled = index > 0,
                                onClick = {
                                    sectionState = sectionState.toMutableList().also {
                                        val moved = it.removeAt(index)
                                        it.add(index - 1, moved)
                                    }
                                }
                            ) { Text("Up") }
                            TextButton(
                                enabled = index < sectionState.size - 1,
                                onClick = {
                                    sectionState = sectionState.toMutableList().also {
                                        val moved = it.removeAt(index)
                                        it.add(index + 1, moved)
                                    }
                                }
                            ) { Text("Down") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.setSectionOrder(context, sectionState.map { it.first })
                    Prefs.setHiddenSections(context, sectionState.filter { !it.second }.map { it.first }.toSet())
                    showAdjust = false
                }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdjust = false
                    sectionState = Prefs.sectionOrder(context)
                        .map { it to (it !in Prefs.hiddenSections(context)) }
                }) { Text("Cancel") }
            }
        )
    }
}

private data class PinEntry(
    val label: String,
    val count: String?,
    val coverUri: String?,
    val icon: ImageVector?,
    val onClick: () -> Unit
)

@Composable
private fun SectionHeader(title: String, actionText: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionText != null) {
            Text(
                actionText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAction() }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SquareCard(
    label: String,
    count: String?,
    coverUri: String?,
    icon: ImageVector?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick ?: {}
        )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (coverUri != null) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (icon != null) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (count != null) {
            Text(
                count,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ListRow(
    icon: ImageVector?,
    label: String,
    count: String?,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (icon != null) 16.dp else 4.dp)
        )
        if (count != null) {
            Text(
                count,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}
