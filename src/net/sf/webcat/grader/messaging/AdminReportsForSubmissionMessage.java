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

package net.sf.webcat.grader.messaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;
import net.sf.webcat.core.User;
import net.sf.webcat.core.messaging.Message;
import net.sf.webcat.core.messaging.SysAdminMessage;
import net.sf.webcat.core.messaging.UnexpectedExceptionMessage;
import net.sf.webcat.grader.Assignment;
import net.sf.webcat.grader.AssignmentOffering;
import net.sf.webcat.grader.Submission;

//-------------------------------------------------------------------------
/**
 * A message that is sent to course instructors when a submission generates
 * admin-directed reports.
 *
 * @author Tony Allevato
 * @author  latest changes by: $Author$
 * @version $Revision$ $Date$
 */
public class AdminReportsForSubmissionMessage extends SysAdminMessage
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public AdminReportsForSubmissionMessage(Submission submission,
            List<String> attachmentPaths)
    {
        this.submission = submission;

        this.attachments = new NSMutableDictionary<String, NSData>();

        for (String path : attachmentPaths)
        {
            File file = new File(path);
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
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Called by the subsystem init() to register the message.
     */
    public static void register()
    {
        Message.registerMessage(
                AdminReportsForSubmissionMessage.class,
                "Grader",
                "Admin Reports for Submission",
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
        return "Reports addressed to the adminstrator are attached.";
    }


    // ----------------------------------------------------------
    @Override
    public String title()
    {
        AssignmentOffering assignment = submission.assignmentOffering();

        return "[Grader] reports: "
            + submission.user().email() + " #"
            + submission.submitNumber()
            + (assignment == null ? "" : (", " + assignment.titleString()));
    }


    // ----------------------------------------------------------
    @Override
    public NSDictionary<String, NSData> attachments()
    {
        return attachments;
    }


    // ----------------------------------------------------------
    @Override
    public NSArray<User> users()
    {
        return submission.assignmentOffering().courseOffering().instructors();
    }


    //~ Static/instance variables .............................................

    private Submission submission;
    private NSMutableDictionary<String, NSData> attachments;
}
