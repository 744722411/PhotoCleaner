# PhotoCleaner - Local Photo Cleanup Assistant

> Chinese documentation: [README_CN.md](README_CN.md)

---

> ## Disclaimer
>
> This is a personal hobby project, not a commercial product.
>
> - Back up important photos before using this app.
> - Review every cleanup action carefully before confirming deletion.
> - The developer is not responsible for data loss.
> - Use at your own risk.

---

## Overview

PhotoCleaner is an Android photo cleanup assistant focused on local, on-device analysis. It scans selected folders, detects obvious low-quality or low-value photos, groups visually similar shots, and gives you a review screen before anything is removed.

Version 1.8.0 improves the scoped scanning flow: selected folders are now honored end to end, scan progress is persisted after each processed item, pause/stop behavior is clearer, partial photo access is surfaced in the UI, and the Settings screen now controls scan batch size and whether existing indexed photos should be re-analyzed.

The app remains fully offline. It does not store API keys, does not include Retrofit/OkHttp/Moshi networking code, and explicitly removes the `INTERNET` permission from the merged manifest.

## What's New in v1.8.0

- Fixed selected-directory scanning so a scan started from the quick entry only processes the chosen directories.
- Persisted completed scan results incrementally, so stopping a scan keeps already detected photos available on the Review screen.
- Clarified pause vs stop:
  - Pause keeps the current scan job alive and waits until you continue.
  - Stop cancels the current scan job; already completed and saved results are kept.
- Added a real Settings screen with batch size and "re-analyze indexed photos" controls.
- Added Android partial photo access status and reauthorization entry points.
- Updated the scan screen to show selected folder count, estimated photo count, batch limit, and partial-access warnings.
- Reduced the Review header height and typography so it matches the rest of the full-screen layout.
- Migrated saved selected directories from comma-separated text to a typed DataStore string set.

## Features

- Local blur, screenshot, document/receipt, blank-photo, and low-quality photo detection.
- Directory discovery with selectable scan scope.
- Incremental scanning by default, so repeat scans focus on new photos.
- Optional re-analysis of already indexed photos from Settings.
- Adjustable per-run batch size: 100, 500, 2000, or all eligible photos.
- Similar-photo grouping with dHash-based matching.
- Review screen with grid and swipe workflows.
- Batch selection and confirmation before cleanup.
- Undo window before pending deletes are submitted.
- Android 11+ system trash integration via `MediaStore.createTrashRequest`.
- Material 3 dark UI with progress logs and offline privacy status.

## Permissions and Privacy

PhotoCleaner needs read access to photos/media so it can scan your local library. It does not request network access.

Relevant manifest behavior:

- Uses `READ_MEDIA_IMAGES` on Android 13+.
- Uses `READ_MEDIA_VISUAL_USER_SELECTED` on Android 14+ for partial-library access.
- Uses `READ_EXTERNAL_STORAGE` up to Android 12.
- Explicitly removes `INTERNET` and other unrelated permissions that dependencies might try to merge.

If Android grants only partial photo access, the app can only discover and scan the photos selected through the system permission UI. The scan and settings screens show this status and provide a reauthorization button.

## How to Use

1. Install the APK on an Android 8.0+ device.
2. Grant photo/media access when prompted.
3. Open Scan, tap "Discover directories", and choose the folders to include.
4. Choose a batch size. Keep the default 2000 for normal use, use a smaller batch to validate results, or choose "All" for a full pass.
5. Start scanning and watch the local processing log.
6. Use Pause if you want to temporarily hold the current scan and continue later.
7. Use Stop if you want to cancel the current scan. Already completed results remain available on the Review screen.
8. Open Review to inspect suggested cleanup, uncertain items, similar groups, or all photos.
9. Confirm deletes carefully. You can undo before final submission to the system trash flow.

## Technical Details

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.3.21 |
| UI Framework | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.60 |
| Database | Room 2.8.4 |
| Image Loading | Coil 3.5.0 |
| Navigation | Navigation Compose 2.9.8 |
| Local ML | ML Kit Image Labeling 17.0.9 |
| Preferences | DataStore Preferences 1.2.1 |
| Min SDK | 26 |
| Target SDK | 36 |
| Compile SDK | 37 |

## Build

Prerequisites:

- JDK 17
- Android SDK API 37
- Android Gradle Plugin-compatible environment

Debug build:

```bash
./gradlew assembleDebug
```

Release build:

```bash
./gradlew assembleRelease
```

Release signing must be configured separately.

## Project Structure

```text
app/src/main/
├── java/com/photocleaner/
│   ├── data/
│   │   ├── classification/    # Local photo classifiers
│   │   ├── local/             # Room DAO, entities, database
│   │   ├── mapper/            # Entity/domain mapping
│   │   ├── repository/        # Repository implementations
│   │   └── service/           # Trash integration
│   ├── di/                    # Hilt modules
│   ├── domain/
│   │   ├── model/             # Photo, Classification, DirectoryInfo
│   │   ├── repository/        # Repository contracts
│   │   ├── service/           # Classifier contracts
│   │   └── usecase/           # Scan/delete use cases
│   ├── service/               # Shared scan state holder
│   ├── ui/                    # Home, scan, review, stats, settings
│   └── util/                  # Image, blur, screenshot, permission helpers
└── AndroidManifest.xml
```

## License

Personal hobby project. No commercial license is granted.
