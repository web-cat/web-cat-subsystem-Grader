/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2008 Virginia Tech
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

package net.sf.webcat.grader.objectquery;

import net.sf.webcat.core.Course;
import net.sf.webcat.core.CourseOffering;
import net.sf.webcat.core.Department;
import net.sf.webcat.core.Semester;
import net.sf.webcat.core.WCComponent;
import net.sf.webcat.grader.Assignment;
import net.sf.webcat.ui.AbstractTreeModel;
import net.sf.webcat.ui.util.ComponentIDGenerator;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOGenericRecord;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import er.extensions.eof.ERXQ;
import er.extensions.eof.ERXS;
import er.extensions.eof.ERXSortOrdering.ERXSortOrderings;

//-------------------------------------------------------------------------
/**
 * A simplified query assistant that allows the user to select all the
 * submissions from one or more assignment offerings that are common across
 * a specified set of course offerings.
 *
 * @author aallowat
 * @version $Id$
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


    //~ Public Nested Classes .................................................

    // ----------------------------------------------------------
    public class CourseTreeModel extends AbstractTreeModel
    {
        //~ Public Methods ....................................................

        // ----------------------------------------------------------
        @Override
        protected NSArray<?> childrenOfItem(Object parentItem)
        {
            if (parentItem == null)
            {
                return allSemesters();
            }
            else if (parentItem instanceof Semester)
            {
                return courseOfferingsForSemester((Semester) parentItem);
            }
            else
            {
                return null;
            }
        }


        // ----------------------------------------------------------
        @Override
        protected boolean itemHasChildren(Object parentItem)
        {
            return !(parentItem instanceof CourseOffering);
        }


        // ----------------------------------------------------------
        @Override
        protected String idForItem(Object item)
        {
            if (item instanceof EOGenericRecord)
            {
                EOGenericRecord record = (EOGenericRecord) item;
                return "id_" + record.entityName() + "_"
                    + record.valueForKey("id");
            }
            else
            {
                return null;
            }
        }


        // ----------------------------------------------------------
        @Override
        protected String labelForItem(Object item)
        {
            if (item instanceof Semester)
            {
                Semester s = (Semester) item;
                return "<b>" + s.seasonName() + " " + s.year() + "</b>";
            }
            else if (item instanceof CourseOffering)
            {
                CourseOffering co = (CourseOffering) item;
                return "<b>" + co.course().deptNumberAndName() + "</b> ("
                    + co.crn() + ")";
            }
            else
            {
                return "";
            }
        }


        // ----------------------------------------------------------
        private NSArray<Semester> allSemesters()
        {
            NSMutableArray<EOSortOrdering> orderings =
                new NSMutableArray<EOSortOrdering>();
            orderings.addObject(ERXS.asc(Semester.YEAR_KEY));
            orderings.addObject(ERXS.asc(Semester.SEASON_KEY));

            EOFetchSpecification fspec = new EOFetchSpecification(
                    Semester.ENTITY_NAME, null, orderings);
            return Semester
                .objectsWithFetchSpecification(localContext(), fspec);
        }


        // ----------------------------------------------------------
        private NSArray<CourseOffering> courseOfferingsForSemester(
                Semester semester)
        {
            NSArray<CourseOffering> offerings =
                CourseOffering.offeringsForSemester(localContext(), semester);

            return ERXS.sorted(offerings,
                    ERXS.ascInsensitive(
                            CourseOffering.COURSE_KEY + "." +
                            Course.DEPARTMENT_KEY + "." +
                            Department.ABBREVIATION_KEY),
                    ERXS.asc(
                            CourseOffering.COURSE_NUMBER_KEY),
                    ERXS.asc(CourseOffering.CRN_KEY));
        }
    }


    // ----------------------------------------------------------
    public class AssignmentTreeModel extends AbstractTreeModel
    {
        //~ Public Methods ....................................................

        // ----------------------------------------------------------
        @Override
        protected NSArray<?> childrenOfItem(Object parentItem)
        {
            if (parentItem == null)
            {
                return model.selectedCourses();
            }
            else if (parentItem instanceof Course)
            {
                return assignmentsForCourse((Course) parentItem);
            }
            else
            {
                return null;
            }
        }


        // ----------------------------------------------------------
        @Override
        protected boolean itemHasChildren(Object parentItem)
        {
            return (parentItem instanceof Course);
        }


        // ----------------------------------------------------------
        @Override
        protected String idForItem(Object item)
        {
            if (item instanceof EOGenericRecord)
            {
                EOGenericRecord record = (EOGenericRecord) item;
                return "id_" + record.entityName() + "_"
                    + record.valueForKey("id");
            }
            else
            {
                return null;
            }
        }


        // ----------------------------------------------------------
        @Override
        protected String labelForItem(Object item)
        {
            if (item instanceof Course)
            {
                Course c = (Course) item;
                return "<b>" + c.deptNumberAndName() + "</b>";
            }
            else if (item instanceof Assignment)
            {
                Assignment a = (Assignment) item;
                return a.name();
            }
            else
            {
                return "";
            }
        }


        // ----------------------------------------------------------
        private NSArray<Assignment> assignmentsForCourse(Course course)
        {
            ERXSortOrderings sortOrderings = Assignment.name.ascInsensitives();

            EOFetchSpecification fetchSpec =
                new EOFetchSpecification(Assignment.ENTITY_NAME,
                        ERXQ.containsObject(
                                Assignment.COURSES_KEY, course),
                        sortOrderings);
            fetchSpec.setUsesDistinct(true);

            return Assignment.objectsWithFetchSpecification(
                localContext(), fetchSpec);
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
    public void pruneAssignmentsFromUnselectedCourses()
    {
        model.pruneAssignmentsFromUnselectedCourses();
    }
}
