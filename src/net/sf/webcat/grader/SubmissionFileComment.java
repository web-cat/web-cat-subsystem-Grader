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

import com.webobjects.foundation.*;
import com.webobjects.foundation.NSComparator.*;

import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Represents one TA comment on one source file in a submission.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class SubmissionFileComment
    extends _SubmissionFileComment
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new SubmissionFileComment object.
     */
    public SubmissionFileComment()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public String category()
    {
        return categoryName[categoryNo()];
    }


    // ----------------------------------------------------------
    public void setCategory( String value )
    {
        log.debug( "setCategory( \"" + value + "\")" );
        setCategoryNo( (byte)categoryIntFromString( value ) );
        log.debug( "category = " + categoryNo() );
    }


    // ----------------------------------------------------------
    public static int categoryIntFromString( String value )
    {
        value = substituteWithSpace(value).trim();
        for ( int i = 0; i < categoryName.length; i++ )
        {
            if ( value.equals( categoryName[i] ) )
            {
                return i;
            }
        }
        return 0;
    }


    // ----------------------------------------------------------
    private static String substituteWithSpace(String value)
    {
        String newvalue = "";
        for(int i = 0; i< value.length(); i++)
        {
            char ch = value.charAt(i);
            if( (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') )
                newvalue += ch;
            else
                newvalue += ' ';          
        }
        return newvalue;
        
    }


    // ----------------------------------------------------------
    public static String categoryName( int category )
    {
        return categoryName[category];
    }


    // ----------------------------------------------------------
    public static String categoryIcon( int category )
    {
        return categoryIcon[category];
    }

    // ----------------------------------------------------------
    public String categoryIcon()
    {
        return categoryIcon[categoryNo()];
    }

    // ----------------------------------------------------------
    public static String toName( int to )
    {
        return toName[to];
    }


    // ----------------------------------------------------------
    public String to()
    {
        return toName[toNo()];
    }


    // ----------------------------------------------------------
    public void setTo( String value )
    {
        setToNo( (byte)targetIntFromString( value ) );
    }


    // ----------------------------------------------------------
    public static int targetIntFromString( String value )
    {
        for ( int i = 0; i < toName.length; i++ )
        {
            if ( value.equals( toName[i] ) )
            {
                return i;
            }
        }
        return -1;
    }


    // ----------------------------------------------------------
    public void setTo( byte value )
    {
        setToNo( value );
    }


    // ----------------------------------------------------------
    public boolean readableByUser( User user )
    {
        int target = toNo();
        if ( target <= 0 )
        {
            return true;
        }
        if ( user == author() )
        {
            return true;
        }
        CourseOffering course = submissionFileStats().submissionResult()
            .submission().assignmentOffering().courseOffering();
        if ( target <= TO_FACULTY_AND_TAS )
        {
            NSArray tas = course.TAs();
            for ( int i = 0; i < tas.count(); i++ )
            {
                if ( user == tas.objectAtIndex( i ) )
                    return true;
            }
        }
        if ( target <= TO_FACULTY_ONLY )
        {
            NSArray instructors = course.instructors();
            for ( int i = 0; i < instructors.count(); i++ )
            {
                if ( user == instructors.objectAtIndex( i ) )
                    return true;
            }
        }
        return false;
    }


    // ----------------------------------------------------------
    public static class AscendingLineComparator
        extends NSComparator
    {
        // ----------------------------------------------------------
        /* (non-Javadoc)
         * @see com.webobjects.foundation.NSComparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare( Object lhs, Object rhs )
            throws ComparisonException
        {
            if ( !( lhs instanceof SubmissionFileComment
                 && rhs instanceof SubmissionFileComment ) )
            {
                throw new ComparisonException(
                    "arguments must be SubmissionFileStats instances" );
            }
            SubmissionFileComment left  = (SubmissionFileComment)lhs;
            SubmissionFileComment right = (SubmissionFileComment)rhs;
            if ( left.lineNo() < right.lineNo() )
            {
                return OrderedAscending;
            }
            else if ( left.lineNo() > right.lineNo() )
            {
                return OrderedDescending;
            }
            else if ( left.categoryNo() < right.categoryNo() )
            {
                return OrderedAscending;
            }
            else if ( left.categoryNo() > right.categoryNo() )
            {
                return OrderedDescending;
            }
            else if ( left.deduction() > right.deduction() )
            {
                return OrderedAscending;
            }
            else if ( left.deduction() < right.deduction() )
            {
                return OrderedDescending;
            }
            else if ( left.deduction() < right.deduction() )
            {
                return OrderedDescending;
            }
            return OrderedSame;
        }
    }
    public static final NSComparator STANDARD_ORDERING =
        new AscendingLineComparator();


// If you add instance variables to store property values you
// should add empty implementions of the Serialization methods
// to avoid unnecessary overhead (the properties will be
// serialized for you in the superclass).

//    // ----------------------------------------------------------
//    /**
//     * Serialize this object (an empty implementation, since the
//     * superclass handles this responsibility).
//     * @param out the stream to write to
//     */
//    private void writeObject( java.io.ObjectOutputStream out )
//        throws java.io.IOException
//    {
//    }
//
//
//    // ----------------------------------------------------------
//    /**
//     * Read in a serialized object (an empty implementation, since the
//     * superclass handles this responsibility).
//     * @param in the stream to read from
//     */
//    private void readObject( java.io.ObjectInputStream in )
//        throws java.io.IOException, java.lang.ClassNotFoundException
//    {
//    }


    //~ Instance/static variables .............................................

    public static final int TO_EVERYONE        = 0;
    public static final int TO_FACULTY_AND_TAS = 1;
    public static final int TO_FACULTY_ONLY    = 2;

    static private String[] toName = new String[]{
            "To Everyone",
            "To Faculty/TAs",
            "To Faculty Only"
        };
    static private String[] categoryName = new String[]{
            "Null Category",
            "Error",
            "Warning",
            "Question",
            "Suggestion",
            "Answer",
            "Good",
            "Extra Credit"
        };
    static private String[] categoryIcon = new String[]{
            "/icons/todo.gif",
            "/icons/exclaim.gif",
            "/icons/caution.gif",
            "/icons/help.gif",
            "/icons/suggestion.gif",
            "/icons/answer.gif",
            "/icons/check.gif",
            "/icons/excred.gif"
        };
    static Logger log = Logger.getLogger( SubmissionFileComment.class );
}
