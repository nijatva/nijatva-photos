package com.vasudev.photos.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vasudev.photos.data.MediaItem
import com.vasudev.photos.ui.DeleteResult
import com.vasudev.photos.ui.deleteMediaForever
import com.vasudev.photos.ui.vm.TrashVMFactory
import com.vasudev.photos.ui.vm.TrashViewModel
import kotlinx.coroutines.launch

/** Items moved to trash: restore them, or delete them forever. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: TrashViewModel = viewModel(factory = TrashVMFactory)
    val items by vm.items.collectAsStateWithLifecycle()

    var actionTarget by remember { mutableStateOf<MediaItem?>(null) }
    var pendingDelete by remember { mutableStateOf<MediaItem?>(null) }
    val scope = rememberCoroutineScope()

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val target = pendingDelete
        pendingDelete = null
        if (result.resultCode == Activity.RESULT_OK && target != null) {
            scope.launch {
                if (deleteMediaForever(context, target) is DeleteResult.Success) {
                    vm.onDeletedForever(target.id)
                    Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            if (items.isEmpty()) {
                Text(
                    "Trash is empty",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    items(items, key = { it.item.id }) { entry ->
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(2.dp))
                        ) {
                            AsyncImage(
                                model = entry.item.uri,
                                contentDescription = entry.item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { actionTarget = entry.item }
                            )
                            // 30-day retention countdown badge
                            Text(
                                if (entry.daysLeft <= 0) "Expired" else "${entry.daysLeft}d",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    actionTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text("Choose an action") },
            text = { Text("\u201C${target.displayName}\u201D") },
            confirmButton = {
                TextButton(onClick = {
                    vm.restore(target.id)
                    actionTarget = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    actionTarget = null
                    scope.launch {
                        when (val r = deleteMediaForever(context, target)) {
                            is DeleteResult.Success -> {
                                vm.onDeletedForever(target.id)
                                Toast.makeText(
                                    context, "Deleted permanently", Toast.LENGTH_SHORT
                                ).show()
                            }
                            is DeleteResult.NeedsPermission -> {
                                pendingDelete = target
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(r.sender).build()
                                )
                            }
                            is DeleteResult.Error ->
                                Toast.makeText(context, r.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Delete forever") }
            }
        )
    }
}
