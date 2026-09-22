---
title: Teleport 组件最佳实践
impact: MEDIUM
impactDescription: Teleport 在组件 DOM 位置外渲染内容，这对覆盖层至关重要但影响样式和布局
type: best-practice
tags: [vue3, teleport, modal, overlay, positioning, responsive]
---

# Teleport 组件最佳实践

**影响：中** - `<Teleport>` 在 DOM 的不同位置渲染组件模板的一部分，同时保留 Vue 组件层次结构。将其用于覆盖层（模态框、通知、工具提示）或任何必须脱离堆叠上下文、溢出或固定定位约束的 UI。

## 任务清单

- 将覆盖层传送到 `body` 或应用根外的专用容器
- 为类似 UI 保持共享目标（`#modals`、`#notifications`）并用顺序或 z-index 控制分层
- 使用 `:disabled` 实现响应式布局，在小屏幕上内联渲染
- 记住 props、emits 和 provide/inject 仍然通过传送工作
- 避免依赖父组件堆叠上下文或变换来处理传送的 UI

## 将覆盖层传送出变换容器

当祖先有 `transform`、`filter` 或 `perspective` 时，固定定位覆盖层的行为就像它们是本地定位的。Teleport 可以脱离该上下文。

**错误：**
```vue
<template>
  <div class="animated-container">
    <button @click="open = true">打开</button>

    <!-- 错误：固定定位范围限定在变换的父组件内 -->
    <div v-if="open" class="modal">模态框</div>
  </div>
</template>

<style>
.animated-container {
  transform: translateZ(0);
}

.modal {
  position: fixed;
  inset: 0;
  z-index: 9999;
}
</style>
```

**正确：**
```vue
<template>
  <div class="animated-container">
    <button @click="open = true">打开</button>

    <Teleport to="body">
      <div v-if="open" class="modal">模态框</div>
    </Teleport>
  </div>
</template>
```

## 使用 `disabled` 实现响应式布局

使用 `:disabled` 在移动端内联渲染，在更大屏幕上传送：

```vue
<script setup>
import { useMediaQuery } from '@vueuse/core'

const isMobile = useMediaQuery('(max-width: 768px)')
</script>

<template>
  <Teleport to="body" :disabled="isMobile">
    <nav class="sidebar">导航</nav>
  </Teleport>
</template>
```

## 逻辑层次结构保留

Teleport 改变 DOM 位置，而不是 Vue 组件树。Props、emits、插槽和 provide/inject 仍然工作：

```vue
<template>
  <Teleport to="body">
    <ChildPanel :message="message" @close="open = false" />
  </Teleport>
</template>
```

## 多个 Teleport 到同一目标

到同一目标的 Teleport 按声明顺序追加：

```vue
<template>
  <Teleport to="#notifications">
    <div>第一个</div>
  </Teleport>

  <Teleport to="#notifications">
    <div>第二个</div>
  </Teleport>
</template>
```

使用共享容器保持堆叠可预测，仅在需要显式分层时应用 z-index。
