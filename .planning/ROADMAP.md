# Roadmap: PassPort Submission Status Query API and Direct Action Handlers

## Phases

### Phase 1: Implement `submissionResult` Direct Action Class
- [ ] Implement `org.webcat.grader.actions.submissionResult`:
  - `viewAction()`: Session check (redirect to login), `id` parameter parsing, resolution of `Submission` and `SubmissionResult`, view access control (admin, staff, submitter, partner), preferences setup, `MostRecent` tab selection, transfer to `FinalReportPage`.
  - `defaultAction()`: Route to `viewAction()`.
  - `editAction()`: Session check (redirect to login), `id` parameter parsing, resolution of `Submission` and `SubmissionResult`, grade access control (admin, staff only), preferences setup, `EnterGrades` tab selection, transfer to `GradeStudentSubmissionPage`.
  - Proper HTTP error handling (400 for bad id, 403 for forbidden access, 404 for missing submission).

### Phase 2: Implement Submission Status Endpoint in `passport.java`
- [ ] Add `submissionStatusAction()` / `statusAction()` to `passport.java`:
  - Enforce sessionless execution (`forgetSession()`).
  - Verify HMAC-SHA256 signature and request headers via `PassPortRequest`.
  - Resolve `User` and `AssignmentOffering` from identifying parameters.
  - Query submissions for offering and student.
  - Resolve submission for grading and most recent submission.
  - Build helper to construct direct action URLs (`submissionResult/view?id=...` and `submissionResult/edit?id=...`).
  - Build JSON output formatting fields (`timestamp`, `submission_number`, `total_score`, `ta_score`, `view_url` / `feedback_url`, `edit_url` / `grading_url`).
  - Route status actions in `v1Action()` and `performActionNamed()`.

### Phase 3: Verification & Walkthrough
- [ ] Code review against `extension-api-addition.md` and `extension-api.md`.
- [ ] Prepare comprehensive `walkthrough.md`.
