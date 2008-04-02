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

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import er.extensions.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipFile;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class presents the list of scripts (grading steps) that
 * are available for selection.
 *
 * @author Stephen Edwards
 * @version $Id$
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
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "appendToResponse()" );
        step = prefs().step();
        if ( baseDir == null )
        {
            baseDir = new java.io.File ( ScriptFile.userScriptDirName(
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
        super.appendToResponse( response, context );
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


    //~ Instance/static variables .............................................

    static Logger log =
        Logger.getLogger( EditReusableScriptParametersPage.class );
}
