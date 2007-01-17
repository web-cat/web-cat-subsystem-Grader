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

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import java.io.File;
import java.io.FileInputStream;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * A page for inlining an HTML fragment stored in a file.  The
 * file property should be bound to a File object referring to the
 * fragment to include.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class PartialInlineReport
    extends WOComponent
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


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     * 
     * @param response The response being built
     * @param context  The context of the request
     */
    public void appendToResponse( WOResponse response, WOContext context )
    {
        NSData d = null;
        if ( file != null )
        {
            // Only read the file if it is really there, of course
            if ( file.exists() )
            {
                try
                {
                    FileInputStream stream = new FileInputStream( file );
                    d = new NSData( stream, (int)file.length() );
                    stream.close();
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
        if ( d != null )
        {
            response.appendContentData( d );
        }
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


    //~ Instance/static variables .............................................

    private File file;

    static Logger log = Logger.getLogger( PartialInlineReport.class );
}
