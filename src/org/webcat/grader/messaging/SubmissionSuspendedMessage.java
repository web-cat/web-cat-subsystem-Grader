/*==========================================================================*\
 |  $Id$
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
import org.webcat.core.User;
import org.webcat.core.messaging.Message;
import org.webcat.grader.Submission;

//-------------------------------------------------------------------------
/**
 * A message that is sent to course instructors when grading of a single
 * submission is suspended due to a technical fault.
 *
 * @author  Tony Allevato
 * @author  Last changed by: $Author$
 * @version $Revision$ $Date$
 */
public class SubmissionSuspendedMessage
    extends SubmissionErrorMessage
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Create a new message.
     * @param submission The submission that caused the problem.
     * @param e          The exception that occurred.
     * @param stage      In what stage of the grading action did the exception
     *                   occur.
     * @param attachmentsDir A directory of files to attach to the message
     *                       (normally the submission's result dir).
     */
    public SubmissionSuspendedMessage(
        Submission submission,
        Exception  e,
        String     stage,
        File       attachmentsDir)
    {
        super(submission, attachmentsDir);
        this.exception = e;
        this.stage = stage;
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

        if (submission() != null)
        {
            if (submission().user() != null)
            {
                username = submission().user().userName();
            }

            if (submission().submitNumberRaw() != null)
            {
                submitNumber = Integer.toString(submission().submitNumber());
            }
        }

        return "[Grader] Grading error: " + username + " #" + submitNumber;
    }


    //~ Static/instance variables .............................................

    private Exception exception;
    private String stage;
}
