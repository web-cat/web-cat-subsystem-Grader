/*==========================================================================*\
 |  $Id: CmsResponse.java,v 1.2 2010/09/27 04:17:43 stedwar2 Exp $
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

//-------------------------------------------------------------------------
/**
 *  The XML response page for CMS requests.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.2 $, $Date: 2010/09/27 04:17:43 $
 */
public class CmsResponse
    extends WOComponent
{

    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new Grader subsystem object.
     * @param context The page's context
     */
    public CmsResponse( WOContext context )
    {
        super( context );
    }

    //~ KVC Attributes (must be public) .......................................

    public String         key;
    public WODisplayGroup paramDisplayGroup;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        paramDisplayGroup.setObjectArray( context().request().formValueKeys() );
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public String keyValue()
    {
        return context().request().stringFormValueForKey( key );
    }
}