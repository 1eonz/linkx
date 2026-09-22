# 接口设计规范

> 参考: RESTful API 设计原则、OpenAPI 3.1 规范、阿里巴巴 API 网关规范、Google API Design Guide

## 一、核心设计原则

| 原则 | 说明 |
|------|------|
| 简洁统一 | 请求方式、参数格式、返回结构、命名规则全局统一 |
| 语义清晰 | 路径、参数、字段命名贴合业务，见名知意 |
| 安全可控 | 自带鉴权、防重放、限流、数据脱敏能力 |
| 兼容迭代 | 支持版本管理，新增功能不破坏旧接口 |
| 容错性强 | 完善的异常捕获、错误提示、兜底机制 |
| 性能最优 | 合理分页、缓存、字段精简 |

## 二、RESTful 规范

### 2.1 URL 设计

- 路径使用名词（资源），禁止动词
- 使用复数名词表示集合: `/api/users`、`/api/orders`
- 资源层级不超过 2 层，深层关系用查询参数
- 全小写，多单词用连字符 `-` 分隔

```
✅ GET  /api/v1/users              # 用户列表
✅ GET  /api/v1/users/123          # 单个用户
✅ POST /api/v1/users              # 创建用户
✅ GET  /api/v1/users/123/orders   # 用户的订单列表
✅ GET  /api/v1/orders?user_id=123 # 推荐: 查询参数替代深层嵌套

❌ GET  /api/getUserList           # 禁止动词
❌ GET  /api/user/123              # 禁止单数
❌ GET  /api/users/123/orders/456/items/789  # 禁止过深嵌套
```

### 2.2 HTTP 方法语义

| 方法 | 语义 | 幂等 | 安全 | 请求体 |
|------|------|------|------|--------|
| GET | 查询资源 | 是 | 是 | 无 |
| POST | 创建资源/复杂查询 | 否 | 否 | 有 |
| PUT | 全量更新 | 是 | 否 | 有 |
| PATCH | 局部更新 | 否 | 否 | 有 |
| DELETE | 删除资源 | 是 | 否 | 无 |

### 2.3 禁止事项

- 禁止用 GET 执行状态变更操作
- 禁止滥用 POST（查询操作用 GET，更新操作用 PUT/PATCH）
- 禁止在 URL 中传递敏感参数（密码、Token）

## 三、版本管理

### 3.1 版本策略

- 版本号嵌入 URL 路径: `/api/v1/users`
- 主版本号变更: 不兼容的破坏性变更（字段删除/类型变更）
- 次版本号变更: 新增字段/接口（向后兼容）
- 旧版本至少保留 3 个月过渡期

### 3.2 兼容性规则

- 新增字段默认可选，不破坏旧客户端
- 禁止直接删除字段，先标记 `@Deprecated`，下个版本移除
- 数据类型变更需兼容旧值

## 四、请求参数规范

### 4.1 公共参数

所有接口统一携带:

| 参数 | 说明 | 必填 |
|------|------|------|
| `Authorization` | 认证 Token（Header） | 是（除公开接口） |

### 4.2 参数命名

- 字段名使用 `camelCase`（与 JSON/Java 一致）
- 参数命名语义明确，禁止缩写（除通用缩写如 id、url）
- 布尔类型以 `is`/`has`/`can` 开头: `isActive`、`hasPermission`
- 分页参数统一: `pageNum`（页码，从 1 开始）、`pageSize`（每页条数）

### 4.3 参数校验

- 必填参数必须校验（`@NotNull`、`@NotBlank`）
- 使用 `@Validated` 分组校验

## 五、响应结构规范

### 5.1 统一响应体

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 业务状态码，0 表示成功 |
| message | string | 响应描述 |
| data | object/array/null | 业务数据 |

### 5.2 分页响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [ ... ],
    "total": 100,
    "current": 1,
    "size": 20
  }
}
```

### 5.3 错误响应

```json
{
  "code": 1,
  "message": "参数校验失败: 用户名不能为空",
  "data": null,
}
```

### 5.4 HTTP 状态码使用

| 状态码 | 场景 |
|--------|------|
| 200 | 成功 |
| 201 | 资源创建成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 429 | 请求限流 |
| 500 | 服务端错误 |

**禁止所有接口一律返回 200 + 业务错误码，应正确使用 HTTP 状态码。**

## 六、开放接口 vs 内部接口

### 6.1 定义与区分

| 维度 | 开放接口 | 内部接口 |
|------|----------|----------|
| 面向 | 第三方开发者/合作伙伴 | 内部服务/前端 |
| 鉴权 | AppKey + AppSecret + 签名 | Token（JWT/Session） |
| 文档 | 公开 API 文档（OpenAPI） | 内部文档 |
| 版本 | 严格版本管理，长期兼容 | 可灵活迭代 |
| 限流 | 严格限流（按 AppKey） | 按服务/用户限流 |
| 变更通知 | 提前 30 天通知 + 兼容期 | 内部协调即可 |
| 数据脱敏 | 严格脱敏 | 按需脱敏 |

### 6.2 开放接口规范

#### 鉴权签名机制

```
       byte[] randomBytes = new byte[24];
        numberGenerator.nextBytes(randomBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte randomByte : randomBytes) {
            String hex = Long.toHexString(0xff & randomByte);
            hexString.append(hex);
        }
        return hexString.toString();
```

#### 防重放

- timestamp 与服务器时间差 > 5 分钟，拒绝请求
- nonce 在 5 分钟内不可重复（Redis 去重）

#### 限流

- 按 AppKey 限流: 默认 100 QPS，可申请提升
- 按接口限流: 高消耗接口单独限制

#### 错误码体系

- 开放接口使用标准化错误码，文档公开
- 错误码格式: `{服务编码}{错误类型}{序号}`（如 `1004001` = 用户服务-参数错误-001）

### 6.3 内部接口规范

#### 鉴权

- 用户请求: JWT Token（网关验证后转发）
- 服务间调用: 内部 Token / 网关透传用户上下文

#### 服务间调用

- 通过 Feign/Dubbo 调用，走内网
- 设置超时和熔断

## 七、接口文档

### 7.1 注解规范

```java
@Tag(name = "用户管理", description = "用户 CRUD 接口")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户信息")
    @Parameter(name = "id", description = "用户ID", required = true)
    @ApiResponse(responseCode = "200", description = "成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) { ... }
}
```

## 八、接口性能

### 8.1 分页

- 列表接口必须支持分页
- 默认 pageSize ≤ 100，禁止无限制查询
- 深度分页使用游标方案（cursor-based）

### 8.2 缓存

- GET 请求可缓存，响应头设置 `Cache-Control` / `ETag`
- 数据变更时主动失效缓存

### 8.3 批量操作

> 批量操作的限制和规范详见 [business-rules.md](./business-rules.md) 第五章

- 批量接口单次不超过 100 条
- 批量操作使用异步 + 回调模式，避免长耗时阻塞
