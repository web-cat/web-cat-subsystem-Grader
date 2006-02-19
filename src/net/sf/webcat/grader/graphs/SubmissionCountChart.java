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
import er.extensions.ERXConstant;

import net.sf.webcat.grader.*;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.data.xy.*;
import org.jfree.data.xy.IntervalXYDataset;

//-------------------------------------------------------------------------
/**
 * A histogram component, implemented using JFreeChart.
 *
 * @author  Stephen Edwards
 * @version $Id$
 */
public class SubmissionCountChart
    extends HistogramChart
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public SubmissionCountChart( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public NSArray submissionResults;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public IntervalXYDataset dataset()
    {
        IntervalXYDataset dataset = super.dataset();
        if ( dataset == null )
        {
            dataset = new Dataset();
            super.setDataset( dataset );
        }
        return dataset;
    }


    // ----------------------------------------------------------
    protected JFreeChart generateChart()
    {
        JFreeChart chart = super.generateChart();
        NumberAxis numberaxis = (NumberAxis)chart.getXYPlot().getDomainAxis();
        numberaxis.setStandardTickUnits(
            NumberAxis.createIntegerTickUnits() );
        return chart;
    }


    // ----------------------------------------------------------
    private class Dataset
        extends AbstractIntervalXYDataset
    {
        public static final String SUBMISSION_COUNT_KEY = "No. of Submissions";

        // ----------------------------------------------------------
        public int getSeriesCount()
        {
            return 1;
        }
    
    
        // ----------------------------------------------------------
        public Comparable getSeriesKey( int series )
        {
            return ( series == 0 )
                ? SUBMISSION_COUNT_KEY
                : null;
        }
    
    
        // ----------------------------------------------------------
        public int indexOf( Comparable seriesKey )
        {
            return SUBMISSION_COUNT_KEY.equals( seriesKey )
                ? 0
                : -1;
        }
    
    
        // ----------------------------------------------------------
        public int getItemCount( int series )
        {
            return submissionResults.count();
        }
    
    
        // ----------------------------------------------------------
        public Number getX( int series, int item )
        {
            return ERXConstant.integerForInt( item + 1 );
        }
    
    
        // ----------------------------------------------------------
        public double getXValue( int series, int item )
        {
            return item + 1;
        }
    
    
        // ----------------------------------------------------------
        public Number getY( int series, int item )
        {
            return ERXConstant.integerForInt(
                submissionResult( item ).submission().submitNumber() );
        }
    
    
        // ----------------------------------------------------------
        public double getYValue( int series, int item )
        {
            return submissionResult( item ).submission().submitNumber();
        }
    
    
        // ----------------------------------------------------------
        public Number getEndX( int series, int item )
        {
            return new Double( getEndXValue( series, item ) );
        }
    
    
        // ----------------------------------------------------------
        public double getEndXValue( int series, int item )
        {
            return getXValue( series, item ) + 0.5;
        }
    
    
        // ----------------------------------------------------------
        public Number getEndY( int series, int item )
        {
            return getY( series, item );
        }
    
    
        // ----------------------------------------------------------
        public double getEndYValue( int series, int item )
        {
            return getYValue( series, item );
        }
        
    
        // ----------------------------------------------------------
        public Number getStartX( int series, int item )
        {
            return new Double( getStartXValue( series, item ) );
        }
    
    
        // ----------------------------------------------------------
        public double getStartXValue( int series, int item )
        {
            return getXValue( series, item ) - 0.5;
        }
    
    
        // ----------------------------------------------------------
        public Number getStartY( int series, int item )
        {
            return getY( series, item );
        }
    
    
        // ----------------------------------------------------------
        public double getStartYValue( int series, int item )
        {
            return getYValue( series, item );
        }
        
        
        // ----------------------------------------------------------
        private SubmissionResult submissionResult( int item )
        {
            return (SubmissionResult)submissionResults.objectAtIndex( item );
        }
    }


}
