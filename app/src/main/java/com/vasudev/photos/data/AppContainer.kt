package com.vasudev.photos.data

import android.content.Context
import com.vasudev.photos.data.db.AppDatabase
import com.vasudev.photos.sync.NoOpSyncProvider
import com.vasudev.photos.sync.SyncEngine

/**
 * Manual dependency container for the app.
 *
 * Note the sync engine is deliberately built with [NoOpSyncProvider]:
 * no Zoho account is used in this version. When Zoho support is added,
 * swap the provider after sign-in via [SyncEngine.setProvider].
 */
class AppContainer(context: Context) {

    val database: AppDatabase = AppDatabase.build(context)

    val repository: PhotoRepository = PhotoRepository(
        mediaSource = MediaStoreDataSource(context),
        db = database
    )

    val smartIndexer: SmartIndexer = SmartIndexer(context, database)


    val syncEngine: SyncEngine = SyncEngine(
        queueDao = database.uploadQueueDao(),
        provider = NoOpSyncProvider()
    )
}
