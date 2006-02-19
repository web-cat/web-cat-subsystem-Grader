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
import com.Ostermiller.util.ExcelCSVParser;
import er.extensions.ERXConstant;
import java.io.*;

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class displays a list of users enrolled in a selected course and
 * allows new users to be added.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class CourseRosterPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     * 
     * @param context The page's context
     */
    public CourseRosterPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup studentDisplayGroup;
    public WODisplayGroup notStudentDisplayGroup;
    /** student in the worepetition */
    public User           student;
    /** index in the worepetition */
    public int            index;

    public String               filePath;
    public NSData               data;
    public AuthenticationDomain domain;
    public AuthenticationDomain domainItem;
    public WODisplayGroup       domainDisplayGroup;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     * 
     * @param response The response being built
     * @param context  The context of the request
     */
    public void appendToResponse( WOResponse response, WOContext context )
    {
        domain = wcSession().user().authenticationDomain();
        // Set up student list filters
        studentDisplayGroup.queryBindings().setObjectForKey(
                wcSession().courseOffering(),
                "courseOffering"
            );
        studentDisplayGroup.fetch();
        notStudentDisplayGroup.setQualifier( new EONotQualifier(
            new EOKeyValueQualifier(
                User.ENROLLED_IN_KEY,
                EOQualifier.QualifierOperatorContains,
                wcSession().courseOffering()
            ) ) );
        notStudentDisplayGroup.fetch();
        super.appendToResponse( response, context );
        oldBatchSize1  = studentDisplayGroup.numberOfObjectsPerBatch();
        oldBatchIndex1 = studentDisplayGroup.currentBatchIndex();
        oldBatchSize2  = notStudentDisplayGroup.numberOfObjectsPerBatch();
        oldBatchIndex2 = notStudentDisplayGroup.currentBatchIndex();
    }


    // ----------------------------------------------------------
    public WOComponent upload()
    {
        if (  filePath != null
           && !filePath.equals( "" )
           && data != null
           && data.length() > 0 )
        {
            try
            {
                readStudentList( data.stream() );
            }
            catch ( IOException e )
            {
                errorMessage( "An IO exception occurred while reading your "
                              + "CSV file." );
            }
        }
        else
        {
            errorMessage( "Please select a CSV file to upload." );
        }
        return null;
    }


    // ----------------------------------------------------------
    public void readStudentList( InputStream stream )
        throws IOException
    {
        EOEditingContext ec = wcSession().localContext();

        // File inputFile = new File( csvFilesDir + "/" + filename );
        // FileInputStream fis = new FileInputStream( inputFile );
        ExcelCSVParser usersFile = new ExcelCSVParser( stream );
//        ExcelCSVParser usersFile = new ExcelCSVParser(
//                new ByteArrayInputStream( data.bytesNoCopy(
//                    new NSMutableRange( 0, data.length() )
//                ) )
//            );
        String[] t;
        User user;
        int row = 0;
        while ( ( t = usersFile.getLine() ) != null )
        {
            row++;
            String firstName = null;
            String lastName  = null;
            String pid       = null;
            String email     = null;
            String idNo      = null;
            String pw        = null;
            if ( BANNER_FORMAT.equals(
                     context().request().stringFormValueForKey( "format" ) ) )
            {
                lastName  = t[1];
                firstName = t[2];
                int pos = t[t.length - 2].indexOf( String.valueOf( '@' ) );
                if ( pos < 0 )
                {
                    errorMessage( "illegal e-mail address '"+ t[t.length - 2]
                                  + "' for '" + lastName + ", " + firstName
                                  + "' on line " + row + ".  Is your CSV file "
                                  + "in VT Banner format?  Ignoring remainder "
                                  + "of file." );
                    break;
                }
                pid = t[t.length - 2].substring( 0, pos );
                idNo = t[0];
            }
            else
            {
                lastName  = t[0];
                firstName = t[1];
                if ( t.length > 2 )
                {
                    email = t[2];
                }
                if ( t.length > 3 && t[3] != null && !t[3].equals( "" ) )
                {
                    pid = t[3];
                }
                else if ( email != null )
                {
                    int pos = email.indexOf( String.valueOf( '@' ) );
                    if ( pos >= 0 )
                        pid = email.substring( 0, pos);                    
                }
                if ( t.length > 4 )
                {
                    idNo = t[4];
                }
                if ( t.length > 5 )
                {
                    pw = t[5];
                }
            }
            
            if ( pid == null )
            {
                errorMessage( "cannot identify user name on line " + row
                              + "." );
                continue;
            }
            
            try
            {
                user = (User)EOUtilities.objectMatchingValues(
                    ec, User.ENTITY_NAME,
                    new NSDictionary(
                        new Object[]{ pid  , domain                 },
                        new Object[]{ User.USER_NAME_KEY,
                                      User.AUTHENTICATION_DOMAIN_KEY }
                    )
                );
                log.debug( "User " + pid + " already exists in database" );
                String val = user.firstName();
                if ( ( val == null || val.equals( "" ) )
                     && firstName != null && !firstName.equals( "" ) )
                {
                    user.setFirstName( firstName );
                }
                val = user.lastName();
                if ( ( val == null || val.equals( "" ) )
                     && lastName != null && !lastName.equals( "" ) )
                {
                    user.setLastName( lastName );
                }
                val = user.universityIDNo();
                if ( ( val == null || val.equals( "" ) )
                     && idNo != null && !idNo.equals( "" ) )
                {
                    user.setUniversityIDNo( idNo );
                }
                val = (String)user.storedValueForKey( User.EMAIL_KEY );
                if ( ( val == null || val.equals( "" ) )
                     && email != null && !email.equals( "" ) )
                {
                    user.setEmail( email );
                }
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
               errorMessage( "Multiple users with username '"
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
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        log.debug( "defaultAction()" );
        if (  filePath != null
           && !filePath.equals( "" )
           && data != null
           && data.length() > 0 )
        {
            upload();
        }
        if (    oldBatchSize1  != studentDisplayGroup.numberOfObjectsPerBatch()
	         || oldBatchIndex1 != studentDisplayGroup.currentBatchIndex()
             || oldBatchSize2  != notStudentDisplayGroup.numberOfObjectsPerBatch()
             || oldBatchIndex2 != notStudentDisplayGroup.currentBatchIndex() )
        {
            return null;
        }
        else
        {
            return super.defaultAction();
        }
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        log.debug( "defaultAction()" );
        if (  filePath != null
           && !filePath.equals( "" )
           && data != null
           && data.length() > 0 )
        {
            upload();
            return null;
        }
        return super.next();
    }


    // ----------------------------------------------------------
    public boolean applyLocalChanges()
    {
        log.debug( "applyLocalChanges()" );
        if (  filePath != null
           && !filePath.equals( "" )
           && data != null
           && data.length() > 0 )
        {
            upload();
        }
        return super.applyLocalChanges();
    }


    // ----------------------------------------------------------
    /**
     * Remove the selected student.
     * @return always null
     */
    public WOComponent removeStudent()
    {
        wcSession().courseOffering().removeFromStudentsRelationship( student );
        wcSession().commitLocalChanges();
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Add the selected student.
     * @return always null
     */
    public WOComponent addStudent()
    {
        wcSession().courseOffering().addToStudentsRelationship( student );
        wcSession().commitLocalChanges();
        return null;
    }


    //~ Instance/static variables .............................................

    /** Saves the state of the student batch navigator to detect setting
     * changes. */
    protected int oldBatchSize1;
    /** Saves the state of the student batch navigator to detect setting
     * changes. */
    protected int oldBatchIndex1;
    /** Saves the state of the staff batch navigator to detect setting
     * changes. */
    protected int oldBatchSize2;
    /** Saves the state of the staff batch navigator to detect setting
     * changes. */
    protected int oldBatchIndex2;
    
    private static final String BANNER_FORMAT  = "0";
    private static final String GENERIC_FORMAT = "1";

    static Logger log = Logger.getLogger( CourseRosterPage.class );
}
