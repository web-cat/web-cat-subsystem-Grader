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
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.*;
import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Present a complete, printable view of all feedback about this submission.
 *
 * @author Stephen Edwards
 * @version $Id$
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
            task = new LongResponseTask( wcSession().user(), result );
        }
        return task;
    }


    // ----------------------------------------------------------
    /**
     * Get the long response task that computes this page's work.
     * @return The task
     */
    public NSMutableArray formattedFiles()
    {
        if ( formattedFiles == null )
        {
            formattedFiles = new NSMutableArray();
            Pair[] rawPairs = (Pair[])task.result();
            if ( rawPairs != null )
            {
                for (int i = 0; i < rawPairs.length; i++ )
                {
                    Pair pair = new Pair();
                    formattedFiles.addObject( pair );
                    pair.file = (SubmissionFileStats)EOUtilities
                        .localInstanceOfObject( wcSession().localContext(),
                            rawPairs[i].file );
                    pair.html = rawPairs[i].html;
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
    public class LongResponseTask
        extends InterpolatingLongResponseTask
    {
        
        // ----------------------------------------------------------
        public LongResponseTask( User viewer, SubmissionResult theResult )
        {
            // Create a local EC, transfer the result into it, and
            // store both locally
            ec = Application.newPeerEditingContext();
            try
            {
                ec.lock();
                submissionResult = (SubmissionResult)EOUtilities
                    .localInstanceOfObject( ec, theResult );
                user = (User)EOUtilities.localInstanceOfObject( ec, viewer );
            }
            finally
            {
                ec.unlock();
            }
        }

        
        // ----------------------------------------------------------
        protected Object setUpTask()
        {
            Pair[] pairs = null;
            try
            {                
                ec.lock();
                NSArray files = ERXArrayUtilities.sortedArraySortedWithKey(
                    submissionResult.submissionFileStats(),
                    SubmissionFileStats.SOURCE_FILE_NAME_KEY );
                if ( files.count() > 0 )
                {
                    int[] weights = new int[files.count()];
                    pairs = new Pair[files.count()];
                    setUnweightedNumberOfSteps( files.count() );

                    for ( int i = 0; i < files.count(); i++ )
                    {
                        SubmissionFileStats file =
                            (SubmissionFileStats)files.objectAtIndex( i );
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
                Application.emailExceptionToAdmins( e, context(),
                    "Exception in setUpTask() preparing full printable report."
                    );
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
            Pair[] pairs = (Pair[])resultSoFar;
            try
            {
                ec.lock();
                pairs[stepNumber].html = pairs[stepNumber].file
                    .codeWithComments( user, false );
            }
            catch ( Exception e )
            {
                pairs[stepNumber].html = "<p>Unexpected exception preparing "
                    + "HTML view of file: <span class=\"warn\">"
                    + e.getMessage() + "</span>.</p>";
            }
            finally
            {
                ec.unlock();
            }
            return pairs;
        }


        // ----------------------------------------------------------
        public void resultNoLongerNeeded()
        {
            submissionResult = null;
            user = null;
            if ( ec != null )
            {
                Application.releasePeerEditingContext( ec );
            }
            ec = null;
        }


        //~ Instance/static variables .........................................
        private EOEditingContext ec;
        private SubmissionResult submissionResult;
        private User             user;
    }


    //~ Instance/static variables .............................................

    private LongResponseTaskWithProgress task;
    private NSMutableArray formattedFiles   = null;
    private Boolean        showCoverageData = null;
}
