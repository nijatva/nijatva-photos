# Nijatva Photos

A local-first Android gallery — your photos stay on your device unless you explicitly share or back them up.

- **App name / launcher label:** Photos
- **Product name:** Nijatva Photos
- **Package:** `com.vasudev.photos`
- **minSdk 26 · targetSdk / compileSdk 34**

## Features

- **Gallery** — Photos / Albums toggle, pinned sections (Camera, Videos, Screenshots, Favourites, Trash, Private), custom section order, sort toggle.
- **Albums** — user albums, bucket albums, featured, media types, pinning.
- **Search** — file names, places, times, media-type filters (GIF, screenshots, videos, documents).
- **On-device AI (ML Kit)** — image labelling, face detection, text recognition (OCR). Nothing leaves the phone.
- **Lens** — visual search and OCR via the bundled MyLensKit, with a floating one-tap Lens button in the viewer.
- **Viewer** — Material 3 viewer with a categorized action sheet (Cloud / Organize / Security / Conversion), video playback, details panel.
- **Photo editor** — the Photalius web editor (fabric.js) merged in-app and hosted in a WebView, so editing needs no separate install. Photos open straight into the canvas and exports are saved to `Pictures/Nijatva Photos`.
- **Private vault** — PIN/biometric-protected private album hidden from the main view.
- **Trash** — 30-day recycle bin with a per-item countdown badge.
- **Sharing** — optional EXIF/metadata stripping before share; PDF conversion.
- **Backup** — Google Photos hand-off (opens Google Photos so you can turn on its own Backup & sync); no account access by this app.

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · Coil (+ coil-video) · ML Kit (image labelling, face detection, text recognition) · androidx.webkit `WebViewAssetLoader` for the embedded editor.

## Build

Requires JDK 17 and the Android SDK (platform 34).

```sh
# point Gradle at your SDK
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Signing

Signing material is **not** in this repository. To produce builds that keep a stable signature (so updates install over an existing install), place `myphotos.keystore` in the project root and a gitignored `keystore.properties` next to it:

```properties
storePassword=...
keyAlias=myphotos
keyPassword=...
```

Without both files the build falls back to the standard Android debug key.

## Notes

- The editor assets under `app/src/main/assets/photalius/` come from the Photalius project (MIT, by Hossein Pira) and are bundled so the editor works fully offline.
