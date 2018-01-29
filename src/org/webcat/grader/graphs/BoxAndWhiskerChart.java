/*==========================================================================*\
 |  $Id: BoxAndWhiskerChart.java,v 1.2 2014/11/07 13:55:04 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2010 Virginia Tech
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

import java.awt.Color;
import java.awt.Font;
import java.util.Date;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.BoxAndWhiskerXYDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;
import static org.webcat.grader.Submission.CumulativeStats;
import com.webobjects.appserver.WOContext;

//-------------------------------------------------------------------------
/**
 * Displays a small horizontal box-and-whisker chart showing the distribution
 * of a list of student scores.
 *
 * TODO extend this to take an arbitrary list instead of the
 * Submission.CumulativeStats object; will need to add KVC interfaces to that
 * class
 *
 * @author Tony Allevato
 * @version $Id: BoxAndWhiskerChart.java,v 1.2 2014/11/07 13:55:04 stedwar2 Exp $
 */
public class BoxAndWhiskerChart extends JFreeChartComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public BoxAndWhiskerChart(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public Double maxScale;
    public CumulativeStats submissionStats;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public boolean sizesToFit()
    {
        return true;
    }


    // ----------------------------------------------------------
    @Override
    public int minimumHeight()
    {
        return 20;
    }


    // ----------------------------------------------------------
    @Override
    public boolean shouldDisplay()
    {
        return true;
    }


    // ----------------------------------------------------------
    public void setSubmissionStats(CumulativeStats stats)
    {
        submissionStats = stats;

        // Force the dataset that was previously generated to be cleared out.
        // It will be regenerated with the new data the next time it's needed.
        setDataset(null);
    }


    // ----------------------------------------------------------
    @Override
    public XYDataset dataset()
    {
        BoxAndWhiskerXYDataset dataset =
            (BoxAndWhiskerXYDataset)super.dataset();

        if (dataset == null)
        {
            DefaultBoxAndWhiskerXYDataset newDataset =
                new DefaultBoxAndWhiskerXYDataset("Submissions");

            if (submissionStats.allScores().size() > 0)
            {
                newDataset.add(new Date(),
                    BoxAndWhiskerCalculator.calculateBoxAndWhiskerStatistics(
                        submissionStats.allScores()));
            }

            dataset = newDataset;
            super.setDataset(dataset);
        }

        return dataset;
    }


    // ----------------------------------------------------------
    @Override
    protected JFreeChart generateChart(WCChartTheme chartTheme)
    {
        setChartHeight(36);

        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
            null, null, yAxisLabel(), boxAndWhiskerXYDataset(), false);
        chart.getXYPlot().setOrientation(PlotOrientation.HORIZONTAL);
        chart.setPadding(new RectangleInsets(0, 6, 0, 6));

        XYPlot plot = chart.getXYPlot();
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(new Color(0, 0, 0, 0));
        XYBoxAndWhiskerRenderer renderer =
            (XYBoxAndWhiskerRenderer)plot.getRenderer();
        renderer.setAutoPopulateSeriesOutlinePaint(true);

        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setVisible(false);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        double max = (maxScale == null) ? submissionStats.max() : maxScale;
        rangeAxis.setRange(-0.5, max + 0.5);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        Font oldFont = rangeAxis.getTickLabelFont();
        rangeAxis.setTickLabelFont(oldFont.deriveFont(
            oldFont.getSize2D() * 0.8f));

        return chart;
    }


    // ----------------------------------------------------------
    public BoxAndWhiskerXYDataset boxAndWhiskerXYDataset()
    {
        return (BoxAndWhiskerXYDataset) dataset();
    }
}
