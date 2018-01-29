/*==========================================================================*\
 |  $Id: PickStepPage.java,v 1.2 2010/09/27 04:23:20 stedwar2 Exp $
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

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 * This class presents the list of scripts (grading steps) that
 * are available for selection.
 *
 * @author  Stephen Edwards
 * @author  Latest changes by: $Author: stedwar2 $
 * @version $Revision: 1.2 $, $Date: 2010/09/27 04:23:20 $
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
    public GradingPlugin  script;
    public WODisplayGroup assignmentGroup;
    public Assignment     assignment;
    public int            selectedAssignmentIndex = -1;
    public int            selectedScriptIndex     = -1;
    public int            publishedScriptIndex    = -1;
    public int            index                   = -1;
    public boolean        createNew               = false;
    public NSData         uploadedData;
    public String         uploadedName;
    public Assignment     targetAssignment;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void awake()
    {
        super.awake();
        log.debug( "awake()" );
        selectedAssignmentIndex = -1;
        selectedScriptIndex     = -1;
        publishedScriptIndex    = -1;
        createNew               = false;
    }


    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        if (targetAssignment == null)
        {
            targetAssignment = prefs().assignment();
            if (targetAssignment == null)
            {
                targetAssignment = prefs().assignmentOffering().assignment();
            }
        }
//        assignmentGroup.queryBindings().setObjectForKey(
//            coreSelections().courseOffering(),
//            "courseOffering"
//        );
//        assignmentGroup.queryBindings().setObjectForKey(
//            user(),
//            "user"
//        );
//        assignmentGroup.fetch();

        Course course = coreSelections().course();
        if (coreSelections().courseOffering() != null)
        {
            course = coreSelections().courseOffering().course();
        }
        scriptGroup.queryBindings().setObjectForKey(course, "course");
        scriptGroup.queryBindings().setObjectForKey(user(), "user");
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
            publishedScriptIndex = 0;//assignmentGroup.displayedObjects().count();
        }
        else if ( scriptGroup.displayedObjects().count() > 0 )
        {
            selectedScriptIndex = 0;//assignmentGroup.displayedObjects().count();
        }
        else
        {
            createNew = true;
        }

        super.beforeAppendToResponse( response, context );
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
        if ( log.isDebugEnabled() )
        {
            log.debug( "next():" );
            log.debug(" selected assignment = " + selectedAssignmentIndex );
            log.debug(" selected published  = " + publishedScriptIndex );
            log.debug(" selected script     = " + selectedScriptIndex );
            log.debug(" request = " + context().request() );
        }
        if ( createNew )
        {
            log.debug( "uploading a new script" );
            if ( uploadedName == null || uploadedData == null )
            {
                error( "Please select a file to upload." );
                return null;
            }
            GradingPlugin newScript = GradingPlugin.createNewGradingPlugin(
                localContext(),
                user(),
                uploadedName,
                uploadedData,
                false,
                true,
                messages()
            );
            if ( newScript != null )
            {
                targetAssignment.addNewStep(newScript);
            }
        }
//        else if ( selectedAssignmentIndex >= 0 )
//        {
//            log.debug(" selected assignment = " + selectedAssignmentIndex );
//            Assignment copyTo = targetAssignment;
//            Assignment copyFrom = (Assignment)assignmentGroup
//                .displayedObjects().objectAtIndex( selectedAssignmentIndex );
//            for (Step step : copyFrom.steps())
//            {
//                copyTo.copyStep(step, true);
//            }
//            applyLocalChanges();
//        }
        else if ( publishedScriptIndex >= 0 )
        {
//            publishedScriptIndex -= assignmentGroup.displayedObjects().count();
            log.debug(" selected published  = " + publishedScriptIndex );
            targetAssignment.addNewStep(
                (GradingPlugin)publishedScriptGroup.displayedObjects()
                    .objectAtIndex( publishedScriptIndex ) );
            applyLocalChanges();
        }
        else if ( selectedScriptIndex >= 0 )
        {
//            selectedScriptIndex -= assignmentGroup.displayedObjects().count();
            selectedScriptIndex -=
                publishedScriptGroup.displayedObjects().count();
            log.debug(" selected script     = " + selectedScriptIndex );
            targetAssignment.addNewStep(
                (GradingPlugin)scriptGroup.displayedObjects()
                    .objectAtIndex( selectedScriptIndex ) );
            applyLocalChanges();
        }
        else
        {
            error( "Please select an option before continuing." );
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
        return super.next();
    }


    // ----------------------------------------------------------
    public int index1()
    {
        return index;
    }


    // ----------------------------------------------------------
    public int index2()
    {
        return index1();// + assignmentGroup.displayedObjects().count();
    }


    // ----------------------------------------------------------
    public int index3()
    {
        return index2() + publishedScriptGroup.displayedObjects().count();
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( PickStepPage.class );
}
