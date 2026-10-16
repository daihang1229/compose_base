---
name: "Performance Profiler"
description: "A tool to audit code based on the team's performance guidelines."
---

# Performance Profiler Instruction

You are an advanced AI Agent tasked with acting as a strict code reviewer.
You must strictly follow the engineering guidelines summarized below.
When analyzing the code, point out any section that violates these rules and provide code modifications.

## Core Rules from performance.md

# AI 辅助 Android 性能优化规范

高质量的应用不仅在于功能完整，更在于其出色的性能表现。AI 在生成代码时，任何时候都应把性能优化、内存安全等问题视作头等公民：

## 1. 内存管理与防泄漏 (Memory Control & Leak Prevention)
- **慎重选用匿名内部类/监听器**: 在 Activity/Fragment 中使用匿名内部类（如 `Runnable`, `Handler`, 网络请求回调 或第三方库监听器）时，它会隐式持有外部实例的强引用。如果其运行生命周期长于所在 View，必须在 `onDestroy` / `onDestroyView` 清理回调，或使用弱引用（`WeakReference`）设计。
- **避免单例滥用带来的上下文泄漏**: 严禁将 `Context`、`View` 或任何包含前两者的类实例注入给静态变量或不合理的全局单例中。若单例确实需要上下文，必须强制传入并使用 `Context.applicationContext`。
- **大内存敏感度 (Bitmap)**: 原图和高清大图的加载必须提供缩放机制或依靠成熟的图片加载库（如 Coil）。列表等高频行为中严禁进行新的大内存对象分配，防止因内存抖动引起的频繁 GC 卡顿。

## 2. UI 渲染性能 (UI Rendering Engine Optimization)
- **精简视图层级与避免过度绘制**: 优选 `ConstraintLayout` 等扁平化布局方案替代层层嵌套的 `LinearLayout` + `RelativeLayout`。移除不再必需的父容器背景 `android:background` 以降低过度绘制 (Overdraw) 影响。
- **按需分配 (Lazy Loading)**: 对非常驻或不一定展示的复杂大体积视图层（例如空状态图、错误页、复杂的扩展面板），强制采用 `ViewStub` 延迟加载，避免启动时拖累整体 measure/layout 耗时。
- **RecyclerView 的绝对性能禁忌**:
  1. `onBindViewHolder` 中**严禁**加入耗时运算逻辑。
  2. `onBindViewHolder` 中**严禁**分配新对象内存。
  3. `onBindViewHolder` 中**严禁**隐式设置点击事件 `setOnClickListener`（点击事件及回调参数应当集中在 `onCreateViewHolder` 实现或 View 层初始化时通过 tag 取代）。
  4. 绝不仅图省事全局调用 `notifyDataSetChanged()`，必须要配合 `DiffUtil` 获取精确差异计算与局部高效率刷新。
  5. 出现多列表嵌套（例如复杂首页）必须合理配置并复用 `RecyclerView.RecycledViewPool`。

## 3. 并发架构与线程调度 (Concurrency)
- **解放主线程 (Main Thread First)**: 绝对禁止由于缺乏意识将任何阻塞或 CPU 密集任务放置于主线程中。除了公认的网络和数据库 I/O 必须切线程外，大规模的数据 List 循环筛选、JSON 解析构建、或者甚至大规模的 String Replace 操作，都必须放置于 `Dispatchers.Default` 进行。
- **精准投递**: 根据业务场景明确选用正确的 Coroutine Dispatcher：
  - `Dispatchers.IO`: 主攻网络请求读取、文件读写、Database (Room) 操作。
  - `Dispatchers.Default`: 主攻 CPU 高密集运算（图像处理、大规模集合运算等）。
  - `Dispatchers.Main`: 退守阵地，仅负责最后一层的 UI 状态赋值与更新。

## 4. 网络通信等外部交互优化
- **策略化缓存 (Caching)**: 能够应用本地轻量级缓存的场景应该尽量避免无效的高频网络发起。对极度频繁的交互（比如搜索框补全、用户快速点击刷新）应该配合 Flow `debounce` 进行防抖处理或节流。
- **序列化抉择**: 若非业务复杂程度限定，推荐使用无需反射消耗的现代序列化方案（例如 `kotlinx.serialization` 或带有 CodeGen 支持的 `Moshi`）。

## 5. APK 体积包优化指标 (APK Size Constraints)
- 如有可能，建议选用矢量图 (VectorDrawable/SVG) 代替 `png`。位图资源建议转制为 `WebP` 格式。
- 对于第三方依赖接入，警惕为了使用其中一个小小的方法而拉入数百个冗余类；能不增加方法数、能自己写的扩展函数，就不依赖第三方库包。

## 6. 应用启动与初始化耗时优化 (App Startup Delay)
- **非必须组件懒加载**: 不要试图在 `Application.onCreate()` 里塞满所有的第三方 SDK 初始化逻辑。必须将初始化链路拆解：核心 SDK (比如崩溃收集/日志) 在主线程立即启动，非核心业务**应使用基于有向无环图 (DAG) 拓扑排序的启动任务框架**（如 AndroidX Startup 或自建任务调度）进行延迟加载。
- **避免 IO 阻塞**: 在 `Application` 的启动链条中，绝对禁止执行耗时 I/O 操作 (如主线程读取大量属性文件或执行数据库预警)，所有持久化配置加载应当放入后台线程完成预热。

## 7. 动画与自定义 View 性能 (Animations & Custom Views)
- **及时暂停动画**: 当含有不断轮询绘制的 `ValueAnimator`，或者是循环跑马灯类特效以及带有内部 Handler 定时循环的自定义 View 不可见，或宿主页面退入后台时，必须在对应的生命周期回调中自动终止或暂停，以避免持续占用 CPU 并造成无谓的电量损耗与内存满载。
- **杜绝 `onDraw` 内存抖动**: 在自定义 View 内部，**严禁**在 `onDraw()` 方法中创建任何新对象实例（如 `new Paint()`, `new Rect()` 等）。这会随着屏幕的刷新快速引发海量 GC 导致界面丢帧掉帧。需要渲染的对象应当在 View 的初始化截断预先分配缓存并多次复用。

## 8. 集合与数据操作基准约束 (Collections & Data Source Efficiency)
- **Sequence 对大规模流式链的性能挽救**: 当需要对含有大量元素的 List 或数据集合进行多次链式高阶函数变换（如长串的 `.map { }.filter { }.sortedBy { }`）时，**必须**前置将其转化为 `asSequence()` 序列。这样将改为惰性求值流式处理，避免在每一个链式操作中间阶段都产生全新的临时中间集合对象而拖慢整体性能和暴增内存开销。

## End of Rules

## Additional: Mandatory Pre-Commit Review
> Agent 完成编码准备提交前，必须自动运行与本次修改相关的规范审查（架构、代码风格、命名、Android 专项、安全）。
> 审查未通过的代码禁止提交。
