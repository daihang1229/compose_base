# Compose Base

通用的 Compose MVI 底层框架，供多个仓库复用。包含：

- **Compose MVI 架构**（`lib-compose-core`）：`UiState` / `Intent` / `Effect` 契约、`BaseMviViewModel`、`StateController`、`CollectEffect`、`BaseComposeActivity`、`BaseViewModel` 等。
- **Compose 设计系统**（`lib-compose-ui`）：`DramaTheme`、`DramaTitleBar`、`StatusOverlay`、`PullToRefreshLayout`、`DramaDialog`、`DramaImage` 等组件，以及语义化颜色 / 尺寸 / 字体 / 图标 / Lottie 资源。
- **通用工具类**（`lib-compose-core/utils`）：`SingleLiveEvent`、`ActivityUtils`、`StateSizeTracker` 等。

> 本仓库由 drama-android 项目抽取、解耦而来。原业务耦合（TheRouter / RxBus / 沉浸式 / 埋点 / BaseKV 等）已移除，仅保留 AndroidX + Compose + 协程 依赖。

## 模块结构

```
compose_base/
├── lib-compose-core/        # MVI 契约 + 基类 + 通用工具（无业务依赖）
│   └── com.rainbow.compose.core
│       ├── contract/        # MviContract / BaseMviViewModel / StateController / UiStateExt / ApiResult
│       ├── base/            # BaseViewModel / BaseComposeActivity
│       ├── ui/              # CollectEffect
│       ├── utils/           # SingleLiveEvent / ActivityUtils / StateSizeTracker
│       └── log/             # ComposeLog
└── lib-compose-ui/          # 设计系统组件 + 主题 + 资源
    └── com.rainbow.compose.ui
        ├── theme/           # DramaTheme / DramaColors / Typography / Color
        ├── component/       # DramaTitleBar / StatusOverlay / PullToRefreshLayout ...
        ├── state/           # DefaultLoadingOverlay / DefaultErrorPage / DefaultEmptyPage ...
        ├── dimen/           # Dimens
        ├── keyboard/        # DramaKeyboard
        └── BaseDramaComposeActivity.kt
```

## 快速开始

### 1. 引入模块

在宿主工程 `settings.gradle` 中通过 `includeBuild` 或源码 `include` 引入：

```groovy
// settings.gradle
includeBuild('../compose_base')
// 或
include ':lib-compose-core'; project(':lib-compose-core').projectDir = new File('../compose_base/lib-compose-core')
include ':lib-compose-ui';   project(':lib-compose-ui').projectDir   = new File('../compose_base/lib-compose-ui')
```

依赖：

```groovy
dependencies {
    implementation project(':lib-compose-core')
    implementation project(':lib-compose-ui')
}
```

### 2. 写一个 MVI 页面

```kotlin
// Contract
sealed class HomeIntent : UiIntent {
    data object Load : HomeIntent()
}
sealed class HomeEffect : UiEffect {
    data class ShowToast(val msg: String) : HomeEffect()
}

// ViewModel
class HomeViewModel : BaseMviViewModel<HomeIntent, HomeEffect>() {
    private val stateController = StateController<List<Item>>()
    val uiState = stateController.state

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> {
                stateController.loading()
                stateController.success(listOf(Item(1, "A")))
            }
        }
    }
}

// Activity
class HomeComposeActivity : BaseDramaComposeActivity() {
    private val viewModel by viewModels<HomeViewModel>()

    @Composable
    override fun screen() {
        HomeScreen(viewModel)
    }
}
```

### 3. 使用设计系统

```kotlin
@Composable
fun HomeContent(uiState: UiState<List<Item>>, onIntent: (HomeIntent) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column {
            DramaTitleBar(title = "首页", onBack = { })
            // ... 列表内容
        }
        StatusOverlay(state = uiState, onRetry = { onIntent(HomeIntent.Load) })
    }
}
```

## 环境要求

- Android Gradle Plugin `8.8.2`
- Kotlin `2.1.10`
- Compose BOM `2024.12.01`
- minSdk `24` / targetSdk `36`
- JDK 17

## 构建

```bash
./gradlew :lib-compose-core:assembleRelease
./gradlew :lib-compose-ui:assembleRelease
```

## 与原项目（drama-android）的差异

| 项 | drama-android | compose_base |
|---|---|---|
| 包名 | `com.storymatrix.drama.*` | `com.rainbow.compose.*` |
| `BaseComposeActivity` 继承链 | TheRouter / RxBus / 沉浸式 / 埋点 | 纯 `ComponentActivity` |
| `NetworkResult` 依赖 | `lib_http.NetworkResult` | 本地 `ApiResult` |
| 深色模式判断 | `ViewUtils.isDarkMode()` | `isSystemInDarkTheme()` |
| 日志 | `XlogUtils` | 本地 `ComposeLog` |
| 资源 | 跨模块 `BizR` / `LangR` | 模块内 `R` |
| 埋点 | `trackComposeClick` | 已移除 |
