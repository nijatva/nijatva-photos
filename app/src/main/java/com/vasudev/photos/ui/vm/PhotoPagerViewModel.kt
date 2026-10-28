package com.vasudev.photos.ui.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vasudev.photos.data.MediaItem
import com.vasudev.photos.data.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoPagerViewModel(
    private val handle: SavedStateHandle,
    private val repository: PhotoRepository
) : ViewModel() {

    // Navigation delivers path args as Strings; accept either to be safe.
    private val startId: Long = when (val v = handle.get<Any>("mediaId")) {
        is Long -> v
        is String -> v.toLongOrNull() ?: -1L
        else -> -1L
    }

    val media = MutableStateFlow<List<MediaItem>>(emptyList())

    val favorites = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val userAlbums = repository.userAlbumsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Index of the tapped photo; evaluated after media loads (pager waits for it). */
    val initialIndex: Int
        get() = media.value.indexOfFirst { it.id == startId }.coerceAtLeast(0)

    init {
        viewModelScope.launch { media.value = repository.refresh() }
    }

    fun refreshMedia() {
        viewModelScope.launch { media.value = repository.refresh() }
    }

    fun toggleFavorite(mediaId: Long) {
        viewModelScope.launch { repository.toggleFavorite(mediaId) }
    }

    fun addToAlbum(albumId: Long, mediaId: Long) {
        viewModelScope.launch { repository.addToAlbum(albumId, listOf(mediaId)) }
    }

    /** Removes the item from every other user album, then adds it to the target. */
    fun moveToAlbum(targetAlbumId: Long, mediaId: Long) {
        viewModelScope.launch {
            val others = userAlbums.value
                .filter { it.mediaIds.contains(mediaId) && it.album.albumId != targetAlbumId }
                .map { it.album.albumId }
            others.forEach { repository.removeFromAlbum(it, listOf(mediaId)) }
            repository.addToAlbum(targetAlbumId, listOf(mediaId))
        }
    }

    suspend fun createAlbumAndAdd(name: String, mediaId: Long, move: Boolean = false) {
        val albumId = repository.createAlbum(name)
        if (move) {
            userAlbums.value
                .filter { it.mediaIds.contains(mediaId) }
                .forEach { repository.removeFromAlbum(it.album.albumId, listOf(mediaId)) }
        }
        repository.addToAlbum(albumId, listOf(mediaId))
    }

    fun makePrivate(mediaId: Long) {
        viewModelScope.launch {
            repository.makePrivate(mediaId)
            media.value = media.value.filterNot { it.id == mediaId }
        }
    }

    fun moveToTrash(mediaId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(mediaId)
            media.value = media.value.filterNot { it.id == mediaId }
        }
    }
}
