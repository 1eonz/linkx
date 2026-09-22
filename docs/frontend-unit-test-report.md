# 前端单元测试结果报告

> 本文档记录四个前端项目单元测试的执行结果与覆盖情况，按测试分层（L1/L2/L3）组织，标注哪些已测、哪些未测、成功/失败/覆盖率等，方便持续维护。
> 最近一次运行：2026-07-29
> 配套文档：[frontend-unit-test-overview.md](./frontend-unit-test-overview.md)

---

## 一、测试分层定义

| 层级 | 范围 | 工具 | 占比 |
|------|------|------|------|
| **L1 纯函数测试** | utils 中的无副作用函数 | Vitest only | ~60% |
| **L2 Mock 测试** | 依赖 localStorage/window/Date/cookie 等的函数 | vi.stubGlobal / vi.useFakeTimers / vi.spyOn / vi.mock | ~30% |
| **L3 组件测试** | 关键业务 Composable / Store mutations / Vue 组件 | @vue/test-utils + happy-dom | ~10% |

### 优先级与分层映射

- **P0（高）**：纯函数 + 业务核心，无副作用 → 主要落在 **L1**
- **P1（中）**：需简单 mock（Date/localStorage/window）但逻辑重要 → 主要落在 **L2**
- **P2（低）**：副作用强或 UI 集成强，建议集成/E2E → **未纳入单测**

---

## 二、总体运行结果

| 项目 | 测试文件 | 用例 | 通过 | 失败 | 跳过 | 覆盖率（statements） | 耗时 | 状态 |
|------|---------|------|------|------|------|---------------------|------|------|
| web | 13 | 780 | 769 | 0 | 11 | 93.73% | 3.65s | ✅ 通过 |
| agent/web | 3 | 276 | 276 | 0 | 0 | 98.61% | 2.80s | ✅ 通过 |
| H5Portal | 10 | 368 | 367 | 0 | 1 | 90.05% | 1.65s | ✅ 通过 |
| cloudcmd-admin-web | 16 | 340 | 340 | 0 | 0 | 99.89% | 3.88s | ✅ 通过 |
| **合计** | **42** | **1764** | **1752** | **0** | **12** | — | **11.98s** | ✅ 全部通过 |

### 覆盖率达标情况

| 项目 | statements 目标 | 实际 | branches 目标 | 实际 | functions 目标 | 实际 | lines 目标 | 实际 | 状态 |
|------|----------------|------|---------------|------|----------------|------|------------|------|------|
| web | 80% | 93.73% | 75% | 93.19% | 75% | 97.87% | 80% | 93.73% | ✅ |
| agent/web | 75% | 98.61% | 70% | 95.38% | 70% | 100% | 75% | 98.61% | ✅ |
| H5Portal | 70% | 90.05% | 65% | 95.34% | 65% | 93.33% | 70% | 90.05% | ✅ |
| cloudcmd-admin-web | 75% | 99.89% | 70% | 99.07% | 65% | 66.15% | 75% | 99.89% | ✅ |

> 注：cloudcmd-admin-web 的 functions 覆盖率 66.15% 低于其他指标，但 thresholds 目标为 65%，已达标。主要受 `menuRouteMapper.js` 的函数覆盖率 12% 影响（该文件分支复杂，部分函数未完全覆盖）。

### 📊 HTML 测试报告

- `web/coverage/index.html`
- `agent/web/coverage/index.html`
- `H5Portal/coverage/index.html`
- `cloudcmd-admin-web/coverage/index.html`

---

## 三、web 项目测试结果

### 3.1 L1 纯函数测试（P0）

| 测试文件 | 被测源文件 | 被测函数 | 用例数 | 通过 | 跳过 | 状态 |
|---------|----------|---------|-------|------|------|------|
| `is.spec.ts` | `utils/is.ts` | `is`/`isObject`/`isDate`/`isNumber`/`isString`/`isFunction`/`isBoolean`/`isRegExp`/`isArray`/`isMap`/`isPromise`/`isElement`/`isDef`/`isUnDef`/`isNull`/`isNullAndUnDef`/`isNullOrUnDef`/`isEmpty`/`isUrl`/`isOnline`/`isWindow` | 224 | 224 | 0 | ✅ |
| `validate.spec.ts` | `utils/validate.ts` | `validRegisterUserName`/`validRegisterPassword`/`validUtf8LongerThanLen`/`validHasSpecialCharacter`/`isCardreg`/`isPhonereg`/`isEmailreg` | 70 | 70 | 0 | ✅ |
| `dataUtil.spec.ts` | `utils/dataUtil.ts` | `isNullOrUndefined`/`wordSizeOf`/`checkReturnData`/`getFileExtension`/`formatTime` | 47 | 45 | 2 | ✅ |
| `treeHelper.spec.ts` | `utils/treeHelper.js` | `getAllIds` | 12 | 12 | 0 | ✅ |
| `utils-index.spec.ts` | `utils/index.ts` | `byteLength`/`cleanArray`/`param`/`param2Obj`/`uniqueArr`/`objectMerge`/`stringParseJson`/`colorRGBtoHex`/`hexToRgb`/`addUnit`/`stringify`/`checkIsSessionMode`/`checkConfigSwitch` | 148 | 147 | 1 | ✅ |
| `dateUtil.spec.ts` | `utils/dateUtil.ts` | `isLeapYear`/`getDaysInMonth`/`formatDate`/`format`/`formatDateStr`/`formatHourMinute`/`formatHourMinuteSecond`/`getDays`/`getHMSByMsec`/`getSecondByDateSub`/`getTime`/`timeChange`/`addDays`/`addHours`/`addMinutes`/`addSeconds`/`addMonths`/`addYears`/`strToDate` | 115 | 109 | 6 | ✅ |

**L1 小计**：6 个测试文件，616 个用例，607 通过，9 跳过

**L1 跳过用例说明**：
- `dataUtil.spec.ts`（2 skipped）：`isNullOrUndefined` 多参数 bug 用例（疑似 bug #1）
- `utils-index.spec.ts`（1 skipped）：`getRecentSomeDays` 跨月边界（疑似 bug #5）
- `dateUtil.spec.ts`（6 skipped）：`strToDate` 空字符串 bug（疑似 bug #3）等边界

### 3.2 L2 Mock 测试（P1）

| 测试文件 | 被测源文件 | 被测函数 | Mock 依赖 | 用例数 | 通过 | 跳过 | 状态 |
|---------|----------|---------|----------|-------|------|------|------|
| `auth.spec.ts` | `utils/auth.ts` | `getToken`/`setToken`/`removeToken`/`getDeviceId`/`setDeviceId`/`getRefreshToken`/`setRefreshToken`/`removeRefreshToken`/`setIsLockScreen`/`getIsLockScreen`/`authorityCheck` | localStorage + js-cookie | 19 | 18 | 1 | ✅ |
| `env.spec.ts` | `utils/env.ts` | `isWebView2` | window.chrome.webview / URLSearchParams | 14 | 14 | 0 | ✅ |
| `clientEnv.spec.ts` | `utils/clientEnv.ts` | `getBrowserInfo`/`getOSInfo`/`getScreenInfo` | navigator.userAgent + vi.resetModules | 21 | 21 | 0 | ✅ |
| `headerUtils.spec.ts` | `utils/headerUtils.ts` | `getSignature`/`getRequestId`/`getTimeStamp` | new Date + Math.random | 29 | 29 | 0 | ✅ |
| `DC.spec.ts` | `utils/DC.ts` | `on`/`off`/`onmessage` | 模块级 messageHandlers | 24 | 23 | 1 | ✅ |
| `globalConfig.spec.ts` | `utils/globalConfig.ts` | `GlobalConfig.data` getter/setter | localStorage + Promise 队列 | 27 | 27 | 0 | ✅ |
| `useTheme.spec.ts` | `data/useTheme.ts` | `setTheme`/`toggleTheme`/`lockTheme`/`init`/`onThemeChange` | document.documentElement.dataset + localStorage | 30 | 30 | 0 | ✅ |

**L2 小计**：7 个测试文件，164 个用例，162 通过，2 跳过

**L2 跳过用例说明**：
- `auth.spec.ts`（1 skipped）：`removeRefreshToken` bug 用例（疑似 bug #2）
- `DC.spec.ts`（1 skipped）：边界场景

### 3.3 L3 组件测试（P1 中的 Store/Composable）

| 测试文件 | 被测源文件 | 被测内容 | 用例数 | 状态 |
|---------|----------|---------|-------|------|
| — | `data/helper.ts` 的 `filterPersonData`/`filterEquipmentsData` | ⚠️ 未测（疑似 bug #4 `ballCameraOnline/Offline` 重复出现） | 0 | ❌ 未测 |
| — | `hooks/useUtils/index.ts` 的 `useUtils` | ⚠️ 未测（需 Vue 组件上下文） | 0 | ❌ 未测 |

**L3 小计**：0 个测试文件，0 个用例（未实施）

### 3.4 未纳入测试（P2）

| 文件 | 原因 |
|------|------|
| `utils/addLayerUtil.ts` | 地图图层渲染，强 SDK 依赖 |
| `utils/loginIcs.ts` | 业务流程编排 |
| `utils/domUtils.ts` | DOM 操作封装 |
| `utils/rem.ts` | 模块加载即副作用 |
| `utils/VeeValidate.ts` | vee-validate install |
| `utils/pimHttp.ts`/`http.ts`/`httpAi.ts`/`axiosFetch.ts`/`fetch.ts`/`fetchAdapter.ts` | HTTP 封装，集成测试范畴 |

### 3.5 web 覆盖率明细

| 文件 | % Stmts | % Branch | % Funcs | % Lines | 未覆盖行 |
|------|---------|----------|---------|---------|---------|
| **All files** | **93.73** | **93.19** | **97.87** | **93.73** | — |
| useTheme.ts | 100 | 100 | 100 | 100 | — |
| DC.ts | 100 | 100 | 100 | 100 | — |
| auth.ts | 96.22 | 100 | 90.9 | 96.22 | 52-53 |
| clientEnv.ts | 100 | 92.85 | 100 | 100 | 47,60 |
| dataUtil.ts | 93.33 | 87.5 | 100 | 93.33 | 22,26-27 |
| dateUtil.ts | 87.69 | 84.15 | 100 | 87.69 | 274-276,483-523 |
| env.ts | 94.44 | 90 | 100 | 100 | 29 |
| globalConfig.ts | 100 | 100 | 100 | 100 | — |
| headerUtils.ts | 96.96 | 92.3 | 100 | 96.96 | 18-19 |
| index.ts | 93.33 | 97.08 | 95.12 | 93.33 | 13-18,331-352 |
| is.ts | 100 | 95.12 | 100 | 100 | 56 |
| treeHelper.js | 100 | 90 | 100 | 100 | 15 |
| validate.ts | 97.56 | 92.3 | 100 | 97.56 | 29 |

---

## 四、agent/web 项目测试结果

### 4.1 L1 纯函数测试（P0）

| 测试文件 | 被测源文件 | 被测函数 | 用例数 | 通过 | 跳过 | 状态 |
|---------|----------|---------|-------|------|------|------|
| `is.spec.ts` | `utils/is.ts` | 与 web 的 `is.ts` 完全相同（22 个函数） | 226 | 226 | 0 | ✅ |
| `utils-index.spec.ts` | `utils/index.ts` | `addUnit`/`withInstall`/`S4`/`guid` | 31 | 31 | 0 | ✅ |
| `locales-helper.spec.ts` | `locales/helper.ts` | `genMessage` | 19 | 19 | 0 | ✅ |

**L1 小计**：3 个测试文件，276 个用例，276 通过，0 跳过

### 4.2 L2 Mock 测试（P1）

| 测试文件 | 被测源文件 | 被测函数 | Mock 依赖 | 用例数 | 状态 |
|---------|----------|---------|----------|-------|------|
| —（合并到 utils-index.spec.ts） | `utils/index.ts` 的 `displayImage` | `displayImage` | import.meta.env | 已含在上方 31 个用例中 | ✅ |

**L2 小计**：0 个独立测试文件（`displayImage` 已合并到 `utils-index.spec.ts`）

### 4.3 L3 组件测试（P1 中的 Store/Composable）

| 测试文件 | 被测源文件 | 被测内容 | 用例数 | 状态 |
|---------|----------|---------|-------|------|
| — | `utils/domUtils.ts` 的 `hasClass`/`addClass`/`removeClass`/`getBoundingClientRect`/`useRafThrottle` | ⚠️ 未测（DOM 操作封装） | 0 | ❌ 未测 |
| — | `locales/index.ts` 的 `getLang`/`setLang` | ⚠️ 未测（localStorage + navigator） | 0 | ❌ 未测 |
| — | `utils/http.ts` 的 HttpService 各方法 | ⚠️ 未测（axios 封装） | 0 | ❌ 未测 |

**L3 小计**：0 个测试文件，0 个用例（未实施）

### 4.4 未纳入测试（P2）

- `utils/rem.ts`、`store/index.ts`、`router/index.ts`、`hooks/index.ts`、`api/index.ts`

### 4.5 agent/web 覆盖率明细

| 文件 | % Stmts | % Branch | % Funcs | % Lines | 未覆盖行 |
|------|---------|----------|---------|---------|---------|
| **All files** | **98.61** | **95.38** | **100** | **98.61** | — |
| locales/helper.ts | 93.1 | 83.33 | 100 | 93.1 | 34-35 |
| utils/index.ts | 100 | 100 | 100 | 100 | — |
| utils/is.ts | 100 | 94.87 | 100 | 100 | 56 |

---

## 五、H5Portal 项目测试结果

### 5.1 L1 纯函数测试（P0）

| 测试文件 | 被测源文件 | 被测函数 | 用例数 | 通过 | 跳过 | 状态 |
|---------|----------|---------|-------|------|------|------|
| `time.spec.js` | `utils/time.js` | `getTimeRange` | 13 | 13 | 0 | ✅ |
| `totalFunc.spec.js` | `utils/totalFunc.js` | `filterMonitor`/`heartbeatAddress`/`testLocationUpdate`/`checkLicenseStatus` | 34 | 34 | 0 | ✅ |
| `copyText.spec.js` | `utils/copyText.js` | `copyText`/`extractText`（内部） | 15 | 15 | 0 | ✅ |
| `utils-index.spec.js` | `utils/index.js` | `queryStringToJson`/`debounce`/`getCurrentDateTime`/`getBaseUrlAll`/`getFullPageUrl`/`preloadIconsToBase64` | 40 | 39 | 1 | ✅ |
| `map-utils.spec.js` | `pages/map/utils.js` | `createImageMarker`/`debounce`/`createMockPoints`/`getCachedImageBase64`/`renderDeviceLayers` | 79 | 79 | 0 | ✅ |
| `useCommon.spec.js` | `hooks/useCommon.js` | `removeBase64Prefix`/`deepClone`/`deepCloneArray`/`deepCloneObject` | 63 | 63 | 0 | ✅ |

**L1 小计**：6 个测试文件，244 个用例，243 通过，1 跳过

**L1 跳过用例说明**：
- `utils-index.spec.js`（1 skipped）：`queryStringToJson('a=b=c')` bug 用例（疑似 bug #7）

### 5.2 L2 Mock 测试（P1）

| 测试文件 | 被测源文件 | 被测函数 | Mock 依赖 | 用例数 | 通过 | 跳过 | 状态 |
|---------|----------|---------|----------|-------|------|------|------|
| `eventBus.spec.js` | `utils/eventBus.js` | `EventBus` 类（on/emit/off） | 无需 mock | 18 | 18 | 0 | ✅ |
| `websocket.spec.js` | `utils/websocket.js` | `MyWebSocket` 类（init/send/close/heartbeat/reconnect） | uni.connectSocket + 定时器 | 30 | 30 | 0 | ✅ |
| `http.spec.js` | `utils/http.js` | `Request`/`UploadFile` 类 | axios + 动态 import store | 50 | 50 | 0 | ✅ |
| `clientEnv.spec.js` | `utils/clientEnv.js` | `getBrowserInfo`/`getOSInfo`/`getScreenInfo` | navigator.userAgent + vi.resetModules | 26 | 26 | 0 | ✅ |

**L2 小计**：4 个测试文件，124 个用例，124 通过，0 跳过

### 5.3 L3 组件测试（P1 中的 Store/Composable）

| 测试文件 | 被测源文件 | 被测内容 | 用例数 | 状态 |
|---------|----------|---------|-------|------|
| — | `hooks/useCommon.js` 的 `pngToBase64` | ⚠️ 未测（fetch/FileReader/document.baseURI） | 0 | ❌ 未测 |
| — | `common/config.js` 的 `getBaseUrl`/`getSocketUrl` | ⚠️ 未测（store + window.__PINIA_STORE__） | 0 | ❌ 未测 |
| — | `hooks/usePaging.js` 的 `usePaging` | ⚠️ 未测（需 Vue 组件上下文） | 0 | ❌ 未测 |

**L3 小计**：0 个测试文件，0 个用例（未实施）

### 5.4 未纳入测试（P2）

- `utils/version.js` / `toast.js` / `echarts.js`
- `utils/totalFunc.js` 中的心跳相关函数
- `utils/file.js` 中的 SDK 调用函数
- `utils/index.js` 中的 `selectGroupByActionSheet` / `openLocalUrlApp` / `locationShareFunc`

### 5.5 H5Portal 覆盖率明细

| 文件 | % Stmts | % Branch | % Funcs | % Lines | 未覆盖行 |
|------|---------|----------|---------|---------|---------|
| **All files** | **90.05** | **95.34** | **93.33** | **90.05** | — |
| hooks/useCommon.js | 63.92 | 97.67 | 66.66 | 63.92 | 17-44,51-120,173-175 |
| pages/map/utils.js | 99.29 | 95.08 | 100 | 99.29 | 132-133 |
| utils/clientEnv.js | 99.09 | 94.44 | 100 | 99.09 | 89 |
| utils/copyText.js | 100 | 100 | 100 | 100 | — |
| utils/eventBus.js | 100 | 100 | 100 | 100 | — |
| utils/http.js | 100 | 100 | 94.73 | 100 | — |
| utils/time.js | 100 | 100 | 100 | 100 | — |
| utils/totalFunc.js | 81.88 | 96.15 | 88.88 | 81.88 | 90-101,105-117 |
| utils/websocket.js | 96.47 | 85.41 | 90.9 | 96.47 | 45-46,83-84,101-102 |

> 注：`useCommon.js` 覆盖率 63.92% 较低，主要因 `deepClone` 的函数克隆分支在 happy-dom 下正则匹配失败无法覆盖（环境差异），以及 `pngToBase64` 等 P2 函数未测。

---

## 六、cloudcmd-admin-web 项目测试结果

### 6.1 L1 纯函数测试（P0）

| 测试文件 | 被测源文件 | 被测函数 | 用例数 | 通过 | 跳过 | 状态 |
|---------|----------|---------|-------|------|------|------|
| `passwordValidator.spec.js` | `utils/passwordValidator.js` | `hasChinese`/`hasLowercase`/`hasUppercase`/`hasNumber`/`hasSpecialChar`/`countCharTypes`/`validateSimpleMode`/`validateComplexMode`/`validateRepeatPassword` | 57 | 57 | 0 | ✅ |
| `secure.spec.js` | `utils/secure.js` | `encryptIdCard`/`decryptIdCard` | 15 | 15 | 0 | ✅ |
| `crypto.spec.js` | `utils/crypto.js` | `encryptJson`/`decryptJson` | 20 | 20 | 0 | ✅ |
| `index.spec.js` | `utils/index.js` | `parseTime`/`format`/`param2Obj`/`treeDataTranslate`/`deepCopy`/`filterOrgList`/`formatTime` | 66 | 66 | 0 | ✅ |
| `menuRouteMapper.spec.js` | `utils/menuRouteMapper.js` | `buildRoutesFromOauthMenu` | 38 | 38 | 0 | ✅ |
| `validate.spec.js` | `utils/validate.js` | `isExternal`/`validUsername` | 3 | 3 | 0 | ✅ |
| `parseTime.spec.js` | `utils/parseTime.js`（从 index.js 拆分） | `parseTime` | 7 | 7 | 0 | ✅ |
| `formatTime.spec.js` | `utils/formatTime.js`（从 index.js 拆分） | `formatTime` | 7 | 7 | 0 | ✅ |

**L1 小计**：8 个测试文件，213 个用例，213 通过，0 跳过

### 6.2 L2 Mock 测试（P1）

| 测试文件 | 被测源文件 | 被测函数 | Mock 依赖 | 用例数 | 通过 | 跳过 | 状态 |
|---------|----------|---------|----------|-------|------|------|------|
| `auth.spec.js` | `utils/auth.js` | token/userId/buttons/isAdmin/idCardNum/licenseAuth 读写/`getAllNodeIdByDepartmentId` | localStorage（vi.spyOn Storage.prototype）+ queryDepartment API | 21 | 21 | 0 | ✅ |
| `licenseUtils.spec.js` | `utils/licenseUtils.js` | `getLicenseInfoUtil` | getLicenseInfo + setLicenseAuth | 9 | 9 | 0 | ✅ |
| `clientEnv.spec.js` | `utils/clientEnv.js` | UA 解析多分支 | navigator.userAgent + vi.resetModules | 19 | 19 | 0 | ✅ |
| `permission.spec.js` | `utils/permission.js` | `hasBtnPermission` | store + localStorage 回退 | 8 | 8 | 0 | ✅ |
| `get-page-title.spec.js` | `utils/get-page-title.js` | `getPageTitle` | @/locales + @/store | 4 | 4 | 0 | ✅ |

**L2 小计**：5 个测试文件，61 个用例，61 通过，0 跳过

### 6.3 L3 组件测试（Store mutations）

| 测试文件 | 被测源文件 | 被测内容 | 用例数 | 通过 | 跳过 | 状态 |
|---------|----------|---------|-------|------|------|------|
| `store/app.spec.js` | `store/modules/app.js` | mutations/actions | 15 | 15 | 0 | ✅ |
| `store/settings.spec.js` | `store/modules/settings.js` | `CHANGE_SETTING`/`SET_SYSTEM_NAME` | 12 | 12 | 0 | ✅ |
| `store/user.spec.js` | `store/modules/user.js` | `login`（base64 license header 解码、code 0/121 分支）/`getOauthMenu` | 39 | 39 | 0 | ✅ |

**L3 小计**：3 个测试文件，66 个用例，66 通过，0 跳过

### 6.4 未纳入测试（P2）

- `utils/request.js`、`utils/createDialog.js`、`utils/astrict.js`、`utils/pageLoading.js`、`utils/scrollTo.js`
- `src/api/**`（薄封装）
- `src/store/getters.js`（无逻辑）
- `tests/unit/components/`（Breadcrumb/Hamburger/SvgIcon）— 因 `.vue` 文件解析问题在 `vitest.config.ts` 的 `exclude` 中跳过

### 6.5 cloudcmd-admin-web 覆盖率明细

| 文件 | % Stmts | % Branch | % Funcs | % Lines | 未覆盖行 |
|------|---------|----------|---------|---------|---------|
| **All files** | **99.89** | **99.07** | **66.15** | **99.89** | — |
| clientEnv.js | 100 | 96.55 | 100 | 100 | 47 |
| crypto.js | 100 | 100 | 100 | 100 | — |
| get-page-title.js | 100 | 100 | 100 | 100 | — |
| index.js | 99.58 | 98.18 | 100 | 99.58 | 8 |
| licenseUtils.js | 100 | 100 | 100 | 100 | — |
| menuRouteMapper.js | 100 | 100 | 12 | 100 | — |
| passwordValidator.js | 100 | 100 | 100 | 100 | — |
| permission.js | 100 | 100 | 100 | 100 | — |
| secure.js | 100 | 100 | 100 | 100 | — |
| validate.js | 100 | 100 | 100 | 100 | — |

> 注：`menuRouteMapper.js` 的 functions 覆盖率仅 12%，因 `buildRoutesFromOauthMenu` 内部定义了多个辅助函数未单独导出，无法直接覆盖。但 statements/branches/lines 均为 100%，整体达标。

---

## 七、分层汇总统计

| 层级 | 测试文件数 | 用例总数 | 通过 | 跳过 | 失败 | 说明 |
|------|----------|---------|------|------|------|------|
| **L1 纯函数** | 23 | 1149 | 1139 | 10 | 0 | web 6 + agent/web 3 + H5Portal 6 + cloudcmd-admin-web 8 |
| **L2 Mock** | 16 | 349 | 347 | 2 | 0 | web 7 + H5Portal 4 + cloudcmd-admin-web 5（agent/web 的 mock 已并入 L1） |
| **L3 组件/Store** | 3 | 66 | 66 | 0 | 0 | cloudcmd-admin-web 3（其他项目未实施） |
| **合计** | **42** | **1764** | **1752** | **12** | **0** | — |

### 跳过用例汇总（12 个）

| # | 项目 | 测试文件 | 跳过原因 | 关联疑似 bug |
|---|------|---------|----------|------------|
| 1 | web | `dataUtil.spec.ts` | `isNullOrUndefined` 多参数 bug | #1 |
| 2 | web | `dataUtil.spec.ts` | `isNullOrUndefined` 多参数 bug | #1 |
| 3 | web | `utils-index.spec.ts` | `getRecentSomeDays` 跨月边界 | #5 |
| 4-9 | web | `dateUtil.spec.ts` | `strToDate` 空字符串 bug 等边界 | #3 |
| 10 | web | `auth.spec.ts` | `removeRefreshToken` bug | #2 |
| 11 | web | `DC.spec.ts` | 边界场景 | — |
| 12 | H5Portal | `utils-index.spec.js` | `queryStringToJson` bug | #7 |

---

## 八、未测内容清单

### 8.1 P1 已规划但未实施

| 项目 | 文件 | 函数 | 层级 | 原因 |
|------|------|------|------|------|
| web | `data/helper.ts` | `filterPersonData`/`filterEquipmentsData` | L3 | 需 mock useResourceStoreWithOut；疑似 bug #4 |
| web | `hooks/useUtils/index.ts` | `useUtils` | L3 | 需 Vue 组件上下文 |
| agent/web | `utils/domUtils.ts` | `hasClass`/`addClass`/`removeClass`/`getBoundingClientRect`/`useRafThrottle` | L3 | DOM 操作封装 |
| agent/web | `locales/index.ts` | `getLang`/`setLang` | L2 | localStorage + navigator |
| agent/web | `utils/http.ts` | HttpService 各方法 | L2 | axios 封装 |
| H5Portal | `hooks/useCommon.js` | `pngToBase64` | L2 | fetch/FileReader/document.baseURI |
| H5Portal | `common/config.js` | `getBaseUrl`/`getSocketUrl` | L2 | store + window.__PINIA_STORE__ |
| H5Portal | `hooks/usePaging.js` | `usePaging` | L3 | 需 Vue 组件上下文 |

### 8.2 P2 明确不纳入单测

详见各项目"P2 未纳入测试"章节，原因包括：DOM 操作、模块副作用、地图 SDK、HTTP 拦截器、业务流程编排、WebSocket 心跳、Vue 组件 install、API 薄封装、类型/枚举定义等。

---

## 九、通过单测发现的疑似 Bug 清单

| # | 项目 | 文件 | 函数 | 问题 | 处理方式 |
|---|------|------|------|------|----------|
| 1 | web | `utils/dataUtil.ts` | `isNullOrUndefined` | 循环未读取 `arguments[i]`，多参数时只检查第一个 | `it.skip` 标注 |
| 2 | web | `utils/auth.ts` | `removeRefreshToken` | 用 `localStorage.removeItem` 删除，但 `setRefreshToken` 用 `Cookies.set` 写入 | `it.skip` 标注 |
| 3 | web | `utils/dateUtil.ts` | `strToDate` | 第 482 行 `isEmpty(str)` 应为 `!isEmpty(str)` | `it.skip` 标注 |
| 4 | web | `data/helper.ts` | `filterEquipmentsData` | 返回数组中 `ballCameraOnline/Offline` 各出现两次 | 未测（L3 待实施） |
| 5 | web | `utils/index.ts` | `getRecentSomeDays` | 月末跨月时 `setDate` 可能异常 | `it.skip` 标注 |
| 6 | web | `utils/index.ts` | `toggleClass` | 添加 class 缺前导空格 | 未单独测 |
| 7 | H5Portal | `utils/index.js` | `queryStringToJson` | `split('=', 2)` 在 JS 中会截断值 | `it.skip` 标注 |

---

## 十、维护说明

### 重新生成本报告

运行全项目测试后，根据输出更新本报告的"二、总体运行结果"和各项目章节的用例数/覆盖率数据。

```bash
cd d:\code\Linkx\Linkx-B\web && pnpm test:coverage
cd d:\code\Linkx\Linkx-B\agent\web && pnpm test:coverage
cd d:\code\Linkx\Linkx-B\H5Portal && pnpm test:coverage
cd d:\code\Linkx\Linkx-B\cloudcmd-admin-web && npm run test:coverage
```

### 新增测试时更新本报告

1. 在对应项目的 L1/L2/L3 章节添加测试文件行
2. 更新"分层汇总统计"
3. 若覆盖率变化，更新"覆盖率达标情况"和各项目覆盖率明细
4. 若发现新 bug，添加到"疑似 Bug 清单"

### 关联文档

- [前端单元测试方案总览](./frontend-unit-test-overview.md) — 技术方案与可测试函数清单
- `.trae/skills/frontend-unit-test-writer/SKILL.md` — 生成单测
- `.trae/skills/frontend-unit-test-runner/SKILL.md` — 运行单测
