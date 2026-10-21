const path = require('node:path');
const fs = require('node:fs');
const {createRequire} = require('node:module');
const root = path.resolve(__dirname, '../../..');
const {test, expect} = createRequire(path.join(root, 'SupportConfigFiles/package.json'))('@playwright/test');
const template = fs.readFileSync(path.join(root, 'src/main/resources/templates/admin.html'), 'utf8');
const markup = template.match(/<section id="dispatch"[\s\S]*?<\/section>/)[0];

async function setup(page) {
    const state = {
        rates:{hourlyRate:0, perDeliveryRate:0},
        preview:{shiftId:9, deliveredCount:3, approvedMinutes:240, clockInTime:'09:00:00', clockOutTime:'13:00:00',
            orderSales:1000, onlineOrderPaid:0, onlineTips:0, cashTips:0,
            hourlyRate:0, perDeliveryRate:0, hourlyPay:0, deliveryPay:0, expectedCashDue:1000},
        previewHandler:null, saveHandler:null, previewError:false, saveError:false
    };
    const profile = () => ({id:42, branchId:1, firstName:'Test', lastName:'Driver', active:true,
        email:'driver@example.com', ...state.rates});
    await page.addInitScript(() => {
        window.createSimpleStompClient = options => ({
            connect:() => options.onConnect(),
            subscribe:(_topic, callback) => {window.dispatchRateEvent = callback;}
        });
    });
    await page.route('**/cash-up-test', route => route.fulfill({contentType:'text/html', body:
        `<style>.d-none{display:none}</style>${markup}<script src="/js/admin-dispatch.js"></script>`}));
    await page.route('**/api/admin/dispatch**', async route => {
        const url = new URL(route.request().url());
        if (url.pathname === '/api/admin/dispatch') return route.fulfill({json:{branchId:1, drivers:[profile()],
            shifts:[{id:9, driverId:42, state:'CLOCKED_OUT', ...state.rates}], queue:[], assignments:[]}});
        if (url.pathname.endsWith('/settlements')) return route.fulfill({json:[]});
        if (url.pathname.endsWith('/cash-up')) {
            if (state.previewHandler) return state.previewHandler(route);
            if (state.previewError) return route.fulfill({status:503, json:{error:'Preview unavailable'}});
            return route.fulfill({json:state.preview});
        }
        if (url.pathname.endsWith('/rates')) {
            if (state.saveError) return route.fulfill({status:503, json:{error:'Rate save unavailable'}});
            if (state.saveHandler) await state.saveHandler(route.request().postDataJSON());
            return route.fulfill({json:profile()});
        }
        throw new Error(`Unexpected dispatch request: ${url}`);
    });
    await page.goto('/cash-up-test');
    await page.getByRole('button', {name:'Cash-up', exact:true}).click();
    await expect(page.locator('#dispatchExpectedCash')).toContainText(/R\s*1\s?000,00/);
    return state;
}

test('rate saves and live updates recalculate both deductions without resetting cash-up entries', async ({page}) => {
    const state = await setup(page);
    await page.locator('#dispatchCashHanded').fill('900.00');
    await page.locator('#dispatchTimeReason').fill('Manager approved hours');
    await page.locator('#dispatchVarianceReason').fill('Cash recount');
    state.saveHandler = async body => {
        state.rates = {hourlyRate:Number(body.hourlyRate), perDeliveryRate:Number(body.perDeliveryRate)};
        Object.assign(state.preview, state.rates, {hourlyPay:60, deliveryPay:75, expectedCashDue:865});
    };
    await page.locator('[data-hourly]').fill('15.00');
    await page.locator('[data-delivery]').fill('25.00');
    await page.locator('[data-rates]').click();
    await expect(page.locator('#dispatchExpectedCash')).toContainText(/R\s*865,00/);
    await expect(page.locator('#dispatchHourlyPay')).toContainText(/R\s*60,00/);
    await expect(page.locator('#dispatchDeliveryPay')).toContainText(/R\s*75,00/);
    await expect(page.locator('#dispatchHourlyCalculation')).toContainText(/R\s*15,00\/hour/);
    await expect(page.locator('#dispatchDeliveryCalculation')).toContainText(/R\s*25,00\/delivery/);
    await expect(page.locator('#dispatchVariance')).toContainText(/R\s*35,00/);

    state.rates = {hourlyRate:20, perDeliveryRate:30};
    Object.assign(state.preview, state.rates, {hourlyPay:80, deliveryPay:90, expectedCashDue:830});
    await page.evaluate(() => window.dispatchRateEvent({body:JSON.stringify({eventType:'RATES_CHANGED', branchId:1})}));
    await expect(page.locator('#dispatchExpectedCash')).toContainText(/R\s*830,00/);
    await expect(page.locator('#dispatchVariance')).toContainText(/R\s*70,00/);
    await expect(page.locator('#dispatchClockInTime')).toHaveValue('09:00');
    await expect(page.locator('#dispatchClockOutTime')).toHaveValue('13:00');
    await expect(page.locator('#dispatchCashHanded')).toHaveValue('900.00');
    await expect(page.locator('#dispatchCashPaid')).toHaveValue('0.00');
    await expect(page.locator('#dispatchTimeReason')).toHaveValue('Manager approved hours');
    await expect(page.locator('#dispatchVarianceReason')).toHaveValue('Cash recount');
    await expect(page.locator('#dispatchCashUpSubmit')).toBeEnabled();

    state.previewError = true;
    await page.locator('#dispatchRefresh').click();
    await expect(page.locator('#dispatchCashUpSubmit')).toBeDisabled();
    await expect(page.locator('#dispatchError')).toHaveText('Preview unavailable');
    await expect(page.locator('#dispatchCashHanded')).toHaveValue('900.00');
});

test('an older preview cannot replace the rates returned after a save', async ({page}) => {
    const state = await setup(page);
    let releaseOld;
    let oldStarted;
    const started = new Promise(resolve => {oldStarted = resolve;});
    const release = new Promise(resolve => {releaseOld = resolve;});
    let delay = true;
    state.previewHandler = async route => {
        const preview = {...state.preview};
        if (delay) {delay = false; oldStarted(); await release;}
        await route.fulfill({json:preview});
    };
    await page.locator('#dispatchRefresh').click();
    await started;
    state.saveHandler = async () => {
        state.rates = {hourlyRate:15, perDeliveryRate:25};
        Object.assign(state.preview, state.rates, {hourlyPay:60, deliveryPay:75, expectedCashDue:865});
    };
    await page.locator('[data-hourly]').fill('15.00');
    await page.locator('[data-delivery]').fill('25.00');
    await page.locator('[data-rates]').click();
    await expect(page.locator('#dispatchExpectedCash')).toContainText(/R\s*865,00/);
    const oldResponse = page.waitForResponse(response => response.url().includes('/cash-up')
        && response.status() === 200);
    releaseOld();
    await (await oldResponse).finished();
    await expect(page.locator('#dispatchExpectedCash')).toContainText(/R\s*865,00/);
    await expect(page.locator('#dispatchCashHanded')).toHaveValue('1000.00');
});

test('a failed rate save restores the saved preview and keeps the failure visible', async ({page}) => {
    const state = await setup(page);
    state.saveError = true;
    await page.locator('#dispatchCashHanded').fill('900.00');
    await page.locator('[data-hourly]').fill('15.00');
    await page.locator('[data-delivery]').fill('25.00');
    await page.locator('[data-rates]').click();
    await expect(page.locator('#dispatchCashUpSubmit')).toBeEnabled();
    await expect(page.locator('#dispatchError')).toHaveText('Rate save unavailable');
    await expect(page.locator('#dispatchExpectedCash')).toContainText(/R\s*1\s?000,00/);
    await expect(page.locator('#dispatchCashHanded')).toHaveValue('900.00');
    await expect(page.locator('[data-hourly]')).toHaveValue('15.00');
    await expect(page.locator('[data-delivery]')).toHaveValue('25.00');
});
