/*==========================================================================*\
 |  $Id: PickAssignmentToSubmitPage.java,v 1.3 2011/03/23 15:10:56 aallowat Exp $
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

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.eof.ERXConstant;
import er.extensions.foundation.ERXValueUtilities;
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 *  This page presents a list of assignments available for the user
 *  to choose from.
 *
 *  @author  Stephen Edwards
 *  @author  Latest changes by: $Author: aallowat $
 *  @version $Revision: 1.3 $, $Date: 2011/03/23 15:10:56 $
 */
public class PickAssignmentToSubmitPage
    extends GraderCourseComponent
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
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug( "starting appendToResponse()" );
        showHaltedMessage = false;
        NSTimestamp currentTime   = new NSTimestamp();
        NSMutableArray<EOQualifier> qualifiers =
            new NSMutableArray<EOQualifier>();
        CourseOffering selectedCourse = coreSelections().courseOffering();
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

        NSDictionary<String, Object> config =
            wcSession().tabs.selectedDescendant().config();
        if ( config == null ||
             !ERXValueUtilities.booleanValueWithDefault(
                             config.objectForKey( "all" ), false ) )
        {
            //
            // assigments which are still open
            //
            if ( !(  selectedCourse.isInstructor( user() )
                            || selectedCourse.isGrader( user() ) ) )
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
        if ( !(  selectedCourse.isInstructor( user() )
              || selectedCourse.isGrader( user() ) ) )
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

        super.beforeAppendToResponse( response, context );
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


    // ----------------------------------------------------------
    /**
     * Determine if the current assignment has suspended submissions (that
     * this user can see).
     *
     * @return true if the user can see this assignment's status and this
     * assignment has suspended submissions
     */
    public boolean assignmentHasSuspendedSubs()
    {
        return ( user().hasAdminPrivileges()
                 || anAssignmentOffering.courseOffering().instructors()
                     .containsObject( user() ) )
               && anAssignmentOffering.suspendedSubmissionsInQueue().count() > 0;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( PickAssignmentToSubmitPage.class );
}
