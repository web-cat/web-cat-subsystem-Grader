/*==========================================================================*\
 |  $Id: PageWithAssignmentNavigation.java,v 1.1 2011/05/02 19:37:34 aallowat Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2011 Virginia Tech
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

import org.webcat.core.PageWithCourseNavigation;
import org.webcat.core.WCPageWithNavigation;
import com.webobjects.appserver.WOContext;

//-------------------------------------------------------------------------
/**
 * A page wrapper that should be used instead of {@link WCPageWithNavigation}
 * on pages that need to let users view and change the currently selected
 * assignment. This page wrapper includes a breadcrumb-style set of drop-down
 * lists to change the assignment selection.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author: aallowat $
 * @version $Revision: 1.1 $, $Date: 2011/05/02 19:37:34 $
 */
public class PageWithAssignmentNavigation extends PageWithCourseNavigation
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Initializes a new {@code PageWithAssignmentNavigation}.
     *
     * @param context the context
     */
    public PageWithAssignmentNavigation(WOContext context)
    {
        super(context);
    }


    //~ KVC attributes (must be public) .......................................

    public boolean hideClosedAssignmentsFromStudents;
}
