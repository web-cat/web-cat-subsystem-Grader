/*==========================================================================*\
 |  $Id: SubmissionBatchHandler.java,v 1.3 2013/08/11 02:04:48 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2008 Virginia Tech
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

import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.webcat.core.WCProperties;

//-------------------------------------------------------------------------
/**
 * A batch handler for Submissions. Adds the following properties to the batch:
 *
 * <dl>
 * <dt>submissionPath</dt>
 * <dd>The absolute path to the file that the student submitted.</dd>
 * <dt>gradingPropertiesPath</dt>
 * <dd>The absolute path to the grading.properties file for the submission.</dd>
 * <dt>isSubmissionForGrading</dt>
 * <dd>True if this submission is the "submission for grading".</dd>
 * <dt>hasResults</dt>
 * <dd>True if this submission was graded and has results.</dd>
 * </dl>
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.3 $, $Date: 2013/08/11 02:04:48 $
 */
public class SubmissionBatchHandler
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public SubmissionBatchHandler(WCProperties properties, File workingDir)
    {
        this.properties = properties;
        this.workingDir = workingDir;
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void setUpItem(Submission submission)
    {
        File propertiesFile = submission.gradingPropertiesFile();
        SubmissionResult result = submission.result();

        if (result == null)
        {
            try
            {
                submission.createInitialGradingPropertiesFile();
            }
            catch (IOException e)
            {
                log.error("Could not create grading.properties file", e);
            }
        }

        properties.setProperty("gradingPropertiesPath",
                propertiesFile.getAbsolutePath());
        properties.setProperty("submissionPath",
                submission.file().getAbsolutePath());
        properties.setProperty("submissionUserName",
            submission.user().userName());
        properties.setProperty("isSubmissionForGrading",
                Boolean.toString(submission.isSubmissionForGrading()));
        properties.setProperty("hasResults",
                Boolean.toString(result != null));

        if (result != null)
        {
            // Add other properties.
        }
    }


    // ----------------------------------------------------------
    public void tearDownItem(Submission submission)
    {
        // Remove result properties.
    }


    //~ Static/instance variables .............................................

    private WCProperties properties;

    @SuppressWarnings("unused")
    private File workingDir;

    private static final Logger log = Logger.getLogger(
            SubmissionBatchHandler.class);
}
