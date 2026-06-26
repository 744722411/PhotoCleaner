# PhotoCleaner - 本地照片清理助手

---

> ## 免责声明
>
> 这是一个个人兴趣项目，不是商业产品。
>
> - 使用前请先备份重要照片。
> - 所有清理和删除操作都应仔细确认。
> - 开发者不对任何数据丢失负责。
> - 使用风险自负。

---

## 应用介绍

PhotoCleaner 是一款 Android 本地照片清理助手。它可以扫描你选择的相册目录，在设备端检测明显模糊、低质量、截图/票据类、可能无用的照片，并通过 dHash 将视觉上相近的照片聚合成相似组，最后交给你在审查页逐项确认。

从 v1.7.0 开始，项目彻底移除了远程 AI / OpenAI 兼容分类链路。应用不再保存 API Key，不再包含 Retrofit / OkHttp / Moshi 网络分类代码，并在 Manifest 中显式移除 `INTERNET` 权限。

## v1.7.0 更新

- 扫描和检测流程完全本地化。
- 移除 OpenAI 兼容 API 设置、API Key 存储、网络客户端、DTO 和远程分类用例。
- 设置页改为展示本地处理和离线隐私状态。
- 首页、扫描页和设置页文案统一为本地处理语义。
- Manifest 显式移除 `android.permission.INTERNET`。
- 保留审查页、5 秒撤销、批量删除和 Android 11+ 系统回收站提交能力。

## 功能特性

- 本地模糊、截图、票据/文档、低质量照片检测。
- 自动发现相册目录，并支持选择扫描范围。
- 增量扫描，重复扫描时优先处理新照片。
- dHash 相似照片聚合。
- 网格审查和滑动卡片审查。
- 批量选择、删除确认和短时间撤销。
- Android 11+ 使用 `MediaStore.createTrashRequest` 提交到系统回收站。
- Material 3 深色界面、现代卡片和扫描日志。

## 权限与隐私

PhotoCleaner 只需要读取照片/媒体权限来扫描本地相册，不请求网络权限。

Manifest 相关行为：

- Android 13+ 使用 `READ_MEDIA_IMAGES`。
- Android 14+ 使用 `READ_MEDIA_VISUAL_USER_SELECTED` 适配部分照片访问。
- Android 12 及以下使用 `READ_EXTERNAL_STORAGE`。
- 显式移除 `INTERNET` 以及其他依赖可能合并进来的无关权限。

## 使用方法

1. 在 Android 8.0+ 设备上安装 APK。
2. 根据系统提示授权照片/媒体读取权限。
3. 进入“扫描”，选择要处理的目录和单批处理数量。
4. 开始扫描，查看本地处理日志。
5. 进入“审查”，按建议清理、人工审查、保留、相似照片等视图逐项确认。
6. 删除前会二次确认；撤销窗口结束后，可提交到系统回收站。

## 技术栈

| 组件 | 技术 |
|------|------|
| 开发语言 | Kotlin 2.3.21 |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture |
| 依赖注入 | Hilt 2.59.2 |
| 数据库 | Room 2.8.4 |
| 图片加载 | Coil 3.4.0 |
| 导航 | Navigation Compose 2.9.8 |
| 本地 ML | ML Kit Image Labeling |
| 偏好设置 | DataStore Preferences |
| 最低 SDK | 26 |
| 目标 SDK | 36 |

## 构建说明

前置要求：

- JDK 17
- Android SDK API 36
- 与当前 Android Gradle Plugin 兼容的构建环境

Debug 构建：

```bash
.\gradlew.bat assembleDebug
```

Release 构建：

```bash
.\gradlew.bat assembleRelease
```

Release 包需要单独配置签名。

## 项目结构

```text
app/src/main/
├── java/com/photocleaner/
│   ├── data/
│   │   ├── local/             # Room DAO、实体、数据库
│   │   └── repository/        # 仓库实现
│   ├── di/                    # Hilt 模块
│   ├── domain/
│   │   ├── model/             # Photo、Classification、DirectoryInfo
│   │   ├── repository/        # 仓库接口
│   │   └── usecase/           # 扫描/删除用例
│   ├── service/               # 扫描状态持有器
│   ├── ui/                    # 首页、扫描、审查、统计、设置
│   └── util/                  # 图片、模糊、截图、权限工具
└── AndroidManifest.xml
```

## 许可

个人兴趣项目，不授权商业使用。
