# Gallery — Jetpack Compose Photo & Video Gallery

A complete, fully-functional local gallery app built with Kotlin + Jetpack Compose.
It reads every photo, screenshot, and video already on the device (no cloud, no
custom camera) and lets you browse, view, and manage them.

## Features

- **All / Photos / Videos / Screenshots tabs** — browse everything in one place
  or filter by type. Screenshots are auto-detected from their folder/name.
- **Albums** — automatically grouped by device folder (Camera, WhatsApp,
  Downloads, Screenshots, etc.), with cover thumbnail and item count.
- **Full-screen viewer**
  - Swipe left/right between items (Compose `HorizontalPager`)
  - Pinch-to-zoom and pan on photos
  - Native video playback with controls (Media3 ExoPlayer)
  - Share via the system share sheet
  - Delete (handles Android 10+ scoped-storage confirmation dialog)
  - Details dialog: file name, date, size, resolution, folder, duration, MIME type
- **Runtime permission handling** for Android 13+ (`READ_MEDIA_IMAGES` /
  `READ_MEDIA_VIDEO`) and Android 12 and below (`READ_EXTERNAL_STORAGE`), with
  a friendly rationale screen.
- Thumbnail grid with video-duration badges, built on Coil (with video-frame
  decoding support).

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Navigation Compose (single-Activity, multi-screen)
- MVVM: `GalleryViewModel` + `MediaRepository` (raw `ContentResolver` / MediaStore
  queries — no database needed since MediaStore *is* the source of truth)
- Coil (image + video-frame thumbnails)
- Media3 ExoPlayer (video playback)
- Accompanist Permissions (runtime permission requests)

No advanced/experimental architecture (no Room, no DI framework, no paging
library) — kept intentionally simple while still being fully featured.

## Opening the project

1. Open Android Studio (Koala or newer recommended).
2. **File > Open** → select the `GalleryApp` folder.
3. Let Gradle sync (Android Studio will generate the wrapper jar automatically
   the first time if it's missing; it uses the Gradle 8.7 distribution
   declared in `gradle/wrapper/gradle-wrapper.properties`).
4. Run on a device or emulator running Android 7.0 (API 24) or later.
5. On first launch, grant photo/video access when prompted.

## Project layout

```
app/src/main/java/com/example/gallery/
├── MainActivity.kt
├── data/
│   ├── MediaItem.kt        # MediaItem + Album models
│   └── MediaRepository.kt  # MediaStore queries, delete handling
├── viewmodel/
│   └── GalleryViewModel.kt # UI state, tab filtering, delete flow
└── ui/
    ├── theme/              # Color, Type, Theme
    ├── navigation/
    │   └── NavGraph.kt     # home / albums / album detail / viewer routes
    ├── components/
    │   ├── MediaGrid.kt        # thumbnail grid + video badge
    │   └── PermissionGate.kt   # runtime permission UX
    └── screens/
        ├── GalleryHomeScreen.kt
        ├── AlbumsScreen.kt
        ├── AlbumDetailScreen.kt
        └── MediaViewerScreen.kt
```

## Notes

- Minimum SDK 24, target/compile SDK 34.
- Deleting a file on Android 10+ may show a system confirmation dialog
  (required by scoped storage) — this is handled automatically via
  `IntentSenderRequest`.
- The launcher icon is a simple vector drawable so the project builds without
  needing generated PNG mipmap sets.
