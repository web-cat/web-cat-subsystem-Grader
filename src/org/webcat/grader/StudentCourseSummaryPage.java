/*==========================================================================*\
 |  $Id: StudentCourseSummaryPage.java,v 1.7 2014/06/16 17:27:58 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2011-2012 Virginia Tech
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
import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.User;
import org.webcat.core.WCComponent;
import org.webcat.ui.generators.JavascriptGenerator;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import er.extensions.appserver.ERXDisplayGroup;

//-------------------------------------------------------------------------
/**
 * TODO real description
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.7 $, $Date: 2014/06/16 17:27:58 $
 */
public class StudentCourseSummaryPage
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public StudentCourseSummaryPage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public ERXDisplayGroup<AssignmentOffering> assignmentOfferingsDisplayGroup;
    public AssignmentOffering                  assignmentOffering;
    public int                                 index;

    public NSArray<User>                       studentsInCourse;
    public User                                aStudent;
    public User                                selectedStudent;

    public UserSubmissionPair selectedUserSubmissionForPickerDialog;

    public boolean anyAssignmentUsesTestingScore;
    public boolean anyAssignmentUsesToolCheckScore;
    public boolean anyAssignmentUsesTAScore;
    public boolean anyAssignmentUsesBonusesOrPenalties;
    public boolean anyAssignmentUsesExtraColumns;

    public AssignmentOffering          selectedAssignmentOffering;
    public ERXDisplayGroup<Submission> submissionDisplayGroup;
    public Submission                  aSubmission;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    protected void beforeAppendToResponse(WOResponse response,
            WOContext context)
    {
        Course courseToFetch = null;

        if (coreSelections().courseOffering() != null)
        {
            courseOfferings = new NSArray<CourseOffering>(
                    coreSelections().courseOffering());
            courseToFetch = coreSelections().courseOffering().course();
        }
        else
        {
            courseOfferings = coreSelections().course().offerings();
            courseToFetch = coreSelections().course();
        }

        // Collect the students to display in the drop-down list.

        NSMutableArray<User> students = new NSMutableArray<User>();

        if (isUserStaff())
        {
            for (CourseOffering offering : courseOfferings)
            {
                students.addObjectsFromArray(offering.studentsAndStaff());
            }

            if (students.isEmpty())
            {
                students.addObject(wcSession().primeUser());
            }

            // Sort students by name.
            User.name_LF.ascInsensitive().sort(students);

            studentsInCourse = students;
        }

        if (selectedStudent == null)
        {
            selectedStudent = wcSession().user();
        }

        // For every assignment in the course, determine the assignment
        // offering that the student actually submitted to (if a student
        // switched labs, it might be different than his "home" offering). If
        // the student didn't make any submissions, just use his "home"
        // offering as the one to display.

        CourseOffering homeOffering = courseOfferings.objectAtIndex(0);

        NSArray<Assignment> assignments = Assignment.objectsMatchingQualifier(
                localContext(), Assignment.courses.is(courseToFetch));

        NSMutableArray<AssignmentOffering> assignmentOfferings =
            new NSMutableArray<AssignmentOffering>();

        NSMutableDictionary<AssignmentOffering, Submission> submissions =
            new NSMutableDictionary<AssignmentOffering, Submission>();

        anyAssignmentUsesTestingScore = false;
        anyAssignmentUsesToolCheckScore = false;
        anyAssignmentUsesTAScore = false;
        anyAssignmentUsesBonusesOrPenalties = false;
        selectedAssignmentOffering = prefs().assignmentOffering();

        for (Assignment assignment : assignments)
        {
            boolean foundSubmission = false;
            AssignmentOffering homeAssignmentOffering = null;

            for (AssignmentOffering ao : assignment.offerings())
            {
                CourseOffering co = ao.courseOffering();

                if (co.equals(homeOffering))
                {
                    homeAssignmentOffering = ao;
                    if (selectedAssignmentOffering == null
                        && assignment == prefs().assignment())
                    {
                        selectedAssignmentOffering = ao;
                    }
                }

                NSArray<Submission> subs =
                    Submission.submissionsForGrading(
                        localContext(), ao, selectedStudent);

                if (subs.size() > 0)
                {
                    foundSubmission = true;
                    assignmentOfferings.addObject(ao);
                    if (ao.assignment().usesTestingScore())
                    {
                        anyAssignmentUsesTestingScore = true;
                    }
                    if (ao.assignment().usesToolCheckScore())
                    {
                        anyAssignmentUsesToolCheckScore = true;
                    }
                    if (ao.assignment().usesTAScore())
                    {
                        anyAssignmentUsesTAScore = true;
                    }
                    if (ao.assignment().usesBonusesOrPenalties())
                    {
                        anyAssignmentUsesBonusesOrPenalties = true;
                    }
                    submissions.setObjectForKey(subs.objectAtIndex(0), ao);
                }
            }

            if (!foundSubmission && homeAssignmentOffering != null)
            {
                assignmentOfferings.addObject(homeAssignmentOffering);
            }
        }
        int extraColCount = 0;
        if (anyAssignmentUsesTestingScore)
        {
            extraColCount++;
        }
        if (anyAssignmentUsesToolCheckScore)
        {
            extraColCount++;
        }
        if (anyAssignmentUsesTAScore)
        {
            extraColCount++;
        }
        if (anyAssignmentUsesBonusesOrPenalties)
        {
            extraColCount++;
        }
        anyAssignmentUsesExtraColumns = (extraColCount > 1);

        assignmentOfferingsDisplayGroup.setObjectArray(assignmentOfferings);
        //assignmentOfferingsDisplayGroup.fetch();

        submissionsByAssignmentOffering = submissions;

        if (selectedAssignmentOffering != null)
        {
            submissionDisplayGroup.setObjectArray(
                Submission.submissionsForAssignmentOfferingAndUser(
                    localContext(),
                    selectedAssignmentOffering,
                    selectedStudent));
        }
        else
        {
            submissionDisplayGroup.setObjectArray(NSArray.EmptyArray);
        }

        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    @Override
    public boolean requiresAssignmentOffering()
    {
        return false;
    }


    // ----------------------------------------------------------
    @Override
    public boolean allowsAllOfferingsForCourse()
    {
        return false;
    }


    // ----------------------------------------------------------
    public boolean isUserStaff()
    {
        User user = wcSession().primeUser();

        for (CourseOffering offering : courseOfferings)
        {
            if (offering.isStaff(user))
            {
                return true;
            }
        }

        return user.hasAdminPrivileges();
    }


    // ----------------------------------------------------------
    public WOActionResults changeStudent()
    {
        studentsInCourse = null;
        return null;
    }


    // ----------------------------------------------------------
    public Submission submissionForAssignmentOffering()
    {
        return submissionsByAssignmentOffering.objectForKey(assignmentOffering);
    }


    // ----------------------------------------------------------
    public String submitTimeSpanClass()
    {
        if (submissionForAssignmentOffering().isLate())
        {
            return "warn";
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    public boolean isMostRecentSubmission()
    {
        Submission sub = submissionForAssignmentOffering();
        return sub == sub.latestSubmission();
    }


    // ----------------------------------------------------------
    public int mostRecentSubmissionNo()
    {
        return submissionForAssignmentOffering().latestSubmission()
            .submitNumber();
    }


    // ----------------------------------------------------------
    /**
     * Checks for errors, then records the currently selected item.
     *
     * @return true if no errors are present
     */
    protected boolean saveSelectionCanContinue()
    {
        Submission submission = aSubmission;
        if (submission == null && assignmentOffering != null)
        {
            submission = submissionForAssignmentOffering();
        }

        if (submission == null)
        {
            log.debug("saveSelectionCanContinue(): no selected "
                + "submission, submission was null");
            error("Please choose a submission.");
        }
        else if (!submission.resultIsReady())
        {
            error("Results for that submission are not available.");
        }
        else
        {
            prefs().setAssignmentOfferingRelationship(
                submission.assignmentOffering());
            prefs().setSubmissionRelationship(submission);

            log.debug("Changing selection, sub #"
                + prefs().submission().submitNumber());
        }

        return !hasMessages();
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
    public WOActionResults selectAssignment()
    {
        if (assignmentOffering != null)
        {
            prefs().setAssignmentOfferingRelationship(assignmentOffering);
        }
        return null;
    }


    // ----------------------------------------------------------
    public WOActionResults viewSubmission()
    {
        if (saveSelectionCanContinue())
        {
            if (isUserStaff())
            {
                GradeStudentSubmissionPage page =
                    pageWithName(GradeStudentSubmissionPage.class);

                page.availableSubmissions = null;
                page.thisSubmissionIndex = 0;
                page.nextPage = this;

                page.reloadGraderPrefs();
                return page;
            }
            else
            {
                reloadGraderPrefs();
                return pageWithName(FinalReportPage.class);
            }
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    public WOActionResults pickOtherSubmission()
    {
        selectedUserSubmissionForPickerDialog = new UserSubmissionPair(
                selectedStudent, submissionForAssignmentOffering());

        JavascriptGenerator js = new JavascriptGenerator();
        js.dijit("pickSubmissionDialog").call("show");
        return js;
    }


    // ----------------------------------------------------------
    public WCComponent self()
    {
        return this;
    }


    //~ Static/instance variables .............................................

    private NSDictionary<AssignmentOffering, Submission>
        submissionsByAssignmentOffering;

    private NSArray<CourseOffering> courseOfferings;

    private static final Logger log = Logger.getLogger(
            StudentCourseSummaryPage.class);
}
