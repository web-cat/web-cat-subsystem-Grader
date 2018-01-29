/*==========================================================================*\
 |  $Id: EditPluginGlobalsPage.java,v 1.2 2010/09/27 04:19:54 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2010 Virginia Tech
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
 *  This class allows one to edit the global settings for a plug-in.
 *  The creator of this page must set the plugin attribute before
 *  rendering the page.
 *
 *  @author  Stephen Edwards
 *  @author  Latest changes by: $Author: stedwar2 $
 *  @version $Revision: 1.2 $, $Date: 2010/09/27 04:19:54 $
 */
public class EditPluginGlobalsPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public EditPluginGlobalsPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public GradingPlugin   plugin;
    public java.io.File baseDir;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug( "appendToResponse()" );
        if ( baseDir == null )
        {
            baseDir = new java.io.File ( GradingPlugin.userScriptDirName(
                user(), true ).toString() );
        }
        if ( log.isDebugEnabled() )
        {
            log.debug( "plug-in global settings =\n"
                + plugin.globalConfigSettings() );
        }
        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        return null;
    }
}
