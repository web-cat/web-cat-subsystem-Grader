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

package net.sf.webcat.grader.graphs;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

import net.sf.webcat.core.*;
import net.sf.webcat.grader.*;

// -------------------------------------------------------------------------
/**
 * Presents graphs of this assignment's data.
 *
 * @author  Stephen Edwards
 * @version $Id$
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
    public void appendToResponse( WOResponse response, WOContext context )
    {
        SubmissionResult subResult = prefs().assignmentOffering()
            .mostRecentSubmissionResultFor( wcSession().user() );
        mostRecentScore = ( subResult == null )
            ? null
            : new Double( subResult.graphableScore() );
        pastSubResults = SubmissionResult.objectsForUser(
            wcSession().localContext(),
            prefs().assignmentOffering(),
            wcSession().user() );
        hasSubmissions = pastSubResults != null
            && pastSubResults.count() > 0;
        correctnessToolsDataset = new SubmissionResultDataset(
            pastSubResults,
            SubmissionResultDataset.TESTING_AND_STATIC_TOOLS_SCORE_SERIES );
        opportunitiesDataset = new SubmissionResultDataset(
            pastSubResults,
            SubmissionResultDataset.TESTING_AND_STATIC_TOOLS_LOSS_SERIES );
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public boolean isStaff()
    {
        CourseOffering course = prefs().assignmentOffering().courseOffering();
        boolean result = course.isInstructor( wcSession().user() )
            || course.isTA( wcSession().user() );
        System.out.println( "isStaff = " + result );
        return result;
    }


    // ----------------------------------------------------------
    public NSArray submissionResultsByNumber()
    {
        if ( subResultsByNumber == null )
        {
            subResultsByNumber =
                SubmissionResult.objectsForMostRecentSubmissionsByNumber(
                    wcSession().localContext(),
                    prefs().assignmentOffering() );
        }
        return subResultsByNumber;
    }


    //~ Instance/static variables .............................................

    private NSArray pastSubResults;
    private NSArray subResultsByNumber;
}
