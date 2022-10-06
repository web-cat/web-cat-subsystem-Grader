/*==========================================================================*\
 |  Copyright (C) 2020-2021 Virginia Tech
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

import org.webcat.core.User;
import com.webobjects.eoaccess.EOAdaptorChannel;
import com.webobjects.eoaccess.EOAdaptorOperation;
import com.webobjects.eoaccess.EOGeneralAdaptorException;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSTimestamp;

// -------------------------------------------------------------------------
/**
 * TODO: place a real description here.
 *
 * @author
 */
public class PageViewLog
    extends _PageViewLog
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new PageViewLog object.
     */
    public PageViewLog()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public static PageViewLog log(
        EOEditingContext ec,
        String pageName,
        User viewer)
    {
        return log(ec, pageName, viewer, null);
    }


    // ----------------------------------------------------------
    public static PageViewLog log(
        EOEditingContext ec,
        String pageName,
        User viewer,
        String extra)
    {
        return log(ec, pageName, viewer, null, null, extra);
    }


    // ----------------------------------------------------------
    public static PageViewLog log(
        EOEditingContext ec,
        String pageName,
        User viewer,
        Submission sub,
        SubmissionResult result,
        String extra)
    {
        if (viewer.editingContext() != ec)
        {
            viewer = viewer.localInstance(ec);
        }
        int tries = 3;
        PageViewLog view = null;
        while (tries-- > 0)
        {
            try
            {
                view = create(ec, pageName, new NSTimestamp(), viewer);
                if (sub != null)
                {
                    if (sub.editingContext() != ec)
                    {
                        sub = sub.localInstance(ec);
                    }
                    view.setSubmissionRelationship(sub);
                }
                if (result != null)
                {
                    if (result.editingContext() != ec)
                    {
                        result = result.localInstance(ec);
                    }
                    view.setSubmissionResultRelationship(result);
                }
                if (extra != null && !extra.isEmpty())
                {
                    view.setInfo(extra);
                }

                ec.saveChanges();
            }
            catch (EOGeneralAdaptorException e)
            {
                view = null;
                NSDictionary<?, ?> userInfo = e.userInfo();
                if (userInfo != null)
                {
                    EOAdaptorOperation op =
                        (EOAdaptorOperation)userInfo.objectForKey(
                            EOAdaptorChannel.FailedAdaptorOperationKey);

                    if (op.entity() != null
                        && ENTITY_NAME.equals(op.entity().name()))
                    {
                        // then retry, assuming this is an insert failure
                        // due to a duplicate key race condition
                    }
                    else
                    {
                        throw e;
                    }
                }
                else
                {
                    throw e;
                }
            }
        }
        if (view == null)
        {
            log.error("Unable to insert PageViewLog for " + pageName
                + ", "+ viewer + ", " + sub + ", " + extra);
        }
        return view;
    }
}
