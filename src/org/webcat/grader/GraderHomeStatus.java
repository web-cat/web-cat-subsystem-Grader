/*==========================================================================*\
 |  $Id$
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
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 *  Generates the grader subsystem's rows in the system status block.
 *
 *  @author  Stephen Edwards
 *  @author Last changed by $Author$
 *  @version $Revision$, $Date$
 */
public class GraderHomeStatus
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderSystemStatusRows object.
     *
     * @param context The page's context
     */
    public GraderHomeStatus( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup     enqueuedJobGroup;
    public EnqueuedJob        job;
    public WODisplayGroup     assignmentGroup;
    public WODisplayGroup     upcomingAssignmentsGroup;
    public AssignmentOffering assignment;
    public int                index;


    //~ Methods ...............................................................

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
        log.debug( "starting appendToResponse()" );

        enqueuedJobGroup.queryBindings().setObjectForKey(
                user(),
                "user"
            );
        enqueuedJobGroup.fetch();

        currentTime = new NSTimestamp();
        NSMutableArray<EOQualifier> qualifiers =
            new NSMutableArray<EOQualifier>();
        qualifiers.addObject( new EOKeyValueQualifier(
                AssignmentOffering.AVAILABLE_FROM_KEY,
                EOQualifier.QualifierOperatorLessThan,
                currentTime
            ) );
        qualifiers.addObject( new EOKeyValueQualifier(
                AssignmentOffering.PUBLISH_KEY,
                EOQualifier.QualifierOperatorEqual,
                ERXConstant.integerForInt( 1 )
            ) );
        qualifiers.addObject( new EOKeyValueQualifier(
                AssignmentOffering.COURSE_OFFERING_STUDENTS_KEY,
                EOQualifier.QualifierOperatorContains,
                user()
            ) );
        qualifiers = new NSMutableArray<EOQualifier>(
            new EOAndQualifier(qualifiers));
        qualifiers.addObject( new EOKeyValueQualifier(
                AssignmentOffering.COURSE_OFFERING_INSTRUCTORS_KEY,
                EOQualifier.QualifierOperatorContains,
                user()
            ) );
        qualifiers.addObject( new EOKeyValueQualifier(
                AssignmentOffering.COURSE_OFFERING_GRADERS_KEY,
                EOQualifier.QualifierOperatorContains,
                user()
            ) );
        qualifiers = new NSMutableArray<EOQualifier>(
            new EOOrQualifier(qualifiers));
        EOQualifier deadlineQualifier = new EOKeyValueQualifier(
            AssignmentOffering.LATE_DEADLINE_KEY,
            EOQualifier.QualifierOperatorGreaterThan,
            currentTime
            );
        qualifiers.addObject( deadlineQualifier );
        EOQualifier assignmentQualifier = new EOAndQualifier( qualifiers );
        assignmentGroup.setQualifier( assignmentQualifier );
        if (log.isDebugEnabled())
        {
            log.debug( "qualifier = " + assignmentGroup.qualifier() );
        }
        assignmentGroup.fetch();
        if (log.isDebugEnabled())
        {
            log.debug( "results = " + assignmentGroup.displayedObjects() );
        }

        // Now set up the upcoming assignments list
        qualifiers = new NSMutableArray<EOQualifier>(deadlineQualifier);
        // Also, more recent than two weeks ago
        qualifiers.addObject( new EOKeyValueQualifier(
            AssignmentOffering.DUE_DATE_KEY,
            EOQualifier.QualifierOperatorGreaterThan,
            currentTime.timestampByAddingGregorianUnits( 0, 0, -14, 0, 0, 0 )
            ) );
        // Also, some time within the next 4 weeks
        qualifiers.addObject( new EOKeyValueQualifier(
            AssignmentOffering.DUE_DATE_KEY,
            EOQualifier.QualifierOperatorLessThan,
            currentTime.timestampByAddingGregorianUnits( 0, 0, 28, 0, 0, 0 )
            ) );
        qualifiers.addObject( new EONotQualifier( assignmentQualifier ) );
        upcomingAssignmentsGroup.setQualifier(
            new EOAndQualifier( qualifiers ) );
        upcomingAssignmentsGroup.fetch();
        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    /**
     * View results for the most recent submission to the selected assignments.
     *
     * @return the most recent results page
     */
    public Number mostRecentScore()
    {
        SubmissionResult subResult = assignment.mostRecentSubmissionResultFor(
            user());
        return (subResult == null)
            ? null
            : new Double(subResult.automatedScore());
    }


    // ----------------------------------------------------------
    /**
     * Check whether the user can edit the selected assignment.
     *
     * @return true if the user can edit the assignment
     */
    public boolean canEditAssignment()
    {
        return assignment.courseOffering().isInstructor( user() );
    }


    // ----------------------------------------------------------
    /**
     * Check whether the user can edit the selected assignment.
     *
     * @return true if the user can edit the assignment
     */
    public boolean canGradeAssignment()
    {
        boolean result =
            assignment.courseOffering().isInstructor(user())
            || assignment.courseOffering().isGrader(user());
        log.debug("can grade = " + result);
        return result;
    }


    // ----------------------------------------------------------
    /**
     * An action to go to the submission page for a given assignment.
     *
     * @return the submission page for the selected assignment
     */
    public WOComponent submitAssignment()
    {
        coreSelections().setCourseOfferingRelationship(
            assignment.courseOffering());
        prefs().setAssignmentOfferingRelationship(assignment);
        return pageWithName(
            wcSession().tabs.selectById("UploadSubmission").pageName());
    }


    // ----------------------------------------------------------
    /**
     * View results for the most recent submission to the selected assignments.
     *
     * @return the most recent results page
     */
    public WOComponent viewResults()
    {
        coreSelections().setCourseOfferingRelationship(
            assignment.courseOffering());
        prefs().setAssignmentOfferingRelationship(assignment);
        SubmissionResult subResult =
            assignment.mostRecentSubmissionResultFor(user());
        String destinationPageName = null;
        if (subResult != null)
        {
            prefs().setSubmissionRelationship( subResult.submission());
            destinationPageName =
                wcSession().tabs.selectById("MostRecent").pageName();
        }
        else
        {
            destinationPageName =
                wcSession().tabs.selectById("MostRecent").pageName();
        }
        return pageWithName(destinationPageName);
    }


    // ----------------------------------------------------------
    /**
     * An action to go to the graphing page for a given assignment.
     *
     * @return the graphing page for the selected assignment
     */
    public WOComponent graphResults()
    {
        coreSelections().setCourseOfferingRelationship(
            assignment.courseOffering());
        prefs().setAssignmentOfferingRelationship(assignment);
        return pageWithName(
            wcSession().tabs.selectById("GraphResults").pageName());
    }


    // ----------------------------------------------------------
    /**
     * An action to go to edit page for a given assignment.
     *
     * @return the properties page for the selected assignment
     */
    public WOComponent editAssignment()
    {
        coreSelections().setCourseOfferingRelationship(
            assignment.courseOffering());
        prefs().setAssignmentOfferingRelationship(assignment);
        return pageWithName(
            wcSession().tabs.selectById("AssignmentProperties").pageName());
    }


    // ----------------------------------------------------------
    /**
     * An action to go to edit page for a given assignment.
     *
     * @return the properties page for the selected assignment
     */
    public WOComponent gradeAssignment()
    {
        coreSelections().setCourseOfferingRelationship(
            assignment.courseOffering());
        prefs().setAssignmentOfferingRelationship(assignment);
        return pageWithName(
            wcSession().tabs.selectById("EnterGrades").pageName());
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
                 || assignment.courseOffering().instructors()
                     .containsObject( user() ) )
               && assignment.getSuspendedSubs().count() > 0;
    }


    //~ Instance/static variables .............................................

    private NSTimestamp currentTime;
    static Logger log = Logger.getLogger( GraderHomeStatus.class );
}
