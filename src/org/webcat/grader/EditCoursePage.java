/*==========================================================================*\
 |  $Id: EditCoursePage.java,v 1.2 2010/09/27 04:19:54 stedwar2 Exp $
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
import com.webobjects.foundation.*;
import org.apache.log4j.Logger;
import org.webcat.core.*;

//-------------------------------------------------------------------------
/**
 * Represents a standard Web-CAT page that has not yet been implemented
 * (is "to be defined").
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.2 $, $Date: 2010/09/27 04:19:54 $
 */
public class EditCoursePage
    extends GraderCourseEditComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new TBDPage object.
     *
     * @param context The context to use
     */
    public EditCoursePage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public WODisplayGroup      courseDisplayGroup;
    public WODisplayGroup      instructorDisplayGroup;
    public WODisplayGroup      TADisplayGroup;
    public Course              course;
    public User                aUser;
    public int                 index;
    public NSArray<Semester>   semesters;
    public Semester            aSemester;
    public boolean             earliestAndLatestComputed;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        if ( semesters == null )
        {
            semesters =
                Semester.allObjectsOrderedByStartDate(localContext());
        }
        instructorDisplayGroup.setMasterObject(courseOffering());
        TADisplayGroup.setMasterObject(courseOffering());
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public boolean allowsAllOfferingsForCourse()
    {
        return false;
    }


    // ----------------------------------------------------------
    public WOComponent cancel()
    {
        clearMessages();
        cancelLocalChanges();
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Used to filter out the current user from some functions.
     * @return true if the user we are iterating over is the same as
     *     the currently logged in user
     */
    public boolean matchesUser()
    {
        return aUser == user();
    }


    // ----------------------------------------------------------
    /**
     * Remove the selected instructor.
     * @return always null
     */
    public WOComponent removeInstructor()
    {
        courseOffering().removeFromInstructorsRelationship(aUser);
        return apply();
    }


    // ----------------------------------------------------------
    /**
     * Remove the selected TA.
     * @return always null
     */
    public WOComponent removeTA()
    {
        courseOffering().removeFromGradersRelationship(aUser);
        return apply();
    }


    // ----------------------------------------------------------
    /**
     * Add a new instructor.
     * @return the add instructor page
     */
    public WOComponent addInstructor()
    {
        EditStaffPage addPage = (EditStaffPage)pageWithName(
            EditStaffPage.class.getName());
        addPage.editInstructors = true;
        addPage.nextPage = this;
        return addPage;
    }


    // ----------------------------------------------------------
    /**
     * Add a new TA.
     * @return the add TA page
     */
    public WOComponent addTA()
    {
        EditStaffPage addPage = (EditStaffPage)pageWithName(
            EditStaffPage.class.getName());
        addPage.editInstructors = false;
        addPage.nextPage = this;
        return addPage;
    }


    // ----------------------------------------------------------
    /**
     * Edit the student roster.
     * @return the roster page
     */
    public WOComponent editRoster()
    {
        CourseRosterPage page = (CourseRosterPage)pageWithName(
            CourseRosterPage.class.getName());
        page.nextPage = this;
        return page;
    }


    // ----------------------------------------------------------
    public WOComponent deleteActionOk()
    {
        if (!applyLocalChanges()) return null;
        CourseOffering thisOffering = courseOffering();
        setCourseOffering(null);
        coreSelections().setCourseOfferingRelationship(null);
        localContext().deleteObject(thisOffering);
        return finish();
    }


    // ----------------------------------------------------------
    public WOComponent delete()
    {
        ConfirmPage confirmPage = null;
        confirmPage =
            (ConfirmPage)pageWithName(ConfirmPage.class.getName());
        confirmPage.nextPage       = this;
        confirmPage.message        =
            "This action will <b>delete the course offering</b>. "
            + "This action cannot be undone.</p>";
        confirmPage.actionReceiver = this;
        confirmPage.actionOk       = "deleteActionOk";
        confirmPage.setTitle("Confirm Delete Request");
        return confirmPage;
    }


    // ----------------------------------------------------------
    /**
     * Find the dates of the earliest and latest submissions for
     * any assignment associated with this course.
     * @return null, to force a page refresh
     */
    public WOComponent computeSubmissionDateRange()
    {
        log.debug("computeSubmissionDateRange()");
        Submission earliestSub = Submission.earliestSubmissionForCourseOffering(
            localContext(), courseOffering());
        if (earliestSub != null)
        {
            earliest = earliestSub.submitTime();
            Submission latestSub = Submission.latestSubmissionForCourseOffering(
                    localContext(), courseOffering());
            latest = latestSub.submitTime();
        }
        earliestAndLatestComputed = true;
        return null;
    }


    // ----------------------------------------------------------
    public NSTimestamp earliest()
    {
        if (!earliestAndLatestComputed)
        {
            computeSubmissionDateRange();
        }
        return earliest;
    }


    // ----------------------------------------------------------
    public NSTimestamp latest()
    {
        if (!earliestAndLatestComputed)
        {
            computeSubmissionDateRange();
        }
        return latest;
    }


    // ----------------------------------------------------------
    public WOResponse accumulate()
    {
        accum += " and another";
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Extracts course offering identification from the given startup
     * parameters.  Note that this is copied directly from
     * {@link GraderCourseComponent}, but can't be inherited because of
     * MI restrictions.
     * @param params A dictionary of form values to decode
     * @return True if successful, false if the parameter is missing
     */
    public boolean startWith(NSDictionary<String, Object> params)
    {
        boolean result = false;
        String crn = stringValueForKey(params, CourseOffering.CRN_KEY);
        if (crn != null)
        {
            result = startWith(CourseOffering
                .offeringForCrn(localContext(), crn));
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Sets the relevant course and course offering properties for this
     * session.  Note that this is copied directly from
     * {@link GraderCourseComponent}, but can't be inherited because of
     * MI restrictions.
     * @param offering the course offering to use for generating settings
     * @return True if successful, false if the course offering is not valid
     */
    protected boolean startWith(CourseOffering offering)
    {
        boolean result = false;
        User sessionUser = user();
        if (offering != null
             && (sessionUser.enrolledIn().contains(offering)
                  || offering.isInstructor(sessionUser)
                  || offering.isGrader(sessionUser)))
        {
            result = true;
            coreSelections().setCourseRelationship(offering.course());
            coreSelections().setCourseOfferingRelationship(offering);
        }
        return result;
    }


    //~ Instance/static variables .............................................

    private NSTimestamp  earliest;
    private NSTimestamp  latest;
    public  String       accum = "A";

    static Logger log = Logger.getLogger(EditCoursePage.class);
}
