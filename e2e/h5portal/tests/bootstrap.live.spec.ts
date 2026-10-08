import { test, expect } from '../../shared/fixtures/test.js';
test('live H5 SDK returns authenticated host identity', async ({ hostPage }) => {
  const user = await hostPage.evaluate(() => (window as any).WeSpaceSDK.getUserInfo());
  expect(user).toBeTruthy();
  expect(user.userId || user.userid).toBeTruthy();
  expect(user.aastoken).toBeTruthy();
});
