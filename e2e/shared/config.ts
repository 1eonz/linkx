import { defineConfig, devices } from '@playwright/test';
import type { Target } from './hosts/offline-host.js';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../', import.meta.url));
const targets: Target[] = ['h5portal', 'web-bspc', 'web-cspc'];
export function config(kind: 'contracts' | 'offline' | 'live') {
  const selected = process.env.E2E_TARGET;
  if (selected && !targets.includes(selected as Target)) throw new Error(`Unknown E2E_TARGET ${selected}`);
  const contract = kind === 'contracts';
  const mode = kind === 'live' ? 'live' : 'offline';
  const harness = process.env.E2E_HARNESS_URL || 'http://127.0.0.1:4179';
  return defineConfig({
    testDir: root,
    testIgnore: ['**/h5portal/tests/home.spec.js'],
    testMatch: contract ? '**/contracts/*.spec.ts' : `**/tests/*.${mode}.spec.ts`,
    fullyParallel: mode === 'offline', workers: mode === 'live' ? 1 : undefined,
    timeout: 45000, expect: { timeout: 10000 }, retries: process.env.CI ? 1 : 0,
    outputDir: `${root}/reports/${kind}/results`,
    reporter: [['list'], ['html', { outputFolder: `reports/${kind}/html`, open: 'never' }], ['junit', { outputFile: `reports/${kind}/junit.xml` }]],
    use: { ignoreHTTPSErrors: true, serviceWorkers: 'block', trace: 'retain-on-failure', screenshot: 'only-on-failure', video: 'retain-on-failure' },
    projects: targets.filter(target => !selected || selected === target).map(target => ({
      name: `${target}-${kind}`, testMatch: contract ? '**/contracts/*.spec.ts' : `**/${target}/tests/*.${mode}.spec.ts`,
      use: { ...(target === 'h5portal' ? devices['iPhone 13'] : devices['Desktop Chrome']), browserName: 'chromium' as const, channel: process.env.E2E_BROWSER_CHANNEL as 'chrome' | 'msedge' | undefined, target, mode, baseURL: contract ? harness : target === 'h5portal' ? process.env.E2E_H5_URL || 'http://localhost:8001/linkx/h5portal/' : process.env.E2E_WEB_URL || 'https://localhost:3100/' },
    })),
    webServer: contract ? { command: 'node shared/harness/server.mjs', cwd: root, url: harness, reuseExistingServer: !process.env.CI } : process.env.E2E_START_SERVERS === '1' ? [
      { command: 'pnpm --dir ../H5Portal dev', url: 'http://localhost:8001', reuseExistingServer: !process.env.CI, timeout: 120000 },
      { command: 'pnpm --dir ../web dev', url: 'https://localhost:3100', ignoreHTTPSErrors: true, reuseExistingServer: !process.env.CI, timeout: 120000 },
    ] : undefined,
  });
}
