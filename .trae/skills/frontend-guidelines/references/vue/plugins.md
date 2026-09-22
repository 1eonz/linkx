---
title: Vue 插件最佳实践
impact: MEDIUM
impactDescription: 不正确的插件结构或注入键策略会导致安装失败、冲突和不安全的 API
type: best-practice
tags: [vue3, plugins, provide-inject, typescript, dependency-injection]
---

# Vue 插件最佳实践

**影响：中** - Vue 插件应该遵循 `app.use()` 契约，暴露显式能力，并使用防冲突的注入键。这使插件设置在大型应用中可预测且可组合。

## 任务清单

- 将插件导出为有 `install()` 的对象或安装函数
- 在 `install()` 中使用 `app` 实例注册组件/指令/提供
- 使用 `Plugin` 类型化插件 API（需要时使用选项元组类型）
- 在插件中为 `provide/inject` 使用符号键（偏好 `InjectionKey<T>`）
- 为必需注入添加小型类型化可组合函数包装器以快速失败

## 为 `app.use()` 结构化插件

Vue 插件必须是以下之一：
- 有 `install(app, options?)` 的对象
- 有相同签名的函数

**错误：**
```ts
const notAPlugin = {
  doSomething() {}
}

app.use(notAPlugin)
```

**正确：**
```ts
import type { App } from 'vue'

interface PluginOptions {
  prefix?: string
  debug?: boolean
}

const myPlugin = {
  install(app: App, options: PluginOptions = {}) {
    const { prefix = 'my', debug = false } = options

    if (debug) {
      console.log('安装 myPlugin，前缀：', prefix)
    }

    app.provide('myPlugin', { prefix })
  }
}

app.use(myPlugin, { prefix: 'custom', debug: true })
```

**正确：**
```ts
import type { App } from 'vue'

function simplePlugin(app: App, options?: { message: string }) {
  app.config.globalProperties.$greet = () => options?.message ?? '你好！'
}

app.use(simplePlugin, { message: '欢迎！' })
```

## 在 `install()` 中显式注册能力

在 `install()` 内，通过 Vue 应用 API 连接行为：
- `app.component()` 用于全局组件
- `app.directive()` 用于全局指令
- `app.provide()` 用于可注入服务和配置
- `app.config.globalProperties` 用于可选全局辅助函数（慎用）

**错误：**
```ts
const uselessPlugin = {
  install(app, options) {
    const service = createService(options)
  }
}
```

**正确：**
```ts
const usefulPlugin = {
  install(app, options) {
    const service = createService(options)
    app.provide(serviceKey, service)
  }
}
```

## 类型化插件契约

使用 Vue 的 `Plugin` 类型保持安装签名和选项类型安全。

```ts
import type { App, Plugin } from 'vue'

interface MyOptions {
  apiKey: string
}

const myPlugin: Plugin<[MyOptions]> = {
  install(app: App, options: MyOptions) {
    app.provide(apiKeyKey, options.apiKey)
  }
}
```

## 在插件中使用符号注入键

字符串键可能冲突（`'http'`、`'config'`、`'i18n'`）。使用带 `InjectionKey<T>` 的符号键以便注入唯一且类型化。

**错误：**
```ts
export default {
  install(app) {
    app.provide('http', axios)
    app.provide('config', appConfig)
  }
}
```

**正确：**
```ts
import type { InjectionKey } from 'vue'
import type { AxiosInstance } from 'axios'

interface AppConfig {
  apiUrl: string
  timeout: number
}

export const httpKey: InjectionKey<AxiosInstance> = Symbol('http')
export const configKey: InjectionKey<AppConfig> = Symbol('appConfig')

export default {
  install(app) {
    app.provide(httpKey, axios)
    app.provide(configKey, { apiUrl: '/api', timeout: 5000 })
  }
}
```

## 提供必需注入辅助函数

将必需注入包装在可组合函数中，抛出清晰的设置错误。

```ts
import { inject } from 'vue'
import { authKey, type AuthService } from '@/injection-keys'

export function useAuth(): AuthService {
  const auth = inject(authKey)
  if (!auth) {
    throw new Error('Auth 插件未安装。你忘记 app.use(authPlugin) 了吗？')
  }
  return auth
}
```
