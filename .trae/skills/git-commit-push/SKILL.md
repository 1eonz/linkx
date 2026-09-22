---
name: git-commit-push
description: "辅助用户提交和推送代码，串联 AI Code Review 流程。当用户要求提交代码、commit、push、推送代码时触发。自动处理 git add、AI 审查、commit、push 全流程。"
alwaysApply: false
---

# Git Commit & Push Skill

辅助用户完成 git add → commit → AI Code Review → push 全流程。

## 核心原则

**仅对不可逆操作、需主观判断的决策、代码修改三类操作要求用户确认。** 默认行为与已授权操作的延续可自动执行，连续同目标操作合并为一次确认。

### 确认策略分级

| 类别 | 策略 | 示例 |
|------|------|------|
| 不可逆 / 需主观判断 / 代码修改 | **必须确认** | push、commit message、修复方案、修复 vs 跳过 |
| 连续同目标操作 | **合并为一次确认** | pull+push 合并、message 确认即 commit 确认 |
| 默认行为 / 已授权操作的延续 | **自动执行** | 暂存区已有内容时直接使用、修复后重新 add、push 失败重试 pull |

## 上下文隔离（重要）

本 skill 涉及的 git 操作对准确性要求极高——提交信息必须反映实际变更、审查结论必须对应当前 diff。而会话中积累的上下文（读过的文件内容、讨论过的提交意图、之前的审查结论）随时可能过时，依赖它们会导致提交信息与实际变更不符、审查结论错位、甚至提交错误的代码。因此必须隔离之前的上下文。

### 双轨隔离机制

**SOLO 模式（SubAgent 真隔离）**：当本 skill 由 Plan 主智能体调度给专门 SubAgent 执行时，SubAgent 天然拥有独立上下文——不会继承主会话的对话历史，只看到 Plan 分配的任务描述和自身执行过程中产生的信息。这是最彻底的隔离，无需额外处理。

**IDE 模式（提示词隔离兜底）**：当本 skill 在 IDE 模式的 chat agent 中触发时，没有子进程级隔离能力，会话历史完整可见。此时通过以下规则实现提示词层面的隔离——将 git 仓库实际状态确立为唯一可信的事实来源，明确要求忽略可能过时的会话记忆。

### IDE 模式下的隔离规则

**唯一事实来源**：`git status` / `git diff` 的实际输出。所有判断——是否有变更、变更了什么、提交信息怎么写、是否需要审查——都必须基于 Step 1 中刚执行的 git 命令输出，而非会话记忆。

**必须忽略的会话记忆**：
- 之前读过的文件内容（文件可能已变化，以当前 diff 为准）
- 之前讨论过的"这次提交要写什么 message"（必须基于当前实际 diff 重新生成）
- 之前运行 code-review 的结论（diff 变了旧结论就失效；只有当 `.trae/review-passed` 标记的 diffHash 与当前 diff 匹配时才有效）
- 之前对"哪些文件改了"的描述（一律以 `git status` 实际输出为准）

**唯一允许引用会话上下文的情况**：用户在本 skill 触发**之后**明确引用了之前的某段对话（例如"就用我刚才说的那个提交信息"）。即便如此，仍需先用 Step 1 的 git 命令确认实际状态，再结合用户引用的内容生成提交信息，并请用户最终确认。

## 终端兼容性（重要）

在 IDEA / PowerShell 集成终端下执行 git 命令时，必须遵守以下规则，否则命令会卡死无响应：

1. **所有只读 git 命令（status / diff / log / show）必须加 `--no-pager`**：
   - 本机未配置 `core.pager` / `GIT_PAGER` / `PAGER`，Git for Windows 会回落到默认 pager `less`
   - IDEA 集成终端是真 TTY，git 检测到后会启动 `less` 分页器；agent 无法向 `less` 发送按键，`less` 会一直阻塞等待输入 → 命令假死无反馈
   - ❌ 错误：`git diff` / `git status` / `git diff --cached`  → 卡死
   - ✅ 正确：`git --no-pager diff` / `git --no-pager status` / `git --no-pager diff --cached`
2. **同时禁用颜色输出**（`-c color.ui=false`），避免 ANSI 转义码污染 agent 对 diff 的解析：
   - ✅ 推荐写法：`git --no-pager -c color.ui=false status`
3. IDEA PowerShell 终端会在每条命令输出首部混入控制序列（如 `[?1004h`、`[?9001h`、窗口标题 OSC 序列 `]0;...`），属终端正常现象，解析时忽略首部乱码、从真正的 git 输出开始读即可。
4. `git commit` / `git add` / `git push` / `git pull` / `git rebase` 不会触发分页器，无需加 `--no-pager`。
5. 若用 RunCommand 执行长输出命令，优先加 `--stat` 或 `--name-only` 截断，避免一次性吞入超大 diff。

## 触发条件

当用户提到以下关键词时激活：
- "提交代码" / "commit" / "提交" / "push" / "推送" / "推代码"
- "git commit" / "git push" / "提交并推送"

## 流程

### Step 1: 检查工作区状态（重建事实基线）

本步骤是上下文隔离的执行点：以下命令的输出是后续所有判断的唯一依据，**不得**用会话中此前对文件/变更的记忆来替代或"补充"这些输出。

1. 执行 `git --no-pager -c color.ui=false status` 查看变更文件——以实际输出为准，忽略会话中此前对"改了哪些文件"的任何描述
2. 执行 `git --no-pager -c color.ui=false diff` 查看未暂存的变更
3. 执行 `git --no-pager -c color.ui=false diff --cached` 查看已暂存的变更
4. 向用户展示变更摘要（文件列表 + 变更行数）

如果没有变更：
- 告知用户"无变更需要提交"，流程结束

### Step 2: 暂存变更（按暂存区状态分流）

根据 Step 1 中 `git status` 与 `git diff --cached` 的实际输出分三种情况处理：

1. **暂存区已有内容**（`git diff --cached` 非空）：
   - **直接使用当前暂存区内容**，不再执行 `git add`，无需询问
   - 注意：工作区可能还有未暂存的修改，这些修改不纳入本次提交（保持用户已有的暂存意图）
2. **暂存区为空，但有工作区修改**（`git diff --cached` 空，`git diff` 非空）：
   - **【确认点】询问用户要暂存哪些文件**
   - 提供选项：全部暂存（`git add -A`）/ 指定文件暂存
   - 用户选择指定文件时，按用户给出的文件列表执行 `git add`
3. **暂存区为空且工作区也无修改**：流程已在 Step 1 结束（无变更），不会进入本步骤

### Step 3: AI Code Review（前置审查）

在确认提交信息和 commit 之前完成审查。这样做的关键好处：审查可能触发修复，修复会改变暂存区 diff，而 commit message 必须反映最终提交内容——因此审查在前、message 在后，避免确认了一个随后就失效的 message。pre-commit hook 仍作为兜底门禁保留（防绕过）。

1. 先检查 `.trae/review-passed` 标记是否匹配当前暂存区 diff：
   - 如果存在且 diffHash 匹配当前暂存区 diff → 审查已通过，跳过审查，直接进入 Step 4
   - 如果不存在或不匹配 → 继续下方审查流程
2. 执行 `git --no-pager -c color.ui=false diff --cached` 获取暂存区变更
3. 调用 `code-review` Skill 对暂存区变更进行全维度审查
4. 展示审查结果（问题 + 修复建议）
5. **【确认点】询问用户：修复问题 / 跳过审查直接提交**
6. 如果用户选择修复：
   - 展示具体修复方案（要改哪个文件、改什么内容）
   - **【确认点】用户确认修复方案 = 授权修改代码 + 重新暂存**（作为一次决策单元，不再分别询问；commit 在 Step 5 统一执行）
   - 用户确认后：修改代码 → 自动执行 `git add` 重新暂存（已授权延续）
   - 修复完成后：执行 `pnpm review:pass` 创建 `.trae/review-passed` 标记（脚本自动计算当前 diff 的 hash 并写入）
7. 如果用户选择跳过审查：
   - 执行 `pnpm review:pass` 创建 `.trae/review-passed` 标记（表示用户主动决定不审查，让 hook 放行）
8. 如果审查通过（LGTM，无问题）：
   - 执行 `pnpm review:pass` 创建 `.trae/review-passed` 标记
9. 标记创建完成后，进入 Step 4 确认提交信息

### Step 4: 确认提交信息（确认即授权 commit）

1. 根据**当前** `git diff --cached` 的实际输出（即暂存区内容）生成建议的 commit message（遵循 `type: subject` 格式）。注意：若 Step 3 发生过修复，暂存区 diff 已变化，必须基于修复后的最终 diff 生成 message，而非 Step 1 时的原始 diff。**提交信息必须反映当前实际变更**——即使会话中此前讨论过提交意图，也以当前 diff 为准重新生成，除非用户在本 skill 触发后明确要求沿用之前的说法
2. **一次性向用户展示**：变更摘要 + 建议 message + 即将执行 `git commit -m "<message>"`
3. **用户确认 message 即等于授权执行 commit**，无需再单独询问"是否执行 commit"
4. 用户确认后，直接进入 Step 5 执行 commit

提交信息格式参考：
- `feat: 新增用户注册功能`
- `fix: 修复登录页面样式错乱`
- `refactor: 重构订单服务事务处理`
- `perf: 优化列表查询分页性能`

### Step 5: 执行 Commit

1. 执行 `git commit -m "<用户确认的提交信息>"`（已获 Step 4 授权 + Step 3 审查通过/跳过）
2. pre-commit hook 检测 `.trae/review-passed` 标记：
   - 存在且 diffHash 匹配当前暂存区 diff → 放行提交 → 自动清理标记
   - 不匹配（异常情况，如标记失效或暂存区在审查后被修改）→ 阻断提交
3. 如果 hook 放行：commit 成功，进入 Step 6
4. 如果 hook 阻断（异常兜底）：
   - 告知用户"提交被拦截，可能是因为审查标记失效或暂存区在审查后被修改"
   - 提示用户重新运行流程或手动检查
   - 流程结束（不自动重试，避免循环）

### Step 6: 推送确认（pull + push 合并授权）

1. 提交成功后，**【确认点】询问用户是否推送到远程**，一次性告知即将执行：`git pull --rebase` → `git push`
2. 如果用户不推送，流程结束
3. 用户确认推送 = 授权整个 pull + push 流程，无需再分别询问 pull 和 push
4. 执行 `git pull --rebase`
5. 检查是否有冲突：
   - 执行 `git --no-pager -c color.ui=false status` 检查是否处于 rebase 冲突状态
   - 如果有冲突：**停止推送流程**，告知用户需要手动解决冲突：
     ```
     ⚠️ 检测到代码冲突，请手动解决：
     1. 查看冲突文件列表
     2. 手动编辑解决冲突
     3. 执行 git add <冲突文件>
     4. 执行 git rebase --continue
     5. 解决完成后再次要求推送
     ```
     展示冲突文件列表，流程结束
   - 如果无冲突：继续
6. 执行 `git push`（已获 Step 6 授权，无需再次询问）
7. 如果 push 失败（远程有新提交）：
   - **自动再次执行 `git pull --rebase`**（已授权推送流程的延续，无需再次询问）
   - 重复冲突检查逻辑；若仍无冲突则自动重新 `git push`
8. push 成功后，告知用户推送完成

### Step 7: 清理

1. 执行 `pnpm review:clean` 清理残留的 passed 标记文件（如果存在）
2. 输出最终状态摘要

## 确认点清单（共 5 处）

整个流程保留以下 5 个用户确认点，其余操作自动执行或合并授权：

| # | 确认点 | 位置 | 触发条件 | 授权范围 |
|---|--------|------|---------|---------|
| 1 | 暂存文件选择 | Step 2-2 | 暂存区为空且有工作区修改时 | 全部暂存 / 指定文件暂存 |
| 2 | 审查结果处置选择 | Step 3-5 | AI 审查完成后（commit 前） | 修复问题 / 跳过审查直接提交 |
| 3 | 修复方案确认 | Step 3-6 | 用户选择「修复问题」时 | 授权修改代码 + 重新暂存（commit 在 Step 5 统一执行） |
| 4 | commit message 确认 | Step 4 | 审查/修复完成后（基于最终 diff 生成） | 确认 message = 授权执行 commit |
| 5 | 推送确认 | Step 6 | commit 成功后 | 授权 pull --rebase + push（含失败重试） |

**不触发确认的情况**：暂存区已有内容时（Step 2-1），直接使用暂存区，无需询问。

**强制停止点**（非确认）：代码冲突必须停止，提示手动处理；无变更直接结束。

## 约束

1. **代码修改必须展示修改内容并等待用户确认**，绝不擅自修改
2. commit message **必须**由用户确认，不能自动决定
3. **仅以下 5 处需用户确认**：暂存文件选择（Step 2-2，仅暂存区为空且有修改时触发）、审查处置选择（Step 3-5）、修复方案（Step 3-6）、commit message（Step 4，审查/修复后基于最终 diff 生成）、推送（Step 6）。其余 git 操作（暂存区已有内容时直接使用、pull、push 失败重试等）在已获授权后自动执行，无需重复询问
4. 遇到代码冲突**必须停止**，不能自动解决，提示用户手动处理
5. AI Code Review 发现问题时，由用户决定修复还是标记通过
6. 如果用户选择修复，展示具体修复方案，用户确认后才改代码（确认即授权修改+重新暂存；commit 在 Step 5 统一执行）
7. 审查通过或用户跳过审查后，执行 `pnpm review:pass` 创建 `.trae/review-passed` 标记（脚本自动计算 diffHash），让 hook 放行随后在 Step 5 执行的 commit
8. 不执行 `git push --force`，除非用户明确要求
9. **上下文隔离**：所有判断必须基于 Step 1 中 git 命令的实际输出，不得用会话中此前的记忆替代——包括文件内容、变更范围、提交意图、审查结论
10. **SOLO 模式下**：本 skill 适合由 Plan 主智能体调度给专门 SubAgent 执行，SubAgent 天然独立上下文，主 Agent 等待结果汇总即可