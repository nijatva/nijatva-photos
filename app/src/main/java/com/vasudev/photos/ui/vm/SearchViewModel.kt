package com.vasudev.photos.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vasudev.photos.data.MediaItem
import com.vasudev.photos.data.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

enum class SearchFilter { ALL, FAVORITES, VIDEOS, GIFS, SCREENSHOTS }

/** A chronological group for the Times row on the Search tab. */
data class TimeGroup(val label: String, val coverUri: String?, val items: List<MediaItem>)

class SearchViewModel(private val repository: PhotoRepository) : ViewModel() {

    private val media = MutableStateFlow<List<MediaItem>>(emptyList())

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(SearchFilter.ALL)
    val times = MutableStateFlow<List<TimeGroup>>(emptyList())

    val favorites = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val results: StateFlow<List<MediaItem>> =
        combine(media, query, filter, favorites) { all, q, f, favs ->
            val base = when (f) {
                SearchFilter.ALL -> all
                SearchFilter.FAVORITES -> all.filter { it.id in favs }
                SearchFilter.VIDEOS -> all.filter { it.isVideo }
                SearchFilter.GIFS -> all.filter { it.mimeType == "image/gif" }
                SearchFilter.SCREENSHOTS -> all.filter {
                    it.bucketName?.contains("screenshot", ignoreCase = true) == true
                }
            }
            val trimmed = q.trim()
            if (trimmed.isEmpty()) base
            else base.filter { it.displayName.contains(trimmed, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            media.value = repository.refresh()
            times.value = computeTimes(media.value)
        }
    }

    private fun computeTimes(all: List<MediaItem>): List<TimeGroup> {
        if (all.isEmpty()) return emptyList()
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()
        val result = mutableListOf<TimeGroup>()

        val lastYear = all.filter {
            Instant.ofEpochMilli(it.dateTakenMillis).atZone(zone).year == now.year - 1
        }
        if (lastYear.isNotEmpty()) {
            result += TimeGroup("Last year", lastYear.first().uri, lastYear)
        }

        var cursor = LocalDate.of(now.year, now.month, 1)
        repeat(6) {
            val items = all.filter {
                val date = Instant.ofEpochMilli(it.dateTakenMillis).atZone(zone).toLocalDate()
                date.year == cursor.year && date.month == cursor.month
            }
            if (items.isNotEmpty()) {
                val label = cursor.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) +
                    " " + cursor.year
                result += TimeGroup(label, items.first().uri, items)
            }
            cursor = cursor.minusMonths(1)
        }
        return result
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun setFilter(value: SearchFilter) {
        filter.value = value
    }
}
