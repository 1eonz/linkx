# 异常与日志规范

> 参考: 阿里巴巴 Java 开发手册（异常日志章节）、Google Java Style Guide

## 一、异常处理

### 1.1 异常分类

| 类型 | 基类 | 场景 | 处理方式 |
|------|------|------|----------|
| 业务异常 | `BusinessException` extends RuntimeException | 参数校验失败、业务规则违反 | 捕获后返回业务错误码+提示 |
| 系统异常 | `SystemException` extends RuntimeException | 外部服务不可用、配置错误 | 捕获后记录日志+返回通用错误 |
| 框架异常 | Spring/MyBatis 等框架抛出 | SQL 错误、网络超时 | 全局处理器统一捕获 |
| 不可恢复错误 | `Error` | OOM、StackOverflow | 不捕获，让 JVM 处理 |

### 1.2 自定义业务异常

```java
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }
}
```

### 1.3 强制规则

- **禁止空 catch 块**: 捕获异常后至少记录日志
- **禁止 `e.printStackTrace()`**: 无法输出到日志系统且丢失上下文
- **禁止生吞异常**: catch 后仅打印日志不做任何处理
- **禁止用异常做流程控制**: 异常仅用于真正的异常场景，不用 try-catch 替代 if-else
- **异常必须保留原始原因**: 使用 `new BusinessException(msg, cause)` 传递 cause
- **checked 异常必须处理或声明**: 不允许忽略 IOException 等
- **运行时异常通过前置校验规避**: `Objects.requireNonNull()`、`CollectionUtils.isEmpty()`
- **方法返回值可以为 null 时使用 Optional**: 避免调用方 NPE

### 1.4 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: code={}, msg={}, path={}", e.getCode(), e.getMessage(), request.getRequestURI());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: path={}", request.getRequestURI(), e);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}
```

## 二、日志规范

### 2.1 框架选择

| 项目 | 日志门面 | 日志实现 |
|------|----------|----------|
| agent/server | SLF4J | Logback |
| ccmd | SLF4J | Log4j2 |

- **禁止 `System.out.println()`** 和 `System.err.println()`
- **禁止 `e.printStackTrace()`**
- 日志对象声明: `private static final Logger log = LoggerFactory.getLogger(XxxClass.class);`
- Lombok 可用 `@Slf4j` 简化

### 2.2 日志级别

| 级别 | 场景 | 示例 |
|------|------|------|
| ERROR | 不可恢复错误，需人工介入 | 数据库连接池耗尽、第三方服务持续不可用 |
| WARN | 可降级/可恢复的异常 | 第三方接口超时降级、配置项缺失使用默认值 |
| INFO | 核心业务流程里程碑 | 订单创建成功、用户登录、定时任务执行 |
| DEBUG | 开发调试信息 | 方法入参出参、中间变量值 |
| TRACE | 极细粒度调试 | 循环内每次迭代的值 |

规则:
- 生产环境日志级别: INFO
- 开发/测试环境: DEBUG
- ERROR 日志必须包含完整堆栈 + 上下文信息
- WARN 日志需附带修复建议或影响说明
- INFO 日志禁止出现敏感信息

### 2.3 日志格式

- 使用占位符 `{}`，**禁止字符串拼接**

```java
// 禁止
log.info("用户登录: userId=" + userId + ", name=" + name);

// 正确
log.info("用户登录: userId={}, name={}", userId, name);
```

- 结构化日志必须包含: traceId、业务主键、操作人

```java
log.info("创建订单: traceId={}, orderId={}, userId={}, amount={}",
    traceId, orderId, userId, amount);
```

### 2.4 日志脱敏

| 数据类型 | 脱敏规则 | 示例 |
|----------|----------|----------|
| 手机号 | 中间 4 位 | `138****8888` |
| 身份证 | 中间 8 位 | `110***********1234` |
| 密码 | 全部 | `******` |
| 银行卡 | 中间保留 | `6222****1234` |
| Token | 全部 | `******` |
| 邮箱 | 用户名部分 | `z***@example.com` |

### 2.5 日志配置要求

- 生产环境使用异步 Appender（AsyncAppender），避免日志 IO 阻塞业务线程
- 日志文件滚动: 按天 + 按大小（单文件 ≤ 200MB），保留 30 天
- 日志文件编码: UTF-8
- 敏感操作日志保留 6 个月（安全合规要求）

### 2.6 日志打印时机

| 时机 | 级别 | 说明 |
|------|------|------|
| 方法入口 | DEBUG | 记录入参（脱敏后） |
| 方法出口 | DEBUG | 记录出参和耗时 |
| 业务异常 | WARN | 记录错误码+描述+上下文 |
| 系统异常 | ERROR | 记录完整堆栈+上下文 |
| 外部调用前 | INFO | 记录目标+参数 |
| 外部调用后 | INFO | 记录结果+耗时 |
| 关键业务操作 | INFO | 记录操作人+操作内容+结果 |
| 定时任务 | INFO | 记录开始+结束+耗时+处理数量 |
