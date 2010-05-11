/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2010 Virginia Tech
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

package org.webcat.grader.graphs;

import org.webcat.core.*;
import org.webcat.grader.*;
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

// -------------------------------------------------------------------------
/**
 * Presents graphs of this assignment's data.
 *
 * @author  Stephen Edwards
 * @author  latest changes by: $Author$
 * @version $Revision$, $Date$
 */
public class AssignmentGraphPage
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public AssignmentGraphPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public Number mostRecentScore;
    public boolean hasSubmissions;
    public SubmissionResultDataset correctnessToolsDataset;
    public SubmissionResultDataset opportunitiesDataset;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        SubmissionResult subResult = prefs().assignmentOffering()
            .mostRecentSubmissionResultFor( user() );
        mostRecentScore = ( subResult == null )
            ? null
            : new Double( subResult.automatedScore() );
        pastSubResults = SubmissionResult.resultsForAssignmentAndUser(
            localContext(),
            prefs().assignmentOffering(),
            user() );
        hasSubmissions = pastSubResults != null
            && pastSubResults.count() > 0;
        correctnessToolsDataset = new SubmissionResultDataset(
            pastSubResults,
            SubmissionResultDataset.TESTING_AND_STATIC_TOOLS_SCORE_SERIES );
        opportunitiesDataset = new SubmissionResultDataset(
            pastSubResults,
            SubmissionResultDataset.TESTING_AND_STATIC_TOOLS_LOSS_SERIES );
        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public boolean isStaff()
    {
        CourseOffering course = prefs().assignmentOffering().courseOffering();
        boolean result = course.isInstructor( user() )
            || course.isGrader( user() );
        System.out.println( "isStaff = " + result );
        return result;
    }


    // ----------------------------------------------------------
    public NSArray<SubmissionResult> submissionResultsByNumber()
    {
        if ( subResultsByNumber == null )
        {
            subResultsByNumber =
                SubmissionResult.mostRecentResultsForAssignmentOrderedByNumber(
                    localContext(), prefs().assignmentOffering() );
        }
        return subResultsByNumber;
    }


    //~ Instance/static variables .............................................

    private NSArray<SubmissionResult> pastSubResults;
    private NSArray<SubmissionResult> subResultsByNumber;
}
