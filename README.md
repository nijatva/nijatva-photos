# Zoho Photos

A **local-first, Google Photos-style gallery for Android**. Your photos and videos
stay on your device — nothing is uploaded, and no account is needed.

**Zoho account support is designed in but switched off.** A future version will
connect a Zoho account for cloud backup; the sync engine, provider interface and
persistent upload queue are already in place (see "Zoho integration roadmap" below).

## Features (v1.0.0)

- **Photos tab** — full gallery of photos and videos in a date-grouped grid, newest first
- **Memories** — "1 year ago / N years ago" cards for photos taken on today's date in past years
- **Albums** — create your own albums, plus automatic "Folders on device" (Camera, Screenshots, Downloads, WhatsApp, etc.)
- **Search** — search by file name with quick filters (All / Favorites / Videos)
- **Favorites** — mark photos as favorites (stored in a local database)
- **Full-screen viewer** — swipe through photos, favorite, share, view details, add to album
- **Share** — standard Android share sheet (any app: WhatsApp, Gmail, etc.)
- **Backup queue** — tap the cloud icon on a photo to queue it for backup; it uploads
  automatically once a Zoho account is connected in a future version
- **Local-first by design** — media is read in place from MediaStore; never copied or modified

## Tech stack

- Kotlin, Jetpack Compose (Material 3, dynamic color)
- MediaStore API for photos/videos (scoped storage compliant)
- Room database (albums, favorites, upload queue)
- Coil for image loading
- Navigation Compose, ViewModel + StateFlow (MVVM)

## Requirements

- **Android Studio** Ladybug (2024.1.1) or newer
- JDK 17 (bundled with Android Studio)
- An Android device or emulator running **Android 8.0 (API 26)** or newer

## Build & run

1. Unzip this project.
2. In Android Studio: **File → Open…** and select the `ZohoPhotos` folder.
3. Let Gradle sync finish (first sync downloads dependencies — if Android Studio
   prompts about a missing Gradle wrapper, accept its offer to fix/use a default Gradle 8.7).
4. Connect your phone with USB debugging enabled (or start an emulator).
5. Press **Run**.

To build an APK from the command line instead:

```bash
gradle wrapper          # once, if you have Gradle installed
./gradlew assembleDebug
# APK lands in app/build/outputs/apk/debug/
```

> Note: this project was generated without a Gradle wrapper JAR, so building from
> Android Studio (which provisions Gradle itself) is the easiest path.

## Permissions

- Android 13+: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` (granular media access)
- Android 8–12: `READ_EXTERNAL_STORAGE`
- `INTERNET` is declared for the future Zoho sync but is unused in v1.

## Project structure

```
app/src/main/java/com/zoho/photos/
├── ZohoPhotosApplication.kt      # App + container wiring
├── MainActivity.kt
├── data/
│   ├── MediaItem.kt               # Photo/video model, Memory model
│   ├── MediaStoreDataSource.kt    # Reads device photos & videos
│   ├── PhotoRepository.kt         # Combines media + local metadata
│   ├── AppContainer.kt            # Manual DI container
│   └── db/AppDatabase.kt          # Room: albums, favorites, upload queue
├── sync/                          # ← Zoho-ready sync layer
│   ├── CloudSyncProvider.kt      # Provider interface (pluggable)
│   ├── SyncEngine.kt             # Queue draining + status
│   ├── NoOpSyncProvider.kt       # Active provider (local only)
│   └── zoho/ZohoSyncProvider.kt  # Placeholder for the real Zoho integration
└── ui/
    ├── ZohoPhotosRoot.kt          # Navigation + bottom bar
    ├── theme/Theme.kt
    ├── components/PhotoGrid.kt    # Grid, day headers, memories cards
    ├── vm/                        # ViewModels + factories
    └── screens/                   # Photos, Albums, AlbumDetail,
                                   # PhotoPager, Search, Settings
```

## Zoho integration roadmap (future version)

The app is deliberately architected so adding a Zoho account needs **no UI rework**:

1. **Implement `sync/zoho/ZohoSyncProvider.kt`** — the file documents the exact
   steps (Zoho Accounts OAuth token exchange, storage backend choice such as
   Zoho WorkDrive, chunked upload for large videos).
2. **Add a sign-in screen** and store tokens in EncryptedSharedPreferences.
3. **After sign-in**, call:
   ```kotlin
   app.container.syncEngine.setProvider(ZohoSyncProvider())
   engine.syncNow() // drains everything queued so far
   ```
4. Items the user already marked "back up" (the cloud icon in the viewer) sit in
   the persistent `upload_queue` table and upload automatically at this point.
5. Later: album sync, shared albums, trash, and remote quota display can all be
   added through the same `CloudSyncProvider` interface.

## Roadmap ideas

- Pinch-to-zoom and basic editing (crop, filters)
- On-device semantic search (ML Kit image labeling — still private to the device)
- People & places grouping (on-device face clustering)
- Trash and archiving
- Multi-select in the main grid for bulk album/favorite actions

## Notes

- This is a personal-use project. `applicationId` is `com.zoho.photos`; if you
  ever publish it, change the id to something you own (e.g. `com.yourname.zohophotos`)
  to avoid conflicts with Zoho Corporation's own apps and trademarks.
