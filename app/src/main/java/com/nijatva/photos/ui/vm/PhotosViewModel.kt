package com.nijatva.photos.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.data.Memory
import com.nijatva.photos.data.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotosViewModel(private val repository: PhotoRepository) : ViewModel() {

    private val _media = MutableStateFlow<List<MediaItem>>(emptyList())
    val media: StateFlow<List<MediaItem>> = _media.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _memories = MutableStateFlow<List<Memory>>(emptyList())
    val memories: StateFlow<List<Memory>> = _memories.asStateFlow()

    val favorites: StateFlow<Set<Long>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _media.value = repository.refresh()
            _memories.value = repository.memories()
            _loading.value = false
        }
    }

    fun toggleFavorite(mediaId: Long) {
        viewModelScope.launch { repository.toggleFavorite(mediaId) }
    }
}
