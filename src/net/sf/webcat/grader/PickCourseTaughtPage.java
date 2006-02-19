/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006 Virginia Tech
 |
 |  This file is part of Web-CAT.
 |
 |  Web-CAT is free software; you can redistribute it and/or modify
 |  it under the terms of the GNU General Public License as published by
 |  the Free Software Foundation; either version 2 of the License, or
 |  (at your option) any later version.
 |
 |  Web-CAT is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU General Public License for more details.
 |
 |  You should have received a copy of the GNU General Public License
 |  along with Web-CAT; if not, write to the Free Software
 |  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 |
 |  Project manager: Stephen Edwards <edwards@cs.vt.edu>
 |  Virginia Tech CS Dept, 660 McBryde Hall (0106), Blacksburg, VA 24061 USA
\*==========================================================================*/

package net.sf.webcat.grader;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 *  This page presents a list of courses for an instructor or TA
 *  to choose from.  It shows a list containing the union of all
 *  courses taught or TA'ed for.
 *
 *  @author  Stephen Edwards
 *  @version $Id$
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

    public NSArray             staffCourses;
    public NSArray             adminCourses;
    public CourseOffering      courseOffering;
    public int                 index;
    public int                 selectedStaffIndex;
    public int                 selectedAdminIndex;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void awake()
    {
        log.debug( "awake()" );
        selectedStaffIndex = -1;
        selectedAdminIndex = -1;
    }


    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        User user = wcSession().user();
        staffCourses = user.staffFor();
        adminCourses = user.adminForButNotStaff();
        if ( staffCourses.count() == 0 && adminCourses.count() == 0 )
        {
            // There are no enrolled courses
            errorMessage(
                "You are not listed as the instructor or TA for any courses." );
        }
        CourseOffering selectedCourse = wcSession().courseOffering();
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
            wcSession().setCourseOfferingRelationship( null );
            selectedCourse = null;
        }
        if ( selectedCourse == null  )
        {
            if ( staffCourses.count() > 0 )
            {
                selectedStaffIndex = 0;
                wcSession().setCourseOfferingRelationship(
                    (CourseOffering)staffCourses.objectAtIndex(
                        selectedStaffIndex ) );
            }
            else if ( adminCourses.count() > 0 )
            {
                selectedAdminIndex = 0;
                wcSession().setCourseOfferingRelationship(
                    (CourseOffering)adminCourses.objectAtIndex(
                        selectedAdminIndex ) );
                selectedAdminIndex += staffCourses.count();
            }
        }
        
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        clearErrors();
        if ( selectedStaffIndex >= 0 )
        {
            wcSession().setCourseOfferingRelationship(
                (CourseOffering)staffCourses.objectAtIndex(
                    selectedStaffIndex ) );
            return super.next();
        }
        else if ( selectedAdminIndex >= 0 )
        {
            selectedAdminIndex -= staffCourses.count();
            wcSession().setCourseOfferingRelationship(
                (CourseOffering)adminCourses.objectAtIndex(
                    selectedAdminIndex ) );
            return super.next();
        }
        else
        {
            errorMessage( "You must choose a course to proceed." );
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
        NSDictionary config = wcSession().tabs.selectedDescendant().config();
        if ( config != null
             && config.objectForKey( "resetPrimeUser" ) != null )
        {
            wcSession().setLocalUser( wcSession().primeUser() );
        }
        super.cancelLocalChanges();
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( PickCourseTaughtPage.class );
}
