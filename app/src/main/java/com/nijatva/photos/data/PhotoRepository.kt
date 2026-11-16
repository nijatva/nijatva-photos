package com.nijatva.photos.data

import com.nijatva.photos.data.db.AlbumEntity
import com.nijatva.photos.data.db.AlbumPhotoEntity
import com.nijatva.photos.data.db.AppDatabase
import com.nijatva.photos.data.db.FavoriteEntity
import com.nijatva.photos.data.db.PrivateMediaEntity
import com.nijatva.photos.data.db.TrashEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single source of truth for the app.
 * Combines device media (MediaStore) with local metadata (Room):
 * favorites, user albums, private vault and "on this day" memories.
 */
class PhotoRepository(
    private val mediaSource: MediaStoreDataSource,
    private val db: AppDatabase
) {
    /** All media, unfiltered (used by the vault). */
    private var allCached: List<MediaItem> = emptyList()

    /** Media visible in the gallery (private vault items excluded). */
    private var cachedMedia: List<MediaItem> = emptyList()

    val favoritesFlow: Flow<Set<Long>> = db.favoriteDao().favorites().map { it.toSet() }

    val trashCountFlow: Flow<Int> = db.trashDao().count()

    val userAlbumsFlow: Flow<List<UserAlbum>> = db.albumDao().albums().map { list ->
        list.map { awp -> UserAlbum(awp.album, awp.photos.map { it.mediaId }) }
    }

    suspend fun refresh(): List<MediaItem> = withContext(Dispatchers.IO) {
        val hidden = db.privateMediaDao().all().toSet() + db.trashDao().all().toSet()
        allCached = mediaSource.loadMedia()
        cachedMedia = allCached.filter { it.id !in hidden }
        cachedMedia
    }

    /** Items currently locked in the private vault. */
    suspend fun vaultItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        val base = allCached.ifEmpty { mediaSource.loadMedia().also { allCached = it } }
        val private = db.privateMediaDao().all().toSet()
        base.filter { it.id in private }
    }

    suspend fun makePrivate(mediaId: Long) = withContext(Dispatchers.IO) {
        db.privateMediaDao().insert(PrivateMediaEntity(mediaId))
    }

    suspend fun restoreFromVault(mediaId: Long) = withContext(Dispatchers.IO) {
        db.privateMediaDao().delete(mediaId)
    }

    /* ---- Trash ---- */

    suspend fun moveToTrash(mediaId: Long) = withContext(Dispatchers.IO) {
        db.trashDao().insert(TrashEntity(mediaId))
    }

    suspend fun restoreFromTrash(mediaId: Long) = withContext(Dispatchers.IO) {
        db.trashDao().delete(mediaId)
    }

    /** Drops the trash row only (used after the file itself is deleted for real). */
    suspend fun removeFromTrash(mediaId: Long) = withContext(Dispatchers.IO) {
        db.trashDao().delete(mediaId)
    }

    /** Trash entries with their trashedAt timestamp (for the 30-day countdown). */
    suspend fun trashEntries(): List<Pair<MediaItem, Long>> = withContext(Dispatchers.IO) {
        val base = allCached.ifEmpty { mediaSource.loadMedia().also { allCached = it } }
        val byId = base.associateBy { it.id }
        db.trashDao().allEntities()
            .mapNotNull { entity ->
                byId[entity.mediaId]?.let { it to entity.trashedAt }
            }
            .sortedByDescending { it.second }
    }

    suspend fun trashItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        val base = allCached.ifEmpty { mediaSource.loadMedia().also { allCached = it } }
        val trashed = db.trashDao().all().toSet()
        base.filter { it.id in trashed }.sortedByDescending { it.id }
    }

    /* ---- Cleanup suggestions (no ML: exact-size heuristics) ---- */

    /** Groups of files with identical type, size and dimensions — likely duplicates. */
    suspend fun duplicateGroups(): List<List<MediaItem>> = withContext(Dispatchers.IO) {
        val base = cachedMedia.ifEmpty { refresh() }
        base.groupBy { Triple(it.mimeType, it.sizeBytes, it.width to it.height) }
            .filter { it.value.size > 1 && it.key.second > 0L }
            .map { it.value.sortedByDescending { it.dateTakenMillis } }
    }

    /** The largest files on the device (videos and photos). */
    suspend fun largeFiles(): List<MediaItem> = withContext(Dispatchers.IO) {
        val base = cachedMedia.ifEmpty { refresh() }
        base.sortedByDescending { it.sizeBytes }.take(10)
    }

    suspend fun toggleFavorite(mediaId: Long) = withContext(Dispatchers.IO) {
        val dao = db.favoriteDao()
        if (dao.isFavorite(mediaId)) dao.remove(mediaId)
        else dao.add(FavoriteEntity(mediaId))
    }

    suspend fun createAlbum(name: String): Long = withContext(Dispatchers.IO) {
        db.albumDao().insertAlbum(AlbumEntity(name = name))
    }

    suspend fun deleteAlbum(albumId: Long) = withContext(Dispatchers.IO) {
        db.albumDao().deleteAlbum(albumId)
    }

    suspend fun addToAlbum(albumId: Long, mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        db.albumDao().addToAlbum(
            mediaIds.map { AlbumPhotoEntity(albumId = albumId, mediaId = it) }
        )
    }

    suspend fun removeFromAlbum(albumId: Long, mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        db.albumDao().removeFromAlbum(albumId, mediaIds)
    }

    /** Groups photos taken on today's month/day in previous years. */
    suspend fun memories(today: LocalDate = LocalDate.now()): List<Memory> =
        withContext(Dispatchers.IO) {
            val media = cachedMedia.ifEmpty { refresh() }
            val zone = ZoneId.systemDefault()
            media.mapNotNull { item ->
                val date = Instant.ofEpochMilli(item.dateTakenMillis).atZone(zone).toLocalDate()
                if (date.month == today.month && date.dayOfMonth == today.dayOfMonth &&
                    date.year < today.year
                ) item to (today.year - date.year) else null
            }
                .groupBy({ it.second }, { it.first })
                .map { (yearsAgo, items) ->
                    Memory(
                        yearsAgo = yearsAgo,
                        title = if (yearsAgo == 1) "1 year ago" else "$yearsAgo years ago",
                        items = items.sortedByDescending { it.dateTakenMillis }
                    )
                }
                .sortedBy { it.yearsAgo }
        }
}

data class UserAlbum(
    val album: AlbumEntity,
    val mediaIds: List<Long>
)
