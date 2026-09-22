# 数据库设计规范

> 参考: 阿里巴巴 Java 开发手册（MySQL 数据库章节）、Google SQL Style Guide

## 一、表设计规范

### 1.1 强制规则

- 表名使用小写 + 下划线分隔: `user_info`、`order_detail`
- 表名使用业务域前缀: `sys_user`、`biz_order`、`log_operation`
- 每张表必须有主键，类型 `BIGINT ` 主键必须采用雪花算法生成
- 每张表必须有 `COMMENT` 注释 
- 字符集统一 `utf8mb4`，排序规则 `utf8mb4_general_ci`
- 存储引擎统一 `InnoDB`
- 单表字段数 ≤ 50，超过考虑垂直拆分
- 禁止使用外键，应用层保证数据一致性
- 禁止使用存储过程、视图、触发器，业务逻辑放应用层
- 禁止使用保留字作为表名/字段名（如 `order`、`group`、`key`）

### 1.2 字段设计

| 规则 | 说明 |
|------|------|
| 字段名小写+下划线 | `user_name`、`order_status` |
| 字段必须有 COMMENT | 每个字段都要注释 |
| 布尔字段用 `is_` 前缀 | `is_deleted`、`is_active`（TINYINT 1/0） |
| 金额用 DECIMAL | `DECIMAL(18,2)`，禁止 FLOAT/DOUBLE |
| 时间用 DATETIME/TIMESTAMP | 禁止用 VARCHAR 存时间 |
| IP 地址用 INT UNSIGNED | `INET_ATON`/`INET_NTOA` 转换 |
| 枚举用 TINYINT | 禁止用 ENUM 类型（扩展困难） |
| 文本长度固定用 CHAR | 如手机号 `CHAR(11)` |
| 文本长度可变用 VARCHAR | 按需设长度，禁止一律 `VARCHAR(255)` |
| 大文本用 TEXT/JSON | 超过 5000 字符用 TEXT |
| 逻辑删除字段 | `is_deleted TINYINT DEFAULT 0` |

### 1.3 公共字段

每张业务表必须包含:

| 字段 | 类型 | 说明 |
|----|------|------|
| `id` | BIGINT  | 主键 |
| `gmt_created` | datetime  | 创建时间 |
| `gmt_modified` | datetime  | 修改时间 |
| `is_deleted` | TINYINT DEFAULT 0 | 逻辑删除（0:未删除 1:已删除） |

## 二、索引规范

### 2.1 强制规则

- 单表索引数不超过 5 个
- 联合索引字段数不超过 3 个
- 联合索引遵循最左前缀原则，字段顺序按区分度降序排列
- 禁止在低区分度字段上建索引（如 `gender`、`is_deleted`）
- 禁止冗余索引（已有 `(a,b)` 则无需单独建 `a`）
- 禁止重复索引
- 唯一索引名: `uk_字段名`
- 普通索引名: `idx_字段名`

### 2.2 索引失效场景（禁止）

- WHERE 条件对字段使用函数: `YEAR(create_time) = 2025` → 改为范围查询
- WHERE 条件隐式类型转换: `varchar_field = 123` → `varchar_field = '123'`
- LIKE 左模糊: `LIKE '%abc'` → 全文索引或搜索引擎
- OR 条件中有无索引字段
- 联合索引跳过左列: 索引 `(a,b,c)`，查询条件只有 `b`

### 2.3 索引优化建议

```sql
-- 禁止: 函数导致索引失效
SELECT * FROM orders WHERE YEAR(gmt_create) = 2025;

-- 正确: 范围查询走索引
SELECT * FROM orders WHERE gmt_create >= '2025-01-01' AND gmt_create < '2026-01-01';
```

## 三、SQL 规范

### 3.1 强制规则

- **禁止 `SELECT *`**，明确指定字段
```sql
-- 禁止: select *
SELECT * FROM user WHERE id = 1;
```


- **禁止大表 JOIN**（超过 3 张表 JOIN 需人工确认）

- **禁止不带 WHERE 的 UPDATE/DELETE**

  ```sql
  -- 禁止: 不带条件update、delete
  update t_user set name = 'xx';
  
  delete from t_user;
  ```

- INSERT 必须指定字段名，禁止依赖字段顺序
- 使用 `IS NULL`/`IS NOT NULL` 判空，禁止 `= NULL`
- 避免深分页: `LIMIT 100000, 10` → 使用游标方案
- 事务中禁止 RPC 调用，避免长事务
- 事务范围尽量小，`@Transactional` 只加在 Service 方法上

### 3.2 分页优化

```sql
-- 禁止: 深度分页全表扫描
SELECT * FROM orders ORDER BY id LIMIT 100000, 10;

-- 正确: 游标分页
SELECT * FROM orders WHERE id > 100000 ORDER BY id LIMIT 10;
```

### 3.3 批量操作

- 批量 INSERT 单次不超过 500 条
- 批量 UPDATE/DELETE 分批执行，每批 500 条

## 四、数据库迁移

### 4.1 Flyway 规范

> Flyway 命名规范、版本格式、DDL/DML 分离、可重复执行、修改已创建脚本需人工确认等项目特有约定见 `.trae/rules/backend.md` §10。

通用规则:
- 变更必须向后兼容（新增字段设默认值、不删除字段）

## 五、MyBatis-Plus 规范

### 5.1 Mapper 命名

- Mapper 接口: `XxxMapper` extends `BaseMapper<XxxDO>`
- XML 文件: 与 Mapper 接口同路径

### 5.2 查询规范

- 简单查询（单表查询）用 MyBatis-Plus QueryWrapper
- 复杂查询（多表 JOIN、子查询）写 XML SQL
- 禁止在 Java 代码中拼接 SQL
- 分页使用 `Page` 对象，禁止手动 LIMIT

```java
// 简单查询
LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(UserDO::getStatus, 1)
       .like(StringUtils.isNotBlank(keyword), UserDO::getName, keyword)
       .orderByDesc(UserDO::getGmtCreate);
Page<UserDO> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
```
