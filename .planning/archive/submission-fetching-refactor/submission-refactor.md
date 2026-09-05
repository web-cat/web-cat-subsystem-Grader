# Submission Fetching Refactor Design (Revised)

## Problem Statement
The current implementation of `Submission.submissionsForGrading()` uses an $O(N)$ query pattern (one query per student in a course offering), leading to severe performance bottlenecks. Furthermore, there is no efficient way to fetch submissions across multiple offerings or to incrementally update an existing set of grading results without re-fetching everything.

## Proposed Design

### 1. Data Structures

#### `StudentSubmissionInfo`
Represents a student's submission state for a single `AssignmentOffering`.
- **`user`**: The student (`User`).
- **`offering`**: The `AssignmentOffering`.
- **`gradedSubmission`**: The "best" submission selected for grading (the one with the primary result).
- **`allSubmissions`**: A `List<Submission>` of all submissions by this student for this offering, sorted by `submitTime`.

#### `SubmissionGradingState`
A container for the results of a grading query, designed for seamless WebObjects integration and incremental updates.
- **Inheritance**: Subclasses `NSMutableDictionary<AssignmentOffering, Map<User, StudentSubmissionInfo>>`.
- **`lastFetchTimestamp`**: `NSTimestamp` marking when the last query was executed.
- **`offerings`**: `NSArray<AssignmentOffering>` defining the scope of the fetch.
- **`users`**: `NSDictionary<AssignmentOffering, NSArray<User>>` (optional) defining specific students to track per offering.
- **KVC Compatibility**: By inheriting from `NSMutableDictionary`, the state object can be used directly in `.wod` and `.html` templates using standard key-path access (e.g., `state.someOffering`).

### 2. Batch Fetching API
The API provides two overloaded versions of `submissionsForGrading`: one for initial/full fetches and one for incremental updates using an existing state.

**Method Signatures:**
```java
/**
 * Convenience wrapper for initial fetches.
 */
public static SubmissionGradingState submissionsForGrading(
    NSArray<AssignmentOffering> offerings, 
    NSDictionary<AssignmentOffering, NSArray<User>> users) {
    return submissionsForGrading(new SubmissionGradingState(offerings, users));
}

/**
 * Core incremental fetching method.
 * Uses the timestamp, offerings, and users stored in the state.
 */
public static SubmissionGradingState submissionsForGrading(
    SubmissionGradingState state)
```

### 3. Incremental Update Strategy
1. **Timestamp Capture**: Record `currentQueryTime = new NSTimestamp()`.
2. **Qualifier Construction**:
   - `Submission.ASSIGNMENT_OFFERING.in(state.offerings())`
   - If `state.users() != null`: `Submission.USER.in(state.users().allValues().@flatten)` (or specialized per-offering qualifiers).
   - If `state.lastFetchTimestamp() != null`: `Submission.SUBMIT_TIME.greaterThan(state.lastFetchTimestamp())`
3. **Merge**: Newly fetched submissions are added to the appropriate `StudentSubmissionInfo.allSubmissions` list within the dictionary (creating info objects as needed).
4. **Re-evaluate**: Only "dirty" `StudentSubmissionInfo` objects have their `gradedSubmission` re-calculated using `isBetterGradingChoiceThan()`.
5. **Finalize**: Update `state.lastFetchTimestamp = currentQueryTime`.

### 4. Integration with Grader Pages

#### `StudentsForAssignmentPage.java`
**Refactored Implementation:**
```java
// Initial fetch for all displayed offerings
SubmissionGradingState state = Submission.submissionsForGrading(
    offerings.displayedObjects(), null);

for (AssignmentOffering ao : offerings.displayedObjects()) {
    Map<User, StudentSubmissionInfo> aoResults = state.get(ao);
    
    // Convert to UserSubmissionPair for UI compatibility
    userGroup(ao).setObjectArray(UserSubmissionPair.fromInfoMap(aoResults));
    
    // Accumulate stats in one pass
    studentStats(ao).accumulate(aoResults.values());
}
```

#### `StudentCourseSummaryPage.java`
**Refactored Implementation:**
```java
// Collect all offerings and perform a single student-specific fetch
NSArray<AssignmentOffering> allOfferings = (NSArray<AssignmentOffering>)
    assignments.valueForKeyPath("offerings.@flatten");

// Map each offering to the selected student
NSMutableDictionary<AssignmentOffering, NSArray<User>> usersByOffering =
    new NSMutableDictionary<>();
NSArray<User> userList = new NSArray<>(selectedStudent);
for (AssignmentOffering ao : allOfferings) {
    usersByOffering.setObjectForKey(userList, ao);
}

SubmissionGradingState state = Submission.submissionsForGrading(
    allOfferings, usersByOffering);

for (AssignmentOffering ao : allOfferings) {
    StudentSubmissionInfo info = state.infoForUser(ao, selectedStudent);
    if (info != null) {
        submissions.setObjectForKey(info.gradedSubmission(), ao);
    }
}
```

#### `DownloadScoresDialog.java`
**Refactored Implementation:**
```java
private void collectSubmissionsToExport() {
    NSMutableDictionary<AssignmentOffering, NSArray<User>> usersByOffering =
        new NSMutableDictionary<>();
    for (AssignmentOffering ao : assignmentOfferings) {
        usersByOffering.setObjectForKey(includeStaff 
            ? ao.courseOffering().studentsAndStaff() 
            : ao.courseOffering().studentsWithoutStaff(), ao);
    }

    // Single batch fetch
    SubmissionGradingState state = Submission.submissionsForGrading(
        assignmentOfferings, usersByOffering);

    NSMutableArray<UserSubmissionPair> submissions = new NSMutableArray<>();
    for (AssignmentOffering ao : assignmentOfferings) {
        Map<User, StudentSubmissionInfo> results = state.get(ao);
        for (StudentSubmissionInfo info : results.values()) {
            if (useFullAllFormat || useFullAllDetailedFormat) {
                for (Submission s : info.allSubmissions()) {
                    submissions.add(new UserSubmissionPair(info.user(), s));
                }
            } else if (info.gradedSubmission() != null) {
                submissions.add(new UserSubmissionPair(info.user(), info.gradedSubmission()));
            }
        }
    }
    submissionsToExport = submissions;
}
```

### 5. Benefits
- **$O(1)$ Database Round-trips**: Fetches everything needed for a page in one or two queries.
- **UI Responsiveness**: Separates business logic (best submission selection) from retrieval.
- **KVC Integration**: `SubmissionGradingState` is directly usable in WebObjects templates.
- **Encapsulated Context**: The state object carries its own query parameters, simplifying incremental "refresh" logic.

### 6. Integration Plan
1. Implement `StudentSubmissionInfo` and `SubmissionGradingState` (subclass of `NSMutableDictionary`) in `Submission.java`.
2. Update `StudentsForAssignmentPage`, `StudentCourseSummaryPage`, and `DownloadScoresDialog` to use the batch fetch methods.
3. Ensure `UserSubmissionPair` provides a static helper `fromInfoMap` to bridge the new data structure with existing legacy code.
