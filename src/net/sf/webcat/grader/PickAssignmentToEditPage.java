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
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.ERXValueUtilities;
import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 *  This page presents a list of assignments available for the user
 *  to choose from.
 *
 *  @author  Stephen Edwards
 *  @version $Id$
 */
public class PickAssignmentToEditPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new PickAssignmentPage object.
     * 
     * @param context The context to use
     */
    public PickAssignmentToEditPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public AssignmentOffering anAssignmentOffering;
    public WODisplayGroup     assignmentDisplayGroup;
    public int                index         = -1;
    public int                selectedIndex = -1;
    public boolean            createNew;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "starting appendToResponse()" );
        createNew = false;
        selectedIndex = -1;
        assignmentDisplayGroup.setQualifier(
                new EOKeyValueQualifier( AssignmentOffering.COURSE_OFFERING_KEY,
                                         EOQualifier.QualifierOperatorEqual,
                                         wcSession().courseOffering() )
            );
        assignmentDisplayGroup.fetch();
        if ( assignmentDisplayGroup.displayedObjects().count() == 0 )
        {
            log.debug( "attempting second group fetch" );
            assignmentDisplayGroup.fetch();
        }
        AssignmentOffering selectedAssignment = prefs().assignmentOffering();
        if ( selectedAssignment != null )
        {
            selectedIndex =
                assignmentDisplayGroup.displayedObjects().
                    indexOfIdenticalObject( selectedAssignment );
            if ( selectedIndex == NSArray.NotFound )
            {
                prefs().setAssignmentOfferingRelationship( null );
                selectedAssignment = null;
            }
        }
        if ( selectedAssignment == null  &&
             assignmentDisplayGroup.displayedObjects().count() > 0 )
        {
            selectedIndex = 0;
            prefs().setAssignmentOfferingRelationship(
                    (AssignmentOffering)assignmentDisplayGroup
                        .displayedObjects().objectAtIndex( selectedIndex )
                );
        }
        createNew = selectedIndex == -1 && canCreate();

        super.appendToResponse( response, context );
        log.debug( "ending appendToResponse()" );
    }


    // ----------------------------------------------------------
    public boolean nextEnabled()
    {
        return (  assignmentDisplayGroup.displayedObjects().count() > 0
               || canCreate() )
            &&  super.nextEnabled();
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        if ( selectedIndex == -1 && !createNew )
        {
            errorMessage( "You must choose an assignment to proceed." );
            return null;
        }
        else if ( createNew )
        {
            WCComponent createPage = (WCComponent)pageWithName(
                CreateAssignmentPage.class.getName() );
            createPage.nextPage = this;
            return createPage;
        }
        else // if ( selectedIndex > -1 )
        {
            log.debug( "existing assignment selected ("
                       + selectedIndex
                       + ")" );
            prefs().setAssignmentOfferingRelationship(
                (AssignmentOffering)assignmentDisplayGroup
                    .displayedObjects().objectAtIndex( selectedIndex ) );
            return super.next();
        }
    }


    // ----------------------------------------------------------
    public void cancelLocalChanges()
    {
        NSDictionary config = wcSession().tabs.selectedDescendant().config();
        if ( config != null
             && config.objectForKey( "resetPrimeUser" ) != null )
        {
            wcSession().setLocalUser( wcSession().primeUser() );
        }
        super.cancelLocalChanges();
    }


    // ----------------------------------------------------------
    public boolean showTable()
    {
        return assignmentDisplayGroup.displayedObjects().count() > 0
            || canCreate();
    }


    // ----------------------------------------------------------
    public boolean canCreate()
    {
        NSDictionary config = wcSession().tabs.selectedDescendant().config();
        return config != null &&
            ERXValueUtilities.booleanValueWithDefault(
                config.objectForKey( "allowCreate" ), false );
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( PickAssignmentToEditPage.class );
}
