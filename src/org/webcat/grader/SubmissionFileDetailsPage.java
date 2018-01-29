/*==========================================================================*\
 |  $Id: SubmissionFileDetailsPage.java,v 1.4 2011/05/19 16:53:03 stedwar2 Exp $
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
import com.webobjects.foundation.NSNumberFormatter;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class presents the details for one file in a submission,
 * including all markup comments and a color-highlighted version
 * of the source code.
 *
 * @author  Stephen Edwards
 * @author  Latest changes by: $Author: stedwar2 $
 * @version $Revision: 1.4 $, $Date: 2011/05/19 16:53:03 $
 */
public class SubmissionFileDetailsPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     * @param context The page's context
     */
    public SubmissionFileDetailsPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public String codeWithComments = null;
    public WODisplayGroup filesDisplayGroup;
    public SubmissionFileStats thisFile;
    public SubmissionFileStats file;
    public SubmissionFileStats selectedFile = null;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug( "beginning appendToResponse()" );
        if ( thisFile == null )
        {
            thisFile = prefs().submissionFileStats();
        }
        codeWithComments = initializeCodeWithComments();
        filesDisplayGroup.setObjectArray(
            thisFile.submissionResult().submissionFileStats() );
        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    protected void afterAppendToResponse(WOResponse response, WOContext context)
    {
        super.afterAppendToResponse(response, context);
        codeWithComments = null;
        log.debug( "ending appendToResponse()" );
    }


    // ----------------------------------------------------------
    public String initializeCodeWithComments()
    {
        try
        {
            String result = thisFile.codeWithComments(
                user(), false, context().request() );
            return result;
        }
        catch ( Exception e )
        {
            error(
                "An error occurred while trying to prepare the source code "
                + "view for this file.  The error has been reported to the "
                + "administrator." );
            return null;
        }
    }


    // ----------------------------------------------------------
    public WOComponent viewNextFile()
    {
        log.debug( "viewNextFile()" );
        if ( selectedFile == null )
        {
            log.debug( "viewNextFile(): returning next()" );
            return next();
        }
        else
        {
            prefs().setSubmissionFileStatsRelationship( selectedFile );
            prefs().setSubmissionRelationship(
                thisFile.submissionResult().submissionFor(user()));
            SubmissionFileDetailsPage statsPage = (SubmissionFileDetailsPage)
                pageWithName(
                    SubmissionFileDetailsPage.class.getName() );
            statsPage.thisFile = selectedFile;
            statsPage.nextPage = nextPage;
            log.debug( "viewNextFile(): returning new page" );
            return statsPage;
        }
    }


    // ----------------------------------------------------------
    @Override
    public String title()
    {
        Submission submission =
            thisFile.submissionResult().submissionFor(user());
        AssignmentOffering offering = submission.assignmentOffering();
        return offering.courseOffering().compactName()
            + ": "
            + offering.assignment().name()
            + " try #"
            + submission.submitNumber()
            + " ("
            + formatter.format(
                thisFile.submissionResult().finalScoreVisibleTo(user()))
            + "/"
            + formatter.format(
                offering.assignment().submissionProfile().availablePoints())
            + ")";
    }


    //~ Instance/static variables .............................................

    private static final NSNumberFormatter formatter =
        new NSNumberFormatter("0.0");
    static Logger log = Logger.getLogger( SubmissionFileDetailsPage.class );
}
