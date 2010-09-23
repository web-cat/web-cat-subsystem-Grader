/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2009 Virginia Tech
 |
 |  This file is part of Web-CAT.
 |
 |  Web-CAT is free software; you can redistribute it and/or modify
 |  it under the terms of the GNU Affero General Public License as published
 |  by the Free Software Foundation; either version 3 of the License, or
 |  (at your option) any later version.
 |
 |  Web-CAT is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU General Public License for more details.
 |
 |  You should have received a copy of the GNU Affero General Public License
 |  along with Web-CAT; if not, see <http://www.gnu.org/licenses/>.
\*==========================================================================*/

package org.webcat.grader.messaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.webcat.core.User;
import org.webcat.core.messaging.Message;
import org.webcat.grader.Submission;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

//-------------------------------------------------------------------------
/**
 * A message that is sent to course instructors when grading of a single
 * submission is suspended due to a technical fault.
 *
 * @author Tony Allevato
 * @author  latest changes by: $Author$
 * @version $Revision$ $Date$
 */
public class SubmissionSuspendedMessage extends Message
{
    //~ Constructors ..........................................................

    public SubmissionSuspendedMessage(Submission submission, Exception e,
            String stage, File attachmentsDir)
    {
        this.submission = submission;
        this.exception = e;
        this.stage = stage;

        if (attachmentsDir != null && attachmentsDir.exists())
        {
            this.attachments = new NSMutableDictionary<String, NSData>();

            File[] fileList = attachmentsDir.listFiles();

            for (File file : fileList)
            {
                if (!file.isDirectory())
                {
                    addAttachment(file);
                }
            }
        }
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Called by the subsystem init() to register the message.
     */
    public static void register()
    {
        Message.registerMessage(
                SubmissionSuspendedMessage.class,
                "Grader",
                "Submission Suspended",
                false,
                User.INSTRUCTOR_PRIVILEGES);
    }


    // ----------------------------------------------------------
    @Override
    public String fullBody()
    {
        return shortBody();
    }


    // ----------------------------------------------------------
    @Override
    public String shortBody()
    {
        String errorMsg = "An " + ((exception == null) ? "error": "exception")
                + " occurred " + stage;

        if (exception != null)
        {
            errorMsg += ":\n" + exception;
        }

        errorMsg += "\n\nGrading of this submission has been suspended.\n";

        return errorMsg;
    }


    // ----------------------------------------------------------
    @Override
    public String title()
    {
        String username = "<no user>";
        String submitNumber = "<no submit number>";

        if (submission != null)
        {
            if (submission.user() != null)
            {
                username = submission.user().userName();
            }

            if (submission.submitNumberRaw() != null)
            {
                submitNumber = Integer.toString(submission.submitNumber());
            }
        }

        return "[Grader] Grading error: "
            + username + " #" + submitNumber;
    }


    // ----------------------------------------------------------
    @Override
    public NSArray<User> users()
    {
        if (submission != null && submission.assignmentOffering() != null &&
                submission.assignmentOffering().courseOffering() != null)
        {
            return submission.assignmentOffering().courseOffering().instructors();
        }
        else
        {
            return new NSArray<User>();
        }
    }


    // ----------------------------------------------------------
    @Override
    public NSDictionary<String, NSData> attachments()
    {
        return attachments;
    }


    // ----------------------------------------------------------
    private void addAttachment(File file)
    {
        FileInputStream stream = null;

        try
        {
            stream = new FileInputStream(file);
            NSData data = new NSData(stream, 0);

            attachments.setObjectForKey(data, file.getName());
        }
        catch (IOException e)
        {
            // Do nothing.
        }
        finally
        {
            try
            {
                if (stream != null)
                {
                    stream.close();
                }
            }
            catch (IOException e)
            {
                // Do nothing.
            }
        }
    }


    //~ Static/instance variables .............................................

    private Submission submission;
    private Exception exception;
    private String stage;
    private NSMutableDictionary<String, NSData> attachments;
}
