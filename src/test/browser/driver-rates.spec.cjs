const path = require('node:path');
const fs = require('node:fs');
const {createRequire} = require('node:module');
const projectRoot = path.resolve(__dirname, '../../..');
const {test, expect} = createRequire(path.join(projectRoot, 'SupportConfigFiles/package.json'))('@playwright/test');
const template = fs.readFileSync(path.join(projectRoot, 'src/main/resources/templates/admin.html'), 'utf8');
const dispatchMarkup = template.match(/<section id="dispatch"[\s\S]*?<\/section>/)[0];

function deferred() {
    let resolve;
    const promise = new Promise(done => { resolve = done; });
    return {promise, resolve};
}

async function setup(page) {
    const state = {
        rates: {1: {hourlyRate:0, perDeliveryRate:0}, 2: {hourlyRate:20, perDeliveryRate:5}},
        dashboardHandler: null, historyHandler: null, saveHandler: null
    };
    state.profile = branchId => ({id:42, branchId, firstName:'Driver', lastName:String(branchId),
        email:`driver${branchId}@example.com`, active:true, ...state.rates[branchId]});
    state.dashboard = branchId => ({branchId, drivers:[state.profile(branchId)], queue:[], shifts:[], assignments:[]});
    await page.route('**/dispatch-test', route => route.fulfill({contentType:'text/html', body:
        `<style>.d-none{display:none}</style>${dispatchMarkup}<script src="/js/admin-dispatch.js"></script>`}));
    await page.route('**/api/admin/dispatch**', async route => {
        const url = new URL(route.request().url());
        const branchId = Number(url.searchParams.get('branchId') || 1);
        if (url.pathname === '/api/admin/dispatch') {
            if (state.dashboardHandler) return state.dashboardHandler(route, branchId);
            return route.fulfill({json:state.dashboard(branchId)});
        }
        if (url.pathname.endsWith('/settlements')) {
            if (state.historyHandler) return state.historyHandler(route, branchId);
            return route.fulfill({json:[]});
        }
        if (url.pathname.endsWith('/rates')) {
            if (state.saveHandler) return state.saveHandler(route);
            const body = route.request().postDataJSON();
            state.rates[body.branchId] = {hourlyRate:Number(body.hourlyRate), perDeliveryRate:Number(body.perDeliveryRate)};
            return route.fulfill({json:state.profile(body.branchId)});
        }
        throw new Error(`Unexpected dispatch request: ${url}`);
    });
    await page.goto('/dispatch-test');
    await expect(page.locator('#dispatchRoster')).toContainText('Driver 1');
    await expect(page.locator('#dispatchHistory')).toContainText('No settled shifts yet.');
    return state;
}

async function refresh(page) {
    await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/admin/dispatch/settlements')),
        page.locator('#dispatchRefresh').click()
    ]);
}

async function finishResponse(page, responsePromise, release) {
    release.resolve();
    const response = await responsePromise;
    await response.finished();
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(resolve)));
}

test('refresh preserves edited rates while untouched fields accept server updates', async ({page}) => {
    const state = await setup(page);
    await page.locator('[data-hourly]').fill('60.00');
    state.rates[1] = {hourlyRate:75, perDeliveryRate:12};
    await refresh(page);
    await expect(page.locator('[data-hourly]')).toHaveValue('60.00');
    await expect(page.locator('[data-delivery]')).toHaveValue('12');
    await page.locator('[data-hourly]').fill('');
    await refresh(page);
    await expect(page.locator('[data-hourly]')).toHaveValue('');
});

test('failed saves preserve both edited rates through refresh', async ({page}) => {
    const state = await setup(page);
    state.saveHandler = route => route.fulfill({status:503, json:{error:'Save unavailable'}});
    await page.locator('[data-hourly]').fill('60.00');
    await page.locator('[data-delivery]').fill('10.00');
    await page.locator('[data-rates]').click();
    await expect(page.locator('#dispatchError')).toHaveText('Save unavailable');
    await expect(page.locator('[data-rates]')).toBeEnabled();
    await refresh(page);
    await expect(page.locator('[data-hourly]')).toHaveValue('60.00');
    await expect(page.locator('[data-delivery]')).toHaveValue('10.00');
});

test('successful save clears only the edits included in that save', async ({page}) => {
    const state = await setup(page);
    const started = deferred();
    const release = deferred();
    state.saveHandler = async route => {
        const body = route.request().postDataJSON();
        started.resolve();
        await release.promise;
        state.rates[1] = {hourlyRate:Number(body.hourlyRate), perDeliveryRate:Number(body.perDeliveryRate)};
        await route.fulfill({json:state.profile(1)});
    };
    await page.locator('[data-hourly]').fill('60.00');
    await page.locator('[data-delivery]').fill('10.00');
    await page.locator('[data-rates]').click();
    await started.promise;
    await refresh(page);
    await expect(page.locator('[data-rates]')).toBeDisabled();
    // Returning to the submitted value is still a new edit and must survive completion.
    await page.locator('[data-hourly]').fill('90.00');
    await page.locator('[data-hourly]').fill('60.00');
    release.resolve();
    await expect(page.locator('[data-rates]')).toBeEnabled();
    state.rates[1] = {hourlyRate:75, perDeliveryRate:12};
    await refresh(page);
    await expect(page.locator('[data-hourly]')).toHaveValue('60.00');
    await expect(page.locator('[data-delivery]')).toHaveValue('12');
    state.saveHandler = null;
    await page.locator('[data-rates]').click();
    await expect(page.locator('[data-rates]')).toBeEnabled();
    state.rates[1] = {hourlyRate:80, perDeliveryRate:15};
    await refresh(page);
    await expect(page.locator('[data-hourly]')).toHaveValue('80');
    await expect(page.locator('[data-delivery]')).toHaveValue('15');
});

test('older dashboard responses cannot overwrite newer data', async ({page}) => {
    const state = await setup(page);
    const started = deferred();
    const release = deferred();
    let first = true;
    state.dashboardHandler = async (route, branchId) => {
        const data = state.dashboard(branchId);
        if (first) {
            first = false;
            started.resolve();
            await release.promise;
        } else {
            data.drivers[0].lastName = 'Latest';
        }
        await route.fulfill({json:data});
    };
    await page.locator('#dispatchRefresh').click();
    await started.promise;
    await refresh(page);
    await expect(page.locator('#dispatchRoster')).toContainText('Driver Latest');
    const oldResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/dispatch');
    await finishResponse(page, oldResponse, release);
    await expect(page.locator('#dispatchRoster')).toContainText('Driver Latest');
});

test('branch changes isolate drafts and ignore old dashboard responses', async ({page}) => {
    const state = await setup(page);
    await page.locator('[data-hourly]').fill('60.00');
    const started = deferred();
    const release = deferred();
    state.dashboardHandler = async (route, branchId) => {
        if (branchId === 1) {
            started.resolve();
            await release.promise;
        }
        await route.fulfill({json:state.dashboard(branchId)});
    };
    await page.locator('#dispatchRefresh').click();
    await started.promise;
    await page.locator('#dispatchBranch').selectOption('2');
    await expect(page.locator('#dispatchRoster')).toContainText('Driver 2');
    await expect(page.locator('[data-hourly]')).toHaveValue('20');
    await page.locator('[data-hourly]').fill('40.00');
    const oldResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/admin/dispatch');
    await finishResponse(page, oldResponse, release);
    await expect(page.locator('#dispatchRoster')).toContainText('Driver 2');
    await expect(page.locator('[data-hourly]')).toHaveValue('40.00');
    state.dashboardHandler = null;
    await page.locator('#dispatchBranch').selectOption('1');
    await expect(page.locator('#dispatchRoster')).toContainText('Driver 1');
    await expect(page.locator('[data-hourly]')).toHaveValue('60.00');
});

test('old settlement responses cannot replace the selected branch history', async ({page}) => {
    const state = await setup(page);
    const started = deferred();
    const release = deferred();
    state.historyHandler = async (route, branchId) => {
        if (branchId === 1) {
            started.resolve();
            await release.promise;
        }
        await route.fulfill({json:[{shiftId:branchId, managerUsername:`Manager ${branchId}`}]});
    };
    await page.locator('#dispatchRefresh').click();
    await started.promise;
    await page.locator('#dispatchBranch').selectOption('2');
    await expect(page.locator('#dispatchHistory')).toContainText('Manager 2');
    const oldResponse = page.waitForResponse(response => response.url().includes('/dispatch/settlements?branchId=1'));
    await finishResponse(page, oldResponse, release);
    await expect(page.locator('#dispatchHistory')).toContainText('Manager 2');
});
