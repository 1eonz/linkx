# AI Code Review

基于 Trae IDE 的 AI 代码审查系统。零 API Key、零外部依赖，pre-commit 自动拦截，git-commit-push Skill 编排全流程，Trae 内置模型执行审查。

## 快速开始

### 初始化（首次）

```bash
cd <项目根目录>
pnpm install
pnpm prepare    # 初始化 Husky hooks
```

### 日常使用

**在 Trae 对话框中输入「提交代码」**，系统自动编排全流程：

1. 检查工作区状态 → 暂存变更
2. **前置审查**（commit 之前）：调用 code-review Skill 全维度审查
3. 审查通过 → 创建 review-passed 标记 → 确认 commit message → 提交
4. 审查有问题 → 展示修复方案 → 确认后修复 → 重新暂存 → 重新审查
5. 询问是否 push → pull --rebase → push

也可以单独审查代码：在对话框输入「审查代码」或「review code」。

### 手动命令

```bash
pnpm review            # 触发暂存区变更审查（pre-commit 同款）
pnpm review:branch     # 审查与 main 分支的差异
pnpm review:no-block   # 仅收集不阻断
pnpm review:pass       # 创建审查通过标记（审查通过后由 Skill 调用）
pnpm review:clean      # 清理残留的 passed 标记文件
```

### 跳过审查

紧急情况：`git commit --no-verify -m "your message"`

直接在控制台 `git commit` 会被拦截，提示到 Trae 对话框操作。跳过的提交仍会在 CI 流水线中被 SonarQube/Semgrep 二次检查。

---

## 工作流程

```
在 Trae 中说「提交代码」
       ↓
git-commit-push Skill 激活
       ↓
Step 1: git status / git diff 重建事实基线（忽略会话记忆）
       ↓
Step 2: 暂存变更（暂存区已有内容则直接用；否则询问暂存范围）
       ↓
Step 3: AI Code Review（前置审查，commit 之前完成）
       ↓
┌── .trae/review-passed 标记存在且 diffHash 匹配 → 跳过审查
└── 无标记/哈希不匹配 → git diff --cached → 调用 code-review Skill 全维度审查
       ↓
┌── 有问题 → 展示修复方案 → 用户确认 → 修改代码 → git add 重新暂存
│       ↓
│   pnpm review:pass 创建 .trae/review-passed 标记（脚本计算 diffHash）
│
├── LGTM → pnpm review:pass 创建 .trae/review-passed 标记
│
└── 用户主动跳过 → pnpm review:pass 创建 .trae/review-passed 标记
       ↓
Step 4: 基于最终 diff 生成 commit message → 用户确认
       ↓
Step 5: git commit → pre-commit hook 校验 review-passed 哈希
       ↓
┌── 哈希一致 → 放行提交 → 自动清理标记
└── 哈希不匹配（异常兜底）→ 阻断提交，提示重新审查
       ↓
Step 6: 询问是否 push → git pull --rebase → 检查冲突 → git push
       ↓
Step 7: pnpm review:clean 清理残留标记
```

**关键设计**：审查前置（commit 之前完成）而非 hook 阻断后审查——审查可能触发修复改变暂存区 diff，因此先审查、后确认 message，确保 commit message 反映最终提交内容。pre-commit hook 仅作为兜底门禁保留，防止通过控制台直接 `git commit` 绕过审查。

## 架构

| 组件 | 职责 | 模型 |
|------|------|------|
| `.husky/pre-commit` | 兜底门禁：触发脚本、阻断/放行提交（防绕过） | 无（Git Hook） |
| `scripts/ai-code-review.mjs` | 校验哈希标记、阻断/放行提交、创建通过标记 | 无（纯脚本） |
| `.trae/skills/git-commit-push/SKILL.md` | 编排全流程：status → 暂存 → 前置审查 → 修复 → commit → push | Trae 内置模型 |
| `.trae/skills/code-review/SKILL.md` | 全维度审查：安全、Bug、性能、规范 | Trae 内置模型 |
| `.trae/rules/` | 常驻项目约定（审查依据之一） | — |
| `.trae/skills/*-guidelines/references/` | 按需加载的详细规范（审查依据之二） | — |

## 审查维度

| 维度 | 权重 | 覆盖项 |
|------|------|--------|
| 安全 | 30% | SQL 注入、命令注入、硬编码密钥、XSS、越权访问、敏感数据明文日志、不安全加密 |
| Bug 风险 | 30% | NPE、资源泄露、并发竞态、幂等性缺失、状态机错误、事务边界、集合操作异常 |
| 性能 | 20% | N+1 查询、深分页、循环内 DB 调用、SELECT *、缓存未失效、大事务 |
| 规范 | 20% | 魔法值、硬编码错误码、缺少 @Transactional/@Operation/@PreAuthorize、日志规范、Vue 规范 |

## 审查依据

审查依据分两层：

- **常驻 rules**（始终加载，项目特有约定）：`.trae/rules/frontend.md`（含 §13 Vue 编码项目差异点）+ `.trae/rules/backend.md`
- **按需 references**（按文件类型匹配）：`.trae/skills/*-guidelines/references/*.md`，详见下表

| 文件类型 | 加载的 references |
|----------|-------------------|
| Controller | `backend-guidelines` 的 `api-design.md` + `security.md` |
| Service | `backend-guidelines` 的 `business-rules.md` + `exception-logging.md` |
| Mapper | `backend-guidelines` 的 `database.md` |
| Config | `backend-guidelines` 的 `microservice.md` + `security.md` |
| Java 通用 | `backend-guidelines` 的 `java.md` + `error-code.md` |
| Vue / TS / TSX | `frontend-guidelines` 的 `lint-config.md` |
| CSS/Less/SCSS | `frontend-guidelines` 的 `lint-config.md` |
| 前端安全审查 | `frontend-guidelines` 的 `security.md` + `performance.md` + `business-rules.md` |
| 后端安全审查 | `backend-guidelines` 的 `security.md` + `exception-logging.md` |

> Vue/TS/TSX 文件的 Vue 编码核心规则已在常驻 `.trae/rules/frontend.md` §13 加载，按需仅补充 `lint-config.md`。Vue 通用知识（响应式、组件设计、性能优化技巧）由 `frontend-guidelines` 技能的 `vue/` 子目录按需加载。

## 审查通过标记机制

git-commit-push Skill 在 Step 3 完成前置审查后（无论 LGTM、用户修复后、还是用户主动跳过），执行 `pnpm review:pass`（即 `node scripts/ai-code-review.mjs --mark-passed`），由脚本计算当前暂存区 diff 的 SHA-256 哈希（截断 16 位）并写入 `.trae/review-passed` 标记文件。随后 Step 5 执行 `git commit` 时，pre-commit hook 重新计算当前 diff 哈希并与标记中的哈希比对：

```
git-commit-push Skill 前置审查（Step 3）
       ↓
审查通过 / 用户跳过 → pnpm review:pass → 脚本计算 diffHash → 写入 .trae/review-passed（防篡改）
       ↓
git commit（Step 5）→ hook 计算当前 diff 哈希 → 与标记中 diffHash 比对
       ↓
┌── 哈希一致 → 放行提交 → 自动清理标记
└── 哈希不一致（暂存区在审查后被修改）→ 阻断提交（异常兜底）→ 提示重新审查
```

哈希计算统一由 `scripts/ai-code-review.mjs` 完成（创建标记和校验标记同一套逻辑），避免 Skill 侧与 Hook 侧算法不一致的风险。这防止了手动创建标记文件绕过审查，或审查后修改代码导致标记失效的问题。

## 问题输出格式

```
🔴/🟠/🟡/🔵/⚪ [维度] 问题标题
📄 文件: src/main/java/com/example/OrderService.java:42
💬 问题描述: 此处直接拼接 SQL 字符串，存在 SQL 注入风险
💡 修复建议: 使用 MyBatis-Plus 的 QueryWrapper 或参数化查询
📖 规范: backend/security.md — SQL 注入防护
```

严重程度分级：
- 🔴 Critical: 必须修复的安全漏洞 / 数据丢失风险
- 🟠 High: 高概率 Bug / 安全弱点
- 🟡 Medium: 潜在问题 / 性能风险
- 🔵 Low: 改进建议
- ⚪ Info: 参考信息

## 文件结构

```
.husky/pre-commit                          # Git Hook，兜底门禁（防绕过）
scripts/ai-code-review.mjs                 # 校验哈希标记 → 阻断/放行 → --mark-passed 创建通过标记
.trae/review-passed                        # 审查通过标记（gitignore，运行时生成，含 diffHash，SHA-256 截断 16 位）
.trae/skills/git-commit-push/SKILL.md      # 提交推送编排 Skill（前置审查流程）
.trae/skills/code-review/SKILL.md          # 代码审查 Skill
.trae/rules/                               # 常驻项目约定：frontend.md / backend.md（审查依据之一）
.trae/skills/frontend-guidelines/references/   # 前端按需规范：lint-config / security / performance / business-rules
.trae/skills/backend-guidelines/references/    # 后端按需规范：api-design / database / java / error-code / microservice / security / ...
.trae/skills/frontend-guidelines/references/vue/   # Vue 通用知识：响应式 / 组件设计 / 性能优化技巧
package.json                               # review / review:branch / review:no-block / review:pass / review:clean
```
