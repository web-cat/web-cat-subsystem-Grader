/*==========================================================================*\
 |  $Id: GraderNavigator.java,v 1.7 2012/05/09 16:31:03 stedwar2 Exp $
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

package org.webcat.grader;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.webcat.core.CoreNavigator;
import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.EntityUtils;
import org.webcat.core.INavigatorObject;
import org.webcat.ui.generators.JavascriptGenerator;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;
import er.extensions.eof.ERXQ;
import er.extensions.foundation.ERXArrayUtilities;

//--------------------------------------------------------------------------
/**
 * The popup assignment selector that serves as the basis for the Web-CAT
 * grader subsystem navigation scheme.
 *
 * <h2>Bindings</h2>
 * <dl>
 * <dt>allowsAllSemesters</dt>
 * <dd>A boolean value that adds an option to the semester drop-down that
 * allows the user to select "All" semesters. If false, the user can only
 * select a single semester. Defaults to true.</dd>
 * <dt>allowsAllOfferingsForCourse</dt>
 * <dd>A boolean value that adds an option for each course in the course
 * drop-down that allows the user to select "All" offerings for that course. If
 * false, the user may only select a single offering for a single course.
 * Defaults to true.</dd>
 * <dt>includeAdminAccess</dt>
 * <dd>A boolean value indicating whether the course drop-down should include
 * courses that the user is not teaching or enrolled in but does have admin
 * access for. If the user is an administrator, he can change this in the user
 * interface. Defaults to false.</dd>
 * <dt>includeWhatImTeaching</dt>
 * <dd>A boolean value indicating whether the course drop-down should include
 * courses that the user is teaching. If the user has TA privileges or higher,
 * he can change this in the user interface. Defaults to true.</dd>
 * <dt>showClosedAssignments</dt>
 * <dd>A boolean value indicating whether the assignment drop-down should
 * include assignments that have been closed for submissions. This option is
 * only available if the user has TA privileges or higher. Defaults to
 * false.</dd>
 * <dt>showUnpublishedAssignments</dt>
 * <dd>A boolean value indicating whether the assignment drop-down should
 * include assignments that are unpublished. This option is only available if
 * the user has TA privileges or higher. Defaults to true.</dd>
 * <dt>hideClosedAssignmentsFromStudents</dt>
 * <dd>A boolean value indicating whether the student should be allowed to
 * choose a closed assignment and be given the option to toggle the visibility
 * of closed assignments in the navigator. Defaults to false.</dd>
 * </dl>
 *
 * @author  Tony Allevato
 * @author  latest changes by: $Author: stedwar2 $
 * @version $Revision: 1.7 $ $Date: 2012/05/09 16:31:03 $
 */
public class GraderNavigator
    extends CoreNavigator
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Create a new object.
     *
     * @param context
     */
    public GraderNavigator(WOContext context)
    {
        super(context);
    }


    //~ KVC attributes (must be public) .......................................

    public NSMutableArray<INavigatorObject> assignments;
    public INavigatorObject assignmentInRepetition;
    public INavigatorObject selectedAssignment;
    public boolean hideClosedAssignmentsFromStudents = false;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void awake()
    {
        log.debug("entering awake()");
        super.awake();
        if (!(selectionsParent instanceof GraderAssignmentComponent))
        {
            throw new IllegalStateException("GraderNavigator can only be "
                + "embedded inside a GraderAssignmentComponent page");
        }
        graderParent = (GraderAssignmentComponent)selectionsParent;

        log.debug("selected assignment = " + selectedAssignment);
        log.debug("graderParent = " + graderParent.getClass().getName());
        log.debug("leaving awake()");
    }


    // ----------------------------------------------------------
    /**
     * Gets an array containing the identifiers of the course and assignment
     * panes, for refresh purposes.
     *
     * @return an array containing the course and assignment pane identifiers
     */
    public NSArray<String> idsForCourseAndAssignmentPanes()
    {
        return new NSArray<String>(new String[] {
                idFor.get("coursePane"), idFor.get("assignmentPane")
        });
    }


    // ----------------------------------------------------------
    /**
     * Updates the list of course offerings, followed by assignments.
     *
     * @return the result is ignored
     */
    public JavascriptGenerator updateCourseOfferings()
    {
        log.debug("updateCourseOfferings()");
        super.updateCourseOfferings();
        return updateAssignments().refresh(idFor.get("coursePane"));
    }


    // ----------------------------------------------------------
    public JavascriptGenerator updateCourseOfferingsAndClearSelection()
    {
        log.debug("updateCourseOfferingsAndClearSelection()");
        super.updateCourseOfferingsAndClearSelection();
        selectedAssignment = null;
        JavascriptGenerator result = updateAssignments();
        if (selectedAssignment == null)
        {
            result = result.refresh(idFor.get("coursePane"));
        }
        else
        {
            saveAssignmentSelection();
            result = new JavascriptGenerator().redirectTo("");
        }
        return result;
    }


    // ----------------------------------------------------------
    public boolean isAssignmentFilterPlaceholder()
    {
        return assignmentInRepetition ==
            GraderNavigatorObjects.FILTER_PLACEHOLDER;
    }


    // ----------------------------------------------------------
    public boolean isAssignmentNoCoursePlaceholder()
    {
        return assignmentInRepetition ==
            GraderNavigatorObjects.NO_COURSE_PLACEHOLDER;
    }


    // ----------------------------------------------------------
    private void gatherAssignments()
    {
        log.debug("gatherAssignments()");
        assignments = new NSMutableArray<INavigatorObject>();

        if (selectedCourseOffering == null)
        {
            assignments.addObject(
                    GraderNavigatorObjects.NO_COURSE_PLACEHOLDER);
            return;
        }

        if (userIsStaffForSelectedCourse()
                || !hideClosedAssignmentsFromStudents)
        {
            assignments.addObject(
                    GraderNavigatorObjects.FILTER_PLACEHOLDER);
        }

        @SuppressWarnings("unchecked")
        NSArray<CourseOffering> offerings = (NSArray<CourseOffering>)
            selectedCourseOffering.representedObjects();

        if (offerings == null)
        {
            return;
        }

        EOQualifier unpublishedQual = null;

        if (!showUnpublishedAssignments())
        {
            unpublishedQual = ERXQ.isTrue("publish");
        }

        EOFetchSpecification fspec = new EOFetchSpecification(
                AssignmentOffering.ENTITY_NAME,
                ERXQ.and(
                        ERXQ.in("courseOffering", offerings),
                        unpublishedQual),
                null);

        NSArray<AssignmentOffering> assnOffs = AssignmentOffering
            .objectsWithFetchSpecification(localContext(), fspec);
        if (log.isDebugEnabled())
        {
            log.debug("scanning assignment offerings: " + assnOffs);
        }

        // Filter out closed assignments (lateDeadline is not a database
        // property so we have to do it here, in memory.

        NSTimestamp now = new NSTimestamp();
        boolean hideClosed =
            (hideClosedAssignmentsFromStudents &&
                    !userIsStaffForSelectedCourse()) ||
                    !showClosedAssignments();

        if (hideClosed)
        {
            assnOffs = ERXQ.filtered(assnOffs,
                    ERXQ.greaterThan("lateDeadline", now));
        }
        Map<Assignment, Assignment> closedAssigns =
            new HashMap<Assignment, Assignment>();
        Map<Assignment, Assignment> unpublishedAssigns =
            new HashMap<Assignment, Assignment>();
        for (AssignmentOffering ao : assnOffs)
        {
            if (!ao.publish())
            {
                Assignment a = ao.assignment();
                unpublishedAssigns.put(a, a);
            }
            if (ao.lateDeadline() != null && now.after(ao.lateDeadline()))
            {
                Assignment a = ao.assignment();
                closedAssigns.put(a, a);
            }
        }

        @SuppressWarnings("unchecked")
        NSArray<Assignment> assigns =
            ERXArrayUtilities.arrayWithoutDuplicates(
                (NSArray<Assignment>)assnOffs.valueForKey("assignment"));

        assigns = EOSortOrdering.sortedArrayUsingKeyOrderArray(assigns,
            EntityUtils.sortOrderingsForEntityNamed(Assignment.ENTITY_NAME));

        Assignment targetAssignment = graderParent.prefs().assignment();
        if (targetAssignment == null)
        {
            AssignmentOffering ao = graderParent.prefs().assignmentOffering();
            if (ao != null)
            {
                targetAssignment = ao.assignment();
            }
        }
        String targetName = null;
        if (targetAssignment != null)
        {
            targetName = targetAssignment.name();
            if (targetName == null)
            {
                targetName = "";
            }
            targetName = targetName.toLowerCase();
        }

        for (Assignment assignment : assigns)
        {
            INavigatorObject thisAssignment =
                new GraderNavigatorObjects.SingleAssignment(
                    assignment,
                    unpublishedAssigns.containsKey(assignment),
                    closedAssigns.containsKey(assignment));
            if (assignment == targetAssignment)
            {
                selectedAssignment = thisAssignment;
            }
            else if (selectedAssignment == null && targetName != null)
            {
                // default to any assignment with the same name, ignoring case
                String thisName = assignment.name();
                if (thisName == null)
                {
                    thisName = "";
                }
                thisName = thisName.toLowerCase();
                if (targetName.equals(thisName))
                {
                    selectedAssignment = thisAssignment;
                }
            }
            assignments.addObject(thisAssignment);
        }

        if (assignments.count() == 0 && userIsStaffForSelectedCourse())
        {
            // If none were found ...
            if (!showUnpublishedAssignments())
            {
                // First, try enabling unpublished assignments
                setShowUnpublishedAssignments(true);
                gatherAssignments();
            }
            else if (!showClosedAssignments())
            {
                // Then try enabling closed assignments
                setShowClosedAssignments(true);
                gatherAssignments();
            }
        }

        /*if (assignments.count() > firstAssignmentIndex
                && selectedAssignment == null)
        {
            selectedAssignment =
                assignments.objectAtIndex(firstAssignmentIndex);
        }*/

        if (log.isDebugEnabled())
        {
            log.debug("assignments = " + assignments);
            log.debug("selected assignment = " + selectedAssignment);
        }
    }


    // ----------------------------------------------------------
    /**
     * Updates the list of available assignments.
     *
     * @return the result is ignored
     */
    public JavascriptGenerator updateAssignments()
    {
        log.debug("updateAssignments()");
        gatherAssignments();
        return new JavascriptGenerator().refresh(idFor.get("assignmentPane"))
            .append("window.navUnderlay.hide(); window.navUnderlay.destroyRecursive()");
    }


    // ----------------------------------------------------------
    public JavascriptGenerator updateAssignmentMenu()
    {
        log.debug("updateAssignmentsMenu()");
        gatherAssignments();
        return new JavascriptGenerator().refresh(idFor.get("assignmentMenu"))
            .append("window.navUnderlay.hide(); window.navUnderlay.destroyRecursive()");
    }


    // ----------------------------------------------------------
    public JavascriptGenerator updateAssignmentsAndClearSelection()
    {
        log.debug("updateAssignmentsAndClearSelection()");
        saveCourseSelection();
        selectedAssignment = null;
        JavascriptGenerator result = updateAssignments();
        if (selectedAssignment != null)
        {
            saveAssignmentSelection();
            result = new JavascriptGenerator().redirectTo("");
        }
        return result;
    }


    // ----------------------------------------------------------
    protected void saveAssignmentSelection()
    {
        if (selectedAssignment != null)
        {
            NSArray<?> assignArray = selectedAssignment.representedObjects();
            if (assignArray.count() > 0)
            {
                Assignment targetAssignment =
                    (Assignment)assignArray.objectAtIndex(0);
                if (log.isDebugEnabled())
                {
                    log.debug("saving assignment selection = "
                        + targetAssignment);
                }
                graderParent.prefs().setAssignmentRelationship(
                    targetAssignment);
                graderParent.prefs().setAssignmentOfferingRelationship(null);
                CourseOffering co =
                    selectionsParent.coreSelections().courseOffering();
                Course course = selectionsParent.coreSelections().course();
                for (AssignmentOffering ao : targetAssignment.offerings())
                {
                    if ((co != null && co == ao.courseOffering())
                        || (co == null
                            && course == ao.courseOffering().course()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("saving assignment offering selection = "
                                + ao);
                        }
                        graderParent.prefs().setAssignmentOfferingRelationship(
                            ao);
                        break;
                    }
                }
            }
            else
            {
                log.debug("saving assignment selection = null");
                graderParent.prefs().setAssignmentRelationship(null);
            }
        }
        else
        {
            log.debug("saving assignment selection = null");
            graderParent.prefs().setAssignmentRelationship(null);
        }
    }


    // ----------------------------------------------------------
    @Override
    protected void saveSelections()
    {
        super.saveSelections();
        saveAssignmentSelection();
    }


    // ----------------------------------------------------------
    public void setShowUnpublishedAssignments(
        boolean showUnpublishedAssignments)
    {
        graderParent.prefs().setShowUnpublishedAssignments(
            showUnpublishedAssignments);
    }


    // ----------------------------------------------------------
    public boolean showUnpublishedAssignments()
    {
        return graderParent.prefs().showUnpublishedAssignments();
    }


    // ----------------------------------------------------------
    public JavascriptGenerator toggleUnpublishedAssignments()
    {
        setShowUnpublishedAssignments(!showUnpublishedAssignments());
        return updateAssignmentMenu();
    }


    // ----------------------------------------------------------
    public void setShowClosedAssignments(boolean showClosedAssignments)
    {
        graderParent.prefs().setShowClosedAssignments(
            showClosedAssignments);
    }


    // ----------------------------------------------------------
    public boolean showClosedAssignments()
    {
        return graderParent.prefs().showClosedAssignments();
    }


    // ----------------------------------------------------------
    public JavascriptGenerator toggleClosedAssignments()
    {
        setShowClosedAssignments(!showClosedAssignments());
        return updateAssignmentMenu();
    }


    // ----------------------------------------------------------
    public boolean userIsStaffForSelectedCourse()
    {
        if (user().hasAdminPrivileges())
        {
            return true;
        }

        if (selectedCourseOffering == null)
        {
            return false;
        }

        @SuppressWarnings("unchecked")
        NSArray<CourseOffering> offerings = (NSArray<CourseOffering>)
            selectedCourseOffering.representedObjects();

        for (CourseOffering co : offerings)
        {
            if (!co.isGrader(user()) && !co.isInstructor(user()))
            {
                return false;
            }
        }

        return true;
    }


    //~ Static/instance variables .............................................

    protected GraderAssignmentComponent graderParent;
    static Logger log = Logger.getLogger(GraderNavigator.class);
}
