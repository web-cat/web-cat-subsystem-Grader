/*==========================================================================*\
 |  $Id: MiniBarGraph.java,v 1.4 2011/10/25 15:32:06 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2011 Virginia Tech
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

import com.webobjects.appserver.*;
import com.webobjects.appserver._private.WODynamicElementCreationException;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSNumberFormatter;
import org.webcat.core.*;
import org.webcat.woextensions.DynamicElement;
import org.webcat.woextensions.WCResourceManager;

// -------------------------------------------------------------------------
/**
 * A dynamic element used to generate tiny bar graphs.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.4 $, $Date: 2011/10/25 15:32:06 $
 */
public class MiniBarGraph
    extends DynamicElement
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Construct a new object.
     * @param name         the component instance's name
     * @param associations this instance's parameter bindings
     * @param children     this instance's child elements
     */
    public MiniBarGraph(
        String name,
        NSDictionary<String, WOAssociation> associations,
        WOElement children )
    {
        super( name, associations, children );

        // Validate bindings
        if ( !hasAssociationForKey( GRAPH_SUMMARY_KEY ) )
        {
            throw new WODynamicElementCreationException(
                "<" + getClass().getName()
                + "> Must specify '" + GRAPH_SUMMARY_KEY + "'" );
        }
    }


    //~ Constants (for key names) .............................................

    public static final String GRAPH_SUMMARY_KEY = "graphSummary";
    public static final String MARK_SCORE_KEY    = "markScore";
    public static final String MAX_VALUE_KEY     = "maxValue";


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Add the HTML rendering of the graph.
     * @param aResponse the response being built
     * @param aContext  the request context
     */
    public void appendToResponse( WOResponse aResponse, WOContext aContext )
    {
        WOComponent c = aContext.component();

        aResponse.appendContentString( "<table class=\"distchart\"><tr>\n" );
        float[] values = dataValues( c );
        float scaleFactor = scaleFactor( c );
        float largest = 0.0f;
        for ( int i = 0; i < values.length; i++ )
        {
            if ( scaleFactor != 0.0f && scaleFactor != 1.0f )
            {
                values[i] /= scaleFactor;
            }
            if ( values[i] > largest )
            {
                largest = values[i];
            }
        }

        float zoomFactor = 1.0f;
        if ( largest < 0.1 )      { zoomFactor = 5.0f; }
        else if ( largest < 0.2 ) { zoomFactor = 4.0f; }
        else if ( largest < 0.3 ) { zoomFactor = 3.0f; }
        else if ( largest < 0.4 ) { zoomFactor = 2.0f; }
        else if ( largest < 0.6 ) { zoomFactor = 1.5f; }

        Object markScoreObject =
            associationValueForKey( MARK_SCORE_KEY, c );
        if ( markScoreObject != null && !( markScoreObject instanceof Number ) )
        {
            throw new WODynamicElementCreationException(
                "<" + getClass().getName()
                + "> '" + MARK_SCORE_KEY + "' must be bound to a number" );
        }
        Number markScoreNumber = (Number)markScoreObject;
        float markScore  = 0.0f;
        int   markBin    = 0;
        int   markOffset = 0;
        if ( markScoreNumber != null )
        {
            AssignmentSummary summary =
                (AssignmentSummary)associationValueForKey(
                    GRAPH_SUMMARY_KEY, c );
            markScore = markScoreNumber.floatValue();
            markBin = summary.binFor( markScore );
            markOffset = summary.interpolateInBin( markScore, markBin, 10 );
        }

        for ( int i = 0; i < values.length; i++ )
        {
            int height = Math.round( values[i] * zoomFactor * 10.0f );
            aResponse.appendContentString(
                "<td class=\"b" + height + "\">");
            if ( markScoreNumber != null && markBin == i )
            {
                StringBuffer buf = new StringBuffer();
                buf.append( "Your score: " );
                formatter.format( markScoreNumber, buf, null );
                String msg =  buf.toString();
                aResponse.appendContentString(
                    "<img class=\"bar\" src=\"" + markerUrlPrefix()
                    + markOffset + ".gif\" title=\"" + msg + "\" alt=\""
                    + msg + "\"/>");
            }
            aResponse.appendContentString( "</td>\n" );
        }
        aResponse.appendContentString( "</tr></table>" );
    }


    // ----------------------------------------------------------
    /**
     * Get the array of values to be plotted.
     * @param aComponent the current component
     * @return the array of data values
     */
    public float[] dataValues( WOComponent aComponent )
    {
        AssignmentSummary summary = (AssignmentSummary)associationValueForKey(
            GRAPH_SUMMARY_KEY, aComponent );
        return summary.percentageDistribution();
    }


    // ----------------------------------------------------------
    /**
     * Get the array of values to be plotted.
     * @param aComponent the current component
     * @return the array of data values
     */
    public float scaleFactor( WOComponent aComponent )
    {
        return associationFloatValueForKey( MAX_VALUE_KEY, aComponent );
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    private String markerUrlPrefix()
    {
        if ( _markerUrlPrefix == null )
        {
            _markerUrlPrefix = WCResourceManager.versionlessResourceURLFor(
                "images/marker0.gif", "Grader", null, null );
            if ( _markerUrlPrefix != null )
            {
                _markerUrlPrefix = _markerUrlPrefix.substring( 0,
                    _markerUrlPrefix.length() - "0.gif".length() );
            }
            else
            {
                _markerUrlPrefix = "http://web-cat.cs.vt.edu/wcstatic/"
                    + "Grader.framework/WebServerResources/images/marker";
            }
        }
        return _markerUrlPrefix;
    }


    //~ Instance/static variables .............................................

    private static String _markerUrlPrefix;
    private static NSNumberFormatter formatter = new NSNumberFormatter("0.0");
}
