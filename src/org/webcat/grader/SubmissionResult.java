/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2011 Virginia Tech
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

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.io.File;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.woextensions.MigratingEditingContext;

// -------------------------------------------------------------------------
/**
 *  Represents the results for a student submission.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class SubmissionResult
    extends _SubmissionResult
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new SubmissionResult object.
     */
    public SubmissionResult()
    {
        super();
    }


    //~ KVC Attributes (must be public) .......................................

    public static final String SUBMISSION_ASSIGNMENT_OFFERING_KEY =
        SUBMISSIONS_KEY + "." + Submission.ASSIGNMENT_OFFERING_KEY;
    public static final String SUBMISSION_USER_KEY =
        SUBMISSIONS_KEY + "." + Submission.USER_KEY;
    public static final String SUBMISSION_SUBMIT_TIME_KEY =
        SUBMISSIONS_KEY + "." + Submission.SUBMIT_TIME_KEY;
    public static final byte FORMAT_HTML = 0;
    public static final byte FORMAT_TEXT = 1;
    public static final NSArray<Byte> formats = new NSArray<Byte>(
        new Byte[] { new Byte( FORMAT_HTML ), new Byte( FORMAT_TEXT ) });
    public static final NSArray<String> formatStrings = new NSArray<String>(
        new String[] { "HTML", "Plain Text" });


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Retrieve the primary submission associated with this result.
     * The primary submission is the one associated with the student
     * who actually made the submission, as opposed to one of the
     * partners associated with this submission.
     * @return The submission from the primary submitter.
     */
    public Submission submission()
    {
        Submission result = null;
        NSArray<Submission> mySubmissions = submissions();
        if (mySubmissions != null && mySubmissions.count() > 0)
        {
            result = mySubmissions.objectAtIndex(0);
            Submission primary = result.primarySubmission();

            if (primary != null)
            {
                result = primary;
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrueve the submission associated with this result for the
     * given user.
     * @param partner The user whose submission should be retrieved--either
     *                the primary submitter or one of the partners.
     * @return The submission associated with the given user (partner) and
     *         this result, or the primary submission if this user does
     *         not have any submission for this result.
     */
    public Submission submissionFor(User partner)
    {
        for (Submission sub : submissions())
        {
            if (sub.user() == partner)
            {
                return sub;
            }
        }
        return submission();
    }


    // ----------------------------------------------------------
    /**
     * Computes the early bonus for this submission.  The bonus
     * is a positive amount added to the raw score.
     *
     * @return the bonus amount, or 0.0 if none
     */
    public double earlyBonus()
    {
        double earlyBonus = 0.0;
        Submission submission = submission();
        long submitTime = submission.submitTime().getTime();
        long dueTime = submission.assignmentOffering().dueDate().getTime();
        SubmissionProfile profile =
            submission.assignmentOffering().assignment().submissionProfile();

        if ( profile.awardEarlyBonus()
             && dueTime > submitTime
             && profile.earlyBonusUnitTimeRaw() != null)
        {
            // Early bonus
            //
            long  earlyBonusUnitTime = profile.earlyBonusUnitTime();
            long  earlyTime  = dueTime - submitTime;
            float earlyUnits = earlyTime / earlyBonusUnitTime;
            earlyBonus = earlyUnits * profile.earlyBonusUnitPts();
            if ( profile.earlyBonusMaxPtsRaw() != null
                 && earlyBonus > profile.earlyBonusMaxPts() )
            {
                earlyBonus = profile.earlyBonusMaxPts();
            }
        }
        return earlyBonus;
    }


    // ----------------------------------------------------------
    /**
     * Computes the late penalty for this submission.  The penalty
     * is a positive amount subtracted from the raw score.
     *
     * @return the penalty amount, or 0.0 if none
     */
    public double latePenalty()
    {
        double latePenalty = 0.0;
        Submission submission = submission();
        long submitTime = submission.submitTime().getTime();
        long dueTime = submission.assignmentOffering().dueDate().getTime();
        SubmissionProfile profile =
            submission.assignmentOffering().assignment().submissionProfile();

        if ( profile.deductLatePenalty()
             && dueTime < submitTime
             && profile.latePenaltyUnitTimeRaw() != null)
        {
            // Late penalty
            //
            long latePenaltyUnitTime = profile.latePenaltyUnitTime();
            long lateTime  = submitTime - dueTime;
            long lateUnits = (long)java.lang.Math.ceil(
                ( (double)lateTime ) / (double)latePenaltyUnitTime );
            latePenalty = lateUnits * profile.latePenaltyUnitPts();
            if ( profile.latePenaltyMaxPtsRaw() != null
                 && latePenalty > profile.latePenaltyMaxPts() )
            {
                latePenalty = profile.latePenaltyMaxPts();
            }
        }
        return latePenalty;

    }


    // ----------------------------------------------------------
    /**
     * Computes the combined bonus and/or late penalty for this
     * submission, if any.
     *
     * @return the bonus/penalty adjustment amount
     */
    public double scoreAdjustment()
    {
        return earlyBonus() - latePenalty();
    }


    // ----------------------------------------------------------
    /**
     * Computes the raw score for this submission, viewable by students.
     * The raw score is the correctnessScore() plus the toolScore() plus
     * (if grading results are viewable by students) the taScore().
     *
     * @return the raw score
     */
    public double rawScoreForStudent()
    {
        double result = correctnessScore() + toolScore();
        if (status() == Status.CHECK)
        {
            result += taScore();
        }
        return ( result >= 0.0 ) ? result : 0.0;
    }


    // ----------------------------------------------------------
    /**
     * Computes the raw score for this submission, viewable by staff.
     * The raw score is the correctnessScore() plus the toolScore() plus
     * the taScore().
     *
     * @return the raw score
     */
    public double rawScore()
    {
        double result = correctnessScore() + toolScore() + taScore();
        return ( result >= 0.0 ) ? result : 0.0;
    }


    // ----------------------------------------------------------
    /**
     * Computes the raw score for this submission, as viewable by the
     * given user (either course staff or a student).
     *
     * @param user the user
     * @return the final score
     */
    public double rawScoreVisibleTo(User user)
    {
        if (user.hasAdminPrivileges() ||
            submission().assignmentOffering().courseOffering().isStaff(user))
        {
            return rawScore();
        }
        else
        {
            return rawScoreForStudent();
        }
    }


    // ----------------------------------------------------------
    /**
     * Computes the final score for this submission, viewable by students.
     * The final score is the rawScore() plus the earlyBonus() minus the
     * latePenalty().
     *
     * @return the final score
     */
    public double finalScoreForStudent()
    {
        double result = rawScoreForStudent() + earlyBonus() - latePenalty();
        return ( result >= 0.0 ) ? result : 0.0;
    }


    // ----------------------------------------------------------
    /**
     * Computes the final score for this submission, viewable by staff.
     * The final score is the rawScoreForStaff() plus the earlyBonus()
     * minus the latePenalty().
     *
     * @return the final score
     */
    public double finalScore()
    {
        double result = rawScore() + earlyBonus() - latePenalty();
        return ( result >= 0.0 ) ? result : 0.0;
    }


    // ----------------------------------------------------------
    /**
     * Computes the final score for this submission, viewable by the
     * given user (either course staff or a student).
     *
     * @param user the user
     * @return the final score
     */
    public double finalScoreVisibleTo(User user)
    {
        if (user.hasAdminPrivileges()
            || submission().assignmentOffering().courseOffering().isStaff(user))
        {
            return finalScore();
        }
        else
        {
            return finalScoreForStudent();
        }
    }


    // ----------------------------------------------------------
    /**
     * Check whether manual grading has been completed on this
     * submission.
     *
     * @return true if TA markup by hand has been completed
     * @deprecated Resurrected for old reports, but should not be used by
     *             any new code.
     */
    public boolean taGradingFinished()
    {
        return taScoreRaw() != null;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the "inline report" file as a File object.
     * @return the file for this submission
     */
    public File resultFile()
    {
        return new File( submission().resultDirName(), resultFileName() );
    }
    // TODO: should this operation (and its relatives) be in the Submission
    // class instead of in this class?


    // ----------------------------------------------------------
    /**
     * Retrieve the base file name for the "inline report".
     * @return the base file name
     */
    public static String resultFileName()
    {
        return "GraderReport.html";
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the staff-directed "inline report" file as a File object.
     * @return the file for this submission
     */
    public File staffResultFile()
    {
        return new File( submission().resultDirName(), staffResultFileName() );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the base file name for the staff-directed "inline report".
     * @return the base file name
     */
    public static String staffResultFileName()
    {
        return "StaffGraderReport.html";
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the properties file as a File object.
     * @return the file for this submission
     */
    public File propertiesFile()
    {
        if ( propertiesFile == null )
        {
            propertiesFile =
                new File( submission().resultDirName(), propertiesFileName() );
        }
        return propertiesFile;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the base file name for the result properties file.
     * @return the base file name
     */
    public static String propertiesFileName()
    {
        return "grading.properties";
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the properties object for this submission result.
     * @return the properties object attached to the properties file
     */
    public WCProperties properties()
    {
        if ( properties == null )
        {
            properties = new WCProperties(
                submission().resultDirName() + "/" + propertiesFileName(),
                null );
        }
        return properties;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the "inline summary"  file as a File object.
     * @return the file for this submission
     */
    public File summaryFile()
    {
        return new File( submission().resultDirName(), summaryFileName() );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the base file name for the "inline summary".
     * @return the base file name
     */
    public String summaryFileName()
    {
        return "GraderSummary.html";
    }


    // ----------------------------------------------------------
    public String scoreModifiers()
    {
        String result = null;
        double rawScore = finalScore();
        double earlyBonus = earlyBonus();
        if ( earlyBonus > 0.0 )
        {
            rawScore -= earlyBonus;
            result = " + " + earlyBonus + " early bonus";
        }
        double latePenalty = latePenalty();
        if ( latePenalty < 0.0 )
        {
            rawScore -= latePenalty;
            result = (result == null ? "" : result)
                + " - " + (-latePenalty) + " late penalty";
        }
        if ( result != null )
        {
            result = "" + rawScore + result + " = ";
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Get the corresponding icon URL for this file's grading status.
     *
     * @return The image URL as a string
     */
    public String statusURL()
    {
        return Status.statusURL( status() );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the score for this result that is used in graphs.  The score
     * for graphing includes the raw correctness score plus the raw static
     * analysis score, without any late penalty or TA manual grading
     * included.
     * @return the score
     */
    public double automatedScore()
    {
        double result = correctnessScore() + toolScore();
        if ( log.isDebugEnabled() )
        {
            log.debug( "automatedScore() = " + result );
        }
        return ( result >= 0.0 ) ? result : 0.0;
    }


    // ----------------------------------------------------------
    protected boolean shouldMigrateIsMostRecent()
    {
        return (isMostRecentRaw() == null);
    }


    // ----------------------------------------------------------
    protected void migrateIsMostRecent(MigratingEditingContext mec)
    {
        setAsMostRecentIfNecessary(mec);
    }


    // ----------------------------------------------------------
    private void setAsMostRecentIfNecessary(EOEditingContext mec)
    {
        if ( log.isDebugEnabled() )
        {
            NSArray<SubmissionResult> subs = resultsForAssignmentAndUser(
                mec, submission().assignmentOffering(), submission().user() );
            for ( int i = 0; i < subs.count(); i++ )
            {
                SubmissionResult sr = subs.objectAtIndex( i );
                log.debug( "sub " + i + ": " + sr.submission().submitNumber()
                           + " " + sr.submission().submitTime() );
            }
        }
        SubmissionResult newest = null;
        NSArray<SubmissionResult> subs = mostRecentResultsForAssignmentAndUser(
            mec, submission().assignmentOffering(), submission().user() );
        if ( subs.count() > 0 )
        {
            newest = subs.objectAtIndex(0);
            if ( !newest.isMostRecent() )
            {
                log.warn(
                    "most recent submission is unmarked: "
                    + newest.submission().user().userName()
                    + " #" + newest.submission().submitNumber() );
            }
        }
        for ( int i = 1; i < subs.count(); i++ )
        {
            SubmissionResult thisSub = subs.objectAtIndex( i );
            log.warn(
                "multiple submissions marked most recent: "
                + thisSub.submission().user().userName()
                + " #" + thisSub.submission().submitNumber() );
            thisSub.setIsMostRecent( false );
        }
        if ( newest != null )
        {
            if ( newest.submission().submitNumber() <
                 submission().submitNumber() )
            {
                newest.setIsMostRecent( false );
                setIsMostRecent( true );
            }
        }
        else // ( newest == null )
        {
            setIsMostRecent( true );
        }
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>isMostRecent</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setIsMostRecent(boolean value)
    {
        boolean wasMostRecent = isMostRecent();
        if (log.isDebugEnabled())
        {
            log.debug("setIsMostRecent(" + value + ") called");
            log.debug("   submission = " + submission());
            log.debug("   wasMostRecent = " + wasMostRecent);
        }

        User user = submission().user();

        if (!submission().assignmentOffering().courseOffering().isStaff(user))
        {
            if (wasMostRecent && !value)
            {
                submission().assignmentOffering().graphSummary()
                    .removeSubmission(automatedScore());
            }
            else if (!wasMostRecent && value)
            {
                submission().assignmentOffering().graphSummary()
                    .addSubmission(automatedScore());
            }
        }

        super.setIsMostRecent(value);
    }


    // ----------------------------------------------------------
    /**
     * Determine whether or not this submission is more recent than the
     * one currently marked as most recent, and then set the flag and
     * update any derived information if it is.  Any changes are committed
     * using this object's editing context.
     */
    public void setAsMostRecentIfNecessary()
    {
        if ( submission() == null )
        {
            return;
        }

        EOEditingContext ec = editingContext();
        setAsMostRecentIfNecessary(ec);
        ec.saveChanges();
    }


    // ----------------------------------------------------------
    public boolean hasCoverageData()
    {
        boolean result = false;
        for (SubmissionFileStats sfs : submissionFileStats())
        {
            if (sfs.elementsRaw() != null)
            {
                result = true;
                break;
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    public void addCommentByLineFor(User commenter, String priorComments)
    {
        String newComments = comments();
        if (newComments != null
            && (newComments.trim().equals("<br />") || newComments.equals("")))
        {
            setComments(null);
            newComments = null;
        }
        if (status() == Status.TO_DO && newComments != null)
        {
            setStatus(Status.UNFINISHED);
        }
        if (newComments != null
            && newComments.indexOf("<") < 0
            && newComments.indexOf(">") < 0)
        {
            setCommentFormat(SubmissionResult.FORMAT_TEXT);
        }
        if (newComments != null && !newComments.equals(priorComments))
        {
            // update author info:
            String byLine = "-- last updated by " + commenter.name();
            if (commentFormat() == SubmissionResult.FORMAT_HTML)
            {
                byLine = "<p><span style=\"font-size:smaller\"><i>"
                    + byLine + "</i></span></p>";
            }
            if (log.isDebugEnabled())
            {
                log.debug("new comments ='" + newComments + "'");
                log.debug("byline ='" + byLine + "'");
            }
            if (!newComments.trim().endsWith(byLine))
            {
                log.debug("byLine not found");
                if (commentFormat() == SubmissionResult.FORMAT_TEXT)
                {
                    byLine = "\n" + byLine + "\n";
                }
                if (!(newComments.endsWith( "\n")
                      || newComments.endsWith("\r")))
                {
                    byLine = "\n" + byLine;
                }
                setComments(newComments + byLine);
            }
        }
    }


    // ----------------------------------------------------------
    @Override
    public void mightDelete()
    {
        log.debug("mightDelete()");
        Submission sub = submission();
        if (sub != null)
        {
            subdirToDelete = sub.resultDirName();
        }
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

    private File propertiesFile;
    private WCProperties properties;
    private String subdirToDelete;
    static Logger log = Logger.getLogger( SubmissionResult.class );
}
