# 技术栈与架构说明

> 本文档列出 compose-base 框架引入的所有第三方库、构建工具与架构设计，并说明每一项的引入原因。

---

## 一、架构总览

### 1.1 模块划分（双模块）

```
compose-base/
├── lib-compose-core/   # 内核：MVI 契约 + 基类 + 网络 + 依赖注入 + 通用工具
└── lib-compose-ui/     # 展示层：主题 + 设计系统组件 + 资源
```

| 模块 | 职责 | 依赖边界 |
|---|---|---|
| `lib-compose-core` | MVI 契约、`BaseMviViewModel`、`BaseViewModel`、`BaseComposeActivity`、网络、DI、通用工具 | 仅 AndroidX + Compose + 协程 + 网络 + DI，**无 UI 业务组件** |
| `lib-compose-ui` | `CommonTheme`、`CommonTitleBar`、`StatusOverlay`、`PullToRefreshLayout` 等组件与主题资源 | 依赖 `lib-compose-core` + Material3 + 图片 + Lottie |

**为什么双模块**：MVI 内核（契约/基类/网络/DI）与 UI 组件（主题/控件）关注点不同，分开可独立发布、独立复用。业务方只想用 MVI 内核时无需拖入整套设计系统。

### 1.2 MVI 数据流

```
        sendIntent(intent)
UI ────────────────────────► BaseMviViewModel
   ◄────────────────────────── StateFlow<UiState<T>>   (moveLoading/Success/Error/Empty)
   ◄────────────────────────── Flow<E> (Effect)         (CollectEffect 收集一次性副作用)
```

- **单向数据流**：View 只通过 `Intent` 表达意图，ViewModel 只通过 `State`（状态）和 `Effect`（副作用）回吐。
- **状态五态**：`UiState = Idle / Loading / Success / Error / Empty`，配合 `StateController` / `moveXxx()` 扩展。
- **副作用分离**：`Effect` 用 Channel 投递，`EffectDelivery.DELIVER_ALL`（导航/支付）与 `LATEST_ONLY`（Toast）两种策略。

### 1.3 包结构

```
com.rainbow.compose.core
├── contract/   MviContract · BaseMviViewModel · StateController · UiStateExt · ApiResult · EffectDelivery
├── base/       BaseViewModel · BaseComposeActivity
├── ui/         CollectEffect
├── utils/      SingleLiveEvent · ActivityUtils · StateSizeTracker
└── log/        ComposeLog

com.rainbow.compose.ui
├── theme/      CommonTheme · CommonColors · CommonTypography · Color
├── component/  CommonTitleBar · StatusOverlay · PullToRefreshLayout · CommonDialog ...
├── state/      DefaultLoadingOverlay · DefaultErrorPage · DefaultEmptyPage · UiStateContainer
├── dimen/      Dimens
├── keyboard/   CommonKeyboard
└── BaseCommonComposeActivity.kt
```

---

## 二、第三方依赖清单

### 2.1 lib-compose-core

| 库 | 版本 | 用途 | 为什么引入 |
|---|---|---|---|
| `androidx.core:core-ktx` | 1.13.1 | Android 核心扩展 API | 提供 `Context`、`Bundle` 等 Kotlin 扩展，几乎每个 Android 库的基础依赖 |
| `androidx.appcompat:appcompat` | 1.6.1 | `AppCompatActivity` 基类 | `BaseComposeActivity` 的宿主类型来源，兼容旧设备 Activity |
| `androidx.activity:activity-compose` | 1.9.0 | `ComponentActivity.setContent` | 让 Activity 直接装载 Compose 内容，Compose 页面唯一入口 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.4 | 生命周期协程支持 | `lifecycleScope` / `repeatOnLifecycle`，保证 UI 收集与生命周期绑定 |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.8.4 | `ViewModel` / `viewModelScope` | MVI 的 ViewModel 宿主与协程作用域来源 |
| `androidx.lifecycle:lifecycle-livedata-ktx` | 2.8.4 | `LiveData` 支持 | `SingleLiveEvent` 依赖 `MutableLiveData` |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.4 | `collectAsStateWithLifecycle` | 生命周期感知的 StateFlow 收集，避免后台继续收集 |
| `org.jetbrains.kotlinx:coroutines-*` | 1.8.1 | 协程 | MVI 异步处理、`Flow`/`Channel`/`StateFlow` 的底层 |
| `androidx.compose:compose-bom` | 2024.12.01 | Compose 版本统一 | BOM 统一各 compose 构件版本，避免冲突 |
| `androidx.compose.ui:ui` / `foundation` | BOM | Compose UI 基础 | `Modifier`、`@Composable`、布局等 Compose 基础 |
| `androidx.compose.ui:ui-tooling-preview` | BOM | `@Preview` | 设计系统组件 Preview 与业务侧预览 |
| **`com.squareup.retrofit2:retrofit`** | 2.11.0 | HTTP 客户端封装 | 声明式网络请求，Android 最主流的 REST 客户端 |
| **`com.squareup.retrofit2:converter-gson`** | 2.11.0 | JSON 反序列化 | 把响应体自动转成 DTO 数据类（Gson 随其传递引入，无需单独声明） |
| **`com.squareup.okhttp3:okhttp`** | 4.12.0 | 底层 HTTP 栈 | Retrofit 的底层传输层，负责连接池/重定向/缓存 |
| **`com.squareup.okhttp3:logging-interceptor`** | 4.12.0 | 网络日志 | 调试期打印请求/响应，排查接口问题 |
| **`com.google.dagger:hilt-android`** | 2.55 | 依赖注入运行时 | Google 官方推荐 DI 框架，配合 `@HiltViewModel` 注入 UseCase/Repository |

> **网络库说明**：`core/contract/ApiResult` 是网络层与 MVI 层的桥接。Repository 返回 `Flow<ApiResult<T>>`，经 `toUiStateFlow()` 一键映射为 `UiState`，屏蔽具体网络实现（Retrofit 返回值 `Response` / `NetworkResult` 由业务侧自行转换）。

> **图片库说明**：Coil 放在 `lib-compose-ui`（见 2.2），因为图片加载与 UI 组件（`CommonImage`）强相关；core 层不接触图片。

### 2.2 lib-compose-ui

| 库 | 版本 | 用途 | 为什么引入 |
|---|---|---|---|
| `androidx.compose.material3:material3` | BOM | Material3 组件 | `Scaffold`/`Button`/`Dialog`/`Snackbar` 等组件的来源 |
| `androidx.compose.material:material-icons-extended` | BOM | 图标集 | `CommonTitleBar` 返回箭头、`ActionItemRow` 右箭头等矢量图标 |
| `androidx.compose.ui:ui-tooling`（debug） | BOM | 布局检查器 | 调试期 Preview 渲染与 Layout Inspector |
| **`io.coil-kt.coil3:coil-compose`** | 3.2.0 | 图片加载（Compose） | `CommonImage` 统一封装网络图片加载/占位图/错误图，Coil3 是 Compose 官方推荐图片库 |
| **`io.coil-kt.coil3:coil-network-okhttp`** | 3.2.0 | 图片网络栈 | Coil3 分离了网络层，此构件让 Coil 复用 OkHttp（与核心网络库同栈，共享连接池/拦截器） |
| `com.airbnb.android:lottie-compose` | 6.2.0 | Lottie 动画 | `PullToRefreshLayout` 刷新指示器、`DefaultLoadingOverlay` 加载动画 |

---

## 三、构建工具链

| 工具 | 版本 | 用途 | 为什么引入 |
|---|---|---|---|
| Gradle Wrapper | 8.10.2 | 构建脚本分发 | 锁定 Gradle 版本，团队/CI 构建一致 |
| Android Gradle Plugin (AGP) | 8.8.2 | Android 构建 | 编译、打包 AAR、资源合并 |
| Kotlin | 2.1.10 | 开发语言 | 全库 Kotlin 编写 |
| Kotlin Compose Compiler Plugin | 2.1.10 | Compose 编译 | Kotlin 2.x 起 Compose 编译器随 Kotlin 插件分发 |
| **KSP** | 2.1.10-1.0.31 | 注解处理 | 替代 KAPT，为 Hilt 生成依赖注入代码（编译更快） |
| **Hilt Gradle Plugin** | 2.55 | DI 代码生成 | 让 Hilt 注解处理器接入构建，生成 Hilt 组件 |

---

## 四、架构设计要点（为什么这样设计）

### 4.1 `ApiResult` 而非直接依赖 Retrofit 类型

`contract/ApiResult` 是自建的 `Loading / Success / Error` 三态密封接口。业务 Repository 把 Retrofit 的 `Response` 或自建 `NetworkResult` 转成 `ApiResult`，从而：

- MVI 层**不直接依赖 Retrofit 类型**，网络实现可替换。
- `toUiStateFlow()` 统一完成 `ApiResult → UiState` 的五态映射。

### 4.2 `BaseComposeActivity` 只继承 `ComponentActivity`

不引入路由（TheRouter）、事件总线（RxBus/LiveEventBus）、沉浸式等业务耦合，只做 `setContent` 装载。这样任何宿主工程都能直接继承，路由/沉浸式由业务侧按需叠加。

### 4.3 统一日志 `ComposeLog`

集中一个日志入口，默认委托 `android.util.Log`，带全局 `enabled` 开关。后续可无痛替换为脱敏/分级/生产关闭的实现，业务侧不直接调用原生 Log。

### 4.4 `SingleLiveEvent` 与 `Effect` 的分工

- `SingleLiveEvent`：供 XML/传统 View 侧使用的**一次性 LiveData**。
- `Effect`（Channel）：Compose MVI 的**一次性副作用**通道。

两者覆盖不同 UI 范式，避免配置变更时事件重放。

### 4.5 资源自包含

图标（返回箭头、空态、错误态）、Lottie `refresh.json`、文案全部下沉到 `lib-compose-ui/src/main/res`，设计系统组件只引用本模块 `R`，不跨模块取资源，保证库可独立编译与预览。

---

## 五、依赖关系图

```
lib-compose-ui ──api──► lib-compose-core
     │                      │
     ├─ Compose Material3    ├─ AndroidX / Lifecycle
     ├─ Coil / Lottie        ├─ Compose (BOM)
     └─ 图标/动画资源         ├─ Retrofit / OkHttp / Gson
                            └─ Hilt
```
