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
import er.extensions.ERXConstant;
import er.extensions.ERXValueUtilities;
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
public class PickAssignmentToSubmitPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new PickAssignmentPage object.
     * 
     * @param context The context to use
     */
    public PickAssignmentToSubmitPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public AssignmentOffering anAssignmentOffering;
    public WODisplayGroup     assignmentDisplayGroup;
    public int                index             = -1;
    public int                selectedIndex     = -1;
    public boolean            showHaltedMessage = false;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "starting appendToResponse()" );
        showHaltedMessage = false;
        NSTimestamp currentTime   = new NSTimestamp();
        NSMutableArray qualifiers = new NSMutableArray();
        CourseOffering selectedCourse = wcSession().courseOffering();
        //
        // assignments for this course
        //
        qualifiers.addObject( new EOKeyValueQualifier(
            AssignmentOffering.COURSE_OFFERING_KEY,
            EOQualifier.QualifierOperatorEqual,
            selectedCourse ) );
//        assignmentDisplayGroup.setQualifier(
//            new EOAndQualifier( qualifiers ) );
//        log.debug( "qualifier = " + assignmentDisplayGroup.qualifier() );
//        assignmentDisplayGroup.fetch();
//        log.debug( "results = " + assignmentDisplayGroup.displayedObjects() );

        NSDictionary config = wcSession().tabs.selectedDescendant().config();
        if ( config == null ||
             !ERXValueUtilities.booleanValueWithDefault(
                             config.objectForKey( "all" ), false ) )
        {
            //
            // assigments which are still open
            //
            if ( !(  selectedCourse.isInstructor( wcSession().user() ) 
                            || selectedCourse.isTA( wcSession().user() ) ) )
            {
                qualifiers.addObject( new EOKeyValueQualifier(
                                  AssignmentOffering.AVAILABLE_FROM_KEY, 
                                  EOQualifier.QualifierOperatorLessThan, 
                                  currentTime
                                ) );
            }
            qualifiers.addObject( new EOKeyValueQualifier(
                                  AssignmentOffering.LATE_DEADLINE_KEY, 
                                  EOQualifier.QualifierOperatorGreaterThan,
                                  currentTime
                                ) );
        }
//        assignmentDisplayGroup.setQualifier(
//            new EOAndQualifier( qualifiers ) );
//        log.debug( "qualifier = " + assignmentDisplayGroup.qualifier() );
//        assignmentDisplayGroup.fetch();
//        log.debug( "results = " + assignmentDisplayGroup.displayedObjects() );

        //
        // assignments which are published
        //
        if ( !(  selectedCourse.isInstructor( wcSession().user() ) 
              || selectedCourse.isTA( wcSession().user() ) ) )
        {
            log.debug( "hiding unpublished assignments" );
            qualifiers.addObject( new EOKeyValueQualifier( 
                                      AssignmentOffering.PUBLISH_KEY, 
                                      EOQualifier.QualifierOperatorEqual,
                                      ERXConstant.integerForInt( 1 )
                                    ) );
        }
        assignmentDisplayGroup.setQualifier(
                new EOAndQualifier( qualifiers ) );
//        log.debug( "qualifier = " + assignmentDisplayGroup.qualifier() );
        assignmentDisplayGroup.fetch();
//        log.debug( "results = " + assignmentDisplayGroup.displayedObjects() );
        if ( assignmentDisplayGroup.displayedObjects().count() == 0 )
        {
            log.debug( "attempting second group fetch" );
            assignmentDisplayGroup.fetch();
        }
        AssignmentOffering selectedAssignment = prefs().assignmentOffering();
        if ( selectedAssignment != null )
        {
            selectedIndex =
                assignmentDisplayGroup.displayedObjects().
                    indexOfIdenticalObject( selectedAssignment );
            if ( selectedIndex == NSArray.NotFound )
            {
                prefs().setAssignmentOfferingRelationship( null );
                selectedAssignment = null;
            }
        }
        if ( selectedAssignment == null  &&
             assignmentDisplayGroup.displayedObjects().count() > 0 )
        {
            selectedIndex = 0;
            prefs().setAssignmentOfferingRelationship(
                    (AssignmentOffering)assignmentDisplayGroup
                        .displayedObjects().objectAtIndex( selectedIndex )
                );
        }

        super.appendToResponse( response, context );
        log.debug( "ending appendToResponse()" );
    }


    // ----------------------------------------------------------
    public boolean nextEnabled()
    {
        return assignmentDisplayGroup.displayedObjects().count() > 0
            &&  super.nextEnabled();
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        if ( selectedIndex == -1 )
        {
            error( "You must choose an assignment to proceed." );
            return null;
        }
        else // if ( selectedIndex > -1 )
        {
            log.debug( "existing assignment selected ("
                       + selectedIndex
                       + ")" );
            prefs().setAssignmentOfferingRelationship(
                (AssignmentOffering)assignmentDisplayGroup
                    .displayedObjects().objectAtIndex( selectedIndex ) );
            return super.next();
        }
    }


    // ----------------------------------------------------------
    /**
     * A boolean predicate used to determine whether or not grading
     * has been halted for the current assignment in the display group.
     * This result is also silently accumulated in the
     * showGradingHaltedMessage data member.
     *
     * @return true if grading has been halted for this assignment
     */
    public boolean gradingIsHalted()
    {
        boolean result = anAssignmentOffering.gradingSuspended();
        showHaltedMessage |= result;
        return result;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( PickAssignmentToSubmitPage.class );
}
