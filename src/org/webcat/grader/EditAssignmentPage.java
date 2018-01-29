/*==========================================================================*\
 |  $Id: EditAssignmentPage.java,v 1.15 2014/11/07 13:55:03 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2011 Virginia Tech
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
import com.webobjects.foundation.*;
import er.extensions.appserver.ERXDisplayGroup;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.ui.generators.JavascriptGenerator;
import org.webcat.woextensions.ECAction;
import static org.webcat.woextensions.ECAction.run;

// -------------------------------------------------------------------------
/**
 *  This class presents an assignment's properties so they can be edited.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.15 $, $Date: 2014/11/07 13:55:03 $
 */
public class EditAssignmentPage
    extends GraderAssignmentsComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public EditAssignmentPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public ERXDisplayGroup<Step>               scriptDisplayGroup;
    public int                                 index;
    public Step                                thisStep;
    public ERXDisplayGroup<SubmissionProfile>  submissionProfileDisplayGroup;
    public SubmissionProfile                   submissionProfile;
    public AssignmentOffering                  upcomingOffering;
    public AssignmentOffering                  thisOffering;
    public int                                 thisOfferingIndex;
    public Assignment                          assignment;
    public AssignmentOffering                  selectedOffering;
    public ERXDisplayGroup<AssignmentOffering> offeringGroup;

    public NSArray<GradingPlugin> gradingPluginsToAdd;
    public GradingPlugin gradingPluginToAdd;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        timeStart = System.currentTimeMillis();

        log.debug("starting appendToResponse()");
        currentTime = new NSTimestamp();

        // Get all the available grading plugins.
        gradingPluginsToAdd = GradingPlugin.pluginsAvailableToUser(
                localContext(), user());

        offeringGroup.setObjectArray(assignmentOfferings(courseOfferings()));
        if (selectedOffering == null)
        {
            if (offeringGroup.displayedObjects().count() > 0)
            {
                selectedOffering = offeringGroup.displayedObjects()
                    .objectAtIndex(0);
            }
            else
            {
                selectedOffering = prefs().assignmentOffering();
            }
        }
        if (assignment == null)
        {
            assignment = prefs().assignment();
            if (assignment == null && selectedOffering != null)
            {
                assignment = selectedOffering.assignment();
            }
        }
        scriptDisplayGroup.setMasterObject(assignment);
        // TODO: Fix NPEs on this page when no selectedOffering
        if (selectedOffering != null)
        {
            submissionProfileDisplayGroup.setObjectArray(
                SubmissionProfile.profilesForCourseIncludingMine(
                    localContext(),
                    user(),
                    selectedOffering.courseOffering().course(),
                    assignment.submissionProfile() )
                 );
        }
        log.debug("starting super.appendToResponse()");

        areDueDatesLocked = areDueDatesSame();

        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    protected void afterAppendToResponse(WOResponse response, WOContext context)
    {
        super.afterAppendToResponse(response, context);
        log.debug( "finishing super.appendToResponse()" );
        log.debug( "finishing appendToResponse()" );

        long timeTaken = System.currentTimeMillis() - timeStart;
        log.debug("Time in appendToResponse(): " + timeTaken + " ms");
    }


    // ----------------------------------------------------------
    public boolean validateURL( String assignUrl )
    {
        boolean result = ( assignUrl == null );
        if ( assignUrl != null )
        {
            try
            {
                // Try to create an instance of URL, which will
                // throw an exception if the name isn't valid
                result = ( new URL( assignUrl ) != null );
            }
            catch ( MalformedURLException e )
            {
                error( "The specified URL is not valid." );
                log.error( "Error in validateURL()", e );
            }
        }
        log.debug( "url validation = " + result );
        return result;
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
    public void flushNavigatorDerivedData()
    {
        selectedOffering = null;
        assignment = null;
    }


    // ----------------------------------------------------------
    /* Checks for errors, then records the current selections.
     *
     * @returns false if there is an error message to display.
     */
    protected boolean saveAndCanProceed()
    {
        return saveAndCanProceed( true );
    }


    // ----------------------------------------------------------
    /* Checks for errors, then records the current selections.
     *
     * @param requireProfile if true, a missing submission profile will
     *        trigger a false result
     * @returns false if there is an error message to display.
     */
    protected boolean saveAndCanProceed(boolean requireProfile)
    {
        if (requireProfile && assignment.submissionProfile() == null)
        {
            error("please select submission rules for this assignment.");
        }
        return validateURL(assignment.url()) && !hasMessages();
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        if (saveAndCanProceed())
        {
            return super.next();
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    public boolean applyEnabled()
    {
        return hasSubmissionProfile();
    }


    // ----------------------------------------------------------
    public boolean finishEnabled()
    {
        return applyEnabled();
    }


    // ----------------------------------------------------------
    public boolean applyLocalChanges()
    {
        return saveAndCanProceed() && super.applyLocalChanges();
    }


    // ----------------------------------------------------------
    public boolean hasSubmissionProfile()
    {
        return null != assignment.submissionProfile();
    }


    // ----------------------------------------------------------
    public WOComponent editSubmissionProfile()
    {
        WCComponent result = null;
        if ( saveAndCanProceed() )
        {
//            result = (WCComponent)pageWithName(
//                SelectSubmissionProfile.class.getName());
            result = pageWithName(EditSubmissionProfilePage.class);
            result.nextPage = this;
        }
        return result;
    }


    // ----------------------------------------------------------
    public WOComponent newSubmissionProfile()
    {
        WCComponent result = null;
        clearAllMessages();
        if (saveAndCanProceed(false))
        {
            SubmissionProfile newProfile = new SubmissionProfile();
            localContext().insertObject(newProfile);
            assignment.setSubmissionProfileRelationship(newProfile);
            newProfile.setAuthor(user());
            result = pageWithName(EditSubmissionProfilePage.class);
            result.nextPage = this;
        }
        return result;
    }


    // ----------------------------------------------------------
    public boolean canDeleteOffering(AssignmentOffering offering)
    {
        return !offering.isNewObject() && !offering.hasStudentSubmissions();
    }


    // ----------------------------------------------------------
    public boolean canDeleteThisOffering()
    {
        return canDeleteOffering(thisOffering);
    }


    // ----------------------------------------------------------
    public boolean canDeleteAnyOffering()
    {
        for (AssignmentOffering offering : offeringGroup.displayedObjects())
        {
            if (canDeleteOffering(offering))
            {
                return true;
            }
        }

        return false;
    }


    // ----------------------------------------------------------
    public boolean shouldShowDueDatePicker()
    {
        return !areDueDatesLocked
            || (offeringGroup.displayedObjects().count() > 0
                && thisOffering.equals(
                        offeringGroup.displayedObjects().objectAtIndex(0)));
    }


    // ----------------------------------------------------------
    public boolean areDueDatesLocked()
    {
        return areDueDatesLocked;
    }


    // ----------------------------------------------------------
    public boolean areDueDatesSame()
    {
        NSTimestamp exemplar = null;

        for (AssignmentOffering offering : offeringGroup.displayedObjects())
        {
            if (exemplar == null)
            {
                exemplar = offering.dueDate();
            }
            else
            {
                if (offering.dueDate() == null
                        || !exemplar.equals(offering.dueDate()))
                {
                    return false;
                }
            }
        }

        return true;
    }


    // ----------------------------------------------------------
    public WOActionResults lockDueDates()
    {
        if (saveAndCanProceed())
        {
            areDueDatesLocked = true;

            NSTimestamp exemplar = null;

            for (AssignmentOffering offering : offeringGroup.displayedObjects())
            {
                if (exemplar == null)
                {
                    exemplar = offering.dueDate();
                }
                else
                {
                    offering.setDueDate(exemplar);
                }
            }

            applyLocalChanges();
        }

        return new JavascriptGenerator()
            .refresh("allOfferings", "error-panel");
    }


    // ----------------------------------------------------------
    public WOActionResults unlockDueDates()
    {
        if (saveAndCanProceed())
        {
            areDueDatesLocked = false;
            applyLocalChanges();
        }

        return new JavascriptGenerator()
            .refresh("allOfferings", "error-panel");
    }


    // ----------------------------------------------------------
    public NSTimestamp dueDate()
    {
        return thisOffering.dueDate();
    }


    // ----------------------------------------------------------
    public void setDueDate(NSTimestamp value)
    {
        if (areDueDatesLocked)
        {
            for (AssignmentOffering offering : offeringGroup.displayedObjects())
            {
                offering.setDueDate(value);
            }
        }
        else
        {
            thisOffering.setDueDate(value);
        }
    }


    // ----------------------------------------------------------
    public boolean isPublished()
    {
        boolean published = true;

        for (AssignmentOffering offering : offeringGroup.displayedObjects())
        {
            if (!offering.publish())
            {
                published = false;
                break;
            }
        }

        return published;
    }


    // ----------------------------------------------------------
    public WOActionResults togglePublished()
    {
        return updatePublishedAction(!isPublished());
    }


    // ----------------------------------------------------------
    private WOActionResults updatePublishedAction(boolean value)
    {
        if (saveAndCanProceed())
        {
            for (AssignmentOffering offering : offeringGroup.displayedObjects())
            {
                offering.setPublish(value);
            }

            applyLocalChanges();
        }

        return new JavascriptGenerator()
            .refresh("allOfferingsActions", "error-panel");
    }


    // ----------------------------------------------------------
    public String iconForPublishedListItem()
    {
        return "icons/" + (isPublished() ? "eye.png" : "eye-half.png");
    }


    // ----------------------------------------------------------
    public boolean hasSuspendedSubs()
    {
        suspendedSubmissionCount = 0;

        for (AssignmentOffering offering : offeringGroup.displayedObjects())
        {
            suspendedSubmissionCount +=
                offering.suspendedSubmissionsInQueue().count();
        }

        return suspendedSubmissionCount > 0;
    }


    // ----------------------------------------------------------
    public int numSuspendedSubs()
    {
        return suspendedSubmissionCount;
    }


    // ----------------------------------------------------------
    public String descriptionOfEnqueuedSubmissions()
    {
        int active = thisOffering.activeSubmissionsInQueue().count();
        int suspended = thisOffering.suspendedSubmissionsInQueue().count();

        StringBuffer buffer = new StringBuffer();

        if (active == 0 && suspended == 0)
        {
            return "No submissions in queue";
        }
        else
        {
            if (active > 0 && suspended > 0)
            {
                buffer.append(Integer.toString(active));
                buffer.append(" <span class=\"check\">active</span>, ");
                buffer.append(Integer.toString(suspended));
                buffer.append(" <span class=\"warn\">suspended</span>");
            }
            else if (active > 0)
            {
                buffer.append(Integer.toString(active));
                buffer.append(" <span class=\"check\">active</span>");
            }
            else
            {
                buffer.append(Integer.toString(suspended));
                buffer.append(" <span class=\"warn\">suspended</span>");
            }
        }

        buffer.append(" submission");

        if (active + suspended > 1)
        {
            buffer.append('s');
        }

        buffer.append(" in queue");

        return buffer.toString();
    }


    // ----------------------------------------------------------
    public WOActionResults releaseSuspendedSubs()
    {
        log.info("releasing all paused assignments: "
              + assignment.titleString());

        run(new ECAction() {
            public void action()
            {
                NSMutableArray<EnqueuedJob> jobs =
                    new NSMutableArray<EnqueuedJob>();
                for (AssignmentOffering offering :
                    offeringGroup.displayedObjects())
                {
                    AssignmentOffering localAO = offering.localInstance(ec);
                    NSArray<EnqueuedJob> jobList =
                        localAO.suspendedSubmissionsInQueue();
                    for (EnqueuedJob job : jobList)
                    {
                        job.setPaused(false);
                        job.setQueueTime(new NSTimestamp());
                    }
                    jobs.addAll(jobList);
                    log.info("released " + jobList.count() + " jobs");
                }

                ec.saveChanges();
                GraderQueueProcessor.processJobs(jobs);
            }
        });

        // trigger the grading queue to read the released jobs

        return new JavascriptGenerator().refresh(
                "allOfferings", "allOfferingsActions", "error-panel");
    }


    // ----------------------------------------------------------
    public WOActionResults cancelSuspendedSubs()
    {
        run(new ECAction() { public void action() {
            for (AssignmentOffering offering : offeringGroup.displayedObjects())
            {
                AssignmentOffering localAO = offering.localInstance(ec);
                log.info(
                    "cancelling all paused assignments: "
                    + localAO.courseOffering().course().deptNumber()
                    + " "
                    + localAO.assignment().name());
                NSArray<EnqueuedJob> jobList =
                    localAO.suspendedSubmissionsInQueue();
                for (EnqueuedJob job : jobList)
                {
                    ec.deleteObject(job);
                }
                log.info("cancelled " + jobList.count() + " jobs");
            }

            ec.saveChanges();
        }});

        return new JavascriptGenerator().refresh(
            "allOfferings", "allOfferingsActions", "error-panel");
    }


    // ----------------------------------------------------------
    public WOActionResults toggleSuspended()
    {
        return updateSuspendedAction(!isSuspended());
    }


    // ----------------------------------------------------------
    private WOActionResults updateSuspendedAction(boolean value)
    {
        if (saveAndCanProceed())
        {
            for (AssignmentOffering offering : offeringGroup.displayedObjects())
            {
                offering.setGradingSuspended(value);
            }

            if (applyLocalChanges() && !value)
            {
                releaseSuspendedSubs();
            }
        }

        return new JavascriptGenerator().refresh(
                "allOfferings", "allOfferingsActions", "error-panel");
    }


    // ----------------------------------------------------------
    public boolean isSuspended()
    {
        boolean suspended = false;

        for (AssignmentOffering offering : offeringGroup.displayedObjects())
        {
            if (offering.gradingSuspended())
            {
                suspended = true;
                break;
            }
        }

        return suspended;
    }


    // ----------------------------------------------------------
    public String iconForSuspendedListItem()
    {
        return "icons/" + (isSuspended() ? "robot-off.png" : "robot.png");
    }


    // ----------------------------------------------------------
    public JavascriptGenerator addStep()
    {
        if (gradingPluginToAdd != null)
        {
            if (saveAndCanProceed())
            {
                assignment.addNewStep(gradingPluginToAdd);
                applyLocalChanges();

                scriptDisplayGroup.fetch();
            }
        }

        return new JavascriptGenerator()
            .refresh("gradingSteps", "error-panel")
            .unblock("gradingStepsTable");
    }


    // ----------------------------------------------------------
    public WOActionResults removeStepActionOK()
    {
        int pos = stepForAction.order();

        for (int i = pos;
             i < scriptDisplayGroup.displayedObjects().count();
             i++)
        {
            scriptDisplayGroup.displayedObjects().objectAtIndex(i).setOrder(i);
        }

        if (stepForAction.config() != null
                && stepForAction.config().steps().count() == 1)
        {
            StepConfig thisConfig = stepForAction.config();
            stepForAction.setConfigRelationship(null);
            localContext().deleteObject(thisConfig);
        }

        localContext().deleteObject(stepForAction);
        localContext().saveChanges();

        stepForAction = null;

        scriptDisplayGroup.fetch();

        return null; //FIXME new JavascriptGenerator().refresh("gradingSteps");
    }


    // ----------------------------------------------------------
    public WOActionResults removeStep()
    {
        if (!saveAndCanProceed())
        {
            return displayMessages();
        }

        stepForAction = thisStep;
        return new ConfirmingAction(this, false)
        {
            @Override
            protected String confirmationTitle()
            {
                return "Remove This Grading Step?";
            }

            @Override
            protected String confirmationMessage()
            {
                return "<p>Are you sure that you want to remove the grading"
                    + " step <b>" + stepForAction.gradingPlugin().name()
                    + "</b>? All of the configuration settings for this step"
                    + "will be lost. This cannot be undone.</p>";
            }

            @Override
            protected WOActionResults actionWasConfirmed()
            {
                return removeStepActionOK();
            }
        };
    }


    // ----------------------------------------------------------
    public boolean stepAllowsTimeout()
    {
        return thisStep.gradingPlugin().timeoutMultiplier() != 0;
    }


    // ----------------------------------------------------------
    public WOComponent editStep()
    {
        WCComponent result = null;
        if ( saveAndCanProceed() )
        {
            log.debug( "step = " + thisStep );
            prefs().setAssignmentOfferingRelationship(selectedOffering);
            prefs().setStepRelationship( thisStep );
            result = pageWithName(EditStepPage.class);
            result.nextPage = this;
        }
        return result;
    }


    // ----------------------------------------------------------
    public void gradingStepsWereDropped(
            String sourceId, int[] dragIndices,
            String targetId, int[] dropIndices,
            boolean isCopy)
    {
        NSMutableArray<Step> steps =
            scriptDisplayGroup.displayedObjects().mutableClone();
        NSMutableArray<Step> stepsRemoved = new NSMutableArray<Step>();
        TreeMap<Integer, Step> finalStepPositions =
            new TreeMap<Integer, Step>();

        for (int i = 0; i < dragIndices.length; i++)
        {
            Step step = steps.objectAtIndex(dragIndices[i]);
            finalStepPositions.put(dropIndices[i], step);
            stepsRemoved.addObject(step);
        }

        steps.removeObjectsInArray(stepsRemoved);

        for (Map.Entry<Integer, Step> movement : finalStepPositions.entrySet())
        {
            int dropIndex = movement.getKey();
            Step step = movement.getValue();

            steps.insertObjectAtIndex(step, dropIndex);
        }

        int order = 1;
        for (Step step : steps)
        {
            step.setOrder(order++);
        }

        scriptDisplayGroup.fetch();
    }


    // ----------------------------------------------------------
    public Integer stepTimeout()
    {
        log.debug( "step = " + thisStep );
        log.debug( "num steps = "
                   + scriptDisplayGroup.displayedObjects().count() );
        return thisStep.timeoutRaw();
    }


    // ----------------------------------------------------------
    public void setStepTimeout( Integer value )
    {
        if ( value != null && !Step.timeoutIsWithinLimits( value ) )
        {
            // set error message if timeout is out of range
            error(
                "The maximum timeout allowed is "
                + Step.maxTimeout()
                + ".  Contact the administrator for higher limits." );
        }
        // This will automatically restrict to the max value anyway
        thisStep.setTimeoutRaw( value );
    }


    // ----------------------------------------------------------
    public JavascriptGenerator clearGraph()
    {
        JavascriptGenerator js = new JavascriptGenerator();

        for (AssignmentOffering offering : offeringGroup.displayedObjects())
        {
            offering.clearGraphSummary();
            js.refresh("scoreHistogram" + offering.id());
        }

        applyLocalChanges();

        return js;
    }


    // ----------------------------------------------------------
    public void takeValuesFromRequest(WORequest arg0, WOContext arg1)
    {
        log.debug("takeValuesFromRequest()");
        long timeStartedHere = System.currentTimeMillis();

        super.takeValuesFromRequest(arg0, arg1);

        if (assignment != null)
        {
            log.debug("looking for similar submission profile by name");
            String name = assignment.name();
            if ( assignment.submissionProfile() == null
                 && name != null )
            {
                NSArray<AssignmentOffering> similar = AssignmentOffering
                    .offeringsWithSimilarNames(
                        localContext(),
                        name,
                        selectedOffering.courseOffering(),
                        1);
                if (similar.count() > 0)
                {
                    AssignmentOffering ao = similar.objectAtIndex(0);
                    assignment.setSubmissionProfile(
                        ao.assignment().submissionProfile());
                }
            }
        }

        long timeTaken = System.currentTimeMillis() - timeStartedHere;
        log.debug("Time in takeValuesFromRequest(): " + timeTaken + " ms");
    }


    // ----------------------------------------------------------
    private WOComponent flush(WOComponent page)
    {
        flushNavigatorDerivedData();
        return page;
    }


    // ----------------------------------------------------------
    private WOComponent deleteOfferingActionOk()
    {
        prefs().setAssignmentOfferingRelationship(null);
        for (AssignmentOffering ao : offeringGroup.displayedObjects())
        {
            if (ao != offeringForAction)
            {
                prefs().setAssignmentOfferingRelationship(ao);
                break;
            }
        }
        if (!applyLocalChanges()) return flush(null);
        localContext().deleteObject(offeringForAction);
        if (!applyLocalChanges()) return flush(null);
        if (assignment.offerings().count() == 0)
        {
            prefs().setAssignmentOfferingRelationship(null);
            prefs().setAssignmentRelationship(null);
            localContext().deleteObject(assignment);
            applyLocalChanges();
        }
        return flush(null);
    }


    // ----------------------------------------------------------
    public WOActionResults deleteOffering()
    {
        if (!applyLocalChanges())
        {
            return displayMessages();
        }

        offeringForAction = thisOffering;
        return new ConfirmingAction(this, false) {
            @Override
            protected String confirmationTitle()
            {
                return "Delete This Assignment Offering?";
            }

            @Override
            protected String confirmationMessage()
            {
                String message =
                    "<p>This action will <b>delete the assignment offering \""
                    + offeringForAction
                    + "\"</b>, "
                    + "together with any staff submissions that have been "
                    + "made to it.</p>";
                if (offeringForAction.assignment().offerings().count() == 1)
                {
                    message +=
                        "<p>Since this is the only offering of the selected "
                        + "assignment, this action will also <b>delete the "
                        + "assignment altogether</b>.  This action cannot be "
                        + "undone.</p>";
                }
                return message + "<p class=\"center\">Delete this "
                    + "assignment offering?</p>";
            }

            @Override
            protected WOActionResults actionWasConfirmed()
            {
                return deleteOfferingActionOk();
            }
        };
    }


    // ----------------------------------------------------------
    /**
     * Check whether the selected assignment is past the due date.
     *
     * @return true if any submissions to this assignment will be counted
     *         as late
     */
    public boolean upcomingOfferingIsLate()
    {
        NSTimestamp dueDate = upcomingOffering.dueDate();

        if (dueDate != null)
        {
            return dueDate.before(currentTime);
        }
        else
        {
            // FIXME is this the best answer?
            return true;
        }
    }


    // ----------------------------------------------------------
    public NSArray<AssignmentOffering> upcomingOfferings()
    {
        if (upcomingOfferings == null)
        {
            upcomingOfferings = AssignmentOffering.allOfferingsOrderedByDueDate(
                localContext()).mutableClone();
            upcomingOfferings.removeObject(selectedOffering);
        }
        return upcomingOfferings;
    }


    // ----------------------------------------------------------
    public TimeZone timeZone()
    {
        return TimeZone.getTimeZone(user().timeZoneName());
    }


    // ----------------------------------------------------------
    public Boolean surveysSupported()
    {
        if (surveysSupported == null)
        {
            surveysSupported = Boolean.valueOf(
                wcApplication().subsystemManager().subsystem("Opinions")
                != null);
        }
        return surveysSupported.booleanValue();
    }


    //~ Instance/static variables .............................................

    private int            suspendedSubmissionCount = 0;
    private NSMutableArray<AssignmentOffering> upcomingOfferings;
    private NSTimestamp    currentTime;
    private AssignmentOffering offeringForAction;
    private Step           stepForAction;
    private boolean        areDueDatesLocked;

    private long timeStart;

    private static Boolean surveysSupported;

    static Logger log = Logger.getLogger( EditAssignmentPage.class );
}
