# Compose Base

通用的 Compose MVI 底层框架，供多个仓库复用。包含：

- **Compose MVI 架构**（`lib-compose-core`）：`UiState` / `Intent` / `Effect` 契约、`BaseMviViewModel`、`StateController`、`CollectEffect`、`BaseComposeActivity`、`BaseViewModel` 等。
- **Compose 设计系统**（`lib-compose-ui`）：`CommonTheme`、`CommonTitleBar`、`StatusOverlay`、`PullToRefreshLayout`、`CommonDialog`、`CommonImage` 等组件，以及语义化颜色 / 尺寸 / 字体 / 图标 / Lottie 资源。
- **通用工具类**（`lib-compose-core/utils`）：`SingleLiveEvent`、`ActivityUtils`、`StateSizeTracker` 等。

## 模块结构

```
compose_base/
├── lib-compose-core/        # MVI 契约 + 基类 + 网络 + DI + 通用工具
│   └── com.rainbow.compose.core
│       ├── contract/        # MviContract / BaseMviViewModel / StateController / UiStateExt / ApiResult
│       ├── base/            # BaseViewModel / BaseComposeActivity
│       ├── ui/              # CollectEffect
│       ├── utils/           # SingleLiveEvent / ActivityUtils / StateSizeTracker
│       └── log/             # ComposeLog
└── lib-compose-ui/          # 设计系统组件 + 主题 + 资源
    └── com.rainbow.compose.ui
        ├── theme/           # CommonTheme / CommonColors / Typography / Color
        ├── component/       # CommonTitleBar / StatusOverlay / PullToRefreshLayout ...
        ├── state/           # DefaultLoadingOverlay / DefaultErrorPage / DefaultEmptyPage ...
        ├── dimen/           # Dimens
        ├── keyboard/        # CommonKeyboard
        └── BaseCommonComposeActivity.kt
```

## 技术栈

| 分类 | 库 |
|---|---|
| UI | Compose BOM · Material3 · Material Icons Extended |
| 架构 | Lifecycle · ViewModel · Coroutines · **Hilt** |
| 网络 | **Retrofit · OkHttp · Gson · Logging-Interceptor** |
| 图片/动画 | **Coil3** · Lottie |
| 构建 | Gradle 8.10.2 · AGP 8.8.2 · Kotlin 2.1.10 · KSP |

> 每个库的**版本、用途与引入原因**详见 [`docs/TECH_STACK.md`](docs/TECH_STACK.md)。

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

> **使用 Hilt**：`lib-compose-core` 已把 `hilt-android` 作为 `api` 暴露。若业务侧要用 `@HiltViewModel` / `@AndroidEntryPoint`，需在自身模块应用 Hilt 插件与 KSP：
> ```groovy
> plugins {
>     alias(libs.plugins.hilt)
>     alias(libs.plugins.ksp)
> }
> dependencies {
>     ksp libs.hilt.android.compiler
> }
> ```

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
class HomeComposeActivity : BaseCommonComposeActivity() {
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
            CommonTitleBar(title = "首页", onBack = { })
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
- KSP `2.1.10-1.0.31`（Hilt 注解处理）
- Hilt `2.55`
- minSdk `24` / targetSdk `36`
- JDK 17

## 构建

```bash
./gradlew :lib-compose-core:assembleRelease
./gradlew :lib-compose-ui:assembleRelease
```
