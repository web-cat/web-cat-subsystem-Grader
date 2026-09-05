# Phase 4: UI Compatibility - Research

**Researched:** 2024-05-24
**Domain:** Java, WebObjects, ERXKey, KVC, UI Compatibility
**Confidence:** HIGH

## Summary

Phase 4 focuses on bridging the new batch-fetching infrastructure (introduced in Phases 1-3) with the legacy UI components that expect `UserSubmissionPair` objects. The core of this phase is implementing a static helper method, `fromInfoMap`, in the `UserSubmissionPair` class.

`UserSubmissionPair` is a simple POJO (Plain Old Java Object) that implements `NSKeyValueCodingAdditions`, making it compatible with WebObjects components and `ERXDisplayGroup`. It serves as a container for a `User` and their corresponding `Submission` for a specific assignment offering.

The new `Submission.StudentSubmissionInfo` data structure already tracks the `gradedSubmission` for each user. The `fromInfoMap` method will iterate over a map of these info objects, apply filtering logic (such as omitting partner links), and produce an `NSArray` of `UserSubmissionPair` objects suitable for display in the UI.

**Primary recommendation:** Implement `UserSubmissionPair.fromInfoMap` with support for optional user lists (to preserve order), partner link filtering (`omitPartners`), and statistical accumulation (`CumulativeStats`).

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.webobjects.foundation` | - | Foundation classes (NSArray, NSDictionary, KVC) | Core framework for Web-CAT |
| `er.extensions` | - | Project Wonder extensions (ERXKey, ERXDisplayGroup) | Standard extensions for Web-CAT |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `java.util.Map` | - | Java Map interface | Input to the bridge method |

## Architecture Patterns

### Recommended Project Structure
`UserSubmissionPair` is located in:
`src/org/webcat/grader/UserSubmissionPair.java`

It belongs to the same package as `Submission.java`, where its primary data sources (`StudentSubmissionInfo`, `CumulativeStats`) are defined as inner classes.

### Pattern: Bridge Method
The `fromInfoMap` method acts as a bridge between the new in-memory state (`SubmissionGradingState`) and the legacy UI logic. It encapsulates the conversion logic, ensuring that:
1. Users with no submissions are included with a `null` submission reference.
2. Users who are partner links are filtered out when `omitPartners` is true.
3. Statistics are accumulated in a single pass during conversion.
4. The output is a standard `NSArray` compatible with existing `ERXDisplayGroup` configurations.

### Anti-Patterns to Avoid
- **Sorting in the Bridge:** Avoid sorting the array inside `fromInfoMap`. Legacy UI components (like `StudentsForAssignmentPage`) already configure their `ERXDisplayGroup` with sort orderings based on `UserSubmissionPair.user`. Re-sorting in the bridge is redundant and less flexible.
- **Hand-Rolling KVC:** `UserSubmissionPair` correctly uses `NSKeyValueCodingAdditions.DefaultImplementation`, which should be preserved.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| UI Data Binding | Custom binding logic | `NSKeyValueCoding` | `ERXDisplayGroup` and WO components rely on KVC for property access. |
| Object-Property Keys | String literals | `ERXKey` | Provides type safety and avoids typo-related runtime errors. |
| Partner Filtering | Custom loops in every UI page | `fromInfoMap(..., omitPartners)` | Centralizes the logic for identifying and skipping partner links. |

## Common Pitfalls

### Pitfall 1: Random Map Iteration
**What goes wrong:** Iterating over `Map.values()` directly produces a random order, which might cause the UI to "flicker" before sorting is applied.
**How to avoid:** Provide an overload of `fromInfoMap` that takes an `NSArray<User>` to define the output order, matching the legacy `generateUserSubmissionPairs` behavior.

### Pitfall 2: Null gradedSubmission
**What goes wrong:** Assuming `info.gradedSubmission` is always non-null.
**Why it happens:** Some users may not have submitted anything.
**How to avoid:** Explicitly handle null checks and allow `UserSubmissionPair` to store a null submission (which it already supports via `userHasSubmission()`).

### Pitfall 3: CumulativeStats Accumulation
**What goes wrong:** Forgetting to accumulate stats during the bridge conversion, leading to empty or incorrect summary headers in the UI.
**How to avoid:** Include an optional `CumulativeStats` parameter in `fromInfoMap` to handle accumulation in the same pass.

## Code Examples

### Planned Implementation for `UserSubmissionPair.fromInfoMap`

```java
// Source: Proposed implementation for src/org/webcat/grader/UserSubmissionPair.java

/**
 * Creates an array of UserSubmissionPair objects from a map of submission info.
 * 
 * @param infoMap the map of user submission information
 * @param omitPartners true if partner links should be excluded
 * @param accumulator optional stats accumulator
 * @return an array of pairs
 */
public static NSArray<UserSubmissionPair> fromInfoMap(
        Map<User, Submission.StudentSubmissionInfo> infoMap,
        boolean omitPartners,
        Submission.CumulativeStats accumulator)
{
    if (infoMap == null)
    {
        return NSArray.emptyArray();
    }
    
    // Default to an unsorted collection if no user list is provided
    NSMutableArray<UserSubmissionPair> results =
        new NSMutableArray<UserSubmissionPair>(infoMap.size());

    for (Submission.StudentSubmissionInfo info : infoMap.values())
    {
        Submission sub = info.gradedSubmission();
        if (omitPartners && sub != null && sub.partnerLink())
        {
            continue;
        }

        results.addObject(new UserSubmissionPair(info.user(), sub));

        if (accumulator != null && sub != null)
        {
            accumulator.accumulate(sub);
        }
    }
    
    return results;
}

/**
 * Creates an array of UserSubmissionPair objects from a map of submission info,
 * preserving the order of the provided users list.
 */
public static NSArray<UserSubmissionPair> fromInfoMap(
        Map<User, Submission.StudentSubmissionInfo> infoMap,
        NSArray<User> users,
        boolean omitPartners,
        Submission.CumulativeStats accumulator)
{
    if (users == null)
    {
        return fromInfoMap(infoMap, omitPartners, accumulator);
    }

    NSMutableArray<UserSubmissionPair> results =
        new NSMutableArray<UserSubmissionPair>(users.size());

    for (User u : users)
    {
        Submission.StudentSubmissionInfo info = infoMap.get(u);
        Submission sub = (info != null) ? info.gradedSubmission() : null;

        if (omitPartners && sub != null && sub.partnerLink())
        {
            continue;
        }

        results.addObject(new UserSubmissionPair(u, sub));

        if (accumulator != null && sub != null)
        {
            accumulator.accumulate(sub);
        }
    }
    
    return results;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `generateUserSubmissionPairs` | `UserSubmissionPair.fromInfoMap` | Phase 4 | Centralizes UI conversion logic and handles new batch-fetch data structures. |

## Open Questions

1. **Staff/Student separation:** Should `fromInfoMap` also take `omitStaff`? 
   - **Recommendation:** No. The `infoMap` should already be pre-filtered by the caller (by providing the correct user set to the batch fetch). Adding it to `fromInfoMap` would add redundant complexity.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (existing) |
| Quick run command | `ant test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| COMPAT-01 | `fromInfoMap` correctly converts Map to NSArray with partner filtering. | Unit | `ant test` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `ant test` (if applicable)
- **Phase gate:** Full manual verification in `StudentsForAssignmentPage` during Phase 5.

### Wave 0 Gaps
- [ ] `tests/org/webcat/grader/UserSubmissionPairTest.java` — Needs to be created to verify the bridge logic.

## Sources

### Primary (HIGH confidence)
- `src/org/webcat/grader/UserSubmissionPair.java` - Inspected existing class structure.
- `src/org/webcat/grader/Submission.java` - Inspected `StudentSubmissionInfo`, `CumulativeStats`, and legacy `submissionsForGrading` logic.
- `src/org/webcat/grader/StudentsForAssignmentPage.java` - Inspected legacy UI expectations.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Core Web-CAT/WebObjects libraries are well-known.
- Architecture: HIGH - Bridge pattern is standard and well-supported by KVC.
- Pitfalls: HIGH - Identified via comparison with legacy code.

**Research date:** 2024-05-24
**Valid until:** 2024-06-23
