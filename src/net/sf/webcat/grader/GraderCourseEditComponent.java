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

import net.sf.webcat.core.*;

// -------------------------------------------------------------------------
/**
 *  A custom version of {@link GraderComponent} that managed a currently
 *  selected course for editing.  This is not a subclass of
 *  {@link GraderCourseComponent}, since that class is about being able to
 *  set selections from direct action parameters, while this class is only
 *  about creating/editing course offerings using a selection that may
 *  not yet be stored in the user's {@link CoreSelections}.
 *
 *  @author  stedwar2
 *  @version Feb 4, 2008
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
            coreSelections().setCourseOffering(offering);
        }
        return result;
    }


    //~ Instance/static variables .............................................

    private CourseOffering offering;
}
