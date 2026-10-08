import { test as base } from '@playwright/test';
import { InPageHostController } from '../hosts/in-page-host.js';
import type { HostMode } from '../types/host.js';

export const test = base.extend<{ host: InPageHostController }>({
  host: async ({ page }, use, testInfo) => {
    const mode = (testInfo.project.name === 'h5'
      ? 'h5'
      : testInfo.project.name === 'pc-app'
        ? 'pc-app'
        : 'pc-web') as HostMode;
    const host = new InPageHostController(page, mode);
    await host.install();
    await use(host);
  },
});

export { expect } from '@playwright/test';
