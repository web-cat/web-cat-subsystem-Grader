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

package org.webcat.grader;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.core.messaging.UnexpectedExceptionMessage;
import org.webcat.grader.messaging.AdminReportsForSubmissionMessage;
import org.webcat.grader.messaging.GraderKilledMessage;
import org.webcat.grader.messaging.GraderMarkupParseError;
import org.webcat.grader.messaging.GradingResultsAvailableMessage;
import org.webcat.grader.messaging.SubmissionSuspendedMessage;

//-------------------------------------------------------------------------
/**
 *  The subsystem defining Web-CAT administrative tasks.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author$
 *  @version $Revision$, $Date$
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

        // Install or update any plug-ins that need it

        GradingPlugin.autoUpdateAndInstall();
    }

    // ----------------------------------------------------------
    /**
     * Performs all startup actions for this subsystem.
     */
    public void start()
    {
        // Create the queue and the queueprocessor
        graderQueue          = new GraderQueue();
        graderQueueProcessor = new GraderQueueProcessor( graderQueue );

        // Kick off the processor thread
        graderQueueProcessor.start();

        if ( Application.configurationProperties().booleanForKey(
                "grader.resumeSuspendedJobs" ) )
        {
            // Resume any enqueued jobs (if grader is coming back up
            // after an application restart)
            EOEditingContext ec = Application.newPeerEditingContext();
            try
            {
                ec.lock();

                for (EnqueuedJob job : EnqueuedJob.allObjects(ec))
                {
                    if (!job.paused())
                    {
                        // Only need to trigger the queue processor once,
                        // and it will slurp up all the jobs that are ready.
                        graderQueue.enqueue(null);
                        break;
                    }
                }
            }
            finally
            {
                ec.unlock();
                Application.releasePeerEditingContext( ec );
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Initialize the subsystem-specific session data in a newly created
     * session object.  This method is called once by the core for
     * each newly created session object.
     *
     * @param s The new session object
     */
    public void initializeSessionData( Session s )
    {
        super.initializeSessionData(s);
        try
        {
            EOUtilities.objectsForEntityNamed( s.sessionContext(),
                                               Assignment.ENTITY_NAME );
        }
        catch ( Exception e )
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
     * Access the grader job queue.
     *
     * @return the grader job queue associated with this subsystem
     */
    public GraderQueue graderQueue()
    {
        return graderQueue;
    }


    // ----------------------------------------------------------
    /**
     * Find out how many grading jobs have been processed so far.
     *
     * @return the number of jobs process so far
     */
    public int processedJobCount()
    {
        return graderQueueProcessor.processedJobCount();
    }


    // ----------------------------------------------------------
    /**
     * Find out the processing delay for the most recently completed job.
     *
     * @return the time in milliseconds
     */
    public long mostRecentJobWait()
    {
        return graderQueueProcessor.mostRecentJobWait();
    }


    // ----------------------------------------------------------
    /**
     * Find out the processing delay for the most recently completed job.
     *
     * @return the time in milliseconds
     */
    public long estimatedJobTime()
    {
        return graderQueueProcessor.estimatedJobTime();
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
            WOContext context )
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
        log.debug( "path = " + request.requestHandlerPath() );
        if ( "cmsRequest".equals( request.requestHandlerPath() ) )
        {
            return handleCmsRequest( request, session, context );
        }
        if ( session == null )
        {
            log.error( "handleDirectAction(): null session" );
            log.error( Application.extraInfoForContext( context ) );
        }
        else if ( !context.hasSession() )
        {
            log.error( "handleDirectAction(): no session on context!" );
            log.error( Application.extraInfoForContext( context ) );
        }
        else if ( session != context.session() )
        {
            log.error( "handleDirectAction(): session mismatch with context!" );
            log.error( "session = " + session );
            log.error( "context session = " + context.session() );
            log.error( Application.extraInfoForContext( context ) );
        }
        if ( "submit".equals( request.requestHandlerPath() ) )
        {
            results = handleSubmission( request, session, context );
        }
        else
        {
            results = handleReport( request, session, context );
        }
//      log.debug( "handleDirectAction() returning" );
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
            WOContext context )
    {
        CmsResponse result = (CmsResponse)Application.application()
            .pageWithName( CmsResponse.class.getName(), context );
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
            WOContext context )
    {
        log.debug( "handleSubmission()" );
        String scheme   = request.stringFormValueForKey( "a" );
        log.debug( "scheme = " + scheme );
        String crn      = request.stringFormValueForKey( "crn" );
        log.debug( "crn = " + crn );
        Integer courseNo = null;
        try
        {
            Number num = request.numericFormValueForKey( "course",
                new NSNumberFormatter("0"));
            courseNo = (num instanceof Integer)
                ? (Integer)num
                : new Integer(num.intValue());
        }
        catch (Exception e)
        {
            // Ignore it, and treat it as an undefined course number
        }
        log.debug( "courseNo = " + courseNo );
        String partnerList = request.stringFormValueForKey( "partners" );
        log.debug( "partners = " + partnerList );
        NSData file     = (NSData)request.formValueForKey( "file1" );
        String fileName = request.stringFormValueForKey( "file1.filename" );
        log.debug( "fileName = " + fileName );

        SubmitResponse result = (SubmitResponse)Application.application()
           .pageWithName( SubmitResponse.class.getName(), context );
        EOEditingContext ec = result.localContext();
        result.sessionID = session.sessionID();
        log.debug( "handleSubmission(): sessionID = " + result.sessionID );
        NSTimestamp currentTime   = new NSTimestamp();
//        NSMutableArray qualifiers = new NSMutableArray();
        log.debug( "user = " + session.user()
                   + "(prime = " + session.primeUser() + ")" );
//        qualifiers.addObject( new EOKeyValueQualifier(
//                              AssignmentOffering.COURSE_OFFERING_STUDENTS_KEY,
//                              EOQualifier.QualifierOperatorEqual,
//                              session.user()
//                             ) );
//        log.debug( "scheme = " + scheme );
//        qualifiers.addObject( new EOKeyValueQualifier(
//                                      AssignmentOffering.ASSIGNMENT_NAME_KEY,
//                                      EOQualifier.QualifierOperatorEqual,
//                                      scheme
//                                  ) );
//        qualifiers.addObject( new EOKeyValueQualifier(
//                                      AssignmentOffering.PUBLISH_KEY,
//                                      EOQualifier.QualifierOperatorGreaterThan,
//                                      ERXConstant.integerForInt( 0 )
//                                    ) );
        NSArray<AssignmentOffering> assignments = null;
        try
        {
            if ( crn != null )
            {
                assignments = AssignmentOffering.objectsMatchingQualifier(ec,
                    AssignmentOffering.courseOffering
                        .dot(CourseOffering.students).eq(session.user()).and(
                    AssignmentOffering.assignment.dot(Assignment.name)
                        .eq(scheme).and(
                    AssignmentOffering.publish.eq(1).and(
                    AssignmentOffering.courseOffering.dot(CourseOffering.crn)
                        .eq(crn)))));
            }
            else if ( courseNo != null )
            {
                assignments = AssignmentOffering.objectsMatchingQualifier(ec,
                    AssignmentOffering.courseOffering
                    .dot(CourseOffering.students).eq(session.user()).and(
                AssignmentOffering.assignment.dot(Assignment.name)
                    .eq(scheme).and(
                AssignmentOffering.publish.eq(1).and(
                AssignmentOffering.courseOffering.dot(CourseOffering.course)
                    .dot(Course.number).eq(courseNo)))));
            }
            else
            {
                assignments = AssignmentOffering.objectsMatchingQualifier(ec,
                    AssignmentOffering.courseOffering
                    .dot(CourseOffering.students).eq(session.user()).and(
                AssignmentOffering.assignment.dot(Assignment.name)
                    .eq(scheme).and(
                AssignmentOffering.publish.eq(1))));
            }
//            ec.objectsWithFetchSpecification(
//                new EOFetchSpecification(
//                        AssignmentOffering.ENTITY_NAME,
//                        new EOAndQualifier( qualifiers ),
//                        null ) );
        }
        catch ( Exception e )
        {
            if ( crn != null )
            {
                assignments = AssignmentOffering.objectsMatchingQualifier(ec,
                    AssignmentOffering.courseOffering
                        .dot(CourseOffering.students).eq(session.user()).and(
                    AssignmentOffering.assignment.dot(Assignment.name)
                        .eq(scheme).and(
                    AssignmentOffering.publish.eq(1).and(
                    AssignmentOffering.courseOffering.dot(CourseOffering.crn)
                        .eq(crn)))));
            }
            else if ( courseNo != null )
            {
                assignments = AssignmentOffering.objectsMatchingQualifier(ec,
                    AssignmentOffering.courseOffering
                    .dot(CourseOffering.students).eq(session.user()).and(
                AssignmentOffering.assignment.dot(Assignment.name)
                    .eq(scheme).and(
                AssignmentOffering.publish.eq(1).and(
                AssignmentOffering.courseOffering.dot(CourseOffering.course)
                    .dot(Course.number).eq(courseNo)))));
            }
            else
            {
                assignments = AssignmentOffering.objectsMatchingQualifier(ec,
                    AssignmentOffering.courseOffering
                    .dot(CourseOffering.students).eq(session.user()).and(
                AssignmentOffering.assignment.dot(Assignment.name)
                    .eq(scheme).and(
                AssignmentOffering.publish.eq(1))));
            }
        }
        AssignmentOffering assignment = null;
        if ( assignments != null && assignments.count() > 0 )
        {
            for (AssignmentOffering thisAssignment : assignments)
            {
                log.debug( "assignment = "
                           + thisAssignment.assignment().name() );
                CourseOffering co = thisAssignment.courseOffering();
                if ( co.isInstructor( session.user() )
                     || co.isGrader( session.user() )
                     || ( currentTime.after( thisAssignment.availableFrom() )
                     && currentTime.before( thisAssignment.lateDeadline() ) ) )
                {
                    log.debug( "found matching assignment that is open." );
                    if ( assignment != null )
                    {
                        result.message =
                            "Warning: multiple matching assignments found.";
                    }
                    assignment = thisAssignment;
                }
            }
        }
        if ( assignment == null )
        {
            // Look for an assignment where this user is course staff
            NSMutableArray<EOQualifier> qualifiers =
                new NSMutableArray<EOQualifier>();
//            qualifiers.addObject( new EOKeyValueQualifier(
//                                  AssignmentOffering.COURSE_OFFERING_INSTRUCTORS_KEY,
//                                  EOQualifier.QualifierOperatorEqual,
//                                  session.user()
//                                 ) );
//            qualifiers.addObject( new EOKeyValueQualifier(
//                            AssignmentOffering.COURSE_OFFERING_TAS_KEY,
//                            EOQualifier.QualifierOperatorEqual,
//                            session.user()
//                           ) );
//            qualifiers = new NSMutableArray( new EOOrQualifier( qualifiers ) );
            qualifiers.addObject(
                AssignmentOffering.assignment.dot(Assignment.name).eq(scheme));
            if ( crn != null )
            {
                qualifiers.addObject(
                    AssignmentOffering.courseOffering
                        .dot(CourseOffering.crn).eq(crn));
            }
            else if ( courseNo != null )
            {
                qualifiers.addObject(
                    AssignmentOffering.courseOffering
                        .dot(CourseOffering.course)
                        .dot(Course.number).eq(courseNo));
            }
            if ( log.isDebugEnabled() )
            {
                log.debug( "qualifiers = " + qualifiers );
            }
            try
            {
                assignments = AssignmentOffering.objectsMatchingQualifier(
                    ec, new EOAndQualifier(qualifiers));
                // Remove any that the user doesn't have staff access to.
                // Can't put this in an EOOrQualifier, since the generated
                // SQL will be broken.
                if (assignments.count() > 0 )
                {
                    NSMutableArray<AssignmentOffering> filtered =
                        assignments.mutableClone();
                    int i = 0;
                    while (i < filtered.count())
                    {
                        AssignmentOffering ao = filtered.objectAtIndex(i);
                        CourseOffering co = ao.courseOffering();
                        if ( session.user().hasAdminPrivileges()
                             || co.isInstructor(session.user())
                             || co.isGrader(session.user()) )
                        {
                            i++;
                        }
                        else
                        {
                            filtered.remove(i);
                        }
                    }
                    assignments = filtered;
                }
                if ( assignments.count() > 0 )
                {
                    log.debug( "found matching assignment for course staff." );
                    if ( assignments.count() > 1 )
                    {
                        result.message =
                            "Warning: multiple matching assignments found.";
                    }
                    assignment = assignments.objectAtIndex( 0 );
                }
            }
            catch ( Exception e )
            {
                // Swallow it
            }
        }

        if ( assignment == null )
        {
            log.debug( "no assignments are open." );
            result.message = "The requested assignment is not accepting "
                + "submissions at this time or it could not be found.  "
                + "The deadline may have passed.";
            result.assignmentClosed = true;
            return result.generateResponse();
        }

        result.coreSelections().setCourseOfferingRelationship(
            assignment.courseOffering() );
        result.prefs().setAssignmentOfferingRelationship( assignment );
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
        if ( maxSubmissions != null
             && currentSubNo > maxSubmissions.intValue() )
        {
            result.message = "You have exceeded the allowable number "
                + "of submissions for this assignment.";
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

        result.setPartnersNotFound(partnersNotFound);
        result.startSubmission( currentSubNo, result.user() );
        result.submissionInProcess().setPartners( partners );
        result.submissionInProcess().setUploadedFile( file );
        result.submissionInProcess().setUploadedFileName( fileName );

        int len = 0;
        try
        {
            len = file.length();
        }
        catch (Exception e)
        {
            // Ignore it: length() could produce an NPE on a bad POST request
        }
        if ( len == 0 )
        {
            result.clearSubmission();
            result.submissionInProcess().clearUpload();
            result.message =
                "Your file submission is empty.  "
                + "Please choose an appropriate file.";
            return result.generateResponse();
        }
        else if ( len > assignment.assignment().submissionProfile()
                        .effectiveMaxFileUploadSize() )
        {
            result.clearSubmission();
            result.submissionInProcess().clearUpload();
            result.message =
                "Your file exceeds the file size limit for this "
                + "assignment ("
                + assignment.assignment().submissionProfile()
                      .effectiveMaxFileUploadSize()
                + ").  Please choose a smaller file.";
            return result.generateResponse();
        }
        try
        {
            result.message =
                result.commitSubmission( context, currentTime );
        }
        catch ( Exception e )
        {
            new UnexpectedExceptionMessage(e, context, null, null).send();
            result.clearSubmission();
            result.submissionInProcess().clearUpload();
            result.cancelLocalChanges();
            result.message =
                "An unexpected exception occurred while trying to commit "
                + "your submission.  The error has been reported to the "
                + "Web-CAT administrator.  Please try your submission again.";
            result.criticalError = true;
        }

        log.debug( "handleSubmission() returning" );
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
    public WOActionResults handleReport(
            WORequest request,
            Session   session,
            WOContext context )
    {
        log.debug( "handleReport()" );
        WOActionResults result = null;
        GraderComponent genericGComp =
            (GraderComponent)Application.application().pageWithName(
                PickCourseEnrolledPage.class.getName(), context);
        if ( genericGComp.wcSession().primeUser() == null
             || genericGComp.prefs().submission() == null )
        {

            result = ( (Application)Application.application() ).gotoLoginPage(
                            context );
        }
        else
        {
//            result = Application.application().pageWithName(
//                session.tabs.selectById( "MostRecent" ).pageName(),
//                context ).generateResponse();
            result = genericGComp.pageWithName(
                session.tabs.selectById( "MostRecent" ).pageName())
                .generateResponse();
        }
        log.debug( "handleReport() returning" );
        return result;
    }


    //~ Instance/static variables .............................................

    /**
     * This is a reference to the single instance of this class, representing
     * this subsystem.  It is initialized by the constructor.
     */
    private static Grader instance;

    /** this is the main single grader queue */
    private static GraderQueue graderQueue;

    /** this is the queue processor for processing grader jobs */
    private static GraderQueueProcessor graderQueueProcessor;

    static Logger log = Logger.getLogger( Grader.class );
}
