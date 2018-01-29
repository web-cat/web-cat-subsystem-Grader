/*==========================================================================*\
 |  $Id: SubmissionCountChart.java,v 1.2 2011/03/07 18:57:09 stedwar2 Exp $
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

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.webcat.grader.SubmissionResult;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import er.extensions.eof.ERXConstant;

//-------------------------------------------------------------------------
/**
 * A histogram component, implemented using JFreeChart.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.2 $, $Date: 2011/03/07 18:57:09 $
 */
public class SubmissionCountChart extends HistogramChart
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

    public NSArray<SubmissionResult> submissionResults;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public boolean sizesToFit()
    {
        return true;
    }


    // ----------------------------------------------------------
    @Override
    public boolean shouldDisplay()
    {
        return true;
    }


    // ----------------------------------------------------------
    @Override
    public XYDataset dataset()
    {
        IntervalXYDataset dataset = (IntervalXYDataset) super.dataset();

        if (dataset == null)
        {
            dataset = new Dataset();
            super.setDataset(dataset);
        }

        return dataset;
    }


    // ----------------------------------------------------------
    @Override
    protected JFreeChart generateChart(WCChartTheme chartTheme)
    {
        JFreeChart chart = super.generateChart(chartTheme);

        NumberAxis domainAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return chart;
    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    private class Dataset extends AbstractIntervalXYDataset
    {
        public static final String SUBMISSION_COUNT_KEY = "No. of Submissions";

        // ----------------------------------------------------------
        public int getSeriesCount()
        {
            return 1;
        }


        // ----------------------------------------------------------
        public Comparable getSeriesKey(int series)
        {
            return (series == 0)
                ? SUBMISSION_COUNT_KEY
                : null;
        }


        // ----------------------------------------------------------
        public int indexOf(Comparable seriesKey)
        {
            return SUBMISSION_COUNT_KEY.equals(seriesKey)
                ? 0
                : -1;
        }


        // ----------------------------------------------------------
        public int getItemCount(int series)
        {
            return submissionResults.count();
        }


        // ----------------------------------------------------------
        public Number getX( int series, int item )
        {
            return ERXConstant.integerForInt(item + 1);
        }


        // ----------------------------------------------------------
        public double getXValue(int series, int item)
        {
            return item + 1;
        }


        // ----------------------------------------------------------
        public Number getY(int series, int item)
        {
            return ERXConstant.integerForInt(
                submissionResult(item).submission().submitNumber());
        }


        // ----------------------------------------------------------
        public double getYValue(int series, int item)
        {
            return submissionResult(item).submission().submitNumber();
        }


        // ----------------------------------------------------------
        public Number getEndX(int series, int item)
        {
            return Double.valueOf(getEndXValue(series, item));
        }


        // ----------------------------------------------------------
        public double getEndXValue(int series, int item)
        {
            return getXValue(series, item) + 0.5;
        }


        // ----------------------------------------------------------
        public Number getEndY(int series, int item)
        {
            return getY(series, item);
        }


        // ----------------------------------------------------------
        public double getEndYValue(int series, int item)
        {
            return getYValue(series, item);
        }


        // ----------------------------------------------------------
        public Number getStartX(int series, int item)
        {
            return Double.valueOf(getStartXValue(series, item));
        }


        // ----------------------------------------------------------
        public double getStartXValue(int series, int item)
        {
            return getXValue(series, item) - 0.5;
        }


        // ----------------------------------------------------------
        public Number getStartY(int series, int item)
        {
            return getY(series, item);
        }


        // ----------------------------------------------------------
        public double getStartYValue(int series, int item)
        {
            return getYValue(series, item);
        }


        // ----------------------------------------------------------
        private SubmissionResult submissionResult(int item)
        {
            return submissionResults.objectAtIndex(item);
        }
    }
}
