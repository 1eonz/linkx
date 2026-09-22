---
title: 组件插槽最佳实践
impact: MEDIUM
impactDescription: 糟糕的插槽 API 设计会导致空 DOM 包装器、弱 TypeScript 安全性、脆弱的默认值和不必要的组件开销
type: best-practice
tags: [vue3, slots, components, typescript, composables]
---

# 组件插槽最佳实践

**影响：中** - 插槽是 Vue 中核心的组件 API 表面。有意识地构建它们，使模板保持可预测、类型化和高性能。

## 任务清单

- 对具名插槽使用简写语法（用 `#` 代替 `v-slot:`）
- 仅当插槽内容存在时渲染可选插槽包装元素（`$slots` 检查）
- 在 TypeScript 组件中使用 `defineSlots` 类型化作用域插槽契约
- 为可选插槽提供回退内容
- 对纯逻辑复用偏好可组合函数而非无渲染组件

## 具名插槽的简写语法

**错误：**
```vue
<MyComponent>
  <template v-slot:header> ... </template>
</MyComponent>
```

**正确：**
```vue
<MyComponent>
  <template #header> ... </template>
</MyComponent>
```

## 条件渲染可选插槽包装器

当包装元素添加间距、边框或布局约束时使用 `$slots` 检查。

**错误：**
```vue
<!-- Card.vue -->
<template>
  <article class="card">
    <header class="card-header">
      <slot name="header" />
    </header>

    <section class="card-body">
      <slot />
    </section>

    <footer class="card-footer">
      <slot name="footer" />
    </footer>
  </article>
</template>
```

**正确：**
```vue
<!-- Card.vue -->
<template>
  <article class="card">
    <header v-if="$slots.header" class="card-header">
      <slot name="header" />
    </header>

    <section v-if="$slots.default" class="card-body">
      <slot />
    </section>

    <footer v-if="$slots.footer" class="card-footer">
      <slot name="footer" />
    </footer>
  </article>
</template>
```

## 使用 defineSlots 类型化作用域插槽属性

在 `<script setup lang="ts">` 中，使用 `defineSlots` 让插槽消费者获得自动补全和静态检查。

**错误：**
```vue
<!-- ProductList.vue -->
<script setup lang="ts">
interface Product {
  id: number
  name: string
}

defineProps<{ products: Product[] }>()
</script>

<template>
  <ul>
    <li v-for="(product, index) in products" :key="product.id">
      <slot :product="product" :index="index" />
    </li>
  </ul>
</template>
```

**正确：**
```vue
<!-- ProductList.vue -->
<script setup lang="ts">
interface Product {
  id: number
  name: string
}

defineProps<{ products: Product[] }>()

defineSlots<{
  default(props: { product: Product; index: number }): any
  empty(): any
}>()
</script>

<template>
  <ul v-if="products.length">
    <li v-for="(product, index) in products" :key="product.id">
      <slot :product="product" :index="index" />
    </li>
  </ul>
  <slot v-else name="empty" />
</template>
```

## 提供插槽回退内容

回退内容使组件在父组件省略可选插槽时具有弹性。

**错误：**
```vue
<!-- SubmitButton.vue -->
<template>
  <button type="submit" class="btn-primary">
    <slot />
  </button>
</template>
```

**正确：**
```vue
<!-- SubmitButton.vue -->
<template>
  <button type="submit" class="btn-primary">
    <slot>提交</slot>
  </button>
</template>
```

## 对纯逻辑复用偏好可组合函数

无渲染组件对于插槽驱动的组合仍然有用，但对于仅逻辑复用，可组合函数通常更清晰。

**错误：**
```vue
<!-- MouseTracker.vue -->
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const x = ref(0)
const y = ref(0)

function onMove(event: MouseEvent) {
  x.value = event.pageX
  y.value = event.pageY
}

onMounted(() => window.addEventListener('mousemove', onMove))
onUnmounted(() => window.removeEventListener('mousemove', onMove))
</script>

<template>
  <slot :x="x" :y="y" />
</template>
```

**正确：**
```ts
// composables/useMouse.ts
import { ref, onMounted, onUnmounted } from 'vue'

export function useMouse() {
  const x = ref(0)
  const y = ref(0)

  function onMove(event: MouseEvent) {
    x.value = event.pageX
    y.value = event.pageY
  }

  onMounted(() => window.addEventListener('mousemove', onMove))
  onUnmounted(() => window.removeEventListener('mousemove', onMove))

  return { x, y }
}
```

```vue
<!-- MousePosition.vue -->
<script setup lang="ts">
import { useMouse } from '@/composables/useMouse'

const { x, y } = useMouse()
</script>

<template>
  <p>{{ x }}, {{ y }}</p>
</template>
```
