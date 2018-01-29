/*==========================================================================*\
 |  $Id: EditReusableScriptParametersPage.java,v 1.2 2010/09/27 04:19:54 stedwar2 Exp $
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
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class presents the list of scripts (grading steps) that
 * are available for selection.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.2 $, $Date: 2010/09/27 04:19:54 $
 */
public class EditReusableScriptParametersPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public EditReusableScriptParametersPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public Step              step;
    public WODisplayGroup    assignmentStepGroup;
    public Step              otherAssignmentStep;
    public int               index;
    public java.io.File      baseDir;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug( "appendToResponse()" );
        step = prefs().step();
        if ( baseDir == null )
        {
            baseDir = new java.io.File ( GradingPlugin.userScriptDirName(
                user(), true ).toString() );
        }
        if ( step.config() == null )
        {
            log.debug( "null config detected, populating it" );
            StepConfig newConfig = new StepConfig();
            localContext().insertObject( newConfig );
            step.setConfigRelationship( newConfig );
            newConfig.setAuthor( user() );
        }
        assignmentStepGroup.setObjectArray( step.config().steps() );
        if ( log.isDebugEnabled() )
        {
            log.debug( "assignment option values =\n" + step.configSettings() );
            log.debug( "shared option values =\n" + step.config().configSettings() );
        }
        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "new assignment option values =\n"
                       + step.configSettings() );
            log.debug( "new shared option values =\n"
                       + step.config().configSettings() );
        }
        applyLocalChanges();
        return super.next();
    }


    // ----------------------------------------------------------
    public WOComponent resetOptions()
    {
        step.config().configSettings().removeAllObjects();
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        return null;
    }


    // ----------------------------------------------------------
    public String title()
    {
        String plugin = "Plug-in";
        if (step != null && step.gradingPlugin() != null)
        {
            plugin = step.gradingPlugin().displayableName();
        }
        return "Edit Reusable Options for " + plugin;
    }


    //~ Instance/static variables .............................................

    static Logger log =
        Logger.getLogger( EditReusableScriptParametersPage.class );
}
