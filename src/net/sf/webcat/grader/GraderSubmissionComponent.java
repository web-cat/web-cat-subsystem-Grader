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

//-------------------------------------------------------------------------
/**
 *  A {@link GraderAssignmentComponent} that adds support for initializing
 *  the current submission selection from login parameters.
 *
 *  @author  Stephen Edwards
 *  @version $Id$
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
    public boolean startWith( NSDictionary params )
    {
        boolean result = false;
        String sid = stringValueForKey( params, Submission.ID_FORM_KEY );
        if ( sid != null )
        {
            result = startWith( Submission
                .submissionForId( wcSession().localContext(), sid ) );
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
        net.sf.webcat.core.User user = wcSession().user();
        if ( submission != null
             && ( submission.user() == user
                  || submission.assignmentOffering().courseOffering()
                      .isInstructor( user )
                  || submission.assignmentOffering().courseOffering()
                      .isTA( user ) )
             && startWith( submission.assignmentOffering().courseOffering() ) )
        {
            prefs().setSubmissionRelationship( submission );
            result = true;
        }
        return result;
    }

}
