/*==========================================================================*\
 |  $Id$
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

package net.sf.webcat.grader;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Allow the user to add/remove lab or program assignment partners on
 * this assignment (who will also be able to see the submission and its
 * results).
 *
 * @author Stephen Edwards
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class EditPartnersPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public EditPartnersPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public SubmissionResult    result;

    public WODisplayGroup      partnerDisplayGroup;
    public Submission          partnerSubmission;
    public int                 partnerIndex;

    public WODisplayGroup      studentDisplayGroup;
    public User                student;
    public int                 studentIndex;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void _appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "selected submission = "
                   + prefs().submission().submitNumber()
                   + " for " + prefs().submission().user().userName() );
        if (result == null)
        {
            result = prefs().submission().result();
        }
        if ( result == null )
        {
            log.error( "appendToResponse(): null submission result" );
        }
        partnerDisplayGroup.setMasterObject( result );
        studentDisplayGroup.setMasterObject(
            coreSelections().courseOffering() );
        super._appendToResponse( response, context );
    }


    public boolean alreadyPartner()
    {
        boolean myResult = false;
        for ( int i = 0; i < partnerDisplayGroup.allObjects().count(); i++ )
        {
            Submission sub = (Submission)
                partnerDisplayGroup.allObjects().objectAtIndex( i );
            if ( student == sub.user() )
            {
                myResult = true;
                break;
            }
        }
        return myResult;
    }


    // ----------------------------------------------------------
    public boolean originalSubmission()
    {
        return !partnerSubmission.partnerLink();
    }


    // ----------------------------------------------------------
    public WOComponent addPartner()
    {
        int submitNumber = 1;
        NSArray<Submission> studentSubmissions =
            Submission.objectsMatchingValues(
                localContext(),
                Submission.ASSIGNMENT_OFFERING_KEY,
                prefs().assignmentOffering(),
                Submission.USER_KEY,
                student);
        for (Submission thisSubmission : studentSubmissions)
        {
            int thisSubNo = thisSubmission.submitNumber();
            if ( thisSubNo  >= submitNumber )
            {
                submitNumber = thisSubNo + 1;
            }
        }
        // Don't need the return value: we just want it to be created, and
        // partnerSubmission() will save the changes to the DB
        result.submission().partnerSubmission(
                student, submitNumber, localContext());
        applyLocalChanges();
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent removePartner()
    {
        log.debug( "selected submission = "
                   + result.submission().submitNumber()
                   + " for " + result.submission().user().userName() );
        log.debug( "removing submission " + partnerSubmission.submitNumber()
                   + " for " + partnerSubmission.user().userName() );
        partnerSubmission.setResultRelationship( null );
        localContext().deleteObject( partnerSubmission );
        applyLocalChanges();
        return null;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( EditPartnersPage.class );
}