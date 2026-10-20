const path = require('node:path');
const {createRequire} = require('node:module');

const projectRoot = path.resolve(__dirname, '../../..');
const configRequire = createRequire(path.join(projectRoot, 'SupportConfigFiles/package.json'));
const {test, expect} = configRequire('@playwright/test');

for (const {route, username, destination} of [
    {route: '/login', username: 'browser@example.com', destination: '/'},
    {route: '/admin/login', username: 'browser-admin', destination: '/admin'}
]) {
    test(`${route} reveals and hides the password without disrupting sign-in`, async ({page}) => {
        await page.goto(route);
        const password = page.locator('#password');
        const toggle = page.locator('[data-password-toggle]');
        await expect(password).toHaveAttribute('type', 'password');
        await expect(toggle).toHaveAttribute('type', 'button');
        await expect(toggle).toHaveAttribute('aria-pressed', 'false');

        await password.fill('Browser-test-123');
        await toggle.click();
        await expect(password).toHaveAttribute('type', 'text');
        await expect(password).toBeFocused();
        await expect(toggle).toHaveAccessibleName('Hide password');
        await expect(toggle).toHaveAttribute('aria-pressed', 'true');
        await expect(password).toHaveValue('Browser-test-123');

        await toggle.click();
        await expect(password).toHaveAttribute('type', 'password');
        await expect(toggle).toHaveAccessibleName('Show password');
        await expect(toggle).toHaveAttribute('aria-pressed', 'false');

        await toggle.focus();
        await page.keyboard.press('Space');
        await expect(password).toHaveAttribute('type', 'text');
        await toggle.focus();
        await page.keyboard.press('Enter');
        await expect(password).toHaveAttribute('type', 'password');

        await page.setViewportSize({width: 390, height: 844});
        await expect(toggle).toBeVisible();
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
        await toggle.click();
        await page.locator('#username').fill(username);
        await page.locator('button[type="submit"]').click();
        await expect(page).toHaveURL(new RegExp(`${destination === '/' ? '/$' : '/admin$'}`));
    });
}
