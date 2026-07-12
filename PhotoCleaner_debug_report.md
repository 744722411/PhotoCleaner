# PhotoCleaner Debug Report: Scan Crash Issue

## 1. Problem Description
When clicking the "Start Scan" (开始扫描) button on the Scan Screen (ScanScreen.kt), the application consistently crashes (force-closes) and returns to the system launcher interface.

## 2. ADB Logcat Output (Fatal Exception Trace)
The key logs captured via adb logcat:
```plain
06-29 22:12:35.306  4432  6774 I am_crash: [24427,0,com.photocleaner,550026822,java.lang.IllegalArgumentException,Key "1782742355276" was already used. If you are using LazyColumn/Row please make sure you provide a unique key for each item.,InlineClassHelper.kt,36,0]
06-29 22:12:35.306  4432  2080 I DropBoxManagerService: add tag=data_app_crash isTagEnabled=true flags=0x2
06-29 22:12:35.307  4432  6774 W ActivityTaskManager:   Force finishing activity com.photocleaner/.MainActivity

FATAL EXCEPTION: main
Process: com.photocleaner, PID: 24427
java.lang.IllegalArgumentException: Key "1782742355276" was already used. If you are using LazyColumn/Row please make sure you provide a unique key for each item.
    at androidx.compose.ui.internal.InlineClassHelperKt.throwIllegalArgumentException(InlineClassHelper.kt:36)
    at androidx.compose.ui.layout.LayoutNodeSubcompositionsState.subcompose(SubcomposeLayout.kt:1592)
    at androidx.compose.ui.layout.LayoutNodeSubcompositionsState$Scope.subcompose(SubcomposeLayout.kt:1353)
    at androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScopeImpl.compose(LazyLayoutMeasureScope.kt:94)
    at androidx.compose.foundation.lazy.layout.LazyLayoutMeasuredItemProvider.getPlaceables-3p2s80s(LazyLayoutMeasuredItem.kt:60)
    at androidx.compose.foundation.lazy.LazyListMeasuredItemProvider.getAndMeasure-0kLqBqw(LazyListMeasuredItemProvider.kt:54)
    ...
```

## 3. Root Cause Analysis
In `ScanScreen.kt`, the logs list panel renders using a `LazyColumn` component:
```kotlin
// ScanScreen.kt Line 237-239
LazyColumn(state = listState, modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    items(logs, key = { entry -> entry.timestamp }) { entry -> ScanLogItem(entry = entry) }
}
```
* **The issue**: In the logging pipeline (`ScanPhotosUseCase.kt`), when scanning starts or transitions, multiple `ScanLogEntry` items are quickly dispatched within the exact same millisecond.
* **The result**: `entry.timestamp` (represented by `System.currentTimeMillis()`) is identical for these rapid events. Jetpack Compose expects every item key in a `LazyColumn`/`LazyRow` to be globally unique. Duplicate keys trigger an `IllegalArgumentException` on the main UI thread, causing the crash.

## 4. Resolution Plan
Modify the `key` parameter of `items()` in `ScanScreen.kt`. We will use a combination of index and timestamp (e.g., generating a unique key like `"${entry.timestamp}_${index}"` or utilizing a unique ID) to ensure that even logs dispatched in the same millisecond will not have overlapping keys.

We will proceed to implement the fix.
