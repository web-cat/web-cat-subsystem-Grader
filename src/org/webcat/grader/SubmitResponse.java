/*==========================================================================*\
 |  $Id: SubmitResponse.java,v 1.5 2011/01/20 18:43:10 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2008 Virginia Tech
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

import com.webobjects.appserver.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 *  This is the response page for BlueJ submission actions.  It is produced
 *  by {@link Grader#handleDirectAction(WORequest,Session,WOContext)}
 *  in response to a BlueJ submitter direct action transaction.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.5 $, $Date: 2011/01/20 18:43:10 $
 */
public class SubmitResponse
    extends GraderSubmissionUploadComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new SubmitResponse object.
     * @param context the context for the request
     */
    public SubmitResponse(WOContext context)
    {
        super(context);
        log.debug("constructor");
    }


    //~ KVC attributes (must be public) .......................................

    public String aPartnerNotFound;
    public String  sessionID;
    public boolean criticalError = false;
    public boolean assignmentClosed = false;
    public boolean noEnergy = false;
    public NSArray<String> partnersNotFound;
    public NSMutableArray<String> errorMessages = new NSMutableArray<String>();
    public String aMessage;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Check whether there is a non-empty error message.
     * @return true if an error has occurred
     */
    public boolean error()
    {
        boolean result = errorMessages != null && errorMessages.count() > 0;
        log.debug("error() = " + result);
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Check whether the current submission has been paused by
     * the grading queue because of a submission-specific failure.
     *
     * @return true if this submission has been paused/deferred
     */
    public boolean gradingPaused()
    {
        if (prefs() == null || prefs().submission() == null)
            return false;
        EnqueuedJob job = prefs().submission().enqueuedJob();
        return job != null  &&  job.paused();
    }


    // ----------------------------------------------------------
    /**
     * Check to see if Web-CAT is accepting submissions for this
     * assignment.
     *
     * @return true if submissions are closed.
     */
    public boolean notAcceptingSubmissions()
    {
        boolean result = assignmentClosed;
        if (result)
        {
            if (error())
            {
                errorMessages.add("This assignment is not open for submission.");
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    public NSTimestamp timeOfNextCharge()
    {
        try
        {
            return prefs().assignmentOffering()
                .energyBarForUser(user()).timeOfNextCharge();
        }
        catch (Exception e)
        {
            log.error(e);
        }
        return null;
    }


    // ----------------------------------------------------------
    public boolean noEnergy()
    {
        boolean result = this.noEnergy;
        if (result)
        {
            if (error())
            {
                errorMessages.add("You do not have any submission energy.  "
                    + "Wait for it to regenerate.");
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Returns the URL for the direct action to view the results.
     * @return the URL as a string
     */
    public String resultsURL()
    {
        String dest = Application.completeURLWithRequestHandlerKey(
                context(),
                "wa",
                "report",
                "wosid=" + sessionID,
                false,
                0);
        log.debug("link = " + dest);
        return dest;
    }


    // ----------------------------------------------------------
    public boolean shouldRefreshImmediately()
    {
        boolean werePartnersNotFound =
            (partnersNotFound != null && partnersNotFound.count() > 0);

        return !criticalError
            && !error()
            && !notAcceptingSubmissions()
            && !gradingPaused()
            && !prefs().assignmentOffering().gradingSuspended()
            && !werePartnersNotFound;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger(SubmitResponse.class);
}
