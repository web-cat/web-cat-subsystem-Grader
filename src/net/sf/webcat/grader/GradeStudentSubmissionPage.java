/*==========================================================================*\
 |  $Id$
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

package net.sf.webcat.grader;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Allow the user to enter/edit "TA" comments for a submission.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class GradeStudentSubmissionPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public GradeStudentSubmissionPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public SubmissionResult    result;
    public WODisplayGroup      statsDisplayGroup;
    // For iterating over display group
    public SubmissionFileStats stats;
    public int                 index;

    /** true if submission file stats are recorded for this submission */
    public boolean hasFileStats = false;

    public String javascriptText = "<script type=\"text/javascript\">\n"
        + "var editor = null;\n"
        + "function initEditor() {\n"
        + "editor = new HTMLArea(\"source\");\n"
        + "editor.generate();\n"
        + "}\n"
        + "</script>\n";

    public NSArray formats = SubmissionResult.formats;
    public byte aFormat;

    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        if ( prefs().submission() == null )
        {
            throw new RuntimeException( "null submission selected" );
        }
        if ( prefs().submission().result() == null )
        {
            throw new RuntimeException( "null submission result" );
        }
        result = prefs().submission().result();
        if (log.isDebugEnabled())
        {
            log.debug( "result = " + result);
            if (result != null)
            {
                log.debug( "result = " + result.hashCode());
                log.debug( "result EC = " + result.editingContext().hashCode());
            }
        }
        hasFileStats = ( result.submissionFileStats().count() > 0 );
        statsDisplayGroup.setObjectArray( result.submissionFileStats() );
        showCoverageData = null;
        priorComments = result.comments();
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public void saveGrading()
    {
        String comments = result.comments();
        if ( comments != null
             && comments.trim().equals( "<br />" ) )
        {
            result.setComments( null );
            comments = null;
        }
        if (     result.status() == Status.TO_DO
             && ( result.taScoreRaw() != null
                  || comments != null ) )
        {
            result.setStatus( Status.UNFINISHED );
        }
        if ( comments != null
             && priorComments == null
             && comments.indexOf( "<" ) < 0
             && comments.indexOf( ">" ) < 0 )
        {
            result.setCommentFormat( SubmissionResult.FORMAT_TEXT );
        }
        if ( comments != null && !comments.equals( priorComments ) )
        {
            // update author info:
            String byLine = "-- last updated by " + user().name();
            if ( result.commentFormat() == SubmissionResult.FORMAT_HTML )
            {
                byLine = "<p><span style=\"font-size:smaller\"><i>"
                    + byLine + "</i></span></p>";
            }
            if ( log.isDebugEnabled() )
            {
                log.debug( "new comments ='" + comments + "'" );
                log.debug( "byline ='" + byLine + "'" );
            }
            if ( !comments.trim().endsWith( byLine ) )
            {
                log.debug( "byLine not found" );
                if ( result.commentFormat() == SubmissionResult.FORMAT_TEXT )
                {
                    byLine = "\n" + byLine + "\n";
                }
                if ( !( comments.endsWith( "\n")
                        || comments.endsWith( "\r" ) ) )
                {
                    byLine = "\n" + byLine;
                }
                result.setComments( comments + byLine );
            }
        }
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        saveGrading();
        return super.next();
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        log.debug( "defaultAction()" );
        log.debug( "form values = " + context().request().formValues() );
        return null;
    }


//    // ----------------------------------------------------------
//    public WOComponent finish()
//    {
//        log.debug( "finish()" );
//        return super.finish();
//    }
//
//
//    // ----------------------------------------------------------
//    public WOComponent apply()
//    {
//        log.debug( "apply()" );
//        return super.apply();
//    }
//
//
//    // ----------------------------------------------------------
//    public WOActionResults invokeAction( WORequest arg0, WOContext arg1 )
//    {
//        log.debug( "invokeAction(): request.formValues = " + arg0.formValues() );
//        log.debug( "invokeAction(): request = " + arg0 );
////         log.debug( "invokeAction(): context = " + arg1 );
//        log.debug( "invokeAction(): senderID = " + arg1.senderID() );
//        log.debug( "invokeAction(): elementID = " + arg1.elementID() );
//        wcSession().logExtraInfo( log, org.apache.log4j.Level.DEBUG, arg1 );
//        return super.invokeAction( arg0, arg1 );
//    }


    // ----------------------------------------------------------
    public boolean applyLocalChanges()
    {
        saveGrading();
        return super.applyLocalChanges();
    }


    // ----------------------------------------------------------
//    public WOComponent downloadSubmission()
//    {
//        saveGrading();
//        DeliverFile filePage =
//            (DeliverFile)pageWithName( DeliverFile.class.getName() );
//        filePage.setFileName( new java.io.File(
//            prefs().submission().resultDirName(),
//            "../" + prefs().submission().fileName() ) );
//        filePage.setContentType( "application/octet-stream" );
//        filePage.setStartDownload( true );
//        return nextPage;
//    }
//

    // ----------------------------------------------------------
    public WOComponent fileStatsDetails()
    {
        log.debug( "fileStatsDetails()" );
        prefs().setSubmissionFileStatsRelationship( stats );
        WCComponent statsPage =
            (WCComponent)pageWithName( EditFileCommentsPage.class.getName() );
        statsPage.nextPage = this;
        return statsPage;
    }


    // ----------------------------------------------------------
//    public WOComponent previewStudentFeedback()
//    {
//        saveGrading();
//        FinalReportPage reportPage =
//            (FinalReportPage)pageWithName( FinalReportPage.class.getName() );
//        reportPage.nextPage = this;
//        reportPage.showReturnToGrading = true;
//        return reportPage;
//    }


    // ----------------------------------------------------------
    public WOComponent selectSubmission()
    {
        saveGrading();
        PickSubmissionPage submissionPage =
            (PickSubmissionPage)pageWithName( PickSubmissionPage.class.getName() );
        submissionPage.nextPage = this;
        submissionPage.sideStepTitle = "Pick submission to grade";
        return submissionPage;
    }


    // ----------------------------------------------------------
    public String coverageMeter()
    {
        return FinalReportPage.meter( ( (double)stats.elementsCovered() ) /
                                      ( (double)stats.elements() ) );
    }


    // ----------------------------------------------------------
    public boolean gradingDone()
    {
        return result.status() == Status.CHECK;
    }


    // ----------------------------------------------------------
    public void setGradingDone( boolean done )
    {
        boolean canSet = done;
        if ( done )
        {
            // Not ready for this part yet
//            for ( int i = 0; i < statsDisplayGroup.allObjects().count(); i++ )
//            {
//                if ( ( (SubmissionFileStats) statsDisplayGroup
//                         .allObjects().objectAtIndex( i ) ).statusAsInt()
//                     != WCCoreTask.TASK_DONE )
//                {
//                    canSet = false;
//                    errorMessage = "Please finish commenting on all classes "
//                                   + "listed before marking this submission "
//                                   + "as completely graded.";
//                    break;
//                }
//            }
        }
        if ( canSet )
        {
            result.setStatus( Status.CHECK );
            com.webobjects.foundation.NSArray subs = result.submissions();
            for ( int i = 0; i < subs.count(); i++ )
            {
                Submission sub = (Submission)subs.objectAtIndex( i );
                sub.emailNotificationToStudent(
                    "has been updated by the course staff" );
            }
        }
        else
        {
            result.setStatus( Status.UNFINISHED );
        }
    }


    // ----------------------------------------------------------
    public WOComponent regradeActionOk()
    {
        if (!applyLocalChanges()) return null;
        Submission submission = prefs().submission();
        submission.requeueForGrading( localContext() );
        prefs().setSubmissionRelationship( null );
        applyLocalChanges();
        Grader.getInstance().graderQueue().enqueue( null );
        return back();
    }


    // ----------------------------------------------------------
    public WOComponent regrade()
    {
        ConfirmPage confirmPage = null;
        saveGrading();
        if ( !hasMessages() )
        {
            confirmPage =
                (ConfirmPage)pageWithName( ConfirmPage.class.getName() );
            confirmPage.nextPage       = this;
            confirmPage.message        =
                "This action will <b>regrade this submission</b> "
            + "for the selected student.</p>"
            + "<p>This will also <b>delete all prior results</b> for the "
            + "submission, <b>delete all partner associations</b> for the "
            + "submission, and <b>delete all TA comments and "
            + "scoring</b> that have been recorded for the submission.</p>"
            + "<p>This submission will be "
            + "re-queued for grading, and the student will receive an e-mail "
            + "message when new results are available.";
            confirmPage.actionReceiver = this;
            confirmPage.actionOk       = "regradeActionOk";
            confirmPage.setTitle( "Confirm Regrade of This Submission" );
        }
        return confirmPage;
    }


    // ----------------------------------------------------------
    public String formatLabel()
    {
        return (String)SubmissionResult.formatStrings.objectAtIndex( aFormat );
    }


    // ----------------------------------------------------------
    public Boolean showCoverageData()
    {
        if ( showCoverageData == null )
        {
            showCoverageData = Boolean.FALSE;
            if ( hasFileStats )
            {
                for ( int i = 0; i < statsDisplayGroup.allObjects().count();
                      i++ )
                {
                    SubmissionFileStats sfs = (SubmissionFileStats)
                        statsDisplayGroup.allObjects().objectAtIndex( i );
                    if ( sfs.elementsRaw() != null )
                    {
                        showCoverageData = Boolean.TRUE;
                        break;
                    }
                }
            }
        }
        return showCoverageData;
    }


    // ----------------------------------------------------------
    public WOComponent fullPrintableReport()
    {
        FullPrintableReport report = (FullPrintableReport)
            pageWithName( FullPrintableReport.class.getName() );
        report.result = result;
        report.nextPage = this;
        return report;
    }


    // ----------------------------------------------------------
    public Boolean showAutoGradedComments()
    {
        if (showAutoGradedComments == null)
        {
            if (result.submission().assignmentOffering().assignment()
                    .submissionProfile().toolPoints() > 0.0)
            {
                showAutoGradedComments = Boolean.TRUE;
            }
            else
            {
                showAutoGradedComments = Boolean.FALSE;
                for (int i = 0; i < result.submissionFileStats().count(); i++)
                {
                    SubmissionFileStats thisStats =
                        result.submissionFileStats().objectAtIndex(i);
                    if (thisStats.remarks() > 0)
                    {
                        showAutoGradedComments = Boolean.TRUE;
                        break;
                    }
                }
            }
        }
        return showAutoGradedComments;
    }


    //~ Instance/static variables .............................................

    private Boolean showCoverageData;
    private String priorComments;
    private Boolean showAutoGradedComments;
    static Logger log = Logger.getLogger( GradeStudentSubmissionPage.class );
}
