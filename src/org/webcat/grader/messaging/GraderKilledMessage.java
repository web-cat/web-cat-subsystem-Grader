/*==========================================================================*\
 |  $Id: GraderKilledMessage.java,v 1.3 2011/12/25 21:11:41 stedwar2 Exp $
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
import org.webcat.core.messaging.Message;
import org.webcat.core.messaging.UnexpectedExceptionMessage;

//-------------------------------------------------------------------------
/**
 * A message that is sent to the system administrator when a severe error
 * causes the entire grader to be killed.
 *
 * This is a separate subclass of UnexpectedExceptionMessage so that it can be
 * configured independently from other exception notifications if desired.
 *
 * @author  Tony Allevato
 * @author  Last changed by: $Author: stedwar2 $
 * @version $Revision: 1.3 $ $Date: 2011/12/25 21:11:41 $
 */
public class GraderKilledMessage
    extends UnexpectedExceptionMessage
{
    //~ Constructors ..........................................................

    public GraderKilledMessage(Throwable e)
    {
        super(e, null, null, "Grader job queue processing halted.");
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Called by the subsystem init() to register the message.
     */
    public static void register()
    {
        Message.registerMessage(
            GraderKilledMessage.class,
            "Grader",
            "Grader Killed",
            false,
            User.WEBCAT_RW_PRIVILEGES);
    }
}
