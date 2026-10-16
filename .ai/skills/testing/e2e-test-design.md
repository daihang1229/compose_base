# E2E P0 测试用例设计 Skill

## 元数据
```yaml
name: e2e-test-design
version: 2.1.0
author: android-team
description: 在需求分析/Spec阶段，基于需求文档和代码改动，自动生成P0级E2E测试用例（含测试上下文）
category: testing
compatible_tools:
  - opencode
  - antigravity
trigger: 由 Test-Design Agent 在 `/test-design <slug>` 阶段加载(Dev 之后、Review 之前)
```

## 适用场景
- **Spec 阶段（推荐）**：需求分析完成后，编码开始前，先设计 P0 测试用例
- **编码完成后**：代码提交前，根据实际代码改动补充/修正测试用例
- **Code Review 阶段**：审查代码时，验证是否有遗漏的 P0 场景

## 核心理念

> **P0 测试用例 = 需求验收标准的可执行版本**
> 
> 不是测代码逻辑，而是测"用户能不能完成这个功能"。

## 执行步骤

### Phase 0：UI 元素自动发现与映射（必做！）

> ⚠️ **第一步永远是查阅并补充 `.ai/maestro-tests/ui-elements.yaml`**，获取真实的 resource-id。
> 不要根据代码中的变量名猜测 resource-id！代码变量名和运行时 resource-id 经常不一致。

> ⚠️ **snake_case 陷阱**：Android layout XML 中使用 snake_case（如 `ll_calendar_reminder`），
> ViewBinding 自动转为驼峰属性（如 `llCalendarReminder`）。**Maestro 和 ADB 中必须使用 layout XML 中的 snake_case 命名**，
> 不是代码中的驼峰变量名。必须通过 `uiautomator dump` 确认运行时实际的 resource-id。

#### Step 0.1：查阅已有映射

1. **读取 `.ai/maestro-tests/ui-elements.yaml`**，查找目标页面涉及的所有元素
2. 如果目标页面**已存在**且信息充分，跳到 Phase 1

#### Step 0.2：自动发现新页面元素（Auto-Discovery）

> 当目标页面不在映射表中，或映射信息不完整时，**必须**执行以下自动发现流程。

1. **导航到目标页面**：
   - 使用 ADB 或 Maestro 自动导航到目标页面
   - 如果需要多步导航，参考 `ui-elements.yaml` 中已知的导航元素
   - 处理途中弹窗：`adb shell input tap <x> <y>` 点击 `iv_close` 坐标

2. **Dump UI 层级**：
   ```bash
   # 基础 dump（大多数静态页面可用）
   adb shell uiautomator dump /sdcard/ui_<page_name>.xml
   adb pull /sdcard/ui_<page_name>.xml /tmp/

   # 如果报 "could not get idle state"（动画页面），等待后重试：
   adb shell sleep 3 && adb shell uiautomator dump /sdcard/ui_<page_name>.xml
   ```

3. **提取 resource-id 列表**：
   ```bash
   cat /tmp/ui_<page_name>.xml | grep -oE 'resource-id="com\.rainbow\.compose:id/[^"]*"' \
     | sed 's/resource-id="com\.rainbow\.compose:id\///' | sed 's/"//' | sort -u
   ```

4. **分析关键元素**：对每个 resource-id，从 XML 中提取：
   - `class`（控件类型）
   - `text`（显示文案）
   - `bounds`（位置坐标）
   - `clickable`（是否可点击）
   - 判断该元素在测试中的用途（导航、输入、验证、可忽略）

5. **更新 `ui-elements.yaml`**：
   - 按页面分组添加新发现的元素
   - 使用统一的 YAML 格式（参考现有页面结构）
   - 为每个元素添加 `maestro_selector` 字段
   - 添加 `notes` 标注易错点或特殊行为
   - **注意**：`bounds` 坐标仅供参考，不同设备/分辨率会不同

#### Step 0.3：记录元素匹配策略

- **每个导航步骤和验证点**都必须标注使用 `id` 匹配还是 `text` 匹配
- **优先级**：`id` > `text`（id 更稳定，text 可能被运营修改或多语言不同）
- **text 匹配规则**：使用 `.*关键词.*` 模糊匹配，避免完全匹配

### Phase 0.5：业务知识查询（可选）

> 💡 **P0 用例 = 需求验收标准的可执行版本**。UI 元素映射（Phase 0）只给"技术怎么点"，业务知识才给"应该测什么、哪些前置条件 critical、哪些边界易出 Bug"。

1. 若项目接入了业务知识库，可查询业务上下文（核心业务规则 / 已知坑 / 边界场景）：
   - 核心业务规则 → 推导核心用户路径 → P0 用例主流程
   - 已知坑 / 边界场景 → 推导 critical 前置条件 + 边界用例（如状态机流转、权限触发时机）
   - 发现业务知识与实际代码不符 → 记录并反馈

2. **示例**：
   - 业务给出"订单状态机 PENDING→PAID→DELIVERED" → P0 用例必须覆盖各状态流转 + 异常态前置条件
   - 业务提示"日历权限首次触发时机" → critical 前置条件 = 日历权限状态

> 输出：把提炼结果写入 test-spec 的"测试上下文 §1.0 业务上下文"，每条 P0 用例的 critical 前置条件标注依据。

### Phase 1：需求理解与功能拆解

1. **输入来源**（按优先级）：
   - Spec 文档（`.openspec/active/xxx.spec.md`）
   - PRD/需求描述
   - Git diff（代码改动）
   
2. **提取核心用户故事**：
   ```
   作为 [角色]，我要 [操作]，以便 [目标]
   ```

3. **识别功能边界**：
   - 入口：用户从哪里进入这个功能？
   - 操作：用户需要做什么？
   - 出口：操作完成后用户看到什么？

### Phase 2：测试上下文分析（关键！）

> ⚠️ **这是此前测试失败的核心原因**：缺少测试上下文导致 AI 不知道前置条件和导航路径。

必须输出以下上下文信息：

#### 2.1 前置条件清单
```yaml
preconditions:
  - id: PRE-001
    name: "用户身份要求"
    description: "用户需要是XX状态"
    how_to_setup: "具体操作步骤"
    verify_command: "adb shell 命令或UI验证方式"
    critical: true  # 缺失此条件是否导致功能不可见/不可用
```

**必须回答的问题**：
- [ ] 用户需要登录吗？
- [ ] 用户需要特定身份（会员/VIP/新用户）？
- [ ] 需要特定的服务端配置（AB实验/开关/运营配置）？
- [ ] 需要特定设备状态（网络/权限/安装特定App）？
- [ ] 需要先完成其他操作（购买/观看/签到）？

#### 2.2 导航路径分析

**从代码推导导航路径的方法**：

1. **找到目标 Fragment/Activity**：
   - 通过代码改动定位目标页面类
   
2. **反向追踪入口**：
   - 搜索 `startActivity`/`navigate`/`findNavController` 中引用该页面的代码
   - 搜索 `FragmentTransaction`/`ViewPager`/`TabLayout` 中加载该 Fragment 的代码
   - 检查 `AndroidManifest.xml` 中的 intent-filter 和 deeplink
   
3. **追踪条件分支**：
   - 检查代码中的 `if/when` 分支：同一入口可能因配置不同展示不同页面
   - 例如：`AppKV.bottomTab == 2` 时福利tab展示积分页，否则展示充值页
   
4. **输出所有可行路径**（标注推荐路径）：
```yaml
navigation_paths:
  - id: NAV-001
    name: "路径1：我的 → 福利中心 → 会员积分tab"
    recommended: true
    steps:
      - action: "点击底部tab'我的'"
        target: "id: .*id_ll_profile"    # 来自 ui-elements.yaml，注意不是 id_tv_mine
      - action: "关闭可能的弹窗"
        target: "引用 flows/dismiss_popups.yaml"
      - action: "点击'福利中心'"
        target: "text: 福利中心"
      - action: "点击'会员积分'tab"
        target: "text: 会员积分"
    preconditions: ["PRE-001"]
    
  - id: NAV-002
    name: "路径2：底部tab'福利'（仅bottomTab==2时）"
    recommended: false
    conditional: "AppKV.bottomTab == 2"
    steps:
      - action: "点击底部tab'福利'"
        target: "id: .*id_tv_member"     # 来自 ui-elements.yaml
```

#### 2.3 验证点分析

**从代码推导验证点的方法**：

1. **UI 变化**：搜索代码中的 `setText`/`setVisibility`/`setEnabled`/`setImageResource`
2. **状态变化**：搜索 `StateFlow`/`LiveData` 的 emit/setValue
3. **数据变化**：搜索 API 调用后的数据更新逻辑
4. **跳转行为**：搜索 `startActivity`/`ExternalAppLauncher`/`Intent`

```yaml
verification_points:
  - id: VP-001
    what: "目标元素可见"
    how: "assertVisible: id/text"
    code_ref: "XxxFragment.kt:行号"
    
  - id: VP-002
    what: "状态变化"
    how: "assertVisible: '变化后的文案'"
    code_ref: "XxxFragment.kt:行号"
    trigger: "触发操作"
```

### Phase 3：P0 测试用例生成

每个 P0 用例必须包含：

```yaml
test_case:
  id: "TC-P0-001"
  name: "[测试用例名称]"
  priority: P0
  
  preconditions:
    - "PRE-001: [条件描述]"
  
  navigation: "NAV-001"  # 引用导航路径
  
  steps:
    - step: 1
      action: "[操作]"
      expected: "[预期结果]"
      verification: "VP-001"
  
  maestro_script: ".ai/maestro-tests/[功能名]_p0.yaml"
```

### Phase 4：Maestro 脚本生成

根据 Phase 3 的用例，自动生成 Maestro YAML。

**生成规则**：
1. 所有 resource-id 必须来自 `.ai/maestro-tests/ui-elements.yaml`，禁止猜测
2. **优先使用 `id` 匹配（resource-id），其次用 `text` 匹配（业务文案）** — id 更稳定，text 可能被运营修改或因多语言不同
3. 每步之间加 `waitForAnimationToEnd`
4. 弹窗处理统一引用公共子流程：`- runFlow: { file: flows/dismiss_popups.yaml }`
5. 使用 `scrollUntilVisible` 而非假设元素可见
6. 外部 App 跳转用 `pressKey: back` 返回
7. text 匹配使用简单正则，避免 `[x]?`、`(?:)` 等高级语法（Maestro 不支持）
8. 使用 `.*关键词.*` 模糊匹配服务端下发的文案，避免完全匹配（运营可能改文案或有拼写错误）

**Maestro 已知限制**（必须遵守）：
- 正则仅支持基础语法：`.`、`*`、`+`、`|`、`()`、`\\d`、`\\w`
- 不支持：`[abc]`、`[^abc]`、`x?`（零或一次）、`(?:)`、`\\b`
- `runFlow.when.visible` 检测 id 时必须用 `id:` 字段，不能用纯字符串
- `assertVisible` 的 `optional: true` 不会导致测试失败，适合用于非关键验证（如 Toast）
- `scrollUntilVisible` 的 timeout 建议 10000-15000ms，太短容易误报
- **`scrollUntilVisible` 的 `direction` 参数**：`DOWN` 表示向下滚动查找（元素在下方），`UP` 表示向上滚动查找（元素在上方）。注意进入页面后的默认滚动位置，选择正确的方向
- **`runFlow: file:` 路径是相对于调用脚本所在目录的**，不是项目根目录。如果脚本在 `.ai/maestro-tests/` 下，引用 `flows/dismiss_popups.yaml` 正确；如果脚本在子目录如 `.ai/maestro-tests/membership/`，则需要 `../flows/dismiss_popups.yaml`

**实战踩坑记录**：
- **resource-id 命名差异**：layout XML 使用 snake_case（`ll_calendar_reminder`），ViewBinding 生成驼峰属性（`llCalendarReminder`）。**Maestro 中必须使用 snake_case**，必须通过 `uiautomator dump` 确认
- **弹窗关闭按钮 ID 冲突**：`iv_close`（通用弹窗）vs `close`（挂件浮层）vs 日历弹窗的 `close` 是不同元素，选择器要注意精确性

**模板引用**：`.ai/maestro-tests/templates/base_flow.yaml`
**弹窗子流程**：`.ai/maestro-tests/flows/dismiss_popups.yaml`
**元素映射表**：`.ai/maestro-tests/ui-elements.yaml`

## 输出格式

最终输出一个完整的测试文档，由 Test-Design Agent 保存到 `.openspec/active/<slug>/05-test-design.md`（Maestro 脚本另存 `05-<feature>_p0.yaml`；Quick 模式则追加到 `quick.md §5.5.1`）：

```markdown
# E2E P0 测试规格：[需求名]

## 1. 测试上下文

### 1.0 业务上下文（如有知识库则填写）
[业务核心规则 / 已知坑 / 边界场景 摘录]
[前置条件列表]

### 1.1 前置条件
[preconditions 列表（critical 项标注依据）]

### 1.2 导航路径
[navigation_paths 列表]

### 1.3 验证点
[verification_points 列表]

## 2. P0 测试用例

### TC-P0-001: [用例名]
[完整用例描述]

## 3. Maestro 脚本
[自动生成的 YAML 脚本路径和内容]

## 4. 手动验证备注
[AI无法自动验证的点，需人工确认]
```

## 检查清单

生成测试用例时，必须逐项确认：

- [ ] **是否已查询业务知识（如有知识库）并把核心规则/warnings 纳入 §1.0？**
- [ ] 是否先查阅了 `.ai/maestro-tests/ui-elements.yaml` 获取真实 resource-id？
- [ ] 是否通过 `uiautomator dump` 确认了运行时 resource-id（注意 snake_case vs camelCase）？
- [ ] 导航路径中的每个 target 是否都来自 ui-elements.yaml 或 adb dump（不是猜测）？
- [ ] 是否明确了所有前置条件（尤其是用户身份/付费状态）？
- [ ] 是否找到了所有可行的导航路径？
- [ ] 是否标注了条件分支（如 AB 实验/配置开关导致的不同入口）？
- [ ] 每个验证点是否有对应的代码引用？
- [ ] 弹窗处理是否引用了 `flows/dismiss_popups.yaml` 而非内联重复编写？
- [ ] Maestro 脚本中的正则是否避免了不支持的语法（`[x]?`、`(?:)` 等）？
- [ ] text 匹配是否使用了 `.*关键词.*` 模糊匹配而非完全匹配？
- [ ] 外部跳转（如浏览器/第三方App）后的返回是否有处理？
- [ ] `scrollUntilVisible` 的 `direction` 是否根据元素位置设置正确？
- [ ] `runFlow: file:` 路径是否相对于脚本所在目录（而非项目根目录）？
- [ ] Maestro 脚本是否可以独立运行？

## 与工作流的集成（OpenSpec 五阶段，CHARTER v1.12.0）

```
/req → /design → /dev → /test-design → /test-run → /review → /commit → archive
                  04       │05            06         07         08
                          ▼
                 [本 Skill: e2e-test-design]  ← 由 Test-Design Agent 加载
                  Phase 0   UI 元素映射(ui-elements.yaml)
                  Phase 0.5 业务知识(如有知识库)
                  Phase 1-4 P0 用例 + Maestro 脚本
                          ▼
                 产物: 05-test-design.md + 05-<feature>_p0.yaml(待研发确认)
                          ▼
                 研发确认 → /test-run(加载 e2e-test-execute Skill 执行)
```

> Quick 模式:用例设计追加 `quick.md §5.5.1`(不拆双 Agent)。

