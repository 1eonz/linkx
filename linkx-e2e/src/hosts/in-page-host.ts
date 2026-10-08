import type { Page } from '@playwright/test';
import type { HostCall, HostController, HostMode, HostResponse } from '../types/host.js';

/**
 * 通用宿主控制器。
 * 它只负责把请求从浏览器页面传到 Node 测试进程，具体的 H5、PC Web、PC-APP
 * 通道注入由 mode 分支提供。原始 WeSpaceSDK 仍然由业务页面加载。
 */
export class InPageHostController implements HostController {
  private readonly received: HostCall[] = [];
  private readonly waiters = new Map<string, Array<(call: HostCall) => void>>();

  constructor(
    private readonly page: Page,
    private readonly mode: HostMode,
  ) {}

  async install() {
    await this.page.exposeBinding('__linkxHostRequest', async (_source, request: HostCall) => {
      const call = { ...request, receivedAt: Date.now() };
      this.received.push(call);
      this.waiters.get(call.method)?.splice(0).forEach(resolve => resolve(call));
    });

    await this.page.addInitScript(({ mode }) => {
      const send = (message: unknown) => {
        void (window as any).__linkxHostRequest(message);
      };

      (window as any).__linkxTestHost = {
        mode,
        sendResponse(response: unknown) {
          window.dispatchEvent(new CustomEvent('__linkx_host_response__', { detail: response }));
        },
        sendEvent(event: unknown) {
          window.dispatchEvent(new CustomEvent('__linkx_host_event__', { detail: event }));
        },
      };

      if (mode === 'h5') {
        (window as any).flutterNativeBridge = {
          postMessage(raw: string) {
            send(JSON.parse(raw));
          },
        };
      }

      if (mode === 'pc-web') {
        // PC Web 的 SETUP_CHANNEL / MessageChannel 适配器在此注册。
        // 具体响应协议由对应 SDK 版本的宿主契约测试补齐。
        (window as any).__linkxPcWebHostReady = true;
      }

      if (mode === 'pc-app') {
        const listeners: Array<(event: MessageEvent) => void> = [];
        (window as any).chrome = (window as any).chrome || {};
        (window as any).chrome.webview = {
          postMessage(raw: string) {
            send(JSON.parse(raw));
          },
          addEventListener(type: string, handler: (event: MessageEvent) => void) {
            if (type === 'message') listeners.push(handler);
          },
          __emit(raw: unknown) {
            const data = typeof raw === 'string' ? raw : JSON.stringify(raw);
            listeners.forEach(handler => handler(new MessageEvent('message', { data })));
          },
        };
      }
    }, { mode: this.mode });
  }

  calls() {
    return [...this.received];
  }

  waitForCall(method: string, timeoutMs = 5_000) {
    const existing = this.received.find(call => call.method === method);
    if (existing) return Promise.resolve(existing);

    return new Promise<HostCall>((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error(`Timed out waiting for host call: ${method}`)), timeoutMs);
      const handlers = this.waiters.get(method) ?? [];
      handlers.push(call => {
        clearTimeout(timer);
        resolve(call);
      });
      this.waiters.set(method, handlers);
    });
  }

  async respond(call: HostCall, response: HostResponse) {
    await this.page.evaluate(({ mode, call, response }) => {
      const message = { id: call.id, method: '', data: response };
      if (mode === 'pc-app') {
        (window as any).chrome?.webview?.__emit(message);
        return;
      }
      // H5 和 PC Web SDK 都把宿主返回交给 window.jsBridge.receiveMessage。
      (window as any).jsBridge?.receiveMessage(
        call.id,
        '',
        JSON.stringify(response),
      );
    }, { mode: this.mode, call, response });
  }

  async emit(method: string, data?: unknown) {
    await this.page.evaluate(({ mode, method, data }) => {
      const message = { id: `event-${Date.now()}`, method, data };
      if (mode === 'pc-app') {
        (window as any).chrome?.webview?.__emit(message);
        return;
      }
      (window as any).jsBridge?.receiveMessage(
        message.id,
        method,
        JSON.stringify(data),
      );
    }, { mode: this.mode, method, data });
  }
}
