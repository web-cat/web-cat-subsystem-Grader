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
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;

import java.util.*;

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
public class PickStepPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     * 
     * @param context The page's context
     */
    public PickStepPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup scriptGroup;
    public WODisplayGroup publishedScriptGroup;
    public ScriptFile     script;
    public WODisplayGroup assignmentGroup;
    public Assignment     assignment;
    public int            selectedAssignmentIndex = -1;
    public int            selectedScriptIndex     = -1;
    public int            publishedScriptIndex    = -1;
    public int            index                   = -1;
    public boolean        createNew               = false;
    public NSData         uploadedData;
    public String         uploadedName;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void awake()
    {
        log.debug( "awake()" );
        selectedAssignmentIndex = -1;
        selectedScriptIndex     = -1;
        publishedScriptIndex    = -1;
        createNew               = false;
    }


    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        assignmentGroup.queryBindings().setObjectForKey(
            wcSession().courseOffering(),
            "courseOffering"
        );
        assignmentGroup.queryBindings().setObjectForKey(
            wcSession().user(),
            "user"
        );
        assignmentGroup.fetch();
        
        scriptGroup.queryBindings().setObjectForKey(
            wcSession().courseOffering().course(),
            "course"
        );
        scriptGroup.queryBindings().setObjectForKey(
            wcSession().user(),
            "user"
        );
        scriptGroup.fetch();

//        NSMutableArray qualifiers = new NSMutableArray();
//
//        // Look for submission profiles authored by this individual
//        qualifiers.addObject( new EOKeyValueQualifier(
//                                      ScriptFile.AUTHOR_KEY,
//                                      EOQualifier.QualifierOperatorEqual,
//                                      wcSession().user()
//                                  ) );
//        // Also look for submission profiles used in this class
//        qualifiers.addObject(
//            new EOKeyValueQualifier(
//                ScriptFile.COURSE_OFFERINGS_KEY,
//                EOQualifier.QualifierOperatorContains,
//                wcSession().courseOffering()
//                ) );
//        try
//        {
//            scriptGroup.setQualifier(
//                new EOOrQualifier( qualifiers ) );
//            scriptGroup.fetch();
//        }
//        catch ( Exception e )
//        {
//            log.error( "exception searching for steps", e );
//            Application.emailExceptionToAdmins(
//                e, context(), "Exception searching for steps" );
//            try
//            {
//                // Try fetching all of the submission profiles, which should
//                // deepen all the relationships (I hope!)
//                EOUtilities.objectsForEntityNamed(
//                    wcSession().localContext(),
//                    ScriptFile.ENTITY_NAME );
//                scriptGroup.setQualifier(
//                    new EOOrQualifier( qualifiers ) );
//                scriptGroup.fetch();
//            }
//            catch ( Exception e2 )
//            {
//                log.error( "2nd exception searching for steps", e2 );
//                Application.emailExceptionToAdmins(
//                    e2, context(), "2nd exception searching for steps" );
//                // OK, just kill the second part of the qualifier to
//                // get only this user's stuff
//                scriptGroup.setQualifier(
//                    (EOQualifier)qualifiers.objectAtIndex( 0 ) );
//                scriptGroup.fetch();
//            }
//        }

//        Step selectedStep = prefs().step();
//        log.debug( "step = " + selectedStep );
//        ScriptFile selectedScript = selectedStep.script();
//        if ( selectedScript != null )
//        {
//            selectedScriptIndex = scriptGroup.displayedObjects()
//                .indexOfIdenticalObject( selectedScript );
//            if ( selectedScriptIndex == NSArray.NotFound )
//            {
//                log.error( "how can this be?" );
//                selectedStep.setScriptRelationship( null );
//                selectedScript = null;
//            }
//        }
//        if ( selectedScript == null  &&
//             scriptGroup.displayedObjects().count() > 0 )
//        {
//            selectedScriptIndex = 0;
//            selectedStep.setScriptRelationship(
//                    (ScriptFile)scriptGroup
//                        .displayedObjects().objectAtIndex(
//                                        selectedScriptIndex ) );
//        }

        if ( publishedScriptGroup.displayedObjects().count() > 0 )
        {
            publishedScriptIndex = assignmentGroup.displayedObjects().count();
        }
        else if ( scriptGroup.displayedObjects().count() > 0 )
        {
            selectedScriptIndex = assignmentGroup.displayedObjects().count();
        }
        else
        {
            createNew = true;
        }

        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    /* Checks for errors, then records the currently selected item.
     *
     * @returns true if the next page should be a detail edit view
     */
//    protected boolean saveSelectionCheckForEditing()
//    {
//        log.debug( "saving selection" );
//        if ( selectedScriptIndex == -1 && !createNew )
//        {
//            log.debug( "error detected in selection state" );
//            errorMessage( "You must choose a script to proceed." );
//            return false;
//        }
//        else if ( createNew )
//        {
//            // Must be creating a new script, but that is done by
//            // the EditScript page
//            log.debug( "need to upload a new script" );
//            Step selectedStep = prefs().step();
//            ScriptFile oldScript = selectedStep.script();
//            if ( oldScript != null )
//            {
//                selectedStep.setScriptRelationship( null );
//            }
//            return true;
//        }
//        else // if ( selectedIndex > -1 )
//        {
//            log.debug( "existing script selected ("
//                       + selectedScriptIndex
//                       + ")" );
//            clearErrors();
//            Step selectedStep = prefs().step();
//            selectedStep.setScriptRelationship(
//                    (ScriptFile)scriptGroup
//                        .displayedObjects().objectAtIndex(
//                                        selectedScriptIndex ) );
//            return false;
//        }
//    }


    // ----------------------------------------------------------
//    public WOComponent editScript()
//    {
//        ScriptFile scriptToEdit = script;
//        WOComponent result = null;
//        if ( saveSelectionCheckForEditing() || !hasErrors() )
//        {
//            // override whichever one was selected, and then choose the
//            // one that was edit-clicked instead.
//            // TODO: if the EditScript page used a separate selection,
//            // then we could independently edit and select without
//            // problems.
//            Step selectedStep = prefs().step();
//            selectedStep.setScriptRelationship( scriptToEdit );
//            WCComponent comp = (WCComponent)pageWithName(
//                EditScriptPage.class.getName() );
//            comp.nextPage = nextPage;
//            result = comp;
//        }
//        return result;
//    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        WOComponent result = null;
        if ( log.isDebugEnabled() )
        {
            log.debug( "next():" );
            log.debug(" selected assignment = " + selectedAssignmentIndex );
            log.debug(" selected published  = " + publishedScriptIndex );
            log.debug(" selected script     = " + selectedScriptIndex );
            log.debug(" request = " + context().request() );
        }
        clearErrors();
        if ( createNew )
        {
            log.debug( "uploading a new script" );
            if ( errors == null )
            {
                errors = new NSMutableDictionary();
            }
            if ( uploadedName == null || uploadedData == null )
            {
                errorMessage( "Please select a file to upload." );
                return null;
            }
            ScriptFile newScript = ScriptFile.createNewScriptFile(
                wcSession().localContext(),
                wcSession().user(),
                uploadedName,
                uploadedData,
                false,
                true,
                errors
            );
            if ( newScript != null )
            {
                prefs().assignmentOffering().assignment().addNewStep(
                    newScript );
            }
        }
        else if ( selectedAssignmentIndex >= 0 )
        {
            log.debug(" selected assignment = " + selectedAssignmentIndex );
            Assignment copyTo = prefs().assignmentOffering().assignment();
            Assignment copyFrom = (Assignment)assignmentGroup
                .displayedObjects().objectAtIndex( selectedAssignmentIndex );
            Enumeration steps = copyFrom.steps().objectEnumerator();
            while ( steps.hasMoreElements() )
            {
                copyTo.copyStep( (Step)steps.nextElement(), true );
            }
        }
        else if ( publishedScriptIndex >= 0 )
        {
            publishedScriptIndex -= assignmentGroup.displayedObjects().count();
            log.debug(" selected published  = " + publishedScriptIndex );
            prefs().assignmentOffering().assignment().addNewStep(
                (ScriptFile)publishedScriptGroup.displayedObjects()
                    .objectAtIndex( publishedScriptIndex ) );
        }
        else if ( selectedScriptIndex >= 0 )
        {
            selectedScriptIndex -= assignmentGroup.displayedObjects().count();
            selectedScriptIndex -=
                publishedScriptGroup.displayedObjects().count();
            log.debug(" selected script     = " + selectedScriptIndex );
            prefs().assignmentOffering().assignment().addNewStep(
                (ScriptFile)scriptGroup.displayedObjects()
                    .objectAtIndex( selectedScriptIndex ) );
        }
        else
        {
            errorMessage( "Please select an option before continuing." );
        }

//        if ( saveSelectionCheckForEditing() )
//        {
//            WCComponent comp = (WCComponent)pageWithName(
//                EditScriptPage.class.getName() );
//            comp.nextPage = nextPage;
//            result = comp;
//        }

        uploadedName = null;
        uploadedData = null;
        if ( !hasErrors() )
        {
            result = super.next();
        }
        return result;
    }


    // ----------------------------------------------------------
    public int index1()
    {
        return index;
    }


    // ----------------------------------------------------------
    public int index2()
    {
        return index1() + assignmentGroup.displayedObjects().count();
    }


    // ----------------------------------------------------------
    public int index3()
    {
        return index2() + publishedScriptGroup.displayedObjects().count();
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( PickStepPage.class );
}
