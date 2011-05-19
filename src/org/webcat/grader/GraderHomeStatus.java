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
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.eof.ERXQ;
import org.apache.log4j.Logger;
import org.webcat.core.Application;
import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.Semester;

// -------------------------------------------------------------------------
/**
 *  Generates the grader subsystem's page sections for the home->status page.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class GraderHomeStatus
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderSystemStatusRows object.
     *
     * @param context The page's context
     */
    public GraderHomeStatus( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup     enqueuedJobGroup;
    public EnqueuedJob        job;
    public WODisplayGroup     assignmentGroup;
    public WODisplayGroup     oldAssignmentGroup;
    public WODisplayGroup     upcomingAssignmentsGroup;
    public AssignmentOffering assignment;
    public int                index;


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
        if (log.isDebugEnabled())
        {
            log.debug( "starting beforeAppendToResponse()" );
            Application.enableSQLLogging();
        }

        enqueuedJobGroup.queryBindings().setObjectForKey(
                user(),
                "user"
            );
        enqueuedJobGroup.fetch();

        currentTime = new NSTimestamp();
        EOQualifier baseQualifier =
            AssignmentOffering.availableFrom.lessThan(currentTime).and(
            AssignmentOffering.publish.isTrue()).and(
                new EOKeyValueQualifier(
                    AssignmentOffering.COURSE_OFFERING_STUDENTS_KEY,
                    EOQualifier.QualifierOperatorContains,
                    user()));
        baseQualifier = ERXQ.or(baseQualifier,
            new EOKeyValueQualifier(
                AssignmentOffering.COURSE_OFFERING_INSTRUCTORS_KEY,
                EOQualifier.QualifierOperatorContains,
                user()),
            new EOKeyValueQualifier(
                AssignmentOffering.COURSE_OFFERING_GRADERS_KEY,
                EOQualifier.QualifierOperatorContains,
                user()));
        assignmentGroup.setQualifier( ERXQ.and(baseQualifier,
            AssignmentOffering.lateDeadline.greaterThan(currentTime)));
        if (log.isDebugEnabled())
        {
            log.debug( "qualifier = " + assignmentGroup.qualifier() );
        }
        assignmentGroup.fetch();
        if (log.isDebugEnabled())
        {
            log.debug( "results = " + assignmentGroup.displayedObjects() );
        }

        Semester currentSemester = null;
        NSArray<Semester> semesters =
            Semester.allObjectsOrderedByStartDate(localContext());
        if (semesters.count() > 0)
        {
            currentSemester = semesters.get(0);
        }
        oldAssignmentGroup.setQualifier(
            currentSemester == null
                ? ERXQ.and(baseQualifier,
                    ERXQ.not(AssignmentOffering.lateDeadline
                        .greaterThan(currentTime)))
                : ERXQ.and(baseQualifier,
                    ERXQ.not(AssignmentOffering.lateDeadline
                        .greaterThan(currentTime)),
                    AssignmentOffering.courseOffering.dot(
                        CourseOffering.semester).is(currentSemester))
            );
        oldAssignmentGroup.fetch();


        // Now set up the upcoming assignments list
        upcomingAssignmentsGroup.setQualifier(ERXQ.and(
            // Not in either of the upper lists
            ERXQ.not(baseQualifier),
            // Also, more recent than two weeks ago
            AssignmentOffering.dueDate.greaterThan(
                currentTime.timestampByAddingGregorianUnits(0, 0, -14, 0, 0, 0)
                ),
            // Also, some time within the next 4 weeks
            AssignmentOffering.dueDate.lessThan(
                currentTime.timestampByAddingGregorianUnits(0, 0, 28, 0, 0, 0))
            ));
        upcomingAssignmentsGroup.fetch();
        if (log.isDebugEnabled())
        {
            Application.disableSQLLogging();
            log.debug("ending beforeAppendToResponse()");
        }
        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    /**
     * View results for the most recent submission to the selected assignments.
     *
     * @return the most recent results page
     */
    public Number mostRecentScore()
    {
        SubmissionResult subResult = assignment.mostRecentSubmissionResultFor(
            user());
        return (subResult == null)
            ? null
            : new Double(subResult.automatedScore());
    }


    // ----------------------------------------------------------
    /**
     * Check whether the user can edit the selected assignment.
     *
     * @return true if the user can edit the assignment
     */
    public boolean canEditAssignment()
    {
        return assignment.courseOffering().isInstructor( user() );
    }


    // ----------------------------------------------------------
    /**
     * Check whether the user can edit the selected assignment.
     *
     * @return true if the user can edit the assignment
     */
    public boolean canGradeAssignment()
    {
        boolean result =
            assignment.courseOffering().isInstructor(user())
            || assignment.courseOffering().isGrader(user());
        log.debug("can grade = " + result);
        return result;
    }


    // ----------------------------------------------------------
    /**
     * An action to go to the submission page for a given assignment.
     *
     * @return the submission page for the selected assignment
     */
    public WOComponent submitAssignment()
    {
        selectAssignment(assignment);
        return pageWithName(
            wcSession().tabs.selectById("UploadSubmission").pageName());
    }


    // ----------------------------------------------------------
    /**
     * View results for the most recent submission to the selected assignments.
     *
     * @return the most recent results page
     */
    public WOComponent viewResults()
    {
        selectSubmission(assignment);
        return pageWithName(
            wcSession().tabs.selectById("MostRecent").pageName());
    }


    // ----------------------------------------------------------
    /**
     * An action to go to the graphing page for a given assignment.
     *
     * @return the graphing page for the selected assignment
     */
    public WOComponent graphResults()
    {
        selectSubmission(assignment);
        return pageWithName(
            wcSession().tabs.selectById("GraphResults").pageName());
    }


    // ----------------------------------------------------------
    /**
     * An action to go to edit page for a given assignment.
     *
     * @return the properties page for the selected assignment
     */
    public WOComponent editAssignment()
    {
        selectAssignment(assignment);
        return pageWithName(
            wcSession().tabs.selectById("AssignmentProperties").pageName());
    }


    // ----------------------------------------------------------
    /**
     * An action to go to edit page for a given assignment.
     *
     * @return the properties page for the selected assignment
     */
    public WOComponent gradeAssignment()
    {
        selectAssignment(assignment);
        return pageWithName(
            wcSession().tabs.selectById("EnterGrades").pageName());
    }


    // ----------------------------------------------------------
    private void selectAssignment(AssignmentOffering offering)
    {
        coreSelections().setSemester(offering.courseOffering().semester());
        coreSelections().setCourseOfferingRelationship(
            offering.courseOffering());
        coreSelections().setCourseRelationship(
            offering.courseOffering().course());
        prefs().setAssignmentRelationship(offering.assignment());
        prefs().setAssignmentOfferingRelationship(offering);
        if (!assignment.courseOffering().isStaff(user())
            && user().hasAdminPrivileges())
        {
            coreSelections().setIncludeAdminAccess(true);
        }
        if (!offering.publish())
        {
            prefs().setShowUnpublishedAssignments(true);
        }
        if (offering.lateDeadline().before(new NSTimestamp()))
        {
            prefs().setShowClosedAssignments(true);
        }
    }


    // ----------------------------------------------------------
    private void selectSubmission(AssignmentOffering offering)
    {
        selectAssignment(offering);
        SubmissionResult subResult =
            assignment.mostRecentSubmissionResultFor(user());
        Submission sub = null;
        if (subResult != null)
        {
            sub = subResult.submissionFor(user());
        }
        prefs().setSubmissionRelationship(sub);
    }


    // ----------------------------------------------------------
    /**
     * Determine if the current assignment has suspended submissions (that
     * this user can see).
     *
     * @return true if the user can see this assignment's status and this
     * assignment has suspended submissions
     */
    public boolean assignmentHasSuspendedSubs()
    {
        return ( user().hasAdminPrivileges()
                 || assignment.courseOffering().instructors()
                     .containsObject( user() ) )
               && assignment.suspendedSubmissionsInQueue().count() > 0;
    }


    // ----------------------------------------------------------
    private NSMutableDictionary<Course,
        NSMutableDictionary<Assignment, NSMutableArray<AssignmentOffering>>>
        organizeAssignments(NSArray<AssignmentOffering> offerings)
    {
        NSMutableDictionary<Course, NSMutableDictionary<Assignment,
            NSMutableArray<AssignmentOffering>>> result =
            new NSMutableDictionary<Course, NSMutableDictionary<Assignment,
                NSMutableArray<AssignmentOffering>>>();

        for (AssignmentOffering ao : offerings)
        {
            Course c = ao.courseOffering().course();

            // Look up the course in the result, creating it if necessary
            NSMutableDictionary<Assignment, NSMutableArray<AssignmentOffering>>
                courseAssignments = result.get(c);
            if (courseAssignments == null)
            {
                courseAssignments = new NSMutableDictionary<Assignment,
                    NSMutableArray<AssignmentOffering>>();
                result.put(c, courseAssignments);
            }

            // Look up the assignment, creating its array if necessary
            Assignment a = ao.assignment();
            NSMutableArray<AssignmentOffering> cOfferings =
                courseAssignments.get(a);
            if (cOfferings == null)
            {
                cOfferings = new NSMutableArray<AssignmentOffering>();
                courseAssignments.put(a, cOfferings);
            }

            cOfferings.add(ao);
        }
        return result;
    }

    //~ Instance/static variables .............................................

    private NSTimestamp currentTime;
    private NSMutableDictionary<Course,
        NSMutableDictionary<Assignment, NSMutableArray<AssignmentOffering>>>
        currentAssignments;
    private NSMutableDictionary<Course,
        NSMutableDictionary<Assignment, NSMutableArray<AssignmentOffering>>>
        oldAssignments;
    static Logger log = Logger.getLogger( GraderHomeStatus.class );
}
