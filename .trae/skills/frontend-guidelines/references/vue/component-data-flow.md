---
title: 组件数据流最佳实践
impact: HIGH
impactDescription: 组件间清晰的数据流可防止状态错误、过期 UI 和脆弱的耦合
type: best-practice
tags: [vue3, props, emits, v-model, provide-inject, data-flow, typescript]
---

# 组件数据流最佳实践

**影响：高** - 当数据流显式时，Vue 组件保持可靠：props 向下传递，events 向上冒泡，`v-model` 处理双向绑定，provide/inject 支持跨树依赖。模糊这些边界会导致过期状态、隐藏耦合和难以调试的 UI。

Vue.js 中数据流的主要原则是 **Props 向下 / Events 向上**。这是最可维护的默认方式，单向流扩展性良好。

## 任务清单

- 将 props 视为只读输入
- 使用 props/emit 进行组件通信；将 refs 保留给命令式操作
- 当需要 refs 用于命令式 API 时，使用模板 refs 进行类型化
- 发射事件而不是直接变更父组件状态
- 在现代 Vue (3.4+) 中使用 `defineModel` 处理 v-model
- 在子组件中慎重处理 v-model 修饰符
- 使用符号作为 provide/inject 键以避免 props 逐层传递（超过约 3 层）
- 将变更保留在提供者中或暴露显式操作
- 在 TypeScript 项目中，偏好基于类型的 `defineProps`、`defineEmits` 和 `InjectionKey`

## Props：单向数据向下传递

Props 是输入。不要在子组件中变更它们。

**错误：**
```vue
<script setup>
const props = defineProps({ count: Number })

function increment() {
  props.count++
}
</script>
```

**正确：**

如果状态需要改变，发射事件、使用 `v-model` 或创建本地副本。

## 偏好 props/emit 而非组件 refs

**错误：**
```vue
<script setup>
import { ref } from 'vue'
import UserForm from './UserForm.vue'

const formRef = ref(null)

function submitForm() {
  if (formRef.value.isValid) {
    formRef.value.submit()
  }
}
</script>

<template>
  <UserForm ref="formRef" />
  <button @click="submitForm">提交</button>
</template>
```

**正确：**
```vue
<script setup>
import UserForm from './UserForm.vue'

function handleSubmit(formData) {
  api.submit(formData)
}
</script>

<template>
  <UserForm @submit="handleSubmit" />
</template>
```

## 当需要命令式访问时类型化组件 refs

默认偏好 props/emits。当父组件必须调用暴露的子组件方法时，显式类型化 ref 并使用 `defineExpose` 从子组件仅暴露预期的 API。

**错误：**
```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import DialogPanel from './DialogPanel.vue'

const panelRef = ref(null)

onMounted(() => {
  panelRef.value.open()
})
</script>

<template>
  <DialogPanel ref="panelRef" />
</template>
```

**正确：**
```vue
<!-- DialogPanel.vue -->
<script setup lang="ts">
function open() {}

defineExpose({ open })
</script>
```

```vue
<!-- Parent.vue -->
<script setup lang="ts">
import { onMounted, useTemplateRef } from 'vue'
import DialogPanel from './DialogPanel.vue'

// Vue 3.5+ 使用 useTemplateRef
const panelRef = useTemplateRef('panelRef')

// Vue 3.5 之前使用手动类型化和 ref
// const panelRef = ref<InstanceType<typeof DialogPanel> | null>(null)

onMounted(() => {
  panelRef.value?.open()
})
</script>

<template>
  <DialogPanel ref="panelRef" />
</template>
```

## Emits：显式事件向上冒泡

组件事件不会冒泡。如果父组件需要知道某个事件，显式重新发射它。

**错误：**
```vue
<!-- 父组件期望从孙组件获得 "saved"，但它不会冒泡 -->
<Child @saved="onSaved" />
```

**正确：**
```vue
<!-- Child.vue -->
<script setup>
const emit = defineEmits(['saved'])

function onGrandchildSaved(payload) {
  emit('saved', payload)
}
</script>

<template>
  <Grandchild @saved="onGrandchildSaved" />
</template>
```

**事件命名：** 在模板中使用 kebab-case，在脚本中使用 camelCase：
```vue
<script setup>
const emit = defineEmits(['updateUser'])
</script>

<template>
  <ProfileForm @update-user="emit('updateUser', $event)" />
</template>
```

## `v-model`：可预测的双向绑定

默认使用 `defineModel` 进行组件绑定并在输入时发射更新。仅在使用 Vue < 3.4 时才使用 `modelValue` + `update:modelValue` 模式。

**错误：**
```vue
<script setup>
const props = defineProps({ value: String })
</script>

<template>
  <input :value="props.value" @input="$emit('input', $event.target.value)" />
</template>
```

**正确（Vue 3.4+）：**
```vue
<script setup>
const model = defineModel({ type: String })
</script>

<template>
  <input v-model="model" />
</template>
```

**正确（Vue < 3.4）：**
```vue
<script setup>
const props = defineProps({ modelValue: String })
const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <input
    :value="props.modelValue"
    @input="emit('update:modelValue', $event.target.value)"
  />
</template>
```

如果需要在变更后立即获取更新后的值，使用输入事件值或父组件中的 `nextTick`。

## Provide/Inject：无 Prop 逐层传递的共享上下文

使用 provide/inject 处理跨树状态，但将变更集中在提供者中并暴露显式操作。

**错误：**
```vue
// Provider.vue
provide('theme', reactive({ dark: false }))

// Consumer.vue
const theme = inject('theme')
// 从任意深度变更共享状态变得难以追踪
theme.dark = true
```

**正确：**
```vue
// Provider.vue
const theme = reactive({ dark: false })
const toggleTheme = () => { theme.dark = !theme.dark }

provide(themeKey, readonly(theme))
provide(themeActionsKey, { toggleTheme })

// Consumer.vue
const theme = inject(themeKey)
const { toggleTheme } = inject(themeActionsKey)
```

在大型应用中使用符号作为键以避免冲突：
```ts
export const themeKey = Symbol('theme')
export const themeActionsKey = Symbol('theme-actions')
```

## 为公共组件 API 使用 TypeScript 契约

在 TypeScript 项目中，使用 `defineProps`、`defineEmits` 和 `InjectionKey` 直接类型化组件边界，以便无效的载荷和不匹配的注入在编译时失败。

**错误：**
```vue
<script setup lang="ts">
import { inject } from 'vue'

const props = defineProps({
  userId: String
})

const emit = defineEmits(['save'])
const settings = inject('settings')

// 此处不检查载荷形状
emit('save', 123)

// 键是基于字符串的，不是类型安全的
settings?.theme = 'dark'
</script>
```

**正确：**
```vue
<script setup lang="ts">
import { inject, provide } from 'vue'
import type { InjectionKey } from 'vue'

interface Props {
  userId: string
}

interface Emits {
  save: [payload: { id: string; draft: boolean }]
}

interface Settings {
  theme: 'light' | 'dark'
}

const settingsKey: InjectionKey<Settings> = Symbol('settings')

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

provide(settingsKey, { theme: 'light' })

const settings = inject(settingsKey)
if (settings) {
  emit('save', { id: props.userId, draft: false })
}
</script>
```
