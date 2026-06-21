# PhotoCleaner - AI 照片清理助手

---

> ## ⚠️ 免责声明 / DISCLAIMER ⚠️
>
> **This is a personal hobby project / 这是一个个人兴趣项目**
>
> - **NOT a commercial product** / 非商业产品
> - **The developer is NOT responsible for any data loss** / 开发者不对任何数据丢失负责
> - **Users MUST backup all important photos before using this tool** / 使用前必须备份所有重要照片
> - **All delete operations should be reviewed carefully before confirming** / 所有删除操作请仔细确认后再执行
> - **Use at your own risk / 使用风险自负**
> - **By using this app you acknowledge and accept these terms** / 使用本应用即表示您已知悉并接受以上条款
>
> 📸 **请在使用前备份您的珍贵照片！**

---

## 📱 App Description / 应用介绍

PhotoCleaner is an AI-powered photo cleaning assistant for Android that helps you identify and remove useless, blurry, duplicate, or low-quality photos from your device. It uses on-device AI classification to categorize your photos and provides an intuitive review interface for easy cleanup.

PhotoCleaner 是一款基于 AI 的 Android 照片清理助手，帮助您识别并删除设备上无用的、模糊的、重复的或低质量的照片。它使用设备端 AI 分类技术对照片进行分类，并提供直观的审查界面以便快速清理。

## 📢 Recent Updates (v1.5.0)

- **Security Enhancements**: Moved OpenAI API Key storage to `EncryptedSharedPreferences`.
- **Performance**: Removed `runBlocking` calls from network interceptors to prevent ANR. Replaced heavy `getPixel()` image analysis with highly optimized `getPixels()` array operations.
- **Smart Scanning**: Introduced an incremental scan algorithm that protects existing AI analysis results and drastically speeds up rescan times. 
- **Navigation Safety**: Upgraded to Navigation Compose 2.9.0 with fully type-safe routes (`@Serializable`).
- **Stability Fixes**: Fixed `InputStream` leak in AI service and ensured correct serialization for API responses (`MessageContent`).
- **Safe Recovery**: Completely overhauled the trash recovery system to correctly restore photos to MediaStore instead of just the internal database.

## ✨ Features / 功能特性

- **🤖 AI Photo Classification** - Automatically classifies photos as useful, useless, or uncertain
- **📊 Confidence Scoring** - Shows AI confidence level for each classification
- **🔍 Review Interface** - Swipe through photos with full-screen preview
- **☑️ Batch Operations** - Select multiple photos for bulk delete or keep
- **🗑️ Safe Deletion** - Delete confirmation dialog with warning before all delete actions
- **↩️ Undo Support** - Undo recent deletions within 5 seconds
- **🎨 Modern Dark UI** - Beautiful Material 3 dark theme with smooth animations
- **🏷️ Category Tags** - Photos organized by categories
- **📱 Adaptive Icon** - Clean, modern app icon

## 🚀 How to Use / 使用方法

1. **Install** the APK on your Android device (requires Android 8.0+)
2. **Grant permissions** - Allow access to photos/media when prompted
3. **Scan** - The app will automatically scan and classify your photos
4. **Review** - Browse through photos, filtered by classification:
   - 🔴 **Useless (无用)** - AI recommends deletion
   - 🟡 **Uncertain (待定)** - Needs manual review
   - 🟢 **Keep (保留)** - Good photos to keep
5. **Delete** - Tap the delete button on individual photos or use batch mode:
   - Enable batch mode via the checklist icon
   - Select multiple photos
   - Tap the red FAB to delete selected
   - **Confirm deletion** in the warning dialog
6. **Undo** - Made a mistake? Tap "撤销" within 5 seconds to restore

## 🛠️ Technical Details / 技术细节

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.1.0 |
| UI Framework | Jetpack Compose (Material 3) |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt 2.59.2 |
| Database | Room 2.8.4 |
| Image Loading | Coil 3.5.0 |
| Navigation | Navigation Compose 2.9.0 |
| Network | Retrofit 2.11.0 + OkHttp 4.12.0 |
| Serialization | Moshi 1.15.1 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 |
| Build System | Gradle with Kotlin DSL |

## 🔨 Build Instructions / 构建说明

### Prerequisites
- JDK 17
- Android SDK (API 36)
- Gradle (wrapper included)

### Build Debug APK

```bash
cd PhotoCleaner
export JAVA_HOME="/path/to/jdk-17"
export ANDROID_HOME="/path/to/android-sdk"
./gradlew assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK

```bash
./gradlew assembleRelease
```

> Note: Release builds require signing configuration in `app/build.gradle.kts`

## 📁 Project Structure / 项目结构

```
app/src/main/
├── java/com/photocleaner/
│   ├── di/                    # Hilt dependency injection
│   ├── domain/
│   │   ├── model/             # Data models (Photo, Classification)
│   │   ├── repository/        # Repository interfaces
│   │   └── usecase/           # Business logic use cases
│   ├── ui/
│   │   ├── components/        # Shared UI components
│   │   ├── review/            # Photo review screen
│   │   └── theme/             # Material 3 theme (colors, typography)
│   └── util/                  # Utility classes
├── res/
│   ├── drawable/              # Vector icons & backgrounds
│   ├── mipmap-*/              # App launcher icons
│   └── values/                # Strings, themes, etc.
└── AndroidManifest.xml
```

## 📄 License / 许可

This is a personal hobby project. No license granted for commercial use.

这是一个个人兴趣项目，不授权商业使用。

---

*Built with ❤️ using Kotlin and Jetpack Compose*
