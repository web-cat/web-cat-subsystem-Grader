/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2017-2018 Virginia Tech
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

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSTimeZone;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.foundation.NSTimestampFormatter;

//-------------------------------------------------------------------------
/**
 * TODO: describe this type.
 *
 * @author  edwards
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class EnergyBarDisplay
    extends WOComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The component's context
     */
    public EnergyBarDisplay(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public EnergyBar bar;
    public String cssClass;
    public int slotNo;
    public Boolean hasMissionContent;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse(WOResponse aResponse, WOContext aContext)
    {
        if (this.bar != null)
        {
            bar.reevaluateCharge();
        }
        super.appendToResponse(aResponse, aContext);
    }


    // ----------------------------------------------------------
    public String cssClass()
    {
        String result = "energy-bar-display";
        if (cssClass != null)
        {
            result = result + " " + cssClass;
        }
        return result;
    }


    // ----------------------------------------------------------
    @SuppressWarnings("deprecation")
    public NSTimestampFormatter timeFormatter()
    {
        NSTimestampFormatter f =
            new NSTimestampFormatter(bar.user().timeFormat());
        NSTimeZone zone = NSTimeZone.timeZoneWithName(
            bar.user().timeZoneName(), true);
        f.setDefaultFormatTimeZone(zone);
        f.setDefaultParseTimeZone(zone);
        return f;
    }


    // ----------------------------------------------------------
    public String slotCssClass()
    {
        String result = "energy-slot";
        if (bar.charge() <= slotNo)
        {
            result = result + " empty";
        }
        if (slotNo == 0)
        {
            result = result + " first";
        }
        else if (slotNo ==
            bar.assignmentOffering().energyBarConfig().numSlots() - 1)
        {
            result = result + " last";
        }
        return result;
    }


    // ----------------------------------------------------------
    public long now()
    {
        return System.currentTimeMillis();
    }


    // ----------------------------------------------------------
    public long slotExpiration()
    {
        if (bar.charge() > slotNo)
        {
            return 0L;
        }
        return bar.timeOfNextCharge().getTime()
            + (slotNo - bar.charge())
            * bar.assignmentOffering().energyBarConfig().rechargeTime() * 1000;
    }


    // ----------------------------------------------------------
    public boolean isCloseToDeadline()
    {
        return bar.isCloseToDeadline(new NSTimestamp());
    }
}
