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

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.*;
import java.io.File;

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 *  Represents the results for a student submission.
 *
 *  @author Stephen Edwards
 *  @version $Id$
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
    public static final NSArray formats = new NSArray( new Byte[] {
                    new Byte( FORMAT_HTML ), new Byte( FORMAT_TEXT )
                    });
    public static final NSArray formatStrings = new NSArray( new String[] {
                    "HTML", "Plain Text"
                    });


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public Submission submission()
    {
        Submission result = null;
        NSArray submissions = submissions();
        if ( submissions != null )
        {
            for ( int i = 0; i < submissions().count(); i++ )
            {
                Submission thisSubmission =
                    (Submission)submissions.objectAtIndex( i );
                if ( ! thisSubmission.partnerLink() )
                {
                    result = thisSubmission;
                    break;
                }
            }
            if ( result == null && submissions().count() > 0 )
            {
                Submission thisSubmission =
                    (Submission)submissions.objectAtIndex( 0 );
                thisSubmission.setPartnerLink( false );
                result = thisSubmission;
            }
        }
        return result;
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
        String result = null;
        Submission submission = submission();
        long submitTime = submission.submitTime().getTime();
        long dueTime = submission.assignmentOffering().dueDate().getTime();
        if ( dueTime >= submitTime )
        {
            // Early submission
            result =
                Submission.getStringTimeRepresentation( dueTime - submitTime )
                + " early";
        }
        else
        {
            // Late submission
            result =
                Submission.getStringTimeRepresentation( submitTime - dueTime )
                + " late";
        }
        return result;
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
             && dueTime > submitTime )
        {
            if ( profile.earlyBonusUnitTimeRaw() != null
                 && profile.earlyBonusUnitPtsRaw() != null )
            {
                // Early bonus
                //
                long  earlyBonusUnitTime = profile.earlyBonusUnitTime();
                long  earlyTime  = dueTime - submitTime;
                float earlyUnits = earlyTime / earlyBonusUnitTime;
                earlyBonus = earlyUnits * profile.earlyBonusUnitPts();
                if ( earlyBonus > profile.earlyBonusMaxPts() )
                {
                    earlyBonus = profile.earlyBonusMaxPts();
                }
                else
                {
                    log.warn( "null earlyBonusMaxPts() in '"
                              + profile.name()
                              + "', assessing "
                              + submission );
                }
            }
            else
            {
                if ( profile.earlyBonusUnitTimeRaw() == null )
                {
                    log.warn( "null earlyBonusUnitTime() in '"
                              + profile.name()
                              + "', assessing "
                              + submission );
                }
                if ( profile.earlyBonusUnitPtsRaw() == null )
                {
                    log.warn( "null earlyBonusUnitPts() in '"
                              + profile.name()
                              + "', assessing "
                              + submission );
                }
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

        if ( profile.deductLatePenalty() && dueTime < submitTime )
        {
            if ( profile.latePenaltyUnitTimeRaw() != null
                 && profile.latePenaltyUnitPtsRaw() != null )
            {
                // Late penalty
                //
                long latePenaltyUnitTime = profile.latePenaltyUnitTime();
                long lateTime  = submitTime - dueTime;
                long lateUnits = (long)java.lang.Math.ceil(
                        ( (double)lateTime ) / (double)latePenaltyUnitTime );
                latePenalty = lateUnits * profile.latePenaltyUnitPts();
                if ( latePenalty > profile.latePenaltyMaxPts() )
                {
                    latePenalty = profile.latePenaltyMaxPts();
                }
            }
            else
            {
                if ( profile.latePenaltyUnitTimeRaw() == null )
                {
                    log.warn( "null latePenaltyUnitTime() in '"
                              + profile.name()
                              + "', assessing "
                              + submission );
                }
                if ( profile.latePenaltyUnitPtsRaw() == null )
                {
                    log.warn( "null latePenaltyUnitPts() in '"
                              + profile.name()
                              + "', assessing "
                              + submission );
                }
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
     * Computes the final score for this submission.  The final
     * score is the rawScore() plus the earlyBonus() minus the
     * latePenalty().
     *
     * @return the final score
     */
    public double finalScore()
    {
        double result = correctnessScore() + toolScore() + taScore()
            + earlyBonus() - latePenalty();
        return ( result >= 0.0 ) ? result : 0.0;
    }


    // ----------------------------------------------------------
    /**
     * Check whether manual grading has been completed on this
     * submission.
     *
     * @return true if TA markup by hand has been completed
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
            result = " - " + latePenalty + " late penalty";
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
    public double graphableScore()
    {
        double result = correctnessScore() + toolScore();
        if ( log.isDebugEnabled() )
        {
            log.debug( "graphableScore() = " + result );
        }
        return ( result >= 0.0 ) ? result : 0.0;
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>isMostRecent</code>
     * property.
     * 
     * @param value The new value for this property
     */
    public void setIsMostRecent( boolean value )
    {
        boolean wasMostRecent = isMostRecent();
        if ( log.isDebugEnabled() )
        {
            log.debug( "setIsMostRecent(" + value + ") called" );
            log.debug( "   submission = " + submission() );
            log.debug( "   wasMostRecent = " + wasMostRecent );
        }
        if ( wasMostRecent && !value )
        {
            submission().assignmentOffering().graphSummary().removeSubmission(
                graphableScore() );
        }
        else if ( !wasMostRecent && value )
        {
            submission().assignmentOffering().graphSummary().addSubmission(
                graphableScore() );
        }
        super.setIsMostRecent( value );
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
        EOEditingContext ec = editingContext();
        if ( log.isDebugEnabled() )
        {
            NSArray subs = objectsForUser(
                ec, submission().assignmentOffering(), submission().user() );
            for ( int i = 0; i < subs.count(); i++ )
            {
                SubmissionResult sr =
                    (SubmissionResult)subs.objectAtIndex( i );
                log.debug( "sub " + i + ": " + sr.submission().submitNumber()
                           + " " + sr.submission().submitTime() );
            }
        }
        SubmissionResult newest = null;
        NSArray subs = objectsForMostRecentByDate(
            ec, submission().assignmentOffering(), submission().user() );
        if ( subs.count() > 0 )
        {
            newest = (SubmissionResult)subs.objectAtIndex( 0 );
            if ( !newest.isMostRecent() )
            {
                log.warn(
                    "most recent submission is unmarked: "
                    + newest.submission().user().userName()
                    + " #" + newest.submission().submitNumber() );
            }
        }
        subs = objectsForMostRecentSubmission(
            ec, submission().assignmentOffering(), submission().user() );
        for ( int i = 1; i < subs.count(); i++ )
        {
            SubmissionResult thisSub =
                (SubmissionResult)subs.objectAtIndex( i );
            log.warn(
                "multiple submissions marked most recent: "
                + thisSub.submission().user().userName()
                + " #" + thisSub.submission().submitNumber() );
            thisSub.setIsMostRecent( false );
        }
        if ( subs.count() > 0 )
        {
            SubmissionResult thisSub =
                (SubmissionResult)subs.objectAtIndex( 0 );
            if ( newest == null )
            {
                newest = thisSub;
            }
            else if ( newest != thisSub )
            {
                if ( newest.submission().submitNumber() >
                     thisSub.submission().submitNumber() )
                {
                    thisSub.setIsMostRecent( false );
                }
                else
                {
                    newest = thisSub;
                }
            }
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
        ec.saveChanges();
    }


// If you add instance variables to store property values you
// should add empty implementions of the Serialization methods
// to avoid unnecessary overhead (the properties will be
// serialized for you in the superclass).

//    // ----------------------------------------------------------
//    /**
//     * Serialize this object (an empty implementation, since the
//     * superclass handles this responsibility).
//     * @param out the stream to write to
//     */
//    private void writeObject( java.io.ObjectOutputStream out )
//        throws java.io.IOException
//    {
//    }
//
//
//    // ----------------------------------------------------------
//    /**
//     * Read in a serialized object (an empty implementation, since the
//     * superclass handles this responsibility).
//     * @param in the stream to read from
//     */
//    private void readObject( java.io.ObjectInputStream in )
//        throws java.io.IOException, java.lang.ClassNotFoundException
//    {
//    }


    //~ Instance/static variables .............................................

    private File propertiesFile;
    private WCProperties properties;
    static Logger log = Logger.getLogger( SubmissionResult.class );
}
