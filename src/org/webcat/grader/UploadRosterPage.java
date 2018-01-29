/*==========================================================================*\
 |  $Id: UploadRosterPage.java,v 1.4 2014/06/16 17:26:24 stedwar2 Exp $
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

import com.Ostermiller.util.*;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.io.*;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.ui.generators.JavascriptGenerator;

//-------------------------------------------------------------------------
/**
 * This class allows a CSV file of new users to be added to a course.
 *
 * @author  Stephen Edwards
 * @author  Latest changes by: $Author: stedwar2 $
 * @version $Revision: 1.4 $, $Date: 2014/06/16 17:26:24 $
 */
public class UploadRosterPage
    extends GraderCourseEditComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public UploadRosterPage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public String               filePath;
    public NSData               data;
    public AuthenticationDomain domain;
    public AuthenticationDomain domainItem;
    public WODisplayGroup       domainDisplayGroup;
    public String               newFilePath;
    public NSData               newData;

    public Delimiter            aDelimiter;
    public Delimiter            selectedDelimiter;
    public Delimiter            previousDelimiter;

    public NSMutableArray<NSMutableArray<String>> previewLines;
    public NSArray<String>        aPreviewLine;
    public String                 cell;
    public int                    numberOfRecords;
    public int                    maxRecordLength;
    public boolean                gapBeforeLongLine;
    public NSMutableArray<String> longPreviewLine;
    public boolean                gapAfterLongLine;
    public int                    index;
    public boolean                firstLineColumnHeadings;

    public NSMutableArray<String> columns;
    public String                 aColumn;
    public int                    colIndex;

    public boolean                removeUnlisted;


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        if (domain == null)
        {
            domain = courseOffering().course().department().institution();
        }
        if (previewLines == null)
        {
            refresh(true, false);
        }
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public WOActionResults replace()
    {
        if (newFilePath != null
           && !newFilePath.equals("")
           && newData != null
           && newData.length() > 0)
        {
            filePath = newFilePath;
            data = newData;
            previewLines = null;
            guessFileParameters();
        }
        else
        {
            error("Please select a (non-empty) CSV file to upload.");
        }
        return refresh();
    }


    // ----------------------------------------------------------
    public WOActionResults refresh()
    {
        log.debug("refresh()");
        clearAllMessages();
        refresh(false, true);
        JavascriptGenerator page = new JavascriptGenerator();
        page.refresh("preview", "fileInfo", "error-panel");
        return page;
    }


    // ----------------------------------------------------------
    public boolean applyLocalChanges()
    {
        clearAllMessages();
        if (columnSelectionsAreOK())
        {
            readStudentList(true);
            return super.applyLocalChanges();
        }
        else
        {
            return false;
        }
    }


    // ----------------------------------------------------------
    public static class Delimiter
    {
        public char character;
        public String label;

        public Delimiter(char c, String s)
        {
            character = c;
            label = s;
        }

        public String toString()
        {
            return (label == null) ? Character.toString(character) : label;
        }
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        clearAllMessages();
        refresh(false, false);
        return null;
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    private void refresh(boolean guessColumns, boolean showConfirmation)
    {
        if (selectedDelimiter == null)
        {
            guessDelimiter();
        }
        if (previewLines == null
            || previousDelimiter == null
            || previousDelimiter.character != selectedDelimiter.character)
        {
            extractPreviewLines();
            previousDelimiter = selectedDelimiter;
        }
        if (guessColumns)
        {
            guessColumns();
        }
        if (columnSelectionsAreOK())
        {
            if (showConfirmation)
            {
                confirmationMessage(
                    "No column labeling inconsistencies were detected.");
            }
            readStudentList(false);
        }
    }


    // ----------------------------------------------------------
    private void guessFileParameters()
    {
        extractPreviewLines();
        guessColumns();
    }


    // ----------------------------------------------------------
    private void guessDelimiter()
    {
        log.debug("guessDelimiter()");
        // Default is a comma
        selectedDelimiter = DELIMITERS.objectAtIndex(0);
        try
        {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(data.stream(), "UTF8"));

            // Find first non-blank line
            String line = in.readLine();
            while (line != null && BLANK_LINE.matcher(line).matches())
            {
                line = in.readLine();
            }

            if (line != null)
            {
                // Find a second non-blank line to compare against
                String line2 = in.readLine();
                while (line2 != null && BLANK_LINE.matcher(line2).matches())
                {
                    line2 = in.readLine();
                }
                if (line2 == null)
                {
                    line2 = "";
                }

                // Scan for most frequently occurring delimiter that has the
                // same frequency in both lines (but only chose Space if no
                // other delimiter has any hits)
                int bestDelimCount1 = 0;
                int bestDelimCount2 = 0;
                for (Delimiter d : DELIMITERS)
                {
                    int count1 = countOccurrences(line, d.character);
                    int count2 = countOccurrences(line2, d.character);
                    if (count1 == count2
                        && count1 > bestDelimCount2
                        && (d.character != ' ' || bestDelimCount2 == 0))
                    {
                        selectedDelimiter = d;
                        bestDelimCount2 = count1;
                    }
                    else if (bestDelimCount2 == 0
                             && count1 > bestDelimCount1
                             && (d.character != ' ' || bestDelimCount1 == 0))
                    {
                        selectedDelimiter = d;
                        bestDelimCount1 = count1;
                    }
                }
            }
            in.close();
        }
        catch (UnsupportedEncodingException e)
        {
            log.error("Error decoding UTF8 stream:", e);
            throw new NSForwardException(e);
        }
        catch (IOException e)
        {
            log.error("Error reading from CSV stream:", e);
            throw new NSForwardException(e);
        }
        if (log.isDebugEnabled())
        {
            log.debug("guessDelimiter() = " + selectedDelimiter);
        }
    }


    // ----------------------------------------------------------
    private int countOccurrences(String s, char c)
    {
        if (s == null)
        {
            return 0;
        }
        int count = 0;
        int loc = s.indexOf(c);
        while (loc >= 0)
        {
            count++;
            loc++;
            if (loc >= s.length())
            {
                break;
            }
            loc = s.indexOf(c, loc);
        }
        return count;
    }


    // ----------------------------------------------------------
    private void extractPreviewLines()
    {
        log.debug("extractPreviewLines()");
        try
        {
            InputStreamReader stream =
                new InputStreamReader(data.stream(), "UTF8");
            CSVParser in = new CSVParser(stream);
            in.changeDelimiter(selectedDelimiter.character);

            previewLines = new NSMutableArray<NSMutableArray<String>>();
            longPreviewLine = null;
            numberOfRecords = 0;
            maxRecordLength = 0;
            gapBeforeLongLine = false;
            gapAfterLongLine = false;

            String[] line = getNonBlankLine(in);
            while (line != null)
            {
                numberOfRecords++;
                if (numberOfRecords <= NUM_PREVIEW_LINES)
                {
                    previewLines.add(new NSMutableArray<String>(line));
                }
                else
                {
                    if (line.length > maxRecordLength)
                    {
                        longPreviewLine = new NSMutableArray<String>(line);
                        gapBeforeLongLine =
                            numberOfRecords > NUM_PREVIEW_LINES + 1;
                    }
                    else
                    {
                        if (longPreviewLine == null)
                        {
                            gapBeforeLongLine = true;
                        }
                        else
                        {
                            gapAfterLongLine = true;
                        }
                    }
                }
                maxRecordLength = Math.max(maxRecordLength, line.length);
                line = getNonBlankLine(in);
            }

            stream.close();

            for (NSMutableArray<String> thisLine : previewLines)
            {
                while (thisLine.count() < maxRecordLength)
                {
                    thisLine.add("");
                }
            }
            if (longPreviewLine != null)
            {
                while (longPreviewLine.count() < maxRecordLength)
                {
                    longPreviewLine.add("");
                }
            }
        }
        catch (UnsupportedEncodingException e)
        {
            log.error("Error decoding UTF8 stream:", e);
            throw new NSForwardException(e);
        }
        catch (IOException e)
        {
            log.error("Error reading from CSV stream:", e);
            throw new NSForwardException(e);
        }
    }


    // ----------------------------------------------------------
    private void guessColumns()
    {
        log.debug("guessColumns()");
        columns = new NSMutableArray<String>();

        // Default for all columns is "Unused"
        for (int i = 0; i < maxRecordLength; i++)
        {
            columns.add(COLUMNS.objectAtIndex(0));
        }

        // Keep track of which columns we've found and where they are at
        int [] columnLocation = new int[COLUMNS.count()];
        for (int i = 0; i < columnLocation.length; i++)
        {
            columnLocation[i] = -1;
        }

        // Check for potential column labels in first row
        if (previewLines.count() > 0)
        {
            NSArray<String> firstLine = previewLines.objectAtIndex(0);
            // boolean atSignFound = false;
            for (int i = 0; i < firstLine.count(); i++)
            {
                String heading = firstLine.objectAtIndex(i);
                if (heading == null)
                {
                    continue;
                }
                heading = heading.trim();
                // if (cell.indexOf('@') >= 0) { atSignFound = true; }

                for (int colId = 1; colId < COLUMN_PATTERNS.length; colId++)
                {
                    if (COLUMN_PATTERNS[colId].matcher(heading).find()
                        && columnLocation[colId] < 0)
                    {
                        columnLocation[colId] = i;
                        columns.set(i, COLUMNS.objectAtIndex(colId));
                        firstLineColumnHeadings = true;
                    }
                }
            }
        }

        // If no e-mail column was found, try to pick a line of data and
        // guess columns from that
        if (columnLocation[COL_EMAIL] < 0)
        {
            NSArray<String> model = longPreviewLine;
            if (model == null && previewLines.count() > 0)
            {
                int modelLineNo =
                    (firstLineColumnHeadings && previewLines.count() > 1)
                    ? 1
                    : 0;
                model = previewLines.objectAtIndex(modelLineNo);
            }

            if (model != null)
            {
                for (int i = 0; i < model.count(); i++)
                {
                    String thisCell = model.objectAtIndex(i);
                    if (thisCell.indexOf('@') >= 0)
                    {
                        columnLocation[COL_EMAIL] = i;
                        columns.set(
                            i, COLUMNS.objectAtIndex(COL_EMAIL));
                        break;
                    }
                }
            }
        }

        // Now try to guess any remaining based on older "generic" Web-CAT
        // CSV layout, or VT Banner layout
        if (!firstLineColumnHeadings)
        {
            int[] layout = GENERIC_COLUMNS;
            if (maxRecordLength > 7
                 && columnLocation[COL_EMAIL] >= maxRecordLength - 3)
            {
                layout = BANNER_COLUMNS;
            }
            for (int i = 1; i < layout.length; i++)
            {
                // Skip unused columns
                if (layout[i] < 0)
                {
                    continue;
                }

                // i is the COL_FIRST_NAME .. COL_URL
                // layout[i] is the pos where the given content is expected

                // if there is no known position for this content column
                // and layout[i] is a position that exists
                // and we don't know what is in that position yet
                if (columnLocation[i] < 0
                    && layout[i] < columns.count()
                    && COLUMNS.objectAtIndex(0).equals(
                        columns.objectAtIndex(layout[i])))
                {
                    columnLocation[i] = layout[i];
                    columns.set(layout[i], COLUMNS.objectAtIndex(i));
                }
            }
        }
    }


    // ----------------------------------------------------------
    private boolean isBlank(String[] line)
    {
        return line == null
            || line.length == 0
            || (line.length == 1
                && (line[0] == null
                    || line[0].trim().equals("")));
    }


    // ----------------------------------------------------------
    private String[] getNonBlankLine(CSVParser in)
        throws IOException
    {
        String[] line = in.getLine();
        while (line != null && isBlank(line))
        {
            line = in.getLine();
        }
        return line;
    }


    // ----------------------------------------------------------
    private String extractColumn(String[] line, int columnId)
    {
        String result = null;
        int col = colLocation[columnId];
        if (col >= 0 && col < line.length)
        {
            result = line[col];
        }
        return result;
    }


    // ----------------------------------------------------------
    private void setPropertyIfMissing(
        User user, String property, String value)
    {
        if (value == null || property == null)
        {
            return;
        }
        String old = (String)user.valueForKey(property);
        if (old == null || old.equals(""))
        {
            user.takeValueForKey(value, property);
        }
    }


    // ----------------------------------------------------------
    private static class Name
    {
        public String first;
        public String last;

        public Name(String firstName, String lastName, String lastFirst)
        {
            first = firstName;
            last = lastName;
            if (first == null || last == null)
            {
                extractFirstAndLast(lastFirst);
            }
        }

        private void extractFirstAndLast(String lastFirst)
        {
            if (lastFirst == null)
            {
                return;
            }
            int commaPos = lastFirst.lastIndexOf(',');
            if (last == null && commaPos > 0)
            {
                last = lastFirst.substring(0, commaPos).trim();
            }
            if (first == null
                && commaPos >= 0
                && commaPos < lastFirst.length() - 1)
            {
                first = lastFirst.substring(commaPos + 1).trim();
            }
        }
    }


    // ----------------------------------------------------------
    private void readStudentList(boolean execute)
    {
        EOEditingContext ec = localContext();

        try
        {
            InputStreamReader stream =
                new InputStreamReader(data.stream(), "UTF8");
            String newUserNames = null;
            String existingUserNames = null;
            CSVParser in = new CSVParser(stream);
            in.changeDelimiter(selectedDelimiter.character);

            int row = 0;
            int numExistingAdded   = 0;
            int numNewCreated      = 0;
            int numAlreadyEnrolled = 0;
            String[] line = getNonBlankLine(in);
            if (firstLineColumnHeadings && line != null)
            {
                // skip the headings line
                line = getNonBlankLine(in);
            }
            while (line != null)
            {
                row++;
                User user = null;
                Name name = new Name(
                    extractColumn(line, COL_FIRST_NAME),
                    extractColumn(line, COL_LAST_NAME ),
                    extractColumn(line, COL_LAST_FIRST));
                String pid       = extractColumn(line, COL_USER_NAME);
                String email     = extractColumn(line, COL_EMAIL    );
                String idNo      = extractColumn(line, COL_ID_NUMBER);
                String pw        = extractColumn(line, COL_PASSWORD );
                String url       = extractColumn(line, COL_URL      );

                if (pid == null && email != null)
                {
                    int pos = email.indexOf('@');
                    if (pos >= 0)
                    {
                        pid = email.substring(0, pos);
                    }
                }

                if (pid == null)
                {
                    error("cannot identify user name for line "
                        + in.lastLineNumber() + ": "
                        + java.util.Arrays.toString(line) + ".");
                }
                else
                {
                    boolean isExistingUser = false;
                    try
                    {
                        user = (User)EOUtilities.objectMatchingValues(
                            ec, User.ENTITY_NAME,
                            new NSDictionary<String, Object>(
                                new Object[]{ pid  , domain                  },
                                new String[]{ User.USER_NAME_KEY,
                                              User.AUTHENTICATION_DOMAIN_KEY }
                           ));
                        log.debug(
                            "User " + pid + " already exists in database");
                        numExistingAdded++;
                        isExistingUser = true;
                        if (execute)
                        {
                            setPropertyIfMissing(user, User.FIRST_NAME_KEY,
                                name.first);
                            setPropertyIfMissing(user, User.LAST_NAME_KEY,
                                name.last);
                            setPropertyIfMissing(user,
                                User.UNIVERSITY_ID_NO_KEY, idNo);
                            setPropertyIfMissing(user, User.EMAIL_KEY,
                                email);
                            setPropertyIfMissing(user, User.URL_KEY, url);
                        }
                    }
                    catch (EOObjectNotAvailableException e)
                    {
                        log.info("Creating new user " + pid);
                        numNewCreated++;
                        if (newUserNames == null)
                        {
                            newUserNames = pid;
                        }
                        else
                        {
                            newUserNames += ", " + pid;
                        }
                        if (execute)
                        {
                            user = User.createUser(pid, null, domain,
                                User.STUDENT_PRIVILEGES, ec);
                            user.setFirstName(name.first);
                            user.setLastName(name.last);
                            user.setUniversityIDNo(idNo);
                            user.setEmail(email);
                            user.setUrl(url);
                            if (user.canChangePassword())
                            {
                                if (pw != null && !pw.equals(""))
                                {
                                    user.changePassword(pw);
                                }
                                else
                                {
                                    user.newRandomPassword();
                                }
                            }
                        }
                    }
                    catch (EOUtilities.MoreThanOneException e)
                    {
                        log.error("More than one user with same "
                            + "pid exists in Database");
                        error("Multiple users with username '"
                            + pid + "' exist; cannot add ambiguous user name "
                            + "to course.");
                        user = null;
                    }

                    if (user != null)
                    {
                        NSArray<CourseOffering> enrolledIn = user.enrolledIn();
                        if (enrolledIn != null
                             && enrolledIn.containsObject(courseOffering()))
                        {
                            log.debug("Relationship exists");
                            numAlreadyEnrolled++;
                            numExistingAdded--;
                        }
                        else
                        {
                            log.debug("relationship does not exist");
                            if (execute)
                            {
                                user.addToEnrolledInRelationship(
                                    courseOffering());
                            }
                            if (isExistingUser)
                            {
                                if (existingUserNames == null)
                                {
                                    existingUserNames = pid;
                                }
                                else
                                {
                                    existingUserNames += ", " + pid;
                                }
                            }
                        }
                    }
                }
                if (execute)
                {
                    super.applyLocalChanges();
                }
                line = getNonBlankLine(in);
            }
            WCComponentWithErrorMessages recipient = this;
            if (!hasMessages() && execute)
            {
                // If no problems, report confirmation
                recipient = nextPage;
            }
            if (numNewCreated == 0
                 && numExistingAdded == 0
                 && numAlreadyEnrolled == 0)
            {
                error("No existing or new student accounts identified!");
            }
            if (numNewCreated > 0)
            {
                recipient.confirmationMessage(
                    numNewCreated + " new student account"
                    + (numNewCreated > 1 ? "s" : "")
                    + (execute ? "" : " will be")
                    + " created and added to course (" + newUserNames
                    + ").");
            }
            if (numExistingAdded > 0)
            {
                recipient.confirmationMessage(
                    numExistingAdded + " existing student account"
                    + (numExistingAdded > 1 ? "s" : "")
                    + (execute ? "" : " will be")
                    + " added to course (" + existingUserNames + ").");
            }
            if (numAlreadyEnrolled > 0)
            {
                recipient.confirmationMessage(
                    numAlreadyEnrolled + " existing student account"
                    + (numAlreadyEnrolled > 1 ? "s" : "")
                    + (execute ? " were" : " are")
                    + " already enrolled in this course.");
            }
        }
        catch (UnsupportedEncodingException e)
        {
            log.error("Error decoding UTF8 stream:", e);
            error("Error decoding UTF8 stream: " + e.getMessage());
            // throw new NSForwardException(e);
        }
        catch (IOException e)
        {
            log.error("Error reading from CSV stream:", e);
            error("Error reading from CSV stream: " + e.getMessage());
            // throw new NSForwardException(e);
        }
    }


    // ----------------------------------------------------------
    private boolean columnSelectionsAreOK()
    {
        colLocation = new int[COLUMNS.count()];
        for (int i = 0; i < colLocation.length; i++)
        {
            colLocation[i] = -1;
        }
        for (int i = 0; i < columns.count(); i++)
        {
            String col = columns.objectAtIndex(i);
            if (col.equals(COLUMNS.objectAtIndex(0)))
            {
                continue;
            }
            for (int j = 1; j < COLUMNS.count(); j++)
            {
                String possible = COLUMNS.objectAtIndex(j);
                if (possible.equals(col))
                {
                    if (colLocation[j] >= 0)
                    {
                        error("Please choose a single column for \""
                            + col + "\".");
                    }
                    else
                    {
                        colLocation[j] = i;
                    }
                    continue;
                }
            }
        }
        if (colLocation[COL_EMAIL] < 0 && colLocation[COL_USER_NAME] < 0)
        {
            error("Please identify a column containing either user names "
                + "or e-mail addresses.");
        }
        log.debug("columnSelectionsAreOK() = " + !hasMessages());
        return !hasMessages();
    }


    //~ Instance/static variables .............................................

    private int[] colLocation;

    // private static final int COL_UNUSED     = 0;
    private static final int COL_FIRST_NAME = 1;
    private static final int COL_LAST_NAME  = 2;
    private static final int COL_LAST_FIRST = 3;
    private static final int COL_EMAIL      = 4;
    private static final int COL_USER_NAME  = 5;
    private static final int COL_PASSWORD   = 6;
    private static final int COL_ID_NUMBER  = 7;
    private static final int COL_URL        = 8;

    public static final NSArray<String> COLUMNS = new NSArray<String>(
        new String[]{
        "Unused",
        "First Name",
        "Last Name",
        "Last Name, First",
        "E-mail",
        "User Name",
        "Password",
        "ID No.",
        "URL"
    });

    private static final Pattern[] COLUMN_PATTERNS = new Pattern[]{
        null,
        Pattern.compile("(?i)^\\s*first(\\b|\\s)"),
        Pattern.compile("(?i)^\\s*(last\\b|surname)"),
        Pattern.compile("(?i)^\\s*(name|((last\\b|surname).*,.*first))"),
        Pattern.compile("(?i)\\be(-)?mail\\b(?!\\s+confidential)"),
        Pattern.compile("(?i)^\\s*(user\\s*name?|uid|pid|user\\s*id|user)$"),
        Pattern.compile("(?i)^\\s*(pass(word)?|pw$)"),
        Pattern.compile("(?i)\\bid(\\s*(number|#|no|no\\.)?)\\b"),
        Pattern.compile("(?i)^\\s*(url|web(\\b|\\s*addr))")
    };

    private static final int[] BANNER_COLUMNS = new int[]{
        -1,     // Unused
        4,      // First Name
        3,      // Last Name
        -1,     // Last, First
        8,      // E-mail
        -1,     // User Name
        -1,     // Password
        -1,      // ID No.
        -1      // URL
    };

    private static final int[] GENERIC_COLUMNS = new int[]{
        -1,     // Unused
        1,      // First Name
        0,      // Last Name
        -1,     // Last, First
        2,      // E-mail
        3,      // User Name
        5,      // Password
        4,      // ID No.
        -1      // URL
    };

    public static final NSArray<Delimiter> DELIMITERS =
        new NSArray<Delimiter>(new Delimiter[] {
            new Delimiter(',', ",  Comma"    ),
            new Delimiter('\t', "   Tab"     ),
            new Delimiter(':', ":  Colon"    ),
            new Delimiter(';', ";  Semicolon"),
            new Delimiter(' ', "    Space"   )
    });

    private static final int NUM_PREVIEW_LINES = 5;
    private static final Pattern BLANK_LINE = Pattern.compile("^\\s*$");

    static Logger log = Logger.getLogger(UploadRosterPage.class);
}
