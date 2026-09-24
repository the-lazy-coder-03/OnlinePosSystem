const {defineConfig} = require('@playwright/test');
const path = require('node:path');

const projectRoot = path.resolve(__dirname, '..');

module.exports = defineConfig({
    testDir: path.join(projectRoot, 'src/test/browser'),
    outputDir: path.join(projectRoot, 'target/browser-results'),
    workers: 1,
    retries: 0,
    use: {
        baseURL: 'http://127.0.0.1:18081', trace: 'retain-on-failure',
        launchOptions: {executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE}
    },
    webServer: {
        cwd: projectRoot,
        command: 'bash scripts/browser-test-server.sh',
        url: 'http://127.0.0.1:18081/',
        reuseExistingServer: false,
        timeout: 60000,
        gracefulShutdown: {signal: 'SIGTERM', timeout: 10000}
    }
});
