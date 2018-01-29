/*==========================================================================*\
 |  $Id: StudentsForAssignmentPage.java,v 1.23 2014/06/16 17:27:47 stedwar2 Exp $
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

import java.util.HashMap;
import java.util.Map;
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import er.extensions.appserver.ERXDisplayGroup;
import er.extensions.foundation.ERXArrayUtilities;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.ui.WCTable;
import org.webcat.ui.generators.JavascriptFunction;
import org.webcat.ui.generators.JavascriptGenerator;
import org.webcat.ui.util.ComponentIDGenerator;

// -------------------------------------------------------------------------
/**
 * Show an overview of class grades for an assignment, and allow the user
 * to download them in spreadsheet form or edit them one at a time.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.23 $, $Date: 2014/06/16 17:27:47 $
 */
public class StudentsForAssignmentPage
    extends GraderAssignmentsComponent
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

        staffSubmissionGroup = new ERXDisplayGroup<Submission>();
        staffSubmissionGroup.setNumberOfObjectsPerBatch(100);
        staffSubmissionGroup.setSortOrderings(
            Submission.user.dot(User.name_LF).ascInsensitives().then(
                Submission.user.dot(User.userName).ascInsensitive())
            );

        offerings = new ERXDisplayGroup<AssignmentOffering>();
        offerings.setSortOrderings(
            AssignmentOffering.titleString.ascInsensitives());

        studentNewerSubmissions = new ERXDisplayGroup<Submission>();
        studentNewerSubmissions.setDetailKey("allSubmissions");
    }


    //~ KVC Attributes (must be public) .......................................

    /** Submission in the worepetition */
    public UserSubmissionPair aUserSubmission;
    public Submission  aSubmission;
    public Submission  partnerSubmission;

    public UserSubmissionPair  selectedUserSubmissionForPickerDialog;
    public NSArray<UserSubmissionPair> allUserSubmissionsForNavigationForPickerDialog;

    /** index in the student worepetition */
    public int         index;

    public ERXDisplayGroup<Submission> staffSubmissionGroup;
    /** index in the staff worepetition */
    public int         staffIndex;

    public ERXDisplayGroup<AssignmentOffering> offerings;
    public AssignmentOffering assignmentOffering;

    public Submission                  aNewerSubmission;

    /** Value of the corresponding checkbox on the page. */
    public boolean omitStaff           = true;
    public boolean useBlackboardFormat = true;

    public ComponentIDGenerator idFor = new ComponentIDGenerator(this);


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug("appendToResponse()");

        subStats =
            new HashMap<AssignmentOffering, Submission.CumulativeStats>();
        offerings.setObjectArray(assignmentOfferings(courseOfferings()));
        if (log.isDebugEnabled())
        {
            log.debug("assignment offerings:");
            for (AssignmentOffering ao : offerings.allObjects())
            {
                log.debug("\t" + ao);
            }
        }

        NSMutableArray<Submission> staffSubs =
            new NSMutableArray<Submission>();
        NSArray<User> admins = User.administrators(localContext());

        for (AssignmentOffering ao : offerings.displayedObjects())
        {
            // Stuff the index variable into the public key so the group/stats
            // methods will work for us
            assignmentOffering = ao;
            NSArray<UserSubmissionPair> subs =
                Submission.submissionsForGrading(
                        localContext(),
                        ao,
                        true,  // omitPartners
                        omitStaff,
                        studentStats());
            userGroup().setObjectArray(subs);

            @SuppressWarnings("unchecked")
            NSArray<User> staff = ERXArrayUtilities
                .arrayByAddingObjectsFromArrayWithoutDuplicates(
                    ao.courseOffering().staff(),
                    admins);
            staffSubs.addAll(extractSubmissions(
                    Submission.submissionsForGrading(
                            localContext(),
                            ao,
                            true,  // omitPartners
                            staff,
                            null)));
        }

        staffSubmissionGroup.setObjectArray(staffSubs);

        selectedUserSubmissionForPickerDialog = null;
        allUserSubmissionsForNavigationForPickerDialog = null;

        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    private NSArray<Submission> extractSubmissions(
            NSArray<UserSubmissionPair> userSubs)
    {
        NSMutableArray<Submission> submissions =
            new NSMutableArray<Submission>();

        for (UserSubmissionPair pair : userSubs)
        {
            if (pair.userHasSubmission())
            {
                submissions.addObject(pair.submission());
            }
        }

        return submissions;
    }


    // ----------------------------------------------------------
    public void setAUserSubmission(UserSubmissionPair pair)
    {
        aUserSubmission = pair;
        aSubmission = (pair != null ? pair.submission() : null);
    }


    // ----------------------------------------------------------
    public WOActionResults pickOtherSubmission()
    {
        selectedUserSubmissionForPickerDialog = aUserSubmission;
        allUserSubmissionsForNavigationForPickerDialog =
            userGroup().displayedObjects();

        JavascriptGenerator js = new JavascriptGenerator();
        js.dijit("pickSubmissionDialog").call("show");
        return js;
    }


    // ----------------------------------------------------------
    public WCComponent self()
    {
        return this;
    }


    // ----------------------------------------------------------
    public String tableId()
    {
        return idFor.get("submissionsTable_" + assignmentOffering.id());
    }


    // ----------------------------------------------------------
    public WOActionResults regradeSubmissions()
    {
        return new ConfirmingAction(this, false)
        {
            @Override
            protected String confirmationTitle()
            {
                return "Regrade Everyone's Submission?";
            }

            @Override
            protected String confirmationMessage()
            {
                return "<p>This action will <b>regrade the most recent "
                    + "submission for every student</b> who has submitted to "
                    + "this assignment.</p><p>This will also <b>delete all "
                    + "prior results</b> for the submissions to be regraded "
                    + "and <b>delete all TA comments and scoring</b> that "
                    + "have been recorded for the submissions to be regraded."
                    + "</p><p>Each student\'s most recent submission will be "
                    + "re-queued for grading, and each student will receive "
                    + "an e-mail message when their new results are "
                    + "available.</p><p class=\"center\">Regrade everyone's "
                    + "most recent submission?</p>";
            }

            @Override
            protected WOActionResults actionWasConfirmed()
            {
                for (AssignmentOffering offering :
                    assignmentOfferings(courseOfferings()))
                {
                    offering.regradeMostRecentSubsForAll(localContext());
                }

                applyLocalChanges();
                return null;
            }
        };
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
            else if (!aSubmission.resultIsReady())
            {
                log.error("editSubmissionScore(): null submission result!");
                log.error("student = " + aSubmission.user().userName());
            }
            prefs().setSubmissionRelationship(aSubmission);

            destination = (WCComponent) super.next();
            if (destination instanceof GradeStudentSubmissionPage)
            {
                GradeStudentSubmissionPage page =
                    (GradeStudentSubmissionPage) destination;

                if (aUserSubmission != null)
                {
                    page.availableSubmissions =
                        userGroup().displayedObjects().immutableClone();
                    page.thisSubmissionIndex =
                        page.availableSubmissions.indexOf(aUserSubmission);
                }
            }

            destination.nextPage = this;
        }

        return destination;
    }


    // ----------------------------------------------------------
    public WOComponent editNewerSubmissionScore()
    {
        WCComponent destination = null;
        if (!hasMessages())
        {
            if (aNewerSubmission == null)
            {
                log.error("editNewerSubmissionScore(): null submission!");
            }
            else if (!aNewerSubmission.resultIsReady())
            {
                log.error("editNewerSubmissionScore(): null submission result!");
                log.error("student = " + aNewerSubmission.user().userName());
            }
            prefs().setSubmissionRelationship(aNewerSubmission);

//            destination = pageWithName(GradeStudentSubmissionPage.class);
            destination = (WCComponent) super.next();
            if (destination instanceof GradeStudentSubmissionPage)
            {
                GradeStudentSubmissionPage page =
                    (GradeStudentSubmissionPage)destination;

                if (aUserSubmission != null)
                {
                    page.availableSubmissions =
                        userGroup().displayedObjects().immutableClone();
                    page.thisSubmissionIndex =
                        page.availableSubmissions.indexOf(aUserSubmission);
                }
            }

            destination.nextPage = this;
        }
        return destination;
    }


    // ----------------------------------------------------------
    public String markCompleteStatusIndicatorId()
    {
        return idFor.get("markCompleteStatusIndicator_"
                + assignmentOffering.id());
    }


    // ----------------------------------------------------------
    /**
     * Marks all the submissions shown that have been partially graded as
     * being completed, sending e-mail notifications as necessary.
     * @return null to force this page to reload
     */
    public int markSubmissionsAsComplete()
    {
        int numberNotified = 0;

        assignmentOffering = offeringForAction;
        for (UserSubmissionPair pair : userGroup().allObjects())
        {
            if (pair.userHasSubmission())
            {
                Submission sub = pair.submission();

                if (sub.result().status() == Status.UNFINISHED
                    || (sub.result().status() != Status.CHECK
                        && !sub.assignmentOffering().assignment()
                            .usesTAScore()))
                {
                    sub.result().setStatus(Status.CHECK);
                    if (applyLocalChanges())
                    {
                        numberNotified++;
                        sub.emailNotificationToStudent(
                            "has been updated by the course staff");
                    }
                }
            }
        }

        return numberNotified;
    }


    // ----------------------------------------------------------
    /**
     * Marks all the submissions shown that have been partially graded as
     * being completed, sending e-mail notifications as necessary.
     * @return null to force this page to reload
     */
    public WOActionResults markAsComplete()
    {
        offeringForAction = assignmentOffering;

        return new ConfirmingAction(this, true)
        {
            @Override
            protected String confirmationTitle()
            {
                return "Confirm Grading Is Complete?";
            }

            @Override
            protected String confirmationMessage()
            {
                return "<p>You are about to mark all <b>partially graded</b> "
                    + "submissions as now complete so that students can see "
                    + "their feedback from you.  Submissions that have "
                    + "no remarks or manual scoring information will not be "
                    + "affected.  All students who are affected will receive "
                    + "an e-mail notification.</p><p class=\"center\">"
                    + "Mark partially graded submissions as complete?</p>";
            }

            @Override
            protected void beforeActionWasConfirmed(JavascriptGenerator js)
            {
                InlineStatusIndicator.updateWithSpinner(js,
                        markCompleteStatusIndicatorId(),
                        "Notifying students that their scores are ready...");
            }

            @Override
            protected WOActionResults actionWasConfirmed()
            {
                final int numberNotified = markSubmissionsAsComplete();

                JavascriptGenerator js = new JavascriptGenerator();
                WCTable.refresh(js, tableId(), new JavascriptFunction() {
                    @Override
                    public void generate(JavascriptGenerator g)
                    {
                        String students;

                        if (numberNotified == 1)
                        {
                            students = "1 student was";
                        }
                        else
                        {
                            students = "" + numberNotified + " students were";
                        }

                        InlineStatusIndicator.updateWithState(g,
                                markCompleteStatusIndicatorId(),
                                InlineStatusIndicator.SUCCESS,
                                students + " notified.");
                    }
                });

                return js;
            }
        };
    }


    // ----------------------------------------------------------
    public ERXDisplayGroup<Submission> studentNewerSubmissions()
    {
        if (studentNewerSubmissions.masterObject() != aSubmission)
        {
            studentNewerSubmissions.setMasterObject(aSubmission);
            studentNewerSubmissions.setObjectArray(
                aSubmission.allSubmissions());
            studentNewerSubmissions.queryMin().takeValueForKey(
                aSubmission.submitNumber() + 1, Submission.SUBMIT_NUMBER_KEY);
            studentNewerSubmissions.setQualifier(
                studentNewerSubmissions.qualifierFromQueryValues());
        }
        return studentNewerSubmissions;
    }


    // ----------------------------------------------------------
    public boolean hasTAScore()
    {
        return aSubmission.result().taScoreRaw() != null;
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
    public String newerSubmitTimeSpanClass()
    {
        if (aNewerSubmission.isLate())
        {
            return "warn sm";
        }
        else
        {
            return "sm";
        }
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
        for (UserSubmissionPair pair : userGroup().allObjects())
        {
            Submission sub = pair.submission();

            if (sub != null && sub.resultIsReady())
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


    // ----------------------------------------------------------
    public Submission.CumulativeStats studentStats()
    {
        Submission.CumulativeStats stats = subStats.get(assignmentOffering);
        if (stats == null)
        {
            stats = new Submission.CumulativeStats();
            subStats.put(assignmentOffering, stats);
        }
        return stats;
    }


    // ----------------------------------------------------------
    public ERXDisplayGroup<UserSubmissionPair> userGroup()
    {
        ERXDisplayGroup<UserSubmissionPair> group =
            userGroups.get(assignmentOffering);
        if (group == null)
        {
            group = new ERXDisplayGroup<UserSubmissionPair>();
            group.setNumberOfObjectsPerBatch(100);
            group.setSortOrderings(
                UserSubmissionPair.user.dot(User.name_LF).ascInsensitives().then(
                    UserSubmissionPair.user.dot(User.userName).ascInsensitive())
                );
            userGroups.put(assignmentOffering, group);
        }
        return group;
    }


    // ----------------------------------------------------------
    public String newerSubmissionStatus()
    {
        String result = "feedback entered on earlier submission";
        if (!aNewerSubmission.resultIsReady())
        {
            result = "suspended";
            EnqueuedJob job = aNewerSubmission.enqueuedJob();
            if (job == null)
            {
                result = "cancelled";
            }
            else if (!job.paused())
            {
                result = "queued for grading";
            }
        }
        // check date of submission against date of feedback
        else if (aSubmission.resultIsReady()
                && aSubmission.result().lastUpdated() != null
                && aNewerSubmission.submitTime().after(
                    aSubmission.result().lastUpdated()))
        {
            result = "newer than feedback";
        }

        if (log.isDebugEnabled())
        {
            log.debug("newerSubmissionStatus() for " + aNewerSubmission
                + " = " + result);
            if (aSubmission.resultIsReady()
                && aSubmission.result().lastUpdated() != null)
            {
                log.debug("    selected submission last updated: "
                    + aSubmission.result().lastUpdated());
            }
            log.debug("    newer submission on: "
                + aNewerSubmission.submitTime());
        }
        return result;
    }


    //~ Instance/static variables .............................................

    private Map<AssignmentOffering, ERXDisplayGroup<UserSubmissionPair>> userGroups =
        new HashMap<AssignmentOffering, ERXDisplayGroup<UserSubmissionPair>>();
    private Map<AssignmentOffering, Submission.CumulativeStats> subStats;

    private AssignmentOffering offeringForAction;

    private ERXDisplayGroup<Submission> studentNewerSubmissions;

    static Logger log = Logger.getLogger(StudentsForAssignmentPage.class);
}
