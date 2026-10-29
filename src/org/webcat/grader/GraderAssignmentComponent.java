/*==========================================================================*\
 |  Copyright (C) 2006-2018 Virginia Tech
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

import org.webcat.core.*;
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

//-------------------------------------------------------------------------
/**
 *  A {@link GraderCourseComponent} that adds support for initializing
 *  assignment selections from login parameters.
 *
 *  @author  Stephen Edwards
 */
public class GraderAssignmentComponent
    extends GraderCourseComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderAssignmentComponent object.
     *
     * @param context The context to use
     */
    public GraderAssignmentComponent(WOContext context)
    {
        super(context);
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public void lateAwake()
    {
        super.lateAwake();
        if (!assignmentSelectionsSeeded)
        {
            seedAssignmentSelections();
        }
    }


    // ----------------------------------------------------------
    /**
     * Access the currently selected assignment for this page.
     * @return the selected assignment
     */
    public Assignment selectedAssignment()
    {
        if (selectedAssignment == null && selectedAssignmentOffering == null && !assignmentSelectionsSeeded)
        {
            seedAssignmentSelections();
        }
        return selectedAssignment;
    }


    // ----------------------------------------------------------
    /**
     * Sets the selected assignment for this page.
     * @param value the assignment
     */
    public void setSelectedAssignment(Assignment value)
    {
        this.selectedAssignment = value;
    }


    // ----------------------------------------------------------
    /**
     * Access the currently selected assignment offering for this page.
     * @return the selected assignment offering
     */
    public AssignmentOffering selectedAssignmentOffering()
    {
        if (selectedAssignmentOffering == null && selectedAssignment == null && !assignmentSelectionsSeeded)
        {
            seedAssignmentSelections();
        }
        return selectedAssignmentOffering;
    }


    // ----------------------------------------------------------
    /**
     * Sets the selected assignment offering for this page.
     * @param value the assignment offering
     */
    public void setSelectedAssignmentOffering(AssignmentOffering value)
    {
        this.selectedAssignmentOffering = value;
        if (value != null)
        {
            if (this.selectedAssignment == null)
            {
                this.selectedAssignment = value.assignment();
            }
            if (value.courseOffering() != null)
            {
                setSelectedCourseOffering(value.courseOffering());
                setSelectedCourse(value.courseOffering().course());
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Seeds the page-level assignment selections from persistent preferences
     * if the page does not already have an active selection.
     */
    protected void seedAssignmentSelections()
    {
        assignmentSelectionsSeeded = true;
        if (selectedAssignmentOffering == null && selectedAssignment == null)
        {
            AssignmentOffering ao = prefs().assignmentOffering();
            Assignment a = prefs().assignment();
            if (ao != null)
            {
                this.selectedAssignmentOffering = ao;
                if (this.selectedAssignment == null)
                {
                    this.selectedAssignment = ao.assignment();
                }
            }
            else if (a != null)
            {
                this.selectedAssignment = a;
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * This method determines whether any embedded navigator will
     * automatically pop up to force a selection and page reload.
     * @return True if the navigator should start out by opening automatically.
     */
    public boolean forceNavigatorSelection()
    {
        boolean result = super.forceNavigatorSelection();
        if (selectedAssignment() == null)
        {
            result = true;
        }
        else if (!result && requiresAssignmentOffering())
        {
            AssignmentOffering ao = selectedAssignmentOffering();
            Assignment assignment = selectedAssignment();
            if (ao == null && assignment != null)
            {
                CourseOffering co = bestMatchingCourseOffering();
                for (AssignmentOffering offering : assignment.offerings())
                {
                    if (offering.courseOffering() == co)
                    {
                        setSelectedAssignmentOffering(offering);
                        prefs().setAssignmentOfferingRelationship(offering);
                        if (selectedCourseOffering() != co)
                        {
                            setSelectedCourseOffering(co);
                            coreSelections().setCourseOfferingRelationship(co);
                            if (selectedCourse() != co.course())
                            {
                                setSelectedCourse(null);
                                coreSelections().setCourseRelationship(null);
                            }
                        }
                        break;
                    }
                }
            }
            result = (selectedAssignmentOffering() == null);
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * This method determines whether the current page requires the
     * user to have a selected AssignmentOffering.
     * The default implementation returns true, but is designed
     * to be overridden in subclasses.
     * @return True if the page requires a selected assignment offering.
     */
    public boolean requiresAssignmentOffering()
    {
        return true;
    }


    // ----------------------------------------------------------
    /**
     * Extracts assignment identification from the given startup
     * parameters.
     * @param params A dictionary of form values to decode
     * @return True if successful, false if the parameter is missing
     */
    @Override
    public boolean startWith(NSDictionary<String, NSArray<Object>> params)
    {
        boolean result = false;
        String aoid =
            stringValueForKey(params, AssignmentOffering.ID_FORM_KEY);
        if (aoid != null)
        {
            result = startWith(AssignmentOffering
                .forId(localContext(), aoid));
        }
        else
        {
            String aid = stringValueForKey(params, Assignment.ID_FORM_KEY);
            result = startWith(Assignment
                .forId(localContext(), aid));
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Sets the relevant assignment offering properties for this
     * session.
     * @param assignment the assignment to use for generating settings
     * @return True if successful, false if the offering is not valid
     */
    protected boolean startWith(Assignment assignment)
    {
        return assignment != null
            && startWith(assignment.offeringForUser(user()));
    }


    // ----------------------------------------------------------
    /**
     * Sets the relevant assignment offering properties for this
     * session.
     * @param offering the assignment offering to use for generating settings
     * @return True if successful, false if the offering is not valid
     */
    protected boolean startWith(AssignmentOffering offering)
    {
        boolean result = false;
        if (offering != null && startWith(offering.courseOffering()))
        {
            setSelectedAssignmentOffering(offering);
            setSelectedAssignment(offering.assignment());
            prefs().setAssignmentOfferingRelationship(offering);
            result = true;
        }
        return result;
    }


    // ----------------------------------------------------------
    public boolean needsLTIClickthrough()
    {
        AssignmentOffering ao = selectedAssignmentOffering();
        return ao != null
            && ao.courseOffering() != null
            && ao.courseOffering().isStudent(user())
            && ao.needsLTIClickthrough(user());
    }



    // ----------------------------------------------------------
    public void startInIFrame(boolean value)
    {
        isInIFrame = value;
    }


    // ----------------------------------------------------------
    public void setIsInIFrame(boolean value)
    {
        // ignore, to disable synchronization with child
        // components!
    }


    // ----------------------------------------------------------
    public boolean isInIFrame()
    {
        return isInIFrame;
    }


    // ----------------------------------------------------------
    public WOComponent refreshNoIFrame()
    {
        isInIFrame = false;
        return this;
    }


    //~ Instance/static variables .............................................

    private Assignment selectedAssignment;
    private AssignmentOffering selectedAssignmentOffering;
    private boolean assignmentSelectionsSeeded = false;

    private boolean isInIFrame = false;
}
