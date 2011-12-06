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

package org.webcat.grader.messaging;

import org.webcat.core.User;
import org.webcat.core.WCProperties;
import org.webcat.core.messaging.Message;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

//-------------------------------------------------------------------------
/**
 * A message that is sent to the owner of a submission when his or her grading
 * results are available.
 *
 * @author  Tony Allevato
 * @author  Latest changes by: $Author$
 * @version $Revision$ $Date$
 */
public class GradingResultsAvailableMessage extends Message
{
    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    public GradingResultsAvailableMessage(User user, WCProperties properties)
    {
        EOEditingContext ec = editingContext();
        try
        {
            ec.lock();
            this.user = user.localInstance(ec);
        }
        finally
        {
            ec.unlock();
        }
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
                properties.stringForKey("submission.result.link"),
                "View your feedback");

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
