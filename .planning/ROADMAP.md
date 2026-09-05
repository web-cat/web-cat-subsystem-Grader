# Roadmap: PassPort Protocol v1 Extension API

## Phases

### Phase 1: Data Model (`PassportClient`)
- [ ] Define `PassportClient.plist` in `Grader.eomodeld/`.
- [ ] Register `PassportClient` in `Grader.eomodeld/index.eomodeld`.
- [ ] Implement `_PassportClient.java` and `PassportClient.java` in `org.webcat.grader`.
- [ ] Add migration `updateIncrement41()` in `GraderDatabaseUpdates.java`.

### Phase 2: Core Verification & Security Layer
- [ ] Implement `PassPortSecurity`: HMAC-SHA256 computation, timing-safe comparison, timestamp freshness checks (5-minute window).
- [ ] Implement domain pattern matching and IP/host screening against `PassportClient.domainPattern` and global whitelist.
- [ ] Implement `PassPortRequest`: Extract headers, verify signatures against `PassportClient`, parse JSON payload components, resolve `AssignmentOffering` and `User`.

### Phase 3: Direct Action Controller (`org.webcat.grader.actions.passport`)
- [ ] Implement `passport.java` extending `WCDirectAction`.
- [ ] Ensure sessionless execution (`forgetSession()`).
- [ ] Implement dynamic registration Phase 1 (POST `/register`): Whitelist validation, HTTPS check, domain matching, immediate `202 Accepted`.
- [ ] Implement dynamic registration Phase 2 (Async credential delivery): Create `PassportClient`, send POST to `callback_url`.

### Phase 4: Extension Lifecycle & Rollback
- [ ] Implement `extensionAction()`:
  - POST: Validate signature, resolve entities, check 409 Conflict, create/update `StudentExtension`, update aggregate availability bounds, return 200 OK.
  - DELETE: Validate signature, resolve extension via `request_id` or payload, rollback/delete extension, return 200 OK or 404 Not Found.
- [ ] Implement in-memory idempotency & rollback registry.

### Phase 5: Verification & Walkthrough
- [ ] Code review & static audit against `extension-api.md`.
- [ ] Prepare comprehensive `walkthrough.md`.
