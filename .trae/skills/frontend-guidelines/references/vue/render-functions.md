---
title: 渲染函数模式和性能
impact: MEDIUM
impactDescription: 渲染函数需要为列表、事件、v-model 和性能使用显式模式以保持正确和可维护
type: best-practice
tags: [vue3, render-function, h, v-model, directives, performance, jsx]
---

# 渲染函数模式和性能

**影响：中** - 渲染函数强大但退出模板编译器优化。有意使用它们并应用以下关键模式保持输出正确和高性能。

## 任务清单

- 偏好模板；仅当模板无法表达逻辑时使用渲染函数
- 用 `h()`/JSX 渲染列表时始终添加稳定键
- 使用 `withModifiers` / `withKeys` 处理事件修饰符
- 通过 `modelValue` + `onUpdate:modelValue` 实现 `v-model`
- 使用 `withDirectives` 应用自定义指令
- 为无状态展示 UI 使用函数式组件

## 偏好模板而非渲染函数

**错误：**
```vue
<script setup>
import { h, ref } from 'vue'

const count = ref(0)
const render = () => h('div', `计数：${count.value}`)
</script>
```

**正确：**
```vue
<script setup>
import { ref } from 'vue'

const count = ref(0)
</script>

<template>
  <div>计数：{{ count }}</div>
</template>
```

## 始终为列表渲染添加键

**错误：**
```javascript
import { h, ref } from 'vue'

export default {
  setup() {
    const items = ref([{ id: 1, name: 'Apple' }])

    return () => h('ul',
      items.value.map(item => h('li', item.name))
    )
  }
}
```

**正确：**
```javascript
import { h, ref } from 'vue'

export default {
  setup() {
    const items = ref([{ id: 1, name: 'Apple' }])

    return () => h('ul',
      items.value.map(item => h('li', { key: item.id }, item.name))
    )
  }
}
```

## 使用 `withModifiers` / `withKeys` 处理事件修饰符

**错误：**
```javascript
import { h } from 'vue'

export default {
  setup() {
    const handleClick = (e) => {
      e.stopPropagation()
      e.preventDefault()
    }

    return () => h('button', { onClick: handleClick }, '点击')
  }
}
```

**正确：**
```javascript
import { h, withModifiers, withKeys } from 'vue'

export default {
  setup() {
    const handleClick = () => {}
    const handleEnter = () => {}

    return () => h('div', [
      h('button', {
        onClick: withModifiers(handleClick, ['stop', 'prevent'])
      }, '点击'),
      h('input', {
        onKeyup: withKeys(handleEnter, ['enter'])
      })
    ])
  }
}
```

## 显式实现 `v-model`

**错误：**
```javascript
import { h, ref } from 'vue'
import CustomInput from './CustomInput.vue'

export default {
  setup() {
    const text = ref('')
    return () => h(CustomInput, { modelValue: text.value })
  }
}
```

**正确：**
```javascript
import { h, ref } from 'vue'
import CustomInput from './CustomInput.vue'

export default {
  setup() {
    const text = ref('')
    return () => h(CustomInput, {
      modelValue: text.value,
      'onUpdate:modelValue': (value) => { text.value = value }
    })
  }
}
```

## 使用 `withDirectives` 处理自定义指令

**错误：**
```javascript
import { h } from 'vue'

const vFocus = { mounted: (el) => el.focus() }

export default {
  setup() {
    return () => h('input', { 'v-focus': true })
  }
}
```

**正确：**
```javascript
import { h, withDirectives } from 'vue'

const vFocus = { mounted: (el) => el.focus() }

export default {
  setup() {
    return () => withDirectives(h('input'), [[vFocus]])
  }
}
```

## 为无状态 UI 偏好函数式组件

**错误：**
```javascript
import { h } from 'vue'

export default {
  setup() {
    return () => h('span', { class: 'badge' }, '新')
  }
}
```

**正确：**
```javascript
import { h } from 'vue'

function Badge(props, { slots }) {
  return h('span', { class: 'badge' }, slots.default?.())
}

Badge.props = ['variant']

export default Badge
```
