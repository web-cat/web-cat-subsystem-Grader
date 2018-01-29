/*==========================================================================*\
 |  $Id: GradingResultsAvailableMessage.java,v 1.5 2011/12/25 21:11:41 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2010-2011 Virginia Tech
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
import org.webcat.core.messaging.SingleUserMessage;
import com.webobjects.foundation.NSDictionary;

//-------------------------------------------------------------------------
/**
 * A message that is sent to the owner of a submission when his or her grading
 * results are available.
 *
 * @author  Tony Allevato
 * @author  Last changed by: $Author: stedwar2 $
 * @version $Revision: 1.5 $ $Date: 2011/12/25 21:11:41 $
 */
public class GradingResultsAvailableMessage
    extends SingleUserMessage
{
    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    public GradingResultsAvailableMessage(User user, WCProperties properties)
    {
        super(user);
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
        return new NSDictionary<String, String>(
            properties.stringForKey("submission.result.link"),
            "View your feedback");
    }


    // ----------------------------------------------------------
    @Override
    public String title()
    {
        return properties.stringForKeyWithDefault(
            "submission.email.title",
            "[Grader] results available: #${submission.number}, "
            + "${assignment.title}");
    }


    //~ Static/instance variables .............................................

    private WCProperties properties;
}
