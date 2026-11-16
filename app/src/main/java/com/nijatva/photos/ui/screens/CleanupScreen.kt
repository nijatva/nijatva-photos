package com.nijatva.photos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.ui.components.formatSize
import com.nijatva.photos.ui.vm.CleanupVMFactory
import com.nijatva.photos.ui.vm.CleanupViewModel

/**
 * Cleanup suggestions (no ML): files with identical type/size/dimensions are
 * likely duplicates, plus the largest files on the device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(onBack: () -> Unit) {
    val vm: CleanupViewModel = viewModel(factory = CleanupVMFactory)

    val duplicateGroups by vm.duplicateGroups.collectAsStateWithLifecycle()
    val largeFiles by vm.largeFiles.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clean up storage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (duplicateGroups.isEmpty() && largeFiles.isEmpty()) {
                item {
                    Text(
                        "No suggestions right now \u2014 your storage looks tidy!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (duplicateGroups.isNotEmpty()) {
                item {
                    Text(
                        "Potential duplicates",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(duplicateGroups) { group ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            group.forEach { item -> CleanupRow(item, vm) }
                        }
                    }
                }
            }

            if (largeFiles.isNotEmpty()) {
                item {
                    Text(
                        "Largest files",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(largeFiles) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Box(Modifier.padding(12.dp)) {
                            CleanupRow(item, vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanupRow(item: MediaItem, vm: CleanupViewModel) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.fillMaxWidth(0.65f)) {
            Text(
                item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatSize(item.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = { vm.trash(item.id) }) { Text("Trash") }
    }
}
