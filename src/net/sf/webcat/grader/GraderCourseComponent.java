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
import com.webobjects.foundation.*;
import net.sf.webcat.core.*;

//-------------------------------------------------------------------------
/**
 *  A {@link GraderComponent} that adds support for initializing course
 *  selections from login parameters.
 *
 *  @author  Stephen Edwards
 *  @version $Id$
 */
public class GraderCourseComponent
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderCourseComponent object.
     *
     * @param context The context to use
     */
    public GraderCourseComponent( WOContext context )
    {
        super( context );
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Extracts course offering identification from the given startup
     * parameters.
     * @param params A dictionary of form values to decode
     * @return True if successful, false if the parameter is missing
     */
    public boolean startWith( NSDictionary params )
    {
        boolean result = false;
        String crn = stringValueForKey( params, CourseOffering.CRN_KEY );
        if ( crn != null )
        {
            result = startWith( CourseOffering
                .offeringForCrn( wcSession().localContext(), crn ) );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Sets the relevant course and course offering properties for this
     * session.
     * @param offering the course offering to use for generating settings
     * @return True if successful, false if the course offering is not valid
     */
    protected boolean startWith( CourseOffering offering )
    {
        boolean result = false;
        User user = wcSession().user();
        if ( offering != null
             && ( user.enrolledIn().contains(  offering )
                  || offering.isInstructor( user )
                  || offering.isTA( user ) ) )
        {
            result = true;
            wcSession().setCourse( offering.course() );
            wcSession().setCourseOffering( offering );
        }
        return result;
    }
}
