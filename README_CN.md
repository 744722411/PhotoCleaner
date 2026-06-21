# PhotoCleaner - AI 照片清理助手

---

> ## ⚠️ 免责声明 / DISCLAIMER ⚠️
>
> **这是一个个人兴趣项目 / This is a personal hobby project**
>
> - **非商业产品** / NOT a commercial product
> - **开发者不对任何数据丢失负责** / The developer is NOT responsible for any data loss
> - **使用前必须备份所有重要照片** / Users MUST backup all important photos before using this tool
> - **所有删除操作请仔细确认后再执行** / All delete operations should be reviewed carefully before confirming
> - **使用风险自负** / Use at your own risk
> - **使用本应用即表示您已知悉并接受以上条款** / By using this app you acknowledge and accept these terms
>
> 📸 **请在使用前备份您的珍贵照片！**

---

## 📱 应用介绍

**PhotoCleaner** 是一款运行在 Android 端、基于 AI 与感知哈希算法的智能照片清理工具。它可以帮助您快速识别并清理设备中无用的、严重模糊的、曝光失误的废片，以及视觉上高度重复的相似连拍照片。

通过引入本地轻量化检测（模糊度、空白度、文档识别）与云端高阶 AI 语义分类，PhotoCleaner 能够智能化评估每一张照片的保留价值，并通过灵动解压的“飞卡”手势及分组卡片，为您带来极致流畅的照片整理体验。

---

## 📢 最新更新 (v1.6.0)

本版本对系统特性进行了深度适配，并进行了一次全面的安全性与性能优化审计：

*   **♻️ Android 11+ 系统回收站对接**：适配 `MediaStore.createTrashRequest`，删除时直接移入系统回收站，完美兼容低版本备份逻辑，拒绝空间二次占用。
*   **⏳ 5 秒延迟撤销机制**：删除时本地 Room 拦截并隐去照片，提供 5 秒极简撤销 Snackbar。**全程无系统弹窗打扰**，页面关闭或倒计时结束时一次性发起批量系统授权，交互手感行云流水。
*   **📊 本地 dHash 相似/重复照片聚合**：采用 64 位感知差分哈希（dHash）指纹，自动将 10 分钟内且汉明距离 ≤ 5 的相片聚合成“相似组”，并支持依据“模糊度 > 分辨率 > 文件体积”一键智能“保留最佳”。
*   **🛡️ Android 14+ 部分授权增量引导**：精准检测 `READ_MEDIA_VISUAL_USER_SELECTED` 部分授权状态，首页顶部增设醒目引导横幅，支持一键追加勾选新照片。
*   **🎨 Material You 动态色彩渐变背景**：全面重构页面背景，色彩自动吸附并融合用户系统的 Material You 壁纸主题色。
*   **📳 解压级触觉手势反馈**：适配 Android 12+，在卡片滑动越过删除/保留临界线及飞出时，触发精致的定制微物理振动。
*   **⚡ 核心稳定性与性能修复**：
    *   **ANR 规避**：将 $O(N^2)$ 的相似图片聚合运算移出主线程（切至 `Dispatchers.Default`），大相册亦绝不卡死。
    *   **手势流畅度提升**：改用平铺 State 承接滑动位移，彻底消除在 drag 手势中高频 launch 协程造成的内存抖动与 GC 卡顿。
    *   **防止闪退**：对 API 基准地址（Base URL）进行强格式验证并套入异常防护网，杜绝不合规输入引起的无限闪退；补齐 `MIGRATION_2_3` 防止旧版升级时清空历史数据。
    *   **句柄泄露防范**：重构 EXIF 读取和图片本地检测的流逻辑，用 `.use {}` 保证输入流在异常情况下绝对被物理关闭。

---

## ✨ 功能特性

*   **🤖 AI 照片分类**：自动将照片归类为“建议清理（Useless）”、“保留（Keep）”及“待定审查（Uncertain）”。
*   **📊 置信度评分**：直观展示 AI 对照片分类的置信度百分比。
*   **🔍 沉浸式审查**：支持左右滑动的“Tinder 飞卡”模式以及传统的大图浏览，无缝集成触觉震动反馈。
*   **☑️ 批量操作**：网格视图下可一键全选或多选进行批量标记。
*   **🗑️ 安全删除防护**：物理删除前必须进行二次弹窗确认，且支持 Android 11+ 原生系统回收站的跨版本安全撤销。
*   **📂 智能目录过滤**：自动发现并过滤无意义的系统图标、Emoji 表情、缓存垃圾文件夹，只专注于用户相册。

---

## 🚀 使用方法

1.  **安装**：下载并安装 APK 文件的（要求 Android 8.0/API 26 及以上系统）。
2.  **授权**：首次打开时，根据系统提示授予媒体文件读取权限。在 Android 13+ 系统上，请一并允许通知发送以展示扫描进度。
3.  **扫描配置**：在首页点击“开始扫描”跳转至扫描设置：
    *   在目录列表中勾选你想要分析的相册文件夹。
    *   选择每次处理的单批次照片数量（默认 100 张）。
    *   点击“开始扫描”，前台服务会接管并在通知栏实时显示检测日志。
4.  **审查与清理**：
    *   🔴 **建议清理 (Useless)**：主要是空白照、拍摄抖动的模糊废片、镜头误触的黑色误拍，可安全右滑保留或左滑/批量一键清理。
    *   🟡 **人工审查 (Uncertain)**：主要包括截图、收据发票、二维码等可能临时有用但容易积压的照片，方便您核对后整理。
    *   🟢 **保留 (Keep)**：个人生活照、风景照等，建议珍藏。
5.  **撤销操作**：误删了照片？在底部的 Snackbar 消失前（5 秒内）点击“撤销”可瞬间回滚。
6.  **高级设置**：如果希望开启云端高阶 AI 分析，可前往“高级设置”中配置你个人的 OpenAI 兼容 API Key、Base URL 及模型名称（如 `gpt-4o-mini`），非必填，留空将默认使用本地规则引擎。

---

## 🛠️ 技术栈

| 组件 | 所用技术 |
| :--- | :--- |
| **开发语言** | Kotlin 2.1.0 |
| **UI 框架** | Jetpack Compose (Material 3) |
| **架构设计** | MVVM + 干净架构 (Clean Architecture) |
| **依赖注入** | Hilt 2.59.2 |
| **数据持久化** | Room Database 2.8.4 |
| **图片加载** | Coil 3.5.0 (支持 Kotlin Multiplatform 级别的跨端协程加载) |
| **路由导航** | Navigation Compose 2.9.0 (全面启用 `@Serializable` 类型安全导航) |
| **网络请求** | Retrofit 2.11.0 + OkHttp 4.12.0 |
| **反序列化** | Moshi 1.15.1 (配合代码生成与自定义的多态适配器) |
| **安全加密** | Androidx Security Crypto 1.1.0 (用于存储 API Key) |
| **端侧算法** | ML Kit Image Labeling (识别文档票据) |
| **最低支持** | SDK 26 (Android 8.0) |
| **目标支持** | SDK 36 (Android 16) |

---

## 🔨 构建与开发说明

### 前期准备
*   JDK 17
*   Android SDK (API 36)
*   Gradle (项目已包含 gradlew 包装器)

### 构建 Debug 测试包

在项目根目录下，使用 Powershell / 终端执行：

```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

生成的安装包位于：
`app/build/outputs/apk/debug/app-debug.apk`

### 构建 Release 正式包

```bash
./gradlew assembleRelease
```
> 注：Release 正式包需要您在 `app/build.gradle.kts` 中配置相应的签名证书 (signingConfigs) 才能顺利完成编译。

---

## 📁 项目结构说明

```
app/src/main/
├── java/com/photocleaner/
│   ├── di/                    # Hilt 依赖注入模块管理
│   ├── domain/
│   │   ├── model/             # 核心数据实体 (Photo, Classification)
│   │   ├── repository/        # 仓库接口定义
│   │   └── usecase/           # 独立业务逻辑用例层
│   ├── data/
│   │   ├── local/             # Room 本地持久化层 (Dao, Entities, Migrations)
│   │   ├── remote/            # Retrofit 云端 AI 接口层 (Api, Services, DTO)
│   │   └── repository/        # 仓库模式的具体实现 (PhotoRepositoryImpl)
│   ├── service/               # 后台扫描前台服务 (ScanService)
│   ├── ui/
│   │   ├── components/        # 公用毛玻璃卡片、状态页、Badges 等微组件
│   │   ├── navigation/        # 类型安全导航图 (NavGraph)
│   │   ├── home/              # 首页总览
│   │   ├── scan/              # 扫描与日志输出页
│   │   ├── review/            # Tinder 卡片与相似聚合审查页
│   │   ├── settings/          # AI 参数设置页
│   │   ├── stats/             # 数据分析页
│   │   └── theme/             # Material 3 动态色彩与字体排版配置
│   └── util/                  # 差分哈希计算、模糊算法、权限引导等工具类
├── res/
│   ├── drawable/              # 矢量图标与渐变遮罩
│   └── values/                # 本地化字符串与全局主题 definition
└── AndroidManifest.xml
```

---

*Built with ❤️ using Kotlin and Jetpack Compose*
