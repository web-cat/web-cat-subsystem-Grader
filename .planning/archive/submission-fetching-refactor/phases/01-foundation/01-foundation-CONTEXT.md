# Context: Phase 1 (Foundation)

## Objective
Implement core data structures `StudentSubmissionInfo` and `SubmissionGradingState` in `Submission.java`.

## User Instructions
1. Add `import java.util.Map;` to `src/org/webcat/grader/Submission.java`.
2. Implement `public static class StudentSubmissionInfo` as a static inner class in `Submission.java` (near `CumulativeStats`). It should track `User`, `AssignmentOffering`, `gradedSubmission`, and `allSubmissions` (NSArray<Submission>).
3. Implement `public static class SubmissionGradingState` as a static inner class in `Submission.java`, subclassing `NSMutableDictionary<AssignmentOffering, Map<User, StudentSubmissionInfo>>`.
4. Add fields to `SubmissionGradingState` for `lastFetchTimestamp` (NSTimestamp), `offerings` (NSArray<AssignmentOffering>), and `users` (NSArray<User>).
5. Ensure `SubmissionGradingState` follows KVC patterns (public getters) for compatibility.
6. Verification plan: Compile and verify the new classes are accessible via standard Java and WebObjects KVC.

## Locked Decisions
- `SubmissionGradingState` must subclass `NSMutableDictionary` for KVC compatibility.
- Classes must be static inner classes in `Submission.java`.
