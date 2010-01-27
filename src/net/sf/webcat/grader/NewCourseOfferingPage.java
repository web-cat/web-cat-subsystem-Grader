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

package net.sf.webcat.grader;

import java.util.Calendar;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;
import er.extensions.appserver.ERXDisplayGroup;
import er.extensions.eof.ERXQ;
import net.sf.webcat.core.*;
import net.sf.webcat.ui.generators.JavascriptGenerator;

//-------------------------------------------------------------------------
/**
 * Allows the user to create a new course offering.
 *
 * @author Stephen Edwards
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class NewCourseOfferingPage
    extends GraderCourseEditComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new object.
     *
     * @param context The context to use
     */
    public NewCourseOfferingPage( WOContext context )
    {
        super( context );
        nextPerformsSave = true;
    }


    //~ KVC Attributes (must be public) .......................................

    public Course               course;
    public ERXDisplayGroup<Course> courseDisplayGroup;
    public NSArray<AuthenticationDomain> institutions;
    public AuthenticationDomain institution;
    public AuthenticationDomain anInstitution;
    public Semester            semester;
    public NSArray<Semester>   semesters;
    public Semester            aSemester;
    public String              crn;
    public Integer             aSeason;
    public Department          aDepartment;
    public String              newCourseName = "Intro to Programming";
    public NSTimestamp         startDate;
    public NSTimestamp         endDate;


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
        log.debug("appendToResponse()");
        if ( semesters == null )
        {
            semesters =
                Semester.allObjectsOrderedByStartDate(localContext());
            if (semesters.size() > 0)
            {
                semester = semesters.get(0);
            }
        }
        if (institutions == null)
        {
            institutions = AuthenticationDomain.authDomains();
            institution = user().authenticationDomain();
        }

        if (coreSelections().courseOffering() != null)
        {
            coreSelections().setCourseRelationship(
                coreSelections().courseOffering().course());
        }
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public String refilterCourseList()
    {
        if (allCourses == null)
        {
            allCourses = Course.allObjects(localContext());
        }
        if (institution == null)
        {
            courseDisplayGroup.setObjectArray(allCourses);
        }
        else
        {
            courseDisplayGroup.setObjectArray(
                ERXQ.filtered(
                    allCourses,
                    Course.department.dot(Department.institution)
                        .is(institution)));
        }
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        // When semester list changes, make sure not to take the
        // default action, which is to click "next".
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Create a new course offering object and move on to an edit page.
     * @see net.sf.webcat.core.WCComponent#next()
     */
    public WOComponent next()
    {
        if (coreSelections().course() == null)
        {
            error("Please select a course.");
            return null;
        }
        if (crn == null || crn.length() == 0)
        {
            error("Please enter a CRN (unique identifier) for your "
                + "course offering.");
            return null;
        }
        CourseOffering newOffering = CourseOffering.create(localContext());
        newOffering.setCourseRelationship(coreSelections().course());
        newOffering.setSemesterRelationship(semester);
        newOffering.addToInstructorsRelationship(user());
        newOffering.setCrn(crn);
        setCourseOffering(newOffering);
        coreSelections().setCourseOfferingRelationship(newOffering);
        return super.next();
    }


    // ----------------------------------------------------------
    public JavascriptGenerator update()
    {
        return new JavascriptGenerator().refresh("courseblock");
    }


    // ----------------------------------------------------------
    public Integer season()
    {
        if (season == null)
        {
            guessNextSeason();
        }
        return season;
    }


    // ----------------------------------------------------------
    public NSArray<Integer> seasons()
    {
        return Semester.integersInNS;
    }


    // ----------------------------------------------------------
    public String seasonName()
    {
        return Semester.names.get(aSeason);
    }


    // ----------------------------------------------------------
    public void setSeason(Integer value)
    {
        season = value;
    }


    // ----------------------------------------------------------
    public int year()
    {
        if (year == 0)
        {
            guessNextSeason();
        }
        return year;
    }


    // ----------------------------------------------------------
    public void setYear(Object value)
    {
        if (value == null)
        {
            return;
        }
        else if (value instanceof Number)
        {
            year = ((Number)value).intValue();
        }
        else
        {
            try
            {
                year = Integer.parseInt(value.toString());
            }
            catch (NumberFormatException e)
            {
                error(e.getMessage());
            }
        }
    }


    // ----------------------------------------------------------
    public NSArray<Department> departments()
    {
        if (departments == null || departments.size() == 0 ||
            departments.get(0).institution() != institution)
        {
            departments = Department.objectsMatchingQualifier(localContext(),
                Department.institution.is(institution),
                Department.name.ascInsensitives());
        }
        return departments;
    }


    // ----------------------------------------------------------
    public boolean hasMultipleDepartments()
    {
        return departments().size() > 1;
    }


    // ----------------------------------------------------------
    public Department department()
    {
        if (department == null || department.institution() != institution)
        {
            if (departments().size() > 0)
            {
                department = departments().get(0);
            }
            else
            {
                department = null;
            }
        }
        return department;
    }


    // ----------------------------------------------------------
    public void setDepartment(Department value)
    {
        department = value;
    }


    // ----------------------------------------------------------
    public int newCourseNumber()
    {
        return newCourseNumber;
    }


    // ----------------------------------------------------------
    public void setNewCourseNumber(Object value)
    {
        if (value == null)
        {
            return;
        }
        else if (value instanceof Number)
        {
            newCourseNumber = ((Number)value).intValue();
        }
        else
        {
            try
            {
                newCourseNumber = Integer.parseInt(value.toString());
            }
            catch (NumberFormatException e)
            {
                error(e.getMessage());
            }
        }
    }


    // ----------------------------------------------------------
    public WOActionResults createSemester()
    {
        Semester newSemester = Semester.create(
            localContext(), season(), startDate, endDate, year());
        if (applyLocalChanges())
        {
            semester = newSemester;
            semesters = Semester.allObjectsOrderedByStartDate(localContext());
        }

        JavascriptGenerator page = new JavascriptGenerator();
        page.refresh("courseblock", "error-panel");
        return page;
    }


    // ----------------------------------------------------------
    public WOActionResults createCourse()
    {
        JavascriptGenerator page = new JavascriptGenerator();
        page.refresh("courseblock,error-panel");

        if (newCourseNumber == 0)
        {
            error("Please provide a course number.");
            return page;
        }
        if (newCourseName == null || newCourseName.equals(""))
        {
            error("Please provide a course name.");
            return page;
        }

        // Check for duplicate number
        NSArray<Course> old = Course.objectsMatchingQualifier(localContext(),
            Course.number.is(newCourseNumber).and(
                Course.department.is(department)));
        if (old.size() > 0)
        {
            error("Course " + department.abbreviation() + " " + newCourseNumber
                + " already exists.");
            return page;
        }

        Course newCourse =
            Course.create(localContext(), newCourseName, newCourseNumber);
        newCourse.setDepartmentRelationship(department);
        if (applyLocalChanges())
        {
            coreSelections().setCourseRelationship(newCourse);
            coreSelections().setCourseOfferingRelationship(null);
            allCourses = null;  // force a new fetch
        }

        // This will force the main section of the page to be reloaded,
        // which will in turn force filterCourseList() to be invoked,
        // which will reload the course list
        return page;
    }


    // ----------------------------------------------------------
    public TimeZone timeZone()
    {
        if (timeZone == null)
        {
            String tz = user().timeZoneName();
            if (tz != null)
            {
                timeZone = TimeZone.getTimeZone(tz);
            }
        }
        return timeZone;
    }


    // ----------------------------------------------------------
    private void guessNextSeason()
    {
        if (semesters == null || semesters.size() == 0)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new java.util.Date());
            if (timeZone() != null)
            {
                cal.setTimeZone(timeZone());
            }
            year = cal.get(Calendar.YEAR);
            season = Semester.integers[Semester.defaultSemesterFor(cal)];
        }
        else
        {
            Semester old = semesters.get(0);
            int nextSeason = old.season().intValue() + 1;
            year = old.year();
            if (nextSeason == Semester.integers.length)
            {
                nextSeason = Semester.integers[0];
                year++;
            }
            season = Semester.integers[nextSeason];
        }
        if (startDate == null)
        {
            startDate = Semester.defaultStartingDate(season, year, timeZone());
        }
        if (endDate == null)
        {
            endDate = Semester.defaultEndingDate(season, year, timeZone());
        }
    }


    //~ Instance/static variables .............................................

    private int year;
    private Integer season;
    private Department department;
    private NSArray<Department> departments;
    private int newCourseNumber = 101;
    private TimeZone timeZone;
    private NSArray<Course> allCourses;
    static Logger log = Logger.getLogger(NewCourseOfferingPage.class);
}
