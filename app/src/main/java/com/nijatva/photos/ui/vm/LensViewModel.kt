package com.nijatva.photos.ui.vm

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mylens.kit.MyLens
import com.nijatva.photos.data.MediaItem
import com.nijatva.photos.data.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** One MyLensKit analysis result, mapped for the Compose UI. */
data class LensAnalysis(
    val labels: List<String>,
    val topLabel: String?,
    val faces: Int,
    val ocrText: String?,
    val errors: List<String>
)

/**
 * Lens screen state, powered by MyLensKit:
 *  - on-device analysis via MyLens.analyze (labels, OCR, faces)
 *  - server mode via MyLens.analyzeOnServer (launched from the screen)
 */
class LensViewModel(
    handle: SavedStateHandle,
    private val repository: PhotoRepository,
    private val appContext: Context
) : ViewModel() {

    // Navigation delivers path args as Strings; accept either to be safe.
    private val startId: Long = when (val v = handle.get<Any>("mediaId")) {
        is Long -> v
        is String -> v.toLongOrNull() ?: -1L
        else -> -1L
    }

    val media = MutableStateFlow<List<MediaItem>>(emptyList())
    val selected = MutableStateFlow<MediaItem?>(null)
    val analysis = MutableStateFlow<LensAnalysis?>(null)
    val analyzing = MutableStateFlow(false)

    val favorites = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            media.value = repository.refresh()
            if (startId > 0) {
                media.value.firstOrNull { it.id == startId }?.let { select(it) }
            }
        }
    }

    fun select(item: MediaItem) {
        selected.value = item
        analysis.value = null
        analyzing.value = true
        viewModelScope.launch {
            val result = analyzeWithMyLens(Uri.parse(item.uri))
            analysis.value = result
            analyzing.value = false
        }
    }

    fun pickAnother() {
        selected.value = null
        analysis.value = null
    }

    /** Bridges MyLensKit's main-thread callback into a coroutine. */
    private suspend fun analyzeWithMyLens(uri: Uri): LensAnalysis =
        suspendCancellableCoroutine { cont ->
            MyLens.analyze(appContext, uri) { result ->
                cont.resume(
                    LensAnalysis(
                        labels = result.labels.toList(),
                        topLabel = result.topLabel,
                        faces = result.faces,
                        ocrText = result.ocrText,
                        errors = result.errors.toList()
                    )
                )
            }
        }
}
