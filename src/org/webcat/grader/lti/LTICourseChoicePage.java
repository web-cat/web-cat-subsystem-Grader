/*==========================================================================*\
 |  Copyright (C) 2018 Virginia Tech
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


package org.webcat.grader.lti;

import org.webcat.core.CourseOffering;
import org.webcat.grader.GraderCourseComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;

//-------------------------------------------------------------------------
/**
 * Allows instructors to pick the course offering(s) associated with their
 * LTI launch context.
 *
 * @author  Stephen Edwards
 */
public class LTICourseChoicePage
    extends GraderCourseComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new object.
     *
     * @param context The context to use
     */
    public LTICourseChoicePage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public GraderLTILaunchRequest ltiRequest;
    public NSArray<CourseOffering> courseOfferings;
    public CourseOffering anOffering;


    //~ Methods ...............................................................

    //~ Instance/static fields ................................................

}
