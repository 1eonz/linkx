---
title: 组件透传属性最佳实践
impact: MEDIUM
impactDescription: 不正确的 $attrs 访问和响应式假设会导致未定义的值和永不触发的侦听器
type: best-practice
tags: [vue3, attrs, fallthrough-attributes, composition-api, reactivity]
---

# 组件透传属性最佳实践

**影响：中** - 一旦遵循 Vue 的约定，透传属性就很简单：连字符命名的名称使用括号表示法，监听器键是 camelCase `onX`，`useAttrs()` 是当前值但不响应式。

## 任务清单

- 使用括号表示法访问连字符属性名（例如 `attrs['data-testid']`）
- 使用 camelCase `onX` 键访问事件监听器（例如 `attrs.onClick`）
- 不要 `watch()` 从 `useAttrs()` 返回的值；这些侦听器不会在属性变更时触发
- 对属性驱动的副作用使用 `onUpdated()`
- 当需要响应式观察时，将频繁观察的属性提升为 props

## 正确访问属性和监听器键

连字符属性名在 JavaScript 中保留原始大小写，因此点表示法不适用于包含 `-` 的键。

**错误：**
```vue
<script setup>
import { useAttrs } from 'vue'

const attrs = useAttrs()

console.log(attrs.data-testid)  // 语法错误
console.log(attrs.dataTestid)   // data-testid 的结果是 undefined
console.log(attrs['on-click'])  // undefined
console.log(attrs['@click'])    // undefined
</script>
```

**正确：**
```vue
<script setup>
import { useAttrs } from 'vue'

const attrs = useAttrs()

console.log(attrs['data-testid'])
console.log(attrs['aria-label'])
console.log(attrs['foo-bar'])

console.log(attrs.onClick)
console.log(attrs.onCustomEvent)
console.log(attrs.onMouseEnter)
</script>
```

### 命名参考

| 父组件用法 | 在 `attrs` 中访问 |
|------------|-------------------|
| `class="foo"` | `attrs.class` |
| `data-id="123"` | `attrs['data-id']` |
| `aria-label="..."` | `attrs['aria-label']` |
| `foo-bar="baz"` | `attrs['foo-bar']` |
| `@click="fn"` | `attrs.onClick` |
| `@custom-event="fn"` | `attrs.onCustomEvent` |
| `@update:modelValue="fn"` | `attrs['onUpdate:modelValue']` |

## `useAttrs()` 不是响应式的

`useAttrs()` 始终反映最新值，但它故意不响应式用于侦听器追踪。

**错误：**
```vue
<script setup>
import { watch, watchEffect, useAttrs } from 'vue'

const attrs = useAttrs()

watch(
  () => attrs.someAttr,
  (newValue) => {
    console.log('已变更：', newValue) // 在属性变更时永不运行
  }
)

watchEffect(() => {
  console.log(attrs.class) // 在 setup 时运行，不在属性更新时运行
})
</script>
```

**正确：**
```vue
<script setup>
import { onUpdated, useAttrs } from 'vue'

const attrs = useAttrs()

onUpdated(() => {
  console.log('最新属性：', attrs)
})
</script>
```

**正确：**
```vue
<script setup>
import { watch } from 'vue'

const props = defineProps({
  someAttr: String
})

watch(
  () => props.someAttr,
  (newValue) => {
    console.log('已变更：', newValue)
  }
)
</script>
```

## 常见模式

### 安全检查可选属性

```vue
<script setup>
import { computed, useAttrs } from 'vue'

const attrs = useAttrs()

const hasTestId = computed(() => 'data-testid' in attrs)
const ariaLabel = computed(() => attrs['aria-label'] ?? '默认标签')
</script>
```

### 在内部逻辑后转发监听器

```vue
<script setup>
import { useAttrs } from 'vue'

defineOptions({ inheritAttrs: false })

const attrs = useAttrs()

function handleClick(event) {
  console.log('先内部处理')
  attrs.onClick?.(event)
}
</script>

<template>
  <button @click="handleClick">
    <slot />
  </button>
</template>
```

## TypeScript 注意事项

`useAttrs()` 类型为 `Record<string, unknown>`，因此需要时转换单个键。

```vue
<script setup lang="ts">
import { useAttrs } from 'vue'

const attrs = useAttrs()

const testId = attrs['data-testid'] as string | undefined
const onClick = attrs.onClick as ((event: MouseEvent) => void) | undefined
</script>
```
