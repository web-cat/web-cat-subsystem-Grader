/*==========================================================================*\
 |  $Id: HistogramChart.java,v 1.1 2010/05/11 14:51:40 aallowat Exp $
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

package org.webcat.grader.graphs;

import java.awt.BasicStroke;
import java.awt.Color;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import com.webobjects.appserver.WOContext;

//-------------------------------------------------------------------------
/**
 * A histogram component, implemented using JFreeChart.
 *
 * @author  Stephen Edwards
 * @version $Id: HistogramChart.java,v 1.1 2010/05/11 14:51:40 aallowat Exp $
 */
public class HistogramChart extends JFreeChartComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public HistogramChart(WOContext context)
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public Number            markValue;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected int minimumHeight()
    {
        return 150;
    }


    // ----------------------------------------------------------
    protected JFreeChart generateChart(WCChartTheme chartTheme)
    {
        JFreeChart chart = ChartFactory.createHistogram(
            null, xAxisLabel(), yAxisLabel(), intervalXYDataset(),
            orientation(), false, false, false);

        XYPlot plot = chart.getXYPlot();
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setAutoPopulateSeriesOutlinePaint(true);
        renderer.setDrawBarOutline(true);
        renderer.setShadowVisible(false);

        if (markValue != null)
        {
            plot.setDomainCrosshairVisible(true);
            plot.setDomainCrosshairValue(markValue.doubleValue());
            plot.setDomainCrosshairPaint(Color.red);
            plot.setDomainCrosshairStroke(MARKER_STROKE);
        }

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        return chart;
    }


    // ----------------------------------------------------------
    public IntervalXYDataset intervalXYDataset()
    {
        return (IntervalXYDataset) dataset();
    }


    //~ Instance/static variables .............................................

    private static final BasicStroke MARKER_STROKE = new BasicStroke(1.5f);
}
