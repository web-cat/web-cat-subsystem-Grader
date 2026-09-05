# Requirements: PassPort Protocol v1 Extension API

## Functional Requirements

### 1. Data Model (`PassportClient`)
- **REQ-DM-1**: Create `PassportClient` EO entity in `Grader.eomodeld` with:
  - `id` (primary key)
  - `clientId` (string, unique, indexed, used for `X-PassPort-Client-ID`)
  - `clientSecret` (string, shared secret for HMAC-SHA256 signing)
  - `name` (string, client display name)
  - `brokerBaseUrl` (string, base URL of the broker)
  - `domainPattern` (string, pattern/domain/IP whitelist for screening requests from this client)
  - `requestedProperties` (string, comma-separated list of properties negotiated during registration)
  - `created`, `lastModified` (NSTimestamps)
- **REQ-DM-2**: Provide database migration in `GraderDatabaseUpdates` (`updateIncrement41()`) to create table `PassportClient` with appropriate indices.

### 2. Authentication & Request Verification
- **REQ-AUTH-1**: Require `X-PassPort-Client-ID`, `X-PassPort-Signature`, and `X-PassPort-Timestamp` on all extension and rollback calls.
- **REQ-AUTH-2**: Lookup `PassportClient` by `clientId`. Return `401 Unauthorized` if unknown or inactive.
- **REQ-AUTH-3**: Verify timestamp is within 300 seconds (5 minutes) of current server time. Return `401 Unauthorized` if outside window.
- **REQ-AUTH-4**: Compute HMAC-SHA256 of raw request body using `clientSecret` and compare with `X-PassPort-Signature` using timing-safe comparison (`MessageDigest.isEqual`). Return `401 Unauthorized` on mismatch.
- **REQ-AUTH-5**: If `PassportClient.domainPattern` is specified, verify that the incoming client IP/host matches the allowed pattern.

### 3. Extension Management (POST `/passport/v1/extension`)
- **REQ-EXT-1**: Parse JSON payload conforming to PassPort v1 schema (`request_id`, `context`, `user`, `resource`, `extension`).
- **REQ-EXT-2**: Resolve `AssignmentOffering` using `resource.lti_resource_link_id` or `resource.canvas_assignment_id` matching `AssignmentOffering.lmsAssignmentId`. If contextual course ID is provided, verify offering belongs to course. Return `404 Not Found` if missing.
- **REQ-EXT-3**: Resolve `User` using `user.lti_user_id` (via `LMSIdentity`), `user.email`, or `user.userName`. Return `404 Not Found` if missing.
- **REQ-EXT-4**: Check if student currently has an active deadline later than `extension.new_due_date`. Return `409 Conflict` if so.
- **REQ-EXT-5**: Create or update `StudentExtension` with `dueDate = new_due_date`. Ensure `AssignmentOffering` availability bounds (`minOpensOn`, `maxClosesOn`) update automatically. Return `200 OK`.

### 4. Failure Recovery / Rollback (DELETE `/passport/v1/extension`)
- **REQ-ROLL-1**: Authenticate DELETE request using HMAC-SHA256 signature protocol.
- **REQ-ROLL-2**: Identify extension using `request_id` (from memory registry) or user/resource payload.
- **REQ-ROLL-3**: Revert extension: delete newly created extension or restore prior due date. Return `200 OK` on success, or `404 Not Found` if not found.

### 5. Dynamic Registration (2-Phase Handshake)
- **REQ-REG-1**: Endpoint `/passport/v1/register` (or `/wa/passport/register`) receives POST with `broker_base_url`, `callback_url`, `name`, `passport_version`.
- **REQ-REG-2**: Validate:
  - `broker_base_url` matches global domain whitelist (e.g. `*.vt.edu`).
  - Both URLs use HTTPS (permitting HTTP for localhost in development).
  - `callback_url` scheme, host, and port match `broker_base_url`.
- **REQ-REG-3**: Return `202 Accepted` immediately without exposing credentials.
- **REQ-REG-4**: Asynchronously generate `PassportClient` record and POST credentials to `callback_url` with `tool_name`, `passport_version`, `endpoints`, `requested_properties`, and `credentials` (`client_id`, `client_secret`).
