# E2E P0 自动化测试执行 Skill

## 元数据
```yaml
name: e2e-test-execute
version: 2.1.0
author: android-team
description: 编码完成后，基于P0测试规格自动执行E2E测试验证，产出测试报告
category: testing
compatible_tools:
  - opencode
trigger: 由 Test-Run Agent 在 `/test-run <slug>` 阶段加载(研发确认用例后、Review 之前)
requires:
  - adb (Android Debug Bridge)
  - maestro CLI (可选，优先使用)
  - 真机或模拟器已连接
```

## 适用场景
- 编码完成后，自动验证 P0 测试用例
- MR/PR 提交前的自动化回归验证
- 需求交付前的验收测试

## 核心原则

> **AI 执行测试的五大原则**：
>
> 1. **先验前置条件，再跑测试** → 用 ADB 或 Maestro 检查 critical 前置条件（如登录状态、会员状态），不满足则立即中止
> 2. **先查映射表，再写选择器** → 所有 resource-id 来自 `.ai/maestro-tests/ui-elements.yaml`，注意运行时是 snake_case
> 3. **弹窗处理用公共子流程** → 引用 `flows/dismiss_popups.yaml`，不要内联编写
> 4. **ADB 验证通过即有效** → 不必执着于 Maestro 脚本全自动通过，ADB 手动验证 + 截图 = 有效证据
> 5. **验证到即止** → 核心验证点确认通过后立即出报告，不要反复重跑脚本试图追求完美

## 执行步骤

### Step 0：环境检查

```bash
# 1. 检查 adb 连接
adb devices

# 2. 检查 App 是否已安装
adb shell pm list packages | grep com.rainbow.compose

# 3. 检查 Maestro 是否可用（可选，不可用时降级为 ADB 模式）
maestro --version

# 4. 获取设备信息（用于报告）
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
```

### Step 0.5：前置条件检查

> ⚠️ **必须在执行测试之前验证前置条件，避免浪费时间。**

根据 test-spec.md 中的 `preconditions` 列表，逐条验证。常见的验证方式：

```bash
# 方式1：ADB + uiautomator dump 验证（推荐，稳定）
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml /tmp/
grep "有效期至" /tmp/ui.xml    # 检查会员状态
grep "ID " /tmp/ui.xml          # 检查登录状态

# 方式2：ADB 截图 + 人工确认
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png /tmp/

# 方式3：ADB shell 命令验证（如权限检查）
adb shell dumpsys package com.rainbow.compose | grep "calendar"
```

> 注意：`adb shell cat /sdcard/ui.xml` 在新版 Android 上可能因 scoped storage 失败，
> 建议先 `adb pull` 到本地再 `grep`。

**如果检查失败**：
- critical=true 的条件失败 → **立即中止测试，报告 BLOCKED 状态**
- critical=false 的条件失败 → 记录并继续，在报告中标注

### Step 1：读取测试规格

从以下位置查找测试规格（按优先级）：
1. `.openspec/active/<slug>/05-test-design.md` — Test-Design 阶段产出的测试规格（Quick 模式则 `quick.md §5.5.1`）
2. `.openspec/active/<slug>/05-<feature>_p0.yaml` — Test-Design 生成的 Maestro 脚本
3. Git diff 中的代码改动 — 兜底：从代码推导

**解析内容**：
- `preconditions` → 前置条件列表
- `navigation_paths` → 导航路径列表（选取 recommended=true 的路径）
- `test_cases` → P0 用例列表
- `verification_points` → 验证点列表

### Step 2：验证前置条件

对每个 precondition 逐条验证：

```yaml
# 示例：验证用户是否为会员
- id: PRE-001
  check_method: "ui"  # ui / adb_command / api
  steps:
    - action: "导航到'我的'页面"
    - action: "检查会员卡区域是否显示'会员'和有效期"
    - verify: "uiautomator dump 中存在 text='会员'"
  fallback:
    - action: "如果不是会员，提示用户手动充值"
    - action: "或尝试自动完成会员订阅（如有沙盒环境）"
```

**前置条件验证结果**：
| PRE-ID | 状态 | 备注 |
|--------|------|------|
| PRE-001 | PASS/FAIL/SKIP | 具体信息 |

> ⚠️ 如果 critical=true 的前置条件 FAIL，立即中止测试并报告。

### Step 3：执行测试（分级验证策略）

> ⚠️ **分级验证原则**：不要在 Maestro 脚本调试上花太多时间。
> Maestro 跑通最好，跑不通就降级到 ADB，ADB 验证 + 截图 = 有效证据。

#### Level 1：Maestro 全自动（优先尝试，最多 2 次）

```bash
maestro test .openspec/active/<slug>/05-<feature>_p0.yaml
```

**如果第 1 次失败**：
- 分析失败原因（元素找不到？弹窗遮挡？正则语法？resource-id snake_case？）
- 修正脚本后重试 1 次
- **最多重试 1 次**，不要陷入反复调试 Maestro 脚本的循环

#### Level 2：ADB 逐步验证（Maestro 2 次失败后立即切换）

使用 `adb shell input` + `uiautomator dump` + `screencap` 逐步执行：

```
对每个 test_case:
  1. 按 navigation_path 导航到目标页面
  2. 对每个 step:
     a. 执行 action（tap/swipe/input）
     b. 等待页面稳定（sleep 2-3s）
     c. 截图保存证据
     d. 验证 expected（pull ui.xml 到本地后 grep，或截图确认）
  3. 记录每步的 PASS/FAIL
```

**关键规则**：
- 每个验证点截图保存作为证据
- uiautomator dump 超时（`ERROR: could not get idle state`）时，用截图替代
- **一旦核心验证点（VP-001 等）通过，立即停止继续验证，直接进入 Step 5 生成报告**

#### Level 3：截图 + 人工确认（ADB 也不稳定时）

- 截图保存关键页面状态
- 记录已验证的点和无法自动验证的点
- 将无法自动验证的点标记为"需人工确认"

**执行过程中的障碍处理**：

| 障碍 | 检测方式 | 处理方式 |
|------|----------|----------|
| 弹窗遮挡 | 截图确认 或 dump 检查 | 引用 `flows/dismiss_popups.yaml`；ADB 模式用 `adb shell input tap` 点击 iv_close |
| 页面加载中 | screencap 为空白/黑屏 | 重试等待，最多3次，每次5秒 |
| UI idle 超时 | uiautomator dump 返回 ERROR | **放弃 dump，改用截图验证**（动画页面 dump 经常超时） |
| 外部App跳转 | 当前 Activity 不是本App | 连续按 BACK 返回，或用 `am start` 重启本App |
| 导航路径不通 | 目标元素不可见 | 切换到备选 navigation_path |
| Maestro 正则不支持 | 测试报错 `Element not found` | 改用简单正则 `.*关键词.*`，避免 `[x]?`、`(?:)` 等语法 |
| 元素 id 不匹配 | Maestro 找不到元素 | **查阅 `ui-elements.yaml`**，不要凭猜测修改；注意 snake_case vs camelCase |
| scrollUntilVisible 找不到 | timeout 内未发现元素 | 检查 `direction` 是否正确（UP/DOWN），增大 timeout，或降低 `visibilityPercentage` |

### Step 4：结果验证

对每个 verification_point：

```python
# 伪代码
def verify(vp):
    # 优先 pull 到本地再检查（避免 scoped storage 问题）
    adb_pull("/sdcard/ui.xml", "/tmp/ui.xml")
    screenshot = adb_screencap()
    
    if vp.method == "assertVisible":
        return vp.target_text in local_grep("/tmp/ui.xml")
    elif vp.method == "assertNotVisible":
        return vp.target_text not in local_grep("/tmp/ui.xml")
    elif vp.method == "value_change":
        before = vp.before_value
        after = extract_value("/tmp/ui.xml", vp.target_id)
        return after == before + vp.expected_delta
```

### Step 5：生成测试报告

```markdown
# E2E P0 测试报告

## 概览
- 测试时间：YYYY-MM-DD HH:mm
- 设备信息：[型号] [Android版本] [分辨率]
- App版本：[versionName] ([versionCode])
- 分支：[git branch]
- 测试结果：PASS / FAIL / PARTIAL

## 前置条件验证
| PRE-ID | 条件 | 状态 | 备注 |
|--------|------|------|------|
| PRE-001 | [条件描述] | ✅ PASS | [详情] |

## 测试用例执行结果
| TC-ID | 用例名 | 状态 | 耗时 |
|-------|--------|------|------|
| TC-P0-001 | [用例名] | ✅ PASS | 45s |

### TC-P0-001 详细步骤
| Step | 操作 | 预期 | 实际 | 状态 | 截图 |
|------|------|------|------|------|------|
| 1 | [操作] | [预期] | [实际] | ✅ | s1.png |

## 验证点汇总
| VP-ID | 验证内容 | 状态 |
|-------|----------|------|
| VP-001 | [描述] | ✅ |

## 截图证据
[所有截图保存路径]

## 问题与建议
[测试过程中发现的问题]
```

报告由 Test-Run Agent 保存到：`.openspec/active/<slug>/06-test-run.md`（按 `test-run-template.md`）；截图证据存 `.openspec/active/<slug>/06-test-reports/`（git 忽略，目录不存在时自动创建）。Quick 模式则追加到 `quick.md §5.5.2`。

## 与工作流的集成（OpenSpec 五阶段，CHARTER v1.12.0）

```
/dev → /test-design(05-test-design + 05-<feature>_p0.yaml,待研发确认)
            ↓ 研发确认
       /test-run → 本 Skill(e2e-test-execute)
            ↓ 分级执行(Maestro→ADB→截图)
       产物: 06-test-run.md + 06-test-reports/
            ↓
     ┌─── PASS / PARTIAL(环境受限) → /review
     └─── FAIL(P0 功能 Bug) → /dev 回流修复(读 06-test-run §失败清单)
                              → 重跑 /test-run 复测轮 → 循环至 PASS
```

> 降级哲学:真实功能 FAIL → 阻断回 /dev;环境受限(无设备/无 maestro/dump 超时)→ PARTIAL/BLOCKED(环境),标注建议人工补测,门禁层 warn 不阻断。
> 业务知识:执行阶段**按需**查业务知识库(前置条件失败/验证点异常时,判断是否已知业务行为,避免误报 FAIL)。
> Quick 模式:执行报告追加 `quick.md §5.5.2`。


## 检查清单

执行测试前：
- [ ] 设备已连接且 adb 可用
- [ ] App 已安装且为最新构建
- [ ] **前置条件已通过**（ADB 验证或 Maestro 验证）
- [ ] 测试规格文件存在且格式正确
- [ ] **`ui-elements.yaml` 已查阅**，Maestro 脚本中的 id 都来自映射表
- [ ] **确认 resource-id 使用 snake_case**（不是代码中的 camelCase）

执行测试中：
- [ ] Maestro 最多重试 2 次，失败后立即降级到 ADB
- [ ] ADB 验证每步都有截图证据
- [ ] **核心验证点通过后立即停止**，不要反复重跑
- [ ] `scrollUntilVisible` direction 与元素位置匹配

执行测试后：
- [ ] 所有 P0 用例 PASS（或标记 BLOCKED/SKIP 并说明原因）
- [ ] 测试报告已生成
- [ ] 截图证据已保存
- [ ] 发现的问题已记录
- [ ] **新发现的 UI 元素已补充到 `ui-elements.yaml`**
