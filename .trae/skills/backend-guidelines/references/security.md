# 后端安全编码规范

> 参考: OWASP Top 10、阿里巴巴 Java 开发手册（安全规约）、Google Java Style Guide

## 一、SQL 注入防护

### 强制规则

- **禁止字符串拼接 SQL**，必须使用 `PreparedStatement` 或 ORM 参数化查询
- MyBatis 中必须使用 `#{}`，禁止使用 `${}` 拼接用户输入（`${}` 仅允许用于动态表名/列名）
- JPA/Hibernate 使用 `@Param` 参数绑定，禁止原生 SQL 拼接

```java
// 禁止
String sql = "SELECT * FROM users WHERE name = '" + name + "'";

// 正确
String sql = "SELECT * FROM users WHERE name = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, name);
```

### MyBatis 规则

```xml
<!-- 正确: 参数化 -->
<select id="getUser" resultType="User">
  SELECT * FROM users WHERE name = #{name}
</select>

<!-- 禁止: 字符串替换 -->
<select id="getUser" resultType="User">
  SELECT * FROM users WHERE name = '${name}'
</select>
```

## 二、XSS 防护

### 强制规则

- 所有用户输入输出到 HTML 时必须进行编码转义
- 使用模板引擎默认转义（Thymeleaf `th:text`、JSP `<c:out>`），禁止使用 `th:utext` 除非内容可信
- 富文本场景必须使用白名单过滤（如 Jsoup），禁止直接输出原始 HTML

```java
// OWASP Java Encoder
import org.owasp.encoder.Encode;
String safeOutput = Encode.forHtml(userInput);      // HTML 上下文
String safeJs = Encode.forJavaScript(userInput);     // JS 上下文
String safeUrl = Encode.forUriComponent(userInput);  // URL 上下文

// Jsoup 富文本白名单过滤
String safe = Jsoup.clean(userInput, Safelist.relaxed());
```

## 三、CSRF 防护

### 强制规则

- Spring Security 默认启用 CSRF 保护，**不得关闭**（除非无状态 REST API 使用 Token 认证）
- 敏感操作（支付、改密、删除）必须二次确认
- Cookie 设置 `SameSite=Lax` 或 `Strict`

## 四、认证与授权

### 强制规则

- 密码存储必须使用 BCrypt/PBKDF2/Argon2 加盐哈希，**禁止 MD5/SHA-1**
- 遵循最小权限原则: RBAC 模型，角色与资源通过中间表显式声明

```java
// 密码存储
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
String encoded = encoder.encode(rawPassword);


```

## 五、敏感数据保护

### 强制规则

- 传输全程 HTTPS（TLS 1.2+），禁止 HTTP 明文传输
- 日志中禁止打印密码、身份证号等敏感信息，必须脱敏
- 配置中的数据库密码（对称加密算法）、API Key 等必须加密存储（Nacos 加密配置/Jasypt），禁止明文入库

```yaml
# Logback 脱敏配置示例
logging:
  pattern:
    console: "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %replace(%msg){'password=\\S+', 'password=***'}%n"
```

## 六、文件上传安全

### 强制规则

- 校验文件类型: Content-Type 
- 限制文件大小（不同文件类型限制不同，具体数值见 `.trae/rules/backend.md` §9）
- 文件存储路径禁止使用用户输入，防止目录穿越
- 上传文件隔离至独立域名/时间/存储桶，禁止与应用同域

## 七、反序列化安全

### 强制规则

- 禁止使用 `ObjectInputStream` 直接反序列化不可信数据
- 优先使用 JSON 序列化（Jackson/Gson）替代 Java 原生序列化

## 八、HTTP 安全头

### 强制规则

所有 HTTP 响应必须包含以下安全头:

| Header | 值 | 作用 |
|--------|----|----|
| Strict-Transport-Security | `max-age=31536000; includeSubDomains` | 强制 HTTPS |
| X-Content-Type-Options | `nosniff` | 禁止 MIME 嗅探 |
| X-Frame-Options | `DENY` | 防点击劫持 |
| X-XSS-Protection | `1; mode=block` | 浏览器 XSS 过滤 |
| Content-Security-Policy | 按业务配置 | 限制资源加载源 |

## 九、依赖安全

### 强制规则

- 禁用不必要的 HTTP 方法（TRACE、OPTIONS）

## 十、异常处理安全

> 异常处理的详细规范和代码示例见 [exception-logging.md](./exception-logging.md)

### 强制规则

- 异常响应禁止返回堆栈信息、SQL 语句、内部路径等技术细节
- 统一异常处理返回标准 JSON 响应体
- 日志记录完整堆栈（仅服务端），前端只返回通用错误消息

## 十一、数据库安全

> 数据库设计、索引、SQL 规范见 [database.md](./database.md)

### 强制规则

- 数据库连接密码必须加密配置（Jasypt/Nacos 加密）
