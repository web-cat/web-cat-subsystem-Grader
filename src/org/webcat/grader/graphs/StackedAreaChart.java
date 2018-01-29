/*==========================================================================*\
 |  $Id: StackedAreaChart.java,v 1.1 2010/05/11 14:51:40 aallowat Exp $
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.axis.PeriodAxisLabelInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.TableXYDataset;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSTimestamp;

//-------------------------------------------------------------------------
/**
 * A stacked area graph component, implemented using JFreeChart.
 *
 * @author  Stephen Edwards
 * @version $Id: StackedAreaChart.java,v 1.1 2010/05/11 14:51:40 aallowat Exp $
 */
public class StackedAreaChart extends JFreeChartComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public StackedAreaChart(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public Number           markValue;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    private TableXYDataset tableXYDataset()
    {
        return (TableXYDataset) dataset();
    }


    // ----------------------------------------------------------
    @Override
    public boolean shouldDisplay()
    {
        return super.shouldDisplay() && tableXYDataset().getItemCount() > 1;
    }


    // ----------------------------------------------------------
    @Override
    protected JFreeChart generateChart(WCChartTheme chartTheme)
    {
        JFreeChart chart = ChartFactory.createStackedXYAreaChart(
                null, xAxisLabel(), yAxisLabel(), tableXYDataset(),
                orientation(), true, false, false);

        XYPlot plot = chart.getXYPlot();

        long diff = (long) tableXYDataset().getXValue(
                0, tableXYDataset().getItemCount() - 1)
            - (long) tableXYDataset().getXValue(0, 0);

        GregorianCalendar calDiff = new GregorianCalendar();
        calDiff.setTime(new NSTimestamp(diff));

        // Set the time axis
        PeriodAxis axis = new PeriodAxis(null); // ( "Date" );
        PeriodAxisLabelInfo labelinfo[] = new PeriodAxisLabelInfo[2];

        if (calDiff.get(Calendar.DAY_OF_YEAR) > 1)
        {
            axis.setTimeZone(TimeZone.getTimeZone(user().timeZoneName()));
            axis.setAutoRangeTimePeriodClass(org.jfree.data.time.Day.class);
            axis.setMajorTickTimePeriodClass(org.jfree.data.time.Week.class);

            labelinfo[0] = new PeriodAxisLabelInfo(
                org.jfree.data.time.Day.class,
                new SimpleDateFormat("d"),
                PeriodAxisLabelInfo.DEFAULT_INSETS,
                chartTheme.smallFont(),
                chartTheme.textColor(),
                true,
                PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
                PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);

            labelinfo[1] = new PeriodAxisLabelInfo(
                org.jfree.data.time.Month.class,
                new SimpleDateFormat("MMM"),
                PeriodAxisLabelInfo.DEFAULT_INSETS,
                chartTheme.smallFont(),
                chartTheme.textColor(),
                true,
                PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
                PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
        }
        else
        {
            axis.setAutoRangeTimePeriodClass(org.jfree.data.time.Hour.class);

            labelinfo[0] = new PeriodAxisLabelInfo(
                org.jfree.data.time.Day.class,
                new SimpleDateFormat("ha"),
                PeriodAxisLabelInfo.DEFAULT_INSETS,
                chartTheme.smallFont(),
                chartTheme.textColor(),
                true,
                PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
                PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);

            labelinfo[1] = new PeriodAxisLabelInfo(
                org.jfree.data.time.Month.class,
                new SimpleDateFormat("MMM-d"),
                PeriodAxisLabelInfo.DEFAULT_INSETS,
                chartTheme.smallFont(),
                chartTheme.textColor(),
                true,
                PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
                PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
        }

        axis.setLabelInfo(labelinfo);
        plot.setDomainAxis(axis);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        NumberTickUnit tickUnit = new NumberTickUnit(5);
        rangeAxis.setTickUnit(tickUnit);

        XYAreaRenderer2 renderer = (XYAreaRenderer2) plot.getRenderer();
        renderer.setOutline(true);
        renderer.setAutoPopulateSeriesOutlinePaint(true);

        plot.setDomainMinorGridlinesVisible(false);
        plot.setRangeMinorGridlinesVisible(false);

        if (markValue != null)
        {
            plot.setDomainCrosshairVisible(true);
            plot.setDomainCrosshairValue(markValue.doubleValue());
            plot.setDomainCrosshairPaint(Color.red);
            plot.setDomainCrosshairStroke(MARKER_STROKE);
        }

        chart.getLegend().setBorder(0, 0, 0, 0);

        return chart;
    }


    //~ Instance/static variables .............................................

    private static final BasicStroke MARKER_STROKE = new BasicStroke(1.5f);
}
