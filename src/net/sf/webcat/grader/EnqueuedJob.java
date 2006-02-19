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
import com.webobjects.eocontrol.*;

// -------------------------------------------------------------------------
/**
 * This class represents the database record of a student file submission
 * enqueued for compilation/processing but not yet handled.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class EnqueuedJob
    extends _EnqueuedJob
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new EnqueuedJob object.
     */
    public EnqueuedJob()
    {
        super();
    }


    //~ Constants (for key names) .............................................

    public static final String ASSIGNMENT_OFFERING_KEY =
        SUBMISSION_KEY
        + "." + Submission.ASSIGNMENT_OFFERING_KEY;
    public static final String SUBMIT_TIME_KEY =
        SUBMISSION_KEY
        + "." + Submission.SUBMIT_TIME_KEY;
    public static final String USER_KEY =
        SUBMISSION_KEY
        + "." + Submission.USER_KEY;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where this submission is stored.
     * @return the directory name
     */
    public String workingDirName()
    {
        StringBuffer dir = new StringBuffer( 50 );
        dir.append( net.sf.webcat.core.Application
            .configurationProperties().getProperty( "grader.workarea" ) );
        dir.append( '/' );
        dir.append( submission().user().authenticationDomain().subdirName() );
        dir.append( '/' );
        dir.append( submission().user().userName() );
        return dir.toString();
    }


    // ----------------------------------------------------------
    public String toString()
    {
        Submission sub = submission();
        if ( sub != null )
        {
            return submission().toString() + "("
                + ( paused() ? "paused" : "ready" ) + ")";
        }
        else
        {
            return "job with <null> submission";
        }
    }


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
}
