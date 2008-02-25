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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.sf.webcat.archives.ArchiveManager;
import net.sf.webcat.archives.IArchiveEntry;
import net.sf.webcat.core.*;

import org.apache.log4j.Logger;


// -------------------------------------------------------------------------
/**
 * This class summarizes the student's submission and asks for
 * confirmation before making it "official".
 *
 * @author Amit Kulkarni
 * @version $Id$
 */
public class ConfirmSubmissionPage
    extends GraderSubmissionUploadComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public ConfirmSubmissionPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    /** File object for the uploaded file */
    public IArchiveEntry file;
    /** index of the file in the repetition table */
    public int index;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * This function is used to provide a notion that the wizard remembers
     * the previous selection
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "The submission number is "
                   + prefs().submission().submitNumber() );
        if ( !submissionInProcess().hasValidFileUpload() )
        {
            WOComponent prevPage = back();
            if ( prevPage != null )
            {
                log.debug( "skipping to previous page" );
                response.setContent(
                    prevPage.generateResponse().content() );
                // skip calling super.appendToResponse
                return;
            }
        }
        if ( submissionInProcess().uploadedFileList() == null )
        {
            // Initialize the list of files contained in this submission,
            // which is cached in the wizard state as a non-persistent
            // NSArray field.  This array is an array of ZipEntry objects.
            try
            {
                submissionInProcess().setUploadedFileList( new NSArray(
                    ArchiveManager.getInstance().getContents(
                        submissionInProcess().uploadedFileName(),
                        submissionInProcess().uploadedFile().stream(),
                        submissionInProcess().uploadedFile().length()
                    ) ) );
            }
            catch ( Exception e )
            {
                submissionInProcess().clearUpload();
                error(
                    "An error occurred while unpacking "
                    + "your submission.  The error has been "
                    + "reported to the administrator.  If you "
                    + "have uploaded the wrong file by accident, "
                    + "use the Back button to try again." );
                Application.emailExceptionToAdmins(
                    e,
                    context(),
                    "Exception unzipping submission file"
                );
            }
        }
        else
        {
            log.debug( "file list has already been initialized" );
        }
        // preProcessSubmission();
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    /**
     * Returns a string version of the file size for the currrent file.
     *
     * @return the file size as a string
     */
    public String fileSize()
    {
        return Submission.fileSizeAsString( file.length() );
    }


    // ----------------------------------------------------------
    public boolean singleFile()
    {
        NSArray list = submissionInProcess().uploadedFileList();
        return list == null || list.count() <= 1;
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        NSTimestamp submitTime = new NSTimestamp();
        NSTimestamp deadline = new NSTimestamp(
            prefs().assignmentOffering().dueDate().getTime()
            + prefs().assignmentOffering().assignment()
                  .submissionProfile().deadTimeDelta() );
        CourseOffering course = prefs().assignmentOffering().courseOffering();
        User primeUser = wcSession().primeUser().localInstance(localContext());
        if ( deadline.before( submitTime )
             && !course.isInstructor( primeUser )
             && !course.isTA( primeUser ) )
        {
            error(
                "Unfortunately, the final deadline for this assignment "
                + "has passed.  No more submissions are being accepted." );
        }
        else
        {
            String msg = commitSubmission( context(), submitTime );
            if ( msg != null )
            {
                log.debug( "Submission error = " + msg );
                error( msg );
            }
        }

        if ( hasMessages() )
        {
            return null;
        }
        else
        {
            NSDictionary config =
                wcSession().tabs.selectedDescendant().config();
            if ( config != null
                 && config.objectForKey( "resetPrimeUser" ) != null )
            {
                setLocalUser( wcSession().primeUser() );
            }
            return super.next();
        }
    }


    // ----------------------------------------------------------
    public void cancelLocalChanges()
    {
        clearSubmission();
        NSDictionary config = wcSession().tabs.selectedDescendant().config();
        if ( config != null
             && config.objectForKey( "resetPrimeUser" ) != null )
        {
            setLocalUser( wcSession().primeUser() );
        }
        super.cancelLocalChanges();
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( ConfirmSubmissionPage.class );
}
