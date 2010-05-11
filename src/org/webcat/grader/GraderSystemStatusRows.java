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

package org.webcat.grader;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.foundation.*;
import er.extensions.eof.ERXConstant;
import org.apache.log4j.*;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 *  Generates the grader subsystem's rows in the system status block.
 *
 *  @author  Stephen Edwards
 *  @version $Id$
 */
public class GraderSystemStatusRows
    extends WOComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderSystemStatusRows object.
     *
     * @param context The page's context
     */
    public GraderSystemStatusRows( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public int     index;


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
        queuedJobs = -1;
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public void awake()
    {
        super.awake();
        grader = Grader.getInstance();
    }


    // ----------------------------------------------------------
    /**
     * Access the count of suspended assignments.
     * @return the number of assignments that have grading suspended
     */
    public int haltedCount()
    {
        NSArray haltedAssignments = null;
        try
        {
            haltedAssignments = EOUtilities.objectsMatchingValues(
                ( (Session)session() ).sessionContext(),
                AssignmentOffering.ENTITY_NAME,
                new NSDictionary(
                    new Object[] { ERXConstant.integerForInt( 1 ) },
                    new Object[] { AssignmentOffering.GRADING_SUSPENDED_KEY }
                )
            );
        }
        catch ( Exception e )
        {
            log.debug( "Retrying halted fetch" );
            haltedAssignments = EOUtilities.objectsMatchingValues(
                            ( (Session)session() ).sessionContext(),
                            AssignmentOffering.ENTITY_NAME,
                            new NSDictionary(
                                new Object[] { ERXConstant.integerForInt( 1 ) },
                                new Object[] { AssignmentOffering.GRADING_SUSPENDED_KEY }
                            )
                        );
        }
        return haltedAssignments == null
            ? 0 : haltedAssignments.count();
    }


    // ----------------------------------------------------------
    /**
     * Returns the number of jobs queued for grade processing
     * @return the number of jobs queued
     */
    public int queuedJobCount()
    {
        if ( queuedJobs < 0 )
        {
            NSArray jobs = null;
            try
            {
                jobs = EOUtilities.objectsMatchingValues(
                    ( (Session)session() ).sessionContext(),
                    EnqueuedJob.ENTITY_NAME,
                    new NSDictionary(
                        new Object[] { ERXConstant.integerForInt( 0 )  },
                        new Object[] { EnqueuedJob.PAUSED_KEY }
                    )
                );
            }
            catch ( Exception e )
            {
                log.debug( "Retrying queued job fetch" );
                jobs = EOUtilities.objectsMatchingValues(
                    ( (Session)session() ).sessionContext(),
                    EnqueuedJob.ENTITY_NAME,
                    new NSDictionary(
                        new Object[] { ERXConstant.integerForInt( 0 )  },
                        new Object[] { EnqueuedJob.PAUSED_KEY }
                    )
                );
            }
            queuedJobs = ( jobs == null ) ? 0 : jobs.count();
        }
        return queuedJobs;
    }


    // ----------------------------------------------------------
    /**
     * Returns the number of jobs stalled in the queue
     * @return the number of jobs stalled
     */
    public int stalledJobCount()
    {
        NSArray jobs = null;
        try
        {
            jobs = EOUtilities.objectsMatchingValues(
                ( (Session)session() ).sessionContext(),
                EnqueuedJob.ENTITY_NAME,
                new NSDictionary(
                    new Object[] { ERXConstant.integerForInt( 1 )  },
                    new Object[] { EnqueuedJob.PAUSED_KEY }
                )
            );
        }
        catch ( Exception e )
        {
            log.debug( "Retrying queued job fetch" );
            jobs = EOUtilities.objectsMatchingValues(
                ( (Session)session() ).sessionContext(),
                EnqueuedJob.ENTITY_NAME,
                new NSDictionary(
                    new Object[] { ERXConstant.integerForInt( 1 )  },
                    new Object[] { EnqueuedJob.PAUSED_KEY }
                )
            );
        }
        return jobs == null ? 0 : jobs.count();
    }


    // ----------------------------------------------------------
    /**
     * Access the number of processed submissions
     * @return the number of jobs processed/graded since last restart
     */
    public int processedJobCount()
    {
        return ( grader == null )
            ? -1
            : grader.processedJobCount();
    }


    // ----------------------------------------------------------
    /**
     * Access estimatd queue wait.
     * @return expected time to clear queue
     */
    public NSTimestamp estimatedWait()
    {
        NSTimestamp result =  new NSTimestamp(
            ( grader == null )
                ? 0
                : ( grader.estimatedJobTime() * ( queuedJobCount() + 1 ) ) );
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Compute the appropriate format string for displaying the average
     * job time.
     * @return the date format string for the corresponding time value
     */
    public String estimatedWaitFormat()
    {
        return FinalReportPage.formatForSmallTime(
            ( grader == null )
                ? 0
                : ( grader.estimatedJobTime() * ( queuedJobCount() + 1 ) ) );
    }


    // ----------------------------------------------------------
    /**
     * Access average time per job.
     * @return average time taken to process one job
     */
    public NSTimestamp averageTimePerJob()
    {
        NSTimestamp result =  new NSTimestamp( ( grader == null )
                                               ? 0
                                               : grader.estimatedJobTime() );
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Compute the appropriate format string for displaying the average
     * job time.
     * @return the date format string for the corresponding time value
     */
    public String averageTimePerJobFormat()
    {
        return FinalReportPage.formatForSmallTime(
                        ( grader == null )
                            ? 0
                            : grader.estimatedJobTime() );
    }


    // ----------------------------------------------------------
    /**
     * Access the time used on the most recently completed submission.
     * @return the time taken to process the most recent job
     */
    public NSTimestamp mostRecentJobWait()
    {
        NSTimestamp result =  new NSTimestamp( ( grader == null )
                                               ? 0
                                               : grader.mostRecentJobWait() );
        // log.debug( "mostRecentJobWait() = " + result );
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Compute the appropriate format string for displaying the most
     * recent job wait, depending on how large the wait was.
     * @return the date format string for the corresponding time value
     */
    public String mostRecentJobWaitFormat()
    {
        return FinalReportPage.formatForSmallTime(
                        ( grader == null )
                            ? 0
                            : grader.mostRecentJobWait() );
    }


    //~ Instance/static variables .............................................

    private Grader grader;
    private int queuedJobs;

    static Logger log = Logger.getLogger( GraderSystemStatusRows.class );
}