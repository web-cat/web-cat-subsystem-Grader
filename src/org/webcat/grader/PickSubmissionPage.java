/*==========================================================================*\
 |  $Id: PickSubmissionPage.java,v 1.4 2014/06/16 17:28:27 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2010 Virginia Tech
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
import com.webobjects.foundation.*;
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 * This class presents the list of previous submissions for the selected
 * assignment so that one submission can be chosen.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.4 $, $Date: 2014/06/16 17:28:27 $
 */
public class PickSubmissionPage
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public PickSubmissionPage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup submissionDisplayGroup;
    public Submission     aSubmission;
    public int            index;

    /** index of selected submission. */
    public int selectedIndex;

    /** true if previous submissions exist */
    public boolean previousSubmissions;

    public boolean showCourseOffering = false;
    public String sideStepTitle;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug( "entering appendToResponse()" );
        selectedIndex = -1;
        User user = user();
        // Why is this here?
//        if ( prefs().submission() != null )
//        {
//            user = prefs().submission().user();
//        }
        previousSubmissions = false;
        if (prefs().assignmentOffering() != null)
        {
            submissions =
                Submission.submissionsForAssignmentOfferingAndUserDescending(
                    localContext(), prefs().assignmentOffering(), user);
            submissionDisplayGroup.setObjectArray(submissions);
            previousSubmissions = (submissions.count() > 0);
        }
        if (prefs().assignment() != null
            && !previousSubmissions
            && coreSelections().semester() != null)
        {
            submissions =
                Submission.submissionsForAssignmentAndUserDescending(
                    localContext(),
                    prefs().assignment(),
                    coreSelections().semester(),
                    user);
            submissionDisplayGroup.setObjectArray(submissions);
            previousSubmissions = (submissions.count() > 0);
        }
        if ( !previousSubmissions )
        {
            error(
                "You have not completed any submissions for this assignment.");
        }
        if (prefs().submission() != null)
        {
            log.debug( "Currently has a submission chosen" );
            int idx = submissions.indexOfIdenticalObject(prefs().submission());
            if (idx == NSArray.NotFound)
            {
                log.debug("Invalid submission being cleared");
                prefs().setSubmissionRelationship(null);
            }
            else
            {
                selectedIndex = idx;
                log.debug("Attempting to select submission = "
                           + selectedIndex);
            }
        }
        if (prefs().submission() == null &&
            submissionDisplayGroup.displayedObjects().count() > 0)
        {
            selectedIndex = 0;
            prefs().setSubmissionRelationship(
                    (Submission)submissionDisplayGroup.displayedObjects().
                        objectAtIndex(selectedIndex));
            log.debug(
                "No selection; selecting index "
                + selectedIndex
                + ", sub #"
                + prefs().submission().submitNumber());
        }
        log.debug("calling super.appendToResponse()");
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    protected void afterAppendToResponse(WOResponse response, WOContext context)
    {
        super.afterAppendToResponse(response, context);
        oldBatchSize  = submissionDisplayGroup.numberOfObjectsPerBatch();
        oldBatchIndex = submissionDisplayGroup.currentBatchIndex();
        log.debug("leaving appendToResponse()");
    }


    // ----------------------------------------------------------
    /* Checks for errors, then records the currently selected item.
     *
     * @returns true if no errors are present
     */
    protected boolean saveSelectionCanContinue()
    {
        if (selectedIndex < 0)
        {
            log.debug("saveSelectionCanContinue(): no selected "
                + "submission, no index");
            error("Please choose a submission.");
        }
        else if (selectedIndex >= 0)
        {
            prefs().setSubmissionRelationship(
                    (Submission)submissionDisplayGroup.displayedObjects().
                        objectAtIndex(selectedIndex));
            log.debug(
                "Changing selection; selecting index "
                + selectedIndex
                + ", sub #"
                + prefs().submission().submitNumber());
        }
        if (prefs().submission() == null)
        {
            log.warn("saveSelectionCanContinue(): null submission!");
            error("Please choose a submission.");
        }
        else if (!prefs().submission().resultIsReady())
        {
            error("The Grader has not yet completed processing "
                + "on that submission.");
        }
        return !hasMessages();
    }


    // ----------------------------------------------------------
    public boolean hasResult()
    {
        boolean result = (aSubmission.resultIsReady());
        log.debug("hasResult() = " + result);
        return result;
    }


    // ----------------------------------------------------------
    public String submissionStatus()
    {
        String result = "suspended";
        EnqueuedJob job = aSubmission.enqueuedJob();
        if (job == null)
        {
            result = "cancelled";
        }
        else if (!job.paused())
        {
            result = "queued for grading";
        }
        log.debug("submissionStatus() = " + result);
        return result;
    }


    // ----------------------------------------------------------
    public boolean selectingForDifferentUser()
    {
        boolean result = false;
        if (prefs().submission() != null)
        {
            result = user() != prefs().submission().user();
        }
        return result;
    }


    // ----------------------------------------------------------
    public boolean nextEnabled()
    {
        return previousSubmissions  &&  super.nextEnabled();
    }


    // ----------------------------------------------------------
    public WOComponent selectSubmission()
    {
        selectedIndex = index;
        return next();
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        WOComponent result = null;
        if (saveSelectionCanContinue())
        {
            result = super.next();
            if (result instanceof GraderComponent)
            {
                ((GraderComponent)result).reloadGraderPrefs();
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        log.debug( "defaultAction()" );
        if (oldBatchSize != submissionDisplayGroup.numberOfObjectsPerBatch()
            || oldBatchIndex != submissionDisplayGroup.currentBatchIndex())
        {
            return null;
        }
        else
        {
            return super.defaultAction();
        }
    }


    //~ Instance/static variables .............................................

    protected NSArray<Submission> submissions;
    protected int                 oldBatchSize;
    protected int                 oldBatchIndex;

    static Logger log = Logger.getLogger(PickSubmissionPage.class);
}
