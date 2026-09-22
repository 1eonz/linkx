---
name: backend-guidelines
description: "Linkx 后端项目规范与最佳实践唯一入口。强触发: .java/.xml/.yml/.yaml/.properties/.sql 等后端文件，agent/server/、ccmd/ 任一目录，Spring Boot/Spring Cloud/Nacos/Dubbo/MyBatis-Plus/MyBatis-Plus-Join/MapStruct/Lombok/OpenFeign/Resilience4j/Sentinel/Redisson/Flyway/RestController/Service/Mapper/Transactional/PreAuthorize/全局异常处理/日志脱敏/接口签名/幂等/分布式事务/熔断限流/错误码/SQL 优化/索引设计/分页优化/缓存 key 等关键词。覆盖: Java 编码规范、接口设计、数据库设计、微服务架构、异常日志、错误码体系、安全编码、业务规则。"
alwaysApply: false
---

# 后端规范工作流

本技能是 Linkx 项目后端规范的唯一入口。项目特有约定（模块结构、Lombok 配置、服务编码段、缓存 key 命名、超时控制、文件上传限制、Flyway 命名）见 `.trae/rules/backend.md`，本技能承载详细的编码规范、设计指南、安全教程、业务规则。

## 触发场景

任一命中即加载：
- 任务涉及 `.java`/`.xml`/`.yml`/`.yaml`/`.properties`/`.sql` 等后端文件
- 任务涉及 `agent/server/`、`ccmd/` 任一目录
- 用户提及 Spring Boot/Spring Cloud/Nacos/Dubbo/MyBatis-Plus/MapStruct/Lombok/Feign/Resilience4j/Sentinel/Redisson/Flyway 等后端框架或工具
- 用户提及 RestController/Service/Mapper/Transactional/PreAuthorize/全局异常处理/日志脱敏/接口签名/幂等/分布式事务/熔断限流/错误码/SQL 优化/索引设计/分页优化/缓存 key 等后端概念
- 写/改后端代码、设计 RESTful 接口、数据库表设计、微服务架构设计、后端安全审查

不应触发：纯前端任务（→ `frontend-guidelines`）、纯运维任务。

## 工作流

### Step 1: 确认目标项目

修改前先确认目标项目（agent/server 或 ccmd），项目栈差异见 `.trae/rules/backend.md` 第一章。两个项目的 Java 版本、Spring Boot 版本、RPC 框架、ORM、日志实现均不同。

### Step 2: 按需加载 references

根据任务类型加载对应 references，不要全量加载：

| 任务 | 必读 references |
|------|----------------|
| Java 通用编码（命名/OOP/集合/并发/注释） | `java.md` |
| 设计 RESTful 接口、开放接口签名 | `api-design.md` |
| 建表、索引设计、SQL 优化、MyBatis-Plus | `database.md` |
| 微服务架构、熔断/限流/降级、分布式事务 | `microservice.md` |
| 异常处理、日志规范 | `exception-logging.md` |
| 错误码定义、查询完整错误码枚举 | `error-code.md` |
| 后端安全审查（SQL 注入/XSS/认证/加密） | `security.md` |
| 业务幂等、批量操作、缓存、操作日志 | `business-rules.md` |

### Step 3: 应用规范并自检

按 reference 中的规范实施，完成后对照 `.trae/rules/backend.md` 验证项目特定约定（模块路径、包名、Lombok 配置、服务编码段、缓存 key 格式、超时设置）未被破坏。

## references 索引

- `java.md` — Java 编码规范详版（命名规范、格式化、OOP 规约、集合处理、并发处理、注释规范、agent/server 与 ccmd 各自规范）
- `api-design.md` — 接口设计规范详版（RESTful 规范、版本管理、请求参数、响应结构、开放接口鉴权签名防重放限流、内部接口规范、接口文档注解、接口性能）
- `database.md` — 数据库设计规范详版（表设计、字段设计、公共字段、索引规范、SQL 规范、分页优化、批量操作、Flyway 迁移、MyBatis-Plus 规范）
- `microservice.md` — 微服务架构详版（服务拆分原则、服务治理、服务间通信、容错与韧性、分布式事务、可观测性、数据管理、部署运维）
- `exception-logging.md` — 异常与日志规范详版（异常分类、自定义业务异常、全局异常处理、日志框架选择、日志级别、日志格式、日志脱敏、日志配置要求、日志打印时机）
- `error-code.md` — 错误码体系详版（错误码格式、服务/模块/类型编码表、全局通用错误码 900000-909999、系统服务错误码 100000-109999、ErrorCode 枚举完整定义）
- `security.md` — 后端安全编码详版（SQL 注入防护、XSS 防护、CSRF 防护、认证授权、敏感数据保护、文件上传安全、反序列化安全、HTTP 安全头、依赖安全、异常处理安全、数据库安全）
- `business-rules.md` — 后端业务规则详版（接口幂等性、幂等 Token 机制、操作日志、枚举管理、批量操作、导入导出、缓存使用）

## 约束

1. 修改代码前必须确认目标项目（agent/server 或 ccmd），禁止跨项目混用风格
2. references 按需加载，不要全量加载
3. 项目特有约定（模块路径/包名/Lombok 配置/服务编码段/缓存 key 格式/超时设置/Flyway 命名）以 `.trae/rules/backend.md` 为准，本技能 references 不重复
4. 完整错误码枚举仅在 `error-code.md` reference，rules 只保留编码规则
