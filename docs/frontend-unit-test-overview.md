# 前端单元测试方案总览

> 本文档为四个前端项目单元测试体系的统一入口，供 AI 助手后续写单测/跑单测时读取上下文。
> 配套 Skill：
> - `.trae/skills/frontend-unit-test-writer/SKILL.md` — 生成单测
> - `.trae/skills/frontend-unit-test-runner/SKILL.md` — 运行单测

---

## 一、项目总览

| 项目 | 路径 | 框架 | 构建 | 包管理器 | 测试文件扩展名 | 主要测试目标 |
|------|------|------|------|---------|--------------|------------|
| web | `web/` | Vue3 + TS + Vite + Element Plus + Pinia | Vite 5 | pnpm | `.spec.ts` | utils/data/hooks 业务核心纯函数 |
| agent/web | `agent/web/` | Vue3 + TS + Vite + Element Plus + Pinia | Vite 5 | pnpm | `.spec.ts` | utils/locales 工具与国际化加载 |
| H5Portal | `H5Portal/` | Vue3 + JS + Vite 7 + Vant + Pinia | Vite 7 | pnpm | `.spec.js` | utils/hooks/pages 业务函数 |
| cloudcmd-admin-web | `cloudcmd-admin-web/` | Vue2 + JS + vue-cli + Element UI + Vuex | Webpack 4 | npm | `.spec.js` | utils/store 业务核心 |

### 统一框架选择理由

| 维度 | Vitest | Jest |
|------|--------|------|
| Vite 项目原生支持 | ✅ 共享 vite.config 别名/插件/globals | ❌ 需额外配置 transformIgnorePatterns |
| ESM 支持 | ✅ 原生 | ⚠️ 需 babel/ts-jest 转译 |
| 启动速度 | ✅ 毫秒级（按需转译） | ❌ 慢 |
| TS/JSX/Vue 支持 | ✅ 开箱即用 | ❌ 需 vue-jest + babel-jest |
| API 兼容性 | ✅ 与 Jest API 一致（describe/it/expect/mock） | — |
| Watch/HMR | ✅ 原生 | ⚠️ 需配置 |
| 覆盖率 | ✅ v8 provider | ✅ istanbul |

**决策**：四个项目统一采用 **Vitest**，cloudcmd-admin-web 也从 Jest 迁移到 Vitest（API 几乎一致，迁移成本低）。

---

## 二、测试框架与依赖

统一采用 **Vitest 2.1.8** 体系：

| 依赖 | 版本 | 用途 |
|------|------|------|
| `vitest` | ^2.1.8 | 测试框架 |
| `happy-dom` | ^15.11.7 | DOM 环境模拟（轻量，快于 jsdom） |
| `@vitest/coverage-v8` | ^2.1.8 | v8 覆盖率统计 |
| `@vue/test-utils` | ^2.4.6（Vue3）/ ^1.3.6（Vue2） | Vue 组件测试工具 |

---

## 三、测试分层

| 层级 | 范围 | 工具 | 占比 |
|------|------|------|------|
| L1 纯函数测试 | utils 中的无副作用函数 | Vitest only | ~60% |
| L2 Mock 测试 | 依赖 localStorage/window/Date 的函数 | vi.stubGlobal / vi.useFakeTimers / vi.spyOn | ~30% |
| L3 组件测试 | 关键业务 Composable / Store mutations | @vue/test-utils + happy-dom | ~10% |

### 优先级定义

- **P0（高）**：纯函数 + 业务核心，无副作用，立即测试
- **P1（中）**：需简单 mock（Date/localStorage/window）但逻辑重要
- **P2（低）**：副作用强或 UI 集成强，建议集成/E2E，暂不纳入单测范围

---

## 四、目录结构

每个项目的测试结构统一为：

```
<project>/
├── vitest.config.ts          # 测试配置（独立文件，不污染 vite.config）
├── tests/
│   └── unit/
│       ├── setup.ts          # 全局 setup（mock vant、清理副作用等）
│       ├── __mocks__/         # 公共 mock 模块（如 cloudcmd-admin-web 的 layout-stub）
│       └── <module>.spec.ts  # 测试文件（与源文件同名）
└── src/
    └── utils/
        └── <module>.ts       # 被测源文件
```

**命名规范**：
- 测试文件与源文件同名，加 `.spec` 后缀（`validate.ts` → `validate.spec.ts`）
- 位置：`tests/unit/` 下与 src 同名（便于查找）
- 顶层 `describe('<模块名>')`，内层 `describe('<函数名>')`
- 用例描述用中文：`it('应<行为描述>', ...)`

---

## 五、命令速查

| 操作 | web / agent/web / H5Portal | cloudcmd-admin-web |
|------|---------------------------|-------------------|
| 运行全部测试 | `pnpm test` | `npm test` |
| 运行覆盖率 | `pnpm test:coverage` | `npm run test:coverage` |
| Watch 模式 | `pnpm test:watch` | `npm run test:watch` |
| 单文件运行 | `pnpm vitest run tests/unit/<file>.spec.ts` | `npx vitest run tests/unit/<file>.spec.js` |
| 受影响范围（基于 git diff） | `git diff --name-only HEAD` 推断后 `pnpm vitest run <affected-specs>` | 同左 |

**默认行为**：`test:coverage` 会生成 HTML 报告，位于 `<project>/coverage/index.html`。

### 全项目运行（CI 场景）

```bash
cd d:\code\Linkx\Linkx-B\web && pnpm test:coverage
cd d:\code\Linkx\Linkx-B\agent\web && pnpm test:coverage
cd d:\code\Linkx\Linkx-B\H5Portal && pnpm test:coverage
cd d:\code\Linkx\Linkx-B\cloudcmd-admin-web && npm run test:coverage
```

---

## 六、可测试函数清单

### 6.1 web 项目

#### P0 — 纯函数（立即测试）

| 文件 | 函数 | 测试要点 |
|------|------|----------|
| `utils/is.ts` | `is` / `isObject` / `isDate` / `isNumber` / `isString` / `isFunction` / `isBoolean` / `isRegExp` / `isArray` / `isMap` / `isPromise` / `isElement` | 各类型正反例、边界值（null/undefined/空对象） |
| 同上 | `isDef` / `isUnDef` / `isNull` / `isNullAndUnDef` / `isNullOrUnDef` | 空值判断；`isNullAndUnDef` 逻辑恒假 |
| 同上 | `isEmpty` | 空数组/字符串/Map/Set/对象/原始类型 |
| 同上 | `isUrl(path)` | http/https/无协议/带端口/路径 |
| 同上 | `isOnline(data)` | bizStatus 1/4/6 在线，其他离线 |
| `utils/validate.ts` | `validRegisterUserName` / `validRegisterPassword` / `validUtf8LongerThanLen` / `validHasSpecialCharacter` | 用户名/密码/UTF-8 字节长度/特殊字符 |
| 同上 | `isCardreg` / `isPhonereg` / `isEmailreg` | 身份证 15/18 位、手机号、邮箱 |
| `utils/dataUtil.ts` | `isNullOrUndefined` / `wordSizeOf` / `checkReturnData` / `getFileExtension` / `formatTime` | ⚠️ `isNullOrUndefined` bug：多参数时失效 |
| `utils/treeHelper.js` | `getAllIds` | 空对象/单对象/数组/深层嵌套/无 children |
| `utils/index.ts` | `byteLength` / `cleanArray` / `param` / `param2Obj` / `uniqueArr` / `objectMerge` | `param2Obj` 的 + 号转空格；`objectMerge` 数组分支 |
| 同上 | `stringParseJson` / `colorRGBtoHex` / `hexToRgb` / `addUnit` / `stringify` / `checkIsSessionMode` / `checkConfigSwitch` | JSON 解析含 `\n`、颜色转换、配置开关 |
| `utils/dateUtil.ts` | `isLeapYear` / `getDaysInMonth` / `formatDate` / `format` / `formatDateStr` | 闰年 2000/1900/2024、平闰年 2 月、各占位符 |
| 同上 | `formatHourMinute` / `formatHourMinuteSecond` / `getDays` / `getHMSByMsec` / `getSecondByDateSub` / `getTime` / `timeChange` | 时差格式化、跨日、补零 |
| 同上 | `addDays` / `addHours` / `addMinutes` / `addSeconds` / `addMonths` / `addYears` | **会 mutate 入参**，跨年/闰年/2-29 边界 |
| `utils/auth.ts` | `authorityCheck` | 权限查找：匹配/不匹配/空数组 |
| `utils/headerUtils.ts` | `getSignature` | HMAC-SHA256 签名，确定性测试 |

#### P1 — 需 mock 但逻辑重要

| 文件 | 函数 | Mock 依赖 |
|------|------|----------|
| `utils/auth.ts` | `getToken` / `setToken` / `removeToken` | localStorage |
| 同上 | `getDeviceId` / `setDeviceId` / `getRefreshToken` / `setRefreshToken` / `removeRefreshToken` | js-cookie + localStorage（⚠️ `removeRefreshToken` bug） |
| 同上 | `setIsLockScreen` / `getIsLockScreen` | js-cookie |
| `utils/env.ts` | `isWebView2` | window.chrome.webview / URLSearchParams / window.PIM_GetPlatform |
| `utils/clientEnv.ts` | `getBrowserInfo` / `getOSInfo` / `getScreenInfo` | navigator.userAgent / window.screen + vi.resetModules |
| `utils/dateUtil.ts` | `getCurrentTime` / `getTodayWeek` / `dataToYesterday` / `getMondayTimesTamp` / `transformTime` | new Date() + vi.useFakeTimers / vue-i18n |
| `utils/index.ts` | `generateUUID` / `S4` / `guid` / `getRecentSomeDays` / `getRecentDateRange` / `parseTime` / `formatTime` | Math.random + fake timers / vue-i18n |
| `utils/headerUtils.ts` | `getRequestId` / `getTimeStamp` | new Date + Math.random + 模块状态 |
| `utils/DC.ts` | `on` / `off` / `onmessage` | 模块级 messageHandlers |
| `utils/globalConfig.ts` | `GlobalConfig.data` getter/setter | localStorage + Promise 队列 |
| `data/useTheme.ts` | `setTheme` / `toggleTheme` / `lockTheme` / `init` / `onThemeChange` | document.documentElement.dataset + localStorage |
| `data/helper.ts` | `filterPersonData` / `filterEquipmentsData` | useResourceStoreWithOut（⚠️ `ballCameraOnline/Offline` 重复出现疑似 bug） |

#### P2 — 暂不纳入单测

- `utils/addLayerUtil.ts`（地图图层渲染）、`utils/loginIcs.ts`（业务流程编排）、`utils/domUtils.ts`（DOM 操作封装）、`utils/rem.ts`（模块加载即副作用）、`utils/VeeValidate.ts`（vee-validate install）、`utils/pimHttp.ts`/`http.ts`/`httpAi.ts`/`axiosFetch.ts`（HTTP 封装）

### 6.2 agent/web 项目

#### P0 — 纯函数

| 文件 | 函数 | 测试要点 |
|------|------|----------|
| `utils/is.ts` | 同 web（22 个函数） | 与 web 一致，测试用例可直接复用 |
| `utils/index.ts` | `addUnit` | 同 web |
| `locales/helper.ts` | `genMessage` | 路径解析为嵌套对象，多边界用例 |

#### P1 — 需 mock

| 文件 | 函数 | Mock 依赖 |
|------|------|----------|
| `utils/index.ts` | `withInstall` / `S4` / `guid` | App / Math.random |
| 同上 | `displayImage` | import.meta.env |
| `utils/domUtils.ts` | `hasClass` / `addClass` / `removeClass` / `getBoundingClientRect` / `useRafThrottle` | DOM / requestAnimationFrame |
| `locales/index.ts` | `getLang` / `setLang` | localStorage + navigator |
| `locales/helper.ts` | `setHtmlPageLang` / `setLoadLocalePool` | DOM + 模块状态 |
| `utils/http.ts` | HttpService 各方法 | axios + localStorage |

#### P2 — 暂不纳入

- `utils/rem.ts`、`store/index.ts`、`router/index.ts`、`hooks/index.ts`、`api/index.ts`

### 6.3 H5Portal 项目

#### P0 — 纯函数

| 文件 | 函数 | 测试要点 |
|------|------|----------|
| `utils/index.js` | `queryStringToJson` / `debounce` / `getCurrentDateTime` | ⚠️ `queryStringToJson` 的 `split('=', 2)` bug |
| `utils/time.js` | `getTimeRange` | 7days/month/year、非法入参抛错 |
| `utils/totalFunc.js` | `filterMonitor` | falsy 过滤、name 去重、license 过滤"设备调度" |
| `utils/copyText.js` | `extractText`（内部函数） | 纯文本/含 HTML/嵌套标签/非字符串 |
| `hooks/useCommon.js` | `removeBase64Prefix` / `deepClone` / `deepCloneArray` / `deepCloneObject` | 循环引用/Date/RegExp/Map/Set/Error/Symbol |
| `pages/map/utils.js` | `createImageMarker` / `debounce` | 在线/离线颜色、HTML 结构 |

#### P1 — 需 mock

| 文件 | 函数 | Mock 依赖 |
|------|------|----------|
| `utils/eventBus.js` | `EventBus` 类（on/emit/off） | 无需 mock，直接测内部状态 |
| `utils/websocket.js` | `Message` 类 / `MyWebSocket` 类 | uni.connectSocket + 定时器 |
| `utils/http.js` | `Request` / `UploadFile` 类 | axios + 动态 import store |
| `utils/clientEnv.js` | `getBrowserInfo` / `getOSInfo` / `getScreenInfo` | navigator.userAgent + vi.resetModules |
| `utils/copyText.js` | `copyText` | navigator.clipboard + document |
| `hooks/useCommon.js` | `pngToBase64` | fetch/FileReader/document.baseURI |
| `common/config.js` | `getBaseUrl` / `getSocketUrl` | store + window.__PINIA_STORE__ |

#### P2 — 暂不纳入

- `utils/version.js` / `toast.js` / `echarts.js`、`totalFunc.js` 中的心跳相关函数、`file.js` 中的 SDK 调用函数、`index.js` 中的 `selectGroupByActionSheet` / `openLocalUrlApp` / `locationShareFunc`

### 6.4 cloudcmd-admin-web 项目

#### P0 — 纯函数

| 文件 | 函数 | 测试要点 |
|------|------|----------|
| `utils/passwordValidator.js` | `hasChinese` / `hasLowercase` / `hasUppercase` / `hasNumber` / `hasSpecialChar` / `countCharTypes` | 字符类型识别各边界 |
| 同上 | `validateSimpleMode` / `validateComplexMode` / `validateRepeatPassword` | 空/中文/长度 7-8 边界/类型数 1-2/与账号相同 |
| `utils/secure.js` | `encryptIdCard` / `decryptIdCard` | 加解密可逆、空值安全 |
| `utils/crypto.js` | `encryptJson` / `decryptJson` | AES 加解密可逆、错误密钥返回 null、损坏数据返回 null |
| `utils/index.js` | `parseTime` / `format` / `param2Obj` / `treeDataTranslate` / `deepCopy` | 各占位符、10/13 位时间戳、循环引用 |
| `utils/menuRouteMapper.js` | `buildRoutesFromOauthMenu` | status=0 过滤、Dashboard 特例、sort 排序、routeOnly 反向收集 |
| `utils/validate.js` | `isExternal` | http/https/mailto/tel 正则 |

#### P1 — 需 mock

| 文件 | 函数 | Mock 依赖 |
|------|------|----------|
| `utils/auth.js` | token/userId/buttons/isAdmin/idCardNum/licenseAuth 读写 | localStorage（vi.spyOn Storage.prototype） |
| 同上 | `getAllNodeIdByDepartmentId` | queryDepartment API（递归） |
| `utils/licenseUtils.js` | `getLicenseInfoUtil` | getLicenseInfo + setLicenseAuth |
| `utils/clientEnv.js` | UA 解析多分支 | navigator.userAgent + vi.resetModules |
| `utils/permission.js` | `hasBtnPermission` | store + localStorage 回退 |
| `utils/get-page-title.js` | `getPageTitle` | @/locales + @/store |
| `utils/passwordValidator.js` | `createSimpleValidator` / `createComplexValidator` / `createRepeatValidator` | vm（含 $t） |
| `store/modules/app.js` | mutations/actions | js-cookie |
| `store/modules/settings.js` | `CHANGE_SETTING` / `SET_SYSTEM_NAME` | @/settings + localStorage |
| `store/modules/user.js` | `login`（base64 license header 解码、code 0/121 分支） | 多个 api + auth + router |

#### P2 — 暂不纳入

- `utils/request.js`、`utils/createDialog.js`、`utils/astrict.js`、`utils/pageLoading.js`、`utils/scrollTo.js`、`src/api/**`（薄封装）、`src/store/getters.js`（无逻辑）

---

## 七、不纳入测试的范围（明确说明）

| 类别 | 文件示例 | 原因 |
|------|----------|------|
| DOM 操作封装 | `domUtils.ts` | jsdom 与真实 DOM 差异，价值低 |
| 模块加载即副作用 | `rem.ts` | 无法隔离，应走 E2E |
| 地图图层渲染 | `addLayerUtil.ts` | 强依赖地图 SDK |
| HTTP 拦截器 | `http.ts` / `request.js` | 集成测试范畴 |
| 业务流程编排 | `loginIcs.ts` / `permission.js` | 强依赖 store/router/api |
| WebSocket 心跳 | `websocket.js` | 定时器 + SDK |
| Vue 组件 install | `VeeValidate.ts` | 副作用强 |
| API 薄封装 | `src/api/**` | 无业务逻辑 |
| 类型/枚举定义 | `types.ts` / `enums/index.ts` | 无可执行逻辑 |

---

## 八、覆盖率配置

### 关键原则

**`coverage.include` 采用精确文件列表**，只统计实际有测试覆盖的源文件，避免未测试的业务文件（语言包、页面组件等）拉低全局覆盖率。

### 覆盖率目标

| 项目 | statements | branches | functions | lines |
|------|-----------|----------|-----------|-------|
| web | 80% | 75% | 75% | 80% |
| agent/web | 75% | 70% | 70% | 75% |
| H5Portal | 70% | 65% | 65% | 70% |
| cloudcmd-admin-web | 75% | 70% | 65% | 75% |

### 各项目配置

#### web

```typescript
coverage: {
  include: [
    'src/utils/is.ts', 'src/utils/validate.ts', 'src/utils/dataUtil.ts',
    'src/utils/treeHelper.js', 'src/utils/index.ts', 'src/utils/dateUtil.ts',
    'src/utils/auth.ts', 'src/utils/headerUtils.ts', 'src/utils/env.ts',
    'src/utils/clientEnv.ts', 'src/utils/DC.ts', 'src/utils/globalConfig.ts',
    'src/data/useTheme.ts',
  ],
  thresholds: { statements: 80, branches: 75, functions: 75, lines: 80 },
}
```

#### agent/web

```typescript
coverage: {
  include: ['src/utils/is.ts', 'src/utils/index.ts', 'src/locales/helper.ts'],
  thresholds: { statements: 75, branches: 70, functions: 70, lines: 75 },
}
```

#### H5Portal

```typescript
coverage: {
  include: [
    'src/utils/time.js', 'src/utils/totalFunc.js', 'src/utils/copyText.js',
    'src/utils/eventBus.js', 'src/utils/websocket.js', 'src/utils/http.js',
    'src/utils/clientEnv.js', 'src/hooks/useCommon.js', 'src/pages/map/utils.js',
  ],
  thresholds: { statements: 70, branches: 65, functions: 65, lines: 70 },
}
```

#### cloudcmd-admin-web

```typescript
coverage: {
  include: ['src/utils/**/*.{js,vue}'],
  exclude: ['src/utils/auth.js', 'src/utils/request.js', 'src/utils/createDialog.js',
            'src/utils/astrict.js', 'src/utils/pageLoading.js', 'src/utils/scrollTo.js'],
  thresholds: { statements: 75, branches: 70, functions: 65, lines: 75 },
}
```

---

## 九、别名配置

| 项目 | 别名 |
|------|------|
| web | `@/` → `src/`、`#/` → `types/` |
| agent/web | `@/` → `src/`、`#/` → `types/` |
| H5Portal | `@/` → `src/` |
| cloudcmd-admin-web | `@/` → `src/`（含 `.vue` stub 特殊处理） |

### cloudcmd-admin-web 的特殊处理

Vue 2.6.10 不兼容 `@vitejs/plugin-vue2`（需 Vue 2.7+），通过 alias stub 解决：

```typescript
alias: [
  { find: '@/layout', replacement: resolve(__dirname, 'tests/unit/__mocks__/layout-stub.js') },
  { find: /@\/views\/.*/, replacement: resolve(__dirname, 'tests/unit/__mocks__/view-stub.js') },
  { find: /@\/(.*)/, replacement: resolve(__dirname, 'src/$1') },
],
// 将 .vue 文件 inline 处理为空组件
server: { deps: { inline: [/\.vue$/] } },
```

---

## 十、全局 setup 文件

所有项目的 `tests/unit/setup.ts` 都包含统一的 mock 清理逻辑：

```typescript
import { vi, afterEach } from 'vitest';

afterEach(() => {
  vi.restoreAllMocks();
  vi.useRealTimers();
  localStorage.clear();
  sessionStorage.clear();
});
```

**H5Portal 额外 mock vant**（避免 `src/utils/index.js` 导入时触发副作用）：

```typescript
vi.mock('vant', () => ({
  Popup: { name: 'Popup' },
  showToast: vi.fn(),
  showConfirmDialog: vi.fn(),
  showDialog: vi.fn(),
}));
```

---

## 十一、Mock 策略速查

| 依赖类型 | 推荐方式 |
|----------|---------|
| `localStorage` / `sessionStorage` | 直接使用（happy-dom 提供），`afterEach` 清理 |
| `js-cookie` | `vi.mock('js-cookie', () => ({ default: { get, set, remove } }))` |
| `new Date()` / `Date.now()` | `vi.useFakeTimers()` + `vi.setSystemTime(...)` |
| `Math.random` | `vi.spyOn(Math, 'random').mockReturnValue(0.5)` |
| `window` / `navigator` 属性 | `vi.stubGlobal('navigator', { ... })` |
| 模块级副作用 | `vi.resetModules()` + 重新 `import()` |
| 外部 API 调用 | `vi.mock('@/api/xxx', () => ({ funcName: vi.fn() }))` |
| `import.meta.env` | `vi.stubEnv('VITE_XXX', 'value')` |
| `requestAnimationFrame` | `vi.useFakeTimers()` + `vi.advanceTimersByTime()` |

### Mock 模式示例

#### Mock localStorage

```typescript
import { vi, beforeEach, test, expect } from 'vitest';
import { getToken, setToken } from '@/utils/auth';

beforeEach(() => {
  localStorage.clear();
});

test('setToken/getToken 应可逆', () => {
  setToken('abc123');
  expect(getToken()).toBe('abc123');
  expect(localStorage.getItem('Admin-Token')).toBe('abc123');
});
```

#### Mock js-cookie

```typescript
import { vi, test, expect } from 'vitest';
import Cookies from 'js-cookie';
import { getDeviceId, setDeviceId } from '@/utils/auth';

vi.mock('js-cookie', () => ({
  default: {
    get: vi.fn(),
    set: vi.fn(),
    remove: vi.fn(),
  },
}));

test('setDeviceId 应写入 Cookie', () => {
  setDeviceId('device-001');
  expect(Cookies.set).toHaveBeenCalledWith('deviceId', 'device-001', expect.anything());
});
```

#### Mock Date（fake timers）

```typescript
import { vi, beforeEach, afterEach, test, expect } from 'vitest';
import { getTimeRange } from '@/utils/time';

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date('2026-07-27T10:00:00Z'));
});

afterEach(() => {
  vi.useRealTimers();
});

test('getTimeRange("7days") 应返回最近 7 天的起止', () => {
  const { startTime, endTime } = getTimeRange('7days');
  expect(startTime).toBe('2026-07-21 00:00:00');
  expect(endTime).toBe('2026-07-27 23:59:59');
});
```

#### Mock window 属性

```typescript
import { vi, test, expect } from 'vitest';
import { isWebView2 } from '@/utils/env';

test('isWebView2 应识别 chrome.webview', () => {
  vi.stubGlobal('chrome', { webview: {} });
  expect(isWebView2()).toBe(true);
  vi.unstubAllGlobals();
});
```

#### Mock 模块级状态（UA 解析类）

```typescript
import { vi, beforeEach, test, expect } from 'vitest';

beforeEach(() => {
  vi.resetModules();
});

test('Chrome UA 解析', async () => {
  vi.stubGlobal('navigator', {
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0',
  });
  const { getBrowserInfo } = await import('@/utils/clientEnv');
  expect(getBrowserInfo()).toContain('Chrome');
  vi.unstubAllGlobals();
});
```

---

## 十二、用例覆盖矩阵

每个被测函数至少覆盖以下维度（适用时）：

| 维度 | 描述 | 示例 |
|------|------|------|
| **正常值** | 业务期望的输入 | `isEmailreg('a@b.com')` → true |
| **边界值** | 长度/数值的临界点 | 0 / -1 / MAX_SAFE_INTEGER / 空字符串 |
| **异常值** | 非法类型/格式 | null / undefined / NaN / 对象 |
| **分支覆盖** | 所有 if/switch 分支 | bizStatus 为 1/4/6（在线）与其他（离线） |
| **正则边界** | 校验类函数的特殊字符 | 含中文 / emoji / 控制字符 |
| **副作用验证** | mock 后验证调用 | `expect(Cookies.set).toHaveBeenCalledWith(...)` |
| **可逆性** | 加解密/序列化对 | `decrypt(encrypt(x)) === x` |
| **不修改入参** | 纯函数契约 | 深拷贝入参后断言原值未变 |

### 测试用例组织示例

```typescript
import { describe, it, expect } from 'vitest';
import { isUrl, isEmpty } from '@/utils/is';

describe('utils/is', () => {
  describe('isUrl', () => {
    it('应识别 http 协议的 URL', () => {
      expect(isUrl('http://example.com')).toBe(true);
    });
    it('应拒绝无协议的路径', () => {
      expect(isUrl('example.com')).toBe(false);
    });
  });

  describe('isEmpty', () => {
    it.each([
      ['空字符串', '', true],
      ['空数组', [], true],
      ['空对象', {}, true],
      ['空 Map', new Map(), true],
      ['非空字符串', 'a', false],
      ['数字 0', 0, false], // 注意：isEmpty(0) 为 false
    ])('当输入 %s 时应返回 %s', (_name, input, expected) => {
      expect(isEmpty(input)).toBe(expected);
    });
  });
});
```

---

## 十三、疑似 Bug 清单

> 以下 bug 在编写测试时发现，**不要修复源码**（不在测试 Skill 职责范围内），用 `it.skip` 标注。

| # | 项目 | 文件 | 函数 | 问题 | 建议测试用例 |
|---|------|------|------|------|------------|
| 1 | web | `utils/dataUtil.ts` | `isNullOrUndefined` | 循环未读取 `arguments[i]`，多参数时只检查第一个 | `isNullOrUndefined(null, undefined)` 应 true，当前对第 2 参无效 |
| 2 | web | `utils/auth.ts` | `removeRefreshToken` | 用 `localStorage.removeItem` 删除，但 `setRefreshToken` 用 `Cookies.set` 写入 | 设置后应删除，实际 Cookie 仍在 |
| 3 | web | `utils/dateUtil.ts` | `strToDate` | 第 482 行 `isEmpty(str)` 应为 `!isEmpty(str)` | 空字符串应原样返回，当前进入解析分支 |
| 4 | web | `data/helper.ts` | `filterEquipmentsData` | 返回数组中 `ballCameraOnline/Offline` 各出现两次 | 快照测试可暴露 |
| 5 | web | `utils/index.ts` | `getRecentSomeDays` | 注释"包含今天共 7 天"，但月末跨月时 `setDate` 可能异常 | 跨月边界用例 |
| 6 | web | `utils/index.ts` | `toggleClass` | 添加 class 缺前导空格 | class 已存在时应不重复添加 |
| 7 | H5Portal | `utils/index.js` | `queryStringToJson` | `split('=', 2)` 在 JS 中会截断值 | `queryStringToJson('a=b=c')` 应返回 `{ a: 'b=c' }`，当前返回 `{ a: 'b' }` |

### 疑似 Bug 处理规范

```typescript
// ⚠️ 疑似 Bug：源码 isEmpty(str) 应为 !isEmpty(str)
// 空字符串应原样返回，当前进入解析分支
it.skip('空字符串应原样返回', () => {
  expect(strToDate('')).toBe('');
});
```

---

## 十四、已知环境差异与陷阱

> 以下差异**不是 bug**，是 happy-dom 测试环境特性。测试应反映实际运行时行为，不要为"正确性"写与环境不符的断言。

### happy-dom 与真实浏览器差异

| 函数 | 真实浏览器 | happy-dom | 原因 |
|------|-----------|-----------|------|
| `isPromise(Promise.resolve())` | true | false | happy-dom 的 toString 返回 `[object Object]` 而非 `[object Promise]` |
| `isWindow(window)` | true | false | happy-dom 返回 `[object global]` 而非 `[object Window]` |
| `isElement(div)` | true | false | happy-dom 返回 `[object HTMLDivElement]` 而非 `[object Object]` |

### 各项目已知问题与解决方案

| 项目 | 问题 | 解决方案 |
|------|------|---------|
| web | `@/utils/index.ts` 导入报 `indexedDB is not defined` | `vi.stubGlobal('indexedDB', ...)` 或 `vi.mock('@/hooks', ...)` |
| web | `isNullOrUndefined(null, 1)` TS 报错 | 源码仅声明 1 参数，测试加 `@ts-expect-error` |
| web | `dateUtil.ts` 的 `strToDate('')` 进入解析分支 | 源码 `isEmpty(str)` 应为 `!isEmpty(str)`，用 `it.skip` 标注 bug |
| H5Portal | `@/common/config` 循环依赖 | 测试中 `vi.mock('@/common/config', ...)` |
| H5Portal | `@/utils/totalFunc.js` 顶层调用 `useCommunicationStore()` | `vi.mock('@/stores/communication.js', ...)` |
| H5Portal | `queryStringToJson('a=b=c')` 丢失 `=c` | `split('=', 2)` 在 JS 中截断数组，用 `it.skip` 标注 |
| cloudcmd-admin-web | `@/locales/lang/cn.js` 报 `require.context is not a function` | webpack API 在 Vite 下不可用，`vi.mock('@/locales/lang/cn', ...)` |
| cloudcmd-admin-web | `@/utils/index.js` 顶层访问 `localStorage` | `beforeEach` 中 `localStorage.setItem('localLanguage', 'cn')` |
| cloudcmd-admin-web | `.vue` 文件无法解析 | alias stub + `server.deps.inline` |
| cloudcmd-admin-web | `secure.js` 依赖 `Buffer` | happy-dom + Node 环境透传 Node 全局，可用；若不可用 `vi.stubGlobal('Buffer', Buffer)` |

### 模块级副作用清单（import 即触发，需 mock）

- `cloudcmd-admin-web/src/utils/index.js`：顶层 `localStorage.getItem('localLanguage')`
- `H5Portal/src/utils/totalFunc.js`：顶层调用 `useCommunicationStore()`（需 active pinia）
- `H5Portal/src/utils/clientEnv.js`：顶层访问 `navigator.userAgent` + `window.screen`
- `agent/web/src/locales/index.ts`：顶层 `setLang(getLang())` 写 localStorage
- `H5Portal/src/utils/index.js`：import `vant` 的 `Popup`/`showToast`（需 `vi.mock('vant')`）

### 使用 import.meta.env（需 `vi.stubEnv`）

- `web/src/utils/index.ts`：`VITE_PUBLIC_PATH`、`DEV`、`VITE_PROXY`、`import.meta.url`
- `agent/web/src/utils/index.ts`：`DEV`、`VITE_PROXY`
- `H5Portal/src/hooks/useCommon.js`：`import.meta.env?.DEV`

### 调用 useI18n（需 mock vue-i18n 或跳过）

- `web/src/utils/index.ts`：`parseTime`、`formatTime`
- `web/src/utils/dateUtil.ts`：`dataToYesterday`、`getMondayTimesTamp`、`transformTime`

### 关键发现

- `agent/web/src/utils/is.ts` 与 `web/src/utils/is.ts` **完全相同**（byte-for-byte），测试用例可直接复用
- `H5Portal/src/utils/index.js` 不能直接 import，必须 `vi.mock('vant')` 后才能安全导入
- `web/agent/web` 是子 monorepo（含 `pnpm-workspace.yaml`），但测试文件放项目根 `tests/unit/` 即可

---

## 十五、失败原因分类与修复建议

| 失败类型 | 识别特征 | 修复方向 |
|----------|---------|---------|
| **断言不符** | `expected X to be Y` | 检查测试期望或源码逻辑 |
| **源码 Bug** | 测试期望合理，源码实现有误 | 标注 ⚠️ 疑似 Bug，建议用户确认后修复源码 |
| **测试过时** | 源码函数签名/返回值已变更 | 调用 frontend-unit-test-writer 重新生成 |
| **Mock 未生效** | `vi.mock` 未拦截 / `undefined is not a function` | 检查 mock 路径与导出方式 |
| **环境缺失** | `localStorage is not defined` / `navigator is undefined` | 检查 `environment: 'happy-dom'` 配置 |
| **超时** | `Test timed out in 5000ms` | 检查异步/Promise 未 resolve |
| **快照不符** | `Snapshot: - expected + received` | 询问用户是否更新快照 `pnpm vitest -u` |

---

## 十六、CI 集成注意事项

### cloudcmd-admin-web 的 Node 版本兼容

CI 环境使用 Node 16.15.1，但 `node-releases@2.0.50` 要求 Node >= 18。通过 `.yarnrc` 跳过引擎检查：

```
# cloudcmd-admin-web/.yarnrc
ignore-engines true
```

> ⚠️ `.yarnrc` 文件格式要求 `key value` 形式，**不能**写成 `--ignore-engines`（命令行参数格式在配置文件中无效）。

### tsconfig 类型检查范围

web 和 agent/web 的 `tsconfig.json` 已将 `tests` 从 `include` 移到 `exclude`，避免 `vue-tsc --noEmit` 类型检查测试文件报错（测试文件中可能有 `@ts-expect-error` 等刻意类型违规）。

### Jest → Vitest 迁移要点（cloudcmd-admin-web）

迁移时需要：
1. 移除 `@vue/cli-plugin-unit-jest`、`babel-jest`、`vue-jest`、`jest-transform-stub`、`jest-serializer-vue`
2. 安装 `@vitejs/plugin-vue2`、`vitest`、`@vue/test-utils@1`、`happy-dom`、`@vitest/coverage-v8`
3. 删除 `jest.config.js`
4. 更新 package.json scripts

API 替换：
- `jest.fn()` → `vi.fn()`
- `jest.mock('xxx')` → `vi.mock('xxx')`
- `jest.useFakeTimers()` → `vi.useFakeTimers()`
- `jest.spyOn(obj, 'method')` → `vi.spyOn(obj, 'method')`
- `expect.any()` / `expect.anything()` 等 Vitest 原生兼容，无需改

---

## 十七、Skill 协同模式

### 编写 → 运行闭环

```
用户："为 validate.ts 写单测"
    ↓
frontend-unit-test-writer 生成测试文件
    ↓
frontend-unit-test-writer 调用 frontend-unit-test-runner 验证
    ↓
frontend-unit-test-runner 返回结果
    ↓
若有失败 → frontend-unit-test-writer 修复 → 再次运行
    ↓
全绿 → 输出最终报告
```

### 提交守卫闭环

```
用户："提交代码"
    ↓
git-commit-push Skill 激活
    ↓
pre-commit hook 检测到 src/utils/ 或 tests/unit/ 有变更
    ↓
git-commit-push 调用 frontend-unit-test-runner
    ↓
frontend-unit-test-runner 运行受影响测试
    ↓
全绿 → 允许提交
失败 → 阻断提交，返回失败清单
```

### 受影响范围推断规则

- `src/utils/xxx.ts` → `tests/unit/xxx.spec.ts`
- `src/hooks/xxx.ts` → `tests/unit/xxx.spec.ts`
- `src/data/xxx.ts` → `tests/unit/xxx.spec.ts`
- 若无对应测试文件，跳过（提示用户可用 frontend-unit-test-writer 生成）

---

## 十八、测试文件清单

### web（13 个）

```
tests/unit/
├── DC.spec.ts
├── auth.spec.ts
├── clientEnv.spec.ts
├── dataUtil.spec.ts
├── dateUtil.spec.ts
├── env.spec.ts
├── globalConfig.spec.ts
├── headerUtils.spec.ts
├── is.spec.ts
├── setup.ts
├── treeHelper.spec.ts
├── useTheme.spec.ts
├── utils-index.spec.ts
└── validate.spec.ts
```

### agent/web（1 个）

```
tests/unit/
├── is.spec.ts
└── setup.ts
```

### H5Portal（10 个）

```
tests/unit/
├── clientEnv.spec.js
├── copyText.spec.js
├── eventBus.spec.js
├── http.spec.js
├── map-utils.spec.js
├── setup.ts
├── time.spec.js
├── totalFunc.spec.js
├── useCommon.spec.js
├── utils-index.spec.js
└── websocket.spec.js
```

### cloudcmd-admin-web（18 个）

```
tests/unit/
├── __mocks__/
│   ├── layout-stub.js
│   └── view-stub.js
├── components/
│   ├── Breadcrumb.spec.js
│   ├── Hamburger.spec.js
│   └── SvgIcon.spec.js
├── setup.js
├── store/
│   ├── app.spec.js
│   ├── settings.spec.js
│   └── user.spec.js
└── utils/
    ├── auth.spec.js
    ├── clientEnv.spec.js
    ├── crypto.spec.js
    ├── formatTime.spec.js
    ├── get-page-title.spec.js
    ├── index.spec.js
    ├── licenseUtils.spec.js
    ├── menuRouteMapper.spec.js
    ├── parseTime.spec.js
    ├── passwordValidator.spec.js
    ├── permission.spec.js
    ├── secure.spec.js
    └── validate.spec.js
```

> 注：`components/` 下的测试因依赖 `.vue` 文件解析，已在 `vitest.config.ts` 的 `exclude` 中跳过。

---

## 十九、扩展测试时的操作指引

### 新增工具函数测试

1. 在对应项目的 `tests/unit/` 下创建 `<module>.spec.<ext>`
2. 在 `vitest.config.ts` 的 `coverage.include` 中添加该源文件路径
3. 参考 `.trae/skills/frontend-unit-test-writer/SKILL.md` 的用例覆盖矩阵
4. 运行 `pnpm test` 或 `npm test` 验证通过

### 运行测试

参考 `.trae/skills/frontend-unit-test-runner/SKILL.md`：
- 单文件：`pnpm vitest run tests/unit/<file>.spec.ts --reporter=verbose`
- 单项目：`pnpm test:coverage`（默认生成 HTML 报告）
- 全项目：依次进入四个项目目录执行

### 覆盖率不达标时

1. 查看 `coverage/index.html` 定位未覆盖的文件/行
2. 调用 `frontend-unit-test-writer` Skill 补充测试
3. 重新运行 `pnpm test:coverage` 验证

---

## 二十、参考文档

- [Vitest 官方文档](https://vitest.dev/)
- [@vue/test-utils](https://test-utils.vuejs.org/)
- [happy-dom](https://github.com/capricorn86/happy-dom)
- 项目规范：`.trae/rules/`（frontend.md / backend.md）
- Skill 文件：
  - `.trae/skills/frontend-unit-test-writer/SKILL.md`
  - `.trae/skills/frontend-unit-test-runner/SKILL.md`

---

**本文档为前端单元测试体系的统一入口。AI 助手后续写单测/跑单测时应先读取本文档获取上下文，再配合两个 Skill 文件执行具体任务。**
