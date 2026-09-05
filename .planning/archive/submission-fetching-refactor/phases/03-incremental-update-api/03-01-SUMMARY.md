---
phase: 03-incremental-update-api
plan: 01
subsystem: Incremental Fetching
tags: [API, Performance]
requirements: [API-02, LOGIC-01]
tech-stack: [Java, EOF, ERXQ]
key-files: [src/org/webcat/grader/Submission.java]
metrics:
  duration: 0h 15m
  completed_date: "2026-03-26T12:45:00Z"
decisions:
  - Capture query start time before the fetch to avoid race conditions (submissions made during query execution).
  - Use ERXQ.and for combining assignment offering filters with timestamp-based filters.
---

# Phase 03 Plan 01: Implement incremental fetching logic Summary

Implemented incremental fetching logic in `Submission.submissionsForGrading(SubmissionGradingState state)` to improve performance of subsequent calls by only fetching new submissions.

## Substantive Changes
- Captured `queryStart` timestamp at the very beginning of the method.
- Modified the fetch qualifier to include `submitTime.greaterThan(state.lastFetchTimestamp())` if the state contains a previous fetch timestamp.
- Updated the state's `lastFetchTimestamp` with `queryStart` at the end of the method to ensure that all submissions made after the query start are picked up in the next call.
- Preserved in-memory user filtering logic to handle complex filtering requirements across multiple assignment offerings.

## Deviations from Plan
- None - plan executed exactly as written.

## Self-Check: PASSED
- [x] `submissionsForGrading` implements timestamp-based incremental fetching.
- [x] `state.lastFetchTimestamp` is updated to `queryStart`.
- [x] All changes are committed.

## Commits
- 444c37d: feat(03-01): capture queryStart and build incremental qualifier
- 1ec5580: feat(03-01): update lastFetchTimestamp with queryStart
