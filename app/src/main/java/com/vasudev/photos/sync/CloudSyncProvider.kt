package com.vasudev.photos.sync

/**
 * A cloud backup target. The app ships with [NoOpSyncProvider] (local only).
 * When Zoho account support arrives, [com.vasudev.photos.sync.zoho.ZohoSyncProvider]
 * implements this same interface and is swapped in via [SyncEngine.setProvider] —
 * no other code needs to change.
 */
interface CloudSyncProvider {
    val providerId: String
    val label: String

    /** True when a valid, non-expired account session exists. */
    suspend fun isConfigured(): Boolean

    /** Exchange OAuth credentials for a signed-in [SyncAccount]. */
    suspend fun authenticate(credentials: SyncCredentials): SyncAccount

    /** Sign out and clear stored tokens. */
    suspend fun disconnect()

    /**
     * Upload one item from MediaStore (the provider resolves the content
     * URI itself from the media id) and return the outcome.
     */
    suspend fun upload(mediaId: Long): SyncResult

    /** Remaining cloud storage in bytes, or null if unknown. */
    suspend fun availableQuotaBytes(): Long?
}

data class SyncCredentials(
    val clientId: String,
    val clientSecret: String,
    val accessToken: String,
    val refreshToken: String? = null,
    val apiBaseUrl: String? = null
)

data class SyncAccount(
    val providerId: String,
    val displayName: String,
    val email: String?
)

sealed interface SyncResult {
    data object Success : SyncResult
    data object NotConfigured : SyncResult
    data class Error(val message: String) : SyncResult
}
