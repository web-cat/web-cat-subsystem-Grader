/*==========================================================================*\
 |  $Id: GraderSubmissionUploadComponent.java,v 1.9 2014/11/07 13:55:03 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2012 Virginia Tech
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
import com.webobjects.foundation.*;
import java.io.*;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.core.messaging.UnexpectedExceptionMessage;

//-------------------------------------------------------------------------
/**
 *  A {@link GraderAssignmentComponent} that adds support for a
 *  {@link SubmissionInProcess} state object.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.9 $, $Date: 2014/11/07 13:55:03 $
 */
public class GraderSubmissionUploadComponent
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new object.
     *
     * @param context The context to use
     */
    public GraderSubmissionUploadComponent( WOContext context )
    {
        super( context );
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Access the user's current submission in progress.
     * @return the submission in progress for this page
     */
    public SubmissionInProcess submissionInProcess()
    {
        if (sip == null)
        {
            sip = (SubmissionInProcess)transientState().valueForKey(KEY);
            if (sip == null)
            {
                sip = new SubmissionInProcess();
                transientState().takeValueForKey(sip, KEY);
            }
        }
        return sip;
    }


    // ----------------------------------------------------------
    /**
     * Creates a fresh (but not saved or committed) submission
     * object and binds it to the submission key.
     * @param submitNumber the number of the new submission
     * @param user the person making the submission
     * @param partners an array of partners to be associated with the
     *     submission
     */
    public void startSubmission(
        int submitNumber, User user, AssignmentOffering assignmentOffering)
    {
        log.debug("startSubmission( " + submitNumber + ", " + user + " )");
        submissionInProcess().startSubmission(
            user, assignmentOffering, submitNumber);
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
    public String commitSubmission(WOContext context, NSTimestamp submitTime)
    {
        String errorMessage = null;
        log.debug("committing submission");

        EnergyBar bar = null;
        if (wcSession().primeUser() == null)
        {
            log.error(new Exception(
                "null primeUser in session for submission by " +
                submissionInProcess().user()));
        }
        if (submissionInProcess().assignmentOffering() != null
            && (wcSession().primeUser() == null
                || submissionInProcess().user().id()
                    == wcSession().primeUser().id()))
        {
            bar = submissionInProcess().assignmentOffering().energyBarForUser(
                submissionInProcess().user());
            if (bar != null
                && !bar.hasEnergy()
                && !bar.isCloseToDeadline(submitTime))
            {
                bar.logEvent(EnergyBar.SUBMISSION_DENIED,
                    submissionInProcess().assignmentOffering());
                return "You are out of submission energy, but submission "
                    + "energy is required to submit to this assignment. Wait "
                    + "for your submission energy to recharge.";
            }
        }
        String uploadedFileName = submissionInProcess().uploadedFileName();
        if (uploadedFileName == null)
        {
            return "No file name provided for uploaded file!";
        }

        Submission submission = Submission.create(localContext(), false);
        submission.setSubmitNumber(submissionInProcess().submitNumber());
        submission.setUserRelationship(submissionInProcess().user());
        submission.setSubmitTime(submitTime);
        submission.setFileName(uploadedFileName);
        // wcSession().localContext().insertObject(submission);
        //      ec.saveChanges();
        submission.setAssignmentOfferingRelationship(
            submissionInProcess().assignmentOffering());
        submissionInProcess().assignmentOffering()
            .addToSubmissionsRelationship(submission);
        log.debug("Uploaded file name: " + uploadedFileName);

        // Do the actual partnering of the users (this will create the dummy
        // Submission objects for the other partners).

        if (submissionInProcess().partners() != null)
        {
            submission.partnerWith(submissionInProcess().partners());
        }

        // Then, make the necessary directory.

        try
        {
            File dirFile = new File(submission.dirName());
            dirFile.mkdirs();
        }
        catch (Exception e)
        {
            // Security exception
            new UnexpectedExceptionMessage(e, context, null,
                    "Exception creating submission directory").send();
            localContext().deleteObject(submission);
            prefs().setSubmissionRelationship(null);
            submissionInProcess().cancelSubmission();
            applyLocalChanges();
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
            log.debug("Local file name: " + outFile.getPath());
            FileOutputStream out = new FileOutputStream(outFile);
            submissionInProcess().uploadedFile().writeToStream(out);
            out.close();
        }
        catch (Exception e)
        {
            // Do something with the exception
            new UnexpectedExceptionMessage(e, context, null,
                    "Exception uploading submission file").send();
            localContext().deleteObject(submission);
            prefs().setSubmissionRelationship(null);
            submissionInProcess().cancelSubmission();
            applyLocalChanges();
            return "A file error occurred while saving your "
                + "submission.  The error has been reported "
                + "to the administrator.  Please try your "
                + "submission again later once the problem "
                + "has been corrected.";
        }

        // Clear out older jobs
        try
        {
            NSArray<EnqueuedJob> oldJobs =
                EnqueuedJob.objectsMatchingQualifier(localContext(),
                    EnqueuedJob.submission.dot(Submission.user).eq(
                        submission.user())
                    .and(
                    EnqueuedJob.submission.dot(Submission.assignmentOffering)
                        .eq(submission.assignmentOffering()))
                    .and(EnqueuedJob.regrading.isFalse()));
            for (EnqueuedJob job : oldJobs)
            {
                job.setDiscarded(true);
            }
        }
        catch ( Exception e )
        {
            // ignore it
        }

        // commit changes before creating enqueued job, since we don't
        // want job created before all submission data has been stored
        // to db, and we can't control the order in which the changes
        // will be applied (possible concurrency race condition)
        applyLocalChanges();

        // Queue it up for the grader
        EnqueuedJob job = new EnqueuedJob();
//      job.setSubmission(submission);
        localContext().insertObject(job);
        job.setSubmissionRelationship(submission);
        job.setQueueTime(new NSTimestamp());
        if (bar != null)
        {
            if (bar.consumeEnergyIfPossible(submitTime))
            {
                bar.logEvent(EnergyBar.CHARGE_CONSUMED, submission);
            }
            else if (bar.isCloseToDeadline(submitTime))
            {
              bar.logEvent(EnergyBar.SUBMISSION_CLOSE_TO_DEADLINE, submission);
            }
            else
            {
                bar.logEvent(EnergyBar.SUBMISSION_DENY_FAILED, submission);
                log.error("submission " + submission +
                    "committed without energy! bar = " + bar);
            }
        }
        applyLocalChanges();
        prefs().setSubmissionRelationship(submission);
        {
            try
            {
                log.info("enqueued job " + job + ", queue size now = "
                    + EnqueuedJob.countOfObjectsMatchingQualifier(
                        localContext(),
                        EnqueuedJob.paused.isFalse()
                            .and(EnqueuedJob.regrading.isFalse())
                            .and(EnqueuedJob.submission.isNotNull())));
            }
            catch (Exception e)
            {
                log.error("Unexpected exception", e);
            }
        }

        GraderQueueProcessor.processJob(job);

        // reset the submission in process so it is clear again
        submissionInProcess().cancelSubmission();

        return errorMessage;
    }


    // ----------------------------------------------------------
    /**
     * Erases the submission in progress and nulls out the corresponding
     * data members.
     */
    public void clearSubmission()
    {
        submissionInProcess().cancelSubmission();
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

    private SubmissionInProcess sip;
    private static final String KEY = SubmissionInProcess.class.getName();

    static Logger log = Logger.getLogger(GraderSubmissionUploadComponent.class);
}
