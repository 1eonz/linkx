# LinkX Encryptor - 国密加解密服务

## 项目简介

LinkX Encryptor 是基于国密算法（SM2/SM4）的加解密服务，提供文本和文件的加解密功能，支持跨机房调用。

### 核心功能

- ✅ **SM2 非对称加密**：支持文本加解密
- ✅ **SM4 对称加密**：支持文本和文件加解密
- ✅ **流式文件处理**：接收文件上传，流式返回处理后的文件
- ✅ **跨机房支持**：无需共享文件系统，支持跨机房调用
- ✅ **多环境配置**：支持 dev/test/prod 环境切换
- ✅ **加密方式切换**：支持 SDK 和 BouncyCastle 切换
- ✅ **RESTful API**：提供标准 REST 接口
- ✅ **监控指标**：集成 Actuator 和 Prometheus

---

## 技术栈

- **Java 21**
- **Spring Boot 3.2.5**
- **SdkSM 2.1** - 国密算法实现
- **Lombok** - 代码简化

---

## 快速开始

### 1. 环境要求

- JDK 21+
- Maven 3.6+

### 2. 编译项目

```bash
cd code/linkx-encryptor
mvn clean compile
```

### 3. 启动服务

#### 方式一：使用默认配置（开发环境）
```bash
mvn spring-boot:run
```

#### 方式二：指定环境启动
```bash
# 测试环境
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=test

# 生产环境
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod
```

#### 方式三：通过环境变量启动
```bash
# Linux/Mac
export ENV=prod
export SM_HOST=https://ucpsapi.pimse.gab.ydxxw
export SM_SECRET=aoeiuv
mvn spring-boot:run

# Windows PowerShell
$env:ENV="prod"
$env:SM_HOST="https://ucpsapi.pimse.gab.ydxxw"
$env:SM_SECRET="aoeiuv"
mvn spring-boot:run
```

#### 方式四：JAR 包启动
```bash
# 打包
mvn clean package

# 启动（指定环境）
java -jar target/linkx-encryptor.jar --spring.profiles.active=prod

# 或通过环境变量
java -DENV=prod -jar target/linkx-encryptor.jar
```

### 4. 验证启动

访问健康检查接口：

```bash
curl http://localhost:8099/actuator/health
```

响应：
```json
{
  "status": "UP"
}
```

---

## 配置说明

### 环境配置文件

项目支持以下环境配置文件：
- `application.yml` - 主配置（支持环境变量覆盖）
- `application-dev.yml` - 开发环境
- `application-test.yml` - 测试环境
- `application-prod.yml` - 生产环境

### 配置项说明

#### 必需配置项（通过环境变量或配置文件）

| 配置项 | 环境变量 | 说明 |
|--------|---------|------|
| 环境标识 | ENV | 环境标识（dev/test/prod） |
| SM 服务地址 | SM_HOST | SM 服务地址 |
| SM 密钥 | SM_SECRET | SM 密钥 |

#### 可选配置项（有默认值）

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|---------|--------|------|
| HTTP 连接器 | HTTP_CONNECTOR | false | HTTP 连接器开关 |
| BZ 服务 ID | BZ_SERVERID | bz_server.01 | BZ 服务 ID |
| Pod UID | POD_UID | 1234567890 | Pod UID |
| 加密类型 | ENCRYPT_TYPE | SDK/BouncyCastle | 加密服务类型 |
| SM4 密钥 | SM4_KEY | 1234567890123456 | SM4 密钥（16字节） |
| SM2 公钥 | SM2_PUBLIC_KEY | - | SM2 公钥（Base64） |
| SM2 私钥 | SM2_PRIVATE_KEY | - | SM2 私钥（Base64） |

### 加密服务类型说明

| 类型 | 说明 | 适用环境 | 支持功能 |
|------|------|---------|---------|
| SDK | 使用 SdkSM 的国密算法（SM2/SM4） | 生产环境 | 文本+文件流式 |
| BOUNCYCASTLE | 使用 BouncyCastle 开源库（SM2/SM4） | 开发/测试环境 | 文本+文件流式 |

**重要说明**：
- ✅ **SDK**：生产环境推荐，真正的国密算法，支持所有功能
- ✅ **BouncyCastle**：开源国密实现，无需外部服务，支持文本和文件流式

**各环境默认加密类型**：
- 开发环境（dev）：BOUNCYCASTLE（开源实现，支持所有功能）
- 测试环境（test）：BOUNCYCASTLE（开源实现，支持所有功能）
- 生产环境（prod）：SDK（生产级实现）

---

## API 接口

### 文本加解密接口

#### 1. SM2 加密
```bash
POST /linkx/encryptor/sm2/encrypt
Content-Type: application/json

{
  "data": "Hello World"
}
```

#### 2. SM2 解密
```bash
POST /linkx/encryptor/sm2/decrypt
Content-Type: application/json

{
  "data": "加密结果（Base64）"
}
```

#### 3. SM4 加密
```bash
POST /linkx/encryptor/sm4/encrypt
Content-Type: application/json

{
  "data": "Hello World"
}
```

#### 4. SM4 解密
```bash
POST /linkx/encryptor/sm4/decrypt
Content-Type: application/json

{
  "data": "加密结果（Base64）"
}
```

---

### 文件流式加解密接口（跨机房）

#### 5. SM4 文件流式加密
```bash
POST /linkx/encryptor/sm4/stream/encrypt
Content-Type: multipart/form-data

参数：
- file: 待加密的文件（必填）
- bufferSize: 缓冲区大小（可选，默认 8192 字节）

响应：加密后的文件流
```

**特点**：
- ✅ 接收文件上传，流式返回加密文件
- ✅ 无需共享文件系统
- ✅ 支持跨机房调用
- ✅ 适合大文件处理

#### 6. SM4 文件流式解密
```bash
POST /linkx/encryptor/sm4/stream/decrypt
Content-Type: multipart/form-data

参数：
- file: 待解密的文件（必填）
- bufferSize: 缓冲区大小（可选，默认 8192 字节）

响应：解密后的文件流
```

---

## 使用示例

### curl 命令

#### 文本加解密
```bash
# SM2 加密
curl -X POST http://localhost:8099/linkx/encryptor/sm2/encrypt \
  -H "Content-Type: application/json" \
  -d '{"data": "Hello World"}'

# SM4 加密
curl -X POST http://localhost:8099/linkx/encryptor/sm4/encrypt \
  -H "Content-Type: application/json" \
  -d '{"data": "Hello World"}'
```

#### 文件流式加解密
```bash
# 加密文件
curl -X POST http://localhost:8099/linkx/encryptor/sm4/stream/encrypt \
  -F "file=@source.txt" \
  -o encrypted.txt

# 解密文件
curl -X POST http://localhost:8099/linkx/encryptor/sm4/stream/decrypt \
  -F "file=@encrypted.txt" \
  -o decrypted.txt

# 指定缓冲区大小
curl -X POST http://localhost:8099/linkx/encryptor/sm4/stream/encrypt \
  -F "file=@source.txt" \
  -F "bufferSize=16384" \
  -o encrypted.txt
```

---

## 性能建议

### 缓冲区大小选择

| 文件大小 | 推荐缓冲区 | 说明 |
|----------|-----------|------|
| < 10MB | 8KB (8192) | 默认值，适合小文件 |
| 10MB - 100MB | 16KB (16384) | 中等文件 |
| 100MB - 1GB | 32KB (32768) | 大文件 |
| > 1GB | 64KB (65536) | 超大文件 |

### 内存优化

- ✅ 流式处理，不会一次性加载整个文件到内存
- ✅ 内存占用 = 缓冲区大小
- ✅ 支持处理 GB 级别文件

---

## 项目结构

```
linkx-encryptor/
├── src/main/java/com/tdtech/linkx/encryptor/
│   ├── EncryptorApplication.java          # 启动类
│   ├── config/
│   │   ├── SdkProperties.java             # SDK 配置属性
│   │   ├── SecServiceConfig.java          # 国密服务配置
│   │   └── EncryptServiceConfig.java      # 加密服务动态配置
│   ├── controller/
│   │   └── EncryptController.java         # 加密控制器
│   ├── service/
│   │   ├── EncryptService.java            # 服务接口
│   │   ├── IEncryptService.java           # 加密服务接口
│   │   ├── EncryptMode.java               # 加密模式枚举
│   │   └── impl/
│   │       ├── EncryptServiceImpl.java    # 服务实现
│   │       ├── SdkEncryptServiceImpl.java # SDK 实现
│   │       └── Base64EncryptServiceImpl.java # Base64 实现
│   ├── request/
│   │   └── TextEncryptRequest.java        # 文本加解密请求
│   ├── resp/
│   │   └── R.java                         # 统一响应对象
│   ├── exception/
│   │   └── GlobalExceptionHandler.java    # 全局异常处理
│   └── enums/
│       ├── AlgorithmEnum.java             # 算法枚举
│       └── ResponseCodeEnum.java          # 响应码枚举
├── src/main/resources/
│   ├── application.yml                    # 主配置文件
│   ├── application-dev.yml                # 开发环境配置
│   ├── application-test.yml               # 测试环境配置
│   └── application-prod.yml               # 生产环境配置
├── lib/
│   └── SdkSM-2.1-RELEASE.Java21.jar       # 国密SDK
├── API_DOCUMENT.md                        # API详细文档
├── STREAM_API_GUIDE.md                    # 流式接口使用指南
├── ENCRYPT_SERVICE_REFACTOR.md            # 加密服务重构说明
└── README.md                              # 项目说明
```

---

## 核心实现

### 加密服务架构

```
IEncryptService (接口)
    ├── SdkEncryptServiceImpl (SDK 实现 - 国密算法)
    └── Base64EncryptServiceImpl (Base64 实现 - 测试用)
            ↓
    EncryptServiceConfig (动态选择)
            ↓
    EncryptServiceImpl (业务服务)
```

### 流式文件处理

```
调用方 → HTTP请求（文件流） → 服务方边读边加密 → HTTP响应（加密流） → 调用方接收
```

**优势**：
- ✅ 无需共享文件系统
- ✅ 支持跨机房调用
- ✅ 内存高效
- ✅ 支持大文件

---

## 监控指标

### Actuator 端点

- **健康检查**：`/actuator/health`
- **应用信息**：`/actuator/info`
- **指标数据**：`/actuator/metrics`
- **Prometheus**：`/actuator/prometheus`

### 示例

```bash
# 健康检查
curl http://localhost:8099/actuator/health

# 查看指标
curl http://localhost:8099/actuator/metrics

# Prometheus 格式
curl http://localhost:8099/actuator/prometheus
```

---

## 多环境部署

### CodeArts 构建

详细配置请参考 `ci/codearts-pipeline-config.md`

#### 环境变量配置

| 参数名 | 类型 | 说明 |
|--------|------|------|
| ENV | 枚举 | dev/test/prod |
| SM_HOST | 普通 | SM 服务地址 |
| SM_SECRET | 密文 | SM 密钥（推荐密文类型） |
| ENCRYPT_TYPE | 普通 | 加密类型（SDK/BASE64） |

### Docker 部署

```bash
# 构建镜像
docker build -t linkx-encryptor:latest .

# 运行容器
docker run -d \
  -e ENV=prod \
  -e SM_HOST=https://ucpsapi.pimse.gab.ydxxw \
  -e SM_SECRET=aoeiuv \
  -e ENCRYPT_TYPE=SDK \
  -p 8099:8099 \
  linkx-encryptor:latest
```

### Kubernetes 部署

详细配置请参考 `ci/kubernetes-deployment.yaml`

---

## 常见问题

### 1. 启动失败：找不到 ISecService 类

**原因**：SdkSM jar 包未正确加载

**解决**：检查 lib 目录下是否存在 `SdkSM-2.1-RELEASE.Java21.jar`

### 2. 加密失败

**原因**：输入数据格式不正确

**解决**：
- 检查数据是否为空
- 检查数据编码是否为 UTF-8
- 查看日志获取详细错误信息

### 3. SDK 服务连接失败

**原因**：SM_HOST 或 SM_SECRET 配置错误

**解决**：
- 检查 SM_HOST 是否正确
- 检查 SM_SECRET 是否正确
- 使用 ENCRYPT_TYPE=BASE64 进行测试

### 4. 文件上传失败

**原因**：文件大小超过限制

**解决**：调整配置
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 6GB
      max-request-size: 6GB
```

---

## 安全建议

1. **密钥管理**
   - 使用环境变量或密钥管理服务
   - 不要在代码中硬编码密钥
   - 定期更换密钥

2. **加密类型选择**
   - 生产环境必须使用 SDK（国密算法）
   - Base64 仅用于开发测试环境
   - Base64 不提供安全保护

3. **数据传输**
   - 使用 HTTPS 加密传输
   - 敏感数据不要记录日志

4. **文件权限**
   - 加密文件设置适当的访问权限
   - 原始文件加密后及时删除

---

## 相关文档

- [API_DOCUMENT.md](API_DOCUMENT.md) - API 详细文档
- [STREAM_API_GUIDE.md](STREAM_API_GUIDE.md) - 流式接口使用指南
- [BOUNCYCASTLE_IMPLEMENTATION.md](BOUNCYCASTLE_IMPLEMENTATION.md) - BouncyCastle 实现说明
- [KEY_MANAGEMENT.md](KEY_MANAGEMENT.md) - 密钥管理说明
- [FILE_DECRYPT_USAGE.md](FILE_DECRYPT_USAGE.md) - 文件解密接口使用说明
- [QUICK_TEST_GUIDE.md](QUICK_TEST_GUIDE.md) - 快速测试指南

---

## 更新日志

### v1.0.0 (2024-01-01)

- ✅ 实现 SM2 文本加解密
- ✅ 实现 SM4 文本加解密
- ✅ 实现 SM4 文件流式加解密（跨机房）
- ✅ 支持多环境配置（dev/test/prod）
- ✅ 支持加密方式切换（SDK/BouncyCastle）
- ✅ 添加 BouncyCastle 开源实现
- ✅ 完善错误处理和日志记录
- ✅ 添加参数验证
- ✅ 集成监控指标
- ✅ 移除 AMQP 依赖
- ✅ 全局异常处理

---

## 许可证

内部项目，仅供授权使用。

---

## 联系方式

如有问题，请联系开发团队。
