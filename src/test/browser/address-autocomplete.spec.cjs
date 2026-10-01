const path = require('node:path');
const {createRequire} = require('node:module');

const projectRoot = path.resolve(__dirname, '../../..');
const configRequire = createRequire(path.join(projectRoot, 'SupportConfigFiles/package.json'));
const {test, expect} = configRequire('@playwright/test');

async function mountAddressSearch(page) {
    await page.setContent(`
        <form id="addressForm">
            <div id="testAddress"
                 data-address-autocomplete
                 data-require="true"
                 data-google-maps-key="test-key"
                 data-field-google-place-id="#googlePlaceId"
                 data-field-formatted-address="#formattedAddress"
                 data-field-latitude="#latitude"
                 data-field-longitude="#longitude"
                 data-field-house-number="#houseNumber"
                 data-field-street="#street"
                 data-field-area="#area"
                 data-field-city="#city"
                 data-field-postal-code="#postalCode"
                 data-field-complex-name="#complexName"
                 data-field-province="#province"
                 data-field-country="#country">
                <label>Delivery address</label>
                <div data-address-widget></div>
                <small data-address-status hidden></small>
            </div>
            <input id="googlePlaceId">
            <input id="formattedAddress">
            <input id="latitude">
            <input id="longitude">
            <input id="houseNumber">
            <input id="street">
            <input id="area">
            <input id="city">
            <input id="postalCode">
            <input id="complexName" value="Unit 4">
            <input id="province">
            <input id="country">
        </form>
    `);
    await page.evaluate(() => {
        window.__addressRequests = [];
        window.__addressTokens = 0;
        window.__placeFields = [];

        class AutocompleteSessionToken {
            constructor() {
                this.id = ++window.__addressTokens;
            }
        }

        function prediction(label, id = label.toLowerCase().replaceAll(' ', '-')) {
            return {
                text: {toString: () => `${label}, Cape Town, South Africa`},
                toPlace() {
                    return {
                        id: `places/${id}`,
                        formattedAddress: `${label}, Cape Town, 7550, South Africa`,
                        location: {lat: () => -33.861, lng: () => 18.65},
                        addressComponents: [
                            {longText: '12', types: ['street_number']},
                            {longText: label, types: ['route']},
                            {longText: 'Kenridge', types: ['sublocality_level_1']},
                            {longText: 'Cape Town', types: ['locality']},
                            {longText: '7550', types: ['postal_code']},
                            {longText: 'Western Cape', types: ['administrative_area_level_1']},
                            {longText: 'South Africa', types: ['country']}
                        ],
                        async fetchFields(request) {
                            window.__placeFields.push(request.fields);
                        }
                    };
                }
            };
        }

        const AutocompleteSuggestion = {
            async fetchAutocompleteSuggestions(request) {
                window.__addressRequests.push({
                    input: request.input,
                    includedRegionCodes: request.includedRegionCodes,
                    language: request.language,
                    region: request.region,
                    token: request.sessionToken?.id
                });
                if (request.input === 'error') throw new Error('mock provider failure');
                if (request.input === 'zz') return {suggestions: []};
                if (request.input === 'old') {
                    await new Promise(resolve => setTimeout(resolve, 500));
                    return {suggestions: [{placePrediction: prediction('Old Street')}]};
                }
                if (request.input === 'new') {
                    await new Promise(resolve => setTimeout(resolve, 10));
                    return {suggestions: [{placePrediction: prediction('New Street')}]};
                }
                return {suggestions: [
                    {placePrediction: prediction('Main Street')},
                    {placePrediction: prediction('Main Road')},
                    {placePrediction: prediction('Main Avenue')},
                    {placePrediction: prediction('Main Close')},
                    {placePrediction: prediction('Main Crescent')},
                    {placePrediction: prediction('Main Lane')}
                ]};
            }
        };

        window.google = {maps: {importLibrary: async name => {
            if (name !== 'places') throw new Error('unexpected library');
            return {AutocompleteSessionToken, AutocompleteSuggestion};
        }}};
    });
    await page.addStyleTag({path: path.join(projectRoot, 'src/main/resources/static/css/address-autocomplete.css')});
    await page.addScriptTag({path: path.join(projectRoot, 'src/main/resources/static/js/address-autocomplete.js')});
    await expect(page.locator('.address-search-input')).toBeEnabled();
}

test('live address suggestions are debounced, accessible and populate verified fields', async ({page}) => {
    await mountAddressSearch(page);
    const search = page.locator('.address-search-input');

    await search.fill('m');
    await page.waitForTimeout(300);
    expect(await page.evaluate(() => window.__addressRequests)).toEqual([]);
    await expect(page.locator('[data-address-status]')).toHaveText('Type at least 2 characters to search for an address.');

    await search.fill('ma');
    await expect(page.getByRole('option')).toHaveCount(5);
    await expect(search).toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('.address-attribution')).toHaveText('Google Maps');
    expect(await page.evaluate(() => window.__addressRequests)).toEqual([{
        input: 'ma', includedRegionCodes: ['za'], language: 'en', region: 'za', token: 1
    }]);

    await search.press('ArrowDown');
    await expect(page.getByRole('option').first()).toHaveAttribute('aria-selected', 'true');
    await search.press('Enter');
    await expect(page.locator('[data-address-status]')).toHaveText('Address verified.');
    await expect(search).toHaveValue('Main Street, Cape Town, 7550, South Africa');
    await expect(page.locator('#googlePlaceId')).toHaveValue('places/main-street');
    await expect(page.locator('#street')).toHaveValue('Main Street');
    await expect(page.locator('#area')).toHaveValue('Kenridge');
    await expect(page.locator('#city')).toHaveValue('Cape Town');
    await expect(page.locator('#complexName')).toHaveValue('Unit 4');
    expect(await page.evaluate(() => window.__placeFields)).toEqual([
        ['id', 'formattedAddress', 'location', 'addressComponents']
    ]);
    expect(await page.evaluate(() => window.__addressTokens)).toBe(2);
    expect(await page.evaluate(() => window.PetesAddressAutocomplete.get('testAddress').isVerified())).toBe(true);

    await page.locator('#street').fill('Changed Street');
    await expect(page.locator('#googlePlaceId')).toHaveValue('');
    await expect(page.locator('[data-address-status]')).toContainText('Address edited.');
    expect(await page.evaluate(() => window.PetesAddressAutocomplete.get('testAddress').requireValid())).toBe(false);
});

test('address search ignores stale results and reports empty and failed searches', async ({page}) => {
    await mountAddressSearch(page);
    const search = page.locator('.address-search-input');

    await search.fill('old');
    await expect.poll(() => page.evaluate(() => window.__addressRequests.length)).toBe(1);
    await search.fill('new');
    await expect(page.getByRole('option')).toHaveText(['New Street, Cape Town, South Africa']);
    await page.waitForTimeout(550);
    await expect(page.getByRole('option')).toHaveText(['New Street, Cape Town, South Africa']);

    await page.getByRole('option').click();
    await expect(page.locator('[data-address-status]')).toHaveText('Address verified.');
    await expect(page.locator('#googlePlaceId')).toHaveValue('places/new-street');

    await search.fill('ma');
    await expect(page.getByRole('option')).toHaveCount(5);
    await search.press('Escape');
    await expect(search).toHaveAttribute('aria-expanded', 'false');
    await search.fill('zz');
    await expect(page.locator('.address-results-empty')).toBeVisible();
    await expect(page.locator('[data-address-status]')).toHaveText('No address suggestions found.');

    await search.fill('error');
    await expect(page.locator('[data-address-status]')).toHaveText('Google address search is unavailable. Check your connection and try again.');
    await expect(search).toHaveAttribute('aria-expanded', 'false');

    await search.fill('ma');
    await expect(page.getByRole('option')).toHaveCount(5);
    await search.press('Tab');
    await expect(search).toHaveAttribute('aria-expanded', 'false');

    await search.fill('');
    await expect(page.locator('[data-address-status]')).toHaveText('Start typing to search for an address.');
});

test('address search stays usable at a phone viewport', async ({page}) => {
    await page.setViewportSize({width: 390, height: 844});
    await mountAddressSearch(page);
    await page.locator('.address-search-input').fill('ma');
    await expect(page.getByRole('option')).toHaveCount(5);
    const inputBox = await page.locator('.address-search-input').boundingBox();
    const resultsBox = await page.locator('.address-results').boundingBox();
    expect(inputBox.width).toBeLessThanOrEqual(390);
    expect(resultsBox.width).toBeLessThanOrEqual(390);
    await expect(page.locator('.address-result').first()).toHaveCSS('min-height', '44px');
});
