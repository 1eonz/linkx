---
name: "package-deploy"
description: "打包并部署指定服务。识别“打包部署 + 环境/服务名/ip”等自然语言指令，按配置文件将环境关键词映射为 ip，使用 mvn 打包（跳过测试）生成 jar，再通过表单上传接口部署。当用户说打包部署/部署某个服务、要求上传 jar 时触发。"
---

# Package Deploy（打包部署）

将自然语言指令解析为“mvn 打包 -> 上传部署”两步操作，将指定服务打成 jar 并上传到 `http://<ip>:8099/service/upload` 完成发布。目标 ip 可通过**环境关键词**从配置文件解析，也可由指令显式给出。

## 环境与 ip 配置

所有 `环境关键词 -> ip` 的映射维护在配置文件:

> 配置文件: `.trae/skills/package-deploy/deploy-regions.properties`

格式为 `环境关键词=ip`，例如:

```
主线=172.16.23.8
冀中=172.16.23.30
雄安=10.28.70.21
```

解析 ip 的优先级:
1. 指令中**显式给出的 ip** 优先级最高，直接使用。
2. 否则识别指令中的**环境关键词**，在配置文件 `deploy-regions.properties` 中查表得到 ip。
3. 两者都缺省时，向用户确认，不猜测。

需要新增/修改环境的 ip 时，直接编辑该配置文件即可，无需改动本 skill 逻辑。

## 触发场景

用户输入包含“打包部署 / 部署 / 上传”等语义，且带有**服务名**，通过**环境关键词**或**目标 ip** 定位部署目标。典型输入如下:

- `打包部署主线 cloudcmd-im`（关键词“主线” -> 查配置得 ip）
- `部署冀中 linkx-third`（关键词“冀中” -> 查配置得 ip）
- `打包部署主线 cloudcmd-im，arm 平台`（含“arm” -> `-P arm`；未提则默认 `-P x86`）
- `打包部署 cloudcmd-admin 172.16.23.8`（显式 ip 优先）
- `打包部署主线 cloudcmd-auth 172.16.23.8`（显式 ip 覆盖关键词）
- `帮我打包并部署 cloudcmd-admin，ip 是 10.0.0.5`
- `重新部署主线 cloudcmd-im`（含“重新部署” -> `reuse`：复用已有 jar，不重新打包）
- `打包部署主线 cloudcmd-im-jingxin、cloudcmd-im-openapi`（多个服务 -> 依次 clean 打包并逐一到集群部署）
- `打包部署主线 agent-gateway`（agent 服务 -> `namespace=agent`，构建 `crs-gateway` 模块；接口 service 传 `agent-gateway`，JDK17）
- `部署冀中 agent-ai-agent、agent-system`（agent 多服务 -> 各自映射为 `crs-module-ai-agent-server`/`crs-module-system-server`，namespace=agent）

## 执行流程

> 若步骤 1 解析出**多个服务**，则步骤 2~5（定位模块 -> 打包/复用 -> 选 jar -> 上传）对每个服务**逐个完整执行**，全部完成后统一反馈结果。

### 1. 解析参数
从自然语言中提取以下信息:

| 参数 | 来源 | 说明 |
|------|------|------|
| `service` | 服务名（支持多个） | 可含多个服务，按分隔符拆分为**服务列表**，逐个执行打包+上传 |
| `ip` | 显式 ip 或环境关键词查配置 | 优先级: 显式 ip > 环境关键词查表；拼进 `http://<ip>:8099/service/upload` |
| `type` | 固定值 | 恒为 `Deployment` |
| `namespace` | 由项目决定 | ccmd（cloudcmd-*）服务为 `linkx`；agent（agent-*）四服务为 `agent` |
| `platform` | 自然语言关键词 | **默认 `x86`**；指令含“arm”则用 `arm`，决定 mvn 的 `-P` profile |
| `strategy` | 自然语言关键词 | **默认 `rebuild`**；仅当指令含“重新部署 / 复用 / 重发 / 重新上传”等语义时为 `reuse`，决定是否重新 clean 打包 |

**service 拆分步骤（支持多服务）:**
- 用分隔符（`、` `,` `，` 空白等）把服务名拆成**列表**，例如 `打包部署主线cloudcmd-im-jingxin、cloudcmd-im-openapi` 得到 `[cloudcmd-im-jingxin, cloudcmd-im-openapi]`。
- `ip`、`platform`、`strategy`、`type`、`namespace` 对该列表内所有服务**共用同一套取值**。
- 后续**打包 + 上传两步骤对列表内每个服务依次执行一遍**（逐个 clean 打包、逐个上传），直到全部完成；单个失败则记录并继续其余服务，最后汇总结果，不中途整体放弃。

**strategy 解析步骤:**
- 指令含“重新部署 / 复用已有 / 重发 / 重新上传”等 → `strategy=reuse`：**不重新打包**，直接复用 target 下已有 jar 上传。
- 否则（含“打包部署 / 部署/ 打包”等）→ `strategy=rebuild`：**每次都 `mvn clean package` 重新打包**，即使 target 已有 jar 也**不复用**。

**ip 解析步骤:**
1. 读 `.trae/skills/package-deploy/deploy-regions.properties`，按 `=` 拆分为 `环境关键词->ip` 映射。
2. 若指令含显式 ip（IPv4 形式），优先用它。
3. 否则在指令中查找是否命中配置文件中的某个环境关键词（如 主线/冀中），命中则取其 ip。
4. 都匹配不到则向用户确认。

**platform 解析步骤:**
- 指令中出现“arm / 鲲鹏 / 麒麟 / 龙芯”等 arm 语义 → `platform=arm`
- 否则默认 `platform=x86`（linux-x86_64 目标）
- 需要 Windows 本地运行时才考虑 `win`，默认不用。

**项目归属 + namespace + 模块映射（重要）:**

部署时接口的 `service` 与 `namespace` 参数由项目归属决定，且**服务名与要构建的 maven 模块可能不同**。按服务名前缀区分项目:

| 项目 | 服务名前缀 | namespace | 编译 JDK |
|------|-----------|-----------|---------|
| ccmd（cloudcmd-*） | `cloudcmd-*` / `linkx-*` | `linkx` | JDK11 |
| agent（agent-*） | `agent-*` | `agent` | **JDK17** |

**agent 服务名 -> maven 模块映射表**: agent 项目有 4 个服务，**自然语言里的服务名**（如 `agent-ai-agent`）与**要构建的模块**（如 `crs-module-ai-agent-server`）不一致，构建时需映射:

| 服务名（接口 service 参数） | 要构建的 maven 模块 | 模块目录 |
|------|------|------|
| `agent-ai-agent` | `crs-module-ai-agent-server` | `agent/server/crs-module-ai-agent/crs-module-ai-agent-server` |
| `agent-gateway` | `crs-gateway` | `agent/server/crs-gateway` |
| `agent-infra` | `crs-module-infra-server` | `agent/server/crs-module-infra/crs-module-infra-server` |
| `agent-system` | `crs-module-system-server` | `agent/server/crs-module-system/crs-module-system-server` |

解析时: 若服务名以 `agent-` 开头，视为 agent 项目服务，`namespace=agent`，并查上表把**要构建的模块**定位到 **`agent` 聚合根**下的对应模块；否则视为 ccmd 服务，`namespace=linkx`，模块路径在 **`ccmd`** 聚合根下。上传接口的 `service` 参数填**自然语言里的服务名**（如 `agent-ai-agent`），**不是** maven 模块名。

> 若解析不到服务名，先向用户确认，不要凭空假设。

### 2. 定位服务模块
服务名可能是 maven 模块/目录名，也可能是 agent 项目的别名服务名。按如下规则定位构建目录:

- **agent 服务**（服务名以 `agent-` 开头）: 用上表「要构建的 maven 模块」定位，即 Glob 搜索 `agent/server/**/<映射模块>/pom.xml`（如 `agent/server/crs-module-ai-agent/crs-module-ai-agent-server/pom.xml`、`agent/server/crs-gateway/pom.xml`）。
- **ccmd 服务**（`cloudcmd-*` / `linkx-*`）: 用 Glob 搜索 `**/<service>/pom.xml` 定位模块的 `pom.xml`。
- 找到该模块所属的**聚合根 pom**（agent 项目聚合根是 `agent/server/pom.xml`；ccmd 项目聚合根是 `ccmd/pom.xml`）。
- 确认 `packaging` 为非 `pom` 且能产出可执行 jar 的模块——通常是 `${service}-service` 子模块（如 `cloudcmd-admin-service`）。若服务名就是聚合模块名（如 `cloudcmd-im`、`cloudcmd-admin`），默认构建其 `*-service` 子模块。

### 3. 执行 mvn 打包（rebuild 策略，跳过测试，用本地依赖）

- **`strategy=rebuild`（默认）**: **每次都必须 `mvn clean package` 重新打包**，即使 target 下已有 jar 也**不复用**，保证产物为最新源码构建。
- **`strategy=reuse`（指令含“重新部署”等）**: **跳过本步骤**，直接进入步骤 4 复用 target 下已有 jar；若 jar 不存在则降级为 rebuild 重新打包。

**必须从对应项目的聚合根构建**，原因: 若只从某一级目录（如 `cloudcmd-service`、`crs-module-system`）构建，reactor 外的依赖模块会到远程 snapshot 仓库下载**旧版 jar**，导致编译报“找不到符号”。从聚合根用 `-am` 构建会让所有依赖都编译**本地源码**，避免旧包。

**两个项目构建差异:**

| 项目 | 聚合根（cwd） | 编译 JDK | `-s` settings |
|------|--------------|---------|--------------|
| ccmd | 当前用户机器上的 `ccmd` 目录 | JDK11 | `ccmd/settings.xml`（华为云镜像，必须） |
| agent | 当前用户机器上的 `agent/server` 目录 | **JDK17**（agent pom 已设 `java.version=17`） | 复用 `ccmd/settings.xml`（华为云镜像，必须，否则中央仓超时） |

**JDK 与 settings 路径必须动态探测，不能写死某台机器的绝对路径**（不同开发者安装路径不同）。构建前按以下优先级探测与校验，**找不到就给出明确提示，让用户提供路径，不盲目猜测**:

**settings.xml:**
1. 优先用 `ccmd/settings.xml`（ccmd 与 agent 两个项目都可复用，指向华为云镜像）。
2. 用 Glob/precedence 在该仓库根 `**/settings.xml` 下查找；找不到则提示“未找到 settings.xml，请提供华为云镜像 settings 路径，否则可能中央仓连接超时”。

**JDK（确认 `JAVA_HOME` 或 `mvn -version` 里的 Java 版本）：**
1. ccmd 需要 JDK11、agent 需要 JDK17。
2. 按机器常见位置探测（如 `D:\TD\javatool\jdk-17.*`、`D:\TD\javatool\jdk-11*`、`C:\Program Files\Java\jdk-17*` 等），得到具体 JDK 绝对路径。
3. 探测到 JDK 目录后，确认版本（读取其 `bin\java.exe` 或目录名含 `17`/`11`）。
4. **若所需 JDK 未找到**，提示：“未找到该服务需要的 JDK（ccmd=11 / agent=17），请在终端设置对应 JAVA_HOME 后重试”，并让用户提供 JDK 路径，不强行用错误版本构建。

构建前先确认 `JAVA_HOME`/`maven.compiler` 与项目所需 JDK 一致（ccmd 用 JDK11、agent 用 JDK17），避免因 JDK 版本不匹配编译报错。

**ccmd 项目构建命令**（在聚合根 `ccmd` 执行，`platform` 来自步骤 1，默认 `x86`）:

```
mvn -s <ccmd/settings.xml 绝对路径> \
    -P <platform> \
    -pl <reactor相对路径>/<module> \
    -am clean package -DskipTests
```

**agent 项目构建命令**（在聚合根 `agent\server` 执行; agent 为 JDK17 工程，且**必须先设置 JDK17 的 JAVA_HOME**; **必须复用 ccmd 的 settings.xml**（华为云镜像），否则中央仓超时; 通常无需 `-P x86/arm`，如本地确有 platform/opencv 相关 profile 再由步骤 1 的 platform 决定）:

```
# 设置 JDK17（探测到的具体路径，示例仅示意，勿写死）
$env:JAVA_HOME="<探测到的 jdk-17.x 目录>"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn -s "<ccmd/settings.xml 绝对路径>" -pl <模块路径> -am clean package -DskipTests
```

- `-P <platform>`（仅 ccmd）: 取 `x86` 或 `arm`。该 profile 提供 `${javacv.version}` 与 `${platform}` 属性（对应 opencv 等原生库平台），**不加会因属性未定义直接报错**。
- `<reactor相对路径>/<module>`: 相对的聚合根模块路径，例如 ccmd 下 `cloudcmd-service/cloudcmd-im/cloudcmd-im-jingxin`；agent 下 `crs-module-ai-agent/crs-module-ai-agent-server`、`crs-gateway`。
- `-am`: 一并构建依赖且同 reactor 的模块；reactor 内依赖走本地源码，reactor 外（若确实不在 reactor）用对应 settings 拉取本地/华为云仓库。
- `clean`: 清理旧 target 后重新打包，确保不使用历史产物。
- 常见报错排查:
  - “Plugin ... could not be resolved ... transfer failed / Connection timed out”: 镜像 settings 未生效，检查是否加了 `-s ccmd/settings.xml`。
  - “无效的目标发行版：17 / 无效的目标发行版：11”: `JAVA_HOME` 版本不对（agent 需 17、ccmd 需 11），切到对应 JDK 后再构建。

等待构建成功（`BUILD SUCCESS`），失败则停止并反馈错误日志。

### 4. 确定 jar 产物
在模块 `target/` 下选择**可执行主 jar**（`strategy=reuse` 时即复用该已有 jar；`rebuild` 后为最新产物），规则:

- 文件名形如 `<artifactId>-<version>.jar`（如 `cloudcmd-admin-service-1.0.0-SNAPSHOT.jar`）。agent 模块产物名以**模块 artifactId**命名，且**可能无版本号**（如 `crs-module-ai-agent-server.jar`、`crs-gateway.jar`），需用 Glob 按 artifactId 前缀查找实际产物名。
- **排除** `-sources.jar`、`-javadoc.jar`、`-plain.jar`、`-original.jar`、以及 `*-api.jar`、`*-client.jar`、`*-openapi.jar` 等非启动 jar。
- 用 Glob 找 `target/*.jar` 后结合以上规则挑选；拿不准时确认启动类/`target` 下唯一的非依赖 jar。

### 5. 上传部署（PowerShell HttpClient multipart）
使用 POST + `multipart/form-data` 上传，无需鉴权。**用 PowerShell `HttpClient` 实现**（`curl.exe`/`curl` 常被管理员 AppLocker 策略拦截，授权 `Cannot run program ... error=786`）。在 PowerShell 中执行:

```powershell
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromMinutes(10)
$form = New-Object System.Net.Http.MultipartFormDataContent
$form.Add((New-Object System.Net.Http.StringContent('<service>')), 'service')
$form.Add((New-Object System.Net.Http.StringContent('Deployment')), 'type')
$form.Add((New-Object System.Net.Http.StringContent('<namespace>')), 'namespace')
[byte[]]$bytes = [System.IO.File]::ReadAllBytes('<jar绝对路径>')
$fc = New-Object System.Net.Http.ByteArrayContent -ArgumentList (,$bytes)
$fc.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue('application/octet-stream')
$form.Add($fc, 'file', '<jar文件名>')
$resp = $client.PostAsync('http://<ip>:8099/service/upload', $form).Result
$body = $resp.Content.ReadAsStringAsync().Result
Write-Output ("HTTP_" + [int]$resp.StatusCode)
Write-Output $body
$client.Dispose()
```

- `<service>` / `<namespace>` / `<jar绝对路径>` / `<jar文件名>` 分别填步骤 1 的服务名、项目对应 namespace（ccmd=`linkx`、agent=`agent`）、步骤 4 的 jar 绝对路径与 jar 文件名；`<ip>` 为步骤 1 解析的 ip。
- **`<jar绝对路径>` 不写死具体机器的绝对路径**: 各开发者仓库根目录不同（仓库根可能不是 `D:\TD\linkx\Linkx`），jar 绝对路径 = `<当前仓库根>` + `<模块相对路径>` + `\target\<jar文件名>`。用 Glob 基于 **当前仓库根** 定位：如 `**/<模块目录>/target/<artifactPrefix>*.jar`（如 `agent/server/crs-module-ai-agent/crs-module-ai-agent-server/target/crs-module-ai-agent-server.jar`）。jar 始终在所选模块的 **`target/`** 下。
- **`<service>` 填自然语言里的服务名**（agent 服务如 `agent-ai-agent`），不是构建的 maven 模块名。
- 路径中的正斜杠（`/`）可用，注意保持引号。
- 大 jar（如几百 MB）上传较慢，设置 `Timeout` 并等待，勿重复发起。

> 不要用 `curl`/`curl.exe`，若确实无法用 HttpClient（个别环境）再回退，但优先 HttpClient。

### 6. 更新服务映射（上传返回 200 后，GET 触发映射，确保最新 jar 部署生效）
**前置条件: 只有上传接口返回 HTTP 200（且响应无 error）才允许执行本步骤**。若上传非 200，则不调用 GET，直接按失败处理——反馈上传响应原文与排查建议（ip 可达性、路径、jar 是否正确）。

上传 200 时，这不是「复核」，而是**必须执行的映射步骤**: 上传接口只是把 jar 传上去，还需调用 GET 触发服务映射，平台才能把最新的 jar 部署/更新为真正运行的服务，从而**确保最新 jar 部署成功生效**。跳过此步则服务不会切换到最新 jar。

```
GET http://<ip>:8099/service/map?service=<service>&type=Deployment&namespace=<namespace>
```

用 PowerShell `HttpClient` 执行（无需鉴权）:

```powershell
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(60)
$resp = $client.GetAsync("http://<ip>:8099/service/map?service=<service>&type=Deployment&namespace=<namespace>").Result
$body = $resp.Content.ReadAsStringAsync().Result
Write-Output ("GET_HTTP_" + [int]$resp.StatusCode)
Write-Output $body
$client.Dispose()
```

- `<service>`、`<namespace>`、`<ip>` 分别填步骤 1 的服务名（agent 服务用别名如 `agent-ai-agent`，非模块名）、项目对应 namespace（ccmd=`linkx`、agent=`agent`）、ip。
- **结果判定**: 2xx 且返回的服务映射/列表里**存在该服务且状态为已部署/健康**，视为最新 jar 映射部署成功；否则按失败处理——给出 GET 响应原文与排查建议（服务是否注册成功、是否有启动报错），并停止（不盲目重传）。
- GET 响应原文（非 `success` 结构时）按实际返回内容判断，把关键字段（服务名、状态）反馈给用户。

## 执行原则（重要）

- **命令可稳定执行时，直接执行，无需人工确认，也无需人工点“运行/允许”**。所有终端命令（mvn 打包、PowerShell 上传等）一律设置为**自动运行**（`requires_approval=false`），直接下发执行，不弹人工授权窗。
- 构建/上传类长命令用**非阻塞异步**方式发起，再轮询结果，不要把控制权交回等待人工确认。
- 仅在下述情况暂停并向用户确认:
  - **解析失败**（服务名、ip 都解析不到且无法通过环境关键词确定目标）；
  - **参数存在歧义**（同类服务名匹配到多个模块）；
  - **执行出错**（mvn 构建失败、上传非 2xx 等），此时反馈错误并停止，不盲目重试。
- 不因流程惯例询问"是否开始/是否上传"。除非出错，完整走完 打包 -> 上传 -> 反馈结果。

## 注意事项

- **路径用绝对路径**，jar 路径与 mvn 构建目录必须准确，避免上传错文件。
- **不要改动任何源代码**，本 skill 只负责构建与上传。
- 构建慢时使用工具异步等待并轮询结果，不要重复触发打包。
- 绝不使用破坏性 git 命令，不额外提交代码。