Perform a comprehensive, adversarial security audit of my entire application and repository.

Production URL:
https://crowdcam.co.za

Assume I own and authorize security testing of this application, its codebase, and its own infrastructure. However, DO NOT perform destructive testing against production. Prefer code review, local testing, automated security tooling, and an isolated/local environment. Production testing must be low-impact and non-destructive.

The application is a Spring Boot web application backed by PostgreSQL and deployed using Docker and Nginx. It contains customer accounts, authentication, ordering functionality, customer information, branch/menu data, password resets, WebSockets, database migrations, and Row Level Security.

I want a genuinely deep security review, not a superficial checklist.

## Audit phase dashboard

Status legend:

- **PENDING** — review has not started.
- **IN PROGRESS** — review, remediation, or verification is underway.
- **BLOCKED** — a required external/manual action prevents verification.
- **COMPLETE** — every actionable finding in the phase is fixed and verified.

| Phase | Status | Date | Resolved findings | Evidence / remaining blocker |
|---:|---|---|---|---|
| 1 | COMPLETE | 2026-09-27 | — | Trust-boundary diagram and exhaustive endpoint inventory in `SECURITY_AUDIT.md`; controller/security/RLS/deployment inventory reviewed. |
| 2 | COMPLETE | 2026-09-27 | SA-002, SA-005, SA-006 | `MultiLoginTest`, `ApiSecurityTest`, credential refresh/JWT tests. |
| 3 | COMPLETE | 2026-09-27 | SA-002, SA-005, SA-006 | Reset entropy/hash/expiry/single-use/concurrency and credential invalidation verified in PostgreSQL and service tests. |
| 4 | COMPLETE | 2026-09-27 | SA-001, SA-003 | Cross-customer/cross-branch IDs, every order child, and mutation filtering verified by `RlsPostgresIT`. |
| 5 | COMPLETE | 2026-09-27 | SA-003, SA-005 | Registration forces USER/0; profile DTOs cannot set authority; account triggers and super-admin methods tested. |
| 6 | COMPLETE | 2026-09-27 | SA-001 | Separate owner/runtime roles, signed context, grants, forced RLS, definer functions, startup verifier and contract drift tests pass. |
| 7 | COMPLETE | 2026-09-27 | — | Native/JDBC/JPQL paths traced; runtime values use parameters; no reachable injection found. Migration identifiers are owner-controlled and quoted. |
| 8 | COMPLETE | 2026-09-27 | SA-004, SA-006 | Server pricing validation plus malformed/disabled/mismatch/quantity/replay/concurrency tests. |
| 9 | COMPLETE | 2026-09-27 | SA-003, SA-004, SA-006 | HTTP method/route authorization, DTO bounds, status codes, CORS, JWT and CSRF tests. |
| 10 | COMPLETE | 2026-09-27 | — | Session mutations reject missing/stale tokens; JSON auth exceptions are narrow; forms contain CSRF tokens. |
| 11 | COMPLETE | 2026-09-27 | SA-008 (code) | Renderer/template/DOM review and browser XSS regression; nonce CSP verified locally. Production header rollout tracked in Phase 15. |
| 12 | COMPLETE | 2026-09-27 | SA-002 | Anonymous/unknown/cross-branch subscriptions and all SEND denied; credential/access changes close sockets. |
| 13 | COMPLETE | 2026-09-27 | SA-002 | Secure/HttpOnly/SameSite cookies, session rotation/logout, stale credential invalidation and HTTPS behavior reviewed/tested. |
| 14 | COMPLETE | 2026-09-27 | — | Exact configured origin only, credentials without wildcard, attacker preflight 403 locally and in production. |
| 15 | BLOCKED | — | SA-008 fixed in code | Production still lacks CSP, Referrer-Policy and Permissions-Policy. Requires separately approved deployment and live browser verification. |
| 16 | BLOCKED | — | SA-013 fixed in code | TLS 1.1 rejected and Host 421 live; reviewed Nginx changes are not deployed/validated on the actual proxy. |
| 17 | BLOCKED | — | SA-009 fixed in code | Repository container is non-root/read-only/capability-dropped; exact production image/container inspection requires deployment access. |
| 18 | BLOCKED | — | Current-source cleanup complete | SA-011 Google key rotation and SA-012 former database credential rejection require provider-side action. No history rewrite performed. |
| 19 | COMPLETE | 2026-09-27 | SA-007 | OSV/Trivy app scans clean; stale Maven/Nginx images replaced; current runtime/build/proxy/PostgreSQL/Certbot bases manually triaged and fixable Certbot packages patched. |
| 20 | COMPLETE | 2026-09-27 | SA-014 | Request matchers/method security/Actuator/error/debug review; malformed and missing-route tests return sanitized results. |
| 21 | COMPLETE | 2026-09-27 | SA-006 | Dedicated bounded DTOs/explicit mapping; no request-bound JPA entities or bindable authority/price/owner fields. |
| 22 | COMPLETE | 2026-09-27 | SA-001, SA-014 | RLS/customer DTO review; logs and errors sanitized; branch staff see only linked customers. |
| 23 | COMPLETE | 2026-09-27 | SA-014 | Error inclusion disabled; authorization/database failures translated; malformed-request regression verified. |
| 24 | COMPLETE | 2026-09-27 | SA-006 | Login/register/reset/order/quote/geocode limits and bounded limiter storage verified with deterministic 429 behavior. |
| 25 | COMPLETE | 2026-09-28 | SA-007 | Full PostgreSQL verification (192 + 19 tests), five Playwright scenarios, syntax validation, and Gitleaks/OSV/Semgrep/Trivy/npm scans passed or were manually triaged. |

Focused verification command completed successfully:

```text
./scripts/test-postgres.sh -B -Dtest=ApiSecurityTest,MultiLoginTest,RlsPostgresIT test -Prls-it
Tests run: 40, failures: 0, errors: 0, skipped: 0
```

Final verification completed successfully:

```text
./scripts/test-postgres.sh -B clean verify -Prls-it
Unit/integration tests: 192 passed
Dedicated PostgreSQL RLS tests: 19 passed

npm --prefix SupportConfigFiles run test:browser
Playwright tests: 5 passed
```

## Phase completion evidence ledger

| Phases | Status / date | Resolved IDs | Verification evidence and commands |
|---|---|---|---|
| 1 | COMPLETE / 2026-09-27 | — | Trust diagram and route inventory in `SECURITY_AUDIT.md`; `rg` inventory of controllers, matchers, STOMP destinations, migrations, and deployment files. |
| 2–5 | COMPLETE / 2026-09-27 | SA-001–SA-006 | `ApiSecurityTest`, `MultiLoginTest`, `PasswordResetServiceTest`, and `RlsPostgresIT` in the focused and full PostgreSQL commands above. |
| 6–11 | COMPLETE / 2026-09-27 | SA-001, SA-003, SA-004, SA-006, SA-008 (code) | Full `rls-it` verification; migration/contract drift tests; Playwright renderer/CSRF/order scenarios; `python3 scripts/check-template-scripts.py`; JavaScript and SQL validation. |
| 12–14 | COMPLETE / 2026-09-27 | SA-002 | `WebSocketConfigTest`, `ApiSecurityTest`, browser session tests, and one low-impact production CORS/WebSocket check per case. |
| 15 | BLOCKED | SA-008 fixed in code | Local header and browser checks pass. A separately approved deployment and live CSP/Referrer/Permissions verification remain required. |
| 16 | BLOCKED | SA-013 fixed in code | Local configuration review and live TLS/Host checks completed. Actual proxy rollout plus `nginx -t` remains required. |
| 17 | BLOCKED | SA-009 fixed in code | Trivy configuration/image review and Compose/Dockerfile validation completed. Exact built production image/container inspection remains required. |
| 18 | BLOCKED | Current-source cleanup complete | `gitleaks dir` over 381 tracked/non-ignored files found zero current leaks; history scan found SA-011 and SA-012. Provider rotation/rejection evidence remains required. |
| 19–24 | COMPLETE / 2026-09-27 | SA-001, SA-006, SA-007, SA-014 | OSV and npm: zero vulnerabilities; Trivy: zero source/dependency vulnerabilities; full PostgreSQL suite and focused malformed/error/rate-limit/DTO tests pass. |
| 25 | COMPLETE / 2026-09-28 | SA-007 | Full Maven/Playwright/static suites pass. Semgrep’s 19 alerts were manually classified as Thymeleaf/parser or context-aware proxy/escaping false positives. Trivy’s remaining Certbot-root rule is constrained by dropped capabilities, `no-new-privileges`, no listener, and isolated certificate volumes. |

See root `SECURITY_AUDIT.md` for sanitized findings, attack chains, production evidence, commands, and blockers.

## Main objective

Try to determine how an attacker could:

- access another customer's data
- access admin functionality
- escalate privileges
- bypass authentication
- bypass authorization
- manipulate orders or prices
- impersonate another user
- steal sessions
- reset another user's password
- exploit WebSockets
- exploit APIs directly instead of through the UI
- manipulate IDs or object references
- extract information from PostgreSQL
- exploit incorrect RLS policies
- inject SQL
- inject HTML or JavaScript
- abuse redirects
- exploit CORS
- exploit CSRF weaknesses
- abuse account registration
- enumerate users
- brute-force authentication
- abuse password-reset functionality
- exploit race conditions
- exploit business logic
- leak secrets
- access internal services
- exploit Docker or Nginx configuration
- access endpoints that should not be public
- obtain sensitive information from logs, errors, stack traces, Actuator endpoints, source maps, configuration, backups, or environment variables

Think like an attacker and trace complete attack paths rather than checking isolated files.

## Phase 1: Map the application

First understand the complete architecture before changing anything.

Inspect:

- controllers
- REST APIs
- services
- repositories
- entities
- DTOs
- security configuration
- Spring Security filters
- authentication providers
- sessions
- cookies
- JWT code, if present
- password hashing
- password reset flow
- registration flow
- authorization checks
- role handling
- SUPER_ADMIN/admin functionality
- WebSocket configuration
- STOMP subscriptions
- Thymeleaf templates
- JavaScript
- forms
- database migrations
- PostgreSQL roles
- PostgreSQL grants
- RLS configuration
- Dockerfiles
- docker-compose files
- Nginx configuration
- environment variable handling
- CI/CD workflows
- GitHub Actions
- logging
- exception handling
- external APIs
- Resend/email integration
- geolocation functionality
- delivery-distance functionality
- any file upload/download functionality
- administrative tools or database-management interfaces

Build a trust-boundary map showing:

Browser -> Nginx -> Spring Boot -> PostgreSQL -> external services

Also identify any additional components you discover.

Create an inventory of every externally reachable endpoint and categorize it as:

PUBLIC
AUTHENTICATED CUSTOMER
STAFF
ADMIN
SUPER_ADMIN
INTERNAL

Do not assume an endpoint is protected just because the UI does not link to it.

## Phase 2: Authentication audit

Deeply inspect authentication.

Check for:

- authentication bypass
- weak password requirements
- plaintext or reversibly encrypted passwords
- insecure password hashing
- BCrypt strength
- user enumeration
- timing differences
- login brute-force protection
- credential stuffing protection
- session fixation
- session hijacking
- session invalidation after logout
- concurrent session behavior
- remember-me vulnerabilities
- login CSRF
- authentication state confusion
- insecure redirects after authentication
- email/phone login ambiguity
- case-sensitivity problems
- duplicate accounts
- malformed identity values

Verify that changing HTTP parameters, cookies, headers, paths, IDs, or request bodies cannot cause authentication as another account.

## Phase 3: Password reset audit

Treat password reset as a critical security boundary.

Inspect the entire flow from:

forgot-password request
-> token creation
-> storage
-> email generation
-> reset link
-> token validation
-> password update
-> token invalidation

Check:

- cryptographically secure token generation
- token entropy
- expiration
- single-use enforcement
- token hashing in the database
- token replay
- account enumeration
- reset flooding
- host-header poisoning
- reset-link manipulation
- incorrect domain construction
- old token validity after requesting a new one
- token validity after password change
- session invalidation after reset
- ability to reset another account
- leakage through logs
- leakage through URLs/referrers
- Resend API key exposure

Never send real reset emails during automated testing unless absolutely necessary and explicitly configured for a test account.

## Phase 4: Authorization and IDOR

This is one of the highest priorities.

Search every endpoint that accepts identifiers such as:

userId
customerId
orderId
addressId
branchId
staffId
accountId
UUIDs
numeric IDs

Determine whether changing an identifier allows access to another user's object.

Verify authorization at the backend, not merely in templates or JavaScript.

Look for patterns such as:

repository.findById(id)

where ownership is not subsequently verified.

Attempt to trace possible:

horizontal privilege escalation
customer -> customer

vertical privilege escalation
customer -> staff/admin/SUPER_ADMIN

Check GET, POST, PUT, PATCH, DELETE, WebSocket messages, form submissions, and hidden endpoints.

## Phase 5: Role and privilege escalation

Audit every use of:

ROLE_USER
USER
ADMIN
STAFF
SUPER_ADMIN
access_level
roles
authorities

Find inconsistencies between these concepts.

Verify that roles cannot be supplied or changed through:

registration DTOs
profile updates
JSON properties
form fields
query parameters
mass assignment
database defaults
cookies
headers
WebSocket messages

Search specifically for situations where the client can influence authorization-related fields.

Confirm new accounts receive only the minimum customer permissions.

## Phase 6: PostgreSQL and RLS audit

Perform a deep PostgreSQL security review.

RLS should primarily protect customer/private data. Public menu/catalog data does not need unnecessary RLS unless there is a clear security reason.

Identify tables containing:

customer details
addresses
credentials
password reset tokens
orders
order history
payment-related information
sessions
staff information
private account information

Review:

- ENABLE ROW LEVEL SECURITY
- FORCE ROW LEVEL SECURITY
- policies
- USING expressions
- WITH CHECK expressions
- database roles
- table owners
- migration user
- runtime user
- grants
- schema privileges
- function privileges
- SECURITY DEFINER functions
- search_path
- bypassrls
- superuser permissions

Pay special attention to helper functions such as:

app_security.actor_role()
app_security.actor_id()

Determine whether application-controlled session/database variables used by RLS can be forged.

Verify that the Spring Boot runtime database account cannot:

- bypass RLS
- become a superuser
- alter policies
- change ownership
- arbitrarily SET security-sensitive variables
- read tables it should not be able to read directly

Try to identify paths where a SQL injection vulnerability combined with incorrectly designed RLS could expose customer data.

Review all migrations for privilege regressions.

## Phase 7: SQL injection

Trace every path where untrusted input reaches PostgreSQL.

Search for:

- native queries
- EntityManager
- JdbcTemplate
- createNativeQuery
- Statement
- string-built SQL
- dynamic ORDER BY
- dynamic WHERE clauses
- dynamic table/column names

Do not simply search for obvious SQL strings.

Trace user-controlled values through services and helper methods.

For confirmed risks, demonstrate using harmless local test cases only.

Never DROP, DELETE, truncate, corrupt, or modify production data to prove a vulnerability.

## Phase 8: Business logic attacks

The application is an online food-ordering system, so test security beyond ordinary OWASP issues.

Determine whether a customer can manipulate:

- product prices
- quantities
- totals
- delivery fees
- discounts
- specials
- extras
- pizza sizes
- burger/combo selections
- branch selection
- delivery distance
- delivery eligibility
- tax
- order status
- customer ownership
- server-calculated values

The browser must never be trusted to calculate authoritative prices.

Confirm the server independently calculates:

item price
extras
special pricing
delivery charge
order total

Attempt scenarios such as:

- negative quantity
- zero quantity
- extremely large quantity
- negative prices
- decimal manipulation
- duplicate parameters
- unknown menu IDs
- disabled products
- archived products
- branch mismatch
- changing branch after obtaining a quote
- changing product ID after obtaining a quote
- stale prices
- replayed requests
- duplicate order submission
- simultaneous order submission

Look for TOCTOU and race-condition vulnerabilities.

## Phase 9: API security

Apply OWASP API Security Top 10 principles.

Inspect every API for:

- BOLA / IDOR
- broken authentication
- broken object property authorization
- unrestricted resource consumption
- broken function-level authorization
- unrestricted access to sensitive business flows
- SSRF
- security misconfiguration
- improper inventory management
- unsafe API consumption

Attempt unexpected HTTP methods where safe.

Example:

GET endpoint also accepting POST
PUT endpoint accepting unauthenticated requests
DELETE accessible to ordinary users

Verify correct status codes and no sensitive information leakage.

## Phase 10: CSRF

Determine which authentication model the application uses.

If cookies/sessions are used, inspect CSRF protection carefully.

Check every state-changing endpoint:

POST
PUT
PATCH
DELETE

Verify CSRF cannot be bypassed by:

- alternate content types
- multipart forms
- JSON endpoints
- missing Spring Security matchers
- method overrides
- WebSockets
- CORS configuration

Do not disable CSRF globally merely to fix configuration problems.

## Phase 11: XSS and HTML injection

Inspect all user-controlled content rendered in:

Thymeleaf
HTML
JavaScript
attributes
URLs
admin pages

Look for:

th:utext
innerHTML
document.write
unsafe JavaScript interpolation
unescaped template output

Test persistent, reflected, and DOM-based XSS locally.

Pay special attention to customer names, addresses, order notes, profile information, and anything eventually displayed to staff/admin users.

Stored XSS against an administrator should be considered high or critical depending on impact.

## Phase 12: WebSocket security

Audit the complete WebSocket/STOMP system.

Determine whether authentication from the HTTP application is correctly propagated to WebSocket connections.

Inspect:

WebSocketConfig
STOMP endpoints
SUBSCRIBE authorization
SEND authorization
topic destinations
user destinations
AccountAccessReader
AccountWebSocketSessions

Pay special attention to destinations such as admin order topics and branch-specific admin topics.

Try to determine whether a normal customer can:

- subscribe to admin topics
- receive another customer's updates
- send messages to privileged destinations
- spoof account IDs
- spoof branch IDs
- reconnect using a stale session
- bypass HTTP authorization through WebSockets

Authorization must occur server-side for both subscriptions and messages.

## Phase 13: Session and cookie security

Inspect production cookie configuration.

Verify:

Secure
HttpOnly
SameSite

Verify session IDs rotate after authentication.

Determine whether sensitive cookies are scoped too broadly.

Check logout behavior.

Look for session IDs in:

URLs
logs
HTML
JavaScript
error messages

Also verify HTTPS is consistently enforced.

## Phase 14: CORS

Review all CORS configuration.

Look for:

*
credentials + wildcard combinations
reflected Origin values
overly broad allowed origins
unnecessary methods
unnecessary headers

Test whether an attacker-controlled website could read authenticated API responses.

## Phase 15: HTTP security headers

Inspect the live production responses from:

https://crowdcam.co.za

Check:

Content-Security-Policy
Strict-Transport-Security
X-Content-Type-Options
Referrer-Policy
Permissions-Policy
frame-ancestors
X-Frame-Options where applicable
Cache-Control on sensitive pages

Develop a strong CSP compatible with the application rather than blindly setting values that break functionality.

Pay attention to external Google Maps resources if they are used.

## Phase 16: Nginx and TLS

Review the complete Nginx configuration.

Check:

- HTTP -> HTTPS redirect
- TLS versions
- weak protocols/ciphers
- proxy headers
- Host handling
- X-Forwarded-* handling
- request size limits
- hidden files
- backup files
- dotfiles
- directory traversal
- accidental static exposure
- server version leakage
- internal services exposed publicly
- WebSocket proxy configuration

Verify Spring Boot correctly understands whether the original connection was HTTPS.

Look for host-header attacks affecting generated links.

## Phase 17: Docker security

Review:

Dockerfile
docker-compose.yml
volumes
networks
ports
environment variables
container users
capabilities
mounts

Look for:

- containers running unnecessarily as root
- Docker socket exposure
- privileged containers
- unnecessary host ports
- database exposed publicly
- NocoDB/admin tools exposed publicly
- secrets embedded in images
- .env copied into images
- writable sensitive mounts
- unnecessary Linux capabilities
- containers sharing excessive networks
- old/vulnerable base images

The PostgreSQL container should not need to be internet-accessible.

## Phase 18: Secrets audit

Search the entire repository including git history where practical for:

API keys
database passwords
JWT secrets
session secrets
Resend keys
Google API keys
private keys
SSH keys
tokens
credentials
connection strings

Check:

.env
application.properties
application.yml
Docker
GitHub Actions
shell scripts
tests
documentation
old commits

Use secret-scanning tooling where available.

Do NOT print full discovered secrets in the report.

Mask them like:

re_abc...xyz

If a real secret appears committed, mark it as requiring rotation even if removed from the current branch.

## Phase 19: Dependency security

Review Maven dependencies and container images for known vulnerabilities.

Use appropriate tools where available, such as:

OWASP Dependency-Check
OSV Scanner
Trivy
Semgrep

Do not blindly upgrade major versions.

For every relevant vulnerability determine whether the vulnerable functionality is actually reachable in this application.

Also inspect Maven plugins and transitive dependencies.

## Phase 20: Spring Boot specific review

Check for:

Spring Boot Actuator exposure
/error information leakage
Whitelabel stack traces
debug mode
development profile enabled in production
unsafe management endpoints
Spring Security misconfiguration
request matcher mistakes
permitAll mistakes
anonymous access
method security
@PreAuthorize coverage
@PostAuthorize where relevant
proxy/security annotation mistakes

Search for endpoints that rely solely on UI hiding rather than authorization.

## Phase 21: Mass assignment / DTO problems

Do not allow JPA entities to be blindly populated directly from attacker-controlled request bodies.

Look for dangerous bindable properties such as:

id
userId
customerId
role
accessLevel
admin
enabled
verified
price
total
orderStatus
createdBy
ownerId

Use dedicated DTOs and explicit mapping where appropriate.

## Phase 22: Privacy and sensitive customer data

Determine exactly what personally identifiable information is stored.

Check whether unnecessary information is:

logged
returned by APIs
embedded into HTML
cached
included in exception messages
available to unrelated customers
accessible to staff who do not require it

Review database queries for accidental over-fetching and serialization of complete entities.

## Phase 23: Error handling

Attempt malformed requests locally.

Look for leakage of:

stack traces
SQL
table names
database errors
filesystem paths
Java package names
internal IP addresses
environment configuration
secrets

Production should return useful but non-sensitive errors.

## Phase 24: Rate limiting and abuse protection

Identify high-risk endpoints such as:

login
registration
forgot password
password reset
address/geolocation
order submission
quote endpoints
email-triggering endpoints

Determine whether rate limiting is needed at Nginx, application, or both.

Ensure controls do not rely solely on client-side JavaScript.

## Phase 25: Automated security tooling

Use appropriate security tools if available, but do not treat scanner output as automatically true.

Consider:

Semgrep
Trivy
OSV Scanner
OWASP Dependency-Check
Gitleaks
git grep
Maven dependency analysis

Validate findings manually.

Do not add massive security tooling dependencies to the application merely to perform the audit.

## Production testing rules

You may inspect the production website at:

https://crowdcam.co.za

But production tests MUST be non-destructive.

Do NOT:

- delete data
- modify real customer data
- brute force accounts
- perform denial-of-service testing
- flood endpoints
- send large amounts of email
- send SMS
- intentionally corrupt orders
- attack Google, Resend, Azure, or any other third-party provider
- exploit infrastructure belonging to anyone else

When a vulnerability can be proven safely against a local instance, do that instead.

Use the smallest harmless proof necessary.

## Severity system

Classify confirmed findings as:

CRITICAL
HIGH
MEDIUM
LOW
INFORMATIONAL

For each finding include:

### Finding
Clear vulnerability name.

### Severity
CRITICAL / HIGH / MEDIUM / LOW / INFORMATIONAL

### CWE
Include the relevant CWE where appropriate.

### OWASP category
Map it to OWASP Top 10 / API Top 10 where appropriate.

### Location
Exact file, class, method, endpoint, SQL migration, or configuration.

Include line numbers when practical.

### Attack scenario
Explain exactly how an attacker could exploit it.

### Impact
Explain what the attacker gains.

### Evidence
Show the relevant code or safe test evidence.

Never include actual secrets.

### Remediation
Explain the correct fix.

### Verification
Explain how to test that the vulnerability is fixed.

## Avoid false positives

Do not report theoretical vulnerabilities without investigating whether they are actually reachable.

For every serious issue, trace:

attacker-controlled input
-> application path
-> security control
-> sensitive operation/data
-> impact

Clearly differentiate:

CONFIRMED
LIKELY
NEEDS MANUAL VERIFICATION
DEFENSE IN DEPTH

Do not inflate severity.

## Create an attack-path analysis

In addition to individual findings, look for vulnerabilities that become serious when chained together.

For example:

user enumeration
-> password reset weakness
-> account takeover

or:

IDOR
-> customer information disclosure
-> admin stored XSS
-> admin account compromise

or:

SQL injection
-> manipulation of RLS context
-> cross-customer database access

Show realistic chains you discover.

## Fixing vulnerabilities

After the audit:

1. Fix confirmed CRITICAL vulnerabilities.
2. Fix confirmed HIGH vulnerabilities.
3. Fix straightforward MEDIUM vulnerabilities where the change is low-risk.
4. Do not perform broad architectural rewrites unless necessary.
5. Preserve existing functionality.
6. Add regression tests for every security fix.
7. Run the full test suite afterward.
8. Do not weaken security controls just to make tests pass.

Before changing something security-sensitive, understand why it exists.

Do NOT solve security problems by doing things such as:

- disabling CSRF
- disabling RLS
- making endpoints public
- granting the database runtime user broad privileges
- allowing CORS from *
- hardcoding secrets
- reducing password security
- bypassing authorization checks

## Required final deliverables

Create:

SECURITY_AUDIT.md

with these sections:

# Executive Summary

# Architecture and Trust Boundaries

# Attack Surface

# Critical Findings

# High Findings

# Medium Findings

# Low Findings

# Informational Findings

# Attack Chains

# Authentication Review

# Authorization / IDOR Review

# PostgreSQL / RLS Review

# WebSocket Review

# Business Logic Review

# Infrastructure Review

# Dependency Review

# Secrets Review

# Production HTTP/TLS Review

# Fixes Implemented

# Remaining Recommendations

# Security Test Coverage

# Commands Used

# Final Risk Summary

Also create or update automated security regression tests where appropriate.

## Final response to me

When finished, give me a concise summary containing:

- number of CRITICAL findings
- number of HIGH findings
- number of MEDIUM findings
- number of LOW findings
- the five most important concrete issues
- which vulnerabilities you fixed
- which vulnerabilities remain
- files changed
- tests added
- test results
- anything that requires immediate manual action, especially secret rotation or infrastructure changes

Do not just run scanners and dump their output.

Read the application, understand the architecture, trace authorization and data flows, actively challenge assumptions, and approach this as a real security assessment of an internet-facing production application.
