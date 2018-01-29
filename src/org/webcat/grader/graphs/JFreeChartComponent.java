/*==========================================================================*\
 |  $Id: JFreeChartComponent.java,v 1.3 2010/10/19 18:37:37 aallowat Exp $
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.webcat.core.Application;
import org.webcat.core.WCComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;
import com.webobjects.foundation.NSData;

//-------------------------------------------------------------------------
/**
 * An abstract base class for the other components that render JFreeChart
 * images (StackedAreaChart and HistogramChart).
 *
 * @author Tony Allevato
 * @version $Id: JFreeChartComponent.java,v 1.3 2010/10/19 18:37:37 aallowat Exp $
 */
public abstract class JFreeChartComponent extends WCComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Initializes a new instance of the JFreeChartComponent class.
     *
     * @param context the context
     */
    public JFreeChartComponent(WOContext context)
    {
        super(context);
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Automatically provides the component template for subclasses.
     *
     * @return the component template
     */
    @Override
    public WOElement template()
    {
        if (cachedTemplate == null)
        {
            String htmlTemplate =
                "<webobject name=\"ShouldDisplay\">"
                + "<webobject name=\"Chart\"/>"
                + "</webobject>";

            String bindingDefinitions =
                "Chart: WOImage {"
                + "data     = pngChart;"
                + "mimeType = \"image/png\";";

            if (!sizesToFit())
            {
                bindingDefinitions +=
                    "width    = chartWidth;"
                    + "height   = chartHeight;";
            }

            if (style != null)
            {
                bindingDefinitions +=
                    "style    = \"" + style + "\";";
            }

            bindingDefinitions +=
                "title    = title;"
                + "alt      = title;"
                + "}"
                + "ShouldDisplay: WOConditional {"
                + "condition = shouldDisplay;"
                + "}";

            cachedTemplate = templateWithHTMLString(null, null,
                    htmlTemplate, bindingDefinitions, null,
                    Application.application().associationFactoryRegistry(),
                    Application.application().namespaceProvider());
        }

        return cachedTemplate;
    }


    // ----------------------------------------------------------
    /**
     * Gets the dataset used to generate this chart.
     *
     * @return the dataset
     */
    public XYDataset dataset()
    {
        return dataset;
    }


    // ----------------------------------------------------------
    /**
     * Sets the dataset used to generate this chart.
     *
     * @param aDataset the dataset
     */
    public void setDataset(XYDataset aDataset)
    {
        dataset = aDataset;

        // Clear the cached chart so that it has to be regenerated with the
        // new data.
        pngChart = null;
    }


    // ----------------------------------------------------------
    /**
     * Gets the title to display at the top of this chart.
     *
     * @return the chart title
     */
    public String title()
    {
        return title;
    }


    // ----------------------------------------------------------
    /**
     * Sets the title to display at the top of this chart.
     *
     * @param aTitle the chart title
     */
    public void setTitle(String aTitle)
    {
        title = aTitle;

        // Clear the cached chart so that it has to be regenerated with the
        // new labels.
        pngChart = null;
    }


    // ----------------------------------------------------------
    /**
     * Gets the label to display on the x-axis of this chart.
     *
     * @return the x-axis label
     */
    public String xAxisLabel()
    {
        return xAxisLabel;
    }


    // ----------------------------------------------------------
    /**
     * Sets the label to display on the x-axis of this chart.
     *
     * @param aLabel the x-axis label
     */
    public void setXAxisLabel(String aLabel)
    {
        xAxisLabel = aLabel;

        // Clear the cached chart so that it has to be regenerated with the
        // new labels.
        pngChart = null;
    }


    // ----------------------------------------------------------
    /**
     * Gets the label to display on the y-axis of this chart.
     *
     * @return the y-axis label
     */
    public String yAxisLabel()
    {
        return yAxisLabel;
    }


    // ----------------------------------------------------------
    /**
     * Sets the label to display on the y-axis of this chart.
     *
     * @param aLabel the y-axis label
     */
    public void setYAxisLabel(String aLabel)
    {
        yAxisLabel = aLabel;

        // Clear the cached chart so that it has to be regenerated with the
        // new labels.
        pngChart = null;
    }


    // ----------------------------------------------------------
    /**
     * Gets the orientation of this chart.
     *
     * @return the chart orientation
     */
    public PlotOrientation orientation()
    {
        return orientation;
    }


    // ----------------------------------------------------------
    /**
     * Sets the orientation of this chart.
     *
     * @param anOrientation the chart orientation
     */
    public void setOrientation(PlotOrientation anOrientation)
    {
        orientation = anOrientation;

        // Clear the cached chart so that it has to be regenerated with the
        // new orientation.
        pngChart = null;
    }


    // ----------------------------------------------------------
    /**
     * Gets the width of the chart.
     *
     * @return the width of the chart
     */
    public int chartWidth()
    {
        // Force generation of the cached chart, which may force the size of
        // the image to change.
        pngChart();

        return chartWidth;
    }


    // ----------------------------------------------------------
    /**
     * Sets the width of the chart.
     *
     * @param value the width of the chart
     */
    public void setChartWidth(int value)
    {
        chartWidth = value;

        // Clear the cached chart so that it has to be regenerated with the
        // new size.
        pngChart = null;
    }


    // ----------------------------------------------------------
    /**
     * Gets the height of the chart.
     *
     * @return the height of the chart
     */
    public int chartHeight()
    {
        // Force generation of the cached chart, which may force the size of
        // the image to change.
        pngChart();

        return chartHeight;
    }


    // ----------------------------------------------------------
    /**
     * Sets the height of the chart.
     *
     * @param value the height of the chart
     */
    public void setChartHeight(int value)
    {
        chartHeight = value;

        // Clear the cached chart so that it has to be regenerated with the
        // new size.
        pngChart = null;
    }


    // ----------------------------------------------------------
    /**
     * Gets the label to display on the x-axis of this chart.
     *
     * @return the x-axis label
     */
    public String style()
    {
        return style;
    }


    // ----------------------------------------------------------
    /**
     * Sets the label to display on the x-axis of this chart.
     *
     * @param aLabel the x-axis label
     */
    public void setStyle(String aStyle)
    {
        style = aStyle;
    }


    // ----------------------------------------------------------
    /**
     * Gets a value indicating whether the specified chart width and height
     * values should be ignored and the image tag should automatically size to
     * fit the chart.
     *
     * @return true to ignore the chart width/height; false to force the
     *     specified dimensions
     */
    public boolean sizesToFit()
    {
        return false;
    }


    // ----------------------------------------------------------
    /**
     * Gets a value indicating whether this chart should be displayed.
     *
     * @return true to display the chart; false to hide it
     */
    public boolean shouldDisplay()
    {
        return dataset != null;
    }


    // ----------------------------------------------------------
    /**
     * Gets the PNG image data for this chart.
     *
     * @return an NSData object containing the PNG image data
     */
    public NSData pngChart()
    {
        if (pngChart != null)
        {
            return pngChart;
        }

        // We synchronize on the ChartFactory class since the chart theme that
        // we use is based on the user's settings, and we want to ensure that
        // only one user is modifying it at a time.
        synchronized (ChartFactory.class)
        {
            WCChartTheme chartTheme = new WCChartTheme(wcSession().theme());
            ChartFactory.setChartTheme(chartTheme);

            JFreeChart chart = generateChart(chartTheme);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Adjust the chart height, if necessary.

            double maxItems = 2;

            for (int i = 0; i < dataset.getItemCount(0); i++)
            {
                double x = dataset.getYValue(0, i);

                if (x > maxItems)
                {
                    maxItems = x;
                }
            }

            int minHeight = minimumHeight();

            if (maxItems < 10 && chartHeight > minHeight)
            {
                chartHeight -= minHeight;
                maxItems -= 2;
                chartHeight = (int) (chartHeight * maxItems / 10 + 0.5);
                chartHeight += minHeight;
            }

            try
            {
                ChartUtilities.writeChartAsPNG(
                        baos, chart, chartWidth, chartHeight);

                pngChart = new NSData(baos.toByteArray());
            }
            catch (IOException e)
            {
                log.error("Exception creating chart", e);
            }
        }

        return pngChart;
    }


    // ----------------------------------------------------------
    /**
     * Gets the minimum height that this chart can be sized.
     *
     * @return the minimum height of the chart
     */
    protected int minimumHeight()
    {
        return 100;
    }


    // ----------------------------------------------------------
    /**
     * Generates the JFreeChart object for the chart. Subclasses must override
     * this to provide the appropriate type of chart.
     *
     * @param chartTheme the chart theme used to determine colors
     * @return the JFreeChart object for the chart
     */
    protected abstract JFreeChart generateChart(WCChartTheme chartTheme);


    //~ Static/instance variables .............................................

    private WOElement       cachedTemplate;
    private NSData          pngChart;

    private XYDataset       dataset;
    private int             chartWidth  = 400;
    private int             chartHeight = 400;
    private String          title;
    private String          xAxisLabel;
    private String          yAxisLabel;
    private String          style;
    private PlotOrientation orientation = PlotOrientation.VERTICAL;

    protected static Logger log = Logger.getLogger(JFreeChartComponent.class);
}
