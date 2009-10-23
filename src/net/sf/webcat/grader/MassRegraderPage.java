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

import net.sf.webcat.core.Application;
import net.sf.webcat.core.Course;
import net.sf.webcat.core.CourseOffering;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXQ;
import er.extensions.foundation.ERXArrayUtilities;

//--------------------------------------------------------------------------
/**
 * Implements a simple interface for performing mass-regrades of submissions to
 * an assignment (typically for data collection with an updated grading
 * plug-in).
 * 
 * @author Tony Allevato
 * @version $Id$
 */
public class MassRegraderPage extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................
    
    // ----------------------------------------------------------
    public MassRegraderPage(WOContext context)
    {
        super(context);
    }
    

    //~ KVC attributes (must be public) .......................................

    public String qualifierString;
    public NSMutableDictionary<String, String> qualifierErrors;
    public int numberOfSubmissions;

    public EnqueuedJob job;
    public int index;
    
    public int cachedCountOfRegradingJobsInQueue = 0;
    public int cachedCountOfSuspendedJobsInQueue = 0;
    

    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public void appendToResponse(WOResponse response, WOContext context)
    {
        updateSubmissionCount();

        super.appendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public WOActionResults massRegrade()
    {
        EOQualifier q;
        qualifierErrors = null;

        boolean hasQualString = false;
        if (qualifierString != null && qualifierString.trim().length() > 0)
        {
            hasQualString = true;
        }

        if (!hasQualString &&
                (prefs().assignment() == null &&
                        prefs().assignmentOffering() == null))
        {
            return null;
        }

        NSArray<EOSortOrdering> sortOrderings = new NSArray<EOSortOrdering>(
                new EOSortOrdering[] {
                        new EOSortOrdering("user.userName",
                                EOSortOrdering.CompareCaseInsensitiveAscending),
                        new EOSortOrdering("submitNumber",
                                EOSortOrdering.CompareAscending)
                });

        if (hasQualString)
        {
            try
            {
                q = EOQualifier.qualifierWithQualifierFormat(
                        qualifierString, null);
            }
            catch (Exception e)
            {
                String msg = e.getMessage();
                qualifierErrors = new NSMutableDictionary<String, String>();
                qualifierErrors.setObjectForKey(msg, msg);
                q = null;
            }
        }
        else
        {
            if (prefs().assignmentOffering() != null)
            {
                q = ERXQ.is("assignmentOffering", prefs().assignmentOffering());
            }
            else
            {
                q = ERXQ.is("assignmentOffering.assignment",
                        prefs().assignment());
            }
        }

        if (q != null)
        {
            NSArray<Submission> submissions = null;

            try
            {
                EOFetchSpecification fspec = new EOFetchSpecification(
                        Submission.ENTITY_NAME, q, sortOrderings);
        
                submissions =
                    localContext().objectsWithFetchSpecification(fspec);
            }
            catch (Exception e)
            {
                String msg = e.getMessage();
                qualifierErrors = new NSMutableDictionary<String, String>();
                qualifierErrors.setObjectForKey(msg, msg);
            }
    
            // Enqueue the whole lot of submissions for regrading.
            
            if (submissions != null)
            {
                if (enqueueThread == null)
                {
                    enqueueThread = new EnqueueSubmissionsThread();
                    enqueueThread.addSubmissions(submissions);
                    enqueueThread.start();
                }
                else
                {
                    enqueueThread.addSubmissions(submissions);
                }
            }
        }
        
        return null;
    }
    

    // ----------------------------------------------------------
    public boolean isCurrentlyEnqueuing()
    {
        if (enqueueThread != null)
        {
            if (enqueueThread.isDone())
            {
                enqueueThread = null;
                return false;
            }
            else
            {
                return true;
            }
        }
        
        return false;
    }


    // ----------------------------------------------------------
    public int submissionsEnqueuedSoFar()
    {
        if (enqueueThread != null)
        {
            return enqueueThread.enqueuedSoFar();
        }
        else
        {
            return 0;
        }
    }


    // ----------------------------------------------------------
    public int totalSubmissionsToEnqueue()
    {
        if (enqueueThread != null)
        {
            return enqueueThread.submissionsAddedSoFar();
        }
        else
        {
            return 0;
        }
    }
    
    
    // ----------------------------------------------------------
    public String enqueueProgress()
    {
        int soFar = submissionsEnqueuedSoFar();
        int total = totalSubmissionsToEnqueue();
        
        if (total == 0)
        {
            return "100%";
        }
        else
        {
            int progress = (int) ((double) soFar / total * 100 + 0.5);
            return "" + progress + "%";
        }
    }


    // ----------------------------------------------------------
    public WOActionResults updateSubmissionCount()
    {
        EOQualifier q;
        qualifierErrors = null;

        boolean hasQualString = false;
        if (qualifierString != null && qualifierString.trim().length() > 0)
        {
            hasQualString = true;
        }

        if (!hasQualString &&
                (prefs().assignment() == null &&
                        prefs().assignmentOffering() == null))
        {
            numberOfSubmissions = 0;
            return null;
        }

        if (hasQualString)
        {
            try
            {
                q = EOQualifier.qualifierWithQualifierFormat(
                        qualifierString, null);
            }
            catch (Exception e)
            {
                String msg = e.getMessage();
                qualifierErrors = new NSMutableDictionary<String, String>();
                qualifierErrors.setObjectForKey(msg, msg);
                q = null;
            }
        }
        else
        {
            if (prefs().assignmentOffering() != null)
            {
                q = ERXQ.is("assignmentOffering", prefs().assignmentOffering());
            }
            else
            {
                q = ERXQ.is("assignmentOffering.assignment",
                        prefs().assignment());
            }
        }

        if (q != null)
        {
            try
            {
                numberOfSubmissions =
                    ERXEOControlUtilities.objectCountWithQualifier(
                        localContext(), Submission.ENTITY_NAME, q);
            }
            catch (Exception e)
            {
                String msg = e.getMessage();
                qualifierErrors = new NSMutableDictionary<String, String>();
                qualifierErrors.setObjectForKey(msg, msg);
                numberOfSubmissions = 0;
            }
        }
        else
        {
            numberOfSubmissions = 0;
        }
        
        return null;
    }


    // ----------------------------------------------------------
    public int updateCountOfRegradingJobsInQueue()
    {
        cachedCountOfRegradingJobsInQueue =
            ERXEOControlUtilities.objectCountWithQualifier(
                localContext(),
                EnqueuedJob.ENTITY_NAME,
                ERXQ.and(
                        ERXQ.isTrue(EnqueuedJob.REGRADING_KEY),
                        ERXQ.isFalse(EnqueuedJob.PAUSED_KEY)));

        return cachedCountOfRegradingJobsInQueue;
    }
    
    
    // ----------------------------------------------------------
    public int updateCountOfSuspendedJobsInQueue()
    {
        cachedCountOfSuspendedJobsInQueue =
            ERXEOControlUtilities.objectCountWithQualifier(
                localContext(),
                EnqueuedJob.ENTITY_NAME,
                ERXQ.and(
                        ERXQ.isTrue(EnqueuedJob.REGRADING_KEY),
                        ERXQ.isTrue(EnqueuedJob.PAUSED_KEY)));

        return cachedCountOfSuspendedJobsInQueue;
    }
    

    // ----------------------------------------------------------
    public NSArray<EnqueuedJob> nextSetOfJobsInQueue()
    {
        EOFetchSpecification fspec = new EOFetchSpecification(
                EnqueuedJob.ENTITY_NAME,
                ERXQ.and(
                        ERXQ.isTrue(EnqueuedJob.REGRADING_KEY),
                        ERXQ.isFalse(EnqueuedJob.PAUSED_KEY)),
                null);
        fspec.setFetchLimit(10);
        
        return localContext().objectsWithFetchSpecification(fspec);
    }
    

    //~ Private classes .......................................................

    // ----------------------------------------------------------
    /**
     * A separate thread that pops submissions off an array and enqueues them
     * to the grader.
     */
    private class EnqueueSubmissionsThread extends Thread
    {
        //~ Constructor .......................................................

        // ----------------------------------------------------------
        public EnqueueSubmissionsThread()
        {
            submissions = new NSMutableArray<Submission>();
        }


        //~ Methods ...........................................................

        // ----------------------------------------------------------
        public void run()
        {
            EOEditingContext ec = Application.newPeerEditingContext();

            clearEnqueuedSoFar();

            while (hasMoreSubmissions())
            {
                popNextSubmission().requeueForGrading(ec);
                incrementEnqueuedSoFar();
            }

            setIsDone(true);

            ec.saveChanges();
            Grader.getInstance().graderQueue().enqueue( null );
            
            Application.releasePeerEditingContext(ec);
        }
        

        // ----------------------------------------------------------
        public synchronized void addSubmissions(NSArray<Submission> subs)
        {
            subsAdded += subs.count();
            submissions.addObjectsFromArray(subs);
        }


        // ----------------------------------------------------------
        public synchronized Submission popNextSubmission()
        {
            return submissions.removeObjectAtIndex(0);
        }


        // ----------------------------------------------------------
        public synchronized boolean hasMoreSubmissions()
        {
            return !submissions.isEmpty();
        }


        // ----------------------------------------------------------
        public synchronized boolean isDone()
        {
            return isDone;
        }

        
        // ----------------------------------------------------------
        public synchronized int enqueuedSoFar()
        {
            return enqueuedSoFar;
        }


        // ----------------------------------------------------------
        public synchronized int submissionsAddedSoFar()
        {
            return subsAdded;
        }


        // ----------------------------------------------------------
        private synchronized void clearEnqueuedSoFar()
        {
            enqueuedSoFar = 0;
        }


        // ----------------------------------------------------------
        private synchronized void incrementEnqueuedSoFar()
        {
            enqueuedSoFar++;
        }


        // ----------------------------------------------------------
        private synchronized void setIsDone(boolean value)
        {
            isDone = value;
            
            if (isDone)
            {
                subsAdded = 0;
                enqueuedSoFar = 0;
            }
        }


        //~ Static/instance variables .........................................

        private boolean isDone = false;
        private int enqueuedSoFar = 0;
        private int subsAdded = 0;
        private NSMutableArray<Submission> submissions;
    }
    

    //~ Static/instance variables .............................................

    /**
     * This is static because it needs to stay alive even if the user navigates
     * away from the page and back while submissions are being enqueued, but
     * it's not really significant enough to create a separate queue processor
     * for it.
     */
    private static EnqueueSubmissionsThread enqueueThread;
}
