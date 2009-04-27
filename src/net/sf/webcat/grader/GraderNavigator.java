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

package net.sf.webcat.grader;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import net.sf.webcat.core.CoreNavigator;
import net.sf.webcat.core.CoreNavigatorObjects;
import net.sf.webcat.core.Course;
import net.sf.webcat.core.CourseOffering;
import net.sf.webcat.core.EntityUtils;
import net.sf.webcat.core.INavigatorObject;
import net.sf.webcat.core.Semester;
import net.sf.webcat.core.WCComponent;
import net.sf.webcat.ui.util.ComponentIDGenerator;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
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
 * the user has TA privileges or higher. Defaults to false.</dd>
 * </dl>
 *
 * @author Tony Allevato
 * @version $Id$
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

    public boolean showUnpublishedAssignments = false;
    public boolean showClosedAssignments = false;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void awake()
    {
        log.debug("entering awake()");
        super.awake();
        log.debug("selected assignment = " + selectedAssignment);
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
        return new NSMutableArray<String>(new String[] {
                idFor.valueForKey("coursePane").toString(),
                idFor.valueForKey("assignmentPane").toString()
        });
    }


    // ----------------------------------------------------------
    /**
     * Updates the list of course offerings, followed by assignments.
     *
     * @return the result is ignored
     */
    public WOActionResults updateCourseOfferings()
    {
        super.updateCourseOfferings();
        return updateAssignments();
    }


    // ----------------------------------------------------------
    /**
     * Updates the list of available assignments.
     *
     * @return the result is ignored
     */
    public WOActionResults updateAssignments()
    {
    	log.debug("updateAssignments()");
        assignments = new NSMutableArray<INavigatorObject>();

        if (selectedCourseOffering == null)
        {
            return null;
        }

        NSArray<CourseOffering> offerings = (NSArray<CourseOffering>)
            selectedCourseOffering.representedObjects();

        EOQualifier unpublishedQual = null;

        if (!showUnpublishedAssignments)
        {
            unpublishedQual = ERXQ.isTrue("publish");
        }

        EOFetchSpecification fspec = new EOFetchSpecification(
                AssignmentOffering.ENTITY_NAME,
                ERXQ.and(
                        ERXQ.in("courseOffering", offerings),
                        unpublishedQual),
                null);

        NSArray<AssignmentOffering> assnOffs =
            localContext().objectsWithFetchSpecification(fspec);
        if (log.isDebugEnabled())
        {
        	log.debug("scanning assignment offerings: " + assnOffs);
        }

        // Filter out closed assignments (lateDeadline is not a database
        // property so we have to do it here, in memory.

        NSTimestamp now = new NSTimestamp();
        if (!showClosedAssignments)
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
            if (now.after(ao.lateDeadline()))
            {
                Assignment a = ao.assignment();
                closedAssigns.put(a, a);
            }
        }

        NSArray<Assignment> assigns =
            ERXArrayUtilities.arrayWithoutDuplicates(
                (NSArray<Assignment>) assnOffs.valueForKey("assignment"));

        assigns = EOSortOrdering.sortedArrayUsingKeyOrderArray(assigns,
            EntityUtils.sortOrderingsForEntityNamed(Assignment.ENTITY_NAME));

        for (Assignment assignment : assigns)
        {
            assignments.addObject(
                new GraderNavigatorObjects.SingleAssignment(
                    assignment,
                    unpublishedAssigns.containsKey(assignment),
                    closedAssigns.containsKey(assignment)));
        }

        if (assignments.count() == 0 && user().hasTAPrivileges())
        {
        	// If none were found ...
        	if (!showUnpublishedAssignments)
        	{
        		// First, try enabling unpublished assignments
        		showUnpublishedAssignments = true;
        		return updateAssignments();
        	}
        	else if (!showClosedAssignments)
        	{
        		// Then try enabling closed assignments
        		showClosedAssignments = true;
        		return updateAssignments();
        	}
        }

        if (assignments.count() > 0 && selectedAssignment == null)
        {
            selectedAssignment = assignments.objectAtIndex(0);
        }

        if (log.isDebugEnabled())
        {
            log.debug("assignments = " + assignments);
            log.debug("selected assignment = " + selectedAssignment);
        }
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Invoked when the OK button in the dialog is pressed.
     *
     * @return null to reload the current page
     */
    public WOActionResults okPressed()
    {
    	log.debug("okPressed()");

    	// TODO store the user's current semester, course, and assignment
        // selection in the session or wherever the nav-state is being
        // persisted

        return super.okPressed();
    }


    //~ Static/instance variables .............................................

    static Logger log = Logger.getLogger(GraderNavigator.class);
}
