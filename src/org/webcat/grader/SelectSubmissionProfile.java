/*==========================================================================*\
 |  $Id: SelectSubmissionProfile.java,v 1.3 2011/12/25 21:11:41 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2011 Virginia Tech
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
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.core.messaging.UnexpectedExceptionMessage;

// -------------------------------------------------------------------------
/**
 * This class presents the list of current submission profiles that
 * are available for selection.
 *
 * @author  Stephen Edwards
 * @author  Last changed by: $Author: stedwar2 $
 * @version $Revision: 1.3 $, $Date: 2011/12/25 21:11:41 $
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
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        NSMutableArray<EOQualifier> qualifiers =
            new NSMutableArray<EOQualifier>();

        // Look for submission profiles authored by this individual
        qualifiers.addObject( new EOKeyValueQualifier(
                                      SubmissionProfile.AUTHOR_KEY,
                                      EOQualifier.QualifierOperatorEqual,
                                      user()
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
            new UnexpectedExceptionMessage(e, context(), null,
                    "Exception searching for sub profiles").send();
            try
            {
                // Try fetching all of the submission profiles, which should
                // deepen all the relationships (I hope!)
                EOUtilities.objectsForEntityNamed(
                    localContext(),
                    SubmissionProfile.ENTITY_NAME );
                gradingProfileDisplayGroup.setQualifier(
                    new EOOrQualifier( qualifiers ) );
                gradingProfileDisplayGroup.fetch();
            }
            catch ( Exception e2 )
            {
                log.error( "2nd exception searching for sub profiles", e2 );
                new UnexpectedExceptionMessage(e2, context(), null,
                    "2nd exception searching for sub profiles")
                    .send();
                // OK, just kill the second part of the qualifier to
                // get only this user's stuff
                gradingProfileDisplayGroup.setQualifier(
                    qualifiers.objectAtIndex( 0 ) );
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

        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    /* Checks for errors, then records the currently selected item.
     *
     * @returns true if the next page should be a detail edit view
     */
    protected boolean saveSelectionCheckForEditing()
    {
        if ( selectedIndex == -1 && !createNew )
        {
            error(
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
        if ( saveSelectionCheckForEditing() || !hasMessages() )
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
        else
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
        else
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
        else
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
        localContext().insertObject( newProfile );
        Assignment selectedAssignment =
            prefs().assignmentOffering().assignment();
        selectedAssignment.setSubmissionProfileRelationship( newProfile );
        newProfile.setAuthor( user() );
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( SelectSubmissionProfile.class );
}
