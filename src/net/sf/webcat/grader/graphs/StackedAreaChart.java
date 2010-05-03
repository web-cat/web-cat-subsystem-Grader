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

package net.sf.webcat.grader.graphs;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import net.sf.webcat.core.WCComponent;
import org.apache.log4j.Logger;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.axis.PeriodAxisLabelInfo;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.TableXYDataset;

//-------------------------------------------------------------------------
/**
 * A stacked area graph component, implemented using JFreeChart.
 *
 * @author  Stephen Edwards
 * @version $Id$
 */
public class StackedAreaChart
extends WCComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public StackedAreaChart( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public TableXYDataset   dataset;
    public String           title;
    public String           xAxisLabel;
    public String           yAxisLabel;
    public String           chartType;
    public int              chartWidth  = 400;
    public int              chartHeight = 400;
    public Number           markValue;
    public PlotOrientation  orientation = PlotOrientation.VERTICAL;


    //~ Methods ...............................................................

    public NSData pngChart()
    {
        if ( pngChart == null )
        {
            WCChartTheme chartTheme = new WCChartTheme(user().theme());
            ChartFactory.setChartTheme(chartTheme);
            JFreeChart chart = org.jfree.chart.ChartFactory
                .createStackedXYAreaChart(
                    null, xAxisLabel, yAxisLabel, dataset, orientation,
                    true, false, false );

            XYPlot plot = chart.getXYPlot();

            long diff = (long)dataset.getXValue( 0, dataset.getItemCount() - 1 )
                - (long)dataset.getXValue( 0, 0 );
            GregorianCalendar calDiff = new GregorianCalendar();
            calDiff.setTime( new NSTimestamp( diff ) );

            // Set the time axis
            PeriodAxis axis = new PeriodAxis( null );// ( "Date" );
            PeriodAxisLabelInfo labelinfo[] = new PeriodAxisLabelInfo[2];

            if (calDiff.get(Calendar.DAY_OF_YEAR) > 1)
            {
                // axis.setTimeZone(TimeZone.getTimeZone("Pacific/Auckland"));
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
                axis.setAutoRangeTimePeriodClass(
                        org.jfree.data.time.Hour.class);
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
            axis.setLabelInfo( labelinfo );
            plot.setDomainAxis( axis );

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            NumberTickUnit tickUnit = new NumberTickUnit(5);
            rangeAxis.setTickUnit(tickUnit);

            XYItemRenderer renderer = plot.getRenderer();
            renderer.setSeriesPaint(0, chartTheme.seriesPaintAtIndex(0));
            renderer.setSeriesPaint(1, chartTheme.seriesPaintAtIndex(1));

            plot.setDomainMinorGridlinesVisible(false);
            plot.setRangeMinorGridlinesVisible(false);

            if (markValue != null)
            {
                plot.setDomainCrosshairVisible(true);
                plot.setDomainCrosshairValue(markValue.doubleValue());
                plot.setDomainCrosshairPaint(Color.red);
                plot.setDomainCrosshairStroke(myStroke);
            }

            chart.getLegend().setBorder(0, 0, 0, 0);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // adjust chart height
            double maxItems = 2;
            for ( int i = 0; i < dataset.getItemCount( 0 ); i++ )
            {
                double x = dataset.getYValue( 0, i );
                if ( x > maxItems ) maxItems = x;
            }
            if ( maxItems < 10 && chartHeight > 100 )
            {
                chartHeight -= 100;
                maxItems -= 2;
                chartHeight = (int)( chartHeight * maxItems / 10 + 0.5 );
                chartHeight += 100;
            }
            try
            {
                org.jfree.chart.ChartUtilities.writeChartAsPNG(
                    baos, chart, chartWidth, chartHeight );
                pngChart = new NSData( baos.toByteArray() );
            }
            catch ( java.io.IOException e )
            {
                log.error( "Exception creating chart", e );
            }
        }
        return pngChart;
    }


    // ----------------------------------------------------------
    public boolean shouldDisplay()
    {
        return dataset != null
            && dataset.getItemCount() > 1;
    }


    //~ Instance/static variables .............................................

    private NSData pngChart;
    private static final Color darkGreen = new Color( 0x33, 0x66, 0x33 );
    private static final BasicStroke myStroke = new BasicStroke( 1.5f );
    static Logger log = Logger.getLogger( StackedAreaChart.class );
}
