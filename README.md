# PhotoCleaner - Local Photo Cleanup Assistant

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

Version 1.7.1 removes the remote AI/OpenAI-compatible classification pipeline. The app no longer stores API keys, no longer includes Retrofit/OkHttp/Moshi networking code, and explicitly removes the `INTERNET` permission from the merged manifest.

## What's New in v1.7.1

- Fully local scanning and detection flow.
- Removed OpenAI-compatible API settings, encrypted API key storage, network clients, DTOs, and remote classification use cases.
- Settings now shows local processing and offline privacy status.
- Scan and home screens now use local-processing language instead of model/API configuration language.
- Added an explicit manifest removal for `android.permission.INTERNET`.
- Preserved safe review, undo, and Android 11+ system trash submission behavior.

## Features

- Local blur, screenshot, document/receipt, and low-quality photo detection.
- Directory discovery and selectable scan scope.
- Incremental scanning so repeat scans focus on new photos.
- Similar-photo grouping with dHash-based matching.
- Review screen with grid and swipe workflows.
- Batch selection and confirmation before cleanup.
- Five-second undo window before pending deletes are submitted.
- Android 11+ system trash integration via `MediaStore.createTrashRequest`.
- Material 3 dark UI with modern cards and progress logs.

## Permissions and Privacy

PhotoCleaner needs read access to photos/media so it can scan your local library. It does not request network access.

Relevant manifest behavior:

- Uses `READ_MEDIA_IMAGES` on Android 13+.
- Uses `READ_MEDIA_VISUAL_USER_SELECTED` on Android 14+ for partial-library access.
- Uses `READ_EXTERNAL_STORAGE` up to Android 12.
- Explicitly removes `INTERNET` and other unrelated permissions that dependencies might try to merge.

## How to Use

1. Install the APK on an Android 8.0+ device.
2. Grant photo/media access when prompted.
3. Open Scan, choose the folders to include, and select a batch size.
4. Start scanning and watch the local processing log.
5. Open Review to inspect suggested cleanup, uncertain items, similar groups, or all photos.
6. Confirm deletes carefully. You can undo within the short undo window before final submission.

## Technical Details

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.3.21 |
| UI Framework | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.59.2 |
| Database | Room 2.8.4 |
| Image Loading | Coil 3.4.0 |
| Navigation | Navigation Compose 2.9.8 |
| Local ML | ML Kit Image Labeling |
| Preferences | DataStore Preferences |
| Min SDK | 26 |
| Target SDK | 36 |

## Build

Prerequisites:

- JDK 17
- Android SDK API 36
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
│   │   ├── local/             # Room DAO, entities, database
│   │   └── repository/        # Repository implementations
│   ├── di/                    # Hilt modules
│   ├── domain/
│   │   ├── model/             # Photo, Classification, DirectoryInfo
│   │   ├── repository/        # Repository contracts
│   │   └── usecase/           # Scan/delete use cases
│   ├── service/               # Shared scan state holder
│   ├── ui/                    # Home, scan, review, stats, settings
│   └── util/                  # Image, blur, screenshot, permission helpers
└── AndroidManifest.xml
```

## License

Personal hobby project. No commercial license is granted.
