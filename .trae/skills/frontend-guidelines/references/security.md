# 前端安全编码规范

> 参考: OWASP Top 10、OWASP ASVS、CWE
>
> 适用范围: web/、agent/web/、H5Portal（Vue3）与 cloudcmd-admin-web（Vue2）。本仓库前端栈仅 Vue，不涉及 React/Angular。

## 一、XSS 防护

### 强制规则

- 所有用户输入必须进行校验和转义，禁止直接使用 `innerHTML` 插入不可信内容
- 优先使用 `textContent` / `innerText` 替代 `innerHTML`
- 富文本必须使用白名单过滤库净化后再渲染
- 开启 CSP（Content-Security-Policy），限制脚本、样式、图片加载来源

### Vue 框架专项（Vue3 web/agent/web/H5Portal 与 Vue2 admin）

| 版本 | 安全做法 | 危险 API |
|------|----------|----------|
| Vue3 | 使用 `{{ }}` 插值（自动转义）、`v-text` | 慎用 `v-html`，必须配合白名单净化库 |
| Vue2 | 使用 `{{ }}` 插值、`v-text` | 同上 |

> **DOMPurify 依赖现状**: web/agent/web、H5Portal、admin 的 `package.json` **均未安装** `dompurify`。若业务确需渲染富文本（如 IM 消息、公告），必须先在对应项目安装 `dompurify` 并经净化后再 `v-html`；未安装时禁止使用 `v-html` 渲染任何外部/用户内容。

> **与 ESLint 的关系**: `vue/no-v-html` 在所有项目均为 `off`（lint 不强制），但业务上仍按本节执行——安全约束不依赖 lint。

```javascript
// Vue3/Vue2: v-html 必须净化（需先安装 dompurify）
import DOMPurify from 'dompurify';
const sanitizedHtml = DOMPurify.sanitize(rawHtml);
```

### CSP 配置示例

```
Content-Security-Policy: default-src 'self'; script-src 'self' https://trusted.cdn.com; style-src 'self' 'unsafe-inline'
```

## 二、CSRF 防护

### 强制规则

- 所有敏感操作必须携带 CSRF Token 并在服务端验证
- Cookie 设置 `SameSite=Lax` 或 `Strict`，配合 `Secure` 和 `HttpOnly`
- 高风险操作（转账、改密）需二次确认（验证码/短信/弹窗）
- 禁止用 GET 请求执行状态变更操作

## 三、点击劫持防护

### 强制规则

- 后端返回 `X-Frame-Options: DENY` 或 `Content-Security-Policy: frame-ancestors 'self'`
- 前端检测页面是否被 iframe 嵌套，必要时跳出

```javascript
if (window.top !== window.self) {
  window.top.location = window.self.location;
}
```

## 四、前端存储安全

### 强制规则

- **禁止**在 `localStorage` / `sessionStorage` 存储敏感信息（Token、密码、身份证号）
- Token 使用 `HttpOnly Cookie` 存储，避免被 XSS 读取
- 如必须前端存储，需加密/混淆并控制生命周期
- 禁止在前端代码中硬编码 API Key、密钥、密码

```javascript
// 禁止
const API_KEY = 'sk-xxxxxxxxxxxx';  // 硬编码密钥

// 正确: 通过环境变量注入
const API_KEY = import.meta.env.VITE_API_KEY;
```

## 五、依赖安全

### 强制规则

- 定期运行 `npm audit` / `pnpm audit` / `snyk test` 扫描依赖漏洞
- 只使用可信赖的第三方库和 CDN
- CDN 资源开启 SRI（Subresource Integrity）校验
- 移除未使用的依赖，减少攻击面
- 锁定依赖版本（`package-lock.json` / `pnpm-lock.yaml`）

```html
<!-- SRI 校验 -->
<script src="https://cdn.example.com/lib.js"
        integrity="sha384-xxxxx"
        crossorigin="anonymous"></script>
```

## 六、HTTPS 与传输安全

### 强制规则

- 全站启用 HTTPS，禁止 HTTP 明文传输
- 配置 HSTS: `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- 禁止混合内容（HTTPS 页面加载 HTTP 资源）

## 七、敏感信息保护

### 强制规则

- 不在前端代码里硬编码密钥、密码、内部接口路径
- 使用 `.env` 管理环境变量，构建时区分 dev/prod
- 后端代理第三方 API，不让前端直接调用带密钥的接口
- 接口返回数据脱敏（手机号 `138****8888`、身份证 `110***********1234`）
- 生产环境关闭 `console.log` 和错误堆栈输出

## 八、文件上传安全

### 强制规则

- 前端限制文件类型和大小
- 后端必须再次验证 MIME 类型和扩展名
- 上传文件名进行处理，避免执行脚本
- 禁止将上传文件放在 Web 可直接访问的路径

## 九、重定向与跳转安全

### 强制规则

- 避免开放重定向，跳转 URL 使用白名单
- 不信任 query 参数中的跳转链接

```javascript
// 禁止: 直接使用用户输入的 URL
window.location.href = redirectUrl;

// 正确: 白名单校验
const allowedDomains = ['example.com', 'app.example.com'];
const url = new URL(redirectUrl);
if (allowedDomains.includes(url.hostname)) {
  window.location.href = redirectUrl;
}
```

## 十、请求与接口安全

> 开放接口签名机制的详细规范见后端 `.trae/rules/backend.md` 与 `backend-guidelines` 技能的 `api-design.md` reference

### 强制规则

- 接口需鉴权与限流，防止暴力刷接口
- 使用验证码/滑块防止恶意请求
- 接口数据脱敏返回
- 请求签名防篡改（timestamp + sign + nonce）

## 十一、危险函数禁止

### 强制规则

| 函数 | 风险 | 替代方案 |
|------|------|----------|
| `eval()` | 代码注入 | `JSON.parse()` / `Function()` |
| `new Function(str)` | 代码注入 | 预定义函数 |
| `setTimeout(str)` / `setInterval(str)` | 代码注入 | 传入函数引用 |
| `document.write()` | DOM 篡改 | DOM API 操作 |
| `innerHTML` (不可信内容) | XSS | `textContent` / 白名单净化库 |

## 十二、Vue3 项目专项安全（web/、agent/web/、H5Portal）

> admin（Vue2）不适用本节，遵循前述通用安全规则即可。

### 强制规则

- 使用 `<script setup>`（web/agent/web 强制 `lang="ts"`，H5Portal 以 JS 为主）+ `defineProps` 类型/校验增强类型安全
- `v-html` 仅用于可信内容或经净化库处理后的内容（见第一节 DOMPurify 现状）
- 组件 emits 必须显式声明（`defineEmits`，与 [lint-config.md](./lint-config.md) `require-explicit-emits` 对齐）
- 路由守卫中做权限校验，但**核心权限校验必须在后端完成**
- 前端权限控制仅用于 UI 展示/隐藏，不作为安全边界
