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
import java.util.List;
import org.webcat.core.User;
import org.webcat.core.messaging.Message;
import org.webcat.core.messaging.SysAdminMessage;
import org.webcat.grader.AssignmentOffering;
import org.webcat.grader.Submission;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

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
        EOEditingContext ec = editingContext();
        try
        {
            ec.lock();
            this.submission = submission.localInstance(ec);
        }
        finally
        {
            ec.unlock();
        }

        this.attachments = new NSMutableDictionary<String, String>();

        for (String path : attachmentPaths)
        {
            attachments.setObjectForKey(path, new File(path).getName());
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
    public NSDictionary<String, String> attachments()
    {
        return attachments;
    }


    // ----------------------------------------------------------
    @Override
    public synchronized NSArray<User> users()
    {
        EOEditingContext ec = editingContext();
        try
        {
            ec.lock();
            if (submission != null
                && submission.assignmentOffering() != null
                && submission.assignmentOffering().courseOffering() != null)
            {
                return submission.assignmentOffering().courseOffering()
                    .instructors();
            }
            else
            {
                return new NSArray<User>();
            }
        }
        finally
        {
            ec.unlock();
        }
    }


    //~ Static/instance variables .............................................

    private Submission submission;
    private NSMutableDictionary<String, String> attachments;
}
