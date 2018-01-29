/*==========================================================================*\
 |  $Id: CourseAndAssignmentResultOutcomesAssistant.java,v 1.2 2012/05/09 16:22:44 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2012 Virginia Tech
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

package org.webcat.grader.objectquery;

import com.webobjects.appserver.*;

//-------------------------------------------------------------------------
/**
 * A simplified query assistant that allows the user to select all the
 * result outcomes from submissions from one or more assignment offerings that
 * are common across a specified set of course offerings.
 *
 * Right now the code for this component is no different than the code for the
 * CourseAndAssignmentSubmissionsAssistant -- it is the models that differ, and
 * those places where the component UI is different act upon the models
 * directly instead of going through methods or properties on this class.  For
 * this reason, currently, this class merely subclasses
 * CourseAndAssignmentSubmissionsAssistant and adds nothing to it.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.2 $, $Date: 2012/05/09 16:22:44 $
 */
public class CourseAndAssignmentResultOutcomesAssistant
    extends CourseAndAssignmentSubmissionsAssistant
{
    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Create a new object.
     * @param context the page's context
     */
    public CourseAndAssignmentResultOutcomesAssistant(WOContext context)
    {
        super(context);
    }
}
