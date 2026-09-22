---
alwaysApply: true
---
# Linkx 项目规范索引

> **规则级别说明**: 🔴 强制（必须遵循，违反即 bug） / 🟡 推荐（应遵循，特殊情况可偏离） / 🔵 参考（指导性建议）
>
> **本地规则说明**: 本文件已在 `.gitignore` 中声明，作为本地规则不提交 Git，仅在本地生效。如需团队共享规则，请提交 `.trae/rules/project-rules.md`（带连字符）。

## 一、项目概览

| 类型 | 项目 | 路径 | 技术栈 |
|------|------|------|--------|
| 前端 | H5Portal | `H5Portal/` | Vue3+Vite7+Vant+SCSS |
| 前端 | web | `web/` | Vue3+Vite5+ElementPlus+Less |
| 前端 | admin | `cloudcmd-admin-web/` | Vue2+Webpack+ElementUI+SCSS |
| 后端 | agent/server | `agent/server/` | Java17+SpringBoot3.4+Nacos+MyBatis-Plus |
| 后端 | ccmd | `ccmd/` | Java11+SpringBoot2.7+Dubbo3.2+MyBatis-Plus |
| 运维 | agent/ci | `agent/ci/` | Python3.12+Poetry+Ansible |

## 二、本地命令环境规则（Windows / PowerShell）🔴

> 本规则旨在避免在 PowerShell 环境中反复尝试找不到 `node` / `npm` / `pnpm` 等命令。

### 2.1 Node.js 可执行文件路径

`node` 不在默认 PATH 中，必须使用**全路径**调用。已知可用路径（按优先级）：

```powershell
# 推荐：nvm4w 管理的当前版本
$nodeExe = "C:\nvm4w\node-v22.17.1-win-x64\node.exe"

# 备选 1：nvm4w symlink
$nodeExe = "C:\nvm4w\v22.17.1\node.exe"

# 备选 2：Program Files
$nodeExe = "C:\Program Files\nodejs\node.exe"

# 备选 3：Trae 内置 SDK（仅 Trae 内部 skill 脚本可用）
$nodeExe = "C:\Users\9951257\.trae-cn-enterprise\sdks\workspaces\1dce0f22\versions\node\current\node.exe"
```

### 2.2 调用方式规则 🔴

| 场景 | 正确调用方式 |
|------|--------------|
| 执行 skill 脚本 | `& "C:\nvm4w\node-v22.17.1-win-x64\node.exe" "C:\Users\9951257\.trae-cn-enterprise\skills\<skill>\scripts\<script>.mjs" --target <项目>` |
| 执行项目 npm/pnpm 命令 | **不要直接用 `npm run` / `pnpm run`**，改为 `<nodeExe> node_modules\<pkg>\bin\<cmd>` 或切换 PATH 后调用 |
| 切换 PATH 后调用 | `$env:Path = "C:\nvm4w\node-v22.17.1-win-x64;$env:Path"; npm run lint` |

### 2.3 强制规则 🔴

- **禁止直接调用 `node`、`npm`、`pnpm`**（会报"无法将 node 项识别为 cmdlet"）
- **必须使用 `& <全路径>` 调用**，或先 `$env:Path = "<nodeDir>;$env:Path"` 再调用
- **首次尝试前先验证路径存在**：`Test-Path "C:\nvm4w\node-v22.17.1-win-x64\node.exe"`
- **若首选路径失效，按 2.1 顺序尝试备选路径**，禁止直接放弃
- **避免重复尝试**：一旦确定可用路径，本对话内复用，不要再次 `where.exe node` 探测

### 2.4 impeccable skill 脚本调用模板

```powershell
# skill 根目录
$skillDir = "C:\Users\9951257\.trae-cn-enterprise\skills\impeccable"
$nodeExe = "C:\nvm4w\node-v22.17.1-win-x64\node.exe"

# 1. 加载项目上下文（首次或新会话）
& $nodeExe "$skillDir\scripts\context.mjs" --target <项目路径>

# 2. 智能推荐（不带参数）
& $nodeExe "$skillDir\scripts\context-signals.mjs"

# 3. 检测器（仅 web 项目）
& $nodeExe "$skillDir\scripts\detect.mjs" --json <扫描目标>

# 4. live 模式（需 dev server 运行）
& $nodeExe "$skillDir\scripts\live.mjs"
```

### 2.5 版本要求

- **Node.js**: v22.17.1+（项目 `package.json` 要求 `>=22.11.0`）
- **包管理器**: pnpm（项目 `preinstall` 脚本强制 `npx only-allow pnpm`）

## 三、前端设计 Skill 运用规则

> 本节定义 `frontend-design` 与 `impeccable` 两个全局 skill 在 Linkx 前端项目中的运用规范，支持**主动触发**与**非主动触发**两种模式。

### 3.1 Skill 角色定位

| Skill | 定位 | 何时使用 |
|-------|------|----------|
| `frontend-design` | **设计方向探索器** —— 给美学方向、视觉风格、设计灵感 | 视觉方向未定、需要"灵感/方向感"时 |
| `impeccable` | **工程化设计系统** —— 23 个子命令覆盖从构思到生产全流程 | 方向已定、要落地为生产级代码、或评估/打磨/审计已有页面 |

**核心原则**: `frontend-design` 先行定方向，`impeccable` 收尾做工程化落地。

### 3.2 自动触发策略 🔴

**默认开启自动触发**，AI 检测到以下意图时主动调用 skill，无需用户显式要求。

#### 3.2.1 触发条件矩阵

| 用户意图关键词 | 触发的 Skill | 推荐子命令 |
|----------------|-------------|-----------|
| "设计/做一个新页面"、"新建登录页/落地页/Dashboard" | `frontend-design` → `impeccable` | design 探索 → `shape` → `craft` |
| "重构样式"、"重做视觉"、"重新设计这个页面" | `frontend-design` → `impeccable` | design 探索 → `bolder`/`quieter`/`craft` |
| "页面太丑/平淡/没设计感" | `impeccable` | `bolder <file>` |
| "颜色单调/没层次" | `impeccable` | `colorize <file>` |
| "间距乱/对不齐/视觉层级差" | `impeccable` | `layout <file>` |
| "字体难看/排版差" | `impeccable` | `typeset <file>` |
| "错误提示文案差/按钮文案差" | `impeccable` | `clarify <file>` |
| "页面动起来/加动效" | `impeccable` | `animate <file>` |
| "适配移动端/响应式有问题" | `impeccable` | `adapt <file>` |
| "页面卡顿/性能差" | `impeccable` | `optimize <file>` |
| "加空状态/首启/onboarding" | `impeccable` | `onboard <file>` |
| "发布前过一遍/上线前检查" | `impeccable` | `polish <file>` + `audit <file>` |
| "精简一下/去掉复杂度" | `impeccable` | `distill <file>` |
| "让设计更有个性/记忆点" | `impeccable` | `delight <file>` |
| "不知道怎么改，给建议" | `impeccable` | 不带参数，让其智能推荐 |
| "浏览器里实时改" | `impeccable` | `live`（需 dev server 运行） |

#### 3.2.2 不触发场景 🔴

以下场景**禁止**触发 skill，避免过度工程化：

- 修复 bug（样式 bug 除外，如视觉问题）
- 新增/修改字段、调整 API 调用
- 调整业务逻辑、状态管理
- 纯文案修改（非 UX 文案）
- 类型定义、工具函数修改
- 简单的 CRUD 后台页面（admin 项目中无视觉追求的列表/表单）

#### 3.2.3 触发优先级

当两个 skill 都可能适用时，按以下顺序决策：

1. **视觉方向未定** → 先 `frontend-design` 探索，再 `impeccable` 落地
2. **视觉方向已定** → 直接 `impeccable` 对应子命令
3. **不确定** → 先 `frontend-design` 给 2-3 个方向选项，用户选定后 `impeccable`

### 3.3 项目上下文管理 🔴

#### 3.3.1 上下文独立原则 + docs-local 统一存放

每个前端项目**独立维护**自己的 PRODUCT.md / DESIGN.md / design.json / live 配置，互不干扰。**所有由 skill 生成的文档与配置统一存放于仓库根的 `docs-local/` 目录下**（已在 `.gitignore` 中，不提交 Git，仅本地生效），按项目名分子目录管理：

| 项目 | 上下文目录（docs-local 下） | PRODUCT.md | DESIGN.md | sidecar 与 live 配置 | Register | 平台 |
|------|----------------------------|------------|-----------|----------------------|----------|------|
| `web/` | `docs-local/web/` | `docs-local/web/PRODUCT.md` | `docs-local/web/DESIGN.md` | `docs-local/web/.impeccable/design.json` + `docs-local/web/.impeccable/live/config.json` | `product` | `web` |
| `H5Portal/` | `docs-local/H5Portal/` | `docs-local/H5Portal/PRODUCT.md` | `docs-local/H5Portal/DESIGN.md` | `docs-local/H5Portal/.impeccable/...` | `product`（功能页）/ `brand`（登录/落地页） | `web` |
| `admin/` | `docs-local/admin/` | `docs-local/admin/PRODUCT.md` | `docs-local/admin/DESIGN.md` | `docs-local/admin/.impeccable/...` | `product` | `web` |
| `agent/web/` | `docs-local/agent-web/` | `docs-local/agent-web/PRODUCT.md` | `docs-local/agent-web/DESIGN.md` | `docs-local/agent-web/.impeccable/...` | `product` | `web` |

> `docs-local/` 目录已在 `.gitignore` 中声明，自动被 Git 忽略；生成文件不会污染提交。

#### 3.3.2 skill 生成物的默认输出路径规则 🔴

调用 `impeccable` 任何子命令时，**必须**把生成的 `.md` 文件、`design.json`、`.impeccable/` 配置写入对应的 `docs-local/<项目>/` 子目录，**禁止**写入项目源码目录（如 `web/PRODUCT.md`），避免：
- 污染 Git 提交
- 与源码混淆
- 在不同开发机器上产生冲突

具体路径表见 3.3.1。

#### 3.3.3 首次使用前必做

**每个前端项目首次使用 `impeccable` 前，必须先执行 `/impeccable init --target <项目路径>`** 建立上下文，并将生成的文件写入 `docs-local/<项目>/` 下。后续所有命令复用该上下文。

判断"是否首次使用"的方法：
- `docs-local/<项目>/PRODUCT.md` 不存在 → 首次，先 init
- `docs-local/<项目>/PRODUCT.md` 已存在 → 非首次，直接用子命令

#### 3.3.4 上下文更新时机

PRODUCT.md / DESIGN.md 在以下情况需更新（始终写入 `docs-local/<项目>/` 下）：
- 项目技术栈重大变更（如 UI 框架升级）
- 新增设计系统 token（颜色/间距/字号体系）
- 业务定位调整（如从工具型转向品牌型）

### 3.4 样式规则的项目管理 🟡

#### 3.4.1 Token / 设计变量管理

##### 🔴 强制规则

- **禁止硬编码颜色/间距/字号/圆角值**，必须使用项目既定的 token 或 CSS 变量
- 新增设计变量必须放入项目的统一变量文件，禁止散落在组件内
- token 命名遵循语义化原则（`--color-primary` 而非 `--color-blue`）
- 颜色值统一使用 OKLCH 格式（新项目）或保持与现有 token 一致的格式（老项目）

##### 各项目 token 来源与扩展规则

| 项目 | 变量文件 | 适配方案 | 扩展规则 |
|------|----------|----------|----------|
| `web/` | `web/src/styles/themes/*.less` + `useTheme` | `postcss-pxtorem`（rootValue=16） | 直接写 px（自动转 rem）；**禁止**用 `var(--spacing-*)` 包裹 px 值（双重转换冲突）；需固定 px 用大写 `PX` |
| `agent/web/` | `agent/web/src/styles/*.less` | `postcss-pxtorem`（rootValue=16） | 同 `web/` |
| `H5Portal/` | `H5Portal/src/styles/index.scss` | **未启用** pxtorem，手动 rem/vw/百分比 | 间距/字号用 `var(--spacing-*)` 变量或 rem |
| `admin/` | `cloudcmd-admin-web/src/styles/*` | 固定 px（PC 后台） | 直接写 px；Element UI 主题变量优先 |

##### 颜色策略选择

在 `impeccable` 中确定颜色策略前，先回答一句话场景问题（谁用、在哪、什么光线、什么心情），再从四档中选择：

| 策略 | 适用 | 描述 |
|------|------|------|
| Restrained | 产品默认、品牌极简 | 中性色 + 单一强调色 ≤10% |
| Committed | 品牌身份页 | 单一饱和色占 30-60% |
| Full palette | 数据可视化、品牌活动 | 3-4 个命名角色色，每个刻意使用 |
| Drenched | 品牌首屏、活动页 | 表面即颜色 |

#### 3.4.2 禁止事项（继承自 impeccable） 🔴

以下"AI 痕迹"模式**禁止**出现在任何前端项目：

- 侧边条纹边框（`border-left/right` 大于 1px 作为彩色装饰）
- 渐变文字（`background-clip: text` + 渐变背景）
- 装饰性玻璃拟态（除非刻意且必要）
- 大数字 + 小标签 + 配套统计 + 渐变强调的"hero-metric 模板"
- 完全相同的卡片网格（图标 + 标题 + 文本无限重复）
- 每个章节上方都加"小号大写字间距大的 eyebrow"（`ABOUT` / `PROCESS` / `PRICING`）
- 默认用 `01 / 02 / 03` 编号章节（除非真是有序流程）
- 文本溢出容器（未在所有断点测试长标题）

#### 3.4.3 响应式与可访问性基线 🟡

- 正文文字对比度 ≥ 4.5:1；大字（≥18px 或 bold ≥14px）≥ 3:1
- placeholder 文字同样需 4.5:1，禁止用 muted gray 默认值
- 所有动画必须提供 `@media (prefers-reduced-motion: reduce)` 降级
- 标题用 `text-wrap: balance`，长文用 `text-wrap: pretty`
- 正文行宽限制 65-75ch
- z-index 使用语义化层级（dropdown → sticky → modal-backdrop → modal → toast → tooltip），禁止 999/9999

### 3.5 收尾验证清单 🔴

**skill 触发并完成 UI 改动后，AI 必须主动执行以下收尾动作**：

#### 3.5.1 工程规范验证

| 验证项 | 命令 / 检查内容 |
|--------|----------------|
| Lint | 按项目运行 lint（注意：参考"本地命令环境规则"使用全路径 node，禁止直接 `npm run`） |
| ESLint 规则 | 对照 `.trae/rules/frontend/eslint.md` 检查项目对应规则 |
| 业务规则 | 对照 `.trae/rules/frontend/business-rules.md` 检查防抖节流、二次确认、分页边界、表单校验等 |

#### 3.5.2 响应式与边界检查

- [ ] 移动端断点（375px / 768px）下布局正常
- [ ] 平板断点（1024px）下布局正常
- [ ] 桌面端（≥1280px）布局正常
- [ ] 长文本溢出处理（标题、用户名、长段落）
- [ ] 空状态展示（无数据、无列表、无搜索结果）
- [ ] 加载状态展示
- [ ] 错误状态展示

#### 3.5.3 变更摘要与待办建议

每次 skill 触发完成后，AI 必须输出：

1. **变更摘要**：本次改动的文件、核心变化点（视觉/交互/性能维度）
2. **规范符合性**：对照工程规范的合规情况（通过 / 偏离及原因）
3. **潜在风险**：可能影响的下游、需要回归测试的场景
4. **下一步建议**：是否需要 `polish` / `audit` / `harden` 等后续动作

### 3.6 分项目推荐工作流

> 所有生成文件均写入 `docs-local/<项目>/` 下，不污染源码目录。

#### 3.6.1 `web/` 项目（设计系统最完整，深度使用 impeccable）

```
新功能开发流程：
1. frontend-design → 视觉方向探索
2. /impeccable init --target web（首次，输出到 docs-local/web/）
3. /impeccable extract web/src/styles/themes（抽取已有 token，sidecar 写入 docs-local/web/.impeccable/）
4. /impeccable shape <file> → UX 规划
5. /impeccable craft <file> → 端到端实现
6. /impeccable polish <file> → 打磨
7. /impeccable audit <file> → 审计
```

#### 3.6.2 `H5Portal/` 项目（移动端，手动 rem 适配）

```
页面开发流程：
1. /impeccable init --target H5Portal（首次，输出到 docs-local/H5Portal/）
2. frontend-design → 移动端视觉方向
3. /impeccable craft H5Portal/src/pages/<file>
4. /impeccable adapt H5Portal/src/pages/<file> → 多设备适配
5. /impeccable audit H5Portal/src/pages/<file>
```

#### 3.6.3 `admin/` 项目（Vue2 + Element UI，按需使用）

```
仅关键页面使用：
- 复杂表单页、Dashboard、登录页 → 使用 skill
- 纯 CRUD 列表 → 直接写代码，遵循 Element UI 规范
- 使用时优先 frontend-design 给方向，impeccable polish 收尾
- 生成文件写入 docs-local/admin/ 下
```

#### 3.6.4 `agent/web/` 项目

与 `web/` 共享规范与配置，参照 3.6.1 执行；生成文件写入 `docs-local/agent-web/` 下。

### 3.7 Skill 选用决策速查表

| 场景 | frontend-design | impeccable | 推荐流程 |
|------|:---:|:---:|----------|
| 新页面，视觉方向未定 | ✅ | ✅ | design → shape → craft |
| 新页面，视觉方向已定 | ❌ | ✅ | shape → craft |
| 重构已有页面视觉 | ✅ | ✅ | design → bolder/quieter → polish |
| 微调样式（间距/颜色/字体） | ❌ | ✅ | 直接 layout/colorize/typeset |
| 修复 UX 文案 | ❌ | ✅ | clarify |
| 适配响应式 | ❌ | ✅ | adapt |
| 发布前质量过一遍 | ❌ | ✅ | polish + audit |
| 浏览器实时迭代 | ❌ | ✅ | live（需 dev server） |
| 不知道从哪下手 | ❌ | ✅ | 不带参数，让其推荐 |
| 纯 bug 修复 / 字段调整 | ❌ | ❌ | 直接改代码 |
| CRUD 后台无视觉追求 | ❌ | ❌ | 直接写代码 |

### 3.8 规则生效与维护

- 本规则为 🟡 推荐级别，特殊场景可偏离，但需说明原因
- skill 行为如有版本更新（参考 `impeccable` 的 `UPDATE_AVAILABLE` 指令），规则同步更新
- 各项目可在自己的 `PRODUCT.md` 中覆盖本节的细项配置（如 register、platform）

## 四、项目规则

### 4.1 代码注释规范 🔴

- 修改代码时**不得删除**已有的注释，包括被注释掉的代码行
- 新增的代码逻辑必须添加纯文字的中文注释，说明代码的用途和意图
- 注释应简洁明了，描述"为什么"而非"是什么"
- 保持与现有代码注释风格一致

### 4.2 接口请求规范 🔴

- 接口请求非必要时使用 `.then/.catch` 写法，避免使用 `try/catch` 包裹 `await`
- 仅在需要同时等待多个异步操作（如 `Promise.all`）或需要 `finally` 逻辑时使用 `async/await` + `try/catch`
- 示例：

```javascript
// 推荐：简单请求使用 .then
getGroupRatingStatus(groupId).then((res) => {
  if (res && res.code === 0 && res.data) {
    ratingStatus.value = res.data;
  }
}).catch((error) => {
  console.error('获取评价状态失败:', error);
});

// 不推荐：简单请求使用 try/catch
try {
  const res = await getGroupRatingStatus(groupId);
  if (res && res.code === 0 && res.data) {
    ratingStatus.value = res.data;
  }
} catch (error) {
  console.error('获取评价状态失败:', error);
}
```

### 4.3 数据驱动渲染规范 🔴

- 当模板中存在多个结构相同、仅数据不同的重复元素时，应提取为数据数组，使用 `v-for` 渲染
- 避免在模板中手写多个重复的标签，提升可维护性和可扩展性

### 4.4 web 项目实际使用路由规则 🔴

> web 项目存在两套路由文件，**只有 `index2.ts` 是实际使用的**，`index.ts` 是死代码（遗留未清理）。
>
> ⚠️ **重要事实**: `useRouterGuard` 只在死代码 `index.ts` 中被调用，`index2.ts` 从未调用 `useRouterGuard`，因此 `addRouter(router)` 动态注册逻辑从未执行，`router/modules/*.ts`（policeTask/policeAdmin/bigScreen/dutyInformation 模块路由）的 export 从未被任何地方 import 使用。

#### 4.4.1 路由文件使用规则 🔴

| 文件 | 状态 | 说明 |
|------|------|------|
| `web/src/router/index2.ts` | ✅ **实际使用** | 通过 [web/src/hooks/useCreateApp/index.ts#L8](file:///d:/work/master/Linkx/web/src/hooks/useCreateApp/index.ts#L8) 的 `import { setupRouter } from '@/router/index2'` 引入。**注意**: 仅调用 `setupRouter`，未调用 `useRouterGuard`，因此只注册了内部定义的 3 条静态路由 |
| `web/src/router/index.ts` | ❌ **死代码（未使用）** | 含 `/login`、`/iconView` 两个路由，并调用了 `useRouterGuard(router)`。由于该文件未被任何入口引入，**动态路由注册逻辑从未执行**。**禁止**在此文件新增路由 |
| `web/src/router/routerGuard.ts` | ❌ **未生效** | 定义了 `useRouterGuard` + `addRouter` 动态注册逻辑（基于 `homeRouters` + 权限过滤），但只被死代码 `index.ts` 调用。`policeTask`/`policeAdmin`/`bigScreen`/`dutyInformation` 路由实际未注册到运行时 |
| `web/src/router/modules/*.ts` | ❌ **未生效** | 5 个模块路由文件（policeTask/policeAdmin/bigScreen/dutyInformation/index）的 export 仅在 `routerGuard.ts` 的 `addRouter` 中被引用，由于 `addRouter` 未执行，这些模块的页面实际**不通过路由访问** |

#### 4.4.2 实际注册的路由清单 🔴

**基于 `index2.ts` 实际注册的路由**（运行时仅有这 3 条 + 1 条重定向）：

| path | name | 组件 | 说明 |
|------|------|------|------|
| `/` | — | redirect → `/statics` | 默认重定向 |
| `/home` | `Home` | `@/pages/home/index.vue` | 应用主入口（Home） |
| `/home/statics`（Home 子路由） | `statics` | `@/pages/statics/index.vue` | 统计页（默认进入页） |
| `/home/dutyInformation`（Home 子路由） | `dutyInformation` | `@/pages/dutyInformation/index.vue` | 值班信息页 |

> ⚠️ **其他业务页面（mission/alarm/videoControl/messages/policeAdmin/bigScreen/planSafety/leadVehicle 等）虽然代码存在，但路由未注册**。这些页面可能通过以下方式被间接使用：
> 1. 在其他页面中以组件形式 `import` 后通过 `<component :is>` 或 `Dialog({content: Component})` 动态渲染
> 2. 通过 `router.push` / `openUrl` 等方式跳转（但目标路由不存在会失效）
> 3. 属于历史遗留死代码

修改这些页面前，必须先用 `Grep` 工具搜索该页面是否被其他**实际使用**的页面以组件方式 `import` 引用。如未被引用则视为死页面。

#### 4.4.3 实际使用页面清单（基于代码静态分析）

**确认实际使用的页面**（通过 `index2.ts` 注册 + 被这些页面以组件方式 import 的其他页面）：

**核心入口与布局**（路由直接注册）
- `@/pages/home/index.vue` — 应用主入口（Home）
- `@/pages/statics/index.vue` — 统计页
- `@/pages/dutyInformation/index.vue` — 值班信息页

**Home 子组件**（被 home/index.vue 直接 import）
- `@/pages/home/voiceRemind.vue` — 语音提醒

> 其他业务模块（mission/alarm/videoControl/messages/policeAdmin/bigScreen/planSafety/leadVehicle/coordination 等）是否被实际使用，**需通过静态分析 `statics/index.vue` 和 `dutyInformation/index.vue` 的完整组件 import 链确认**。详见 `docs-local/web/01-页面与路由.md` 中的组件依赖图。

#### 4.4.4 死页面识别规则 🔴

**以下目录/文件视为死页面，禁止新增功能或修改，仅可清理**：
- `web/src/router/index.ts` — 死代码路由文件
- `web/src/router/routerGuard.ts` — 未生效的路由守卫（除非后续在 `index2.ts` 中添加 `useRouterGuard(router)` 调用）
- `web/src/router/modules/*.ts` — 未生效的模块路由文件（5 个）
- `web/src/router/index.ts` 引用的页面（如 `@/pages/login/index.vue`、`@/pages/iconView/index.vue`）—— 实际登录走 bridge，不走此路由
- `web/src/pages/` 下未被 `index2.ts` 注册 + 未被实际使用页面以组件方式 import 的所有页面（如 `aiModule/`、`guide/`、`mission/common.ts` 等历史遗留）

#### 4.4.5 修改 web 代码前的强制检查 🔴

修改 `web/src/pages/` 下任何页面前，必须先确认：
1. 该页面是否被 `index2.ts` 直接注册？
2. 若未直接注册，是否被 `index2.ts` 注册的页面（home/statics/dutyInformation）以组件方式 `import` 引用（需递归检查整个 import 链）？
3. 若都未引用 → **死页面**，禁止修改（除非任务是清理死代码）
4. 若被引用 → 正常修改

**判断方法**：
```bash
# 1. 检查路由是否注册
# 例如确认 pages/statics/index.vue 是否注册：
grep -r "pages/statics" web/src/router/index2.ts

# 2. 检查是否被实际使用的页面以组件方式 import
# 例如确认 pages/coordination/index.vue 是否被引用：
grep -rn "pages/coordination" web/src/pages/home/
grep -rn "pages/coordination" web/src/pages/statics/
grep -rn "pages/coordination" web/src/pages/dutyInformation/
```

#### 4.4.6 修复建议 🟡

若后续需要恢复动态路由注册功能，应在 `index2.ts` 中添加：
```typescript
import { useRouterGuard } from './routerGuard';
useRouterGuard(router);
```
此时 `router/modules/*.ts` 的模块路由才会生效，4.4.3 节的"其他业务页面"也才能通过路由访问。修复后需同步更新本规则。

## 五、前端项目文档强制维护规则 🔴

> 本规则定义 H5Portal / web / admin 三个前端项目的本地文档（`docs-local/`）维护规范。
>
> **本地规则说明**: `docs-local/` 目录已在 `.gitignore` 中声明，作为本地文档不提交 Git，仅在本地生效。

### 5.1 文档存放位置 🔴

所有项目文档统一存放于仓库根的 `docs-local/` 目录下，按项目名分子目录管理：

| 项目 | 文档目录 | INDEX 索引文件 |
|------|---------|----------------|
| H5Portal | `docs-local/H5Portal/` | [docs-local/H5Portal/INDEX.md](file:///d:/work/master/Linkx/docs-local/H5Portal/INDEX.md) |
| web | `docs-local/web/` | [docs-local/web/INDEX.md](file:///d:/work/master/Linkx/docs-local/web/INDEX.md) |
| admin | `docs-local/admin/` | [docs-local/admin/INDEX.md](file:///d:/work/master/Linkx/docs-local/admin/INDEX.md) |

### 5.2 文档结构 🟡

每个项目的文档按以下结构组织：

```
docs-local/<项目>/
├── INDEX.md                          # 主索引（文档清单 + 项目概览 + 维护说明）
├── 01-页面与路由.md                  # 路由清单 + 页面功能描述 + 路由守卫
├── 02-组件与状态.md 或合并版         # Store + 组件 + Hooks 清单（H5Portal/web 独立，admin 合并到 02）
├── 03-权限与API.md 或合并版           # API + 鉴权 + 权限（H5Portal/web 独立，admin 合并到 02）
└── 深度分析-*.md                     # 业务模块的深度分析（业务细节、交互流程、数据流向）
```

### 5.3 强制更新规则 🔴

**每次对话中，当 AI 修改了以下内容时，必须同步更新对应的文档**：

| 修改内容 | 需要更新的文档 |
|---------|---------------|
| 路由配置（router/index、router/modules、routerGuard） | `01-页面与路由.md` |
| 页面文件（新增/删除/重命名/修改业务逻辑） | `01-页面与路由.md` + 对应的`深度分析-*.md` |
| Store（新增/删除/修改 state/getters/actions） | `02-组件与状态.md`（admin 为 `02-组件状态权限API.md`） |
| 组件（新增/删除/重命名/修改 Props/Emits） | `02-组件与状态.md` |
| Hooks（新增/删除/修改） | `02-组件与状态.md` |
| API 接口（新增/删除/修改 URL/方法/参数） | `03-权限与API.md`（admin 为 `02-组件状态权限API.md`） |
| HTTP 封装（拦截器/Token 注入/错误处理） | `03-权限与API.md` |
| 鉴权流程（登录/登出/Token 刷新） | `03-权限与API.md` |
| License 权限（新增/删除权限位） | `03-权限与API.md` + `01-页面与路由.md` |
| Bridge 桥接（WebView2/浏览器环境差异） | `03-权限与API.md` |
| 主题系统（light/dark 切换机制） | `03-权限与API.md` |

### 5.4 每次对话前的文档检查 🟡

**每次对话开始时，AI 应主动检查**：

1. **用户提问涉及哪个项目？** → 定位到 `docs-local/<项目>/INDEX.md`
2. **用户提问涉及哪个模块？** → 根据 INDEX.md 的文档清单定位到具体文档
3. **读取对应文档**了解项目当前状态后再开始工作
4. **工作完成后**，根据 5.3 节的规则判断是否需要更新文档
5. **若更新了文档**，同步修改 INDEX.md 的"最后更新"时间

### 5.5 多人开发模式下的文档同步 🟡

由于 `docs-local/` 不提交 Git，多人开发时可能出现文档不同步的情况：

1. **每次拉取代码后**，AI 应主动检查代码与文档是否一致
2. **若发现代码与文档不一致**（如新增了路由但文档未更新），应主动更新文档
3. **用户执行某个指令后**（如 `git pull`、`npm install`），AI 可查看项目代码然后更新文档
4. **更新文档时**，在文档末尾添加更新记录：`> 更新记录: YYYY-MM-DD - 更新内容描述`

### 5.6 文档写作规范 🟡

- **语言**: 中文
- **代码引用**: 使用 markdown 链接引用代码文件，如 `[文件名](file:///绝对路径)`
- **代码块**: 使用标准 markdown 代码块，标注语言
- **表格**: 使用 markdown 表格，对齐整齐
- **标题层级**: 使用 `#`、`##`、`###`、`####` 四级标题
- **文档头部**: 必须包含文档位置、项目路径、最后更新时间、维护说明
