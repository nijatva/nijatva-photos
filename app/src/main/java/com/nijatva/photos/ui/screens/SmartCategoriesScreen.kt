package com.nijatva.photos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.ui.components.PhotoGrid
import com.nijatva.photos.ui.vm.SmartVMFactory
import com.nijatva.photos.ui.vm.SmartViewModel

/**
 * Smart categories (on-device ML Kit): scan your photos once, then browse
 * them by automatic labels and see photos that contain faces (People).
 * Everything runs offline; nothing is uploaded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCategoriesScreen(
    onBack: () -> Unit,
    onOpenPhoto: (MediaItem) -> Unit
) {
    val vm: SmartViewModel = viewModel(factory = SmartVMFactory)

    val labelCounts by vm.labelCounts.collectAsStateWithLifecycle()
    val peopleItems by vm.peopleItems.collectAsStateWithLifecycle()
    val indexedCount by vm.indexedCount.collectAsStateWithLifecycle()
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle()
    val selectedLabel by vm.selectedLabel.collectAsStateWithLifecycle()
    val labelItems by vm.labelItems.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()

    val currentLabel = selectedLabel
    if (currentLabel != null) {
        // ---- label detail view ----
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentLabel.replaceFirstChar { it.uppercase() }) },
                    navigationIcon = {
                        IconButton(onClick = vm::closeLabel) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (labelItems.isEmpty()) {
                    Text(
                        "No photos for this category",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    PhotoGrid(labelItems, favorites, onOpenPhoto)
                }
            }
        }
        return
    }

    // ---- category list view ----
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                Column {
                    Text(
                        "On-device ML Kit",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Labels and face detection run offline on your phone. " +
                            "Your photos are never uploaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- People (photos with faces) ----
            if (peopleItems.isNotEmpty()) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    Column {
                        Text(
                            "People",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems(peopleItems, key = { it.id }) { item ->
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable { onOpenPhoto(item) }
                                )
                            }
                        }
                    }
                }
            }

            // ---- categories ----
            if (labelCounts.isNotEmpty()) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    Text(
                        "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                items(labelCounts, key = { it.first }) { (label, count) ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                label.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            Text(
                                "$count photos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ---- scan controls ----
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                val progress = scanProgress
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (progress != null) {
                            Text(
                                "Scanning\u2026 ${progress.first} / ${progress.second}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val fraction =
                                if (progress.second == 0) 0f
                                else progress.first.toFloat() / progress.second
                            @Suppress("DEPRECATION")
                            LinearProgressIndicator(
                                progress = fraction,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                if (indexedCount == 0)
                                    "Scan your photos to build smart categories and find people."
                                else
                                    "$indexedCount photos indexed. Rescan to pick up new photos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = vm::scan) {
                                Text(
                                    if (indexedCount == 0) "Scan now" else "Rescan"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
