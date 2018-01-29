/*==========================================================================*\
 |  $Id: PickEnrolledStudentPage.java,v 1.2 2010/09/27 04:23:20 stedwar2 Exp $
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
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 * Allow the user to select an enrolled student from the current course.
 *
 * @author  Stephen Edwards
 * @author  Latest changes by: $Author: stedwar2 $
 * @version $Revision: 1.2 $, $Date: 2010/09/27 04:23:20 $
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
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        studentDisplayGroup.setMasterObject(
            coreSelections().courseOffering() );
        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public User localUser()
    {
        return user();
    }


    // ----------------------------------------------------------
    public void cancelLocalChanges()
    {
        resetPrimeUser();
        super.cancelLocalChanges();
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( PickEnrolledStudentPage.class );
}