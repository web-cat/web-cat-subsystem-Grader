/*==========================================================================*\
 |  $Id: GraderAssignmentsComponent.java,v 1.4 2012/05/09 16:29:12 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2010-2012 Virginia Tech
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

import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.Semester;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

//-------------------------------------------------------------------------
/**
 * A subclass of {@link GraderAssignmentComponent} that allows for
 * multi-offering course/assignment selections.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.4 $, $Date: 2012/05/09 16:29:12 $
 */
public class GraderAssignmentsComponent
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderAssignmentsComponent object.
     *
     * @param context The context to use
     */
    public GraderAssignmentsComponent(WOContext context)
    {
        super(context);
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * This method determines whether the current page requires the
     * user to have a selected AssignmentOffering.
     * The default implementation returns true, but is designed
     * to be overridden in subclasses.
     * @return True if the page requires a selected assignment offering.
     */
    @Override
    public boolean requiresAssignmentOffering()
    {
        return false;
    }


    // ----------------------------------------------------------
    @Override
    public void awake()
    {
        willForceNavigatorSelection = null;
        super.awake();
    }


    // ----------------------------------------------------------
    @Override
    public boolean forceNavigatorSelection()
    {
        boolean answer = super.forceNavigatorSelection();
        if (!answer)
        {
            if (willForceNavigatorSelection == null)
            {
                NSArray<CourseOffering> courses = internalCourseOfferings();
                willForceNavigatorSelection =
                    (courses.count() == 0)
                    || (assignmentOfferings(courses).count() == 0);
            }
            answer = willForceNavigatorSelection;
        }
        return answer;
    }


    // ----------------------------------------------------------
    @Override
    public void takeValuesFromRequest(WORequest request, WOContext context)
    {
        super.takeValuesFromRequest(request, context);
        willForceNavigatorSelection = null;
    }


    // ----------------------------------------------------------
    /*
     * Get the selected course offering(s) for this page.
     * @return The list of course offerings (empty if none is selected).
     */
    public NSArray<CourseOffering> courseOfferings()
    {
        return forceNavigatorSelection()
            ? new NSArray<CourseOffering>()
            : internalCourseOfferings();
    }


    // ----------------------------------------------------------
    /*
     * Get the selected assignment offering(s) for this page.
     * @return The list of assignment offerings (empty if none is selected).
     */
    public NSArray<AssignmentOffering> assignmentOfferings(
        NSArray<CourseOffering> courseOfferings)
    {
        NSMutableArray<AssignmentOffering> assignmentOfferings =
            new NSMutableArray<AssignmentOffering>(courseOfferings.size());
        Assignment assignment = prefs().assignment();
        if (assignment != null)
        {
            for (CourseOffering co : courseOfferings)
            {
                assignmentOfferings.addAll(
                    AssignmentOffering.objectsMatchingQualifier(
                        localContext(),
                        AssignmentOffering.assignment.eq(assignment).and(
                            AssignmentOffering.courseOffering.eq(co))));
            }
        }
        return assignmentOfferings;
    }


    // ----------------------------------------------------------
    /*
     * Get the selected course offering(s) for this page.
     * @return The list of course offerings (empty if none is selected).
     */
    private NSArray<CourseOffering> internalCourseOfferings()
    {
        NSMutableArray<CourseOffering> courseOfferings =
            new NSMutableArray<CourseOffering>(10);
        Course course = coreSelections().course();
        if (course == null)
        {
            // Just one offering selected
            CourseOffering co = coreSelections().courseOffering();
            if (co != null &&
                    (co.isStaff(user()) || user().hasAdminPrivileges()))
            {
                courseOfferings.add(co);
            }
        }
        else
        {
            Semester semester = coreSelections().semester();
            // Find all offerings that this user can see
            NSArray<CourseOffering> candidates = (semester == null)
                ? course.offerings()
                : CourseOffering.offeringsForSemesterAndCourse(
                    localContext(), course, semester);

            for (CourseOffering co : candidates)
            {
                if (co.isStaff(user()) || user().hasAdminPrivileges())
                {
                    courseOfferings.add(co);
                }
            }
        }
        return courseOfferings;
    }


    //~ Instance/static variables ...............NSMutableArray................

    private Boolean willForceNavigatorSelection;
}
