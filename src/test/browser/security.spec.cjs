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

async function chooseKenridgeWhenPrompted(page) {
    const branchButton = page.locator('#branchKenridge');
    if (await branchButton.isVisible()) await branchButton.click();
}

async function seedVerifiedAddress(page, ids = {}) {
    await page.evaluate(ids => {
        const values = {
            googlePlaceId: 'places/browser-test',
            formattedAddress: '12 Main Street, Kenridge, Cape Town, 7550, South Africa',
            latitude: '-33.861000',
            longitude: '18.650000',
            province: 'Western Cape',
            country: 'South Africa',
            ...ids
        };
        for (const [id, value] of Object.entries(values)) {
            const element = document.getElementById(id);
            if (element) {
                element.value = value;
                element.dispatchEvent(new Event('input', {bubbles: true}));
            }
        }
        document.getElementById('checkoutAddressAutocomplete')
            ?.dispatchEvent(new CustomEvent('address:state', {bubbles: true}));
    }, ids);
}

test('order renderer treats every customer field as text and allows only known status classes', async ({page}) => {
    await page.setContent('<main></main>');
    await page.addScriptTag({path: path.join(projectRoot, 'src/main/resources/static/js/pos-order-renderer.js')});
    const attack = '<img src=x onerror="window.compromised=true"><script>window.compromised=true</script>';
    await page.evaluate(attack => {
        const order = {
            id: 1, customerName: attack, branchName: attack, orderType: 'delivery', gateAccessCode: attack,
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

test('order confirmation map fallback hides failed map images', async ({page}) => {
    await page.setContent(`
        <div class="map-frame">
            <img data-confirmation-map src="/missing-map.png" alt="Delivery map">
            <div data-map-fallback hidden>Map unavailable</div>
        </div>
    `);
    await page.addScriptTag({path: path.join(projectRoot, 'src/main/resources/static/js/order-confirmation.js')});
    await page.locator('[data-confirmation-map]').dispatchEvent('error');
    await expect(page.locator('[data-confirmation-map]')).toBeHidden();
    await expect(page.locator('[data-map-fallback]')).toBeVisible();
});

test('pizza customization defaults to the available 30cm size', async ({page}) => {
    await login(page, 'browser@example.com');
    await page.goto('/order');
    await chooseKenridgeWhenPrompted(page);
    const pizza = page.locator('#listBody .row:not(.special-unavailable)')
        .filter({hasText: /Click to customize/}).first();
    await expect(pizza).toContainText('Click to customize');
    await pizza.click();
    await expect(page.locator('#size_30')).toBeChecked();
});

test('customer order, profile and live admin queue work with RLS and CSRF', async ({page, browser}) => {
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
    await login(page, 'browser@example.com');
    await page.goto('/order');
    await chooseKenridgeWhenPrompted(page);
    await expect(page.locator('#branchOverlay')).not.toHaveClass(/show/);
    await expect(page.locator('#typeCollection, #typeDelivery')).toHaveCount(0);
    await page.locator('#listBody .row').filter({hasText: /Click to customize|Click to add/}).first().click();
    await expect(page.locator('#btnAddToCart')).toBeVisible();
    await expect(page.locator('#stickyTotal')).not.toHaveText('R0.00');
    await page.locator('#btnAddToCart').click();
    await page.locator('#btnViewCart').click();
    const maliciousName = '<img src=x onerror="window.compromised=true">';
    await expect(page.locator('#custNameInput, #custPhoneInput')).toHaveCount(0);

    const adminContext = await browser.newContext();
    const admin = await adminContext.newPage();
    admin.on('pageerror', error => errors.push(error.message));
    admin.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
    await login(admin, 'branch@example.com');
    await admin.goto('/input-orders');
    await expect(admin.locator('#liveStatus')).toContainText('connected.');

    await Promise.all([
        page.waitForURL('**/checkout'),
        page.locator('#btnConfirmOrder').click()
    ]);
    await expect(page.locator('#checkoutItems .cart-item')).toHaveCount(1);
    await expect(page.locator('#deliveryButton')).not.toHaveClass(/active/);
    await expect(page.locator('#pickupButton')).not.toHaveClass(/active/);
    await expect(page.locator('#placeOrderButton')).toBeDisabled();
    const desktopSummary = await page.locator('.order-summary').boundingBox();
    const desktopLayout = await page.locator('.checkout-layout').boundingBox();
    expect(desktopSummary.width).toBeGreaterThanOrEqual(380);
    expect(desktopSummary.width / desktopLayout.width).toBeGreaterThan(0.39);
    await page.setViewportSize({width: 390, height: 844});
    await expect(page.locator('.order-summary')).toHaveCSS('position', 'static');
    await page.setViewportSize({width: 1280, height: 720});
    await page.locator('#deliveryButton').click();
    await expect(page.locator('#deliveryFields')).toBeVisible();
    await expect(page.locator('#checkoutAddressAutocomplete .address-search-input'))
        .toHaveValue('12 Main Street, Kenridge, Cape Town, 7550, South Africa');
    await expect(page.locator('#houseNumber')).toHaveValue('12');
    await expect(page.locator('#street')).toHaveValue('Main Street');
    await expect(page.locator('#area')).toHaveValue('Kenridge');
    await expect(page.locator('#city')).toHaveValue('Cape Town');
    await expect(page.locator('#postalCode')).toHaveValue('7550');
    await expect(page.locator('#googlePlaceId')).toHaveValue('places/browser-saved');
    expect(await page.evaluate(() => window.PetesAddressAutocomplete
        .get('checkoutAddressAutocomplete').isVerified())).toBe(true);
    await expect(page.locator('#street')).toHaveAttribute('required', '');
    await expect(page.locator('#gateAccessCode')).toHaveAttribute('maxlength', '64');
    await page.locator('#gateAccessCode').fill('Gate 4*');
    await page.locator('#pickupButton').click();
    await expect(page.locator('#deliveryFields')).toBeHidden();
    await expect(page.locator('#street')).not.toHaveAttribute('required', '');
    await expect(page.locator('#placeOrderButton')).toBeEnabled();
    await page.locator('#deliveryButton').click();
    await expect(page.locator('#gateAccessCode')).toHaveValue('Gate 4*');
    await page.locator('#fullName').fill(maliciousName);
    await page.locator('#phone').fill('0712345678');

    const placed = page.waitForResponse(response => response.url().endsWith('/api/orders') && response.request().method() === 'POST');
    await page.locator('#placeOrderButton').click();
    const response = await placed;
    expect(response.status()).toBe(200);
    await expect(page).toHaveURL(/\/orders\/\d+\/confirmation$/);
    const orderId = page.url().match(/\/orders\/(\d+)\/confirmation$/)[1];
    await expect(page.getByRole('heading', {name: 'Order Confirmed'})).toBeVisible();
    await expect(page.locator('body')).toContainText(`Order #${orderId}`);
    await expect(page.locator('body')).toContainText('Your order has been received by Pete\'s Pizzas Kenridge.');
    await expect(page.locator('body')).toContainText('Delivery');
    await expect(page.locator('body')).toContainText('12 Main Street, Kenridge, Cape Town, 7550, South Africa');
    await expect(page.locator('body')).toContainText('Gate 4*');
    await expect(page.locator('body')).toContainText('Map preview is unavailable for this order.');
    await expect(page.locator('[data-confirmation-map]')).toHaveCount(0);
    await expect(page.locator('.summary-total')).toContainText('R');
    await expect(admin.locator('#orderListQueue')).toContainText(maliciousName);
    await expect(admin.locator('#orderListQueue')).toContainText('Gate access: Gate 4*');
    await expect(admin.locator('#orderListQueue img')).toHaveCount(0);
    expect(await admin.evaluate(() => window.compromised)).toBeUndefined();

    const updated = admin.waitForResponse(response => response.url().endsWith(`/${orderId}/status`));
    await admin.getByRole('button', {name: 'Prepare', exact: true}).first().click();
    expect((await updated).status()).toBe(200);
    await expect(admin.locator('#orderListQueue .status').first()).toHaveText('Preparing');
    await page.goto('/profile/edit');
    await expect(page.locator('body')).toContainText(orderId);
    expect(errors).toEqual([]);
    await adminContext.close();
});

test('checkout preserves a temporary delivery address for the current order only', async ({page}) => {
    await login(page, 'browser@example.com');
    await page.goto('/order');
    await page.evaluate(() => {
        sessionStorage.setItem('petesPizza.checkoutDraft.v1', JSON.stringify({
            accountEmail: 'browser@example.com',
            branch: {id: 1, name: 'Kenridge'},
            orderType: 'delivery',
            customer: {
                name: 'Browser Customer',
                phone: '0712345678',
                deliveryAddress: {
                    googlePlaceId: 'places/temporary-address',
                    formattedAddress: '44 Temporary Road, Durbanville, Cape Town, 7550, South Africa',
                    latitude: '-33.833000',
                    longitude: '18.650000',
                    houseNumber: '44',
                    street: 'Temporary Road',
                    area: 'Durbanville',
                    city: 'Cape Town',
                    postalCode: '7550',
                    complexName: '',
                    province: 'Western Cape',
                    country: 'South Africa'
                }
            },
            items: [{type: 'menu', menuItemId: 1, name: 'Draft item', quantity: 1, pricing: {lineTotal: 10}}]
        }));
    });

    await page.goto('/checkout');
    await expect(page.locator('#checkoutAddressAutocomplete .address-search-input'))
        .toHaveValue('44 Temporary Road, Durbanville, Cape Town, 7550, South Africa');
    await expect(page.locator('#street')).toHaveValue('Temporary Road');
    await page.goto('/order');
    await page.goto('/checkout');
    await expect(page.locator('#checkoutAddressAutocomplete .address-search-input'))
        .toHaveValue('44 Temporary Road, Durbanville, Cape Town, 7550, South Africa');

    await page.evaluate(() => sessionStorage.removeItem('petesPizza.checkoutDraft.v1'));
    await page.reload();
    await expect(page.locator('#checkoutAddressAutocomplete .address-search-input'))
        .toHaveValue('12 Main Street, Kenridge, Cape Town, 7550, South Africa');
    await expect(page.locator('#googlePlaceId')).toHaveValue('places/browser-saved');
});

test('registration rotates the anonymous session and profile changes remain authenticated', async ({page, context}) => {
    await page.goto('/register');
    await expect(page.locator('#registerAddressAutocomplete .address-search-input')).toBeVisible();
    const before = (await context.cookies()).find(cookie => cookie.name === 'JSESSIONID').value;
    const fields = {
        firstName: 'Registered', lastName: 'Customer', email: 'registered@example.com', password: 'Browser-test-123',
        house_number: '12', street: 'Main Street', area: 'Kenridge', city: 'Cape Town', postalCode: '7550', phone: '0798765432'
    };
    for (const [name, value] of Object.entries(fields)) await page.locator(`[name="${name}"]`).fill(value);
    await seedVerifiedAddress(page, {
        registerGooglePlaceId: 'places/browser-registration',
        registerFormattedAddress: '12 Main Street, Kenridge, Cape Town, 7550, South Africa',
        registerLatitude: '-33.861000',
        registerLongitude: '18.650000',
        registerProvince: 'Western Cape',
        registerCountry: 'South Africa'
    });
    await page.locator('[name="preferred_store"]').selectOption('Kenridge Branch');
    await page.getByRole('button', {name: 'Register', exact: true}).click();
    await expect(page).toHaveURL('http://127.0.0.1:18081/');
    const after = (await context.cookies()).find(cookie => cookie.name === 'JSESSIONID').value;
    expect(after).not.toBe(before);
    await page.goto('/profile/edit');
    await expect(page.locator('#profileAddressAutocomplete .address-search-input'))
        .toHaveValue('12 Main Street, Kenridge, Cape Town, 7550, South Africa');
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
