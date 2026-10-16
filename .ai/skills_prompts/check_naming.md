# Naming Review Prompt

> 此文件是自动生成的审查/生成指令组件。
> 在 Copilot Chat 中，请使用 `#file_name` 引用此文件，并让 Copilot 严格根据下文的规范审查选中代码。

## 规范定义：
# 命名规范

## 1. 类命名

### 1.1 基本规则

| 类型 | 格式 | 示例 |
|------|------|------|
| 普通类 | PascalCase + 名词 | `User`, `OrderManager`, `HttpClient` |
| 接口 | PascalCase + 名词/形容词 | `Clickable`, `DataSource`, `UserRepository` |
| 抽象类 | `Base` 或 `Abstract` 前缀 | `BaseActivity`, `AbstractParser` |
| 实现类 | 接口名 + `Impl` | `UserRepositoryImpl`, `LocalDataSourceImpl` |
| 工具类 | 名词 + `Utils` 或 `Helper` | `StringUtils`, `DateHelper` |
| 扩展类 | 被扩展类 + `Ext` 或 `Extensions` | `StringExt`, `ViewExtensions` |
| 常量对象 | 领域 + `Constants` | `NetworkConstants`, `RechargeConstants` |

### 1.2 Android 组件命名

| 组件 | 格式 | 示例 |
|------|------|------|
| Application | App名 + `Application` | `MyApplication` |
| Activity | 功能 + `Activity` | `LoginActivity`, `UserDetailActivity` |
| Fragment | 功能 + `Fragment` | `HomeFragment`, `SettingsFragment` |
| ViewModel | 功能 + `ViewModel` | `LoginViewModel`, `UserDetailViewModel` |
| Adapter | 数据类型 + `Adapter` | `UserAdapter`, `OrderListAdapter` |
| ViewHolder | 条目类型 + `ViewHolder` | `UserViewHolder`, `HeaderViewHolder` |
| Service | 功能 + `Service` | `DownloadService`, `MusicPlayService` |
| BroadcastReceiver | 功能 + `Receiver` | `BootReceiver`, `NetworkChangeReceiver` |
| ContentProvider | 功能 + `Provider` | `FileProvider`, `SearchProvider` |
| Worker (WorkManager) | 功能 + `Worker` | `SyncDataWorker`, `CleanupWorker` |
| Dialog | 功能 + `Dialog` | `ConfirmDialog`, `LoadingDialog` |
| DialogFragment | 功能 + `DialogFragment` | `DatePickerDialogFragment` |

### 1.3 架构组件命名

| 组件 | 格式 | 示例 |
|------|------|------|
| UseCase | 动作 + 名词 + `UseCase` | `GetUserUseCase`, `PlaceOrderUseCase` |
| Repository | 领域 + `Repository` | `UserRepository`, `OrderRepository` |
| DataSource | 来源 + `DataSource` | `UserRemoteDataSource`, `UserLocalDataSource` |
| Mapper | 源类型 + `To` + 目标类型 + `Mapper` | `UserDtoToEntityMapper` |
| Module (Hilt) | 功能 + `Module` | `NetworkModule`, `DatabaseModule` |

### 1.4 密封类 / 密封接口子类命名

优先使用 `sealed interface`（内存更友好）。子类使用 **PascalCase**（区别于枚举的 SCREAMING_SNAKE_CASE）。

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

- 无数据子类用 `data object`，有数据子类用 `data class`
- 事件子类可用 `On` 前缀：`OnPurchaseSuccess`、`OnLoadingComplete`
- UI 状态常见命名：`Loading` / `Success` / `Error` / `Empty`

### 1.5 Helper vs Manager vs Utils 语义区分

| 后缀 | 语义 | 持有状态 | 示例 |
|------|------|---------|------|
| `Utils` | 纯静态/顶层工具函数，无状态 | ❌ | `StringUtils`, `DateUtils` |
| `Helper` | 辅助类，可持有少量上下文 | ⚠️ 少量 | `NavigationHelper`, `PermissionHelper` |
| `Manager` | 管理类，持有状态，管理资源生命周期 | ✅ | `DownloadManager`, `CacheManager` |

> Kotlin 官方建议避免无意义的 `Manager` / `Wrapper`。只有当类确实管理资源生命周期时才用 `Manager`。能用顶层函数或扩展函数解决的，不要创建 `Utils` 类。

## 2. 函数命名

### 2.1 基本规则

| 类型 | 格式 | 示例 |
|------|------|------|
| 获取数据 | `get` + 名词 | `getUser()`, `getUserById()` |
| 设置数据 | `set` + 名词 | `setUserName()`, `setEnabled()` |
| 判断状态 | `is`/`has`/`can`/`should` + 形容词/名词 | `isValid()`, `hasPermission()`, `canSubmit()` |
| 异步操作 | 动词（suspend 函数） | `suspend fun fetchUser()` |
| 回调监听 | `on` + 事件 | `onClick()`, `onUserLoaded()` |
| 转换数据 | `to` + 目标类型 | `toEntity()`, `toUiModel()` |
| 执行操作 | 动词 + 名词 | `saveUser()`, `deleteOrder()`, `processPayment()` |

### 2.2 常用动词

| 场景 | 推荐动词 |
|------|---------|
| 创建 | `create`, `make`, `build`, `generate` |
| 读取 | `get`, `fetch`, `load`, `find`, `query` |
| 更新 | `update`, `modify`, `change`, `set` |
| 删除 | `delete`, `remove`, `clear` |
| 转换 | `to`, `convert`, `parse`, `format` |
| 验证 | `validate`, `check`, `verify`, `ensure` |
| 计算 | `calculate`, `compute`, `sum` |
| 初始化 | `init`, `setup`, `configure` |

**语义区分**：`get` 同步/缓存获取；`fetch` 远程拉取；`load` 完整流程（缓存→网络→兜底）；`find` 返回单个可空；`query` 返回集合。

### 2.3 Android 特定函数

| 类别 | 函数名 |
|------|--------|
| 生命周期 | `onCreate()`, `onCreateView()` |
| 初始化 | `initViews()`, `initData()`, `setupObservers()`, `setupListeners()` |
| 导航 | `navigateToDetail()`, `navigateBack()` |
| UI 更新 | `updateUI()`, `showLoading()`, `showError()`, `showContent()` |

### 2.4 工厂函数命名

| 动词 | 用途 | 示例 |
|------|------|------|
| `newInstance()` | Fragment 工厂（Android 惯例） | `UserFragment.newInstance(userId)` |
| `createXxx()` | 通用对象创建 | `createIntent(context, userId)` |
| `buildXxx()` | Builder 模式最终调用 | `buildNotification()` |
| `of()` / `from()` | 静态工厂（Kotlin 标准库风格） | `Color.of(hex)` |

## 3. 变量命名

### 3.1 基本规则

| 类型 | 格式 | 示例 |
|------|------|------|
| 普通变量 | camelCase | `userName`, `isLoading`, `userList` |
| Backing Property | `_` 前缀 + camelCase | `_uiState` / `uiState` |
| 常量 | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| 单例对象 | PascalCase | `UserManager`, `NetworkConfig` |

### 3.2 集合命名

```kotlin
val users: List<User>           // 复数
val userList: List<User>        // 或者加 List 后缀
val userSet: Set<User>
val userMap: Map<String, User>
val userIds: List<String>       // ID 列表
```

### 3.3 Android 特定变量

**Key 命名前缀约定**：

| 用途 | 前缀 | 示例 |
|------|------|------|
| Fragment 参数 | `ARG_` | `ARG_USER_ID` |
| Intent Extra | `EXTRA_` | `EXTRA_USER_ID` |
| Preferences Key | `KEY_` | `KEY_ACCESS_TOKEN` |
| Bundle SavedState | `STATE_` | `STATE_SCROLL_POSITION` |

```kotlin
// ViewBinding / ViewModel
private lateinit var binding: ActivityMainBinding
private val viewModel: MainViewModel by viewModels()

// Key 常量（在 companion object 中）
companion object {
    private const val ARG_USER_ID = "arg_user_id"
    const val EXTRA_USER_ID = "extra_user_id"
    
    fun newInstance(userId: String) = UserFragment().apply {
        arguments = bundleOf(ARG_USER_ID to userId)
    }
}

object PrefsKeys {
    const val KEY_ACCESS_TOKEN = "key_access_token"
    const val KEY_USER_ID = "key_user_id"
}
```

### 3.4 Backing Property 命名

内部用 `_` 前缀（可变），外部去掉前缀暴露不可变版本。

```kotlin
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

private val _events = Channel<UiEvent>(Channel.BUFFERED)
val events = _events.receiveAsFlow()

### 3.5 枚举常量命名

枚举常量用 `SCREAMING_SNAKE_CASE`，sealed class 子类用 PascalCase，二者不混淆。

```kotlin
enum class PaymentMethod { GOOGLE_PLAY, STRIPE, PAYPAL }

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Int) : DownloadState
}
```

**选择依据**：所有值已知且无附加数据 → `enum class`；子类携带不同数据 → `sealed interface`。

## 4. 资源命名

所有资源文件和 ID 使用 `snake_case`，前缀区分类型。

### 4.1 布局文件

| 类型 | 格式 | 示例 |
|------|------|------|
| Activity | `activity_` + 功能 | `activity_main.xml`, `activity_login.xml` |
| Fragment | `fragment_` + 功能 | `fragment_home.xml`, `fragment_user.xml` |
| Dialog | `dialog_` + 功能 | `dialog_confirm.xml`, `dialog_loading.xml` |
| 列表项 | `item_` + 类型 | `item_user.xml`, `item_order.xml` |
| Include 布局 | `layout_` + 功能 | `layout_toolbar.xml`, `layout_empty.xml` |

### 4.2 Drawable

| 类型 | 格式 | 示例 |
|------|------|------|
| Icon | `ic_` + 名称 | `ic_home.xml`, `ic_arrow_back.xml` |
| 背景 | `bg_` + 名称 | `bg_button.xml`, `bg_card.xml` |
| Shape | `shape_` + 描述 | `shape_bg_button_primary.xml`, `shape_rounded_corner.xml` |
| 选择器 | `selector_` + 名称 | `selector_button.xml` |
| 分隔线 | `divider_` + 描述 | `divider_horizontal.xml` |
| 占位图 | `placeholder_` + 类型 | `placeholder_avatar.xml` |

### 4.3 String

| 格式 | 示例 |
|------|------|
| `模块_功能_描述` | `login_title`, `login_button_submit`, `login_error_invalid_email` |
| `common_` 通用前缀 | `common_button_ok`, `common_button_cancel`, `common_error_network` |
| 带参数 | `user_greeting` → `你好，%1$s`；`order_total` → `共 %1$d 件，合计 ¥%2$.2f` |

### 4.4 Dimen

| 格式 | 示例 |
|------|------|
| `margin_` + 大小 | `margin_small`(8dp), `margin_medium`(16dp), `margin_large`(24dp) |
| `text_size_` + 用途 | `text_size_title`(18sp), `text_size_body`(14sp), `text_size_caption`(12sp) |
| 组件 + `_` + 属性 | `button_height`(48dp), `icon_size_small`(24dp) |

### 4.5 Color

语义化命名，禁止用十六进制值作为颜色名（❌ `color_FF1C1C1E` → ✅ `colorSurface`）。

| 格式 | 示例 |
|------|------|
| 主题色 | `colorPrimary`, `colorSecondary` |
| 文本色 | `textPrimary`, `textSecondary` |
| 功能色 | `background`, `surface`, `error` |

### 4.6 Animation 资源命名

| 类型 | 格式 | 示例 |
|------|------|------|
| 属性动画 | `anim_` + 动作 | `anim_fade_in.xml`, `anim_slide_up.xml` |
| 转场动画 | `transition_` + 描述 | `transition_shared_element.xml` |

### 4.7 View ID 命名

View ID 使用 camelCase，前缀标识控件类型：

| 控件 | 前缀 | 示例 |
|------|------|------|
| TextView | `tv` | `tvTitle`, `tvUserName` |
| ImageView | `iv` | `ivCover`, `ivAvatar` |
| Button | `btn` | `btnSubmit`, `btnCancel` |
| EditText | `et` | `etSearch`, `etEmail` |
| RecyclerView | `rv` | `rvItems`, `rvUserList` |
| ProgressBar | `pb` | `pbLoading` |
| CheckBox | `cb` | `cbAgree` |
| Switch | `sw` | `swNotification` |
| ConstraintLayout | `cl` | `clContainer` |
| LinearLayout | `ll` | `llContent` |
| FrameLayout | `fl` | `flPlayer` |
| CardView | `cv` | `cvItem` |

## 5. 包结构命名

```
com.example.myapp/
├── di/                     # 依赖注入
├── data/                   # 数据层
│   ├── local/              # 本地数据源
│   ├── remote/             # 远程数据源
│   ├── repository/         # Repository 实现
│   └── model/              # DTO、Entity
├── domain/                 # 领域层
│   ├── model/              # 领域模型
│   ├── repository/         # Repository 接口
│   └── usecase/            # UseCase
├── presentation/           # 表现层
│   ├── common/             # 公共组件
│   └── feature/            # 按功能划分
│       ├── login/
│       ├── home/
│       └── user/
├── util/                   # 工具类
├── ext/                    # 扩展函数
└── constant/               # 常量定义
```

包名：全小写，无下划线（✅ `com.example.myapp.feature.login` ❌ `com.example.myApp.feature.Login`）

> ⚠️ 统一使用 `model/` 作为数据类包名。`bean/` 为历史遗留，**新代码禁止使用 `bean/`**。

## 6. 泛型类型参数命名

| 字母 | 含义 |
|------|------|
| `T` | Type（通用） |
| `E` | Element（集合元素） |
| `K` | Key |
| `V` | Value |
| `R` | Return / Result |

泛型含义复杂或多个类型参数时，使用描述性 PascalCase 名称：

```kotlin
interface Mapper<Input, Output> {
    fun map(input: Input): Output
}
fun <E : Entity> process(entity: E)
```

## 7. 首字母缩写词处理

- **两个字母**的缩写全大写：`IO`, `ID`
- **三个字母及以上**的缩写仅首字母大写：`Xml`, `Http`, `Json`, `Url`

```kotlin
class XmlParser           // XML → Xml
class HttpClient          // HTTP → Http
val userId                // ID 两字母全大写
```

**常用缩写速查**：

| 缩写 | 类名中 | 变量名中 |
|------|--------|---------|
| ID | `UserID` | `userId` |
| IO | `FileIO` | `fileIO` |
| URL | `UrlParser` | `urlString` |
| HTTP | `HttpClient` | `httpClient` |
| XML | `XmlFormatter` | `xmlData` |
| JSON | `JsonConverter` | `jsonString` |
| API | `ApiService` | `apiClient` |
| UI | `UiState` | `uiModel` |
| DTO | `UserDto` | `userDto` |
| DAO | `UserDao` | `userDao` |

## 8. 检查清单

### 类与接口
- [ ] 类名是否使用 PascalCase 并正确反映职责
- [ ] Android 组件是否使用正确后缀（`Activity`, `Fragment`, `ViewModel`, `Worker` 等）
- [ ] 实现类是否以 `Impl` 结尾
- [ ] sealed class/interface 子类是否使用 PascalCase
- [ ] `Utils` / `Helper` / `Manager` 后缀是否语义正确

### 函数
- [ ] 函数名是否使用 camelCase 并以动词开头
- [ ] 布尔返回函数是否使用 `is`/`has`/`can`/`should` 前缀
- [ ] Fragment 工厂是否命名为 `newInstance()`
- [ ] 数据转换函数是否使用 `toXxx()` 命名

### 变量与常量
- [ ] 变量是否使用 camelCase
- [ ] 常量 / 枚举常量是否使用 `SCREAMING_SNAKE_CASE`
- [ ] Backing Property 是否遵循 `_privateName` / `publicName` 模式
- [ ] Key 常量是否使用正确前缀（`ARG_`, `EXTRA_`, `KEY_`）
- [ ] 布尔变量是否以 `is`/`has`/`can`/`should` 开头

### 资源
- [ ] 布局文件是否使用正确前缀（`activity_`, `fragment_`, `item_`, `dialog_`）
- [ ] Drawable 是否使用正确前缀（`ic_`, `bg_`, `shape_`, `selector_`）
- [ ] View ID 是否 camelCase 且有控件类型前缀（`tvTitle`, `btnPlay`）
- [ ] Color 是否语义化命名（禁止十六进制命名）
- [ ] 是否避免使用缩写（除非是广泛认知的如 URL、HTTP）

### 包与泛型
- [ ] 包名是否全小写
- [ ] 数据类包名是否使用 `model/`（而非 `bean/`）
- [ ] 首字母缩写词是否按规则处理（两字母全大写，三字母以上仅首字母大写）
- [ ] 泛型参数是否使用标准字母或描述性名称
