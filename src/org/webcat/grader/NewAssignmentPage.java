/*==========================================================================*\
 |  $Id: NewAssignmentPage.java,v 1.4 2014/06/16 17:30:02 stedwar2 Exp $
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

import java.util.GregorianCalendar;
import org.apache.log4j.Logger;
import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.Semester;
import org.webcat.ui.generators.JavascriptGenerator;
import org.webcat.ui.util.ComponentIDGenerator;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.foundation.NSValidation;
import er.extensions.foundation.ERXArrayUtilities;

//-------------------------------------------------------------------------
/**
 * Allows the user to create a new assignment + offering.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.4 $, $Date: 2014/06/16 17:30:02 $
 */
public class NewAssignmentPage
    extends GraderCourseComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new object.
     *
     * @param context The context to use
     */
    public NewAssignmentPage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public String aName;
    public String title;
    public String targetCourse;
    public boolean reuseOpen = false;

    public ComponentIDGenerator idFor;
    public Semester          toSemester;
    public CourseOffering    toCourseOffering;
    public Assignment        assignmentToReoffer;

    public Semester                semesterInRepetition;
    public NSArray<CourseOffering> toCourseOfferings;
    public CourseOffering          courseOfferingInRepetition;
    public NSArray<Assignment>     assignments;
    public Assignment              assignmentInRepetition;


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
        idFor = new ComponentIDGenerator(this);
        if (coreSelections().course() == null
            && coreSelections().courseOffering() == null)
        {
            if (prefs().assignmentOffering() != null)
            {
                coreSelections().setCourseRelationship(
                    prefs().assignmentOffering().courseOffering().course());
            }
            else if (prefs().assignment() != null
                && prefs().assignment().offerings().count() > 0)
            {
                coreSelections().setCourseRelationship(
                    prefs().assignment().offerings().objectAtIndex(0)
                        .courseOffering().course());
            }
        }
        if (coreSelections().course() == null
            && coreSelections().courseOffering() == null)
        {
            targetCourse = "Please select a course ->";
        }
        else if (coreSelections().course() != null)
        {
            Semester semester = coreSelections().semester();
            targetCourse = coreSelections().course().toString()
                + " (all offerings, "
                + ((semester == null) ? "all semesters" : semester.toString())
                + ")";
            forAllSections = Boolean.TRUE;
        }
        else
        {
            targetCourse = coreSelections().courseOffering().compactName();
            if (forAllSections == null)
            {
                forAllSections = Boolean.TRUE;
            }
        }
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public boolean allowsAllSemesters()
    {
        return false;
    }


    // ----------------------------------------------------------
    public boolean allowsAllOfferingsForCourse()
    {
        return true;
    }


    // ----------------------------------------------------------
    public boolean requiresAssignmentOffering()
    {
        // Want to show all offerings for this assignment.
        return false;
    }


    // ----------------------------------------------------------
    public WOComponent reoffer()
    {
        AssignmentOffering newOffering = AssignmentOffering.create(
            localContext(),
            assignmentToReoffer,
            toCourseOffering);
        localContext().insertObject(newOffering);
        prefs().setAssignmentOfferingRelationship(newOffering);
        prefs().setAssignmentRelationship(assignmentToReoffer);
        configureNewAssignmentOffering(newOffering, null);
        applyLocalChanges();
        return super.next();
    }


    // ----------------------------------------------------------
    /**
     * Create a new assignment object as well as its associated assignment
     * offering objects, and then move on to an edit page.
     * @see org.webcat.core.WCComponent#next()
     */
    public WOComponent next()
    {
        if (aName == null || aName.length() == 0)
        {
            error("Please enter a name for your assignment.");
            return null;
        }

        Semester semester = coreSelections().semester();
        Course course = coreSelections().course();
        NSArray<CourseOffering> offerings = null;
        if (course == null
            && forAllSections()
            && coreSelections().courseOffering() != null)
        {
            course = coreSelections().courseOffering().course();
            semester = coreSelections().courseOffering().semester();
        }
        if (course != null)
        {
            offerings = course.offerings();
            if (semester != null)
            {
                NSMutableArray<CourseOffering> fullOfferings =
                    offerings.mutableClone();
                for (int i = 0; i < fullOfferings.count(); i++)
                {
                    if (fullOfferings.objectAtIndex(i).semester() != semester)
                    {
                        fullOfferings.remove(i);
                        i--;
                    }
                }
                offerings = fullOfferings;
            }
        }
        else
        {
            offerings = new NSArray<CourseOffering>(
                coreSelections().courseOffering());
        }

        if (offerings == null || offerings.count() == 0)
        {
            error("Please select a course in which to create the assignment.");
            return null;
        }

        log.debug("creating new assignment");
        Assignment newAssignment = Assignment.create(localContext(), false);
        newAssignment.setShortDescription(title);
        newAssignment.setAuthorRelationship(user());
        // Make sure the name is set first, so that date guessing works
        // correctly.
        newAssignment.setName(aName);

        NSArray<AssignmentOffering> similar = AssignmentOffering
            .offeringsWithSimilarNames(
                localContext(),
                aName,
                course,
                1);
        if (similar.count() > 0)
        {
            AssignmentOffering model = similar.objectAtIndex(0);
            newAssignment.setSubmissionProfile(
                model.assignment().submissionProfile());
            newAssignment.setTrackOpinions(
                model.assignment().trackOpinions());
            for (Step oldStep : model.assignment().steps())
            {
                Step newStep = Step.create(localContext(), false);
                newStep.setAssignmentRelationship(newAssignment);
                newStep.setOrder(oldStep.order());
                newStep.setTimeout(oldStep.timeout());
                newStep.setConfigRelationship(oldStep.config());
                newStep.setGradingPluginRelationship(oldStep.gradingPlugin());
            }
        }


        NSTimestamp common = newAssignment.commonOfferingsDueDate();
        AssignmentOffering firstOffering = null;

        for (CourseOffering offering: offerings)
        {
            log.debug("creating new assignment offering for " + offering);
            AssignmentOffering newOffering = AssignmentOffering.create(
                localContext(),
                newAssignment,
                offering);
            if (firstOffering == null)
            {
                firstOffering = newOffering;
            }
            prefs().setAssignmentOfferingRelationship(newOffering);
            configureNewAssignmentOffering(newOffering, common);
        }

        // Now clear the name so that the validation check will be performed
        newAssignment.setName(null);
        try
        {
            newAssignment.validateName(aName);
        }
        catch (NSValidation.ValidationException e)
        {
            error(e);
            cancelLocalChanges();
            return null;
        }
        // Finally, put the name back
        newAssignment.setName(aName);
        applyLocalChanges();
        prefs().setAssignmentRelationship(newAssignment);
        prefs().setAssignmentOfferingRelationship(firstOffering);
        applyLocalChanges();

        return super.next();
    }


    // ----------------------------------------------------------
    public void configureNewAssignmentOffering(
        AssignmentOffering newOffering, NSTimestamp commonDueDate)
    {
        NSTimestamp ts = new NSTimestamp();

        // first, look for any other assignments, and use their due date
        // as a default
        {
            AssignmentOffering other = null;
//            System.out.println("ao = " + newOffering);
//            System.out.println("assignment = " + newOffering.assignment());
//            System.out.println("offerings = " + newOffering.assignment().offerings());
            NSArray<AssignmentOffering> others =
                newOffering.assignment().offerings();
            for (AssignmentOffering ao : others)
            {
                if (ao != newOffering)
                {
                    other = ao;
                    break;
                }
            }
            if (other == null)
            {
                if (commonDueDate != null)
                {
                    ts = commonDueDate;
                }
                else
                {
                    GregorianCalendar dueDateTime = new GregorianCalendar();
                    dueDateTime.setTime(ts
                        .timestampByAddingGregorianUnits(0, 0, 15, 18, 55, 00));
                    dueDateTime.set(GregorianCalendar.AM_PM,
                                    GregorianCalendar.PM );
                    dueDateTime.set(GregorianCalendar.HOUR   , 11);
                    dueDateTime.set(GregorianCalendar.MINUTE , 55);
                    dueDateTime.set(GregorianCalendar.SECOND , 0);

                    ts = new NSTimestamp(dueDateTime.getTime());
                }
            }
            else
            {
                ts = other.dueDate();
            }
        }

        // Next, look for assignments for this course with similar names,
        // and try to spot a trend
        String name1 = newOffering.assignment().name();
        if (name1 != null)
        {
            NSMutableArray<AssignmentOffering> others =
                AssignmentOffering.offeringsWithSimilarNames(
                    localContext(), name1,
                    newOffering.courseOffering(), 2);
            if (others.count() > 1)
            {
                AssignmentOffering ao1 = others.objectAtIndex(0);
                GregorianCalendar ao1DateTime = new GregorianCalendar();
                ao1DateTime.setTime(ao1.dueDate());
                AssignmentOffering ao2 = others.objectAtIndex(1);
                GregorianCalendar ao2DateTime = new GregorianCalendar();
                ao2DateTime.setTime(ao2.dueDate());

                if (ao1DateTime.get(GregorianCalendar.HOUR_OF_DAY)
                    == ao2DateTime.get(GregorianCalendar.HOUR_OF_DAY)
                    && ao1DateTime.get(GregorianCalendar.MINUTE)
                    == ao2DateTime.get(GregorianCalendar.MINUTE)
                    )
                {
                    int days = ao1DateTime.get(GregorianCalendar.DAY_OF_YEAR)
                        - ao2DateTime.get(GregorianCalendar.DAY_OF_YEAR);
                    if (days < 0)
                    {
                        GregorianCalendar yearLen = new GregorianCalendar(
                            ao1DateTime.get(GregorianCalendar.YEAR),
                            0, 1);
                        yearLen.add(GregorianCalendar.DAY_OF_YEAR, -1);
                        days += yearLen.get(GregorianCalendar.DAY_OF_YEAR);
                    }

                    log.debug("day gap: " + days);
                    log.debug("old time: " + ao1DateTime);
                    ao1DateTime.add(GregorianCalendar.DAY_OF_YEAR, days);
                    GregorianCalendar today = new GregorianCalendar();
                    while (today.after(ao1DateTime))
                    {
                        ao1DateTime.add(GregorianCalendar.DAY_OF_YEAR, 7);
                    }
                    log.debug("new time: " + ao1DateTime);
                    ts = new NSTimestamp(ao1DateTime.getTime());
                }
                else
                {
                    ts = new NSTimestamp(
                        adjustTimeLike(ts, ao1DateTime).getTime());
                }
            }
            else if (others.count() > 0)
            {
                AssignmentOffering ao = others.objectAtIndex(0);
                GregorianCalendar aoDateTime = new GregorianCalendar();
                aoDateTime.setTime(ao.dueDate());
                ts = new NSTimestamp(
                    adjustTimeLike(ts, aoDateTime).getTime());
            }
        }

        newOffering.setDueDate(ts);
    }


    // ----------------------------------------------------------
    public boolean forAllSections()
    {
        return forAllSections != null && forAllSections.booleanValue();
    }


    // ----------------------------------------------------------
    public void setForAllSections(boolean value)
    {
        forAllSections = Boolean.valueOf(value);
    }


    // ----------------------------------------------------------
    public boolean hasMultipleSections()
    {
        NSArray<CourseOffering> offerings = null;
        Course course = coreSelections().course();
        if (course == null && coreSelections().courseOffering() != null)
        {
            course = coreSelections().courseOffering().course();
        }
        Semester semester = coreSelections().semester();
        if (semester == null && coreSelections().courseOffering() != null)
        {
            semester = coreSelections().courseOffering().semester();
        }
        if (course != null && semester != null)
        {
            offerings = CourseOffering.offeringsForSemesterAndCourse(
                localContext(), course, semester);
        }
        return offerings != null && offerings.count() > 1;
    }


    // ----------------------------------------------------------
    public NSArray<Semester> semesters()
    {
        if ( semesters == null )
        {
            semesters =
                Semester.allObjectsOrderedByStartDate( localContext() );
            toSemester = coreSelections().semester();

            updateReofferPane();

        }
        return semesters;
    }


    // ----------------------------------------------------------
    public JavascriptGenerator updateReofferPane()
    {
        if (myCourses == null)
        {
            myCourses = ERXArrayUtilities.sortedArraySortedWithKeys(
                user().teaching(),
                new NSArray<String>(
                    new String[] {"course.number", "label", "crn" }),
                null);
        }

        if (savedToSemester != toSemester)
        {
            toCourseOfferings = null;
            savedToSemester = toSemester;
        }

        if (toCourseOfferings == null)
        {
            toCourseOffering = null;
            // TODO: collapse the next two statements and eliminate the
            // temporary, once it will compile without warnings against
            // WONDER.
            @SuppressWarnings("unchecked")
            NSArray<CourseOffering> newList =
                ERXArrayUtilities.filteredArrayWithQualifierEvaluation(
                    myCourses,
                    CourseOffering.semester.eq(toSemester));
            toCourseOfferings = newList;
        }

        if (toCourseOffering == null)
        {
            assignments = null;
            Course selected = coreSelections().course();
            if (selected == null && coreSelections().courseOffering() != null)
            {
                selected = coreSelections().courseOffering().course();
            }
            if (selected != null)
            {
                toCourseOffering = (CourseOffering)
                    ERXArrayUtilities.firstObjectWithValueForKeyPath(
                        toCourseOfferings, selected, "course");
            }
        }

        if (savedToCourseOffering != toCourseOffering)
        {
            assignments = null;
            savedToCourseOffering = toCourseOffering;
        }

        if (assignments == null && toCourseOffering != null)
        {
            assignmentToReoffer = null;
            // TODO: collapse the next two statements and eliminate the
            // temporary, once it will compile without warnings against
            // WONDER.
            @SuppressWarnings("unchecked")
            NSArray<Assignment> newList =
                ERXArrayUtilities.filteredArrayWithQualifierEvaluation(
                    Assignment.assignmentsForReuseInCourse(
                        localContext(),
                        toCourseOffering.course(),
                        toCourseOffering),
                    new Assignment.NonDuplicateAssignmentNameQualifier(
                        toCourseOffering));
            assignments = newList;
        }

        if (assignmentToReoffer == null
            && assignments != null
            && assignments.count() > 0)
        {
            assignmentToReoffer = assignments.objectAtIndex(0);
        }

        reuseOpen = true;
        return new JavascriptGenerator()
            .refresh((String) idFor.valueForKey("reofferPane"));
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    private GregorianCalendar adjustTimeLike(
        NSTimestamp starting, GregorianCalendar similarTo)
    {
        GregorianCalendar result = new GregorianCalendar();
        result.setTime(starting);

        // First, copy the time and day of the week from the old
        // assignment
        result.set(GregorianCalendar.HOUR_OF_DAY,
                   similarTo.get( GregorianCalendar.HOUR_OF_DAY));
        result.set(GregorianCalendar.MINUTE,
                   similarTo.get( GregorianCalendar.MINUTE));
        result.set(GregorianCalendar.SECOND,
                   similarTo.get( GregorianCalendar.SECOND));
        result.set(GregorianCalendar.DAY_OF_WEEK,
                   similarTo.get( GregorianCalendar.DAY_OF_WEEK));

        // jump ahead by weeks until we're in the future
        GregorianCalendar today = new GregorianCalendar();
        while (today.after(result))
        {
            result.add(GregorianCalendar.DAY_OF_YEAR, 7);
        }

        return result;
    }


    //~ Instance/static variables .............................................

    private NSArray<Semester>       semesters;
    private Boolean                 forAllSections = null;
    private Semester                savedToSemester;
    private CourseOffering          savedToCourseOffering;
    private NSArray<CourseOffering> myCourses;
    static Logger log = Logger.getLogger(NewAssignmentPage.class);
}
