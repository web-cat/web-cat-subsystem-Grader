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

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

//-------------------------------------------------------------------------
/**
 *  This class allows one to edit the global settings for a plug-in.
 *  The creator of this page must set the plugin attribute before
 *  rendering the page.
 *
 *  @author Stephen Edwards
 *  @version $Id$
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

    public ScriptFile   plugin;
    public java.io.File baseDir;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "appendToResponse()" );
        if ( baseDir == null )
        {
            baseDir = new java.io.File ( ScriptFile.userScriptDirName(
                wcSession().user(), true ).toString() );
        }
        if ( log.isDebugEnabled() )
        {
            log.debug( "plug-in global settings =\n"
                + plugin.globalConfigSettings() );
        }
        super.appendToResponse( response, context );
    }
}
