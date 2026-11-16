package com.nijatva.photos.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mylens.kit.MyLens
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.data.Prefs
import com.nijatva.photos.data.UserAlbum
import com.nijatva.photos.R
import com.nijatva.photos.findApp
import com.nijatva.photos.sync.SyncResult
import com.nijatva.photos.ui.DeleteResult
import com.nijatva.photos.ui.RenameResult
import com.nijatva.photos.ui.components.formatDate
import com.nijatva.photos.ui.components.ActionSheet
import com.nijatva.photos.ui.components.SheetAction
import com.nijatva.photos.ui.components.SheetSection
import com.nijatva.photos.ui.components.formatSize
import com.nijatva.photos.ui.convertToPdf
import com.nijatva.photos.ui.readExifLocation
import com.nijatva.photos.ui.sendToGooglePhotos
import com.nijatva.photos.ui.prepareScrubbedShare
import com.nijatva.photos.ui.renameMedia
import com.nijatva.photos.ui.setContactAvatar
import com.nijatva.photos.ui.setWallpaperFrom
import com.nijatva.photos.ui.vm.PhotoPagerVMFactory
import com.nijatva.photos.ui.vm.PhotoPagerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Which album-sheet mode the viewer is showing. */
private enum class AlbumPickerMode(val title: String) {
    ADD("Add to album"),
    MOVE("Move to album"),
    COPY("Copy to album")
}

@Composable
fun PhotoPagerScreen(
    onBack: () -> Unit,
    onOpenAlbum: (Long) -> Unit = {},
    onOpenBucket: (String) -> Unit = {},
    onOpenLens: (Long) -> Unit = {},
    onOpenEditor: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val vm: PhotoPagerViewModel = viewModel(factory = PhotoPagerVMFactory)

    val media by vm.media.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val albums by vm.userAlbums.collectAsStateWithLifecycle()

    var showInfo by remember { mutableStateOf(false) }
    var albumPicker by remember { mutableStateOf<AlbumPickerMode?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var pendingRename by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (media.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
        return
    }

    val pagerState = rememberPagerState(initialPage = vm.initialIndex, pageCount = { media.size })
    val current = media[pagerState.currentPage.coerceIn(0, media.size - 1)]
    val isFavorite = current.id in favorites

    // ---- actions (need `current`, so they live here) ----

    val doConvertPdf: () -> Unit = {
        scope.launch {
            val name = convertToPdf(context, Uri.parse(current.uri), current.displayName)
            Toast.makeText(
                context,
                if (name != null) "Saved $name to Downloads" else "Could not convert to PDF",
                Toast.LENGTH_LONG
            ).show()
        }
        Unit
    }

    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = setContactAvatar(context, Uri.parse(current.uri), uri)
            Toast.makeText(
                context,
                if (ok) "Contact photo updated" else "Could not update contact photo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val contactPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) contactPicker.launch(null)
        else Toast.makeText(
            context, "Contacts permission is needed to set an avatar", Toast.LENGTH_SHORT
        ).show()
    }

    val legacyStorage = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) doConvertPdf()
        else Toast.makeText(
            context, "Storage permission is needed to save the PDF", Toast.LENGTH_SHORT
        ).show()
    }

    val doWallpaper: () -> Unit = {
        scope.launch {
            val ok = setWallpaperFrom(context, Uri.parse(current.uri))
            Toast.makeText(
                context, if (ok) "Wallpaper set" else "Could not set wallpaper", Toast.LENGTH_SHORT
            ).show()
        }
        Unit
    }

    val doContactAvatar: () -> Unit = {
        val needed = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        if (needed.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        ) contactPicker.launch(null)
        else contactPermissions.launch(needed)
    }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingRename?.let { target ->
                scope.launch {
                    if (renameMedia(context, current, target) is RenameResult.Success) {
                        vm.refreshMedia()
                        Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                    pendingRename = null
                }
            }
        } else pendingRename = null
    }

    val onConvertPdf: () -> Unit = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                doConvertPdf()
            } else {
                legacyStorage.launch(perm)
            }
        } else {
            doConvertPdf()
        }
        Unit
    }

    // ---- layout ----

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(state = pagerState) { page ->
            val pageItem = media[page]
            if (pageItem.isVideo) {
                VideoPlayer(uri = pageItem.uri, modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = pageItem.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top bar: back | date & time | camera + overflow menu
        val takenAt = remember(current.id) {
            val zdt = java.time.Instant.ofEpochMilli(current.dateTakenMillis)
                .atZone(java.time.ZoneId.systemDefault())
            java.time.format.DateTimeFormatter.ofPattern("d MMMM").format(zdt) to
                java.time.format.DateTimeFormatter.ofPattern("h:mm a").format(zdt).lowercase()
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    takenAt.first,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    takenAt.second,
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = {
                // Opens the system camera app (demo-grade integration).
                try {
                    context.startActivity(
                        android.content.Intent("android.media.action.STILL_IMAGE_CAMERA")
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "No camera app found", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(
                    painterResource(R.drawable.ic_camera),
                    contentDescription = "Open camera",
                    tint = Color.White
                )
            }
            Row {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, "More options", tint = Color.White)
                }
            }
        }

        // ---- Material 3 categorized action sheet (overflow menu) ----
        if (menuOpen) {
            ActionSheet(
                sections = listOf(
                    SheetSection("Edit", listOf(
                        SheetAction("Edit in Photalius", R.drawable.ic_edit) {
                            onOpenEditor(current.uri)
                        }
                    )),
                    SheetSection("Cloud", listOf(
                        SheetAction("Back up now", R.drawable.ic_cloud_upload) {
                            // Zoho-ready sync layer: item is queued persistently,
                            // but nothing uploads until a Zoho account exists.
                            scope.launch {
                                val result = context.findApp().container.syncEngine.enqueue(current.id)
                                val message = when (result) {
                                    SyncResult.Success -> "Backed up"
                                    SyncResult.NotConfigured ->
                                        "Zoho account not connected yet \u2014 item saved to the backup queue"
                                    is SyncResult.Error -> "Backup failed: ${result.message}"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        SheetAction("Back up to Google Photos", R.drawable.ic_camera) {
                            sendToGooglePhotos(context, current)
                        }
                    )),
                    SheetSection("Organize", listOf(
                        SheetAction("Move to album", R.drawable.ic_folder) {
                            albumPicker = AlbumPickerMode.MOVE
                        },
                        SheetAction("Copy to album", R.drawable.ic_copy) {
                            albumPicker = AlbumPickerMode.COPY
                        },
                        SheetAction("Rename", R.drawable.ic_edit) {
                            showRename = true
                        }
                    )),
                    SheetSection("Security", listOf(
                        SheetAction("Set as private", R.drawable.ic_lock) {
                            vm.makePrivate(current.id)
                            Toast.makeText(
                                context, "Moved to the private vault", Toast.LENGTH_SHORT
                            ).show()
                            onBack()
                        },
                        SheetAction("Set as wallpaper", R.drawable.ic_wallpaper) {
                            doWallpaper()
                        },
                        SheetAction("Set as contact avatar", R.drawable.ic_person) {
                            doContactAvatar()
                        }
                    )),
                    SheetSection("Conversion & search", listOf(
                        SheetAction("Convert to PDF", R.drawable.ic_pdf) {
                            onConvertPdf()
                        },
                        SheetAction("Lens analysis", R.drawable.ic_lens) {
                            onOpenLens(current.id)
                        },
                        SheetAction("Visual matches on web", R.drawable.ic_search) {
                            (context as? android.app.Activity)?.let { act ->
                                MyLens.openVisualMatches(act, Uri.parse(current.uri))
                            }
                        }
                    ))
                ),
                onDismiss = { menuOpen = false }
            )
        }

        // Bottom action bar — one seamless pill: Share | Favorite | Edit | Trash | Info
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = { scope.launch { shareItem(context, current) } }) {
                Icon(Icons.Filled.Share, "Share", tint = Color.White)
            }
            IconButton(onClick = { vm.toggleFavorite(current.id) }) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (isFavorite) "Remove favorite" else "Favorite",
                    tint = Color.White
                )
            }
            IconButton(onClick = {
                // Open the merged in-app Photalius editor on this photo.
                onOpenEditor(current.uri)
            }) {
                Icon(Icons.Filled.Edit, "Edit in Photalius", tint = Color.White)
            }
            IconButton(onClick = {
                vm.moveToTrash(current.id)
                Toast.makeText(context, "Moved to trash", Toast.LENGTH_SHORT).show()
                onBack()
            }) {
                Icon(Icons.Filled.Delete, "Delete", tint = Color.White)
            }
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Filled.Info, "Details", tint = Color.White)
            }
        }

        // Direct visual-search button (Google-Lens style) — one tap, no menu.
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 74.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .clickable { onOpenLens(current.id) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_lens),
                contentDescription = "Visual search",
                tint = Color(0xFF1A1A1A),
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Lens",
                color = Color(0xFF1A1A1A),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    // ---- sheets & dialogs ----

    if (showInfo) {
        DetailsSheet(
            context = context,
            item = current,
            albums = albums.filter { it.mediaIds.contains(current.id) },
            onOpenBucket = {
                showInfo = false
                current.bucketName?.let(onOpenBucket)
            },
            onOpenAlbum = {
                showInfo = false
                onOpenAlbum(it)
            },
            onDismiss = { showInfo = false }
        )
    }

    albumPicker?.let { mode ->
        AlbumPickerSheet(
            title = mode.title,
            albums = albums,
            onDismiss = { albumPicker = null },
            onPick = { albumId ->
                when (mode) {
                    AlbumPickerMode.ADD, AlbumPickerMode.COPY -> vm.addToAlbum(albumId, current.id)
                    AlbumPickerMode.MOVE -> vm.moveToAlbum(albumId, current.id)
                }
                albumPicker = null
            },
            onCreateNew = { name ->
                scope.launch {
                    vm.createAlbumAndAdd(name, current.id, move = mode == AlbumPickerMode.MOVE)
                }
                albumPicker = null
            }
        )
    }

    if (showRename) {
        var newName by remember(current.id) { mutableStateOf(current.displayName) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("File name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        showRename = false
                        val target = newName.trim()
                        scope.launch {
                            when (val r = renameMedia(context, current, target)) {
                                is RenameResult.Success -> {
                                    vm.refreshMedia()
                                    Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show()
                                }
                                is RenameResult.NeedsPermission -> {
                                    pendingRename = target
                                    intentSenderLauncher.launch(
                                        IntentSenderRequest.Builder(r.sender).build()
                                    )
                                }
                                is RenameResult.Error ->
                                    Toast.makeText(context, r.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * In-app video player with Google Photos-style overlay controls:
 * pause/play | elapsed / total time | mute (per-player volume).
 * Playback stops when the page leaves the viewport.
 */
@Composable
private fun VideoPlayer(uri: String, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }

    val player = remember {
        MediaPlayer().apply {
            setOnPreparedListener { mp ->
                durationMs = mp.duration
                mp.start()
                isPlaying = true
            }
            setOnCompletionListener {
                isPlaying = false
                positionMs = durationMs
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = player.currentPosition
            delay(200)
        }
    }

    DisposableEffect(uri) {
        onDispose {
            runCatching { player.stop() }
            runCatching { player.release() }
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            runCatching {
                                player.reset()
                                player.setDataSource(ctx, Uri.parse(uri))
                                player.setDisplay(holder)
                                player.prepareAsync()
                            }
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder, format: Int, width: Int, height: Int
                        ) {}

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            runCatching { player.stop() }
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay control bar (screenshot style)
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                runCatching {
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        if (durationMs > 0 && positionMs >= durationMs) {
                            player.seekTo(0)
                            positionMs = 0
                        }
                        player.start()
                        isPlaying = true
                    }
                }
            }) {
                if (isPlaying) PauseGlyph()
                else Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White)
            }
            Text(
                "${formatMillis(positionMs)} / ${formatMillis(durationMs)}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
            IconButton(onClick = {
                isMuted = !isMuted
                runCatching {
                    val volume = if (isMuted) 0f else 1f
                    player.setVolume(volume, volume)
                }
            }) {
                Icon(
                    painterResource(
                        if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
                    ),
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White
                )
            }
        }
    }
}

/** Two-bar pause glyph (the pause icon is not in the core icon set). */
@Composable
private fun PauseGlyph() {
    Canvas(Modifier.size(width = 18.dp, height = 18.dp)) {
        val bar = size.width / 5f
        drawRect(
            color = Color.White,
            topLeft = Offset(0f, 0f),
            size = Size(bar, size.height)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(bar * 4f, 0f),
            size = Size(bar, size.height)
        )
    }
}

private fun formatMillis(ms: Int): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** Circular white-on-translucent action button used in the viewer's bottom bar. */
@Composable
private fun CircleAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = Color.White)
    }
}

private suspend fun shareItem(context: Context, item: MediaItem) {
    var uriToShare = Uri.parse(item.uri)
    var scrubbed = false
    // "Erase personal information" (Settings): images are shared without EXIF.
    if (Prefs.stripMetadata(context) && !item.isVideo) {
        val file = prepareScrubbedShare(context, item)
        if (file != null) {
            uriToShare = FileProvider.getUriForFile(
                context, "com.nijatva.photos.fileprovider", file
            )
            scrubbed = true
        }
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType
        putExtra(Intent.EXTRA_STREAM, uriToShare)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share via"))
    if (scrubbed) {
        Toast.makeText(
            context, "Shared without location / camera metadata", Toast.LENGTH_SHORT
        ).show()
    }
}

/* ------------------------------------------------------------------ */
/* Details sheet (screenshot 1 workflow)                               */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSheet(
    context: Context,
    item: MediaItem,
    albums: List<UserAlbum>,
    onOpenBucket: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var location by remember(item.id) { mutableStateOf<String?>(null) }
    var locationChecked by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        location = readExifLocation(context, item)
        locationChecked = true
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(formatDate(item.dateTakenMillis), style = MaterialTheme.typography.titleMedium)
            Text(
                item.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (!locationChecked) "Checking location\u2026"
                else (location ?: "No location information"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(mediaLabel(item), style = MaterialTheme.typography.titleSmall)
                Text(
                    techLine(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!item.relativePath.isNullOrBlank()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBucket() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Phone/${item.relativePath!!.trim('/')}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            albums.forEach { album ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAlbum(album.album.albumId) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "From \u201C${album.album.name}\u201D album",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            Text(
                "Storage: On this device (local-first)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun mediaLabel(item: MediaItem): String = when {
    item.isVideo -> "Video"
    item.bucketName?.contains("screenshot", ignoreCase = true) == true -> "Screenshot"
    else -> "Photo"
}

private fun techLine(item: MediaItem): String {
    val mp = if (item.width > 0 && item.height > 0)
        "%.1fMP | ".format(item.width * item.height / 1_000_000f) else ""
    val dims = if (item.width > 0) "${item.width} \u00D7 ${item.height} | " else ""
    return "$mp${dims}${formatSize(item.sizeBytes)} | ${item.mimeType.substringAfter('/')}"
}

/* ------------------------------------------------------------------ */
/* Album picker sheet (add / move / copy)                              */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumPickerSheet(
    title: String,
    albums: List<UserAlbum>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onCreateNew: (String) -> Unit
) {
    var newAlbumName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (albums.isEmpty()) {
                Text(
                    "No albums yet \u2014 create one below",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            albums.forEach { album ->
                ListItem(
                    headlineContent = { Text(album.album.name) },
                    supportingContent = { Text("${album.mediaIds.size} items") },
                    modifier = Modifier.clickable { onPick(album.album.albumId) }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            OutlinedTextField(
                value = newAlbumName,
                onValueChange = { newAlbumName = it },
                label = { Text("New album name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onCreateNew(newAlbumName.trim()) },
                enabled = newAlbumName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create album") }
        }
    }
}
