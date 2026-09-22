---
title: 可组合函数组织模式
impact: MEDIUM
impactDescription: 结构良好的可组合函数提高可维护性、可复用性和更新性能
type: best-practice
tags: [vue3, composables, composition-api, code-organization, api-design, readonly, utilities]
---

# 可组合函数组织模式

**影响：中** - 将可组合函数视为可复用的、有状态的构建块，并按功能关注点组织其代码。这使大型组件可维护，并防止难以调试的变更和 API 设计问题。

## 任务清单

- 从小型、专注的可组合函数组合复杂行为
- 对有多个可选参数的可组合函数使用选项对象
- 当更新必须通过显式操作流时返回只读状态
- 将纯工具函数保持为普通工具，而非可组合函数
- 按功能关注点组织可组合函数和组件代码，并在组件增长时提取可组合函数

## 从更小的原语组合可组合函数

**错误：**
```vue
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const x = ref(0)
const y = ref(0)
const inside = ref(false)
const el = ref(null)

function onMove(e) {
  x.value = e.pageX
  y.value = e.pageY
  if (!el.value) return
  const r = el.value.getBoundingClientRect()
  inside.value = x.value >= r.left && x.value <= r.right &&
    y.value >= r.top && y.value <= r.bottom
}

onMounted(() => window.addEventListener('mousemove', onMove))
onUnmounted(() => window.removeEventListener('mousemove', onMove))
</script>
```

**正确：**
```javascript
// composables/useEventListener.js
import { onMounted, onUnmounted, toValue } from 'vue'

export function useEventListener(target, event, callback) {
  onMounted(() => toValue(target).addEventListener(event, callback))
  onUnmounted(() => toValue(target).removeEventListener(event, callback))
}
```

```javascript
// composables/useMouse.js
import { ref } from 'vue'
import { useEventListener } from './useEventListener'

export function useMouse() {
  const x = ref(0)
  const y = ref(0)

  useEventListener(window, 'mousemove', (e) => {
    x.value = e.pageX
    y.value = e.pageY
  })

  return { x, y }
}
```

```javascript
// composables/useMouseInElement.js
import { computed } from 'vue'
import { useMouse } from './useMouse'

export function useMouseInElement(elementRef) {
  const { x, y } = useMouse()

  const isOutside = computed(() => {
    if (!elementRef.value) return true
    const rect = elementRef.value.getBoundingClientRect()
    return x.value < rect.left || x.value > rect.right ||
      y.value < rect.top || y.value > rect.bottom
  })

  return { x, y, isOutside }
}
```

## 为可组合函数参数使用选项对象模式

**错误：**
```javascript
export function useFetch(url, method, headers, timeout, retries, immediate) {
  // 难以阅读且容易顺序错误
}

useFetch('/api/users', 'GET', null, 5000, 3, true)
```

**正确：**
```javascript
export function useFetch(url, options = {}) {
  const {
    method = 'GET',
    headers = {},
    timeout = 30000,
    retries = 0,
    immediate = true
  } = options

  // 实现
  return { method, headers, timeout, retries, immediate }
}

useFetch('/api/users', {
  method: 'POST',
  timeout: 5000,
  retries: 3
})
```

```typescript
interface UseCounterOptions {
  initial?: number
  min?: number
  max?: number
  step?: number
}

export function useCounter(options: UseCounterOptions = {}) {
  const { initial = 0, min = -Infinity, max = Infinity, step = 1 } = options
  // 实现
}
```

## 返回只读状态和显式操作

**错误：**
```javascript
export function useCart() {
  const items = ref([])
  const total = computed(() => items.value.reduce((sum, item) => sum + item.price, 0))
  return { items, total } // 任何消费者都可以直接变更
}

const { items } = useCart()
items.value.push({ id: 1, price: 10 })
```

**正确：**
```javascript
import { ref, computed, readonly } from 'vue'

export function useCart() {
  const _items = ref([])

  const total = computed(() =>
    _items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  function addItem(product, quantity = 1) {
    const existing = _items.value.find(item => item.id === product.id)
    if (existing) {
      existing.quantity += quantity
      return
    }
    _items.value.push({ ...product, quantity })
  }

  function removeItem(productId) {
    _items.value = _items.value.filter(item => item.id !== productId)
  }

  return {
    items: readonly(_items),
    total,
    addItem,
    removeItem
  }
}
```

## 将工具保持为工具

**错误：**
```javascript
export function useFormatters() {
  const formatDate = (date) => new Intl.DateTimeFormat('en-US').format(date)
  const formatCurrency = (amount) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
  return { formatDate, formatCurrency }
}

const { formatDate } = useFormatters()
```

**正确：**
```javascript
// utils/formatters.js
export function formatDate(date) {
  return new Intl.DateTimeFormat('en-US').format(date)
}

export function formatCurrency(amount) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(amount)
}
```

```javascript
// composables/useInvoiceSummary.js
import { computed } from 'vue'
import { formatCurrency } from '@/utils/formatters'

export function useInvoiceSummary(invoiceRef) {
  const totalLabel = computed(() => formatCurrency(invoiceRef.value.total))
  return { totalLabel }
}
```

## 按功能关注点组织可组合函数和组件代码

**错误：**
```vue
<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const searchQuery = ref('')
const items = ref([])
const selected = ref(null)
const showModal = ref(false)
const sortBy = ref('name')
const filter = ref('all')
const loading = ref(false)

const filtered = computed(() => items.value.filter(i => i.category === filter.value))
function openModal() { showModal.value = true }
const sorted = computed(() => [...filtered.value].sort(/* ... */))
watch(searchQuery, () => { /* ... */ })
onMounted(() => { /* ... */ })
</script>
```

**正确：**
```vue
<script setup>
import { useItems } from '@/composables/useItems'
import { useSearch } from '@/composables/useSearch'
import { useSelectionModal } from '@/composables/useSelectionModal'

// 数据
const { items, loading, fetchItems } = useItems()

// 搜索/过滤/排序
const { query, visibleItems } = useSearch(items)

// 选择 + 模态框
const { selectedItem, isModalOpen, selectItem, closeModal } = useSelectionModal()
</script>
```

```javascript
// composables/useItems.js
import { ref, onMounted } from 'vue'

export function useItems() {
  const items = ref([])
  const loading = ref(false)

  async function fetchItems() {
    loading.value = true
    try {
      items.value = await api.getItems()
    } finally {
      loading.value = false
    }
  }

  onMounted(fetchItems)
  return { items, loading, fetchItems }
}
```
