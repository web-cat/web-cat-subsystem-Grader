/*==========================================================================*\
 |  $Id: SubmissionResultDataset.java,v 1.2 2011/03/07 18:57:09 stedwar2 Exp $
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

package org.webcat.grader.graphs;

import com.webobjects.foundation.NSArray;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.AbstractXYDataset;
import org.webcat.grader.*;

// -------------------------------------------------------------------------
/**
 * A JFreeChart-oriented dataset used to graph a collection of submission
 * results.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.2 $, $Date: 2011/03/07 18:57:09 $
 */
@SuppressWarnings("unchecked")
public class SubmissionResultDataset
    extends AbstractXYDataset
    implements TableXYDataset
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Construct a dataset from an array of submission results.
     * @param submissionResults The submission results, sorted by increasing
     * submission time
     * @param series one of the *_SERIES constants, to determine which
     * number(s) from the submission results should be projected into data
     * series
     */
    public SubmissionResultDataset( NSArray submissionResults, int series )
    {
        results     = submissionResults;
        seriesStyle = series;
    }


    //~ Constants .............................................................

    public static final int TESTING_SCORE_SERIES                  = 0;
    public static final int STATIC_TOOLS_SCORE_SERIES             = 1;
    public static final int TA_SCORE_SERIES                       = 2;
    public static final int TESTING_AND_STATIC_TOOLS_SCORE_SERIES = 3;
    public static final int TESTING_AND_STATIC_TOOLS_LOSS_SERIES  = 4;
    public static final int ALL_SCORE_SERIES                      = 5;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public int getSeriesCount()
    {
        switch ( seriesStyle )
        {
            case TESTING_AND_STATIC_TOOLS_SCORE_SERIES:
            case TESTING_AND_STATIC_TOOLS_LOSS_SERIES:   return 2;

            case ALL_SCORE_SERIES:                       return 4;
            default:                                     return 1;
        }
    }


    // ----------------------------------------------------------
    public Comparable getSeriesKey( int series )
    {
        return seriesName[seriesStyle][series];
    }


    // ----------------------------------------------------------
    public int indexOf( Comparable seriesKey )
    {
        for ( int i = 0; i < seriesName[seriesStyle].length; i++ )
        {
            if ( seriesName[seriesStyle][i].equals( seriesKey ) )
            {
                return i;
            }
        }
        return -1;
    }


    // ----------------------------------------------------------
    public int getItemCount()
    {
        return results.count();
    }


    // ----------------------------------------------------------
    public int getItemCount( int series )
    {
        return getItemCount();
    }


    // ----------------------------------------------------------
    public Number getX( int series, int item )
    {
        return new Double( getXValue( series, item ) );
    }


    // ----------------------------------------------------------
    public double getXValue( int series, int item )
    {
        return submission( item ).submitTime().getTime();
    }


    // ----------------------------------------------------------
    public Number getY( int series, int item )
    {
        return new Double( getYValue( series, item ) );
    }


    // ----------------------------------------------------------
    public double getYValue( int series, int item )
    {
        switch ( seriesStyle )
        {
            case TESTING_SCORE_SERIES:
                return submissionResult( item ).correctnessScore();

            case STATIC_TOOLS_SCORE_SERIES:
                return submissionResult( item ).toolScore();

            case TA_SCORE_SERIES:
                return submissionResult( item ).taScore();

            case TESTING_AND_STATIC_TOOLS_SCORE_SERIES:
                return ( series == 0 )
                    ? submissionResult( item ).correctnessScore()
                    : submissionResult( item ).toolScore();

            case TESTING_AND_STATIC_TOOLS_LOSS_SERIES:
                SubmissionResult sr = submissionResult( item );
                SubmissionProfile profile = sr.submission()
                    .assignmentOffering().assignment().submissionProfile();
                return ( series == 0 )
                    ? ( profile.availablePoints()
                          - profile.toolPoints()
                          - profile.taPoints()
                          - sr.correctnessScore() )
                    : ( profile.toolPoints() - sr.toolScore() );

            default: // ALL_SCORE_SERIES
                switch ( series )
                {
                    case 0:
                        return submissionResult( item ).correctnessScore();
                    case 1:
                        return submissionResult( item ).toolScore();
                    case 2:
                        return submissionResult( item ).taScore();
                    default:
                        return submissionResult( item ).scoreAdjustment();
                }
        }
    }


    //~ Private Methods .......................................................

    private SubmissionResult submissionResult( int pos )
    {
        return (SubmissionResult)results.objectAtIndex( pos );
    }


    private Submission submission( int pos )
    {
        return ( (SubmissionResult)results.objectAtIndex( pos ) ).submission();
    }


    //~ Instance/static variables .............................................

    private NSArray results;
    private int     seriesStyle;

    private static final String[][] seriesName  = {
            { "Correctness/Testing" },
            { "Static Analysis" },
            { "TA/Manual" },
            { "Correctness/Testing", "Style/Coding" },
            { "Correctness/Testing", "Style/Coding" },
            { "Correctness/Testing", "Style/Coding", "Design/Readability",
              "Early/Late" }
        };
}
