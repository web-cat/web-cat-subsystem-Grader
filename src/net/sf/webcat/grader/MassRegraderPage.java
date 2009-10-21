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

import net.sf.webcat.core.Course;
import net.sf.webcat.core.CourseOffering;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
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
                for (Submission sub : submissions)
                {
                    sub.requeueForGrading( localContext() );
                }
        
                localContext().saveChanges();
                Grader.getInstance().graderQueue().enqueue( null );
            }
        }
        
        return null;
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
                ERXQ.isTrue(EnqueuedJob.REGRADING_KEY));

        return cachedCountOfRegradingJobsInQueue;
    }
    
    
    // ----------------------------------------------------------
    public NSArray<EnqueuedJob> nextSetOfJobsInQueue()
    {
        NSArray<EOSortOrdering> sortOrderings = new NSArray<EOSortOrdering>(
                new EOSortOrdering[] {
                        new EOSortOrdering("submission.user.userName",
                                EOSortOrdering.CompareCaseInsensitiveAscending),
                        new EOSortOrdering("submission.submitNumber",
                                EOSortOrdering.CompareAscending)
                });

        EOFetchSpecification fspec = new EOFetchSpecification(
                EnqueuedJob.ENTITY_NAME,
                ERXQ.isTrue(EnqueuedJob.REGRADING_KEY),
                sortOrderings);
        fspec.setFetchLimit(10);
        
        return localContext().objectsWithFetchSpecification(fspec);
    }
}
