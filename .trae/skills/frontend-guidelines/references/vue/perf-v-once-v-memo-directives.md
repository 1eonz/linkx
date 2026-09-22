---
title: 使用 v-once 和 v-memo 跳过不必要的更新
impact: MEDIUM
impactDescription: v-once 为静态内容跳过所有未来更新；v-memo 条件性地记忆化子树
type: efficiency
tags: [vue3, performance, v-once, v-memo, optimization, directives]
---

# 使用 v-once 和 v-memo 跳过不必要的更新

**影响：中** - Vue 在每次响应式变更时重新评估模板。对于永不变化或很少变化的内容，`v-once` 和 `v-memo` 告诉 Vue 跳过更新，减少渲染工作。

对真正静态的内容使用 `v-once`，对列表中条件静态的内容使用 `v-memo`。

## 任务清单

- 对使用运行时数据但永不需要更新的元素应用 `v-once`
- 对应该仅在特定条件变更时更新的列表项应用 `v-memo`
- 验证记忆化内容不需要响应其他状态变更
- 使用 Vue DevTools 分析以确认更新跳过

## v-once：渲染一次，永不更新

**错误：**
```vue
<template>
  <!-- 错误：在每次父组件重渲染时重新评估 -->
  <div class="terms-content">
    <h1>服务条款</h1>
    <p>版本：{{ termsVersion }}</p>
    <div v-html="termsContent"></div>
  </div>

  <!-- 此内容永不变化，但 Vue 每次渲染都检查它 -->
  <footer>
    <p>版权 {{ copyrightYear }} {{ companyName }}</p>
  </footer>
</template>
```

**正确：**
```vue
<template>
  <!-- 正确：渲染一次，所有未来更新跳过 -->
  <div class="terms-content" v-once>
    <h1>服务条款</h1>
    <p>版本：{{ termsVersion }}</p>
    <div v-html="termsContent"></div>
  </div>

  <!-- v-once 告诉 Vue 这永不需要更新 -->
  <footer v-once>
    <p>版权 {{ copyrightYear }} {{ companyName }}</p>
  </footer>
</template>

<script setup>
// 这些值在组件创建时设置一次
const termsVersion = '2.1'
const termsContent = fetchedTermsHTML
const copyrightYear = 2024
const companyName = 'Acme Corp'
</script>
```

## v-memo：列表的条件记忆化

**错误：**
```vue
<template>
  <!-- 错误：selectedId 变化时所有项重渲染 -->
  <div v-for="item in list" :key="item.id">
    <div :class="{ selected: item.id === selectedId }">
      <ExpensiveComponent :data="item" />
    </div>
  </div>
</template>
```

**正确：**
```vue
<template>
  <!-- 正确：项仅在其选择状态变更时重渲染 -->
  <div
    v-for="item in list"
    :key="item.id"
    v-memo="[item.id === selectedId]"
  >
    <div :class="{ selected: item.id === selectedId }">
      <ExpensiveComponent :data="item" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const list = ref([/* 很多项 */])
const selectedId = ref(null)

// 当 selectedId 变化时：
// - 只有之前选中的项重渲染（selected: true -> false）
// - 只有新选中的项重渲染（selected: false -> true）
// - 所有其他项被跳过（v-memo 值未变）
</script>
```

## 有多个依赖的 v-memo

```vue
<template>
  <!-- 仅当项的选择或编辑状态变更时重渲染 -->
  <div
    v-for="item in items"
    :key="item.id"
    v-memo="[item.id === selectedId, item.id === editingId]"
  >
    <ItemCard
      :item="item"
      :selected="item.id === selectedId"
      :editing="item.id === editingId"
    />
  </div>
</template>

<script setup>
const selectedId = ref(null)
const editingId = ref(null)
const items = ref([/* ... */])
</script>
```

## v-memo 与空数组 = v-once

```vue
<template>
  <!-- v-memo="[]" 等价于 v-once -->
  <div v-for="item in staticList" :key="item.id" v-memo="[]">
    {{ item.name }}
  </div>
</template>
```

## 何时不使用这些指令

```vue
<template>
  <!-- 不要：内容确实需要更新 -->
  <div v-once>
    <span>计数：{{ count }}</span>  <!-- count 不会更新！ -->
  </div>

  <!-- 不要：当子组件有自己的响应式状态 -->
  <div v-memo="[selected]">
    <InputField v-model="item.name" />  <!-- v-model 不能正常工作 -->
  </div>

  <!-- 不要：当记忆化收益微小时 -->
  <span v-once>{{ simpleText }}</span>  <!-- 开销不值得 -->
</template>
```

## 性能比较

| 场景 | 无指令 | 有 v-once/v-memo |
|------|--------|------------------|
| 静态头部，父组件重渲染 100x | 重新评估 100x | 评估 1x |
| 1000 项，选择变更 | 1000 项重渲染 | 2 项重渲染 |
| 复杂子组件 | 完全重渲染 | 如记忆化则跳过 |

## 调试记忆化组件

```vue
<script setup>
import { onUpdated } from 'vue'

// 如果 v-memo 阻止更新，这不会触发
onUpdated(() => {
  console.log('组件已更新')
})
</script>
```
