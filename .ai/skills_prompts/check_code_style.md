# Code-style Review Prompt

> 此文件是自动生成的审查/生成指令组件。
> 在 Copilot Chat 中，请使用 `#file_name` 引用此文件，并让 Copilot 严格根据下文的规范审查选中代码。

## 规范定义：
# 代码风格规范

## 1. 命名规范

> 命名的完整规则（类、函数、变量、资源、包结构等）请参阅 **[naming.md](naming.md)**。

## 2. 格式规范

### 2.1 缩进和换行

```kotlin
// 缩进：4 个空格（不使用 Tab）
class User(
    val id: String,
    val name: String,
    val email: String
)

// 链式调用换行
val result = list
    .filter { it.isActive }
    .map { it.name }
    .sorted()
    .take(10)

// 参数过长时换行
fun processUser(
    userId: String,
    userName: String,
    userEmail: String,
    callback: (Result<User>) -> Unit
) {
    // ...
}
```

### 2.2 大括号

```kotlin
// 左大括号不换行
if (condition) {
    doSomething()
} else {
    doOther()
}

// 单行表达式可省略大括号
if (condition) doSomething()

// when 表达式
when (value) {
    1 -> handleOne()
    2 -> handleTwo()
    else -> handleOther()
}
```

### 2.3 空格

```kotlin
// 操作符两侧加空格
val sum = a + b
val range = 1..10

// 冒号
class MyClass : BaseClass()
fun getValue(): String
val map: Map<String, Int>

// lambda
list.map { it.name }
list.forEach { item -> process(item) }

// 逗号后加空格
fun foo(a: Int, b: String, c: Boolean)
```

### 2.4 空行

```kotlin
class UserViewModel {
    
    // 属性之间：相关属性不加空行，不相关加一个空行
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()
    
    // 函数之间：加一个空行
    fun loadData() {
        // ...
    }
    
    fun refresh() {
        // ...
    }
}
```

### 2.5 行宽限制

- 单行不超过 **120 字符**
- 超出时按参数列表、链式调用、条件表达式等自然断点换行

## 3. 函数规范

### 3.1 函数长度

- 函数不超过 **30 行**
- 如果超过，考虑拆分为多个小函数

### 3.2 表达式函数体

```kotlin
// 单行表达式优先使用表达式函数体
fun getFullName() = "$firstName $lastName"

fun isValid() = name.isNotBlank() && email.contains("@")

// 多行表达式也可以使用
fun calculateTotal() = items
    .filter { it.isSelected }
    .sumOf { it.price }
```

### 3.3 参数默认值

```kotlin
// 使用默认参数替代重载
fun showDialog(
    title: String,
    message: String,
    positiveText: String = "确定",
    negativeText: String? = null,
    cancelable: Boolean = true
)
```

### 3.4 扩展函数

```kotlin
// 合理使用扩展函数增强可读性
fun String.isValidEmail(): Boolean {
    return this.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

// 使用
if (email.isValidEmail()) { ... }
context.showToast("保存成功")
```

## 4. 类规范

### 4.1 类长度

- 类不超过 **300 行**
- 单个文件不超过 **500 行**
- 如果超过，考虑拆分职责

### 4.2 类成员顺序

```kotlin
class MyClass {
    // 1. companion object
    companion object {
        private const val TAG = "MyClass"
    }
    
    // 2. 属性声明
    private val _state = MutableStateFlow<State>()
    val state = _state.asStateFlow()
    
    // 3. 初始化块
    init {
        // ...
    }
    
    // 4. 构造函数
    constructor(param: String) : this() {
        // ...
    }
    
    // 5. 公开函数
    fun publicMethod() { }
    
    // 6. 内部/私有函数
    private fun privateMethod() { }
    
    // 7. 内部类
    inner class InnerClass { }
}
```

### 4.3 数据类

```kotlin
data class User(
    val id: String,
    val name: String,
    val email: String
)

// 可以添加计算属性
data class Order(
    val items: List<OrderItem>,
    val discount: Double = 0.0
) {
    val totalPrice: Double
        get() = items.sumOf { it.price } * (1 - discount)
}
```

### 4.4 密封类

```kotlin
// 使用 sealed class/interface 表示受限的类型层次
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
    object Loading : Result<Nothing>
}

// 使用 when 处理所有情况
fun handleResult(result: Result<User>) {
    when (result) {
        is Result.Success -> showUser(result.data)
        is Result.Error -> showError(result.exception)
        is Result.Loading -> showLoading()
    }
}
```

## 5. 控制流

### 5.1 when 替代 if-else

```kotlin
// 使用 when 替代多个 if-else
fun getDescription(status: Status) = when (status) {
    Status.PENDING -> "待处理"
    Status.PROCESSING -> "处理中"
    Status.COMPLETED -> "已完成"
    Status.FAILED -> "失败"
}
```

### 5.2 提前返回

```kotlin
// 使用提前返回减少嵌套
fun processUser(user: User?): Result {
    if (user == null) return Result.Error("User is null")
    if (user.name.isBlank()) return Result.Error("Name is empty")
    if (!user.email.isValidEmail()) return Result.Error("Invalid email")
    
    return doProcess(user)
}
```

## 6. 空安全

### 6.1 避免 !! 操作符

```kotlin
// 使用安全调用和 Elvis 操作符
val name = user?.name ?: "Unknown"
val length = text?.length ?: 0

// 使用 let
user?.let { 
    processUser(it)
}
```

### 6.2 平台类型处理

```kotlin
// 来自 Java 的返回值是平台类型，显式声明可空性
val view: View? = findViewById(R.id.myView)  // 可能返回 null
val context: Context = requireContext()       // 确保不为 null
```

## 7. 集合操作

### 7.1 优先使用函数式操作

```kotlin
val activeUserNames = users
    .filter { it.isActive }
    .map { it.name }
    .sorted()
```

### 7.2 选择正确的集合类型

```kotlin
// 只读集合
val list: List<User> = listOf()
val set: Set<String> = setOf()
val map: Map<String, User> = mapOf()

// 可变集合（仅在需要时使用）
val mutableList: MutableList<User> = mutableListOf()
```

## 8. 注释规范

### 8.1 KDoc 注释

```kotlin
/**
 * 用户仓库，负责用户数据的获取和存储。
 *
 * @property remoteDataSource 远程数据源
 * @property localDataSource 本地数据源
 */
class UserRepository(
    private val remoteDataSource: RemoteDataSource,
    private val localDataSource: LocalDataSource
) {
    /**
     * 根据 ID 获取用户。
     *
     * @param userId 用户 ID
     * @return 用户信息，如果不存在则返回 null
     * @throws NetworkException 网络请求失败时抛出
     */
    suspend fun getUser(userId: String): User? {
        // ...
    }
}
```

### 8.2 TODO 注释

```kotlin
// TODO: 优化性能，考虑使用缓存
// FIXME: 修复边界情况的处理
// HACK: 临时解决方案，等待后端修复后移除
```

## 9. XML 代码风格

### 9.1 XML 缩进与格式

```xml
<!-- 每个属性独立一行，使用 4 个空格缩进 -->
<Button
    android:id="@+id/btnSubmit"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="提交"
    android:textColor="@color/white" />
```

- 缩进使用 **4 个空格**（与 Kotlin 一致）
- 属性超过 2 个时，每个属性单独一行
- 自闭合标签使用 `/>` 结尾

### 9.2 属性排序规则

```xml
<TextView
    android:id="@+id/tvUserName"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:layout_constraintTop_toTopOf="parent"
    android:text="用户名"
    android:textColor="@color/black"
    android:textSize="16sp"
    tools:text="预览文本" />
```

**属性排序优先级**（从上到下）：

1. `xmlns:` 命名空间声明（仅根元素）
2. `style` 属性
3. `android:id`
4. `android:layout_width` / `android:layout_height`
5. 其他 `android:layout_*` 属性（margin、gravity、constraint 等）
6. 其他 `android:` 属性（text、textColor、background 等）
7. `app:` 属性
8. `tools:` 属性（最后）

### 9.3 Namespace 声明

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
```

- 根元素必须声明所有用到的 namespace
- 顺序：`xmlns:android` → `xmlns:app` → `xmlns:tools`

### 9.4 Style 与 Theme 使用

```xml
<!-- 提取为 Style，避免重复 -->
<TextView
    style="@style/TextAppearance.App.Body"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

**提取规则**：

- 相同属性组合出现 **3 次以上**时，应提取为 Style
- 通用按钮、文本样式等应使用预定义 Style
- 不要在 XML 中重复写 textSize、textColor 等通用属性

### 9.5 `tools:` 属性

```xml
<!-- 使用 tools:text 做布局预览 -->
<TextView
    android:id="@+id/tvUserName"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    tools:text="张三" />
```

**常用 tools 属性**：

- `tools:text` - 预览文本内容
- `tools:src` - 预览 ImageView 图片
- `tools:listitem` - 预览 RecyclerView 列表项
- `tools:visibility` - 预览时的可见性

## 10. 作用域函数

### 10.1 选择指南

| 函数 | 对象引用 | 返回值 | 典型场景 |
|------|---------|--------|---------|
| `let` | `it` | Lambda 结果 | 空安全调用、局部变量转换 |
| `apply` | `this` | 对象本身 | 对象初始化、Builder 配置 |
| `run` | `this` | Lambda 结果 | 对象配置 + 计算结果 |
| `also` | `it` | 对象本身 | 附加操作（日志、验证） |
| `with` | `this` | Lambda 结果 | 对已有对象调用多个方法 |

### 10.2 用法示例

```kotlin
// let — 空安全 + 转换
val length = name?.let { it.trim().length } ?: 0

// apply — 对象初始化
val intent = Intent(context, TargetActivity::class.java).apply {
    putExtra("key", value)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

// also — 附加副作用
val user = repository.getUser(id).also {
    logger.d(TAG, "Fetched user: ${it.id}")
}

// with — 批量操作已有对象（推荐用于 ViewBinding）
with(binding) {
    tvTitle.text = item.title
    tvSubtitle.text = item.subtitle
    ivCover.load(item.imageUrl)
}
```

### 10.3 反模式

```kotlin
// 改用安全调用链，避免嵌套超过 2 层
val city = user?.address?.city ?: "Unknown"
```

## 11. 协程与 Flow

### 11.1 协程启动

```kotlin
// ViewModel 中使用 viewModelScope
viewModelScope.launch {
    val result = withContext(Dispatchers.IO) {
        repository.fetchData()
    }
    _uiState.value = result.toUiState()
}

// ❌ 禁止 GlobalScope
// ❌ 禁止在主线程执行 I/O
```

### 11.2 Flow 收集

```kotlin
// Fragment 中安全收集 Flow
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            updateUI(state)
        }
    }
}

// 多个 Flow 并行收集
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect { updateUI(it) } }
        launch { viewModel.events.collect { handleEvent(it) } }
    }
}
```

### 11.3 StateFlow vs SharedFlow

| 场景 | 选择 | 原因 |
|------|------|------|
| UI 状态 | `StateFlow` | 有初始值，自动去重，新订阅者收到最新值 |
| 一次性事件 | `Channel` + `receiveAsFlow()` | 不丢失、不重放 |
| 广播事件 | `SharedFlow(replay=0)` | 多订阅者，不缓存历史 |

```kotlin
// StateFlow — UI 状态
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Channel — 一次性事件（Toast、导航）
private val _events = Channel<UiEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()
```

## 12. ViewBinding

### 12.1 Activity

```kotlin
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
```

### 12.2 Fragment

```kotlin
class HomeFragment : BaseFragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // 必须置空，防止内存泄漏
    }
}
```

### 12.3 批量操作推荐 `with(binding)`

```kotlin
private fun updateUI(state: UiState) {
    with(binding) {
        tvTitle.text = state.title
        tvSubtitle.text = state.subtitle
        progressBar.isVisible = state.isLoading
    }
}
```

## 13. 扩展函数组织

- 文件命名：`XxxExt.kt` 或 `XxxExtensions.kt`（如 `ViewExt.kt`、`StringExt.kt`）
- 存放位置：`ext/` 或 `extensions/` 包下
- 按被扩展类型分文件，避免单个文件过大
- 公共扩展放通用模块，业务扩展留在 feature 模块

```
ext/
├── ViewExt.kt        // View 相关扩展
├── StringExt.kt      // String 相关扩展
├── ContextExt.kt     // Context 相关扩展
└── FlowExt.kt        // Flow 相关扩展
```

## 14. 日志

- **禁止** `android.util.Log`，必须使用项目统一日志工具（如 `Timber` 或自定义 `Logger`）
- **禁止**在日志中打印明文敏感数据（Token、密码、手机号等 PII）
- TAG 使用 `companion object` 中的常量，值与类名一致

```kotlin
companion object {
    private const val TAG = "UserViewModel"
}

logger.d(TAG, "User loaded: id=${user.id}")
```

## 15. 序列化

- 优先使用 `kotlinx.serialization`（`@Serializable`），次选 Gson/Moshi
- 跨进程 / Intent 传递使用 `@Parcelize`
- 网络解析必须防御性处理：字段声明为可空或提供默认值

```kotlin
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val avatar: String? = null  // 防御性默认值
)

@Parcelize
data class UserArgs(
    val userId: String,
    val userName: String
) : Parcelable
```

## 16. 检查清单

### Kotlin 代码风格
- [ ] 单行是否不超过 120 字符
- [ ] 函数是否不超过 30 行
- [ ] 类是否不超过 300 行
- [ ] 是否避免了 `!!` 操作符
- [ ] 是否使用 `when` 替代多个 `if-else`
- [ ] 是否使用表达式函数体（适用时）
- [ ] 公开 API 是否有 KDoc 注释
- [ ] 作用域函数嵌套是否不超过 2 层

### 协程与 Flow
- [ ] 是否在 `viewModelScope` / `lifecycleScope` 内启动协程
- [ ] Fragment 是否使用 `viewLifecycleOwner` + `repeatOnLifecycle` 收集 Flow
- [ ] I/O 操作是否切换到 `Dispatchers.IO`
- [ ] UI 状态是否使用 `StateFlow`，一次性事件是否使用 `Channel`

### ViewBinding
- [ ] Fragment 的 `_binding` 是否在 `onDestroyView()` 中置空
- [ ] 批量 UI 更新是否使用 `with(binding)` 

### 日志与序列化
- [ ] 是否使用项目统一日志工具（非 `android.util.Log`）
- [ ] 日志是否不包含明文敏感数据
- [ ] 网络 DTO 字段是否有防御性默认值

### XML 代码风格
- [ ] XML 属性是否按规定顺序排列（style → id → layout_width/height → 其他 layout → android → app → tools）
- [ ] 多属性 View 是否每个属性单独一行
- [ ] 是否使用 `tools:` 属性做布局预览，而非硬编码到 `android:text` 等
- [ ] 重复的属性组合是否已提取为 Style
- [ ] Namespace 声明顺序是否正确（android → app → tools）
