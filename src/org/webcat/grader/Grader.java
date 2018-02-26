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

import org.apache.log4j.Logger;
import org.webcat.core.Application;
import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.EntityResourceRequestHandler;
import org.webcat.core.Session;
import org.webcat.core.Subsystem;
import org.webcat.core.TabDescriptor;
import org.webcat.core.User;
import org.webcat.core.lti.LTIMessagePage;
import org.webcat.core.messaging.UnexpectedExceptionMessage;
import org.webcat.grader.lti.GraderLTILaunchRequest;
import org.webcat.grader.lti.LISResultId;
import org.webcat.grader.messaging.AdminReportsForSubmissionMessage;
import org.webcat.grader.messaging.GraderKilledMessage;
import org.webcat.grader.messaging.GraderMarkupParseError;
import org.webcat.grader.messaging.GradingResultsAvailableMessage;
import org.webcat.grader.messaging.SubmissionSuspendedMessage;
import org.webcat.woextensions.ECAction;
import org.webcat.woextensions.WCFetchSpecification;
import static org.webcat.woextensions.ECAction.run;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSNumberFormatter;
import com.webobjects.foundation.NSTimestamp;
import er.extensions.qualifiers.ERXKeyValueQualifier;

//-------------------------------------------------------------------------
/**
 *  The subsystem defining Web-CAT administrative tasks.
 *
 *  @author  Stephen Edwards
 */
public class Grader
   extends Subsystem
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new Grader subsystem object.
     */
    public Grader()
    {
        super();
        instance = this;
    }


    // ----------------------------------------------------------
    /**
     * Returns the current subsystem object.  In principle, only one instance
     * of this class exists.  However, we're not using the singleton pattern
     * exactly, since the instance is created using a normal constructor
     * via reflection.  However, this class has a private static data member
     * that keeps track of the most recently created instance, and this
     * method provides access to it.  The result is much like a singleton,
     * but without the guarantees provided by a hidden constructor.
     * @return the current Grader subsystem instance
     */
    public static Grader getInstance()
    {
        return instance;
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Performs all initialization actions for this subsystem.
     */
    public void init()
    {
        super.init();

        // Register notification messages.

        GradingResultsAvailableMessage.register();
        AdminReportsForSubmissionMessage.register();
        SubmissionSuspendedMessage.register();
        GraderKilledMessage.register();
        GraderMarkupParseError.register();

        EntityResourceRequestHandler.registerHandler(GradingPlugin.class,
                new GradingPluginResourceHandler());

        // Install or update any plug-ins that need it

        GradingPlugin.autoUpdateAndInstall();

        EnergyBarConfig.ensureDefaultConfig();
    }

    // ----------------------------------------------------------
    /**
     * Performs all startup actions for this subsystem.
     */
    public void start()
    {
        // Remove stale paused jobs
        new ECAction() { public void action() {
            // First, attempt to force the initial JNDI exception because
            // the name jdbc is not bound
            try
            {
                EnqueuedJob.allObjects(ec);
            }
            catch (Exception e)
            {
                // Silently swallow it, then retry on the next line
            }
            try
            {
                for (EnqueuedJob job : EnqueuedJob.objectsMatchingQualifier(ec,
                    EnqueuedJob.paused.isTrue().and(
                        EnqueuedJob.queueTime.before(
                            new NSTimestamp().timestampByAddingGregorianUnits(
                                0, -3, 0, 0, 0, 0)
                        ))))
                {
                    job.delete();
                }
            }
            catch (Exception e)
            {
                log.error(
                    "Unable to purge old stale paused jobs due to exception",
                    e);
            }
            try
            {
                for (EnqueuedJob job : EnqueuedJob.objectsMatchingQualifier(ec,
                    EnqueuedJob.processor.isNotNull()))
                {
                    job.setProcessorRaw(null);
                }
            }
            catch (Exception e)
            {
                log.error("Unable to purge old job processor settings due "
                    + "to exception", e);
            }
            ec.saveChanges();

            // Resume any enqueued jobs (if grader is coming back up
            // after an application restart)
            GraderQueueProcessor.processJobs(
                EnqueuedJob.objectsMatchingQualifier(ec,
                    EnqueuedJob.paused.isFalse().or(
                    EnqueuedJob.paused.isNull()),
                    EnqueuedJob.queueTime.ascs()));
        }}.run();
//        new Thread(new FiveMinuteMaintenance()).start();
    }


    // ----------------------------------------------------------
    /**
     * Initialize the subsystem-specific session data in a newly created
     * session object.  This method is called once by the core for
     * each newly created session object.
     *
     * @param s The new session object
     */
    public void initializeSessionData(Session s)
    {
        super.initializeSessionData(s);
        try
        {
            Assignment.allObjects(s.sessionContext());
        }
        catch (Exception e)
        {
            // Swallow the exception--we want to force a failure on
            // the first cross-model search in this session, so that
            // later searches will work OK.
        }
    }


    // ----------------------------------------------------------
    /**
     * Generate the component definitions and bindings for a given
     * pre-defined information fragment, so that the result can be
     * plugged into other pages defined elsewhere in the system.
     * @param fragmentKey the identifier for the fragment to generate
     *        (see the keys defined in {@link SubsystemFragmentCollector}
     * @param htmlBuffer add the html template for the subsystem's fragment
     *        to this buffer
     * @param wodBuffer add the binding definitions (the .wod file contents)
     *        for the subsystem's fragment to this buffer
     */
/*    public void collectSubsystemFragments(
        String fragmentKey, StringBuffer htmlBuffer, StringBuffer wodBuffer )
    {
        if ( fragmentKey.equals(
                        SubsystemFragmentCollector.SYSTEM_STATUS_ROWS_KEY ) )
        {
            htmlBuffer.append( "<webobject name=\"GraderSystemStatusRows\"/>" );
            wodBuffer.append(
                "GraderSystemStatusRows: "
                + GraderSystemStatusRows.class.getName()
                + "{ index = ^index; }\n"
            );
        }
        else if ( fragmentKey.equals(
                        SubsystemFragmentCollector.HOME_STATUS_KEY ) )
        {
            htmlBuffer.append( "<webobject name=\"GraderHomeStatus\"/>" );
            wodBuffer.append(
                "GraderHomeStatus: "
                + GraderHomeStatus.class.getName()
                + "{}\n"
            );
        }
    }*/


    // ----------------------------------------------------------
    /**
     * Find out how many grading jobs have been processed so far.
     *
     * @return the number of jobs process so far
     */
    public int processedJobCount()
    {
        return GraderQueueProcessor.processedJobCount();
    }


    // ----------------------------------------------------------
    /**
     * Find out the processing delay for the most recently completed job.
     *
     * @return the time in milliseconds
     */
    public long mostRecentJobWait()
    {
        return GraderQueueProcessor.mostRecentJobWait();
    }


    // ----------------------------------------------------------
    /**
     * Find out the processing delay for the most recently completed job.
     *
     * @return the time in milliseconds
     */
    public long estimatedJobTime()
    {
        return GraderQueueProcessor.estimatedJobTime();
    }


    // ----------------------------------------------------------
    /**
     * Handle a direct action request.  The user's login session will be
     * passed in as well.
     *
     * @param request the request to respond to
     * @param session the user's session
     * @param context the context for the request
     * @return The response page or contents
     */
    public WOActionResults handleDirectAction(
        WORequest request,
        Session   session,
        WOContext context)
    {
        return handleDirectAction(request, session, context, null);
    }


    // ----------------------------------------------------------
    /**
     * Handle a direct action request.  The user's login session will be
     * passed in as well.
     *
     * @param request the request to respond to
     * @param session the user's session
     * @param context the context for the request
     * @return The response page or contents
     */
    public WOActionResults handleDirectAction(
        WORequest request,
        Session   session,
        WOContext context,
        NSDictionary<String, NSData> files)
    {
        // Wait until this subsystem has actually started
        while (!hasStarted())
        {
            try
            {
                // sleep for 2 seconds
                Thread.sleep(2000);
            }
            catch (InterruptedException e)
            {
                // silently repeat the loop
            }
        }

//      log.debug( "handleDirectAction(): session = " + session );
//      log.debug( "handleDirectAction(): context = " + context );
        WOActionResults results = null;
        log.debug("path = " + request.requestHandlerPath());
        if ("cmsRequest".equals( request.requestHandlerPath()))
        {
            return handleCmsRequest(request, session, context);
        }
        if (session == null)
        {
            log.error("handleDirectAction(): null session");
            log.error(Application.extraInfoForContext(context));
        }
        else if (!context.hasSession())
        {
            log.error("handleDirectAction(): no session on context!");
            log.error(Application.extraInfoForContext(context));
        }
        else if (session != context.session())
        {
            log.error("handleDirectAction(): session mismatch with context!");
            log.error("session = " + session);
            log.error("context session = " + context.session());
            log.error(Application.extraInfoForContext(context));
        }
        if ("submit".equals(request.requestHandlerPath()))
        {
            results = handleSubmission(request, session, context, files);
        }
        else if ("ltiLaunch".equals(request.requestHandlerPath()))
        {
            results = handleLTILaunch(request, session, context);
        }
        else
        {
            results = handleReport(request, session, context);
        }
//      log.debug("handleDirectAction() returning");
        return results;
    }


    // ----------------------------------------------------------
    /**
     * Handle a direct action request.  The user's login session will be
     * passed in as well.
     *
     * @param request the request to respond to
     * @param session the user's session
     * @param context the context for the request
     * @return The response page or contents
     */
    public WOActionResults handleCmsRequest(
        WORequest request,
        Session   session,
        WOContext context)
    {
        CmsResponse result = Application.wcApplication()
            .pageWithName(CmsResponse.class, context);
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Handle a direct action request.  The user's login session will be
     * passed in as well.
     *
     * @param request the request to respond to
     * @param session the user's session
     * @param context the context for the request
     * @return The response page or contents
     */
    public WOActionResults handleSubmission(
        WORequest request,
        Session   session,
        WOContext context,
        NSDictionary <String, NSData> files)
    {
        log.debug("handleSubmission()");
        String scheme   = request.stringFormValueForKey("a");
        log.debug("scheme = " + scheme);
        String crn      = request.stringFormValueForKey("crn");
        log.debug("crn = " + crn);
        Integer courseNo = null;
        try
        {
            Number num = request.numericFormValueForKey("course",
                new NSNumberFormatter("0"));
            courseNo = (num instanceof Integer)
                ? (Integer)num
                : new Integer(num.intValue());
        }
        catch (Exception e)
        {
            // Ignore it, and treat it as an undefined course number
        }
        log.debug("courseNo = " + courseNo);
        String partnerList = request.stringFormValueForKey("partners");
        log.debug("partners = " + partnerList);
        NSData file = (NSData)request.formValueForKey("file1");
        if (files != null && files.containsKey("file1"))
        {
            file = files.get("file1");
        }
        String fileName = request.stringFormValueForKey("file1.filename");
        log.debug("fileName = " + fileName);

        SubmitResponse result = Application.wcApplication().pageWithName(
            SubmitResponse.class, context);
        EOEditingContext ec = result.localContext();
        result.sessionID = session.sessionID();
        log.debug("handleSubmission(): sessionID = " + result.sessionID);
        NSTimestamp currentTime   = new NSTimestamp();
        log.debug("user = " + session.user()
            + "(prime = " + session.primeUser() + ")");

        NSArray<EOSortOrdering> orderings = AssignmentOffering.dueDate.descs();
        ERXKeyValueQualifier matchesScheme = AssignmentOffering.assignment
            .dot(Assignment.name).eq(scheme);
        EOQualifier qualifier = matchesScheme;
        NSArray<AssignmentOffering> assignments = null;
        if (crn != null)
        {
            qualifier = matchesScheme.and(AssignmentOffering.courseOffering
                .dot(CourseOffering.crn).eq(crn));
        }
        else if (courseNo != null)
        {
            qualifier = matchesScheme.and(AssignmentOffering.courseOffering
                .dot(CourseOffering.course).dot(Course.number).eq(courseNo));
        }

        try
        {
            assignments = AssignmentOffering.objectsMatchingQualifier(
                ec, qualifier, orderings);
        }
        catch (Exception e)
        {
            assignments = AssignmentOffering.objectsMatchingQualifier(
                ec, qualifier, orderings);
        }
        AssignmentOffering assignment = null;
        User localizedUser = result.user();
        if (assignments != null && assignments.count() > 0)
        {
            String msg = null;
            for (AssignmentOffering thisAssignment : assignments)
            {
                log.debug("assignment = " + thisAssignment.assignment().name());
                CourseOffering co = thisAssignment.courseOffering();
                if (co.isInstructor(localizedUser)
                    || co.isGrader(localizedUser)
                    || (co.students().contains(localizedUser)
                        && thisAssignment.publish()
                        && currentTime.after(thisAssignment.availableFrom())
                        && currentTime.before(thisAssignment.lateDeadline())))
                {
                    log.debug("found matching assignment that is open.");
                    if (assignment == null)
                    {
                        assignment = thisAssignment;
                    }
                    else
                    {
                        if (msg == null)
                        {
                            msg = "Warning: multiple matching assignments "
                                + "found.<br/>"
                                + "Submitting to: " + assignment;
                        }
                        msg += "<br/>Ignoring: " + thisAssignment;
                    }
                }
            }
            if (msg != null)
            {
                result.errorMessages.add(msg);
                msg = msg.replaceAll("<br/>", "\n\t");
                log.warn(msg + "\n\tUser = " + session.user());
            }
        }

        if (assignment == null)
        {
            log.debug("no assignments are open.");
            String msg = "The requested assignment is not accepting "
                + "submissions at this time or it could not be found.  "
                + "The deadline may have passed.";
            result.errorMessages.add(msg);
            result.assignmentClosed = true;
            log.warn(msg + "  User = " + session.user() + ", a = " + scheme
                + ", crn = " + crn + ", courseNo = " + courseNo);
            return result.generateResponse();
        }

        EnergyBar bar = assignment.energyBarForUser(localizedUser);
        if ((bar != null) &&
            (!bar.hasEnergy()) &&
            (!bar.isCloseToDeadline(currentTime)))
        {
            bar.logEvent(EnergyBar.SUBMISSION_DENIED, assignment);
            log.debug("no submission energy for " + bar);
            result.noEnergy = true;
            return result.generateResponse();
        }

        result.coreSelections().setCourseOfferingRelationship(
            assignment.courseOffering());
        result.prefs().setAssignmentOfferingRelationship(assignment);
        NSArray<Submission> submissions =
            Submission.submissionsForAssignmentOfferingAndUser(
                ec, assignment, result.user());
        int currentSubNo = submissions.count() + 1;
        for (int i = 0; i < submissions.count(); i++)
        {
            int sno = submissions.objectAtIndex(i).submitNumber();
            if (sno >= currentSubNo)
            {
                currentSubNo = sno + 1;
            }
        }

        // TODO: This max submission check doesn't take partners into account
        Number maxSubmissions = assignment.assignment().submissionProfile()
            .maxSubmissionsRaw();
        if (maxSubmissions != null
            && currentSubNo > maxSubmissions.intValue()
            && !assignment.courseOffering().isStaff(session.user()))
        {
            String msg = "You have exceeded the allowable number "
                + "of submissions for this assignment.";
            result.errorMessages.add(msg);
            log.warn(msg + "  User = " + session.user()
                + "\n\t" + assignment);
            return result.generateResponse();
        }

        // Parse the partner list and get the User objects.

        NSMutableArray<User> partners = new NSMutableArray<User>();
        NSMutableArray<String> partnersNotFound = new NSMutableArray<String>();

        if (partnerList != null)
        {
            String[] usernames = partnerList.split("[,\\s]+");

            for (String username : usernames)
            {
                username = username.trim();

                if (username.length() > 0)
                {
                    User partner = User.userWithDomainAndName(
                        ec, session.user().authenticationDomain(), username);

                    if (partner != null)
                    {
                        partners.addObject(partner);
                    }
                    else
                    {
                        partnersNotFound.addObject(username);
                    }
                }
            }
        }

        result.partnersNotFound = partnersNotFound;
        result.startSubmission(currentSubNo, result.user());
        result.submissionInProcess().setPartners(partners);
        result.submissionInProcess().setUploadedFile(file);
        result.submissionInProcess().setUploadedFileName(fileName);

        int len = 0;
        try
        {
            len = file.length();
        }
        catch (Exception e)
        {
            // Ignore it: length() could produce an NPE on a bad POST request
        }
        if (len == 0)
        {
            result.clearSubmission();
            result.submissionInProcess().clearUpload();
            String msg = "Your file submission is empty.  "
                + "Please choose an appropriate file.";
            result.errorMessages.add(msg);
            log.warn(msg + "  User = " + session.user()
                + "\n\t" + assignment);
            return result.generateResponse();
        }
        else if (len > assignment.assignment().submissionProfile()
                        .effectiveMaxFileUploadSize())
        {
            result.clearSubmission();
            result.submissionInProcess().clearUpload();
            String msg = "Your file exceeds the file size limit for "
                + "this assignment ("
                + assignment.assignment().submissionProfile()
                      .effectiveMaxFileUploadSize()
                + ").  Please choose a smaller file.";
            result.errorMessages.add(msg);
            log.warn(msg + "  User = " + session.user()
                + "\n\t" + assignment);
            return result.generateResponse();
        }
        try
        {
            String msg = result.commitSubmission(context, currentTime);
            if (msg != null)
            {
                log.warn(msg + "  User = " + session.user()
                    + "\n\t" + assignment);
                result.errorMessages.add(msg);
            }
        }
        catch (Exception e)
        {
            new UnexpectedExceptionMessage(e, context, null, null)
                .send();
            result.clearSubmission();
            result.submissionInProcess().clearUpload();
            result.cancelLocalChanges();
            String msg =
                "An unexpected exception occurred while trying to commit "
                + "your submission.  The error has been reported to the "
                + "Web-CAT administrator.  Please try your submission again.";
            result.errorMessages.add(msg);
            log.error(msg + "  User = " + session.user()
                + "\n\t" + assignment, e);
            result.criticalError = true;
        }

        log.debug("handleSubmission() returning");
        return result.generateResponse();
    }


    // ----------------------------------------------------------
    /**
     * Handle a direct action request.  The user's login session will be
     * passed in as well.
     *
     * @param request the request to respond to
     * @param session the user's session
     * @param context the context for the request
     * @return The response page or contents
     */
    public WOActionResults handleLTILaunch(
        WORequest request,
        Session   session,
        WOContext context)
    {
        // LTI request has already been checked for validity
        GraderLTILaunchRequest lti = new GraderLTILaunchRequest(
            context, session.defaultEditingContext());
        NSArray<CourseOffering> courses = lti.courseOfferings();
        NSArray<CourseOffering> userCourses =
            lti.filterCoursesForUser(session.user(), courses);
        log.debug("user courses = " + userCourses);
        if (userCourses.count() == 0)
        {
            if (courses.count() == 1)
            {
                // Auto-enroll user in course!
                CourseOffering co = courses.get(0);
                log.debug("enrolling user " + session.user() + " in " + co);
                session.user().addToEnrolledInRelationship(co);
                session.defaultEditingContext().saveChanges();
                userCourses = courses;
            }
            else if (courses.count() > 1)
            {
                LTIMessagePage page = Application.wcApplication()
                    .pageWithName(LTIMessagePage.class);
                page.ltiRequest = lti;
                page.message = "You have not been enrolled in any of the "
                    + "corresponding course offerings on Web-CAT yet. "
                    + "Your instructor needs to enroll you in the correct "
                    + "course section for you to proceed.";
                return page;
            }
            // FIXME: test response page first
//            if (lti.isInstructor() && false)
//            {
//                courses = lti.suggestedCourseOfferings(session.user());
//                LTICourseChoicePage page = Application.wcApplication()
//                    .pageWithName(LTICourseChoicePage.class);
//                page.ltiRequest = lti;
//                page.courseOfferings = courses;
//                return page;
//            }
            else
            {
                LTIMessagePage page = Application.wcApplication()
                    .pageWithName(LTIMessagePage.class);
                page.ltiRequest = lti;
                page.message = "This course has not been set up on "
                    + "Web-CAT yet. Your instructor needs to complete the "
                    + "set up before you can access this course's activities.";
                return page;
            }
        }
        // If a course navigation launch
        GraderComponent genericGComp =
            Application.wcApplication().pageWithName(
                PickCourseEnrolledPage.class, context);
        if (!lti.hasAssignmentId())
        {
            if (userCourses.count() > 0)
            {
                CourseOffering co = userCourses.get(0);
                genericGComp.coreSelections().setSemester(co.semester());
                genericGComp.coreSelections().setCourseRelationship(
                    co.course());
                if (userCourses.count() == 1)
                {
                    genericGComp.coreSelections()
                        .setCourseOfferingRelationship(co);
                }
            }

            TabDescriptor previousPage = session.tabs.selectedDescendant();
            previousPage.select();
            return genericGComp.pageWithName(session.currentPageName())
                .generateResponse();
        }

        // Find list of matching assignments
        NSArray<AssignmentOffering> assignments = lti.assignmentOfferings();
        // Find matching assignment for this user
        NSArray<AssignmentOffering> courseAssignments =
            lti.filterAssignmentsForCourses(userCourses, assignments);
        log.debug("course assignments = " + courseAssignments);
        if (courseAssignments.count() == 0)
        {
            // If no matching assignments
            // If user is instructor, create assignment
            // find list of course offerings
                // redirect to new assignment page with reasonable defaults
            // if no matching course offerings
                // redirect to new course offering page with reasonable defaults
          if (lti.isInstructor())
          {
//            courses = lti.suggestedCourseOfferings(session.user());
//            LTICourseChoicePage page = Application.wcApplication()
//                .pageWithName(LTICourseChoicePage.class);
//            page.ltiRequest = lti;
//            page.courseOfferings = courses;
//            return page;
              LTIMessagePage page = Application.wcApplication()
                  .pageWithName(LTIMessagePage.class);
              page.ltiRequest = lti;
              page.message = "There isn't an assignment associated with this "
                  + "LTI activity yet. We're working on allowing you to set "
                  + "up your assignment here, but for now, please go back to "
                  + "Canvas and copy the direct URL to the assignment, then "
                  + "go to Web-CAT and edit your assignment's properties, and "
                  + "paste the URL from Canvas into the assignment's URL "
                  + "field, so we know which assignment matches.";
              return page;
          }
          else
          {
              // else if user is not an instructor, error page
              LTIMessagePage page = Application.wcApplication()
                  .pageWithName(LTIMessagePage.class);
              page.ltiRequest = lti;
              page.message = "This assignment has not been set up on "
                  + "Web-CAT yet. Your instructor needs to complete the "
                  + "set up before you can access this assignment.";
              return page;
          }
        }

        if (courseAssignments.count() > 1 && !lti.isInstructor())
        {
            log.error("Multiple assignments found for user " + session.user()
                + " in lti launch: " + assignments);
        }
        AssignmentOffering assignment = courseAssignments.get(0);
        if (assignment.lisOutcomeServiceUrl() == null
            && lti.lisOutcomeServiceUrl() != null)
        {
            assignment.setLisOutcomeServiceUrl(lti.lisOutcomeServiceUrl());
            assignment.editingContext().saveChanges();
        }
        if (lti.lisResultSourcedId() != null)
        {
            LISResultId.ensureExists(session.defaultEditingContext(),
                session.user(), assignment,
                lti.lmsInstance(), lti.lisResultSourcedId());
        }

        genericGComp.coreSelections().setSemester(
            assignment.courseOffering().semester());
        genericGComp.coreSelections().setCourseOfferingRelationship(
            assignment.courseOffering());
        genericGComp.coreSelections().setCourseRelationship(
            assignment.courseOffering().course());
        genericGComp.prefs().setAssignmentOfferingRelationship(assignment);
        genericGComp.changeWorkflow();
        // if any submissions
            // redirect to results page for that assignment
        // else
            // redirect to submit page for that assignment
        if (Submission.submissionsForAssignmentOfferingAndUser(
            session.defaultEditingContext(), assignment, session.user())
            .count() > 0)
        {
            return genericGComp.pageWithName(
                session.tabs.selectById("MostRecent").pageName())
                .generateResponse();
        }
        else
        {
            return genericGComp.pageWithName(
                session.tabs.selectById("UploadSubmission").pageName())
            .generateResponse();
        }
    }


    // ----------------------------------------------------------
    /**
     * Handle a direct action request.  The user's login session will be
     * passed in as well.
     *
     * @param request the request to respond to
     * @param session the user's session
     * @param context the context for the request
     * @return The response page or contents
     */
    public WOActionResults handleReport(
        WORequest request,
        Session   session,
        WOContext context)
    {
        log.debug("handleReport()");
        WOActionResults result = null;
        GraderComponent genericGComp =
            Application.wcApplication().pageWithName(
                PickCourseEnrolledPage.class, context);
        if (genericGComp.wcSession().primeUser() == null
            || genericGComp.prefs().submission() == null)
        {

            result = Application.wcApplication().gotoLoginPage(context);
        }
        else
        {
//            result = Application.application().pageWithName(
//                session.tabs.selectById( "MostRecent" ).pageName(),
//                context ).generateResponse();
            result = genericGComp.pageWithName(
                session.tabs.selectById("MostRecent").pageName())
                .generateResponse();
        }
        log.debug("handleReport() returning");
        return result;
    }


    // ----------------------------------------------------------
    public static StorageStatusTracker storageTracker()
    {
        if (storageTracker == null)
        {
            storageTracker = new StorageStatusTracker(
                Application.configurationProperties()
                .getProperty("grader.submissiondir"));
        }
        return storageTracker;
    }


    // ----------------------------------------------------------
    public static StorageStatusTracker workAreaTracker()
    {
        if (workAreaTracker == null)
        {
            workAreaTracker = new StorageStatusTracker(
                Application.configurationProperties()
                .getProperty("grader.workarea"));
        }
        return workAreaTracker;
    }


    // ----------------------------------------------------------
    @Override
    protected void performPeriodicMaintenance()
    {
        run(new ECAction() {
            // ----------------------------------------------------------
            @Override
            public void action()
            {
                WCFetchSpecification<Submission> needsMigration =
                    new WCFetchSpecification<Submission>(
                        Submission.ENTITY_NAME,
                        Submission.isSubmissionForGrading.isNull(),
                        Submission.submitTime.descs());
                needsMigration.setRefreshesRefetchedObjects(false);
                needsMigration.setFetchLimit(500);

                NSArray<Submission> migrated = Submission
                    .objectsWithFetchSpecification(ec, needsMigration);
                while (migrated.size() > 0)
                {
                    log.info("performPeriodicMaintenance(): migrated "
                        + migrated.size() + " submissions");
                    try
                    {
                        // Sleep for 2 seconds
                        Thread.sleep(2000);
                    }
                    catch (InterruptedException e)
                    {
                        // ignore
                    }
                    migrated = Submission
                        .objectsWithFetchSpecification(ec, needsMigration);
                }
            }
        });
    }


    // ----------------------------------------------------------
    private static class FiveMinuteMaintenance
        implements Runnable
    {
        // ----------------------------------------------------------
        public void run()
        {
            while (true)
            {
                try
                {
                    Thread.sleep(5 * 60 * 1000);
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
                ECAction.run(new ECAction() {
                    @Override
                    public void action()
                    {
                        GraderQueueProcessor.processJobs(
                            EnqueuedJob.objectsMatchingQualifier(ec,
                                EnqueuedJob.paused.isFalse().and(
                                    EnqueuedJob.processor.isNull())));
                    }
                });
            }
        }
    }


    //~ Instance/static variables .............................................

    /**
     * This is a reference to the single instance of this class, representing
     * this subsystem.  It is initialized by the constructor.
     */
    private static Grader instance;
    private static StorageStatusTracker storageTracker;
    private static StorageStatusTracker workAreaTracker;

    static Logger log = Logger.getLogger(Grader.class);
}
