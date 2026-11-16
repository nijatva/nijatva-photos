package com.nijatva.photos.ui.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.data.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Serves both album routes:
 *  - "album/{albumId}" -> a user-created album (Room)
 *  - "bucket/{bucket}"  -> an auto album for a device folder (MediaStore bucket)
 */
class AlbumDetailViewModel(
    private val handle: SavedStateHandle,
    private val repository: PhotoRepository
) : ViewModel() {

    // Navigation delivers path args as Strings; accept either to be safe.
    val albumId: Long = when (val v = handle.get<Any>("albumId")) {
        is Long -> v
        is String -> v.toLongOrNull() ?: -1L
        else -> -1L
    }
    val bucket: String? = handle.get<String>("bucket")
    val isUserAlbum: Boolean = bucket == null

    private val _allMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val allMedia: StateFlow<List<MediaItem>> = _allMedia

    private val _media = MutableStateFlow<List<MediaItem>>(emptyList())
    val media: StateFlow<List<MediaItem>> = _media

    val mediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val title = MutableStateFlow(bucket ?: "")

    val favorites = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            _allMedia.value = repository.refresh()
            if (isUserAlbum) {
                repository.userAlbumsFlow.collect { albums ->
                    val match = albums.firstOrNull { it.album.albumId == albumId } ?: return@collect
                    val ids = match.mediaIds.toSet()
                    title.value = match.album.name
                    mediaIds.value = ids
                    _media.value = _allMedia.value.filter { it.id in ids }
                }
            } else {
                // Pinned pseudo-folders + real device folders.
                combine(_allMedia, favorites) { all, favs ->
                    when (bucket) {
                        "Videos" -> all.filter { it.isVideo }
                        "Screenshots" -> all.filter {
                            it.bucketName?.contains("screenshot", ignoreCase = true) == true
                        }
                        "Favorites" -> all.filter { it.id in favs }
                        "GIFs" -> all.filter { it.mimeType == "image/gif" }
                        else -> all.filter { it.bucketName == bucket }
                    }
                }.collect { _media.value = it }
            }
        }
    }

    fun updateMembers(add: List<Long>, remove: List<Long>) {
        viewModelScope.launch {
            if (add.isNotEmpty()) repository.addToAlbum(albumId, add)
            if (remove.isNotEmpty()) repository.removeFromAlbum(albumId, remove)
        }
    }
}
