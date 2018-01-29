/*==========================================================================*\
 |  $Id: PickSubmissionDialog.java,v 1.9 2014/06/16 17:28:39 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2012 Virginia Tech
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

import org.apache.log4j.Logger;
import org.webcat.core.WCComponent;
import org.webcat.core.WCComponentWithErrorMessages;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import er.extensions.appserver.ERXDisplayGroup;

//-------------------------------------------------------------------------
/**
 * Allows a grader to choose a different submission to grade than the one that
 * is displayed on the StudentsForAssignment page.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.9 $, $Date: 2014/06/16 17:28:39 $
 */
public class PickSubmissionDialog
	extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public PickSubmissionDialog(WOContext context)
    {
        super(context);
    }


    //~ KVC attributes (must be public) .......................................

    public WCComponent                 nextPageForResultsPage;
    public UserSubmissionPair          rootUserSubmission;
    public NSArray<UserSubmissionPair> allUserSubmissionsForNavigation;
    public ERXDisplayGroup<Submission> submissionDisplayGroup;
    public Submission                  aSubmission;
    public boolean                     sendsToGradingPage;
    public int                         extraColumnCount;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Sets the "root submission" for which this dialog is being invoked, which
     * is the submission that will be used to access all of the other
     * submissions made on this assignment by the user.
     *
     * @param pair the UserSubmissionPair for which the dialog is being invoked
     */
    public void setRootUserSubmission(UserSubmissionPair pair)
    {
        rootUserSubmission = pair;
        collectSubmissions();
        extraColumnCount = 0;
        if (pair != null && pair.submission() != null)
        {
            Assignment a = pair.submission().assignmentOffering().assignment();
            if (a.usesTAScore())
            {
                extraColumnCount++;
            }
            if (a.usesTestingScore())
            {
                extraColumnCount++;
            }
            if (a.usesToolCheckScore())
            {
                extraColumnCount++;
            }
            if (a.usesBonusesOrPenalties())
            {
                extraColumnCount++;
            }
        }
    }


    // ----------------------------------------------------------
    private void collectSubmissions()
    {
        if (rootUserSubmission != null
                && rootUserSubmission != lastRootUserSubmission)
        {
            lastRootUserSubmission = rootUserSubmission;

            NSArray<Submission> submissions =
                rootUserSubmission.submission().allSubmissions();
            // Migrate graded submission, if needed
            Submission graded =
                rootUserSubmission.submission().gradedSubmission();
            if (log.isDebugEnabled())
            {
                log.debug("graded submission = " + graded + " = "
                    + graded.isSubmissionForGradingRaw());
            }
            submissionDisplayGroup.setObjectArray(submissions);

            for (int i = 0; i < submissions.count(); i++)
            {
                if (rootUserSubmission.submission() ==
                        submissions.objectAtIndex(i))
                {
                    submissionDisplayGroup.selectObject(
                            submissions.objectAtIndex(i));
                    break;
                }
            }
        }
    }


    // ----------------------------------------------------------
    public String submitTimeSpanClass()
    {
        if (aSubmission.isLate())
        {
            return "warn";
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    private WCComponentWithErrorMessages errorMessageOnParent(String message)
    {
        WCComponentWithErrorMessages owner = null;
        WOComponent container = parent();
        while (container != null)
        {
            if (container instanceof WCComponentWithErrorMessages)
            {
                owner = (WCComponentWithErrorMessages)container;
            }
            container = container.parent();
        }
        if (owner != null && message != null)
        {
            owner.error(message);
        }
        return owner;
    }


    // ----------------------------------------------------------
    public WOComponent viewSubmission()
    {
        Submission selectedSub = submissionDisplayGroup.selectedObject();

        if (selectedSub == null)
        {
            selectedSub = rootUserSubmission.submission();
        }

        GraderComponent pageToReturn = null;

        prefs().setSubmissionRelationship(selectedSub);

        if (selectedSub == null)
        {
            return errorMessageOnParent("Please choose a submission.");
        }
        else if (!selectedSub.resultIsReady())
        {
            return errorMessageOnParent(
                "Results for that submission are not available.");
        }
        else
        {
            if (sendsToGradingPage)
            {
                GradeStudentSubmissionPage page =
                    pageWithName(GradeStudentSubmissionPage.class);

                if (allUserSubmissionsForNavigation == null)
                {
                    page.availableSubmissions = null;
                    page.thisSubmissionIndex = 0;
                }
                else
                {
                    page.availableSubmissions =
                        allUserSubmissionsForNavigation.immutableClone();
                    page.thisSubmissionIndex =
                        page.availableSubmissions.indexOf(rootUserSubmission);
                }

                page.nextPage = nextPageForResultsPage;

                pageToReturn = page;
            }
            else
            {
                pageToReturn = pageWithName(FinalReportPage.class);
            }
            pageToReturn.reloadGraderPrefs();
            if (nextPageForResultsPage != null)
            {
                pageToReturn.setCurrentTab(
                    nextPageForResultsPage.currentTab());
            }
        }

        return pageToReturn;
    }


    //~ Static/instance variables .............................................

    private UserSubmissionPair lastRootUserSubmission;

    static final Logger log = Logger.getLogger(PickSubmissionDialog.class);
}
