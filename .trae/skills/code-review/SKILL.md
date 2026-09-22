---
name: code-review
description: "pre-commit 审查拦截后由 git-commit-push 调用进行深度语义审查，或用户主动要求审查代码、Review PR、检查变更时触发。"
alwaysApply: false
---

# Code Review Skill

基于项目 `.trae/rules`（项目特有约定）和 `.trae/skills/*-guidelines`（详细规范 references）中定义的团队规范，对代码变更进行全维度 AI 审查。

**本 Skill 直接使用 Trae 内置大模型（豆包/DeepSeek），无需配置任何 API Key。**

**本 Skill 承担全部审查逻辑**——Git Hook 脚本只负责阻断/放行提交，所有安全/Bug/性能/规范检测均由本 Skill 完成。

**本 Skill 由 git-commit-push Skill 编排调用**，也可手动触发独立使用。

## 触发机制

**被 git-commit-push 调用（核心路径）**：
1. 用户说"提交代码" → git-commit-push Skill 激活
2. git commit → pre-commit hook 阻断提交
3. git-commit-push 执行 `git diff --cached` 获取变更 → 调用本 Skill 进行审查
4. 本 Skill 输出审查结果 → git-commit-push 根据结果继续流程

**手动触发**：用户在对话中提到"审查代码"、"review code"、"检查变更"等关键词时激活

## 审查流程

### Step 1: 读取变更

1. 如果由 git-commit-push 调用（pre-commit 被阻断场景），执行 `git --no-pager -c color.ui=false diff --cached` 获取暂存区变更
2. 如果是手动触发，读取用户在对话中提供的代码或当前打开的文件

### Step 2: 规则匹配

根据文件路径和语言，加载 `.trae/rules` 项目特有约定（常驻）+ 对应 skill 的 references（按需）：

**常驻 rules（始终加载，项目特有约定）**:
- `.trae/rules/frontend.md` — 4 个前端项目的差异化约定（含 §13 Vue 编码项目差异点）
- `.trae/rules/backend.md` — 2 个后端项目的差异化约定

**按需加载 skill references（根据文件路径和任务类型）**:

| 文件路径模式 | 加载的 skill references |
|---|---|
| `**/controller/*.java` 或 `**/*Controller.java` | `backend-guidelines` 的 `api-design.md` + `security.md` |
| `**/service/*.java` 或 `**/*Service.java` | `backend-guidelines` 的 `business-rules.md` + `exception-logging.md` |
| `**/mapper/*.xml` 或 `**/*Mapper.java` | `backend-guidelines` 的 `database.md` |
| `**/config/*.java` 或 `**/*Config.java` | `backend-guidelines` 的 `microservice.md` + `security.md` |
| `**/*.java` (通用) | `backend-guidelines` 的 `java.md` + `error-code.md` |
| `**/*.vue` | `frontend-guidelines` 的 `lint-config.md`（Vue 编码核心规则已在 `.trae/rules/frontend.md` §13 始终加载） |
| `**/*.ts` / `**/*.tsx` | `frontend-guidelines` 的 `lint-config.md`（Vue 编码核心规则已在 `.trae/rules/frontend.md` §13 始终加载） |
| `**/*.less` / `**/*.scss` / `**/*.css` | `frontend-guidelines` 的 `lint-config.md` |
| 前端安全审查 | `frontend-guidelines` 的 `security.md` + `performance.md` + `business-rules.md` |
| 后端安全审查 | `backend-guidelines` 的 `security.md` + `exception-logging.md` |

> Vue 通用知识（响应式、组件设计、性能优化技巧）由 `frontend-guidelines` 技能的 `vue/` 子目录承载，Vue 任务时一并加载。

### Step 3: 多维度审查

#### 维度 1: 安全（权重 30%）

逐一检查：
- SQL 注入（字符串拼接 SQL、MyBatis ${} 使用）
- 命令注入（Runtime.exec / ProcessBuilder 未校验输入）
- 硬编码密钥或密码
- 不安全的加密（MD5、DES）
- XSS（v-html、未转义用户输入）
- 权限验证缺失（缺少 @PreAuthorize / @PermitAll）
- 敏感数据明文日志（手机号、身份证未脱敏）
- 越权访问（未校验数据所属用户/部门）
- SSRF（不受控的 URL 请求）
- 不安全的反序列化
- localStorage 存 Token

#### 维度 2: Bug 风险（权重 30%）

- 空指针异常（NPE）— 链式调用未判空、集合取值未判空
- 资源泄露 — 流未关闭、连接未释放
- 并发问题 — 线程安全、竞态条件
- 边界条件遗漏 — 空列表、0 值、负数、超长字符串
- 幂等性缺失 — 支付/订单/库存操作未做幂等控制
- 状态机流转错误 — 非法状态转换
- 事务边界错误 — @Transactional 缺失或范围不当
- 集合操作错误 — ConcurrentModificationException

#### 维度 3: 性能（权重 20%）

- N+1 查询 — 循环内单条查询
- 深分页 — OFFSET 过大未用游标
- 循环内数据库调用
- SELECT * 全量查询
- 缓存未失效 — 数据变更后未主动失效缓存
- 大事务 — 事务内包含 RPC 调用或耗时操作
- 前端 — 不必要的重渲染、大列表未虚拟化

#### 维度 4: 规范与可维护性（权重 20%）

- 命名不符合 camelCase / 业务语义
- 魔法值未用枚举
- 缺少 @Transactional
- 日志规范 — 级别不当、未脱敏、异常日志缺堆栈
- 接口文档缺失 — 缺少 @Operation 注解
- 错误码不规范 — 硬编码错误码
- System.out.println / e.printStackTrace
- Vue 规范 — Composition API + script setup

### Step 4: 输出审查报告

对每个发现的问题，输出：

```
🔴/🟠/🟡/🔵/⚪ [维度] 问题标题
📄 文件: src/main/java/com/example/OrderService.java:42
💬 问题描述: 此处直接拼接 SQL 字符串，存在 SQL 注入风险
💡 修复建议: 使用 MyBatis-Plus 的 QueryWrapper 或参数化查询
📖 规范: backend-guidelines/references/security.md — SQL 注入防护
```

严重程度分级：
- 🔴 Critical: 必须修复的安全漏洞 / 数据丢失风险
- 🟠 High: 高概率 Bug / 安全弱点
- 🟡 Medium: 潜在问题 / 性能风险
- 🔵 Low: 改进建议
- ⚪ Info: 参考信息

如果代码没有问题：

> ✅ LGTM，未发现问题。代码质量良好！

### Step 5: 返回审查结果

- 如果由 git-commit-push 调用：返回审查结果给 git-commit-push 处理后续流程（审查通过后由 git-commit-push 执行 `pnpm review:pass` 创建 review-passed 标记）
- 如果独立使用：告知用户审查已完成，可通过 git-commit-push Skill 提交代码

## 约束

1. **只指出真正的问题**，不要为了审查而审查
2. 代码没问题就直接说 LGTM
3. 每个问题**必须**给出具体行号和修复建议
4. 不报告 linter 已能检测的纯格式问题（缩进、分号等）
5. 修复建议要具体到代码级别，不要笼统的方向
6. 引用 `.trae/rules/*.md` 或 `.trae/skills/*-guidelines/references/*.md` 中的规范文件名，让开发者知道依据