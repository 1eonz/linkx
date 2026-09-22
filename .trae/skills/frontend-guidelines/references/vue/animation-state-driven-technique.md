---
title: 使用 CSS 过渡和样式绑定的状态驱动动画
impact: LOW
impactDescription: 将 Vue 的响应式样式绑定与 CSS 过渡结合可创建平滑、交互式的动画
type: best-practice
tags: [vue3, animation, css, transition, style-binding, state, interactive]
---

# 使用 CSS 过渡和样式绑定的状态驱动动画

**影响：低** - 对于响应用户输入或状态变化的响应式、交互式动画，将 Vue 的动态样式绑定与 CSS 过渡结合使用。这可以创建基于状态实时插值的平滑动画。

## 任务清单

- 对频繁变化的动态属性使用 `:style` 绑定
- 添加 CSS `transition` 属性以平滑地在值之间动画
- 考虑使用 `transform` 和 `opacity` 进行 GPU 加速动画
- 对于复杂的值插值，使用侦听器配合动画库

## 基本模式

```vue
<template>
  <div
    @mousemove="onMousemove"
    :style="{ backgroundColor: `hsl(${hue}, 80%, 50%)` }"
    class="interactive-area"
  >
    <p>在此 div 上移动鼠标...</p>
    <p>色相：{{ hue }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const hue = ref(0)

function onMousemove(e) {
  // 将鼠标 X 位置映射到色相 (0-360)
  const rect = e.currentTarget.getBoundingClientRect()
  hue.value = Math.round((e.clientX - rect.left) / rect.width * 360)
}
</script>

<style>
.interactive-area {
  transition: background-color 0.3s ease;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>
```

## 常见用例

### 跟随鼠标位置

```vue
<template>
  <div
    class="container"
    @mousemove="onMousemove"
  >
    <div
      class="follower"
      :style="{
        transform: `translate(${x}px, ${y}px)`
      }"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const x = ref(0)
const y = ref(0)

function onMousemove(e) {
  const rect = e.currentTarget.getBoundingClientRect()
  x.value = e.clientX - rect.left
  y.value = e.clientY - rect.top
}
</script>

<style>
.container {
  position: relative;
  height: 300px;
}

.follower {
  position: absolute;
  width: 20px;
  height: 20px;
  background: blue;
  border-radius: 50%;
  /* 使用过渡平滑跟随 */
  transition: transform 0.1s ease-out;
  /* 防止跟随者触发 mousemove */
  pointer-events: none;
}
</style>
```

### 进度动画

```vue
<template>
  <div class="progress-container">
    <div
      class="progress-bar"
      :style="{ width: `${progress}%` }"
    />
  </div>
  <input
    type="range"
    v-model.number="progress"
    min="0"
    max="100"
  />
</template>

<script setup>
import { ref } from 'vue'

const progress = ref(0)
</script>

<style>
.progress-container {
  height: 20px;
  background: #e0e0e0;
  border-radius: 10px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #4CAF50, #8BC34A);
  transition: width 0.3s ease;
}
</style>
```

### 基于滚动的动画

```vue
<template>
  <div
    class="hero"
    :style="{
      opacity: heroOpacity,
      transform: `translateY(${scrollOffset}px)`
    }"
  >
    <h1>向下滚动</h1>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const scrollY = ref(0)

const heroOpacity = computed(() => {
  return Math.max(0, 1 - scrollY.value / 300)
})

const scrollOffset = computed(() => {
  return scrollY.value * 0.5  // 视差效果
})

function handleScroll() {
  scrollY.value = window.scrollY
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style>
.hero {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 注意：基于滚动的动画不需要过渡 - 它们应该是即时的 */
}
</style>
```

### 颜色主题过渡

```vue
<template>
  <div
    class="app"
    :style="themeStyles"
  >
    <button @click="toggleTheme">切换主题</button>
    <p>当前主题：{{ isDark ? '深色' : '浅色' }}</p>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const isDark = ref(false)

const themeStyles = computed(() => ({
  '--bg-color': isDark.value ? '#1a1a1a' : '#ffffff',
  '--text-color': isDark.value ? '#ffffff' : '#1a1a1a',
  backgroundColor: 'var(--bg-color)',
  color: 'var(--text-color)'
}))

function toggleTheme() {
  isDark.value = !isDark.value
}
</script>

<style>
.app {
  min-height: 100vh;
  transition: background-color 0.5s ease, color 0.5s ease;
}
</style>
```

## 高级：使用侦听器进行数值补间

对于平滑的数字动画（计数器、统计数据），使用侦听器配合动画库：

```vue
<template>
  <div>
    <input v-model.number="targetNumber" type="number" />
    <p class="counter">{{ displayNumber.toFixed(0) }}</p>
  </div>
</template>

<script setup>
import { computed, ref, reactive, watch } from 'vue'
import gsap from 'gsap'

const targetNumber = ref(0)
const tweened = reactive({ value: 0 })

// 用于显示的计算属性
const displayNumber = computed(() => tweened.value)

watch(targetNumber, (newValue) => {
  gsap.to(tweened, {
    duration: 0.5,
    value: Number(newValue) || 0,
    ease: 'power2.out'
  })
})
</script>
```

## 性能考虑

```vue
<style>
/* 好：GPU 加速属性 */
.element {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

/* 避免：触发布局重计算的属性 */
.element {
  transition: width 0.3s ease, height 0.3s ease, margin 0.3s ease;
}

/* 对于高频更新，考虑 will-change */
.frequently-animated {
  will-change: transform;
}
</style>
```
