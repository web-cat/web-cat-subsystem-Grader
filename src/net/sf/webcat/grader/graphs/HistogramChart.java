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
import net.sf.webcat.core.WCComponent;
import org.apache.log4j.Logger;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;

//-------------------------------------------------------------------------
/**
 * A histogram component, implemented using JFreeChart.
 *
 * @author  Stephen Edwards
 * @version $Id$
 */
public class HistogramChart
    extends WCComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public HistogramChart( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public String            title;
    public String            xAxisLabel;
    public String            yAxisLabel;
    public String            chartType;
    public int               chartWidth  = 400;
    public int               chartHeight = 400;
    public Number            markValue;
    public PlotOrientation   orientation = PlotOrientation.VERTICAL;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public NSData pngChart()
    {
        if ( pngChart == null )
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // adjust chart height
            double maxItems = 2;
            IntervalXYDataset dataset = dataset();
            for ( int i = 0; i < dataset.getItemCount( 0 ); i++ )
            {
                double x = dataset.getYValue( 0, i );
                if ( x > maxItems ) maxItems = x;
            }
            if ( maxItems < 10 && chartHeight > MIN_HEIGHT )
            {
                chartHeight -= MIN_HEIGHT;
                maxItems -= 2;
                chartHeight = (int)( chartHeight * maxItems / 10 + 0.5 );
                chartHeight += MIN_HEIGHT;
            }
            try
            {
                org.jfree.chart.ChartUtilities.writeChartAsPNG(
                    baos, generateChart(), chartWidth, chartHeight );
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
    protected JFreeChart generateChart()
    {
        WCChartTheme chartTheme = new WCChartTheme(user().theme());
        ChartFactory.setChartTheme(chartTheme);
        JFreeChart chart = org.jfree.chart.ChartFactory.createHistogram(
            null, xAxisLabel, yAxisLabel, dataset(), orientation,
            false, false, false );

        XYPlot plot = chart.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint( 0, chartTheme.seriesPaintAtIndex(0) );
        if ( markValue != null )
        {
            plot.setDomainCrosshairVisible( true );
            plot.setDomainCrosshairValue( markValue.doubleValue() );
            plot.setDomainCrosshairPaint( Color.red );
            plot.setDomainCrosshairStroke( MY_STROKE );
        }
        NumberAxis numberaxis = (NumberAxis)plot.getRangeAxis();
        numberaxis.setStandardTickUnits(
            NumberAxis.createIntegerTickUnits() );
        return chart;
    }


    // ----------------------------------------------------------
    public void setDataset( IntervalXYDataset data )
    {
        theDataset = data;
    }


    // ----------------------------------------------------------
    public IntervalXYDataset dataset()
    {
        return theDataset;
    }


    //~ Instance/static variables .............................................

    private NSData pngChart;
    private IntervalXYDataset theDataset;

    private static final Color DARK_GREEN = new Color( 0x33, 0x66, 0x33 );
    private static final BasicStroke MY_STROKE = new BasicStroke( 1.5f );
    private static final int MIN_HEIGHT = 150;

    static Logger log = Logger.getLogger( HistogramChart.class );
}
