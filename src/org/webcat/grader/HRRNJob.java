package org.webcat.grader;
/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2011 Virginia Tech
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

//-------------------------------------------------------------------------
/**
 * TODO: describe this type.
 *
 * @author  edwards
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public interface HRRNJob
    extends Runnable
{
    // ----------------------------------------------------------
    /**
     * An meta-level priority for this job, where smaller values are
     * treated as coming before higher values (natural ordering).
     * This is used for coarse-grained prioritizing of different categories
     * of jobs.
     * @return This job's priority.
     */
    public byte priority();


    // ----------------------------------------------------------
    /**
     * Given the current time as a long, calculate the response ratio for
     * this job, based on its stored enqueue time and estimated execution time.
     * @param time The current time (in NSTimestamp format)
     * @return This job's response ratio.
     */
    public double responseRatio(long time);
}
