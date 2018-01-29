/*==========================================================================*\
 |  $Id: GraderAssignmentComponent.java,v 1.3 2010/10/12 02:39:56 stedwar2 Exp $
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

import org.webcat.core.*;
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

//-------------------------------------------------------------------------
/**
 *  A {@link GraderCourseComponent} that adds support for initializing
 *  assignment selections from login parameters.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.3 $, $Date: 2010/10/12 02:39:56 $
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
     * This method determines whether any embedded navigator will
     * automatically pop up to force a selection and page reload.
     * @return True if the navigator should start out by opening automatically.
     */
    public boolean forceNavigatorSelection()
    {
        boolean result = super.forceNavigatorSelection();
        if (prefs().assignment() == null)
        {
            result = true;
        }
        else if (!result && requiresAssignmentOffering())
        {
            AssignmentOffering ao = prefs().assignmentOffering();
            Assignment assignment = prefs().assignment();
            if (ao == null && assignment != null)
            {
                CourseOffering co = bestMatchingCourseOffering();
                for (AssignmentOffering offering : assignment.offerings())
                {
                    if (offering.courseOffering() == co)
                    {
                        prefs().setAssignmentOfferingRelationship(offering);
                        if (coreSelections().courseOffering() != co)
                        {
                            coreSelections().setCourseOfferingRelationship(co);
                            if (coreSelections().course() != co.course())
                            {
                                coreSelections().setCourseRelationship(null);
                            }
                        }
                        break;
                    }
                }
            }
            result = (prefs().assignmentOffering() == null);
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * This method determines whether the current page requires the
     * user to have a selected AssignmentOffering.
     * The default implementation returns true, but is designed
     * to be overridden in subclasses.
     * @return True if the page requires a selected assignment offering.
     */
    public boolean requiresAssignmentOffering()
    {
        return true;
    }


    // ----------------------------------------------------------
    /**
     * Extracts assignment identification from the given startup
     * parameters.
     * @param params A dictionary of form values to decode
     * @return True if successful, false if the parameter is missing
     */
    @Override
    public boolean startWith( NSDictionary<String, Object> params )
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
