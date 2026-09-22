---
title: 指令最佳实践
impact: MEDIUM
impactDescription: 自定义指令强大但容易误用；遵循模式可防止泄漏、无效用法和不清晰的抽象
type: best-practice
tags: [vue3, directives, custom-directives, composition, typescript]
---

# 指令最佳实践

**影响：中** - 指令用于低级 DOM 访问。谨慎使用，保持副作用安全，当需要有状态或可复用的 UI 行为时偏好组件或可组合函数。

## 任务清单

- 仅在需要直接 DOM 访问时使用指令
- 不要变更指令参数或绑定对象
- 在 `unmounted` 中清理定时器、监听器和观察器
- 在 `<script setup>` 中使用 `v-` 前缀注册指令
- 在 TypeScript 项目中，类型化指令值并增强模板指令类型
- 对复杂行为偏好组件或可组合函数

## 将指令参数视为只读

指令绑定不是响应式存储。不要写入它们。

```ts
const vFocus = {
  mounted(el, binding) {
    // binding.value 是只读的
    el.focus()
  }
}
```

## 避免在组件上使用指令

指令应用于 DOM 元素。在组件上使用时，它们附加到根元素，如果根元素变化可能会损坏。

**错误：**
```vue
<MyInput v-focus />
```

**正确：**
```vue
<!-- MyInput.vue -->
<script setup>
const vFocus = (el) => el.focus()
</script>

<template>
  <input v-focus />
</template>
```

## 在 `unmounted` 中清理副作用

任何定时器、监听器或观察器必须移除以避免泄漏。

```ts
const vResize = {
  mounted(el) {
    const observer = new ResizeObserver(() => {})
    observer.observe(el)
    el._observer = observer
  },
  unmounted(el) {
    el._observer?.disconnect()
  }
}
```

## 为单钩子指令偏好函数简写

如果只需要 `mounted`/`updated`，使用函数形式。

```ts
const vAutofocus = (el) => el.focus()
```

## 使用 `v-` 前缀和 Script Setup 注册

```vue
<script setup>
const vFocus = (el) => el.focus()
</script>

<template>
  <input v-focus />
</template>
```

## 在 TypeScript 项目中类型化自定义指令

使用 `Directive<Element, ValueType>` 让 `binding.value` 类型化，并增强 Vue 的模板类型以便指令在 SFC 模板中被识别。

**错误：**
```ts
// 无类型化的指令值且无模板类型增强
export const vHighlight = {
  mounted(el, binding) {
    el.style.backgroundColor = binding.value
  }
}
```

**正确：**
```ts
import type { Directive } from 'vue'

type HighlightValue = string

export const vHighlight = {
  mounted(el, binding) {
    el.style.backgroundColor = binding.value
  }
} satisfies Directive<HTMLElement, HighlightValue>

declare module 'vue' {
  interface ComponentCustomProperties {
    vHighlight: typeof vHighlight
  }
}
```

## 使用 `getSSRProps` 处理 SSR

`mounted` 和 `updated` 等指令钩子在 SSR 期间不运行。如果指令设置影响渲染 HTML 的属性/类，通过 `getSSRProps` 提供 SSR 等价物以避免水合不匹配。

**错误：**
```ts
const vTooltip = {
  mounted(el, binding) {
    el.setAttribute('data-tooltip', binding.value)
    el.classList.add('has-tooltip')
  }
}
```

**正确：**
```ts
const vTooltip = {
  mounted(el, binding) {
    el.setAttribute('data-tooltip', binding.value)
    el.classList.add('has-tooltip')
  },
  getSSRProps(binding) {
    return {
      'data-tooltip': binding.value,
      class: 'has-tooltip'
    }
  }
}
```

## 尽可能偏好声明式模板

如果标准属性或绑定可行，使用它而不是指令。

## 在指令和组件之间决定

为 DOM 级行为使用指令。当行为影响结构、状态或渲染时使用组件。
