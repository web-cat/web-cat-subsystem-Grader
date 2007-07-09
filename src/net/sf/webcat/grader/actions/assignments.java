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

package net.sf.webcat.grader.actions;

import com.webobjects.appserver.*;

import er.extensions.*;

// -------------------------------------------------------------------------
/**
 * This direct action class handles generation of assignment
 * definitions published for the BlueJ submitter extension.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class assignments
    extends ERXDirectAction
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new DirectAction object.
     *
     * @param aRequest The request to respond to
     */
    public assignments( WORequest aRequest )
    {
        super( aRequest );
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Generate a list of BlueJ assignment submission targets in the format
     * used by the BlueJ submitter extension.
     *
     * @return A new BlueJSubmitterDefinitions page
     */
    public WOActionResults bluejAction()
    {
        return pageWithName( BlueJSubmitterDefinitions.class.getName() );
    }


    // ----------------------------------------------------------
    /**
     * Generate a list of assignment submission targets, in the format
     * used by the Eclipse project submitter extension.
     *
     * @return A new EclipseSubmitterDefinitions page
     */
    public WOActionResults eclipseAction()
    {
        return pageWithName( EclipseSubmitterDefinitions.class.getName() );
    }


    // ----------------------------------------------------------
    /**
     * Generate a list of assignment submission targets, in the format
     * used by the Eclipse project submitter extension.
     *
     * @return A new EclipseSubmitterDefinitions page
     */
    public WOActionResults icalAction()
    {
        return pageWithName( ICalView.class.getName() );
    }


    // ----------------------------------------------------------
    /**
     * The default action simply calls bluejAction().
     *
     * @return A new BlueJSubmitterDefinitions page
     */
    public WOActionResults defaultAction()
    {
        return bluejAction();
    }


    // ----------------------------------------------------------
    /**
     * Dispatch an action.
     * @param actionName The name of the action to dispatch
     * @return the action's result
     */
    public WOActionResults performActionNamed( String actionName )
    {
        if ( "ical.ics".equals( actionName ) )
        {
            actionName = "ical";
        }
        return super.performActionNamed( actionName );
    }
}
