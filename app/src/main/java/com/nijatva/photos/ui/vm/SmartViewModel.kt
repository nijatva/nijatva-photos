package com.nijatva.photos.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.data.PhotoRepository
import com.nijatva.photos.data.SmartIndexer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Powers the Smart categories screen: on-device ML Kit labels,
 * face-based People row, scan progress and label-filtered grids.
 */
class SmartViewModel(
    private val indexer: SmartIndexer,
    private val repository: PhotoRepository
) : ViewModel() {

    private val allMedia = MutableStateFlow<List<MediaItem>>(emptyList())

    val labelCounts = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val peopleItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val indexedCount = MutableStateFlow(0)
    val scanProgress = MutableStateFlow<Pair<Int, Int>?>(null)

    val selectedLabel = MutableStateFlow<String?>(null)
    val labelItems = MutableStateFlow<List<MediaItem>>(emptyList())

    val favorites = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            allMedia.value = repository.refresh()
            refreshIndex()
        }
    }

    private fun refreshIndex() {
        viewModelScope.launch {
            val labels = indexer.labelIndex()
            labelCounts.value = labels.map { it.key to it.value.size }
                .sortedByDescending { it.second }
            val faces = indexer.faceIds()
            peopleItems.value = allMedia.value.filter { it.id in faces }
            indexedCount.value = indexer.indexedCount()
        }
    }

    fun scan() {
        if (scanProgress.value != null) return
        viewModelScope.launch {
            scanProgress.value = 0 to 1
            indexer.indexAll(allMedia.value) { done, total ->
                scanProgress.value = done to total
            }
            scanProgress.value = null
            refreshIndex()
        }
    }

    fun openLabel(label: String) {
        selectedLabel.value = label
        viewModelScope.launch {
            val ids = indexer.idsForLabel(label)
            labelItems.value = allMedia.value.filter { it.id in ids }
        }
    }

    fun closeLabel() {
        selectedLabel.value = null
    }
}
