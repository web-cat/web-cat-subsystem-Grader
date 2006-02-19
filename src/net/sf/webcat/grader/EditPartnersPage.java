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
 * @version $Id$
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
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "selected submission = "
                   + prefs().submission().submitNumber()
                   + " for " + prefs().submission().user().userName() );
        result = prefs().submission().result();
        if ( result == null )
        {
            log.error( "appendToResponse(): null submission result" );
        }
        partnerDisplayGroup.setMasterObject( result );
        studentDisplayGroup.setMasterObject(
            wcSession().courseOffering() );
        super.appendToResponse( response, context );
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
        NSArray studentSubmissions = EOUtilities.objectsMatchingValues(
                wcSession().localContext(),
                Submission.ENTITY_NAME,
                new NSDictionary(
                    new Object[] { prefs().assignmentOffering(),
                                   student
                         },
                    new Object[] { Submission.ASSIGNMENT_OFFERING_KEY,
                                   Submission.USER_KEY }
                )                
            );
        for ( int i = 0; i < studentSubmissions.count(); i++ )
        {
            Submission thisSubmission = (Submission)
                studentSubmissions.objectAtIndex( i );
            int thisSubNo = thisSubmission.submitNumber();
            if ( thisSubNo  >= submitNumber )
            {
                submitNumber = thisSubNo + 1;
            }
        }
        // Don't need the return value: we just want it to be created, and
        // partnerSubmission() will save the changes to the DB
        prefs().submission().partnerSubmission(
                student,
                submitNumber,
                wcSession().localContext() );
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent removePartner()
    {
        log.debug( "selected submission = "
                   + prefs().submission().submitNumber()
                   + " for " + prefs().submission().user().userName() );
        log.debug( "removing submission " + partnerSubmission.submitNumber()
                   + " for " + partnerSubmission.user().userName() );
        partnerSubmission.setResultRelationship( null );
        wcSession().localContext().deleteObject( partnerSubmission );
        return null;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( EditPartnersPage.class );
}