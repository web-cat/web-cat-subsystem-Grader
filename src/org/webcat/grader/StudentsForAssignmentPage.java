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

import java.util.HashMap;
import java.util.Map;
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import er.extensions.appserver.ERXDisplayGroup;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.ui.generators.JavascriptGenerator;
import org.webcat.ui.util.ComponentIDGenerator;

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

    public ComponentIDGenerator idFor;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        idFor = new ComponentIDGenerator(this);

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

            staffSubs.addAll(extractSubmissions(
                    Submission.submissionsForGrading(
                            localContext(),
                            ao,
                            true,  // omitPartners
                            ao.courseOffering().staff(),
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
            else if (aNewerSubmission.result() == null)
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
    /**
     * Marks all the submissions shown that have been partially graded as
     * being completed, sending e-mail notifications as necessary.
     * @return null to force this page to reload
     */
    public WOComponent markAsCompleteActionOk()
    {
        assignmentOffering = offeringForAction;
        for (UserSubmissionPair pair : userGroup().allObjects())
        {
            if (pair.userHasSubmission())
            {
                Submission sub = pair.submission();

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
        }
        return null;
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

        return new ConfirmingAction(this)
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
            protected WOActionResults performStandardAction()
            {
                return markAsCompleteActionOk();
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

            if (sub != null && sub.result() != null)
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
        String result = null;
        if (aNewerSubmission.result() == null)
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
        else
        {
            // check date of submission against date of feedback
            NSTimestamp feedbackTime = aSubmission.submitTime();
            if (aSubmission.result() != null)
            {
                feedbackTime = aSubmission.result().lastUpdated();
            }
            if (aNewerSubmission.submitTime().after(feedbackTime))
            {
                result = "newer than feedback";
            }
        }
        log.debug("newerSubmissionStatus() = " + result);
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
