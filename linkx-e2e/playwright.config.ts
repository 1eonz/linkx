import { defineConfig, devices } from '@playwright/test';

const h5Url = process.env.LINKX_H5_URL ?? 'http://127.0.0.1:8001';
const webUrl = process.env.LINKX_WEB_URL ?? 'http://127.0.0.1:5173';

export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [['html'], ['junit', { outputFile: 'reports/junit.xml' }]] : [['list'], ['html']],
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    serviceWorkers: 'block',
  },
  projects: [
    {
      name: 'h5',
      testMatch: /tests\/h5\/.*\.spec\.ts/,
      use: { ...devices['Pixel 7'], baseURL: h5Url },
    },
    {
      name: 'pc-web',
      testMatch: /tests\/pc-web\/.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: webUrl },
    },
    {
      name: 'pc-app',
      testMatch: /tests\/pc-app\/.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: webUrl },
    },
    {
      name: 'contracts',
      testMatch: /tests\/contracts\/.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: webUrl },
    },
  ],
});
