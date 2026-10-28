package com.vasudev.photos.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.vasudev.photos.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mylens.kit.MyLens
import com.vasudev.photos.data.Prefs
import com.vasudev.photos.ui.components.PhotoGrid
import com.vasudev.photos.ui.vm.LensAnalysis
import com.vasudev.photos.ui.vm.LensVMFactory
import com.vasudev.photos.ui.vm.LensViewModel

/**
 * Lens — visual search powered by MyLensKit.
 *  - No server configured: fully on-device analysis (labels, OCR, faces)
 *    with Web search and Visual matches (web) actions.
 *  - Server URL set in Settings: photos go to the MyLensKit/OpenLens server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensScreen(onBack: () -> Unit) {
    val vm: LensViewModel = viewModel(factory = LensVMFactory)
    val context = LocalContext.current
    val activity = context as? Activity

    val media by vm.media.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val analysis by vm.analysis.collectAsStateWithLifecycle()
    val analyzing by vm.analyzing.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()

    val item = selected
    if (item == null) {
        // ---- picker ----
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Lens \u00B7 pick a photo") },
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
                if (media.isEmpty()) {
                    Text(
                        "No photos to analyze",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val serverUrl = Prefs.lensApiUrl(context)
                    PhotoGrid(
                        media = media,
                        favorites = favorites,
                        onOpenPhoto = { photo ->
                            if (serverUrl.isNotBlank() && activity != null) {
                                MyLens.analyzeOnServer(
                                    activity, Uri.parse(photo.uri), serverUrl
                                )
                            } else {
                                vm.select(photo)
                            }
                        }
                    )
                }
            }
        }
        return
    }

    // ---- result view ----
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lens") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (vm.selected.value == null) onBack() else vm.pickAnother()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            if (analyzing) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            val a = analysis
            if (a != null) {
                Text(
                    "Analyzed with MyLensKit \u00B7 on-device",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (a.labels.isNotEmpty()) {
                    LabelFlow(a.labels)
                }

                val ocrText = a.ocrText
                if (ocrText != null) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Detected text", style = MaterialTheme.typography.titleSmall)
                            Text(ocrText, style = MaterialTheme.typography.bodySmall)
                            // Quick actions on the extracted text
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard =
                                            context.getSystemService(ClipboardManager::class.java)
                                        clipboard?.setPrimaryClip(
                                            ClipData.newPlainText("Lens text", ocrText)
                                        )
                                        Toast.makeText(
                                            context, "Text copied", Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_copy),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy")
                                }
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(
                                                        "https://translate.google.com/?sl=auto&tl=en&op=translate&text=" +
                                                            java.net.URLEncoder.encode(
                                                                ocrText.take(1500), "UTF-8"
                                                            )
                                                    )
                                                )
                                            )
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context, "No browser found", Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_translate),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Translate")
                                }
                                OutlinedButton(
                                    onClick = {
                                        activity?.let { MyLens.webSearch(it, ocrText.take(300)) }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_search),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Search")
                                }
                            }
                        }
                    }
                }

                if (a.faces > 0) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Faces", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${a.faces} face(s) detected",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (a.errors.isNotEmpty()) {
                    Text(
                        a.errors.joinToString("\n"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ---- MyLensKit actions ----
                if (activity != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { MyLens.openVisualMatches(activity, Uri.parse(item.uri)) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Visual matches") }
                        OutlinedButton(
                            onClick = { MyLens.webSearch(activity, a.topLabel ?: "") },
                            enabled = !a.topLabel.isNullOrBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Web search") }
                    }
                }
            }

            Button(onClick = vm::pickAnother, modifier = Modifier.fillMaxWidth()) {
                Text("Analyze another photo")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelFlow(labels: List<String>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("What's in this photo", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                labels.forEach { label ->
                    AssistChip(onClick = {}, label = { Text(label) })
                }
            }
        }
    }
}
