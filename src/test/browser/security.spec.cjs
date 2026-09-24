const path = require('node:path');
const {createRequire} = require('node:module');

const projectRoot = path.resolve(__dirname, '../../..');
const configRequire = createRequire(path.join(projectRoot, 'SupportConfigFiles/package.json'));
const {test, expect} = configRequire('@playwright/test');

async function login(page, username) {
    await page.goto('/login');
    await page.locator('#username').fill(username);
    await page.locator('#password').fill('Browser-test-123');
    await page.getByRole('button', {name: 'Login', exact: true}).click();
    await expect(page).not.toHaveURL(/\/login/);
}

test('order renderer treats every customer field as text and allows only known status classes', async ({page}) => {
    await page.setContent('<main></main>');
    await page.addScriptTag({path: path.join(projectRoot, 'src/main/resources/static/js/pos-order-renderer.js')});
    const attack = '<img src=x onerror="window.compromised=true"><script>window.compromised=true</script>';
    await page.evaluate(attack => {
        const order = {
            id: 1, customerName: attack, branchName: attack, orderType: attack,
            houseNumber: attack, street: attack, area: attack, city: attack,
            postalCode: attack, complexName: attack, status: attack,
            menuItems: [{qty: 1, menuItemName: attack, extras: [{name: attack}]}],
            pizzaItems: [{qty: 1, pizzaName: attack, pizzaSizeCm: attack,
                pizzaBaseOptionName: attack, extras: [{ingredientName: attack}]}]
        };
        const options = {showBranch: true, onStatusChange: status => window.nextStatus = status};
        document.querySelector('main').append(PosOrderRenderer.queueRow(order, options), PosOrderRenderer.orderCard(order, options));
    }, attack);
    await expect(page.locator('main img, main script')).toHaveCount(0);
    await expect(page.locator('main')).toContainText(attack);
    await expect(page.locator('.status')).toHaveAttribute('class', 'status Unknown');
    expect(await page.evaluate(() => window.compromised)).toBeUndefined();
    await page.evaluate(() => {
        document.querySelector('main').replaceChildren(PosOrderRenderer.queueRow({id: 2, status: 'Pending'}, {
            onStatusChange: status => window.nextStatus = status
        }));
    });
    await page.getByRole('button', {name: 'Prepare', exact: true}).click();
    expect(await page.evaluate(() => window.nextStatus)).toBe('Preparing');
});

test('customer order, profile and live admin queue work with RLS and CSRF', async ({page, browser}) => {
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
    await login(page, 'browser@example.com');
    await page.goto('/order');
    await page.locator('#branchKenridge').click();
    await page.locator('#typeCollection').click();
    await page.locator('#listBody .row').first().click();
    await expect(page.locator('#btnAddToCart')).toBeVisible();
    await expect(page.locator('#stickyTotal')).not.toHaveText('R0.00');
    await page.locator('#btnAddToCart').click();
    await page.locator('#btnViewCart').click();
    const maliciousName = '<img src=x onerror="window.compromised=true">';
    await page.locator('#custNameInput').fill(maliciousName);

    const adminContext = await browser.newContext();
    const admin = await adminContext.newPage();
    admin.on('pageerror', error => errors.push(error.message));
    admin.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
    await login(admin, 'branch@example.com');
    await admin.goto('/input-orders');
    await expect(admin.locator('#liveStatus')).toContainText('connected.');

    await page.locator('#btnConfirmOrder').click();
    await expect(page).toHaveURL(/\/checkout$/);
    await expect(page.locator('#fullName')).toHaveValue(maliciousName);
    await page.locator('#phone').fill('0712345678');

    const placed = page.waitForResponse(response => response.url().endsWith('/api/orders') && response.request().method() === 'POST');
    await page.locator('#placeOrderButton').click();
    const response = await placed;
    expect(response.status()).toBe(200);
    const order = await response.json();
    await expect(admin.locator('#orderListQueue')).toContainText(maliciousName);
    await expect(admin.locator('#orderListQueue img')).toHaveCount(0);
    expect(await admin.evaluate(() => window.compromised)).toBeUndefined();

    const updated = admin.waitForResponse(response => response.url().endsWith(`/${order.id}/status`));
    await admin.getByRole('button', {name: 'Prepare', exact: true}).first().click();
    expect((await updated).status()).toBe(200);
    await expect(admin.locator('#orderListQueue .status').first()).toHaveText('Preparing');
    await page.goto('/profile/edit');
    await expect(page.locator('body')).toContainText(String(order.id));
    expect(errors).toEqual([]);
    await adminContext.close();
});

test('registration rotates the anonymous session and profile changes remain authenticated', async ({page, context}) => {
    await page.goto('/register');
    const before = (await context.cookies()).find(cookie => cookie.name === 'JSESSIONID').value;
    const fields = {
        firstName: 'Registered', lastName: 'Customer', email: 'registered@example.com', password: 'Browser-test-123',
        house_number: '12', street: 'Main Street', area: 'Kenridge', postalCode: '7550', phone: '0712345678'
    };
    for (const [name, value] of Object.entries(fields)) await page.locator(`[name="${name}"]`).fill(value);
    await page.locator('[name="preferred_store"]').selectOption('Kenridge Branch');
    await page.getByRole('button', {name: 'Register', exact: true}).click();
    await expect(page).toHaveURL('http://127.0.0.1:18081/');
    const after = (await context.cookies()).find(cookie => cookie.name === 'JSESSIONID').value;
    expect(after).not.toBe(before);
    await page.goto('/profile/edit');
    await page.locator('#firstName').fill('Updated');
    await page.getByRole('button', {name: 'Save Profile', exact: true}).click();
    await expect(page).toHaveURL(/\/profile\/edit\?success/);
    await expect(page.locator('#firstName')).toHaveValue('Updated');
});

test('public pages render and browser sessions cannot mutate without CSRF', async ({page}) => {
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    for (const route of ['/', '/menu/kenridge', '/menu/uitzicht', '/register', '/forgot-password', '/reset-password?token=invalid']) {
        expect((await page.goto(route)).status()).toBe(200);
    }
    await login(page, 'browser-admin');
    await page.goto('/admin');
    expect(await page.evaluate(async () => (await fetch('/api/orders', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: '{}'
    })).status)).toBe(403);
    expect(await page.evaluate(async () => (await fetch('/api/orders', {
        method: 'POST', headers: {'Content-Type': 'application/json', Authorization: 'Bearer invalid'}, body: '{}'
    })).status)).toBe(401);
    expect(errors).toEqual([]);
});
