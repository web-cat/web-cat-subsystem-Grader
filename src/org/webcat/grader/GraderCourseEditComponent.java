/*==========================================================================*\
 |  $Id: GraderCourseEditComponent.java,v 1.2 2010/09/27 04:20:41 stedwar2 Exp $
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

// -------------------------------------------------------------------------
/**
 *  A custom version of {@link GraderComponent} that managed a currently
 *  selected course for editing.  This is not a subclass of
 *  {@link GraderCourseComponent}, since that class is about being able to
 *  set selections from direct action parameters, while this class is only
 *  about creating/editing course offerings using a selection that may
 *  not yet be stored in the user's {@link CoreSelections}.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.2 $, $Date: 2010/09/27 04:20:41 $
 */
public class GraderCourseEditComponent
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new object.
     *
     * @param context The page's context
     */
    public GraderCourseEditComponent( WOContext context )
    {
        super( context );
    }


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Access the currently selected course offering.
     * @return The current course offering
     */
    public CourseOffering courseOffering()
    {
        return (offering == null)
            ? coreSelections().courseOffering()
            : offering;
    }


    // ----------------------------------------------------------
    /**
     * Set the currently selected course offering.
     * @param courseOffering The course offering to edit
     */
    public void setCourseOffering(CourseOffering courseOffering)
    {
        offering = courseOffering;
    }


    // ----------------------------------------------------------
    public WOComponent pageWithName( String name )
    {
        WOComponent result = super.pageWithName( name );
        if (offering != null && result instanceof GraderCourseEditComponent)
        {
            ((GraderCourseEditComponent)result).setCourseOffering(offering);
        }
        return result;
    }


    // ----------------------------------------------------------
    public boolean applyLocalChanges()
    {
        boolean result = super.applyLocalChanges();
        if (result && offering != null)
        {
            coreSelections().setCourseOfferingRelationship(offering);
        }
        return result;
    }


    //~ Instance/static variables .............................................

    private CourseOffering offering;
}
