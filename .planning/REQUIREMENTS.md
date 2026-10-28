# Requirements: PassPort Submission Status Query API and Direct Action Handlers

## Functional Requirements

### 1. Direct Action Controller (`submissionResult`)
- **REQ-DA-1**: Create `submissionResult.java` in `org.webcat.grader.actions` extending `WCDirectAction`.
- **REQ-DA-2**: `viewAction()`:
  - Check if user has an active authenticated session (`session.primeUser() != null`). If not, redirect to login page via `Application.wcApplication().gotoLoginPage(context())`.
  - Extract parameter `id` (or `sid`), resolve `SubmissionResult` and `Submission`.
  - Double-check view permission: allow if user is admin, course staff, or one of the submitters/partners on the submission. If not, return HTTP 403 Forbidden.
  - Setup session preferences (`prefs().setSubmissionRelationship(sub)`, `prefs().setAssignmentOfferingRelationship(ao)`, core selections).
  - Select tab `MostRecent` and return `FinalReportPage` initialized with submission and result.
- **REQ-DA-3**: `defaultAction()`:
  - Dispatches directly to `viewAction()`.
- **REQ-DA-4**: `editAction()`:
  - Check if user has an active authenticated session. If not, redirect to login page.
  - Extract parameter `id`, resolve `SubmissionResult` and `Submission`.
  - Double-check edit/grade permission: allow only if user is admin or course staff (`co.isStaff(user)`). Forbid students (return HTTP 403 Forbidden).
  - Setup session preferences and select tab `EnterGrades`.
  - Transfer to `GradeStudentSubmissionPage` initialized with submission and result.

### 2. Status Query Endpoint in `passport.java`
- **REQ-EP-1**: Add `submissionStatusAction()` / `statusAction()` to `passport.java`.
- **REQ-EP-2**: Validate incoming request using `PassPortRequest`:
  - Require valid HMAC-SHA256 signature, client credentials, timestamp freshness (< 5 mins). Return HTTP 401 Unauthorized if invalid.
- **REQ-EP-3**: Resolve `User` and `AssignmentOffering` via standard PassPort identifying parameters (`context`, `user`, `resource`). Return HTTP 404 Not Found if either entity is unresolved.
- **REQ-EP-4**: Determine the submission for grading:
  - Query `Submission.submissionsForGrading(ec, ao, student)`. Fall back to checking `isSubmissionForGrading()` or best grading choice among all student submissions for the offering.
- **REQ-EP-5**: Determine the most recent submission:
  - Query `Submission.submissionsForAssignmentOfferingAndUserDescending(ec, ao, student)` and take the first object.
- **REQ-EP-6**: Construct JSON response:
  - For `submission_for_grading`:
    - `timestamp`: formatted ISO-8601 UTC timestamp of submission.
    - `submission_number`: integer submit number (`submitNumber()`).
    - `total_score`: final score (`finalScore()`) or null if no result.
    - `ta_score`: TA score (`taScoreRaw()`), explicit null if none entered.
    - `direct url for student view of feedback`: full URL to `submissionResult/view?id=...`.
    - `direct url to view grading page`: full URL to `submissionResult/edit?id=...`.
  - For `most_recent_submission`:
    - If present and distinct from `submission_for_grading`, include the same 6 fields.
    - If absent or identical to `submission_for_grading`, omit `most_recent_submission`.
- **REQ-EP-7**: Route `/passport/v1/status` and variants in `v1Action()` and `performActionNamed()`.
