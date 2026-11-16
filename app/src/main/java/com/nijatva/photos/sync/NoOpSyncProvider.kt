package com.nijatva.photos.sync

/**
 * The provider active while no Zoho account is connected.
 * Everything stays on the device; sync attempts report [SyncResult.NotConfigured].
 */
class NoOpSyncProvider : CloudSyncProvider {

    override val providerId: String = "local-only"
    override val label: String = "Local only (Zoho account not connected)"

    override suspend fun isConfigured(): Boolean = false

    override suspend fun authenticate(credentials: SyncCredentials): SyncAccount =
        throw UnsupportedOperationException("No cloud provider is connected in this version.")

    override suspend fun disconnect() = Unit

    override suspend fun upload(mediaId: Long): SyncResult = SyncResult.NotConfigured

    override suspend fun availableQuotaBytes(): Long? = null
}
