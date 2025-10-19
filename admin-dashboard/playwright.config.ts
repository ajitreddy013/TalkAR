import { defineConfig } from '@playwright/test';

const baseURL = process.env.FRONTEND_URL || 'http://localhost:3000';

export default defineConfig({
  testDir: 'tests/e2e',
  fullyParallel: true,
  retries: 0,
  // Store Playwright artifacts outside of the HTML report directory to avoid clashes
  outputDir: 'test-results/artifacts',
  reporter: [
    ['line'],
    ['junit', { outputFile: 'test-results/junit.xml' }],
    // Use the default folder outside of test-results to prevent cleanup conflicts
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
  },
});
