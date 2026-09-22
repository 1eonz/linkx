---
title: KeepAlive 组件最佳实践
impact: HIGH
impactDescription: KeepAlive 缓存组件实例；误用会导致过期数据、内存增长或意外的生命周期行为
type: best-practice
tags: [vue3, keepalive, cache, performance, router, dynamic-components]
---

# KeepAlive 组件最佳实践

**影响：高** - `<KeepAlive>` 缓存组件实例而不是销毁它们。使用它来在切换时保留状态，但要显式管理缓存大小和新鲜度以避免内存增长或过期 UI。

## 任务清单

- 仅在状态保留改善 UX 的地方使用 KeepAlive
- 设置合理的 `max` 以限制缓存大小
- 为 include/exclude 匹配声明组件名称
- 使用 `onActivated`/`onDeactivated` 处理缓存感知逻辑
- 决定缓存视图如何以及何时刷新其数据
- 避免缓存内存密集型或安全敏感的视图

## 何时使用 KeepAlive

在需要持久化状态的视图之间切换时使用 KeepAlive（标签页、多步骤表单、仪表板）。当每次访问应该从头开始时避免使用。

**错误：**
```vue
<template>
  <!-- 每次切换时状态重置 -->
  <component :is="currentTab" />
</template>
```

**正确：**
```vue
<template>
  <!-- 切换时状态保留 -->
  <KeepAlive>
    <component :is="currentTab" />
  </KeepAlive>
</template>
```

## 何时不使用 KeepAlive

- 用户期望新结果的搜索或过滤页面
- 内存密集型组件（地图、大型表格、媒体播放器）
- 退出时必须清除数据的敏感流程
- 无法暂停的重型后台活动组件

## 限制和控制缓存

始终使用 `max` 限制缓存大小，并尽可能将缓存限制为特定组件。

```vue
<template>
  <KeepAlive :max="5" include="Dashboard,Settings">
    <component :is="currentView" />
  </KeepAlive>
</template>
```

## 确保组件名称匹配 include/exclude

`include` 和 `exclude` 匹配组件 `name` 选项。显式设置名称以获得可靠的缓存。

```vue
<!-- TabA.vue -->
<script setup>
defineOptions({ name: 'TabA' })
</script>
```

```vue
<template>
  <KeepAlive include="TabA,TabB">
    <component :is="currentTab" />
  </KeepAlive>
</template>
```

## 缓存失效策略

Vue 3 没有直接 API 来移除特定缓存实例。使用键或动态 include/exclude 来强制刷新。

```vue
<script setup>
import { ref, reactive } from 'vue'

const currentView = ref('Dashboard')
const viewKeys = reactive({ Dashboard: 0, Settings: 0 })

function invalidateCache(view) {
  viewKeys[view]++
}
</script>

<template>
  <KeepAlive>
    <component :is="currentView" :key="`${currentView}-${viewKeys[currentView]}`" />
  </KeepAlive>
</template>
```

## 缓存组件的生命周期钩子

缓存组件在切换时不会被销毁。使用激活钩子进行刷新和清理。

```vue
<script setup>
import { onActivated, onDeactivated } from 'vue'

onActivated(() => {
  refreshData()
})

onDeactivated(() => {
  pauseTimers()
})
</script>
```

## 路由缓存和新鲜度

决定导航应该显示缓存状态还是新视图。常见模式是当参数变化时按键路由。

```vue
<template>
  <router-view v-slot="{ Component, route }">
    <KeepAlive>
      <component :is="Component" :key="route.fullPath" />
    </KeepAlive>
  </router-view>
</template>
```

如果需要缓存复用但数据新鲜，在 `onActivated` 中刷新并在获取前比较 query/params。
