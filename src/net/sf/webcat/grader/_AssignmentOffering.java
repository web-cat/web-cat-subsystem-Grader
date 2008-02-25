/*==========================================================================*\
 |  _AssignmentOffering.java
 |*-------------------------------------------------------------------------*|
 |  Created by eogenerator
 |  DO NOT EDIT.  Make changes to AssignmentOffering.java instead.
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

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.util.Enumeration;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * An automatically generated EOGenericRecord subclass.  DO NOT EDIT.
 * To change, use EOModeler, or make additions in
 * AssignmentOffering.java.
 *
 * @author Generated by eogenerator
 * @version version suppressed to control auto-generation
 */
public abstract class _AssignmentOffering
    extends er.extensions.ERXGenericRecord
    implements net.sf.webcat.core.MutableContainer.MutableContainerOwner
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new _AssignmentOffering object.
     */
    public _AssignmentOffering()
    {
        super();
    }


    // ----------------------------------------------------------
    /**
     * A static factory method for creating a new
     * _AssignmentOffering object given required
     * attributes and relationships.
     * @param editingContext The context in which the new object will be
     * inserted
     * @param gradingSuspended
     * @param publish
     * @param updateMutableFields
     * @return The newly created object
     */
    public static AssignmentOffering create(
        EOEditingContext editingContext,
        boolean gradingSuspended,
        boolean publish,
        boolean updateMutableFields
        )
    {
        AssignmentOffering eoObject = (AssignmentOffering)
            EOUtilities.createAndInsertInstance(
                editingContext,
                _AssignmentOffering.ENTITY_NAME);
        eoObject.setGradingSuspended(gradingSuspended);
        eoObject.setPublish(publish);
        eoObject.setUpdateMutableFields(updateMutableFields);
        return eoObject;
    }


    // ----------------------------------------------------------
    /**
     * Get a local instance of the given object in another editing context.
     * @param editingContext The target editing context
     * @param eo The object to import
     * @return An instance of the given object in the target editing context
     */
    public static AssignmentOffering localInstance(
        EOEditingContext editingContext, AssignmentOffering eo)
    {
        return (eo == null)
            ? null
            : (AssignmentOffering)EOUtilities.localInstanceOfObject(
                editingContext, eo);
    }


    // ----------------------------------------------------------
    /**
     * Look up an object by id number.  Assumes the editing
     * context is appropriately locked.
     * @param ec The editing context to use
     * @param id The id to look up
     * @return The object, or null if no such id exists
     */
    public static AssignmentOffering forId(
        EOEditingContext ec, int id )
    {
        AssignmentOffering obj = null;
        if (id > 0)
        {
            NSArray results = EOUtilities.objectsMatchingKeyAndValue( ec,
                ENTITY_NAME, "id", new Integer( id ) );
            if ( results != null && results.count() > 0 )
            {
                obj = (AssignmentOffering)results.objectAtIndex( 0 );
            }
        }
        return obj;
    }


    // ----------------------------------------------------------
    /**
     * Look up an object by id number.  Assumes the editing
     * context is appropriately locked.
     * @param ec The editing context to use
     * @param id The id to look up
     * @return The object, or null if no such id exists
     */
    public static AssignmentOffering forId(
        EOEditingContext ec, String id )
    {
        return forId( ec, er.extensions.ERXValueUtilities.intValue( id ) );
    }


    //~ Constants (for key names) .............................................

    // Attributes ---
    public static final String DUE_DATE_KEY = "dueDate";
    public static final String GRADING_SUSPENDED_KEY = "gradingSuspended";
    public static final String GRAPH_SUMMARY_KEY = "graphSummary";
    public static final String MOODLE_ID_KEY = "moodleId";
    public static final String PUBLISH_KEY = "publish";
    public static final String UPDATE_MUTABLE_FIELDS_KEY = "updateMutableFields";
    // To-one relationships ---
    public static final String ASSIGNMENT_KEY = "assignment";
    public static final String COURSE_OFFERING_KEY = "courseOffering";
    // To-many relationships ---
    public static final String SUBMISSIONS_KEY = "submissions";
    // Fetch specifications ---
    public static final String ALL_OFFERINGS_FSPEC = "allOfferings";
    public static final String COURSE_OFFERING_FSPEC = "courseOffering";
    public static final String STAFF_FSPEC = "staff";
    public static final String STUDENT_FSPEC = "student";
    public static final String SUBMITTER_ENGINE_BASE_FSPEC = "submitterEngineBase";
    public static final String ENTITY_NAME = "AssignmentOffering";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Get a local instance of this object in another editing context.
     * @param editingContext The target editing context
     * @return An instance of this object in the target editing context
     */
    public AssignmentOffering localInstance(EOEditingContext editingContext)
    {
        return (AssignmentOffering)EOUtilities.localInstanceOfObject(
            editingContext, this);
    }


    // ----------------------------------------------------------
    /**
     * Get a list of changes between this object's current state and the
     * last committed version.
     * @return a dictionary of the changes that have not yet been committed
     */
    public NSDictionary changedProperties()
    {
        return changesFromSnapshot(
            editingContext().committedSnapshotForObject(this) );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>id</code> value.
     * @return the value of the attribute
     */
    public Number id()
    {
        try
        {
            return (Number)EOUtilities.primaryKeyForObject(
                editingContext() , this ).objectForKey( "id" );
        }
        catch (Exception e)
        {
            return er.extensions.ERXConstant.ZeroInteger;
        }
    }

    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>dueDate</code> value.
     * @return the value of the attribute
     */
    public NSTimestamp dueDate()
    {
        return (NSTimestamp)storedValueForKey( "dueDate" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>dueDate</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setDueDate( NSTimestamp value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setDueDate("
                + value + "): was " + dueDate() );
        }
        takeStoredValueForKey( value, "dueDate" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>gradingSuspended</code> value.
     * @return the value of the attribute
     */
    public boolean gradingSuspended()
    {
        Number result =
            (Number)storedValueForKey( "gradingSuspended" );
        return ( result == null )
            ? false
            : ( result.intValue() > 0 );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>gradingSuspended</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setGradingSuspended( boolean value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setGradingSuspended("
                + value + "): was " + gradingSuspended() );
        }
        Number actual =
            er.extensions.ERXConstant.integerForInt( value ? 1 : 0 );
        setGradingSuspendedRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>gradingSuspended</code> value.
     * @return the value of the attribute
     */
    public Number gradingSuspendedRaw()
    {
        return (Number)storedValueForKey( "gradingSuspended" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>gradingSuspended</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setGradingSuspendedRaw( Number value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setGradingSuspendedRaw("
                + value + "): was " + gradingSuspendedRaw() );
        }
        takeStoredValueForKey( value, "gradingSuspended" );
    }


    //-- Local mutable cache --
    private net.sf.webcat.grader.graphs.AssignmentSummary graphSummaryCache;
    private NSData graphSummaryRawCache;

    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>graphSummary</code> value.
     * @return the value of the attribute
     */
    public net.sf.webcat.grader.graphs.AssignmentSummary graphSummary()
    {
    	NSData dbValue =
            (NSData)storedValueForKey( "graphSummary" );
        if ( graphSummaryRawCache != dbValue )
        {
            if ( dbValue != null && dbValue.equals( graphSummaryRawCache ) )
            {
                // They are still equal, so just update the raw cache
                graphSummaryRawCache = dbValue;
            }
            else
            {
                // Underlying attribute may have changed because
                // of a concurrent update through another editing
                // context, so throw away current values.
                graphSummaryRawCache = dbValue;
                net.sf.webcat.grader.graphs.AssignmentSummary newValue =
                    net.sf.webcat.grader.graphs.AssignmentSummary
                    .objectWithArchiveData( dbValue );
                if ( graphSummaryCache != null )
                {
                    graphSummaryCache.copyFrom( newValue );
                }
                else
                {
                    graphSummaryCache = newValue;
                }
                graphSummaryCache.setOwner( this );
                setUpdateMutableFields( true );
            }
        }
        else if ( dbValue == null && graphSummaryCache == null )
        {
            graphSummaryCache =
                net.sf.webcat.grader.graphs.AssignmentSummary
                .objectWithArchiveData( dbValue );
             graphSummaryCache.setOwner( this );
             setUpdateMutableFields( true );
        }
        return graphSummaryCache;
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>graphSummary</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setGraphSummary( net.sf.webcat.grader.graphs.AssignmentSummary value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setGraphSummary("
                + value + ")" );
        }
        if ( graphSummaryCache == null )
        {
            graphSummaryCache = value;
            value.setHasChanged( false );
            graphSummaryRawCache = value.archiveData();
            takeStoredValueForKey( graphSummaryRawCache, "graphSummary" );
        }
        else if ( graphSummaryCache != value )  // ( graphSummaryCache != null )
        {
            graphSummaryCache.copyFrom( value );
            setUpdateMutableFields( true );
        }
        else  // ( graphSummaryCache == non-null value )
        {
            // no nothing
        }
    }


    // ----------------------------------------------------------
    /**
     * Clear the value of this object's <code>graphSummary</code>
     * property.
     */
    public void clearGraphSummary()
    {
        if (log.isDebugEnabled())
        {
            log.debug( "clearGraphSummary()" );
        }
        takeStoredValueForKey( null, "graphSummary" );
        graphSummaryRawCache = null;
        graphSummaryCache = null;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>moodleId</code> value.
     * @return the value of the attribute
     */
    public Number moodleId()
    {
        return (Number)storedValueForKey( "moodleId" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>moodleId</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setMoodleId( Number value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setMoodleId("
                + value + "): was " + moodleId() );
        }
        takeStoredValueForKey( value, "moodleId" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>publish</code> value.
     * @return the value of the attribute
     */
    public boolean publish()
    {
        Number result =
            (Number)storedValueForKey( "publish" );
        return ( result == null )
            ? false
            : ( result.intValue() > 0 );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>publish</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setPublish( boolean value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setPublish("
                + value + "): was " + publish() );
        }
        Number actual =
            er.extensions.ERXConstant.integerForInt( value ? 1 : 0 );
        setPublishRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>publish</code> value.
     * @return the value of the attribute
     */
    public Number publishRaw()
    {
        return (Number)storedValueForKey( "publish" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>publish</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setPublishRaw( Number value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setPublishRaw("
                + value + "): was " + publishRaw() );
        }
        takeStoredValueForKey( value, "publish" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>updateMutableFields</code> value.
     * @return the value of the attribute
     */
    public boolean updateMutableFields()
    {
        Number result =
            (Number)storedValueForKey( "updateMutableFields" );
        return ( result == null )
            ? false
            : ( result.intValue() > 0 );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>updateMutableFields</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setUpdateMutableFields( boolean value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setUpdateMutableFields("
                + value + "): was " + updateMutableFields() );
        }
        Number actual =
            er.extensions.ERXConstant.integerForInt( value ? 1 : 0 );
        setUpdateMutableFieldsRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>updateMutableFields</code> value.
     * @return the value of the attribute
     */
    public Number updateMutableFieldsRaw()
    {
        return (Number)storedValueForKey( "updateMutableFields" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>updateMutableFields</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setUpdateMutableFieldsRaw( Number value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setUpdateMutableFieldsRaw("
                + value + "): was " + updateMutableFieldsRaw() );
        }
        takeStoredValueForKey( value, "updateMutableFields" );
    }


    // ----------------------------------------------------------
    /**
     * Called just before this object is saved to the database.
     */
    public void saveMutables()
    {
        log.debug("saveMutables()");
        if ( graphSummaryCache != null
            && graphSummaryCache.hasChanged() )
        {
            graphSummaryRawCache = graphSummaryCache.archiveData();
            takeStoredValueForKey( graphSummaryRawCache, "graphSummary" );
            graphSummaryCache.setHasChanged( false );
        }

        setUpdateMutableFields( false );
    }


    // ----------------------------------------------------------
    /**
     * Called just before this object is saved to the database.
     */
    public void willUpdate()
    {
        log.debug("willUpdate()");
        saveMutables();
        super.willUpdate();
    }


    // ----------------------------------------------------------
    /**
     * Called just before this object is inserted into the database.
     */
    public void willInsert()
    {
        log.debug("willInsert()");
        saveMutables();
        super.willInsert();
    }


    // ----------------------------------------------------------
    /**
     * Called when the object is invalidated.
     */
    public void flushCaches()
    {
        log.debug("flushCaches()");
        graphSummaryCache = null;
        graphSummaryRawCache  = null;
        setUpdateMutableFields( false );
        super.flushCaches();
    }


    // ----------------------------------------------------------
    /**
     * Called when an owned mutable container object is changed.
     */
    public void mutableContainerHasChanged()
    {
        setUpdateMutableFields( true );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>assignment</code>
     * relationship.
     * @return the entity in the relationship
     */
    public net.sf.webcat.grader.Assignment assignment()
    {
        return (net.sf.webcat.grader.Assignment)storedValueForKey( "assignment" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>assignment</code>
     * relationship (DO NOT USE--instead, use
     * <code>setAssignmentRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The new entity to relate to
     */
    public void setAssignment( net.sf.webcat.grader.Assignment value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setAssignment("
                + value + "): was " + assignment() );
        }
        takeStoredValueForKey( value, "assignment" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>assignment</code>
     * relationship.  This method is a type-safe version of
     * <code>addObjectToBothSidesOfRelationshipWithKey()</code>.
     *
     * @param value The new entity to relate to
     */
    public void setAssignmentRelationship(
        net.sf.webcat.grader.Assignment value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setAssignmentRelationship("
                + value + "): was " + assignment() );
        }
        if ( value == null )
        {
            net.sf.webcat.grader.Assignment object = assignment();
            if ( object != null )
                removeObjectFromBothSidesOfRelationshipWithKey( object, "assignment" );
        }
        else
        {
            addObjectToBothSidesOfRelationshipWithKey( value, "assignment" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>courseOffering</code>
     * relationship.
     * @return the entity in the relationship
     */
    public net.sf.webcat.core.CourseOffering courseOffering()
    {
        return (net.sf.webcat.core.CourseOffering)storedValueForKey( "courseOffering" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>courseOffering</code>
     * relationship (DO NOT USE--instead, use
     * <code>setCourseOfferingRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The new entity to relate to
     */
    public void setCourseOffering( net.sf.webcat.core.CourseOffering value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setCourseOffering("
                + value + "): was " + courseOffering() );
        }
        takeStoredValueForKey( value, "courseOffering" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>courseOffering</code>
     * relationship.  This method is a type-safe version of
     * <code>addObjectToBothSidesOfRelationshipWithKey()</code>.
     *
     * @param value The new entity to relate to
     */
    public void setCourseOfferingRelationship(
        net.sf.webcat.core.CourseOffering value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setCourseOfferingRelationship("
                + value + "): was " + courseOffering() );
        }
        if ( value == null )
        {
            net.sf.webcat.core.CourseOffering object = courseOffering();
            if ( object != null )
                removeObjectFromBothSidesOfRelationshipWithKey( object, "courseOffering" );
        }
        else
        {
            addObjectToBothSidesOfRelationshipWithKey( value, "courseOffering" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entities pointed to by the <code>submissions</code>
     * relationship.
     * @return an NSArray of the entities in the relationship
     */
    public NSArray submissions()
    {
        return (NSArray)storedValueForKey( "submissions" );
    }


    // ----------------------------------------------------------
    /**
     * Replace the list of entities pointed to by the
     * <code>submissions</code> relationship.
     *
     * @param value The new set of entities to relate to
     */
    public void setSubmissions( NSMutableArray value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setSubmissions("
                + value + "): was " + submissions() );
        }
        takeStoredValueForKey( value, "submissions" );
    }


    // ----------------------------------------------------------
    /**
     * Add a new entity to the <code>submissions</code>
     * relationship (DO NOT USE--instead, use
     * <code>addToSubmissionsRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The new entity to relate to
     */
    public void addToSubmissions( net.sf.webcat.grader.Submission value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "addToSubmissions("
                + value + "): was " + submissions() );
        }
        NSMutableArray array = (NSMutableArray)submissions();
        willChange();
        array.addObject( value );
    }


    // ----------------------------------------------------------
    /**
     * Remove a specific entity from the <code>submissions</code>
     * relationship (DO NOT USE--instead, use
     * <code>removeFromSubmissionsRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The entity to remove from the relationship
     */
    public void removeFromSubmissions( net.sf.webcat.grader.Submission value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "RemoveFromSubmissions("
                + value + "): was " + submissions() );
        }
        NSMutableArray array = (NSMutableArray)submissions();
        willChange();
        array.removeObject( value );
    }


    // ----------------------------------------------------------
    /**
     * Add a new entity to the <code>submissions</code>
     * relationship.
     *
     * @param value The new entity to relate to
     */
    public void addToSubmissionsRelationship( net.sf.webcat.grader.Submission value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "addToSubmissionsRelationship("
                + value + "): was " + submissions() );
        }
        addObjectToBothSidesOfRelationshipWithKey(
            value, "submissions" );
    }


    // ----------------------------------------------------------
    /**
     * Remove a specific entity from the <code>submissions</code>
     * relationship.
     *
     * @param value The entity to remove from the relationship
     */
    public void removeFromSubmissionsRelationship( net.sf.webcat.grader.Submission value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "removeFromSubmissionsRelationship("
                + value + "): was " + submissions() );
        }
        removeObjectFromBothSidesOfRelationshipWithKey(
            value, "submissions" );
    }


    // ----------------------------------------------------------
    /**
     * Create a brand new object that is a member of the
     * <code>submissions</code> relationship.
     *
     * @return The new entity
     */
    public net.sf.webcat.grader.Submission createSubmissionsRelationship()
    {
        if (log.isDebugEnabled())
        {
            log.debug( "createSubmissionsRelationship()" );
        }
        EOClassDescription eoClassDesc = EOClassDescription
            .classDescriptionForEntityName( "Submission" );
        EOEnterpriseObject eoObject = eoClassDesc
            .createInstanceWithEditingContext( editingContext(), null );
        editingContext().insertObject( eoObject );
        addObjectToBothSidesOfRelationshipWithKey(
            eoObject, "submissions" );
        return (net.sf.webcat.grader.Submission)eoObject;
    }


    // ----------------------------------------------------------
    /**
     * Remove and then delete a specific entity that is a member of the
     * <code>submissions</code> relationship.
     *
     * @param value The entity to remove from the relationship and then delete
     */
    public void deleteSubmissionsRelationship( net.sf.webcat.grader.Submission value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "deleteSubmissionsRelationship("
                + value + "): was " + submissions() );
        }
        removeObjectFromBothSidesOfRelationshipWithKey(
            value, "submissions" );
        editingContext().deleteObject( value );
    }


    // ----------------------------------------------------------
    /**
     * Remove (and then delete, if owned) all entities that are members of the
     * <code>submissions</code> relationship.
     */
    public void deleteAllSubmissionsRelationships()
    {
        if (log.isDebugEnabled())
        {
            log.debug( "deleteAllSubmissionsRelationships(): was "
                + submissions() );
        }
        Enumeration objects = submissions().objectEnumerator();
        while ( objects.hasMoreElements() )
            deleteSubmissionsRelationship(
                (net.sf.webcat.grader.Submission)objects.nextElement() );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve object according to the <code>AllOfferings</code>
     * fetch specification.
     *
     * @param context The editing context to use
     * @return an NSArray of the entities retrieved
     */
    public static NSArray objectsForAllOfferings(
            EOEditingContext context
        )
    {
        EOFetchSpecification spec = EOFetchSpecification
            .fetchSpecificationNamed( "allOfferings", "AssignmentOffering" );

        NSArray result = context.objectsWithFetchSpecification( spec );
        if (log.isDebugEnabled())
        {
            log.debug( "objectsForAllOfferings(ec"
            
                + "): " + result );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve object according to the <code>CourseOffering</code>
     * fetch specification.
     *
     * @param context The editing context to use
     * @param courseOfferingBinding fetch spec parameter
     * @return an NSArray of the entities retrieved
     */
    public static NSArray objectsForCourseOffering(
            EOEditingContext context,
            net.sf.webcat.core.CourseOffering courseOfferingBinding
        )
    {
        EOFetchSpecification spec = EOFetchSpecification
            .fetchSpecificationNamed( "courseOffering", "AssignmentOffering" );

        NSMutableDictionary bindings = new NSMutableDictionary();

        if ( courseOfferingBinding != null )
            bindings.setObjectForKey( courseOfferingBinding,
                                      "courseOffering" );
        spec = spec.fetchSpecificationWithQualifierBindings( bindings );

        NSArray result = context.objectsWithFetchSpecification( spec );
        if (log.isDebugEnabled())
        {
            log.debug( "objectsForCourseOffering(ec"
            
                + ", " + courseOfferingBinding
                + "): " + result );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve object according to the <code>Staff</code>
     * fetch specification.
     *
     * @param context The editing context to use
     * @param userBinding fetch spec parameter
     * @return an NSArray of the entities retrieved
     */
    public static NSArray objectsForStaff(
            EOEditingContext context,
            net.sf.webcat.core.User userBinding
        )
    {
        EOFetchSpecification spec = EOFetchSpecification
            .fetchSpecificationNamed( "staff", "AssignmentOffering" );

        NSMutableDictionary bindings = new NSMutableDictionary();

        if ( userBinding != null )
            bindings.setObjectForKey( userBinding,
                                      "user" );
        spec = spec.fetchSpecificationWithQualifierBindings( bindings );

        NSArray result = context.objectsWithFetchSpecification( spec );
        if (log.isDebugEnabled())
        {
            log.debug( "objectsForStaff(ec"
            
                + ", " + userBinding
                + "): " + result );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve object according to the <code>Student</code>
     * fetch specification.
     *
     * @param context The editing context to use
     * @param userBinding fetch spec parameter
     * @return an NSArray of the entities retrieved
     */
    public static NSArray objectsForStudent(
            EOEditingContext context,
            net.sf.webcat.core.User userBinding
        )
    {
        EOFetchSpecification spec = EOFetchSpecification
            .fetchSpecificationNamed( "student", "AssignmentOffering" );

        NSMutableDictionary bindings = new NSMutableDictionary();

        if ( userBinding != null )
            bindings.setObjectForKey( userBinding,
                                      "user" );
        spec = spec.fetchSpecificationWithQualifierBindings( bindings );

        NSArray result = context.objectsWithFetchSpecification( spec );
        if (log.isDebugEnabled())
        {
            log.debug( "objectsForStudent(ec"
            
                + ", " + userBinding
                + "): " + result );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve object according to the <code>SubmitterEngineBase</code>
     * fetch specification.
     *
     * @param context The editing context to use
     * @return an NSArray of the entities retrieved
     */
    public static NSArray objectsForSubmitterEngineBase(
            EOEditingContext context
        )
    {
        EOFetchSpecification spec = EOFetchSpecification
            .fetchSpecificationNamed( "submitterEngineBase", "AssignmentOffering" );

        NSArray result = context.objectsWithFetchSpecification( spec );
        if (log.isDebugEnabled())
        {
            log.debug( "objectsForSubmitterEngineBase(ec"
            
                + "): " + result );
        }
        return result;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( AssignmentOffering.class );
}
