/*==========================================================================*\
 |  $Id: GraderPrefs.java,v 1.3 2010/09/27 04:21:37 stedwar2 Exp $
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

package org.webcat.grader;

import org.apache.log4j.Level;

// -------------------------------------------------------------------------
/**
 * A simple EO to record persistent user choices for assignments and
 * so on used in navigation.
 *
 * @author  Stephen Edwards
 * @author  latest changes by: $Author: stedwar2 $
 * @version $Revision: 1.3 $ $Date: 2010/09/27 04:21:37 $
 */
public class GraderPrefs
    extends _GraderPrefs
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new GraderPrefs object.
     */
    public GraderPrefs()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>assignmentOffering</code>
     * relationship.
     * @return the entity in the relationship
     */
    public AssignmentOffering assignmentOffering()
    {
        try
        {
            AssignmentOffering result =  super.assignmentOffering();
            if ( result != null )
                result.dueDate();  // Force access of this object
            return result;
        }
        catch ( com.webobjects.eoaccess.EOObjectNotAvailableException e )
        {
            log.debug("assignmentOffering(): attempting to force null after "
                + e);
            if (log.isDebugEnabled())
            {
                // cut off debugging in base class to avoid recursive
                // calls to this method!
                Level oldLevel = log.getLevel();
                try
                {
                    log.setLevel( Level.OFF );
                    // Do NOT call setAssignmentOfferingRelationship, since
                    // it in turn calls assignmentOffering()!
                    super.setAssignmentOffering( null );
                }
                finally
                {
                    log.setLevel( oldLevel );
                }
            }
            else
            {
                // Do NOT call setAssignmentOfferingRelationship, since it in
                // turn calls assignmentOffering()!
                super.setAssignmentOffering( null );
            }
            return super.assignmentOffering();
        }
    }


    // ----------------------------------------------------------
    @Override
    public void setAssignmentOffering(AssignmentOffering value)
    {
        Assignment oldAssignment = super.assignment();
        // Clear the "assignment" pref (which represents "all offerings" of
        // some assignment) if a specific assignment offering is being
        // set.
        if (value != null && oldAssignment != null)
        {
            super.setAssignmentRelationship(null);
        }
        super.setAssignmentOffering(value);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>step</code>
     * relationship.
     * @return the entity in the relationship
     */
    public Step step()
    {
        try
        {
            Step result =  super.step();
            if ( result != null )
                result.order();  // Force access of this object
            return result;
        }
        catch ( com.webobjects.eoaccess.EOObjectNotAvailableException e )
        {
            log.debug("step(): attempting to force null after " + e);
            if (log.isDebugEnabled())
            {
                // cut off debugging in base class to avoid recursive
                // calls to this method!
                Level oldLevel = log.getLevel();
                try
                {
                    log.setLevel( Level.OFF );
                    // Do NOT call setStepRelationship, since it in
                    // turn calls step()!
                    super.setStep( null );
                }
                finally
                {
                    log.setLevel( oldLevel );
                }
            }
            else
            {
                // Do NOT call setStepRelationship, since it in
                // turn calls step()!
                super.setStep( null );
            }
            return super.step();
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>submission</code>
     * relationship.
     * @return the entity in the relationship
     */
    public Submission submission()
    {
        try
        {
            Submission result =  super.submission();
            if ( result != null )
                result.submitNumber();  // Force access of this object
            return result;
        }
        catch ( com.webobjects.eoaccess.EOObjectNotAvailableException e )
        {
            log.debug("submission(): attempting to force null after " + e);
            if (log.isDebugEnabled())
            {
                // cut off debugging in base class to avoid recursive
                // calls to this method!
                Level oldLevel = log.getLevel();
                try
                {
                    log.setLevel( Level.OFF );
                    // Do NOT call setSubmissionRelationship, since it in
                    // turn calls submission()!
                    super.setSubmission( null );
                }
                finally
                {
                    log.setLevel( oldLevel );
                }
            }
            else
            {
                // Do NOT call setSubmissionRelationship, since it in
                // turn calls submission()!
                super.setSubmission( null );
            }
            return super.submission();
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>submissionFileStats</code>
     * relationship.
     * @return the entity in the relationship
     */
    public SubmissionFileStats submissionFileStats()
    {
        try
        {
            SubmissionFileStats result =  super.submissionFileStats();
            if ( result != null )
                result.loc();  // Force access of this object
            return result;
        }
        catch ( com.webobjects.eoaccess.EOObjectNotAvailableException e )
        {
            log.debug("submissionFileStats(): attempting to force null after "
                + e);
            if (log.isDebugEnabled())
            {
                // cut off debugging in base class to avoid recursive
                // calls to this method!
                Level oldLevel = log.getLevel();
                try
                {
                    log.setLevel( Level.OFF );
                    // Do NOT call setSubmissionFileStatsRelationship, since
                    // it in turn calls submissionFileStats()!
                    super.setSubmissionFileStats( null );
                }
                finally
                {
                    log.setLevel( oldLevel );
                }
            }
            else
            {
                // Do NOT call setSubmissionFileStatsRelationship, since it in
                // turn calls submissionFileStats()!
                super.setSubmissionFileStats( null );
            }
            return super.submissionFileStats();
        }
    }
}
