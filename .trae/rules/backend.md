---
alwaysApply: false
globs: agent/server/**,ccmd/**,*.java,*.xml,*.yml,*.yaml,*.properties,*.sql
description: agent/server 与 ccmd 两个后端项目的开发任务：接口设计、数据库与 SQL、MyBatis-Plus、Spring Cloud/Dubbo 微服务、Nacos 配置中心、错误码、缓存 key、Feign/Dubbo 服务通信、Flyway 迁移、Lombok、文件上传、超时与容错
---

# 后端项目特有约定

> 通用 Java 编码规范、微服务教程、安全教程、SQL 优化教程、完整错误码枚举不在此文档，由 `backend-guidelines` 技能按需加载。本文件仅记录 2 个后端项目的差异化约定，修改前先确认目标项目。

> **何时调用 `backend-guidelines` 技能**: 需要完整错误码枚举（`references/error-code.md`）、SQL 优化细则与索引设计、Spring Cloud/Dubbo/MyBatis-Plus 教程、安全编码细节（`security.md`）、分布式事务/熔断限流、Flyway 高级用法时调用。本文件未覆盖的通用后端知识均由该技能提供。

## 〇、全局约束

- 🔴 **先确认项目，再遵循规则**: 修改前确认目标项目，遵循其自身配置，不要跨项目混用风格
- 🔴 **后端技术栈差异**: agent/server 是 Java17+SpringBoot3+Nacos；ccmd 是 Java11+SpringBoot2+Dubbo
- 🔴 **安全红线**: SQL 禁止拼接、密码禁止 MD5/明文、敏感数据必须脱敏、全站 HTTPS（详见 backend-guidelines 技能的 security.md）
- 🟡 **Git 分支保护**: main/develop 禁止直接 push，PR 至少 1 人审查

> 规则级别: 🔴 强制 / 🟡 推荐 / 🔵 参考

## 一、项目栈

| 项目 | 路径 | Java | Spring Boot | RPC | 注册中心 | ORM | 日志 | 包名 |
|------|------|------|------------|-----|---------|-----|------|------|
| agent/server | agent/server/ | 17 | 3.4.5 | Spring Cloud | Nacos | MyBatis-Plus + MyBatis-Plus-Join | Logback | com.chinasoft.cloud |
| ccmd | ccmd/ | 11 | 2.7.18 | Dubbo 3.2 | Nacos/Zookeeper | MyBatis-Plus 3.5.15 | Log4j2 | com.tdtech.cloudcmd |

## 二、agent/server 模块结构

- `crs-dependencies/` — BOM 依赖管理
- `crs-framework/` — 框架层（mybatis/redis/security/web/mq/job/websocket 等 starter）
- `crs-gateway/` — API 网关（Spring Cloud Gateway）
- `crs-module-system/` — 系统管理模块（api + server）
- `crs-module-infra/` — 基础设施模块（api + server，含代码生成器，支持 Vue3 模板）
- `crs-module-ai-agent/` — AI Agent 模块（api + server）
- `tools/mock-ai/` — Mock AI 工具

模块分层: `*-api`（接口定义）+ `*-server`（实现）
配置中心: Nacos（`optional:nacos:${spring.application.name}-${spring.profiles.active}.yaml`）

## 三、ccmd 模块结构

- `cloudcmd-boot/` — 启动依赖聚合
- `cloudcmd-common/` — 公共模块（util/log/i18n/redis/mybatis/dubbo/encryptor 等）
- `cloudcmd-service/` — 业务服务（admin/auth/base/cagent/gateway/im/dashboard/node/third/icp）
- `cloudcmd-heterogeneity/` — 异构服务（script/sql-runner）
- `linkx-encryptor/` — 加密服务
- `tools/` — 工具（data-tool/cnx）
- `package/` — 打包部署配置

配置: bootstrap.yml

## 四、Lombok 配置

lombok.config:
- `config.stopBubbling = true`
- `lombok.tostring.callsuper = CALL`
- `lombok.equalsandhashcode.callsuper = CALL`
- `lombok.accessors.chain = true`（链式调用）

注解处理器顺序: Lombok → MapStruct（agent/server 使用 MapStruct）

## 五、错误码服务编码段

8 位格式: `{服务编码2位}{模块编码2位}{错误类型2位}{序号2位}`

服务编码段分配:

| 编码 | 服务 |
|------|------|
| 10 | 系统服务 (system) |
| 20 | 基础设施服务 (infra) |
| 30 | AI Agent 服务 (ai-agent) |
| 40 | 网关服务 (gateway) |
| 50 | 用户服务 (user) |
| 60 | 订单服务 (order) |
| 70 | 支付服务 (payment) |
| 80 | 通知服务 (notification) |
| 90 | 通用/公共 (common) |

> 新增服务编码由架构组分配，预留 91-99
> 完整错误码枚举详见 `backend-guidelines` 技能的 `references/error-code.md`

## 六、缓存 key 命名

格式: `{服务名}:{模块}:{业务标识}`，如 `cloudcmd:base:globals:MAP_ACTIVE_MAX_COUNT`

## 七、服务间通信与容错

| 项目 | 同步查询 | 高性能 RPC | 熔断方案 |
|------|---------|-----------|---------|
| agent/server | OpenFeign | — | Spring Cloud CircuitBreaker (Resilience4j) |
| ccmd | — | Dubbo 3.2 | Dubbo Mock / Sentinel |

## 八、超时控制

| 层级 | 超时设置 |
|------|---------|
| 网关 | 连接 3s，读 30s |
| Feign/Dubbo | 连接 1s，读 5s |
| 数据库 | 查询 5s |
| HTTP Client | 连接 2s，读 10s |

## 九、文件上传大小限制

| 类型 | 上限 |
|------|------|
| 图片 | 10MB |
| 视频 | 100MB |
| 其他文档 | 100MB |

## 十、Flyway 迁移脚本

- 命名: `V{version}__{description}.sql`，version 格式 `101.01`（如 `V101.01__LINKX_SPC170B001.sql`）
- DDL 和 DML 分开脚本
- 脚本必须可重复执行
- 修改已创建的脚本需人工确认
