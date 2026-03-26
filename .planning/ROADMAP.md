# Roadmap: Submission Fetching Refactor

## Phases

- [x] **Phase 1: Foundation** - Implement core data structures `StudentSubmissionInfo` and `SubmissionGradingState`.
- [ ] **Phase 2: Initial Batch Fetching** - Implement `submissionsForGrading` initial fetch API and merging logic.
- [ ] **Phase 3: Incremental Update API** - Implement timestamp-based incremental queries and "dirty" object re-evaluation.
- [ ] **Phase 4: UI Compatibility Layer** - Implement `UserSubmissionPair.fromInfoMap` for legacy UI support.
- [ ] **Phase 5: Assignment Page Refactor** - Update `StudentsForAssignmentPage.java` to use batch fetching.
- [ ] **Phase 6: Summary and Export Refactor** - Update `StudentCourseSummaryPage.java` and `DownloadScoresDialog.java` to use batch fetching.

## Phase Details

### Phase 1: Foundation
**Goal**: Users (developers) have the data structures to store and track student submissions.
**Depends on**: Nothing
**Requirements**: CORE-01, CORE-02
**Success Criteria**:
  1. `StudentSubmissionInfo` exists and correctly tracks `user`, `offering`, and `allSubmissions`.
  2. `SubmissionGradingState` is a subclass of `NSMutableDictionary`.
**Plans**:
- [x] 01-01-PLAN.md — Implement StudentSubmissionInfo and SubmissionGradingState in Submission.java

### Phase 2: Initial Batch Fetching
**Goal**: Developers can fetch all submissions for multiple offerings in a single call.
**Depends on**: Phase 1
**Requirements**: [API-01, LOGIC-02]
**Success Criteria**:
  1. Initial batch fetch query returns all expected submissions in one (or small number of) database round-trips.
  2. `gradedSubmission` is correctly identified for each student based on the current selection logic.
**Plans**:
- [ ] 02-01-PLAN.md — Implement initial batch-fetching API and logic

### Phase 3: Incremental Update API
**Goal**: Developers can efficiently refresh the submission state with only new data.
**Depends on**: Phase 2
**Requirements**: [API-02, LOGIC-01]
**Success Criteria**:
  1. Incremental fetch only retrieves submissions newer than the last fetch timestamp.
  2. Only "dirty" objects are re-evaluated for the best submission.
**Plans**: TBD

### Phase 4: UI Compatibility Layer
**Goal**: Existing UI components can interact with the new data structures.
**Depends on**: Phase 3
**Requirements**: [COMPAT-01]
**Success Criteria**:
  1. `UserSubmissionPair.fromInfoMap` correctly bridges the new `Map` structure back to the existing legacy type.
**Plans**: TBD

### Phase 5: Assignment Page Refactor
**Goal**: The main assignments page loads and refreshes with minimal database queries.
**Depends on**: Phase 4
**Requirements**: [UI-01]
**Success Criteria**:
  1. `StudentsForAssignmentPage` performs a single batch fetch for all displayed offerings.
  2. Stats on the page are accumulated in a single pass over the results.
**Plans**: TBD

### Phase 6: Summary and Export Refactor
**Goal**: Student summary and score exports benefit from batch fetching.
**Depends on**: Phase 4
**Requirements**: [UI-02, UI-03]
**Success Criteria**:
  1. `StudentCourseSummaryPage` uses a student-specific batch fetch across all offerings.
  2. `DownloadScoresDialog` uses a single batch fetch for all students/offerings instead of individual queries.
**Plans**: TBD

## Progress Table

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 1/1 | Completed | 2024-05-22 |
| 2. Initial Batch Fetching | 0/1 | Planning | - |
| 3. Incremental Update API | 0/1 | Not started | - |
| 4. UI Compatibility Layer | 0/1 | Not started | - |
| 5. Assignment Page Refactor | 0/1 | Not started | - |
| 6. Summary and Export Refactor | 0/1 | Not started | - |
