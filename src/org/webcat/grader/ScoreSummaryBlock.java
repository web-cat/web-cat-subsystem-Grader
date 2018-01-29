/*==========================================================================*\
 |  $Id: ScoreSummaryBlock.java,v 1.6 2014/11/07 13:55:02 stedwar2 Exp $
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
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 *  Renders a descriptive table containing a submission result's basic
 *  identifying information.  An optional submission file stats object,
 *  if present, will be used to present file-specific data.
 *
 *  @author  Stephen Edwards
 *  @author  Latest changes by: $Author: stedwar2 $
 *  @version $Revision: 1.6 $, $Date: 2014/11/07 13:55:02 $
 */
public class ScoreSummaryBlock
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public ScoreSummaryBlock( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public boolean          allowScoreEdit   = false;
    public NSKeyValueCodingAdditions submission;
    public Scorable         result;
    public int              rowNumber;
    public boolean          includeGraph = true;

    public String taLabel = "Design/Readability";
    public String toolLabel = "Style/Coding";
    public String testingLabel = "Correctness/Testing";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        rowNumber = 0;
        if (submission != null)
        {
            result = (Scorable)submission.valueForKey(Submission.RESULT_KEY);
            super.beforeAppendToResponse(response, context);
        }
    }


    // ----------------------------------------------------------
    public boolean hasTAGrade()
    {
        return (Double)submission.valueForKeyPath(
            "assignmentOffering.submissionProfile.taPoints") > 0.0
            || (result != null
                && result.valueForKey("taScoreRaw") != null
                && (Double)result.valueForKey(SubmissionResult.TA_SCORE_KEY)
                    > 0.0)
            || allowScoreEdit;
    }


    // ----------------------------------------------------------
    public Object taScore()
    {
        return ((Byte)result.valueForKey(SubmissionResult.STATUS_KEY)
            == Status.CHECK)
            ? result.valueForKey("taScoreRaw")
            : null;
    }


    // ----------------------------------------------------------
    public String taMeter()
    {
        Number taPossibleNum = (Number)submission.valueForKeyPath(
            "assignmentOffering.submissionProfile.taPointsRaw");
        double taPossible = (taPossibleNum == null)
            ? 1.0 : taPossibleNum.doubleValue();
        Number taPtsNum = (Number)result.valueForKey("taScoreRaw");
        if (taPtsNum == null
            || (!allowScoreEdit
                && (Byte)result.valueForKey(SubmissionResult.STATUS_KEY)
                    != Status.CHECK))
        {
            return "&lt;Awaiting Staff&gt;";
        }
        double taPts = taPtsNum.doubleValue();
        return FinalReportPage.meter( taPts / taPossible );
    }


    // ----------------------------------------------------------
    public String toolMeter()
    {
        Number toolPossibleNum = (Number)submission.valueForKeyPath(
            "assignmentOffering.submissionProfile.toolPointsRaw");
        double toolPossible = (toolPossibleNum == null)
            ? 1.0 : toolPossibleNum.doubleValue();
        double toolPts =
            (Double)result.valueForKey(SubmissionResult.TOOL_SCORE_KEY);
        return FinalReportPage.meter(toolPts / toolPossible);
    }


    // ----------------------------------------------------------
    public String correctnessMeter()
    {
        double possible = (Double)submission.valueForKeyPath(
            "assignmentOffering.submissionProfile.correctnessPoints");
        double pts = (Double)result.valueForKey(
            SubmissionResult.CORRECTNESS_SCORE_KEY);
        return FinalReportPage.meter(pts / possible);
    }


    // ----------------------------------------------------------
    public String finalMeter()
    {
        double possible = (Double)submission.valueForKeyPath(
            "assignmentOffering.submissionProfile.availablePoints");
        double pts = result.finalScoreVisibleTo(user());
        return FinalReportPage.meter(pts / possible);
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger(ScoreSummaryBlock.class);
}
