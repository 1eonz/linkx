---
name: frontend-unit-test-runner
description: "运行前端 Vitest 单元测试并分析结果。当用户要求跑测试、运行测试、检查覆盖率、验证测试是否通过，或在提交代码前需要测试守卫时触发。支持单文件/单项目/全项目运行，自动定位失败用例并给出修复建议。"
alwaysApply: false
---

# Frontend Unit Test Runner Skill

运行前端 Vitest 单元测试，分析测试结果，定位失败用例，校验覆盖率，并给出修复建议。

**本 Skill 覆盖四个前端项目**：`web`、`agent/web`、`H5Portal`、`cloudcmd-admin-web`。

**可被 `git-commit-push` Skill 编排调用**，在提交代码前运行受影响的测试作为守卫。

## 触发条件

**关键词触发**：
- "跑测试" / "运行测试" / "跑单测"
- "测试通过了吗" / "测试挂了吗"
- "检查覆盖率" / "覆盖率报告"
- "verify tests" / "run tests" / "test coverage"

**场景触发**：
- 用户修改了 `src/utils/**` 或 `tests/unit/**` 后
- 用户要求提交代码（与 `git-commit-push` 联动）
- CI 流水线中测试失败需要定位

## 核心工作流

### Step 1: 确定运行范围

| 场景 | 判断依据 | 运行范围 |
|------|---------|---------|
| 单文件 | 用户指定某 `.spec` 文件 | 仅该文件 |
| 单项目 | 用户指定项目名（web/H5Portal/...） | 该项目全部测试 |
| 全项目 | 用户说"跑所有前端测试" | 四个项目依次运行 |
| 受影响范围 | 被 git-commit-push 调用 | 基于 `git diff --name-only` 推断 |

**受影响范围推断规则**：
- `src/utils/xxx.ts` → `tests/unit/xxx.spec.ts`
- `src/hooks/xxx.ts` → `tests/unit/xxx.spec.ts`
- `src/data/xxx.ts` → `tests/unit/xxx.spec.ts`
- 若无对应测试文件，跳过（提示用户可用 frontend-unit-test-writer 生成）

### Step 2: 检查前置条件

1. 项目是否安装 vitest 依赖（检查 `node_modules/.bin/vitest` 是否存在）
2. 是否存在 `vitest.config.ts`
3. 是否存在 `tests/unit/` 目录与测试文件
4. 若缺失，提示用户先运行 frontend-unit-test-writer 或手动初始化

### Step 3: 执行测试命令

在项目根目录用 `RunCommand` 执行（`blocking=true`）：

| 场景 | 命令 |
|------|------|
| 单文件 | `pnpm vitest run tests/unit/<file>.spec.ts --reporter=verbose` |
| 单项目 | `pnpm test`（web/agent/web/H5Portal）或 `npm test`（cloudcmd-admin-web） |
| 覆盖率 | `pnpm test:coverage` 或 `npm run test:coverage` |
| Watch 模式 | `pnpm test:watch`（仅开发时，用户明确要求时才用） |

**默认行为**：运行测试时**默认附加 `--coverage` 参数**，自动生成覆盖率报告。这样用户无需额外命令即可获得：
- 终端实时覆盖率摘要（statements/branches/functions/lines）
- HTML 可视化报告（生成在 `<项目>/coverage/index.html`）

**报告生成位置**：

| 项目 | HTML 报告路径 |
|------|-------------|
| web | `d:\code\Linkx\Linkx-B\web\coverage\index.html` |
| agent/web | `d:\code\Linkx\Linkx-B\agent\web\coverage\index.html` |
| H5Portal | `d:\code\Linkx\Linkx-B\H5Portal\coverage\index.html` |
| cloudcmd-admin-web | `d:\code\Linkx\Linkx-B\cloudcmd-admin-web\coverage\index.html` |

**实际执行命令**（默认带 coverage）：
- web/agent/web/H5Portal：`pnpm test:coverage`
- cloudcmd-admin-web：`npm run test:coverage`

若用户明确说"只跑测试不要覆盖率"或"快速跑一下"，则使用不带 coverage 的命令（`pnpm test` / `npm test`），不生成 HTML 报告。

### Step 4: 解析测试输出

从 Vitest 输出中提取：

| 信息 | 提取方式 |
|------|---------|
| 通过数 / 失败数 / 跳过数 / 总数 | `Tests X passed | Y failed | Z skipped (N)` |
| 测试文件数 | `Test Files X passed | Y failed (Z)` |
| 覆盖率 | `Coverage X% statements | Y% branches | Z% functions | W% lines` |
| 失败用例的文件:行号 | `at Object.<anonymous> (tests/unit/xxx.spec.ts:25:17)` |
| 源码文件:行号 | `at funcName (src/utils/xxx.ts:12:15)` |
| 耗时 | `Duration X.XXs` |

### Step 5: 失败定位与修复建议

读取失败用例的源码与测试代码，分类失败原因：

| 失败类型 | 识别特征 | 修复方向 |
|----------|---------|---------|
| **断言不符** | `expected X to be Y` | 检查测试期望或源码逻辑 |
| **源码 Bug** | 测试期望合理，源码实现有误 | 标注 ⚠️ 疑似 Bug，建议用户确认后修复源码 |
| **测试过时** | 源码函数签名/返回值已变更 | 调用 frontend-unit-test-writer 重新生成 |
| **Mock 未生效** | `vi.mock` 未拦截 / `undefined is not a function` | 检查 mock 路径与导出方式 |
| **环境缺失** | `localStorage is not defined` / `navigator is undefined` | 检查 `environment: 'happy-dom'` 配置 |
| **超时** | `Test timed out in 5000ms` | 检查异步/Promise 未 resolve |
| **快照不符** | `Snapshot: - expected + received` | 询问用户是否更新快照 `pnpm vitest -u` |

### Step 6: 覆盖率校验

对比实际覆盖率与目标（见覆盖率校验规则）：
1. 从覆盖率输出提取四个指标
2. 与目标对比，任一不达标 → ⚠️ 警告
3. 列出未覆盖的文件
4. 建议调用 frontend-unit-test-writer 补充测试

### Step 7: 输出结果与决策

| 场景 | 输出 | 决策 |
|------|------|------|
| 全绿 + 覆盖率达标 | ✅ 通过摘要 | 可提交代码 |
| 有失败 | ❌ 失败清单 + 修复建议 | 阻断提交 |
| 全绿但覆盖率下降 | ⚠️ 警告 + 补测建议 | 提示用户 |
| 被 git-commit-push 调用 | 返回决策结果（全绿/失败） | 给 git-commit-push |

## 命令模板

### 项目路径与包管理器映射

| 项目 | 路径 | 包管理器 | 测试命令 | 覆盖率命令 |
|------|------|---------|---------|-----------|
| web | `d:\code\Linkx\Linkx-B\web` | pnpm | `pnpm test` | `pnpm test:coverage` |
| agent/web | `d:\code\Linkx\Linkx-B\agent\web` | pnpm | `pnpm test` | `pnpm test:coverage` |
| H5Portal | `d:\code\Linkx\Linkx-B\H5Portal` | pnpm | `pnpm test` | `pnpm test:coverage` |
| cloudcmd-admin-web | `d:\code\Linkx\Linkx-B\cloudcmd-admin-web` | npm | `npm test` | `npm run test:coverage` |

### 单文件运行

```bash
cd d:\code\Linkx\Linkx-B\<project>
pnpm vitest run tests/unit/validate.spec.ts --reporter=verbose
```

### 单项目运行

```bash
cd d:\code\Linkx\Linkx-B\web
pnpm test
```

### 覆盖率运行

```bash
cd d:\code\Linkx\Linkx-B\<project>
pnpm test:coverage
```

### 受影响范围运行（基于 git diff）

```bash
cd d:\code\Linkx\Linkx-B\<project>
git --no-pager -c color.ui=false diff --name-only HEAD
# 提取 src/utils/xxx.ts → tests/unit/xxx.spec.ts
pnpm vitest run <affected-spec-files>
```

### 全项目运行（CI 场景）

```bash
cd d:\code\Linkx\Linkx-B\web && pnpm test:coverage
cd d:\code\Linkx\Linkx-B\agent\web && pnpm test:coverage
cd d:\code\Linkx\Linkx-B\H5Portal && pnpm test:coverage
cd d:\code\Linkx\Linkx-B\cloudcmd-admin-web && npm run test:coverage
```

## 覆盖率校验规则

各项目覆盖率目标（来自 `vitest.config.ts` 的 `coverage.thresholds`）：

| 项目 | statements | branches | functions | lines |
|------|-----------|----------|-----------|-------|
| web | 80% | 75% | 75% | 80% |
| agent/web | 75% | 70% | 70% | 75% |
| H5Portal | 70% | 65% | 65% | 70% |
| cloudcmd-admin-web | 75% | 70% | 70% | 75% |

**校验逻辑**：
1. 从覆盖率输出提取四个指标
2. 与目标对比，任一不达标 → ⚠️ 警告
3. 列出未覆盖的文件（`Uncovered Lines` 报告）
4. 建议调用 frontend-unit-test-writer 补充测试

## 输出格式

### 全绿场景

```markdown
## ✅ 测试通过

**项目**：web
**范围**：tests/unit/validate.spec.ts

| 指标 | 值 |
|------|-----|
| 用例 | 8 passed / 0 failed / 0 skipped |
| 覆盖率 | statements 100% | branches 87.5% | functions 100% | lines 100% |
| 耗时 | 1.2s |

### 📊 测试报告
- HTML 覆盖率报告：`web/coverage/index.html`（用浏览器打开查看可视化报告）
- 终端已输出实时覆盖率摘要

✅ 全部通过，可以提交代码。
```

### 失败场景

```markdown
## ❌ 测试失败

**项目**：web
**范围**：tests/unit/auth.spec.ts

| 指标 | 值 |
|------|-----|
| 用例 | 5 passed / 2 failed / 1 skipped |
| 耗时 | 2.3s |

### 失败用例

#### 1. utils/auth > removeRefreshToken > 应删除 refreshToken
- 📄 测试：tests/unit/auth.spec.ts:45
- 📄 源码：src/utils/auth.ts:28
- 💬 期望 refreshToken 已删除，实际仍存在于 Cookie 中
- ⚠️ 疑似 Bug：`removeRefreshToken` 用 `localStorage.removeItem`，但 `setRefreshToken` 用 `Cookies.set`
- 💡 修复建议：将 `removeRefreshToken` 改为 `Cookies.remove('refreshToken')`

### 下一步
1. 修复上述 2 个失败用例
2. 重新运行 `pnpm test` 验证
3. 确认全绿后再提交代码

### 📊 测试报告
- HTML 覆盖率报告：`web/coverage/index.html`（用浏览器打开查看失败用例的代码覆盖详情）
```

### 覆盖率不足场景

```markdown
## ⚠️ 测试通过但覆盖率不足

**项目**：H5Portal
**覆盖率**：

| 指标 | 实际 | 目标 | 状态 |
|------|------|------|------|
| statements | 68% | 70% | ❌ |
| branches | 60% | 65% | ❌ |
| functions | 72% | 65% | ✅ |
| lines | 69% | 70% | ❌ |

### 未覆盖文件
- `src/utils/websocket.js`（0% — 建议走集成测试，可加入 exclude）
- `src/utils/file.js`（45% — `base64ToFile` 未导出，建议先导出再测）

### 📊 测试报告
- HTML 覆盖率报告：`H5Portal/coverage/index.html`（用浏览器打开查看每行代码的覆盖详情）
- 报告中可点击具体文件查看未覆盖的行号

### 建议
- 将 websocket.js 加入 vitest.config.ts coverage.exclude
- 调用 frontend-unit-test-writer 为 file.js 补充测试
```

## 与 git-commit-push 的联动

### 提交守卫闭环

当被 `git-commit-push` Skill 调用时：

1. 接收 git-commit-push 传递的受影响文件列表（或自行执行 `git diff --name-only`）
2. 推断受影响的测试文件（映射规则：`src/utils/xxx.ts` → `tests/unit/xxx.spec.ts`）
3. 运行受影响的测试
4. 返回决策结果：
   - 全绿 → 允许提交
   - 失败 → 阻断提交，返回失败清单

### 联动接口

通过自然语言消息传递，无需特殊协议：
- git-commit-push → frontend-unit-test-runner：「提交前请运行受影响的单元测试」
- frontend-unit-test-runner → git-commit-push：「测试全绿，可以提交」或「测试失败，阻断提交」

## 已知环境差异（基于实施经验）

### happy-dom 与真实浏览器差异

以下差异**不是 bug**，是测试环境特性。测试应反映实际运行时行为：

| 函数 | 真实浏览器 | happy-dom | 原因 |
|------|-----------|-----------|------|
| `isPromise(Promise.resolve())` | true | false | happy-dom 的 toString 返回 `[object Object]` 而非 `[object Promise]` |
| `isWindow(window)` | true | false | happy-dom 返回 `[object global]` 而非 `[object Window]` |
| `isElement(div)` | true | false | happy-dom 返回 `[object HTMLDivElement]` 而非 `[object Object]` |

### 各项目已知运行问题

| 项目 | 问题 | 解决方案 |
|------|------|---------|
| web | `@/utils/index.ts` 导入链触发 `indexedDB is not defined` | 测试文件中 `vi.stubGlobal('indexedDB', ...)` 或 `vi.mock('@/hooks', ...)` |
| H5Portal | `@/utils/index.js` 导入 vant 副作用 | `setup.ts` 已全局 mock vant |
| H5Portal | `@/common/config` 循环依赖 | 测试文件中 `vi.mock('@/common/config', ...)` |
| cloudcmd-admin-web | `@/locales/lang/cn` 使用 `require.context` | 测试文件中 `vi.mock('@/locales/lang/cn', ...)` |
| cloudcmd-admin-web | `.vue` 文件无法解析 | `vitest.config.ts` 中配置 alias stub |
| cloudcmd-admin-web | `@/utils/index.js` 顶层访问 `localStorage` | `beforeEach` 中设置 `localStorage.setItem('localLanguage', 'cn')` |

## 约束

1. **必须在项目根目录执行测试命令**（通过 `RunCommand` 的 `cwd` 参数指定）
2. **PowerShell 环境下不要使用 `tail`/`head` 等 Unix 命令**，直接运行 `pnpm test` 即可
3. **失败用例必须读取源码后给出修复建议**，不要笼统说"测试失败"
4. **覆盖率不达标时必须列出未覆盖文件**，并建议调用 frontend-unit-test-writer 补充
5. **不要自动修复源码 bug**，仅标注 ⚠️ 疑似 Bug 并建议用户确认
6. **被 git-commit-push 调用时只返回决策结果**（全绿/失败），不输出完整报告
7. **运行测试前检查 `node_modules` 是否存在**，若不存在提示用户先 `pnpm install`
8. **happy-dom 环境差异不是失败**，测试应反映实际运行时行为
