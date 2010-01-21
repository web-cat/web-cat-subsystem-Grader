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

import com.Ostermiller.util.ExcelCSVPrinter;
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import er.extensions.foundation.ERXArrayUtilities;
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
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
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
    public DownloadScoresPage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    /** Value of the corresponding checkbox on the page. */
    public boolean omitStaff = true;
    public boolean useBlackboardFormat;
    public boolean useMoodleFormat;
    public boolean useFullFormat;

    public AssignmentOffering assignmentOffering;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse(WOResponse response, WOContext context)
    {
        if (assignmentOffering == null)
        {
            assignmentOffering = prefs().assignmentOffering();
            if (assignmentOffering == null)
            {
                Assignment assignment = prefs().assignment();
                CourseOffering courseOffering =
                    coreSelections().courseOffering();
                assignmentOffering = AssignmentOffering
                    .firstObjectMatchingValues(
                        localContext(),
                        null,
                        AssignmentOffering.COURSE_OFFERING_KEY,
                        courseOffering,
                        AssignmentOffering.ASSIGNMENT_KEY,
                        assignment);
            }
        }

        if (assignmentOffering.moodleId() != null)
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
        super.appendToResponse(response, context);
    }


    // ----------------------------------------------------------
    private void print(ExcelCSVPrinter out, String field)
    {
        if (field == null)
        {
            out.print("");
        }
        else
        {
            out.print(field);
        }
    }


    // ----------------------------------------------------------
    private void print(ExcelCSVPrinter out, Number field)
    {
        if (field == null)
        {
            out.print("");
        }
        else
        {
            out.print(field.toString());
        }
    }


    // ----------------------------------------------------------
    public byte[] exportAsWebCATCSV()
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream(4096);
        ExcelCSVPrinter out = new ExcelCSVPrinter(outBytes);

        // Basic header information
        out.print("Course");
        out.print("");
        out.println(
            assignmentOffering.courseOffering().course().deptNumber());
        out.print("Semester");
        out.print("");
        out.println(assignmentOffering.courseOffering().semester().name());
        out.print("Assignment");
        out.print("");
        out.println(assignmentOffering.assignment().name());
        out.print("Generated on");
        out.print("");
        out.println(wcSession().timeFormatter().format(new NSTimestamp()));
        out.println("");

        // Column titles
        out.print("ID No.");
        out.print("User");
        out.print("Last Name");
        out.print("First Name");
        out.print("Sub No.");
        out.print("Time");
        out.print("Correctness");
        out.print("Style");
        out.print("Design");
        out.print("Penalty/Bonus");
        out.println("Total");

        NSArray<Submission> submissions = submissionsToExport;
        if (submissions != null)
        {
            for (Submission thisSubmission : submissions)
            {
                User student = thisSubmission.user();
                print(out, student.universityIDNo());
                print(out, student.userName());
                print(out, student.lastName());
                print(out, student.firstName());

                log.debug("submission found = "
                    + thisSubmission.submitNumber());
                print(out, thisSubmission.submitNumberRaw());
                print(out, thisSubmission.submitTime().toString());
                SubmissionResult result = thisSubmission.result();
                print(out, result.correctnessScoreRaw());
                print(out, result.toolScoreRaw());
                print(out, result.taScoreRaw());
                print(out, Double.toString(
                    result.earlyBonus() - result.latePenalty()));
                out.println(Double.toString(result.finalScore()));
            }
        }

        return outBytes.toByteArray();
    }


    // ----------------------------------------------------------
    public byte[] exportAsBlackboardCSV(boolean targetMoodle)
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream(4096);
        ExcelCSVPrinter out = new ExcelCSVPrinter(outBytes);

        // Basic header information
        if (targetMoodle)
        {
            out.print("username");
            Number moodleAssignmentNo = assignmentOffering.moodleId();
            out.println(moodleAssignmentNo == null
                ? "" : moodleAssignmentNo.toString());
        }
        else
        {
            out.print("PID");
            String name = assignmentOffering.assignment().name();
            if (name.startsWith("Lab"))
            {
                name += " (Lab)";
            }
            out.println(name);
        }
        NSArray<Submission> submissions = submissionsToExport;
        if (submissions != null)
        {
            for (Submission thisSubmission : submissions)
            {
                print(out, thisSubmission.user().userName());
                out.println(Double.toString(
                    thisSubmission.result().finalScore()));
            }
        }
        if (!targetMoodle)
        {
            out.print("Points Possible");
            out.println(assignmentOffering.assignment()
                .submissionProfile().availablePointsRaw().toString());
        }
        return outBytes.toByteArray();
    }


    // ----------------------------------------------------------
    private void collectSubmissionsToExport()
    {
        NSMutableArray<User> students =
            assignmentOffering.courseOffering().students().mutableClone();
        if (omitStaff)
        {
            students.removeObjectsInArray(
                assignmentOffering.courseOffering().instructors());
            students.removeObjectsInArray(
                assignmentOffering.courseOffering().graders());
        }
        else
        {
            ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
                students,
                assignmentOffering.courseOffering().instructors());
            ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
                students,
                assignmentOffering.courseOffering().graders());
        }
        submissionsToExport = new NSMutableArray<Submission>();
        if (students != null)
        {
            for (User student : students)
            {
                log.debug("checking " + student.userName());

                Submission thisSubmission = null;
                Submission gradedSubmission = null;
                // Find the submission
                NSArray<Submission> thisSubmissionSet =
                    Submission.objectsMatchingValues(
                        localContext(),
                        Submission.USER_KEY,
                        student,
                        Submission.ASSIGNMENT_OFFERING_KEY,
                        assignmentOffering
                    );
                log.debug("searching for submissions");
                for (Submission sub : thisSubmissionSet)
                {
                    log.debug("\tsub #" + sub.submitNumber());
                    if (sub.result() != null)
                    {
                        if (thisSubmission == null)
                        {
                            thisSubmission = sub;
                        }
                        else if (sub.submitNumberRaw() != null)
                        {
                            int num = sub.submitNumber();
                            if (num > thisSubmission.submitNumber())
                            {
                                thisSubmission = sub;
                            }
                        }
                        if (sub.result().status() != Status.TO_DO)
                        {
                            if (gradedSubmission == null)
                            {
                                gradedSubmission = sub;
                            }
                            else if (sub.submitNumberRaw() != null)
                            {
                                int num = sub.submitNumber();
                                if (num > gradedSubmission.submitNumber())
                                {
                                    gradedSubmission = sub;
                                }
                            }
                        }
                    }
                }
                if (gradedSubmission != null)
                {
                    thisSubmission = gradedSubmission;
                }
                if (thisSubmission != null)
                {
                    log.debug("\t saving for export = "
                              + thisSubmission.submitNumber());
                    submissionsToExport.addObject(thisSubmission);
                }
                else
                {
                    log.debug("no submission found");
                }
            }
        }
    }


    // ----------------------------------------------------------
    public WOComponent downloadScoreFile()
    {
        collectSubmissionsToExport();
        byte[] rawData;
        if (useFullFormat)
        {
            rawData = exportAsWebCATCSV();
        }
        else
        {
            rawData = exportAsBlackboardCSV(useMoodleFormat);
        }

        DeliverFile csvFile = pageWithName(DeliverFile.class);
        csvFile.setFileData(new NSData(rawData));
        csvFile.setFileName(new File( ""
           + assignmentOffering.courseOffering().crnSubdirName()
           + "-"
           + assignmentOffering.assignment().subdirName()
           + ".csv"));
        csvFile.setContentType("application/octet-stream");
        csvFile.setStartDownload(true);
        return csvFile;
    }


    // ----------------------------------------------------------
    public void flushNavigatorDerivedData()
    {
        assignmentOffering = null;
        super.flushNavigatorDerivedData();
    }


    //~ Instance/static variables .............................................

    private NSMutableArray<Submission> submissionsToExport;

    static Logger log = Logger.getLogger(DownloadScoresPage.class);
}
