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
import com.webobjects.eoaccess.*;
import com.webobjects.foundation.*;

import net.sf.webcat.core.*;

//-------------------------------------------------------------------------
/**
 * Allows the user to create a new course offering.
 *
 *  @author Stephen Edwards
 *  @version $Id$
 */
public class NewCourseOfferingPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new TBDPage object.
     * 
     * @param context The context to use
     */
    public NewCourseOfferingPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public Course         course;
    public WODisplayGroup courseDisplayGroup;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     * 
     * @param response The response being built
     * @param context  The context of the request
     */
    public void appendToResponse( WOResponse response, WOContext context )
    {
        if ( wcSession().courseOffering() != null )
        {
            wcSession().setCourseRelationship(
                            wcSession().courseOffering().course() );
        }
        super.appendToResponse( response, context );
    }
    
    // ----------------------------------------------------------
    /**
     * Create a new course offering object and move on to an edit page.
     * @see net.sf.webcat.core.WCComponent#next()
     */
    public WOComponent next()
    {
        CourseOffering newOffering = new CourseOffering();
        wcSession().localContext().insertObject( newOffering );
        newOffering.setCourseRelationship( wcSession().course() );
        NSArray semesters = EOUtilities.objectsForEntityNamed(
                        wcSession().localContext(), Semester.ENTITY_NAME );
        newOffering.setSemesterRelationship(
                        (Semester)semesters.lastObject() );
        newOffering.addToInstructorsRelationship( wcSession().user() );
        wcSession().setCourseOfferingRelationship( newOffering );
        return super.next();
    }
}
