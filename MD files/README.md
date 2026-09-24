# Project configuration

Project JSON files and browser-test configuration live in [`SupportConfigFiles`](../SupportConfigFiles/):

- `package.json` and `package-lock.json`: pinned browser-test dependencies.
- `playwright.config.cjs`: browser-test settings, with paths resolved from the repository root.
- `rls-contract.json`: reviewed PostgreSQL security definitions. Maven packages only this JSON as `config/rls-contract.json` in the application jar. Do not replace it with definitions from a deployed database.
- `staff-config.json`: legacy staff configuration, retained for compatibility. Staff PIN credentials are no longer used for authentication. `STAFF_CONFIG_PATH` can still override the configured path for installations that keep this file elsewhere.

Run browser tooling from the repository root:

```sh
npm --prefix SupportConfigFiles ci
npm --prefix SupportConfigFiles exec -- playwright install chromium
npm --prefix SupportConfigFiles run test:browser
```

Browser tests require the packaged application and disposable PostgreSQL credentials described in [DATABASE_SETUP.md](DATABASE_SETUP.md).

Keep local secrets in the ignored `SupportConfigFiles/.env` file. Maven and the Docker build explicitly select the RLS contract rather than copying the entire configuration directory into the application.
