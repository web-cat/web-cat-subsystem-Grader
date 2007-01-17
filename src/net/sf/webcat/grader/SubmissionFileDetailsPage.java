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
import com.webobjects.foundation.*;

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class presents the details for one file in a submission,
 * including all markup comments and a color-highlighted version
 * of the source code.
 *
 * @author Stephen Edwards
 * @version $Id$
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
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "beginning appendToResponse()" );
        if ( thisFile == null )
        {
            thisFile = prefs().submissionFileStats();
        }
        codeWithComments = initializeCodeWithComments();
        filesDisplayGroup.setObjectArray(
            prefs().submission().result().submissionFileStats() );
        super.appendToResponse( response, context );
        codeWithComments = null;
        log.debug( "ending appendToResponse()" );
    }


    // ----------------------------------------------------------
    public String initializeCodeWithComments()
    {
        try
        {
            String result = thisFile.codeWithComments( 
                wcSession().user(), false );
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
            SubmissionFileDetailsPage statsPage = (SubmissionFileDetailsPage)
                pageWithName(
                    SubmissionFileDetailsPage.class.getName() );
            statsPage.thisFile = selectedFile;
            statsPage.nextPage = nextPage;
            log.debug( "viewNextFile(): returning new page" );
            return statsPage;
        }
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( SubmissionFileDetailsPage.class );
}
