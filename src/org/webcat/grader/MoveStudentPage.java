/*==========================================================================*\
 |  Copyright (C) 2019-2021 Virginia Tech
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

import org.webcat.core.CoreSelections;
import org.webcat.core.CourseOffering;
import org.webcat.core.UsagePeriod;
import org.webcat.core.User;
import org.webcat.core.WCComponent;
import org.webcat.core.lti.LMSIdentity;
import org.webcat.grader.lti.LISResultId;
import org.webcat.woextensions.WCFetchSpecification;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableDictionary;

//-------------------------------------------------------------------------
/**
 * Admin page to allow merging two user accounts--for expert use only!
 *
 * @author  Stephen Edwards
 */
public class MoveStudentPage
    extends WCComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new page object.
     *
     * @param context The context to use
     */
    public MoveStudentPage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public int student_id;
    public String from_co_id;
    public String to_co_id;


    //~ Methods ...............................................................

    public WOComponent move()
    {
        clearAllMessages();
        StringBuilder message = new StringBuilder();
        EOEditingContext ec = localContext();

        if (student_id == 0 || from_co_id == null || from_co_id.isEmpty()
            || to_co_id == null || to_co_id.isEmpty())
        {
            error("Missing parameter.");
            return null;
        }

        User student = null;
        try
        {
            student = User.forId(ec, student_id);
        }
        catch (Exception e)
        {
            error(e);
            return null;
        }

        CourseOffering fromCO = null;
        try
        {
            fromCO = CourseOffering.uniqueObjectMatchingQualifier(ec,
                CourseOffering.crn.is(from_co_id));
        }
        catch (Exception e)
        {
            error(e);
            return null;
        }

        CourseOffering toCO = null;
        try
        {
            toCO = CourseOffering.uniqueObjectMatchingQualifier(ec,
                CourseOffering.crn.is(to_co_id));
        }
        catch (Exception e)
        {
            error(e);
            return null;
        }

        try
        {
        message.append("<p>student = ");
        message.append(student.toString());
        message.append(", from offering = ");
        message.append(fromCO.toString());
        message.append(", to offering = ");
        message.append(toCO.toString());
        message.append("</p>");

        // build map of assignments to assignments
        NSMutableDictionary<AssignmentOffering, AssignmentOffering>
            assignments = new NSMutableDictionary<AssignmentOffering,
            AssignmentOffering>();
        for (AssignmentOffering ao : AssignmentOffering
            .objectsMatchingQualifier(ec, AssignmentOffering
            .courseOffering.is(fromCO)))
        {
            NSArray<AssignmentOffering> targets = AssignmentOffering
                .objectsMatchingQualifier(ec, AssignmentOffering
                .assignment.is(ao.assignment()).and(AssignmentOffering
                .courseOffering.is(toCO)));
            if (targets.size() > 0)
            {
                assignments.put(ao, targets.get(0));
                if (targets.size() > 1)
                {
                    message.append("<p><strong>Multiple matches for "
                        + ao + ": " + targets + "</strong></p>");
                }
            }
            else
            {
                message.append("<p><strong>Cannot identify match for "
                    + ao + "</strong></p>");
            }
        }
        
        // remap lisresultid values
        
        for (LISResultId id : LISResultId.objectsMatchingQualifier(ec,
            LISResultId.user.is(student).and(LISResultId.assignmentOffering.dot(
                AssignmentOffering.courseOffering).is(fromCO))))
        {
            AssignmentOffering target =
                assignments.get(id.assignmentOffering());
            if (target == null)
            {
                message.append("<p><strong>Cannot remap LISResultId "
                    + id + ", no matching assignment offering "
                    + "found.</strong></p>");
            }
            else
            {
                id.setAssignmentOffering(target);
                message.append("<p>remapping LISResultId " + id + " => "
                    + target + "</p>");
            }
        }
        
        // remap course enrollments
        if (!toCO.isStudent(student))
        {
            toCO.addToStudentsRelationship(student);
            message.append("<p>adding user as student to target course</p>");
        }
        else
        {
            message.append("<p>student is already enrolled in target course</p>");
        }
        if (fromCO.isStudent(student))
        {
            fromCO.removeFromStudentsRelationship(student);
            message.append("<p>removing user as student from original course</p>");
        }
        else
        {
            message.append("<p>student is not enrolled in original course</p>");
        }

        // remap submissions
        NSArray<Submission> subs = Submission.objectsMatchingQualifier(ec,
            Submission.user.is(student).and(Submission.assignmentOffering
                .dot(AssignmentOffering.courseOffering).is(fromCO)));
        for (Submission sub : subs)
        {
            AssignmentOffering target =
                assignments.get(sub.assignmentOffering());
            if (target == null)
            {
                message.append("<p><strong>Cannot remap Submission "
                    + sub + ", no matching assignment offering "
                    + "found.</strong></p>");
             }
            else
            {
                sub.setAssignmentOffering(target);
                message.append("<p>remapping Submission " + sub + " => "
                  + target + "</p>");
            }
        }

        // Save changes
        confirmationMessage(message.toString());
        ec.saveChanges();
        for (Submission sub : subs)
        {
            sub.sendScoreToLTIConsumerIfNecessary();
        }
        }
        catch (Exception e)
        {
            error(e);
        }


        return null;
    }


    //~ Instance/static fields ................................................

}
