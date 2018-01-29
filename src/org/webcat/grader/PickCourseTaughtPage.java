/*==========================================================================*\
 |  $Id: PickCourseTaughtPage.java,v 1.2 2010/09/27 04:23:20 stedwar2 Exp $
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
import er.extensions.eof.ERXConstant;
import er.extensions.foundation.ERXValueUtilities;
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 *  This page presents a list of courses for an instructor or TA
 *  to choose from.  It shows a list containing the union of all
 *  courses taught or TA'ed for.
 *
 *  @author  Stephen Edwards
 *  @author  Latest changes by: $Author: stedwar2 $
 *  @version $Revision: 1.2 $, $Date: 2010/09/27 04:23:20 $
 */
public class PickCourseTaughtPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new PickCourseTaughtPage object.
     *
     * @param context The context to use
     */
    public PickCourseTaughtPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public NSArray<CourseOffering> staffCourses;
    public NSArray<CourseOffering> adminCourses;
    public CourseOffering          courseOffering;
    public int                     index;
    public int                     selectedStaffIndex;
    public int                     selectedAdminIndex;
    public NSArray<Semester>       semesters;
    public Semester                semester;
    public Semester                aSemester;

    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void awake()
    {
        super.awake();
        selectedStaffIndex = -1;
        selectedAdminIndex = -1;
    }


    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        User user = user();
        if ( semesters == null )
        {
            semesters =
                Semester.allObjectsOrderedByStartDate( localContext() );
            Object semesterPref = user.preferences()
                .valueForKey( PickCourseEnrolledPage.SEMESTER_PREF_KEY );
            if (semesterPref == null && semesters.count() > 0)
            {
                // Default to most recent semester, if no preference is set
                semester = semesters.objectAtIndex(0);
            }
            else
            {
                semester = Semester.forId( localContext(),
                    ERXValueUtilities.intValue( semesterPref ) );
            }
        }
        // Save selected semester
        user.preferences().takeValueForKey(
            semester == null ? ERXConstant.ZeroInteger : semester.id(),
            PickCourseEnrolledPage.SEMESTER_PREF_KEY );
        user.savePreferences();

        staffCourses = user.staffFor(semester);
        adminCourses = user.adminForButNotStaff(semester);
        if ( staffCourses.count() == 0 && adminCourses.count() == 0 )
        {
            // There are no enrolled courses
            error(
                "You are not listed as the instructor or TA for any courses"
                + (semester == null ? "" : " in the selected semester")
                + "." );
        }
        CourseOffering selectedCourse = coreSelections().courseOffering();
        if ( selectedCourse != null )
        {
            selectedStaffIndex =
                staffCourses.indexOfIdenticalObject( selectedCourse );
        }
        if ( selectedStaffIndex == NSArray.NotFound )
        {
            selectedAdminIndex =
                adminCourses.indexOfIdenticalObject( selectedCourse );
            if ( selectedAdminIndex != NSArray.NotFound )
            {
                selectedAdminIndex += staffCourses.count();
            }
        }
        if ( selectedStaffIndex == NSArray.NotFound
             && selectedAdminIndex == NSArray.NotFound )
        {
            coreSelections().setCourseOfferingRelationship( null );
            selectedCourse = null;
        }
        if ( selectedCourse == null  )
        {
            if ( staffCourses.count() > 0 )
            {
                selectedStaffIndex = 0;
                coreSelections().setCourseOfferingRelationship(
                    staffCourses.objectAtIndex( selectedStaffIndex ) );
            }
            else if ( adminCourses.count() > 0 )
            {
                selectedAdminIndex = 0;
                coreSelections().setCourseOfferingRelationship(
                    adminCourses.objectAtIndex( selectedAdminIndex ) );
                selectedAdminIndex += staffCourses.count();
            }
        }

        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        if ( selectedStaffIndex >= 0 )
        {
            coreSelections().setCourseOfferingRelationship(
                staffCourses.objectAtIndex( selectedStaffIndex ) );
            return super.next();
        }
        else if ( selectedAdminIndex >= 0 )
        {
            selectedAdminIndex -= staffCourses.count();
            coreSelections().setCourseOfferingRelationship(
                adminCourses.objectAtIndex( selectedAdminIndex ) );
            return super.next();
        }
        else
        {
            error( "You must choose a course to proceed." );
            return null;
        }
    }


    // ----------------------------------------------------------
    public boolean nextEnabled()
    {
        return ( staffCourses.count() > 0
                 || adminCourses.count() > 0 )
                 &&  super.nextEnabled();
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        // When semester list changes, make sure not to take the
        // default action, which is to click "next".
        return null;
    }


    // ----------------------------------------------------------
    public int index1()
    {
        return index;
    }


    // ----------------------------------------------------------
    public int index2()
    {
        return index1() + staffCourses.count();
    }


    // ----------------------------------------------------------
    public void cancelLocalChanges()
    {
        NSDictionary<?, ?> config =
            wcSession().tabs.selectedDescendant().config();
        if ( config != null
             && config.objectForKey( "resetPrimeUser" ) != null )
        {
            setLocalUser( wcSession().primeUser() );
        }
        super.cancelLocalChanges();
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( PickCourseTaughtPage.class );
}
