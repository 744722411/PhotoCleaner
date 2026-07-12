# PhotoCleaner - 本地照片清理助手

> English documentation: [README.md](README.md)

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

PhotoCleaner 是一款 Android 本地照片清理助手。它会扫描你选择的相册目录，在设备端检测明显模糊、空白、截图、票据/文档、低价值或可能重复的照片，并把结果交给你在“审查”页逐项确认。

版本 2026.07.12 针对 Android 16 完成永久删除状态校验、目录范围查询、自适应导航、无障碍和前台扫描支持。

应用仍然完全离线运行：不保存 API Key，不包含 Retrofit / OkHttp / Moshi 等联网分类代码，也不声明 `INTERNET` 权限。

## 2026.07.12 更新

- 最低支持 Android 11，目标 SDK 升级至 37；所有清理统一经过系统永久删除确认，移除不安全的旧系统直接删除和私有恢复分支。
- 权限结果和 Activity 恢复时都会刷新媒体访问状态，扫描页和设置页保持同步。
- 接入 Material 3 Adaptive Navigation Suite，并让审查网格根据手机、平板、横屏和折叠屏自动调整列数。
- 为 TalkBack 补充图标描述和保留/删除自定义操作，并尊重系统动画时长设置。
- 升级 Kotlin 2.4.0、KSP 2.3.10、Compose BOM 2026.06.01、Hilt 2.60.1、Hilt Navigation Compose 1.4.0 和 Turbine 1.2.1。
- 新增系统永久删除确认、取消和请求失败回归测试；单元测试、Lint、Debug 构建和 R8 Release 构建全部通过。

## 历史更新：v1.9.0

- 修复统计页饼图所有扇区都显示为灰色的问题，现在按分类正确着色。
- 文件大小格式化改为与区域设置无关（`Locale.US`），显示一致且单元测试稳定。
- 扫描检测改为并行处理（一次 4 张），大幅加快大量照片的扫描速度。
- 将 `fallbackToDestructiveMigrationOnDowngrade` 迁移到未废弃的 Room 新 API。
- 启用 R8 full mode 和资源/语言裁剪（仅保留中文），减小 Release 包体积。
- 移除无用代码（未使用的 DAO 查询），并明确 v3→v4 数据库迁移的无操作意图。
- 核对 Android 14+ 合规性：分级媒体权限、部分照片访问和系统永久删除。

## 历史更新：v1.8.0

- 修复选择 6 个目录后仍扫描全库的问题，扫描范围现在会贯穿目录发现、开始扫描和本地检测。
- 扫描结果按已完成项目增量保存，中途停止后，已经发现的照片会保留在审查页。
- 明确“暂停”和“停止”的差异：
  - 暂停：保留当前扫描任务，只是暂时不继续处理，点击继续后接着跑。
  - 停止：取消当前扫描任务，已完成并保存的结果保留，未处理的照片下次再扫。
- 新增真正的设置页：可调整处理批量，并开启“重新检测已入库照片”。
- 增加 Android 部分照片访问状态提示和重新授权入口。
- 扫描页显示已选目录数、估算照片数、处理上限和部分访问提醒。
- 调整照片审查页头部高度和标题字号，使全屏布局下不再贴近系统通知栏。
- 将已选目录设置从逗号拼接字符串迁移为 DataStore `StringSet`，避免目录名特殊字符导致解析错误。

## 功能特性

- 本地模糊、截图、空白照片、票据/文档、低质量照片检测。
- 自动发现相册目录，并支持选择扫描范围。
- 默认增量扫描，重复扫描时只处理新照片。
- 可在设置页开启“重新检测已入库照片”，适合算法或目录规则更新后使用。
- 每轮处理数量可选：100、500、2000 或全部。
- 使用 dHash 聚合视觉相似照片。
- 审查页支持网格浏览、滑动审查、批量选择和相似组处理。
- 删除前有确认和撤销流程。
- Android 11+ 使用 `MediaStore.createDeleteRequest` 请求永久删除，并在返回后核验每个媒体 URI。
- Material 3 深色界面，带扫描日志和离线隐私状态。

## 权限与隐私

PhotoCleaner 只需要读取照片/媒体权限来扫描本地相册，不请求网络权限。

Manifest 行为：

- Android 13+ 使用 `READ_MEDIA_IMAGES`。
- Android 14+ 使用 `READ_MEDIA_VISUAL_USER_SELECTED` 适配部分照片访问。
- Android 12 及以下使用 `READ_EXTERNAL_STORAGE`。
- 不声明 `INTERNET` 或其他无关敏感权限。

如果系统只授予“部分照片访问”，应用只能发现和扫描系统授权范围内的照片。扫描页和设置页会显示这一状态，并提供重新授权入口。

## 使用方法

1. 在 Android 11+ 设备上安装 APK。
2. 按系统提示授予照片/媒体读取权限。
3. 进入“扫描”，点击“发现目录”，选择要处理的目录。
4. 选择每次处理数量。日常使用建议保留默认 2000；想先验证结果可选 100 或 500；需要完整处理当前范围可选“全部”。
5. 点击“开始扫描”，查看本地处理日志。
6. 如果只是临时不想继续，点击“暂停”，之后可点击“继续”接着处理。
7. 如果要取消当前任务，点击“停止”。已经完成检测并写入数据库的照片会保留在“审查”页。
8. 进入“审查”，按建议清理、人工审查、保留、相似照片等视图逐项确认。
9. 删除前会二次确认，随后立即显示系统永久删除授权页；此操作无法撤销。

## 技术栈

| 组件 | 技术 |
|------|------|
| 开发语言 | Kotlin 2.4.0 |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture |
| 依赖注入 | Hilt 2.60 |
| 数据库 | Room 2.8.4 |
| 图片加载 | Coil 3.5.0 |
| 导航 | Navigation Compose 2.9.8 |
| 本地 ML | ML Kit Image Labeling 17.0.9 |
| 偏好设置 | DataStore Preferences 1.2.1 |
| 最低 SDK | 30 |
| 目标 SDK | 37 |
| 编译 SDK | 37 |

## 构建说明

前置要求：

- JDK 17
- Android SDK API 37
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
│   │   ├── classification/    # 本地照片分类器
│   │   ├── local/             # Room DAO、实体、数据库
│   │   ├── mapper/            # Entity / Domain 映射
│   │   ├── repository/        # 仓库实现
│   │   └── service/           # 系统永久删除集成
│   ├── di/                    # Hilt 模块
│   ├── domain/
│   │   ├── model/             # Photo、Classification、DirectoryInfo
│   │   ├── repository/        # 仓库接口
│   │   ├── service/           # 分类器接口
│   │   └── usecase/           # 扫描/删除用例
│   ├── service/               # 共享扫描状态
│   ├── ui/                    # 首页、扫描、审查、统计、设置
│   └── util/                  # 图片、模糊、截图、权限工具
└── AndroidManifest.xml
```

## 许可

个人兴趣项目，不授权商业使用。
