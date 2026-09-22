---
title: 状态管理策略
impact: HIGH
impactDescription: 选择错误的存储模式会导致 SSR 请求泄漏、脆弱的变更流和糟糕的扩展性
type: best-practice
tags: [vue3, state-management, pinia, composables, ssr, vueuse]
---

# 状态管理策略

**影响：高** - 使用适合应用架构的最轻量状态解决方案。仅 SPA 应用可以使用轻量全局可组合函数，而 SSR/Nuxt 应用应该默认使用 Pinia 以获得请求安全隔离和可预测的工具。

## 任务清单

- 首先保持状态本地，然后仅在需要时提升到共享/全局
- 仅在非 SSR 应用中使用单例可组合函数
- 将全局状态暴露为只读并通过显式操作变更
- 为 SSR/Nuxt、大型应用和高级调试/插件需求偏好 Pinia
- 避免直接导出可变的模块级响应式状态

## 选择最轻量的存储方法

- **功能可组合函数：** 有本地/功能级状态的可复用逻辑的默认选择。
- **单例可组合函数或 VueUse `createGlobalState`：** 需要共享应用状态的小型非 SSR 应用。
- **Pinia：** SSR/Nuxt 应用、中大型应用，以及需要 DevTools、插件或操作追踪的情况。

## 避免导出可变的模块状态

**错误：**
```ts
// store/cart.ts
import { reactive } from 'vue'

export const cart = reactive({
  items: [] as Array<{ id: string; qty: number }>
})
```

**正确：**
```ts
// composables/useCartStore.ts
import { reactive, readonly } from 'vue'

let _store: ReturnType<typeof createCartStore> | null = null

function createCartStore() {
  const state = reactive({
    items: [] as Array<{ id: string; qty: number }>
  })

  function addItem(id: string, qty = 1) {
    const existing = state.items.find((item) => item.id === id)
    if (existing) {
      existing.qty += qty
      return
    }
    state.items.push({ id, qty })
  }

  return {
    state: readonly(state),
    addItem
  }
}

export function useCartStore() {
  if (!_store) _store = createCartStore()
  return _store
}
```

## 不要在 SSR 中使用运行时单例

模块单例在运行时生命周期内存在。在 SSR 中这可能在请求间泄漏状态。

**错误：**
```ts
// 跨请求复用的共享单例
const cartStore = useCartStore()

export function useServerCart() {
  return cartStore
}
```

**正确：**

> 需要 `pinia` 依赖。

```ts
// stores/cart.ts
import { defineStore } from 'pinia'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [] as Array<{ id: string; qty: number }>
  }),
  actions: {
    addItem(id: string, qty = 1) {
      const existing = this.items.find((item) => item.id === id)
      if (existing) {
        existing.qty += qty
        return
      }
      this.items.push({ id, qty })
    }
  }
})
```

## 使用 `createGlobalState` 处理小型 SPA 全局状态

> 需要 `@vueuse/core` 依赖。

如果应用非 SSR 且已使用 VueUse，`createGlobalState` 可移除单例样板代码。

```ts
import { createGlobalState } from '@vueuse/core'
import { computed, ref } from 'vue'

export const useAuthState = createGlobalState(() => {
  const token = ref<string | null>(null)
  const isAuthenticated = computed(() => token.value !== null)

  function setToken(next: string | null) {
    token.value = next
  }

  return {
    token,
    isAuthenticated,
    setToken
  }
})
```
