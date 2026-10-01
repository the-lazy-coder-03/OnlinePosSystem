#!/usr/bin/env node
'use strict';

const path = require('node:path');
const {createRequire} = require('node:module');

const projectRoot = path.resolve(__dirname, '..');
const configRequire = createRequire(path.join(projectRoot, 'SupportConfigFiles/package.json'));
const {chromium} = configRequire('playwright');
const productionOrigin = (process.env.PRODUCTION_ORIGIN || 'https://crowdcam.co.za').replace(/\/$/, '');

function sanitize(message) {
    return String(message || '').replace(/AIza[0-9A-Za-z_-]+/g, '<redacted-key>');
}

async function main() {
    const parsedOrigin = new URL(productionOrigin);
    if (parsedOrigin.protocol !== 'https:' || parsedOrigin.hostname !== 'crowdcam.co.za') {
        throw new Error('Production address smoke test only permits https://crowdcam.co.za');
    }

    const browser = await chromium.launch({headless: true});
    try {
        const page = await browser.newPage();
        const providerFailures = [];
        page.on('response', response => {
            if (response.url().includes('places.googleapis.com') && response.status() >= 400) {
                providerFailures.push(`Google Places returned HTTP ${response.status()}`);
            }
        });
        page.on('requestfailed', request => {
            if (/googleapis\.com|gstatic\.com/.test(request.url())) {
                providerFailures.push(`Google request failed: ${sanitize(request.failure()?.errorText)}`);
            }
        });

        await page.goto(`${productionOrigin}/register`, {waitUntil: 'networkidle', timeout: 30000});
        const search = page.locator('.address-search-input');
        await search.waitFor({state: 'visible', timeout: 10000});
        if (!(await search.isEnabled())) throw new Error('Address search input is disabled');

        await search.fill('12 Main Road Cape Town');
        const firstResult = page.locator('.address-result').first();
        await firstResult.waitFor({state: 'visible', timeout: 15000});
        await firstResult.click();
        await page.locator('[data-address-status]').filter({hasText: 'Address verified.'})
            .waitFor({state: 'visible', timeout: 15000});

        const requiredValues = await Promise.all([
            page.locator('#registerGooglePlaceId').inputValue(),
            page.locator('#registerLatitude').inputValue(),
            page.locator('#registerLongitude').inputValue(),
        ]);
        if (requiredValues.some(value => !value.trim())) {
            throw new Error('Selected address did not populate place ID and coordinates');
        }
        if (providerFailures.length) throw new Error(providerFailures.join('; '));
        console.log('Production address search smoke test passed');
    } finally {
        await browser.close();
    }
}

main().catch(error => {
    console.error(`Production address search smoke test failed: ${sanitize(error.message)}`);
    process.exitCode = 1;
});
