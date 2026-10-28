# Project State: PassPort Submission Status Query API and Direct Action Handlers

## Project Reference
- **Core Value**: Enables external late pass brokers to query student submission status and scores with signed REST requests, providing secure direct action URLs for students to view feedback and staff to grade submissions.
- **Status**: COMPLETED

## Current Position

### Phases
- **Phase 1 (submissionResult Direct Action Class)**: [████████████████████] 100%
- **Phase 2 (passport Status Query Endpoint)**: [████████████████████] 100%
- **Phase 3 (Verification & Walkthrough)**: [████████████████████] 100%

## Accumulated Context
- **Decisions**:
  - Implemented `org.webcat.grader.actions.submission` extending `WCDirectAction` with `viewAction()` (default), `editAction()`, and `defaultAction()`.
  - Switched direct action handler URLs and parameter lookup to use `Submission` ID directly (`sub.id()`), with fallback support for `SubmissionResult` ID.
  - Session verification enforces that the user must be authenticated with an active session (`primeUser() != null`), redirecting to login via `Application.wcApplication().gotoLoginPage(context())` if unauthenticated.
  - Access control is strictly enforced: `view` allows admin, course staff (`isStaff()`), and submission partners/owner; `edit` allows admin and course staff only (students forbidden with HTTP 403 Forbidden).
  - Added `submissionStatusAction()` / `statusAction()` to `org.webcat.grader.actions.passport` supporting `/passport/v1/status` and `/submissionStatus`.
  - Submission status query performs sessionless execution (`forgetSession()`), timing-safe HMAC-SHA256 signature verification via `PassPortRequest`, entity resolution of `User` and `AssignmentOffering`, determination of `submission_for_grading` and `most_recent_submission`, and output formatting for timestamps, scores, and direct action URLs (`wa/submission/view?id=...` and `wa/submission/edit?id=...`).
- **Blockers**: None.
