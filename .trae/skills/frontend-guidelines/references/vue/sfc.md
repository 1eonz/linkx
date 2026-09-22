---
title: 单文件组件结构、样式和模板模式
impact: MEDIUM
impactDescription: 一致的 SFC 结构和样式选择改善可维护性、工具支持和渲染性能
type: best-practice
tags: [vue3, sfc, scoped-css, styles, build-tools, performance, template, v-html, v-for, computed, v-if, v-show]
---

# 单文件组件结构、样式和模板模式

**影响：中** - 使用具有一致结构和高性能样式的 SFC 使组件更易维护并避免不必要的渲染开销。

## 任务清单

- 使用 `.vue` SFC 而非分离的 `.js`/`.ts` 和 `.css` 文件用于组件
- 默认将模板、脚本和样式放在同一 SFC 中
- 在模板和文件名中为组件名使用 PascalCase
- 偏好组件作用域样式
- 在作用域 CSS 中偏好类选择器（而非元素选择器）以获得性能
- 在 Vue 3.5+ 中使用 `useTemplateRef()` 访问 DOM / 组件 refs
- 在 `:style` 绑定中使用 camelCase 键以保持一致性和 IDE 支持
- 正确使用 `v-for` 和 `v-if`
- 永远不要对不可信/用户提供的内容使用 `v-html`
- 根据切换频率和初始渲染成本选择 `v-if` vs `v-show`

## 将模板、脚本和样式放在同一位置

**错误：**
```
components/
├── UserCard.vue
├── UserCard.js
└── UserCard.css
```

**正确：**
```vue
<!-- components/UserCard.vue -->
<script setup>
import { computed } from 'vue'

const props = defineProps({
  user: { type: Object, required: true }
})

const displayName = computed(() =>
  `${props.user.firstName} ${props.user.lastName}`
)
</script>

<template>
  <div class="user-card">
    <h3 class="name">{{ displayName }}</h3>
  </div>
</template>

<style scoped>
.user-card {
  padding: 1rem;
}

.name {
  margin: 0;
}
</style>
```

## 为组件名使用 PascalCase

**错误：**
```vue
<script setup>
import userProfile from './user-profile.vue'
</script>

<template>
  <user-profile :user="currentUser" />
</template>
```

**正确：**
```vue
<script setup>
import UserProfile from './UserProfile.vue'
</script>

<template>
  <UserProfile :user="currentUser" />
</template>
```

## SFC 中 `<style>` 块的最佳实践

### 偏好组件作用域样式

- 对属于组件的样式使用 `<style scoped>`。
- 将**全局 CSS** 保留在专用文件中（例如 `src/assets/main.css`）用于重置、排版、令牌等。
- 慎用 `:deep()`（仅限边缘情况）。

**错误：**

```vue
<style>
/* ❌ 到处泄漏 */
button { border-radius: 999px; }
</style>
```

**正确：**

```vue
<style scoped>
.button { border-radius: 999px; }
</style>
```

**正确：**

```css
/* src/assets/main.css */
/* ✅ 重置、令牌、排版、应用范围规则 */
:root { --radius: 999px; }
```

### 在作用域 CSS 中使用类选择器

**错误：**
```vue
<template>
  <article>
    <h1>{{ title }}</h1>
    <p>{{ subtitle }}</p>
  </article>
</template>

<style scoped>
article { max-width: 800px; }
h1 { font-size: 2rem; }
p { line-height: 1.6; }
</style>
```

**正确：**
```vue
<template>
  <article class="article">
    <h1 class="article-title">{{ title }}</h1>
    <p class="article-subtitle">{{ subtitle }}</p>
  </article>
</template>

<style scoped>
.article { max-width: 800px; }
.article-title { font-size: 2rem; }
.article-subtitle { line-height: 1.6; }
</style>
```

## 使用 `useTemplateRef()` 访问 DOM / 组件 refs

对于 Vue 3.5+：使用 `useTemplateRef()` 访问模板 refs。

```vue
<script setup lang="ts">
import { onMounted, useTemplateRef } from 'vue'

const inputRef = useTemplateRef<HTMLInputElement>('input')

onMounted(() => {
  inputRef.value?.focus()
})
</script>

<template>
  <input ref="input" />
</template>
```

## 在 `:style` 绑定中使用 camelCase

**错误：**
```vue
<template>
  <div :style="{ 'font-size': fontSize + 'px', 'background-color': bg }">
    内容
  </div>
</template>
```

**正确：**
```vue
<template>
  <div :style="{ fontSize: fontSize + 'px', backgroundColor: bg }">
    内容
  </div>
</template>
```

## 正确使用 `v-for` 和 `v-if`

### 始终提供稳定的 `:key`

- 偏好原始键（`string | number`）。
- 避免使用对象作为键。

**正确：**

```vue
<li v-for="item in items" :key="item.id">
  <input v-model="item.text" />
</li>
```

### 避免在同一元素上使用 `v-if` 和 `v-for`

这会导致意图不清和不必要的工作。
（[参考](https://vuejs.org/guide/essentials/list.html#v-for-with-v-if)）

**过滤项**
**错误：**

```vue
<li v-for="user in users" v-if="user.active" :key="user.id">
  {{ user.name }}
</li>
```

**正确：**

```vue
<script setup lang="ts">
import { computed } from 'vue'

const activeUsers = computed(() => users.value.filter(u => u.active))
</script>

<template>
  <li v-for="user in activeUsers" :key="user.id">
    {{ user.name }}
  </li>
</template>
```

**条件显示/隐藏整个列表**
**正确：**

```vue
<ul v-if="shouldShowUsers">
  <li v-for="user in users" :key="user.id">
    {{ user.name }}
  </li>
</ul>
```

## 永远不要用 `v-html` 渲染不可信 HTML

**错误：**
```vue
<template>
  <!-- 危险：不可信输入可以注入脚本 -->
  <article v-html="userProvidedContent"></article>
</template>
```

**正确：**
```vue
<script setup>
import { computed } from 'vue'
import DOMPurify from 'dompurify'

const props = defineProps<{
  trustedHtml?: string
  plainText: string
}>()

const safeHtml = computed(() => DOMPurify.sanitize(props.trustedHtml ?? ''))
</script>

<template>
  <!-- 首选：转义插值 -->
  <p>{{ props.plainText }}</p>

  <!-- 仅用于可信/已净化的 HTML -->
  <article v-html="safeHtml"></article>
</template>
```

## 根据切换行为选择 `v-if` vs `v-show`

**错误：**
```vue
<template>
  <!-- 频繁切换用 v-if 导致重复挂载/卸载 -->
  <ComplexPanel v-if="isPanelOpen" />

  <!-- 很少显示的内容用 v-show 支付初始渲染成本 -->
  <AdminPanel v-show="isAdmin" />
</template>
```

**正确：**
```vue
<template>
  <!-- 频繁切换：保持在 DOM 中，切换显示 -->
  <ComplexPanel v-show="isPanelOpen" />

  <!-- 罕见条件：仅当为真时懒渲染 -->
  <AdminPanel v-if="isAdmin" />
</template>
```
