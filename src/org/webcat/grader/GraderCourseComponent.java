/*==========================================================================*\
 |  $Id: GraderCourseComponent.java,v 1.2 2010/09/27 04:20:41 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2008 Virginia Tech
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

import org.webcat.core.*;
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

//-------------------------------------------------------------------------
/**
 *  A {@link GraderComponent} that adds support for initializing course
 *  selections from login parameters.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.2 $, $Date: 2010/09/27 04:20:41 $
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
    public boolean startWith( NSDictionary<String, Object> params )
    {
        boolean result = false;
        String crn = stringValueForKey( params, CourseOffering.CRN_KEY );
        if ( crn != null )
        {
            result = startWith( CourseOffering
                .offeringForCrn( localContext(), crn ) );
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
        User user = user();
        if ( offering != null
             && ( user.enrolledIn().contains(  offering )
                  || offering.isInstructor( user )
                  || offering.isGrader( user ) ) )
        {
            result = true;
            coreSelections().setCourseRelationship( offering.course() );
            coreSelections().setCourseOfferingRelationship( offering );
        }
        return result;
    }
}
