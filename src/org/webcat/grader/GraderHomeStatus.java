/*==========================================================================*\
 |  Copyright (C) 2006-2021 Virginia Tech
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
import com.webobjects.eoaccess.EOObjectNotAvailableException;
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
            {
                // resolve faults from fetch immediately to prevent
                // problems if jobs get deleted before page generation is
                // complete
                NSMutableArray<EnqueuedJob> jobs =
                    enqueuedJobGroup.allObjects().mutableClone();
                boolean anyPaused = false;
                int oldSize = jobs.size();
                for (int i = 0; i < jobs.size(); i++)
                {
                    try
                    {
                        // force resolving fault
                        anyPaused = jobs.get(i).paused() || anyPaused;
                    }
                    catch (EOObjectNotAvailableException e)
                    {
                        jobs.remove(i);
                        i--;
                    }
                }
                if (oldSize != jobs.size())
                {
                    enqueuedJobGroup.setObjectArray(jobs);
                }
            }

            currentTime = new NSTimestamp();

            // Centralized fetch of all open offerings for this user:
            NSArray<AssignmentOffering> open = AssignmentOffering
                .openOfferingsForUser(localContext(), user(), currentTime);
            openOfferings = open;
            currentAssignments = organizeAssignments(open);
            courses.setObjectArray(
                new NSArray<Course>(currentAssignments.keySet()));

            // Centralized fetch of closed offerings:
            NSArray<Semester> semesters =
                Semester.allObjectsOrderedByStartDate(localContext());
            Semester currentSemester =
                (semesters.count() > 0) ? semesters.get(0) : null;
            NSArray<AssignmentOffering> old = AssignmentOffering
                .closedOfferingsForUser(
                    localContext(), user(), currentSemester, currentTime);
            oldAssignments = organizeAssignments(old);
            coursesForOld.setObjectArray(
                new NSArray<Course>(oldAssignments.keySet()));
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
        editLog.debug("editAssignment() -> before selection, assignment = "
            + prefs().assignment());
        editLog.debug("editAssignment() -> before selection, offering = "
            + prefs().assignmentOffering());
        assignmentOffering =
            currentAssignments.get(course()).get(anAssignment()).get(0);
        editLog.debug("editAssignment() -> selectAssignment("
            + assignmentOffering + ")");
        selectAssignment(assignmentOffering);
        editLog.debug("editAssignment() -> selected assignment = "
            + prefs().assignment());
        editLog.debug("editAssignment() -> selected offering = "
            + prefs().assignmentOffering());
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
        editLog.debug("editOldAssignment() -> before selection, assignment = "
            + prefs().assignment());
        editLog.debug("editOldAssignment() -> before selection, offering = "
            + prefs().assignmentOffering());
        assignmentOffering =
            oldAssignments.get(courseForOld()).get(anOldAssignment()).get(0);
        editLog.debug("editOldAssignment() -> selectAssignment("
            + assignmentOffering + ")");
        selectAssignment(assignmentOffering);
        editLog.debug("editOldAssignment() -> selected assignment = "
            + prefs().assignment());
        editLog.debug("editOldAssignment() -> selected offering = "
            + prefs().assignmentOffering());
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
        editLog.debug("viewOrGrade() -> before selection, assignment = "
            + prefs().assignment());
        editLog.debug("viewOrGrade() -> before selection, offering = "
            + prefs().assignmentOffering());
        assignmentOffering =
            currentAssignments.get(course()).get(anAssignment()).get(0);
        editLog.debug("viewOrGrade() -> selectAssignment("
            + assignmentOffering + ")");
        selectAssignment(assignmentOffering);
        editLog.debug("viewOrGrade() -> selected assignment = "
            + prefs().assignment());
        editLog.debug("viewOrGrade() -> selected offering = "
            + prefs().assignmentOffering());
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
        editLog.debug("viewOrGradeOld() -> before selection, assignment = "
            + prefs().assignment());
        editLog.debug("viewOrGradeOld() -> before selection, offering = "
            + prefs().assignmentOffering());
        assignmentOffering =
            oldAssignments.get(courseForOld()).get(anOldAssignment()).get(0);
        editLog.debug("viewOrGradeOld() -> selectAssignment("
            + assignmentOffering + ")");
        selectAssignment(assignmentOffering);
        editLog.debug("viewOrGradeOld() -> selected assignment = "
            + prefs().assignment());
        editLog.debug("viewOrGradeOld() -> selected offering = "
            + prefs().assignmentOffering());
        return pageWithName(
            wcSession().tabs.selectById("EnterGrades").pageName());
    }


    // ----------------------------------------------------------
    private void selectAssignment(AssignmentOffering offering)
    {
        editLog.debug("selectAssignment(" + offering + ")");
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
        if (offering.isClosedFor(user(), new NSTimestamp()))
        {
            prefs().setShowClosedAssignments(true);
        }
        editLog.debug("selectAssignment() => " + user().coreSelections());
        editLog.debug("selectAssignment() => "
            + GraderPrefs.objectsForUser(localContext(), user()));
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
            && assignmentOffering.isUnavailableFor(user(), currentTime);
    }


    // ----------------------------------------------------------
    public boolean hasUpcomingAssignments()
    {
        // set up the upcoming assignments list
        if (upcomingAssignmentsGroup.allObjects() == null
            || upcomingAssignmentsGroup.allObjects().count() == 0)
        {
            NSTimestamp windowStart = currentTime.timestampByAddingGregorianUnits(
                0, 0, -14, 0, 0, 0);
            NSTimestamp windowEnd = currentTime.timestampByAddingGregorianUnits(
                0, 0, 28, 0, 0, 0);

            EOQualifier dateQual = AssignmentOffering.dueDate.greaterThan(windowStart)
                .and(AssignmentOffering.dueDate.lessThan(windowEnd));

            EOQualifier instructorQual =
                AssignmentOffering.courseOffering.dot(CourseOffering.instructors).is(user())
                .and(dateQual);
            NSMutableArray<AssignmentOffering> upcoming =
                new NSMutableArray<AssignmentOffering>(
                    AssignmentOffering.fetchWithPrefetch(localContext(), instructorQual, null));

            EOQualifier graderQual =
                AssignmentOffering.courseOffering.dot(CourseOffering.graders).is(user())
                .and(dateQual);
            ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
                upcoming,
                AssignmentOffering.fetchWithPrefetch(localContext(), graderQual, null));

            if (openOfferings != null)
            {
                upcoming.removeObjectsInArray(openOfferings);
            }
            if (oldAssignmentGroup.allObjects() != null)
            {
                upcoming.removeObjectsInArray(oldAssignmentGroup.allObjects());
            }

            EOSortOrdering.sortArrayUsingKeyOrderArray(
                upcoming, new NSArray<EOSortOrdering>(AssignmentOffering.dueDate.asc()));

            upcomingAssignmentsGroup.setObjectArray(upcoming);
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
    private NSArray<AssignmentOffering> openOfferings;
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

    static Logger log = Logger.getLogger(GraderHomeStatus.class);
    static Logger editLog = Logger.getLogger(
        GraderHomeStatus.class.getName() + ".edit");
}
