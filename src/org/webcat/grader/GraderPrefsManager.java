/*==========================================================================*\
 |  $Id: GraderPrefsManager.java,v 1.3 2012/01/20 21:21:44 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2012 Virginia Tech
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

import org.webcat.core.*;
import er.extensions.foundation.ERXValueUtilities;

//-------------------------------------------------------------------------
/**
 *  An {@link CachingEOManager} specialized for managing a
 *  {@link GraderPrefs} object.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.3 $, $Date: 2012/01/20 21:21:44 $
 */
public class GraderPrefsManager
    extends CachingEOManager
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new manager for the given GraderPrefs object (which
     * presumably lives in the session's editing context).
     * @param prefs the object to manage
     * @param manager the (probably shared) editing context manager to use
     * for independent saving of the given eo
     */
    public GraderPrefsManager(GraderPrefs prefs, ECManager manager)
    {
        super(prefs, manager);
    }


    //~ Constants .............................................................

    public static final String SHOW_UNPUBLISHED_ASSIGNMENTS_KEY
        = "showUnpublishedAssignments";
    public static final String SHOW_CLOSED_ASSIGNMENTS_KEY
        = "showClosedAssignments";


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>commentHistory</code> value.
     * @return the value of the attribute
     */
    public String commentHistory()
    {
        return (String)handleQueryWithUnboundKey(
            GraderPrefs.COMMENT_HISTORY_KEY);
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>commentHistory</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setCommentHistory(String value)
    {
        handleTakeValueForUnboundKey(value, GraderPrefs.COMMENT_HISTORY_KEY);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>assignment</code>
     * relationship.
     * @return the entity in the relationship
     */
    public Assignment assignment()
    {
        return (Assignment)handleQueryWithUnboundKey(
            GraderPrefs.ASSIGNMENT_KEY);
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>assignment</code>
     * relationship.
     *
     * @param value The new entity to relate to
     */
    public void setAssignmentRelationship(Assignment value)
    {
        addObjectToBothSidesOfRelationshipWithKey(
            value, GraderPrefs.ASSIGNMENT_KEY);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>assignmentOffering</code>
     * relationship.
     * @return the entity in the relationship
     */
    public AssignmentOffering assignmentOffering()
    {
        return (AssignmentOffering)handleQueryWithUnboundKey(
            GraderPrefs.ASSIGNMENT_OFFERING_KEY);
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>assignmentOffering</code>
     * relationship.
     *
     * @param value The new entity to relate to
     */
    public void setAssignmentOfferingRelationship(AssignmentOffering value)
    {
        addObjectToBothSidesOfRelationshipWithKey(
            value, GraderPrefs.ASSIGNMENT_OFFERING_KEY);
        if (value != null)
        {
            setAssignmentRelationship(value.assignment());
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>step</code>
     * relationship.
     * @return the entity in the relationship
     */
    public Step step()
    {
        return (Step)handleQueryWithUnboundKey(GraderPrefs.STEP_KEY);
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>step</code>
     * relationship.
     *
     * @param value The new entity to relate to
     */
    public void setStepRelationship(Step value)
    {
        addObjectToBothSidesOfRelationshipWithKey(value, GraderPrefs.STEP_KEY);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>submission</code>
     * relationship.
     * @return the entity in the relationship
     */
    public Submission submission()
    {
        return (Submission)handleQueryWithUnboundKey(
            GraderPrefs.SUBMISSION_KEY);
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>submission</code>
     * relationship.
     *
     * @param value The new entity to relate to
     */
    public void setSubmissionRelationship(Submission value)
    {
        addObjectToBothSidesOfRelationshipWithKey(
            value, GraderPrefs.SUBMISSION_KEY);
        if (value != null && value.assignmentOffering() != assignmentOffering())
        {
            setAssignmentOfferingRelationship(value.assignmentOffering());
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
        return (SubmissionFileStats)handleQueryWithUnboundKey(
            GraderPrefs.SUBMISSION_FILE_STATS_KEY);
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>submissionFileStats</code>
     * relationship.
     *
     * @param value The new entity to relate to
     */
    public void setSubmissionFileStatsRelationship(SubmissionFileStats value)
    {
        addObjectToBothSidesOfRelationshipWithKey(
            value, GraderPrefs.SUBMISSION_FILE_STATS_KEY);
    }


    // ----------------------------------------------------------
    public boolean showUnpublishedAssignments()
    {
        if (showUnpublishedAssignments == null)
        {
            User user = (User)handleQueryWithUnboundKey(GraderPrefs.USER_KEY);
            showUnpublishedAssignments = Boolean.valueOf(
                ERXValueUtilities.booleanValueWithDefault(
                    user.preferences().valueForKey(
                        SHOW_UNPUBLISHED_ASSIGNMENTS_KEY),
                    true));
        }
        return showUnpublishedAssignments.booleanValue();
    }


    // ----------------------------------------------------------
    public void setShowUnpublishedAssignments(boolean value)
    {
        showUnpublishedAssignments = Boolean.valueOf(value);
        User user = (User)handleQueryWithUnboundKey(GraderPrefs.USER_KEY);
        user.preferences().takeValueForKey(
            showUnpublishedAssignments,
            SHOW_UNPUBLISHED_ASSIGNMENTS_KEY);
        user.savePreferences();
    }


    // ----------------------------------------------------------
    public boolean showClosedAssignments()
    {
        if (showClosedAssignments == null)
        {
            User user = (User)handleQueryWithUnboundKey(GraderPrefs.USER_KEY);
            showClosedAssignments = Boolean.valueOf(
                ERXValueUtilities.booleanValueWithDefault(
                    user.preferences().valueForKey(
                        SHOW_CLOSED_ASSIGNMENTS_KEY),
                    false));
        }
        return showClosedAssignments.booleanValue();
    }


    // ----------------------------------------------------------
    public void setShowClosedAssignments(boolean value)
    {
        showClosedAssignments = Boolean.valueOf(value);
        User user = (User)handleQueryWithUnboundKey(GraderPrefs.USER_KEY);
        user.preferences().takeValueForKey(
            showClosedAssignments,
            SHOW_CLOSED_ASSIGNMENTS_KEY);
        user.savePreferences();
    }


    //~ Instance/static variables .............................................

    private Boolean showUnpublishedAssignments;
    private Boolean showClosedAssignments;
}
