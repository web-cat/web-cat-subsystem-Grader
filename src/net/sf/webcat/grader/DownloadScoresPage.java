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

import com.Ostermiller.util.ExcelCSVPrinter;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.foundation.*;


import java.io.ByteArrayOutputStream;
import java.io.File;

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Allow the user to download grades for an assignment in spreadsheet form
 * as a CSV file.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class DownloadScoresPage
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public DownloadScoresPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    /** Value of the corresponding checkbox on the page. */
    public boolean omitStaff           = true;
    public boolean useBlackboardFormat;
    public boolean useMoodleFormat;
    public boolean useFullFormat;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        if ( prefs().assignmentOffering().moodleId() != null )
        {
            useBlackboardFormat = false;
            useMoodleFormat     = true;
            useFullFormat       = false;
        }
        else
        {
            useBlackboardFormat = true;
            useMoodleFormat     = false;
            useFullFormat       = false;
        }
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    private void print( ExcelCSVPrinter out, String field )
    {
        if ( field == null )
        {
            out.print( "" );
        }
        else
        {
            out.print( field );
        }
    }


    // ----------------------------------------------------------
    private void print( ExcelCSVPrinter out, Number field )
    {
        if ( field == null )
        {
            out.print( "" );
        }
        else
        {
            out.print( field.toString() );
        }
    }


    // ----------------------------------------------------------
    public byte[] exportAsWebCATCSV()
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream( 4096 );
        ExcelCSVPrinter out = new ExcelCSVPrinter( outBytes );

        // Basic header information
        out.print( "Course" );
        out.print( "" );
        out.println(
            wcSession().courseOffering().course().deptNumber() );
        out.print( "Semester" );
        out.print( "" );
        out.println( wcSession().courseOffering().semester().name() );
        out.print( "Assignment" );
        out.print( "" );
        out.println( prefs().assignmentOffering().assignment().name() );
        out.print( "Generated on" );
        out.print( "" );
        out.println( new NSTimestamp().toString() );
        out.println( "" );

        // Column titles
        out.print( "ID No." );
        out.print( "User" );
        out.print( "Last Name" );
        out.print( "First Name" );
        out.print( "Sub No." );
        out.print( "Time" );
        out.print( "Correctness" );
        out.print( "Style" );
        out.print( "Design" );
        out.print( "Penalty/Bonus" );
        out.println( "Total" );

        NSArray submissions = submissionsToExport;
        if ( submissions != null )
        {
            for ( int i = 0; i < submissions.count(); i++ )
            {
                Submission thisSubmission =
                    (Submission)submissions.objectAtIndex( i );
                User student = thisSubmission.user();
                print( out, student.universityIDNo() );
                print( out, student.userName() );
                print( out, student.lastName() );
                print( out, student.firstName() );

                log.debug( "submission found = "
                    + thisSubmission.submitNumber() );
                print( out, thisSubmission.submitNumberRaw() );
                print( out, thisSubmission.submitTime().toString() );
                SubmissionResult result = thisSubmission.result();
                print( out, result.correctnessScoreRaw() );
                print( out, result.toolScoreRaw() );
                print( out, result.taScoreRaw() );
                print( out, Double.toString(
                    result.earlyBonus() - result.latePenalty()
                ) );
                out.println( Double.toString( result.finalScore() ) );
            }
        }

        return outBytes.toByteArray();
    }


    // ----------------------------------------------------------
    public byte[] exportAsBlackboardCSV( boolean targetMoodle )
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream( 4096 );
        ExcelCSVPrinter out = new ExcelCSVPrinter( outBytes );

        // Basic header information
        if ( targetMoodle )
        {
            out.print( "username" );
            Number moodleAssignmentNo =
                prefs().assignmentOffering().moodleId();
            out.println( moodleAssignmentNo == null
                            ? "" : moodleAssignmentNo.toString() );
        }
        else
        {
            out.print( "PID" );
            String name = prefs().assignmentOffering().assignment().name();
            if ( name.startsWith( "Lab" ) )
            {
                name += " (Lab)";
            }
            out.println( name );
        }
        NSArray submissions = submissionsToExport;
        if ( submissions != null )
        {
            for ( int i = 0; i < submissions.count(); i++ )
            {
                Submission thisSubmission =
                    (Submission)submissions.objectAtIndex( i );
                print( out, thisSubmission.user().userName() );
                out.println( Double.toString(
                    thisSubmission.result().finalScore() ) );
            }
        }
        if ( !targetMoodle )
        {
            out.print( "Points Possible" );
            out.println( prefs().assignmentOffering().assignment()
                .submissionProfile().availablePointsRaw().toString() );
        }
        return outBytes.toByteArray();
    }


    // ----------------------------------------------------------
    private void collectSubmissionsToExport()
    {
        NSMutableArray students =
            wcSession().courseOffering().students().mutableClone();
        if ( omitStaff )
        {
            students.removeObjectsInArray(
                wcSession().courseOffering().instructors() );
            students.removeObjectsInArray(
                wcSession().courseOffering().TAs() );
        }
        else
        {
            er.extensions.ERXArrayUtilities
                .addObjectsFromArrayWithoutDuplicates(
                students,
                wcSession().courseOffering().instructors() );
            er.extensions.ERXArrayUtilities
                .addObjectsFromArrayWithoutDuplicates(
                students,
                wcSession().courseOffering().TAs() );
        }
        submissionsToExport = new NSMutableArray();
        if ( students != null )
        {
            for ( int i = 0; i < students.count(); i++ )
            {
                User student = (User)students.objectAtIndex( i );
                log.debug( "checking " + student.userName() );

                Submission thisSubmission = null;
                Submission gradedSubmission = null;
                // Find the submission
                NSArray thisSubmissionSet = EOUtilities.objectsMatchingValues(
                        wcSession().localContext(),
                        Submission.ENTITY_NAME,
                        new NSDictionary(
                            new Object[] {
                                student,
                                prefs().assignmentOffering()
                            },
                            new Object[] {
                                Submission.USER_KEY,
                                Submission.ASSIGNMENT_OFFERING_KEY
                            }
                        )
                    );
                log.debug( "searching for submissions" );
                for ( int j = 0; j < thisSubmissionSet.count(); j++ )
                {
                    Submission sub =
                        (Submission)thisSubmissionSet.objectAtIndex( j );
                    log.debug( "\tsub #" + sub.submitNumber() );
                    if ( sub.result() != null )
                    {
                        if ( thisSubmission == null )
                        {
                            thisSubmission = sub;
                        }
                        else if ( sub.submitNumberRaw() != null )
                        {
                            int num = sub.submitNumber();
                            if ( num > thisSubmission.submitNumber() )
                            {
                                thisSubmission = sub;
                            }
                        }
                        if ( sub.result().status() != Status.TO_DO )
                        {
                            if ( gradedSubmission == null )
                            {
                                gradedSubmission = sub;
                            }
                            else if ( sub.submitNumberRaw() != null )
                            {
                                int num = sub.submitNumber();
                                if ( num > gradedSubmission.submitNumber() )
                                {
                                    gradedSubmission = sub;
                                }
                            }
                        }
                    }
                }
                if ( gradedSubmission != null )
                {
                    thisSubmission = gradedSubmission;
                }
                if ( thisSubmission != null )
                {
                    log.debug( "\t saving for export = "
                               + thisSubmission.submitNumber() );
                    submissionsToExport.addObject( thisSubmission );
                }
                else
                {
                    log.debug( "no submission found" );
                }
            }
        }
    }


    // ----------------------------------------------------------
    public WOComponent downloadScoreFile()
    {
        collectSubmissionsToExport();
        byte[] rawData;
        if ( useFullFormat )
        {
            rawData = exportAsWebCATCSV();
        }
        else
        {
            rawData = exportAsBlackboardCSV( useMoodleFormat );
        }

        DeliverFile csvFile =
            (DeliverFile)pageWithName( DeliverFile.class.getName() );
        csvFile.setFileData( new NSData( rawData ) );
        csvFile.setFileName( new File( ""
           + wcSession().courseOffering().crnSubdirName()
           + "-"
           + prefs().assignmentOffering().assignment().subdirName()
           + ".csv" ) );
        csvFile.setContentType( "application/octet-stream" );
        csvFile.setStartDownload( true );
        return csvFile;
    }


    //~ Instance/static variables .............................................

    private NSMutableArray submissionsToExport;

    static Logger log = Logger.getLogger( DownloadScoresPage.class );
}
