package com.vasudev.photos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vasudev.photos.data.MediaItem
import com.vasudev.photos.ui.components.PhotoGrid
import com.vasudev.photos.ui.vm.SearchFilter
import com.vasudev.photos.ui.vm.SearchVMFactory
import androidx.compose.ui.res.painterResource
import com.vasudev.photos.R
import com.vasudev.photos.ui.vm.SearchViewModel
import com.vasudev.photos.ui.vm.TimeGroup

@Composable
fun SearchScreen(
    onOpenPhoto: (MediaItem) -> Unit,
    onOpenLens: () -> Unit = {}
) {
    val vm: SearchViewModel = viewModel(factory = SearchVMFactory)

    val query by vm.query.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val times by vm.times.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        // Pill search bar + Cancel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                placeholder = { Text("Times, places, file names...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onOpenLens) {
                        Icon(
                            painterResource(R.drawable.ic_lens),
                            contentDescription = "Lens",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f)
            )
            Text(
                "Cancel",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .padding(vertical = 8.dp)
                    .clickable { vm.onQueryChange("") }
            )
        }

        // Times row (shown while not typing)
        if (query.isBlank() && times.isNotEmpty()) {
            Text(
                "Times",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(times) { group ->
                    TimeCard(group) { onOpenPhoto(group.items.first()) }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            FilterChip(
                selected = filter == SearchFilter.ALL,
                onClick = { vm.setFilter(SearchFilter.ALL) },
                label = { Text("All") }
            )
            FilterChip(
                selected = filter == SearchFilter.FAVORITES,
                onClick = { vm.setFilter(SearchFilter.FAVORITES) },
                label = { Text("Favorites") }
            )
            FilterChip(
                selected = filter == SearchFilter.GIFS,
                onClick = { vm.setFilter(SearchFilter.GIFS) },
                label = { Text("GIFs") }
            )
            FilterChip(
                selected = filter == SearchFilter.SCREENSHOTS,
                onClick = { vm.setFilter(SearchFilter.SCREENSHOTS) },
                label = { Text("Screenshots") }
            )
            FilterChip(
                selected = filter == SearchFilter.VIDEOS,
                onClick = { vm.setFilter(SearchFilter.VIDEOS) },
                label = { Text("Videos") }
            )
        }

        if (results.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (query.isBlank())
                        "Search by file name, or use the filters above"
                    else
                        "No results for \u201C$query\u201D",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            PhotoGrid(results, favorites, onOpenPhoto)
        }
    }
}

@Composable
private fun TimeCard(group: TimeGroup, onClick: () -> Unit) {
    Column(
        Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (group.coverUri != null) {
                AsyncImage(
                    model = group.coverUri,
                    contentDescription = group.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            group.label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
