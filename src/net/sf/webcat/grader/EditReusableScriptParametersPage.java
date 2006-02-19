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

    public WODisplayGroup    optionGroup;
    public NSDictionary      option;
    public Step              step;
    public WODisplayGroup    assignmentStepGroup;
    public Step              otherAssignmentStep;
    public int               index;

    public NSArray           categories;
    public String            category;
    public String            chosenCategory;
    public String            displayedCategory;

    public static final String hideInfoKey = "EditScriptPageShowInfo";
    public static final String verboseOptionsKey =
        "EditScriptPageNoVerboseOptions";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "appendToResponse()" );
        step = prefs().step();
        if ( step.config() == null )
        {
            log.debug( "null config detected, populating it" );
            StepConfig newConfig = new StepConfig();
            wcSession().localContext().insertObject( newConfig );
            step.setConfigRelationship( newConfig );
            newConfig.setAuthor( wcSession().user() );
        }
        NSArray options = (NSArray)step.script().configDescription()
            .objectForKey( "options" );
        if ( categories == null )
        {
            categories = (NSArray)step.script().configDescription()
                .objectForKey( "optionCategories" );
            if ( categories != null && categories.count() > 0 )
            {
                chosenCategory = (String)categories.objectAtIndex( 0 );
            }
        }
        displayedCategory = chosenCategory;
        optionGroup.setObjectArray( options );
        assignmentStepGroup.setObjectArray( step.config().steps() );
        if ( log.isDebugEnabled() )
        {
            log.debug( "assignment option values =\n" + step.configSettings() );
            log.debug( "shared option values =\n" + step.config().configSettings() );
        }
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public void toggleHideInfo()
    {
        boolean hideInfo = ERXValueUtilities.booleanValue(
            wcSession().userPreferences.objectForKey( hideInfoKey ) );
        hideInfo = !hideInfo;
        wcSession().userPreferences.setObjectForKey(
            Boolean.valueOf( hideInfo ), hideInfoKey );
    }


    // ----------------------------------------------------------
    public void toggleVerboseOptions()
    {
        boolean verboseOptions = ERXValueUtilities.booleanValue(
            wcSession().userPreferences.objectForKey( verboseOptionsKey ) );
        verboseOptions = !verboseOptions;
        wcSession().userPreferences.setObjectForKey(
            Boolean.valueOf( verboseOptions ), verboseOptionsKey );
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
    public WOComponent resetAssignmentOptions()
    {
        step.configSettings().removeAllObjects();
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent resetOptions()
    {
        step.config().configSettings().removeAllObjects();
        return null;
    }


    // ----------------------------------------------------------
    public boolean canEditScript()
    {
        return wcSession().user().accessLevel() >= User.WEBCAT_RW_PRIVILEGES
            || step.script().author() == wcSession().user();
    }


    // ----------------------------------------------------------
    public boolean showThisOption()
    {
        return displayedCategory == null
           || displayedCategory.equals( option.valueForKey( "category" ) );
    }


    // ----------------------------------------------------------
    public WOComponent editScript()
    {
        EditScriptFilesPage newPage = (EditScriptFilesPage)
            pageWithName( EditScriptFilesPage.class.getName() );
        newPage.nextPage = this;
        newPage.scriptFile = step.script();
        newPage.isEditable = canEditScript();
        return newPage;
    }

    // ----------------------------------------------------------
    public WOComponent togglePublished()
    {
        step.script().setIsPublished( !step.script().isPublished() );
        return null;
    }
    
    // ----------------------------------------------------------
    public String togglePublishedLabel()
    {
        return
        step.script().isPublished()
            ? "Unpublish It"
            : "Publish It";
    }
    
    // ----------------------------------------------------------
    public String isPublished()
    {
        return
            step.script().isPublished()
                ? "yes"
                : "no";
    }


    // ----------------------------------------------------------
    /**
     * Force a fresh reload of the script's config.plist file to pick up
     * any changes (i.e., new attributes, new default values, etc.).
     * @return null, to force this page to reload in the browser when the
     *         action completes
     */
    public WOComponent reloadScriptDefinition()
    {
        clearErrors();
        String errMsg = step.script().initializeConfigAttributes();
        if ( errMsg != null )
        {
            errorMessage( errMsg );
        }
        categories = null;
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        return null;
    }
    
    // ----------------------------------------------------------
//    public WOComponent apply()
//    {
//        log.debug( "apply(): changes:\n"
//                   + wcSession().defaultEditingContext().updatedObjects() );
//        return super.apply();
//    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( EditReusableScriptParametersPage.class );
}
