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
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.log4j.Logger;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.axis.PeriodAxisLabelInfo;
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
extends WOComponent
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
            JFreeChart chart = org.jfree.chart.ChartFactory
                .createStackedXYAreaChart(
                    title, xAxisLabel, yAxisLabel, dataset, orientation,
                    true, false, false );
            chart.setBackgroundPaint( Color.white );
            TextTitle tt = chart.getTitle();
            tt.setPaint( darkGreen );
            XYPlot plot = chart.getXYPlot();

            long diff = (long)dataset.getXValue( 0, dataset.getItemCount() - 1 )
                - (long)dataset.getXValue( 0, 0 );
            GregorianCalendar calDiff = new GregorianCalendar();
            calDiff.setTime( new NSTimestamp( diff ) );
            // Set the time axis
            PeriodAxis axis = new PeriodAxis( null );// ( "Date" );
            PeriodAxisLabelInfo labelinfo[] = new PeriodAxisLabelInfo[2];
            if ( calDiff.get( Calendar.DAY_OF_YEAR ) > 1 )
            {
                // axis.setTimeZone(TimeZone.getTimeZone("Pacific/Auckland"));
                axis.setAutoRangeTimePeriodClass(
                    org.jfree.data.time.Day.class );
                labelinfo[0] = new PeriodAxisLabelInfo(
                    org.jfree.data.time.Day.class, new SimpleDateFormat( "d" ));
                labelinfo[1] = new PeriodAxisLabelInfo(
                    org.jfree.data.time.Month.class,
                    new SimpleDateFormat( "MMM" ) );
            }
            else
            {
                axis.setAutoRangeTimePeriodClass(
                    org.jfree.data.time.Hour.class );
                labelinfo[0] = new PeriodAxisLabelInfo(
                    org.jfree.data.time.Day.class, new SimpleDateFormat( "ha" ));
                labelinfo[1] = new PeriodAxisLabelInfo(
                    org.jfree.data.time.Month.class,
                    new SimpleDateFormat( "MMM-d" ) );
            }
            axis.setLabelInfo( labelinfo );
            plot.setDomainAxis( axis );

            XYItemRenderer renderer = plot.getRenderer();
            renderer.setSeriesPaint( 0, darkGreen );
            if ( markValue != null )
            {
                plot.setDomainCrosshairVisible( true );
                plot.setDomainCrosshairValue( markValue.doubleValue() );
                plot.setDomainCrosshairPaint( Color.red );
                plot.setDomainCrosshairStroke( myStroke );
            }

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
