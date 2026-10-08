# Linkx E2E — Playwright 自动化测试工程

> SDK 代理桥（[dev-bridge/](../dev-bridge/)）的 **Playwright 消费者工程**。
> 桥是工具无关的本地联调基础设施（本地联调手册见 [dev-bridge/README.md](../dev-bridge/README.md)）；本工程仅负责以自动化方式驱动同一套 stub 模式（`?bridge=stub`）跑测试用例。
>
> 当前已接入端：**H5Portal**。

## 一、目录结构

```
e2e/
├── shared/                          # Playwright 消费侧共享
│   ├── fixtures/host-page.js        #   createHostFixture(config) —— 以 ?bridge=stub 驱动页面
│   └── helpers/proxy-health.js      #   hasProvider() 宿主在线检测（用例降级）
├── h5portal/                        # H5Portal 端
│   ├── fixtures/  tests/  README.md
│   └── playwright.config.js         # dev 8001 + 代理桥 8787/8788
└── package.json                     # @playwright/test

# 桥核心（页面脚本/适配层/代理服务）不在本工程 —— 见 dev-bridge/
```

## 二、快速开始

```powershell
# 安装（仅首次；本地联调无需本工程，只要 dev-bridge）
cd e2e; pnpm install

# 跑测试（终端 CI 环境变量需清除: $env:CI=''）
pnpm test:h5portal      # H5Portal 端全部用例
pnpm test:h5portal:ui   # UI 调试模式（推荐）
pnpm report:h5portal    # HTML 报告
```

## 三、工作方式

1. **webServer 自动拉起**：playwright.config 会自动启动 H5Portal dev server + dev-bridge 代理服务（亦可先手动起好，自动复用）
2. **同一 stub 入口**：用例通过 fixture 以 `?bridge=stub` 打开页面，与开发人员本地联调打开的页面完全等价
3. **宿主离线自动降级**：SDK 调用类用例先经 `hasProvider()` 健康检查，宿主未连接时自动 skip，页面结构类用例照常执行
4. **串行执行**：`workers: 1`，避免多用例争抢同一宿主

## 四、调试

- 桥侧调试（连接状态、桩验证、健康检查）：见 [dev-bridge/README.md](../dev-bridge/README.md) 第四节
- Playwright 侧：`pnpm test:h5portal:ui`（逐条运行、watch 模式、失败截图/快照）

## 五、常见问题

| 现象 | 解决 |
|------|------|
| `port already used` 但本地已手动起服务 | 终端带 `CI=1`，`reuseExistingServer` 失效；先 `$env:CI=''` |
| SDK 用例全部 skip | 宿主离线（`hasProvider` 降级）；按 dev-bridge/README 配置宿主加载本机 dev server |
| Chromium 下载失败 | 使用系统 Chrome（`channel: 'chrome'` 已默认）；或设 `PLAYWRIGHT_DOWNLOAD_HOST=https://cdn.npmmirror.com/binaries/playwright` |

## 六、端口速查

| 用途 | 端口 |
|------|------|
| H5Portal dev server | 8001（http） |
| 代理桥 WS | 8787 |
| 代理桥健康检查 | 8788 |

> 新端接入（含 e2e 消费侧）指南见 [dev-bridge/README.md](../dev-bridge/README.md) 第八节。
