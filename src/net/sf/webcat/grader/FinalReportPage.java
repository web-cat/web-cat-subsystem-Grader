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
import java.io.*;

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class presents the final grading report on a student
 * submission.  If the submission has not yet completed grading,
 * then the page presents a message indicating that grading is
 * in-process and automatically reloads after 10 seconds.  If some
 * error happened during grading, then the student is informed.
 * Otherwise, the final grading report is presented.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class FinalReportPage
    extends GraderSubmissionComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public FinalReportPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public Submission          submission;
    public SubmissionResult    result;
    public WODisplayGroup      statsDisplayGroup;
    // For iterating over display group
    public SubmissionFileStats stats;
    public int                 index;

    public boolean submissionChosen = false;
    /** The job's isFinished attribute is tested once and stored here.
        This prevents race conditions arising from testing that field
        multiple times in the body of the page, or between generating a
        value for areInlineReports and later testing the job's isFinished
        attribute separately. */
    public boolean reportIsReady;
    /** The associated refresh interval for this page */
    public int refreshTimeout = 15;
    /** The report object */
    public ResultFile report;
    public ResultFile selectedReport = null;
    /** Array of all the downloadable report files */
    public NSArray reportArray;

    public boolean showReturnToGrading = false;


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
        jobData = null;
        if ( submission == null && prefs() != null )
        {
            submission = prefs().submission();
        }
        if ( submission != null )
        {
            submissionChosen = true;
            result = submission.result();
            reportIsReady   = ( result != null );
            if ( reportIsReady )
            {
                statsDisplayGroup.setObjectArray( result.submissionFileStats() );

                NSMutableArray fileList = result.resultFiles().mutableClone();
                ResultFile userSubmission = new ResultFile();
                userSubmission.setFileName(
                    "../" + submission.fileName() );
                userSubmission.setMimeType( "application/octet-stream" );
                userSubmission.setLabel( "Your original submission" );
                fileList.addObject( userSubmission );
                reportArray = fileList;
            }
        }
        showCoverageData = null;
        super.appendToResponse( response, context );
        log.debug( "ending appendToResponse()" );
    }


    // ----------------------------------------------------------
    /**
     * Returns the file delivery page with the non inline file.
     * @return the file delivery page
     */
    public WOComponent fileDeliveryAction()
    {
        if (selectedReport == null)
        {
            error("Please select a file to download first.");
        }
        DeliverFile download =
            (DeliverFile)pageWithName( DeliverFile.class.getName() );
        download.setFileName(
            new File( submission.resultDirName(),
                      selectedReport.fileName() ) );
        download.setContentType( selectedReport.mimeType() );
        download.setStartDownload( true );
        return download;
    }


    // ----------------------------------------------------------
    /**
     * Returns null to force a reload of the current page.
     * @return always null, to refresh the current page
     */
    public WOComponent refreshAction()
    {
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Jump to the start page for selecting past results.
     * @return the new page
     */
    public WOComponent pastResults()
    {
        return pageWithName(
            wcSession().tabs.selectById( "PastResults" ).pageName() );
    }


    // ----------------------------------------------------------
    /**
     * Returns null to force a reload of the current page.
     * @return always null, to refresh the current page
     */
    public boolean gradingPaused()
    {
        EnqueuedJob job = submission.enqueuedJob();
        return ( job != null  &&  job.paused() );
    }


    // ----------------------------------------------------------
    public boolean hasTAComments()
    {
        return result.status() == Status.CHECK
            && result.comments() != null
            && !result.comments().equals( "" );
    }


    // ----------------------------------------------------------
    public boolean hasNonZeroScore()
    {
    return result.finalScore() > 0.0;
    }


    // ----------------------------------------------------------
    public WOComponent fileStatsDetails()
    {
        log.debug( "fileStatsDetails()" );
        prefs().setSubmissionFileStatsRelationship( stats );
        WCComponent statsPage = (WCComponent)pageWithName(
                        SubmissionFileDetailsPage.class.getName() );
        statsPage.nextPage = this;
        return statsPage;
    }


    // ----------------------------------------------------------
    public WOComponent fullPrintableReport()
    {
        FullPrintableReport fullReport = (FullPrintableReport)
            pageWithName( FullPrintableReport.class.getName() );
        fullReport.result = result;
        fullReport.nextPage = this;
        return fullReport;
    }


    // ----------------------------------------------------------
    public static String meter( double fraction )
    {
        if ( blankGifUrl == null )
        {
            blankGifUrl = WCResourceManager.resourceURLFor(
                "images/blank.gif", "Core", null, null );
        }
        StringBuffer buffer = new StringBuffer( 250 );
        int covered = (int)( 200.0 * fraction + 0.5 );
        int uncovered = 200 - covered;
        buffer.append( "<table class=\"percentbar\"><tr><td " );
        if ( covered < 1 )
        {
            // Completely uncovered
            buffer.append( "class=\"minus\"><img src=\"" );
            buffer.append( blankGifUrl );
            buffer.append( "\" width=\"200\" height=\"12\" alt=\"nothing covered\">" );
        }
        else if ( uncovered > 0 )
        {
            // Partially covered
            buffer.append( "class=\"plus\"><img src=\"" );
            buffer.append( blankGifUrl );
            buffer.append( "\" width=\"" );
            buffer.append( covered );
            buffer.append( "\" height=\"12\" alt=\"" );
            buffer.append( (int)( 100.0 * fraction + 0.5 ) );
            buffer.append( " covered\"></td><td class=\"minus\"><img src=\"" );
            buffer.append( blankGifUrl );
            buffer.append( "\" width=\"" );
            buffer.append( uncovered );
            buffer.append( "\" height=\"12\" alt=\"" );
            buffer.append( 100 - (int)( 100.0 * fraction + 0.5 ) );
            buffer.append( " uncovered\">" );
        }
        else
        {
            // Completely covered
            buffer.append( "class=\"plus\"><img src=\"" );
            buffer.append( blankGifUrl );
            buffer.append( "\" width=\"200\" height=\"12\" alt=\"fully covered\">" );
        }
        buffer.append( "</td></tr></table>" );
        return buffer.toString();
    }


    // ----------------------------------------------------------
    public String coverageMeter()
    {
        return meter( ( (double)stats.elementsCovered() ) /
                      ( (double)stats.elements() ) );
    }


    // ----------------------------------------------------------
    public int queuedJobCount()
    {
        ensureJobDataIsInitialized();
        return jobData.queueSize;
    }


    // ----------------------------------------------------------
    public int queuePosition()
    {
        ensureJobDataIsInitialized();
        return jobData.queuePosition + 1;
    }


    // ----------------------------------------------------------
    /**
     * Returns the estimated time needed to complete processing this job.
     * @return the most recent job wait
     */
    public NSTimestamp estimatedWait()
    {
        ensureJobDataIsInitialized();
        return new NSTimestamp( jobData.estimatedWait );
    }


    // ----------------------------------------------------------
    /**
     * Returns the time taken to process the most recent job.
     * @return the most recent job wait
     */
    public NSTimestamp mostRecentJobWait()
    {
        ensureJobDataIsInitialized();
        return new NSTimestamp( jobData.mostRecentWait );
    }


    // ----------------------------------------------------------
    /**
     * Returns the date format string for the corresponding time value
     * @param timeDelta the time to format
     * @return the time format to use
     */
    public static String formatForSmallTime( long timeDelta )
    {
        String format = "%j days, %H:%M:%S";
        final int minute = 60 * 1000;
        final int hour   = 60 * minute;
        final int day    = 24 * hour;
        if ( timeDelta < minute )
        {
            format = "%S seconds";
        }
        else if ( timeDelta < hour )
        {
            format = "%M:%S minutes";
        }
        else if ( timeDelta < day )
        {
            format = "%H:%M:%S hours";
        }
        return format;
    }


    // ----------------------------------------------------------
    /**
     * Returns the date format string for the corresponding time value
     * @return the time format for the estimated job wait
     */
    public String estimatedWaitFormat()
    {
        ensureJobDataIsInitialized();
        return formatForSmallTime( jobData.estimatedWait );
    }


    // ----------------------------------------------------------
    /**
     * Returns the date format string for the corresponding time value
     * @return the time format for the most recent job wait
     */
    public String mostRecentJobWaitFormat()
    {
        ensureJobDataIsInitialized();
        return formatForSmallTime( jobData.mostRecentWait );
    }


    // ----------------------------------------------------------
    public Boolean showCoverageData()
    {
        if ( showCoverageData == null )
        {
            showCoverageData = Boolean.valueOf( result.hasCoverageData() );
        }
        return showCoverageData;
    }


    // ----------------------------------------------------------
    /**
     * Determine if this assignment is just for collecting submissions,
     * without any automated processing steps.
     *
     * @return true if the submission is just being collected
     */
    public boolean justCollecting()
    {
        NSArray steps =
            result.submission().assignmentOffering().assignment().steps();
        return !result.summaryFile().exists()
            && !result.resultFile().exists()
            && (steps == null || steps.count() == 0);
    }


    // ----------------------------------------------------------
    /**
     * Determine if user can submit to this assignment.
     *
     * @return true if the user can make another submission
     */
    public boolean canSubmitAgain()
    {
        boolean answer = false;
        if ( result != null && !showReturnToGrading )
        {
//            answer = result.submission().assignmentOffering().userCanSubmit(
//                wcSession().localUser() );

            // This is all debugging code to figure out why we occasionally
            // get NPEs on the original line commented out above.
            Submission sub = result.submission();
            if (sub == null)
            {
                log.error("null submission for result found!");
                try
                {
                    log.error("result = " + result);
                }
                catch (Exception e)
                {
                    log.error("unable to print result details:", e);
                }
            }
            else
            {
                AssignmentOffering ao = sub.assignmentOffering();
                if (ao == null)
                {
                    log.error("null assignment offering for submission found!");
                    try
                    {
                        log.error("result = " + result);
                        log.error("submission = " + sub);
                    }
                    catch (Exception e)
                    {
                        log.error(
                            "unable to print submission/result details:", e);
                    }
                }
                else
                {
                    answer = ao.userCanSubmit( user() );
                }
            }
        }
        return answer;
    }


    // ----------------------------------------------------------
    /**
     * An action to go to the submission page for this assignment.
     *
     * @return the submission page for this assignment
     */
    public WOComponent submitAgain()
    {
        return pageWithName(
            wcSession().tabs.selectById( "UploadSubmission" ).pageName() );
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        return null;
    }


    // ----------------------------------------------------------
    public Boolean showAutoGradedComments()
    {
        if (showAutoGradedComments == null)
        {
            if (submission.assignmentOffering().assignment()
                    .submissionProfile().toolPoints() > 0.0)
            {
                showAutoGradedComments = Boolean.TRUE;
            }
            else
            {
                showAutoGradedComments = Boolean.FALSE;
                for (int i = 0; i < result.submissionFileStats().count(); i++)
                {
                    SubmissionFileStats thisStats = (SubmissionFileStats)
                        result.submissionFileStats().objectAtIndex(i);
                    if (thisStats.remarks() > 0)
                    {
                        showAutoGradedComments = Boolean.TRUE;
                        break;
                    }
                }
            }
        }
        return showAutoGradedComments;
    }


    // ----------------------------------------------------------
    static private class JobData
    {
        public NSArray jobs;
        public int queueSize;
        public int queuePosition;
        long mostRecentWait;
        long estimatedWait;
    }


    // ----------------------------------------------------------
    private void ensureJobDataIsInitialized()
    {
        if ( jobData == null )
        {
            jobData = new JobData();
            NSMutableArray qualifiers = new NSMutableArray();
            qualifiers.addObject( new EOKeyValueQualifier(
                            EnqueuedJob.DISCARDED_KEY,
                            EOQualifier.QualifierOperatorEqual,
                            ERXConstant.integerForInt( 0 )
            ) );
            qualifiers.addObject( new EOKeyValueQualifier(
                            EnqueuedJob.PAUSED_KEY,
                            EOQualifier.QualifierOperatorEqual,
                            ERXConstant.integerForInt( 0 )
            ) );
            qualifiers.addObject( new EOKeyValueQualifier(
                            EnqueuedJob.REGRADING_KEY,
                            EOQualifier.QualifierOperatorEqual,
                            ERXConstant.integerForInt( 0 )
            ) );
            EOFetchSpecification fetchSpec =
                new EOFetchSpecification(
                        EnqueuedJob.ENTITY_NAME,
                        new EOAndQualifier( qualifiers ),
                        new NSArray( new Object[]{
                                new EOSortOrdering(
                                        EnqueuedJob.SUBMIT_TIME_KEY,
                                        EOSortOrdering.CompareAscending
                                    )
                            } )
                    );
            jobData.jobs =
                localContext().objectsWithFetchSpecification(
                    fetchSpec
                );
            jobData.queueSize = jobData.jobs.count();
            if ( oldQueuePos < 0
                 || oldQueuePos >= jobData.queueSize )
            {
                oldQueuePos = jobData.queueSize - 1;
            }
            jobData.queuePosition = jobData.queueSize;
            for ( int i = oldQueuePos; i >= 0; i-- )
            {
                if ( jobData.jobs.objectAtIndex( i )
                     == submission.enqueuedJob() )
                {
                    jobData.queuePosition = i;
                    break;
                }
            }
            oldQueuePos = jobData.queuePosition;
            if ( jobData.queuePosition == jobData.queueSize )
            {
                log.error( "cannot find job in queue for:"
                           + submission );
            }
            Grader grader = Grader.getInstance();
            jobData.mostRecentWait = grader.mostRecentJobWait();
            jobData.estimatedWait =
                grader.estimatedJobTime() * ( jobData.queuePosition + 1 );
        }
    }


    //~ Instance/static variables .............................................

    private JobData jobData;
    private int     oldQueuePos = -1;
    private Boolean showCoverageData;
    private Boolean showAutoGradedComments;

    private static String blankGifUrl;
    static Logger log = Logger.getLogger( FinalReportPage.class );
}
