package org.webcat.grader;

import java.util.Map;
import org.webcat.core.User;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import er.extensions.eof.ERXKey;

public class UserSubmissionPair implements NSKeyValueCodingAdditions
{
    //~ Static methods ........................................................

    // ----------------------------------------------------------
    /**
     * Creates an array of UserSubmissionPair objects from a map of submission
     * info.
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

        NSMutableArray<UserSubmissionPair> results =
            new NSMutableArray<UserSubmissionPair>(infoMap.size());

        for (Submission.StudentSubmissionInfo info : infoMap.values())
        {
            Submission sub = info.gradedSubmission;
            if (omitPartners && sub != null && sub.partnerLink())
            {
                continue;
            }

            results.addObject(new UserSubmissionPair(info.user, sub));

            if (accumulator != null && sub != null && sub.result() != null)
            {
                accumulator.accumulate(sub.result());
            }
        }

        return results;
    }


    // ----------------------------------------------------------
    /**
     * Creates an array of UserSubmissionPair objects from a map of submission
     * info, preserving the order of the provided users list.
     *
     * @param infoMap the map of user submission information
     * @param users the list of users to include, in the desired order
     * @param omitPartners true if partner links should be excluded
     * @param accumulator optional stats accumulator
     * @return an array of pairs
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
            Submission sub = (info != null) ? info.gradedSubmission : null;

            if (omitPartners && sub != null && sub.partnerLink())
            {
                continue;
            }

            results.addObject(new UserSubmissionPair(u, sub));

            if (accumulator != null && sub != null && sub.result() != null)
            {
                accumulator.accumulate(sub.result());
            }
        }

        return results;
    }


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public UserSubmissionPair(User aUser, Submission aSubmission)
    {
        this._user = aUser;
        this._submission = aSubmission;
    }


    //~ Public constants ......................................................

    public static final ERXKey<User> user =
        new ERXKey<User>("user");
    public static final ERXKey<Submission> submission =
        new ERXKey<Submission>("submission");


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public User user()
    {
        return _user;
    }


    // ----------------------------------------------------------
    public Submission submission()
    {
        return _submission;
    }


    // ----------------------------------------------------------
    public boolean userHasSubmission()
    {
        return _submission != null;
    }


    // ----------------------------------------------------------
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<" + user().toString() + ", ");

        if (submission() != null)
        {
            buffer.append(submission().toString());
        }
        else
        {
            buffer.append("no submission");
        }

        buffer.append(">");
        return buffer.toString();
    }


    // ----------------------------------------------------------
    public boolean equals(Object object)
    {
        if (object instanceof UserSubmissionPair)
        {
            UserSubmissionPair otherPair = (UserSubmissionPair) object;

            return (otherPair.user() == user()
                    && otherPair.submission() == submission());
        }
        else
        {
            return false;
        }
    }


    //~ KVC implementation ....................................................

    // ----------------------------------------------------------
    public void takeValueForKeyPath(Object value, String keyPath)
    {
        NSKeyValueCodingAdditions.DefaultImplementation.takeValueForKeyPath(
                this, value, keyPath);
    }


    // ----------------------------------------------------------
    public Object valueForKeyPath(String keyPath)
    {
        return NSKeyValueCodingAdditions.DefaultImplementation.valueForKeyPath(
                this, keyPath);
    }


    // ----------------------------------------------------------
    public void takeValueForKey(Object value, String key)
    {
        NSKeyValueCoding.DefaultImplementation.takeValueForKey(
                this, value, key);
    }


    // ----------------------------------------------------------
    public Object valueForKey(String key)
    {
        return NSKeyValueCoding.DefaultImplementation.valueForKey(this, key);
    }


    //~ Static/instance variables .............................................

    private User _user;
    private Submission _submission;
}
