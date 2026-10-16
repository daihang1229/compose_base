---
name: compose-mvi-scaffold
description: >
  Generate complete Compose + MVI feature scaffolding for the compose_base project
  (Contract/ViewModel/Screen/Mapper/Activity/Preview + Route registration).
  ONLY trigger when the user explicitly mentions Compose or MVI:
  "Compose页面", "Compose + MVI", "用Compose写", "MVI Feature", "Compose Feature",
  "搭一个Compose页面", "新建Compose页面", "scaffold compose", "生成Compose代码".
  Do NOT trigger for generic "新建页面", "创建Feature", "写个页面" unless Compose is specified.
  If unclear whether they want Compose or XML, ASK first.
---

# Compose MVI Scaffold for compose_base

Generate all boilerplate files for a new Compose + MVI feature page,
following the exact conventions of the compose_base project.

## Before you start — disambiguation

**If the user says "新建页面" without mentioning Compose, ask:**
> "你要用 Compose + MVI 还是传统的 XML + MVVM？"

Only proceed if they explicitly choose Compose. This skill is NOT for XML pages.

**You need 4 pieces of information from the user** (ask only what's missing):

| # | Info | Example | How to infer |
|---|---|---|---|
| 1 | Feature name (PascalCase) | `AlbumDetail` | From the page description |
| 2 | Target module | `module_xxx` | Match to existing module names under `compose_base/` |
| 3 | Key data fields | `id, title, coverUrl, author` | From the user's UI description |
| 4 | User interactions | 下拉刷新、点击跳转详情、上拉加载更多 | List of things the user can do on this page |

## What this skill generates

Always generate exactly **7 files**:

```
module_xxx/src/main/java/.../module_xxx/compose/
├── XxxComposeActivity.kt       # Activity shell（路由跳转入口）
├── presentation/
│   ├── XxxContract.kt          # UiModel + State + Intent + Effect
│   ├── XxxViewModel.kt         # extends BaseMviViewModel<I, E>
│   ├── XxxScreen.kt            # Screen (route entry) + Content (pure render)
│   └── XxxScreenPreview.kt     # 6 @Preview variants
└── mapper/
    └── XxxMapper.kt            # DTO → UiModel anti-corruption layer
```

**Also update**:
- `module_xxx/src/main/AndroidManifest.xml` — register the Activity

## Workflow

Follow these steps in order. Do NOT skip any step.

### Step 1: Determine module and package

```bash
# List existing modules to find the right one
ls compose_base/ | grep module_
```

Choose the most relevant module based on the feature domain.
Package path convention: `com.rainbow.compose.<module_suffix>.compose`
Example: `module_xxx` → `com.rainbow.compose.xxx.compose`

### Step 2: Generate Contract file

Read `references/contract-template.kt` and fill in:
- Package name
- Feature name prefix for all classes
- UiModel data classes (all fields `val`, all classes `@Immutable`)
- State data class if the page has pagination or complex state
- Intent sealed class (one subclass per user action)
- Effect sealed class (one subclass per one-shot event like Toast)

**Conventions to enforce:**
- All UiModel data classes → `@Immutable`
- All fields → `val`, with defaults where possible
- Intent → `sealed class XxxIntent : UiIntent`, use `data object` for no-arg actions
- Effect → `sealed class XxxEffect : UiEffect`, one subclass per side effect type
- State data class → `@Immutable data class`, separate from UiModel

### Step 3: Generate Mapper file

Read `references/mapper-template.kt` and fill in:
- DTO import (ask user or use a placeholder)
- Extension function `DtoType.toXxxItem(): XxxItem`

**Conventions to enforce:**
- Mapper must be in `compose/mapper/` subpackage (separate from presentation)
- Use `internal fun` extension function style
- DTO types must NOT appear in any presentation file

### Step 4: Generate ViewModel file

Read `references/viewmodel-template.kt` and fill in:
- `@HiltViewModel` with `@Inject constructor` dependencies
- `BaseMviViewModel<XxxIntent, XxxEffect>` inheritance
- State strategy: single `StateFlow` (simple) or dual `StateFlow` (pagination)
- `handleIntent()` with exhaustive `when` on all Intent variants
- Private methods for each business operation
- Use `moveLoading()`/`moveSuccess(data)`/`moveError()`/`moveEmpty()` extensions

**Conventions to enforce:**
- Base class is `BaseMviViewModel<I, E>` (2 type params, NOT 3)
- `sendIntent()` is the only public entry point (already in base class)
- State is `MutableStateFlow<UiState<T>>` initialized to `UiState.Loading`
- Use `moveXxx()` extensions (NOT `_uiState.value = UiState.xxx`)
- Network calls use `launchUI` or `viewModelScope.launch`
- `fetchJob?.cancel()` before new fetch to avoid race conditions

### Step 5: Generate Screen file

Read `references/screen-template.kt` and fill in:
- `XxxScreen` Composable: collects State/Effect, delegates to Content
- `XxxContent` Composable: pure render, no ViewModel dependency
- `CollectEffect` for effect handling
- `StatusOverlay` for Loading/Error/Empty states
- Pull-to-refresh and load-more patterns if needed
- V2 页面性能回调:首帧可见、网络/本地数据耗时、主要内容绑定后的可交互帧

**Conventions to enforce:**
- Screen and Content MUST be two separate functions
- Screen: `@Composable fun XxxScreen(viewModel: XxxViewModel, onBack: () -> Unit)`
- Content: `@Composable internal fun XxxContent(state: UiState<T>, onIntent: (XxxIntent) -> Unit, ...)`
- `collectAsStateWithLifecycle()` (NOT `collectAsState`)
- `remember(viewModel)` wraps intent lambda to stabilize reference
- `CollectEffect(viewModel.effect) { ... }` for one-shot effects
- `LaunchedEffect(Unit) { viewModel.sendIntent(XxxIntent.LoadFirstPage) }` for initial load
- `StatusOverlay(state = uiState, onRetry = onRetry)` for state overlay
- LazyColumn items use `key = { it.id }`

**页面性能 V2 约定:**
- Screen 通过 callback 接收 `onPageVisible`、`onNetworkStart/onNetworkEnd`、`onLocalDataStart/onLocalDataEnd`、`onRenderEnd`
- `onPageVisible` 在首个静态内容或 Loading 状态对应的 `LocalView.current.doOnPreDraw` 中调用
- 数据来源开始点在首次触发主要数据加载前调用,结束点在数据进入 UI 状态后调用
- `Success/Empty/Error` 都必须结束数据计时,并分别传递 `contentState=success/empty/error`
- `onRenderEnd(contentState)` 必须在对应终态内容的 `doOnPreDraw` 中调用,作为 `pageInteractive`
- 主要数据来源只允许 `none/local/network` 三选一;`local` 与 `network` 回调严格二选一
- 同时存在辅助本地读取和网络请求时,只统计驱动页面主要内容的来源

### Step 6: Generate Preview file

Read `references/preview-template.kt` and generate 6 @Preview variants:
1. Loading state
2. Empty state
3. Error state
4. Success with sample data
5. No more data (for paginated lists)
6. Dark mode variant

### Step 7: Generate Activity file + Register Route + Register in Manifest

Read `references/activity-template.kt` and fill in:
- `@AndroidEntryPoint` annotation
- `BaseCommonComposeActivity` base class
- `by viewModels()` for ViewModel
- `screen()` and `initObservable()` implementations

**Route** (自维护，不引入第三方路由框架):
```kotlin
// 页面路由常量(宿主项目路由表统一维护)
const val ROUTE_XXX = "/xxx/compose"
// 跳转:startActivity(Intent(context, XxxComposeActivity::class.java))
```

**Register the Activity in the module's AndroidManifest.xml**:
```xml
<!-- XxxFeatureName Compose Activity -->
<activity
    android:name="com.rainbow.compose.{{module_suffix}}.compose.{{FeatureName}}ComposeActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
    android:exported="false"
    android:launchMode="singleTop"
    android:screenOrientation="portrait"
    android:windowSoftInputMode="adjustPan" />
```
Add this inside `<application>` in `module_xxx/src/main/AndroidManifest.xml`. Never skip this — the Activity won't work without manifest registration.

## Key API Reference

These are the real APIs from the codebase. Use exact signatures.

### Core contracts (from `lib-compose-core`)

```kotlin
// MviContract.kt
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val type: ErrorType, val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
enum class ErrorType { NO_NET, SERVER_ERROR, UNKNOWN }
interface UiIntent
interface UiEffect
```

### ViewModel base (from `lib-compose-core`)

```kotlin
abstract class BaseMviViewModel<I : UiIntent, E : UiEffect>(
    private val effectDelivery: EffectDelivery = EffectDelivery.DELIVER_ALL
) : BaseViewModel() {
    val effect: Flow<E>
    fun sendIntent(intent: I) { viewModelScope.launch { handleIntent(intent) } }
    abstract suspend fun handleIntent(intent: I)
    protected fun sendEffect(effect: E)
    protected fun <T> Flow<T>.toStateFlowWhileSubscribed(initial: T): StateFlow<T>
}
```

### State extensions (from `lib-compose-core`)

```kotlin
fun <T> MutableStateFlow<UiState<T>>.moveLoading()
fun <T> MutableStateFlow<UiState<T>>.moveSuccess(data: T)
fun <T> MutableStateFlow<UiState<T>>.moveError(type: ErrorType = UNKNOWN, message: String)
fun <T> MutableStateFlow<UiState<T>>.moveEmpty()
```

### Effect collection (from `lib-compose-core`)

```kotlin
@Composable
fun <E : UiEffect> CollectEffect(flow: Flow<E>, handler: suspend (E) -> Unit)
// Uses repeatOnLifecycle(STARTED) + Dispatchers.Main.immediate internally
```

### Activity base (from `lib-compose-ui`)

```kotlin
abstract class BaseCommonComposeActivity : BaseComposeActivity() {
    abstract fun initObservable()
    @Composable abstract fun screen()
}
```

### Design System components (from `lib-compose-ui`)

```kotlin
@Composable fun CommonTitleBar(title: String, onBack: () -> Unit, ...)
@Composable fun StatusOverlay(state: UiState<*>, onRetry: (() -> Unit)?, ...)
@Composable fun PullToRefreshLayout(isRefreshing: Boolean, onRefresh: () -> Unit, ...)
@Composable fun ClassicsRefreshIndicator(isRefreshing: Boolean)
```

## Checklist before output

Run through this for every generated file:

- [ ] All UiModel data classes have `@Immutable`
- [ ] Intent/Effect are `sealed class` (not `sealed interface`)
- [ ] No-arg Intent variants use `data object`
- [ ] ViewModel extends `BaseMviViewModel<I, E>` (2 params)
- [ ] State initialized as `MutableStateFlow<UiState<T>>(UiState.Loading)`
- [ ] Uses `moveXxx()` extensions, not raw `_uiState.value = UiState.xxx`
- [ ] Screen and Content are two separate functions
- [ ] `remember(viewModel) { { intent -> viewModel.sendIntent(intent) } }` for lambda stability
- [ ] `key = { it.id }` on LazyColumn items
- [ ] `CollectEffect` for effects, NOT bare `LaunchedEffect + collect`
- [ ] `collectAsStateWithLifecycle()` for StateFlow collection
- [ ] Mapper uses `internal fun` extension function
- [ ] Mapper is in `mapper/` subpackage, not `presentation/`
- [ ] Activity has `@AndroidEntryPoint` annotation
- [ ] Activity registered in `module_xxx/src/main/AndroidManifest.xml` with standard config attributes
- [ ] Content function is `internal` (not `private` — needed for Preview)

## After generating code

Remind the user:
1. Register the Activity in the module's `src/main/AndroidManifest.xml`（如果未自动追加）
2. If the page needs DI dependencies, add them to the ViewModel constructor
5. If the data source is ready, update the Mapper with real DTO types
