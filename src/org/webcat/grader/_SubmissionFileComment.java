/*==========================================================================*\
 |  _SubmissionFileComment.java
 |*-------------------------------------------------------------------------*|
 |  Created by eogenerator
 |  DO NOT EDIT.  Make changes to SubmissionFileComment.java instead.
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
 * SubmissionFileComment.java.
 *
 * @author Generated by eogenerator
 * @version version suppressed to control auto-generation
 */
public abstract class _SubmissionFileComment
    extends er.extensions.eof.ERXGenericRecord
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new _SubmissionFileComment object.
     */
    public _SubmissionFileComment()
    {
        super();
    }


    // ----------------------------------------------------------
    /**
     * A static factory method for creating a new
     * SubmissionFileComment object given required
     * attributes and relationships.
     * @param editingContext The context in which the new object will be
     * inserted
     * @param limitExceededValue
     * @return The newly created object
     */
    public static SubmissionFileComment create(
        EOEditingContext editingContext,
        boolean limitExceededValue
        )
    {
        SubmissionFileComment eoObject = (SubmissionFileComment)
            EOUtilities.createAndInsertInstance(
                editingContext,
                _SubmissionFileComment.ENTITY_NAME);
        eoObject.setLimitExceeded(limitExceededValue);
        return eoObject;
    }


    // ----------------------------------------------------------
    /**
     * Get a local instance of the given object in another editing context.
     * @param editingContext The target editing context
     * @param eo The object to import
     * @return An instance of the given object in the target editing context
     */
    public static SubmissionFileComment localInstance(
        EOEditingContext editingContext, SubmissionFileComment eo)
    {
        return (eo == null)
            ? null
            : (SubmissionFileComment)EOUtilities.localInstanceOfObject(
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
    public static SubmissionFileComment forId(
        EOEditingContext ec, int id )
    {
        SubmissionFileComment obj = null;
        if (id > 0)
        {
            NSArray<SubmissionFileComment> objects =
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
    public static SubmissionFileComment forId(
        EOEditingContext ec, String id )
    {
        return forId( ec, er.extensions.foundation.ERXValueUtilities.intValue( id ) );
    }


    //~ Constants (for key names) .............................................

    // Attributes ---
    public static final String CATEGORY_NO_KEY = "categoryNo";
    public static final ERXKey<Integer> categoryNo =
        new ERXKey<Integer>(CATEGORY_NO_KEY);
    public static final String DEDUCTION_KEY = "deduction";
    public static final ERXKey<Double> deduction =
        new ERXKey<Double>(DEDUCTION_KEY);
    public static final String FILE_NAME_KEY = "fileName";
    public static final ERXKey<String> fileName =
        new ERXKey<String>(FILE_NAME_KEY);
    public static final String GROUP_NAME_KEY = "groupName";
    public static final ERXKey<String> groupName =
        new ERXKey<String>(GROUP_NAME_KEY);
    public static final String LIMIT_EXCEEDED_KEY = "limitExceeded";
    public static final ERXKey<Integer> limitExceeded =
        new ERXKey<Integer>(LIMIT_EXCEEDED_KEY);
    public static final String LINE_NO_KEY = "lineNo";
    public static final ERXKey<Integer> lineNo =
        new ERXKey<Integer>(LINE_NO_KEY);
    public static final String MESSAGE_KEY = "message";
    public static final ERXKey<String> message =
        new ERXKey<String>(MESSAGE_KEY);
    public static final String TO_NO_KEY = "toNo";
    public static final ERXKey<Integer> toNo =
        new ERXKey<Integer>(TO_NO_KEY);
    // To-one relationships ---
    public static final String AUTHOR_KEY = "author";
    public static final ERXKey<org.webcat.core.User> author =
        new ERXKey<org.webcat.core.User>(AUTHOR_KEY);
    public static final String SUBMISSION_FILE_STATS_KEY = "submissionFileStats";
    public static final ERXKey<org.webcat.grader.SubmissionFileStats> submissionFileStats =
        new ERXKey<org.webcat.grader.SubmissionFileStats>(SUBMISSION_FILE_STATS_KEY);
    // To-many relationships ---
    // Fetch specifications ---
    public static final String ENTITY_NAME = "SubmissionFileComment";

    public transient final EOBasedKeyGenerator generateKey =
        new EOBasedKeyGenerator(this);


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Get a local instance of this object in another editing context.
     * @param editingContext The target editing context
     * @return An instance of this object in the target editing context
     */
    public SubmissionFileComment localInstance(EOEditingContext editingContext)
    {
        return (SubmissionFileComment)EOUtilities.localInstanceOfObject(
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
     * Retrieve this object's <code>categoryNo</code> value.
     * @return the value of the attribute
     */
    public byte categoryNo()
    {
        Integer returnValue =
            (Integer)storedValueForKey( "categoryNo" );
        return ( returnValue == null )
            ? 0
            : returnValue.byteValue();
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>categoryNo</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setCategoryNo( byte value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setCategoryNo("
                + value + "): was " + categoryNo() );
        }
        Integer actual =
            er.extensions.eof.ERXConstant.integerForInt( value );
            setCategoryNoRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>categoryNo</code> value.
     * @return the value of the attribute
     */
    public Integer categoryNoRaw()
    {
        return (Integer)storedValueForKey( "categoryNo" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>categoryNo</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setCategoryNoRaw( Integer value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setCategoryNoRaw("
                + value + "): was " + categoryNoRaw() );
        }
        takeStoredValueForKey( value, "categoryNo" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>deduction</code> value.
     * @return the value of the attribute
     */
    public double deduction()
    {
        Double returnValue =
            (Double)storedValueForKey( "deduction" );
        return ( returnValue == null )
            ? 0.0
            : returnValue.doubleValue();
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>deduction</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setDeduction( double value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setDeduction("
                + value + "): was " + deduction() );
        }
        Double actual =
            new Double( value );
            setDeductionRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>deduction</code> value.
     * @return the value of the attribute
     */
    public Double deductionRaw()
    {
        return (Double)storedValueForKey( "deduction" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>deduction</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setDeductionRaw( Double value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setDeductionRaw("
                + value + "): was " + deductionRaw() );
        }
        takeStoredValueForKey( value, "deduction" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>fileName</code> value.
     * @return the value of the attribute
     */
    public String fileName()
    {
        return (String)storedValueForKey( "fileName" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>fileName</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setFileName( String value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setFileName("
                + value + "): was " + fileName() );
        }
        takeStoredValueForKey( value, "fileName" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>groupName</code> value.
     * @return the value of the attribute
     */
    public String groupName()
    {
        return (String)storedValueForKey( "groupName" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>groupName</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setGroupName( String value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setGroupName("
                + value + "): was " + groupName() );
        }
        takeStoredValueForKey( value, "groupName" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>limitExceeded</code> value.
     * @return the value of the attribute
     */
    public boolean limitExceeded()
    {
        Integer returnValue =
            (Integer)storedValueForKey( "limitExceeded" );
        return ( returnValue == null )
            ? false
            : ( returnValue.intValue() > 0 );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>limitExceeded</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setLimitExceeded( boolean value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setLimitExceeded("
                + value + "): was " + limitExceeded() );
        }
        Integer actual =
            er.extensions.eof.ERXConstant.integerForInt( value ? 1 : 0 );
            setLimitExceededRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>limitExceeded</code> value.
     * @return the value of the attribute
     */
    public Integer limitExceededRaw()
    {
        return (Integer)storedValueForKey( "limitExceeded" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>limitExceeded</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setLimitExceededRaw( Integer value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setLimitExceededRaw("
                + value + "): was " + limitExceededRaw() );
        }
        takeStoredValueForKey( value, "limitExceeded" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>lineNo</code> value.
     * @return the value of the attribute
     */
    public int lineNo()
    {
        Integer returnValue =
            (Integer)storedValueForKey( "lineNo" );
        return ( returnValue == null )
            ? 0
            : returnValue.intValue();
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>lineNo</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setLineNo( int value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setLineNo("
                + value + "): was " + lineNo() );
        }
        Integer actual =
            er.extensions.eof.ERXConstant.integerForInt( value );
            setLineNoRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>lineNo</code> value.
     * @return the value of the attribute
     */
    public Integer lineNoRaw()
    {
        return (Integer)storedValueForKey( "lineNo" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>lineNo</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setLineNoRaw( Integer value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setLineNoRaw("
                + value + "): was " + lineNoRaw() );
        }
        takeStoredValueForKey( value, "lineNo" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>message</code> value.
     * @return the value of the attribute
     */
    public String message()
    {
        return (String)storedValueForKey( "message" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>message</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setMessage( String value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setMessage("
                + value + "): was " + message() );
        }
        takeStoredValueForKey( value, "message" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>toNo</code> value.
     * @return the value of the attribute
     */
    public byte toNo()
    {
        Integer returnValue =
            (Integer)storedValueForKey( "toNo" );
        return ( returnValue == null )
            ? 0
            : returnValue.byteValue();
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>toNo</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setToNo( byte value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setToNo("
                + value + "): was " + toNo() );
        }
        Integer actual =
            er.extensions.eof.ERXConstant.integerForInt( value );
            setToNoRaw( actual );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>toNo</code> value.
     * @return the value of the attribute
     */
    public Integer toNoRaw()
    {
        return (Integer)storedValueForKey( "toNo" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>toNo</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setToNoRaw( Integer value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setToNoRaw("
                + value + "): was " + toNoRaw() );
        }
        takeStoredValueForKey( value, "toNo" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>author</code>
     * relationship.
     * @return the entity in the relationship
     */
    public org.webcat.core.User author()
    {
        return (org.webcat.core.User)storedValueForKey( "author" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>author</code>
     * relationship (DO NOT USE--instead, use
     * <code>setAuthorRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The new entity to relate to
     */
    public void setAuthor( org.webcat.core.User value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setAuthor("
                + value + "): was " + author() );
        }
        takeStoredValueForKey( value, "author" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>author</code>
     * relationship.  This method is a type-safe version of
     * <code>addObjectToBothSidesOfRelationshipWithKey()</code>.
     *
     * @param value The new entity to relate to
     */
    public void setAuthorRelationship(
        org.webcat.core.User value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setAuthorRelationship("
                + value + "): was " + author() );
        }
        if ( value == null )
        {
            org.webcat.core.User object = author();
            if ( object != null )
                removeObjectFromBothSidesOfRelationshipWithKey( object, "author" );
        }
        else
        {
            addObjectToBothSidesOfRelationshipWithKey( value, "author" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>submissionFileStats</code>
     * relationship.
     * @return the entity in the relationship
     */
    public org.webcat.grader.SubmissionFileStats submissionFileStats()
    {
        return (org.webcat.grader.SubmissionFileStats)storedValueForKey( "submissionFileStats" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>submissionFileStats</code>
     * relationship (DO NOT USE--instead, use
     * <code>setSubmissionFileStatsRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The new entity to relate to
     */
    public void setSubmissionFileStats( org.webcat.grader.SubmissionFileStats value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setSubmissionFileStats("
                + value + "): was " + submissionFileStats() );
        }
        takeStoredValueForKey( value, "submissionFileStats" );
    }


    // ----------------------------------------------------------
    /**
     * Set the entity pointed to by the <code>submissionFileStats</code>
     * relationship.  This method is a type-safe version of
     * <code>addObjectToBothSidesOfRelationshipWithKey()</code>.
     *
     * @param value The new entity to relate to
     */
    public void setSubmissionFileStatsRelationship(
        org.webcat.grader.SubmissionFileStats value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setSubmissionFileStatsRelationship("
                + value + "): was " + submissionFileStats() );
        }
        if ( value == null )
        {
            org.webcat.grader.SubmissionFileStats object = submissionFileStats();
            if ( object != null )
                removeObjectFromBothSidesOfRelationshipWithKey( object, "submissionFileStats" );
        }
        else
        {
            addObjectToBothSidesOfRelationshipWithKey( value, "submissionFileStats" );
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
    public static NSArray<SubmissionFileComment> objectsWithFetchSpecification(
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
    public static NSArray<SubmissionFileComment> allObjects(
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
    public static NSArray<SubmissionFileComment> objectsMatchingQualifier(
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
    public static NSArray<SubmissionFileComment> objectsMatchingQualifier(
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
    public static SubmissionFileComment firstObjectMatchingQualifier(
        EOEditingContext context,
        EOQualifier qualifier,
        NSArray<EOSortOrdering> sortOrderings)
    {
        NSArray<SubmissionFileComment> objects =
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
    public static SubmissionFileComment uniqueObjectMatchingQualifier(
        EOEditingContext context,
        EOQualifier qualifier) throws EOUtilities.MoreThanOneException
    {
        NSArray<SubmissionFileComment> objects =
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
    public static NSArray<SubmissionFileComment> objectsMatchingValues(
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
    public static NSArray<SubmissionFileComment> objectsMatchingValues(
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
    public static SubmissionFileComment firstObjectMatchingValues(
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
    public static SubmissionFileComment firstObjectMatchingValues(
        EOEditingContext context,
        NSArray<EOSortOrdering> sortOrderings,
        NSDictionary<String, Object> keysAndValues)
    {
        EOFetchSpecification fspec = new EOFetchSpecification(
            ENTITY_NAME,
            EOQualifier.qualifierToMatchAllValues(keysAndValues),
            sortOrderings);
        fspec.setFetchLimit(1);

        NSArray<SubmissionFileComment> objects =
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
    public static SubmissionFileComment uniqueObjectMatchingValues(
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
    public static SubmissionFileComment uniqueObjectMatchingValues(
        EOEditingContext context,
        NSDictionary<String, Object> keysAndValues)
        throws EOUtilities.MoreThanOneException
    {
        try
        {
            return (SubmissionFileComment)EOUtilities.objectMatchingValues(
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

    static Logger log = Logger.getLogger( SubmissionFileComment.class );
}
