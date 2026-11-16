package com.nijatva.photos.sync

import com.nijatva.photos.data.db.UploadQueueDao
import com.nijatva.photos.data.db.UploadQueueEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object InProgress : SyncStatus
    data class NotConfigured(val providerLabel: String) : SyncStatus
    data class Done(val uploaded: Int) : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

/**
 * Drives backup uploads through whichever [CloudSyncProvider] is active.
 *
 * Design (Zoho-ready, switched off for now):
 *  - The active provider starts as [NoOpSyncProvider], so every sync attempt
 *    cleanly reports "not configured" and media stays on-device only.
 *  - Uploads are persisted in the Room `upload_queue` table, so anything the
 *    user marks "back up" is remembered across restarts and drains
 *    automatically once a real Zoho account is connected.
 *  - [setProvider] swaps in the real Zoho implementation in a future version
 *    without touching any screen code.
 */
class SyncEngine(
    private val queueDao: UploadQueueDao,
    private var provider: CloudSyncProvider
) {
    private val mutex = Mutex()

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    val providerLabel: String get() = provider.label

    fun pendingCountFlow(): Flow<Int> = queueDao.pendingCount()

    /** Swap the active provider (e.g. to ZohoSyncProvider after sign-in). */
    fun setProvider(newProvider: CloudSyncProvider) {
        provider = newProvider
        _status.value = SyncStatus.Idle
    }

    /** Queue one item for backup and attempt a sync immediately. */
    suspend fun enqueue(mediaId: Long): SyncResult {
        queueDao.insert(UploadQueueEntity(mediaId = mediaId))
        return syncNow()
    }

    /** Drain the pending upload queue through the active provider. */
    suspend fun syncNow(): SyncResult = mutex.withLock {
        if (!provider.isConfigured()) {
            _status.value = SyncStatus.NotConfigured(provider.label)
            return SyncResult.NotConfigured
        }

        _status.value = SyncStatus.InProgress
        queueDao.resetStale() // rows stuck IN_PROGRESS after a crash become PENDING again
        var uploaded = 0
        try {
            while (true) {
                val next = queueDao.nextPending() ?: break
                queueDao.setState(next.id, "IN_PROGRESS")
                when (val result = provider.upload(next.mediaId)) {
                    is SyncResult.Success -> {
                        queueDao.setState(next.id, "DONE")
                        uploaded++
                    }
                    is SyncResult.Error -> {
                        queueDao.markFailed(next.id, result.message)
                        _status.value = SyncStatus.Failed(result.message)
                        return result
                    }
                    SyncResult.NotConfigured -> {
                        queueDao.setState(next.id, "PENDING")
                        _status.value = SyncStatus.NotConfigured(provider.label)
                        return SyncResult.NotConfigured
                    }
                }
            }
            _status.value = SyncStatus.Done(uploaded)
            return SyncResult.Success
        } catch (e: Exception) {
            _status.value = SyncStatus.Failed(e.message ?: "Unknown sync error")
            return SyncResult.Error(e.message ?: "Unknown sync error")
        }
    }
}
