/*==========================================================================*\
 |  Copyright (C) 2006-2021 Virginia Tech
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
import java.util.Map;
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
import com.webobjects.foundation.NSMutableDictionary;

//-------------------------------------------------------------------------
/**
 * Allow the user to download grades for an assignment in spreadsheet form
 * as a CSV file.
 *
 * @author  Tony Allevato
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
    public boolean useFullAllFormat;
    public boolean useFullAllDetailedFormat;
    public boolean includeStaff = false;
    public boolean includeIds = false;

    public NSArray<AssignmentOffering> assignmentOfferings;
    public NSArray<CourseOffering> courseOfferings;
    public Submission.SubmissionGradingState gradingState;
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
        if (useMoodleFormat || useBlackboardFormat)
        {
            rawData = exportAsBlackboardCSV(useMoodleFormat);
        }
        else
        {
            rawData = exportAsWebCATCSV();
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
        NSMutableArray<Submission> submissions =
            new NSMutableArray<Submission>();

        NSMutableDictionary<AssignmentOffering, NSArray<User>> usersByAO =
            new NSMutableDictionary<AssignmentOffering, NSArray<User>>();

        for (AssignmentOffering ao : assignmentOfferings)
        {
            NSArray<User> allForAO = includeStaff
                ? ao.courseOffering().studentsAndStaff()
                : ao.courseOffering().studentsWithoutStaff();
            usersByAO.setObjectForKey(allForAO, ao);
        }

        boolean needAllSubmissions = useFullAllFormat || useFullAllDetailedFormat;

        if (needAllSubmissions)
        {
            // Instantiate a dedicated state to fetch ALL submissions
            Submission.SubmissionGradingState fullState =
                new Submission.SubmissionGradingState(
                    assignmentOfferings, usersByAO, false, true);

            Submission.submissionsForGrading(fullState);

            for (AssignmentOffering ao : assignmentOfferings)
            {
                Map<User, Submission.StudentSubmissionInfo> infoMap =
                    fullState.resultsForOffering(ao);
                NSArray<User> users = usersByAO.objectForKey(ao);

                // We need ALL submissions for each user, sorted by user name
                // then submit number.
                for (User u : users)
                {
                    Submission.StudentSubmissionInfo info =
                        (infoMap != null) ? infoMap.get(u) : null;
                    if (info != null && info.allSubmissions() != null)
                    {
                        for (Submission s : info.allSubmissions())
                        {
                            submissions.add(s);
                        }
                    }
                }
            }
        }
        else
        {
            // Only graded submissions needed: incrementally update existing state
            Submission.SubmissionGradingState state = gradingState;
            if (state == null)
            {
                state = Submission.submissionsForGrading(assignmentOfferings, usersByAO);
            }
            else
            {
                Submission.submissionsForGrading(state);
            }

            for (AssignmentOffering ao : assignmentOfferings)
            {
                Map<User, Submission.StudentSubmissionInfo> infoMap =
                    state.resultsForOffering(ao);
                NSArray<User> users = usersByAO.objectForKey(ao);

                for (User u : users)
                {
                    Submission.StudentSubmissionInfo info =
                        (infoMap != null) ? infoMap.get(u) : null;
                    if (info != null && info.gradedSubmission() != null)
                    {
                        submissions.add(info.gradedSubmission());
                    }
                }
            }
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
        if (field == null
            || (field instanceof Double && ((Double)field).isNaN())
            || (field instanceof Float && ((Float)field).isNaN()))
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

        // Column titles
        out.print("Course");
        String courseVal = course.deptNumber();
        out.print("CRN");
        out.print("Semester");
        String semesterVal = useFullAllDetailedFormat
            ? semester.dirName()
            : semester.name();
        out.print("Assignment");
        String asgnVal = useFullAllDetailedFormat
            ? assignment.subdirName()
            : assignment.name();
        if (includeIds)
        {
            out.print("ID No.");
        }
        out.print("User");
        out.print("Email");
        out.print("Last Name");
        out.print("First Name");
        if (assignment.submissionProfile().allowPartners())
        {
            out.print("Partners");
        }
        out.print("Sub No.");
        out.print("Time");
        out.print("Timestamp");
        out.print("Due");
        out.print("Due Timestamp");
        out.print("time until due");
        if (useFullAllDetailedFormat)
        {
            out.print("Student Test %");
            out.print("Student Coverage %");
            out.print("Ref Test %");
            out.print("Validation Tests Executed");
            out.print("Validation Tests Passed");
            out.print("Validation Test %");
            out.print("milestonePassed.1");
            out.print("milestonePassed.2");
            out.print("milestonePassed.3");
        }
        out.print("Correctness/Testing Score");
        out.print("Correctness/Testing %");
        out.print("Style Score");
        out.print("Style %");
        out.print("Design");
        out.print("Design %");
        out.print("Penalty/Bonus");
        out.print("Total Score");
        out.print("Total %");
        out.println("Comments");

        for (Submission submission : submissionsToExport)
        {
            User student = submission.user();

            print(out, courseVal);
            print(out,
                useFullAllDetailedFormat
                ? submission.assignmentOffering().courseOffering()
                    .crnSubdirName()
                : submission.assignmentOffering().courseOffering()
                    .crn());
            print(out, semesterVal);
            print(out, asgnVal);
            if (includeIds)
            {
                print(out, student.universityIDNo());
            }
            print(out, student.userName());
            print(out, student.email());
            print(out, student.lastName());
            print(out, student.firstName());
            SubmissionResult result = submission.result();
            if (assignment.submissionProfile().allowPartners())
            {
                String partners = "";
                boolean first = true;
                if (result != null)
                {
                    for (Submission psub : result.submissions())
                    {
                        if (!first)
                        {
                            partners += ";";
                        }
                        partners += psub.user().email();
                        first = false;
                    }
                }
                print(out, partners);
            }

            log.debug("submission found = "
                + submission.submitNumber());
            print(out, submission.submitNumberRaw());
            print(out, submission.submitTime().toString());
            out.print(Long.toString(submission.submitTime().getTime()));
            print(out, submission.assignmentOffering().dueDate().toString());
            print(out, submission.assignmentOffering().dueDate().getTime());
            print(out, submission.assignmentOffering().dueDate().getTime()
                - submission.submitTime().getTime());
            if (result == null)
            {
                if (useFullAllDetailedFormat)
                {
                   print(out, "");
                   print(out, "");
                   print(out, "");
                   print(out, "");
                   print(out, "");
                   print(out, "");
                   print(out, "");
                   print(out, "");
                   print(out, "");
                }
                print(out, "");
                print(out, "");
                print(out, "");
                print(out, "");
                print(out, "");
                print(out, "");
                print(out, "");
                print(out, "");
                print(out, "");
                print(out, "");
                out.println();
            }
            else
            {
                SubmissionProfile sp = submission.assignmentOffering()
                    .assignment().submissionProfile();

                if (useFullAllDetailedFormat)
                {
                    print(out, result.properties()
                        .getProperty("student.test.passRate"));
                    {
                        int elements = 0;
                        int elementsCovered = 0;
                    
                        for (SubmissionFileStats f : result.submissionFileStats())
                        {
                            elements += f.elements();
                            elementsCovered += f.elementsCovered();
                        }
                        print(out, elementsCovered / (1.0 * elements));
                    }
                    print(out, result.properties()
                        .getProperty("instructor.test.passRate"));
                    print(out, result.properties()
                        .getProperty("validate.test.executed"));
                    print(out, result.properties()
                        .getProperty("validate.test.passed"));
                    print(out, result.properties()
                        .getProperty("validate.test.passRate"));
                    print(out, result.properties()
                        .getProperty("milestonePassed.1"));
                    print(out, result.properties()
                        .getProperty("milestonePassed.2"));
                    print(out, result.properties()
                        .getProperty("milestonePassed.3"));
                }
                print(out, result.correctnessScoreRaw());
                print(out, result.correctnessScore()
                    / sp.correctnessPoints());
                print(out, result.toolScoreRaw());
                print(out, result.toolScore()
                    / sp.toolPoints());
                print(out, result.taScoreRaw());
                print(out, result.taScore()
                    / sp.taPoints());
                if (sp.earlyBonusMaxPts() + sp.latePenaltyMaxPts() > 0.00001)
                {
                    print(out, result.earlyBonus() - result.latePenalty());
                }
                else
                {
                    print(out, "");
                }
                print(out, result.finalScore());
                print(out, result.finalScore() / sp.availablePoints());
                print(out, result.comments());
                out.println();
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

        for (Submission submission : submissionsToExport)
        {
            if (submission.result() != null)
            {
                print(out, submission.user().userName());
                out.println(Double.toString(
                        submission.result().finalScore()));
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

    private NSArray<Submission> submissionsToExport;

    private static final Logger log =
        Logger.getLogger(DownloadScoresDialog.class);
}
