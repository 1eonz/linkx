---
name: frontend-unit-test-writer
description: "为前端工具/业务函数生成 Vitest 单元测试。当用户要求写单测、补测试、增加测试用例、为新函数添加测试，或修改 utils/hooks 后需要同步测试时触发。覆盖 web/agent/web/H5Portal/cloudcmd-admin-web 四个前端项目。"
alwaysApply: false
---

# Frontend Unit Test Writer Skill

为前端工具函数、业务函数、hooks 生成符合 Vitest 规范的单元测试代码。

**本 Skill 覆盖四个前端项目**：`web`、`agent/web`、`H5Portal`、`cloudcmd-admin-web`。

## 触发条件

**关键词触发**（任一即可）：
- "写单测" / "写单元测试" / "写测试用例"
- "补测试" / "补充测试" / "补单测"
- "为 xxx 函数加测试"
- "增加测试覆盖"
- "unit test" / "write tests" / "add tests"

**场景触发**：
- 用户修改了 `src/utils/**` 或 `src/hooks/**` 下的函数后
- 用户新增了一个工具函数

## 核心工作流

### Step 1: 识别目标项目与函数

1. 从用户消息或当前打开的文件路径推断目标项目：

| 路径包含 | 项目 | 技术栈 | 测试文件扩展名 |
|---------|------|--------|--------------|
| `web/` | web | TS + Vue3 + Element Plus + Vite 5 | `.spec.ts` |
| `agent/web/` | agent/web | TS + Vue3 + Element Plus + Vite 5 | `.spec.ts` |
| `H5Portal/` | H5Portal | JS + Vue3 + Vant + Vite 7 | `.spec.js` |
| `cloudcmd-admin-web/` | cloudcmd-admin-web | JS + Vue2 + Element UI + Webpack 4 | `.spec.js` |

2. 明确被测函数的路径与名称（必要时向用户确认）

### Step 2: 加载项目规范与配置

1. 读取 `.trae/rules/frontend/eslint.md`（测试代码风格）
2. 读取 `.trae/rules/frontend/business-rules.md`（断言规范）
3. 读取对应项目的 `vitest.config.ts`（别名/globals/include 配置）

### Step 3: 分析被测函数

1. 读取源文件，识别所有导出函数的签名
2. 分类：纯函数 / 需 mock / 副作用强
3. 抽取分支逻辑（if/switch/正则/边界值）
4. 识别依赖：
   - `localStorage` / `sessionStorage`
   - `js-cookie`
   - `new Date()` / `Date.now()`
   - `window` / `navigator` 属性
   - 模块级副作用（顶层立即执行的代码）
   - 外部 API 调用
   - `import.meta.env`
   - `requestAnimationFrame`

### Step 4: 生成测试文件

1. **文件路径**：`tests/unit/<源文件名>.spec.<ext>`
2. **命名规范**：
   - 顶层 `describe('<模块名>')`
   - 内层 `describe('<函数名>')`
   - 用例 `it('应<行为描述>', ...)`
3. **用例覆盖**：按用例覆盖矩阵（见下）覆盖所有维度
4. **Mock 策略**：按依赖类型选择合适方式（见 Mock 策略表）
5. **遵循 ESLint 规范**：import 顺序、命名、对象简写

### Step 5: 自检与提示

1. 运行该测试文件验证通过（调用 frontend-unit-test-runner 或直接 `pnpm vitest run <file>`）
2. 若发现疑似 bug，用 `it.skip` 标注并注释说明期望行为与实际行为差异
3. 输出覆盖率摘要（该文件）
4. 向用户返回结构化摘要（见输出格式）

## 测试代码生成规范

### 文件与命名

| 规则 | 示例 |
|------|------|
| 测试文件位置 | `web/tests/unit/validate.spec.ts` |
| 文件名与源文件同名 | `validate.ts` → `validate.spec.ts` |
| 顶层 describe 用模块名 | `describe('utils/validate', () => {})` |
| 内层 describe 用函数名 | `describe('isEmailreg', () => {})` |
| 单个用例用中文描述行为 | `it('应识别合法邮箱', () => {})` |

### 用例覆盖矩阵

每个被测函数至少覆盖以下维度（适用时）：

| 维度 | 描述 | 示例 |
|------|------|------|
| **正常值** | 业务期望的输入 | `isEmailreg('a@b.com')` → true |
| **边界值** | 长度/数值的临界点 | 0 / -1 / MAX_SAFE_INTEGER / 空字符串 |
| **异常值** | 非法类型/格式 | null / undefined / NaN / 对象 |
| **分支覆盖** | 所有 if/switch 分支 | bizStatus 为 1/4/6（在线）与其他（离线） |
| **正则边界** | 校验类函数的特殊字符 | 含中文 / emoji / 控制字符 |
| **副作用验证** | mock 后验证调用 | `expect(Cookies.set).toHaveBeenCalledWith(...)` |
| **可逆性** | 加解密/序列化对 | `decrypt(encrypt(x)) === x` |
| **不修改入参** | 纯函数契约 | 深拷贝入参后断言原值未变 |

### Mock 策略表

| 依赖类型 | 推荐方式 | 示例 |
|----------|----------|------|
| `localStorage` / `sessionStorage` | 直接使用（happy-dom 提供），`afterEach` 清理 | `localStorage.setItem('x', '1')` |
| `js-cookie` | `vi.mock('js-cookie', () => ({ default: { get: vi.fn(), set: vi.fn(), remove: vi.fn() } }))` | mock 后验证 `Cookies.set` 被调用 |
| `new Date()` / `Date.now()` | `vi.useFakeTimers()` + `vi.setSystemTime(new Date('2026-07-27'))` | 固定时间点断言 |
| `Math.random` | `vi.spyOn(Math, 'random').mockReturnValue(0.5)` | 确定性输出 |
| `window` / `navigator` 属性 | `vi.stubGlobal('navigator', { userAgent: '...' })` | UA 模拟 |
| 模块级副作用（如 clientEnv） | `vi.resetModules()` + 重新 `import()` | 重新加载触发缓存重建 |
| 外部 API 调用 | `vi.mock('@/api/xxx', () => ({ funcName: vi.fn() }))` | 拦截网络请求 |
| `requestAnimationFrame` | `vi.useFakeTimers()` + `vi.advanceTimersByTime()` | rAF 节流测试 |
| `import.meta.env` | `vi.stubEnv('VITE_XXX', 'value')` | Vite 环境变量 |

## 项目差异处理

### web / agent/web（TS + Vue3）

- 测试文件用 `.spec.ts`
- 别名：`@/` → `src/`、`#/` → `types/`
- 使用 `import { describe, it, expect, vi } from 'vitest'`
- `import.meta.env` 需 `vi.stubEnv` mock

### H5Portal（JS + Vue3 + Vant）

- 测试文件用 `.spec.js`
- 别名仅 `@/` → `src/`
- `setup.ts` 已全局 mock `vant`，无需重复 mock
- `@/common/config` 存在循环依赖，需 `vi.mock('@/common/config', ...)`
- `uni.*` 全局对象需 `vi.stubGlobal('uni', ...)`

### cloudcmd-admin-web（JS + Vue2 + Element UI）

- 测试文件用 `.spec.js`
- 别名 `@/` → `src/`
- `@vue/test-utils@1`（Vue2 兼容版）
- Vuex store 测试：`mutations.X(state, payload)` 直接调用
- `@/locales/lang/cn` 使用 `require.context`（webpack API），需 `vi.mock`
- `.vue` 文件需在 `vitest.config.ts` 中配置 alias stub
- `@/utils/index.js` 顶层访问 `localStorage`，需在 `beforeEach` 中设置

## 已知项目特殊问题（基于实施经验）

### web 项目

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `@/utils/index.ts` 导入报 `indexedDB is not defined` | 导入链触发 `plugins/logs` 访问 indexedDB | `vi.stubGlobal('indexedDB', ...)` 或 `vi.mock('@/hooks', ...)` |
| `isPromise(Promise.resolve())` 返回 false | happy-dom 的 toString 返回 `[object Object]` | 测试应反映实际运行时行为，断言为 false |
| `isWindow(window)` 返回 false | happy-dom 返回 `[object global]` | 同上，断言为 false |
| `isElement(div)` 返回 false | happy-dom 返回 `[object HTMLDivElement]` | 同上，断言为 false |
| `@/utils/dateUtil.ts` 的 `strToDate` 空字符串进入解析分支 | 源码 `isEmpty(str)` 应为 `!isEmpty(str)` | 用 `it.skip` 标注 bug |

### H5Portal 项目

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `@/utils/index.js` 导入触发 vant 副作用 | 顶层 `import { Popup, showToast } from 'vant'` | `setup.ts` 已全局 mock vant |
| `@/common/config` 循环依赖 | `@/utils` → `@/common/api/group` → `http` → `config` → `@/utils` | `vi.mock('@/common/config', ...)` |
| `@/utils/totalFunc.js` 顶层调用 `useCommunicationStore()` | 模块级副作用 | `vi.mock('@/stores/communication.js', ...)` |
| `queryStringToJson('a=b=c')` 丢失 `=c` | `split('=', 2)` 在 JS 中截断数组 | 用 `it.skip` 标注 bug |

### cloudcmd-admin-web 项目

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| `@/locales/lang/cn.js` 报 `require.context is not a function` | webpack API 在 Vite 下不可用 | `vi.mock('@/locales/lang/cn', ...)` |
| `@/layout` 导入失败 | 是 `.vue` 文件，无 `@vitejs/plugin-vue2`（需 Vue 2.7+） | `vitest.config.ts` 中配置 alias stub |
| `@/views/*` 动态 import 失败 | `ROUTE_COMPONENT_MAP` 中的 `() => import('@/views/...')` | `vitest.config.ts` 中配置 `/@\/views\/.*/` → stub |
| `@/utils/index.js` 顶层访问 `localStorage` | `const language = localStorage.getItem('localLanguage')` | `beforeEach` 中 `localStorage.setItem('localLanguage', 'cn')` |

### agent/web 项目

- `src/utils/is.ts` 与 web 项目**完全相同**，测试用例可直接复用
- `displayImage` 依赖 `import.meta.env.DEV` 和 `VITE_PROXY`，需 `vi.stubEnv`

## 疑似 Bug 处理规范

当测试发现源码行为与预期不符时：

1. **不要修改源码**（不在本 Skill 职责范围内）
2. 用 `it.skip` 标注用例，并在注释中说明：
   ```typescript
   // ⚠️ 疑似 Bug：源码 isEmpty(str) 应为 !isEmpty(str)
   // 空字符串应原样返回，当前进入解析分支
   it.skip('空字符串应原样返回', () => {
     expect(strToDate('')).toBe('');
   });
   ```
3. 在输出摘要的"⚠️ 疑似 Bug"章节列出所有发现的 bug

## 输出格式

Skill 执行完成后，向用户返回结构化摘要：

```markdown
## 单元测试已生成

**被测函数**：`web/src/utils/validate.ts` 的 `isEmailreg`
**测试文件**：`web/tests/unit/validate.spec.ts`
**用例数**：8 个（正常 3 / 边界 3 / 异常 2）

### 覆盖情况
- ✅ 正常邮箱（含 .com/.cn/子域名）
- ✅ 边界（空字符串、缺@、缺域名、中文域名）
- ✅ 异常（null、undefined、数字入参）

### ⚠️ 疑似 Bug
- `isEmailreg` 对包含空格的邮箱返回 true，可能不符合预期（源码第 12 行）
  建议测试用例已标注 `it.skip`，待确认后改为 `it`

### 执行结果
```
✓ web/tests/unit/validate.spec.ts (8)
  ✓ isEmailreg > 应识别合法邮箱
  ...
Test Files  1 passed (1)
Tests       8 passed (8)
```

### 下一步建议
- 运行 `pnpm test:coverage` 查看整体覆盖率
- 若确认疑似 bug，请修复源码后取消 `it.skip`
```

## 约束

1. **禁止为 P2 类（强副作用）函数强行写单测**，应提示用户走集成/E2E
2. **禁止在测试中调用真实网络请求**（必须 mock）
3. **禁止测试代码依赖执行顺序**（每个 `it` 必须独立）
4. **禁止为追求覆盖率写无意义的"调用了某函数"断言**
5. **禁止在测试中修改源码逻辑**
6. **测试应反映实际运行时行为**，不要为了"正确性"而写与环境不符的断言（如 happy-dom 下的 toString 差异）
7. **每个测试文件必须在生成后运行验证**，确保通过（除 `it.skip` 标注的 bug 用例）
8. **遵循对应项目的 ESLint 规范**（import 顺序、命名风格、对象简写等）
