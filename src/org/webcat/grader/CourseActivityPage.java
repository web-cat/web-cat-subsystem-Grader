/*==========================================================================*\
 |  $Id: CourseActivityPage.java,v 1.1 2012/01/29 00:29:01 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2012 Virginia Tech
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

import org.webcat.core.UsagePeriod;
import org.webcat.core.User;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimeZone;
import com.webobjects.foundation.NSTimestamp;
import er.extensions.appserver.ERXDisplayGroup;

//-------------------------------------------------------------------------
/**
 * Represents a standard Web-CAT page that has not yet been implemented
 * (is "to be defined").
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.1 $, $Date: 2012/01/29 00:29:01 $
 */
public class CourseActivityPage
    extends GraderCourseComponent
{
    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new object.
     *
     * @param context The context to use
     */
    public CourseActivityPage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public ERXDisplayGroup<User> participants;
    public User                  participant;
    public int                   index;

    public NSTimestamp           limitTime;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        if (coreSelections().courseOffering() != null)
        {
            participants.setMasterObject(coreSelections().courseOffering());
        }
        index = 0;
        cachedUser = null;
        periods = null;
        limitTime = new NSTimestamp()
            .timestampByAddingGregorianUnits(0, 0, -60, 0, 0, 0);
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public int periodCount()
    {
        int count = 0;
        if (participant != null)
        {
            if (participant != cachedUser)
            {
                periods = null;
                cachedUser = participant;
            }
            if (periods == null)
            {
                periods = UsagePeriod.objectsMatchingQualifier(localContext(),
                    UsagePeriod.user.is(participant).and(
                        UsagePeriod.startTime.after(limitTime)),
                    UsagePeriod.startTime.descs());
            }
            count = periods.count();
        }
        return count;
    }


    // ----------------------------------------------------------
    public UsagePeriod mostRecentPeriod()
    {
        if (periodCount() > 0)
        {
            return periods.get(0);
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    public String duration()
    {
        UsagePeriod period = mostRecentPeriod();
        return Submission.getStringTimeRepresentation(
            period.endTime().getTime() - period.startTime().getTime());
    }


    // ----------------------------------------------------------
    @SuppressWarnings("deprecation")
    public com.webobjects.foundation.NSTimestampFormatter dateFormatter()
    {
        if (dateFormatter == null)
        {
            dateFormatter = new com.webobjects.foundation.NSTimestampFormatter(
                user().dateFormat());
            NSTimeZone zone = NSTimeZone.timeZoneWithName(
                user().timeZoneName(), true);
            dateFormatter.setDefaultFormatTimeZone(zone);
            dateFormatter.setDefaultParseTimeZone(zone);
        }
        return dateFormatter;
    }


    //~ Instance/static variables .............................................

    private User cachedUser;
    private NSArray<UsagePeriod> periods;
    @SuppressWarnings("deprecation")
    private com.webobjects.foundation.NSTimestampFormatter dateFormatter;
}
