package com.nijatva.photos.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.data.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class VaultViewModel(private val repository: PhotoRepository) : ViewModel() {

    val items = MutableStateFlow<List<MediaItem>>(emptyList())

    fun load() {
        viewModelScope.launch { items.value = repository.vaultItems() }
    }

    fun restore(mediaId: Long) {
        viewModelScope.launch {
            repository.restoreFromVault(mediaId)
            items.value = items.value.filterNot { it.id == mediaId }
        }
    }
}
