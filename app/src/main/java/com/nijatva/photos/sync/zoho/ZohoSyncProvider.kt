package com.nijatva.photos.sync.zoho

import com.nijatva.photos.sync.CloudSyncProvider
import com.nijatva.photos.sync.SyncAccount
import com.nijatva.photos.sync.SyncCredentials
import com.nijatva.photos.sync.SyncResult

/**
 * PLACEHOLDER — future Zoho cloud backup provider.
 *
 * This class exists so the integration surface is already defined and the
 * rest of the app compiles against it. It is NOT wired into [com.nijatva.photos.sync.SyncEngine]
 * yet. When Zoho account support is added, implement the TODOs below and call
 * `SyncEngine.setProvider(ZohoSyncProvider())` after sign-in succeeds.
 *
 * Suggested implementation steps:
 *
 *  1. OAuth 2.0 with Zoho Accounts:
 *     - Register the app at https://api-console.zoho.com to get a Client ID/Secret.
 *     - Run the authorization-code flow against https://accounts.zoho.com/oauth/v2/auth
 *       and exchange the code at https://accounts.zoho.com/oauth/v2/token
 *       (use AppAuth-Android or a Custom Tab; never embed the secret for a
 *       published app — prefer a refresh-token flow).
 *     - Store the access/refresh tokens in EncryptedSharedPreferences.
 *
 *  2. Storage backend — pick one:
 *     - Zoho WorkDrive upload API (per-user folder "Zoho Photos"), or
 *     - a custom Zoho service you control.
 *
 *  3. upload(mediaId):
 *     - Query MediaStore for the content URI by id.
 *     - Stream the file (OkHttp multipart or chunked upload for large videos).
 *     - Record the remote id so re-uploads can be skipped.
 *
 *  4. Deletion / album sync can later reuse the same provider interface.
 */
class ZohoSyncProvider : CloudSyncProvider {

    override val providerId: String = "zoho"
    override val label: String = "Zoho"

    override suspend fun isConfigured(): Boolean = false // TODO: check stored tokens + expiry

    override suspend fun authenticate(credentials: SyncCredentials): SyncAccount {
        // TODO: token exchange / refresh against Zoho Accounts, return the account
        throw NotImplementedError("Zoho account sign-in ships in a future version of Nijatva Photos.")
    }

    override suspend fun disconnect() {
        // TODO: revoke token and clear stored credentials
    }

    override suspend fun upload(mediaId: Long): SyncResult {
        // TODO: resolve MediaStore URI for mediaId and stream it to Zoho storage
        throw NotImplementedError("Zoho backup ships in a future version of Nijatva Photos.")
    }

    override suspend fun availableQuotaBytes(): Long? = null // TODO: query quota endpoint
}
