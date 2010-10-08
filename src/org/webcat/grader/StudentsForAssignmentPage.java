/*==========================================================================*\
 |  $Id$
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
import er.extensions.appserver.ERXDisplayGroup;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 * Show an overview of class grades for an assignment, and allow the user
 * to download them in spreadsheet form or edit them one at a time.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class StudentsForAssignmentPage
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public StudentsForAssignmentPage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public ERXDisplayGroup<Submission> submissionDisplayGroup;
    public ERXDisplayGroup<Submission> staffSubmissionDisplayGroup;
    /** Submission in the worepetition */
    public Submission  aSubmission;
    public Submission  partnerSubmission;
    /** index in the student worepetition */
    public int         index;
    /** index in the staff worepetition */
    public int         staffIndex;

    public AssignmentOffering assignmentOffering;


    /** Value of the corresponding checkbox on the page. */
    public boolean omitStaff           = true;
    public boolean useBlackboardFormat = true;
    public Submission.CumulativeStats studentStats;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug("\n\nappendToResponse()");

        if (assignmentOffering == null)
        {
            assignmentOffering = prefs().assignmentOffering();
            if (assignmentOffering == null)
            {
                Assignment assignment = prefs().assignment();
                CourseOffering courseOffering =
                    coreSelections().courseOffering();
                assignmentOffering = AssignmentOffering
                    .firstObjectMatchingValues(
                        localContext(),
                        null,
                        AssignmentOffering.COURSE_OFFERING_KEY,
                        courseOffering,
                        AssignmentOffering.ASSIGNMENT_KEY,
                        assignment);
                prefs().setAssignmentOfferingRelationship(assignmentOffering);
            }
        }

        studentStats = new Submission.CumulativeStats();
        NSArray<Submission> submissions = Submission.submissionsForGrading(
            localContext(),
            assignmentOffering,
            true,  // omitPartners
            omitStaff,
            studentStats);

        submissionDisplayGroup.setObjectArray(submissions);
        if (log.isDebugEnabled())
        {
            log.debug("Found " + submissions.count() + " submissions:");
            for (Submission sub : submissions)
            {
                log.debug("    "
                    + sub.user()
                    + " # "
                    + sub.submitNumber()
                    + " "
                    + sub.hashCode()
                    + ", "
                    + sub.partnerLink()
                    + ", pri = "
                    + (sub.primarySubmission() == null
                        ? null
                        : sub.primarySubmission().hashCode())
                    + ", "
                    + sub);
            }
        }
        staffSubmissionDisplayGroup.setObjectArray(
            Submission.submissionsForGrading(
                localContext(),
                assignmentOffering,
                true,  // omitPartners
                assignmentOffering.courseOffering().staff(),
                null));
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public WOComponent editSubmissionScore()
    {
        WCComponent destination = null;
        if (!hasMessages())
        {
            if (aSubmission == null)
            {
                log.error("editSubmissionScore(): null submission!");
            }
            else if (aSubmission.result() == null)
            {
                log.error("editSubmissionScore(): null submission result!");
                log.error("student = " + aSubmission.user().userName());
            }
            prefs().setSubmissionRelationship(aSubmission);
//          destination = pageWithName(GradeStudentSubmissionPage.class);
            destination = (WCComponent)super.next();
            if (destination instanceof GradeStudentSubmissionPage)
            {
                GradeStudentSubmissionPage page =
                    (GradeStudentSubmissionPage)destination;
                page.availableSubmissions =
                    submissionDisplayGroup.displayedObjects().immutableClone();
                page.thisSubmissionIndex =
                    page.availableSubmissions.indexOf(aSubmission);
            }
            destination.nextPage = this;
        }
        return destination;
    }


    // ----------------------------------------------------------
    /**
     * Marks all the submissions shown that have been partially graded as
     * being completed, sending e-mail notifications as necessary.
     * @return null to force this page to reload
     */
    public WOComponent markAsCompleteActionOk()
    {
        for (Submission sub : submissionDisplayGroup.allObjects())
        {
            if (sub.result().status() == Status.UNFINISHED)
            {
                sub.result().setStatus(Status.CHECK);
                if (applyLocalChanges())
                {
                    sub.emailNotificationToStudent(
                        "has been updated by the course staff");
                }
            }
        }
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Marks all the submissions shown that have been partially graded as
     * being completed, sending e-mail notifications as necessary.
     * @return null to force this page to reload
     */
    public WOComponent markAsComplete()
    {
        ConfirmPage confirmPage = pageWithName(ConfirmPage.class);
        confirmPage.nextPage       = this;
        confirmPage.message        =
            "You are about to mark all <b>partially graded</b> submissions "
            + "as now complete.  Submissions that have no remarks or manual "
            + "scoring information will not be affected.  All students who "
            + "are affected will receive an e-mail notification.";
        confirmPage.actionReceiver = this;
        confirmPage.actionOk       = "markAsCompleteActionOk";
        confirmPage.setTitle("Confirm Grading Is Complete");
        return confirmPage;
    }


    // ----------------------------------------------------------
    public boolean hasTAScore()
    {
        return aSubmission.result().taScoreRaw() != null;
    }


    // ----------------------------------------------------------
    public boolean hasPartners()
    {
        return aSubmission.result().submissions().count() > 1;
    }


    // ----------------------------------------------------------
    public boolean hasMultiplePartners()
    {
        return aSubmission.result().submissions().count() > 2;
    }


    // ----------------------------------------------------------
    public boolean isAPartner()
    {
        return partnerSubmission.user() != aSubmission.user();
    }


    // ----------------------------------------------------------
    public boolean morePartners()
    {
        NSArray<Submission> submissions = aSubmission.result().submissions();
        Submission lastSubmission = submissions.objectAtIndex(
            submissions.count() - 1);
        if (lastSubmission == aSubmission)
        {
            lastSubmission = submissions.objectAtIndex(submissions.count() - 2);
        }
        return partnerSubmission != lastSubmission;
    }


    // ----------------------------------------------------------
    public boolean isMostRecentSubmission()
    {
        return aSubmission == aSubmission.latestSubmission();
    }


    // ----------------------------------------------------------
    public int mostRecentSubmissionNo()
    {
        return aSubmission.latestSubmission().submitNumber();
    }


    // ----------------------------------------------------------
    public void flushNavigatorDerivedData()
    {
        assignmentOffering = null;
        super.flushNavigatorDerivedData();
    }


    // ----------------------------------------------------------
    public WOComponent repartner()
    {
        for (Submission sub : submissionDisplayGroup.allObjects())
        {
            if (sub.result() != null)
            {
                for (Submission psub : sub.result().submissions())
                {
                    if (psub != sub
                        && psub.assignmentOffering().assignment()
                        != sub.assignmentOffering().assignment())
                    {
                        log.warn("found partner submission "
                            + psub.user() + " #" + psub.submitNumber()
                            + "\non incorrect assignment offering "
                            + psub.assignmentOffering());

                        NSArray<AssignmentOffering> partnerOfferings =
                            AssignmentOffering.objectsMatchingQualifier(
                                localContext(),
                                AssignmentOffering.courseOffering
                                    .dot(CourseOffering.course).eq(
                                        sub.assignmentOffering()
                                        .courseOffering().course())
                                .and(AssignmentOffering.courseOffering
                                    .dot(CourseOffering.students).eq(
                                        psub.user()))
                                .and(AssignmentOffering.assignment
                                .eq(sub.assignmentOffering().assignment())));
                        if (partnerOfferings.count() == 0)
                        {
                            log.error("Cannot locate correct assignment "
                                + "offering for partner"
                                + psub.user() + " #" + psub.submitNumber()
                                + "\non incorrect assignment offering "
                                + psub.assignmentOffering());
                        }
                        else
                        {
                            if (partnerOfferings.count() > 1)
                            {
                                log.warn("Multiple possible offerings for "
                                    + "partner "
                                    + psub.user() + " #" + psub.submitNumber()
                                    + "\non incorrect assignment offering "
                                    + psub.assignmentOffering());
                                for (AssignmentOffering ao : partnerOfferings)
                                {
                                    log.warn("\t" + ao);
                                }
                            }

                            psub.setAssignmentOfferingRelationship(
                                partnerOfferings.get(0));
                        }
                    }
                }
            }
        }
        applyLocalChanges();
        return null;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger(StudentsForAssignmentPage.class);
}
