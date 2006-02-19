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

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

//-------------------------------------------------------------------------
/**
* Represents a standard Web-CAT page that has not yet been implemented
* (is "to be defined").
*
*  @author Stephen Edwards
*  @version $Id$
*/
public class EditCoursePage
    extends WCComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new TBDPage object.
     * 
     * @param context The context to use
     */
    public EditCoursePage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup      courseDisplayGroup;
    public WODisplayGroup      instructorDisplayGroup;
    public WODisplayGroup      TADisplayGroup;
    public Course              course;
    public User                user;
    public int                 index;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse arg0, WOContext arg1 )
    {
        instructorDisplayGroup.setMasterObject( wcSession().courseOffering() );
        TADisplayGroup.setMasterObject( wcSession().courseOffering() );
        super.appendToResponse( arg0, arg1 );
    }


    // ----------------------------------------------------------
    /**
     * Makes the default action call apply.
     * @see net.sf.webcat.core.WCComponent#defaultAction()
     */
    public WOComponent defaultAction()
    {
        apply();
        return super.defaultAction();
    }


    // ----------------------------------------------------------
    /**
     * Used to filter out the current user from some functions.
     * @return true if the user we are iterating over is the same as
     *     the currently logged in user
     */
    public boolean matchesUser()
    {
        return user == wcSession().user();
    }


    // ----------------------------------------------------------
    /**
     * Remove the selected instructor.
     * @return always null
     */
    public WOComponent removeInstructor()
    {
        wcSession().courseOffering().removeFromInstructorsRelationship( user );
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Remove the selected TA.
     * @return always null
     */
    public WOComponent removeTA()
    {
        wcSession().courseOffering().removeFromTAsRelationship( user );
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Add a new instructor.
     * @return the add instructor page
     */
    public WOComponent addInstructor()
    {
        EditStaffPage addPage = (EditStaffPage)pageWithName(
            EditStaffPage.class.getName() );
        addPage.editInstructors = true;
        addPage.nextPage = this;
        return addPage;
    }


    // ----------------------------------------------------------
    /**
     * Add a new TA.
     * @return the add TA page
     */
    public WOComponent addTA()
    {
        EditStaffPage addPage = (EditStaffPage)pageWithName(
            EditStaffPage.class.getName() );
        addPage.editInstructors = false;
        addPage.nextPage = this;
        return addPage;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( EditCoursePage.class );
}
