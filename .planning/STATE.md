# Project State: PassPort Protocol v1 Extension API

## Project Reference
- **Core Value**: Sessionless, HMAC-SHA256 signed REST API implementing the PassPort Protocol v1 specification for extension management and rollback.
- **Status**: COMPLETED
 
## Current Position
 
### Phases
- **Phase 1 (Data Model: PassportClient)**: [████████████████████] 100%
- **Phase 2 (Core Verification & Security Layer)**: [████████████████████] 100%
- **Phase 3 (Direct Action Controller)**: [████████████████████] 100%
- **Phase 4 (Extension Lifecycle & Rollback)**: [████████████████████] 100%
- **Phase 5 (Verification & Walkthrough)**: [████████████████████] 100%

## Accumulated Context
- **Decisions**:
  - Introduce dedicated `PassportClient` entity rather than overloading `LMSInstance`, as PassPort API users/brokers are not standard LMS instances and require dedicated key/secret and domain pattern storage for screening requests.
  - Direct Action class named `passport` placed in `org.webcat.grader.actions`, extending `WCDirectAction` and enforcing sessionless processing (`forgetSession()`).
  - Idempotency & Rollback: Maintain an in-memory cache keyed by `request_id` to store prior extension state for safe rollback and deduplicated POST requests.
  - Resolved `AssignmentOffering` matching in `PassPortRequest`: query `AssignmentOffering` directly (since `CourseOffering` has no relationship to `AssignmentOffering`) matching `Assignment.name`/`shortDescription` and course context, filtered for the course offering the target user is associated with (`students().contains(u) || isStaff(u) || hasAdminPrivileges()`), without filtering by `dueDate` or `maxClosesOn`.
- **Blockers**: None.
