# 后端项目概览与 Java 规范

## 两个后端项目

| 项目 | 路径 | Java | Spring Boot | Spring Cloud | 微服务框架 | 注册中心 |
|------|------|------|-------------|-------------|-----------|---------|
| agent/server | `agent/server/` | 17 | 3.4.5 | — | Spring Cloud (Nacos) | Nacos |
| ccmd | `ccmd/` | 11 | 2.7.18 | 2021.0.9 | Dubbo 3.2 | Nacos/Zookeeper |

## agent/server 模块结构

- `crs-dependencies/` — BOM 依赖管理
- `crs-framework/` — 框架层（mybatis/redis/security/web/mq/job/websocket 等 starter）
- `crs-gateway/` — API 网关
- `crs-module-system/` — 系统管理模块（api + server）
- `crs-module-infra/` — 基础设施模块（api + server，含代码生成）
- `crs-module-ai-agent/` — AI Agent 模块（api + server）
- `tools/mock-ai/` — Mock AI 工具

## ccmd 模块结构

- `cloudcmd-boot/` — 启动依赖聚合
- `cloudcmd-common/` — 公共模块（util/log/i18n/redis/mybatis/dubbo/encryptor 等）
- `cloudcmd-service/` — 业务服务（admin/auth/base/cagent/gateway/im/dashboard/node/third/icp）
- `cloudcmd-heterogeneity/` — 异构服务（script/sql-runner）
- `linkx-encryptor/` — 加密服务
- `tools/` — 工具（data-tool/cnx）
- `package/` — 打包部署配置

## Java 编码规范

> 参考: 阿里巴巴 Java 开发手册（泰山版）、Google Java Style Guide

### 通用规则

- 使用 Lombok（@Data/@Getter/@Setter/@Builder 等），lombok.config 配置:
  - `config.stopBubbling = true`
  - `lombok.tostring.callsuper=CALL`
  - `lombok.equalsandhashcode.callsuper=CALL`
  - `lombok.accessors.chain=true`（链式调用）
- 使用 MapStruct 做对象映射（agent/server），注解处理器顺序: Lombok → MapStruct
- UTF-8 编码，`project.build.sourceEncoding=UTF-8`
- 包命名: `com.chinasoft.cloud`（agent/server）、`com.tdtech.cloudcmd`（ccmd）

### 命名规范

| 类型 | 风格 | 示例 | 禁止 |
|------|------|------|------|
| 类名 | UpperCamelCase | `UserService`、`OrderController` | 拼音、缩写歧义 |
| 方法名 | lowerCamelCase | `getUserById`、`calculateTotal` | 下划线分隔 |
| 变量名 | lowerCamelCase | `userName`、`orderList` | 单字母（i/j/k 除外）、拼音 |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`、`DEFAULT_PAGE_SIZE` | 驼峰式常量 |
| 包名 | 全小写 | `com.chinasoft.cloud.user` | 大写、下划线 |
| 枚举类 | UpperCamelCase | `Gender`、`OrderStatus` | — |
| 枚举值 | UPPER_SNAKE_CASE | `MALE`、`PENDING` | 驼峰式 |
| 抽象类 | Base/Xxx 前缀 | `BaseController`、`AbstractHandler` | — |
| 接口 | I 前缀或形容词 | `IUserService` 或 `Runnable` | — |
| 实现类 | 接口名 + Impl | `UserServiceImpl` | — |
| 测试类 | 被测类 + Test | `UserServiceTest` | — |
| VO/DTO/BO | 后缀标注 | `UserVO`、`OrderDTO`、`UserBO` | 混用 |

补充规则:
- 布尔变量/方法以 `is`/`has`/`can`/`should` 开头: `isActive`、`hasPermission`
- 避免魔法值，使用常量或枚举替代
- Service/DAO 层方法命名约定: `get`（单查询）、`list`（列表）、`count`（计数）、`save`/`insert`（新增）、`update`（修改）、`delete`/`remove`（删除）

### 格式化规范

- 缩进: 4 空格（禁止 Tab）
- 行宽: 每行不超过 120 字符
- 大括号: K&R 风格（左大括号不换行）
- `if`/`for`/`while`/`do` 即使单行也必须使用大括号
- 运算符两侧各一个空格: `a + b`，而非 `a+b`
- 逗号/分号后加空格: `func(a, b, c)`
- 方法之间空一行，逻辑块之间空一行

### OOP 规约

- 避免通过类的对象引用来访问静态变量或方法（用类名访问）
- 避免可变参数（`Object... args`），类型确定用数组，不确定用集合
- 构造方法禁止加入业务逻辑，初始化逻辑放 `@PostConstruct` 或 `init()` 方法
- POJO 类必须写 toString 方法（Lombok `@ToString(callSuper = CALL)`）
- 禁止在 POJO 中同时使用 `@Data` 和手动 getter/setter（Lombok 管全部或不管）
- 类成员顺序: 静态变量 → 实例变量 → 构造方法 → 公有方法 → 私有方法 → 内部类
- 方法参数不超过 5 个，超过则封装为对象
- 方法圈复杂度不超过 10

### 集合处理

- 集合初始化指定容量: `new ArrayList<>(16)`、`new HashMap<>(64)`
- `ArrayList` 的 `subList` 返回的视图不可强转为 `ArrayList`
- `Map` 禁止 `keySet` 遍历取 value，用 `entrySet`
- `foreach` 内禁止 `add`/`remove` 操作，用 `Iterator` 或 `removeIf`
- 集合转数组用 `toArray(T[])`，禁止 `toArray()` 无参
- 数组转集合用 `Arrays.asList()` 返回的是固定大小 List，修改需 `new ArrayList<>(Arrays.asList(...))`
- 禁止 `Collections.EMPTY_LIST` 等返回可变集合，用 `Collections.emptyList()`
- 泛型集合优先使用接口类型声明: `List<String> list = new ArrayList<>()`

### 并发处理

- 线程资源必须通过线程池提供，禁止显式创建线程 `new Thread()`
- 线程池不允许使用 `Executors` 创建（避免 OOM），用 `ThreadPoolExecutor` 构造
- `SimpleDateFormat` 非线程安全，使用 `DateTimeFormatter`（Java 8+）
- 对多线程共享变量必须使用 `volatile`/`synchronized`/`Atomic*`/`Lock`
- `synchronized` 块尽量小，避免大块同步
- 多线程锁顺序必须一致，避免死锁
- `CountDownLatch`/`CyclicBarrier`/`Semaphore` 使用后必须确保释放

### 注释规范

- 类/接口必须有 Javadoc: 说明用途、作者、版本
- 公有方法必须有 Javadoc: 含 `@param`、`@return`、`@throws`
- 注释描述"为什么"而非"是什么"
- 禁止无意义注释: `// 设置名称`、`// 增加 i`
- TODO/FIXME 必须带责任人和时间: `// TODO(zhangsan 2025-01-01): 优化查询逻辑`
- 代码修改时同步更新注释，禁止注释与代码不一致
- 禁止注释掉的代码，无用的代码直接删除（Git 有历史）

### agent/server 规范

- Java 17，Spring Boot 3.4.5
- 配置中心: Nacos（`optional:nacos:${spring.application.name}-${spring.profiles.active}.yaml`）
- ORM: MyBatis-Plus + MyBatis-Plus-Join
- 模块分层: `*-api`（接口定义）+ `*-server`（实现）
- 代码生成: crs-module-infra 内置代码生成器（支持 Vue3 模板）

### ccmd 规范

- Java 11，Spring Boot 2.7.18，Spring Cloud 2021.0.9

- RPC: Dubbo 3.2.19（Zookeeper 注册中心）

- ORM: MyBatis-Plus 3.5.15

- 配置: bootstrap.yml
