To eliminate contention for the submission number and prevent duplicate submissions from IDE clients or the web interface, you can implement a **multi-layered defense-in-depth approach**.

Java 1.8 features (such as `ReentrantLock`, `ConcurrentHashMap.newKeySet()`, lambdas, and diamond operators) are utilized below.

---

### Solution 1: Eliminate Contention for `submitNumber` (Striped In-Memory Locking)

Currently, [Grader.java:588–599](file:///Users/Shared/Web-CAT/Grader/src/org/webcat/grader/Grader.java#L588-L599) queries `Submission.submissionsForAssignmentOfferingAndUser` in an isolated editing context and computes `currentSubNo = max + 1`. When two or more requests arrive concurrently, they all see the same maximum and assign the identical submission number.

#### Why NOT Remove Locks from a Map?
A common temptation is to use a `ConcurrentHashMap<String, Lock>` where a thread inserts a lock, processes the request, and removes the lock in a `finally` block. However, **removing locks from a map creates a fatal race condition**:
1. **Thread 1** arrives, creates `Lock_A` in the map, and begins processing.
2. **Thread 2** arrives for the same student/assignment, finds `Lock_A`, and blocks waiting for Thread 1.
3. **Thread 1** finishes, releases `Lock_A`, and executes `map.remove(key)`.
4. **Thread 2** wakes up, acquires `Lock_A`, and enters its critical section.
5. **Thread 3** arrives now. It checks the map, but the key was removed in step 3! Thread 3 creates a brand new `Lock_B`, acquires it immediately, and enters the critical section **concurrently with Thread 2**.

#### The Fix: Striped Locking
Instead of dynamically allocating and removing locks, use **lock striping** (a fixed array of reusable locks indexed by key hash). 

- **No Map Cleanup Race:** Lock objects are permanent; Thread 3 will hash to the exact same lock as Thread 2 and wait its turn.
- **High Concurrency:** Submissions for different students/assignments hash to different stripes (out of 128) and run in parallel without contention.
- **Zero Allocation & No Memory Leak:** No objects are created or destroyed per request, eliminating garbage collection overhead.

#### Implementation in `Grader.java`:

```java
import java.util.concurrent.locks.ReentrantLock;

// Fixed pool of 128 striped locks (power of two for fast bitmask distribution)
private static final int SUBMISSION_STRIPE_COUNT = 128;
private static final ReentrantLock[] SUBMISSION_LOCKS = new ReentrantLock[SUBMISSION_STRIPE_COUNT];

static
{
    for (int i = 0; i < SUBMISSION_STRIPE_COUNT; i++)
    {
        SUBMISSION_LOCKS[i] = new ReentrantLock();
    }
}

private static ReentrantLock getSubmissionLock(String key)
{
    int h = key.hashCode();
    // Java 8 hash spreader (same technique used in HashMap/ConcurrentHashMap)
    h = h ^ (h >>> 16);
    return SUBMISSION_LOCKS[h & (SUBMISSION_STRIPE_COUNT - 1)];
}
```

Then in `handleSubmission`:

```java
String lockKey = localizedUser.id() + ":" + assignment.id();
ReentrantLock lock = getSubmissionLock(lockKey);

lock.lock();
try
{
    // 1. Re-query the latest submission number inside the locked critical section
    NSArray<Submission> submissions =
        Submission.submissionsForAssignmentOfferingAndUser(
            ec, assignment, result.user());
    int currentSubNo = submissions.count() + 1;
    for (int i = 0; i < submissions.count(); i++)
    {
        int sno = submissions.objectAtIndex(i).submitNumber();
        if (sno >= currentSubNo)
        {
            currentSubNo = sno + 1;
        }
    }

    // 2. Check max submissions policy inside lock
    Number maxSubmissions = assignment.assignment().submissionProfile()
        .maxSubmissionsRaw();
    if (maxSubmissions != null
        && currentSubNo > maxSubmissions.intValue()
        && !assignment.courseOffering().isStaff(session.user()))
    {
        String msg = "You have exceeded the allowable number "
            + "of submissions for this assignment.";
        result.errorMessages.add(msg);
        log.warn(msg + "  User = " + session.user() + "\n\t" + assignment);
        return result.generateResponse();
    }

    // 3. Start submission and set attributes
    result.partnersNotFound = partnersNotFound;
    result.startSubmission(currentSubNo, result.user(), assignment);
    result.submissionInProcess().setPartners(partners);
    result.submissionInProcess().setUploadedFile(file);
    result.submissionInProcess().setUploadedFileName(fileName);

    // 4. Validate file length
    int len = 0;
    try
    {
        len = file.length();
    }
    catch (Exception e)
    {
        // Ignore NPE on invalid POST
    }
    if (len == 0)
    {
        result.clearSubmission();
        result.submissionInProcess().clearUpload();
        String msg = "Your file submission is empty. Please choose an appropriate file.";
        result.errorMessages.add(msg);
        log.warn(msg + "  User = " + session.user() + "\n\t" + assignment);
        return result.generateResponse();
    }
    else if (len > assignment.assignment().submissionProfile().effectiveMaxFileUploadSize())
    {
        result.clearSubmission();
        result.submissionInProcess().clearUpload();
        String msg = "Your file exceeds the file size limit for this assignment ("
            + assignment.assignment().submissionProfile().effectiveMaxFileUploadSize()
            + "). Please choose a smaller file.";
        result.errorMessages.add(msg);
        log.warn(msg + "  User = " + session.user() + "\n\t" + assignment);
        return result.generateResponse();
    }

    // 5. Commit submission and enqueue grading job
    try
    {
        String msg = result.commitSubmission(context, currentTime);
        if (msg != null)
        {
            log.warn(msg + "  User = " + session.user() + "\n\t" + assignment);
            result.errorMessages.add(msg);
        }
    }
    catch (Exception e)
    {
        new UnexpectedExceptionMessage(e, context, null, null).send();
        result.errorMessages.add("An unexpected error occurred while processing your submission.");
    }

    return result.generateResponse();
}
finally
{
    lock.unlock();
}
```

---

### Solution 2: Eliminate Duplicate Requests with Identical Content (Payload Hashing)

If Request 2 has the **exact same file content** as Request 1 and arrived within a short deduplication window (e.g., 30 seconds), it should be safely acknowledged rather than creating submission #8.

#### Implementation: Content Hashing & Self-Cleaning Idempotency Cache
In `Grader.java`:

```java
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

private static class RecentSubmission
{
    final String hash;
    final int submitNumber;
    final long timestamp;

    RecentSubmission(String hash, int submitNumber, long timestamp)
    {
        this.hash = hash;
        this.submitNumber = submitNumber;
        this.timestamp = timestamp;
    }
}

// Cache of (userId:assignmentId) -> RecentSubmission
private static final ConcurrentHashMap<String, RecentSubmission> recentSubmissions =
    new ConcurrentHashMap<>();

private static String computeSHA256(NSData data)
{
    try
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.bytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash)
        {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
    catch (Exception e)
    {
        return null;
    }
}
```

Inside the striped lock in `handleSubmission`:

```java
String payloadHash = computeSHA256(file);
long now = System.currentTimeMillis();

// Optional: Java 8 self-cleaning of entries older than 60 seconds
recentSubmissions.entrySet().removeIf(entry -> (now - entry.getValue().timestamp) > 60_000L);

RecentSubmission recent = recentSubmissions.get(lockKey);

// If identical payload arrived within the last 30 seconds:
if (recent != null 
    && (now - recent.timestamp) < 30_000L 
    && recent.hash != null 
    && recent.hash.equals(payloadHash))
{
    log.info("Discarding duplicate submission from " + localizedUser.userName() 
        + " for " + assignment.assignment().name() 
        + "; identical payload to submission #" + recent.submitNumber);

    // Return the response pointing to the already-committed submission
    result.clearSubmission();
    result.submissionInProcess().clearUpload();
    return result.generateResponse();
}

// Otherwise, proceed to commit and record in cache upon success:
String msg = result.commitSubmission(context, currentTime);
if (msg == null)
{
    recentSubmissions.put(lockKey, new RecentSubmission(payloadHash, currentSubNo, now));
}
```

---

### Solution 3 (Optional): Database Unique Constraint (Hard Safety Net)

To ensure the database can never be corrupted even if multiple application servers are clustered together, add a unique key constraint to MariaDB/MySQL:

```sql
ALTER TABLE TSUBMISSION 
ADD UNIQUE KEY uq_assignment_user_submitno (CASSIGNMENTID, CUSERID, CSUBMITNUMBER);
```

If any unexpected race condition ever bypassed the application locks, the database will reject the duplicate insert with an `IntegrityConstraintViolationException` rather than creating a duplicate row with file collisions.

---

### Solution 4 (Optional): Queue-Level Guard in `GraderQueueProcessor`

As a final failsafe, worker threads in [GraderQueueProcessor.java](file:///Users/Shared/Web-CAT/Grader/src/org/webcat/grader/GraderQueueProcessor.java) should **never process the same submission concurrently**:

Maintain a thread-safe set of active submission IDs in `GraderQueueProcessor` using Java 8 `ConcurrentHashMap.newKeySet()`:

```java
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

private static final Set<EOGlobalID> activeSubmissions = ConcurrentHashMap.newKeySet();
```

In `GraderQueueProcessor.action()`:

```java
EOGlobalID subId = submission.permanentGlobalID();
if (!activeSubmissions.add(subId))
{
    log.warn(getName() + ": submission " + submission 
        + " is already being processed by another worker thread. Deleting duplicate job.");
    job.delete();
    ec.saveChanges();
    return;
}

try
{
    processJobWithProtection(job);
}
finally
{
    activeSubmissions.remove(subId);
}
```

This guarantees that even if duplicate jobs are enqueued or restored from the database on startup, only **one** worker thread can process that submission at a time, preventing filesystem collisions and thread pool deadlocks.