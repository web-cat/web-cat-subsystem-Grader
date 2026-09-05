# Student Extension Design & Assignment Offering Deadline Modernization

## 1. Executive Summary

This document outlines the architectural and implementation design to integrate the `StudentExtension` model into Web-CAT's `Grader` subsystem and modernize assignment availability calculations. 

The design accomplishes three primary objectives:
1. **Four-Field Availability Model on `AssignmentOffering`:**
   - **Global Defaults (`opensOn`, `closesOn`):** Stored attributes representing the default opening date and final closing deadline computed from the offering's `dueDate` and the assignment's `SubmissionProfile` (`availableTimeDelta`, `deadTimeDelta`).
   - **Aggregate Boundaries (`minOpensOn`, `maxClosesOn`):** Stored attributes representing the earliest opening date and latest closing date across both the global defaults and all individual `StudentExtension` records.
   - Automatically kept synchronized via optimized lifecycle triggers and helper methods.
2. **Direct Database Filtering:** Replaces legacy, coarse `dueDate` SQL fetches and in-memory post-filtering (using synthetic `availableFrom()` and `lateDeadline()`) with efficient, database-level queries using `minOpensOn` and `maxClosesOn`.
3. **Per-Student Submission Gating:** Implements individualized effective deadline calculations (`openingDateFor(user)`, `dueDateFor(user)`, `lateDeadlineFor(user)`) based on `StudentExtension` rules, ensuring all submission entry points are gated through `AssignmentOffering.userCanSubmit(user)`.

---

## 2. Data Model & Schema Enhancements

### 2.1 Database & EOModel Updates

```
+-----------------------------------+                 1 : N                 +--------------------------+
|        AssignmentOffering         | <-----------------------------------> |     StudentExtension     |
+-----------------------------------+                                       +--------------------------+
| id                                |                                       | id                       |
| assignmentId                      |                                       | assignmentOfferingId     |
| courseOfferingId                  |                                       | userId                   |
| dueDate                           |                                       | opensOn                  |
| opensOn          [GLOBAL DEFAULT] |                                       | dueDate                  |
| closesOn         [GLOBAL DEFAULT] |                                       | closesOn                 |
| minOpensOn       [AGGREGATE MIN]  |                                       +--------------------------+
| maxClosesOn      [AGGREGATE MAX]  |
| publish                           |
| ...                               |
+-----------------------------------+
```

1. **`AssignmentOffering` Entity (`AssignmentOffering.plist`)**:
   - **`opensOn`**: `prototypeName = dateTime`, `columnName = opensOn`, `allowsNull = Y`. Represents the global default opening date calculated from `dueDate` and `SubmissionProfile.availableTimeDelta`.
   - **`closesOn`**: `prototypeName = dateTime`, `columnName = closesOn`, `allowsNull = Y`. Represents the global default closing date calculated from `dueDate` and `SubmissionProfile.deadTimeDelta`.
   - **`minOpensOn`**: `prototypeName = dateTime`, `columnName = minOpensOn`, `allowsNull = Y`. Represents the earliest opening date across `opensOn` and all student extensions.
   - **`maxClosesOn`**: `prototypeName = dateTime`, `columnName = maxClosesOn`, `allowsNull = Y`. Represents the latest closing date across `closesOn` and all student extensions.
   - **`studentExtensions` Relationship**: To-many relationship to `StudentExtension` with `deleteRule = EODeleteRuleCascade`, destination attribute `assignmentOfferingId`, source attribute `id`.

2. **`StudentExtension` Entity (`StudentExtension.plist`)**:
   - Relationships:
     - `assignmentOffering`: Mandatory to-one `AssignmentOffering` (`assignmentOfferingId`).
     - `user`: Mandatory to-one `User` (`userId`).
   - Attributes:
     - `opensOn` (`dateTime`, nullable): Custom opening date override for the student.
     - `dueDate` (`dateTime`, nullable): Soft deadline extension for the student.
     - `closesOn` (`dateTime`, nullable): Hard cutoff deadline override for the student.

3. **Database Migration (`GraderDatabaseUpdates.java`)**:
   - Add new increment method (`updateIncrement39()`):
     ```sql
     ALTER TABLE TASSIGNMENTOFFERING ADD opensOn DATETIME;
     ALTER TABLE TASSIGNMENTOFFERING ADD closesOn DATETIME;
     ALTER TABLE TASSIGNMENTOFFERING ADD minOpensOn DATETIME;
     ALTER TABLE TASSIGNMENTOFFERING ADD maxClosesOn DATETIME;
     CREATE INDEX idx_assnoff_min_max ON TASSIGNMENTOFFERING (minOpensOn, maxClosesOn);
     CREATE INDEX idx_assnoff_open_close ON TASSIGNMENTOFFERING (opensOn, closesOn);

     CREATE TABLE StudentExtension (
         id INTEGER NOT NULL,
         assignmentOfferingId INTEGER NOT NULL,
         userId INTEGER NOT NULL,
         opensOn DATETIME,
         dueDate DATETIME,
         closesOn DATETIME
     );
     ALTER TABLE StudentExtension ADD PRIMARY KEY (id);
     CREATE INDEX idx_student_ext_assn_user ON StudentExtension (assignmentOfferingId, userId);
     ```
   - Perform a one-time data migration backfill for all existing `AssignmentOffering` records to calculate and populate `opensOn`, `closesOn`, `minOpensOn`, and `maxClosesOn`.

---

## 3. Automated Date Synchronization & Recalculation Engine

### 3.1 Date Calculation Rules

#### A. Global Defaults (`opensOn` and `closesOn`)
Given an `AssignmentOffering` $AO$, its `dueDate`, and associated `SubmissionProfile` $SP$:
- **`AssignmentOffering.opensOn`**:
  $$\text{opensOn} = \begin{cases} \text{null}, & \text{if } \text{dueDate} = \text{null} \\ \text{dueDate} - SP\text{.availableTimeDelta}(), & \text{if } SP \neq \text{null and } SP\text{.availableTimeDelta}() > 0 \\ \text{null}, & \text{otherwise (null represents \"always open\" / immediately available)} \end{cases}$$
- **`AssignmentOffering.closesOn`**:
  $$\text{closesOn} = \begin{cases} \text{null}, & \text{if } \text{dueDate} = \text{null} \\ \text{dueDate} + SP\text{.deadTimeDelta}(), & \text{if } SP \neq \text{null and } SP\text{.deadTimeDelta}() > 0 \\ \text{dueDate}, & \text{otherwise} \end{cases}$$

#### B. Aggregate Availability Envelope (`minOpensOn` and `maxClosesOn`)
The aggregate stored dates on `AssignmentOffering` encompass the global defaults and all active `StudentExtension` records:
- **`AssignmentOffering.minOpensOn`**: The earliest moment any student may begin submitting (where `null` represents "always open"):
  $$\text{minOpensOn} = \begin{cases} \text{null}, & \text{if } \text{opensOn} = \text{null} \text{ (baseline is already immediately available)} \\ \min\left( \text{opensOn},\; \min_{ext \in \text{extensions},\, ext\text{.opensOn} \neq \text{null}}\left( ext\text{.opensOn} \right) \right), & \text{otherwise} \end{cases}$$
- **`AssignmentOffering.maxClosesOn`**: The latest moment any student may submit:
  $$\text{maxClosesOn} = \max\left( \text{closesOn},\; \max_{ext \in \text{extensions}}\left( \text{ext.effectiveClosingDate} \right) \right)$$
  Where for each extension $ext$:
  - If $ext\text{.closesOn} \neq \text{null}$, use $ext\text{.closesOn}$.
  - Else if $ext\text{.dueDate} \neq \text{null}$, use $(SP\text{.deadTimeDelta}() > 0 \ ?\ ext\text{.dueDate} + SP\text{.deadTimeDelta}() :\ ext\text{.dueDate})$.

---

### 3.2 Helper Methods for `AssignmentOffering`

The `AssignmentOffering` class contains overloaded `updateTimes` helper methods to support both profile-wide delta changes and extension-specific incremental modifications. Note that `profile.availableTimeDelta()` and `profile.deadTimeDelta()` safely return `0L` if the underlying column value is null, avoiding verbose null-guard boilerplate.

```java
// =========================================================================
// Helper Methods in AssignmentOffering.java
// =========================================================================

// ----------------------------------------------------------
/**
 * Update the baseline defaults (opensOn, closesOn) and the aggregate bounds
 * (minOpensOn, maxClosesOn) given a SubmissionProfile.
 *
 * Employs an intelligent fast-path:
 *   - If baseOpen is null (no availableTimeDelta): opensOn and minOpensOn are set to null (always open).
 *   - If no extensions exist: minOpensOn = baseOpen, maxClosesOn = baseClose (O(1)).
 *   - If baseOpen expands earlier than currentMinOpensOn: minOpensOn = baseOpen (O(1)).
 *   - If baseOpen contracts/moves later: scans non-deleted extensions for true minimum.
 *   - maxClosesOn is updated across extensions as deadTimeDelta shifts soft extension cutoffs.
 *
 * @param profile the submission profile containing availability and dead deltas
 */
public void updateTimes(SubmissionProfile profile)
{
    NSTimestamp myDueDate = dueDate();
    if (myDueDate == null)
    {
        setOpensOn(null);
        setClosesOn(null);
        setMinOpensOn(null);
        setMaxClosesOn(null);
        return;
    }

    long availDelta = (profile != null) ? profile.availableTimeDelta() : 0L;
    long deadDelta  = (profile != null) ? profile.deadTimeDelta() : 0L;

    // 1. Calculate global baseline default open and close dates
    // Null baseOpen indicates no opening constraint (always open)
    NSTimestamp baseOpen = (availDelta > 0)
        ? new NSTimestamp(-availDelta, myDueDate)
        : null;
    NSTimestamp baseClose = (deadDelta > 0)
        ? new NSTimestamp(deadDelta, myDueDate)
        : myDueDate;

    setOpensOn(baseOpen);
    setClosesOn(baseClose);

    NSArray<StudentExtension> exts = studentExtensions();

    // 2. Calculate minOpensOn
    if (baseOpen == null)
    {
        // When baseline is always open, the aggregate envelope is also always open
        setMinOpensOn(null);
    }
    else if (exts == null || exts.count() == 0)
    {
        setMinOpensOn(baseOpen);
    }
    else
    {
        NSTimestamp currentMinOpen = minOpensOn();
        if (currentMinOpen != null && baseOpen.before(currentMinOpen))
        {
            // Window expanded earlier than any existing extension or previous baseline
            setMinOpensOn(baseOpen);
        }
        else
        {
            // Window contracted or moved later: scan extensions for the true minimum
            NSTimestamp earliestOpen = baseOpen;
            for (StudentExtension ext : exts)
            {
                if (!ext.isDeletedEO() && ext.opensOn() != null)
                {
                    if (earliestOpen == null || ext.opensOn().before(earliestOpen))
                    {
                        earliestOpen = ext.opensOn();
                    }
                }
            }
            setMinOpensOn(earliestOpen);
        }
    }

    // 3. Calculate maxClosesOn (deadDelta changes shift soft extension deadlines)
    if (exts == null || exts.count() == 0)
    {
        setMaxClosesOn(baseClose);
        return;
    }

    NSTimestamp latestClose = baseClose;
    for (StudentExtension ext : exts)
    {
        if (ext.isDeletedEO())
        {
            continue;
        }

        if (ext.closesOn() != null)
        {
            if (latestClose == null || ext.closesOn().after(latestClose))
            {
                latestClose = ext.closesOn();
            }
        }
        else if (ext.dueDate() != null)
        {
            NSTimestamp extLate = (deadDelta > 0)
                ? new NSTimestamp(deadDelta, ext.dueDate())
                : ext.dueDate();
            if (latestClose == null || extLate.after(latestClose))
            {
                latestClose = extLate;
            }
        }
    }
    setMaxClosesOn(latestClose);
}


// ----------------------------------------------------------
/**
 * Update the aggregate availability bounds (minOpensOn, maxClosesOn) given a
 * specific StudentExtension that was created, modified, or marked for deletion.
 *
 * @param ext the student extension triggering the update
 */
public void updateTimes(StudentExtension ext)
{
    NSTimestamp myDueDate = dueDate();
    if (myDueDate == null)
    {
        setMinOpensOn(null);
        setMaxClosesOn(null);
        return;
    }

    // Ensure baseline closesOn exists; if not, do full profile update
    if (closesOn() == null)
    {
        SubmissionProfile profile = (assignment() != null)
            ? assignment().submissionProfile() : null;
        updateTimes(profile);
        return;
    }

    SubmissionProfile profile = (assignment() != null)
        ? assignment().submissionProfile() : null;
    long deadDelta = (profile != null) ? profile.deadTimeDelta() : 0L;

    // 1. Update minOpensOn
    if (opensOn() == null)
    {
        // Baseline is always open
        setMinOpensOn(null);
    }
    else
    {
        NSTimestamp currentMinOpen = minOpensOn();
        // Fast-path: Check if the extension expands the opening window
        if (ext != null && ext.editingContext() != null && !ext.isDeletedEO()
            && ext.opensOn() != null)
        {
            if (currentMinOpen == null || ext.opensOn().before(currentMinOpen))
            {
                setMinOpensOn(ext.opensOn());
            }
        }
        else
        {
            // Contraction / deletion / null check: scan all non-deleted extensions
            NSTimestamp earliestOpen = opensOn();
            NSArray<StudentExtension> exts = studentExtensions();
            if (exts != null && exts.count() > 0)
            {
                for (StudentExtension studentExt : exts)
                {
                    if (!studentExt.isDeletedEO() && studentExt.opensOn() != null)
                    {
                        if (earliestOpen == null || studentExt.opensOn().before(earliestOpen))
                        {
                            earliestOpen = studentExt.opensOn();
                        }
                    }
                }
            }
            setMinOpensOn(earliestOpen);
        }
    }

    // 2. Update maxClosesOn
    // Fast-path evaluation: Check if the extension expands maxClosesOn
    if (ext != null && ext.editingContext() != null && !ext.isDeletedEO())
    {
        NSTimestamp extClose = ext.closesOn();
        if (extClose == null && ext.dueDate() != null)
        {
            extClose = (deadDelta > 0)
                ? new NSTimestamp(deadDelta, ext.dueDate())
                : ext.dueDate();
        }

        NSTimestamp currentMaxClose = maxClosesOn();
        if (extClose != null && (currentMaxClose == null || extClose.after(currentMaxClose)))
        {
            setMaxClosesOn(extClose);
            return;
        }
    }

    // Fallback/Contraction/Deletion: Full scan across all non-deleted extensions
    NSTimestamp latestClose = closesOn();
    NSArray<StudentExtension> exts = studentExtensions();
    if (exts != null && exts.count() > 0)
    {
        for (StudentExtension studentExt : exts)
        {
            if (studentExt.isDeletedEO())
            {
                continue;
            }

            if (studentExt.closesOn() != null)
            {
                if (latestClose == null || studentExt.closesOn().after(latestClose))
                {
                    latestClose = studentExt.closesOn();
                }
            }
            else if (studentExt.dueDate() != null)
            {
                NSTimestamp extLate = (deadDelta > 0)
                    ? new NSTimestamp(deadDelta, studentExt.dueDate())
                    : studentExt.dueDate();
                if (latestClose == null || extLate.after(latestClose))
                {
                    latestClose = extLate;
                }
            }
        }
    }
    setMaxClosesOn(latestClose);
}


// ----------------------------------------------------------
/**
 * Convenience method to recalculate all 4 availability timestamps using
 * this offering's assignment's SubmissionProfile.
 */
public void updateTimes()
{
    SubmissionProfile profile = (assignment() != null)
        ? assignment().submissionProfile() : null;
    updateTimes(profile);
}
```

---

### 3.3 Change-Propagation Triggers & Full Code for Entity Classes

To ensure the 4 fields never fall out of sync, trigger hooks are placed in `AssignmentOffering`, `StudentExtension`, `SubmissionProfile`, and `Assignment`.

```
+-----------------------+     modifies dead/avail deltas     +-----------------------+
|   SubmissionProfile   | ---------------------------------> |  AssignmentOffering   |
+-----------------------+                                    +-----------------------+
            ^                                                            ^
            | references profile                                         | modifies ext dates
            |                                                            |
+-----------------------+                                    +-----------------------+
|      Assignment       | ---------------------------------> |   StudentExtension    |
+-----------------------+   reassigns profile to assignment  +-----------------------+
```

#### 1. `AssignmentOffering.java` Lifecycle Triggers
```java
// =========================================================================
// Lifecycle Methods to Add/Update in AssignmentOffering.java
// =========================================================================

@Override
public void willInsert()
{
    setLastModified(new NSTimestamp());
    updateTimes();
    org.webcat.grader.actions.BlueJSubmitterDefinitions.flushCache();
    super.willInsert();
}

@Override
public void willUpdate()
{
    setLastModified(new NSTimestamp());

    NSDictionary<String, Object> changes = changedProperties();
    if (changes.containsKey(DUE_DATE_KEY)
        || changes.containsKey(ASSIGNMENT_KEY))
    {
        updateTimes();
    }

    org.webcat.grader.actions.BlueJSubmitterDefinitions.flushCache();

    String urlValue = lmsAssignmentUrl();
    if (urlValue != null && !urlValue.isEmpty())
    {
        String id = lmsAssignmentId();
        if (id == null || id.isEmpty())
        {
            int pos = urlValue.indexOf("?");
            if (pos > 0)
            {
                urlValue = urlValue.substring(0, pos);
            }
            // Attempt to guess canvas id from url
            Matcher m = CANVAS_URL.matcher(urlValue);
            if (m.find())
            {
                setLmsAssignmentId(m.group(1));
            }
        }
    }

    super.willUpdate();
}

@Override
public void willDelete()
{
    org.webcat.grader.actions.BlueJSubmitterDefinitions.flushCache();
    super.willDelete();
}
```

#### 2. `StudentExtension.java` Lifecycle Triggers
```java
// =========================================================================
// Lifecycle Methods to Add in StudentExtension.java
// =========================================================================

@Override
public void willInsert()
{
    super.willInsert();
    if (assignmentOffering() != null)
    {
        assignmentOffering().updateTimes(this);
    }
}

@Override
public void willUpdate()
{
    super.willUpdate();
    NSDictionary<String, Object> changes = changedProperties();
    if (changes.containsKey(OPENS_ON_KEY)
        || changes.containsKey(CLOSES_ON_KEY)
        || changes.containsKey(DUE_DATE_KEY)
        || changes.containsKey(ASSIGNMENT_OFFERING_KEY))
    {
        if (assignmentOffering() != null)
        {
            assignmentOffering().updateTimes(this);
        }
    }
}

@Override
public void willDelete()
{
    if (assignmentOffering() != null)
    {
        assignmentOffering().updateTimes(this);
    }
    super.willDelete();
}
```

#### 3. `SubmissionProfile.java` Lifecycle Triggers
```java
// =========================================================================
// Lifecycle Method in SubmissionProfile.java
// =========================================================================

@Override
public void willUpdate()
{
    NSDictionary<String, Object> changes = changedProperties();
    if (changes.containsKey(AVAILABLE_TIME_DELTA_KEY)
        || changes.containsKey(DEAD_TIME_DELTA_KEY))
    {
        EOEditingContext ec = editingContext();
        if (ec != null)
        {
            NSArray<Assignment> assignments = Assignment.objectsMatchingQualifier(
                ec, Assignment.SUBMISSION_PROFILE.is(this));
            if (assignments != null && assignments.count() > 0)
            {
                for (Assignment assignment : assignments)
                {
                    NSArray<AssignmentOffering> offerings = assignment.offerings();
                    if (offerings != null && offerings.count() > 0)
                    {
                        for (AssignmentOffering offering : offerings)
                        {
                            offering.updateTimes(this);
                        }
                    }
                }
            }
        }
    }

    org.webcat.grader.actions.BlueJSubmitterDefinitions.flushCache();
    super.willUpdate();
}
```

#### 4. `Assignment.java` Lifecycle Triggers
```java
// =========================================================================
// Lifecycle Method in Assignment.java
// =========================================================================

@Override
public void willUpdate()
{
    NSDictionary<String, Object> changes = changedProperties();
    if (changes.containsKey(SUBMISSION_PROFILE_KEY))
    {
        SubmissionProfile profile = submissionProfile();
        NSArray<AssignmentOffering> offerings = offerings();
        if (offerings != null && offerings.count() > 0)
        {
            for (AssignmentOffering offering : offerings)
            {
                offering.updateTimes(profile);
            }
        }
    }

    org.webcat.grader.actions.BlueJSubmitterDefinitions.flushCache();
    super.willUpdate();
}
```

---

## 4. Database Query & Submitter Engine Modernization

With `minOpensOn` and `maxClosesOn` persistent and indexed on `AssignmentOffering`, coarse `dueDate` fetching and in-memory post-filtering are replaced with direct database qualification.

### 4.1 Refactoring Target Areas

```
+----------------------------------------------------------------------------------------------------+
|                                    Query Refactoring Mapping                                       |
+------------------------------------+--------------------------------+------------------------------+
| Location                           | Legacy Query / Post-Filter     | Modernized DB Qualifier      |
+------------------------------------+--------------------------------+------------------------------+
| AssignmentOffering                 | In-memory:                     | SQL Qualifier:               |
|   .objectsForSubmitterEngine()     |   lateDeadline > currentTime   |   (minOpensOn IS NULL OR      |
|                                    |   availableFrom < currentTime  |    minOpensOn <= :time) AND   |
|                                    |                                |   maxClosesOn > :time        |
+------------------------------------+--------------------------------+------------------------------+
| GraderHomeStatus                   | Fetch all enrolled, then:      | SQL Qualifier:               |
|   (Active Assignments)             |   in-memory availableFrom/late |   (minOpensOn IS NULL OR      |
|                                    |                                |    minOpensOn <= :now) AND   |
|                                    |                                |   maxClosesOn > :now         |
+------------------------------------+--------------------------------+------------------------------+
| GraderHomeStatus                   | Fetch all enrolled, then:      | SQL Qualifier:               |
|   (Closed/Past Assignments)        |   in-memory !lateDeadline>now  |   maxClosesOn <= :now        |
+------------------------------------+--------------------------------+------------------------------+
| PickAssignmentToSubmitPage         | Synthetic WODisplayGroup keys: | Persistent DB Qualifiers:    |
|                                    |   AVAILABLE_FROM_KEY,          |   (minOpensOn IS NULL OR     |
|                                    |   LATE_DEADLINE_KEY            |    minOpensOn < :now),       |
|                                    |                                |   maxClosesOn > :now         |
+------------------------------------+--------------------------------+------------------------------+
| GraderNavigator                    | In-memory array filter         | SQL Qualifier or EOQualifier |
|                                    |   ao.lateDeadline() > now      |   maxClosesOn > now          |
+------------------------------------+--------------------------------+------------------------------+
```

### 4.2 Declarative Fetch Specifications (`AssignmentOffering.fspec`)

To elevate date filtering to the database layer, eliminate ad-hoc qualifiers in UI controllers, and prevent **N+1 relationship faulting queries** (for `assignment`, `submissionProfile`, `courseOffering`, `course`, and `semester`), `Grader.eomodeld/AssignmentOffering.fspec` is updated. 

> [!IMPORTANT]
> The existing fetch specifications (`allOfferingsOrderedByDueDate`, `offeringsForCourse`, `offeringsForCourseOffering`) must be **retained** because they are actively used by `EditAssignmentPage.java`, `Assignment.java`, and `AssignmentOffering.java`. Below is the **complete, unified contents** of `AssignmentOffering.fspec` with prefetching enabled across all specifications:

```plist
{
    allOfferingsOrderedByDueDate = {
        class = WCFetchSpecification;
        entityName = AssignmentOffering;
        fetchLimit = 0;
        prefetchingRelationshipKeyPaths = (
            assignment,
            "assignment.submissionProfile",
            courseOffering,
            "courseOffering.course",
            "courseOffering.semester"
        );
        sortOrderings = (
            {class = EOSortOrdering; key = dueDate; selectorName = "compareAscending:"; }
        );
    };

    offeringsForCourseOffering = {
        class = WCFetchSpecification;
        entityName = AssignmentOffering;
        fetchLimit = 0;
        prefetchingRelationshipKeyPaths = (
            assignment,
            "assignment.submissionProfile",
            courseOffering,
            "courseOffering.course",
            "courseOffering.semester"
        );
        qualifier = {
            class = EOKeyValueQualifier;
            key = courseOffering;
            selectorName = "isEqualTo:";
            value = {"_key" = courseOffering; class = EOQualifierVariable; };
        };
        sortOrderings = (
            {class = EOSortOrdering; key = dueDate; selectorName = "compareDescending:"; }
        );
    };

    offeringsForCourse = {
        class = WCFetchSpecification;
        entityName = AssignmentOffering;
        fetchLimit = 0;
        prefetchingRelationshipKeyPaths = (
            assignment,
            "assignment.submissionProfile",
            courseOffering,
            "courseOffering.course",
            "courseOffering.semester"
        );
        qualifier = {
            class = EOKeyValueQualifier;
            key = "courseOffering.course";
            selectorName = "isEqualTo:";
            value = {"_key" = course; class = EOQualifierVariable; };
        };
        sortOrderings = (
            {class = EOSortOrdering; key = dueDate; selectorName = "compareDescending:"; }
        );
    };

    openOfferingsForCourseOffering = {
        class = WCFetchSpecification;
        entityName = AssignmentOffering;
        fetchLimit = 0;
        prefetchingRelationshipKeyPaths = (
            assignment,
            "assignment.submissionProfile",
            courseOffering,
            "courseOffering.course",
            "courseOffering.semester"
        );
        qualifier = {
            class = EOAndQualifier;
            qualifiers = (
                {
                    class = EOKeyValueQualifier;
                    key = courseOffering;
                    selectorName = "isEqualTo:";
                    value = {"_key" = courseOffering; class = EOQualifierVariable; };
                },
                {
                    class = EOKeyValueQualifier;
                    key = publish;
                    selectorName = "isEqualTo:";
                    value = {"_key" = publish; class = EOQualifierVariable; };
                },
                {
                    class = EOOrQualifier;
                    qualifiers = (
                        {
                            class = EOKeyValueQualifier;
                            key = minOpensOn;
                            selectorName = "isEqualTo:";
                            value = {class = EONull; };
                        },
                        {
                            class = EOKeyValueQualifier;
                            key = minOpensOn;
                            selectorName = "isLessThanOrEqualTo:";
                            value = {"_key" = currentTime; class = EOQualifierVariable; };
                        }
                    );
                },
                {
                    class = EOKeyValueQualifier;
                    key = maxClosesOn;
                    selectorName = "isGreaterThan:";
                    value = {"_key" = currentTime; class = EOQualifierVariable; };
                }
            );
        };
        sortOrderings = (
            {class = EOSortOrdering; key = dueDate; selectorName = "compareAscending:"; }
        );
    };

    allOpenOfferingsForCourseOffering = {
        class = WCFetchSpecification;
        entityName = AssignmentOffering;
        fetchLimit = 0;
        prefetchingRelationshipKeyPaths = (
            assignment,
            "assignment.submissionProfile",
            courseOffering,
            "courseOffering.course",
            "courseOffering.semester"
        );
        qualifier = {
            class = EOAndQualifier;
            qualifiers = (
                {
                    class = EOKeyValueQualifier;
                    key = courseOffering;
                    selectorName = "isEqualTo:";
                    value = {"_key" = courseOffering; class = EOQualifierVariable; };
                },
                {
                    class = EOKeyValueQualifier;
                    key = maxClosesOn;
                    selectorName = "isGreaterThan:";
                    value = {"_key" = currentTime; class = EOQualifierVariable; };
                }
            );
        };
        sortOrderings = (
            {class = EOSortOrdering; key = dueDate; selectorName = "compareAscending:"; }
        );
    };

    openOfferingsForStudent = {
        class = WCFetchSpecification;
        entityName = AssignmentOffering;
        fetchLimit = 0;
        prefetchingRelationshipKeyPaths = (
            assignment,
            "assignment.submissionProfile",
            courseOffering,
            "courseOffering.course",
            "courseOffering.semester"
        );
        qualifier = {
            class = EOAndQualifier;
            qualifiers = (
                {
                    class = EOKeyValueQualifier;
                    key = "courseOffering.students";
                    selectorName = "doesContain:";
                    value = {"_key" = user; class = EOQualifierVariable; };
                },
                {
                    class = EOKeyValueQualifier;
                    key = publish;
                    selectorName = "isEqualTo:";
                    value = {"_key" = publish; class = EOQualifierVariable; };
                },
                {
                    class = EOOrQualifier;
                    qualifiers = (
                        {
                            class = EOKeyValueQualifier;
                            key = minOpensOn;
                            selectorName = "isEqualTo:";
                            value = {class = EONull; };
                        },
                        {
                            class = EOKeyValueQualifier;
                            key = minOpensOn;
                            selectorName = "isLessThanOrEqualTo:";
                            value = {"_key" = currentTime; class = EOQualifierVariable; };
                        }
                    );
                },
                {
                    class = EOKeyValueQualifier;
                    key = maxClosesOn;
                    selectorName = "isGreaterThan:";
                    value = {"_key" = currentTime; class = EOQualifierVariable; };
                }
            );
        };
        sortOrderings = (
            {class = EOSortOrdering; key = dueDate; selectorName = "compareAscending:"; }
        );
        usesDistinct = YES;
    };

    closedOfferingsForStudent = {
        class = WCFetchSpecification;
        entityName = AssignmentOffering;
        fetchLimit = 0;
        prefetchingRelationshipKeyPaths = (
            assignment,
            "assignment.submissionProfile",
            courseOffering,
            "courseOffering.course",
            "courseOffering.semester"
        );
        qualifier = {
            class = EOAndQualifier;
            qualifiers = (
                {
                    class = EOKeyValueQualifier;
                    key = "courseOffering.students";
                    selectorName = "doesContain:";
                    value = {"_key" = user; class = EOQualifierVariable; };
                },
                {
                    class = EOKeyValueQualifier;
                    key = publish;
                    selectorName = "isEqualTo:";
                    value = {"_key" = publish; class = EOQualifierVariable; };
                },
                {
                    class = EOKeyValueQualifier;
                    key = maxClosesOn;
                    selectorName = "isLessThanOrEqualTo:";
                    value = {"_key" = currentTime; class = EOQualifierVariable; };
                }
            );
        };
        sortOrderings = (
            {class = EOSortOrdering; key = dueDate; selectorName = "compareDescending:"; }
        );
        usesDistinct = YES;
    };

    openOfferingsForStaff = {
        class = WCFetchSpecification;
        entityName = AssignmentOffering;
        fetchLimit = 0;
        prefetchingRelationshipKeyPaths = (
            assignment,
            "assignment.submissionProfile",
            courseOffering,
            "courseOffering.course",
            "courseOffering.semester"
        );
        qualifier = {
            class = EOAndQualifier;
            qualifiers = (
                {
                    class = EOOrQualifier;
                    qualifiers = (
                        {
                            class = EOKeyValueQualifier;
                            key = "courseOffering.instructors";
                            selectorName = "doesContain:";
                            value = {"_key" = user; class = EOQualifierVariable; };
                        },
                        {
                            class = EOKeyValueQualifier;
                            key = "courseOffering.graders";
                            selectorName = "doesContain:";
                            value = {"_key" = user; class = EOQualifierVariable; };
                        }
                    );
                },
                {
                    class = EOKeyValueQualifier;
                    key = maxClosesOn;
                    selectorName = "isGreaterThan:";
                    value = {"_key" = currentTime; class = EOQualifierVariable; };
                }
            );
        };
        sortOrderings = (
            {class = EOSortOrdering; key = dueDate; selectorName = "compareAscending:"; }
        );
        usesDistinct = YES;
    };

    closedOfferingsForStaff = {
        class = WCFetchSpecification;
        entityName = AssignmentOffering;
        fetchLimit = 0;
        prefetchingRelationshipKeyPaths = (
            assignment,
            "assignment.submissionProfile",
            courseOffering,
            "courseOffering.course",
            "courseOffering.semester"
        );
        qualifier = {
            class = EOAndQualifier;
            qualifiers = (
                {
                    class = EOOrQualifier;
                    qualifiers = (
                        {
                            class = EOKeyValueQualifier;
                            key = "courseOffering.instructors";
                            selectorName = "doesContain:";
                            value = {"_key" = user; class = EOQualifierVariable; };
                        },
                        {
                            class = EOKeyValueQualifier;
                            key = "courseOffering.graders";
                            selectorName = "doesContain:";
                            value = {"_key" = user; class = EOQualifierVariable; };
                        }
                    );
                },
                {
                    class = EOKeyValueQualifier;
                    key = maxClosesOn;
                    selectorName = "isLessThanOrEqualTo:";
                    value = {"_key" = currentTime; class = EOQualifierVariable; };
                }
            );
        };
        sortOrderings = (
            {class = EOSortOrdering; key = dueDate; selectorName = "compareDescending:"; }
        );
        usesDistinct = YES;
    };
}
```

### 4.3 N+1 Query Analysis & Prevention Strategy

When retrieving `AssignmentOffering` objects in list contexts (e.g. `GraderHomeStatus`, `PickAssignmentToSubmitPage`), accessing related entities causes severe N+1 database roundtrips if relationships are resolved lazily:

1. **`offering.assignment()` & `assignment.submissionProfile()`**:
   - Accessing `assignment().submissionProfile().deadTimeDelta()` in deadline evaluations triggers 2 additional SQL queries per offering ($2N$).
2. **`offering.courseOffering().course()` & `courseOffering.semester()`**:
   - Displaying course groupings in `GraderHomeStatus.organizeAssignments()` triggers 2 additional queries per offering ($2N$).
3. **`offering.studentExtensions`**:
   - Querying extensions individually per offering via `firstObjectMatchingValues` produces $N$ extra queries.

**Prefetching Mitigation**:
- In both `.fspec` definitions and Java query helpers, `prefetchingRelationshipKeyPaths` is configured with:
  `["assignment", "assignment.submissionProfile", "courseOffering", "courseOffering.course", "courseOffering.semester"]`.
- In `extensionForUser(User user)`, lookups evaluate against the prefetched/faulted in-memory `studentExtensions()` relationship rather than issuing isolated SQL queries.

---

## 5. Single Point of Control: Centralized Retrieval, Eligibility Predicates & Universal Gating

To enforce clean separation of concerns and eliminate scattered date/eligibility logic across UI controllers, all date calculations, database qualifications, temporal state checks, and submission eligibility decisions are centralized within `AssignmentOffering`.

```
               +-------------------------------------------------------------+
               |                  AssignmentOffering                         |
               |                                                             |
               |  +--------------------+             +--------------------+  |
               |  |  Static Retrieval  |             | Instance Predicate |  |
               |  |  & DB Qualifiers   |             | & Date Resolution  |  |
               |  +--------------------+             +--------------------+  |
               |            |                                  |             |
               |            |                                  v             |
               |            |                     +-----------------------+  |
               |            |                     |     userCanSubmit     |  |
               |            |                     +-----------------------+  |
               +------------|----------------------------------|-------------+
                            |                                  |
            +---------------+---------------+                  |
            |               |               |                  |
            v               v               v                  v
     +--------------+ +------------+ +-------------+    +---------------+
     | GraderHome-  | | PickAssn-  | | Submitter-  |    | Submission /  |
     | Status.java  | | ToSubmit   | | Engine (API)|    | Upload Pages  |
     +--------------+ +------------+ +-------------+    +---------------+
```

---

### 5.1 Centralized Static Query & Retrieval Methods on `AssignmentOffering`

These static methods provide canonical query qualifiers and high-level retrieval functions with **built-in relationship prefetching** to eliminate N+1 queries:

```java
// =========================================================================
// Static Query & Retrieval Methods in AssignmentOffering.java
// =========================================================================

/**
 * Standard relationship keypaths to prefetch on all AssignmentOffering queries
 * to eliminate N+1 lazy faulting during page rendering and deadline calculation.
 */
public static final NSArray<String> DEFAULT_PREFETCH_KEYPATHS = new NSArray<String>(new String[] {
    ASSIGNMENT_KEY,
    ASSIGNMENT_KEY + "." + Assignment.SUBMISSION_PROFILE_KEY,
    COURSE_OFFERING_KEY,
    COURSE_OFFERING_KEY + "." + CourseOffering.COURSE_KEY,
    COURSE_OFFERING_KEY + "." + CourseOffering.SEMESTER_KEY
});


// ----------------------------------------------------------
/**
 * Execute a fetch specification with relationship prefetching.
 */
public static NSArray<AssignmentOffering> fetchWithPrefetch(
    EOEditingContext ec,
    EOQualifier qualifier,
    NSArray<EOSortOrdering> sortOrderings)
{
    ERXFetchSpecification<AssignmentOffering> fspec =
        new ERXFetchSpecification<AssignmentOffering>(
            ENTITY_NAME, qualifier, sortOrderings);
    fspec.setPrefetchingRelationshipKeyPaths(DEFAULT_PREFETCH_KEYPATHS);
    return ec.objectsWithFetchSpecification(fspec);
}


// ----------------------------------------------------------
/**
 * Canonical qualifier matching assignment offerings that are currently open
 * (minOpensOn is null OR minOpensOn <= currentTime) AND maxClosesOn > currentTime.
 */
public static EOQualifier openQualifier(NSTimestamp currentTime)
{
    return minOpensOn.isNull()
        .or(minOpensOn.lessThanOrEqualTo(currentTime))
        .and(maxClosesOn.greaterThan(currentTime));
}


// ----------------------------------------------------------
/**
 * Canonical qualifier matching assignment offerings that are closed
 * (maxClosesOn <= currentTime).
 */
public static EOQualifier closedQualifier(NSTimestamp currentTime)
{
    return maxClosesOn.lessThanOrEqualTo(currentTime);
}


// ----------------------------------------------------------
/**
 * Retrieve all active, open assignment offerings for a user (combines student
 * offerings that are published and open, plus course staff offerings)
 * with prefetching for assignment, profile, course, and semester.
 *
 * @param ec the editing context
 * @param user the user (student or instructor/grader)
 * @param currentTime the reference timestamp
 * @return array of open AssignmentOfferings
 */
public static NSArray<AssignmentOffering> openOfferingsForUser(
    EOEditingContext ec, User user, NSTimestamp currentTime)
{
    if (ec == null || user == null)
    {
        return NSArray.emptyArray();
    }

    // 1. Fetch published offerings open for student (with prefetching)
    EOQualifier studentQual = publish.isTrue()
        .and(courseOffering.dot(CourseOffering.students).is(user))
        .and(openQualifier(currentTime));

    NSMutableArray<AssignmentOffering> results =
        new NSMutableArray<AssignmentOffering>(
            fetchWithPrefetch(ec, studentQual, null));

    // 2. Add offerings where user has instructor privileges
    EOQualifier instructorQual = courseOffering.dot(CourseOffering.instructors).is(user)
        .and(maxClosesOn.greaterThan(currentTime));

    ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
        results, fetchWithPrefetch(ec, instructorQual, null));

    // 3. Add offerings where user has grader privileges
    EOQualifier graderQual = courseOffering.dot(CourseOffering.graders).is(user)
        .and(maxClosesOn.greaterThan(currentTime));

    ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
        results, fetchWithPrefetch(ec, graderQual, null));

    EOSortOrdering.sortArrayUsingKeyOrderArray(
        results, new NSArray<EOSortOrdering>(DUE_DATE.asc()));

    return results;
}


// ----------------------------------------------------------
/**
 * Retrieve closed/past assignment offerings for a user, optionally restricted
 * to a specific semester (with prefetching).
 *
 * @param ec the editing context
 * @param user the user
 * @param semester optional semester filter (may be null)
 * @param currentTime the reference timestamp
 * @return array of closed AssignmentOfferings
 */
public static NSArray<AssignmentOffering> closedOfferingsForUser(
    EOEditingContext ec, User user, Semester semester, NSTimestamp currentTime)
{
    if (ec == null || user == null)
    {
        return NSArray.emptyArray();
    }

    EOQualifier closed = closedQualifier(currentTime);
    EOQualifier semQual = (semester != null)
        ? courseOffering.dot(CourseOffering.semester).is(semester)
        : null;

    // 1. Closed offerings for student (only published)
    ERXAndQualifier studentQual = publish.isTrue()
        .and(courseOffering.dot(CourseOffering.students).is(user))
        .and(closed);
    if (semQual != null)
    {
        studentQual = studentQual.and(semQual);
    }

    NSMutableArray<AssignmentOffering> results =
        new NSMutableArray<AssignmentOffering>(
            fetchWithPrefetch(ec, studentQual, null));

    // 2. Closed offerings for instructor (includes unpublished)
    ERXAndQualifier instructorQual =
        courseOffering.dot(CourseOffering.instructors).is(user)
        .and(closed);
    if (semQual != null)
    {
        instructorQual = instructorQual.and(semQual);
    }

    ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
        results, fetchWithPrefetch(ec, instructorQual, null));

    // 3. Closed offerings for grader (includes unpublished)
    ERXAndQualifier graderQual =
        courseOffering.dot(CourseOffering.graders).is(user)
        .and(closed);
    if (semQual != null)
    {
        graderQual = graderQual.and(semQual);
    }

    ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
        results, fetchWithPrefetch(ec, graderQual, null));

    // 4. Sort merged results in memory
    EOSortOrdering.sortArrayUsingKeyOrderArray(
        results, new NSArray<EOSortOrdering>(DUE_DATE.desc()));

    return results;
}


// ----------------------------------------------------------
/**
 * Retrieve assignment offerings for a specific course offering, automatically
 * applying student vs. staff visibility and open status filters (with prefetching).
 *
 * @param ec the editing context
 * @param courseOffering the course offering
 * @param user the current user
 * @param onlyOpen whether to restrict to currently open assignments
 * @param currentTime the reference timestamp
 * @return array of AssignmentOfferings
 */
public static NSArray<AssignmentOffering> offeringsForCourseOffering(
    EOEditingContext ec,
    CourseOffering courseOffering,
    User user,
    boolean onlyOpen,
    NSTimestamp currentTime)
{
    if (ec == null || courseOffering == null)
    {
        return NSArray.emptyArray();
    }

    boolean isStaff = (user != null)
        && (courseOffering.isInstructor(user) || courseOffering.isGrader(user));

    NSMutableArray<EOQualifier> quals = new NSMutableArray<EOQualifier>();
    quals.addObject(COURSE_OFFERING.is(courseOffering));

    if (!isStaff)
    {
        quals.addObject(publish.isTrue());
    }

    if (onlyOpen)
    {
        if (!isStaff)
        {
            quals.addObject(openQualifier(currentTime));
        }
        else
        {
            quals.addObject(maxClosesOn.greaterThan(currentTime));
        }
    }

    return fetchWithPrefetch(ec, ERXQ.and(quals),
        new NSArray<EOSortOrdering>(DUE_DATE.asc()));
}


// ----------------------------------------------------------
/**
 * Submitter engine object retrieval (direct API / IDE plugin).
 */
public static NSArray<AssignmentOffering> objectsForSubmitterEngine(
    EOEditingContext context, boolean showAll, NSTimestamp currentTime)
{
    EOQualifier qualifier;
    if (showAll)
    {
        qualifier = maxClosesOn.greaterThan(
            Semester.forDate(context, currentTime).semesterStartDate());
    }
    else
    {
        qualifier = openQualifier(currentTime);
    }

    return fetchWithPrefetch(context, qualifier,
        new NSArray<EOSortOrdering>(DUE_DATE.asc()));
}
```

---

### 5.2 Individualized Effective Deadlines & Temporal State Predicates

The calculation logic on `AssignmentOffering` resolves effective deadlines for individual students (factoring in `StudentExtension`) and provides temporal status predicates. `extensionForUser` evaluates over the in-memory `studentExtensions()` relationship to prevent per-item N+1 queries:

```java
// =========================================================================
// Instance Methods in AssignmentOffering.java
// =========================================================================

// ----------------------------------------------------------
/**
 * Retrieve the student extension for the given user on this offering, if one exists.
 * Evaluates over the in-memory studentExtensions relationship to prevent N+1 queries.
 */
public StudentExtension extensionForUser(User user)
{
    if (user == null)
    {
        return null;
    }

    NSArray<StudentExtension> exts = studentExtensions();
    if (exts != null && exts.count() > 0)
    {
        return ERXArrayUtilities.firstObject(exts,
            StudentExtension.USER.is(user));
    }
    return null;
}


// ----------------------------------------------------------
/**
 * Effective opening date for a specific student.
 */
public NSTimestamp openingDateFor(User user)
{
    StudentExtension ext = extensionForUser(user);
    if (ext != null && ext.opensOn() != null)
    {
        return ext.opensOn();
    }
    return opensOn();
}


// ----------------------------------------------------------
/**
 * Effective due date (soft deadline) for a specific student.
 */
public NSTimestamp dueDateFor(User user)
{
    StudentExtension ext = extensionForUser(user);
    if (ext != null && ext.dueDate() != null)
    {
        return ext.dueDate();
    }
    return dueDate();
}


// ----------------------------------------------------------
/**
 * Effective hard deadline (cutoff) for a specific student.
 * 3-Tier Resolution:
 *   Tier 1: Explicit student extension hard deadline (ext.closesOn)
 *   Tier 2: Max of base offering closing deadline OR (student soft deadline + deadTimeDelta)
 *   Tier 3: Base offering closing deadline (closesOn)
 */
public NSTimestamp lateDeadlineFor(User user)
{
    StudentExtension ext = extensionForUser(user);
    NSTimestamp baseLate = (closesOn() != null) ? closesOn() : lateDeadline();

    if (ext != null)
    {
        // Tier 1: Explicit extension hard deadline override
        if (ext.closesOn() != null)
        {
            return ext.closesOn();
        }

        // Tier 2: Extended soft deadline + dead window
        if (ext.dueDate() != null)
        {
            SubmissionProfile profile = (assignment() != null)
                ? assignment().submissionProfile() : null;
            long deadDelta = (profile != null) ? profile.deadTimeDelta() : 0L;
            NSTimestamp extLate = (deadDelta > 0)
                ? new NSTimestamp(deadDelta, ext.dueDate()) : ext.dueDate();

            if (baseLate == null || extLate.after(baseLate))
            {
                return extLate;
            }
        }
    }

    // Tier 3: Base offering closing deadline
    return baseLate;
}


// ----------------------------------------------------------
/**
 * Predicate checking if this offering is open for a specific user at currentTime.
 */
public boolean isOpenFor(User user, NSTimestamp currentTime)
{
    NSTimestamp open = openingDateFor(user);
    NSTimestamp close = lateDeadlineFor(user);

    boolean afterOpen = (open == null) || !currentTime.before(open);
    boolean beforeClose = (close == null) || currentTime.before(close);

    return afterOpen && beforeClose;
}


// ----------------------------------------------------------
/**
 * Predicate checking if this offering has closed for a specific user.
 */
public boolean isClosedFor(User user, NSTimestamp currentTime)
{
    NSTimestamp close = lateDeadlineFor(user);
    return close != null && !currentTime.before(close);
}


// ----------------------------------------------------------
/**
 * Predicate checking if this offering is not yet available to a specific user.
 */
public boolean isUnavailableFor(User user, NSTimestamp currentTime)
{
    NSTimestamp open = openingDateFor(user);
    return open != null && open.after(currentTime);
}
```

---

### 5.3 Universal Submission Access Gating (`userCanSubmit`)

`AssignmentOffering.userCanSubmit` serves as the single authority across the entire codebase for verifying student submission eligibility:

```java
// ----------------------------------------------------------
public boolean userCanSubmit(User user)
{
    return userCanSubmit(user, new NSTimestamp());
}


// ----------------------------------------------------------
public boolean userCanSubmit(User user, NSTimestamp currentTime)
{
    if (user == null || courseOffering() == null)
    {
        return false;
    }

    // Course staff (instructors, graders) and admins can always submit
    if (user.hasAdminPrivileges()
        || courseOffering().isInstructor(user)
        || courseOffering().isGrader(user))
    {
        return true;
    }

    // Students must be enrolled, assignment must be published,
    // and currentTime must be within [openingDateFor(user), lateDeadlineFor(user)]
    if (publish() && courseOffering().students().contains(user))
    {
        return isOpenFor(user, currentTime);
    }

    return false;
}
```

---

### 5.4 Refactored Call Sites (Encapsulated Single Point of Control)

With centralized query and predicate methods on `AssignmentOffering`, external callers are drastically simplified:

#### 1. `GraderHomeStatus.java`
```java
// In beforeAppendToResponse():
currentTime = new NSTimestamp();

// Centralized fetch of all open offerings for this user:
NSArray<AssignmentOffering> open =
    AssignmentOffering.openOfferingsForUser(localContext(), user(), currentTime);
currentAssignments = organizeAssignments(open);
courses.setObjectArray(new NSArray<Course>(currentAssignments.keySet()));

// Centralized fetch of closed offerings:
Semester currentSemester = (semesters.count() > 0) ? semesters.get(0) : null;
NSArray<AssignmentOffering> old =
    AssignmentOffering.closedOfferingsForUser(localContext(), user(), currentSemester, currentTime);
oldAssignments = organizeAssignments(old);
coursesForOld.setObjectArray(new NSArray<Course>(oldAssignments.keySet()));
oldAssignmentGroup.setObjectArray(old);


// In assignmentOfferingIsUnavailable():
public boolean assignmentOfferingIsUnavailable()
{
    return assignmentOffering.isUnavailableFor(user(), currentTime);
}


// In selectAssignment(AssignmentOffering offering):
if (offering.isClosedFor(user(), new NSTimestamp()))
{
    prefs().setShowClosedAssignments(true);
}
```

#### 2. `PickAssignmentToSubmitPage.java`
```java
// In beforeAppendToResponse():
NSDictionary<String, Object> config =
    wcSession().tabs.selectedDescendant().config();
boolean onlyOpen = (config == null
    || !ERXValueUtilities.booleanValueWithDefault(config.objectForKey("all"), false));

NSArray<AssignmentOffering> offerings =
    AssignmentOffering.offeringsForCourseOffering(
        localContext(), selectedCourse, user(), onlyOpen, currentTime);

assignmentDisplayGroup.setObjectArray(offerings);
```

#### 3. `GraderNavigator.java`
```java
// In beforeAppendToResponse():
NSTimestamp now = new NSTimestamp();
boolean hideClosed =
    (hideClosedAssignmentsFromStudents && !userIsStaffForSelectedCourse())
    || !showClosedAssignments();

if (hideClosed)
{
    assnOffs = ERXQ.filtered(assnOffs,
        AssignmentOffering.maxClosesOn.greaterThan(now));
}
```

#### 4. Submission Processing (`Grader.java`, `UploadSubmissionPage.java`, `SubmissionBatchHandler.java`)
```java
// Centralized single point of submission gating:
if (thisAssignment.userCanSubmit(localizedUser, currentTime))
{
    // Proceed with submission ingest...
}
```

---

## 6. Implementation Roadmap

```
  +------------------------------------------------------------------------+
  | Phase 1: EOModel & Schema Migration                                   |
  | - Add opensOn, closesOn, minOpensOn, maxClosesOn to AssignmentOffering |
  | - Add opensOn, dueDate, closesOn to StudentExtension                   |
  | - Add studentExtensions relationship to AssignmentOffering.plist       |
  | - Update GraderDatabaseUpdates.java (increment 39) with indices        |
  +-----------------------------------┬------------------------------------+
                                      │
  +-----------------------------------v------------------------------------+
  | Phase 2: Core Calculation & Extension Logic in AssignmentOffering     |
  | - Implement updateTimes(SubmissionProfile) and updateTimes(StudentExt) |
  | - Implement extensionForUser(), openingDateFor(), dueDateFor()         |
  | - Implement lateDeadlineFor() with 3-tier resolution                   |
  | - Update userCanSubmit(User, NSTimestamp)                              |
  +-----------------------------------┬------------------------------------+
                                      │
  +-----------------------------------v------------------------------------+
  | Phase 3: Lifecycle Hooks & Synchronization Triggers                   |
  | - Hook willInsert/willUpdate in AssignmentOffering                     |
  | - Hook willInsert/willUpdate/willDelete in StudentExtension            |
  | - Hook willUpdate in SubmissionProfile and Assignment                  |
  +-----------------------------------┬------------------------------------+
                                      │
  +-----------------------------------v------------------------------------+
  | Phase 4: Database Query & Submitter Engine Modernization              |
  | - Refactor objectsForSubmitterEngine() with minOpensOn & maxClosesOn   |
  | - Refactor GraderHomeStatus, GraderNavigator, PickAssignmentPage      |
  | - Update BlueJSubmitterDefinitions & actions/assignments.java          |
  +-----------------------------------┬------------------------------------+
                                      │
  +-----------------------------------v------------------------------------+
  | Phase 5: Submission Entry Point Gating & Verification                  |
  | - Refactor Grader.processSubmission() to use userCanSubmit()          |
  | - Audit UploadSubmissionPage & SubmissionBatchHandler                  |
  | - Unit tests for extensions, recalculation triggers, and deadline tiers|
  +------------------------------------------------------------------------+
```

---

## 7. Verification & Test Plan

1. **Unit Tests for Date Calculations (`AssignmentOfferingTest`, `StudentExtensionTest`)**:
   - Baseline dates calculation for `opensOn` and `closesOn` with various `availableTimeDelta` and `deadTimeDelta` settings.
   - Aggregate boundaries `minOpensOn` and `maxClosesOn` correctly encompass the global baseline and multiple extensions.
   - Individual extensions overriding `opensOn`, `dueDate`, and `closesOn`.
   - Tier 1, Tier 2, and Tier 3 resolution for `lateDeadlineFor(user)`.
2. **Lifecycle Trigger Tests**:
   - Creating, updating, and deleting a `StudentExtension` automatically adjusts the offering's `minOpensOn` and `maxClosesOn`.
   - Modifying `dueDate` on `AssignmentOffering` updates `opensOn`, `closesOn`, `minOpensOn`, and `maxClosesOn`.
   - Modifying a `SubmissionProfile`'s `availableTimeDelta` or `deadTimeDelta` ripples to all related `AssignmentOffering` instances.
   - Reassigning an `Assignment`'s `submissionProfile` updates all its offerings.
3. **Submission Acceptance Tests**:
   - Student with extension submitting during late window vs. student without extension rejected.
   - Submission before `openingDateFor` rejected.
   - Submission after `lateDeadlineFor` rejected.
   - Course staff submissions permitted regardless of date windows.
