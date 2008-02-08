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

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Allow the user to select an enrolled student from the current course.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class PickEnrolledStudentPage
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public PickEnrolledStudentPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup      studentDisplayGroup;
    public User                student;
    public int                 studentIndex;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        studentDisplayGroup.setMasterObject(
            coreSelections().courseOffering() );
        super.appendToResponse( response, context );
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

    static Logger log = Logger.getLogger( PickEnrolledStudentPage.class );
}