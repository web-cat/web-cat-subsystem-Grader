/*==========================================================================*\
 |  $Id: EnqueuedJob.java,v 1.4 2014/11/07 13:55:03 stedwar2 Exp $
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

package org.webcat.grader;

import org.webcat.core.User;
import com.webobjects.foundation.NSTimestamp;
import er.extensions.eof.ERXKey;

// -------------------------------------------------------------------------
/**
 * This class represents the database record of a student file submission
 * enqueued for compilation/processing but not yet handled.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.4 $, $Date: 2014/11/07 13:55:03 $
 */
public class EnqueuedJob
    extends _EnqueuedJob
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new EnqueuedJob object.
     */
    public EnqueuedJob()
    {
        super();
    }


    //~ Constants (for key names) .............................................

    public static final String ASSIGNMENT_OFFERING_KEY =
        SUBMISSION_KEY
        + "." + Submission.ASSIGNMENT_OFFERING_KEY;
    public static final ERXKey<AssignmentOffering> assignmentOffering =
        new ERXKey<AssignmentOffering>(ASSIGNMENT_OFFERING_KEY);

    public static final String SUBMIT_TIME_KEY =
        SUBMISSION_KEY
        + "." + Submission.SUBMIT_TIME_KEY;
    public static final ERXKey<NSTimestamp> submitTime =
        new ERXKey<NSTimestamp>(SUBMIT_TIME_KEY);

    public static final String USER_KEY =
        SUBMISSION_KEY
        + "." + Submission.USER_KEY;
    public static final ERXKey<User> user = new ERXKey<User>(USER_KEY);


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where this submission is stored.
     * @return the directory name
     */
    public String workingDirName()
    {
        StringBuffer dir = new StringBuffer(100);
        dir.append(org.webcat.core.Application
            .configurationProperties().getProperty("grader.workarea"));
        dir.append('/');
        dir.append(submission().user().authenticationDomain().subdirName());
        dir.append('/');
        dir.append(submission().user().userName());
        dir.append('.');
        dir.append(submission().id());
        return dir.toString();
    }


    // ----------------------------------------------------------
    public String userPresentableDescription()
    {
        Submission sub = submission();
        if ( sub != null )
        {
            return submission().toString() + "("
                + ( paused() ? "paused" : "ready" ) + ")";
        }
        else
        {
            return "job with <null> submission";
        }
    }
}
