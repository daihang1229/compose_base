# Testing Review Prompt

> 此文件是自动生成的审查/生成指令组件。
> 在 Copilot Chat 中，请使用 `#file_name` 引用此文件，并让 Copilot 严格根据下文的规范审查选中代码。

## 规范定义：
# 测试规范

## 1. 测试分层

### 1.1 测试金字塔

```
        ┌───────────────┐
        │   E2E 测试    │   少量、慢、真实环境
        ├───────────────┤
        │  集成测试     │   中等、设备/模拟器
        ├───────────────┤
        │  单元测试     │   大量、快、隔离
        └───────────────┘
```

### 1.2 各层测试职责

| 测试类型 | 位置 | 职责 | 运行环境 |
|---------|------|------|----------|
| 单元测试 | `test/` | ViewModel、UseCase、Repository、工具类 | JVM |
| 集成测试 | `androidTest/` | 数据库、DataStore、多组件协作 | Android 设备/模拟器 |
| UI 测试 | `androidTest/` | 用户交互流程、页面行为 | Android 设备/模拟器 |
| E2E 测试 | `androidTest/` | 完整业务流程 | Android 设备/模拟器 |

## 2. 测试覆盖率要求

| 组件 | 最低覆盖率 | 说明 |
|------|-----------|------|
| ViewModel | 90% | 核心业务逻辑 |
| UseCase | 90% | 领域逻辑 |
| Repository | 80% | 数据处理逻辑 |
| Mapper | 100% | 数据转换 |
| 工具类 | 80% | 通用功能 |
| 整体 | 70% | 项目整体 |

## 3. 单元测试规范

### 3.1 测试框架

```kotlin
// build.gradle.kts
dependencies {
    // 单元测试
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")  // Flow 测试
    testImplementation("com.google.truth:truth:1.1.5")    // 断言库
    
    // Android 测试
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.5")
}
```

### 3.2 测试命名规范

```kotlin
// 格式：`when_条件_then_期望结果`
// 或：`given_前置条件_when_操作_then_期望结果`

class UserViewModelTest {
    
    @Test
    fun `when loadUser called with valid id then uiState is Success`() {
        // ...
    }
    
    @Test
    fun `when loadUser called with invalid id then uiState is Error`() {
        // ...
    }
    
    @Test
    fun `given network unavailable when loadUser called then shows offline data`() {
        // ...
    }
}
```

### 3.3 测试结构（AAA 模式）

```kotlin
@Test
fun `when login with valid credentials then return success`() {
    // Arrange (Given) - 准备
    val email = "test@example.com"
    val password = "password123"
    coEvery { authRepository.login(email, password) } returns Result.success(mockUser)
    
    // Act (When) - 执行
    viewModel.login(email, password)
    
    // Assert (Then) - 验证
    val state = viewModel.uiState.value
    assertThat(state).isInstanceOf(LoginUiState.Success::class.java)
}
```

### 3.4 ViewModel 测试示例

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {
    
    // 规则：主线程调度器替换
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    // Mock 依赖
    private val getUserUseCase: GetUserUseCase = mockk()
    
    // 被测对象
    private lateinit var viewModel: UserViewModel
    
    @Before
    fun setup() {
        viewModel = UserViewModel(getUserUseCase)
    }
    
    @Test
    fun `when loadUser succeeds then uiState is Success with user data`() = runTest {
        // Arrange
        val expectedUser = User(id = "1", name = "Test User", email = "test@example.com")
        coEvery { getUserUseCase("1") } returns Result.success(expectedUser)
        
        // Act
        viewModel.loadUser("1")
        
        // Assert
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(UserUiState.Success::class.java)
        assertThat((state as UserUiState.Success).user.name).isEqualTo("Test User")
    }
    
    @Test
    fun `when loadUser fails then uiState is Error`() = runTest {
        // Arrange
        coEvery { getUserUseCase("1") } returns Result.failure(Exception("Network error"))
        
        // Act
        viewModel.loadUser("1")
        
        // Assert
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(UserUiState.Error::class.java)
    }
    
    @Test
    fun `when loadUser called then loading state is emitted first`() = runTest {
        // Arrange
        coEvery { getUserUseCase("1") } coAnswers {
            delay(100)
            Result.success(mockk())
        }
        
        // Act & Assert
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(UserUiState.Loading::class.java)
            viewModel.loadUser("1")
            assertThat(awaitItem()).isInstanceOf(UserUiState.Loading::class.java)
            assertThat(awaitItem()).isInstanceOf(UserUiState.Success::class.java)
        }
    }
}

// MainDispatcherRule
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### 3.5 UseCase 测试示例

```kotlin
class GetUserUseCaseTest {
    
    private val userRepository: UserRepository = mockk()
    private lateinit var useCase: GetUserUseCase
    
    @Before
    fun setup() {
        useCase = GetUserUseCase(userRepository)
    }
    
    @Test
    fun `when repository returns user then return success`() = runTest {
        // Arrange
        val expectedUser = User(id = "1", name = "Test", email = "test@test.com")
        coEvery { userRepository.getUser("1") } returns Result.success(expectedUser)
        
        // Act
        val result = useCase("1")
        
        // Assert
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedUser)
    }
    
    @Test
    fun `when repository throws exception then return failure`() = runTest {
        // Arrange
        coEvery { userRepository.getUser("1") } returns Result.failure(Exception("Error"))
        
        // Act
        val result = useCase("1")
        
        // Assert
        assertThat(result.isFailure).isTrue()
    }
}
```

### 3.6 Repository 测试示例

```kotlin
class UserRepositoryImplTest {
    
    private val remoteDataSource: UserRemoteDataSource = mockk()
    private val localDataSource: UserLocalDataSource = mockk()
    private val networkMonitor: NetworkMonitor = mockk()
    
    private lateinit var repository: UserRepositoryImpl
    
    @Before
    fun setup() {
        repository = UserRepositoryImpl(remoteDataSource, localDataSource, networkMonitor)
    }
    
    @Test
    fun `when online then fetch from remote and cache locally`() = runTest {
        // Arrange
        val remoteUser = UserDto(userId = "1", firstName = "Test", lastName = "User", email = "test@test.com")
        every { networkMonitor.isOnline() } returns true
        coEvery { remoteDataSource.getUser("1") } returns remoteUser
        coEvery { localDataSource.saveUser(any()) } just Runs
        
        // Act
        val result = repository.getUser("1")
        
        // Assert
        assertThat(result.isSuccess).isTrue()
        coVerify { localDataSource.saveUser(any()) }
    }
    
    @Test
    fun `when offline then fetch from local cache`() = runTest {
        // Arrange
        val localUser = UserEntity(id = "1", firstName = "Cached", lastName = "User", email = "test@test.com", joinDate = 0)
        every { networkMonitor.isOnline() } returns false
        coEvery { localDataSource.getUser("1") } returns localUser
        
        // Act
        val result = repository.getUser("1")
        
        // Assert
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.firstName).isEqualTo("Cached")
        coVerify(exactly = 0) { remoteDataSource.getUser(any()) }
    }
}
```

## 4. Android 集成测试

### 4.1 数据库测试

```kotlin
@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndGetUser() = runTest {
        // Arrange
        val user = UserEntity(id = "1", firstName = "Test", lastName = "User", email = "test@test.com", joinDate = 0)
        
        // Act
        userDao.insert(user)
        val result = userDao.getUserById("1")
        
        // Assert
        assertThat(result).isNotNull()
        assertThat(result?.firstName).isEqualTo("Test")
    }
}
```

### 4.2 UI 测试（Espresso）

```kotlin
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)
    
    @Test
    fun loginButton_isDisabled_whenEmailIsEmpty() {
        // Arrange & Assert
        onView(withId(R.id.btnLogin))
            .check(matches(not(isEnabled())))
    }
    
    @Test
    fun loginButton_isEnabled_whenCredentialsAreValid() {
        // Arrange
        onView(withId(R.id.etEmail))
            .perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword))
            .perform(typeText("password123"), closeSoftKeyboard())
        
        // Assert
        onView(withId(R.id.btnLogin))
            .check(matches(isEnabled()))
    }
    
    @Test
    fun errorMessage_isDisplayed_whenLoginFails() {
        // Arrange
        onView(withId(R.id.etEmail))
            .perform(typeText("wrong@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword))
            .perform(typeText("wrongpass"), closeSoftKeyboard())
        
        // Act
        onView(withId(R.id.btnLogin))
            .perform(click())
        
        // Wait for error state (in real test, use IdlingResource)
        Thread.sleep(1000)
        
        // Assert
        onView(withId(R.id.tvError))
            .check(matches(withText("登录失败")))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun loginFlow_successfulAuthentication() {
        // Arrange
        onView(withId(R.id.etEmail))
            .perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword))
            .perform(typeText("password123"), closeSoftKeyboard())
        
        // Act
        onView(withId(R.id.btnLogin))
            .check(matches(isEnabled()))
            .perform(click())
        
        // Assert - Verify navigation to next screen
        // (In real test, verify expected Activity/Fragment is displayed)
        onView(withId(R.id.tvWelcome))
            .check(matches(isDisplayed()))
    }
}

## 5. 测试最佳实践

### 5.1 测试隔离

```kotlin
// ✅ 每个测试独立，不依赖其他测试
@Before
fun setup() {
    // 每个测试前重置状态
    clearAllMocks()
    viewModel = createViewModel()
}

// ❌ 避免测试间共享状态
companion object {
    private var sharedState: SomeState? = null  // 不要这样做
}
```

### 5.2 避免测试实现细节

```kotlin
// ✅ 测试行为，而非实现
@Test
fun `when login succeeds then navigate to home`() {
    viewModel.login(validEmail, validPassword)
    assertThat(viewModel.navigationEvent.value).isEqualTo(NavigationEvent.GoToHome)
}

// ❌ 避免测试内部实现
@Test
fun `when login succeeds then set isLoggedIn to true`() {
    viewModel.login(validEmail, validPassword)
    // 这测试的是内部状态，不是外部行为
    assertThat(viewModel.isLoggedIn).isTrue()
}
```

### 5.3 测试边界条件

```kotlin
class EmailValidatorTest {
    
    @Test
    fun `valid email returns true`() {
        assertThat("test@example.com".isValidEmail()).isTrue()
    }
    
    @Test
    fun `email without @ returns false`() {
        assertThat("testexample.com".isValidEmail()).isFalse()
    }
    
    @Test
    fun `email without domain returns false`() {
        assertThat("test@".isValidEmail()).isFalse()
    }
    
    @Test
    fun `empty email returns false`() {
        assertThat("".isValidEmail()).isFalse()
    }
    
    @Test
    fun `email with spaces returns false`() {
        assertThat("test @example.com".isValidEmail()).isFalse()
    }
}
```

## 6. 检查清单

- [ ] 测试是否覆盖了主要的业务逻辑
- [ ] 测试命名是否清晰描述了测试内容
- [ ] 测试是否遵循 AAA 模式
- [ ] 测试是否相互独立
- [ ] 是否测试了边界条件和异常情况
- [ ] 测试是否避免了测试实现细节
- [ ] 覆盖率是否达到要求
