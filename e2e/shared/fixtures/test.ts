import { test as base, expect } from '@playwright/test';
import { OfflineHost, type Target } from '../hosts/offline-host.js';
import { Scenario } from '../scenarios/scenario.js';
type Fixtures = { target: Target; mode: 'offline' | 'live'; host: OfflineHost; scenario: Scenario; hostPage: import('@playwright/test').Page };
export const test = base.extend<Fixtures>({
  target: ['h5portal', { option: true }],
  mode: ['offline', { option: true }],
  host: async ({ context, target, mode }, use, info) => {
    if (mode !== 'offline') throw new Error('Offline host fixture cannot be used in live tests');
    const host = new OfflineHost(context, target);
    await host.install();
    try { await use(host); }
    finally {
      host.dispose();
      await info.attach('host-evidence', { body: JSON.stringify(host.evidence(), null, 2), contentType: 'application/json' });
      expect(host.errors, 'Host protocol errors').toEqual([]);
    }
  },
  scenario: async ({ context, mode }, use, info) => {
    const scenario = new Scenario();
    if (mode === 'offline') await scenario.install(context);
    try { await use(scenario); }
    finally {
      await info.attach('scenario-evidence', { body: JSON.stringify({ requests: scenario.requests, errors: scenario.errors }, null, 2), contentType: 'application/json' });
      expect(scenario.errors, 'Unconfigured API / socket traffic').toEqual([]);
    }
  },
  hostPage: async ({ page, target, mode, baseURL }, use) => {
    if (mode !== 'live') throw new Error('hostPage is reserved for live bridge tests; offline tests configure scenario before page.goto');
    const url = new URL(baseURL!);
    const session = process.env.E2E_BRIDGE_SESSION || 'local';
    const health = process.env.E2E_BRIDGE_HEALTH_URL || new URL(`/__dev_bridge/${target}/health`, url).toString();
    const healthURL = new URL(health);
    healthURL.searchParams.set('session', session);
    const result = await page.request.get(healthURL.toString(), { timeout: 10000 });
    if (!result.ok()) throw new Error(`Live environment unavailable: health HTTP ${result.status()}`);
    const state = await result.json();
    if (!state.providerConnected) throw new Error(`Live environment unavailable: no provider for ${target}/${session}`);
    url.searchParams.set('bridge', 'stub');
    url.searchParams.set('bridgeTarget', target);
    url.searchParams.set('bridgeSession', session);
    if (process.env.E2E_BRIDGE_URL) url.searchParams.set('bridgeUrl', process.env.E2E_BRIDGE_URL);
    await page.goto(url.toString());
    await page.waitForFunction(() => (window as any).__DEV_BRIDGE__?.getStatus()?.providerConnected === true, undefined, { timeout: 45000 });
    await use(page);
  },
});
export { expect };
