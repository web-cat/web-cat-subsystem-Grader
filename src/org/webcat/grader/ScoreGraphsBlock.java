/*==========================================================================*\
 |  $Id: ScoreGraphsBlock.java,v 1.2 2010/09/27 04:23:20 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2010 Virginia Tech
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

import com.webobjects.appserver.*;
import er.extensions.foundation.ERXValueUtilities;
import org.apache.log4j.Logger;
import org.webcat.grader.graphs.*;

// -------------------------------------------------------------------------
/**
 *  Renders a descriptive table containing a submission result's basic
 *  identifying information.  An optional submission file stats object,
 *  if present, will be used to present file-specific data.
 *
 *  @author  Stephen Edwards
 *  @author  Latest changes by: $Author: stedwar2 $
 *  @version $Revision: 1.2 $, $Date: 2010/09/27 04:23:20 $
 */
public class ScoreGraphsBlock
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public ScoreGraphsBlock( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public Submission       submission;
    public SubmissionResult result;
    public int              rowNumber;

    public static final String showGraphsKey = "FinalReportShowGraphs";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        rowNumber = 0;
        result = submission.result();
        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public WOComponent toggleShowGraphs()
    {
        boolean showGraphs = ERXValueUtilities.booleanValue(
            user().preferences().objectForKey( showGraphsKey ) );
        log.debug( "toggleShowGraphs: was " + showGraphs );
        showGraphs = !showGraphs;
        user().preferences().setObjectForKey(
            Boolean.valueOf( showGraphs ), showGraphsKey );
        user().savePreferences();
        return context().page();
    }


    // ----------------------------------------------------------
    public SubmissionResultDataset correctnessToolsDataset()
    {
        if ( correctnessToolsDataset == null )
        {
            correctnessToolsDataset = new SubmissionResultDataset(
                SubmissionResult.resultsForAssignmentAndUser(
                    localContext(),
                    submission.assignmentOffering(),
                    result.submission().user() ),
                SubmissionResultDataset.TESTING_AND_STATIC_TOOLS_SCORE_SERIES );
        }
        return correctnessToolsDataset;
    }


    //~ Instance/static variables .............................................

    private SubmissionResultDataset correctnessToolsDataset;

    static Logger log = Logger.getLogger( ScoreGraphsBlock.class );
}
