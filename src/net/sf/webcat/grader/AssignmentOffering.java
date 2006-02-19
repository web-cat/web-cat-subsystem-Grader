/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006 Virginia Tech
 |
 |  This file is part of Web-CAT.
 |
 |  Web-CAT is free software; you can redistribute it and/or modify
 |  it under the terms of the GNU General Public License as published by
 |  the Free Software Foundation; either version 2 of the License, or
 |  (at your option) any later version.
 |
 |  Web-CAT is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU General Public License for more details.
 |
 |  You should have received a copy of the GNU General Public License
 |  along with Web-CAT; if not, write to the Free Software
 |  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 |
 |  Project manager: Stephen Edwards <edwards@cs.vt.edu>
 |  Virginia Tech CS Dept, 660 McBryde Hall (0106), Blacksburg, VA 24061 USA
 \*==========================================================================*/

package net.sf.webcat.grader;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import er.extensions.*;
import net.sf.webcat.core.*;
import net.sf.webcat.grader.graphs.*;

import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Represents the binding between an assignment and a course offering
 * (i.e., giving a specific assignment in a given section of a course).
 *
 * @author Stephen Edwards
 * @version $Id$
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
    public static final String COURSE_OFFERING_TAS_KEY  =
        COURSE_OFFERING_KEY + "." + CourseOffering.TAS_KEY;
    public static final String COURSE_OFFERING_CRN_KEY  =
        COURSE_OFFERING_KEY + "." + CourseOffering.CRN_KEY;
    public static final String COURSE_NUMBER_KEY  =
        COURSE_OFFERING_KEY + "."
        + CourseOffering.COURSE_KEY + "."
        + Course.NUMBER_KEY;

    public static final String SUBMISSION_METHOD_KEY =
        ASSIGNMENT_KEY + "."
        + Assignment.SUBMISSION_PROFILE_KEY + "."
        + SubmissionProfile.SUBMISSION_METHOD_KEY;
    public static final String INSTITUTION_PROPERTY_NAME_KEY =
        COURSE_OFFERING_KEY + "."
        + CourseOffering.COURSE_KEY + "."
        + Course.DEPARTMENT_KEY + "."
        + Department.INSTITUTION_KEY + "."
        + AuthenticationDomain.PROPERTY_NAME_KEY;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Determine whether this assignment offering has any jobs
     * or grading activities suspended.
     * @return true if any jobs or grading activities have been suspended
     */
    public boolean suspendedWarning()
    {
        return gradingSuspended() || hasSuspendedSubs();
    }


    // ----------------------------------------------------------
    /**
     * Determine the latest time when assignments are accepted.
     * @return the final deadline as a timestamp
     */
    public NSTimestamp lateDeadline()
    {
        NSTimestamp dueDate = dueDate();
        if ( dueDate != null )
        {
            Assignment assignment = assignment();
            if ( assignment != null )
            {
                SubmissionProfile submissionProfile =
                    assignment.submissionProfile();
                if ( submissionProfile != null )
                {
                    dueDate = new NSTimestamp(
                        submissionProfile.deadTimeDelta(),
                        dueDate
                    );
                }
            }
        }
        log.debug( "lateDeadline() = " + dueDate );
        return dueDate;
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
             || courseOffering().TAs().containsObject( user ) )
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
        NSTimestamp dueDate = dueDate();
        NSTimestamp openingDate = null;
        if ( dueDate != null )
        {
            Assignment assignment = assignment();
            if ( assignment != null )
            {
                SubmissionProfile submissionProfile =
                    assignment.submissionProfile();
                if (  submissionProfile != null
                   && submissionProfile.availableTimeDeltaRaw() != null )
                {
                    openingDate = new NSTimestamp(
                        - submissionProfile.availableTimeDelta(),
                        dueDate
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
        Assignment assignment = assignment();
        String result = "";
        if ( course != null )
        {
            result += course.compactName() + " ";
        }
        if ( assignment != null )
        {
            result += assignment.titleString();
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
            NSArray subs = SubmissionResult.objectsForMostRecentSubmissions(
                editingContext(), this );
            for ( int i = 0; i < subs.count(); i++ )
            {
                SubmissionResult sr =
                    (SubmissionResult)subs.objectAtIndex( i );
                summary.addSubmission( sr.graphableScore() );
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
    public NSArray getSuspendedSubs()
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
        NSArray subs = EOUtilities.objectsMatchingValues(
                editingContext(),
                Submission.ENTITY_NAME,
                new NSDictionary(
                    new Object[] {  this,
                                    user
                    },
                    new Object[] { Submission.ASSIGNMENT_OFFERING_KEY,
                                   Submission.USER_KEY }
                )
            );
        if ( subs != null && subs.count() > 0 )
        {
            mostRecent = (Submission)subs.objectAtIndex( 0 );
            for ( int j = 1; j < subs.count(); j++ )
            {
                Submission s = (Submission)subs.objectAtIndex( j );
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
    public NSArray mostRecentSubsForAll()
    {
        NSMutableArray recentSubs = new NSMutableArray();
        NSArray students = courseOffering().students();
        for ( int i = 0; i < students.count(); i++ )
        {
            Submission s =
                mostRecentSubFor( (User)students.objectAtIndex( i ));
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
    public SubmissionResult mostRecentSubmissionResultFor( User user )
    {
        SubmissionResult newest = null;
        NSArray subs = SubmissionResult.objectsForMostRecentSubmission(
            editingContext(), this, user );
        if ( subs.count() > 0 )
        {
            newest = (SubmissionResult)subs.objectAtIndex( 0 );
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
    public void regradeMostRecentSubsForAll( EOEditingContext ec )
    {
        NSArray subs = mostRecentSubsForAll();
        for ( int i = 0; i < subs.count(); i++ )
        {
            Submission s = (Submission)subs.objectAtIndex( i );
            s.requeueForGrading( ec );
        }
        ec.saveChanges();
        ( (Grader)( ( (Application)Application.application() )
                    .subsystemManager()
                    .subsystem( Grader.class.getName() ) ) )
            .graderQueue().enqueue( null );
    }
    
    
    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where all submissions for any
     * user in a given authentication domain are stored.
     * @param domain the authentication domain to use in generating the
     *        directory name
     * @return the directory name
     */
    public static StringBuffer submissionBaseDirName(
        AuthenticationDomain domain )
    {
        StringBuffer dir = new StringBuffer( 50 );
        dir.append( net.sf.webcat.core.Application
            .configurationProperties().getProperty( "grader.submissiondir" ) );
        dir.append( '/' );
        dir.append( domain.subdirName() );
        return dir;
    }
    

    // ----------------------------------------------------------
    /**
     * Retrieve the name of the subdirectory where all submissions for this
     * assignment offering are stored.  This subdirectory is relative to
     * the base submission directory for some authentication domain, such
     * as the value returned by
     * {@link #submissionBaseDirName(AuthenticationDomain)}.
     * @param dir the string buffer to add the requested subdirectory to
     *        (a / is added to this buffer, followed by the subdirectory name
     *        generated here)
     * @param assignSubdir the subdirectory name to use for the assignment.
     *        If null is passed in (which is typical), the results of
     *        {@link Assignment#subdirName()} used.  Pass in a non-null value
     *        to override with another choice.
     */
    public void addSubdirNameForAssignment( StringBuffer dir,
                                            String       assignSubdir )
    {
        dir.append( '/' );
        dir.append( courseOffering().semester().dirName() );
        dir.append( '/' );
        dir.append( courseOffering().crnSubdirName() );
        dir.append( '/' );
        dir.append( ( assignSubdir == null )
                        ? assignment().subdirName()
                        : assignSubdir );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>moodleId</code> value.
     * @return the value of the attribute
     */
    public Number moodleId()
    {
        Number result = super.moodleId();
        if ( result == null && assignment() != null )
        {
            result = assignment().moodleId();
        }
        return result;
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
     * @param courseOffering The course offering to search for
     * @param limit the maximum number of assignment offerings to return
     * (or zero, if all should be returned)
     * @return an NSArray of the entities retrieved, sorted in descending order
     * by due date (latest due date first)
     */
    public static NSMutableArray offeringsWithSimilarNames(
            EOEditingContext context,
            String targetName,
            net.sf.webcat.core.CourseOffering courseOffering,
            int limit
        )
    {
        NSMutableArray others = new NSMutableArray();
        NSArray sameSection = AssignmentOffering.objectsForCourseOffering(
            context, courseOffering );
        for ( int i = 0; i < sameSection.count()
                         && ( limit < 1 || others.count() < limit ); i++ )
        {
            AssignmentOffering ao =
                (AssignmentOffering)sameSection.objectAtIndex( i );
            String name = ao.assignment().name();
            if ( !targetName.equals( name )
                 && Assignment.namesAreSimilar( name, targetName ) )
            {
                log.debug( "matching assignment offering (target = '"
                           + targetName + "'): "
                           + ao.titleString() + ", " + ao.dueDate() );
                others.addObject( ao );
            }
        }
        return others;
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
     * @return an array of the given assignment offerings
     */
    public static NSArray objectsForSubmitterEngine(
        EOEditingContext context,
        NSDictionary     formValues,
        NSTimestamp      currentTime,
        int              submitterEngine,
        boolean          groupByCRN
        )
    {
        EOFetchSpecification spec =
            EOFetchSpecification.fetchSpecificationNamed(
                "submitterEngineBase",
                ENTITY_NAME );
        // Set up the qualifier
        NSMutableDictionary restrictions = new NSMutableDictionary();
        restrictions.setObjectForKey(
            ERXConstant.integerForInt( submitterEngine ),
            SUBMISSION_METHOD_KEY );
        Object valueObj = formValueForKey( formValues, "institution" );
        if ( valueObj != null )
        {
            restrictions.setObjectForKey(
                "authenticator." + valueObj,
                INSTITUTION_PROPERTY_NAME_KEY );
        }
        valueObj = formValueForKey( formValues, "crn" );
        if ( valueObj != null )
        {
            restrictions.setObjectForKey(
                valueObj,
                COURSE_OFFERING_CRN_KEY );
        }
        valueObj = formValueForKey( formValues, "course" );
        if ( valueObj != null )
        {
            try
            {
                restrictions.setObjectForKey(
                    Integer.valueOf( valueObj.toString() ),
                    COURSE_NUMBER_KEY
                    );
            }
            catch ( NumberFormatException e )
            {
                Application.emailExceptionToAdmins(
                    e,
                    null,
                    "trying to parse course number = '" + valueObj + "'"
                    );
            }
        }
        boolean forStaff = ERXValueUtilities.booleanValue(
            formValueForKey( formValues, "staff" ) );
        if ( !forStaff )
        {
            restrictions.setObjectForKey(
                ERXConstant.integerForInt( 1 ),
                PUBLISH_KEY );
        }
        spec.setQualifier(
            EOQualifier.qualifierToMatchAllValues( restrictions ) );
        if ( log.isDebugEnabled() )
        {
            log.debug(
                "objectsForSubmitterEngine(): qualifier = "
                + spec.qualifier() );
        }
        if ( groupByCRN )
        {
            NSMutableArray orderings = spec.sortOrderings().mutableClone();
            orderings.insertObjectAtIndex(
                EOSortOrdering.sortOrderingWithKey(
                    COURSE_OFFERING_CRN_KEY,
                    EOSortOrdering.CompareAscending ), 
                orderings.count() - 2
                );
            spec.setSortOrderings( orderings );
        }
        NSArray results = context.objectsWithFetchSpecification( spec );
        if ( !forStaff )
        {
            NSMutableArray qualifiers = new NSMutableArray(
                new EOKeyValueQualifier(
                    AVAILABLE_FROM_KEY, 
                    EOQualifier.QualifierOperatorLessThan, 
                    currentTime
                ) );
            qualifiers.addObject( new EOKeyValueQualifier(
                LATE_DEADLINE_KEY, 
                EOQualifier.QualifierOperatorGreaterThan,
                currentTime
                ) );
            results = ERXArrayUtilities.filteredArrayWithQualifierEvaluation(
                results,
                new EOAndQualifier( qualifiers ) );
        }
        if ( !groupByCRN )
        {
            NSMutableArray filteredResults = results.mutableClone();
            for ( int i = 0; i < filteredResults.count() - 1; i++ )
            {
                AssignmentOffering ao1 =
                    (AssignmentOffering)filteredResults.objectAtIndex( i );
                AssignmentOffering ao2 =
                    (AssignmentOffering)filteredResults.objectAtIndex( i + 1 );
                if ( ao1.assignment() == ao2.assignment() )
                {
                    filteredResults.removeObjectAtIndex( i );
                    i--;
                }
            }
            results = filteredResults;
        }
        return results;
    }


// If you add instance variables to store property values you
// should add empty implementions of the Serialization methods
// to avoid unnecessary overhead (the properties will be
// serialized for you in the superclass).

//    // ----------------------------------------------------------
//    /**
//     * Serialize this object (an empty implementation, since the
//     * superclass handles this responsibility).
//     * @param out the stream to write to
//     */
//    private void writeObject( java.io.ObjectOutputStream out )
//        throws java.io.IOException
//    {
//    }
//
//
//    // ----------------------------------------------------------
//    /**
//     * Read in a serialized object (an empty implementation, since the
//     * superclass handles this responsibility).
//     * @param in the stream to read from
//     */
//    private void readObject( java.io.ObjectInputStream in )
//        throws java.io.IOException, java.lang.ClassNotFoundException
//    {
//    }


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
    static private Object formValueForKey( NSDictionary dict, String key )
    {
        Object values = dict.valueForKey( key );
        if ( values != null && values instanceof NSArray )
        {
            NSArray array = (NSArray)values;
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


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( AssignmentOffering.class );
}
