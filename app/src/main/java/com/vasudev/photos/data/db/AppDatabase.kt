package com.vasudev.photos.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.OnConflictStrategy
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/* ------------------------------------------------------------------ */
/* Entities                                                            */
/* ------------------------------------------------------------------ */

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val albumId: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "album_photos", primaryKeys = ["albumId", "mediaId"])
data class AlbumPhotoEntity(
    val albumId: Long,
    val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Persistent upload queue for the (future) Zoho cloud backup.
 * Items survive restarts, so anything the user marks "back up" will
 * upload automatically once a Zoho account is connected.
 */
@Entity(tableName = "upload_queue")
data class UploadQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val state: String = "PENDING", // PENDING | IN_PROGRESS | DONE | FAILED
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Items hidden from the gallery behind the private vault. */
@Entity(tableName = "private_media")
data class PrivateMediaEntity(
    @PrimaryKey val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * On-device ML Kit index: labels and face counts per photo.
 * Everything is computed locally and never leaves the phone.
 */
@Entity(tableName = "media_index")
data class MediaIndexEntity(
    @PrimaryKey val mediaId: Long,
    val labels: String, // comma-separated, lowercase
    val faceCount: Int,
    val indexedAt: Long = System.currentTimeMillis()
)

/** Items moved to trash: hidden from the gallery until restored or deleted. */
@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val mediaId: Long,
    val trashedAt: Long = System.currentTimeMillis()
)

/* ------------------------------------------------------------------ */
/* Relations                                                           */
/* ------------------------------------------------------------------ */

data class AlbumWithPhotos(
    @Embedded val album: AlbumEntity,
    @Relation(parentColumn = "albumId", entityColumn = "albumId")
    val photos: List<AlbumPhotoEntity>
)

/* ------------------------------------------------------------------ */
/* DAOs                                                                */
/* ------------------------------------------------------------------ */

@Dao
interface AlbumDao {
    @Transaction
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun albums(): Flow<List<AlbumWithPhotos>>

    @Insert
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Query("DELETE FROM albums WHERE albumId = :albumId")
    suspend fun deleteAlbum(albumId: Long)

    @Insert
    suspend fun addToAlbum(entries: List<AlbumPhotoEntity>)

    @Query("DELETE FROM album_photos WHERE albumId = :albumId AND mediaId IN (:mediaIds)")
    suspend fun removeFromAlbum(albumId: Long, mediaIds: List<Long>)
}

@Dao
interface FavoriteDao {
    @Query("SELECT mediaId FROM favorites")
    fun favorites(): Flow<List<Long>>

    @Query("SELECT mediaId FROM favorites")
    suspend fun all(): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    suspend fun isFavorite(mediaId: Long): Boolean

    @Insert
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun remove(mediaId: Long)
}

@Dao
interface UploadQueueDao {
    @Insert
    suspend fun insert(item: UploadQueueEntity): Long

    @Query("SELECT * FROM upload_queue WHERE state = 'PENDING' ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextPending(): UploadQueueEntity?

    @Query("UPDATE upload_queue SET state = 'PENDING' WHERE state = 'IN_PROGRESS'")
    suspend fun resetStale()

    @Query("UPDATE upload_queue SET state = :state WHERE id = :id")
    suspend fun setState(id: Long, state: String)

    @Query("UPDATE upload_queue SET state = 'FAILED', lastError = :error, attempts = attempts + 1 WHERE id = :id")
    suspend fun markFailed(id: Long, error: String?)

    @Query("SELECT COUNT(*) FROM upload_queue WHERE state = 'PENDING'")
    fun pendingCount(): Flow<Int>
}

@Dao
interface PrivateMediaDao {
    @Insert
    suspend fun insert(entity: PrivateMediaEntity)

    @Query("DELETE FROM private_media WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: Long)

    @Query("SELECT mediaId FROM private_media")
    fun allIds(): Flow<List<Long>>

    @Query("SELECT mediaId FROM private_media")
    suspend fun all(): List<Long>
}

@Dao
interface MediaIndexDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaIndexEntity>)

    @Query("SELECT * FROM media_index")
    suspend fun all(): List<MediaIndexEntity>

    @Query("SELECT COUNT(*) FROM media_index")
    suspend fun count(): Int
}

@Dao
interface TrashDao {
    @Insert
    suspend fun insert(entity: TrashEntity)

    @Query("SELECT * FROM trash")
    suspend fun allEntities(): List<TrashEntity>

    @Query("DELETE FROM trash WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: Long)

    @Query("SELECT mediaId FROM trash")
    suspend fun all(): List<Long>

    @Query("SELECT COUNT(*) FROM trash")
    fun count(): Flow<Int>
}

/* ------------------------------------------------------------------ */
/* Database                                                            */
/* ------------------------------------------------------------------ */

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `private_media` " +
                "(`mediaId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`mediaId`))"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `trash` " +
                "(`mediaId` INTEGER NOT NULL, `trashedAt` INTEGER NOT NULL, PRIMARY KEY(`mediaId`))"
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `media_index` " +
                "(`mediaId` INTEGER NOT NULL, `labels` TEXT NOT NULL, " +
                "`faceCount` INTEGER NOT NULL, `indexedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`mediaId`))"
        )
    }
}

@Database(
    entities = [
        AlbumEntity::class,
        AlbumPhotoEntity::class,
        FavoriteEntity::class,
        UploadQueueEntity::class,
        PrivateMediaEntity::class,
        TrashEntity::class,
        MediaIndexEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun uploadQueueDao(): UploadQueueDao
    abstract fun privateMediaDao(): PrivateMediaDao
    abstract fun trashDao(): TrashDao
    abstract fun mediaIndexDao(): MediaIndexDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "zoho_photos.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
