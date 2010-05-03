package net.sf.webcat.grader.graphs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import net.sf.webcat.core.Theme;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;

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

        this.theme = theme;

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

        setBarPainter(new StandardBarPainter());
        setXYBarPainter(new StandardXYBarPainter());
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected String defaultFontName()
    {
        return "Trebuchet MS";
    }


    // ----------------------------------------------------------
    public Font smallFont()
    {
        if (smallFont == null)
        {
            smallFont = new Font(defaultFontName(), Font.PLAIN, 10);
        }

        return smallFont;
    }


    // ----------------------------------------------------------
    public Color textColor()
    {
        return theme.isDark() ?
                textColorForDarkThemes : textColorForLightThemes;
    }


    // ----------------------------------------------------------
    public Paint seriesPaintAtIndex(int index)
    {
        Paint[] paints = theme.isDark() ?
                seriesPaintsForDarkThemes : seriesPaintsForLightThemes;

        return paints[index % paints.length];
    }


    //~ Static/instance variables .............................................

    private Theme theme;

    private Font smallFont;

    private static Color textColorForDarkThemes = new Color(224, 224, 224);

    private static Color textColorForLightThemes = new Color(0, 0, 0);

    private static Paint[] seriesPaintsForLightThemes = {
        new Color(32, 112, 32),     // green
        new Color(192, 32, 32),     // red
        new Color(32, 32, 112),     // blue
        new Color(224, 224, 32),    // yellow
        new Color(224, 112, 32),    // orange
        new Color(32, 192, 192),    // cyan
    };

    private static Paint[] seriesPaintsForDarkThemes = {
        new Color(0, 224, 0),     // green
        new Color(224, 0, 0),     // red
        new Color(48, 128, 224),     // blue
        new Color(248, 248, 32),    // yellow
        new Color(224, 112, 32),    // orange
        new Color(32, 224, 224),    // cyan
    };
}
