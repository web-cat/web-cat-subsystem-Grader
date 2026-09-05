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
import org.webcat.woextensions.WCFetchSpecification;

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

    }


    //~ KVC Attributes (must be public) .......................................

    /** Student in the worepetition */
    public User        aStudent;
    public Submission  aSubmission;
    public Submission  partnerSubmission;

    public Submission.StudentSubmissionInfo selectedUserSubmissionForPickerDialog;
    public NSArray<Submission.StudentSubmissionInfo> allUserSubmissionsForNavigationForPickerDialog;

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

    private long start_time;
    private void log_time(String msg)
    {
        if (user().id().intValue() == 1)
        {
            long elapsed = System.currentTimeMillis() - start_time;
            msg = StudentsForAssignmentPage.class.getSimpleName()
                + " "
                + msg
                + ": elapsed " + elapsed + "ms";
            log.info(msg);
        }
    }


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

        start_time = System.currentTimeMillis();
        log_time("beforeAppendToResponse() query start");
        NSMutableArray<Submission> staffSubs =
            new NSMutableArray<Submission>();
        NSArray<User> admins = User.administrators(localContext());

        NSArray<AssignmentOffering> aos = offerings.displayedObjects();

        boolean offeringsChanged = (lastOfferings == null || !lastOfferings.equals(aos));
        if (gradingState == null || offeringsChanged)
        {
            lastOfferings = aos.immutableClone();

            NSMutableDictionary<AssignmentOffering, NSArray<User>> usersByAO =
                new NSMutableDictionary<AssignmentOffering, NSArray<User>>();

            for (AssignmentOffering ao : aos)
            {
                NSArray<User> students = ao.courseOffering().studentsWithoutStaff();

                NSArray<User> staff = ERXArrayUtilities
                    .arrayByAddingObjectsFromArrayWithoutDuplicates(
                        ao.courseOffering().staff(),
                        admins);

                NSArray<User> allForAO = ERXArrayUtilities
                    .arrayByAddingObjectsFromArrayWithoutDuplicates(students, staff);

                usersByAO.setObjectForKey(allForAO, ao);
            }

            gradingState = Submission.submissionsForGrading(aos, usersByAO);
        }
        else
        {
            Submission.submissionsForGrading(gradingState);
        }
        log_time("beforeAppendToResponse() after batch fetch");

        for (AssignmentOffering ao : aos)
        {
            assignmentOffering = ao;
            Map<User, Submission.StudentSubmissionInfo> infoMap =
                gradingState.resultsForOffering(ao);

            Submission.CumulativeStats stats = studentStats();

            // Student group: populate directly from offering's students
            NSMutableArray<User> students = new NSMutableArray<User>();
            for (User student : ao.courseOffering().studentsWithoutStaff())
            {
                Submission.StudentSubmissionInfo info =
                    (infoMap != null) ? infoMap.get(student) : null;
                Submission sub = (info != null) ? info.gradedSubmission : null;
                if (sub != null && sub.partnerLink())
                {
                    continue;
                }
                students.addObject(student);
                if (sub != null && sub.result() != null)
                {
                    stats.accumulate(sub.result());
                }
            }
            userGroup(ao).setObjectArray(students);

            // Staff group
            @SuppressWarnings("unchecked")
            NSArray<User> staff = ERXArrayUtilities
                .arrayByAddingObjectsFromArrayWithoutDuplicates(
                    ao.courseOffering().staff(),
                    admins);

            for (User u : staff)
            {
                Submission.StudentSubmissionInfo info =
                    (infoMap != null) ? infoMap.get(u) : null;
                Submission sub = (info != null) ? info.gradedSubmission : null;
                if (sub != null && !sub.partnerLink())
                {
                    staffSubs.addObject(sub);
                }
            }
        }

        staffSubmissionGroup.setObjectArray(
            ERXArrayUtilities.arrayWithoutDuplicates(staffSubs));

        log_time("beforeAppendToResponse() query finish");
        selectedUserSubmissionForPickerDialog = null;
        allUserSubmissionsForNavigationForPickerDialog = null;

        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public void setAStudent(User student)
    {
        aStudent = student;
        Submission.StudentSubmissionInfo info = aStudentInfo();
        aSubmission = (info != null ? info.submission() : null);
    }


    // ----------------------------------------------------------
    public Submission.StudentSubmissionInfo aStudentInfo()
    {
        if (gradingState != null && assignmentOffering != null && aStudent != null)
        {
            return gradingState.infoForUser(assignmentOffering, aStudent);
        }
        return null;
    }


    // ----------------------------------------------------------
    public Submission aSubmission()
    {
        if (aSubmission != null)
        {
            return aSubmission;
        }
        Submission.StudentSubmissionInfo info = aStudentInfo();
        return (info != null) ? info.submission() : null;
    }


    // ----------------------------------------------------------
    public boolean userHasSubmission()
    {
        Submission.StudentSubmissionInfo info = aStudentInfo();
        return info != null && info.userHasSubmission();
    }


    // ----------------------------------------------------------
    public NSArray<Submission.StudentSubmissionInfo> availableSubmissionsForGrading()
    {
        NSMutableArray<Submission.StudentSubmissionInfo> list =
            new NSMutableArray<Submission.StudentSubmissionInfo>();
        for (User student : userGroup().displayedObjects())
        {
            Submission.StudentSubmissionInfo info =
                (gradingState != null && assignmentOffering != null)
                ? gradingState.infoForUser(assignmentOffering, student)
                : null;
            if (info != null && info.userHasSubmission())
            {
                list.addObject(info);
            }
        }
        return list.immutableClone();
    }


    // ----------------------------------------------------------
    public WOActionResults pickOtherSubmission()
    {
        selectedUserSubmissionForPickerDialog = aStudentInfo();
        allUserSubmissionsForNavigationForPickerDialog =
            availableSubmissionsForGrading();

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
            Submission sub = aSubmission();
            if (sub == null)
            {
                log.error("editSubmissionScore(): null submission!");
            }
            else if (!sub.resultIsReady())
            {
                log.error("editSubmissionScore(): null submission result!");
                log.error("student = " + sub.user().userName());
            }
            prefs().setSubmissionRelationship(sub);

            destination = (WCComponent) super.next();
            if (destination instanceof GradeStudentSubmissionPage)
            {
                GradeStudentSubmissionPage page =
                    (GradeStudentSubmissionPage) destination;

                page.availableSubmissions =
                    availableSubmissionsForGrading();
                Submission.StudentSubmissionInfo thisInfo = aStudentInfo();
                page.thisSubmissionIndex = (thisInfo != null)
                    ? page.availableSubmissions.indexOf(thisInfo)
                    : -1;
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

            destination = (WCComponent) super.next();
            if (destination instanceof GradeStudentSubmissionPage)
            {
                GradeStudentSubmissionPage page =
                    (GradeStudentSubmissionPage) destination;

                page.availableSubmissions =
                    availableSubmissionsForGrading();
                Submission.StudentSubmissionInfo thisInfo = aStudentInfo();
                page.thisSubmissionIndex = (thisInfo != null)
                    ? page.availableSubmissions.indexOf(thisInfo)
                    : -1;
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
        for (User student : userGroup().allObjects())
        {
            Submission.StudentSubmissionInfo info =
                (gradingState != null && assignmentOffering != null)
                ? gradingState.infoForUser(assignmentOffering, student)
                : null;
            if (info != null && info.userHasSubmission())
            {
                Submission sub = info.submission();

                if (sub.result().status() == Status.UNFINISHED
                    || (sub.result().status() != Status.CHECK
                        && (!sub.assignmentOffering().assignment()
                            .usesTAScore()
                        || sub.result().taScoreRaw() != null)))
                {
                    sub.result().setStatus(Status.CHECK);
                    if (applyLocalChanges())
                    {
                        numberNotified++;
                        sub.result().emailNotificationToStudent(
                            "has been updated by the course staff");
                    }
                }
                else
                {
                    // Send score to LTI consumer, if score is ready/visible
                    sub.sendScoreToLTIConsumerIfNecessary();
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
    public NSArray<Submission> studentNewerSubmissions()
    {
        Submission.StudentSubmissionInfo info = aStudentInfo();
        if (info != null)
        {
            return info.newerSubmissions();
        }
        Submission sub = aSubmission();
        if (sub != null)
        {
            NSMutableArray<Submission> newer = new NSMutableArray<Submission>();
            for (Submission s : sub.allSubmissions())
            {
                if (s.submitNumber() > sub.submitNumber())
                {
                    newer.addObject(s);
                }
            }
            return newer;
        }
        return NSArray.emptyArray();
    }


    // ----------------------------------------------------------
    public boolean hasTAScore()
    {
        Submission sub = aSubmission();
        return sub != null
            && sub.result() != null
            && sub.result().taScoreRaw() != null;
    }


    // ----------------------------------------------------------
    public boolean isMostRecentSubmission()
    {
        Submission.StudentSubmissionInfo info = aStudentInfo();
        if (info != null)
        {
            return info.isMostRecentSubmission();
        }
        Submission sub = aSubmission();
        return sub == null || sub == sub.latestSubmission();
    }


    // ----------------------------------------------------------
    public int mostRecentSubmissionNo()
    {
        Submission.StudentSubmissionInfo info = aStudentInfo();
        if (info != null)
        {
            return info.mostRecentSubmissionNo();
        }
        Submission sub = aSubmission();
        return (sub != null && sub.latestSubmission() != null)
            ? sub.latestSubmission().submitNumber() : 0;
    }


    // ----------------------------------------------------------
    public String submitTimeSpanClass()
    {
        Submission sub = aSubmission();
        if (sub != null && sub.isLate())
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
        for (User student : userGroup().allObjects())
        {
            Submission.StudentSubmissionInfo info =
                (gradingState != null && assignmentOffering != null)
                ? gradingState.infoForUser(assignmentOffering, student)
                : null;
            Submission sub = (info != null) ? info.submission() : null;

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
    public ERXDisplayGroup<User> userGroup()
    {
        return userGroup(assignmentOffering);
    }


    // ----------------------------------------------------------
    public ERXDisplayGroup<User> userGroup(AssignmentOffering ao)
    {
        ERXDisplayGroup<User> group = userGroups.get(ao);
        if (group == null)
        {
            group = new ERXDisplayGroup<User>();
            group.setNumberOfObjectsPerBatch(100);
            group.setSortOrderings(
                User.name_LF.ascInsensitives().then(
                    User.userName.ascInsensitive())
                );
            userGroups.put(ao, group);
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
        else
        {
            Submission sub = aSubmission();
            if (sub != null
                && sub.resultIsReady()
                && sub.result().lastUpdated() != null
                && aNewerSubmission.submitTime().after(
                    sub.result().lastUpdated()))
            {
                result = "newer than feedback";
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("newerSubmissionStatus() for " + aNewerSubmission
                + " = " + result);
            Submission sub = aSubmission();
            if (sub != null
                && sub.resultIsReady()
                && sub.result().lastUpdated() != null)
            {
                log.debug("    selected submission last updated: "
                    + sub.result().lastUpdated());
            }
            log.debug("    newer submission on: "
                + aNewerSubmission.submitTime());
        }
        return result;
    }


    //~ Instance/static variables .............................................

    private Map<AssignmentOffering, ERXDisplayGroup<User>> userGroups =
        new HashMap<AssignmentOffering, ERXDisplayGroup<User>>();
    private Map<AssignmentOffering, Submission.CumulativeStats> subStats;

    private AssignmentOffering offeringForAction;

    public Submission.SubmissionGradingState gradingState;
    private NSArray<AssignmentOffering> lastOfferings;


    // ----------------------------------------------------------
    /**
     * Getter for DownloadScoresDialog binding. Having only a getter without
     * a corresponding setter defeats KVC value passback, making the binding
     * strictly one-way.
     *
     * @return the current grading state
     */
    public Submission.SubmissionGradingState gradingStateForDownload()
    {
        return gradingState;
    }

    static Logger log = Logger.getLogger(StudentsForAssignmentPage.class);
}
