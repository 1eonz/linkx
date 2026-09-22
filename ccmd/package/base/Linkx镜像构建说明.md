# Docker 镜像构建说明

本文档整合了 `java11`、`java21`、`javacv-11`、`javacv-21` 四套镜像的构建步骤。每套镜像均区分 `arm64` 与 `amd64` 两种架构，并分别在不同的服务器上构建。

> ⚠️ **重要前提说明**：
> 1. 所有 Dockerfile 文件均存放于代码仓 `Linkx/ccmd/package/base` 目录下，构建前请先从代码仓拉取最新版本的 Dockerfile。
> 2. `java11`、`java21` 基础镜像（`openeuler-openjre11:11.0.24.3`、`openeuler-openjre:21.0.2.4`）由平台提供，构建时需确保构建服务器已可拉取/访问这些基础镜像，无需自行制作。
> 3. `java-cv` 镜像仅适用于使用了 OpenCV 的微服务，目前只有 `cloudcmd-im-jingxin` 服务使用，非 OpenCV 类服务请使用对应的 `jre` 基础镜像。

---

## 一、构建服务器信息

| 架构 | 服务器地址 | 端口 | 账号 | 密码 |
| --- | --- | --- | --- | --- |
| X86（amd64） | 172.26.130.24 | 9190 | root | eLTE@com |
| ARM（arm64） | 192.168.0.130 | 9092 | root | eLTE@com |

> 说明：amd64 镜像需登录 `172.26.130.24:9190` 服务器构建；arm64 镜像需登录 `192.168.0.130:9092` 服务器构建。

---

## 二、镜像依赖关系

`javacv-11` 与 `javacv-21` 镜像的 `FROM` 指令引用了基础镜像 `jre:11` 与 `jre:21`，因此**必须先构建并推送基础镜像，再构建对应的 cv 镜像**。

```
java11 (基础镜像) ──► javacv-11
java21 (基础镜像) ──► javacv-21
```

构建顺序建议：

1. 构建 `java11` 的 arm64 与 amd64 镜像 → 推送到镜像仓
2. 构建 `java21` 的 arm64 与 amd64 镜像 → 推送到镜像仓
3. 构建 `javacv-11` 的 arm64 与 amd64 镜像 → 推送到镜像仓
4. 构建 `javacv-21` 的 arm64 与 amd64 镜像 → 推送到镜像仓

---

## 三、镜像仓地址

- 镜像仓地址：`repo.rd.td-tech.com/docker-virtual/`
- 镜像命名空间：`linkx/msip/`

| 镜像分类 | 本地镜像名 | 推送目标镜像名 |
| --- | --- | --- |
| java11 | `linkx/msip/jre:11.openeuler.openjre.{arm64\|amd64}` | `repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:11.openeuler.openjre.{arm64\|amd64}` |
| java21 | `linkx/msip/jre:21.openeuler.openjre.{arm64\|amd64}` | `repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:21.openeuler.openjre.{arm64\|amd64}` |
| javacv-11 | `linkx/msip/jre-cv:11.openeuler.openjre.{arm64\|amd64}` | `repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:11.openeuler.openjre.{arm64\|amd64}` |
| javacv-21 | `linkx/msip/jre-cv:21.openeuler.openjre.{arm64\|amd64}` | `repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:21.openeuler.openjre.{arm64\|amd64}` |

---

## 四、通用构建流程

每个镜像的构建流程一致，主要包含以下 3 步：

1. **拷贝 Dockerfile**：将对应目录下的 `Dockerfile-arm64` 或 `Dockerfile-amd64` 拷贝到构建服务器的任意文件夹下。
2. **执行构建命令**：使用 `docker build --no-cache` 构建镜像。
3. **查看并推送镜像**：使用 `docker images` 确认镜像存在，再使用 `docker tag` + `docker push` 推送到镜像仓。

> 注意：所有构建命令均使用 `--no-cache` 参数，确保镜像不使用缓存层。

---

## 五、java11 基础镜像构建

### 5.1 Dockerfile 说明

- 基础镜像：`openeuler-openjre11:11.0.24.3`
- 主要内容：安装 `fontconfig`、`glibc-all-langpacks`、`procps-ng`、`zlib`、`libpwquality`；配置 locale（`en_US.UTF-8`）、时区（`Asia/Shanghai`）以及密码合规策略。
- arm64 与 amd64 的 Dockerfile 内容一致，仅文件名不同。

### 5.2 构建命令

**arm64（在 192.168.0.130 服务器执行）：**

```bash
docker build --no-cache -f Dockerfile-arm64 -t linkx/msip/jre:11.openeuler.openjre.arm64 .
```

**amd64（在 172.26.130.24 服务器执行）：**

```bash
docker build --no-cache -f Dockerfile-amd64 -t linkx/msip/jre:11.openeuler.openjre.amd64 .
```

### 5.3 查看镜像

```bash
docker images | grep jre:11.openeuler.openjre
```

### 5.4 推送镜像到镜像仓

**arm64：**

```bash
# 将 <IMAGE_ID> 替换为 docker images 查询到的镜像 ID
docker tag <IMAGE_ID> repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:11.openeuler.openjre.arm64
docker push repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:11.openeuler.openjre.arm64
```

**amd64：**

```bash
# 将 <IMAGE_ID> 替换为 docker images 查询到的镜像 ID
docker tag <IMAGE_ID> repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:11.openeuler.openjre.amd64
docker push repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:11.openeuler.openjre.amd64
```

---

## 六、java21 基础镜像构建

### 6.1 Dockerfile 说明

- 基础镜像：`openeuler-openjre:21.0.2.4`
- 主要内容：与 java11 一致（安装基础包、配置 locale、时区、密码合规）。
- arm64 与 amd64 的 Dockerfile 内容一致，仅文件名不同。

### 6.2 构建命令

**arm64（在 192.168.0.130 服务器执行）：**

```bash
docker build --no-cache -f Dockerfile-arm64 -t linkx/msip/jre:21.openeuler.openjre.arm64 .
```

**amd64（在 172.26.130.24 服务器执行）：**

```bash
docker build --no-cache -f Dockerfile-amd64 -t linkx/msip/jre:21.openeuler.openjre.amd64 .
```

### 6.3 查看镜像

```bash
docker images | grep jre:21.openeuler.openjre
```

### 6.4 推送镜像到镜像仓

**arm64：**

```bash
docker tag <IMAGE_ID> repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:21.openeuler.openjre.arm64
docker push repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:21.openeuler.openjre.arm64
```

**amd64：**

```bash
docker tag <IMAGE_ID> repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:21.openeuler.openjre.amd64
docker push repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:21.openeuler.openjre.amd64
```

---

## 七、javacv-11 镜像构建（依赖 java11）

> ⚠️ 前置条件：必须先完成 [第五章 java11 基础镜像构建](#五java11-基础镜像构建) 并推送到镜像仓。
>
> 适用范围：仅用于使用 OpenCV 的微服务，目前只有 `cloudcmd-im-jingxin` 服务使用。

### 7.1 Dockerfile 说明

- 基础镜像：
  - arm64：`linkx/msip/jre:11.openeuler.openjre.arm64`
  - amd64：`linkx/msip/jre:11.openeuler.openjre.amd64`
- 主要内容：在基础镜像之上安装 `gtk2`。

### 7.2 构建命令

**arm64（在 192.168.0.130 服务器执行）：**

```bash
docker build --no-cache -f Dockerfile-arm64 -t linkx/msip/jre-cv:11.openeuler.openjre.arm64 .
```

**amd64（在 172.26.130.24 服务器执行）：**

```bash
docker build --no-cache -f Dockerfile-amd64 -t linkx/msip/jre-cv:11.openeuler.openjre.amd64 .
```

### 7.3 查看镜像

```bash
docker images | grep jre-cv:11.openeuler.openjre
```

### 7.4 推送镜像到镜像仓

**arm64：**

```bash
docker tag <IMAGE_ID> repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:11.openeuler.openjre.arm64
docker push repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:11.openeuler.openjre.arm64
```

**amd64：**

```bash
docker tag <IMAGE_ID> repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:11.openeuler.openjre.amd64
docker push repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:11.openeuler.openjre.amd64
```

---

## 八、javacv-21 镜像构建（依赖 java21）

> ⚠️ 前置条件：必须先完成 [第六章 java21 基础镜像构建](#六java21-基础镜像构建) 并推送到镜像仓。
>
> 适用范围：仅用于使用 OpenCV 的微服务，目前只有 `cloudcmd-im-jingxin` 服务使用。

### 8.1 Dockerfile 说明

- 基础镜像：
  - arm64：`linkx/msip/jre:21.openeuler.openjre.arm64`
  - amd64：`repo.rd.td-tech.com/docker-virtual/linkx/msip/jre:21.openeuler.openjre.amd64`
    > 注意：javacv-21 的 amd64 Dockerfile 直接引用了镜像仓地址，构建前请确保构建服务器已登录镜像仓并拉取过该基础镜像。
- 主要内容：在基础镜像之上安装 `gtk2`。

### 8.2 构建命令

**arm64（在 192.168.0.130 服务器执行）：**

```bash
docker build --no-cache -f Dockerfile-arm64 -t linkx/msip/jre-cv:21.openeuler.openjre.arm64 .
```

**amd64（在 172.26.130.24 服务器执行）：**

```bash
docker build --no-cache -f Dockerfile-amd64 -t linkx/msip/jre-cv:21.openeuler.openjre.amd64 .
```

### 8.3 查看镜像

```bash
docker images | grep jre-cv:21.openeuler.openjre
```

### 8.4 推送镜像到镜像仓

**arm64：**

```bash
docker tag <IMAGE_ID> repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:21.openeuler.openjre.arm64
docker push repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:21.openeuler.openjre.arm64
```

**amd64：**

```bash
docker tag <IMAGE_ID> repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:21.openeuler.openjre.amd64
docker push repo.rd.td-tech.com/docker-virtual/linkx/msip/jre-cv:21.openeuler.openjre.amd64
```

---

## 九、完整构建顺序速查表

| 步骤 | 镜像 | 架构 | 构建服务器 | Dockerfile |
| --- | --- | --- | --- | --- |
| 1 | java11 | arm64 | 192.168.0.130 | java11/Dockerfile-arm64 |
| 2 | java11 | amd64 | 172.26.130.24 | java11/Dockerfile-amd64 |
| 3 | java21 | arm64 | 192.168.0.130 | java21/Dockerfile-arm64 |
| 4 | java21 | amd64 | 172.26.130.24 | java21/Dockerfile-amd64 |
| 5 | javacv-11 | arm64 | 192.168.0.130 | javacv-11/Dockerfile-arm64 |
| 6 | javacv-11 | amd64 | 172.26.130.24 | javacv-11/Dockerfile-amd64 |
| 7 | javacv-21 | arm64 | 192.168.0.130 | javacv-21/Dockerfile-arm64 |
| 8 | javacv-21 | amd64 | 172.26.130.24 | javacv-21/Dockerfile-amd64 |

> 步骤 1-4 必须先于 5-8 完成；步骤 1-2 与 3-4 之间无依赖，可并行；步骤 5-6 依赖 1-2；步骤 7-8 依赖 3-4。

---

## 十、注意事项

1. **构建前准备**：构建前请确保服务器已安装 docker，并能访问镜像仓 `repo.rd.td-tech.com`（如需登录请先执行 `docker login`）。
2. **代理配置**：Dockerfile 中默认使用代理 `http://192.168.195.201:8080`，构建完成后会自动清空 `http_proxy` 与 `https_proxy` 环境变量。如服务器网络环境不同，请按需修改 Dockerfile 中的代理地址。
3. **依赖关系**：cv 镜像依赖基础镜像，基础镜像未推送则 cv 镜像构建会失败。
4. **IMAGE_ID**：本文档中推送命令的 `<IMAGE_ID>` 需替换为 `docker images` 实际查询到的镜像 ID 或镜像名:tag。
5. **缓存**：所有构建命令均使用 `--no-cache`，避免历史缓存导致镜像不一致。
6. **适用范围**：`java-cv` 镜像仅适用于使用 OpenCV 的微服务（目前只有 `cloudcmd-im-jingxin`），普通微服务请使用 `jre` 基础镜像，避免镜像体积冗余。
7. **IP 地址时效性**：本文档中出现的所有 IP 地址（包括构建服务器 IP 与代理服务器 IP `192.168.195.201`）均为当时构建时的环境地址。后续如需重新构建镜像，请先确认服务器是否可用；如不可用，请联系 CI 团队提供新的构建服务器和代理服务器地址，并同步更新本文档。
8. **预留镜像说明**：`java21-cv` 为预留镜像，当前 Linkx 服务仍基于 JDK 11；待 Linkx 整体服务升级到 JDK 21 后，`cloudcmd-im-jingxin` 等使用 OpenCV 的微服务需切换使用 `jre-cv:21` 镜像。
