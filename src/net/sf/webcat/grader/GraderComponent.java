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

import er.extensions.*;

import java.io.File;
import java.io.FileOutputStream;

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 *  A specialized version of {@link WCCourseComponent} that adds some extras
 *  for use by components in the Grader subsystem.
 *
 *  @author  Stephen Edwards
 *  @version $Id$
 */
public class GraderComponent
    extends WCCourseComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderComponent object.
     *
     * @param context The context to use
     */
    public GraderComponent( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public static final String GRADER_PREFS_KEY = "graderPrefs";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Returns the currently selected assignment offering.
     * @return The assignment offering
     */
    public GraderPrefs prefs()
    {
        if ( prefs == null )
        {
            prefs = (GraderPrefs)wcSession().subsystemData.valueForKey(
                GRADER_PREFS_KEY );
        }
        if ( prefs == null )
        {
            User prime = wcSession().primeUser();
            if ( prime.editingContext() != wcSession().localContext() )
            {
                prime = (User)EOUtilities.localInstanceOfObject(
                    wcSession().localContext(), prime );
            }
            NSArray results = null;
            try
            {
                results = GraderPrefs.objectsForUser(
                    wcSession().localContext(), prime );
            }
            catch ( java.lang.IllegalStateException e )
            {
                // Just try again, in case this is a failure due to the
                // use of shared contexts under Win2K
                results = GraderPrefs.objectsForUser(
                    wcSession().localContext(), prime );
            }
            if ( results.count() > 0 )
            {
                prefs = (GraderPrefs)results.objectAtIndex( 0 );
            }
            else
            {
                GraderPrefs newPrefs = new GraderPrefs();
                EOEditingContext ec = Application.newPeerEditingContext();
                try
                {
                    ec.lock();
                    ec.insertObject( newPrefs );
                    User localPrime = (User)EOUtilities.localInstanceOfObject(
                                ec, prime );
                    newPrefs.setUserRelationship( localPrime );
                    ec.saveChanges();
                    prefs = (GraderPrefs)EOUtilities.localInstanceOfObject(
                        wcSession().localContext(), newPrefs );
                }
                catch ( Exception e)
                {
                    Application.emailExceptionToAdmins(
                        e, context(), "failure initializing prefs!" );
                }
                finally
                {
                    ec.unlock();
                    Application.releasePeerEditingContext( ec );
                }
            }
            if ( prefs != null )
            {
                wcSession().subsystemData.takeValueForKey(
                    prefs, GRADER_PREFS_KEY );
            }
        }
        if ( prefs == null )
        {
            log.error( "null prefs!", new Exception( "here" ) );
            log.error( Application.extraInfoForContext( context() ) );
        }
        return prefs;
    }


    // ----------------------------------------------------------
    /**
     * Determine whether "Finish" can be pressed for this task.
     * @return true if a submission is stored
     */
    public boolean canFinish()
    {
        return prefs().submission() != null;
    }


    // ----------------------------------------------------------
    /**
     * Creates a fresh (but not saved or committed) submission
     * object and binds it to the submission key.
     * @param submitNumber the number of the new submission
     * @param user the person making the submission
     */
    public void startSubmission( int submitNumber, User user )
    {
        Submission submission = new Submission();
        wcSession().localContext().insertObject( submission );
        submission.setSubmitNumber( submitNumber );
        submission.setUserRelationship( user );
        log.debug( "startSubmission( " + submitNumber + ", " + user + " )" );
        prefs().setSubmission( submission );
        prefs().setSubmissionInProcess( true );
    }


    // ----------------------------------------------------------
    /**
     * Enters the current submission into the editing context,
     * establishes all the necessary relationships, and saves the
     * uploaded file to disk.
     *
     * @param  context    the context of the request
     * @param  submitTime the time to record for this submission
     * @return a string error message, or null if there were no errors
     */
    public String commitSubmission( WOContext context,
                                    NSTimestamp submitTime )
    {
        String errorMessage = null;
        log.debug( "committing submission" );
        Submission submission = prefs().submission();
        String uploadedFileName = prefs().uploadedFileName();
        submission.setSubmitTime( submitTime );
        submission.setFileName( uploadedFileName );
        // wcSession().localContext().insertObject( submission );
        //      ec.saveChanges();
        submission.setAssignmentOfferingRelationship(
            prefs().assignmentOffering() );
        prefs().assignmentOffering().addToSubmissionsRelationship( submission );
        log.debug( "Uploaded file name: " + uploadedFileName );

        // First, make the necessary directory
        try
        {
            File dirFile = new File( submission.dirName() );
            dirFile.mkdirs();
        }
        catch ( Exception e )
        {
            // Security exception
            Application.emailExceptionToAdmins(
                    e,
                    context,
                    "Exception creating submission directory"
                );
            wcSession().localContext().deleteObject( submission );
            prefs().setSubmissionRelationship( null );
            prefs().setSubmissionInProcess( false );
            wcSession().commitLocalChanges();
            return "A file error occurred while saving your "
                   + "submission.  The error has been reported "
                   + "to the administrator.  Please try your "
                   + "submission again later once the problem "
                   + "has been corrected.";
        }

        // Next, write out the file
        try
        {
            File outFile = submission.file();
            log.debug( "Local file name: " + outFile.getPath() );
            FileOutputStream out = new FileOutputStream( outFile );
            prefs().uploadedFile().writeToStream( out );
            out.close();
        }
        catch ( Exception e )
        {
            // Do something with the exception
            Application.emailExceptionToAdmins(
                    e,
                    context,
                    "Exception uploading submission file"
                );
            wcSession().localContext().deleteObject( submission );
            prefs().setSubmissionRelationship( null );
            prefs().setSubmissionInProcess( false );
            wcSession().commitLocalChanges();
            return "A file error occurred while saving your "
                   + "submission.  The error has been reported "
                   + "to the administrator.  Please try your "
                   + "submission again later once the problem "
                   + "has been corrected.";
        }

        // Clear out older jobs
        try
        {
            NSArray oldJobs = EOUtilities.objectsMatchingValues(
                    wcSession().localContext(),
                    EnqueuedJob.ENTITY_NAME,
                    new NSDictionary(
                        new Object[] {  wcSession().user(),
                                        submission.assignmentOffering()    },
                        new Object[] { EnqueuedJob.USER_KEY,
                                       EnqueuedJob.ASSIGNMENT_OFFERING_KEY }
                ) );
            for ( int i = 0; i < oldJobs.count(); i++ )
            {
                EnqueuedJob job = (EnqueuedJob)oldJobs.objectAtIndex( i );
                job.setDiscarded( true );
            }
        }
        catch ( Exception e )
        {
            // ignore it
        }

        // Queue it up for the grader
        EnqueuedJob job = new EnqueuedJob();
//      job.setSubmission( submission );
        wcSession().localContext().insertObject( job );
        job.setSubmissionRelationship( submission );
        job.setQueueTime( new NSTimestamp() );
        wcSession().commitLocalChanges();

        Grader.getInstance().graderQueue().enqueue( null );

        prefs().clearUpload();
        prefs().setSubmissionInProcess( false );

        return errorMessage;
    }


    // ----------------------------------------------------------
    /**
     * Erases the submission in progress and nulls out the corresponding
     * data members.
     */
    public void clearSubmission()
    {
        if ( prefs().submissionInProcess() )
        {
            Submission submission = prefs().submission();
            if ( submission != null && submission.result() == null )
            {
                wcSession().localContext().deleteObject( submission );
                prefs().setSubmissionRelationship( null );
            }
            prefs().setSubmissionInProcess( false );
        }
    }


    // ----------------------------------------------------------
    /**
     * Cancels any editing in progress.  Typically called when pressing
     * a cancel button or using a tab to transfer to a different page.
     */
    public void cancelLocalChanges()
    {
        clearSubmission();
        super.cancelLocalChanges();
    }


    //~ Instance/static variables .............................................

    private GraderPrefs prefs;
    static Logger log = Logger.getLogger( GraderComponent.class );
}
