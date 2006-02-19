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

//-------------------------------------------------------------------------
/**
*  The XML response page for CMS requests.
*
*  @author Stephen Edwards
*  @version $Id$
*/
public class CmsResponse
    extends WOComponent
{

    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new Grader subsystem object.
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