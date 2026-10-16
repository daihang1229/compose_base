# Android-specific Review Prompt

> 此文件是自动生成的审查/生成指令组件。
> 在 Copilot Chat 中，请使用 `#file_name` 引用此文件，并让 Copilot 严格根据下文的规范审查选中代码。

## 规范定义：
# AI 辅助 Android 开发特性规范

针对 Android 开发，AI 在生成、重构和审查代码时必须严格遵守以下针对性的开发规范与最佳实践：

## 1. 架构模式与依赖注入 (MVVM + UseCase + Hilt)
- **依赖注入 (Hilt)**: 强烈推荐使用 Hilt 进行依赖注入框架。所有 ViewModel 必须使用 `@HiltViewModel` 注解，并通过 `@Inject constructor` 注入依赖；Activity/Fragment 必须使用 `@AndroidEntryPoint` 注解。在全局或单例级别的数据源、仓库及用例管理上，必须通过相应的 Hilt Module （如 `@InstallIn(SingletonComponent::class)` 或 `@InstallIn(ViewModelComponent::class)`）来提供实例。
- **业务逻辑封装 (UseCase / Interactor)**: 当 ViewModel 中的业务逻辑变得复杂、或者是需要跨多个 ViewModel 复用逻辑时，必须将其剥离到独立的 `UseCase` 类中。
  - `UseCase` 应该是单一职责的，并通过 `operator fun invoke()` 提供执行入口。
  - `UseCase` 中应通过借由 Hilt `@Inject` 注入 Repository 或其他的 API/数据源，严禁在 UseCase 内部存在直接的 Android Framework (如 Context, Activity) 依赖（除必需的应用级 Context 或已注入的组件外）。
- **关注点分离**: 严格履行 `Activity`/`Fragment` 仅作 UI 展示和事件转发的职责，**绝不**将业务逻辑、网络请求或复杂的数据流处理写在 View 层。
- **ViewModel 职责**: 状态管理和核心业务逻辑必须放置在 `ViewModel` 中。ViewModel 应该通过 `StateFlow` 或 `SharedFlow` 将数据暴露给 View 层展示。
- **上下文安全**: `ViewModel` 中**绝对禁止**持有 `Activity` 乃至任意 `View` 的强引用，这是极易引发内存泄漏的做法。如果必须使用 Context，请传入或获取 `ApplicationContext`，或者继承 `AndroidViewModel`（配合注入）。

## 2. 数据持久化与键值存储 (DataStore / KV)
- **统一使用 KV Delegate 模式**: 在项目中进行轻量级键值对存取时，应当统一使用基于属性委托 (Property Delegation) 封装的持久化类（由宿主项目提供并在启动时初始化，例如 `AppKV`）。
  - **绝不**直接在代码的各个角落里使用 `SharedPreferences.getXXX()` 或是重复手写 DataStore 的读取逻辑。
  - 新增的独立状态位、配置属性必须要在该 KV 静态对象中集中注册，并利用 `by delegate("key_name", default_value)` 进行类型安全的一行声明。
  - **示例**:
    ```kotlin
    object AppKV : KVData {
        // ...
        var isAutoUnlock by delegate("sp.auto.unlock", 1)
        var hasLogin by delegate("has.login", false)
    }
    ```

## 3. 协程与异步数据流 (Coroutines & Flow)
- **首选 Flow 响应式编程**: 优先使用 Kotlin `StateFlow` (状态) 和 `SharedFlow` (事件) 替代传统的 `LiveData` 或 `EventBus`，以实现更好的测试性和并发控制。
- **正确且安全的协程作用域**:
  - 在 ViewModel 中，必须统一使用 `viewModelScope.launch`。
  - 在 Activity/Fragment 中，必须使用 `lifecycleScope.launch`。
- **生命周期安全的 Flow 收集**:
  - 在 UI 层收集 Flow 时，**绝对禁止**使用无生命周期感知的简单 `collect`。
  - View（XML）侧必须使用 `repeatOnLifecycle(Lifecycle.State.STARTED)` 包裹收集；Compose 侧必须使用 `collectAsStateWithLifecycle()` 或框架封装的 `CollectEffect`，确保退到后台时停止收集，节省资源并避免崩溃。
  - **示例**:
    ```kotlin
    // XML 侧
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { state -> /* update UI */ }
        }
    }

    // Compose 侧
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ```

## 4. 日志记录与数据埋点 (Logging & Tracking)
- **统一的日志工具**: 禁止直接使用 Android 原生的 `android.util.Log` 或 `System.out.println` 输出任何日志。必须使用统一封装的日志控制类（本框架内置 `com.rainbow.compose.core.log.ComposeLog`，宿主机可替换为自定义实现），以确保日志能够在生产环境下被正确脱敏、分级或闭环关闭。
  > ⚠️ **TODO**: 通用日志系统的完整设计（分级策略、生产关闭、回捞机制）尚未定稿，待后续统一设定后再补充规范。
- **规范的无侵入监控**: 分析事件或页面埋点应当统筹调用统一封装的埋点工具类集中处理。
  > ⚠️ **TODO**: 埋点能力当前**不引入**，待宿主项目确定上报方案后再补充。

## 5. UI 操作、视图处理与列表构建
- **优先使用 ViewBinding**: 优先使用且规范使用 `ViewBinding` 操作 UI，严禁使用已废弃的 Kotlin Synthetics / `kotlin-android-extensions` (直接使用 id 引用) 或传统的 `findViewById`。Compose 页面则直接使用 Compose 声明式 UI。
- **Fragment 内存泄漏阻断**: 在 Fragment 中使用 ViewBinding 时，必须存在释放机制，在 `onDestroyView` 中将其置为 `null`，避免 View 的隐式泄漏。
- **避免臃肿的 Activity/Fragment**: 善用 Kotlin 的 Extension Functions (扩展函数) 来封装通用的 UI 样式调整、Toast 提示等基础操作。
- **View 点击防抖处理**: 对于所有的 UI 控件点击事件，**禁止**直接使用原生的 `setOnClickListener`，必须使用统一封装的 `onClickDelay` 扩展防抖函数（默认 500ms 间隔），以防短时间内的重复误触导致的多次网络请求或多次跳转页面。Compose 侧使用 `Modifier.onClickDelay`。
- **UI 尺寸及单位换算**: 当在代码动态构建或者调整视图大小、间距时，**必须**使用统一提供的 `dp`、`px` 相关拓展（如 `10.dp`、`20f.px`），保持代码的一致性与可读性。
- **RecyclerView 的规范构建 (MultiType)**: 当遇到包含多种视图类型（复杂页、多态列表页）的 `RecyclerView` 时，**必须使用 [drakeet 开发的 MultiType 框架](https://github.com/drakeet/MultiType)** 来构建 `Adapter` 及各个子模块。严禁使用原生的、充斥着满屏 `if-else / switch case` 和繁杂 `getItemViewType()` 判断的意大利面条式（Spaghetti Code）旧有方式构建，应当利用 `ItemViewBinder` 解耦对应的 UI 层展示，保持每种样式和实体的绝对分离和单一职责。（Compose 列表使用 `LazyColumn` + `key`）
- **自定义 TextView 规范**: 所有的文本控件 (`TextView`) 必须使用统一封装的文本组件，而非原生 `TextView` 零散设置样式。
- **圆角视图规范**: 若需要展示带有圆角的视图或图片，必须使用统一提供的圆角组件，而非在业务处重复实现。
- **网络图片加载规范**: 所有的网络图片加载必须使用统一封装的图片加载入口（Compose 侧 `CommonImage`，基于 Coil），直接处理占位图、错误图及生命周期绑定，严禁在业务模块中零散调用原始的图片加载 API。

## 6. 生命周期管理 (Lifecycle Awareness)
- **谨慎更新 UI**: 任何异步任务的回调、延时任务的执行（如 `postDelayed`），在更新 UI 代码执行前应当考虑检查 `isAdded` (Fragment) 或 `isFinishing` / `isDestroyed` (Activity)，避免抛出致命的 `IllegalStateException` 或 NPE。
- **后台持久任务**: 若属于离开页面也必须完成的长线后台任务，应推荐或使用 `WorkManager` 或 `Foreground Service`，而不是继续挂载在页面的 CoroutineScope 中。

## 7. 语言特性推荐 (Kotlin 优先)
- **空安全与规范提取**: 强烈且极力建议避免使用强制非空断言 `!!`。必须根据场景选用 `?` 配合作用域函数 `let/apply/also` 处理，或提供合理默认值 `?:`，或者在流程前置节点使用 `requireNotNull` / `checkNotNull` 中断执行并明确抛出清晰异常。
- **代码简洁性**: 主动运用 Kotlin 成熟的高阶函数处理集合 (`map`, `filter`, `associate`, `groupBy` 等) 取代繁琐的 `for` 循环遍历。使用 `sealed class` 或 `sealed interface` 来定义清晰的 UI 状态类或事件类型。
