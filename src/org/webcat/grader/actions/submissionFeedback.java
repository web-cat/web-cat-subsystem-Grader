/*==========================================================================*\
 |  submission.java
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2026 Virginia Tech
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

package org.webcat.grader.actions;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import org.apache.log4j.Logger;
import org.webcat.core.Application;
import org.webcat.core.CourseOffering;
import org.webcat.core.Session;
import org.webcat.core.User;
import org.webcat.core.actions.WCDirectAction;
import org.webcat.grader.AssignmentOffering;
import org.webcat.grader.FinalReportPage;
import org.webcat.grader.GradeStudentSubmissionPage;
import org.webcat.grader.Submission;
import org.webcat.grader.SubmissionResult;

// -------------------------------------------------------------------------
/**
 * Direct action handler class providing session-based direct links to:
 * 1. The student view of feedback for a submission (view action, default)
 * 2. The grading/editing page for a submission (edit action)
 *
 * Both actions require the user to already be logged in (redirecting to login
 * if not), take a submission id number parameter, double-check access permissions,
 * initialize session preferences, and transfer to the appropriate page.
 *
 * @author PassPort API Generator
 */
public class submissionFeedback
    extends WCDirectAction
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new submission direct action handler.
     *
     * @param aRequest The request to respond to
     */
    public submissionFeedback(WORequest aRequest)
    {
        super(aRequest);
    }


    //~ Public Action Methods .................................................

    // ----------------------------------------------------------
    /**
     * Default direct action, which redirects to the view action.
     *
     * @return the result of viewAction()
     */
    public WOActionResults defaultAction()
    {
        return viewAction();
    }


    // ----------------------------------------------------------
    /**
     * Direct action to display the student view of feedback for a submission.
     * Accessible by the submitter, partners, course staff, and admins.
     *
     * @return the FinalReportPage or redirect to login / error response
     */
    public WOActionResults viewAction()
    {
        if (log.isDebugEnabled())
        {
            log.debug("submission.viewAction() called");
        }

        Session session = (Session) session();
        if (session == null || session.user() == null || session.primeUser() == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("submission.viewAction(): user not logged in, redirecting to login");
            }
            return Application.wcApplication().gotoLoginPage(context());
        }

        String idStr = idParameter();
        if (idStr == null || idStr.trim().isEmpty())
        {
            return errorResponse(WOResponse.HTTP_STATUS_NOT_FOUND,
                "Missing required parameter: id");
        }

        int idVal;
        try
        {
            idVal = Integer.parseInt(idStr.trim());
        }
        catch (NumberFormatException e)
        {
            return errorResponse(WOResponse.HTTP_STATUS_NOT_FOUND,
                "Invalid id parameter: " + idStr);
        }

        EOEditingContext ec = session.defaultEditingContext();
        Submission submission = Submission.forId(ec, idVal);
        SubmissionResult subResult = null;
        if (submission != null)
        {
            subResult = submission.result();
        }
        else
        {
            // Backwards-compatibility fallback if a SubmissionResult ID was passed
            subResult = SubmissionResult.forId(ec, idVal);
            if (subResult != null)
            {
                submission = subResult.submissionFor(session.user());
                if (submission == null)
                {
                    submission = subResult.submission();
                }
            }
        }

        if (submission == null)
        {
            return errorResponse(WOResponse.HTTP_STATUS_NOT_FOUND,
                "No submission found matching id: " + idVal);
        }

        if (!canView(session.user(), submission))
        {
            log.warn("User " + session.user().userName()
                + " denied view access to submission " + submission.id());
            return errorResponse(WOResponse.HTTP_STATUS_FORBIDDEN,
                "You do not have permission to view feedback for this submission.");
        }

        session.tabs.selectById("MostRecent");
        FinalReportPage page = (FinalReportPage) pageWithName(
            FinalReportPage.class.getName());
        page.submission = submission;
        if (subResult != null)
        {
            page.result = subResult;
        }

        AssignmentOffering ao = submission.assignmentOffering();
        if (ao != null)
        {
            page.coreSelections().setCourseOfferingRelationship(ao.courseOffering());
            if (ao.courseOffering() != null)
            {
                page.coreSelections().setCourseRelationship(ao.courseOffering().course());
            }
            page.prefs().setAssignmentOfferingRelationship(ao);
        }

        if (log.isDebugEnabled())
        {
            log.debug("submission.viewAction(): transferring to FinalReportPage for sub #"
                + submission.submitNumber());
        }
        return page;
    }


    // ----------------------------------------------------------
    /**
     * Direct action to display the grading page for a submission.
     * Accessible only by course staff (instructors, graders) and admins.
     *
     * @return the GradeStudentSubmissionPage or redirect to login / error response
     */
    public WOActionResults editAction()
    {
        if (log.isDebugEnabled())
        {
            log.debug("submission.editAction() called");
        }

        Session session = (Session) session();
        if (session == null || session.user() == null || session.primeUser() == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("submission.editAction(): user not logged in, redirecting to login");
            }
            return Application.wcApplication().gotoLoginPage(context());
        }

        String idStr = idParameter();
        if (idStr == null || idStr.trim().isEmpty())
        {
            return errorResponse(WOResponse.HTTP_STATUS_NOT_FOUND,
                "Missing required parameter: id");
        }

        int idVal;
        try
        {
            idVal = Integer.parseInt(idStr.trim());
        }
        catch (NumberFormatException e)
        {
            return errorResponse(WOResponse.HTTP_STATUS_NOT_FOUND,
                "Invalid id parameter: " + idStr);
        }

        EOEditingContext ec = session.defaultEditingContext();
        Submission submission = Submission.forId(ec, idVal);
        SubmissionResult subResult = null;
        if (submission != null)
        {
            subResult = submission.result();
        }
        else
        {
            // Backwards-compatibility fallback if a SubmissionResult ID was passed
            subResult = SubmissionResult.forId(ec, idVal);
            if (subResult != null)
            {
                submission = subResult.submission();
            }
        }

        if (submission == null)
        {
            return errorResponse(WOResponse.HTTP_STATUS_NOT_FOUND,
                "No submission found matching id: " + idVal);
        }

        if (!canEdit(session.user(), submission))
        {
            log.warn("User " + session.user().userName()
                + " denied edit/grader access to submission " + submission.id());
            return errorResponse(WOResponse.HTTP_STATUS_FORBIDDEN,
                "You do not have permission to grade or edit this submission.");
        }

        session.tabs.selectById("EnterGrades");
        GradeStudentSubmissionPage page =
            (GradeStudentSubmissionPage) pageWithName(
                GradeStudentSubmissionPage.class.getName());
        page.submission = submission;
        if (subResult != null)
        {
            page.result = subResult;
        }
        page.availableSubmissions = null;
        page.thisSubmissionIndex = 0;
        page.nextPage = null;

        AssignmentOffering ao = submission.assignmentOffering();
        if (ao != null)
        {
            page.coreSelections().setCourseOfferingRelationship(ao.courseOffering());
            if (ao.courseOffering() != null)
            {
                page.coreSelections().setCourseRelationship(ao.courseOffering().course());
            }
            page.prefs().setAssignmentOfferingRelationship(ao);
        }

        if (log.isDebugEnabled())
        {
            log.debug("submission.editAction(): transferring to GradeStudentSubmissionPage for sub #"
                + submission.submitNumber());
        }
        return page;
    }


    // ----------------------------------------------------------
    @Override
    public WOActionResults performActionNamed(String actionName)
    {
        if (actionName != null)
        {
            String lower = actionName.toLowerCase();
            if ("edit".equals(lower))
            {
                return editAction();
            }
            if ("view".equals(lower))
            {
                return viewAction();
            }
        }
        return super.performActionNamed(actionName);
    }


    //~ Private Helper Methods ................................................

    // ----------------------------------------------------------
    /**
     * Extracts the submission id from form values ('id' or 'sid').
     */
    private String idParameter()
    {
        String idStr = request().stringFormValueForKey("id");
        if (idStr == null || idStr.trim().isEmpty())
        {
            idStr = request().stringFormValueForKey("sid");
        }
        return idStr;
    }


    // ----------------------------------------------------------
    /**
     * Verifies if the specified user has permission to view the submission.
     */
    private boolean canView(User user, Submission submission)
    {
        if (user == null || submission == null)
        {
            return false;
        }
        if (user.hasAdminPrivileges())
        {
            return true;
        }
        AssignmentOffering ao = submission.assignmentOffering();
        if (ao != null && ao.courseOffering() != null)
        {
            CourseOffering co = ao.courseOffering();
            if (co.isStaff(user))
            {
                return true;
            }
        }
        Submission primary = (submission.primarySubmission() != null)
            ? submission.primarySubmission()
            : submission;
        if (primary.allUsers() != null && primary.allUsers().containsObject(user))
        {
            return true;
        }
        return false;
    }


    // ----------------------------------------------------------
    /**
     * Verifies if the specified user has permission to grade/edit the submission.
     */
    private boolean canEdit(User user, Submission submission)
    {
        if (user == null || submission == null)
        {
            return false;
        }
        if (user.hasAdminPrivileges())
        {
            return true;
        }
        AssignmentOffering ao = submission.assignmentOffering();
        if (ao != null && ao.courseOffering() != null)
        {
            CourseOffering co = ao.courseOffering();
            return co.isStaff(user);
        }
        return false;
    }


    // ----------------------------------------------------------
    /**
     * Builds a simple plain-text error response with the given status code.
     */
    private static WOResponse errorResponse(int status, String message)
    {
        WOResponse response = new WOResponse();
        response.setStatus(status);
        response.setHeader("text/plain; charset=UTF-8", "Content-Type");
        response.appendContentString(message);
        return response;
    }


    //~ Static fields .........................................................
    private static final Logger log = Logger.getLogger(submissionFeedback.class);
}
