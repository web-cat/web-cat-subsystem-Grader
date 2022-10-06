/*==========================================================================*\
 |  Copyright (C) 2006-2021 Virginia Tech
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
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.*;
import er.extensions.appserver.ERXDisplayGroup;
import er.extensions.eof.ERXQ;
import org.apache.log4j.Logger;
import org.webcat.archives.IArchiveEntry;
import org.webcat.core.*;
import org.webcat.ui.generators.JavascriptGenerator;

// -------------------------------------------------------------------------
/**
 * This wizard page summarizes past submissions and allows a student
 * to upload a program file for the current (new) submission.
 *
 * @author  Stephen Edwards
 */
public class UploadSubmissionPage
    extends GraderSubmissionUploadComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public UploadSubmissionPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public AssignmentOffering offering;
    public ERXDisplayGroup<Submission> submissionDisplayGroup;
    /** submission item in the repetition */
    public Submission aSubmission;
    /** index in repetition */
    public int index;
    /** true if there are previous submissions */
    public boolean hasPreviousSubmissions;
    public Boolean showStudentSelector;

    public ERXDisplayGroup<User> studentDisplayGroup;
    public User                  student;
    public User                  submitAsStudent;
    public NSMutableArray<User>  previousPartners;
    public NSMutableArray<User>  partnersForEditing;
    public User                  aPartner;
    public int                   partnerIndex;
    public int                   extraColumnCount;
    public EnergyBar             bar;


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
        log.debug("primeUser = " + wcSession().primeUser()
                  + ", localUser = " + user());
        if (showStudentSelector == null)
        {
            NSDictionary<?, ?> config =
                wcSession().tabs.selectedDescendant().config();
            showStudentSelector = Boolean.valueOf(
                config != null
                && config.containsKey("showStudentSelector"));
        }
        if (offering == null)
        {
            offering = prefs().assignmentOffering();
        }
        if (showStudentSelector)
        {
            studentDisplayGroup.setMasterObject(offering.courseOffering());
            if (submitAsStudent == null
                && studentDisplayGroup.displayedObjects().count() > 0)
            {
                submitAsStudent =
                    studentDisplayGroup.displayedObjects().objectAtIndex(0);
            }
        }
        int currentSubNo = fillDisplayGroup(user());
        hasPreviousSubmissions = submissionDisplayGroup.displayedObjects()
            .count() > 0;

        Submission latestSub =
            Submission.latestSubmissionForAssignmentOfferingAndUser(
                    localContext(), offering, user());

        if (offering != null)
        {
            bar = offering.energyBarForUser(user());
        }
        if (previousPartners == null)
        {
            if (!offering.assignment().submissionProfile().allowPartners())
            {
                previousPartners = new NSMutableArray<User>();
            }
            else if (submissionInProcess().partners() != null)
            {
                previousPartners =
                    submissionInProcess().partners().mutableClone();
            }
            else
            {
                previousPartners = new NSMutableArray<User>();

                if (latestSub != null)
                {
                    for (Submission partneredSub :
                        latestSub.partneredSubmissions())
                    {
                        previousPartners.addObject(partneredSub.user());
                    }
                }
            }
        }

        if (partnersForEditing == null)
        {
            partnersForEditing = previousPartners.mutableClone();
        }
        if (!offering.assignment().submissionProfile().allowPartners()
            && partnersForEditing.count() > 0)
        {
            partnersForEditing.clear();
        }

        Number maxSubmissions = offering.assignment().submissionProfile()
            .maxSubmissionsRaw();
        okayToSubmit = (maxSubmissions == null
                        || currentSubNo <= maxSubmissions.intValue());

        if (okayToSubmit)
        {
            startSubmission(currentSubNo, user(), offering);
        }

        if (offering.dueDate() != null)
        {
            log.debug("due = " + offering.dueDate().getTime());
            log.debug("grace = "
                + offering.assignment().submissionProfile().deadTimeDelta());

            NSTimestamp deadline = new NSTimestamp(
                offering.dueDate().getTime()
                + offering.assignment().submissionProfile().deadTimeDelta());
            log.debug("time = " + deadline);
        }

        extraColumnCount = 0;
        if (offering != null)
        {
            Assignment a = offering.assignment();
            if (a.usesTAScore())
            {
                extraColumnCount++;
            }
            if (a.usesTestingScore())
            {
                extraColumnCount++;
            }
            if (a.usesToolCheckScore())
            {
                extraColumnCount++;
            }
            if (a.usesBonusesOrPenalties())
            {
                extraColumnCount++;
            }
        }

        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    @Override
    public void flushNavigatorDerivedData()
    {
        offering = null;
        super.flushNavigatorDerivedData();
    }


    // ----------------------------------------------------------
    protected void afterAppendToResponse(
        WOResponse response, WOContext context)
    {
        super.afterAppendToResponse(response, context);
        oldBatchSize  = submissionDisplayGroup.numberOfObjectsPerBatch();
        oldBatchIndex = submissionDisplayGroup.currentBatchIndex();
        cachedUploadedFile     = submissionInProcess().uploadedFile();
        cachedUploadedFileName = submissionInProcess().uploadedFileName();
        cachedUploadedFileList = submissionInProcess().uploadedFileList();
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

        if (!result)
        {
            // If the assignment is closed and the user is not allowed to
            // submit to it (i.e., not a grader or instructor), then force the
            // assignment offering selection to be null and pop open the
            // navigator.

            if (offering == null)
            {
                offering = prefs().assignmentOffering();
            }
            AssignmentOffering assnOff = offering;

            if (!user().hasAdminPrivileges() &&
                (assnOff == null || !assnOff.userCanSubmit(user())))
            {
                prefs().setAssignmentOfferingRelationship(null);
                offering = null;
                result = true;
            }
        }

        return result;
    }


    // ----------------------------------------------------------
    /**
     * A predicate that indicates whether the user can proceed.
     * As a side-effect, it sets the error message if the user cannot
     * proceed.
     *
     * @return true if the user can proceed
     */
    public boolean okayToSubmit()
    {
        if (!okayToSubmit)
        {
            error("You have already made the maximum allowed "
                + "number of submissions for this assignment.");
        }
        return okayToSubmit;
    }


    // ----------------------------------------------------------
    public boolean nextEnabled()
    {
        return okayToSubmit();
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        offering = prefs().assignmentOffering();
        if (log.isDebugEnabled())
        {
            log.debug("next():");
            log.debug(" request = " + context().request());
            log.debug(" form values = " + context().request().formValues());
            log.debug(" multipart = "
                + context().request().isMultipartFormData());
        }
        if (showStudentSelector && submitAsStudent == null)
        {
            error("Please select a student for this submission.");
            return null;
        }
        if (offering == null)
        {
            error("Please select an assignment for this submission.");
            return null;
        }
        NSTimestamp now = new NSTimestamp();
        if (this.bar != null
            && !this.bar.hasEnergy()
            && !this.bar.isCloseToDeadline(now))
        {
            this.bar.logEvent(EnergyBar.SUBMISSION_DENIED, this.offering);
            error("Your submission energy is depleted.  Wait until your "
                + "energy regenerates before submitting.");
            return null;
        }
        if (okayToSubmit)
        {
            NSTimestamp deadline = new NSTimestamp(
                offering.dueDate().getTime()
                + offering.assignment().submissionProfile().deadTimeDelta());
            log.debug("deadline = " + deadline);
            log.debug("now = " + now);
            CourseOffering course = offering.courseOffering();
            User primeUser =
                wcSession().primeUser().localInstance(localContext());
            if (deadline.before(now)
                 && !course.isInstructor(primeUser)
                 && !course.isGrader(primeUser))
            {
                error("Unfortunately, the final deadline for this assignment "
                    + "has passed.  No more submissions are being accepted.");
                return null;
            }
            boolean clearFileList = true;
            if (!submissionInProcess().hasValidFileUpload())
            {
                submissionInProcess().setUploadedFile(cachedUploadedFile);
                submissionInProcess().setUploadedFileName(
                    cachedUploadedFileName);
                submissionInProcess().setUploadedFileList(
                    cachedUploadedFileList);
                clearFileList = false;
            }
            if (!submissionInProcess().hasValidFileUpload())
            {
                error("Please select a file to upload.");
                return null;
            }
            if (clearFileList)
            {
                submissionInProcess().setUploadedFileList(null);
            }
            if (submissionInProcess().uploadedFile().length() >
                offering.assignment().submissionProfile()
                    .effectiveMaxFileUploadSize())
            {
                error("You file exceeds the file size limit for this "
                    + "assignment ("
                    + offering.assignment().submissionProfile()
                        .effectiveMaxFileUploadSize()
                    + ").  Please choose a smaller file.");
                return null;
            }
            if (showStudentSelector)
            {
                setLocalUser(submitAsStudent);
                int currentSubNo = fillDisplayGroup(user());
                // restart the submission with a new user/number
                submissionInProcess().startSubmission(
                    user(), offering, currentSubNo);
            }
            else
            {
                submissionInProcess().startSubmission(
                    user(), offering, submissionInProcess().submitNumber());
            }

            submissionInProcess().setPartners(partnersForEditing);

            return super.next();
        }
        else
        {
            // If we get here, an error message has already been set
            // in okayToSubmit(), so just refresh the page to show it.
            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * A boolean predicate that indicates that this assignment
     * has instructor-provided instructions to show to the student.
     *
     * @return true if there are instructions to show
     */
    public boolean hasInstructions()
    {
        String instructions = offering.assignment().fileUploadMessage();
        return instructions != null && !instructions.equals("");
    }


    // ----------------------------------------------------------
    /**
     * Returns the file size for the currently uploaded file.
     *
     * @return the file size
     */
    public long uploadedFileSize()
    {
        long size = 0L;
        NSData file = submissionInProcess().uploadedFile();
        if (file != null)
        {
            size = file.length();
        }
        return size;
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        log.debug("defaultAction()");
        if (oldBatchSize != submissionDisplayGroup.numberOfObjectsPerBatch()
            || oldBatchIndex != submissionDisplayGroup.currentBatchIndex())
        {
            return null;
        }
        else
        {
            return super.defaultAction();
        }
    }


    // ----------------------------------------------------------
    public void cancelLocalChanges()
    {
        clearSubmission();
        resetPrimeUser();
        super.cancelLocalChanges();
    }


    // ----------------------------------------------------------
    public boolean allowsAllOfferingsForCourse()
    {
        return false;
    }


    // ----------------------------------------------------------
    public void takeValuesFromRequest(WORequest request, WOContext context)
    {
        try
        {
            super.takeValuesFromRequest(request, context);
        }
        catch (Exception e)
        {
            // Ignore it
        }
    }


    // ----------------------------------------------------------
    public String permalink()
    {
        if (offering != null)
        {
            return offering.permalink();
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    public void setPermalink(String value)
    {
        // Do nothing.
    }


    // ----------------------------------------------------------
    private int fillDisplayGroup(User user)
    {
        NSArray<Submission> submissions =
            Submission.submissionsForAssignmentOfferingAndUser(
                localContext(), offering, user);
        submissionDisplayGroup.setObjectArray(submissions);
        int currentSubNo = submissions.count() + 1;
        for (int i = 0; i < submissions.count(); i++)
        {
            int sno = submissions.objectAtIndex(i).submitNumber();
            if (sno >= currentSubNo)
            {
                currentSubNo = sno + 1;
            }
        }
        return currentSubNo;
    }


    // ----------------------------------------------------------
    public String showEditPartnersDialogScript()
    {
        return "dijit.byId('editPartnersDialog').show();";
    }


    // ----------------------------------------------------------
    public JavascriptGenerator partnersChanged()
    {
        previousPartners = partnersForEditing.mutableClone();

        JavascriptGenerator script = new JavascriptGenerator();
        script.refresh("partnersPane").
               dijit("editPartnersDialog").call("hide");

        return script;
    }


    // ----------------------------------------------------------
    public EOQualifier qualifierForStudentsInCourse()
    {
        CourseOffering courseOffering = offering.courseOffering();
        NSArray<CourseOffering> offerings =
            CourseOffering.offeringsForSemesterAndCourse(localContext(),
                courseOffering.course(),
                courseOffering.semester());

        EOQualifier[] enrollmentQuals = new EOQualifier[offerings.count()];
        int i = 0;
        for (CourseOffering co : offerings)
        {
            enrollmentQuals[i++] = User.enrolledIn.is(co);
        }

        return ERXQ.or(enrollmentQuals);
    }


    //~ Instance/static variables .............................................

    /** Saves the state of the batch navigator to detect setting changes */
    protected int oldBatchSize;
    /** Saves the state of the batch navigator to detect setting changes */
    protected int oldBatchIndex;

    protected NSData                 cachedUploadedFile;
    protected String                 cachedUploadedFileName;
    protected NSArray<IArchiveEntry> cachedUploadedFileList;

    /** True if the user has not reached the maximum allowable submissions
     *  for this assignment and its okay to submit another time.
     */
    protected boolean okayToSubmit;

    static Logger log = Logger.getLogger( UploadSubmissionPage.class );
}
