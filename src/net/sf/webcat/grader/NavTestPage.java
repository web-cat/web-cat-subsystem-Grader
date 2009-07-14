/*==========================================================================*\
 |  $Id$
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

package net.sf.webcat.grader;
import org.apache.log4j.Logger;
import com.webobjects.appserver.*;
import net.sf.webcat.core.Theme;
import net.sf.webcat.core.WCComponent;


//-------------------------------------------------------------------------
/**
* Represents a standard Web-CAT page that has not yet been implemented
* (is "to be defined").
*
*  @author Stephen Edwards
*  @version $Id$
*/
public class NavTestPage
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new TBDPage object.
     *
     * @param context The context to use
     */
    public NavTestPage( WOContext context )
    {
        super( context );
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse(WOResponse response, WOContext context)
    {
        log.debug("entering appendToResponse()");
        super.appendToResponse(response, context);
        log.debug("leaving appendToResponse()");
    }


    // ----------------------------------------------------------
    public void awake()
    {
        log.debug("entering awake()");
        super.awake();
        log.debug("leaving awake()");
    }


    // ----------------------------------------------------------
    public WOComponent refresh()
    {
    	return null;
    }


    // ----------------------------------------------------------
    public int count()
    {
    	return ++count;
    }


    //~ Instance/static variables .............................................

    private int count = 0;
    static Logger log = Logger.getLogger(NavTestPage.class);
}