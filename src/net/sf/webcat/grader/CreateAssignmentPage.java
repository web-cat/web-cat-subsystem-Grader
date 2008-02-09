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

import er.extensions.*;

import java.util.GregorianCalendar;
import net.sf.webcat.core.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 *  This page presents a list of assignments available for the user
 *  to choose from.
 *
 *  @author  Stephen Edwards
 *  @version $Id$
 */
public class CreateAssignmentPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new PickAssignmentPage object.
     *
     * @param context The context to use
     */
    public CreateAssignmentPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup assignmentDisplayGroup;
    public Assignment     assignment;
    public int            selectedIndex = -1;
    public int            index         = -1;
    public NSArray        reusableAssignments;
    public NSArray             semesters;
    public Semester            semester;
    public Semester            aSemester;

    public static final String SEMESTER_PREF_KEY =
        "CreateAssignmentPage.semester";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "starting appendToResponse()" );
        index = -1;
        selectedIndex = -1;

        // First, take care of semester list and preference
        User user = wcSession().user();
        if ( semesters == null )
        {
            semesters =
                Semester.objectsForFetchAll( wcSession().localContext() );
            Object semesterPref = user.preferences()
                .valueForKey( SEMESTER_PREF_KEY );
            if (semesterPref == null && semesters.count() > 0)
            {
                // Default to most recent semester, if no preference is set
                semester = (Semester)semesters.objectAtIndex(0);
            }
            else
            {
                semester = Semester.forId( wcSession().localContext(),
                    ERXValueUtilities.intValue( semesterPref ) );
            }
        }
        // Save selected semester
        user.preferences().takeValueForKey(
            semester == null ? ERXConstant.ZeroInteger : semester.id(),
            SEMESTER_PREF_KEY );
        user.savePreferences();

        // Second, make sure list of reusable assignments is set
        if (reusableAssignments == null)
        {
            reusableAssignments =
                ERXArrayUtilities.filteredArrayWithQualifierEvaluation(
                    Assignment.objectsForReuseInCourse(
                        wcSession().localContext(),
                        coreSelections().courseOffering().course(),
                        coreSelections().courseOffering()
                    ),
                    new Assignment.NonDuplicateAssignmentNameQualifier(
                        coreSelections().courseOffering()
                    )
                );
            assignmentDisplayGroup.setObjectArray( reusableAssignments );
        }

        // Set semester filter, if necessary
        if (semester == null)
        {
            assignmentDisplayGroup.setQualifier(null);
            assignmentDisplayGroup.updateDisplayedObjects();
        }
        else
        {
            assignmentDisplayGroup.setQualifier(
                new EOKeyValueQualifier(
                    Assignment.SEMESTERS_KEY,
                    EOQualifier.QualifierOperatorContains,
                    semester
                ) );
            assignmentDisplayGroup.updateDisplayedObjects();
        }
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        if ( selectedIndex == -1 )
        {
            createNewAssignment();
        }
        else // if ( selectedIndex > -1 )
        {
            log.debug( "existing assignment selected ("
                       + selectedIndex
                       + ")" );
            reuseExistingAssignment();
        }
        WOComponent next = super.next();
        // advance twice, if necessary;
        log.debug( "next page = " + next );
        log.debug( "next page class = " + next.getClass().getName() );
        if ( next instanceof PickAssignmentToEditPage
           && currentTab().hasNextSibling() )
        {
            next = pageWithName(
                currentTab().nextSibling().select().pageName() );
        }
        return next;
    }


    // ----------------------------------------------------------
    public void reuseExistingAssignment()
    {
        log.debug( "reuseExistingAssignment()" );
        Assignment selected = (Assignment)assignmentDisplayGroup
            .displayedObjects().objectAtIndex( selectedIndex );
        NSTimestamp common = selected.commonOfferingsDueDate();
        AssignmentOffering newOffering = new AssignmentOffering();
        wcSession().localContext().insertObject( newOffering );
        newOffering.setAssignmentRelationship( selected );
        prefs().setAssignmentOfferingRelationship( newOffering );
        configureNewAssignmentOffering( common );
    }


    // ----------------------------------------------------------
    public void createNewAssignment()
    {
        log.debug( "createNewAssignment()" );
        Assignment newAssignment = new Assignment();
        wcSession().localContext().insertObject( newAssignment );
        AssignmentOffering newOffering = new AssignmentOffering();
        wcSession().localContext().insertObject( newOffering );
        newOffering.setAssignmentRelationship( newAssignment );
        prefs().setAssignmentOfferingRelationship( newOffering );
        newAssignment.setAuthorRelationship( wcSession().user() );
        configureNewAssignmentOffering( null );
    }


    // ----------------------------------------------------------
    public void configureNewAssignmentOffering( NSTimestamp commonDueDate )
    {
        AssignmentOffering newOffering = prefs().assignmentOffering();
        newOffering.setCourseOffering( coreSelections().courseOffering() );
        if ( commonDueDate != null )
        {
            newOffering.setDueDate( commonDueDate );
            return;
        }

        NSTimestamp ts = new NSTimestamp();

        // first, look for any other assignments, and use their due date
        // as a default
        {
            AssignmentOffering other = null;
            NSArray others =
                prefs().assignmentOffering().assignment().offerings();
            for ( int i = 0; i < others.count(); i++ )
            {
                AssignmentOffering ao =
                    (AssignmentOffering)others.objectAtIndex( i );
                if ( ao != prefs().assignmentOffering() )
                {
                    other = ao;
                    break;
                }
            }
            if ( other == null )
            {
                GregorianCalendar dueDateTime = new GregorianCalendar();
                dueDateTime.setTime( ts
                    .timestampByAddingGregorianUnits( 0, 0, 15, 18, 55, 00 ) );
                dueDateTime.set( GregorianCalendar.AM_PM,
                                 GregorianCalendar.PM );
                dueDateTime.set( GregorianCalendar.HOUR , 11 );
                dueDateTime.set( GregorianCalendar.MINUTE , 55 );
                dueDateTime.set( GregorianCalendar.SECOND , 0  );

                ts = new NSTimestamp( dueDateTime.getTime() );
            }
            else
            {
                ts = other.dueDate();
            }
        }

        // Next, look for assignments for this course with similar names,
        // and try to spot a trend
        String name1 = newOffering.assignment().name();
        if ( name1 != null )
        {
            NSMutableArray others =
                AssignmentOffering.offeringsWithSimilarNames(
                    wcSession().localContext(), name1,
                    coreSelections().courseOffering(), 2 );
            if ( others.count() > 1 )
            {
                AssignmentOffering ao1 =
                    (AssignmentOffering)others.objectAtIndex( 0 );
                GregorianCalendar ao1DateTime = new GregorianCalendar();
                ao1DateTime.setTime( ao1.dueDate() );
                AssignmentOffering ao2 =
                    (AssignmentOffering)others.objectAtIndex( 1 );
                GregorianCalendar ao2DateTime = new GregorianCalendar();
                ao2DateTime.setTime( ao2.dueDate() );

                if ( ao1DateTime.get( GregorianCalendar.HOUR_OF_DAY )
                     == ao2DateTime.get( GregorianCalendar.HOUR_OF_DAY )
                     && ao1DateTime.get( GregorianCalendar.MINUTE )
                     == ao2DateTime.get( GregorianCalendar.MINUTE )
                    )
                {
                    int days = ao1DateTime.get( GregorianCalendar.DAY_OF_YEAR )
                        - ao2DateTime.get( GregorianCalendar.DAY_OF_YEAR );
                    if ( days < 0 )
                    {
                        GregorianCalendar yearLen = new GregorianCalendar(
                            ao1DateTime.get( GregorianCalendar.YEAR ),
                            0, 1);
                        yearLen.add( GregorianCalendar.DAY_OF_YEAR, -1 );
                        days += yearLen.get( GregorianCalendar.DAY_OF_YEAR );
                    }

                    log.debug( "day gap: " + days );
                    log.debug( "old time: " + ao1DateTime );
                    ao1DateTime.add( GregorianCalendar.DAY_OF_YEAR, days );
                    GregorianCalendar today = new GregorianCalendar();
                    while ( today.after( ao1DateTime ) )
                    {
                        ao1DateTime.add( GregorianCalendar.DAY_OF_YEAR, 7 );
                    }
                    log.debug( "new time: " + ao1DateTime );
                    ts = new NSTimestamp( ao1DateTime.getTime() );
                }
                else
                {
                    ts = new NSTimestamp(
                        adjustTimeLike( ts, ao1DateTime ).getTime() );

                }
            }
            else if ( others.count() > 0 )
            {
                AssignmentOffering ao =
                    (AssignmentOffering)others.objectAtIndex( 0 );
                GregorianCalendar aoDateTime = new GregorianCalendar();
                aoDateTime.setTime( ao.dueDate() );
                ts = new NSTimestamp(
                    adjustTimeLike( ts, aoDateTime ).getTime() );
            }
        }

        newOffering.setDueDate( ts );
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        // When semester list changes, make sure not to take the
        // default action, which is to click "next".
        return null;
    }


    //~ Private Methods .......................................................

    private GregorianCalendar adjustTimeLike(
        NSTimestamp starting, GregorianCalendar similarTo )
    {
        GregorianCalendar result = new GregorianCalendar();
        result.setTime( starting );

        // First, copy the time and day of the week from the old
        // assignment
        result.set( GregorianCalendar.HOUR_OF_DAY,
                    similarTo.get( GregorianCalendar.HOUR_OF_DAY ) );
        result.set( GregorianCalendar.MINUTE,
                    similarTo.get( GregorianCalendar.MINUTE ) );
        result.set( GregorianCalendar.SECOND,
                    similarTo.get( GregorianCalendar.SECOND ) );
        result.set( GregorianCalendar.DAY_OF_WEEK,
                    similarTo.get( GregorianCalendar.DAY_OF_WEEK ) );

        // jump ahead by weeks until we're in the future
        GregorianCalendar today = new GregorianCalendar();
        while ( today.after( result ) )
        {
            result.add( GregorianCalendar.DAY_OF_YEAR, 7 );
        }

        return result;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( CreateAssignmentPage.class );
}
