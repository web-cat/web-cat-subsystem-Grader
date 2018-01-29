/*==========================================================================*\
 |  $Id: FullPrintableReport.java,v 1.5 2014/06/16 17:31:07 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2011 Virginia Tech
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
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.foundation.ERXArrayUtilities;
import org.webcat.core.*;
import org.webcat.core.messaging.UnexpectedExceptionMessage;
import org.webcat.woextensions.WCEC;

// -------------------------------------------------------------------------
/**
 * Present a complete, printable view of all feedback about this submission.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.5 $, $Date: 2014/06/16 17:31:07 $
 */
public class FullPrintableReport
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public FullPrintableReport( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public SubmissionResult result;
    public Pair             stats;
    public int              index;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Get the long response task that computes this page's work.
     * @return The task
     */
    public LongResponseTaskWithProgress longResponse()
    {
        if ( task == null )
        {
            task = new LongResponseTask( user(), result, context() );
        }
        return task;
    }


    // ----------------------------------------------------------
    /**
     * Get the long response task that computes this page's work.
     * @return The task
     */
    public NSMutableArray<Pair> formattedFiles()
    {
        if ( formattedFiles == null )
        {
            formattedFiles = new NSMutableArray<Pair>();
            Pair[] rawPairs = (Pair[])task.result();
            if (rawPairs != null && rawPairs.length > 0)
            {
                EOEditingContext taskContext =
                    rawPairs[0].file.editingContext();
                try
                {
                    taskContext.lock();
                    for (int i = 0; i < rawPairs.length; i++)
                    {
                        Pair pair = new Pair();
                        formattedFiles.addObject(pair);
                        pair.file = rawPairs[i].file
                            .localInstance(localContext());
                        pair.html = rawPairs[i].html;
                    }
                }
                finally
                {
                    taskContext.unlock();
                }
            }
            task.resultNoLongerNeeded();
        }
        return formattedFiles;
    }


    // ----------------------------------------------------------
    public boolean hasTAComments()
    {
        return result.status() == Status.CHECK
            && result.comments() != null
            && !result.comments().equals( "" );
    }


    // ----------------------------------------------------------
    public Boolean showCoverageData()
    {
        if ( showCoverageData == null )
        {
            showCoverageData = Boolean.valueOf( result.hasCoverageData() );
        }
        return showCoverageData;
    }


    // ----------------------------------------------------------
    public String coverageMeter()
    {
        return FinalReportPage.meter( ( (double)stats.file.elementsCovered() ) /
                      ( (double)stats.file.elements() ) );
    }


    //~ Long response task ....................................................

    // ----------------------------------------------------------
    /**
     * A pair contains a {@link SubmissionFileStats} object together
     * with a <code>String</code> that contains its rendered code
     * with comments.  This class is used to build up the return result
     * for the {@link LongResponseTask}.
     */
    public static class Pair
    {
        /** The stats object. */
        public SubmissionFileStats file;
        /** The corresponding code with comments rendered in HTML. */
        public String              html;
    }

    // ----------------------------------------------------------
    /**
     * Encapsulates all the work needed to generate data for this page's
     * response.  The return value produced by performAction() is an
     * array of {@link Pair}s.
     */
    public static class LongResponseTask
        extends InterpolatingLongResponseTask
    {

        // ----------------------------------------------------------
        public LongResponseTask(
            User viewer, SubmissionResult theResult, WOContext context)
        {
            this.context = context;
            // Create a local EC, transfer the result into it, and
            // store both locally
            ec = WCEC.newEditingContext();
            try
            {
                ec.lock();
                submissionResult = theResult.localInstance( ec );
                user = viewer.localInstance( ec );
            }
            finally
            {
                ec.unlock();
            }
        }


        // ----------------------------------------------------------
        protected Object setUpTask()
        {
            setUnweightedNumberOfSteps( 1 );
            Pair[] pairs = null;
            try
            {
                ec.lock();
                NSArray<SubmissionFileStats> files =
                    ERXArrayUtilities.sortedArraySortedWithKey(
                        submissionResult.submissionFileStats(),
                        SubmissionFileStats.SOURCE_FILE_NAME_KEY );
                if ( files.count() > 0 )
                {
                    int[] weights = new int[files.count()];
                    pairs = new Pair[files.count()];
                    setUnweightedNumberOfSteps( files.count() );

                    for ( int i = 0; i < files.count(); i++ )
                    {
                        SubmissionFileStats file = files.objectAtIndex(i);
                        pairs[i] = new Pair();
                        pairs[i].file = file;
                        int lines = file.loc();
                        weights[i] = ( lines < 1 ) ? 100 : lines;
                    }

                    setStepWeights( weights );
                }
            }
            catch ( Exception e )
            {
                new UnexpectedExceptionMessage(e, context, null,
                    "Exception in setUpTask() preparing full printable report."
                    ).send();
            }
            finally
            {
                ec.unlock();
            }
            return pairs;
        }


        // ----------------------------------------------------------
        protected Object nextStep( int stepNumber, Object resultSoFar )
        {
            if ( resultSoFar == null ) return resultSoFar;
            Pair[] pairs = (Pair[])resultSoFar;
            if (pairs[stepNumber].file.canMarkupFile())
            {
                try
                {
                    ec.lock();
                    pairs[stepNumber].html = pairs[stepNumber].file
                        .codeWithComments( user, false, context.request() );
                }
                catch ( Exception e )
                {
                    pairs[stepNumber].html = "<p>Unexpected exception "
                        + "preparing HTML view of file: <span class=\"warn\">"
                        + e.getMessage() + "</span>.</p>";
                }
                finally
                {
                    ec.unlock();
                }
            }
            return pairs;
        }


        // ----------------------------------------------------------
        public void resultNoLongerNeeded()
        {
            submissionResult = null;
            user = null;
            if (ec != null)
            {
                ec.dispose();
            }
            ec = null;
        }


        //~ Instance/static variables .........................................

        private EOEditingContext ec;
        private SubmissionResult submissionResult;
        private User             user;
        private WOContext        context;
    }


    //~ Instance/static variables .............................................

    private LongResponseTaskWithProgress task;
    private NSMutableArray<Pair>         formattedFiles   = null;
    private Boolean                      showCoverageData = null;
}
