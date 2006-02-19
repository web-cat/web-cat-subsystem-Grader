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

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class presents the list of current submission profiles that
 * are available for selection.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class SelectSubmissionProfile
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     * 
     * @param context The page's context
     */
    public SelectSubmissionProfile( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup    gradingProfileDisplayGroup;
    public SubmissionProfile submissionProfile; // For Repetition1
    public int               selectedIndex = -1;
    public int               index         = -1;
    public boolean           createNew     = false;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        NSMutableArray qualifiers = new NSMutableArray();

        // Look for submission profiles authored by this individual
        qualifiers.addObject( new EOKeyValueQualifier(
                                      SubmissionProfile.AUTHOR_KEY,
                                      EOQualifier.QualifierOperatorEqual,
                                      wcSession().user()
                                  ) );
        // Also look for submission profiles used in this class
        qualifiers.addObject( new EOKeyValueQualifier(
                                      SubmissionProfile.COURSE_OFFERINGS_KEY,
                                      EOQualifier.QualifierOperatorContains,
                                      prefs().assignmentOffering()
                                          .courseOffering()
                                  ) );
        try
        {
            gradingProfileDisplayGroup.setQualifier(
                new EOOrQualifier( qualifiers ) );
            gradingProfileDisplayGroup.fetch();
        }
        catch ( Exception e )
        {
            log.error( "exception searching for sub profiles", e );
            Application.emailExceptionToAdmins(
                e, context(), "Exception searching for sub profiles" );
            try
            {
                // Try fetching all of the submission profiles, which should
                // deepen all the relationships (I hope!)
                EOUtilities.objectsForEntityNamed(
                    wcSession().localContext(),
                    SubmissionProfile.ENTITY_NAME );
                gradingProfileDisplayGroup.setQualifier(
                    new EOOrQualifier( qualifiers ) );
                gradingProfileDisplayGroup.fetch();
            }
            catch ( Exception e2 )
            {
                log.error( "2nd exception searching for sub profiles", e2 );
                Application.emailExceptionToAdmins(
                    e2, context(), "2nd exception searching for sub profiles" );
                // OK, just kill the second part of the qualifier to
                // get only this user's stuff
                gradingProfileDisplayGroup.setQualifier(
                    (EOQualifier)qualifiers.objectAtIndex( 0 ) );
                gradingProfileDisplayGroup.fetch();                    
            }
        }

        Assignment selectedAssignment =
            prefs().assignmentOffering().assignment();
        SubmissionProfile selectedProfile =
            selectedAssignment.submissionProfile();
        if ( selectedProfile != null )
        {
            selectedIndex = gradingProfileDisplayGroup.displayedObjects()
                .indexOfIdenticalObject( selectedProfile );
            if ( selectedIndex == NSArray.NotFound )
            {
                log.error( "how can this be?" );
                selectedAssignment
                    .setSubmissionProfileRelationship( null );
                selectedProfile = null;
            }
        }
        if ( selectedProfile == null  &&
             gradingProfileDisplayGroup.displayedObjects().count() > 0 )
        {
            selectedIndex = 0;
            selectedAssignment.setSubmissionProfileRelationship(
                    (SubmissionProfile)gradingProfileDisplayGroup
                        .displayedObjects().objectAtIndex( selectedIndex ) );
        }

        if ( selectedIndex < 0 && !createNew )
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
    protected boolean saveSelectionCheckForEditing()
    {
        clearErrors();
        if ( selectedIndex == -1 && !createNew )
        {
            errorMessage(
                "You must choose a submission rule profile to proceed." );
            return false;
        }
        else if ( createNew )
        {
            createNewProfile();
            return true;
        }
        else
        {
            log.debug( "existing profile selected ("
                       + selectedIndex
                       + ")" );
            clearErrors();
            Assignment selectedAssignment =
                prefs().assignmentOffering().assignment();
            selectedAssignment.setSubmissionProfileRelationship(
                    (SubmissionProfile)gradingProfileDisplayGroup
                        .displayedObjects().objectAtIndex( selectedIndex ) );
            return false;
        }
    }


    // ----------------------------------------------------------
    public WOComponent editSubmissionProfile()
    {
        WCComponent result = null;
        if ( saveSelectionCheckForEditing() || !hasErrors() )
        {
            result = (WCComponent)pageWithName(
                EditSubmissionProfilePage.class.getName() );
            result.nextPage = nextPage;
        }
        return result;
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        WOComponent result = null;
        if ( saveSelectionCheckForEditing() )
        {
            WCComponent comp = (WCComponent)pageWithName(
                EditSubmissionProfilePage.class.getName() );
            comp.nextPage = nextPage;
            result = comp;
        }
        else if ( !hasErrors() )
        {
            result = super.next();
        }
        return result;
    }


    // ----------------------------------------------------------
    public WOComponent finish()
    {
        WOComponent result = null;
        if ( saveSelectionCheckForEditing() )
        {
            WCComponent comp = (WCComponent)pageWithName(
                EditSubmissionProfilePage.class.getName() );
            comp.nextPage = nextPage;
            result = comp;
        }
        else if ( !hasErrors() )
        {
            result = super.finish();
        }
        return result;
    }


    // ----------------------------------------------------------
    public WOComponent apply()
    {
        WOComponent result = null;
        if ( saveSelectionCheckForEditing() )
        {
            WCComponent comp = (WCComponent)pageWithName(
                EditSubmissionProfilePage.class.getName() );
            comp.nextPage = nextPage;
            result = comp;
        }
        else if ( !hasErrors() )
        {
            result = super.apply();
        }
        return result;
    }


    // ----------------------------------------------------------
    public void createNewProfile()
    {
        log.debug( "createNewProfile()" );
        SubmissionProfile newProfile = new SubmissionProfile();
        wcSession().localContext().insertObject( newProfile );
        Assignment selectedAssignment =
            prefs().assignmentOffering().assignment();
        selectedAssignment.setSubmissionProfileRelationship( newProfile );
        newProfile.setAuthor( wcSession().user() );
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( SelectSubmissionProfile.class );
}
