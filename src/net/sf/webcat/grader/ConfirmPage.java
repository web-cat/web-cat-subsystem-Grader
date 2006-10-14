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
import net.sf.webcat.core.WCComponent;


// -------------------------------------------------------------------------
/**
 * A base class for stackable confirmation pages in wizard steps.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class ConfirmPage
    extends WCComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     * 
     * @param context The page's context
     */
    public ConfirmPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public String           message;
    public String           actionOk;
    public String           actionCancel;
    public NSKeyValueCoding actionReceiver;
    public boolean          hideSteps = false;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public WOComponent okClicked()
    {
        WOComponent next = null;
        if ( actionOk != null )
        {
            next = (WOComponent)actionReceiver.valueForKey( actionOk );
        }
        if ( next == null )
        {
            next = super.next();
        }
        return next;
    }


    // ----------------------------------------------------------
    public WOComponent cancelClicked()
    {
        WOComponent next = null;
        if ( actionCancel != null )
        {
            next = (WOComponent)actionReceiver.valueForKey( actionCancel );
        }
        if ( next == null )
        {
            next = super.next();
        }
        return next;
    }


    // ----------------------------------------------------------
    public String title()
    {
        return title;
    }


    // ----------------------------------------------------------
    public void setTitle( String newTitle )
    {
        title = newTitle;
    }


    //~ Instance/static variables .............................................

    private String title = "Confirm Action";
}
