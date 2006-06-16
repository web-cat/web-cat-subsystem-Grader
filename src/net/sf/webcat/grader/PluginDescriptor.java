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

package net.sf.webcat.grader;

import java.util.*;
import org.apache.log4j.Logger;
import net.sf.webcat.FeatureDescriptor;

// -------------------------------------------------------------------------
/**
 *  This class represents the key properties of an updatable grading plug-in.
 *  The key properties include its version, its provider, and where updates
 *  can be obtained on the web.
 *
 *  @author  stedwar2
 *  @version $Id$
 */
public class PluginDescriptor
    extends FeatureDescriptor
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new plug-in descriptor.  This constructor is protected,
     * since clients should use the {@link ScriptFile.descriptor()}
     * method instead.
     * @param plugin the plug-in this descriptor is for
     */
    protected PluginDescriptor( ScriptFile plugin )
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
            plugin.configDescription().valueForKey( name + "." + propName );
        return result == null
            ? null
            : result.toString();
    }


    //~ Protected Methods .....................................................

    // ----------------------------------------------------------
    /**
     * Access a given property and convert it to a numeric value (with a
     * default of zero).
     * @param propName the name of the property to look up
     * @return the property's value as an int
     */
    protected int intProperty( String propName )
    {
        return er.extensions.ERXValueUtilities.intValue(
            plugin.configDescription().valueForKey( propName ) );
    }


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

    protected ScriptFile plugin;

    static Logger log = Logger.getLogger( PluginDescriptor.class );
}
