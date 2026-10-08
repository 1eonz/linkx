import { randomUUID } from 'node:crypto';
import type { BrowserContext, Frame } from '@playwright/test';

export type Target = 'h5portal' | 'web-bspc' | 'web-cspc';
export type HostResult = { errorCode: number; errorMsg?: string; data?: unknown };
export type HostCall = { id: string; method: string; data: unknown; sequence: number; frame: Frame };
type Handler = (call: HostCall) => HostResult | Promise<HostResult>;
type Waiter = { method: string; predicate: (call: HostCall) => boolean; resolve: (call: HostCall) => void; reject: (error: Error) => void; timer: NodeJS.Timeout };

export class OfflineHost {
  readonly calls: HostCall[] = [];
  readonly errors: string[] = [];
  readonly storage = new Map<string, string>();
  private handlers = new Map<string, Handler | null>();
  private waiters: Waiter[] = [];
  private consumed = new Set<number>();
  private responded = new Set<number>();

  constructor(readonly context: BrowserContext, readonly target: Target, user: Record<string, unknown> = { userId: 'e2e-user', userid: 'e2e-user', username: 'E2E User', aastoken: 'offline-token' }) {
    this.on('getUserInfo', () => ({ errorCode: 0, data: user }));
    this.on('getVersion', () => ({ errorCode: 0, data: { versionCode: 843, deviceType: 'Android', versionName: 'offline-test' } }));
    this.on('getStorage', call => {
      const data = call.data as { data?: string } | string;
      return { errorCode: 0, data: this.storage.get(typeof data === 'string' ? data : data?.data ?? '') ?? '' };
    });
    this.on('setStorage', call => {
      const data = call.data as { key: string; value: string };
      this.storage.set(data.key, data.value);
      return { errorCode: 0, data: true };
    });
  }

  on(method: string, handler: Handler) { this.handlers.set(method, handler); }
  manual(method: string) { this.handlers.set(method, null); }

  async install() {
    await this.context.exposeBinding('__e2eHostCall', async ({ frame }, request: { id: string; method?: string; data: unknown }) => {
      if (!request.method || request.method === 'READY') return;
      const call = { ...request, method: request.method, frame, sequence: this.calls.length };
      this.calls.push(call);
      const waiter = this.waiters.find(w => w.method === call.method && w.predicate(call));
      if (waiter) { this.removeWaiter(waiter); this.consumed.add(call.sequence); waiter.resolve(call); }
      if (!this.handlers.has(call.method)) {
        this.errors.push(`Unknown host method ${call.method} (${call.id})`);
        await this.respond(call, { errorCode: 501, errorMsg: `Unconfigured host method: ${call.method}` });
        return;
      }
      const handler = this.handlers.get(call.method);
      if (handler) {
        try { await this.respond(call, await handler(call)); }
        catch (error) { this.errors.push(`Host handler ${call.method}: ${String(error)}`); }
      }
    });
    await this.context.addInitScript(({ target }) => {
      const win = window as any;
      let port: MessagePort | undefined;
      let ready = false;
      const listeners = new Set<(event: MessageEvent) => void>();
      const send = (raw: string | object) => void win.__e2eHostCall(typeof raw === 'string' ? JSON.parse(raw) : raw);
      win.__e2eHost = {
        deliver(message: any) {
          if (target === 'web-bspc') { if (!ready || !port) throw new Error('BSPC channel not ready'); port.postMessage(message); }
          else if (target === 'web-cspc') listeners.forEach(fn => fn(new MessageEvent('message', { data: JSON.stringify(message) })));
          else {
            if (!win.jsBridge) throw new Error('H5 jsBridge not loaded');
            return win.jsBridge.receiveMessage(message.id, message.method, typeof message.data === 'string' ? message.data : JSON.stringify(message.data));
          }
        },
        async connect() {
          if (target !== 'web-bspc') return;
          if (ready) return;
          const channel = new MessageChannel();
          port = channel.port1;
          await new Promise<void>((resolve, reject) => {
            const timer = setTimeout(() => reject(new Error('BSPC READY timed out')), 5000);
            port!.onmessage = event => {
              if (event.data.method === 'READY') { ready = true; clearTimeout(timer); resolve(); }
              else send(event.data);
            };
            window.postMessage({ method: 'SETUP_CHANNEL', id: 'e2e-setup' }, '*', [channel.port2]);
          });
        },
      };
      if (target === 'h5portal') win.flutterNativeBridge = { postMessage: send };
      if (target === 'web-cspc') {
        win.PIM_GetPlatform = () => 'win';
        win.chrome = win.chrome || {};
        win.chrome.webview = { postMessage: send, addEventListener: (name: string, listener: any) => { if (name === 'message') listeners.add(listener); }, removeEventListener: (_name: string, listener: any) => listeners.delete(listener) };
      }
      if (target === 'web-bspc') {
        // SDK installs the setup listener synchronously; retry until its jsBridge exists.
        const timer = setInterval(() => {
          if (win.jsBridge && !ready) { clearInterval(timer); void win.__e2eHost.connect().catch((e: Error) => { win.__e2eHostFailure = e.message; }); }
        }, 10);
        window.addEventListener('pagehide', () => clearInterval(timer), { once: true });
      }
    }, { target: this.target });
  }

  waitForCall(method: string, options: { timeoutMs?: number; predicate?: (call: HostCall) => boolean } = {}) {
    const predicate = options.predicate ?? (() => true);
    const existing = this.calls.find(call => !this.consumed.has(call.sequence) && call.method === method && predicate(call));
    if (existing) { this.consumed.add(existing.sequence); return Promise.resolve(existing); }
    return new Promise<HostCall>((resolve, reject) => {
      const waiter: Waiter = { method, predicate, resolve, reject, timer: setTimeout(() => { this.removeWaiter(waiter); reject(new Error(`Host call timeout: ${method}`)); }, options.timeoutMs ?? 5000) };
      this.waiters.push(waiter);
    });
  }
  private removeWaiter(waiter: Waiter) { clearTimeout(waiter.timer); this.waiters = this.waiters.filter(w => w !== waiter); }
  async respond(call: HostCall, result: HostResult) {
    if (this.responded.has(call.sequence)) throw new Error(`Already responded to ${call.id}`);
    this.responded.add(call.sequence);
    await this.deliver(call.frame, { id: call.id, method: '', data: result });
  }
  async emit(frame: Frame, method: string, data: unknown) { await this.deliver(frame, { id: randomUUID(), method, data: this.target === 'web-bspc' ? data : JSON.stringify(data) }); }
  private async deliver(frame: Frame, message: unknown) { await frame.evaluate(message => (window as any).__e2eHost.deliver(message), message); }
  evidence() { return { target: this.target, calls: this.calls.map(({ frame, ...call }) => ({ ...call, url: frame.url() })), errors: this.errors }; }
  dispose() {
    for (const waiter of [...this.waiters]) { this.removeWaiter(waiter); waiter.reject(new Error('Host controller disposed')); }
  }
}
