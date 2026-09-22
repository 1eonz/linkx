---
title: TransitionGroup 组件最佳实践
impact: MEDIUM
impactDescription: TransitionGroup 为列表项动画；缺失键或误用会导致列表动画损坏
type: best-practice
tags: [vue3, transition-group, animation, lists, keys]
---

# TransitionGroup 组件最佳实践

**影响：中** - `<TransitionGroup>` 为进入、离开和移动的列表项动画。将其用于 `v-for` 列表或随时间变化的动态集合。

## 任务清单

- 仅对列表和重复项使用 `<TransitionGroup>`
- 为每个直接子元素提供唯一、稳定的键
- 需要语义或布局包装器时使用 `tag`
- 避免使用 `mode` 属性（不支持）
- 使用 JavaScript 钩子实现交错效果

## 对列表使用 TransitionGroup

`<TransitionGroup>` 为列表项设计。需要时使用 `tag` 控制包装元素。

**错误：**
```vue
<template>
  <TransitionGroup name="fade">
    <ComponentA />
    <ComponentB />
  </TransitionGroup>
</template>
```

**正确：**
```vue
<template>
  <TransitionGroup name="list" tag="ul">
    <li v-for="item in items" :key="item.id">
      {{ item.name }}
    </li>
  </TransitionGroup>
</template>
```

## 始终提供稳定的键

键是必需的。没有稳定的键，Vue 无法追踪项目位置，动画会损坏。

**错误：**
```vue
<template>
  <TransitionGroup name="list" tag="ul">
    <li v-for="(item, index) in items" :key="index">
      {{ item.name }}
    </li>
  </TransitionGroup>
</template>
```

**正确：**
```vue
<template>
  <TransitionGroup name="list" tag="ul">
    <li v-for="item in items" :key="item.id">
      {{ item.name }}
    </li>
  </TransitionGroup>
</template>
```

## 不要在 TransitionGroup 上使用 `mode`

`mode` 仅用于 `<Transition>`，因为它替换单个元素。如果需要进/出序列，使用 `<Transition>`。

**错误：**
```vue
<template>
  <TransitionGroup name="list" tag="div" mode="out-in">
    <div v-for="item in items" :key="item.id">{{ item.name }}</div>
  </TransitionGroup>
</template>
```

**正确：**
```vue
<template>
  <Transition name="fade" mode="out-in">
    <component :is="currentView" :key="currentView" />
  </Transition>
</template>
```

## 使用数据属性交错列表动画

对于级联列表动画，将索引传递给 JavaScript 钩子并按项目计算延迟。

```vue
<template>
  <TransitionGroup
    tag="ul"
    :css="false"
    @before-enter="onBeforeEnter"
    @enter="onEnter"
  >
    <li v-for="(item, index) in items" :key="item.id" :data-index="index">
      {{ item.name }}
    </li>
  </TransitionGroup>
</template>

<script setup>
function onBeforeEnter(el) {
  el.style.opacity = 0
  el.style.transform = 'translateY(12px)'
}

function onEnter(el, done) {
  const delay = Number(el.dataset.index) * 80
  setTimeout(() => {
    el.style.transition = 'all 0.25s ease'
    el.style.opacity = 1
    el.style.transform = 'translateY(0)'
    setTimeout(done, 250)
  }, delay)
}
</script>
```
