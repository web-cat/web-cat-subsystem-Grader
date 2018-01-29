/*==========================================================================*\
 |  $Id: DownloadScoresDialog.java,v 1.2 2010/10/19 18:37:37 aallowat Exp $
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import org.apache.log4j.Logger;
import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.DeliverFile;
import org.webcat.core.Semester;
import org.webcat.core.User;
import org.webcat.core.WCComponent;
import org.webcat.ui.util.ComponentIDGenerator;
import com.Ostermiller.util.ExcelCSVPrinter;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

//-------------------------------------------------------------------------
/**
 * Allow the user to download grades for an assignment in spreadsheet form
 * as a CSV file.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author: aallowat $
 * @version $Revision: 1.2 $, $Date: 2010/10/19 18:37:37 $
 */
public class DownloadScoresDialog extends WCComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public DownloadScoresDialog(WOContext context)
    {
        super(context);
    }


    //~ KVC attributes (must be public) .......................................

    public boolean useBlackboardFormat;
    public boolean useMoodleFormat = true;
    public boolean useFullFormat;
    public boolean omitStaff = true;

    public NSArray<AssignmentOffering> assignmentOfferings;
    public NSArray<CourseOffering> courseOfferings;
    public Assignment assignment;
    public Course course;
    public Semester semester;

    public ComponentIDGenerator idFor;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public void appendToResponse(WOResponse response, WOContext context)
    {
        idFor = new ComponentIDGenerator(this);

        AssignmentOffering ao = assignmentOfferings.objectAtIndex(0);

        // Assume these are the same for all assignment offerings (which they
        // should be, unless we redesign the navigation).

        assignment = ao.assignment();
        course = ao.courseOffering().course();
        semester = ao.courseOffering().semester();

        super.appendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public String courseTitleString()
    {
        CourseOffering offering = courseOfferings.objectAtIndex(0);

        if (courseOfferings.count() > 1)
        {
            return offering.course().toString() + " (All)";
        }
        else
        {
            return offering.toString();
        }
    }


    // ----------------------------------------------------------
    /**
     * Downloads the scores in the appropriate file format.
     *
     * @return the CSV content as a DeliverFile component
     */
    public WOActionResults downloadScores()
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

        String filename;
        if (assignmentOfferings.count() == 1)
        {
            filename = assignmentOfferings.objectAtIndex(0).courseOffering()
                .crnSubdirName() + "-";
        }
        else
        {
            filename = course.deptNumber() + "-";
        }

        filename += assignment.subdirName() + ".csv";

        DeliverFile csvFile = pageWithName(DeliverFile.class);
        csvFile.setFileData(new NSData(rawData));
        csvFile.setFileName(new File(filename));
        csvFile.setContentType("application/octet-stream");
        csvFile.setStartDownload(true);
        return csvFile;
    }


    // ----------------------------------------------------------
    private void collectSubmissionsToExport()
    {
        NSMutableArray<UserSubmissionPair> submissions =
            new NSMutableArray<UserSubmissionPair>();

        for (AssignmentOffering ao : assignmentOfferings)
        {
            NSArray<User> students = omitStaff
                ? ao.courseOffering().studentsWithoutStaff()
                : ao.courseOffering().studentsAndStaff();
            submissions.addObjectsFromArray(Submission.submissionsForGrading(
                localContext(), ao, false, students, null));
        }

        submissionsToExport = submissions;
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
        out.println(course.deptNumber());
        out.print("Semester");
        out.print("");
        out.println(semester.name());
        out.print("Assignment");
        out.print("");
        out.println(assignment.name());
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

        for (UserSubmissionPair pair : submissionsToExport)
        {
            if (pair.userHasSubmission())
            {
                User student = pair.user();
                Submission submission = pair.submission();

                print(out, student.universityIDNo());
                print(out, student.userName());
                print(out, student.lastName());
                print(out, student.firstName());

                log.debug("submission found = "
                    + submission.submitNumber());
                print(out, submission.submitNumberRaw());
                print(out, submission.submitTime().toString());
                SubmissionResult result = pair.submission().result();
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
            out.println(assignment.name());
        }
        else
        {
            out.print("PID");
            String name = assignment.name();
            if (name.startsWith("Lab"))
            {
                name += " (Lab)";
            }
            out.println(name);
        }

        for (UserSubmissionPair pair : submissionsToExport)
        {
            if (pair.userHasSubmission())
            {
                print(out, pair.user().userName());
                out.println(Double.toString(
                        pair.submission().result().finalScore()));
            }
        }

        if (!targetMoodle)
        {
            out.print("Points Possible");
            out.println(assignment
                .submissionProfile().availablePointsRaw().toString());
        }
        return outBytes.toByteArray();
    }


    //~ Static/instance variables .............................................

    private NSArray<UserSubmissionPair> submissionsToExport;

    private static final Logger log =
        Logger.getLogger(DownloadScoresDialog.class);
}
