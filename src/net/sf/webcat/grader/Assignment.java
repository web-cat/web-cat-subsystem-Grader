/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2009 Virginia Tech
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

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import er.extensions.foundation.ERXArrayUtilities;
import java.io.File;
import java.util.*;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * An assignment that can be given in one or more classes.
 *
 * @author Stephen Edwards
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class Assignment
    extends _Assignment
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new Assignment object.
     */
    public Assignment()
    {
        super();
    }


    //~ Constants (for key names) .............................................

    public static final String COURSE_OFFERINGS_KEY =
        OFFERINGS_KEY
        + "." + AssignmentOffering.COURSE_OFFERING_KEY;
    public static final String COURSES_KEY =
        COURSE_OFFERINGS_KEY
        + "." + CourseOffering.COURSE_KEY;
    public static final String SEMESTERS_KEY =
        COURSE_OFFERINGS_KEY
        + "." + CourseOffering.SEMESTER_KEY;
    public static final String ID_FORM_KEY = "aid";


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    public String subdirName()
    {
        if ( cachedSubdirName == null )
        {
            String name = name();
            cachedSubdirName = AuthenticationDomain.subdirNameOf( name );
            log.debug( "trimmed name '" + name + "' to '"
                       + cachedSubdirName + "'" );
        }
        return cachedSubdirName;
    }


    // ----------------------------------------------------------
    /**
     * Get a short (no longer than 60 characters) description of this
     * assignment.
     * @return the description
     */
    public String userPresentableDescription()
    {
        String result = name();
        if ( offerings().count() > 0 )
        {
            result += " (" + offerings().objectAtIndex( 0 ).courseOffering()
                .course().deptNumber() + ")";
        }
        return result;
    }


    // ----------------------------------------------------------
    public void setName( String value )
    {
        if ( dirNeedingRenaming == null && name() != null )
        {
            dirNeedingRenaming = subdirName();
        }
        cachedSubdirName = null;
        super.setName( value.trim() );
    }


    // ----------------------------------------------------------
    public Object validateName( Object value )
    {
        if ( value == null )
        {
            log.debug( "conflict exists, throwing exception" );
            throw new ValidationException(
                "Please provide an assignment name." );
        }
        String result = value.toString().trim();
        String newSubdirName = AuthenticationDomain.subdirNameOf( result );
        log.debug( "validateName(" + result + ")" );
        log.debug( "subdir = " + newSubdirName );
        if ( !newSubdirName.equals( subdirName() )
             && conflictingSubdirNameExists( newSubdirName ) )
        {
            log.debug( "conflict exists, throwing exception" );
            throw new ValidationException(
                "The name '" + result + "' conflicts with an existing "
                + "assignment.  Please choose another name." );
        }
        log.debug( "no conflict found" );
        return result;
    }


    // ----------------------------------------------------------
    /* (non-Javadoc)
     * @see er.extensions.eof.ERXGenericRecord#didUpdate()
     */
    public void didUpdate()
    {
        super.didUpdate();
        if ( dirNeedingRenaming != null )
        {
            renameSubdirs( dirNeedingRenaming, subdirName() );
            dirNeedingRenaming = null;
        }
    }


    // ----------------------------------------------------------
    public String titleString()
    {
        String result = name();
        if ( shortDescription() != null )
        {
            result += ": " + shortDescription();
        }
        return result;
    }


    // ----------------------------------------------------------
    public Step addNewStep( ScriptFile script )
    {
        int position = steps().count() + 1;
        Step step = createStepsRelationship();
        step.setOrder( position );
        step.setScriptRelationship( script );
        return step;
    }


    // ----------------------------------------------------------
    public Step copyStep( Step step, boolean keepOrdering )
    {
        Step newStep = addNewStep( step.script() );
        newStep.setTimeout( step.timeout() );
        newStep.setConfigRelationship( step.config() );
        MutableDictionary dict = step.configSettings();
        if ( dict != null )
        {
            newStep.setConfigSettings( new MutableDictionary( dict ) );
        }
        if ( keepOrdering )
        {
            newStep.setOrder( step.order() );
        }
        return newStep;
    }


    // ----------------------------------------------------------
    public boolean usesTestingScore()
    {
        SubmissionProfile profile = submissionProfile();
        return profile != null
            && ( profile.correctnessPoints() > 0.0 );
    }


    // ----------------------------------------------------------
    public boolean usesToolCheckScore()
    {
        SubmissionProfile profile = submissionProfile();
        return profile != null
            && ( profile.toolPointsRaw() != null
                 || profile.toolPoints() != 0 );
    }


    // ----------------------------------------------------------
    public boolean usesBonusesOrPenalties()
    {
        SubmissionProfile profile = submissionProfile();
        return profile != null
            && ( profile.awardEarlyBonus() || profile.deductLatePenalty() );
    }


    // ----------------------------------------------------------
    public NSTimestamp commonOfferingsDueDate()
    {
        NSTimestamp common = null;
        NSArray<AssignmentOffering> offerings = offerings();
        if ( offerings.count() > 1 )
        {
            for (AssignmentOffering ao : offerings)
            {
                if ( common == null )
                {
                    common = ao.dueDate();
                }
                else if ( common.compare( ao.dueDate() ) !=
                          NSComparator.OrderedSame )
                {
                    common = null;
                    break;
                }
            }
        }
        return common;
    }


    // ----------------------------------------------------------
    public AssignmentOffering offeringForUser( User user )
    {
        AssignmentOffering offering = null;
        NSDictionary<String, Object> userBinding =
            new NSDictionary<String, Object>( user, "user" );

        // First, check to see if the user is a student in any of the
        // course offerings associated with the available assignment offerings
        NSArray<?> results = ERXArrayUtilities
            .filteredArrayWithEntityFetchSpecification( offerings(),
                AssignmentOffering.ENTITY_NAME,
                AssignmentOffering.STUDENT_FSPEC,
                userBinding );
        if ( results == null || results.count() == 0 )
        {
            // if the user is not found as a student, check for staff instead
            results = ERXArrayUtilities
                .filteredArrayWithEntityFetchSpecification( offerings(),
                    AssignmentOffering.ENTITY_NAME,
                    AssignmentOffering.STAFF_FSPEC,
                    userBinding );
        }
        if ( results != null && results.count() > 0 )
        {
            offering = (AssignmentOffering)results.objectAtIndex( 0 );
        }
        return offering;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve all the other assignments that share a course offering with
     * one of this assignment's offerings.
     *
     * @param context The editing context to use
     * @return an NSArray of the entities retrieved
     */
    public NSArray<Assignment> objectsForNeighborAssignments(
            EOEditingContext context
        )
    {
        NSMutableArray<Assignment> results = new NSMutableArray<Assignment>();
        for (AssignmentOffering offering : offerings())
        {
            ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(results,
                objectsForNeighborAssignments(
                    context, offering.courseOffering()));
        }
        results.remove(this);
        return results;
    }


    // ----------------------------------------------------------
    public static boolean namesAreSimilar( String name1, String name2 )
    {
        boolean result = false;
        int limit = Math.min( name1.length(), name2.length() );
        for ( int i = 0; i < limit; i++ )
        {
            if ( Character.isLetter( name1.charAt( i ) ) )
            {
                result = ( name1.charAt( i ) == name2.charAt( i ) );
                if ( !result ) break;
            }
            else
            {
                break;
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * This EOQualifier matches any assignment with a directory name
     * that does not match (case-insensitive) any existing assignment offering
     * associated with a given course offering--in other words, succeeds
     * for assignments that would have unique names.
     */
    public static class NonDuplicateAssignmentNameQualifier
        extends EOQualifier
    {
        // ----------------------------------------------------------
        /**
         * Create a new qualifier for assignments in the given course.
         * @param courseOffering the course offering to check against
         */
        public NonDuplicateAssignmentNameQualifier(
            CourseOffering courseOffering)
        {
            if (courseOffering != null)
            {
                lcNames = new HashSet<String>();
                NSArray<AssignmentOffering> assignments =
                    AssignmentOffering.objectsForCourseOffering(
                        courseOffering.editingContext(), courseOffering );
                for (AssignmentOffering ao : assignments)
                {
                    String newName = ao.assignment().subdirName();
                    if (newName != null)
                    {
                        lcNames.add( newName.toLowerCase() );
                    }
                }
            }
        }


        // ----------------------------------------------------------
        /**
         * Create a new qualifier for assignments in the given course.
         * @param lcNames the set of existing assignment names (assumed to all
         * be lowercase-only) to check against
         */
        public NonDuplicateAssignmentNameQualifier(Set<String> lcNames)
        {
            this.lcNames = lcNames;
        }


        // ----------------------------------------------------------
        @Override
        @SuppressWarnings("unchecked")
        public void addQualifierKeysToSet( NSMutableSet qualifierKeys )
        {
            qualifierKeys.add("subdirName");
        }


        // ----------------------------------------------------------
        @Override
        @SuppressWarnings("unchecked")
        public EOQualifier qualifierWithBindings(
            NSDictionary bindings, boolean requiresAll )
        {
            Object courseOfferingBinding = bindings.valueForKey(
                "courseOffering");
            if (courseOfferingBinding == null)
            {
                return new NonDuplicateAssignmentNameQualifier((Set)null);
            }
            else
            {
                return new NonDuplicateAssignmentNameQualifier(
                    (CourseOffering)courseOfferingBinding);
            }
        }


        // ----------------------------------------------------------
        @Override
        public void validateKeysWithRootClassDescription(
            EOClassDescription classDescription )
        {
            if (!classDescription.entityName().equals(ENTITY_NAME))
            {
                throw new RuntimeException("This qualifier can only be "
                    + "applied to " + ENTITY_NAME + " objects.");
            }
        }


        // ----------------------------------------------------------
        @Override
        public boolean evaluateWithObject( Object object )
        {
            String subdirName = ((Assignment)object).subdirName();
            return subdirName != null
                && ( lcNames == null
                     || !lcNames.contains( subdirName.toLowerCase() ) );
        }


        //~ Instance/static variables .........................................

        private Set<String> lcNames;
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    private boolean conflictingSubdirNameExists( String subdir )
    {
        NSArray<Assignment> neighbors =
            objectsForNeighborAssignments(editingContext());
        if (log.isDebugEnabled())
        {
            log.debug( "neighbors = " + neighbors );
        }
        if (subdir != null)
        {
            subdir = subdir.toLowerCase();
        }
        for (Assignment asgn : neighbors)
        {
            String yourSub = asgn.subdirName();
            if (yourSub != null)
            {
                yourSub = yourSub.toLowerCase();
            }
            if ( (subdir == null && yourSub == null)
                 || (subdir != null && subdir.equals(yourSub)))
            {
                return true;
            }
        }
        return false;
    }


    // ----------------------------------------------------------
    private void renameSubdirs( String oldSubdir, String newSubdir )
    {
        NSArray<AuthenticationDomain> domains =
            AuthenticationDomain.authDomains();
        for (AuthenticationDomain domain : domains)
        {
            NSArray<AssignmentOffering> offerings = offerings();
            StringBuffer dir = domain.submissionBaseDirBuffer();
            int baseDirLen = dir.length();
            String msgs = null;
            for (AssignmentOffering offering : offerings)
            {
                // clear out old suffix
                dir.delete( baseDirLen, dir.length() );
                offering.courseOffering().addSubdirTo( dir );
                dir.append('/');
                dir.append( oldSubdir );
                File oldDir = new File( dir.toString() );
                if ( oldDir.exists() )
                {
                    dir.delete( baseDirLen, dir.length() );
                    offering.courseOffering().addSubdirTo( dir );
                    dir.append('/');
                    dir.append( newSubdir );
                    File newDir = new File( dir.toString() );
                    if (!oldDir.renameTo( newDir ))
                    {
                        msgs = (msgs == null ? "" : (msgs + "  "))
                            + "Failed to rename directory: "
                            + oldDir + " => " + newDir;
                    }
                }
            }
            if (msgs != null)
            {
                throw new RuntimeException(msgs);
            }
        }
    }


    //~ Instance/static variables .............................................

    private String cachedSubdirName   = null;
    private String dirNeedingRenaming = null;
    static Logger log = Logger.getLogger( Assignment.class );
}
