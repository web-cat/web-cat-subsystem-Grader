/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2008 Virginia Tech
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
import com.webobjects.eoaccess.*;
import com.webobjects.foundation.*;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This wizard page summarizes past submissions and allows a student
 * to upload a program file for the current (new) submission.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class UploadSubmissionPage
    extends GraderSubmissionUploadComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public UploadSubmissionPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup submissionDisplayGroup;
    /** submission item in the worepetition */
    public Submission aSubmission;
    /** index in worepetition */
    public int index;
    /** true if there are previous submissions */
    public boolean hasPreviousSubmissions;


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
        log.debug( "primeUser = " + wcSession().primeUser()
                   + ", localUser = " + user() );
        NSArray submissions = EOUtilities.objectsMatchingValues(
                localContext(),
                Submission.ENTITY_NAME,
                new NSDictionary(
                        new Object[] {  user(),
                                        prefs().assignmentOffering()
                                     },
                        new Object[] {  Submission.USER_KEY,
                                        Submission.ASSIGNMENT_OFFERING_KEY }
                )
            );
        submissionDisplayGroup.setObjectArray( submissions );
        int currentSubNo = submissions.count() + 1;
        for ( int i = 0; i < submissions.count(); i++ )
        {
            int sno = ( (Submission)submissions.objectAtIndex( i ) )
                .submitNumber();
            if ( sno >= currentSubNo )
            {
                currentSubNo = sno + 1;
            }
        }
        hasPreviousSubmissions = ( submissions.count() > 0 );

        Number maxSubmissions = prefs().assignmentOffering()
            .assignment().submissionProfile().maxSubmissionsRaw();
        okayToSubmit = ( maxSubmissions == null )
            || ( currentSubNo <= maxSubmissions.intValue() );

        if ( okayToSubmit )
        {
            startSubmission( currentSubNo, user() );
        }

        log.debug( "due = "
                   + prefs().assignmentOffering().dueDate().getTime() );
        log.debug( "grace = " +
                   prefs().assignmentOffering().assignment()
                       .submissionProfile().deadTimeDelta() );

        NSTimestamp deadline = new NSTimestamp(
                prefs().assignmentOffering().dueDate().getTime()
                + prefs().assignmentOffering().assignment()
                   .submissionProfile().deadTimeDelta() );
        log.debug( "time = " + deadline );

        super.appendToResponse( response, context );
        oldBatchSize  = submissionDisplayGroup.numberOfObjectsPerBatch();
        oldBatchIndex = submissionDisplayGroup.currentBatchIndex();
        cachedUploadedFile     = submissionInProcess().uploadedFile();
        cachedUploadedFileName = submissionInProcess().uploadedFileName();
        cachedUploadedFileList = submissionInProcess().uploadedFileList();
    }


    // ----------------------------------------------------------
    /**
     * A predicate that indicates whether the user can proceed.
     * As a side-effect, it sets the error message if the user cannot
     * proceed.
     *
     * @return true if the user can proceed
     */
    public boolean okayToSubmit()
    {
        if ( !okayToSubmit )
        {
            error( "You have already made the maximum allowed "
                          + "number of submissions for this assignment." );
        }
        return okayToSubmit;
    }


    // ----------------------------------------------------------
    public boolean nextEnabled()
    {
        return okayToSubmit();
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "next():" );
            log.debug(" request = " + context().request() );
            log.debug(" form values = " + context().request().formValues() );
            log.debug(" multipart = "
                + context().request().isMultipartFormData() );
        }
        if ( okayToSubmit )
        {
            NSTimestamp deadline = new NSTimestamp(
                    prefs().assignmentOffering().dueDate().getTime()
                    + prefs().assignmentOffering().assignment()
                      .submissionProfile().deadTimeDelta() );
            log.debug( "deadline = " + deadline );
            log.debug( "now = " + ( new NSTimestamp() ) );
            CourseOffering course = prefs().assignmentOffering().courseOffering();
            User primeUser =
                wcSession().primeUser().localInstance(localContext());
            if ( deadline.before( new NSTimestamp() )
                 && !course.isInstructor( primeUser )
                 && !course.isTA( primeUser ) )
            {
                error(
                    "Unfortunately, the final deadline for this assignment "
                    + "has passed.  No more submissions are being accepted." );
                return null;
            }
            boolean clearFileList = true;
            if ( !submissionInProcess().hasValidFileUpload() )
            {
                submissionInProcess().setUploadedFile( cachedUploadedFile );
                submissionInProcess().setUploadedFileName( cachedUploadedFileName );
                submissionInProcess().setUploadedFileList( cachedUploadedFileList );
                clearFileList = false;
            }
            if ( !submissionInProcess().hasValidFileUpload() )
            {
                error( "Please select a file to upload." );
                return null;
            }
            if ( clearFileList )
            {
                submissionInProcess().setUploadedFileList( null );
            }
            if ( submissionInProcess().uploadedFile().length() >
                 prefs().assignmentOffering().assignment()
                     .submissionProfile().effectiveMaxFileUploadSize() )
            {
                error(
                    "You file exceeds the file size limit for this assignment ("
                    + prefs().assignmentOffering().assignment()
                          .submissionProfile().effectiveMaxFileUploadSize()
                    + ").  Please choose a smaller file." );
                return null;
            }
/*
        if ( gstate.selectedAssignmentOffering().assignment()
                 .testDrivenAssignment().intValue() == 0 )
        {
            state.navigator().setCurrentPage(
                    state.navigator().currentPage() + 1 );
        }
*/
            return super.next();
        }
        else
        {
            // If we get here, an error message has already been set
            // in okayToSubmit(), so just refresh the page to show it.
            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * A boolean predicate that indicates that this assignment
     * has instructor-provided instructions to show to the student.
     *
     * @return true if there are instructions to show
     */
    public boolean hasInstructions()
    {
        String instructions = prefs().assignmentOffering()
            .assignment().fileUploadMessage();
        return instructions != null && !instructions.equals( "" );
    }


    // ----------------------------------------------------------
    /**
     * Returns a string version of the file size for the currently
     * uploaded file.
     *
     * @return the file size as a string
     */
    public String uploadedFileSize()
    {
        long size = 0L;
        NSData file = submissionInProcess().uploadedFile();
        if ( file != null )
        {
            size = file.length();
        }
        return Submission.fileSizeAsString( size );
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        log.debug( "defaultAction()" );
        if ( oldBatchSize != submissionDisplayGroup.numberOfObjectsPerBatch()
             || oldBatchIndex != submissionDisplayGroup.currentBatchIndex() )
        {
            return null;
        }
        else
        {
            return super.defaultAction();
        }
    }


    // ----------------------------------------------------------
    public void cancelLocalChanges()
    {
        clearSubmission();
        resetPrimeUser();
        super.cancelLocalChanges();
    }


    // ----------------------------------------------------------
    public void takeValuesFromRequest( WORequest arg0, WOContext arg1 )
    {
        try
        {
            super.takeValuesFromRequest( arg0, arg1 );
        }
        catch ( Exception e )
        {
            // Ignore it
//            error( e.getMessage() );
//            Application.emailExceptionToAdmins( e, arg1,
//                "In UploadSubmissionPage:takeValuesFromRequest(), a post "
//                + "request without an attached file submission was received\n."
//                + "Browser info = \n"
//                + context().request().headerForKey("user-agent")
//                );
        }
    }


    //~ Instance/static variables .............................................

    /** Saves the state of the batch navigator to detect setting changes */
    protected int oldBatchSize;
    /** Saves the state of the batch navigator to detect setting changes */
    protected int oldBatchIndex;

    protected NSData  cachedUploadedFile;
    protected String  cachedUploadedFileName;
    protected NSArray cachedUploadedFileList;

    /** True if the user has not reached the maximum allowable submissions
     *  for this assignment and its okay to submit another time.
     */
    protected boolean okayToSubmit;

    static Logger log = Logger.getLogger( UploadSubmissionPage.class );
}
