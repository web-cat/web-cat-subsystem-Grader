/*==========================================================================*\
 |  $Id: CourseAndAssignmentSubmissionsAssistant.java,v 1.7 2014/06/16 17:24:13 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2012 Virginia Tech
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

package org.webcat.grader.objectquery;

import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.Department;
import org.webcat.core.EOBase;
import org.webcat.core.Semester;
import org.webcat.core.WCComponent;
import org.webcat.grader.Assignment;
import org.webcat.ui.WCTreeModel;
import org.webcat.ui.generators.JavascriptGenerator;
import org.webcat.ui.util.ComponentIDGenerator;
import org.webcat.woextensions.WCFetchSpecification;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import er.extensions.eof.ERXQ;
import er.extensions.eof.ERXS;

//-------------------------------------------------------------------------
/**
 * A simplified query assistant that allows the user to select all the
 * submissions from one or more assignment offerings that are common across
 * a specified set of course offerings.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.7 $, $Date: 2014/06/16 17:24:13 $
 */
public class CourseAndAssignmentSubmissionsAssistant
    extends WCComponent
{
    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Create a new object.
     * @param context the page's context
     */
    public CourseAndAssignmentSubmissionsAssistant(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public String                              objectType;
    public CourseAndAssignmentSubmissionsModel model;
    public Assignment                          assignment;
    public int                                 index;
    public CourseTreeModel                     courseModel;
    public AssignmentTreeModel                 assignmentModel;
    public ComponentIDGenerator                idFor;
    public Object                              courseTreeItem;
    public Object                              assignmentTreeItem;


    //~ Public Nested Classes .................................................

    // ----------------------------------------------------------
    public class CourseTreeModel extends WCTreeModel<Object>
    {
        //~ Public Methods ....................................................

        // ----------------------------------------------------------
        @SuppressWarnings("unchecked")
        public NSArray childrenOfObject(Object item)
        {
            NSArray children = null;

            if (item == null)
            {
                children = allSemesters();
            }
            else if (item instanceof Semester)
            {
                children = courseOfferingsForSemester((Semester) item);
            }

            return children;
        }


        // ----------------------------------------------------------
        private NSArray<Semester> allSemesters()
        {
            return Semester.objectsMatchingQualifier(localContext(), null,
                    Semester.year.asc().then(Semester.season.asc()));
        }


        // ----------------------------------------------------------
        private NSArray<CourseOffering> courseOfferingsForSemester(
                Semester semester)
        {
            NSArray<CourseOffering> offerings =
                EOBase.accessibleBy(user()).filtered(
                CourseOffering.offeringsForSemester(localContext(), semester));

            return ERXS.sorted(offerings,
                    ERXS.ascInsensitive(
                            CourseOffering.COURSE_KEY + "." +
                            Course.DEPARTMENT_KEY + "." +
                            Department.ABBREVIATION_KEY),
                    CourseOffering.courseNumber.asc(),
                    CourseOffering.crn.asc());
        }
    }


    // ----------------------------------------------------------
    public class AssignmentTreeModel extends WCTreeModel<Object>
    {
        //~ Public Methods ....................................................

        // ----------------------------------------------------------
        @SuppressWarnings("unchecked")
        public NSArray childrenOfObject(Object item)
        {
            NSArray children = null;

            if (item == null)
            {
                children = model.selectedCourses();
            }
            else if (item instanceof Course)
            {
                children = assignmentsForCourseOfferings(
                    (Course)item,
                    CourseOffering.course.is((Course)item)
                    .filtered(model.selectedCourseOfferings()));
            }

            return children;
        }


        // ----------------------------------------------------------
        private NSArray<Assignment> assignmentsForCourseOfferings(
            Course course, NSArray<CourseOffering> courses)
        {
            if (courses.size() == 0)
            {
                @SuppressWarnings("unchecked")
                NSArray<Assignment> result = NSArray.EmptyArray;
                return result;
            }

            WCFetchSpecification<Assignment> fetchSpec =
                new WCFetchSpecification<Assignment>(
                    Assignment.ENTITY_NAME,
                    ERXQ.containsObject(Assignment.COURSES_KEY, course),
                    Assignment.name.ascInsensitives());
            fetchSpec.setUsesDistinct(true);

            EOQualifier qualifier = ERXQ.containsObject(
                Assignment.COURSE_OFFERINGS_KEY, courses.get(0));
            for (int i = 1; i < courses.size(); i++)
            {
                qualifier = ERXQ.and(qualifier, ERXQ.containsObject(
                    Assignment.COURSE_OFFERINGS_KEY, courses.get(i)));
            }
            qualifier = ERXQ.and(qualifier, EOBase.accessibleBy(user()));

            return EOQualifier.filteredArrayWithQualifier(
                Assignment.objectsWithFetchSpecification(
                    localContext(), fetchSpec),
                qualifier);
        }


        // ----------------------------------------------------------
        protected void selectionDidChange()
        {
            model.setSelectedAssignmentModelItems(
                    assignmentModel.selectedObjects().allObjects());
        }
    }


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    public void appendToResponse(WOResponse response, WOContext context)
    {
        idFor = new ComponentIDGenerator(this);
        courseModel = new CourseTreeModel();
        assignmentModel = new AssignmentTreeModel();

        super.appendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public String titleOfCourseTreeItem()
    {
        if (courseTreeItem instanceof Semester)
        {
            Semester semester = (Semester) courseTreeItem;
            return semester.name();
        }
        else if (courseTreeItem instanceof CourseOffering)
        {
            CourseOffering co = (CourseOffering) courseTreeItem;
            return co.deptNumberAndName();
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    public String titleOfAssignmentTreeItem()
    {
        if (assignmentTreeItem instanceof Course)
        {
            Course course = (Course) assignmentTreeItem;
            return course.deptNumberAndName();
        }
        else if (assignmentTreeItem instanceof Assignment)
        {
            Assignment thisAssignment = (Assignment) assignmentTreeItem;
            return thisAssignment.titleString();
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    public String onCourseTreeSelectionChangedScript()
    {
        return idFor.get("courseTreeSelectionChanged") + "(this);";
    }


    // ----------------------------------------------------------
    public WOActionResults courseTreeSelectionChanged()
    {
        assignmentModel.rearrangeObjects();
        model.setSelectedCourseModelItems(
                courseModel.selectedObjects().allObjects());
        return new JavascriptGenerator().refresh(idFor.get("assignmentPane"));
    }


    // ----------------------------------------------------------
    public void pruneAssignmentsFromUnselectedCourses()
    {
        model.pruneAssignmentsFromUnselectedCourses();
    }
}
