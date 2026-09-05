# Project: PassPort Protocol v1 Extension API

## Core Value
Provides a sessionless, HMAC-SHA256 signed REST API in Web-CAT (`Grader` subsystem) that implements the PassPort Protocol v1 specification. Enables external late pass brokers (e.g. Canvas Extension Manager) to dynamically register, securely communicate student deadline extensions, and perform failure-recovery rollbacks directly into Web-CAT's `StudentExtension` availability model.

## Success Criteria
- [ ] Dedicated `PassportClient` data model entity storing `clientId`, `clientSecret`, `brokerBaseUrl`, and `domainPattern` for screening requests.
- [ ] Sessionless `WCDirectAction` endpoint in `org.webcat.grader.actions.passport` handling `/passport/v1/extension` and `/passport/v1/register`.
- [ ] Two-phase dynamic registration handshake: Phase 1 validation (domain whitelist, HTTPS, matching callback URL) with immediate 202 Accepted, followed by asynchronous Phase 2 credential delivery.
- [ ] Robust HMAC-SHA256 signature verification (`X-PassPort-Signature`, `X-PassPort-Client-ID`, `X-PassPort-Timestamp`) with replay attack prevention (5-minute window).
- [ ] Extension application to `StudentExtension` and `AssignmentOffering` with 200 OK and 409 Conflict checks (when active deadline is already later).
- [ ] Rollback (DELETE) support using `request_id` idempotency registry.
- [ ] Appropriate HTTP status codes (200, 202, 400, 401, 404, 409) with JSON bodies.

## Constraints
- **Framework**: WebObjects / Enterprise Objects Framework (EOF), Project Wonder (`ERXDirectAction` / `WCDirectAction`).
- **Package**: `org.webcat.grader.actions` for the direct action controller.
- **Java Compatibility**: Java 8 features (`java.time`, `javax.crypto.Mac`, `java.security.MessageDigest`).
- **Sessionless**: Must not create or retain user sessions for REST requests (`forgetSession()`).
- **Database Updates**: `GraderDatabaseUpdates` increment method for `PassportClient` table.
