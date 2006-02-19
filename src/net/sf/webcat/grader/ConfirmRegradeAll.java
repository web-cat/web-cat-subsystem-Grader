/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006 Virginia Tech
 |
 |  This file is part of Web-CAT.
 |
 |  Web-CAT is free software; you can redistribute it and/or modify
 |  it under the terms of the GNU General Public License as published by
 |  the Free Software Foundation; either version 2 of the License, or
 |  (at your option) any later version.
 |
 |  Web-CAT is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU General Public License for more details.
 |
 |  You should have received a copy of the GNU General Public License
 |  along with Web-CAT; if not, write to the Free Software
 |  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 |
 |  Project manager: Stephen Edwards <edwards@cs.vt.edu>
 |  Virginia Tech CS Dept, 660 McBryde Hall (0106), Blacksburg, VA 24061 USA
\*==========================================================================*/

package net.sf.webcat.grader;

import com.webobjects.appserver.*;

// -------------------------------------------------------------------------
/**
 * A confirmation page for regrading all student submissions.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class ConfirmRegradeAll
    extends ConfirmPage
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     * 
     * @param context The page's context
     */
    public ConfirmRegradeAll( WOContext context )
    {
        super( context );
        message = "<p>This action will <b>regrade the most recent submission "
            + "for every student</b> who has submitted to this assignment.</p>"
            + "<p>This will also <b>delete all prior results</b> for the "
            + "submissions to be regraded and <b>delete all TA comments and "
            + "scoring</b> that have been recorded for the submissions to be "
            + "regraded.</p><p>Each student's most recent submission will be "
            + "re-queued for grading, and each student will receive an e-mail "
            + "message when their new results are available.</p>";
    }

    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void actionOnOK()
    {
        wcSession().commitLocalChanges();
        prefs().assignmentOffering().regradeMostRecentSubsForAll(
            wcSession().localContext() );
        wcSession().commitLocalChanges();
    }
}
