/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2009 Virginia Tech
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
import er.extensions.eof.ERXQ;
import er.extensions.foundation.ERXFileUtilities;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.core.messaging.UnexpectedExceptionMessage;
import org.webcat.grader.messaging.GradingResultsAvailableMessage;

// -------------------------------------------------------------------------
/**
 *  Represents a single student assignment submission.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author$
 *  @version $Revision$, $Date$
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
    public String userPresentableDescription()
    {
        if ( fileName() != null )
            return file().getPath();
        else
            return dirName();
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where this submission is stored.
     * @return the directory name
     */
    public String dirName()
    {
        if ( partnerLink() && result() != null )
        {
            return result().submission().dirName();
        }
        else
        {
            StringBuffer dir =
                ( user() == null )
                ? new StringBuffer("<null>")
                : user().authenticationDomain().submissionBaseDirBuffer();
            if ( assignmentOffering() != null )
            {
                assignmentOffering().addSubdirTo( dir );
            }
            else
            {
                dir.append( "/ASSIGNMENT" );
            }
            dir.append( '/' );
            dir.append( ( user() == null )
                ? new StringBuffer("<null>")
                : user().userName() );
            dir.append( '/' );
            dir.append( submitNumber() );
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
        return new File( dirName(), fileName() );
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
                editingContext() , this ).objectForKey( "id" );
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
                    + "id for a submission.\n\nSubmission = " + subInfo).send();

            return ERXConstant.ZeroInteger;
        }
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
     * Converts a time to its human-readable format.  Most useful
     * when the time is "small," like a difference between two
     * other time stamps.
     *
     * @param time The time to convert
     * @return     A human-readable version of the time
     */
    public static String getStringTimeRepresentation( long time )
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

        if ( days > 0 )
        {
            buffer.append( days );
            buffer.append( " day" );
            if ( days > 1 )
                buffer.append( 's' );
        }
        if ( hours > 0 )
        {
            if ( buffer.length() > 0 )
                buffer.append( ", " );
            buffer.append( hours );
            buffer.append( " hr" );
            if ( hours > 1 )
                buffer.append( 's' );
        }
        if ( minutes > 0 )
        {
            if ( buffer.length() > 0 )
                buffer.append( ", " );
            buffer.append( minutes );
            buffer.append( " min" );
            if ( minutes > 1 )
                buffer.append( 's' );
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
        NSArray<EnqueuedJob> jobs = enqueuedJobs();
        if ( jobs != null  &&  jobs.count() > 0 )
        {
            if ( jobs.count() > 1 )
            {
                log.error( "too many jobs for submission " + this );
            }
            return jobs.objectAtIndex( 0 );
        }
        else
        {
            return null;
        }
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
        if (ec == null && assignmentOffering( )!= null)
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

        EOQualifier qualifier =
            Submission.assignmentOffering.eq(assignmentOffering())
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

        newSubmission.setFileName( fileName() );
        newSubmission.setPartnerLink( true );
        newSubmission.setSubmitNumber( partnerSubmitNumber );
        newSubmission.setSubmitTime( submitTime() );
        newSubmission.setAssignmentOfferingRelationship(
            assignmentOffering() );
        newSubmission.setResultRelationship( result() );
        newSubmission.setUserRelationship( partner );
        newSubmission.setPrimarySubmissionRelationship(this);

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
     * Gets a string containing the names of the user who made this submission
     * and all of his or her partners, in the form "User 1, User 2, ..., and
     * User N".
     *
     * @return a string containing the names of all of the partners
     */
    public String namesOfAllUsers()
    {
        NSMutableArray<String> names = new NSMutableArray<String>();
        names.addObject(user().name());

        for (Submission partnerSub : partneredSubmissions())
        {
            names.addObject(partnerSub.user().name());
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
     * Delete all the result information for this submission, including
     * all partner links.  This method uses the submission's current
     * editing context to make changes, but does <b>not</b> commit those
     * changes to the database (the caller must use
     * <code>saveChanges()</code>).
     */
    private void deleteResultsForAllPartners()
    {
        SubmissionResult myResult = result();
        if ( myResult != null )
        {
            log.debug( "removing SubmissionResult " + myResult );
            myResult.setIsMostRecent(false);
            setResultRelationship(null);
            for (Submission s : myResult.submissions())
            {
                s.setResultRelationship(null);
            }
            editingContext().deleteObject(myResult);
        }
    }


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
    public void requeueForGrading( EOEditingContext ec )
    {
        if ( enqueuedJob() == null )
        {
            Submission me = this;
            if ( ec != editingContext() )
            {
                me = localInstance( ec );
            }
            me.deleteResultsForAllPartners();
            log.debug( "creating new job for Submission " + this );
            EnqueuedJob job = new EnqueuedJob();
            job.setQueueTime( new NSTimestamp() );
            job.setRegrading( true );
            ec.insertObject( job );
            job.setSubmissionRelationship( me );
        }
    }


    // ----------------------------------------------------------
    public String permalink()
    {
        if ( cachedPermalink == null )
        {
            cachedPermalink = Application.configurationProperties()
                .getProperty( "base.url" )
                + "?page=MostRecent&"
                + ID_FORM_KEY + "=" + id();
        }
        return cachedPermalink;
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
            new GradingResultsAvailableMessage(user(), properties).send();
        }
        catch (Exception e)
        {
            log.error("Unable to notify student of grading results", e);
        }

/*        org.webcat.core.Application.sendSimpleEmail(
            user().email(),
            properties.stringForKeyWithDefault(
                "submission.email.title",
                "[Grader] results available: #${submission.number}, "
                + "${assignment.title}" ),
            properties.stringForKeyWithDefault(
                "submission.email.body",
                "The feedback report for ${assignment.title}\n"
                + "submission number ${submission.number} ${message}.\n\n"
                + "Log in to Web-CAT to view the report:\n\n"
                + "${submission.result.link}\n" )
            );*/
    }


    // ----------------------------------------------------------
    protected boolean shouldMigrateIsSubmissionForGrading()
    {
        return (isSubmissionForGradingRaw() == null);
    }


    // ----------------------------------------------------------
    protected void migrateIsSubmissionForGrading(MigratingEditingContext mec)
    {
        if (user() == null || assignmentOffering() == null)
        {
            setIsSubmissionForGrading(false);
            return;
        }

        Submission latestSubmission = null;
        Submission latestGradedSubmission = null;

        NSArray<Submission> thisSubmissionSet = allSubmissions();

        // Iterate over the whole submission set and find the
        // submission for grading (which is either the last submission
        // is none are graded, or the latest of those that are graded).

        for ( Submission sub : thisSubmissionSet )
        {
            if ( latestSubmission == null )
            {
                latestSubmission = sub;
            }
            else if (sub.submitNumber()
                     > latestSubmission.submitNumber())
            {
                latestSubmission = sub;
            }

            if ( sub.result() != null )
            {
                if ( sub.result().status() != Status.TO_DO )
                {
                    if ( latestGradedSubmission == null )
                    {
                        latestGradedSubmission = sub;
                    }
                    else if (sub.submitNumber() >
                             latestGradedSubmission.submitNumber() )
                    {
                        latestGradedSubmission = sub;
                    }
                }
            }
        }

        if ( latestGradedSubmission != null )
        {
            latestSubmission = latestGradedSubmission;
        }

        // Now that the entire submission chain is fetched, update the
        // isSubmissionForGrading property among all of them.

        for (Submission sub : thisSubmissionSet)
        {
            sub.setIsSubmissionForGrading(sub == latestSubmission);
        }
    }


    // ----------------------------------------------------------
    protected boolean shouldMigratePartnerLink()
    {
        // TODO: Fix the performance problems with auto-migration
        // this method is temporarily disabled until the auto-migration
        // performance problems can be worked out.

//        return (partnerLink() && primarySubmission() == null)
//            || (!partnerLink()
//                && result() != null
//                && result().submissions().count() > 1
//                && partneredSubmissions().count() == 0);
        return false;
    }


    // ----------------------------------------------------------
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
        if (!((partnerLink() && primarySubmission() == null)
             || (!partnerLink()
                 && result() != null
                 && result().submissions().count() > 1
                 && partneredSubmissions().count() == 0)))
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
        // TODO replace this code with a fetch specification when the
        // isSubmissionForGrading property is reified in the database.

        NSArray<Submission> subs = allSubmissions();

        for (Submission sub : subs)
        {
            if (sub.isSubmissionForGrading())
            {
                return sub;
            }
        }

        return null;
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
            }
            else if (sub.submitNumber() > number)
            {
                index--;
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
        if (result() == null)
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
        if (result() == null)
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
        if (result() == null)
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
            SubmissionResult thisResult = sub.result();
            if (thisResult != null)
            {
                double score = thisResult.correctnessScore();
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
            SubmissionResult thisResult = sub.result();
            if (thisResult != null)
            {
                double score = thisResult.toolScore();
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
            SubmissionResult thisResult = sub.result();
            if (thisResult != null)
            {
                double score = thisResult.automatedScore();
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

        properties.addPropertiesFromDictionaryIfNotDefined(
                ((Application) Application.application())
                    .subsystemManager().pluginProperties());
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
                    "Path must not include parent directory or root directory" +
                    "components");
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


    //~ Instance/static variables .............................................

    private int aoSubmissionsCountCache;
    private NSArray<Submission> allSubmissionsCache;

    private String cachedPermalink;
    private String subdirToDelete;

    private static final NSArray<Submission> NO_SUBMISSIONS =
        new NSArray<Submission>();

    static Logger log = Logger.getLogger( Submission.class );
}
