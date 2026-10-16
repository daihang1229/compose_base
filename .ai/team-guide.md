# Compose Base AI 编码规范使用指南 (Team Guide)

本仓库内置了一套 `.ai` 编码规范，供 AI 助手在生成、重构、审查代码时统一遵守。本文档说明规范内容与使用方式。

## 1. 规范体系概览

`.ai/` 目录下按「单一事实来源 (SSOT)」组织，规则只维护一份，审查 prompt / 平台技能由规则派生：

```
.ai/
├── rules/                      # 🔴 规则 SSOT（AI 编码时内化的核心）
│   ├── code-style.md           # 代码风格
│   ├── naming.md               # 命名规范
│   ├── android-specific.md     # Android 专项（MVVM/Hilt/协程/防抖/生命周期）
│   ├── compose-mvi.md          # Compose MVI 架构（7 文件结构）
│   ├── performance.md          # 性能优化
│   ├── security.md             # 安全开发（含能力清单）
│   ├── testing.md              # 测试规范
│   ├── hardcode-detection-rules.yaml  # 硬编码敏感信息扫描
│   └── architecture/
│       └── architecture.md     # 架构总规范（Clean Arch + MVVM）
├── skills_prompts/             # 7 个单维度审查 prompt（Copilot 用 #file 引用）
├── antigravity_skills/         # 7 个审查技能（Antigravity / OpenCode 用）
├── skills/                     # 脚手架 & 测试技能
└── team-guide.md               # 本文档
```

## 2. 八类规则速查

| 规则 | 核心约束 | 何时用 |
|---|---|---|
| **架构** `architecture.md` | Clean Arch 三层、防腐层(Mapper)、UseCase 单职责、Hilt 注入 | 设计/写业务代码时 |
| **代码风格** `code-style.md` | 行宽 120、函数 ≤30 行、避免 `!!`、`when` 替代 if-else、KDoc | 日常写码 |
| **命名** `naming.md` | PascalCase / camelCase / SCREAMING_SNAKE、资源前缀 | 新增类/变量/资源 |
| **Android 专项** `android-specific.md` | Hilt、协程作用域、`onClickDelay` 防抖、生命周期安全 | Android 特性代码 |
| **Compose MVI** `compose-mvi.md` | 7 文件结构、`UiState` 五态、`moveXxx`、`StatusOverlay` | Compose 页面 |
| **性能** `performance.md` | 防泄漏、懒加载、Dispatchers.IO、APK 体积 | 性能敏感代码 |
| **安全** `security.md` | 证书防护、密钥管理、日志脱敏、WebView 白名单 | 网络/存储/鉴权 |
| **测试** `testing.md` | 测试金字塔、覆盖率、AAA 模式、命名规范 | 写测试时 |

## 3. 单维度审查（7 个维度）

`skills_prompts/check_*.md` 对应 7 个审查维度，在 Copilot Chat 中用 `#file_name` 引用即可对选中代码做单维度极严审查：

| 维度 | 文件 |
|---|---|
| 架构 | `check_architecture.md` |
| 代码风格 | `check_code_style.md` |
| 命名 | `check_naming.md` |
| Android 专项 | `check_android_specific.md` |
| 安全 | `check_security.md` |
| 性能 | `check_performance.md` |
| 测试 | `check_testing.md` |

`antigravity_skills/` 下为同名技能（Antigravity 平台 / OpenCode 用），内容与上述审查维度一致。

## 4. 提交前自审

提交代码前，务必对照上述规则自查本次改动涉及的维度（架构 / 风格 / 命名 / Android / 安全，性能与测试按需追加）。硬编码敏感信息扫描由 git `pre-commit` hook 基于 `hardcode-detection-rules.yaml` 自动执行。

## 5. 技能 (Skills)

- **compose-mvi-scaffold**：一键生成 Compose MVI 7 文件脚手架。
- **testing/e2e-test-design / e2e-test-execute**：E2E 测试用例设计与执行。

## 6. 待办 (TODO)

- [ ] 统一日志系统的完整设计（分级策略、生产关闭、回捞机制）
- [ ] 埋点方案确定后补充埋点规范
