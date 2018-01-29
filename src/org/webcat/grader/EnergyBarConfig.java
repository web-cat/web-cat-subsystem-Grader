/*==========================================================================*\
 |  $Id$
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

import org.webcat.woextensions.ECAction;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;

// -------------------------------------------------------------------------
/**
 * TODO: place a real description here.
 *
 * @author
 * @author  Last changed by: $Author$
 * @version $Revision$, $Date$
 */
public class EnergyBarConfig
    extends _EnergyBarConfig
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new EnergyBarConfig object.
     */
    public EnergyBarConfig()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public String userPresentableDescription()
    {
        String desc = numSlots() + " units, ";
        if (rechargeTime() % 60 == 0)
        {
            desc += Integer.toString(rechargeTime() / 60);
        }
        else
        {
            double min = rechargeTime() / 60.0;
            int digits = (rechargeTime() % 6 == 0) ? 1 : 2;
            desc += String.format("%." + digits + "f", min);
        }
        return desc + " minutes each";
    }


    // ----------------------------------------------------------
    public static NSArray<EnergyBarConfig> allConfigs(EOEditingContext context)
    {
        return allObjects(context);
    }


    // ----------------------------------------------------------
    public static void ensureDefaultConfig()
    {
        new ECAction()
        {
            public void action()
            {
                NSArray<EnergyBarConfig> configs =
                    EnergyBarConfig.allObjects(this.ec);
                if (configs.count() == 0)
                {
                    EnergyBarConfig c = EnergyBarConfig.create(ec, true, 3);
                    c.setRechargeTime(3600);
                    ec.saveChanges();
                }
            }
        }.run();
    }
}
