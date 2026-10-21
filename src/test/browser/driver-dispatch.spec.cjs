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

async function refreshDispatch(page) {
    await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/admin/dispatch/settlements')
            && response.status() === 200),
        page.locator('#dispatchRefresh').click()
    ]);
}

test('driver API returns 401 instead of redirecting expired sessions to a web page', async ({request}) => {
    for (const headers of [{}, {Authorization: 'Bearer invalid-test-token'}]) {
        const response = await request.get('/api/driver', {headers, maxRedirects: 0});
        expect(response.status()).toBe(401);
        expect(response.headers().location).toBeUndefined();
    }
});

test('manager dispatch and mobile driver shift flow', async ({browser}, testInfo) => {
    test.setTimeout(90000);
    const managerContext = await browser.newContext();
    const manager = await managerContext.newPage();
    await login(manager, 'branch@example.com');
    await manager.goto('/admin#dispatch');
    await expect(manager.locator('#dispatch')).toBeVisible();
    expect(await manager.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    await expect(manager.locator('#dispatchQueue')).toContainText('Browser Customer');
    await manager.locator('#dispatchEmail').fill('browser-driver@example.com');
    await manager.locator('#dispatchPromote button').click();
    await expect(manager.locator('#dispatchRoster')).toContainText('Browser Driver');
    await manager.locator('[data-hourly]').first().fill('60.00');
    await refreshDispatch(manager);
    await expect(manager.locator('[data-hourly]').first()).toHaveValue('60.00');
    await manager.locator('[data-delivery]').first().fill('10.00');
    await refreshDispatch(manager);
    await expect(manager.locator('[data-hourly]').first()).toHaveValue('60.00');
    await expect(manager.locator('[data-delivery]').first()).toHaveValue('10.00');
    const driverContext = await browser.newContext({viewport:{width:390,height:844}});
    const driver = await driverContext.newPage();
    await login(driver, 'browser-driver@example.com');
    await expect(driver).toHaveURL(/\/driver$/);
    await expect(driver.locator('#driverBranch')).toContainText('Kenridge');
    const [clockInResponse] = await Promise.all([
        driver.waitForResponse(response => response.url().endsWith('/api/driver/clock-in')
            && response.status() === 200),
        driver.locator('#driverClockIn').click()
    ]);
    expect(await clockInResponse.json()).toMatchObject({hourlyRate:0,perDeliveryRate:0});
    const [ratesResponse] = await Promise.all([
        manager.waitForResponse(response => response.url().includes('/rates') && response.status() === 200),
        manager.locator('[data-rates]').first().click()
    ]);
    expect(ratesResponse.request().postDataJSON()).toMatchObject({hourlyRate:'60.00',perDeliveryRate:'10.00'});
    expect(await ratesResponse.json()).toMatchObject({hourlyRate:60,perDeliveryRate:10});
    await expect(driver.locator('#driverShiftState')).toHaveText('On shift');
    await driver.locator('#driverClaim').click();
    await expect(driver.locator('#driverOrders')).toContainText('Browser Customer');
    await expect(driver.locator('#driverOrders')).toContainText('12 Main Street');
    const batchRow = manager.locator('#dispatchQueue .dispatch-row').filter({hasText:'Browser Batch'});
    await expect(batchRow).toBeVisible();
    await batchRow.locator('[data-batch-order]').check();
    await manager.locator('#dispatchBatchDriver').selectOption({index:1});
    await manager.locator('#dispatchBatchAssign').click();
    await expect(manager.locator('#dispatchAssignments')).toContainText('Browser Batch');
    await expect(driver.locator('#driverOrders')).toContainText('Browser Batch');
    expect(await driver.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    for (const name of ['Browser Customer','Browser Batch']) {
        const card = driver.locator('.driver-order').filter({hasText:name});
        await expect(card.getByRole('button', {name:'Confirm pickup'})).toBeEnabled();
        driver.once('dialog', dialog => dialog.accept());
        await card.getByRole('button', {name:'Confirm pickup'}).click();
        await card.getByRole('button', {name:'Mark delivered'}).click();
    }
    await driver.getByRole('button', {name:'Returning'}).click();
    await expect(manager.locator('#dispatchAlert')).toContainText('returning');
    await driver.getByRole('button', {name:'Clock out'}).click();
    await expect(manager.locator('#dispatchRoster')).toContainText('CLOCKED_OUT');

    await manager.getByRole('button', {name:'Cash-up'}).click();
    await expect(manager.locator('#dispatchCashUp')).toBeVisible();
    await manager.locator('#dispatchClockInTime').fill('00:00');
    await manager.locator('#dispatchClockOutTime').fill('00:00');
    await manager.locator('#dispatchClockOutTime').dispatchEvent('change');
    await expect(manager.locator('#dispatchApprovedDuration')).toHaveText('0h 0m');
    await expect(manager.locator('#dispatchExpectedCash')).toContainText(/R\s*55,00/);
    await manager.locator('#dispatchCashHanded').fill('55.00');
    await manager.locator('#dispatchCashPaid').fill('0.00');
    await expect(manager.locator('#dispatchVariance')).toContainText(/R\s*0,00/);
    const localTime = new Intl.DateTimeFormat('en-GB', {timeZone:'Africa/Johannesburg',
        hour:'2-digit', minute:'2-digit', hourCycle:'h23'}).format(new Date());
    const [hour, minute] = localTime.split(':').map(Number);
    if (hour * 60 + minute >= 2) {
        await manager.locator('#dispatchClockOutTime').fill('00:01');
        await manager.locator('#dispatchClockOutTime').dispatchEvent('change');
        await expect(manager.locator('#dispatchApprovedDuration')).toHaveText('0h 1m');
        await expect(manager.locator('#dispatchHourlyPay')).toContainText(/R\s*1,00/);
        await expect(manager.locator('#dispatchVariance')).toContainText(/R\s*1,00/);
        await expect(manager.locator('#dispatchCashHanded')).toHaveValue('55.00');
        await manager.locator('#dispatchClockOutTime').fill('00:00');
        await manager.locator('#dispatchClockOutTime').dispatchEvent('change');
        await expect(manager.locator('#dispatchVariance')).toContainText(/R\s*0,00/);
    }
    await manager.locator('#dispatchCashUp').evaluate(panel => panel.scrollIntoView({behavior:'instant', block:'start'}));
    expect(await manager.locator('#dispatchCashUp').evaluate(panel => panel.getBoundingClientRect().top
        >= document.querySelector('.admin-topbar').getBoundingClientRect().bottom)).toBe(true);
    await manager.screenshot({path:testInfo.outputPath('cash-up-rates.png')});
    await manager.setViewportSize({width:390, height:844});
    expect(await manager.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
    await manager.locator('#dispatchCashUp').evaluate(panel => panel.scrollIntoView({behavior:'instant', block:'start'}));
    expect(await manager.locator('#dispatchCashUp').evaluate(panel => panel.getBoundingClientRect().top
        >= document.querySelector('.admin-topbar').getBoundingClientRect().bottom)).toBe(true);
    await manager.screenshot({path:testInfo.outputPath('cash-up-rates-mobile.png')});
    manager.once('dialog', dialog => dialog.accept());
    await manager.locator('#dispatchCashUpSubmit').click();
    await expect(manager.locator('#dispatchHistory')).toContainText(/R\s*75,00/);
    await driver.reload();
    await expect(driver.locator('#driverSettlements')).toContainText(/R\s*55,00/);
    await driverContext.close();
    await managerContext.close();
});
