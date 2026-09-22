---
alwaysApply: false
globs: web/**,agent/web/**,H5Portal/**,cloudcmd-admin-web/**,*.vue,*.ts,*.tsx,*.js,*.jsx,*.less,*.scss
description: web、agent/web、H5Portal、cloudcmd-admin-web 四个前端项目的开发任务：Vue2/Vue3 组件、路由守卫、Pinia/Vuex 状态管理、API 模块、样式与移动端适配、ESLint/Prettier/Stylelint 配置、TS 类型定义、路径别名、依赖管理
---

# 前端项目特有约定

> 通用 Vue/JS/CSS 规范、性能优化、安全教程、Lint 默认规则不在此文档，由 `frontend-guidelines` 技能按需加载。本文件仅记录 4 个前端项目的差异化约定，修改前先确认目标项目，不要跨项目混用风格。

> **何时调用 `frontend-guidelines` 技能**: 需要 Vue3/Vue2 通用编码规则（响应式、SFC、组件数据流、composables、性能优化技巧）、ESLint/Prettier/Stylelint 默认规则解读、前端安全编码细节（`security.md`，含 v-html/XSS/CSRF）、完整命名规范与类型命名后缀时调用。本文件未覆盖的通用前端知识均由该技能提供。

## 〇、全局约束

- 🔴 **先确认项目，再遵循规则**: 修改前确认目标项目，遵循其自身配置，不要跨项目混用风格
- 🔴 **前端技术栈差异**: admin 是 Vue2+Options API+SCSS；其余 Vue3+Composition API（web/agent/web 用 Less）
- 🔴 **安全红线**: 密码禁止 MD5/明文、敏感数据必须脱敏、全站 HTTPS、未安装 DOMPurify 时禁止 v-html 渲染外部/用户内容（详见 §11 与 frontend-guidelines 技能的 security.md）
- 🟡 **web/agent/web 共享配置**: config/ 目录修改需两项目都验证；路径别名 `#/→types/`（见 §3）

> 规则级别: 🔴 强制 / 🟡 推荐 / 🔵 参考

## 一、项目栈

| 项目 | 路径 | Vue | UI | 样式 | 状态 | 路由 | HTTP | 语言 |
|------|------|-----|----|------|------|------|------|------|
| web | web/ | 3 | Element Plus | Less | Pinia | Vue Router | Axios | TS |
| agent/web | agent/web/ | 3 | Element Plus | Less | Pinia | Vue Router | Axios | TS |
| H5Portal | H5Portal/ | 3 | Vant | SCSS | Pinia | Vue Router | Axios | JS 为主 |
| admin | cloudcmd-admin-web/ | 2 | Element UI | SCSS | Vuex | Vue Router | Axios | JS |

> 各项目页面目录 / Store 目录 / Hooks 目录差异详见 §12「项目目录结构」

## 二、HTTP 入口与 API 模块

| 项目 | HTTP 入口 | API 目录 |
|------|----------|---------|
| web/agent/web | `import http from '@/utils/http'`（默认导出） | src/api/ |
| H5Portal | `import { http } from '@/common/network/http.js'`（命名导出，底层 Request 类在 src/utils/http.js） | src/common/api/ |
| admin | src/api/ 下的 JS 封装 | src/api/ |

禁止在组件中直接写 URL，统一走 API 模块。

## 三、路径别名

- web/: `@/* → src/*`，`#/* → types/*`（types/ 在项目根，非 src/）
- agent/web/: 按 tsconfig.json 的 paths 配置
- H5Portal / admin: `@/* → src/*`

web/ 类型目录分层:
- `types/`（#/* 别名目标）— 全局/跨模块类型声明（global.d.ts / axios.d.ts / components.d.ts / config.d.ts / elementPlusGlobal.d.ts / index.d.ts / module.d.ts）
- `src/types/` — 平台相关类型（如 webview2.d.ts）
- `pages/<module>/types.ts` — 模块内类型

## 四、路由守卫文件

- web/agent/web: `src/router/routerGuard.ts`（不要写成 guard.ts）
- H5Portal: `src/router/index.js`
- admin: `src/router/index.js` + `src/router/modules/*.js`

## 五、组件目录组织

- **web/ / H5Portal**: `src/components/` 下按功能建子目录，组件直接以 `.vue` 文件放置（如 `components/Charts/PieChart.vue`）
- **agent/web/**: 「功能文件夹 + `src/` + 聚合 `index.ts`」模式（参考 vue-vben）：
  ```
  components/Dialog/src/Dialog.vue + helper.ts
  components/Dialog/index.ts   # 仅 export * 聚合
  components/index.ts          # 顶层聚合
  ```
- **admin**: `components/<Name>/index.vue` 形式

## 六、Pinia Store 风格

- web/: 现状以 **Options Store** 为主
- H5Portal/: 现状以 **Setup Store** 为主
- 新增 Store 优先 Setup Store，但同一项目内保持风格统一
- 各项目**未安装** `pinia-plugin-persistedstate`，如需持久化需先安装

## 七、样式与移动端适配

postcss-pxtorem 配置差异：

| 项目 | postcss-pxtorem | 写法 |
|------|----------------|------|
| web/agent/web | 启用（rootValue: 16, propList: ['*']） | 按设计稿直接写 px，自动转 rem；禁止额外用 `var(--spacing-*)` 包裹间距/字号（会双重转换） |
| H5Portal | 未启用 | 用百分比/flex/grid/media query/rem 手动换算；间距字号用 `var(--spacing-*)` |
| admin | 未启用 | PC 端为主，按固定布局写 px |

- web/agent/web: 需保留 px 不转换时用大写 `PX` 或在 selectorBlackList 加选择器
- 颜色/圆角统一用 `var(--color-*)` / `var(--radius-*)` 设计变量

## 八、Lint 配置差异（避免跨项目套用）

### ESLint

| 项目 | 配置格式 | 核心 |
|------|---------|------|
| H5Portal | eslint.config.js (flat) | vue、@typescript-eslint、import、prettier；仅显式设置少量规则（no-console/no-debugger、import/order、no-unused-vars 不忽略 `_` 前缀）；未启用 eqeqeq/object-shorthand/no-var |
| web/agent/web | eslint.config.mjs (flat) → @cs/eslint-config | SFC 块顺序 script→template→style；宏顺序 defineOptions→defineProps→defineEmits→defineSlots；允许 any（渐进迁移）；未使用 TS 变量 `_` 前缀忽略 |
| admin | .eslintrc.js (legacy) | Vue2，plugin:vue/recommended + eslint:recommended |

### Prettier

| 项目 | 配置 | 关键差异 |
|------|------|---------|
| H5Portal | .prettierrc.cjs | printWidth 100, singleQuote, semi true, htmlWhitespaceSensitivity 'css' |
| web/agent/web | .prettierrc.mjs → @cs/prettier-config | htmlWhitespaceSensitivity 'strict', proseWrap 'never' |
| admin | .prettierrc | **semi: false**（不加分号，与其他项目不一致），printWidth 默认 80 |

### Stylelint

| 项目 | 配置 | 关键差异 |
|------|------|---------|
| H5Portal | stylelint.config.js | SCSS，standard + standard-scss，未集成 stylelint-prettier |
| web/ | stylelint.config.mjs → @cs/stylelint-config | Less，recess-order 排序，selector-class-pattern: null |
| agent/web/ | stylelint.config.mjs → @cs/stylelint-config | Less，**BEM 正则强约束**，与 web/ 配置不完全相同 |
| admin | 无 Stylelint | 不参与 lint |

⚠️ web/ 与 agent/web/ 各自维护一份 `config/stylelint-config/index.mjs`，**两者规则不完全相同**，不要视为同一份配置。

## 九、Vue3 项目专项约定（web/agent/web/H5Portal）

- `<script setup>`，web/agent/web 强制 `lang="ts"`，H5Portal 以 JS 为主、新代码鼓励 TS
- SFC 块顺序、宏定义顺序、`defineOptions({ name })`、emits/props 声明方式等统一编码规则见 §13

## 十、admin（Vue2）专项约定

- Options API（data/methods/computed/watch），复用逻辑用 `src/mixins/`
- 文件统一 JavaScript，不强制 TypeScript
- SFC 顺序仍为 script → template → style，样式用 `lang="scss"`
- Vuex（src/store/，按 modules 拆分），不引入 Pinia
- 路由 Vue Router 3.x
- 新增功能优先评估迁移到 Vue3 项目，而非在 admin 内扩建
- 组件目录形式（`components/<Name>/index.vue`）、布局/指令/图标目录命名（单数）见 §5、§12；Vue 编码核心规则见 §13

## 十一、关键依赖现状（影响编码决策）

- **DOMPurify**: 4 个项目**均未安装**，未安装时禁止使用 v-html 渲染任何外部/用户内容；如需富文本须先安装
- **Element Plus**（web/agent/web）: 当前在 `src/hooks/useCreateApp/index.ts` 中 `app.use(ElementPlus)` **全量注册**并引入全量 CSS；新增功能不要扩大开销，未迁移到按需注册前不要在 lint 层强制按需
- **pinia-plugin-persistedstate**: 各项目未安装（持久化需求见 §6）

## 十二、项目目录结构

### web/
```
src/{api,assets,components,composables,data,hooks,pages,router,store,styles,types,utils}
types/                # #/* 别名目标 — 全局/跨模块类型
App.vue, main.ts
```

### agent/web/
```
src/{api,assets,components,directives,enums,hooks,locales,router,store,styles,utils,views}
App.vue, main.ts
```

### H5Portal
```
src/{common/{api,network,utils},components,config,hooks,pages,pagesMain,plugins,router,stores,utils}
App.vue, main.js
```

### admin
```
src/{api,assets,components,directive,icons,layout,locales,mixins,router,store,styles,utils,views}
App.vue, main.js
```

## 十三、Vue 编码项目差异点

> Vue3（web/agent/web/H5Portal）与 Vue2（admin）的通用核心编码规则（SFC 块顺序、宏定义顺序、命名规范、组件名 casing、懒加载、路由 meta 字段、API 模块化、类型命名后缀等）由 `frontend-guidelines` 技能的 `vue/` 子目录按需加载，本节仅记录**项目间差异**，避免重复。

### 13.1 SFC 与样式语言

| 项目 | SFC 写法 | 样式语言 |
|------|---------|---------|
| web/agent/web | `<script setup lang="ts">` | `lang="less"` |
| H5Portal | `<script setup>`（JS 为主，新代码鼓励 TS） | `lang="scss"` |
| admin | Options API（data/methods/computed/watch） | `lang="scss"` |

- 组件 `defineOptions({ name: 'XxxYyy' })`（Vue3 项目，与 eslint `vue/component-definition-name-casing` 对齐）

### 13.2 Props / Emits 声明方式

- **web/agent/web**: 强制 TS 类型声明 `defineProps<{ ... }>()` + `withDefaults`；emits 用 `defineEmits<{ (e: 'update', id: number): void }>()`
- **H5Portal（JS）**: 运行时声明 `{ type, required, default, validator }`；emits 允许 `defineEmits(['update', 'delete'])`，建议逐步补类型
- **admin**: Options API 的 `props`/`emits` 选项式声明
- 所有项目必须显式声明 emits（与 eslint `vue/require-explicit-emits` 对齐）

### 13.3 Composables / Hooks 目录组织

| 项目 | 目录 | 组织形式 |
|------|------|---------|
| web/ / H5Portal | `src/hooks/` | `useXxx.ts` 单文件 |
| agent/web/ | `src/hooks/` | `useXxx/index.ts` 文件夹模式 + `hooks/index.ts` 聚合导出 |
| admin | `src/mixins/` | Options API mixin |

- 功能模块内部可建 `composables/` 子目录（如 `pages/dutySchedule/composables/useDutySchedule.ts`）

### 13.4 状态管理

| 项目 | 方案 | 现状风格 |
|------|------|---------|
| web/ | Pinia | Options Store 为主 |
| H5Portal/ | Pinia | Setup Store 为主 |
| admin | Vuex（`src/store/`，按 modules 拆分） | 不引入 Pinia |

- 新增 Store 优先 Setup Store，同项目内保持统一
- 敏感数据（token、密码）禁止持久化；持久化需先安装 `pinia-plugin-persistedstate`（见 §11）

### 13.5 类型定义（TS 项目: web/agent/web）

- 类型 import: web/ 用 `#/xxx` 别名（types/ 在项目根），agent/web 按 tsconfig paths
- 新增 API 响应必须定义类型，禁止 `Promise<any>`（渐进迁移项目允许 `any`，但新增 API 响应不允许）
- H5Portal 新增 TS 文件遵循本节；历史 JS 文件不强制改造；admin 不适用

> 完整类型命名后缀（Params/Result/Form 等）、interface vs type 选择等通用规则详见 `frontend-guidelines` 技能
