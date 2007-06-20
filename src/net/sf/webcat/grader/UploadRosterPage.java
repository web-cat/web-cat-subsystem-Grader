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

import com.Ostermiller.util.*;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.regex.Pattern;
import org.apache.log4j.*;
import net.sf.webcat.core.*;

//-------------------------------------------------------------------------
/**
 * This class allows a CSV file of new users to be added to a course.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class UploadRosterPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public UploadRosterPage( WOContext context )
    {
        super( context );
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

    public NSMutableArray       previewLines;
    public NSArray              aPreviewLine;
    public String               cell;
    public int                  numberOfRecords;
    public int                  maxRecordLength;
    public boolean              gapBeforeLongLine;
    public NSMutableArray       longPreviewLine;
    public boolean              gapAfterLongLine;
    public int                  index;
    public boolean              firstLineColumnHeadings;

    public NSMutableArray       columns;
    public String               aColumn;
    public int                  colIndex;


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    public void appendToResponse( WOResponse response, WOContext context )
    {
        if ( domain == null )
        {
            domain = wcSession().user().authenticationDomain();
        }
        if ( previewLines == null )
        {
            guessFileParameters();
        }
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public WOComponent replace()
    {
        if (  newFilePath != null
           && !newFilePath.equals( "" )
           && newData != null
           && newData.length() > 0 )
        {
            filePath = newFilePath;
            data = newData;
            guessFileParameters();
        }
        else
        {
            error( "Please select a (non-empty) CSV file to upload." );
        }
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent refresh()
    {
        if ( columnSelectionsAreOK() )
        {
            confirmationMessage(
                "No column labeling inconsistencies were detected." );
        }
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        if ( applyLocalChanges() )
        {
            return super.next();
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    public boolean applyLocalChanges()
    {
        clearAllMessages();
        if ( columnSelectionsAreOK() )
        {
            readStudentList();
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

        public Delimiter( char c, String s )
        {
            character = c;
            label = s;
        }

        public String toString()
        {
            return ( label == null ) ? Character.toString( character ) : label;
        }
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    private void guessFileParameters()
    {
        guessDelimiter();
        extractPreviewLines();
        guessColumns();
    }


    // ----------------------------------------------------------
    private void guessDelimiter()
    {
        log.debug( "guessDelimiter()" );
        // Default is a comma
        selectedDelimiter = (Delimiter)DELIMITERS.objectAtIndex( 0 );
        try
        {
            BufferedReader in = new BufferedReader(
                new InputStreamReader( data.stream(), "UTF8" ) );

            // Find first non-blank line
            String line = in.readLine();
            while ( line != null && BLANK_LINE.matcher( line ).matches() )
            {
                line = in.readLine();
            }

            if ( line != null )
            {
                // Scan for most frequently occurring delimiter (but only
                // chose Space if no other delimiter has any hits)
                int bestDelimCount = 0;
                for ( int i = 0; i < DELIMITERS.count(); i++ )
                {
                    Delimiter d = (Delimiter)DELIMITERS.objectAtIndex( i );
                    int thisCount = countOccurrences( line, d.character );
                    if ( thisCount > bestDelimCount
                         && ( d.character != ' ' || bestDelimCount == 0 ) )
                    {
                        selectedDelimiter = d;
                        bestDelimCount = thisCount;
                    }
                }
            }
            in.close();
        }
        catch ( UnsupportedEncodingException e )
        {
            log.error( "Error decoding UTF8 stream:", e );
            throw new NSForwardException( e );
        }
        catch ( IOException e )
        {
            log.error( "Error reading from CSV stream:", e );
            throw new NSForwardException( e );
        }
        if ( log.isDebugEnabled() )
        {
            log.debug( "guessDelimiter() = " + selectedDelimiter );
        }
    }


    // ----------------------------------------------------------
    private int countOccurrences( String s, char c )
    {
        if ( s == null ) return 0;
        int count = 0;
        int loc = s.indexOf( c );
        while ( loc >= 0 )
        {
            count++;
            loc++;
            if ( loc >= s.length() ) break;
            loc = s.indexOf( c, loc );
        }
        return count;
    }


    // ----------------------------------------------------------
    private void extractPreviewLines()
    {
        log.debug( "extractPreviewLines()" );
        try
        {
            InputStreamReader stream =
                new InputStreamReader( data.stream(), "UTF8" );
            CSVParser in = new CSVParser( stream );
            in.changeDelimiter( selectedDelimiter.character );

            previewLines = new NSMutableArray();
            longPreviewLine = null;
            numberOfRecords = 0;
            maxRecordLength = 0;
            gapBeforeLongLine = false;
            gapAfterLongLine = false;

            String[] line = getNonBlankLine( in );
            while ( line != null )
            {
                numberOfRecords++;
                if ( numberOfRecords <= NUM_PREVIEW_LINES )
                {
                    previewLines.add( new NSMutableArray( line ) );
                }
                else
                {
                    if ( line.length > maxRecordLength )
                    {
                        longPreviewLine = new NSMutableArray( line );
                        gapBeforeLongLine =
                            numberOfRecords > NUM_PREVIEW_LINES + 1;
                    }
                    else
                    {
                        if ( longPreviewLine == null )
                        {
                            gapBeforeLongLine = true;
                        }
                        else
                        {
                            gapAfterLongLine = true;
                        }
                    }
                }
                maxRecordLength = Math.max( maxRecordLength, line.length );
                line = getNonBlankLine( in );
            }

            stream.close();

            for ( int i = 0; i < previewLines.count(); i++ )
            {
                NSMutableArray thisLine =
                    (NSMutableArray)previewLines.objectAtIndex( i );
                while ( thisLine.count() < maxRecordLength )
                {
                    thisLine.add( "" );
                }
            }
            if ( longPreviewLine != null )
            {
                while ( longPreviewLine.count() < maxRecordLength )
                {
                    longPreviewLine.add( "" );
                }
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            log.error( "Error decoding UTF8 stream:", e );
            throw new NSForwardException( e );
        }
        catch ( IOException e )
        {
            log.error( "Error reading from CSV stream:", e );
            throw new NSForwardException( e );
        }
    }


    // ----------------------------------------------------------
    private void guessColumns()
    {
        log.debug( "guessColumns()" );
        columns = new NSMutableArray();

        // Default for all columns is "Unused"
        for ( int i = 0; i < maxRecordLength; i++ )
        {
            columns.add( COLUMNS.objectAtIndex( 0 ) );
        }

        // Keep track of which columns we've found and where they are at
        int [] columnLocation = new int[COLUMNS.count()];
        for ( int i = 0; i < columnLocation.length; i++ )
        {
            columnLocation[i] = -1;
        }

        // Check for potential column labels in first row
        if ( previewLines.count() > 0 )
        {
            NSArray firstLine = (NSArray)previewLines.objectAtIndex( 0 );
            // boolean atSignFound = false;
            for ( int i = 0; i < firstLine.count(); i++ )
            {
                String heading = (String)firstLine.objectAtIndex( i );
                if ( heading == null ) continue;
                heading = heading.trim();
                // if ( cell.indexOf( '@' ) >= 0 ) { atSignFound = true; }

                for ( int colId = 1; colId < COLUMN_PATTERNS.length; colId++ )
                {
                    if ( COLUMN_PATTERNS[colId].matcher( heading ).matches()
                         && columnLocation[colId] < 0 )
                    {
                        columnLocation[colId] = i;
                        columns.set( i, COLUMNS.objectAtIndex( colId ) );
                        firstLineColumnHeadings = true;
                    }
                }
            }
        }

        // If no e-mail column was found, try to pick a line of data and
        // guess columns from that
        if ( columnLocation[COL_EMAIL] < 0 )
        {
            NSArray model = longPreviewLine;
            if ( model == null && previewLines.count() > 0 )
            {
                int modelLineNo =
                    ( firstLineColumnHeadings && previewLines.count() > 1 )
                    ? 1
                    : 0;
                model = (NSArray)previewLines.objectAtIndex( modelLineNo );
            }

            if ( model != null )
            {
                for ( int i = 0; i < model.count(); i++ )
                {
                    String thisCell = (String)model.objectAtIndex( i );
                    if ( thisCell.indexOf( '@' ) >= 0 )
                    {
                        columnLocation[COL_EMAIL] = i;
                        columns.set(
                            i, COLUMNS.objectAtIndex( COL_EMAIL ) );
                        break;
                    }
                }
            }
        }

        // Now try to guess any remaining based on older "generic" Web-CAT
        // CSV layout, or VT Banner layout
        int[] layout = GENERIC_COLUMNS;
        if ( maxRecordLength > 7
             && columnLocation[COL_EMAIL] >= maxRecordLength - 3 )
        {
            layout = BANNER_COLUMNS;
        }
        for ( int i = 1; i < layout.length; i++ )
        {
            // Skip unused columns
            if ( layout[i] < 0 ) continue;

            // i is the COL_FIRST_NAME .. COL_URL
            // layout[i] is the position where the given content is expected

            // if there is no known position for this content column
            // and layout[i] is a position that exists
            // and we don't know what is in that position yet
            if ( columnLocation[i] < 0
                 && layout[i] < columns.count()
                 && COLUMNS.objectAtIndex( 0 ).equals(
                     columns.objectAtIndex( layout[i] ) ) )
            {
                columnLocation[i] = layout[i];
                columns.set( layout[i], COLUMNS.objectAtIndex( i ) );
            }
        }
    }


    // ----------------------------------------------------------
    private boolean isBlank( String[] line )
    {
        return line == null
            || line.length == 0
            || ( line.length == 1
                 && ( line[0] == null
                      || line[0].trim().equals( "" ) ) );
    }


    // ----------------------------------------------------------
    private String[] getNonBlankLine( CSVParser in )
        throws IOException
    {
        String[] line = in.getLine();
        while ( line != null && isBlank( line ) )
        {
            line = in.getLine();
        }
        return line;
    }


    // ----------------------------------------------------------
    private String extractColumn( String[] line, int columnId )
    {
        String result = null;
        int col = colLocation[columnId];
        if ( col >= 0 && col < line.length )
        {
            result = line[col];
        }
        return result;
    }


    // ----------------------------------------------------------
    private void setPropertyIfMissing(
        User user, String property, String value )
    {
        if ( value == null ) return;
        if ( property == null ) return;
        String old = (String)user.valueForKey( property );
        if ( old == null || old.equals( "" ) )
        {
            user.takeValueForKey( value, property );
        }
    }


    // ----------------------------------------------------------
    private void readStudentList()
    {
        EOEditingContext ec = wcSession().localContext();

        try
        {
            InputStreamReader stream =
                new InputStreamReader( data.stream(), "UTF8" );
            CSVParser in = new CSVParser( stream );
            in.changeDelimiter( selectedDelimiter.character );

            User user;
            int row = 0;
            int numExistingAdded   = 0;
            int numNewCreated      = 0;
            int numAlreadyEnrolled = 0;
            String[] line = getNonBlankLine( in );
            if ( firstLineColumnHeadings && line != null )
            {
                // skip the headings line
                line = getNonBlankLine( in );
            }
            while ( line != null )
            {
                row++;
                String firstName = extractColumn( line, COL_FIRST_NAME );
                String lastName  = extractColumn( line, COL_LAST_NAME  );
                String pid       = extractColumn( line, COL_USER_NAME  );
                String email     = extractColumn( line, COL_EMAIL      );
                String idNo      = extractColumn( line, COL_ID_NUMBER  );
                String pw        = extractColumn( line, COL_PASSWORD   );
                String url       = extractColumn( line, COL_URL        );

                if ( pid == null && email != null )
                {
                    int pos = email.indexOf( '@' );
                    if ( pos >= 0 )
                    {
                        pid = email.substring( 0, pos );
                    }
                }

                if ( pid == null )
                {
                    error( "cannot identify user name for line "
                        + in.lastLineNumber() + ": "
                        + java.util.Arrays.toString( line ) + "." );
                }
                else
                {
                    try
                    {
                        user = (User)EOUtilities.objectMatchingValues(
                            ec, User.ENTITY_NAME,
                            new NSDictionary(
                                new Object[]{ pid  , domain                 },
                                new Object[]{ User.USER_NAME_KEY,
                                              User.AUTHENTICATION_DOMAIN_KEY }
                            ) );
                        log.debug(
                            "User " + pid + " already exists in database" );
                        numExistingAdded++;
                        setPropertyIfMissing( user, User.FIRST_NAME_KEY,
                            firstName );
                        setPropertyIfMissing( user, User.LAST_NAME_KEY,
                            lastName );
                        setPropertyIfMissing( user, User.UNIVERSITY_IDNO_KEY,
                            idNo );
                        setPropertyIfMissing( user, User.EMAIL_KEY, email );
                        setPropertyIfMissing( user, User.URL_KEY, url );
                    }
                    catch ( EOObjectNotAvailableException e )
                    {
                        log.info( "Creating new user " + pid );
                        user = User.createUser(
                            pid, null, domain, User.STUDENT_PRIVILEGES, ec );
                        user.setFirstName( firstName );
                        user.setLastName( lastName );
                        user.setUniversityIDNo( idNo );
                        user.setEmail( email );
                        user.setUrl( url );
                        numNewCreated++;
                        if ( user.canChangePassword() )
                        {
                            if ( pw != null && !pw.equals( "" ) )
                            {
                                user.changePassword( pw );
                            }
                            else
                            {
                                user.newRandomPassword();
                            }
                        }
                    }
                    catch ( EOUtilities.MoreThanOneException e )
                    {
                        log.error( "More than one user with same "
                            + "pid exists in Database" );
                        error( "Multiple users with username '"
                            + pid + "' exist; cannot add ambiguous user name "
                            + "to course." );
                        user = null;
                    }

                    if ( user != null )
                    {
                        NSArray enrolledIn = user.enrolledIn();
                        if ( enrolledIn != null
                             && enrolledIn.containsObject(
                                     wcSession().courseOffering() ) )
                        {
                            log.debug( "Relationship exists" );
                            numAlreadyEnrolled++;
                            numExistingAdded--;
                        }
                        else
                        {
                            log.debug( "relationship does not exist" );
                            user.addToEnrolledInRelationship(
                                wcSession().courseOffering() );
                        }
                    }
                }
                wcSession().commitLocalChanges();
                line = getNonBlankLine( in );
            }
            WCComponentWithErrorMessages recipient = this;
            if ( !hasMessages() )
            {
                // If no problems, report confirmation
                recipient = nextPage;
            }
            if ( numNewCreated == 0
                 && numExistingAdded == 0
                 && numAlreadyEnrolled == 0 )
            {
                error( "No existing or new student accounts identified!" );
            }
            if ( numNewCreated > 0 )
            {
                recipient.confirmationMessage(
                    numNewCreated + " new student account"
                    + ( numNewCreated > 1 ? "s" : "" )
                    + " created and added to course." );
            }
            if ( numExistingAdded > 0 )
            {
                recipient.confirmationMessage(
                    numExistingAdded + " existing student account"
                    + ( numExistingAdded > 1 ? "s" : "" )
                    + " added to course." );
            }
            if ( numAlreadyEnrolled > 0 )
            {
                recipient.confirmationMessage(
                    numAlreadyEnrolled + " existing student account"
                    + ( numAlreadyEnrolled > 1 ? "s" : "" )
                    + " were already enrolled in this course." );
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            log.error( "Error decoding UTF8 stream:", e );
            error( "Error decoding UTF8 stream: " + e.getMessage() );
            // throw new NSForwardException( e );
        }
        catch ( IOException e )
        {
            log.error( "Error reading from CSV stream:", e );
            error( "Error reading from CSV stream: " + e.getMessage() );
            // throw new NSForwardException( e );
        }
    }


    // ----------------------------------------------------------
    private boolean columnSelectionsAreOK()
    {
        colLocation = new int[COLUMNS.count()];
        for ( int i = 0; i < colLocation.length; i++ )
        {
            colLocation[i] = -1;
        }
        for ( int i = 0; i < columns.count(); i++ )
        {
            String col = (String)columns.objectAtIndex( i );
            if ( col.equals( COLUMNS.objectAtIndex( 0 ) ) )
            {
                continue;
            }
            for ( int j = 1; j < COLUMNS.count(); j++ )
            {
                String possible = (String)COLUMNS.objectAtIndex( j );
                if ( possible.equals( col ) )
                {
                    if ( colLocation[j] >= 0 )
                    {
                        error( "Please choose a single column for \""
                            + col + "\"." );
                    }
                    else
                    {
                        colLocation[j] = i;
                    }
                    continue;
                }
            }
        }
        if ( colLocation[COL_EMAIL] < 0 && colLocation[COL_USER_NAME] < 0 )
        {
            error( "Please identify a column containing either user names "
                + "or e-mail addresses." );
        }
        log.debug( "columnSelectionsAreOK() = " + !hasMessages() );
        return !hasMessages();
    }


    //~ Instance/static variables .............................................

    private int[] colLocation;

    // private static final int COL_UNUSED     = 0;
    private static final int COL_FIRST_NAME = 1;
    private static final int COL_LAST_NAME  = 2;
    private static final int COL_EMAIL      = 3;
    private static final int COL_USER_NAME  = 4;
    private static final int COL_PASSWORD   = 5;
    private static final int COL_ID_NUMBER  = 6;
    private static final int COL_URL        = 7;

    public static final NSArray COLUMNS = new NSArray( new String[]{
        "Unused",
        "First Name",
        "Last Name",
        "E-mail",
        "User Name",
        "Password",
        "ID No.",
        "URL"
    } );

    private static final Pattern[] COLUMN_PATTERNS = new Pattern[]{
        null,
        Pattern.compile( "(?i)^\\s*first\\b" ),
        Pattern.compile( "(?i)^\\s*(last\\b|surname)" ),
        Pattern.compile( "(?i)\\be(-)?mail\\b" ),
        Pattern.compile( "(?i)^\\s*(user\\s*name?|uid|pid|user\\s*id|user)$" ),
        Pattern.compile( "(?i)^\\s*(pass(word)?|pw$)" ),
        Pattern.compile( "(?i)\\bid(\\s*(number|#|no|no\\.)?)\\b" ),
        Pattern.compile( "(?i)^\\s*(url|web(\\b|\\s*addr))" )
    };

    private static final int[] BANNER_COLUMNS = new int[]{
        -1,     // Unused
        2,      // First Name
        1,      // Last Name
        6,      // E-mail
        -1,     // User Name
        -1,     // Password
        0,      // ID No.
        -1      // URL
    };

    private static final int[] GENERIC_COLUMNS = new int[]{
        -1,     // Unused
        1,      // First Name
        0,      // Last Name
        2,      // E-mail
        3,     // User Name
        5,     // Password
        4,      // ID No.
        -1      // URL
    };

    public static final NSArray DELIMITERS = new NSArray( new Delimiter[]{
        new Delimiter( ',', ",  Comma"     ),
        new Delimiter( '\t', "   Tab"      ),
        new Delimiter( ':', ":  Colon"     ),
        new Delimiter( ';', ";  Semicolon" ),
        new Delimiter( ' ', "    Space"    )
    } );

    private static final int NUM_PREVIEW_LINES = 5;
    private static final Pattern BLANK_LINE = Pattern.compile( "^\\s*$" );

    static Logger log = Logger.getLogger( UploadRosterPage.class );
}
