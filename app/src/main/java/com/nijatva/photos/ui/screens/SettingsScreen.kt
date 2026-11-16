package com.nijatva.photos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nijatva.photos.R
import com.nijatva.photos.data.Prefs
import com.nijatva.photos.findApp
import com.nijatva.photos.ui.openGooglePhotosApp
import com.nijatva.photos.sync.SyncStatus
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onOpenVault: () -> Unit = {},
    onOpenCleanup: () -> Unit = {}
) {
    val context = LocalContext.current
    val engine = context.findApp().container.syncEngine
    val scope = rememberCoroutineScope()

    val status by engine.status.collectAsStateWithLifecycle()
    val pending by engine.pendingCountFlow().collectAsStateWithLifecycle(initialValue = 0)

    var newestFirst by remember { mutableStateOf(Prefs.newestFirst(context)) }
    var autoplay by remember { mutableStateOf(Prefs.autoplayVideos(context)) }
    var stripMetadata by remember { mutableStateOf(Prefs.stripMetadata(context)) }
    var showPrivate by remember { mutableStateOf(Prefs.showPrivateAlbum(context)) }
    var labelAi by remember { mutableStateOf(Prefs.labelAiContent(context)) }
    var editedBehavior by remember { mutableStateOf(Prefs.editedFileBehavior(context)) }
    var lensUrl by remember { mutableStateOf(Prefs.lensApiUrl(context)) }
    var lensKey by remember { mutableStateOf(Prefs.lensApiKey(context)) }
    var editingLensUrl by remember { mutableStateOf(false) }
    var editingLensKey by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        /* ---- Backup ---- */
        SectionLabel("Backup")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingNavRow(
                    title = "Back up to Google Photos",
                    subtitle = "Opens Google Photos \u2014 turn on Backup & sync there"
                ) { openGooglePhotosApp(context) }
                HorizontalDivider()
                Text(
                    when (val s = status) {
                        SyncStatus.Idle -> "Sync idle. Your photos stay on this device."
                        is SyncStatus.NotConfigured ->
                            "${s.providerLabel} \u2014 nothing leaves your phone."
                        SyncStatus.InProgress -> "Syncing\u2026"
                        is SyncStatus.Done -> "${s.uploaded} item(s) backed up."
                        is SyncStatus.Failed -> "Sync failed: ${s.message}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "$pending item(s) in the backup queue",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Button(
                    onClick = { scope.launch { engine.syncNow() } },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) { Text("Sync now") }
                HorizontalDivider()
                SettingNavRow(
                    title = "Connect Zoho account",
                    subtitle = "Coming soon \u2014 the backup engine is already built in"
                ) {
                    Toast.makeText(
                        context,
                        "Zoho account integration arrives in a future version",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        /* ---- Browsing ---- */
        SectionLabel("Browsing")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            newestFirst = !newestFirst
                            Prefs.setNewestFirst(context, newestFirst)
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sort", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (newestFirst) "Newest at top" else "Oldest at top",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HorizontalDivider()
                SwitchRow(
                    title = "Autoplay videos without sound",
                    subtitle = "Applies once video playback arrives",
                    checked = autoplay
                ) {
                    autoplay = it
                    Prefs.setAutoplayVideos(context, it)
                }
            }
        }

        /* ---- Sharing ---- */
        SectionLabel("Sharing")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SwitchRow(
                    title = "Erase personal information",
                    subtitle = "Strip location and camera metadata (EXIF) from shared photos",
                    checked = stripMetadata
                ) {
                    stripMetadata = it
                    Prefs.setStripMetadata(context, it)
                }
            }
        }

        /* ---- Visual search (Lens) ---- */
        SectionLabel("Visual search (Lens)")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingNavRow(
                    title = "Lens API endpoint",
                    subtitle = if (lensUrl.isBlank())
                        "Not set \u2014 Lens runs fully on-device (ML Kit)"
                    else "$lensUrl (MyLensKit server mode)"
                ) { editingLensUrl = true }
                Text(
                    "Leave empty to keep Lens fully on-device (ML Kit). " +
                        "When set, Lens sends the photo to your API for analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        /* ---- More ---- */
        SectionLabel("More")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            editedBehavior = when (editedBehavior) {
                                "Always ask" -> "Overwrite"
                                "Overwrite" -> "Save as copy"
                                else -> "Always ask"
                            }
                            Prefs.setEditedFileBehavior(context, editedBehavior)
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edited-file saving behaviour", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        editedBehavior,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HorizontalDivider()
                SwitchRow(
                    title = "Show Private album",
                    subtitle = "Show the Private album at the bottom of the Albums page",
                    checked = showPrivate
                ) {
                    showPrivate = it
                    Prefs.setShowPrivateAlbum(context, it)
                }
                HorizontalDivider()
                SwitchRow(
                    title = "Label AI content",
                    subtitle = "Badge AI-generated or edited images once on-device AI arrives",
                    checked = labelAi
                ) {
                    labelAi = it
                    Prefs.setLabelAiContent(context, it)
                }
            }
        }

        /* ---- Storage ---- */
        SectionLabel("Storage")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Find potential duplicates and the largest files taking up space.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onOpenCleanup) { Text("Clean up storage") }
            }
        }

        /* ---- About ---- */
        SectionLabel("About")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.brand_logo),
                contentDescription = "Nijatva Photos",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column {
                Text("Nijatva Photos", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Photos \u00B7 local-first gallery",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SettingNavRow(
                    title = "About Nijatva Photos",
                    subtitle = "Nijatva Photos 2.2.0 \u00B7 local-first gallery"
                ) {}
                HorizontalDivider()
                SettingNavRow(
                    title = "Privacy",
                    subtitle = "Photos are read from device storage and never uploaded without your action"
                ) {}
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (editingLensUrl) {
        var value by remember { mutableStateOf(lensUrl) }
        AlertDialog(
            onDismissRequest = { editingLensUrl = false },
            title = { Text("Lens API endpoint") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text("https://your-api.example.com/analyze") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    lensUrl = value
                    Prefs.setLensApiUrl(context, value)
                    editingLensUrl = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingLensUrl = false }) { Text("Cancel") }
            }
        )
    }

    if (editingLensKey) {
        var value by remember { mutableStateOf(lensKey) }
        AlertDialog(
            onDismissRequest = { editingLensKey = false },
            title = { Text("Lens API key") },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text("Paste your API key") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    lensKey = value
                    Prefs.setLensApiKey(context, value)
                    editingLensKey = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingLensKey = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingNavRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
