---
title: 响应式核心模式（ref、reactive、shallowRef、computed、watch）
impact: MEDIUM
impactDescription: 清晰的响应式选择保持状态可预测并减少 Vue 3 应用中不必要的更新
type: efficiency
tags: [vue3, reactivity, ref, reactive, shallowRef, computed, watch, watchEffect, external-state, best-practice]
---

# 响应式核心模式（ref、reactive、shallowRef、computed、watch）

**影响：中** - 首先选择正确的响应式原语，用 `computed` 派生，仅对副作用使用侦听器。

此参考涵盖本地状态、外部数据、派生值和效果的核心响应式决策。

## 任务清单

- 正确声明响应式状态
  - 对原始值始终使用 `shallowRef()` 而不是 `ref()` 以获得更好性能
  - 为对象/数组/Map/Set 选择正确的响应式声明方法
- 遵循 `reactive` 最佳实践
  - 避免直接从 `reactive()` 解构
  - 为 `reactive` 正确侦听
- 遵循 `computed` 最佳实践
  - 偏好 `computed` 而非侦听器赋值的派生 ref
  - 将过滤/排序派生保持在模板外
  - 使用 `computed` 处理可复用的类/样式逻辑
  - 保持计算 getter 纯净（无副作用）并将副作用放在侦听器中
- 遵循侦听器最佳实践
  - 使用 `immediate: true` 代替重复的初始调用
  - 为侦听器清理异步效果

## 正确声明响应式状态

### 对原始值（字符串、数字、布尔值、null 等）始终使用 `shallowRef()` 而不是 `ref()` 以获得更好性能。

**错误：**
```ts
import { ref } from 'vue'
const count = ref(0)
```

**正确：**
```ts
import { shallowRef } from 'vue'
const count = shallowRef(0)
```

### 为对象/数组/Map/Set 选择正确的响应式声明方法

当你经常**替换整个值**（`state.value = newObj`）但仍想内部深度响应式时使用 `ref()`，通常用于：

- 频繁重新赋值的状态（替换获取的对象/列表、重置为默认值、切换预设）。
- 更新主要通过 `.value` 重新赋值发生的可组合函数返回值。

当你主要**变更属性**且完整替换不常见时使用 `reactive()`，通常用于：

- "单一状态对象"模式（存储/表单）：`state.count++`、`state.items.push(...)`、`state.user.name = ...`。
- 想避免 `.value` 并就地更新嵌套字段的情况。

```ts
import { reactive } from 'vue'

const state = reactive({
  count: 0,
  user: { name: 'Alice', age: 30 }
})

state.count++ // ✅ 响应式
state.user.age = 31 // ✅ 响应式
// ❌ 避免替换响应式对象引用：
// state = reactive({ count: 1 })
```

当值是**不透明/不应被代理的**（类实例、外部库对象、非常大的嵌套数据）且你只想在**替换** `state.value` 时触发更新（无深度追踪）时使用 `shallowRef()`，通常用于：

- 存储外部实例/句柄（SDK 客户端、类实例）而不让 Vue 代理内部。
- 通过替换根引用更新的大数据（不可变风格更新）。

```ts
import { shallowRef } from 'vue'

const user = shallowRef({ name: 'Alice', age: 30 })

user.value.age = 31 // ❌ 不响应式
user.value = { name: 'Bob', age: 25 } // ✅ 触发更新
```

当你只想**顶层属性**响应式时使用 `shallowReactive()`；嵌套对象保持原始状态，通常用于：

- 只有顶层键变化的容器对象，嵌套载荷应保持非管理/非代理。
- Vue 追踪包装对象但不深度追踪嵌套或外部对象的混合结构。

```ts
import { shallowReactive } from 'vue'

const state = shallowReactive({
  count: 0,
  user: { name: 'Alice', age: 30 }
})

state.count++ // ✅ 响应式
state.user.age = 31 // ❌ 不响应式
```

## `reactive` 最佳实践

### 避免直接从 `reactive()` 解构

**错误：**

```ts
import { reactive } from 'vue'

const state = reactive({ count: 0 })
const { count } = state // ❌ 与响应式断开连接
```

### 为 reactive 正确侦听

**错误：**

向 `watch()` 传递非 getter 值

```ts
import { reactive, watch } from 'vue'

const state = reactive({ count: 0 })

// ❌ watch 期望 getter、ref、reactive 对象或这些的数组
watch(state.count, () => { /* ... */ })
```

**正确：**

用 `toRefs()` 保留响应式并为 `watch()` 使用 getter

```ts
import { reactive, toRefs, watch } from 'vue'

const state = reactive({ count: 0 })
const { count } = toRefs(state) // ✅ count 是 ref

watch(count, () => { /* ... */ }) // ✅
watch(() => state.count, () => { /* ... */ }) // ✅
```

## `computed` 最佳实践

### 偏好 `computed` 而非侦听器赋值的派生 ref

**错误：**
```ts
import { ref, watchEffect } from 'vue'

const items = ref([{ price: 10 }, { price: 20 }])
const total = ref(0)

watchEffect(() => {
  total.value = items.value.reduce((sum, item) => sum + item.price, 0)
})
```

**正确：**
```ts
import { ref, computed } from 'vue'

const items = ref([{ price: 10 }, { price: 20 }])
const total = computed(() =>
  items.value.reduce((sum, item) => sum + item.price, 0)
)
```

### 将过滤/排序派生保持在模板外

**错误：**
```vue
<template>
  <li v-for="item in items.filter(item => item.active)" :key="item.id">
    {{ item.name }}
  </li>

  <li v-for="item in getSortedItems()" :key="item.id">
    {{ item.name }}
  </li>
</template>

<script setup>
import { ref } from 'vue'

const items = ref([
  { id: 1, name: 'B', active: true },
  { id: 2, name: 'A', active: false }
])

function getSortedItems() {
  return [...items.value].sort((a, b) => a.name.localeCompare(b.name))
}
</script>
```

**正确：**
```vue
<script setup>
import { ref, computed } from 'vue'

const items = ref([
  { id: 1, name: 'B', active: true },
  { id: 2, name: 'A', active: false }
])

const visibleItems = computed(() =>
  items.value
    .filter(item => item.active)
    .sort((a, b) => a.name.localeCompare(b.name))
)
</script>

<template>
  <li v-for="item in visibleItems" :key="item.id">
    {{ item.name }}
  </li>
</template>
```

### 使用 `computed` 处理可复用的类/样式逻辑

**错误：**
```vue
<template>
  <button :class="{ btn: true, 'btn-primary': type === 'primary' && !disabled, 'btn-disabled': disabled }">
    {{ label }}
  </button>
</template>
```

**正确：**
```vue
<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'primary' },
  disabled: Boolean,
  label: String
})

const buttonClasses = computed(() => ({
  btn: true,
  [`btn-${props.type}`]: !props.disabled,
  'btn-disabled': props.disabled
}))
</script>

<template>
  <button :class="buttonClasses">
    {{ label }}
  </button>
</template>
```

### 保持计算 getter 纯净（无副作用），将副作用放在侦听器中

计算 getter 应该只派生值。无变更、无 API 调用、无存储写入、无事件发射。
（[参考](https://vuejs.org/guide/essentials/computed.html#best-practices)）

**错误：**

计算内部有副作用

```ts
const count = ref(0)

const doubled = computed(() => {
  // ❌ 副作用
  if (count.value > 10) console.warn('太大！')
  return count.value * 2
})
```

**正确：**

纯净 computed + `watch()` 处理副作用

```ts
const count = ref(0)
const doubled = computed(() => count.value * 2)

watch(count, (value) => {
  if (value > 10) console.warn('太大！')
})
```

## 侦听器最佳实践

### 使用 `immediate: true` 代替重复的初始调用

**错误：**
```ts
import { ref, watch, onMounted } from 'vue'

const userId = ref(1)

function loadUser(id) {
  // ...
}

onMounted(() => loadUser(userId.value))
watch(userId, (id) => loadUser(id))
```

**正确：**
```ts
import { ref, watch } from 'vue'

const userId = ref(1)

watch(
  userId,
  (id) => loadUser(id),
  { immediate: true }
)
```

### 为侦听器清理异步效果

当响应快速变化（搜索框、过滤器）时，取消之前的请求。

**正确：**

```ts
const query = ref('')
const results = ref<string[]>([])

watch(query, async (q, _prev, onCleanup) => {
  const controller = new AbortController()
  onCleanup(() => controller.abort())

  const res = await fetch(`/api/search?q=${encodeURIComponent(q)}`, {
    signal: controller.signal,
  })

  results.value = await res.json()
})
```
