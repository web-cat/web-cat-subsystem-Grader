/*==========================================================================*\
 |  $Id: PartialInlineReport.java,v 1.7 2012/01/05 19:52:27 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2012 Virginia Tech
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

package org.webcat.grader;

import com.webobjects.appserver.*;
import com.webobjects.foundation.NSDictionary;
import er.extensions.foundation.ERXFileUtilities;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.webcat.core.EntityResourceRequestHandler;
import org.webcat.core.WCComponent;

// -------------------------------------------------------------------------
/**
 * A page for inlining an HTML fragment stored in a file.  The
 * file property should be bound to a File object referring to the
 * fragment to include.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.7 $, $Date: 2012/01/05 19:52:27 $
 */
public class PartialInlineReport
    extends WCComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public PartialInlineReport( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public String title;
    public boolean open;
    public int styleVersion;
    public boolean substituteOldCollapsingRegions = true;
    public String baseUrl;


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    public void appendToResponse( WOResponse response, WOContext context )
    {
        useModule = (styleVersion == 0);

        if ( file != null )
        {
            // Only read the file if it is really there, of course
            if ( file.exists() )
            {
                try
                {
                    content = ERXFileUtilities.stringFromFile(file, "UTF-8");
                    content = replaceVariableURLs(content, context);

                    if (substituteOldCollapsingRegions)
                    {
                        content = substituteCollapsingRegions(content);
                    }

                    if (baseUrl != null)
                    {
                        content = substituteRelativeUrls(content, baseUrl);
                    }
                }
                catch ( Exception e )
                {
                    log.error( "Exception including inlined report:", e );
                }
            }
        }
        else
        {
            log.warn( "file property is null" );
        }
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public boolean useModule()
    {
        return useModule;
    }


    // ----------------------------------------------------------
    public boolean hasData()
    {
        return content != null;
    }


    // ----------------------------------------------------------
    public String content()
    {
        return content;
    }


    // ----------------------------------------------------------
    /**
     * Access the file property value.
     *
     * @return the file property's current value
     */
    public File file()
    {
        return file;
    }


    // ----------------------------------------------------------
    /**
     * Set the file property.
     *
     * @param value the new value for the property
     */
    public void setFile( File value )
    {
        file = value;
    }


    // ----------------------------------------------------------
    /**
     * Access the submissionResult property value.
     *
     * @return the submissionResult property's current value
     */
    public SubmissionResult submissionResult()
    {
        return submissionResult;
    }


    // ----------------------------------------------------------
    /**
     * Set the submissionResult property.
     *
     * @param value the new value for the property
     */
    public void setSubmissionResult( SubmissionResult value )
    {
        submissionResult = value;
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    /**
     * <p>
     * Replaces variable URLs in the content of the report; this allows
     * plug-in authors to embed or link to resources that are generated as
     * part of the grading process.
     * </p><p>
     * Currently, two variables are supported:
     * <dl>
     * <dt>${submissionResultResource}</dt>
     * <dd>Points to the <code>public</code> directory in the submission
     * results folder.</dd>
     * <dt>${pluginResource:NAME}</dt>
     * <dd>Points to the <code>public</code> directory in the grading plug-in
     * with the specified name, where NAME is the name of the plug-in given
     * in its config.plist file.</dd>
     * <dd></dd>
     * </dl>
     * </p>
     * @param rawContent The component content to process
     * @param context The current page request context
     * @return The modified content with all required substitutions made.
     *
     */
    private String replaceVariableURLs(String rawContent, WOContext context)
    {
        // Replace ${submissionResultResource}.

        if (submissionResult != null)
        {
            String resultResourceURL = context.directActionURLForActionNamed(
                "submissionResultResource",
                new NSDictionary<String, Object>(submissionResult.id(), "id"))
                + "&path=";

            rawContent = rawContent.replaceAll("\\$\\{publicResourceURL\\}",
                resultResourceURL);
        }

        // Replace ${pluginResource:NAME}.

        Pattern regex = Pattern.compile("\\$\\{pluginResource:([^}]+)\\}");
        Matcher matcher = regex.matcher(rawContent);

        StringBuffer contentBuffer = new StringBuffer();
        while (matcher.find())
        {
            String pluginName = matcher.group(1);

            GradingPlugin plugin = GradingPlugin.firstObjectMatchingQualifier(
                    localContext(),
                    GradingPlugin.name.is(pluginName),
                    GradingPlugin.lastModified.ascs());

            String pluginResourceURL =
                EntityResourceRequestHandler.urlForEntityResource(
                        context, plugin, "");

            matcher.appendReplacement(contentBuffer, pluginResourceURL);
        }

        matcher.appendTail(contentBuffer);

        rawContent = contentBuffer.toString();

        return rawContent;
    }


    // ----------------------------------------------------------
    private String substituteCollapsingRegions(String rawContent)
    {
        boolean nestedFound = false;

        // Try to substitute out old collapsing regions
        // First, tackle nested regions
        {
            Matcher m = NESTED_MODULE.matcher(rawContent);
            StringBuffer translated =
                new StringBuffer(rawContent.length());
            int pos = 0;
            while (m.find())
            {
                int start = m.start();
                if (start > pos)
                {
                    translated.append(
                        rawContent.substring(pos, start));
                }
                boolean sectionOpen = "expanded".equals(m.group(1));
                String sectionTitle = m.group(2);
                String body = m.group(4);
                translated.append("<nesteddiv "
                    + "dojoType=\"webcat.TitlePane\" title=\"");
                translated.append(
                    sectionTitle.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;"));
                translated.append("\" open=\"");
                translated.append(sectionOpen);
                translated.append("\">\n");
                translated.append(body);
                translated.append("</nesteddiv>");
                pos = m.end();
            }
            if (pos > 0)
            {
                translated.append(rawContent.substring(pos));
                rawContent = translated.toString();
                nestedFound = true;
                useModule = false;
            }
        }

        // Now handle outer regions
        {
            Matcher m = MODULE.matcher(rawContent);
            StringBuffer translated =
                new StringBuffer(rawContent.length());
            int pos = 0;
            while (m.find())
            {
                int start = m.start();
                if (start > pos)
                {
                    translated.append(
                        rawContent.substring(pos, start));
                }
                boolean sectionOpen = "expanded".equals(m.group(1));
                String sectionTitle = m.group(2);
                String body = m.group(4);
                translated.append("<div class=\"module\"><div "
                    + "dojoType=\"webcat.TitlePane\" title=\"");
                translated.append(
                    sectionTitle.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;"));
                translated.append("\" open=\"");
                translated.append(sectionOpen);
                translated.append("\">\n");
                translated.append(body);
                translated.append("</div></div>");
                pos = m.end();
            }
            if (pos > 0)
            {
                translated.append(rawContent.substring(pos));
                rawContent = translated.toString();
                useModule = false;
            }
        }

        if (nestedFound)
        {
            rawContent =
                rawContent.replaceAll("<(/?)nesteddiv", "<$1div");
        }
        return rawContent;
    }


    // ----------------------------------------------------------
    private String substituteRelativeUrls(String rawContent, String newBase)
    {
        if (!newBase.endsWith("/"))
        {
            newBase += "/";
        }
        rawContent = rawContent.replaceAll(
            "(<a\\s([^<>]*\\s)?href\\s*=\\s*[\'\"])(?!(\\w+:)?/)",
            "$1" + newBase);
        rawContent = rawContent.replaceAll(
            "(<img\\s([^<>]*\\s)?src\\s*=\\s*[\'\"])(?!(\\w+:)?/)",
            "$1" + newBase);
        return rawContent;
    }


    //~ Instance/static variables .............................................

    private File   file;
    private SubmissionResult submissionResult;
    private String content;
    private boolean useModule = false;

    static final Pattern MODULE = Pattern.compile(
        "<h2 class=\"collapsible\"><a[^>]*><img[^>]*(collapsed|expanded)[^>]*>"
        + "((.(?!</a>))*.)</a>\\s*</h2>\\s*"
        + "<div[^>]*\\sclass=\"expboxcontent\"[^>]*>((.(?!</div>))*.)</div>"
        ,
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    static final Pattern NESTED_MODULE = Pattern.compile(
        "<h3 class=\"collapsible\"><a[^>]*><img[^>]*(collapsed|expanded)[^>]*>"
        + "((.(?!</a>))*.)</a>\\s*</h3>\\s*"
        + "<div[^>]*\\sclass=\"expboxcontent\"[^>]*>((.(?!</div>))*.)</div>"
        ,
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    static Logger log = Logger.getLogger( PartialInlineReport.class );
}
