/*==========================================================================*\
 |  $Id: WCChartTheme.java,v 1.3 2010/09/26 16:24:50 stedwar2 Exp $
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
import java.awt.Paint;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.webcat.core.Theme;

//-------------------------------------------------------------------------
/**
 * A JFreeChart theme that pulls information about colors from a user's current
 * Web-CAT theme.
 *
 * @author Tony Allevato
 * @version $Id: WCChartTheme.java,v 1.3 2010/09/26 16:24:50 stedwar2 Exp $
 */
public class WCChartTheme extends StandardChartTheme
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Initializes a new WCChartTheme, which is a JFreeChart theme that is
     * customized for Web-CAT and can eventually pull values directly from the
     * user's currently selected Web-CAT theme.
     *
     * @param theme the user's currently selected Web-CAT theme
     */
    public WCChartTheme(Theme theme)
    {
        super("Web-CAT");

        if (theme != null)
        {
            this.theme = theme;
        }
        else
        {
            this.theme = Theme.defaultTheme();
        }

        // Use the font that we currently have as the default Web-CAT font.

        String fontName = defaultFontName();
        setExtraLargeFont(new Font(fontName, Font.BOLD, 20));
        setLargeFont(new Font(fontName, Font.BOLD, 14));
        setRegularFont(new Font(fontName, Font.BOLD, 12));
        setSmallFont(new Font(fontName, Font.BOLD, 10));

        // Set the text color based on whether the theme is light or dark.

        Color textColor = textColor();
        setAxisLabelPaint(textColor);
        setItemLabelPaint(textColor);
        setLegendItemPaint(textColor);
        setSubtitlePaint(textColor);
        setTickLabelPaint(textColor);
        setTitlePaint(textColor);

        // Make the background of the charts transparent.

        setPlotBackgroundPaint(new Color(144, 144, 144, 128));

        Color transparent = new Color(0, 0, 0, 0);
        setChartBackgroundPaint(transparent);
        setLegendBackgroundPaint(transparent);

        // Kill the gradients that newer versions of JFreeChart add by default.

        setDrawingSupplier(new ThemeBasedDrawingSupplier());
        setBarPainter(new StandardBarPainter());
        setXYBarPainter(new StandardXYBarPainter());
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Gets the name of the font that should be used for titles and labels.
     *
     * @return the name of the title/label font
     */
    protected String defaultFontName()
    {
        return "Trebuchet MS";
    }


    // ----------------------------------------------------------
    /**
     * Gets a small font appropriate to display numbers along axes.
     *
     * @return a small font
     */
    public Font smallFont()
    {
        if (smallFont == null)
        {
            smallFont = new Font(defaultFontName(), Font.PLAIN, 10);
        }

        return smallFont;
    }


    // ----------------------------------------------------------
    /**
     * Gets the text color to use for titles, labels, and axes.
     *
     * @return the text color
     */
    public Color textColor()
    {
        return theme.isDark() ?
                textColorForDarkThemes : textColorForLightThemes;
    }


    //~ Static/instance variables .............................................

    // ----------------------------------------------------------
    /**
     * A drawing supplier that uses information from the user's currently
     * selected theme to choose appropriate fill and outline colors.
     */
    private class ThemeBasedDrawingSupplier extends DefaultDrawingSupplier
    {
        // ----------------------------------------------------------
        /**
         * Initializes a new instance of the ThemeBasedDrawingSupplier class.
         */
        public ThemeBasedDrawingSupplier()
        {
            if (theme.isDark())
            {
                outlinePaints = outlinePaintsForDarkThemes;
                barPaints = barPaintsForDarkThemes;
            }
            else
            {
                outlinePaints = outlinePaintsForLightThemes;
                barPaints = barPaintsForLightThemes;
            }
        }


        // ----------------------------------------------------------
        public Paint getNextOutlinePaint()
        {
            Paint paint = outlinePaints[outlinePaintIndex % outlinePaints.length];
            outlinePaintIndex++;
            return paint;
        }


        // ----------------------------------------------------------
        public Paint getNextPaint()
        {
            Paint paint = barPaints[barPaintIndex % barPaints.length];
            barPaintIndex++;
            return paint;
        }


        //~ Static/instance variables .........................................

        private Paint[] outlinePaints;
        private Paint[] barPaints;
        private int outlinePaintIndex;
        private int barPaintIndex;

        private final Paint[] outlinePaintsForLightThemes = {
            new Color(15, 44, 15),      // green
            new Color(75, 10, 15),      // red
            new Color(13, 12, 41),      // blue
            new Color(95, 95, 15),      // yellow
            new Color(92, 42, 15),      // orange
            new Color(17, 75, 75),      // cyan
        };

        private final Paint[] barPaintsForLightThemes = {
            new Color(32, 112, 32),     // green
            new Color(192, 32, 32),     // red
            new Color(32, 32, 112),     // blue
            new Color(224, 224, 32),    // yellow
            new Color(224, 112, 32),    // orange
            new Color(32, 192, 192),    // cyan
        };

        private final Paint[] outlinePaintsForDarkThemes = {
            new Color(15, 96, 0),       // green
            new Color(92, 0, 5),        // red
            new Color(20, 48, 93),      // blue
            new Color(105, 105, 15),    // yellow
            new Color(92, 42, 15),      // orange
            new Color(19, 94, 94),      // cyan
        };

        private final Paint[] barPaintsForDarkThemes = {
            new Color(0, 224, 0),       // green
            new Color(224, 0, 0),       // red
            new Color(48, 128, 224),    // blue
            new Color(248, 248, 32),    // yellow
            new Color(224, 112, 32),    // orange
            new Color(32, 224, 224),    // cyan
        };
    }


    //~ Static/instance variables .............................................

    private Theme theme;
    private Font smallFont;

    private static Color textColorForDarkThemes = new Color(224, 224, 224);
    private static Color textColorForLightThemes = new Color(24, 24, 24);
}
