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

package net.sf.webcat.grader.graphs.tests;

import net.sf.webcat.grader.graphs.*;

// -------------------------------------------------------------------------
/**
 * A set of test cases for assignment summaries.
 *
 * @author  Stephen Edwards
 * @version $Id$
 */
public class AssignmentSummaryTest
    extends junit.framework.TestCase
{
    static final float DELTA = 0.0001f;

    // ----------------------------------------------------------
    protected void setUp()
    {
        summary = new AssignmentSummary();
    }


    // ----------------------------------------------------------
    public void testEmpty()
    {
        assertEquals( 0, summary.students() );
        assertEquals( 0, summary.submissions() );
        assertEquals( 0.0f, summary.mean(), DELTA );
        float[] dist = summary.percentageDistribution();
        assertEquals(
            AssignmentSummary.DEFAULT_NUMBER_OF_DIVISIONS, dist.length );
        for ( int i = 0; i < AssignmentSummary.DEFAULT_NUMBER_OF_DIVISIONS;
              i++ )
        {
            assertEquals( 0.0f, dist[i], DELTA );
        }
    }


    // ----------------------------------------------------------
    public void testAdd1()
    {
        summary.addSubmission( 75.0 );
        assertEquals( 1, summary.students() );
        assertEquals( 1, summary.submissions() );
        assertEquals( 75.0f, summary.mean(), DELTA );
        float[] dist = summary.percentageDistribution();
        assertEquals(
            AssignmentSummary.DEFAULT_NUMBER_OF_DIVISIONS, dist.length );
        for ( int i = 0; i < AssignmentSummary.DEFAULT_NUMBER_OF_DIVISIONS;
              i++ )
        {
            if ( i == 7 )
                assertEquals( 1.0f, dist[i], DELTA );
            else
                assertEquals( 0.0f, dist[i], DELTA );
        }
    }


    // ----------------------------------------------------------
    public void testUpdate1()
    {
        summary.addSubmission( 75.0 );
        assertEquals( 1, summary.students() );
        assertEquals( 1, summary.submissions() );
        summary.updateSubmission( 75.0, 58.0 );
        assertEquals( 1, summary.students() );
        assertEquals( 2, summary.submissions() );
        assertEquals( 58.0f, summary.mean(), DELTA );
        float[] dist = summary.percentageDistribution();
        assertEquals(
            AssignmentSummary.DEFAULT_NUMBER_OF_DIVISIONS, dist.length );
        for ( int i = 0; i < AssignmentSummary.DEFAULT_NUMBER_OF_DIVISIONS;
              i++ )
        {
            if ( i == 5 )
                assertEquals( 1.0f, dist[i], DELTA );
            else
                assertEquals( 0.0f, dist[i], DELTA );
        }
    }




    // ----------------------------------------------------------
    public void testABunch()
    {
        summary.addSubmission( 75.0 );
        summary.updateSubmission( 75.0, 58.0 );
        summary.addSubmission( 76.0 );
        summary.addSubmission( 72.0 );
        summary.addSubmission( 59.0 );
        summary.addSubmission( 97.0 );
        summary.addSubmission( 101.0 );
        assertEquals( 6, summary.students() );
        assertEquals( 7, summary.submissions() );
        assertEquals( 77.1666666f, summary.mean(), DELTA );
        float[] dist = summary.percentageDistribution();
        assertEquals(
            AssignmentSummary.DEFAULT_NUMBER_OF_DIVISIONS, dist.length );
        for ( int i = 0; i < AssignmentSummary.DEFAULT_NUMBER_OF_DIVISIONS;
              i++ )
        {
            if ( i == 5 || i == 7 || i == 9 )
                assertEquals( 0.33333f, dist[i], DELTA );
            else
                assertEquals( 0.0f, dist[i], DELTA );
        }
    }
    //~ Instance/static variables .............................................

    private AssignmentSummary summary;
}
