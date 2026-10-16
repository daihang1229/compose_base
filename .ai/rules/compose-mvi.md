# Compose MVI 架构规范

> **版本**: 2.0.0
> **适用范围**: 经 `/compose` 触发的 Compose MVI 页面
> **触发条件**: `/compose <需求>` 产出 `01-requirement.md` 标 `UI 架构: Compose MVI` → `/design` 走 Compose 设计分支 → `/c-dev` compose-dev Agent 实施 → `/c-review` compose-review Agent 审查
> **⚠️ Compose 与 XML 各自独立工作流**:Compose 走 compose-dev/compose-review 链;XML 走 dev/review 链。两者不混淆。

---

## 1. 文件结构规范(7 文件强制)

每个 Compose MVI feature 必须生成 **7 个文件**（路由常量可选，由宿主项目路由表维护）:

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

**包名约定**: `com.rainbow.compose.<module_suffix>.compose`

---

## 2. Contract 规范(XxxContract.kt)

### 2.1 UiModel

- 所有 UiModel data class 必须 `@Immutable`
- 所有字段必须 `val`,有默认值(防御性)
- 禁止 `var` 字段

```kotlin
@Immutable
data class XxxItem(
    val id: Int,
    val title: String,
    val coverUrl: String = "",
)
```

### 2.2 State(分页列表必须自定义 State)

```kotlin
@Immutable
data class XxxListState(
    val records: List<XxxItem> = emptyList(),
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
)
```

### 2.3 Intent(必须 sealed class,非 sealed interface)

```kotlin
sealed class XxxIntent : UiIntent {
    data object LoadFirstPage : XxxIntent()    // 无参用 data object
    data object Refresh : XxxIntent()
    data class SelectItem(val id: Int) : XxxIntent()  // 有参用 data class
}
```

**命名**: `VerbNoun` 格式(如 `ToggleFavorite` / `SelectEpisode` / `LoadFirstPage`)

### 2.4 Effect(必须 sealed class,一个副作用一个子类)

```kotlin
sealed class XxxEffect : UiEffect {
    data class ShowToast(val message: String) : XxxEffect()
    data class NavigateToDetail(val id: Int) : XxxEffect()
}
```

**EffectDelivery 选择**:
- `DELIVER_ALL` — 导航 / 支付(确保不丢失)
- `LATEST_ONLY` — Toast(避免堆积)

---

## 3. ViewModel 规范(XxxViewModel.kt)

### 3.1 基类继承(2 泛型参数)

```kotlin
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val useCase: GetXxxUseCase
) : BaseMviViewModel<XxxIntent, XxxEffect>(
    effectDelivery = EffectDelivery.DELIVER_ALL
) {
    // ...
}
```

**关键**:
- `BaseMviViewModel<I, E>` 是 2 泛型参数(非 3)
- `BaseMviViewModel` extends `BaseViewModel()` → 兼容现有架构基类要求
- `sendIntent()` 是唯一公开入口(已在基类)
- `handleIntent()` 必须是 `abstract suspend fun`

### 3.2 State 策略(二选一)

**策略 1 — 单 StateFlow(简单页面)**:
```kotlin
private val _uiState = MutableStateFlow<UiState<XxxItem>>(UiState.Loading)
val uiState: StateFlow<UiState<XxxItem>> = _uiState.asStateFlow()
```

**策略 2 — 双 StateFlow(分页列表)**:
```kotlin
private val _uiState = MutableStateFlow<UiState<List<XxxItem>>>(UiState.Loading)
val uiState: StateFlow<UiState<List<XxxItem>>> = _uiState.asStateFlow()
private val _listState = MutableStateFlow(XxxListState())
val listState: StateFlow<XxxListState> = _listState.asStateFlow()
```

### 3.3 状态切换(必须用 moveXxx 扩展)

```kotlin
// ✅ 正确
_uiState.moveLoading()
_uiState.moveSuccess(data)
_uiState.moveError(ErrorType.NO_NET, "网络错误")
_uiState.moveEmpty()

// ❌ 禁止原始赋值
_uiState.value = UiState.Loading
_uiState.value = UiState.Success(data)
```

### 3.4 handleIntent(必须 exhaustive when)

```kotlin
override suspend fun handleIntent(intent: XxxIntent) {
    when (intent) {
        XxxIntent.LoadFirstPage -> loadFirstPage()
        XxxIntent.Refresh -> refresh()
        is XxxIntent.SelectItem -> selectItem(intent.id)
    }
}
```

### 3.5 防竞态

```kotlin
private var fetchJob: Job? = null

private fun fetchPage(page: Int) {
    fetchJob?.cancel()  // 必须先 cancel 旧任务
    fetchJob = viewModelScope.launch {
        // API 调用 → Mapper 转换 → moveXxx
    }
}
```

### 3.6 配置变更存活(rememberSavable)

配置变更(旋转/分屏/折叠)时 `remember` 会丢失状态。需要存活的场景必须用 `rememberSavable`:

```kotlin
// ✅ 输入框文本 — 旋转后不丢失
var text by rememberSaveable { mutableStateOf("") }

// ✅ LazyColumn 滚动位置 — 旋转后恢复
val scrollState = rememberLazyListState()
// LazyListState 实现了 Saver,不需要额外处理

// ❌ 禁止 — 旋转后丢失
var text by remember { mutableStateOf("") }
```

**适用场景**: 输入框、搜索框、展开/折叠状态、Tab 选中位置。**不适用**: StateFlow 驱动的 UI 状态(已由 ViewModel 跨配置变更存活)。

---

## 4. Screen 规范(XxxScreen.kt)

### 4.1 Screen/Content 双函数分离(强制)

```kotlin
// Screen: 持有 ViewModel,收集 State/Effect
@Composable
fun XxxScreen(viewModel: XxxViewModel, onBack: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState by viewModel.listState.collectAsStateWithLifecycle()

    val onIntent: (XxxIntent) -> Unit = remember(viewModel) {
        { intent -> viewModel.sendIntent(intent) }
    }

    LaunchedEffect(Unit) { viewModel.sendIntent(XxxIntent.LoadFirstPage) }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is XxxEffect.ShowToast -> { /* TODO */ }
        }
    }

    XxxContent(uiState = uiState, listState = listState, onIntent = onIntent, onBack = onBack)
}

// Content: 纯渲染,不接触 ViewModel,可独立 @Preview
@Composable
internal fun XxxContent(
    modifier: Modifier = Modifier,
    uiState: UiState<List<XxxItem>>,
    listState: XxxListState,
    onIntent: (XxxIntent) -> Unit,
    onBack: () -> Unit,
) {
    // ...
}
```

**关键约束**:
- Screen 和 Content 必须是两个独立函数
- Content 必须是 `internal fun`(非 `private` — Preview 需要)
- Content 参数只有 State + Callback,不含 ViewModel
- Content 第一个参数必须是 `modifier: Modifier = Modifier`(Google Compose API 设计指南)

### 4.2 StateFlow 收集(必须 collectAsStateWithLifecycle)

```kotlin
// ✅ 正确
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// ❌ 禁止 collectAsState(无生命周期感知)
val uiState by viewModel.uiState.collectAsState()
```

### 4.3 Effect 收集(必须 CollectEffect)

```kotlin
// ✅ 正确
CollectEffect(viewModel.effect) { effect ->
    when (effect) {
        is XxxEffect.ShowToast -> { /* ... */ }
    }
}

// ❌ 禁止裸 LaunchedEffect + collect
LaunchedEffect(Unit) {
    viewModel.effect.collect { /* ... */ }
}
```

### 4.4 lambda 稳定(必须 remember 包裹)

```kotlin
// ✅ 正确 — remember 包裹,稳定引用
val onIntent: (XxxIntent) -> Unit = remember(viewModel) {
    { intent -> viewModel.sendIntent(intent) }
}
val onRefresh = remember(onIntent) { { onIntent(XxxIntent.Refresh) } }
val onLoadMore = remember(onIntent) { { onIntent(XxxIntent.LoadMore) } }
val onRetry = remember(onIntent) { { onIntent(XxxIntent.LoadFirstPage) } }

// ❌ 禁止 — 每次 recomposition 创建新 lambda
val onIntent: (XxxIntent) -> Unit = { intent -> viewModel.sendIntent(intent) }
```

### 4.5 LazyColumn items(必须 key)

```kotlin
// ✅ 正确
LazyColumn {
    items(listState.records, key = { it.id }) { item ->
        XxxCard(item)
    }
}

// ❌ 禁止 — 无 key 导致无效 recomposition
LazyColumn {
    items(listState.records) { item ->
        XxxCard(item)
    }
}
```

### 4.6 状态覆盖(必须 StatusOverlay)

```kotlin
Box(Modifier.fillMaxSize()) {
    Column { CommonTitleBar(...); PullToRefreshLayout(...) { LazyColumn {...} } }
    StatusOverlay(state = uiState, onRetry = onRetry)  // 自动处理 Loading/Error/Empty
}

// ❌ 禁止手写 if-else 处理状态
if (uiState is UiState.Loading) { CircularProgressIndicator() }
else if (uiState is UiState.Error) { Text("错误") }
```

### 4.7 初始加载(必须 LaunchedEffect(Unit))

```kotlin
LaunchedEffect(Unit) { viewModel.sendIntent(XxxIntent.LoadFirstPage) }
```

### 4.8 触底加载更多(derivedStateOf)

```kotlin
val shouldLoadMore by remember {
    derivedStateOf { /* lastVisible >= totalItems - 3 */ }
}
LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore && listState.hasMore && !listState.isLoadingMore) {
        onIntent(XxxIntent.LoadMore)
    }
}
```

---

## 5. Mapper 规范(XxxMapper.kt)

### 5.1 包位置(必须在 compose/mapper/ 子包)

```kotlin
package com.rainbow.compose.xxx.compose.mapper  // ✅ compose/mapper/ 子包

// ❌ 禁止放在 presentation/ 子包
// package com.rainbow.compose.xxx.compose.presentation
```

### 5.2 扩展函数风格(必须 internal fun)

```kotlin
internal fun DtoType.toXxxItem(): XxxItem = XxxItem(
    id = this.id,
    title = this.name,
)
```

### 5.3 DTO 不泄漏(防腐层)

- DTO 类型禁止出现在任何 presentation/ 文件(Contract / ViewModel / Screen / Preview)
- DTO 只能在 mapper/ 文件中出现
- UiModel 不含 DTO 字段

---

## 6. Activity 规范(XxxComposeActivity.kt)

```kotlin
@AndroidEntryPoint
class XxxComposeActivity : BaseCommonComposeActivity() {
    private val viewModel: XxxViewModel by viewModels()

    override fun initObservable() {
        // 旁路副作用流(可选,如全局事件订阅)
    }

    @Composable
    override fun screen() {
        XxxScreen(viewModel = viewModel, onBack = { finish() })
    }
}
```

**必须**:
- `@AndroidEntryPoint` 注解
- 继承 `BaseCommonComposeActivity`(非 BaseActivity)
- `by viewModels()` 获取 ViewModel
- `screen()` 返回顶层 Composable
- `initObservable()` 处理旁路副作用流(可选)

**路由**(自维护,不引入第三方路由框架):
```kotlin
// 页面路由常量(宿主项目路由表统一维护)
const val ROUTE_XXX = "/xxx/compose"
// 跳转:startActivity(Intent(context, XxxComposeActivity::class.java).putExtra(...))
```

**Manifest 注册**(`module_xxx/src/main/AndroidManifest.xml`):
```xml
<activity
    android:name="com.rainbow.compose.xxx.compose.XxxComposeActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
    android:exported="false"
    android:launchMode="singleTop"
    android:screenOrientation="portrait"
    android:windowSoftInputMode="adjustPan" />
```

---

## 7. Preview 规范(XxxScreenPreview.kt)

必须生成 **6 个 @Preview**,全部包在 `CommonTheme` 中:

1. Loading state
2. Empty state
3. Error state(`UiState.Error(ErrorType.NO_NET, "...")`)
4. Success with sample data(至少 3 条样本)
5. No more data(`hasMore=false`,分页列表用)
6. Dark mode(`CommonTheme(darkTheme = true)`)

每个 Preview 推荐加 `showBackground = true`、`showSystemUi = true`,便于检查状态栏/导航栏适配:

```kotlin
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun XxxContentLoadingPreview() {
    CommonTheme {
        XxxContent(
            modifier = Modifier,
            uiState = UiState.Loading,
            listState = XxxListState(),
            onIntent = {},
            onBack = {},
        )
    }
}
```

---

## 8. Design System 组件(lib-compose-ui)

禁止手写下拉刷新 / 标题栏 / 状态覆盖层,必须用 Design System:

| 组件 | 用途 | 禁止替代 |
|---|---|---|
| `CommonTitleBar(title, onBack)` | 标准标题栏 | 手写 TopAppBar |
| `StatusOverlay(state, onRetry)` | Loading/Error/Empty 覆盖 | 手写 if-else |
| `PullToRefreshLayout(isRefreshing, onRefresh, ...)` | 下拉刷新容器 | 手写 SwipeRefresh |
| `ClassicsRefreshIndicator(isRefreshing)` | 上拉加载指示器 | 手写 CircularProgressIndicator |
| `BaseCommonComposeActivity` | Compose Activity 基类 | 继承 BaseActivity |

> **自定义 View 迁移映射表**: `.ai/custom-view-mapping.yaml`
> Migration Agent 分析旧页面时读取此表,自动匹配自定义 View 的 Compose 等价物。
> 未映射的 View 需在此表追加映射(开发负责),验证 3+ 次无误的映射可升级为本节规则。

---

## 9. 资源引用规范

- 字符串:走统一资源入口(非硬编码文案)
- 颜色:走 CommonTheme color scheme(非硬编码 Color.White)
- 尺寸:走 CommonTheme dimens(非硬编码 dp)

### 9.1 Drawable 资源类型限制(painterResource 迁移坑)

> ⚠️ **XML → Compose 迁移高频踩坑**:旧 XML `ImageView.setImageResource()` 支持所有 drawable 类型,但 Compose `Image(painter = painterResource(id))` **只认两种**,其余类型 runtime crash(`IllegalArgumentException: Only VectorDrawables and rasterized asset types are supported`)。

| drawable 类型 | `painterResource()` 支持? | 说明 |
|---|---|---|
| `<vector>` | ✅ | 矢量图,首选 |
| PNG / JPG / WEBP | ✅ | 栅格图,直接引用 |
| `<bitmap>` XML 包装器 | ❌ crash | bitmap + gravity 包装,需跳过找底层 raster |
| `<shape>` | ❌ crash | shape/rectangle/oval,需改 Compose `Modifier.background(shape)` |
| `<layer-list>` | ❌ crash | 多层叠加,需拆成多个 Compose 组件 |
| `<selector>` | ❌ crash | 状态选择,需改 `when` + state 逻辑 |
| `<inset>` / `<ripple>` / `<clip>` | ❌ crash | 其他 XML drawable,需改 Compose 等价实现 |

**迁移规则**:
1. 端口 XML drawable 前,**必须先查 drawable 文件类型**(打开 `.xml` 看根标签)
2. 若是 `<vector>` 或栅格图 → 直接 `painterResource()`
3. 若是 `<bitmap>` 包装器 → 跳过包装器,直接加载底层 `@drawable/xxx` 栅格资源
4. 若是 `<shape>`/`<layer-list>`/`<selector>` → 改用 Compose 等价 API(`Modifier.background(shape)` / 多组件叠加 / `when` 状态分支)

**跨模块资源引用**(library 模块引用 app 模块 drawable):
- library 的 R 类不包含 app 模块资源字段 → 编译期 `R.drawable.xxx` 找不到
- 用运行时 `context.resources.getIdentifier("name", "drawable", context.packageName)` 查 ID,`!= 0` 时 `painterResource(id)`,`== 0` 走 Text/兜底
- 仅在必须引用 app 模块资源时用(如 `app_name` logo),优先方案是下沉资源到共享 library

---

## 10. Modifier 顺序规范

```kotlin
// ✅ 正确 — clickable 在 padding 外(增大点击区域)
Box(
    Modifier
        .clickable { onClick() }
        .padding(16.dp)
)

// ❌ 禁止 — padding 在外(点击区域被 padding 吞掉)
Box(
    Modifier
        .padding(16.dp)
        .clickable { onClick() }
)
```

---

## 11. 核心契约 API(lib-compose-core)

> **源码位置**: 以下 API 定义在 `lib-compose-core` 模块中,是 Compose MVI 架构的运行时依赖。
> 若未来升级 API,需同步更新本文档,避免规范与实现漂移。

```kotlin
// lib-compose-core/.../core/domain/contract/MviContract.kt — UiState 五态
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

// lib-compose-core/.../core/domain/contract/BaseMviViewModel.kt — ViewModel 基类
abstract class BaseMviViewModel<I : UiIntent, E : UiEffect>(
    private val effectDelivery: EffectDelivery = EffectDelivery.DELIVER_ALL
) : BaseViewModel() {
    val effect: Flow<E>
    fun sendIntent(intent: I)
    abstract suspend fun handleIntent(intent: I)
    protected fun sendEffect(effect: E)
}

// lib-compose-core/.../core/domain/contract/UiStateExt.kt — State 扩展函数
fun <T> MutableStateFlow<UiState<T>>.moveLoading()
fun <T> MutableStateFlow<UiState<T>>.moveSuccess(data: T)
fun <T> MutableStateFlow<UiState<T>>.moveError(type: ErrorType = UNKNOWN, message: String)
fun <T> MutableStateFlow<UiState<T>>.moveEmpty()

// lib-compose-core/.../core/ui/CollectEffect.kt — Effect 收集 Composable
@Composable
fun <E : UiEffect> CollectEffect(flow: Flow<E>, handler: suspend (E) -> Unit)
```

---

## 12. 检查清单

### Contract
- [ ] 所有 UiModel data class 有 `@Immutable`
- [ ] 所有字段 `val`(无 `var`)
- [ ] Intent 是 `sealed class`(非 `sealed interface`)
- [ ] 无参 Intent 用 `data object`
- [ ] Effect 是 `sealed class`,一个副作用一个子类

### ViewModel
- [ ] 继承 `BaseMviViewModel<I, E>`(2 泛型参数)
- [ ] State 初始化为 `MutableStateFlow<UiState<T>>(UiState.Loading)`
- [ ] 用 `moveLoading/Success/Error/Empty` 扩展(非原始赋值)
- [ ] `handleIntent` 是 `abstract suspend fun` + exhaustive `when`
- [ ] `fetchJob?.cancel()` 防竞态
- [ ] `sendIntent()` 是唯一公开入口
- [ ] 配置变更敏感状态用 `rememberSavable`(非 `remember`)

### Screen
- [ ] Screen 和 Content 是两个独立函数
- [ ] Content 是 `internal fun`(非 `private`)
- [ ] Content 参数无 ViewModel(只有 State + Callback)
- [ ] Content 第一个参数是 `modifier: Modifier = Modifier`
- [ ] `collectAsStateWithLifecycle()`(非 `collectAsState()`)
- [ ] `CollectEffect`(非裸 `LaunchedEffect + collect`)
- [ ] `remember(viewModel)` 包 intent lambda
- [ ] `remember(onIntent)` 包所有回调 lambda
- [ ] LazyColumn items 有 `key = { it.id }`
- [ ] `StatusOverlay` 处理状态(非手写 if-else)
- [ ] `LaunchedEffect(Unit)` 触发初始加载

### Mapper
- [ ] 在 `compose/mapper/` 子包(非 `presentation/`)
- [ ] `internal fun` 扩展函数风格
- [ ] DTO 不出现在 presentation/ 文件

### Activity
- [ ] `@AndroidEntryPoint` 注解
- [ ] 继承 `BaseCommonComposeActivity`
- [ ] `by viewModels()` 获取 ViewModel
- [ ] Activity 已注册到 `module_xxx/src/main/AndroidManifest.xml`

### Preview
- [ ] 6 个 @Preview(Loading/Empty/Error/WithData/NoMore/DarkMode)
- [ ] 全部包在 `CommonTheme` 中
- [ ] 每个 Preview 推荐 `showBackground = true, showSystemUi = true`

### 资源与 Modifier
- [ ] 字符串用统一资源入口(非硬编码文案)
- [ ] 颜色走 CommonTheme color scheme(非硬编码)
- [ ] Modifier 顺序:`clickable` 在 `padding` 外
- [ ] **Drawable 类型**:端口 XML drawable 前已查类型(`<vector>`/栅格可直用,`<bitmap>`/`<shape>`/`<layer-list>`/`<selector>` 需跳过或改 Compose 等价 API)

---

**Template 版本**: v2.0(对应 `/compose` v2.0,compose-dev/compose-review 链)
