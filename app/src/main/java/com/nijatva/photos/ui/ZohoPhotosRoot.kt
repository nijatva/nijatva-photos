package com.nijatva.photos.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nijatva.photos.findApp
import com.nijatva.photos.ui.screens.AlbumDetailScreen
import com.nijatva.photos.ui.screens.AlbumsScreen
import com.nijatva.photos.ui.screens.AppearanceScreen
import com.nijatva.photos.ui.screens.CleanupScreen
import com.nijatva.photos.ui.screens.DeveloperScreen
import com.nijatva.photos.ui.screens.EditorScreen
import com.nijatva.photos.ui.screens.LensScreen
import com.nijatva.photos.ui.screens.PhotoPagerScreen
import com.nijatva.photos.ui.screens.PhotosScreen
import com.nijatva.photos.ui.screens.SearchScreen
import com.nijatva.photos.ui.screens.SettingsScreen
import com.nijatva.photos.ui.screens.SmartCategoriesScreen
import com.nijatva.photos.ui.screens.TrashScreen
import com.nijatva.photos.ui.screens.VaultScreen
import kotlinx.coroutines.launch

@Composable
fun ZohoPhotosRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == "photos" || currentRoute == "albums"

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var showNewAlbum by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                GalleryBottomBar(
                    currentRoute = currentRoute,
                    onPhotos = { navController.navigateTab("photos") },
                    onAlbums = { navController.navigateTab("albums") },
                    onSettings = { navController.navigate("settings") },
                    onAdd = { showNewAlbum = true }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "photos",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable("photos") {
                PhotosScreen(
                    onOpenPhoto = { navController.navigate("photo/${it.id}") },
                    onOpenSearch = { navController.navigate("search") }
                )
            }
            composable("albums") {
                AlbumsScreen(
                    onOpenUserAlbum = { navController.navigate("album/$it") },
                    onOpenBucket = { navController.navigate("bucket/${toUriPath(it)}") },
                    onOpenTrash = { navController.navigate("trash") },
                    onOpenVault = { navController.navigate("vault") },
                    onOpenCleanup = { navController.navigate("cleanup") },
                    onOpenPhotos = { navController.navigateTab("photos") },
                    onOpenSearch = { navController.navigate("search") },
                    onOpenSmart = { navController.navigate("smart") }
                )
            }
            composable("album/{albumId}") {
                AlbumDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPhoto = { navController.navigate("photo/${it.id}") }
                )
            }
            composable("bucket/{bucket}") {
                AlbumDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPhoto = { navController.navigate("photo/${it.id}") }
                )
            }
            composable("photo/{mediaId}") {
                PhotoPagerScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAlbum = { navController.navigate("album/$it") },
                    onOpenBucket = {
                        navController.navigate("bucket/${toUriPath(it)}")
                    },
                    onOpenLens = {
                        navController.navigate("lens?mediaId=${it}")
                    },
                    onOpenEditor = { uri ->
                        navController.navigate("editor?uri=" + android.net.Uri.encode(uri))
                    }
                )
            }
            composable(
                "editor?uri={uri}",
                arguments = listOf(
                    androidx.navigation.navArgument("uri") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entry ->
                EditorScreen(
                    photoUri = entry.arguments?.getString("uri"),
                    onBack = { navController.popBackStack() }
                )
            }
            composable("search") {
                SearchScreen(
                    onOpenPhoto = { navController.navigate("photo/${it.id}") },
                    onOpenLens = { navController.navigate("lens") }
                )
            }
            composable(
                "lens?mediaId={mediaId}",
                arguments = listOf(
                    androidx.navigation.navArgument("mediaId") {
                        type = androidx.navigation.NavType.StringType
                        defaultValue = "-1"
                    }
                )
            ) {
                LensScreen(onBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(
                    onOpenVault = { navController.navigate("vault") },
                    onOpenCleanup = { navController.navigate("cleanup") },
                    onOpenAppearance = { navController.navigate("appearance") },
                    onOpenDeveloper = { navController.navigate("developer") }
                )
            }
            composable("appearance") {
                AppearanceScreen(onBack = { navController.popBackStack() })
            }
            composable("developer") {
                DeveloperScreen(onBack = { navController.popBackStack() })
            }
            composable("vault") {
                VaultScreen(onBack = { navController.popBackStack() })
            }
            composable("trash") {
                TrashScreen(onBack = { navController.popBackStack() })
            }
            composable("cleanup") {
                CleanupScreen(onBack = { navController.popBackStack() })
            }
            composable("smart") {
                SmartCategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPhoto = { navController.navigate("photo/${it.id}") }
                )
            }
        }
    }

    // Global "New album" dialog, opened by the + button in the bottom bar.
    if (showNewAlbum) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewAlbum = false },
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
                            val albumName = name.trim()
                            scope.launch {
                                context.findApp().container.repository.createAlbum(albumName)
                            }
                            showNewAlbum = false
                            navController.navigateTab("albums")
                            Toast.makeText(context, "Album created", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNewAlbum = false }) { Text("Cancel") }
            }
        )
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun toUriPath(value: String): String = android.net.Uri.encode(value)

/* ------------------------------------------------------------------ */
/* Google Photos-style bottom bar:                                    */
/* [settings]  ( Photos | Albums )  [+]                               */
/* ------------------------------------------------------------------ */

@Composable
private fun GalleryBottomBar(
    currentRoute: String?,
    onPhotos: () -> Unit,
    onAlbums: () -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleButton(icon = Icons.Filled.Settings, contentDescription = "Settings", onClick = onSettings)

        Row(
            Modifier
                .width(220.dp)
                .height(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            PillOption(
                label = "Photos",
                active = currentRoute == "photos",
                onClick = onPhotos,
                modifier = Modifier.weight(1f)
            )
            PillOption(
                label = "Albums",
                active = currentRoute == "albums",
                onClick = onAlbums,
                modifier = Modifier.weight(1f)
            )
        }

        CircleButton(icon = Icons.Filled.Add, contentDescription = "New album", onClick = onAdd)
    }
}

@Composable
private fun CircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PillOption(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

