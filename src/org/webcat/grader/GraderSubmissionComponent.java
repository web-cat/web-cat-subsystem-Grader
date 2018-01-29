/*==========================================================================*\
 |  $Id: GraderSubmissionComponent.java,v 1.2 2010/09/27 04:22:36 stedwar2 Exp $
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

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

//-------------------------------------------------------------------------
/**
 *  A {@link GraderAssignmentComponent} that adds support for initializing
 *  the current submission selection from login parameters.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.2 $, $Date: 2010/09/27 04:22:36 $
 */
public class GraderSubmissionComponent
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderSubmissionComponent object.
     *
     * @param context The context to use
     */
    public GraderSubmissionComponent( WOContext context )
    {
        super( context );
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Extracts submission identification from the given startup
     * parameters.
     * @param params A dictionary of form values to decode
     * @return True if successful, false if the parameter is missing
     */
    @SuppressWarnings("unchecked")
    public boolean startWith( NSDictionary params )
    {
        boolean result = false;
        String sid = stringValueForKey( params, Submission.ID_FORM_KEY );
        if ( sid != null )
        {
            result = startWith( Submission.forId( localContext(), sid ) );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Sets the relevant submission properties for this
     * session.
     * @param submission the submission to use for generating settings
     * @return True if successful, false if the submission is not valid
     */
    protected boolean startWith( Submission submission )
    {
        boolean result = false;
        org.webcat.core.User user = user();
        if ( submission != null
             && ( submission.user() == user
                  || submission.assignmentOffering().courseOffering()
                      .isInstructor( user )
                  || submission.assignmentOffering().courseOffering()
                      .isGrader( user ) )
             && startWith( submission.assignmentOffering().courseOffering() ) )
        {
            prefs().setSubmissionRelationship( submission );
            result = true;
        }
        return result;
    }

}
