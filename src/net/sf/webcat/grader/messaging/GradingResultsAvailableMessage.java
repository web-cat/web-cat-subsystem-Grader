/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2009 Virginia Tech
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

package net.sf.webcat.grader.messaging;

import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;
import net.sf.webcat.core.User;
import net.sf.webcat.core.WCProperties;
import net.sf.webcat.core.messaging.Message;
import net.sf.webcat.core.messaging.UnexpectedExceptionMessage;

//-------------------------------------------------------------------------
/**
 * A message that is sent to the owner of a submission when his or her grading
 * results are available.
 *
 * @author Tony Allevato
 * @author  latest changes by: $Author$
 * @version $Revision$ $Date$
 */
public class GradingResultsAvailableMessage extends Message
{
    // ----------------------------------------------------------
    public GradingResultsAvailableMessage(User user, WCProperties properties)
    {
        this.user = user;
        this.properties = properties;
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Called by the subsystem init() to register the message.
     */
    public static void register()
    {
        Message.registerMessage(
                GradingResultsAvailableMessage.class,
                "Grader",
                "Grading Results Available",
                false,
                User.STUDENT_PRIVILEGES);
    }


    // ----------------------------------------------------------
    @Override
    public String fullBody()
    {
        return shortBody();
    }


    // ----------------------------------------------------------
    @Override
    public String shortBody()
    {
        return properties.stringForKeyWithDefault(
                "submission.email.body",
                "The feedback report for ${assignment.title}\n"
                + "submission number ${submission.number} ${message}.\n\n");
    }


    // ----------------------------------------------------------
    @Override
    public NSDictionary<String, String> links()
    {
        NSMutableDictionary<String, String> links =
            new NSMutableDictionary<String, String>();

        links.setObjectForKey(
                properties.stringForKey("${submission.result.link}"),
                "Click here to log in to Web-CAT and view the report");

        return links;
    }


    // ----------------------------------------------------------
    @Override
    public String title()
    {
        return properties.stringForKeyWithDefault("submission.email.title",
                "[Grader] results available: #${submission.number}, "
                + "${assignment.title}");
    }


    // ----------------------------------------------------------
    @Override
    public NSArray<User> users()
    {
        return new NSArray<User>(user);
    }


    //~ Static/instance variables .............................................

    private User user;
    private WCProperties properties;
}
