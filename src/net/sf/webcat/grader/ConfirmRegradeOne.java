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

import net.sf.webcat.core.*;


// -------------------------------------------------------------------------
/**
 * A confirmation page for regrading all student submissions.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class ConfirmRegradeOne
    extends ConfirmPage
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     * 
     * @param context The page's context
     */
    public ConfirmRegradeOne( WOContext context )
    {
        super( context );
        message = "<p>This action will <b>regrade this submission</b> "
            + "for the selected student.</p>"
            + "<p>This will also <b>delete all prior results</b> for the "
            + "submission, <b>delete all partner associations</b> for the "
            + "submission, and <b>delete all TA comments and "
            + "scoring</b> that have been recorded for the submission.</p>"
            + "<p>This submission will be "
            + "re-queued for grading, and the student will receive an e-mail "
            + "message when new results are available.</p>";
    }

    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void actionOnOK()
    {
        wcSession().commitLocalChanges();
        Submission submission = prefs().submission();
        submission.requeueForGrading( wcSession().localContext() );
        prefs().setSubmission( null );
        wcSession().commitLocalChanges();
        ( (Grader)( ( (Application)Application.application() )
                    .subsystemManager()
                    .subsystem( Grader.class.getName() ) ) )
            .graderQueue().enqueue( null );
        // Skip return to the grading results page
    }
}
