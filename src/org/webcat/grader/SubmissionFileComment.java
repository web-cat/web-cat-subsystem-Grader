/*==========================================================================*\
 |  $Id: SubmissionFileComment.java,v 1.5 2011/05/19 16:53:20 stedwar2 Exp $
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

package org.webcat.grader;

import com.webobjects.foundation.*;
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 * Represents one TA comment on one source file in a submission.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.5 $, $Date: 2011/05/19 16:53:20 $
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
        value = value.trim();
        for ( int i = 0; i < toName.length; i++ )
        {
            if ( value.equals( toName[i] ) )
            {
                return i;
            }
        }
        return 0;
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
            .submissionFor(user).assignmentOffering().courseOffering();
        if (target <= TO_FACULTY_AND_TAS
            && course.graders().contains(user))
        {
            return true;
        }
        if (target <= TO_FACULTY_ONLY
            && course.instructors().contains(user))
        {
            return true;
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
            "icons/todo.gif",
            "icons/comment-error.png",
            "icons/comment-warning.png",
            "icons/comment-question.png",
            "icons/comment-suggestion.png",
            "icons/comment-answer.png",
            "icons/comment-good.png",
            "icons/comment-extracredit.png"
        };
    static Logger log = Logger.getLogger( SubmissionFileComment.class );
}
