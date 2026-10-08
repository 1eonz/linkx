import type { BrowserContext, Route } from '@playwright/test';
type Rule = { method: string; path: string | RegExp; handler: (route: Route, state: Map<string, unknown>) => Promise<void> };
export class Scenario {
  readonly state = new Map<string, unknown>();
  readonly errors: string[] = [];
  readonly requests: { method: string; url: string; body: string | null }[] = [];
  private rules: Rule[] = [];
  private sockets = new Map<string, (socket: any) => void>();
  route(method: string, path: string | RegExp, handler: Rule['handler']) { this.rules.push({ method: method.toUpperCase(), path, handler }); }
  json(method: string, path: string | RegExp, data: unknown, status = 200) { this.route(method, path, async route => { await route.fulfill({ status, json: data }); }); }
  websocket(path: string, handler: (socket: any) => void) { this.sockets.set(path, handler); }
  async install(context: BrowserContext) {
    await context.route('**/*', async route => {
      const req = route.request();
      const url = new URL(req.url());
      const rule = this.rules.find(r => r.method === req.method() && (typeof r.path === 'string' ? r.path === url.pathname : r.path.test(url.pathname)));
      if (rule) {
        this.requests.push({ method: req.method(), url: req.url(), body: req.postData() });
        try { await rule.handler(route, this.state); } catch (error) { this.errors.push(String(error)); await route.abort(); }
        return;
      }
      const dynamic = ['xhr', 'fetch'].includes(req.resourceType()) && !url.pathname.startsWith('/@') && !url.pathname.includes('/node_modules/');
      if (dynamic) { this.errors.push(`Unconfigured API ${req.method()} ${url.pathname}`); await route.fulfill({ status: 501, json: { error: 'Unconfigured E2E API' } }); }
      else await route.continue();
    });
    await context.routeWebSocket('**/*', socket => {
      const url = new URL(socket.url());
      if (url.searchParams.has('token') && url.pathname === '/') { socket.connectToServer(); return; }
      const handler = this.sockets.get(url.pathname);
      if (handler) handler(socket);
      else { this.errors.push(`Unconfigured WebSocket ${url.pathname}`); socket.close({ code: 1008, reason: 'Unconfigured E2E WebSocket' }); }
    });
  }
}
