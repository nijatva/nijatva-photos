package com.nijatva.photos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.ui.components.PhotoGrid
import com.nijatva.photos.ui.vm.AlbumDetailVMFactory
import com.nijatva.photos.ui.vm.AlbumDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    onOpenPhoto: (MediaItem) -> Unit
) {
    val vm: AlbumDetailViewModel = viewModel(factory = AlbumDetailVMFactory)

    val media by vm.media.collectAsStateWithLifecycle()
    val allMedia by vm.allMedia.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()
    val mediaIds by vm.mediaIds.collectAsStateWithLifecycle()

    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (vm.isUserAlbum) {
                ExtendedFloatingActionButton(
                    onClick = { showPicker = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add photos") }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (media.isEmpty()) {
                Text(
                    "This album is empty. Tap \u201CAdd photos\u201D to pick some.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                PhotoGrid(media, favorites, onOpenPhoto)
            }
        }
    }

    if (showPicker && vm.isUserAlbum) {
        MediaPickerDialog(
            allMedia = allMedia,
            initial = mediaIds,
            onDismiss = { showPicker = false },
            onConfirm = { selected ->
                val added = selected - mediaIds
                val removed = mediaIds - selected
                vm.updateMembers(added.toList(), removed.toList())
                showPicker = false
            }
        )
    }
}

@Composable
private fun MediaPickerDialog(
    allMedia: List<MediaItem>,
    initial: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var selected by remember { mutableStateOf(initial) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Text("Select photos", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { onConfirm(selected) }) { Text("Done (${selected.size})") }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(1.dp)
                ) {
                    items(allMedia, key = { it.id }) { item ->
                        val checked = item.id in selected
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .clickable {
                                    selected = if (checked) selected - item.id else selected + item.id
                                }
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (checked) {
                                Checkbox(
                                    checked = true,
                                    onCheckedChange = null,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
