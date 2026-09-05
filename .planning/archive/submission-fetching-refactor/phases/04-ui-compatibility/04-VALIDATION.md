# Phase 4 Validation Plan: UI Compatibility

## Requirements Coverage

| Req ID | Requirement | Verification Method | Automated Command |
|--------|-------------|---------------------|-------------------|
| COMPAT-01 | `fromInfoMap` correctly converts Map to NSArray with partner filtering. | Unit Test | `ant test -Dtest=org.webcat.grader.tests.UserSubmissionPairTest` |

## Acceptance Criteria

- [ ] `UserSubmissionPair.fromInfoMap` handles `null` input by returning an empty `NSArray`.
- [ ] `UserSubmissionPair.fromInfoMap` correctly creates `UserSubmissionPair` objects from `StudentSubmissionInfo` maps.
- [ ] `UserSubmissionPair.fromInfoMap` preserves the order defined by the optional `users` list.
- [ ] `UserSubmissionPair.fromInfoMap` filters out partner links when `omitPartners` is `true`.
- [ ] `UserSubmissionPair.fromInfoMap` correctly accumulates statistics in `Submission.CumulativeStats` if provided.
- [ ] All new overloads of `fromInfoMap` are implemented and functional.
- [ ] `UserSubmissionPairTest.java` contains passing tests for all the above cases.

## Manual Verification (Optional/Post-Phase)

- Verify that `StudentsForAssignmentPage` correctly displays the same data as before when switched to the new batch-fetching infrastructure in Phase 5.
