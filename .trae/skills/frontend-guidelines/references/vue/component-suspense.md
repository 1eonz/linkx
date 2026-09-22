---
title: Suspense 组件最佳实践
impact: MEDIUM
impactDescription: Suspense 协调异步依赖与回退 UI；错误配置会导致缺失加载状态或混乱的 UX
type: best-practice
tags: [vue3, suspense, async-components, async-setup, loading, fallback, router, transition, keepalive]
---

# Suspense 组件最佳实践

**影响：中** - `<Suspense>` 协调异步依赖（异步组件或异步 setup）并在它们解析时渲染回退。错误配置会导致缺失加载状态、空渲染或微妙的 UX 错误。

## 任务清单

- 将默认和回退插槽内容包装在单个根节点中
- 当需要回退在恢复时出现时使用 `timeout`
- 当需要 Suspense 重新触发时使用 `:key` 强制根替换
- 为嵌套 Suspense 边界添加 `suspensible`（Vue 3.3+）
- 使用 `@pending`、`@resolve` 和 `@fallback` 进行编程式加载状态
- 按此顺序嵌套 `RouterView` -> `Transition` -> `KeepAlive` -> `Suspense`
- 在生产环境中保持 Suspense 使用集中且有文档记录

## 默认和回退插槽中的单根

Suspense 在两个插槽中都追踪单个直接子元素。将多个元素包装在单个元素或组件中。

**错误：**
```vue
<template>
  <Suspense>
    <AsyncHeader />
    <AsyncList />

    <template #fallback>
      <LoadingSpinner />
      <LoadingHint />
    </template>
  </Suspense>
</template>
```

**正确：**
```vue
<template>
  <Suspense>
    <div>
      <AsyncHeader />
      <AsyncList />
    </div>

    <template #fallback>
      <div>
        <LoadingSpinner />
        <LoadingHint />
      </div>
    </template>
  </Suspense>
</template>
```

## 恢复时的回退时机（`timeout`）

当 Suspense 已解析且新的异步工作开始时，之前的内容保持可见直到超时结束。使用 `timeout="0"` 立即回退或短延迟避免闪烁。

**错误：**
```vue
<template>
  <Suspense>
    <component :is="currentView" :key="viewKey" />

    <template #fallback>
      加载中...
    </template>
  </Suspense>
</template>
```

**正确：**
```vue
<template>
  <Suspense :timeout="200">
    <component :is="currentView" :key="viewKey" />

    <template #fallback>
      加载中...
    </template>
  </Suspense>
</template>
```

## 待定状态仅在根替换时重新触发

一旦解析，Suspense 仅在默认插槽的根节点变化时重新进入待定状态。如果异步工作发生在树深处，不会出现回退。

**错误：**
```vue
<template>
  <Suspense>
    <TabContainer>
      <AsyncDashboard v-if="tab === 'dashboard'" />
      <AsyncSettings v-else />
    </TabContainer>

    <template #fallback>
      加载中...
    </template>
  </Suspense>
</template>
```

**正确：**
```vue
<template>
  <Suspense>
    <component :is="tabs[tab]" :key="tab" />

    <template #fallback>
      加载中...
    </template>
  </Suspense>
</template>
```

## 为嵌套 Suspense 使用 `suspensible`（Vue 3.3+）

嵌套 Suspense 边界需要在内层边界上使用 `suspensible`，以便父组件可以协调加载状态。没有它，内部异步内容在解析前可能渲染空节点。

**错误：**
```vue
<template>
  <Suspense>
    <LayoutShell>
      <Suspense>
        <AsyncWidget />
        <template #fallback>加载组件...</template>
      </Suspense>
    </LayoutShell>

    <template #fallback>加载布局...</template>
  </Suspense>
</template>
```

**正确：**
```vue
<template>
  <Suspense>
    <LayoutShell>
      <Suspense suspensible>
        <AsyncWidget />
        <template #fallback>加载组件...</template>
      </Suspense>
    </LayoutShell>

    <template #fallback>加载布局...</template>
  </Suspense>
</template>
```

## 使用 Suspense 事件追踪加载

使用 `@pending`、`@resolve` 和 `@fallback` 进行分析、全局加载指示器或协调 Suspense 边界外的 UI。

```vue
<script setup>
import { ref } from 'vue'

const isLoading = ref(false)

const onPending = () => {
  isLoading.value = true
}

const onResolve = () => {
  isLoading.value = false
}
</script>

<template>
  <LoadingBar v-if="isLoading" />

  <Suspense @pending="onPending" @resolve="onResolve">
    <AsyncPage />
    <template #fallback>
      <PageSkeleton />
    </template>
  </Suspense>
</template>
```

## RouterView、Transition、KeepAlive 的推荐嵌套

组合这些组件时，嵌套顺序应该是 `RouterView` -> `Transition` -> `KeepAlive` -> `Suspense`，以便每个包装器正确工作。

**错误：**
```vue
<template>
  <RouterView v-slot="{ Component }">
    <Suspense>
      <KeepAlive>
        <Transition mode="out-in">
          <component :is="Component" />
        </Transition>
      </KeepAlive>
    </Suspense>
  </RouterView>
</template>
```

**正确：**
```vue
<template>
  <RouterView v-slot="{ Component }">
    <Transition mode="out-in">
      <KeepAlive>
        <Suspense>
          <component :is="Component" />
          <template #fallback>加载中...</template>
        </Suspense>
      </KeepAlive>
    </Transition>
  </RouterView>
</template>
```

## 在生产环境中谨慎对待 Suspense

在生产代码中，保持 Suspense 边界最小，记录它们的使用位置，并在需要替换或重构时有回退加载策略。
