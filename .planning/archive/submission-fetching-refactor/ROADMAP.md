# Roadmap: Submission Fetching Refactor (Completed)

## Phases

- [x] **Phase 1: Foundation** - Implement core data structures `StudentSubmissionInfo` and `SubmissionGradingState`.
- [x] **Phase 2: Initial Batch Fetching** - Implement `submissionsForGrading` initial fetch API and merging logic.
- [x] **Phase 3: Incremental Update API** - Implement timestamp-based incremental queries and "dirty" object re-evaluation.
- [x] **Phase 4: UI Compatibility Layer** - Implement `UserSubmissionPair.fromInfoMap` for legacy UI support.
- [x] **Phase 5: Assignment Page Refactor** - Update `StudentsForAssignmentPage.java` to use batch fetching.
- [x] **Phase 6: Summary and Export Refactor** - Update `StudentCourseSummaryPage.java` and `DownloadScoresDialog.java` to use batch fetching.

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
**Requirements**: API-01, LOGIC-02
**Success Criteria**:
  1. Initial batch fetch query returns all expected submissions in one (or small number of) database round-trips.
  2. `gradedSubmission` is correctly identified for each student based on the current selection logic.
**Plans**:
- [x] 02-01-PLAN.md — Implement initial batch-fetching API and logic

### Phase 3: Incremental Update API
**Goal**: Developers can efficiently refresh the submission state with only new data.
**Depends on**: Phase 2
**Requirements**: API-02, LOGIC-01
**Success Criteria**:
  1. Incremental fetch only retrieves submissions newer than the last fetch timestamp.
  2. `state.lastFetchTimestamp` is correctly updated to query start time to avoid race conditions.
**Plans**:
- [x] 03-01-PLAN.md — Implement incremental fetch logic and state timestamp updates

### Phase 4: UI Compatibility Layer
**Goal**: Existing UI components can interact with the new data structures.
**Depends on**: Phase 3
**Requirements**: COMPAT-01
**Success Criteria**:
  1. `UserSubmissionPair.fromInfoMap` correctly bridges the new `Map` structure back to the existing legacy type.
**Plans**:
- [x] 04-ui-compatibility/04-01-PLAN.md — Implement fromInfoMap helper methods in UserSubmissionPair.java

### Phase 5: Assignment Page Refactor
**Goal**: The main assignments page loads and refreshes with minimal database queries.
**Depends on**: Phase 4
**Requirements**: UI-01
**Success Criteria**:
  1. `StudentsForAssignmentPage` performs a single batch fetch for all displayed offerings.
  2. Stats on the page are accumulated in a single pass over the results.
**Plans**:
- [x] 05-assignment-page-refactor/05-01-PLAN.md — Refactor StudentsForAssignmentPage to use batch fetching

### Phase 6: Summary and Export Refactor
**Goal**: Student summary and score exports benefit from batch fetching.
**Depends on**: Phase 4
**Requirements**: UI-02, UI-03
**Success Criteria**:
  1. `StudentCourseSummaryPage` uses a student-specific batch fetch across all offerings.
  2. `DownloadScoresDialog` uses a single batch fetch for all students/offerings instead of individual queries.
**Plans**:
- [x] 06-summary-and-export-refactor/06-01-PLAN.md — Refactor StudentCourseSummaryPage and DownloadScoresDialog to use batch fetching

## Progress Table

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 1/1 | Completed | 2024-05-22 |
| 2. Initial Batch Fetching | 1/1 | Completed | 2024-05-23 |
| 3. Incremental Update API | 1/1 | Completed | 2026-03-26 |
| 4. UI Compatibility Layer | 1/1 | Completed | 2026-09-05 |
| 5. Assignment Page Refactor | 1/1 | Completed | 2026-09-05 |
| 6. Summary and Export Refactor | 1/1 | Completed | 2026-09-05 |
