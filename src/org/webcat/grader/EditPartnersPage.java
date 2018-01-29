/*==========================================================================*\
 |  $Id: EditPartnersPage.java,v 1.4 2010/09/27 04:19:54 stedwar2 Exp $
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
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 * Allow the user to add/remove lab or program assignment partners on
 * this assignment (who will also be able to see the submission and its
 * results).
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.4 $, $Date: 2010/09/27 04:19:54 $
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
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
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
        super.beforeAppendToResponse( response, context );
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
        // Don't need the return value: we just want it to be created, and
        // partnerSubmission() will save the changes to the DB
        result.submission().partnerWith(student);
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
