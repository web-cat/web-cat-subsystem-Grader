# Project: PassPort Submission Status Query API and Direct Action Handlers

## Core Value
Extends Web-CAT's PassPort Protocol implementation with:
1. A sessionless, HMAC-SHA256 signed REST endpoint enabling external late pass brokers to query a student's current submission status, grading results, scores, and direct action URLs.
2. A dedicated session-based Direct Action controller (`submissionResult`) with `view` and `edit` actions allowing authenticated students and course staff to directly view feedback and grade submissions securely with appropriate permission enforcement and session preference initialization.

## Success Criteria
- [ ] Implement `org.webcat.grader.actions.submissionResult` extending `WCDirectAction` with:
  - `viewAction()` (default): session check (redirect to login if unauthenticated), parameter `id` extraction, student/partner/staff/admin access permission verification, session preferences initialization, transfer to `FinalReportPage` (`MostRecent` tab).
  - `defaultAction()`: routes to `viewAction()`.
  - `editAction()`: session check (redirect to login if unauthenticated), parameter `id` extraction, staff/instructor/grader/admin access permission verification (students forbidden), session preferences initialization, transfer to `GradeStudentSubmissionPage` (`EnterGrades` tab).
- [ ] Implement `statusAction()` / `submissionStatusAction()` endpoint in `org.webcat.grader.actions.passport`:
  - Sessionless signed request verification (`PassPortRequest`, `X-PassPort-Signature`, `X-PassPort-Client-ID`, `X-PassPort-Timestamp`).
  - Resolve `User` and `AssignmentOffering` using PassPort identifying parameters (`context`, `user`, `resource`).
  - Retrieve the submission for grading and the most recent submission.
  - Return JSON payload containing timestamp, submission number, total score, TA score (null if none entered), student feedback direct URL, and grading page direct URL for the submission for grading.
  - Conditionally include the same attributes for the most recent submission if different from the submission for grading.
- [ ] Route `/passport/v1/status` and variants in `v1Action()` and `performActionNamed()`.

## Constraints
- **Framework**: WebObjects / Enterprise Objects Framework (EOF), Project Wonder (`WCDirectAction`).
- **Package**: `org.webcat.grader.actions`.
- **Java Compatibility**: Java 8.
- **Security**: Robust access control preventing unauthorized students from grading or viewing peer submissions.
