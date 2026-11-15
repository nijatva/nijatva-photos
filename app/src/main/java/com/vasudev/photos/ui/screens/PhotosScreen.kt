package com.vasudev.photos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.vasudev.photos.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.ui.platform.LocalContext
import com.vasudev.photos.data.MediaItem
import com.vasudev.photos.data.Prefs
import com.vasudev.photos.ui.components.MemoryCard
import com.vasudev.photos.ui.components.PhotoGrid
import com.vasudev.photos.ui.vm.PhotosVMFactory
import com.vasudev.photos.ui.vm.PhotosViewModel

@Composable
fun PhotosScreen(
    onOpenPhoto: (MediaItem) -> Unit,
    onOpenSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: PhotosViewModel = viewModel(factory = PhotosVMFactory)

    val media by vm.media.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val memories by vm.memories.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    var granted by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.all { it }
        if (granted) vm.refresh()
    }

    // Re-scan whenever the tab is opened with permission granted,
    // and again on every re-entry (so renames / private items update).
    LaunchedEffect(granted) {
        if (granted) vm.refresh()
    }
    LaunchedEffect(Unit) {
        if (granted) vm.refresh()
    }

    var newestFirst by remember { mutableStateOf(Prefs.newestFirst(context)) }
    val sortedMedia = remember(media, newestFirst) {
        if (newestFirst) media else media.asReversed()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.brand_logo),
                    contentDescription = "Nijatva Photos",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Text(
                    "Nijatva Photos",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
                IconButton(onClick = {
                    newestFirst = !newestFirst
                    Prefs.setNewestFirst(context, newestFirst)
                }) {
                    Icon(
                        if (newestFirst) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (newestFirst) "Newest first" else "Oldest first"
                    )
                }
            }
        }
        when {
            !granted -> PermissionNeeded { permissionLauncher.launch(permissions) }

            loading && media.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            media.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No photos or videos found on this device",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> PhotoGrid(sortedMedia, favorites, onOpenPhoto) {
                if (memories.isNotEmpty()) {
                    item(key = "memories", span = { GridItemSpan(maxCurrentLineSpan) }) {
                        Column {
                            Text(
                                "Memories",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
                            )
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                                items(memories, key = { it.yearsAgo }) { memory ->
                                    MemoryCard(memory) { onOpenPhoto(memory.items.first()) }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionNeeded(onRequest: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Allow access", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Nijatva Photos stores your photos and videos on this device. " +
                "Grant photo and video access to see your gallery.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Allow access") }
    }
}
