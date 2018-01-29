/*==========================================================================*\
 |  $Id: Scorable.java,v 1.1 2014/11/07 13:55:02 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2014 Virginia Tech
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
import com.webobjects.foundation.NSKeyValueCodingAdditions;

//-------------------------------------------------------------------------
/**
 * Represents the scorable result of a student submission.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.1 $, $Date: 2014/11/07 13:55:02 $
 */
public interface Scorable
    extends NSKeyValueCodingAdditions
{
    double finalScore();
    double finalScoreVisibleTo(User user);
}
