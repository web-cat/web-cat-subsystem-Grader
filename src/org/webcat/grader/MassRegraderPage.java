/*==========================================================================*\
 |  $Id: MassRegraderPage.java,v 1.4 2014/11/07 13:55:03 stedwar2 Exp $
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

import org.webcat.core.User;
import org.webcat.ui.generators.JavascriptGenerator;
import org.webcat.woextensions.ECAction;
import org.webcat.woextensions.WCEC;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import er.extensions.eof.ERXSortOrdering.ERXSortOrderings;

//--------------------------------------------------------------------------
/**
 * Implements a simple interface for performing mass-regrades of submissions to
 * an assignment (typically for data collection with an updated grading
 * plug-in).
 *
 * @author  Tony Allevato
 * @author  Last changed by: $Author: stedwar2 $
 * @version $Revision: 1.4 $, $Date: 2014/11/07 13:55:03 $
 */
public class MassRegraderPage
    extends GraderAssignmentComponent
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
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        updateSubmissionCount();

        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public JavascriptGenerator massRegrade()
    {
        JavascriptGenerator result = new JavascriptGenerator()
            .append("regraderWatcher.startEnqueueMonitor();");

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
            return result;
        }

        ERXSortOrderings sortOrderings =
            Submission.user.dot(User.userName).ascInsensitive().then(
            Submission.submitNumber.asc());

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
                q = Submission.assignmentOffering.is(
                        prefs().assignmentOffering());
            }
            else
            {
                q = Submission.assignmentOffering.dot(
                        AssignmentOffering.assignment).is(prefs().assignment());
            }
        }

        if (q != null)
        {
            NSArray<Submission> submissions = null;

            try
            {
                submissions = Submission.objectsMatchingQualifier(
                        localContext(), q, sortOrderings);
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

        return result;
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
    public JavascriptGenerator updateSubmissionCount()
    {
        JavascriptGenerator response = new JavascriptGenerator();
        response.refresh("submissionCount", "qualifierErrors");

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
            return response;
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
                q = Submission.assignmentOffering.is(
                        prefs().assignmentOffering());
            }
            else
            {
                q = Submission.assignmentOffering.dot(
                        AssignmentOffering.assignment).is(prefs().assignment());
            }
        }

        if (q != null)
        {
            try
            {
                numberOfSubmissions =
                    Submission.countOfObjectsMatchingQualifier(
                            localContext(), q);
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

        return response;
    }


    // ----------------------------------------------------------
    public int updateCountOfRegradingJobsInQueue()
    {
        cachedCountOfRegradingJobsInQueue =
            EnqueuedJob.countOfObjectsMatchingQualifier(localContext(),
                    EnqueuedJob.regrading.isTrue().and(
                    EnqueuedJob.paused.isFalse()));

        return cachedCountOfRegradingJobsInQueue;
    }


    // ----------------------------------------------------------
    public int updateCountOfSuspendedJobsInQueue()
    {
        cachedCountOfSuspendedJobsInQueue =
            EnqueuedJob.countOfObjectsMatchingQualifier(localContext(),
                    EnqueuedJob.regrading.isTrue().and(
                    EnqueuedJob.paused.isTrue()));

        return cachedCountOfSuspendedJobsInQueue;
    }


    // ----------------------------------------------------------
    public NSArray<EnqueuedJob> nextSetOfJobsInQueue()
    {
        EOQualifier qualifier =
            EnqueuedJob.regrading.isTrue().and(
            EnqueuedJob.paused.isFalse());

        ERXSortOrderings sortOrderings = EnqueuedJob.queueTime.ascs();

        EOFetchSpecification fspec = new EOFetchSpecification(
                EnqueuedJob.ENTITY_NAME, qualifier, sortOrderings);
        fspec.setFetchLimit(10);

        return EnqueuedJob.objectsWithFetchSpecification(localContext(), fspec);
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
            super("MassRegraderPage.EnqueueSubmissionsThread");
            submissions = new NSMutableArray<Submission>();
        }


        //~ Methods ...........................................................

        // ----------------------------------------------------------
        public void run()
        {
            new ECAction() { public void action() {
                int countForRecycling = 0;
                clearEnqueuedSoFar();
                NSMutableArray<Submission> subs =
                    new NSMutableArray<Submission>();

                while (hasMoreSubmissions())
                {
                    Submission sub = popNextSubmission();
                    sub.requeueForGrading(ec);
                    subs.add(sub);
                    incrementEnqueuedSoFar();

                    // Recycle the editing context every 100 submissions.

                    countForRecycling++;
                    if (countForRecycling == 100)
                    {
                        ec.saveChanges();
                        GraderQueueProcessor.processSubmissions(subs);
                        subs.clear();
                        ec.unlock();
                        ec.dispose();
                        ec = WCEC.newEditingContext();
                        ec.lock();

                        countForRecycling = 0;
                    }
                }

                setIsDone(true);

                ec.saveChanges();
                GraderQueueProcessor.processSubmissions(subs);
            }}.run();
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
