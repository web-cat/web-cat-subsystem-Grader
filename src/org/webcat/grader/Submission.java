/*==========================================================================*\
 |  Copyright (C) 2006-2018 Virginia Tech
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

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.eof.ERXConstant;
import er.extensions.eof.ERXEOAccessUtilities;
import er.extensions.eof.ERXEOGlobalIDUtilities;
import er.extensions.eof.ERXQ;
import er.extensions.foundation.ERXFileUtilities;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.core.messaging.UnexpectedExceptionMessage;
import org.webcat.grader.lti.LISResultId;
import org.webcat.grader.messaging.GradingResultsAvailableMessage;
import org.webcat.woextensions.ECAction;
import org.webcat.woextensions.WCFetchSpecification;
import org.webcat.woextensions.MigratingEditingContext;

// -------------------------------------------------------------------------
/**
 *  Represents a single student assignment submission.
 *
 *  @author  Stephen Edwards
 */
public class Submission
    extends _Submission
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new Submission object.
     */
    public Submission()
    {
        super();
    }


    //~ Constants (for key names) .............................................

    public static final String ID_FORM_KEY = "sid";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Get a short (no longer than 60 characters) description of this
     * submission, which currently returns {@link #fileName()} or
     * {@link #dirName()}.
     * @return the description
     */
    @Override
    public String userPresentableDescription()
    {
        if (fileName() != null)
        {
            return file().getPath();
        }
        else
        {
            return dirName();
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where this submission is stored.
     * @return the directory name
     */
    public String dirName()
    {
        if (partnerLink() && result() != null)
        {
            return result().submission().dirName();
        }
        else
        {
            StringBuffer dir =
                (user() == null)
                ? new StringBuffer("<null>")
                : user().authenticationDomain().submissionBaseDirBuffer();
            if (assignmentOffering() != null)
            {
                assignmentOffering().addSubdirTo(dir);
            }
            else
            {
                dir.append("/ASSIGNMENT");
            }
            dir.append('/');
            dir.append( (user() == null)
                ? new StringBuffer("<null>")
                : user().userName());
            dir.append('/');
            dir.append(submitNumber());
            return dir.toString();
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the submission file as a File object.
     * @return the file for this submission
     */
    public File file()
    {
        return new File(dirName(), fileName());
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>id</code> value.
     * @return the value of the attribute
     */
    public Number id()
    {
        try
        {
            return (Number)EOUtilities.primaryKeyForObject(
                editingContext() , this).objectForKey("id");
        }
        catch (Exception e)
        {
            String subInfo = null;
            try
            {
                subInfo = toString();
            }
            catch (Exception ee)
            {
                subInfo = ee.toString();
            }

            new UnexpectedExceptionMessage(e, null, null,
                    "An exception was generated trying to retrieve the "
                    + "id for a submission.\n\nSubmission = " + subInfo)
                .send();

            return ERXConstant.ZeroInteger;
        }
    }


    // ----------------------------------------------------------
    /**
     * Determine whether this submission's results are ready for
     * viewing or not.
     * @return True if this submission has a valid/finished result object.
     */
    public boolean resultIsReady()
    {
        return result() != null && result().isReady();
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the submission file as a File object.
     * @return the file for this submission
     */
    public String resultDirName()
    {
        return dirName() +  "/Results";
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the submission file as a File object.
     * @return the file for this submission
     */
    public File resultDir()
    {
        return new File(resultDirName());
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the path to the public resources directory in the result
     * directory. Public resources are those that are generated by plug-ins
     * during grading and are viewable by the user who submitted, any partners,
     * course staff, and administrators.
     *
     * @return the path to the public resources directory
     */
    public File publicResourcesDir()
    {
        return new File(resultDir(), "public");
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the path to the public resource with the specified relative
     * path in the public resources directory. If the path is invalid (for
     * example, it tries to navigate to a parent directory), then null is
     * returned.
     *
     * @param path the relative path to the resource
     * @return the File object that represents the file, or null if the path
     *     was invalid
     */
    public File fileForPublicResourceAtPath(String path)
    {
        File file = new File(publicResourcesDir(), path);

        try
        {
            if (file.getCanonicalPath().startsWith(
                    publicResourcesDir().getCanonicalPath()))
            {
                return file;
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred while retrieving the canonical path "
                    + "of the file " + file.toString());
        }

        return null;
    }


    // ----------------------------------------------------------
    /**
     * Converts a time to its human-readable format.  Most useful
     * when the time is "small," like a difference between two
     * other time stamps.
     *
     * @param time The time to convert
     * @return     A human-readable version of the time
     */
    public static String getStringTimeRepresentation(long time)
    {
        long days;
        long hours;
        long minutes;
        // long seconds;
        // long milliseconds;
        StringBuffer buffer = new StringBuffer();

        days = time / 86400000;
        time %= 86400000;
        hours = time / 3600000;
        time %= 3600000;
        minutes = time / 60000;
        time %= 60000;
        // seconds      = time / 1000;
        // milliseconds = time % 1000;

        if (days > 0)
        {
            buffer.append(days);
            buffer.append(" day");
            if (days > 1)
            {
                buffer.append('s');
            }
        }

        if (hours > 0)
        {
            if (buffer.length() > 0)
            {
                buffer.append(", ");
            }

            buffer.append(hours);
            buffer.append(" hr");
            if (hours > 1)
            {
                buffer.append('s');
            }
        }

        if (minutes > 0)
        {
            if (buffer.length() > 0)
            {
                buffer.append( ", " );
            }

            buffer.append(minutes);
            buffer.append(" min");

            if (minutes > 1)
            {
                buffer.append('s');
            }
        }

        if (days == 0 && hours == 0 && minutes == 0)
        {
            buffer.append("less than 1 min");
        }

        return buffer.toString();
    }


    // ----------------------------------------------------------
    /**
     * Checks whether the uploaded file is an archive.
     * @return true if the uploaded file is a zip or jar file
     */
//    public boolean fileIsArchive()
//    {
//        String fileName = fileName().toLowerCase();
//        return (    fileName != null
//                 && (    fileName.endsWith( ".zip" )
//                      || fileName.endsWith( ".jar" ) ) );
//    }


    // ----------------------------------------------------------
    public EnqueuedJob enqueuedJob()
    {
        EnqueuedJob job = null;
        NSArray<EnqueuedJob> jobs = enqueuedJobs();
        if (jobs != null  &&  jobs.count() > 0)
        {
            if (jobs.count() > 1)
            {
                log.error("too many jobs for submission " + this);
            }
            job = jobs.objectAtIndex(0);
            if (job.isFault())
            {
                try
                {
                    // Force fault to be resolved
                    job.paused();
                }
                catch (EOObjectNotAvailableException e)
                {
                    // fault for object that no longer exists!
                    job = null;
                }
            }
        }
        return job;
    }


    // ----------------------------------------------------------
    @Override
    public void setPrimarySubmission(Submission value)
    {
        super.setPrimarySubmission(value);
        setPartnerLink(value != null);
    }


    // ----------------------------------------------------------
    public void partnerWith(NSArray<User> users)
    {
        // Collect all of the students enrolled in any offering of the course
        // to which the submission is being made.

        NSMutableSet<User> studentsEnrolled = new NSMutableSet<User>();

        EOEditingContext ec = editingContext();
        if (ec == null && assignmentOffering() != null)
        {
            ec = assignmentOffering().editingContext();
        }

        // Find all offerings of the current course in this semester
        NSArray<CourseOffering> offerings =
            CourseOffering.offeringsForSemesterAndCourse(ec,
                assignmentOffering().courseOffering().course(),
                assignmentOffering().courseOffering().semester());
        for (CourseOffering offering : offerings)
        {
            studentsEnrolled.addObjectsFromArray(offering.students());
        }

        for (User partner : users)
        {
            // Only partner a user on a submission if they are enrolled in the
            // same course as the user making the primary submission (but not
            // necessarily in the same offering -- this is a little more
            // flexible).

            if (studentsEnrolled.containsObject(partner))
            {
                partnerWith(partner);
            }
        }
    }


    // ----------------------------------------------------------
    public void partnerWith(User partner)
    {
        // Make sure that a user isn't trying to partner with himself.

        if (partner.equals(user()))
        {
            return;
        }
        if (primarySubmission() != null)
        {
            primarySubmission().partnerWith(partner);
            return;
        }

        EOEditingContext ec = editingContext();
        if (ec == null)
        {
            ec = partner.editingContext();
        }

        int partnerSubmitNumber = 1;

        // Find partner's home courseOffering and its assignment offering
        AssignmentOffering partnerOffering = assignmentOffering();
        NSArray<AssignmentOffering> partnerOfferings = AssignmentOffering
            .objectsMatchingQualifier(ec,
                AssignmentOffering.courseOffering.dot(CourseOffering.course)
                    .eq(assignmentOffering().courseOffering().course())
                .and(AssignmentOffering.courseOffering
                    .dot(CourseOffering.students).eq(partner))
                .and(AssignmentOffering.assignment
                    .eq(assignmentOffering().assignment())));
        if (partnerOfferings.count() > 0)
        {
            partnerOffering = partnerOfferings.get(0);
        }

        EOQualifier qualifier =
            Submission.assignmentOffering.eq(partnerOffering)
                .and(Submission.user.eq(partner));

        Submission highestSubmission = Submission.firstObjectMatchingQualifier(
                ec,
                qualifier,
                Submission.submitNumber.descs());

        if (highestSubmission != null)
        {
            partnerSubmitNumber = highestSubmission.submitNumber() + 1;
        }

        Submission newSubmission = new Submission();
        ec.insertObject( newSubmission );

        newSubmission.setFileName(fileName());
        newSubmission.setPartnerLink(true);
        newSubmission.setSubmitNumber(partnerSubmitNumber);
        newSubmission.setSubmitTime(submitTime());
        newSubmission.setAssignmentOfferingRelationship(partnerOffering);
        newSubmission.setResultRelationship(result());
        newSubmission.setUserRelationship(partner);
        newSubmission.setPrimarySubmissionRelationship(this);
        newSubmission.setIsSubmissionForGrading(isSubmissionForGrading());

        addToPartneredSubmissionsRelationship(newSubmission);
    }


    // ----------------------------------------------------------
    public void unpartnerFrom(NSArray<User> users)
    {
        for (User partner : users)
        {
            unpartnerFrom(partner);
        }
    }


    // ----------------------------------------------------------
    public void unpartnerFrom(User partner)
    {
        EOQualifier qualifier =
            Submission.result.is(result()).and(Submission.user.eq(partner));

        EOEditingContext ec = editingContext();
        if (ec == null)
        {
            ec = partner.editingContext();
        }

        Submission partneredSubmission =
            Submission.firstObjectMatchingQualifier(ec, qualifier, null);

        if (partneredSubmission != null)
        {
            partneredSubmission.setResultRelationship(null);
            ec.deleteObject(partneredSubmission);
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets an array containing all the users associated with this submission;
     * that is, the user who submitted it as well as any partners.
     *
     * @return the array of all users associated with the submission
     */
    public NSArray<User> allUsers()
    {
        NSMutableArray<User> users = (NSMutableArray<User>)allPartners();
        users.addObject(user());
        return users;
    }


    // ----------------------------------------------------------
    /**
     * Gets an array containing all the partners associated with this
     * submission, excluding the user who submitted it.
     *
     * @return the array of all partners associated with the submission
     */
    public NSArray<User> allPartners()
    {
        NSMutableArray<User> users = new NSMutableArray<User>();

        for (Submission partnerSub : partneredSubmissions())
        {
            users.addObject(partnerSub.user());
        }

        return users;
    }


    // ----------------------------------------------------------
    /**
     * Gets a string containing the names of the user who made this submission
     * and all of his or her partners, in the form "User 1, User 2, ..., and
     * User N".
     *
     * @return a string containing the names of all of the partners
     */
    public String namesOfAllUsers()
    {
        NSMutableArray<String> names = new NSMutableArray<String>();
        names.addObject(user().nameAndUid());

        for (Submission partnerSub : partneredSubmissions())
        {
            names.addObject(partnerSub.user().nameAndUid());
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append(names.objectAtIndex(0));

        if (names.count() > 1)
        {
            for (int i = 1; i < names.count() - 1; i++)
            {
                buffer.append(", ");
                buffer.append(names.objectAtIndex(i));
            }

            if (names.count() > 2)
            {
                buffer.append(',');
            }

            buffer.append(" and ");
            buffer.append(names.lastObject());
        }

        return buffer.toString();
    }


    // ----------------------------------------------------------
    /**
     * Gets a string containing the names of the user who made this submission
     * and all of his or her partners, in the form "User 1, User 2, ..., and
     * User N".
     *
     * @return a string containing the names of all of the partners
     */
    public String namesOfAllUsers_LF()
    {
        NSMutableArray<String> names = new NSMutableArray<String>();
        names.addObject(user().nameAndUid_LF());

        for (Submission partnerSub : partneredSubmissions())
        {
            names.addObject(partnerSub.user().nameAndUid_LF());
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append(names.objectAtIndex(0));

        if (names.count() > 1)
        {
            for (int i = 1; i < names.count() - 1; i++)
            {
                buffer.append("; ");
                buffer.append(names.objectAtIndex(i));
            }

            if (names.count() > 2)
            {
                buffer.append(';');
            }

            buffer.append(" and ");
            buffer.append(names.lastObject());
        }

        return buffer.toString();
    }


    // ----------------------------------------------------------
    /**
     * Delete all the result information for this submission, including
     * all partner links.  This method uses the submission's current
     * editing context to make changes, but does <b>not</b> commit those
     * changes to the database (the caller must use
     * <code>saveChanges()</code>).
     */
//    private void deleteResultsForAllPartners()
//    {
//        SubmissionResult myResult = result();
//        if (myResult != null)
//        {
//            log.debug("removing SubmissionResult " + myResult);
//            myResult.setIsMostRecent(false);
//            setResultRelationship(null);
//            clearIsSubmissionForGrading();
//
//            // Have to copy out the list of submissions, since inside
//            // the loop, we'll be removing them one by one from the
//            // relationship.
//            @SuppressWarnings("unchecked")
//            NSArray<Submission> partnerSubs =
//                (NSArray<Submission>)myResult.submissions().clone();
//            for (Submission s : partnerSubs)
//            {
//                s.setResultRelationship(null);
//                s.clearIsSubmissionForGrading();
//            }
//            editingContext().deleteObject(myResult);
//        }
//    }


    // ----------------------------------------------------------
    /**
     * Delete all the result information for this submission, including
     * all partner links, and requeue it for grading.  This method uses the
     * submission's current editing context to make changes, but does
     * <b>not</b> commit those changes to the database (the caller must
     * use <code>saveChanges()</code>).
     * @param ec the editing context in which to make the changes (can be
     * different than the editing context that owns this object)
     */
    public void requeueForGrading(EOEditingContext ec)
    {
        if (enqueuedJob() == null)
        {
            Submission me = this;
            if (ec != editingContext())
            {
                me = localInstance(ec);
            }
//            me.deleteResultsForAllPartners();
            log.debug("creating new job for Submission " + this);
            EnqueuedJob job = new EnqueuedJob();
            job.setQueueTime(new NSTimestamp());
            job.setRegrading(true);
            ec.insertObject(job);
            job.setSubmissionRelationship(me);
        }
    }


    // ----------------------------------------------------------
    public String permalink()
    {
        if (cachedPermalink == null)
        {
            cachedPermalink = Application.configurationProperties()
                .getProperty("base.url")
                + "?page=MostRecent&"
                + ID_FORM_KEY + "=" + id();
        }
        return cachedPermalink;
    }


    // ----------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accessibleByUser(User aUser)
    {
        return aUser == user()
            || (assignmentOffering() != null
                && assignmentOffering().accessibleByUser(aUser));
    }


    // ----------------------------------------------------------
    public void emailNotificationToStudent( String message )
    {
        WCProperties properties =
            new WCProperties( Application.configurationProperties() );
        user().addPropertiesTo( properties );
        if ( properties.getProperty( "login.url" ) == null )
        {
            String dest = Application.application().servletConnectURL();
            properties.setProperty( "login.url", dest );
        }
        properties.setProperty( "submission.number",
            Integer.toString( submitNumber() ) );
        properties.setProperty( "message", message );
        AssignmentOffering assignment = assignmentOffering();
        if ( assignment != null )
        {
            properties.setProperty( "assignment.title",
                assignment.titleString() );
        }
        properties.setProperty(  "submission.result.link", permalink() );

        try
        {
            new GradingResultsAvailableMessage(user(), properties)
                .send();
        }
        catch (Exception e)
        {
            log.error("Unable to notify student of grading results", e);
        }
    }


    // ----------------------------------------------------------
    /**
     * Check to see whether this submission is a better choice as the
     * submission for grading currently set for this student/assignment
     * offering combination, and if so, set it as the submission for grading.
     */
    public void setIsSubmissionForGradingIfNecessary()
    {
        if (isSubmissionForGrading())
        {
            return;
        }
        Submission existingSubmissionForGrading = gradedSubmission();
        if (existingSubmissionForGrading == null)
        {
            setIsSubmissionForGrading(true);
        }
        else
        {
            if (isBetterGradingChoiceThan(existingSubmissionForGrading))
            {
                setIsSubmissionForGrading(true);
                existingSubmissionForGrading.setIsSubmissionForGrading(false);
            }
            else
            {
                setIsSubmissionForGrading(false);
                existingSubmissionForGrading.setIsSubmissionForGrading(true);
            }
        }
    }


    // ----------------------------------------------------------
//    private void clearIsSubmissionForGrading()
//    {
//        if (isSubmissionForGrading())
//        {
//            setIsSubmissionForGrading(false);
////            System.out.println("removing submissionForGrading from " + this);
//            Submission existingSubmissionForGrading = gradedSubmission();
//            if (existingSubmissionForGrading != null)
//            {
//                existingSubmissionForGrading.setIsSubmissionForGrading(true);
////                System.out.println("setting submissionForGrading on "
////                    + existingSubmissionForGrading);
//            }
//        }
//    }


    // ----------------------------------------------------------
    /**
     * Determine whether this submission, or another it is compared against,
     * is the preferable one to use as the submission for grading.  A
     * submission is preferable for grading if it has any newer feedback,
     * or if neither submission has any feedback, if it was made more
     * recently.
     * @param other The other submission to compare against.
     * @return True if this submission should be used as the submission
     * for grading instead of the other one.
     */
    public boolean isBetterGradingChoiceThan(Submission other)
    {
        if (other == null)
        {
            return true;
        }
        if (result() == null)
        {
            if (other.result() == null)
            {
                // Neither has a result!
                return submitTime().after(other.submitTime());
            }
            // Otherwise, go with the other one
            return false;
        }
        else if (other.result() == null)
        {
            return true;
        }
        else if (result().lastUpdated() == null)
        {
            // We have no feedback, so check other
            if (other.result().lastUpdated() != null)
            {
                // Other has feedback, so go with it
                return false;
            }
            else if (result().status() != Status.TO_DO)
            {
                if (other.result().status() != Status.TO_DO)
                {
                    // Both have status change but no feedback timestamp,
                    // so go with newest submission
                    return submitTime().after(other.submitTime());
                }
                else
                {
                    // We have a status change but other does not
                    return true;
                }
            }
            else
            {
                // Neither has feedback, so go with newest submission
                return submitTime().after(other.submitTime());
            }
        }
        else if (other.result().lastUpdated() == null)
        {
            // We have feedback, but other doesn't
            return true;
        }
        else
        {
            // Both have feedback, so go with most recent feedback
            return result().lastUpdated().after(other.result().lastUpdated());
        }
    }


    // ----------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldMigrateIsSubmissionForGrading()
    {
        return (isSubmissionForGradingRaw() == null);
    }



    // ----------------------------------------------------------
    private void refreshIsSubmissionForGrading(NSArray<Submission> submissions)
    {
        Submission gradedSubmission = null;

        // Iterate over the whole submission set and find the
        // submission for grading (which is either the last submission
        // is none are graded, or the latest of those that are graded).

        for (Submission sub : submissions)
        {
            if (gradedSubmission == null
                || sub.isBetterGradingChoiceThan(gradedSubmission))
            {
                gradedSubmission = sub;
            }
        }

        // Now that the entire submission chain is fetched, update the
        // isSubmissionForGrading property among all of them.

        for (Submission sub : submissions)
        {
            sub.setIsSubmissionForGrading(sub == gradedSubmission);
        }
    }


    // ----------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    protected void migrateIsSubmissionForGrading(MigratingEditingContext mec)
    {
        if (user() == null || assignmentOffering() == null)
        {
            setIsSubmissionForGrading(false);
            return;
        }

        WCFetchSpecification<Submission> candidates =
            new WCFetchSpecification<Submission>(ENTITY_NAME,
                Submission.assignmentOffering.eq(assignmentOffering()).and(
                    Submission.user.eq(user())),
                submitNumber.descs());
        candidates.setRefreshesRefetchedObjects(false);
        refreshIsSubmissionForGrading(
            objectsWithFetchSpecification(mec, candidates));
    }


    // ----------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldMigratePartnerLink()
    {
        // TODO: Fix the performance problems with auto-migration
        // this method is temporarily disabled until the auto-migration
        // performance problems can be worked out.

//        return result() != null
//            && ((partnerLink() && primarySubmission() == null)
//                || (!partnerLink()
//                    && result().submissions().count() > 1
//                    && partneredSubmissions().count() == 0));
        return false;
    }


    // ----------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    protected void migratePartnerLink(MigratingEditingContext mec)
    {
        // TODO: Fix the performance problems with auto-migration
        // this method is temporarily disabled until the auto-migration
        // performance problems can be worked out.  To re-enable,
        // erase migratePartnerLink() below and use its contents here.
    }


    // ----------------------------------------------------------
    public void migratePartnerLink()
    {
        // guard from shouldMigratePartnerLink()
        if (!(resultIsReady()
              && ((partnerLink() && primarySubmission() == null)
                  || (!partnerLink()
                      && result().submissions().count() > 1
                      && partneredSubmissions().count() == 0))))
        {
            return;
        }

        // Implementation from migratePartnerLink(MigratingEditingContext)
        NSArray<Submission> mySubmissions = result().submissions();
        if (mySubmissions.count() <= 1)
        {
            return;
        }

        Submission primary = mySubmissions.get(0);

        // Likely an old submission that has not been migrated
        // to use the newer relationships, so search

        // First, find the primary submission
        for (Submission thisSubmission : mySubmissions)
        {
            // Don't check the relationship, since we're presuming
            // this batch of submissions only has the bits set,
            // but not the primarySubmission relationship filled.
            if (!thisSubmission.partnerLink())
            {
                primary = thisSubmission;
                break;
            }
        }

        if (primary.partnerLink())
        {
            // Yikes!  We searched, and didn't find *any* submissions
            // for this result without the partnerLink bit set!  So
            // promote this submission to be the primary submission
            log.error("Cannot locate any primary submission for "
                + "result " + this);
            for (Submission thisSubmission : mySubmissions)
            {
                log.error("    partner sub = "
                    + thisSubmission.user()
                    + " # "
                    + thisSubmission.submitNumber()
                    + " "
                    + thisSubmission.hashCode()
                    + ", "
                    + thisSubmission.partnerLink()
                    + ", pri = "
                    + (thisSubmission.primarySubmission() == null
                        ? null
                        : thisSubmission.primarySubmission().hashCode())
                    + ", "
                    + thisSubmission);
            }
            primary.setPartnerLink(false);
        }

        // Now, set up all the relationships for partners
        for (Submission thisSubmission : mySubmissions)
        {
            if (thisSubmission.partnerLink())
            {
                thisSubmission.setPrimarySubmissionRelationship(primary);
            }
        }

        // Now it is migrated!
    }


    // ----------------------------------------------------------
    /**
     * Gets the array of all submissions in the submission chain that contains
     * this submission (that is, all submissions for this submission's user and
     * assignment offering). The returned array is sorted by submission time in
     * ascending order.
     *
     * @return an NSArray containing the Submission objects in this submission
     *     chain
     */
    public NSArray<Submission> allSubmissions()
    {
        int newAOSubCount = assignmentOffering().submissions().count();
        if (newAOSubCount != aoSubmissionsCountCache)
        {
            allSubmissionsCache = null;
            aoSubmissionsCountCache = newAOSubCount;
        }

        if (allSubmissionsCache == null)
        {
            if (user() != null && assignmentOffering() != null)
            {
                allSubmissionsCache = submissionsForAssignmentOfferingAndUser(
                        editingContext(), assignmentOffering(), user());
            }
        }

        return (allSubmissionsCache != null) ?
                allSubmissionsCache : NO_SUBMISSIONS;
    }


    // ----------------------------------------------------------
    /**
     * Flush any cached data stored by the object in memory.
     */
    @Override
    public void flushCaches()
    {
        // Clear the in-memory cache of the all-submissions chain so that it
        // will be fetched again.

        aoSubmissionsCountCache = 0;
        allSubmissionsCache = null;
    }


    // ----------------------------------------------------------
    /**
     * Gets the submission in this submission chain that represents the
     * "submission for grading". If no manual grading has yet occurred, then
     * this is equivalent to {@link #latestSubmission()}. Otherwise, if a TA
     * has manually graded one or more submissions, then this method returns
     * the latest of those.
     *
     * @return the submission for grading in this submission chain
     */
    public Submission gradedSubmission()
    {
        NSArray<Submission> candidates = submissionsForGrading(
            editingContext(), assignmentOffering(), user());
        if (candidates.size() > 0)
        {
            return candidates.get(0);
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets the earliest (in other words, first) submission made in this
     * submission chain.
     *
     * @return the earliest submission in the submission chain
     */
    public Submission earliestSubmission()
    {
        if (user() == null || assignmentOffering() == null) return null;

        NSArray<Submission> subs = allSubmissions();

        if (subs != null && subs.count() >= 1)
        {
            return subs.objectAtIndex(0);
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets the latest (in other words, last) submission made in this
     * submission chain. Typically clients should prefer to use the
     * {@link #gradedSubmission()} method over this one, depending on their
     * policy regarding the grading of submissions that are not the most recent
     * one; this method exists for symmetry and to allow clients to distinguish
     * between the graded submission and the last one, if necessary.
     *
     * @return the latest submission in the submission chain
     */
    public Submission latestSubmission()
    {
        if (user() == null || assignmentOffering() == null) return null;

        NSArray<Submission> subs = allSubmissions();

        if (subs != null && subs.count() >= 1)
        {
            return subs.objectAtIndex(subs.count() - 1);
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets the index of the submission with the specified submission number in
     * the allSubmissions array. This function isolates the logic required to
     * handle the rare but possible case where there are gaps in the submission
     * numbers of a student's submissions.
     *
     * @param number the submission number to search for
     *
     * @return the index of that submission in the allSubmissions array
     */
    private int indexOfSubmissionWithSubmitNumber(int number)
    {
        NSArray<Submission> subs = allSubmissions();

        if (subs.isEmpty())
        {
            return -1;
        }

        int index = number - 1;

        if (index < 0)
        {
            index = 0;
        }
        else if (index > subs.count() - 1)
        {
            index = subs.count() - 1;
        }

        while (0 <= index && index < subs.count())
        {
            Submission sub = subs.objectAtIndex(index);

            if (sub.submitNumber() == number)
            {
                return index;
            }
            else if (sub.submitNumber() < number)
            {
                index++;
                if (index < subs.count()
                    && subs.objectAtIndex(index).submitNumber() > number)
                {
                    // oops! not found
                    return -1;
                }
            }
            else if (sub.submitNumber() > number)
            {
                index--;
                if (index >= 0
                    && subs.objectAtIndex(index).submitNumber() < number)
                {
                    // oops! not found
                    return -1;
                }
            }
        }

        return -1;
    }


    // ----------------------------------------------------------
    /**
     * Gets from the submission chain the submission with the specified
     * submission number.
     *
     * @param number the number of the submission to retrieve from the chain
     *
     * @return the specified submission, or null if there was not one with
     *     that number in the chain (or there was some other error)
     */
    public Submission submissionWithSubmitNumber(int number)
    {
        if (user() == null || assignmentOffering() == null)
        {
            return null;
        }

        int index = indexOfSubmissionWithSubmitNumber(number);

        if (index == -1)
        {
            return null;
        }
        else
        {
            return allSubmissions().objectAtIndex(index);
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets the previous submission to this one in the submission chain.
     *
     * @return the previous submission, or null if it is the first one (or
     *     there was an error)
     */
    public Submission previousSubmission()
    {
        if (submitNumberRaw() == null)
        {
            return null;
        }
        else
        {
            NSArray<Submission> subs = allSubmissions();
            int index = indexOfSubmissionWithSubmitNumber(submitNumber());

            if (index <= 0)
            {
                return null;
            }
            else
            {
                return subs.objectAtIndex(index - 1);
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets the next submission following this one in the submission chain.
     *
     * @return the next submission, or null if it is the first one (or
     *     there was an error)
     */
    public Submission nextSubmission()
    {
        if (submitNumberRaw() == null)
        {
            return null;
        }
        else
        {
            NSArray<Submission> subs = allSubmissions();
            int index = indexOfSubmissionWithSubmitNumber(submitNumber());

            if (index == -1 || index == subs.count() - 1)
            {
                return null;
            }
            else
            {
                return subs.objectAtIndex(index + 1);
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets the earliest submission in the submission chain for this user and
     * assignment offering that has valid non-zero coverage data.
     *
     * @return the earliest submission with valid non-zero coverage data, or
     *     null if there is no submission satisfying this
     */
    public Submission earliestSubmissionWithCoverage()
    {
        NSArray<Submission> subs = allSubmissions();

        for(Submission submission : subs)
        {
            if (submission.hasCoverage())
            {
                return submission;
            }
        }

        return null;
    }


    // ----------------------------------------------------------
    /**
     * Gets a value indicating whether the submission has valid non-zero
     * coverage data. This implies that the submission compiled without error
     * and executed at least partially (but only for plug-ins that collect code
     * coverage statistics).
     *
     * @return true if the submission has valid non-zero coverage data,
     *     otherwise false.
     */
    public boolean hasCoverage()
    {
        if (!resultIsReady())
        {
            return false;
        }

        NSArray<SubmissionFileStats> files = result().submissionFileStats();

        for(SubmissionFileStats file : files)
        {
            if (file.elements() > 0)
            {
                return true;
            }
        }

        return false;
    }


    // ----------------------------------------------------------
    /**
     * Gets the earliest submission in the submission chain for this user and
     * assignment offering that has valid lines-of-code data.
     *
     * @return the earliest submission with valid lines-of-code data, or
     *     null if there is no submission satisfying this
     */
    public Submission earliestSubmissionWithLOC()
    {
        NSArray<Submission> subs = allSubmissions();

        for(Submission submission : subs)
        {
            if (submission.hasLOC())
            {
                return submission;
            }
        }

        return null;
    }


    // ----------------------------------------------------------
    /**
     * Gets a value indicating whether the submission has valid lines-of-code
     * data.
     *
     * @return true if the submission has valid non-zero lines-of-code data,
     *     otherwise false.
     */
    public boolean hasLOC()
    {
        if (!resultIsReady())
        {
            return false;
        }

        NSArray<SubmissionFileStats> files = result().submissionFileStats();

        for(SubmissionFileStats file : files)
        {
            if (file.loc() > 0)
            {
                return true;
            }
        }

        return false;
    }


    // ----------------------------------------------------------
    /**
     * Gets the earliest submission in the submission chain for this user and
     * assignment offering that has a non-zero correctness score.
     *
     * @return the earliest submission with a non-zero correctness score, or
     *     null if there is no submission satisfying this
     */
    public Submission earliestSubmissionWithCorrectnessScore()
    {
        NSArray<Submission> subs = allSubmissions();

        for(Submission submission : subs)
        {
            if (submission.hasCorrectnessScore())
            {
                return submission;
            }
        }

        return null;
    }


    // ----------------------------------------------------------
    /**
     * Gets a value indicating whether the submission has a non-zero
     * correctness score. This implies that the code ran, but the converse is
     * not true of course -- not all code that runs attains a non-zero
     * correctness score.
     *
     * @return true if the submission has a non-zero correctness score.
     */
    public boolean hasCorrectnessScore()
    {
        if (!resultIsReady())
        {
            return false;
        }

        return result().correctnessScore() > 0;
    }


    // ----------------------------------------------------------
    /**
     * Gets the earliest submission in the submission chain for this user and
     * assignment offering that has a non-zero correctness score.
     *
     * @return the earliest submission with a non-zero correctness score, or
     *     null if there is no submission satisfying this
     */
    public Submission earliestSubmissionWithAnyData()
    {
        NSArray<Submission> subs = allSubmissions();

        for(Submission submission : subs)
        {
            if (submission.hasAnyData())
            {
                return submission;
            }
        }

        return null;
    }


    // ----------------------------------------------------------
    /**
     * Gets a value indicating whether the submission has any data of interest
     * as generated by the grading plug-ins and grading process. For now, this
     * includes the following items: coverage data, lines-of-code, correctness
     * score.
     *
     * @return true if the submission has any valid grading data, otherwise
     *     false.
     */
    public boolean hasAnyData()
    {
        // The order of these is important; they should be listed from most
        // efficient to least efficient in order to short-circuit the test as
        // quickly as possible. Correctness-score only requires checking the
        // field on the results object; coverage and LOC require fetching all
        // of the SubmissionFileStats and iterating over them.

        if (hasCorrectnessScore() || hasCoverage() || hasLOC())
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets the next earliest submission to this one in the submission chain
     * that has any data (LOC, coverage, or correctness score).
     *
     * @return the previous submission with any data, or null if there were no
     *     submissions before this one that had any data
     */
    public Submission previousSubmissionWithAnyData()
    {
        Submission prevSub = previousSubmission();

        while (prevSub != null && !prevSub.hasAnyData())
        {
            prevSub = prevSub.previousSubmission();
        }

        return prevSub;
    }


    // ----------------------------------------------------------
    /**
     * Gets the first submission following this one in the submission chain
     * that has any data (LOC, coverage, or correctness score).
     *
     * @return the next submission with any data, or null if there were no
     *     submissions after this one that had any data
     */
    public Submission nextSubmissionWithAnyData()
    {
        Submission nextSub = nextSubmission();

        while (nextSub != null && !nextSub.hasAnyData())
        {
            nextSub = nextSub.nextSubmission();
        }

        return nextSub;
    }


    // ----------------------------------------------------------
    /**
     * Gets a qualifier that can be used to fetch only submissions that are in
     * the same submission chain as this submission; that is, submissions by
     * the same user to the same assignment offering.
     *
     * @return the qualifier
     */
    public EOQualifier qualifierForSubmissionChain()
    {
        return ERXQ.and(
                ERXQ.equals("user", user()),
                ERXQ.equals("assignmentOffering", assignmentOffering()));
    }


    // ----------------------------------------------------------
    /**
     * Gets the earliest submission that has the highest correctness score
     * among all the submissions in this submission chain.
     *
     * @return the earliest submission with the highest correctness score
     */
    public Submission earliestSubmissionWithMaximumCorrectnessScore()
    {
        if (user() == null || assignmentOffering() == null)
        {
            return null;
        }

        NSArray<Submission> subs = allSubmissions();
        Submission maxSubmission = null;
        double maxCorrectnessScore = Double.MIN_VALUE;

        for (Submission sub : subs)
        {
            if (sub.resultIsReady())
            {
                double score = sub.result().correctnessScore();
                if (score > maxCorrectnessScore)
                {
                    maxSubmission = sub;
                    maxCorrectnessScore = score;
                }
            }
        }

        return maxSubmission;
    }


    // ----------------------------------------------------------
    /**
     * Gets the earliest submission that has the highest tool score among all
     * the submissions in this submission chain.
     *
     * @return the earliest submission with the highest tool score
     */
    public Submission earliestSubmissionWithMaximumToolScore()
    {
        if (user() == null || assignmentOffering() == null)
        {
            return null;
        }

        NSArray<Submission> subs = allSubmissions();
        Submission maxSubmission = null;
        double maxToolScore = Double.MIN_VALUE;

        for (Submission sub : subs)
        {
            if (sub.resultIsReady())
            {
                double score = sub.result().toolScore();
                if (score > maxToolScore)
                {
                    maxSubmission = sub;
                    maxToolScore = score;
                }
            }
        }

        return maxSubmission;
    }


    // ----------------------------------------------------------
    /**
     * Gets the earliest submission that has the highest automated score
     * (correctness + tool) among all the submissions in this submission chain.
     *
     * @return the earliest submission with the highest automated score
     */
    public Submission earliestSubmissionWithMaximumAutomatedScore()
    {
        if (user() == null || assignmentOffering() == null)
        {
            return null;
        }

        NSArray<Submission> subs = allSubmissions();
        Submission maxSubmission = null;
        double maxAutomatedScore = Double.MIN_VALUE;

        for (Submission sub : subs)
        {
            if (sub.resultIsReady())
            {
                double score = sub.result().automatedScore();
                if (score > maxAutomatedScore)
                {
                    maxSubmission = sub;
                    maxAutomatedScore = score;
                }
            }
        }

        return maxSubmission;
    }


    // ----------------------------------------------------------
    /**
     * Gets a value indicating whether or not the submission was late.
     *
     * @return true if the submission was late, otherwise false
     */
    public boolean isLate()
    {
        return submitTime().after(assignmentOffering().dueDate());
    }


    // ----------------------------------------------------------
    /**
     * Get the "status" for this submission: suspended, cancelled, or queued.
     * @return The status, or null if the submission has been completely
     *         processed and has a result available.
     */
    public String status()
    {
        // Force job to be retrieved first, to avoid races
        EnqueuedJob job = enqueuedJob();
        String status = null;
        if (result() == null || job != null)
        {
            if (job == null)
            {
                status = "cancelled";
            }
            else if (job.paused())
            {
                status = "suspended";
            }
            else
            {
                status = "queued for grading";
            }
        }
        return status;
    }


    // ----------------------------------------------------------
    /**
     * Computes the difference between the submission time and the
     * due date/time, and renders it in a human-readable string.
     *
     * @return the string representation of how early or late
     */
    public String earlyLateStatus()
    {
        String description = null;
        long time = submitTime().getTime();
        long dueTime = assignmentOffering().dueDate().getTime();
        if (dueTime >= time)
        {
            // Early submission
            description =
                Submission.getStringTimeRepresentation(dueTime - time)
                + " early";
        }
        else
        {
            // Late submission
            description =
                Submission.getStringTimeRepresentation(time - dueTime)
                + " late";
        }
        return description;
    }


    // ----------------------------------------------------------
    /**
     * Gets the path to the grading.properties file associated with this
     * submission, or where it would be if it did exist.
     *
     * @return the path to the grading.properties file for this submission
     */
    public File gradingPropertiesFile()
    {
        return new File(resultDirName(), SubmissionResult.propertiesFileName());
    }


    // ----------------------------------------------------------
    /**
     * Creates the initial grading.properties file for this submission and
     * returns a WCProperties object representing its contents.
     *
     * If the file already exists, this method does not overwrite it, and it
     * returns the properties that are already in the file.
     *
     * @return a WCProperties object representing the content of the initial
     *     grading.properties file
     * @throws IOException if an I/O error occurs
     */
    public WCProperties createInitialGradingPropertiesFile() throws IOException
    {
        WCProperties properties = new WCProperties();

        File propertiesFile = gradingPropertiesFile();
        if (propertiesFile.exists())
        {
            properties.load(propertiesFile.getAbsolutePath());
            return properties;
        }

        // Create the results directory if it does not already exist.

        propertiesFile.getParentFile().mkdirs();

        // Set the initial properties that only depend on the submission or
        // the application configuration.

        properties.addPropertiesFromDictionaryIfNotDefined(Application
            .wcApplication().subsystemManager().pluginProperties());
        properties.setProperty("frameworksBaseURL",
            Application.application().frameworksBaseURL());

        properties.setProperty("userName", user().userName());
        properties.setProperty("resultDir", resultDirName());
        properties.setProperty("scriptData", GradingPlugin.scriptDataRoot());

        String crn = assignmentOffering().courseOffering().crn();
        properties.setProperty("course",
            assignmentOffering().courseOffering().course().deptNumber());
        properties.setProperty("CRN", (crn == null) ? "null" : crn);
        properties.setProperty("assignment",
            assignmentOffering().assignment().name());
        properties.setProperty("dueDateTimestamp",
            Long.toString(assignmentOffering().dueDate().getTime()));
        properties.setProperty("submissionTimestamp",
            Long.toString(submitTime().getTime()));
        properties.setProperty("submissionNo",
            Integer.toString(submitNumber()));

        properties.setProperty("numReports", "0");

        Number toolPts = assignmentOffering().assignment()
            .submissionProfile().toolPointsRaw();
        properties.setProperty("max.score.tools",
            (toolPts == null) ? "0" : toolPts.toString());

        double maxCorrectnessScore = assignmentOffering().assignment()
            .submissionProfile().availablePoints();
        if (toolPts != null)
        {
            maxCorrectnessScore -= toolPts.doubleValue();
        }

        Number TAPts = assignmentOffering().assignment()
            .submissionProfile().taPointsRaw();
        if (TAPts != null)
        {
            maxCorrectnessScore -= TAPts.doubleValue();
        }
        properties.setProperty("max.score.correctness",
            Double.toString(maxCorrectnessScore));

        // Write the properties file to disk.

        BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(propertiesFile));
        properties.store(
                out, "Web-CAT grader script configuration properties");
        out.close();

        return properties;
    }


    // ----------------------------------------------------------
    public String contentsOfResultFile(String relativePath) throws IOException
    {
        // Massage the path a little so we can prevent access outside the
        // Results folder.

        relativePath = relativePath.replace('\\', '/');
        while (relativePath.startsWith("./"))
        {
            relativePath = relativePath.substring(2);
        }

        if (relativePath.startsWith("../") || relativePath.startsWith("/"))
        {
            throw new IllegalArgumentException(
                "Path must not include parent directory or root directory"
                + "components");
        }

        File file = new File(resultDirName(), relativePath);

        if (file.isDirectory())
        {
            throw new IllegalArgumentException(
            "Path must be a file, not a directory");
        }

        return ERXFileUtilities.stringFromFile(file);
    }


    // ----------------------------------------------------------
    @Override
    public void mightDelete()
    {
        log.debug("mightDelete()");
        subdirToDelete = dirName();
        super.mightDelete();
    }


    // ----------------------------------------------------------
    @Override
    public void didDelete( EOEditingContext context )
    {
        log.debug("didDelete()");
        super.didDelete( context );
        // should check to see if this is a child ec
        EOObjectStore parent = context.parentObjectStore();
        if (parent == null || !(parent instanceof EOEditingContext))
        {
            if (subdirToDelete != null)
            {
                File dir = new File(subdirToDelete);
                if (dir.exists())
                {
                    org.webcat.core.FileUtilities.deleteDirectory(dir);
                }
            }
        }
    }


    // ----------------------------------------------------------
    public void sendScoreToLTIConsumerIfNecessary()
    {
        log.debug("sendScoreToLTIConsumerIfNecessary(): checking " + this);
        if (isSubmissionForGrading())
        {
            SubmissionResult r = result();
            if (r != null && r.status() == Status.CHECK)
            {
                log.debug("sendScoreToLTIConsumerIfNecessary(): "
                    + "attempting to send score");
                LISResultId.sendScoreToLTIConsumer(this);
            }
        }
    }


    // ----------------------------------------------------------
    @Override
    public void willUpdate()
    {
//        if (isSubmissionForGrading()
//            && changedProperties().containsKey(IS_SUBMISSION_FOR_GRADING_KEY))
        {
            sendScoreToLTIConsumerIfNecessary();
        }
        super.willUpdate();
    }


    // ----------------------------------------------------------
    /**
     * Find all submissions that are used for scoring for a given
     * assignment by the specified users.
     *
     * @param ec                 The editing context
     * @param anAssignmentOffering The offering to search for.
     * @param omitPartners       If false, include submissions from all
     *                           partners working together.  If true,
     *                           include only the primary submitter's
     *                           submission.
     * @param users              The list of users to find submissions for.
     * @param accumulator        If non-null, use this object to accumulate
     *                           descriptive summary statistics about the
     *                           submissions.
     * @return An array of the user/submission pairs.
     */
    public static NSArray<UserSubmissionPair> submissionsForGrading(
        EOEditingContext   ec,
        AssignmentOffering anAssignmentOffering,
        boolean            omitPartners,
        NSArray<User>      users,
        CumulativeStats    accumulator)
    {
        NSMutableDictionary<User, Submission> submissions =
            new NSMutableDictionary<User, Submission>();
        NSMutableArray<User> realUsers = users.mutableClone();

        boolean brokenPartners = submissionsForGradingWithMigration(
            ec, anAssignmentOffering, omitPartners, realUsers, submissions,
            accumulator);
        if (brokenPartners)
        {
            // On the first fetch, some partner subs were found that
            // were hooked to the wrong assignment, and they were updated.
            // So re-execute the action to pull in all the results after
            // this fix.
            realUsers = users.mutableClone();
            brokenPartners = submissionsForGradingWithMigration(
                ec, anAssignmentOffering, omitPartners, realUsers,
                submissions, accumulator);
            if (brokenPartners)
            {
                log.error("submissionsForGrading() still found broken "
                    + "partner submissions after two rounds.");
            }
        }

        return generateUserSubmissionPairs(realUsers, submissions);
    }


    // ----------------------------------------------------------
    private static NSArray<UserSubmissionPair> generateUserSubmissionPairs(
            NSArray<User> users, NSDictionary<User, Submission> submissions)
    {
        NSMutableArray<UserSubmissionPair> pairs =
            new NSMutableArray<UserSubmissionPair>(users.size());

        for (User aUser : users)
        {
            Submission submission = submissions.objectForKey(aUser);
            pairs.addObject(new UserSubmissionPair(aUser, submission));
        }

        return pairs;
    }


    // ----------------------------------------------------------
    /**
     * Find all submissions that are used for scoring for a given
     * assignment by the specified users.
     *
     * @param anAssignmentOffering The offering to search for.
     * @param omitPartners       If false, include submissions from all
     *                           partners working together.  If true,
     *                           include only the primary submitter's
     *                           submission.
     * @param users              The list of users to find submissions for.
     * @param submissions        The submissions for grading, keyed by user.
     * @param accumulator        If non-null, use this object to accumulate
     *                           descriptive summary statistics about the
     *                           submissions.
     * @return True if any broken partner submissions were found and we need
     *     to refetch.
     */
    private static boolean submissionsForGradingWithMigration(
        EOEditingContext                      ec,
        final AssignmentOffering              anAssignmentOffering,
        boolean                               omitPartners,
        NSMutableArray<User>                  users,
        NSMutableDictionary<User, Submission> submissions,
        CumulativeStats                       accumulator)
    {
        final NSMutableArray<Submission> brokenPartners =
            new NSMutableArray<Submission>();
//        final Set<Submission> partnerSubsForGrading =
//            new HashSet<Submission>();
//  final NSMutableDictionary<User, Submission> alternatePrimarySubs =
//      new NSMutableDictionary<User, Submission>();

//        final Set<User> keepUsers = new HashSet<User>();
//
//        for (User u : users)
//        {
//            keepUsers.add(u);
//        }
//
//        WCFetchSpecification<Submission> fspec =
//            new WCFetchSpecification<Submission>(
//            Submission.ENTITY_NAME,
//            Submission.assignmentOffering.eq(anAssignmentOffering),
//            Submission.isSubmissionForGrading.descs()
//            .then(Submission.result.dot(SubmissionResult.lastUpdated).desc())
//            .then(Submission.result.dot(SubmissionResult.status).desc())
//            .then(Submission.submitTime.desc())
//            .then(Submission.submitNumber.desc()));
//        fspec.setUsesDistinct(true);
//        fspec.setPrefetchingRelationshipKeyPaths(
//            new NSArray<String>("result"));
//        NSArray<Submission> candidates =
//            objectsWithFetchSpecification(ec, fspec);
//        for (Submission sub : candidates)
//        {
//            if (keepUsers.contains(sub.user()))
//            {
//                Submission forGrading = submissions.get(sub.user());
//                if (sub.isBetterGradingChoiceThan(forGrading))
//                {
//                    submissions.put(sub.user(), sub);
//                }
//            }
//        }
//
//        // Accumulate stats, including partners here
//        if (accumulator != null)
//        {
//            for (Submission sub : submissions.values())
//            {
//                accumulator.accumulate(sub);
//            }
//        }
//
//        // Filter partners if necessary
//        if (omitPartners)
//        {
//            NSMutableArray<User> toRemove = new NSMutableArray<User>();
//            for (Submission sub : submissions.values())
//            {
//                if (sub.partnerLink())
//                {
//                    Submission primary = sub.primarySubmission();
//                    Submission primaryForGrading =
//                        submissions.get(primary.user());
//                    if (primaryForGrading != null
//                        && primaryForGrading == primary)
//                    {
//                        users.remove(sub.user());
//                        toRemove.add(sub.user());
//                    }
//                }
//            }
//            for (User u : toRemove)
//            {
//                submissions.remove(u);
//            }
//        }
//
//        return false;

        for (User student : users.immutableClone())
        {
            log.debug("Scanning submissions for " + student);
            NSArray<Submission> candidates =
                Submission.objectsMatchingQualifier(
                    ec,
                    Submission.assignmentOffering.eq(anAssignmentOffering).and(
//                        Submission.result.isNotNull()).and(
                            Submission.user.eq(student)));

            if (log.isDebugEnabled())
            {
                log.debug("candidates using old fetch = ");
                for (Submission s : candidates)
                {
                    System.out.println("\t" + s);
                }

                NSArray<Submission> newCandidates =
                    Submission.objectsMatchingQualifier(
                        ec,
                        Submission.assignmentOffering.eq(anAssignmentOffering)
                            .and(Submission.user.eq(student)),
                        Submission.isSubmissionForGrading
                            .descs().then(
                        Submission.result.dot(SubmissionResult.lastUpdated)
                            .desc()).then(
                        Submission.result.dot(SubmissionResult.status)
                            .desc()).then(
                        Submission.submitNumber.desc()));
                System.out.println();
                log.debug("candidates using new fetch = ");
                for (Submission s : newCandidates)
                {
                    System.out.println("\t" + s);
                    System.out.println("\t\tnumber = " + s.submitNumber());
                    System.out.println("\t\tlast updated = "
                        + s.result().lastUpdated());
                    System.out.println("\t\tstatus = " + s.result().status());
                    System.out.println("\t\tfor grading = "
                        + s.isSubmissionForGrading());
                }
            }

            Submission forGrading = null;
            Submission bestPrimary = null;
            for (Submission sub : candidates)
            {
                if (!sub.resultIsReady())
                {
                    continue;
                }
                // Check to see if any partners are accidentally broken,
                // and point to the wrong assignment due to an earlier bug
                // in partnerWith()
                for (Submission psub : sub.result().submissions())
                {
                    if (psub != sub
                        && psub.assignmentOffering() != null
                        && sub.assignmentOffering() != null
                        && psub.assignmentOffering().assignment() !=
                            sub.assignmentOffering().assignment())
                    {
                        brokenPartners.add(psub);
                    }
                }
                sub.migratePartnerLink();
                if (forGrading == null
                    || sub.isBetterGradingChoiceThan(forGrading))
                {
                    forGrading = sub;
                }
                if (!sub.partnerLink()
                    && (bestPrimary == null
                        || sub.isBetterGradingChoiceThan(bestPrimary)))
                {
                    bestPrimary = sub;
                }
            }

            if (forGrading != null)
            {
                if (omitPartners && forGrading.partnerLink())
                {
//                    if (bestPrimary == null)
//                    {
//                        users.removeObject(student);
//                    }
//                    else
//                    {
//                        partnerSubsForGrading.add(forGrading);
//                        alternatePrimarySubs.put(student, bestPrimary);
//                    }
                    users.removeObject(student);
                }
                else
                {
                    submissions.setObjectForKey(forGrading, student);
                    if (accumulator != null)
                    {
                        accumulator.accumulate(forGrading);
                    }
                }
            }

            // If any partner submissions that were incorrectly hooked to
            // the wrong assignment were found, patch them now.
            if (brokenPartners.count() > 0)
            {
                new ECAction() { public void action() {
                    try
                    {
                        AssignmentOffering offering =
                            anAssignmentOffering.localInstance(ec);
                        for (Submission sub : brokenPartners)
                        {
                            Submission psub = sub.localInstance(ec);
                            log.warn("found partner submission "
                                + psub.user() + " #" + psub.submitNumber()
                                + "\non incorrect assignment offering "
                                + psub.assignmentOffering());

                            NSArray<AssignmentOffering> partnerOfferings =
                                AssignmentOffering.objectsMatchingQualifier(
                                    ec,
                                    AssignmentOffering.courseOffering
                                        .dot(CourseOffering.course).eq(
                                            offering.courseOffering().course())
                                    .and(AssignmentOffering.courseOffering
                                        .dot(CourseOffering.students).eq(
                                            psub.user()))
                                    .and(AssignmentOffering.assignment
                                    .eq(offering.assignment())));
                            if (partnerOfferings.count() == 0)
                            {
                                log.error("Cannot locate correct assignment "
                                    + "offering for partner"
                                    + psub.user() + " #" + psub.submitNumber()
                                    + "\non incorrect assignment offering "
                                    + psub.assignmentOffering());
                            }
                            else
                            {
                                if (partnerOfferings.count() > 1)
                                {
                                    log.warn("Multiple possible offerings for "
                                        + "partner "
                                        + psub.user() + " #"
                                        + psub.submitNumber()
                                        + "\non incorrect assignment offering "
                                        + psub.assignmentOffering());
                                    for (AssignmentOffering ao :
                                        partnerOfferings)
                                    {
                                        log.warn("\t" + ao);
                                    }
                                }

                                psub.setAssignmentOfferingRelationship(
                                    partnerOfferings.get(0));
                            }
                        }
                        ec.saveChanges();
                    }
                    catch (Exception e)
                    {
                        log.error(
                            "Cannot update broken partner submissions", e);
                    }
                }}.run();
            }
        }

        // Add any potential partner subs back
//        Set<Submission> primarySubmissionsShown =
//            new HashSet<Submission>(submissions.values());
//        for (Submission partnered : partnerSubsForGrading)
//        {
//            User student = partnered.user();
//            if (primarySubmissionsShown.contains(partnered.primarySubmission())
//                || primarySubmissionsShown.contains(
//                    alternatePrimarySubs.get(student)))
//            {
//                users.removeObject(student);
//            }
//            else
//            {
//                Submission forGrading = alternatePrimarySubs.get(student);
//                primarySubmissionsShown.add(forGrading);
//                submissions.setObjectForKey(forGrading, student);
//                if (accumulator != null)
//                {
//                    accumulator.accumulate(forGrading);
//                }
//            }
//        }

        //return new Submissions(subs, brokenPartners);
        return brokenPartners.count() > 0;
    }


    // ----------------------------------------------------------
    /**
     * Find all submissions that are used for scoring for a given
     * assignment.
     *
     * @param ec                 The editing context
     * @param anAssignmentOffering The offering to search for.
     * @param omitPartners       If false, include submissions from all
     *                           partners working together.  If true,
     *                           include only the primary submitter's
     *                           submission.
     * @param omitStaff          If true, leave out course staff.
     * @param accumulator        If non-null, use this object to accumulate
     *                           descriptive summary statistics about the
     *                           submissions.
     * @return An array of the submissions found.
     */
    public static NSArray<UserSubmissionPair> submissionsForGrading(
        EOEditingContext   ec,
        AssignmentOffering anAssignmentOffering,
        boolean            omitPartners,
        boolean            omitStaff,
        CumulativeStats    accumulator)
    {
        CourseOffering courseOffering = anAssignmentOffering.courseOffering();
        NSArray<User> users = omitStaff
            ? courseOffering.studentsWithoutStaff()
            : courseOffering.studentsAndStaff();

        return submissionsForGrading(
            ec, anAssignmentOffering, omitPartners, users, accumulator);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve objects according to the <code>submissionsForGrading</code>
     * fetch specification.
     *
     * @param context The editing context to use
     * @param assignmentOfferingBinding fetch spec parameter
     * @param userBinding fetch spec parameter
     * @return an NSArray of the entities retrieved
     */
    public static NSArray<Submission> submissionsForGrading(
            EOEditingContext context,
            org.webcat.grader.AssignmentOffering assignmentOfferingBinding,
            org.webcat.core.User userBinding
        )
    {
        NSArray<Submission> subs = _Submission.submissionsForGrading(
            context, assignmentOfferingBinding, userBinding);

        if (subs.size() == 0 || !subs.get(0).isSubmissionForGrading())
        {
            NSArray<UserSubmissionPair> subPairs = submissionsForGrading(
                context,
                assignmentOfferingBinding,
                false,  // omitPartners
                new NSArray<User>(userBinding),
                null);

            if (subPairs.size() > 0
                && subPairs.objectAtIndex(0).submission() != null)
            {
                Submission graded = subPairs.objectAtIndex(0).submission();
//                EOEditingContext local = Application.newPeerEditingContext();
//                try
//                {
//                    local.lock();
//                    Submission localSub = graded.localInstance(local);
//                    localSub.setIsSubmissionForGrading(true);
//                    local.saveChanges();
//                }
//                finally
//                {
//                    try
//                    {
//                        local.unlock();
//                    }
//                    finally
//                    {
//                        Application.releasePeerEditingContext(local);
//                    }
//                }
                subs = new NSArray<Submission>(graded);
            }
        }

        return subs;
    }


    // ----------------------------------------------------------
    /**
     * Fetches the most recent submission by each user for a particular
     * assignment.
     *
     * @param ec the editing context
     * @param offering the assignment offering whose submissions should be
     *     fetched
     *
     * @return a dictionary with users as keys and the most recent submission
     *     by that user as the value; only users who have made a submission
     *     will appear in this map
     */
    public static NSDictionary<User, Submission> latestSubmissionsForAssignment(
            EOEditingContext ec,
            AssignmentOffering offering)
    {
        EOModelGroup modelGroup = ERXEOAccessUtilities.modelGroup(ec);
        EOEntity entity = modelGroup.entityNamed(ENTITY_NAME);
        String modelName = entity.model().name();

        String sql = "select OID as id, max(CSUBMITNUMBER) as csubmitnumber"
            + " from TSUBMISSION where CASSIGNMENTID = "
            + offering.id() + " group by CUSERID";

        @SuppressWarnings("unchecked")
        NSArray<NSMutableDictionary<String, Object>> rawRows =
            EOUtilities.rawRowsForSQL(ec, modelName, sql, null);

        NSMutableArray<EOGlobalID> gids = new NSMutableArray<EOGlobalID>();
        for (NSMutableDictionary<String, Object> row : rawRows)
        {
            // This feels kind of like a hack; for globalIDForRow to work
            // correctly, the object id must have the key "id".
            row.setObjectForKey(row.objectForKey("OID"), "id");

            EOGlobalID gid = entity.globalIDForRow(row);

            if (gid != null)
            {
                gids.add(gid);
            }
        }

        @SuppressWarnings("unchecked")
        NSArray<Submission> submissions =
            ERXEOGlobalIDUtilities.fetchObjectsWithGlobalIDs(ec, gids);

        NSMutableDictionary<User, Submission> subMap =
            new NSMutableDictionary<User, Submission>();

        for (Submission sub : submissions)
        {
            subMap.setObjectForKey(sub, sub.user());
        }

        return subMap;
    }


    // ----------------------------------------------------------
    /**
     * A class used to accumulate basic descriptive statistics about
     * multiple submission results.
     */
    public static class CumulativeStats
    {
        //~ Fields ............................................................

        private double            min;
        private double            max;
        private double            total;
        private ArrayList<Double> allScores;
        private Double            cachedMedian;
        private final String      scoreKeyPath;


        //~ Constructors ......................................................

        // ----------------------------------------------------------
        /**
         * Create a new, empty object.
         * @param scoreKeyPath The key path in the scorables to use to
         *                     extract the base stat to be accumulated.
         */
        public CumulativeStats(String scoreKeyPath)
        {
            this.scoreKeyPath = scoreKeyPath;
            min = 0.0;
            max = 0.0;
            total = 0.0;
            allScores = new ArrayList<Double>();
        }


        // ----------------------------------------------------------
        /**
         * Create a new, empty object.
         */
        public CumulativeStats()
        {
            this("finalScore");
        }


        //~ Methods ...........................................................

        // ----------------------------------------------------------
        /**
         * Accumulate data about the given submission result.
         * @param subResult The submission result to add
         */
        public void accumulate(Scorable subResult)
        {
            double score = ((Number)subResult.valueForKeyPath(scoreKeyPath))
                .doubleValue();
            if (allScores.size() == 0)
            {
                min = score;
                max = score;
            }
            else
            {
                if (score < min)
                {
                    min = score;
                }
                if (score > max)
                {
                    max = score;
                }
            }
            total += score;

            cachedMedian = null;
            allScores.add(score);
        }


        // ----------------------------------------------------------
        /**
         * Accumulate data about the given submission, if it has a result.
         * @param submission The submission to add
         */
        public void accumulate(Submission submission)
        {
            if (submission.resultIsReady())
            {
                accumulate(submission.result());
            }
        }


        // ----------------------------------------------------------
        /**
         * Retrieve the minimum final score of all submission results
         * accumulated so far.
         * @return The minimum score
         */
        public double min()
        {
            return min;
        }


        // ----------------------------------------------------------
        /**
         * Retrieve the maximum final score of all submission results
         * accumulated so far.
         * @return The maximum score
         */
        public double max()
        {
            return max;
        }


        // ----------------------------------------------------------
        /**
         * Retrieve the mean (average) final score over all submission
         * results accumulated so far.
         * @return The mean score
         */
        public double mean()
        {
            return (allScores.size() > 1)
                ? (total / allScores.size())
                : total;
        }


        // ----------------------------------------------------------
        /**
         * Retrieve the median final score over all submission results
         * accumulated so far.
         * @return the median score
         */
        public double median()
        {
            if (cachedMedian == null)
            {
                Collections.sort(allScores);

                int count = allScores.size();

                if (count == 0)
                {
                    return 0;
                }
                else if (count % 2 == 0)
                {
                    return (allScores.get((count / 2) - 1)
                            + allScores.get(count / 2)) / 2;
                }
                else
                {
                    return allScores.get(count / 2);
                }
            }

            return (cachedMedian != null ? cachedMedian : 0.0);
        }


        // ----------------------------------------------------------
        public java.util.List<Double> allScores()
        {
            return allScores;
        }


        // ----------------------------------------------------------
        public String toString()
        {
            return "stats: "
                + allScores.size()
                + " subs: hi = "
                + max()
                + ", low = "
                + min()
                + ", avg = "
                + mean();
        }
    }


    //~ Instance/static variables .............................................

    private int aoSubmissionsCountCache;
    private NSArray<Submission> allSubmissionsCache;

    private String cachedPermalink;
    private String subdirToDelete;

    private static final NSArray<Submission> NO_SUBMISSIONS =
        new NSArray<Submission>();

    static Logger log = Logger.getLogger(Submission.class);
}
