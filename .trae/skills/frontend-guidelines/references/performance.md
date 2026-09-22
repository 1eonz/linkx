# 前端性能优化规范

> 适用范围: web/、agent/web/、H5Portal（Vue3）与 cloudcmd-admin-web（Vue2）。
> 标注「目标/待落地」的条目为团队期望但尚未在 CI/生产中实现，不要假设已存在相关基建。
> Vue 通用渲染优化（v-once/v-memo/shallowRef/虚拟列表/组件拆分时机）见本技能 `vue/perf-*.md` references，本文件不重复。

## 一、首屏加载优化

### 1.1 路由懒加载

```typescript
// 所有路由组件必须懒加载（web/H5Portal 用 pages/，agent/web 用 views/）
const routes = [
  {
    path: '/user',
    component: () => import('@/pages/user/index.vue'),  // 懒加载
  },
]
```

> admin（Vue2）同样适用，路径前缀按项目实际（`@/views/...`）。

### 1.2 组件懒加载

```vue
<!-- 大型/非首屏组件使用 defineAsyncComponent -->
<script setup>
import { defineAsyncComponent } from 'vue'
const HeavyChart = defineAsyncComponent(() => import('@/components/HeavyChart.vue'))
</script>
```

### 1.3 资源优化

| 资源 | 优化方式 |
|------|----------|
| 图片 | WebP 格式、懒加载（`loading="lazy"`）、响应式 `srcset` |
| 字体 | `font-display: swap`、子集化 |
| SVG | 内联小图标、雪碧图/图标组件 |
| CSS | 提取关键 CSS、非关键 CSS 异步加载 |
| JS | Tree Shaking、代码分割、动态 import |

### 1.4 预加载策略

```html
<!-- 关键资源预加载 -->
<link rel="preload" href="/fonts/main.woff2" as="font" type="font/woff2" crossorigin>

<!-- 下一页预取 -->
<link rel="prefetch" href="/js/user-page.js">
```

## 二、渲染优化

### 2.1 列表渲染

> v-if/v-for 编码规范见 `.trae/rules/frontend.md` §13

```vue
<!-- 必须使用 key，且 key 必须唯一稳定 -->
<div v-for="item in list" :key="item.id">{{ item.name }}</div>

<!-- 禁止用 index 作 key -->
<div v-for="(item, index) in list" :key="index">{{ item.name }}</div>
```

### 2.2 计算属性 vs 方法

```vue
<script setup>
// 推荐: 计算属性有缓存
const filteredList = computed(() => list.value.filter(item => item.active))

// 禁止: 模板中调用方法（每次渲染都执行）
function getFilteredList() {
  return list.value.filter(item => item.active)
}
</script>
```

### 2.3 组件卸载清理

> Composable 副作用清理见 `vue/composables.md`

```typescript
// 定时器、事件监听器必须在卸载时清理
onMounted(() => {
  window.addEventListener('resize', handleResize)
  timer = setInterval(fetchData, 30000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  clearInterval(timer)
})

// 推荐: 使用 composable 封装清理逻辑
const { pause, resume } = useIntervalFn(fetchData, 30000)
```

> v-once / v-memo / shallowRef 等高级渲染优化技巧见本技能 `vue/perf-v-once-v-memo-directives.md`、`vue/perf-virtualize-large-lists.md`、`vue/perf-avoid-component-abstraction-in-lists.md` references。

## 三、网络优化

### 3.1 请求优化

- 接口合并: 多个小请求合并为一个批量请求
- 请求防抖: 搜索输入 300ms 防抖
- 请求取消: 路由切换时取消未完成请求（AbortController）
- 请求缓存: 相同请求复用结果（SWR 模式）

```typescript
// 请求取消
const controller = new AbortController()

async function fetchData() {
  const res = await fetch('/api/data', { signal: controller.signal })
  return res.json()
}

// 路由切换时取消
onBeforeRouteLeave(() => {
  controller.abort()
})
```

### 3.2 缓存策略

| 数据类型 | 缓存策略 | 过期时间 |
|----------|----------|----------|
| 静态配置 | 内存缓存（Pinia + persist） | 长期 |
| 用户信息 | Pinia Store | 登录期间 |
| 列表数据 | SWR (stale-while-revalidate) | 5 分钟 |
| 详情数据 | 按需缓存 | 10 分钟 |

## 四、构建优化

### 4.1 打包分析

```bash
# 分析打包体积（web/agent/web，需 vite 配置中开启 rollup-plugin-visualizer）
pnpm build --analyze

# H5Portal
npm run build   # 如已配置 visualizer 则生成分析报告

# admin（Vue2 + vue-cli）
yarn build:prod --report   # 需 webpack-bundle-analyzer
```

### 4.2 优化规则

- 第三方库按需引入;**大型库优先 tree-shaking 子模块**（如 `import debounce from 'lodash-es/debounce'`）。
- **Element Plus 现状**: web/agent/web 当前在 `src/hooks/useCreateApp/index.ts` 中 `import ElementPlus from 'element-plus'` + `app.use(ElementPlus, ...)` **全量注册**，并已引入 `element-plus/dist/index.css` 全量样式。新增功能不要扩大这一开销;如需独立优化，可评估迁移到 `unplugin-vue-components` 按需注册（**目标/待落地**，迁移前不要在 lint/规范层强制按需）。
- 大型库考虑 CDN 外置: ECharts、Lodash 等（**目标/待落地**，当前仍走 npm 打包）。
- 开启 Gzip/Brotli 压缩（**目标/待落地**，需在 vite/vue 配置中确认）。
- 静态资源哈希命名: `[name].[hash].js`（vite 默认行为，vue-cli 需在 `vue.config.js` 配置）。

### 4.3 依赖优化

```typescript
// vite.config.ts（web/agent/web，目标/待落地: 当前未必启用 manualChunks）
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-ui': ['element-plus'],
          'vendor-utils': ['axios', 'dayjs', 'lodash-es'],
        },
      },
    },
  },
})
```

> admin 用 `vue.config.js` 的 `configureWebpack` / `chainWebpack` 配置 `splitChunks`，不要套用 vite 写法。

## 五、性能指标

### 5.1 目标值

| 指标 | 目标 | 说明 |
|------|------|------|
| FCP (First Contentful Paint) | < 1.8s | 首次内容绘制 |
| LCP (Largest Contentful Paint) | < 2.5s | 最大内容绘制 |
| FID (First Input Delay) | < 100ms | 首次输入延迟 |
| CLS (Cumulative Layout Shift) | < 0.1 | 累积布局偏移 |
| TTI (Time to Interactive) | < 3.5s | 可交互时间 |
| JS Bundle Size (gzip) | < 300KB | 首屏 JS 体积 |

> 上述为团队目标阈值，**当前未在 CI 中强制卡口**。

### 5.2 监控（目标/待落地）

- 开发阶段使用 Lighthouse 评分（手动）
- CI 中集成 Lighthouse CI —— **未实现**
- 生产环境使用 Web Vitals 监控上报 —— **未实现**
