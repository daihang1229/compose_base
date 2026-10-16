# Compose Base Android 架构规范

> **版本**: 1.0.0
> **适用范围**: 通用 Android / Compose 工程（本框架 + 各宿主项目）
> **⚠️ 致所有 AI 工具**: 本文档是架构的**最高权威文档**。生成、修改、审查代码时，**必须绝对遵守**此文档及子规范中的所有约束。

---

## 目录

1. [设计哲学与核心原则](#一设计哲学与核心原则)
2. [技术栈规范（黑白名单）](#二技术栈规范黑白名单)
3. [整体架构模式：Clean Architecture + MVVM](#三整体架构模式clean-architecture--mvvm)
4. [关键设计模式运用](#四关键设计模式运用)
5. [数据层规范](#五数据层规范)
6. [异步与并发规范](#六异步与并发规范)
7. [UI 层规范](#七ui-层规范)
8. [依赖注入规范（Hilt）](#八依赖注入规范hilt)
9. [全局架构检查清单](#九全局架构检查清单)

---

## 一、设计哲学与核心原则

架构遵循以下核心原则，所有技术决策均以此为准绳：

| 原则 | 含义 | 体现 |
|------|------|------|
| **关注点分离 (SoC)** | 每一层/模块只做自己的事 | Clean Architecture 三层分层 |
| **依赖倒置 (DIP)** | 高层不依赖低层实现，双方依赖抽象 | Repository 接口 + Hilt 注入 |
| **开闭原则 (OCP)** | 对扩展开放，对修改关闭 | 接口抽象 + 配置注入扩展 |
| **单一职责 (SRP)** | 一个类只有一个变化原因 | UseCase 单操作、ViewModel 只管 UI 状态 |
| **最小知识法则** | 模块只知道必须知道的 | 接口解耦，禁止跨模块直接引用实现类 |

---

## 二、技术栈规范（黑白名单）

### 2.1 🚫 绝对禁止技术黑名单

> **AI 必须拦截任何包含以下技术的代码生成/建议。**

| 禁止项 | 禁止原因 | 强制替代方案 |
|--------|---------|------------|
| ❌ **RxJava / RxAndroid** | 已淘汰，复杂度高 | ✅ Kotlin `Coroutines` + `Flow` / `StateFlow` |
| ❌ **EventBus / Otto** | 隐式依赖，难以追踪 | ✅ `SharedFlow` |
| ❌ **AsyncTask & 原生 Thread** | 生命周期不安全 | ✅ `viewModelScope` / `lifecycleScope` 内协程 |
| ❌ **直接操作 SharedPreferences** | 非类型安全，无加密 | ✅ `DataStore` 或统一 KV 封装 |
| ❌ **android.util.Log** | 无法在 Release 自动关闭 | ✅ 统一日志工具（`ComposeLog`） |
| ❌ **GlobalScope.launch** | 生命周期脱离控制 | ✅ `viewModelScope` / `lifecycleScope` |
| ❌ **直接继承 ViewModel()** | 缺少基类公共能力 | ✅ 继承 `BaseViewModel()` |
| ❌ **kotlin.Result<T> 做网络返回** | 缺少业务错误语义 | ✅ `ApiResult<T>` / `UiState<T>` |
| ❌ **裸 try-catch 包 Retrofit** | 不统一错误处理 | ✅ 统一网络层封装 |
| ❌ **onRequestPermissionsResult** | 已废弃 API | ✅ Activity Result API (`registerForActivityResult`) |
| ❌ **DAO 返回 LiveData** | 与协程体系割裂 | ✅ Room DAO 返回 `Flow<List<T>>` |

### 2.2 ✅ 强制使用的技术栈

| 领域 | 技术选型 | 说明 |
|------|---------|------|
| **UI** | Compose MVI（默认）/ XML + ViewBinding | Compose 遵循 `compose-mvi.md`；XML 走 ViewBinding |
| **异步** | Kotlin Coroutines + Flow | 唯一异步方案 |
| **UI 状态** | StateFlow | ViewModel 暴露状态 |
| **一次性事件** | Channel + receiveAsFlow() | Toast、Navigation 等 |
| **依赖注入** | Hilt | 禁止手动构造复杂依赖 |
| **路由** | 自维护路由（页面注册表） | 轻量，禁止引入第三方路由框架 |
| **网络** | Retrofit + OkHttp | 统一网络层 |
| **图片加载** | Coil（Compose）/ 统一封装 | 禁止手动管理 Bitmap |
| **本地存储** | Room（结构化）+ DataStore（KV）| 禁止直接 SharedPreferences |
| **日志** | 统一日志工具（`ComposeLog`） | Release 自动关闭 |

---

## 三、整体架构模式：Clean Architecture + MVVM

### 3.1 三层架构

```
┌─────────────────────────────────────────────┐
│            Presentation Layer                │
│   Activity / Composable  ←→  ViewModel      │
│   (只观察状态，不处理业务)   (只管 UI 逻辑) │
└───────────────────┬─────────────────────────┘
                    ↓ 调用
┌─────────────────────────────────────────────┐
│               Domain Layer                   │
│   UseCase（单操作单职责）                    │
│   Entity（纯业务模型，无框架依赖）           │
│   Repository Interface（领域定义契约）       │
└───────────────────┬─────────────────────────┘
                    ↓ 实现
┌─────────────────────────────────────────────┐
│                Data Layer                    │
│   Repository Impl（数据策略：缓存→网络）     │
│   RemoteDataSource（API / DTO）              │
│   LocalDataSource（Room DAO / DataStore）    │
│   Mapper（DTO ↔ Entity 转换）               │
└─────────────────────────────────────────────┘
```

### 3.2 防腐层（数据转换强制规范）

```
API 响应 (DTO)
  ↓  Data 层 Mapper：dto.toDomain()
Domain Entity
  ↓  ViewModel Mapper：entity.toUiModel()
UI Model (UiState)
  ↓  Composable / ViewBinding
UI 绑定
```

**核心约束：**
- DTO **绝对不能**离开 Data 层（不允许进入 Domain 或 UI 层）
- Entity **绝对不能**直接绑定给 View（必须先转成 UiModel）
- 跨层数据传递，**必须**经过 Mapper 转换

### 3.3 ViewModel 规范

```kotlin
// ✅ 正确：继承 BaseViewModel，统一 UiState 数据类
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val getAlbumUseCase: GetAlbumUseCase
) : BaseViewModel() {                                   // 🔴 必须继承 BaseViewModel

    private val _uiState = MutableStateFlow<UiState<AlbumUiModel>>(UiState.Loading)
    val uiState: StateFlow<UiState<AlbumUiModel>> = _uiState.asStateFlow()

    private val _events = Channel<AlbumEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _uiState.moveLoading()
            getAlbumUseCase(albumId)
                .onSuccess { album -> _uiState.moveSuccess(album.toUiModel()) }
                .onFailure { err -> _uiState.moveError(ErrorType.SERVER_ERROR, err.message ?: "") }
        }
    }
}
```

> Compose MVI 页面使用 `BaseMviViewModel`，详见 `compose-mvi.md`。

### 3.4 UseCase 规范

```kotlin
// ✅ 单一操作，使用 ApiResult<T>
class GetAlbumUseCase @Inject constructor(
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(albumId: String): ApiResult<Album> =
        albumRepository.getAlbum(albumId)
}
```

### 3.5 Repository 规范

```kotlin
// Domain 层：接口定义
interface AlbumRepository {
    suspend fun getAlbum(albumId: String): ApiResult<Album>
    fun observeWatchHistory(): Flow<List<Album>>
}

// Data 层：实现（缓存优先策略）
class AlbumRepositoryImpl @Inject constructor(
    private val remote: AlbumRemoteDataSource,
    private val local: AlbumLocalDataSource
) : AlbumRepository {
    override suspend fun getAlbum(albumId: String): ApiResult<Album> {
        val cached = local.getAlbum(albumId)
        if (cached != null) return ApiResult.Success(cached.toDomain())
        return remote.fetchAlbum(albumId).let { it.toDomain() }
    }
}
```

---

## 四、关键设计模式运用

### 4.1 观察者模式 → StateFlow / SharedFlow

```kotlin
// Fragment 安全收集（必须 viewLifecycleOwner + repeatOnLifecycle）
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> render(state) }
    }
}
```

### 4.2 策略模式 → 接口 + 多实现

```kotlin
// 差异化通过接口注入不同实现（而非硬编码分支）
interface IAppConfig { val apiHost: String }
class ProductionAppConfig : IAppConfig { override val apiHost = "api.example.com" }
```

### 4.3 工厂模式 → newInstance()

```kotlin
companion object {
    fun newInstance(albumId: String) = AlbumFragment().apply {
        arguments = bundleOf("arg_album_id" to albumId)
    }
}
```

### 4.4 责任链模式 → OkHttp Interceptor

```kotlin
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider() ?: return chain.proceed(chain.request())
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token").build()
        return chain.proceed(request)
    }
}
```

### 4.5 防腐层（ACL）→ Mapper 数据转换

```kotlin
// Data 层：DTO → Domain Entity
fun UserDto.toDomain(): User = User(id = userId, name = "$firstName $lastName")

// Presentation 层：Entity → UiModel
fun User.toUiModel(): UserUiModel = UserUiModel(displayName = name, avatarUrl = avatar)
```

---

## 五、数据层规范

### 5.1 网络层结构

```
OkHttp（AuthInterceptor / LoggingInterceptor）
  → Retrofit（ApiService 接口，返回 DTO）
  → RemoteDataSource
  → RepositoryImpl（Mapper 转换，返回 ApiResult<Entity>）
```

### 5.2 本地存储选型

| 场景 | 技术 | 禁止 |
|-----|------|------|
| 结构化数据 | Room | ❌ 直接 SQLite |
| 用户偏好/配置 | DataStore | ❌ SharedPreferences |
| 用户 Token | EncryptedSharedPreferences | ❌ 明文存储 |
| 加密密钥 | Android Keystore | ❌ 代码硬编码 |

### 5.3 Room 规范

```kotlin
// DAO 必须返回 Flow（禁止 LiveData）
@Dao
interface AlbumDao {
    @Query("SELECT * FROM album WHERE id = :id")
    fun observeAlbum(id: String): Flow<AlbumEntity?>   // ✅ Flow，❌ LiveData

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: AlbumEntity)
}
```

---

## 六、异步与并发规范

### 6.1 协程 Scope 使用规则

| 场景 | 使用 Scope | 禁止 |
|-----|-----------|------|
| ViewModel 中 | `viewModelScope` | ❌ GlobalScope |
| Fragment/Activity | `lifecycleScope` | ❌ GlobalScope |
| Fragment Flow 收集 | `viewLifecycleOwner.lifecycleScope` | ❌ `lifecycleScope` |
| 后台 I/O | `Dispatchers.IO` | ❌ 主线程执行 I/O |

### 6.2 Flow 类型选择

```kotlin
// UI 状态 → StateFlow（有初始值）
val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()

// 一次性事件（Toast/Navigation）→ Channel + receiveAsFlow
private val _events = Channel<UiEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()

// 广播事件（多订阅者）→ SharedFlow(replay=0)
val broadcastFlow = MutableSharedFlow<Event>(replay = 0)
```

### 6.3 生命周期安全

```kotlin
// Fragment _binding 必须在 onDestroyView 中置空（防内存泄漏）
override fun onDestroyView() {
    super.onDestroyView()
    _binding = null  // 🔴 必须
}
```

---

## 七、UI 层规范

### 7.1 ViewBinding 规范（XML 页面）

```kotlin
// Fragment 标准模版
private var _binding: FragmentHomeBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...): View {
    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding.root
}
override fun onDestroyView() { super.onDestroyView(); _binding = null }

// 批量 UI 更新：with(binding)
private fun render(state: UiState<T>) = with(binding) {
    tvTitle.text = state.title
    progressBar.isVisible = state.isLoading
}
```

### 7.2 Compose 页面规范

- Compose 页面遵循 `compose-mvi.md` 的 MVI 架构与 7 文件结构
- Activity 继承 `BaseCommonComposeActivity`
- 组件统一使用 `lib-compose-ui` 设计系统（`CommonTheme` / `CommonTitleBar` 等）

### 7.3 基类继承规范

| 组件 | 必须继承 |
|-----|---------|
| Activity (Compose) | `BaseCommonComposeActivity` |
| Activity (XML) | `BaseActivity`（宿主自定义） |
| ViewModel | `BaseViewModel` / `BaseMviViewModel` |

---

## 八、依赖注入规范（Hilt）

```kotlin
// ✅ ViewModel 注入
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val useCase: GetAlbumUseCase
) : BaseViewModel()

// ✅ Repository 绑定（优先 @Binds）
@Module @InstallIn(SingletonComponent::class)
abstract class AlbumModule {
    @Binds
    abstract fun bindAlbumRepository(impl: AlbumRepositoryImpl): AlbumRepository
}

// 作用域规范
// @Singleton          → 全局单例（Repository/DataSource 级别）
// @ActivityRetainedScoped → ViewModel 级别
// @ActivityScoped     → Activity 内
```

---

## 九、全局架构检查清单

### Clean Architecture

- [ ] ViewModel 是否继承 `BaseViewModel()` / `BaseMviViewModel()`？
- [ ] 网络请求返回是否使用 `ApiResult<T>` / `UiState<T>`？
- [ ] DTO 是否只停留在 Data 层？（不允许传入 Domain/UI 层）
- [ ] Entity 是否只停留在 Domain 层？（不允许直接绑定给 View）
- [ ] 跨层数据是否经过 Mapper 转换（`toDomain()` / `toUiModel()`）？
- [ ] UseCase 是否只做一件事（单一职责）？

### 代码质量

- [ ] 协程是否在 `viewModelScope` / `lifecycleScope` 内？
- [ ] Fragment 是否使用 `viewLifecycleOwner.lifecycleScope + repeatOnLifecycle` 收集 Flow？
- [ ] Fragment `_binding` 是否在 `onDestroyView()` 中置空？
- [ ] 日志是否使用统一日志工具（禁止 `android.util.Log`）？
- [ ] Compose 页面是否遵循 `compose-mvi.md` 规范？

---

## 扩展规范文档导航

| 主题 | 文档路径 |
|-----|---------|
| 代码风格规范 | [`../code-style.md`](../code-style.md) |
| 命名规范 | [`../naming.md`](../naming.md) |
| 安全规范 | [`../security.md`](../security.md) |
| 测试规范 | [`../testing.md`](../testing.md) |
| 性能规范 | [`../performance.md`](../performance.md) |
| Android 专项 | [`../android-specific.md`](../android-specific.md) |
| Compose MVI 规范 | [`../compose-mvi.md`](../compose-mvi.md) |
