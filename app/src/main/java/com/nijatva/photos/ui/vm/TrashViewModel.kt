package com.nijatva.photos.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.data.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** A trash entry plus how many of its 30 retention days are left. */
data class TrashEntry(val item: MediaItem, val daysLeft: Int)

class TrashViewModel(private val repository: PhotoRepository) : ViewModel() {

    val items = MutableStateFlow<List<TrashEntry>>(emptyList())

    fun load() {
        viewModelScope.launch {
            val day = 24L * 60 * 60 * 1000
            items.value = repository.trashEntries().map { (item, trashedAt) ->
                val elapsedDays = (System.currentTimeMillis() - trashedAt) / day
                TrashEntry(item, (30 - elapsedDays).toInt().coerceAtLeast(0))
            }
        }
    }

    fun restore(mediaId: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(mediaId)
            items.value = items.value.filterNot { it.item.id == mediaId }
        }
    }

    /** Called after the file was permanently deleted from the MediaStore. */
    fun onDeletedForever(mediaId: Long) {
        viewModelScope.launch {
            repository.removeFromTrash(mediaId)
            items.value = items.value.filterNot { it.item.id == mediaId }
        }
    }
}

class CleanupViewModel(private val repository: PhotoRepository) : ViewModel() {

    val duplicateGroups = MutableStateFlow<List<List<MediaItem>>>(emptyList())
    val largeFiles = MutableStateFlow<List<MediaItem>>(emptyList())

    fun load() {
        viewModelScope.launch {
            duplicateGroups.value = repository.duplicateGroups()
            largeFiles.value = repository.largeFiles()
        }
    }

    fun trash(mediaId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(mediaId)
            duplicateGroups.value = duplicateGroups.value
                .map { group -> group.filterNot { it.id == mediaId } }
                .filter { it.size > 1 }
            largeFiles.value = largeFiles.value.filterNot { it.id == mediaId }
        }
    }
}
