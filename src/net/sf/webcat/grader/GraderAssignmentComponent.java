/*==========================================================================*\
 |  $Id$
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

package net.sf.webcat.grader;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import net.sf.webcat.core.*;

//-------------------------------------------------------------------------
/**
 *  A {@link GraderCourseComponent} that adds support for initializing
 *  assignment selections from login parameters.
 *
 *  @author  Stephen Edwards
 *  @version $Id$
 */
public class GraderAssignmentComponent
    extends GraderCourseComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderAssignmentComponent object.
     *
     * @param context The context to use
     */
    public GraderAssignmentComponent( WOContext context )
    {
        super( context );
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Extracts assignment identification from the given startup
     * parameters.
     * @param params A dictionary of form values to decode
     * @return True if successful, false if the parameter is missing
     */
    public boolean startWith( NSDictionary params )
    {
        boolean result = false;
        String aoid =
            stringValueForKey( params, AssignmentOffering.ID_FORM_KEY );
        if ( aoid != null )
        {
            result = startWith( AssignmentOffering
                .forId( localContext(), aoid ) );
        }
        else
        {
            String aid = stringValueForKey( params, Assignment.ID_FORM_KEY );
            result = startWith( Assignment
                .forId( localContext(), aid ) );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Sets the relevant assignment offering properties for this
     * session.
     * @param assignment the assignment to use for generating settings
     * @return True if successful, false if the offering is not valid
     */
    protected boolean startWith( Assignment assignment )
    {
        return assignment != null && startWith( assignment.offeringForUser(
            user() ) );
    }


    // ----------------------------------------------------------
    /**
     * Sets the relevant assignment offering properties for this
     * session.
     * @param offering the assignment offering to use for generating settings
     * @return True if successful, false if the offering is not valid
     */
    protected boolean startWith( AssignmentOffering offering )
    {
        boolean result = false;
        if ( offering != null && startWith( offering.courseOffering() ) )
        {
            prefs().setAssignmentOfferingRelationship( offering );
            result = true;
        }
        return result;
    }

}
