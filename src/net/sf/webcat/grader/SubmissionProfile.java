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

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Contains all the submission options for an assignment.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class SubmissionProfile
    extends _SubmissionProfile
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new SubmissionProfile object.
     */
    public SubmissionProfile()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void awakeFromInsertion( EOEditingContext ec )
    {
        super.awakeFromInsertion( ec );
        setSubmissionMethod( (byte)0 );
    }


    // ----------------------------------------------------------
    public static long maxMaxFileUploadSize()
    {
        return maxMaxFileUploadSize;
    }


    // ----------------------------------------------------------
    public static boolean maxFileUploadSizeIsWithinLimits( long value )
    {
        return value > 0 && value < maxMaxFileUploadSize;
    }


    // ----------------------------------------------------------
    public void setMaxFileUploadSize( int value )
    {
        if ( maxFileUploadSizeIsWithinLimits( value ) )
        {
            super.setMaxFileUploadSize( value );
        }
        else
        {
            super.setMaxFileUploadSize( maxMaxFileUploadSize );
        }
    }


    // ----------------------------------------------------------
    public long effectiveMaxFileUploadSize()
    {
        long value = maxFileUploadSize();
        return ( value > 0L ) ? value : maxMaxFileUploadSize;
    }


    // ----------------------------------------------------------
    public double correctnessPoints()
    {
        double cp = availablePoints() - taPoints() - toolPoints();
        if ( cp < 0.0 ) cp = 0.0;
        return cp;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>submissionMethod</code> value.
     * @return the value of the attribute
     */
    public String submissionMethodAsString()
    {
        int i = submissionMethod();
        return submitters[i];
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>submissionMethod</code>
     * property.
     * 
     * @param value The new value for this property
     */
    public void setSubmissionMethodAsString( String value )
    {
        for ( byte i = 0; i < submitters.length; i++ )
        {
            if ( submitters[i].equals( value ) )
            {
                setSubmissionMethod( i );
                return;
            }
        }
        setSubmissionMethod( (byte)0 );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve all submission profiles used by assignments associated
     * with the given course.  Also guarantees that an additional
     * submission profile, if specified, is included in the list.
     * 
     * @param context The editing context to use
     * @param user   The user who's profiles should be listed
     * @param course The course to match against
     * @param mine An additional submission profile to include--probably
     * one that wouldn't be picked up by the fetch specification, say because
     * it has been added but not yet committed.  This parameter is only
     * added to the result if it is not already in the results of the fetch.
     * @return an NSArray of the entities retrieved
     */
    public static NSArray profilesForCourseIncludingMine(
            EOEditingContext context,
            net.sf.webcat.core.User user,
            net.sf.webcat.core.Course course,
            SubmissionProfile mine
        )
    {
        NSMutableArray results =
            objectsForCourse( context, course ).mutableClone();
        er.extensions.ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
            results, 
            objectsForUser( context, user ) );
        if ( mine != null && !results.containsObject( mine ) )
        {
            results.addObject( mine );
        }
        return results;
    }


    // ----------------------------------------------------------
    public NSArray timeUnits()
    {
        return timeUnitsArray;
    }


    // ----------------------------------------------------------
    public NSArray submitters()
    {
        return submittersArray;
    }


    // ----------------------------------------------------------
    public static class TimeUnit
    {
        public TimeUnit( String name, long factor )
        {
            this.name   = name;
            this.factor = factor;
        }

        public String name()
        {
            return name;
        }

        public long factor()
        {
            return factor;
        }

        public long unitsFromRaw( long raw )
        {
            return raw / factor;
        }
        
        public long rawFromUnits( long units )
        {
            return units * factor;
        }

        public boolean isUnitFor( long raw )
        {
            return ( raw % factor ) == 0;
        }
        
        private String name;
        private long   factor;
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


    //~ Instance/static variables .............................................

    public static final TimeUnit[] timeUnits = new TimeUnit[] {
            new TimeUnit( "Minute(s)",                60000L ),
            new TimeUnit( "Hour(s)",              60L*60000L ),
            new TimeUnit( "Day(s)",           24L*60L*60000L ),
            new TimeUnit( "Week(s)",       7L*24L*60L*60000L ),
            new TimeUnit( "Month(s)",  30L*7L*24L*60L*60000L )
        };

    public static final String[] submitters = new String[] {
        "Not listed for external submission",
        "List for BlueJ submitter",
        "List for Eclipse submitter"
    };

    public static final NSArray timeUnitsArray = new NSArray( timeUnits );

    public static final NSArray submittersArray = new NSArray( submitters );

    static final long maxMaxFileUploadSize = net.sf.webcat.core.Application
        .configurationProperties()
        .longForKeyWithDefault( "grader.maxFileUploadSize", 200000L );

    static Logger log = Logger.getLogger( SubmissionProfile.class );
}
