/*==========================================================================*\
 |  $Id: PluginDescriptor.java,v 1.3 2010/09/27 04:23:20 stedwar2 Exp $
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

package org.webcat.grader;

import net.sf.webcat.FeatureDescriptor;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 *  This class represents the key properties of an updatable grading plug-in.
 *  The key properties include its version, its provider, and where updates
 *  can be obtained on the web.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.3 $, $Date: 2010/09/27 04:23:20 $
 */
public class PluginDescriptor
    extends FeatureDescriptor
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new plug-in descriptor.  This constructor is protected,
     * since clients should use the {@link GradingPlugin#descriptor()}
     * method instead.
     * @param plugin the plug-in this descriptor is for
     */
    protected PluginDescriptor( GradingPlugin plugin )
    {
        this.plugin = plugin;
        this.name = plugin.name();
        this.isPlugin = true;
    }


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Retrieve a subsystem-specific property's value.
     * @param propName the name of the property to retrieve
     * @return the value of the <i>name.propName</i> property, where
     *     <i>name</i> is the name of this subsystem
     */
    public String getProperty( String propName )
    {
        Object result =
            plugin.configDescription().valueForKey( propName );
        return result == null
            ? null
            : result.toString();
    }


    // ----------------------------------------------------------
    /**
     * Retrieve a subsystem-specific property's value.
     * @param propName the name of the property to retrieve
     * @param defaultValue the value to use if the property is not found
     * @return the value of the <i>name.propName</i> property, where
     *     <i>name</i> is the name of this subsystem, or the defaultValue
     *     if no such property is found
     */
    public String getProperty( String propName, String defaultValue )
    {
        Object result =
            plugin.configDescription().valueForKey( propName );
        return result == null
            ? defaultValue
            : result.toString();
    }


    //~ Protected Methods .....................................................

    // ----------------------------------------------------------
    /**
     * Log an informational message.  This implementation sends output
     * to <code>System.out</code>, but provides a hook so that subclasses
     * can use Log4J (we don't use that here, so that the Log4J library
     * can be dynamically updatable through subsystems).
     * @param msg the message to log
     */
    protected void logInfo( String msg )
    {
        log.info( msg );
    }


    // ----------------------------------------------------------
    /**
     * Log an error message.  This implementation sends output
     * to <code>System.out</code>, but provides a hook so that subclasses
     * can use Log4J (we don't use that here, so that the Log4J library
     * can be dynamically updatable through subsystems).
     * @param msg the message to log
     */
    protected void logError( String msg )
    {
        log.error( msg );
    }


    // ----------------------------------------------------------
    /**
     * Log an error message.  This implementation sends output
     * to <code>System.out</code>, but provides a hook so that subclasses
     * can use Log4J (we don't use that here, so that the Log4J library
     * can be dynamically updatable through subsystems).
     * @param msg the message to log
     * @param exception an optional exception that goes with the message
     */
    protected void logError( String msg, Throwable exception )
    {
        log.error( msg, exception );
    }


    //~ Instance/static variables .............................................

    protected GradingPlugin plugin;

    static Logger log = Logger.getLogger( PluginDescriptor.class );
}
