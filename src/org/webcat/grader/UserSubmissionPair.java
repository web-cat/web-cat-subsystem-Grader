package org.webcat.grader;

import org.webcat.core.User;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import er.extensions.eof.ERXKey;

public class UserSubmissionPair implements NSKeyValueCodingAdditions
{
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
