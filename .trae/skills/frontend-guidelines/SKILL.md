---
name: frontend-guidelines
description: "Linkx 仓库前端任务与 Vue 任务的唯一规范入口。强触发: .vue/.ts/.tsx/.js/.jsx/.less/.scss/.css/.html 等前端文件，web/、agent/web/、H5Portal/、cloudcmd-admin-web/ 目录，vue/Vue2/Vue3/Vite/组件/SFC/script setup/Composition API/Options API/ref/reactive/computed/watch/props/emits/v-model/slot/composables/hooks/Pinia/Vuex/vue-router/Transition/Teleport/KeepAlive/Suspense/指令/插件/SSR 等关键词。覆盖: Vue3/Vue2 编码规范（含响应式/SFC/组件数据流/composables 通用知识）、ESLint/Prettier/Stylelint 配置、性能优化、安全审查、业务交互。"
alwaysApply: false
---

# 前端规范工作流

本技能是 Linkx 项目前端规范与 Vue 最佳实践的**唯一入口**。项目特有约定（目录、别名、HTTP 入口、依赖现状）+ Vue 编码核心规则见 `.trae/rules/frontend.md`，本技能承载 Vue 通用知识（`references/vue/` 子目录）、Lint 配置、性能/安全教程、业务交互规范。

## 核心原则

- **保持状态可预测：** 单一数据源，其他所有数据都从中派生。
- **使数据流显式化：** 大多数情况下，Props 向下传递，Events 向上冒泡。
- **偏好小型、专注的组件：** 更易于测试、复用和维护。
- **避免不必要的重渲染：** 明智地使用计算属性和侦听器。
- **可读性很重要：** 编写清晰、自文档化的代码。

## 触发场景

任一命中即加载：
- 任务涉及 `.vue`/`.ts`/`.tsx`/`.js`/`.jsx`/`.less`/`.scss`/`.css`/`.html` 等前端文件
- 任务涉及 `web/`、`agent/web/`、`H5Portal/`、`cloudcmd-admin-web/` 任一目录
- 用户提及 Vue/Vite/组件/Pinia/Vuex/vue-router 等 Vue 相关概念
- 写/改 Vue 组件、Lint 配置查询、前端性能优化或安全审查、表单/列表/防抖节流等业务交互

不应触发：纯后端任务（→ `backend-guidelines`）、纯运维任务。

## 工作流

### Step 1: 确认目标项目与架构

1. **确认目标项目**（web/agent/web/H5Portal/admin），项目栈差异见 `.trae/rules/frontend.md` 第一章。不同项目的 Vue 版本、UI 框架、Lint 配置、依赖现状均不同，禁止跨项目套用规则。
2. **确认架构**：
   - 默认技术栈：Vue 3 + Composition API + `<script setup lang="ts">`。
   - admin 项目使用 Vue 2 + Options API，遵循 `.trae/rules/frontend.md` §10 admin 专项 + §13 Vue 编码核心规则。
   - 如果项目明确使用 JSX，参考 `vue/render-functions.md`。

### Step 2: 必读核心参考

在实现任何 Vue 任务之前，确保阅读并应用这些核心参考：

- `vue/reactivity.md` — 响应式
- `vue/sfc.md` — SFC 结构和模板安全
- `vue/component-data-flow.md` — 组件数据流
- `vue/composables.md` — 可组合函数

Vue3 组件开发的项目特有写法（SFC 结构、宏顺序、Props/Emits、命名、composables、Pinia、路由、API、类型）已在 `.trae/rules/frontend.md` §13 始终加载，无需额外加载。在整个任务过程中将上述参考保持在活跃的工作上下文中。

### Step 3: 编码前规划组件边界

在实现任何非平凡功能之前创建简要的组件图。

- 用一句话定义每个组件的单一职责。
- 默认将入口/根组件和路由级视图组件保持为组合面。
- 将功能 UI 和功能逻辑从入口/根/视图组件中移出，除非任务是有意的小型单文件演示。
- 在图中为每个子组件定义 props/emits 契约。
- 当添加多个组件时，偏好功能文件夹布局（`components/<feature>/...`、`composables/use<Feature>.ts`）。

### Step 4: 应用基础 Vue 基石

这些是基础性的、必须掌握的基石。使用 Step 2 加载的核心参考，在每个 Vue 任务中应用所有这些基石。

**响应式**（参考 `vue/reactivity.md`）
- 保持源状态最小化（`ref`/`reactive`），尽可能用 `computed` 派生其他所有数据。
- 如需要，使用侦听器处理副作用。
- 避免在模板中重新计算昂贵的逻辑。

**SFC 结构和模板安全**（参考 `vue/sfc.md`）
- 按此顺序保持 SFC 部分：`<script>` → `<template>` → `<style>`。
- `<script setup>` 内部顺序: 宏定义 → 组合式 API import → composables/hooks import → 响应式数据 → 计算属性 → 方法 → 生命周期。
- 宏定义顺序: `defineOptions` → `defineProps` → `defineEmits` → `defineSlots`。
- 组件必须 `defineOptions({ name: 'XxxYyy' })`（与 eslint `vue/component-definition-name-casing` 对齐）。
- 保持 SFC 职责专注；拆分大型组件。
- 保持模板声明式；将分支/派生移至脚本。
- 应用 Vue 模板安全规则（`v-html`、列表渲染、条件渲染选择）。

**项目特有写法**（参考 `.trae/rules/frontend.md` §13，始终加载）
- 样式语言: web/agent/web 用 `lang="less"`，H5Portal 用 `lang="scss"`。
- Props: web/agent/web 强制 TS 类型声明（`defineProps<{ ... }>()`），需要 default 值时用 `withDefaults`；H5Portal（JS）可使用运行时声明。
- Emits: 所有项目必须显式声明 emits（与 eslint `require-explicit-emits` 对齐）。
- 命名规范、composables、Pinia Store、路由、API 请求、类型定义等项目特有约定详见 `.trae/rules/frontend.md` §13。

**保持组件专注**（参考 `vue/component-data-flow.md`）
当组件有**多个明确的职责**时拆分它（例如数据编排 + UI，或多个独立的 UI 部分）。
- 偏好**更小的组件 + 可组合函数**，而不是一个"超级组件"。
- 将 **UI 部分**移入子组件（props 进，events 出）。
- 将 **状态/副作用**移入可组合函数（`useXxx()`）。

应用客观拆分触发器。如果满足**任意**条件，则拆分组件：
- 它同时拥有编排/状态和多个部分的实质性展示标记。
- 它有 3 个以上不同的 UI 部分（例如：表单、过滤器、列表、页脚/状态）。
- 模板块重复或可能变得可复用（项目行、卡片、列表条目）。

入口/根和路由视图规则：
- 保持入口/根和路由视图组件精简：应用外壳/布局、提供者连接和功能组合。
- 当这些功能包含独立部分时，不要将完整的功能实现放在入口/根/视图组件中。
- 对于 CRUD/列表功能（todo、表格、目录、收件箱），至少拆分为：功能容器组件、输入/表单组件、列表（和/或条目）组件、页脚/操作或过滤器/状态组件。
- 仅对非常小的一次性演示允许单文件实现；如果选择，明确说明为什么不需要拆分。

**可组合函数**（参考 `vue/composables.md`）
- 当逻辑可复用、有状态或副作用重时，将其提取到可组合函数中。
- 保持可组合函数 API 小型、类型化和可预测。
- 将功能逻辑与展示组件分离。

### Step 5: 按需加载可选功能 references

不要默认添加这些。仅在需求存在时加载匹配的参考。

**标准可选功能**
- 插槽：父组件需要控制子组件内容/布局 -> `vue/component-slots.md`
- 透传属性：包装器/基础组件必须安全地转发属性/事件 -> `vue/component-fallthrough-attrs.md`
- 内置组件 `<KeepAlive>` 用于有状态视图缓存 -> `vue/component-keep-alive.md`
- 内置组件 `<Teleport>` 用于覆盖层/传送门 -> `vue/component-teleport.md`
- 内置组件 `<Suspense>` 用于异步子树回退边界 -> `vue/component-suspense.md`
- 动画相关功能：选择匹配所需运动行为的最简单方法。
  - `<Transition>` 用于进入/离开效果 -> `vue/component-transition.md`
  - `<TransitionGroup>` 用于动画列表变更 -> `vue/component-transition-group.md`
  - 基于类的动画用于非进入/离开效果 -> `vue/animation-class-based-technique.md`
  - 状态驱动动画用于用户输入驱动的动画 -> `vue/animation-state-driven-technique.md`

**较不常见的可选功能**
- 指令：行为是 DOM 特定的，不适合可组合函数/组件 -> `vue/directives.md`
- 异步组件：重型/很少使用的 UI 应该懒加载 -> `vue/component-async.md`
- 渲染函数仅当模板无法表达需求时 -> `vue/render-functions.md`
- 插件当行为必须在整个应用范围内安装时 -> `vue/plugins.md`
- 状态管理模式：应用范围的共享状态跨越功能边界 -> `vue/state-management.md`

### Step 6: 行为正确后运行性能优化

性能工作是功能完成后的步骤。在核心行为实现并验证之前不要优化。

- 大列表渲染瓶颈 -> `vue/perf-virtualize-large-lists.md`
- 静态子树不必要地重渲染 -> `vue/perf-v-once-v-memo-directives.md`
- 热列表路径中的过度抽象 -> `vue/perf-avoid-component-abstraction-in-lists.md`
- 昂贵的更新触发太频繁 -> `vue/updated-hook-performance.md`
- 首屏加载、渲染优化、网络优化、构建优化、性能指标 -> `performance.md`

### Step 7: 最终自检

- 核心行为工作正常并匹配需求。
- Step 2 的必读参考已阅读并应用。
- 响应式模型最小化且可预测。
- SFC 结构和模板规则已遵循（块顺序、宏顺序、`defineOptions({ name })`）。
- 组件专注且结构良好，在需要时拆分。
- 入口/根和路由视图组件保持为组合面，除非有明确的小型演示例外。
- 组件拆分决策明确且可辩护（职责边界清晰）。
- 数据流契约明确且类型化。
- 可组合函数在复用/复杂性合理时使用。
- 状态/副作用已移入可组合函数（如适用）。
- 可选功能仅在需求要求时使用。
- 性能变更仅在功能完成后应用。
- 对照 `.trae/rules/frontend.md` 验证项目特定约定（目录、别名、HTTP 入口、组件组织方式）未被破坏。

## references 索引

### 项目特有规范

> Vue 编码核心规则（SFC 结构、宏顺序、Props/Emits、命名、composables、Pinia、路由、API、类型）已迁移至 `.trae/rules/frontend.md` §13，始终加载，本技能不再重复维护。

- `lint-config.md` — 4 个项目 ESLint/Prettier/Stylelint 完整配置对比与差异说明（含 web/ 与 agent/web/ 的 @cs/stylelint-config 不一致问题）
- `performance.md` — 前端性能优化详版（首屏加载、渲染优化、网络优化、构建优化、性能指标目标值与监控现状）
- `security.md` — 前端安全编码详版（XSS/CSRF/点击劫持/存储安全/依赖安全/HTTPS/敏感信息/文件上传/重定向/危险函数 + DOMPurify 未安装现状）
- `business-rules.md` — 前端业务交互规范详版（防抖节流时长表与代码、响应式样式与设计变量、postcss-pxtorem 配置、表单校验与重置、列表页规范）

### Vue 通用知识（`vue/` 子目录）

**必读 4 篇**（任何 Vue 任务前阅读）:
- `vue/reactivity.md`
- `vue/sfc.md`
- `vue/component-data-flow.md`
- `vue/composables.md`

**按需加载**:
- `vue/component-slots.md` — 插槽
- `vue/component-fallthrough-attrs.md` — 透传属性
- `vue/component-keep-alive.md` — KeepAlive
- `vue/component-teleport.md` — Teleport
- `vue/component-suspense.md` — Suspense
- `vue/component-transition.md` — Transition
- `vue/component-transition-group.md` — TransitionGroup
- `vue/animation-class-based-technique.md` — 基于类的动画
- `vue/animation-state-driven-technique.md` — 状态驱动动画
- `vue/directives.md` — 自定义指令
- `vue/component-async.md` — 异步组件
- `vue/render-functions.md` — 渲染函数
- `vue/plugins.md` — 插件
- `vue/state-management.md` — 状态管理模式
- `vue/perf-virtualize-large-lists.md` — 大列表虚拟化
- `vue/perf-v-once-v-memo-directives.md` — v-once/v-memo
- `vue/perf-avoid-component-abstraction-in-lists.md` — 列表中避免组件抽象
- `vue/updated-hook-performance.md` — updated 钩子性能

## 约束

1. 修改代码前必须确认目标项目，禁止跨项目混用风格。
2. references 按需加载，不要全量加载。
3. 项目特有约定（目录/别名/HTTP 入口/组件组织）以 `.trae/rules/frontend.md` 为准，本技能 references 不重复。
