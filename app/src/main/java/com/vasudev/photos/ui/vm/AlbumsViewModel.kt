package com.vasudev.photos.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vasudev.photos.data.MediaItem
import com.vasudev.photos.data.PhotoRepository
import com.vasudev.photos.data.UserAlbum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumsViewModel(private val repository: PhotoRepository) : ViewModel() {

    private val _media = MutableStateFlow<List<MediaItem>>(emptyList())
    val media: StateFlow<List<MediaItem>> = _media.asStateFlow()

    val userAlbums: StateFlow<List<UserAlbum>> = repository.userAlbumsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<Set<Long>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val trashCount: StateFlow<Int> = repository.trashCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _media.value = repository.refresh() }
    }

    fun createAlbum(name: String) {
        viewModelScope.launch { repository.createAlbum(name) }
    }

    fun deleteAlbum(albumId: Long) {
        viewModelScope.launch { repository.deleteAlbum(albumId) }
    }
}
