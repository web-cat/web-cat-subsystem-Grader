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

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.net.URL;
import java.net.MalformedURLException;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 *  This class presents an assignment's properties so they can be edited.
 *
 *  @author Stephen Edwards
 *  @version $Id$
 */
public class EditAssignmentPage
    extends GraderAssignmentComponent
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

    public WODisplayGroup scriptDisplayGroup;
    public int            index;
    public Step           thisStep;
    public boolean        isSuspended;
    public WODisplayGroup    submissionProfileDisplayGroup;
    public SubmissionProfile submissionProfile; // For Repetition1
    public AssignmentOffering upcomingAssignment;
    public AssignmentOffering thisAssignment;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse(WOResponse response, WOContext context )
    {
        log.debug( "starting appendToResponse()" );
        upcomingAssignments = null;
        currentTime = new NSTimestamp();
        if (thisAssignment == null)
        {
            thisAssignment = prefs().assignmentOffering();
        }
        scriptDisplayGroup.setMasterObject( thisAssignment.assignment() );
        isSuspended = thisAssignment.gradingSuspended();
        submissionProfileDisplayGroup.setObjectArray(
            SubmissionProfile.profilesForCourseIncludingMine(
                localContext(),
                user(),
                thisAssignment.courseOffering().course(),
                thisAssignment.assignment().submissionProfile() )
             );
        log.debug( "starting super.appendToResponse()" );
        super.appendToResponse( response, context );
        log.debug( "finishing super.appendToResponse()" );
        log.debug( "finishing appendToResponse()" );
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
    protected boolean saveAndCanProceed( boolean requireProfile )
    {
        if (thisAssignment == null) return true;
        boolean offeringIsSuspended =
            thisAssignment.gradingSuspended();
        if ( offeringIsSuspended != isSuspended )
        {
            if ( ! offeringIsSuspended )
            {
                log.debug( "suspending grading on this assignment" );
                thisAssignment.setGradingSuspended( true );
                isSuspended = true;
            }
            else
            {
                log.debug( "resuming grading on this assignment" );
                thisAssignment.setGradingSuspended( false );
                // Have to save this change first!
                if (!applyLocalChanges()) return false;
                releaseSuspendedSubs();
            }
        }
        if ( requireProfile &&
             thisAssignment.assignment().submissionProfile()
             == null )
        {
            error(
                "please select submission rules for this assignment." );
        }
        return  validateURL( thisAssignment.assignment().url() )
            && !hasMessages();
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        if ( saveAndCanProceed() )
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
        return null !=
            thisAssignment.assignment().submissionProfile();
    }


    // ----------------------------------------------------------
    public WOComponent editSubmissionProfile()
    {
        WCComponent result = null;
        if ( saveAndCanProceed() )
        {
//            result = (WCComponent)pageWithName(
//                SelectSubmissionProfile.class.getName() );
            result = (WCComponent)pageWithName(
                EditSubmissionProfilePage.class.getName() );
            result.nextPage = this;
        }
        return result;
    }


    // ----------------------------------------------------------
    public WOComponent newSubmissionProfile()
    {
        WCComponent result = null;
        if ( saveAndCanProceed( false ) )
        {
            SubmissionProfile newProfile = new SubmissionProfile();
            localContext().insertObject( newProfile );
            Assignment selectedAssignment =
                thisAssignment.assignment();
            selectedAssignment.setSubmissionProfileRelationship( newProfile );
            newProfile.setAuthor( user() );
            result = (WCComponent)pageWithName(
                EditSubmissionProfilePage.class.getName() );
            result.nextPage = this;
        }
        return result;
    }


    // ----------------------------------------------------------
    public boolean hasSuspendedSubs()
    {
        log.debug( "script count = "
                   + scriptDisplayGroup.displayedObjects().count() );
        suspendedSubmissionCount =
            thisAssignment.getSuspendedSubs().count();
        return suspendedSubmissionCount > 0;
    }


    // ----------------------------------------------------------
    public int numSuspendedSubs()
    {
        return suspendedSubmissionCount;
    }


    // ----------------------------------------------------------
    public WOComponent releaseSuspendedSubs()
    {
        log.info( "releasing all paused assignments: "
              + thisAssignment.titleString() );
        EOEditingContext ec = Application.newPeerEditingContext();
        try
        {
            ec.lock();
            AssignmentOffering localAO = thisAssignment.localInstance( ec );
            NSArray jobList = localAO.getSuspendedSubs();
            for ( int i = 0; i < jobList.count(); i++ )
            {
                EnqueuedJob job = (EnqueuedJob)jobList.objectAtIndex( i );
                job.setPaused( false );
                job.setQueueTime( new NSTimestamp() );
            }
            log.info( "released " + jobList.count() + " jobs" );
            ec.saveChanges();
        }
        finally
        {
            ec.unlock();
            Application.releasePeerEditingContext( ec );
        }
        // trigger the grading queue to read the released jobs
        Grader.getInstance().graderQueue().enqueue( null );
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent cancelSuspendedSubs()
    {
        EOEditingContext ec = Application.newPeerEditingContext();
        try
        {
            ec.lock();
            AssignmentOffering localAO = thisAssignment.localInstance( ec );
            log.info( "cancelling all paused assignments: "
                      + coreSelections().courseOffering().course().deptNumber()
                      + " "
                      + localAO.assignment().name() );
            NSArray jobList = localAO.getSuspendedSubs();
            for ( int i = 0; i < jobList.count(); i++ )
            {
                EnqueuedJob job = (EnqueuedJob)jobList.objectAtIndex( i );
                ec.deleteObject( job );
            }
            log.info( "cancelled " + jobList.count() + " jobs" );
            ec.saveChanges();
        }
        finally
        {
            ec.unlock();
            Application.releasePeerEditingContext( ec );
        }
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent regradeSubsActionOk()
    {
        if (!applyLocalChanges()) return null;
        thisAssignment.regradeMostRecentSubsForAll( localContext() );
        applyLocalChanges();
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent regradeSubs()
    {
        ConfirmPage confirmPage = null;
        if ( saveAndCanProceed() )
        {
            confirmPage =
                (ConfirmPage)pageWithName( ConfirmPage.class.getName() );
            confirmPage.nextPage       = this;
            confirmPage.message        =
                "This action will <b>regrade the most recent submission "
                + "for every student</b> who has submitted to this "
                + "assignment.</p><p>This will also <b>delete all prior "
                + "results</b> for the submissions to be regraded and "
                + "<b>delete all TA comments and scoring</b> that have been "
                + "recorded for the submissions to be regraded.</p><p>Each "
                + "student\'s most recent submission will be re-queued for "
                + "grading, and each student will receive an e-mail message "
                + "when their new results are available.";
            confirmPage.actionReceiver = this;
            confirmPage.actionOk       = "regradeSubsActionOk";
            confirmPage.setTitle( "Confirm Regrade of All Submissions" );
        }
        return confirmPage;
    }


    // ----------------------------------------------------------
    public WOComponent addStep()
    {
        WCComponent result = null;
        if ( saveAndCanProceed() )
        {
            result = (WCComponent)pageWithName(
                PickStepPage.class.getName() );
            result.nextPage = this;
        }
        return result;
    }


    // ----------------------------------------------------------
    public WOComponent removeStep()
    {
        int pos = thisStep.order();
        for ( int i = pos;
              i < scriptDisplayGroup.displayedObjects().count();
              i++ )
        {
            ( (Step)scriptDisplayGroup.displayedObjects()
                .objectAtIndex( i ) ).setOrder( i );
        }
        if (    thisStep.config() != null
             && thisStep.config().steps().count() == 1 )
        {
            StepConfig thisConfig = thisStep.config();
            thisStep.setConfigRelationship( null );
            localContext().deleteObject( thisConfig );
        }
        localContext().deleteObject( thisStep );
        localContext().saveChanges();
        return null;
    }


    // ----------------------------------------------------------
    public boolean stepAllowsTimeout()
    {
        return thisStep.script().timeoutMultiplier() != 0;
    }


    // ----------------------------------------------------------
    public WOComponent editStep()
    {
        WCComponent result = null;
        if ( saveAndCanProceed() )
        {
            log.debug( "step = " + thisStep );
            prefs().setAssignmentOfferingRelationship( thisAssignment );
            prefs().setStepRelationship( thisStep );
            result = (WCComponent)pageWithName(
                EditStepPage.class.getName() );
            result.nextPage = this;
        }
        return result;
    }


    // ----------------------------------------------------------
    public WOComponent moveStepUp()
    {
        int oldOrder = thisStep.order();
        if ( oldOrder > 1 )
        {
            int switchWithIndex = oldOrder - 2;
            Step switchWith = (Step)scriptDisplayGroup.displayedObjects()
                .objectAtIndex( switchWithIndex );
            int newOrder = switchWith.order();
            thisStep.setOrder( newOrder );
            switchWith.setOrder( oldOrder );
        }
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent moveStepDown()
    {
        int oldOrder = thisStep.order();
        // order value is index + 1, and we need to make
        // sure there is at least one more element, so make sure that
        // order value is less that number of scripts
        if ( oldOrder < scriptDisplayGroup.displayedObjects().count() )
        {
            // switch with one more than this index, which is same as order
            int switchWithIndex = oldOrder;
            Step switchWith = (Step)scriptDisplayGroup.displayedObjects()
                .objectAtIndex( switchWithIndex );
            int newOrder = switchWith.order();
            thisStep.setOrder( newOrder );
            switchWith.setOrder( oldOrder );
        }
        return null;
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
    public WOComponent clearGraph()
    {
        thisAssignment.clearGraphSummary();
        applyLocalChanges();
        return null;
    }


    // ----------------------------------------------------------
    public void takeValuesFromRequest( WORequest arg0, WOContext arg1 )
    {
        super.takeValuesFromRequest( arg0, arg1 );
        String name = thisAssignment.assignment().name();
        if ( thisAssignment.assignment().submissionProfile()
             == null
             && name != null )
        {
            NSArray similar = AssignmentOffering.offeringsWithSimilarNames(
                localContext(),
                name,
                thisAssignment.courseOffering(),
                1 );
            if ( similar.count() > 0 )
            {
                AssignmentOffering ao =
                    (AssignmentOffering)similar.objectAtIndex( 0 );
                thisAssignment.assignment().setSubmissionProfile(
                    ao.assignment().submissionProfile() );
            }
        }
    }


    // ----------------------------------------------------------
    public WOComponent deleteActionOk()
    {
        prefs().setAssignmentOfferingRelationship( null );
        if (!applyLocalChanges()) return null;
        Assignment assignment = thisAssignment.assignment();
        localContext().deleteObject(thisAssignment);
        if (!applyLocalChanges()) return null;
        thisAssignment = null;
        if (assignment.offerings().count() == 0)
        {
            localContext().deleteObject(assignment);
            if (!applyLocalChanges()) return null;
        }
        return finish();
    }


    // ----------------------------------------------------------
    public WOComponent delete()
    {
        ConfirmPage confirmPage = null;
        if ( saveAndCanProceed() )
        {
            confirmPage =
                (ConfirmPage)pageWithName( ConfirmPage.class.getName() );
            confirmPage.nextPage       = this;
            confirmPage.message        =
                "This action will <b>delete the assignment offering</b>, "
                + "together with any staff submissions that have been "
                + "made to it.</p>";
            if (thisAssignment.assignment().offerings().count() > 1)
            {
                confirmPage.message +=
                    "<p>Since this is the only offering of the selected "
                    + "assignment, this action will also <b>delete the "
                    + "assignment altogether</b>.  This action cannot be "
                    + "undone.</p>";
            }
            confirmPage.actionReceiver = this;
            confirmPage.actionOk       = "deleteActionOk";
            confirmPage.setTitle( "Confirm Delete Request" );
        }
        return confirmPage;
    }


    // ----------------------------------------------------------
    /**
     * Check whether the selected assignment is past the due date.
     *
     * @return true if any submissions to this assignment will be counted
     *         as late
     */
    public boolean upcomingAssignmentIsLate()
    {
        return upcomingAssignment.dueDate().before( currentTime );
    }


    // ----------------------------------------------------------
    public NSArray upcomingAssignments()
    {
        if ( upcomingAssignments == null )
        {
            upcomingAssignments = AssignmentOffering.objectsForAllOfferings(
                localContext() ).mutableClone();
            upcomingAssignments.removeObject( thisAssignment );
        }
        return upcomingAssignments;
    }


    //~ Instance/static variables .............................................

    private int            suspendedSubmissionCount = 0;
    private NSMutableArray upcomingAssignments;
    private NSTimestamp    currentTime;

    static Logger log = Logger.getLogger( EditAssignmentPage.class );
}
