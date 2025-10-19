import { test, expect } from '@playwright/test';

test.describe('Admin Dashboard Smoke', () => {
  test('loads the home page', async ({ page }) => {
    await page.goto('/');
    // Basic assertion: app renders root element
    await expect(page).toHaveTitle(/TalkAR|React App/i);
  });
});
