---
name: "Security Scanner"
description: "A tool to audit code based on the team's security guidelines."
---

# Security Scanner Instruction

You are an advanced AI Agent tasked with acting as a strict code reviewer.
You must strictly follow the engineering guidelines summarized below.
When analyzing the code, point out any section that violates these rules and provide code modifications.

## Core Rules from security.md

# 安全开发规范 (Security Guidelines)

本文档为开发人员和 AI 辅助工具提供统一的安全准则与代码生成基准，覆盖网络安全、密钥管理、日志规范、数据持久化及 WebView 容器五大领域。所有规则均为通用原则，具体实现由宿主项目按需落地。

---

## 一、 网络安全开发规范 (Network Security)

### 开发规范 (Specifications)
- **传输层安全**: 严禁在代码中初始化信任所有证书的 `TrustManager` 或 `HostnameVerifier`。应优先排除 `user` 别名证书以防御常规抓包工具。
- **代理防护**: 关键业务（如登录、支付、风控）的 HTTP Client 必须配置 `proxy(Proxy.NO_PROXY)`，防止被代理劫持。
- **签名机制**: 所有请求应统一添加签名头，包含 `timestamp`、`POST Body`、`device_id` 及 `token`。
- **密钥存储**: 签名密钥严禁以完整字符串明文存放，必须采用分段硬编码拼接或加密存储。
- **数据加密**: 建议采用 `AES/CBC/PKCS5Padding` (128 bit) 等标准算法，敏感字段加密后传输。

---

## 二、 第三方 SDK 密钥 (Key) 管理规范 (Key Management)

### 开发规范 (Specifications)
- **禁用硬编码**: 严禁在独立业务文件中硬编码 Key。必须统一收纳至集中配置类或 `strings.xml`。
- **注入选择**: 若 SDK 需要在 Manifest 配置，使用 `manifestPlaceholders`。高敏感度 Key 必须加密或分段存储。
- **命名规范**: 代码级变量使用大写蛇形且带前缀（如 `AF_DEV_KEY`）；XML 资源带语义前缀（如 `facebook_app_id`）。
- **AI 规则**: 检查 Key 是否已存在。高价值 Key 必须实现分段存储，并配置混淆规则保护常量类。

---

## 三、 高效日志规范与回捞机制 (Logging & Retrieval)

### 开发规范 (Specifications)
- **统一调用**: 严禁使用原生 `Log` 类，必须使用统一封装的日志工具（本框架 `ComposeLog`）。
- **屏蔽规则**: 正式包必须在 Release 环境关闭日志输出，仅调试环境开启。
- **数据脱敏**: 严禁记录明文密码、信用卡完整号、身份 Token。归因数据、支付中间参数必须先加密再记录。
- **日志级别**: 复杂对象或 Lifecycle 回调应使用 `v` 或 `d` 级别，错误应使用 `e` 级别保证记录。

> ⚠️ **TODO**: 日志系统的分级策略、生产关闭、回捞上传机制待后续统一设计。

---

## 四、 本地数据持久化安全规范 (Persistence Security)

### 开发规范 (Specifications)
- **存储路径规约**: 严禁将业务数据存在外部公共目录。必须使用 `context.filesDir` 或 `context.cacheDir`，确保数据处于 App 沙盒保护下。
- **敏感控制**: 严禁在数据库（如搜索历史）中记录用户输入的明文密码。
- **状态维护**: Logout 时必须重置相关业务字段，清除敏感缓存。
- **存储选型**: 结构化数据用 Room，用户偏好/配置用 DataStore，高敏感 Token 用加密存储（如 EncryptedSharedPreferences / Keystore），严禁明文。

---

## 五、 WebView 与 H5 容器安全及规范 (WebView Security)

### 开发规范 (Specifications)
- **初始化限制**: 必须统一初始化 WebView 容器，严禁在业务处重写 `WebSettings` 基础安全开关（如禁用 JS 随意开启、文件访问等）。
- **交互规范**: JSBridge 必须使用 `json: String` 统一入参。所有 UI 操作建议在主线程执行。
- **回调规范**: 回调 H5 必须走统一方法，禁止拼接字符串构造 JS。
- **异常规约**: 域名跳转必须校验 host（白名单）。SSL 异常场景应根据业务收紧逻辑，关键支付页严禁放行。
- **认证同步**: Native 的鉴权信息（Cookie/Token）只注入到受控域名，避免泄漏到第三方域名。

---

## 六、 安全能力清单 (Capability Matrix)

> 下表面向本通用框架，标注各安全能力的**落地状态**：`✅ 框架已提供` / `🟡 宿主需实现` / `⏳ 待设计`。

| 能力 | 说明 | 状态 |
|---|---|---|
| 统一日志工具 | 集中日志入口、生产关闭、脱敏 | ✅ `ComposeLog`（分级/回捞待设计 ⏳） |
| 证书信任防护 | 排除 user 别名证书、拒绝全信任 | 🟡 宿主实现（网络层） |
| 代理防护 | 关键业务 `NO_PROXY` | 🟡 宿主实现（网络层） |
| 请求签名 | sign 头（timestamp/body/device/token） | 🟡 宿主实现 |
| 密钥分段存储 | 密钥拆分段、混淆保护常量 | 🟡 宿主实现 |
| 数据加密 | AES/CBC 等标准算法 | 🟡 宿主实现 |
| SDK Key 集中管理 | 集中配置类 + manifestPlaceholders | 🟡 宿主实现 |
| 持久化路径安全 | filesDir/cacheDir 沙盒 | 🟡 宿主实现 |
| 敏感数据不落库 | 明文密码/Token 禁止入库 | 🟡 宿主实现 |
| 日志脱敏 | 明文敏感信息禁止记录 | ✅ 规范约束 + 🟡 实现 |
| WebView 安全 | 域名白名单、JSBridge 规范、Cookie 受控域 | 🟡 宿主实现 |
| 埋点/上报安全 | 归因、支付参数加密后再上报 | ⏳ 埋点暂不引入 |
| 日志回捞机制 | 服务端下发配置、压缩上传 | ⏳ 待设计 |

**缺失/待定能力总结**：框架目前只内置了统一日志入口（`ComposeLog`）这一项通用安全能力；其余（证书防护、代理防护、签名、密钥管理、加密、WebView 安全等）都属于「网络层/业务层」能力，需要宿主项目在落地网络层与 WebView 容器时实现；「日志分级与回捞」「埋点上报安全」两项尚待后续设计。

## End of Rules

## Additional: Mandatory Pre-Commit Review
> Agent 完成编码准备提交前，必须自动运行与本次修改相关的规范审查（架构、代码风格、命名、Android 专项、安全）。
> 审查未通过的代码禁止提交。
