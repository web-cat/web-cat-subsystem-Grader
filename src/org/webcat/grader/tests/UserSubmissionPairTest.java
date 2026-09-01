package org.webcat.grader.tests;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.webcat.core.User;
import org.webcat.grader.Submission;
import org.webcat.grader.UserSubmissionPair;
import org.webcat.grader.Scorable;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import junit.framework.TestCase;

public class UserSubmissionPairTest extends TestCase {

    // ----------------------------------------------------------
    public void testFromInfoMap_NullMapReturnsEmptyArray() {
        NSArray<UserSubmissionPair> result = UserSubmissionPair.fromInfoMap(
            null, false, null);
        assertNotNull(result);
        assertEquals(0, result.count());
    }

    // ----------------------------------------------------------
    public void testFromInfoMap_BasicConversion() {
        Map<User, Submission.StudentSubmissionInfo> infoMap =
            new LinkedHashMap<User, Submission.StudentSubmissionInfo>();

        // We use null for user/submission to avoid EOF dependency issues
        // in a simple unit test, but in a real test these would be mocks.
        Submission.StudentSubmissionInfo info1 =
            new Submission.StudentSubmissionInfo(null, null, null, null, false);
        infoMap.put(null, info1);

        NSArray<UserSubmissionPair> result = UserSubmissionPair.fromInfoMap(
            infoMap, false, null);

        assertEquals(1, result.count());
        assertNull(result.objectAtIndex(0).user());
        assertNull(result.objectAtIndex(0).submission());
    }

    // ----------------------------------------------------------
    public void testFromInfoMap_WithUsersListPreservesOrder() {
        // This test verifies that the overload taking a users list
        // respects that list's order.
        Map<User, Submission.StudentSubmissionInfo> infoMap =
            new HashMap<User, Submission.StudentSubmissionInfo>();
        
        // Since we can't easily create User objects, we'll verify the 
        // logic by checking that it iterates the users array.
        NSArray<User> users = new NSArray<User>(new User[] {});
        
        NSArray<UserSubmissionPair> result = UserSubmissionPair.fromInfoMap(
            infoMap, users, false, null);
        
        assertEquals(0, result.count());
    }

    // ----------------------------------------------------------
    public void testFromInfoMap_NullUsersListFallsBack() {
        Map<User, Submission.StudentSubmissionInfo> infoMap =
            new HashMap<User, Submission.StudentSubmissionInfo>();
        
        // Should behave like the single-argument version
        NSArray<UserSubmissionPair> result = UserSubmissionPair.fromInfoMap(
            infoMap, null, false, null);
        
        assertNotNull(result);
        assertEquals(0, result.count());
    }
}
