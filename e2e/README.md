# Linkx Playwright E2E

`e2e` 是 Linkx 前端自动化测试的 canonical 工程，覆盖 `h5portal`、`web-bspc`、`web-cspc` 三个宿主目标，并提供 `offline` 和 `live` 两种模式。宿主 SDK 文件仍由 H5Portal/web 保持原样；offline 只模拟 SDK 下层传输，live 通过 `dev-bridge` 连接内网真实宿主。

完整的新电脑安装、Vite 启动、内网 Provider、WSS、测试数据和人工原生验收步骤见 [前端自动化测试使用指南](../docs/前端自动化测试使用指南.md)。实施阶段和边界见 [前端自动化实施计划](../docs/前端自动化实施计划.md)，当前覆盖范围见 `coverage-matrix.md`。

## 常用命令

```powershell
pnpm install --frozen-lockfile
pnpm run typecheck
pnpm run test:contracts
$env:E2E_TARGET = 'h5portal' # 或 web-bspc / web-cspc
pnpm run test:offline
pnpm run test:live
pnpm run report
```

本机有 Chrome/Edge 而没有 Playwright Chromium 时设置 `$env:E2E_BROWSER_CHANNEL='chrome'` 或 `msedge`。live 环境缺少 Provider 时会失败 readiness，不会自动 skip。测试结果包含 HTML/JUnit 报告、失败 trace/截图/视频，以及每个用例的 host/scenario JSON evidence。

`linkx-e2e` 是早期试验目录。新用例统一放入本工程；迁移旧用例时重新核对 SDK 事件编码、多参数和真实路由。
