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

import com.webobjects.foundation.NSDictionary;

// -------------------------------------------------------------------------
/**
 * Represents an individual student extension to an assignment offering.
 *
 * @author Stephen Edwards (edwards@cs.vt.edu)
 */
public class StudentExtension
    extends _StudentExtension
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new StudentExtension object.
     */
    public StudentExtension()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public void willInsert()
    {
        super.willInsert();
        if (assignmentOffering() != null)
        {
            assignmentOffering().updateTimes(this);
        }
    }


    // ----------------------------------------------------------
    @Override
    public void willUpdate()
    {
        super.willUpdate();
        NSDictionary<String, Object> changes = changedProperties();
        if (changes.containsKey(OPENS_ON_KEY)
            || changes.containsKey(CLOSES_ON_KEY)
            || changes.containsKey(DUE_DATE_KEY)
            || changes.containsKey(ASSIGNMENT_OFFERING_KEY))
        {
            if (assignmentOffering() != null)
            {
                assignmentOffering().updateTimes(this);
            }
        }
    }


    // ----------------------------------------------------------
    @Override
    public void willDelete()
    {
        if (assignmentOffering() != null)
        {
            assignmentOffering().updateTimes(this);
        }
        super.willDelete();
    }
}
