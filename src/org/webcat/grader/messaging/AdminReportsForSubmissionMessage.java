/*==========================================================================*\
 |  $Id: AdminReportsForSubmissionMessage.java,v 1.4 2011/12/25 21:11:41 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2010-2011 Virginia Tech
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
import org.webcat.grader.AssignmentOffering;
import org.webcat.grader.Submission;

//-------------------------------------------------------------------------
/**
 * A message that is sent to course instructors when a submission generates
 * admin-directed reports.
 *
 * @author  Tony Allevato
 * @author  Last changed by: $Author: stedwar2 $
 * @version $Revision: 1.4 $ $Date: 2011/12/25 21:11:41 $
 */
public class AdminReportsForSubmissionMessage
    extends SubmissionErrorMessage
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public AdminReportsForSubmissionMessage(
        Submission submission, List<File> attachments)
    {
        super(submission, attachments);
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
    public String shortBody()
    {
        return "Reports addressed to the adminstrator are attached.";
    }


    // ----------------------------------------------------------
    @Override
    public String title()
    {
        AssignmentOffering assignment = submission().assignmentOffering();

        return "[Grader] reports: "
            + submission().user().email() + " #"
            + submission().submitNumber()
            + (assignment == null ? "" : (", " + assignment.titleString()));
    }


    // ----------------------------------------------------------
    @Override
    public boolean isSevere()
    {
        return true;
    }
}
