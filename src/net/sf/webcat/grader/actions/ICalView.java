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

package net.sf.webcat.grader.actions;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import org.apache.log4j.Logger;
import net.sf.webcat.grader.*;

//-------------------------------------------------------------------------
/**
 * This page generates an ical-compatible list of assignment due dates.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class ICalView
    extends BlueJSubmitterDefinitions
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new ICalView page.
     *
     * @param context The context for this page
     */
    public ICalView( WOContext context )
    {
        super( context );
    }


    //~ Methods ...............................................................

    // TODO: add customized, assignment-specific URLs
    // TODO: add URLs in My Profile preferences
    // TODO: add UIDs
    // TODO: add filtering for list of courses and list of CRNs

    // Was using this template for events:
    // ------------------------
//    BEGIN:VEVENT
//    DTSTART:<wo:str value="$startTime" formatter="$formatter"/>
//    DTEND:<wo:str value="$anAssignmentOffering.dueDate" formatter="$formatter"/>
//    SUMMARY:<wo:str value="$anAssignmentOffering.courseOffering.course.deptNumber"/>: <wo:str value="$assignmentName"/>
//    URL:<wo:str value="$application.properties.base.url"/>
//    END:VEVENT


    // ----------------------------------------------------------
    public String mimeType()
    {
        return "text/plain"; // "text/calendar";
    }


    // ----------------------------------------------------------
    public int submitterEngine()
    {
        return 0;
    }


    // ----------------------------------------------------------
    public String submitURLParams()
    {
        String result = "?page=UploadSubmission&";
        if ( groupByCRN )
        {
            result += AssignmentOffering.ID_FORM_KEY + "="
                + anAssignmentOffering.id();
        }
        else
        {
            result += Assignment.ID_FORM_KEY + "="
                + anAssignmentOffering.assignment().id();
        }
        return result;
    }


    // ----------------------------------------------------------
    public NSTimestamp startTime()
    {
        return new NSTimestamp( anAssignmentOffering.dueDate().getTime()
            - 1000 * 60 * 60 );  // subtract one hour
    }


    // ----------------------------------------------------------
    public NSTimestampFormatter formatter()
    {
        if ( formatter == null )
        {
            // This field is initialized lazily, since its time zone
            // settings cannot be initialized in its constructor.
            formatter = new NSTimestampFormatter( "%Y%m%dT%H%M%SZ" );
            formatter.setDefaultFormatTimeZone( NSTimeZone.getGMT() );
            formatter.setDefaultParseTimeZone( NSTimeZone.getGMT() );
        }
        return formatter;
    }


    //~ Instance/static variables .............................................

    private static NSTimestampFormatter formatter;
}