/*==========================================================================*\
 |  $Id: GraderHomeStatus.java,v 1.11 2011/10/25 15:30:34 stedwar2 Exp $
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
import er.extensions.appserver.ERXDisplayGroup;
import er.extensions.eof.ERXQ;
import er.extensions.foundation.ERXArrayUtilities;
import org.apache.log4j.Logger;
import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.Semester;

// -------------------------------------------------------------------------
/**
 *  Generates the grader subsystem's page sections for the home->status page.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.11 $, $Date: 2011/10/25 15:30:34 $
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
    public GraderHomeStatus(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public ERXDisplayGroup<EnqueuedJob>        enqueuedJobGroup;
    public EnqueuedJob                         job;
    public ERXDisplayGroup<AssignmentOffering> oldAssignmentGroup;
    public ERXDisplayGroup<AssignmentOffering> upcomingAssignmentsGroup;
    public int                                 index;

    public ERXDisplayGroup<Course> courses;
    public ERXDisplayGroup<Assignment> assignments;
    public ERXDisplayGroup<AssignmentOffering> offerings;
    public AssignmentOffering assignmentOffering;
    public ERXDisplayGroup<Course> coursesForOld;


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
//            Application.enableSQLLogging();
        }

        if (user() != null)
        {
            enqueuedJobGroup.queryBindings().setObjectForKey(user(), "user");
            enqueuedJobGroup.fetch();

            currentTime = new NSTimestamp();
            // First, grab all this student can see
            @SuppressWarnings("unchecked")
            NSMutableArray<AssignmentOffering> interesting =
                new NSMutableArray<AssignmentOffering>(
                    ERXArrayUtilities.filteredArrayWithQualifierEvaluation(
                        AssignmentOffering.objectsMatchingQualifier(
                            localContext(),
                            AssignmentOffering.publish.isTrue().and(
                                AssignmentOffering.courseOffering
                                .dot(CourseOffering.students).is(user()))),
                                AssignmentOffering.availableFrom.lessThan(
                                    currentTime)));

            // Now, add any user has instructor access to:
            ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(interesting,
                AssignmentOffering.objectsMatchingQualifier(localContext(),
                    AssignmentOffering.courseOffering
                    .dot(CourseOffering.instructors).is(user())));

            // Now, add any user has grader access to:
            ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(interesting,
                AssignmentOffering.objectsMatchingQualifier(localContext(),
                    AssignmentOffering.courseOffering
                    .dot(CourseOffering.graders).is(user())));

            @SuppressWarnings("unchecked")
            NSArray<AssignmentOffering> open = ERXArrayUtilities
                .filteredArrayWithQualifierEvaluation(interesting,
                    AssignmentOffering.lateDeadline.greaterThan(currentTime));

            currentAssignments = organizeAssignments(open);
            courses.setObjectArray(
                new NSArray<Course>(currentAssignments.keySet()));
            if (log.isDebugEnabled())
            {
                log.debug("organized = " + currentAssignments);
            }

            Semester currentSemester = null;
            NSArray<Semester> semesters =
                Semester.allObjectsOrderedByStartDate(localContext());
            if (semesters.count() > 0)
            {
                currentSemester = semesters.get(0);
            }
            @SuppressWarnings("unchecked")
            NSArray<AssignmentOffering> old = ERXArrayUtilities
                .filteredArrayWithQualifierEvaluation(interesting,
                    currentSemester == null
                    ? ERXQ.not(AssignmentOffering.lateDeadline
                        .greaterThan(currentTime))
                    : ERXQ.and(
                        ERXQ.not(AssignmentOffering.lateDeadline
                            .greaterThan(currentTime)),
                            AssignmentOffering.courseOffering.dot(
                                CourseOffering.semester).is(currentSemester)));
            oldAssignments = organizeAssignments(old);
            coursesForOld.setObjectArray(
                new NSArray<Course>(oldAssignments.keySet()));
            // FIXME: remove
            oldAssignmentGroup.setObjectArray(old);
        }

        if (log.isDebugEnabled())
        {
//            Application.disableSQLLogging();
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
        SubmissionResult subResult =
            assignmentOffering.mostRecentSubmissionResultFor(user());
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
        if (offerings.displayedObjects() == null
            || offerings.displayedObjects().count() == 0)
        {
            return false;
        }
        AssignmentOffering ao = offerings.displayedObjects().get(0);
        return ao.courseOffering().isInstructor(user());
    }


    // ----------------------------------------------------------
    /**
     * Check whether the user can edit the selected assignment.
     *
     * @return true if the user can edit the assignment
     */
    public boolean canGradeAssignmentOffering()
    {
        boolean result =
            assignmentOffering.courseOffering().isInstructor(user())
            || assignmentOffering.courseOffering().isGrader(user());
        log.debug("can grade = " + result);
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Check whether the user can edit the selected assignment.
     *
     * @return true if the user can edit the assignment
     */
    public boolean canGradeAssignment()
    {
        if (offerings.displayedObjects() == null
            || offerings.displayedObjects().count() == 0)
        {
            return false;
        }
        AssignmentOffering ao = offerings.displayedObjects().get(0);
        boolean result =
            ao.courseOffering().isInstructor(user())
            || ao.courseOffering().isGrader(user());
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
        selectAssignment(assignmentOffering);
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
        selectSubmission(assignmentOffering);
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
        selectSubmission(assignmentOffering);
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
        assignmentOffering =
            currentAssignments.get(course()).get(anAssignment()).get(0);
        selectAssignment(assignmentOffering);
        return pageWithName(
            wcSession().tabs.selectById("AssignmentProperties").pageName());
    }


    // ----------------------------------------------------------
    /**
     * An action to go to edit page for a given assignment.
     *
     * @return the properties page for the selected assignment
     */
    public WOComponent editOldAssignment()
    {
        assignmentOffering =
            oldAssignments.get(courseForOld()).get(anOldAssignment()).get(0);
        selectAssignment(assignmentOffering);
        return pageWithName(
            wcSession().tabs.selectById("AssignmentProperties").pageName());
    }


    // ----------------------------------------------------------
    /**
     * An action to go to edit page for a given assignment.
     *
     * @return the properties page for the selected assignment
     */
    public WOComponent viewOrGrade()
    {
        assignmentOffering =
            currentAssignments.get(course()).get(anAssignment()).get(0);
        selectAssignment(assignmentOffering);
        return pageWithName(
            wcSession().tabs.selectById("EnterGrades").pageName());
    }


    // ----------------------------------------------------------
    /**
     * An action to go to edit page for a given assignment.
     *
     * @return the properties page for the selected assignment
     */
    public WOComponent viewOrGradeOld()
    {
        assignmentOffering =
            oldAssignments.get(courseForOld()).get(anOldAssignment()).get(0);
        selectAssignment(assignmentOffering);
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
        if (!offering.courseOffering().isStaff(user())
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
            offering.mostRecentSubmissionResultFor(user());
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
    public boolean assignmentOfferingHasSuspendedSubs()
    {
        return ( user().hasAdminPrivileges()
                 || assignmentOffering.courseOffering().instructors()
                     .containsObject( user() ) )
               && assignmentOffering.suspendedSubmissionsInQueue().count() > 0;
    }


    // ----------------------------------------------------------
    /**
     * Determine if the current assignment is available to students.
     *
     * @return true if the "available from" time for the offering is after
     * now.
     */
    public boolean assignmentOfferingIsUnavailable()
    {
        return assignmentOffering.availableFrom() != null
            && assignmentOffering.availableFrom().after(currentTime);
    }


    // ----------------------------------------------------------
    public boolean hasUpcomingAssignments()
    {
        // set up the upcoming assignments list
        if (upcomingAssignmentsGroup.displayedObjects() == null
            || upcomingAssignmentsGroup.displayedObjects().count() == 0)
        {
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
            upcomingAssignmentsGroup.setQualifier(ERXQ.and(
                // Not in either of the upper lists
                ERXQ.not(baseQualifier),
                // Also, more recent than two weeks ago
                AssignmentOffering.dueDate.greaterThan(
                    currentTime.timestampByAddingGregorianUnits(
                        0, 0, -14, 0, 0, 0)
                    ),
                // Also, some time within the next 4 weeks
                AssignmentOffering.dueDate.lessThan(
                    currentTime.timestampByAddingGregorianUnits(
                        0, 0, 28, 0, 0, 0))
                ));
            upcomingAssignmentsGroup.fetch();
        }
        return upcomingAssignmentsGroup.displayedObjects().count() > 0;
    }


    // ----------------------------------------------------------
    public Course course()
    {
        return course;
    }


    // ----------------------------------------------------------
    public void setCourse(Course newCourse)
    {
        if (newCourse == null)
        {
            assignments.setObjectArray(NSArray.EmptyArray);
        }
        else
        {
            NSMutableDictionary<Assignment, NSMutableArray<AssignmentOffering>>
                newAssignments = currentAssignments.get(newCourse);
            if (newAssignments == null || newAssignments.isEmpty())
            {
                assignments.setObjectArray(NSArray.EmptyArray);
            }
            else
            {
                assignments.setObjectArray(
                    new NSArray<Assignment>(newAssignments.keySet()));
            }
        }
        course = newCourse;
    }


    // ----------------------------------------------------------
    public Assignment anAssignment()
    {
        return anAssignment;
    }


    // ----------------------------------------------------------
    public void setAnAssignment(Assignment newAssignment)
    {
        if (newAssignment == null)
        {
            offerings.setObjectArray(NSArray.EmptyArray);
        }
        else
        {
            NSMutableArray<AssignmentOffering> newOfferings =
                currentAssignments.get(course()).get(newAssignment);
            if (newOfferings == null || newOfferings.isEmpty())
            {
                offerings.setObjectArray(NSArray.EmptyArray);
            }
            else
            {
                offerings.setObjectArray(newOfferings);
            }
        }
        anAssignment = newAssignment;
    }


    // ----------------------------------------------------------
    public Course courseForOld()
    {
        return courseForOld;
    }


    // ----------------------------------------------------------
    public void setCourseForOld(Course aCourse)
    {
        if (aCourse == null)
        {
            assignments.setObjectArray(NSArray.EmptyArray);
        }
        else
        {
            NSMutableDictionary<Assignment, NSMutableArray<AssignmentOffering>>
                newAssignments = oldAssignments.get(aCourse);
            if (newAssignments == null || newAssignments.isEmpty())
            {
                assignments.setObjectArray(NSArray.EmptyArray);
            }
            else
            {
                assignments.setObjectArray(
                    new NSArray<Assignment>(newAssignments.keySet()));
            }
        }
        courseForOld = aCourse;
    }


    // ----------------------------------------------------------
    public Assignment anOldAssignment()
    {
        return anOldAssignment;
    }


    // ----------------------------------------------------------
    public void setAnOldAssignment(Assignment oldAssignment)
    {
        if (oldAssignment == null)
        {
            offerings.setObjectArray(NSArray.EmptyArray);
        }
        else
        {
            NSMutableArray<AssignmentOffering> newOfferings =
                oldAssignments.get(courseForOld()).get(oldAssignment);
            if (newOfferings == null || newOfferings.isEmpty())
            {
                offerings.setObjectArray(NSArray.EmptyArray);
            }
            else
            {
                offerings.setObjectArray(newOfferings);
            }
        }
        anOldAssignment = oldAssignment;
    }


    // ----------------------------------------------------------
    private NSMutableDictionary<Course,
        NSMutableDictionary<Assignment, NSMutableArray<AssignmentOffering>>>
        organizeAssignments(NSArray<AssignmentOffering> offeringList)
    {
        NSMutableDictionary<Course, NSMutableDictionary<Assignment,
            NSMutableArray<AssignmentOffering>>> result =
            new NSMutableDictionary<Course, NSMutableDictionary<Assignment,
                NSMutableArray<AssignmentOffering>>>();

        for (AssignmentOffering ao : offeringList)
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

    private Course course;
    private Assignment anAssignment;

    private Course courseForOld;
    private Assignment anOldAssignment;

    static Logger log = Logger.getLogger( GraderHomeStatus.class );
}
