/*==========================================================================*\
 |  _EnqueuedJob.java
 |*-------------------------------------------------------------------------*|
 |  Created by eogenerator
 |  DO NOT EDIT.  Make changes to EnqueuedJob.java instead.
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
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXKey;
import org.apache.log4j.Logger;
import org.webcat.core.EOBasedKeyGenerator;

// -------------------------------------------------------------------------
/**
 * An automatically generated EOGenericRecord subclass.  DO NOT EDIT.
 * To change, use EOModeler, or make additions in
 * EnqueuedJob.java.
 *
 * @author Generated by eogenerator
 * @version version suppressed to control auto-generation
 */
public abstract class _EnqueuedJob
    extends er.extensions.eof.ERXGenericRecord
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new _EnqueuedJob object.
     */
    public _EnqueuedJob()
    {
        super();
    }


    // ----------------------------------------------------------
    /**
     * A static factory method for creating a new
     * EnqueuedJob object given required
     * attributes and relationships.
     * @param editingContext The context in which the new object will be
     * inserted
     * @param discardedValue
     * @param pausedValue
     * @param regradingValue
     * @return The newly created object
     */
    public static EnqueuedJob create(
        EOEditingContext editingContext,
        boolean discardedValue,
        boolean pausedValue,
        boolean regradingValue
        )
    {
        EnqueuedJob eoObject = (EnqueuedJob)
            EOUtilities.createAndInsertInstance(
                editingContext,
                _EnqueuedJob.ENTITY_NAME);
        eoObject.setDiscarded(discardedValue);
        eoObject.setPaused(pausedValue);
        eoObject.setRegrading(regradingValue);
        return eoObject;
    }


    // ----------------------------------------------------------
    /**
     * Get a local instance of the given object in another editing context.
     * @param editingContext The target editing context
     * @param eo The object to import
     * @return An instance of the given object in the target editing context
     */
    public static EnqueuedJob localInstance(
        EOEditingContext editingContext, EnqueuedJob eo)
    {
        return (eo == null)
            ? null
            : (EnqueuedJob)EOUtilities.localInstanceOfObject(
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
    public static EnqueuedJob forId(
        EOEditingContext ec, int id )
    {
        EnqueuedJob obj = null;
        if (id > 0)
        {
            NSArray<EnqueuedJob> objects =
                objectsMatchingValues(ec, "id", new Integer(id));
            if (objects != null && objects.count() > 0)
            {
                obj = objects.objectAtIndex(0);
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
    public static EnqueuedJob forId(
        EOEditingContext ec, String id )
    {
        return forId( ec, er.extensions.foundation.ERXValueUtilities.intValue( id ) );
    }


    //~ Constants (for key names) .............................................

    // Attributes ---
    public static final String DISCARDED_KEY = "discarded";
    public static final ERXKey<Integer> discarded =
        new ERXKey<Integer>(DISCARDED_KEY);
    public static final String PAUSED_KEY = "paused";
    public static final ERXKey<Integer> paused =
        new ERXKey<Integer>(PAUSED_KEY);
    public static final String QUEUE_TIME_KEY = "queueTime";
    public static final ERXKey<NSTimestamp> queueTime =
        new ERXKey<NSTimestamp>(QUEUE_TIME_KEY);
    public static final String REGRADING_KEY = "regrading";
    public static final ERXKey<Integer> regrading =
        new ERXKey<Integer>(REGRADING_KEY);
    // To-one relationships ---
    public static final String SUBMISSION_KEY = "submission";
    public static final ERXKey<org.webcat.grader.Submission> submission =
        new ERXKey<org.webcat.grader.Submission>(SUBMISSION_KEY);
    // To-many relationships ---
    // Fetch specifications ---
    public static final String ENTITY_NAME = "EnqueuedJob";

    public final EOBasedKeyGenerator generateKey = new EOBasedKeyGenerator(this);


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Get a local instance of this object in another editing context.
     * @param editingContext The target editing context
     * @return An instance of this object in the target editing context
     */
    public EnqueuedJob localInstance(EOEditingContext editingContext)
    {
        return (EnqueuedJob)EOUtilities.localInstanceOfObject(
            editingContext, this);
    }


    // ----------------------------------------------------------
    /**
     * Get a list of changes between this object's current state and the
     * last committed version.
     * @return a dictionary of the changes that have not yet been committed
     */
    @SuppressWarnings("unchecked")
    public NSDictionary<String, Object> changedProperties()
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
            return er.extensions.eof.ERXConstant.ZeroInteger;
        }
    }

    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>discarded</code> value.
     * @return the value of the attribute
     */
    public boolean discarded()
    {
        Integer returnValue =
            (Integer)storedValueForKey( "discarded" );
        return ( returnValue == null )
            ? false
            : ( returnValue.intValue() > 0 );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>discarded</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setDiscarded( boolean value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setDiscarded("
                + value + "): was " + discarded() );
        }
        Integer actual =
            er.extensions.eof.ERXConstant.integerForInt( value ? 1 : 0 );
            setDiscardedRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>discarded</code> value.
     * @return the value of the attribute
     */
    public Integer discardedRaw()
    {
        return (Integer)storedValueForKey( "discarded" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>discarded</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setDiscardedRaw( Integer value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setDiscardedRaw("
                + value + "): was " + discardedRaw() );
        }
        takeStoredValueForKey( value, "discarded" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>paused</code> value.
     * @return the value of the attribute
     */
    public boolean paused()
    {
        Integer returnValue =
            (Integer)storedValueForKey( "paused" );
        return ( returnValue == null )
            ? false
            : ( returnValue.intValue() > 0 );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>paused</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setPaused( boolean value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setPaused("
                + value + "): was " + paused() );
        }
        Integer actual =
            er.extensions.eof.ERXConstant.integerForInt( value ? 1 : 0 );
            setPausedRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>paused</code> value.
     * @return the value of the attribute
     */
    public Integer pausedRaw()
    {
        return (Integer)storedValueForKey( "paused" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>paused</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setPausedRaw( Integer value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setPausedRaw("
                + value + "): was " + pausedRaw() );
        }
        takeStoredValueForKey( value, "paused" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>queueTime</code> value.
     * @return the value of the attribute
     */
    public NSTimestamp queueTime()
    {
        return (NSTimestamp)storedValueForKey( "queueTime" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>queueTime</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setQueueTime( NSTimestamp value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setQueueTime("
                + value + "): was " + queueTime() );
        }
        takeStoredValueForKey( value, "queueTime" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>regrading</code> value.
     * @return the value of the attribute
     */
    public boolean regrading()
    {
        Integer returnValue =
            (Integer)storedValueForKey( "regrading" );
        return ( returnValue == null )
            ? false
            : ( returnValue.intValue() > 0 );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>regrading</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setRegrading( boolean value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setRegrading("
                + value + "): was " + regrading() );
        }
        Integer actual =
            er.extensions.eof.ERXConstant.integerForInt( value ? 1 : 0 );
            setRegradingRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>regrading</code> value.
     * @return the value of the attribute
     */
    public Integer regradingRaw()
    {
        return (Integer)storedValueForKey( "regrading" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>regrading</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setRegradingRaw( Integer value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setRegradingRaw("
                + value + "): was " + regradingRaw() );
        }
        takeStoredValueForKey( value, "regrading" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>submission</code>
     * relationship.
     * @return the entity in the relationship
     */
    public org.webcat.grader.Submission submission()
    {
        return (org.webcat.grader.Submission)storedValueForKey( "submission" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>submission</code>
     * relationship (DO NOT USE--instead, use
     * <code>setSubmissionRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The new entity to relate to
     */
    public void setSubmission( org.webcat.grader.Submission value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setSubmission("
                + value + "): was " + submission() );
        }
        takeStoredValueForKey( value, "submission" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>submission</code>
     * relationship.  This method is a type-safe version of
     * <code>addObjectToBothSidesOfRelationshipWithKey()</code>.
     *
     * @param value The new entity to relate to
     */
    public void setSubmissionRelationship(
        org.webcat.grader.Submission value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setSubmissionRelationship("
                + value + "): was " + submission() );
        }
        if ( value == null )
        {
            org.webcat.grader.Submission object = submission();
            if ( object != null )
                removeObjectFromBothSidesOfRelationshipWithKey( object, "submission" );
        }
        else
        {
            addObjectToBothSidesOfRelationshipWithKey( value, "submission" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve objects using a fetch specification.
     *
     * @param context The editing context to use
     * @param fspec The fetch specification to use
     *
     * @return an NSArray of the entities retrieved
     */
    @SuppressWarnings("unchecked")
    public static NSArray<EnqueuedJob> objectsWithFetchSpecification(
        EOEditingContext context,
        EOFetchSpecification fspec)
    {
        return context.objectsWithFetchSpecification(fspec);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve all objects of this type.
     *
     * @param context The editing context to use
     *
     * @return an NSArray of the entities retrieved
     */
    public static NSArray<EnqueuedJob> allObjects(
        EOEditingContext context)
    {
        return objectsMatchingQualifier(context, null, null);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve objects using a qualifier.
     *
     * @param context The editing context to use
     * @param qualifier The qualifier to use
     *
     * @return an NSArray of the entities retrieved
     */
    public static NSArray<EnqueuedJob> objectsMatchingQualifier(
        EOEditingContext context,
        EOQualifier qualifier)
    {
        return objectsMatchingQualifier(context, qualifier, null);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve objects using a qualifier and sort orderings.
     *
     * @param context The editing context to use
     * @param qualifier The qualifier to use
     * @param sortOrderings The sort orderings to use
     *
     * @return an NSArray of the entities retrieved
     */
    public static NSArray<EnqueuedJob> objectsMatchingQualifier(
        EOEditingContext context,
        EOQualifier qualifier,
        NSArray<EOSortOrdering> sortOrderings)
    {
        EOFetchSpecification fspec = new EOFetchSpecification(
            ENTITY_NAME, qualifier, sortOrderings);
        fspec.setUsesDistinct(true);
        return objectsWithFetchSpecification(context, fspec);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the first object that matches a qualifier, when
     * sorted with the specified sort orderings.
     *
     * @param context The editing context to use
     * @param qualifier The qualifier to use
     * @param sortOrderings the sort orderings
     *
     * @return the first entity that was retrieved, or null if there was none
     */
    public static EnqueuedJob firstObjectMatchingQualifier(
        EOEditingContext context,
        EOQualifier qualifier,
        NSArray<EOSortOrdering> sortOrderings)
    {
        NSArray<EnqueuedJob> objects =
            objectsMatchingQualifier(context, qualifier, sortOrderings);
        return (objects.size() > 0)
            ? objects.get(0)
            : null;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve a single object using a list of keys and values to match.
     *
     * @param context The editing context to use
     * @param qualifier The qualifier to use
     *
     * @return the single entity that was retrieved
     *
     * @throws EOUtilities.MoreThanOneException
     *     if there is more than one matching object
     */
    public static EnqueuedJob uniqueObjectMatchingQualifier(
        EOEditingContext context,
        EOQualifier qualifier) throws EOUtilities.MoreThanOneException
    {
        NSArray<EnqueuedJob> objects =
            objectsMatchingQualifier(context, qualifier);
        if (objects.size() > 1)
        {
            throw new EOUtilities.MoreThanOneException(null);
        }
        return (objects.size() > 0)
            ? objects.get(0)
            : null;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve objects using a list of keys and values to match.
     *
     * @param context The editing context to use
     * @param keysAndValues a list of keys and values to match, alternating
     *     "key", "value", "key", "value"...
     *
     * @return an NSArray of the entities retrieved
     */
    public static NSArray<EnqueuedJob> objectsMatchingValues(
        EOEditingContext context,
        Object... keysAndValues)
    {
        if (keysAndValues.length % 2 != 0)
        {
            throw new IllegalArgumentException("There should a value " +
                "corresponding to every key that was passed.");
        }

        NSMutableDictionary<String, Object> valueDictionary =
            new NSMutableDictionary<String, Object>();

        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            Object key = keysAndValues[i];
            Object value = keysAndValues[i + 1];

            if (!(key instanceof String))
            {
                throw new IllegalArgumentException("Keys should be strings.");
            }

            valueDictionary.setObjectForKey(value, key);
        }

        return objectsMatchingValues(context, valueDictionary);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve objects using a dictionary of keys and values to match.
     *
     * @param context The editing context to use
     * @param keysAndValues a dictionary of keys and values to match
     *
     * @return an NSArray of the entities retrieved
     */
    @SuppressWarnings("unchecked")
    public static NSArray<EnqueuedJob> objectsMatchingValues(
        EOEditingContext context,
        NSDictionary<String, Object> keysAndValues)
    {
        return EOUtilities.objectsMatchingValues(context, ENTITY_NAME,
            keysAndValues);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the first object that matches a set of keys and values, when
     * sorted with the specified sort orderings.
     *
     * @param context The editing context to use
     * @param sortOrderings the sort orderings
     * @param keysAndValues a list of keys and values to match, alternating
     *     "key", "value", "key", "value"...
     *
     * @return the first entity that was retrieved, or null if there was none
     */
    public static EnqueuedJob firstObjectMatchingValues(
        EOEditingContext context,
        NSArray<EOSortOrdering> sortOrderings,
        Object... keysAndValues)
    {
        if (keysAndValues.length % 2 != 0)
        {
            throw new IllegalArgumentException("There should a value " +
                "corresponding to every key that was passed.");
        }

        NSMutableDictionary<String, Object> valueDictionary =
            new NSMutableDictionary<String, Object>();

        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            Object key = keysAndValues[i];
            Object value = keysAndValues[i + 1];

            if (!(key instanceof String))
            {
                throw new IllegalArgumentException("Keys should be strings.");
            }

            valueDictionary.setObjectForKey(value, key);
        }

        return firstObjectMatchingValues(
            context, sortOrderings, valueDictionary);
    }


    // ----------------------------------------------------------
    /**
     * Retrieves the first object that matches a set of keys and values, when
     * sorted with the specified sort orderings.
     *
     * @param context The editing context to use
     * @param sortOrderings the sort orderings
     * @param keysAndValues a dictionary of keys and values to match
     *
     * @return the first entity that was retrieved, or null if there was none
     */
    public static EnqueuedJob firstObjectMatchingValues(
        EOEditingContext context,
        NSArray<EOSortOrdering> sortOrderings,
        NSDictionary<String, Object> keysAndValues)
    {
        EOFetchSpecification fspec = new EOFetchSpecification(
            ENTITY_NAME,
            EOQualifier.qualifierToMatchAllValues(keysAndValues),
            sortOrderings);
        fspec.setFetchLimit(1);

        NSArray<EnqueuedJob> objects =
            objectsWithFetchSpecification( context, fspec );

        if ( objects.count() == 0 )
        {
            return null;
        }
        else
        {
            return objects.objectAtIndex(0);
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve a single object using a list of keys and values to match.
     *
     * @param context The editing context to use
     * @param keysAndValues a list of keys and values to match, alternating
     *     "key", "value", "key", "value"...
     *
     * @return the single entity that was retrieved, or null if there was none
     *
     * @throws EOUtilities.MoreThanOneException
     *     if there is more than one matching object
     */
    public static EnqueuedJob uniqueObjectMatchingValues(
        EOEditingContext context,
        Object... keysAndValues) throws EOUtilities.MoreThanOneException
    {
        if (keysAndValues.length % 2 != 0)
        {
            throw new IllegalArgumentException("There should a value " +
                "corresponding to every key that was passed.");
        }

        NSMutableDictionary<String, Object> valueDictionary =
            new NSMutableDictionary<String, Object>();

        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            Object key = keysAndValues[i];
            Object value = keysAndValues[i + 1];

            if (!(key instanceof String))
            {
                throw new IllegalArgumentException("Keys should be strings.");
            }

            valueDictionary.setObjectForKey(value, key);
        }

        return uniqueObjectMatchingValues(context, valueDictionary);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve an object using a dictionary of keys and values to match.
     *
     * @param context The editing context to use
     * @param keysAndValues a dictionary of keys and values to match
     *
     * @return the single entity that was retrieved, or null if there was none
     *
     * @throws EOUtilities.MoreThanOneException
     *     if there is more than one matching object
     */
    public static EnqueuedJob uniqueObjectMatchingValues(
        EOEditingContext context,
        NSDictionary<String, Object> keysAndValues)
        throws EOUtilities.MoreThanOneException
    {
        try
        {
            return (EnqueuedJob)EOUtilities.objectMatchingValues(
                context, ENTITY_NAME, keysAndValues);
        }
        catch (EOObjectNotAvailableException e)
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the count of all objects of this type.
     *
     * @param context The editing context to use
     *
     * @return the count of all objects
     */
    public static int countOfAllObjects(EOEditingContext context)
    {
        return countOfObjectsMatchingQualifier(context, null);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the count of objects that match a qualifier.
     *
     * @param context The editing context to use
     * @param qualifier The qualifier to use
     *
     * @return the count of objects matching the qualifier
     */
    public static int countOfObjectsMatchingQualifier(
        EOEditingContext context, EOQualifier qualifier)
    {
        return ERXEOControlUtilities.objectCountWithQualifier(
                context, ENTITY_NAME, qualifier);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the count of objects using a list of keys and values to match.
     *
     * @param context The editing context to use
     * @param keysAndValues a list of keys and values to match, alternating
     *     "key", "value", "key", "value"...
     *
     * @return the count of objects that match the specified values
     */
    public static int countOfObjectsMatchingValues(
        EOEditingContext context,
        Object... keysAndValues)
    {
        if (keysAndValues.length % 2 != 0)
        {
            throw new IllegalArgumentException("There should a value " +
                "corresponding to every key that was passed.");
        }

        NSMutableDictionary<String, Object> valueDictionary =
            new NSMutableDictionary<String, Object>();

        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            Object key = keysAndValues[i];
            Object value = keysAndValues[i + 1];

            if (!(key instanceof String))
            {
                throw new IllegalArgumentException("Keys should be strings.");
            }

            valueDictionary.setObjectForKey(value, key);
        }

        return countOfObjectsMatchingValues(context, valueDictionary);
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the count of objects using a dictionary of keys and values to
     * match.
     *
     * @param context The editing context to use
     * @param keysAndValues a dictionary of keys and values to match
     *
     * @return the count of objects that matched the specified values
     */
    public static int countOfObjectsMatchingValues(
        EOEditingContext context,
        NSDictionary<String, Object> keysAndValues)
    {
        return countOfObjectsMatchingQualifier(context,
                EOQualifier.qualifierToMatchAllValues(keysAndValues));
    }


    // ----------------------------------------------------------
    /**
     * Produce a string representation of this object.  This implementation
     * calls UserPresentableDescription(), which uses WebObjects' internal
     * mechanism to print out the visible fields of this object.  Normally,
     * subclasses would override userPresentableDescription() to change
     * the way the object is printed.
     *
     * @return A string representation of the object's value
     */
    public String toString()
    {
        return userPresentableDescription();
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( EnqueuedJob.class );
}
