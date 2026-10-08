# LinkX 前端自动化测试项目

本目录是与 `H5Portal`、`web` 同级的独立 Playwright 项目。

它加载现有前端和原始 `WeSpaceSDK`，只模拟 SDK 下方的宿主通信协议与业务接口数据，不修改宿主 SDK 文件。

## 运行

```bash
pnpm install
pnpm exec playwright install chromium
pnpm typecheck
pnpm test --project=h5
pnpm test --project=pc-web
pnpm test --project=pc-app
```

默认地址：

- H5：`http://127.0.0.1:8001`
- Web：`http://127.0.0.1:5173`

如果服务已经由开发者手动启动，可通过环境变量替换：

```powershell
$env:LINKX_H5_URL = 'http://127.0.0.1:8001'
$env:LINKX_WEB_URL = 'http://127.0.0.1:5173'
pnpm test --project=h5
```

## 目录职责

- `src/hosts`：宿主通信适配器。H5、PC Web、PC-APP 共享测试侧 API，底层协议分别实现。
- `src/mocks`：场景数据、HTTP 和 WebSocket Mock 的扩展位置。
- `src/fixtures`：Playwright fixture，把页面、宿主控制器和场景组合起来。
- `tests/contracts`：验证 SDK 方法、参数和回调契约。
- `tests/h5`、`tests/pc-web`、`tests/pc-app`：按宿主运行模式组织业务流程。

当前骨架只建立通信边界，不假定具体业务页面和接口响应。新增业务时，先在场景中定义数据，再在测试中通过 `host.waitForCall()` 验证 SDK 调用，最后通过 `host.respond()` 或 `host.emit()` 驱动页面完成闭环。
