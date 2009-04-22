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
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
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
public class MassRegraderPage extends GraderComponent
{
    //~ Constructors ..........................................................
    
    // ----------------------------------------------------------
    public MassRegraderPage(WOContext context)
    {
        super(context);
    }
    

    //~ KVC attributes (must be public) .......................................

    public NSArray<CourseOffering> courseOfferings;
    public CourseOffering selectedCourseOffering;

    public NSArray<AssignmentOffering> assignmentOfferings;
    public AssignmentOffering selectedAssignmentOffering;

    public EnqueuedJob job;
    

    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public void appendToResponse(WOResponse response, WOContext context)
    {
        if (courseOfferings == null)
        {
            courseOfferings =
                ERXArrayUtilities.arrayByAddingObjectsFromArrayWithoutDuplicates(
                        user().teaching(),
                        user().adminForButNoOtherRelationships());
        }

        super.appendToResponse(response, context);
    }

    
    // ----------------------------------------------------------
    public WOActionResults updateAssignments()
    {
        selectedAssignmentOffering = null;
        
        EOFetchSpecification fspec = new EOFetchSpecification(
                AssignmentOffering.ENTITY_NAME,
                ERXQ.equals("courseOffering", selectedCourseOffering),
                null);

        assignmentOfferings =
            localContext().objectsWithFetchSpecification(fspec);

        return null;
    }


    // ----------------------------------------------------------
    public WOActionResults massRegrade()
    {
        if (selectedAssignmentOffering == null
                || selectedCourseOffering == null)
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

        EOFetchSpecification fspec = new EOFetchSpecification(
                Submission.ENTITY_NAME,
                ERXQ.is("assignmentOffering", selectedAssignmentOffering),
                sortOrderings);

        NSArray<Submission> submissions =
            localContext().objectsWithFetchSpecification(fspec);

        // Enqueue the whole lot of submissions for regrading.

        for (Submission sub : submissions)
        {
            sub.requeueForGrading( localContext() );
        }

        localContext().saveChanges();
        Grader.getInstance().graderQueue().enqueue( null );
        
        return null;
    }
    

    // ----------------------------------------------------------
    public int countOfRegradingJobsInQueue()
    {
        return ERXEOControlUtilities.objectCountWithQualifier(
                localContext(),
                EnqueuedJob.ENTITY_NAME,
                ERXQ.isTrue(EnqueuedJob.REGRADING_KEY));
    }
    
    
    // ----------------------------------------------------------
    public NSArray<EnqueuedJob> next10JobsInQueue()
    {
        EOFetchSpecification fspec = new EOFetchSpecification(
                EnqueuedJob.ENTITY_NAME,
                ERXQ.isTrue(EnqueuedJob.REGRADING_KEY),
                null);
        fspec.setFetchLimit(10);
        
        return localContext().objectsWithFetchSpecification(fspec);
    }
}
