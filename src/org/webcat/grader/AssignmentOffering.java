/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2010 Virginia Tech
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

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.eof.ERXConstant;
import er.extensions.eof.ERXKey;
import er.extensions.eof.ERXQ;
import er.extensions.eof.ERXSortOrdering;
import er.extensions.eof.qualifiers.ERXInQualifier;
import er.extensions.foundation.ERXArrayUtilities;
import er.extensions.foundation.ERXValueUtilities;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.core.messaging.UnexpectedExceptionMessage;
import org.webcat.grader.graphs.*;

// -------------------------------------------------------------------------
/**
 * Represents the binding between an assignment and a course offering
 * (i.e., giving a specific assignment in a given section of a course).
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class AssignmentOffering
    extends _AssignmentOffering
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new AssignmentOffering object.
     */
    public AssignmentOffering()
    {
        super();
    }


    // ----------------------------------------------------------
    /**
     * A static factory method for creating a new
     * AssignmentOffering object given required
     * attributes and relationships.
     * @param editingContext The context in which the new object will be
     * inserted
     * @param forAssignment The assignment to be offered
     * @param forCourseOffering The course offering for this assignment offering
     * @return The newly created object
     */
    public static AssignmentOffering create(
        EOEditingContext editingContext,
        Assignment forAssignment,
        CourseOffering forCourseOffering)
    {
        AssignmentOffering result = create(editingContext,
            false,
            false,
            false);
        result.setAssignmentRelationship(forAssignment);
        result.setCourseOfferingRelationship(forCourseOffering);
        return result;
    }


    //~ Constants (for key names) .............................................

    // Derived Attributes ---
    public static final String AVAILABLE_FROM_KEY = "availableFrom";
    public static final String LATE_DEADLINE_KEY  = "lateDeadline";
    public static final String ASSIGNMENT_NAME_KEY  =
        ASSIGNMENT_KEY + "." + Assignment.NAME_KEY;
    public static final String COURSE_OFFERING_STUDENTS_KEY  =
        COURSE_OFFERING_KEY + "." + CourseOffering.STUDENTS_KEY;
    public static final String COURSE_OFFERING_INSTRUCTORS_KEY  =
        COURSE_OFFERING_KEY + "." + CourseOffering.INSTRUCTORS_KEY;
    public static final String COURSE_OFFERING_GRADERS_KEY  =
        COURSE_OFFERING_KEY + "." + CourseOffering.GRADERS_KEY;
    public static final String COURSE_OFFERING_SEMESTER_KEY =
        COURSE_OFFERING_KEY + "."
        + CourseOffering.SEMESTER_KEY;

    public static final String ID_FORM_KEY = "aoid";

    public static final ERXKey<String> titleString =
        new ERXKey<String>("titleString");
    public static final ERXKey<NSTimestamp> availableFrom =
        new ERXKey<NSTimestamp>(AVAILABLE_FROM_KEY);
    public static final ERXKey<NSTimestamp> lateDeadline =
        new ERXKey<NSTimestamp>(LATE_DEADLINE_KEY);


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Get a short (no longer than 60 characters) description of this
     * assignment offering.
     * @return the description
     */
    public String userPresentableDescription()
    {
        String result = "";
        if (courseOffering() != null)
        {
            result += courseOffering().compactName() + ": ";
        }
        if (assignment() != null)
        {
            result += assignment().name();
        }
        if (result.equals(""))
        {
            result = super.userPresentableDescription();
        }
        return result;
    }


    // ----------------------------------------------------------
    public String permalink()
    {
        if ( cachedPermalink == null )
        {
            cachedPermalink = Application.configurationProperties()
                .getProperty( "base.url" )
                + "?page=UploadSubmission&"
                + ID_FORM_KEY + "=" + id();
        }
        return cachedPermalink;
    }


    // ----------------------------------------------------------
    /**
     * Determine the latest time when assignments are accepted.
     * @return the final deadline as a timestamp
     */
    public NSTimestamp lateDeadline()
    {
        NSTimestamp myDueDate = dueDate();
        if ( myDueDate != null )
        {
            Assignment myAssignment = assignment();
            if ( myAssignment != null )
            {
                SubmissionProfile submissionProfile =
                    myAssignment.submissionProfile();
                if ( submissionProfile != null )
                {
                    myDueDate = new NSTimestamp(
                        submissionProfile.deadTimeDelta(),
                        myDueDate
                    );
                }
            }
        }
        log.debug( "lateDeadline() = " + myDueDate );
        return myDueDate;
    }


    // ----------------------------------------------------------
    /**
     * Determine the latest time when assignments are accepted.
     * @param user The user to check for
     * @return The final deadline as a timestamp
     */
    public boolean userCanSubmit( User user )
    {
        boolean result = false;
        if ( courseOffering().instructors().containsObject( user )
             || courseOffering().graders().containsObject( user ) )
        {
            result = true;
        }
        else if ( publish() )
        {
            NSTimestamp now = new NSTimestamp();
            result = availableFrom().before( now )
                && lateDeadline().after( now );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Determine the time when this assignment begins accepting
     * submissions.
     * @return the start time for the acceptance window.
     */
    public NSTimestamp availableFrom()
    {
        NSTimestamp myDueDate = dueDate();
        NSTimestamp openingDate = null;
        if ( myDueDate != null )
        {
            Assignment myAssignment = assignment();
            if ( myAssignment != null )
            {
                SubmissionProfile submissionProfile =
                    myAssignment.submissionProfile();
                if (  submissionProfile != null
                   && submissionProfile.availableTimeDeltaRaw() != null )
                {
                    openingDate = new NSTimestamp(
                        - submissionProfile.availableTimeDelta(),
                        myDueDate
                    );
                }
            }
        }
        log.debug( "availableFrom() = " + openingDate );
        return ( openingDate == null )
            ? new NSTimestamp( 0L )
            : openingDate;
    }


    // ----------------------------------------------------------
    public String titleString()
    {
        CourseOffering course = courseOffering();
        return (course == null)
            ? titleString(null)
            : titleString(course.semester());
    }


    // ----------------------------------------------------------
    public String titleString(Semester semester)
    {
        CourseOffering course = courseOffering();
        Assignment myAssignment = assignment();
        String result = "";
        if ( course != null )
        {
            result += course.compactName();
            if (course.semester() != semester)
            {
                result += "[" + course.semester() + "]";
            }
            result += " ";
        }
        if ( myAssignment != null )
        {
            result += myAssignment.titleString();
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>graphSummary</code> value.
     * @return the value of the attribute
     */
    public AssignmentSummary graphSummary()
    {
        NSData dbValue =
            (NSData)storedValueForKey( "graphSummary" );
        SubmissionProfile profile = ( assignment() == null )
            ? null : assignment().submissionProfile();
        double maxScore = ( profile == null ) ? 100.0
                        : profile.availablePoints() - profile.taPoints();
        AssignmentSummary summary = super.graphSummary();
        if ( profile != null
             && ( dbValue == null ||
                  Math.abs( summary.maxScore() - maxScore ) > 0.01  )  )
        {
            // have to initialize the summary by fetching all the most
            // recent assignments
            summary.setMaxScore( maxScore );
            NSArray<SubmissionResult> subs =
                SubmissionResult.mostRecentResultsForAssignment(
                editingContext(), this );
            for (SubmissionResult sr : subs)
            {
                summary.addSubmission( sr.automatedScore() );
            }
        }
        return summary;
    }


    // ----------------------------------------------------------
    /**
     * Return a list of all the submissions for this assignment that are
     * still in the grading queue but that are marked as suspended, either
     * because of errors or because the instructor has halted grading for
     * this assignment offering.
     * @return an NSArray of EnqueuedJob objects representing suspended
     *         submissions
     */
    @SuppressWarnings("unchecked")
    public NSArray<EnqueuedJob> getSuspendedSubs()
    {
        return EOUtilities.objectsMatchingValues(
            editingContext(),
            EnqueuedJob.ENTITY_NAME,
            new NSDictionary(
                new Object[] {  new Integer( 1 ),
                                this
                    },
                new Object[] { EnqueuedJob.PAUSED_KEY,
                               EnqueuedJob.ASSIGNMENT_OFFERING_KEY }
                    )
            );
    }


    // ----------------------------------------------------------
    /**
     * Return the highest-numbered submission for this assignment offering
     * made by the given user.
     * @param user the user to look up
     * @return the most recent Submission object for the given user, or
     *         null if there is none
     */
    public Submission mostRecentSubFor( User user )
    {
        Submission mostRecent = null;
        NSArray<Submission> subs = Submission.objectsMatchingQualifier(
            editingContext(),
            Submission.assignmentOffering.is(this).and(Submission.user.is(user))
            );
        if ( subs != null && subs.count() > 0 )
        {
            mostRecent = subs.objectAtIndex( 0 );
            for ( int j = 1; j < subs.count(); j++ )
            {
                Submission s = subs.objectAtIndex( j );
                if ( s.submitNumber() > mostRecent.submitNumber() )
                {
                    mostRecent = s;
                }
            }
        }
        return mostRecent;
    }


    // ----------------------------------------------------------
    /**
     * Return a list consisting of the most recent submission for all
     * students who have submitted to this assignment offering.
     * @return an NSArray of Submission objects
     */
    public NSArray<Submission> mostRecentSubsForAll()
    {
        NSMutableArray<Submission> recentSubs =
            new NSMutableArray<Submission>();
        NSMutableArray<User> students =
            courseOffering().students().mutableClone();
        NSArray<User> staff = courseOffering().instructors();
        students.removeObjectsInArray( staff );
        students.addObjectsFromArray( staff );
        staff = courseOffering().graders();
        students.removeObjectsInArray( staff );
        students.addObjectsFromArray( staff );
        for (User user : students)
        {
            Submission s = mostRecentSubFor(user);
            if ( s != null )
            {
                recentSubs.addObject( s );
            }
        }
        return recentSubs;
    }


    // ----------------------------------------------------------
    /**
     * Return the most recent processed submission result for this assignment
     * offering made by the given user.
     * @param user the user to look up
     * @return the most recent SubmissionResult object for the given user, or
     *         null if there is none
     */
    public SubmissionResult mostRecentSubmissionResultFor(User user)
    {
        SubmissionResult newest = null;
        NSArray<SubmissionResult> subs =
            SubmissionResult.resultsForAssignmentAndUser(
            editingContext(), this, user);
        if (subs.count() > 0)
        {
            newest = subs.objectAtIndex(subs.count() - 1);
        }
        return newest;
    }


    // ----------------------------------------------------------
    /**
     * Delete all the result information for this submission, including
     * all partner links, and requeue it for grading.  This method uses the
     * submission's current editing context to make changes, but does
     * <b>not</b> commit those changes to the database (the caller must
     * use <code>saveChanges()</code>).
     * @param ec the editing context to use for updating the grading queue
     */
    public void regradeMostRecentSubsForAll(EOEditingContext ec)
    {
        for (Submission sub : mostRecentSubsForAll())
        {
            // A fake partnered submission will have a non-null
            // primarySubmission attribute. We only want to regrade the actual
            // submissions, so only enqueue the ones with null
            // primarySubmission.

            if (sub.primarySubmission() == null)
            {
                sub.requeueForGrading(ec);
            }
        }
        ec.saveChanges();
        Grader.getInstance().graderQueue().enqueue(null);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the subdirectory where all submissions for this
     * assignment offering are stored.  This subdirectory is relative to
     * the base submission directory for some authentication domain, such
     * as the value returned by
     * {@link AuthenticationDomain#submissionBaseDirBuffer()}.
     * @param dir the string buffer to add the requested subdirectory to
     *        (a / is added to this buffer, followed by the subdirectory name
     *        generated here)
     */
    public void addSubdirTo( StringBuffer dir )
    {
        courseOffering().addSubdirTo( dir );
        dir.append( '/' );
        dir.append( assignment().subdirName() );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>moodleId</code> value.
     * @return the value of the attribute
     */
    public Long moodleId()
    {
        Long result = super.moodleId();
        if ( result == null && assignment() != null )
        {
            result = assignment().moodleId();
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Check whether this assignment is past the due date.
     *
     * @return true if any submissions to this assignment will be counted
     *         as late
     */
    public boolean isLate()
    {
        return ( dueDate() == null )
            ? false
            : dueDate().before( new NSTimestamp() );
    }


    // ----------------------------------------------------------
    @Override
    public void awakeFromFetch(EOEditingContext ec)
    {
        super.awakeFromFetch(ec);

        // Only try to migrate if the EC isn't a migrating context. If it is,
        // we're already trying to migrate and this "awake" is coming from the
        // child migration context.

        if (!(ec instanceof org.webcat.core.MigratingEditingContext))
        {
            migrateAttributeValuesIfNeeded();
        }
    }


    // ----------------------------------------------------------
    /**
     * Called by {@link #awake} to migrate attribute values if needed when the
     * object is retrieved.
     */
    public void migrateAttributeValuesIfNeeded()
    {
        log.debug("migrateAttributeValuesIfNeeded()");

        if ( lastModified() == null )
        {
            if (isNewObject())
            {
                setLastModified(new NSTimestamp());
            }
            else
            {
                MigratingEditingContext mec =
                    Application.newMigratingEditingContext();
                try
                {
                    mec.lock();
                    AssignmentOffering migratingObject = localInstance(mec);

                    migratingObject.setLastModified(new NSTimestamp());

                    mec.saveChanges();
                }
                finally
                {
                    mec.unlock();
                    org.webcat.core.Application
                        .releaseMigratingEditingContext(mec);
                }
            }
        }
    }


    // ----------------------------------------------------------
    @Override
    public void mightDelete()
    {
        log.debug("mightDelete()");
        if (isNewObject()) return;
        if (hasStudentSubmissions())
        {
            log.debug("mightDelete(): offering has non-staff submissions");
            throw new ValidationException("You may not delete an assignment "
                + "offering that has already received student submissions.");
        }
        if (assignment() != null && !assignment()
            .conflictingSubdirNameExists(assignment().subdirName()))
        {
            StringBuffer buf = new StringBuffer("/");
            addSubdirTo(buf);
            subdirToDelete = buf.toString();
        }
        super.mightDelete();
    }


    // ----------------------------------------------------------
    @Override
    public boolean canDelete()
    {
        boolean result = (courseOffering() == null
            || editingContext() == null
            || !hasStudentSubmissions());
        log.debug("canDelete() = " + result);
        return result;
    }


    // ----------------------------------------------------------
    @Override
    public void didDelete( EOEditingContext context )
    {
        log.debug("didDelete()");
        super.didDelete( context );
        // should check to see if this is a child ec
        EOObjectStore parent = context.parentObjectStore();
        if (parent == null || !(parent instanceof EOEditingContext))
        {
            log.debug("didDelete() on " + this);
            if (subdirToDelete != null)
            {
                log.debug("deleting subdir suffix " + subdirToDelete);
                for (AuthenticationDomain domain :
                    AuthenticationDomain.authDomains())
                {
                    StringBuffer dir = domain.submissionBaseDirBuffer();
                    dir.append(subdirToDelete);
                    File assignmentDir = new File(dir.toString());
                    if (assignmentDir.exists())
                    {
                        log.debug("deleting " + assignmentDir);
                        org.webcat.core.FileUtilities.deleteDirectory(
                            assignmentDir);
                    }
                }
            }
            else
            {
                log.debug("not deleting any subdirs");
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Check to see if any students have submitted to this assignment
     * offering.  This check explicitly excludes any users who have
     * instructor-level or TA-level access to the associated course
     * offering.
     * @return true if any student submissions exist
     */
    public boolean hasStudentSubmissions()
    {
        if (isNewObject()) return false;
        NSMutableArray<EOQualifier> qualifiers =
            new NSMutableArray<EOQualifier>();
        // Must be a submission to this assignment
        qualifiers.add(new EOKeyValueQualifier(
            Submission.ASSIGNMENT_OFFERING_KEY,
            EOQualifier.QualifierOperatorEqual,
            this));
        if (this.courseOffering() != null)
        {
            NSArray<User> people = this.courseOffering().instructors();
            // Not an instructor
            if (people.count() > 0)
            {
                for (int i = 0; i < people.count(); i++)
                {
                    // Add to query
                    qualifiers.add(new EONotQualifier(
                        new EOKeyValueQualifier(
                            Submission.USER_KEY,
                            EOQualifier.QualifierOperatorEqual,
                            people.objectAtIndex(i)
                        )));
                }
            }
            people = this.courseOffering().graders();
            // Not a TA
            if (this.courseOffering().graders().count() > 0)
            {
                for (int i = 0; i < people.count(); i++)
                {
                    // Add to query
                    qualifiers.add(new EONotQualifier(
                        new EOKeyValueQualifier(
                            Submission.USER_KEY,
                            EOQualifier.QualifierOperatorEqual,
                            people.objectAtIndex(i)
                        )));
                }
            }
        }
        EOFetchSpecification spec = new EOFetchSpecification(
            Submission.ENTITY_NAME, new EOAndQualifier(qualifiers), null);
        // Only need to return 1, since we're just trying to find out if
        // there are any at all
        spec.setFetchLimit(1);
        NSArray<Submission> result =
            Submission.objectsWithFetchSpecification(editingContext(),  spec);
        if (log.isDebugEnabled())
        {
            log.debug("hasStudentSubmissions(): fetch = " + result);
        }
        return result.count() > 0;
    }


    //~ Public Static Methods .................................................

    // ----------------------------------------------------------
    /**
     * Retrieve a set of assignment offerings for a given course with names
     * similar to some string.  Here, "similar" means that the name of
     * the assignment associated with an offering is similar to the target
     * name, as defined by {@link Assignment#namesAreSimilar(String,String)}.
     *
     * @param context The editing context to use
     * @param targetName The name that results should be similar to
     * @param forCourseOffering The course offering to search for
     * @param limit the maximum number of assignment offerings to return
     * (or zero, if all should be returned)
     * @return an NSArray of the entities retrieved, sorted in descending order
     * by due date (latest due date first)
     */
    public static NSMutableArray<AssignmentOffering> offeringsWithSimilarNames(
            EOEditingContext context,
            String targetName,
            org.webcat.core.CourseOffering forCourseOffering,
            int limit
        )
    {
        NSMutableArray<AssignmentOffering> others =
            new NSMutableArray<AssignmentOffering>();
        NSArray<AssignmentOffering> sameSection = AssignmentOffering
            .offeringsForCourseOffering(context, forCourseOffering);
        for (int i = 0; i < sameSection.count()
                        && ( limit < 1 || others.count() < limit ); i++)
        {
            AssignmentOffering ao = sameSection.objectAtIndex(i);
            String name = ao.assignment().name();
            if (!targetName.equals( name )
                && Assignment.namesAreSimilar(name, targetName))
            {
                log.debug("matching assignment offering (target = '"
                    + targetName + "'): "
                    + ao.titleString() + ", " + ao.dueDate());
                others.addObject(ao);
            }
        }
        return others;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve a set of assignment offerings for a given course with names
     * similar to some string.  Here, "similar" means that the name of
     * the assignment associated with an offering is similar to the target
     * name, as defined by {@link Assignment#namesAreSimilar(String,String)}.
     *
     * @param context The editing context to use
     * @param targetName The name that results should be similar to
     * @param course The course to search for
     * @param limit the maximum number of assignment offerings to return
     * (or zero, if all should be returned)
     * @return an NSArray of the entities retrieved, sorted in descending order
     * by due date (latest due date first)
     */
    public static NSMutableArray<AssignmentOffering> offeringsWithSimilarNames(
            EOEditingContext context,
            String targetName,
            org.webcat.core.Course course,
            int limit
        )
    {
        NSMutableArray<AssignmentOffering> others =
            new NSMutableArray<AssignmentOffering>();
        NSArray<AssignmentOffering> sameSection = AssignmentOffering
            .offeringsForCourse(context, course);
        for (int i = 0; i < sameSection.count()
                        && ( limit < 1 || others.count() < limit ); i++)
        {
            AssignmentOffering ao = sameSection.objectAtIndex(i);
            String name = ao.assignment().name();
            if (!targetName.equals( name )
                && Assignment.namesAreSimilar(name, targetName))
            {
                log.debug("matching assignment offering (target = '"
                    + targetName + "'): "
                    + ao.titleString() + ", " + ao.dueDate());
                others.addObject(ao);
            }
        }
        return others;
    }


    // ----------------------------------------------------------
    private static EOQualifier and(EOQualifier left, EOQualifier right)
    {
        if (left == null)
        {
            return right;
        }
        else if (right == null)
        {
            return left;
        }
        else
        {
            return ERXQ.and(left, right);
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieves a sorted set of assignment offerings that are available for
     * submission via a given submitter engine.  The set of assignments are
     * retrieved is determined by specific keys in the formValues dictionary
     * that is passed in.  If the dictionary is empty, then all published
     * assignment offerings that are both currently accepting submissions
     * and that are also associated with submission profiles that designate
     * the given submitter engine will be returned.  Specific keys in the
     * formValues dictionary can be used to narrow the search:
     *
     * institution     the institution property name: only assignments
     *                 associated with courses in departments from this
     *                 institution will be retrieved.
     * course          the course number: only assignments associated with
     *                 this course number will be retrieved.
     * crn             crn number/id: only assignmnets associated with this
     *                 course offering (course request number) will be
     *                 retrieved.
     * staff           a boolean value that, if true, includes non-published
     *                 assignments as well as assignments that are past their
     *                 due dates.
     *
     * @param context    the editing context to use for fetching.
     * @param formValues a dictionary of values encoding choices about which
     *                   assignment offerings to retrieve, and in what order.
     * @param currentTime the time to use when deciding whether or not due
     *                    dates have past.
     * @param submitterEngine the submitter engine to look for.  Possible
     *                        values correspond to the acceptable values for
     *                        a {@link SubmissionProfile}'s submissionMethod
     *                        attribute, which are defined by the values in
     *                        the {@link SubmissionProfile#submitters} array.
     * @param groupByCRN request that each separate offering of a course will
     *                   have all of its assignment offerings listed separately
     *                   and grouped together.  Otherwise (the default), only
     *                   the assignment offering with the latest due date will
     *                   be shown when one assignment is shared among many
     *                   course offerings.
     * @param showAll    request that all assignments (including those that
     *                   are already closed or that have not yet become
     *                   available) be included.  If true, this also limits
     *                   the request to include only assignments available
     *                   in the current semester.
     * @param preserveDateDifferences if true, offerings of the same assignment
     *                   with different due dates should be preserved (only
     *                   relevant if groupByCRN is false).
     * @return an array of the given assignment offerings
     */
    public static NSArray<AssignmentOffering> objectsForSubmitterEngine(
        EOEditingContext        context,
        NSDictionary<String, ?> formValues,
        NSTimestamp             currentTime,
        int                     submitterEngine,
        boolean                 groupByCRN,
        boolean                 showAll,
        boolean                 preserveDateDifferences
        )
    {
//        EOFetchSpecification spec =
//            EOFetchSpecification.fetchSpecificationNamed(
//                OFFERINGS_FOR_SUBMITTER_ENGINE_BASE_FSPEC,
//                ENTITY_NAME );

        // Set up the qualifier
        EOQualifier qualifier = null;
        if ( submitterEngine > 0 )
        {
            qualifier = and(qualifier,
                AssignmentOffering.assignment.dot(Assignment.submissionProfile)
                .dot(SubmissionProfile.submissionMethod).is(submitterEngine));
        }
        Object valueObj = formValueForKey( formValues, "institution" );
        if ( valueObj != null )
        {
            qualifier = and(qualifier,
                courseOffering.dot(CourseOffering.course)
                .dot(Course.department).dot(Department.institution)
                .dot(AuthenticationDomain.propertyName).is(
                    "authenticator." + valueObj));
        }
        valueObj = formValueForKey( formValues, "crn" );
        if ( valueObj != null )
        {
            qualifier = and(qualifier,
                courseOffering.dot(CourseOffering.crn).is(valueObj.toString()));
        }
        valueObj = formValueForKey( formValues, "course" );
        if ( valueObj != null )
        {
            qualifier = and(qualifier,
                courseOffering.dot(CourseOffering.course).dot(Course.number)
                .is(ERXValueUtilities.intValue(valueObj)));
        }
        boolean forStaff = ERXValueUtilities.booleanValue(
            formValueForKey(formValues, "staff"));
        showAll = ERXValueUtilities.booleanValueWithDefault(
            formValueForKey(formValues, "showAll"), showAll || forStaff);
        if (!forStaff)
        {
            qualifier = and(qualifier, publish.isTrue());
        }

//        if (forStaff || showAll)
//        {
//            spec.setQualifier(
//                courseOffering.dot(CourseOffering.semester).is(
//                    Semester.forDate(context, new NSTimestamp())).and(
//                        EOQualifier.qualifierToMatchAllValues(restrictions)));
//        }

        if (qualifier == null)
        {
            qualifier = assignment.isNotNull();
        }

        if (log.isDebugEnabled())
        {
            log.debug("objectsForSubmitterEngine(): qualifier = " + qualifier);
        }
        NSArray<EOSortOrdering> orderings = groupByCRN
            ? SUBMISSION_CRN_ORDERINGS
            : SUBMISSION_ORDERINGS;
        NSArray<AssignmentOffering> results =
            objectsMatchingQualifier(context, qualifier, orderings);
        valueObj = formValueForKey(formValues, "courses");
        if (valueObj != null)
        {
            // filter down to the given list of courses
            log.debug("before courses filter: " + results);
            NSArray<String> courses = new NSArray<String>(
                valueObj.toString().split("\\s*,\\s*"));
            log.debug("courses filter = " + courses);
            results = EOQualifier.filteredArrayWithQualifier(results,
                new InQualifier(courseOffering.dot(CourseOffering.course)
                    .dot(Course.number).dot("toString").key(), courses));
            log.debug("after courses filter: " + results);
        }
        valueObj = formValueForKey(formValues, "crns");
        if (valueObj != null)
        {
            // filter down to the given list of crns
            log.debug("before crns filter: " + results);
            results = EOQualifier.filteredArrayWithQualifier(results,
                new InQualifier(
                    courseOffering.dot(CourseOffering.crn)
                        .dot("toString").key(),
                    new NSArray<String>(valueObj.toString().split("\\s*,\\s*"))
                ));
            log.debug("after crns filter: " + results);
        }
        qualifier = null;
        if (showAll)
        {
            qualifier = lateDeadline.greaterThan(Semester
                .forDate(context, currentTime).semesterStartDate());
        }
        else
        {
            qualifier = availableFrom.lessThan(currentTime).and(
                lateDeadline.greaterThan(currentTime));
        }
        {
            @SuppressWarnings("unchecked")
            NSArray<AssignmentOffering> newResults =
                ERXArrayUtilities.filteredArrayWithQualifierEvaluation(
                    results, qualifier);
            results = newResults;
        }
        if (!groupByCRN)
        {
            NSMutableArray<AssignmentOffering> filteredResults =
                results.mutableClone();
            Map<Course, Map<Assignment, NSMutableArray<AssignmentOffering>>>
                courseAssignmentMap = new HashMap<Course,
                    Map<Assignment, NSMutableArray<AssignmentOffering>>>();
            for (int i = 0; i < filteredResults.count(); i++)
            {
                AssignmentOffering ao = filteredResults.objectAtIndex(i);
                if (!addAssignmentIfNecessary(
                    ao, courseAssignmentMap, preserveDateDifferences))
                {
                    filteredResults.removeObjectAtIndex(i);
                    i--;
                }
            }
            results = filteredResults;
        }
        if (log.isDebugEnabled())
        {
            log.debug("results = " + results);
            for (AssignmentOffering ao : results)
            {
                log.debug("offering = " + ao + ", semester = "
                    + ao.courseOffering().semester());
            }
        }
        return results;
    }


    // ----------------------------------------------------------
    @Override
    public void willInsert()
    {
        setLastModified(new NSTimestamp());
        super.willInsert();
    }


    // ----------------------------------------------------------
    @Override
    public void willUpdate()
    {
        setLastModified(new NSTimestamp());
        super.willInsert();
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    /**
     * Extract a form value from a dictionary.  The WORequest.formValues()
     * method returns a dictionary mapping each form key to an NSArray of
     * values.  This method looks up the given key, and extracts the first
     * value from the array.
     * @param dict the dictionary of form values, in the format returned by
     *             WORequest.formValues().
     * @param key  The key to look up.
     * @return the value for the given form key, or null if there is none
     */
    static private Object formValueForKey(
        NSDictionary<String, ?> dict, String key)
    {
        Object values = dict.valueForKey( key );
        if ( values != null && values instanceof NSArray )
        {
            NSArray<?> array = (NSArray<?>)values;
            if ( array.count() > 0 )
            {
                return array.objectAtIndex( 0 );
            }
            else
            {
                return null;
            }
        }
        return values;
    }


    // ----------------------------------------------------------
    /**
     * This helper method for {@link #objectsForSubmitterEngine()} simply
     * records an assignment in a two-level map organized first by course
     * and then by assignment name.
     * @param ao   the assignment offering to add to the map
     * @param map  the map to add to
     * @return true, if the assignment was added, which will only happen if
     * there are no other assignment offerings for the same assignment that
     * are already entered for some offering of the same course.
     * Alternatively, returns false if there is already an assignment/course
     * combination registered in the map that matches.
     */
    static private boolean addAssignmentIfNecessary(
        AssignmentOffering ao,
        Map<Course, Map<Assignment,
                        NSMutableArray<AssignmentOffering>>> map,
        boolean preserveDateDifferences)
    {
        Map<Assignment, NSMutableArray<AssignmentOffering>> courseMap =
            map.get(ao.courseOffering().course());
        if (courseMap == null)
        {
            courseMap =
                new HashMap<Assignment,
                            NSMutableArray<AssignmentOffering>>();
            map.put(ao.courseOffering().course(), courseMap);
        }
        if (courseMap.get(ao.assignment()) == null)
        {
            courseMap.put(
                ao.assignment(), new NSMutableArray<AssignmentOffering>(ao));
            return true;
        }
        else if (preserveDateDifferences)
        {
            NSMutableArray<AssignmentOffering> currentOfferings =
                courseMap.get(ao.assignment());
            long due = ao.dueDate().getTime();
            for (AssignmentOffering other : currentOfferings)
            {
                // Is there an existing offering within 5 minutes of this one
                if (Math.abs(due - other.dueDate().getTime()) < 5 * 60 * 1000)
                {
                    return false;
                }
            }
            currentOfferings.add(ao);
            return true;
        }
        else
        {
            return false;
        }
    }


    // ----------------------------------------------------------
    private static class InQualifier
        extends ERXInQualifier
    {
        public InQualifier( String key, NSArray<?> values )
        {
            super( key, values, 1 );
        }

        /** Tests if the given object's key is in the supplied values */
        public boolean evaluateWithObject(Object object)
        {
            Object value = NSKeyValueCodingAdditions.Utility
                .valueForKeyPath(object, key());
            return value != null && values().containsObject(value);
        }

    }


    //~ Instance/static variables .............................................

    private String cachedPermalink;
    private String subdirToDelete;

    private static final NSArray<EOSortOrdering> SUBMISSION_ORDERINGS =
        courseOffering.dot(CourseOffering.course)
        .dot(Course.department).dot(Department.institution)
        .dot(AuthenticationDomain.displayableName).ascInsensitive()
    .then(
        courseOffering.dot(CourseOffering.course)
        .dot(Course.department).dot(Department.abbreviation)
        .ascInsensitive())
    .then(
        courseOffering.dot(CourseOffering.course).dot(Course.number)
        .asc())
    .then(dueDate.asc())
    .then(assignment.dot(Assignment.name).ascInsensitive());

    private static final NSArray<EOSortOrdering> SUBMISSION_CRN_ORDERINGS =
        courseOffering.dot(CourseOffering.course)
        .dot(Course.department).dot(Department.institution)
        .dot(AuthenticationDomain.displayableName).ascInsensitive()
    .then(
        courseOffering.dot(CourseOffering.course)
        .dot(Course.department).dot(Department.abbreviation)
        .ascInsensitive())
    .then(
        courseOffering.dot(CourseOffering.course).dot(Course.number)
        .asc())
    .then(courseOffering.dot(CourseOffering.crn).asc())
    .then(dueDate.asc())
    .then(assignment.dot(Assignment.name).ascInsensitive());

    static Logger log = Logger.getLogger( AssignmentOffering.class );
}
