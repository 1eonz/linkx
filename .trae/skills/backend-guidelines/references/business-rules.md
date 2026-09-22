# 后端业务自定义编码规范

> 本文件记录团队业务层面的自定义规则，随业务演进持续补充

## 一、接口幂等性

### 1.1 强制幂等的操作

| 操作类型 | 幂等方案 | 说明 |
|----------|----------|----------|
| 支付/扣款 | 幂等 Token + 状态机 | 防重复扣款 |
| 订单创建 | 唯一业务键（如: userId+productId+时间窗口） | 防重复下单 |
| 库存扣减 | 乐观锁 / 数据库原子更新 | 防超卖 |
| 状态变更 | CAS + 状态机校验 | 防并发状态覆盖 |
| 消息消费 | 消息 ID 去重 | 防重复消费 |

```java
// 幂等 Token 方案示例
public Result<String> createOrder(CreateOrderDTO dto) {
    // 1. 校验幂等 Token
    String idempotentKey = "order:create:" + dto.getIdempotentToken();
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 10, TimeUnit.MINUTES);
    if (Boolean.FALSE.equals(acquired)) {
        return Result.error(ErrorCode.DUPLICATE_REQUEST);
    }

    // 2. 业务处理
    Order order = orderService.createOrder(dto);
    return Result.success(order.getId());
}
```

### 1.2 幂等 Token 机制

- 前端进入提交页面时，先请求获取幂等 Token
- 提交时将 Token 传入后端
- 后端校验 Token: 首次请求消费 Token 并执行业务，重复请求直接拒绝

## 二、操作日志

### 2.1 强制记录的操作

| 操作类型 | 记录内容 | 保留时长 |
|----------|----------|----------|
| 登录/登出 | 操作人、IP、时间、结果 | 6 个月 |
| 数据新增 | 操作人、模块、数据摘要 | 6 个月 |
| 数据修改 | 操作人、模块、修改前/后值 | 6 个月 |
| 数据删除 | 操作人、模块、删除数据快照 | 6 个月 |
| 权限变更 | 操作人、目标用户、变更内容 | 1 年 |
| 敏感操作 | 操作人、操作内容、二次验证记录 | 1 年 |

## 三、枚举管理

### 3.1 强制规则

- 业务状态、类型等必须使用枚举，禁止魔法值
- 枚举类统一放在对应模块的 `enums` 包下
- 枚举必须包含 `code`（存储值）和 `desc`（描述）
- 数据库存储枚举的 `code` 值（TINYINT/SMALLINT）

```java
@Getter
@AllArgsConstructor
public enum OrderStatus implements IEnum<Integer> {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getValue() {
        return code;
    }
}
```

## 五、批量操作

### 5.1 批量操作限制

| 操作 | 单次上限 | 说明 |
|------|----------|------|
| 批量新增 | 500 条 | 超过需分批 |
| 批量修改 | 200 条 | 超过需分批 |
| 批量删除 | 100 条 | 必须二次确认 |
| 批量导出 | 10000 条 | 超过使用异步导出 |

### 5.2 批量删除规范

- 逻辑删除优先，物理删除需审批
- 批量删除必须记录操作日志
- 批量删除返回成功/失败明细

```java
@OperationLog(module = "用户管理", action = "批量删除用户", type = OperType.DELETE)
@DeleteMapping("/users/batch")
public Result<BatchResult> batchDeleteUsers(@RequestBody List<Long> ids) {
    // 1. 执行删除
    List<Long> deletable = ids.stream().filter(id -> !undeletable.contains(id)).toList();
    userService.batchDelete(deletable);
    // 2. 返回明细
    return Result.success(new BatchResult(deletable.size(), undeletable.size(), undeletable));
}
```

## 六、导入导出

### 6.1 导入规范

- 使用 EasyExcel 处理 Excel 导入
- 导入模板通过接口下载，禁止前端自行生成
- 导入数据必须校验: 格式、必填、业务规则
- 导入结果返回明细: 成功数、失败数、失败原因
- 大量数据导入使用异步 + 进度查询

### 6.2 导出规范

- 导出字段需脱敏（手机号、身份证号）
- 超过 10000 条使用异步导出（生成文件 → 通知下载）
- 导出操作记录操作日志

## 七、缓存使用

### 7.1 缓存场景

| 场景 | 缓存方案 | 过期时间 |
|------|----------|----------|
| 字典数据 | Redis Hash | 24 小时 |
| 用户权限 | Redis Hash | 登录期间 |
| 热点查询 | Redis String/Hash | 5-30 分钟 |
| 接口限流计数 | Redis String + TTL | 按窗口 |
| 分布式锁 | Redis SETNX | 30 秒 |

### 7.2 缓存规则

- 数据变更时主动失效缓存
- 缓存穿透: 空值缓存 5 分钟
- 缓存击穿: 热点 key 使用分布式锁重建

> 缓存 key 命名格式（`{服务名}:{模块}:{业务标识}`）见 `.trae/rules/backend.md` §6。

---

## 自定义规则追加区

> 以下区域供团队持续补充业务自定义规则，格式: `### N.x 规则名称`

### 待补充

- （在此追加新的业务规则）
