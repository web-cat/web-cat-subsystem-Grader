/*==========================================================================*\
 |  _StepConfig.java
 |*-------------------------------------------------------------------------*|
 |  Created by eogenerator
 |  DO NOT EDIT.  Make changes to StepConfig.java instead.
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
 * StepConfig.java.
 *
 * @author Generated by eogenerator
 * @version version suppressed to control auto-generation
 */
public abstract class _StepConfig
    extends er.extensions.ERXGenericRecord
    implements net.sf.webcat.core.MutableContainer.MutableContainerOwner
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new _StepConfig object.
     */
    public _StepConfig()
    {
        super();
    }


    // ----------------------------------------------------------
    /**
     * A static factory method for creating a new
     * _StepConfig object given required
     * attributes and relationships.
     * @param editingContext The context in which the new object will be
     * inserted
     * @param updateMutableFields
     * @return The newly created object
     */
    public static StepConfig create(
        EOEditingContext editingContext,
        boolean updateMutableFields
        )
    {
        StepConfig eoObject = (StepConfig)
            EOUtilities.createAndInsertInstance(
                editingContext,
                _StepConfig.ENTITY_NAME);
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
    public static StepConfig localInstance(
        EOEditingContext editingContext, StepConfig eo)
    {
        return (eo == null)
            ? null
            : (StepConfig)EOUtilities.localInstanceOfObject(
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
    public static StepConfig forId(
        EOEditingContext ec, int id )
    {
        StepConfig obj = null;
        if (id > 0)
        {
            NSArray results = EOUtilities.objectsMatchingKeyAndValue( ec,
                ENTITY_NAME, "id", new Integer( id ) );
            if ( results != null && results.count() > 0 )
            {
                obj = (StepConfig)results.objectAtIndex( 0 );
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
    public static StepConfig forId(
        EOEditingContext ec, String id )
    {
        return forId( ec, er.extensions.ERXValueUtilities.intValue( id ) );
    }


    //~ Constants (for key names) .............................................

    // Attributes ---
    public static final String CONFIG_SETTINGS_KEY = "configSettings";
    public static final String NAME_KEY = "name";
    public static final String UPDATE_MUTABLE_FIELDS_KEY = "updateMutableFields";
    // To-one relationships ---
    public static final String AUTHOR_KEY = "author";
    // To-many relationships ---
    public static final String STEPS_KEY = "steps";
    // Fetch specifications ---
    public static final String COURSE_AND_SCRIPT_FSPEC = "courseAndScript";
    public static final String USER_FSPEC = "user";
    public static final String ENTITY_NAME = "StepConfig";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Get a local instance of this object in another editing context.
     * @param editingContext The target editing context
     * @return An instance of this object in the target editing context
     */
    public StepConfig localInstance(EOEditingContext editingContext)
    {
        return (StepConfig)EOUtilities.localInstanceOfObject(
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

    //-- Local mutable cache --
    private net.sf.webcat.core.MutableDictionary configSettingsCache;
    private NSData configSettingsRawCache;

    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>configSettings</code> value.
     * @return the value of the attribute
     */
    public net.sf.webcat.core.MutableDictionary configSettings()
    {
    	NSData dbValue =
            (NSData)storedValueForKey( "configSettings" );
        if ( configSettingsRawCache != dbValue )
        {
            if ( dbValue != null && dbValue.equals( configSettingsRawCache ) )
            {
                // They are still equal, so just update the raw cache
                configSettingsRawCache = dbValue;
            }
            else
            {
                // Underlying attribute may have changed because
                // of a concurrent update through another editing
                // context, so throw away current values.
                configSettingsRawCache = dbValue;
                net.sf.webcat.core.MutableDictionary newValue =
                    net.sf.webcat.core.MutableDictionary
                    .objectWithArchiveData( dbValue );
                if ( configSettingsCache != null )
                {
                    configSettingsCache.copyFrom( newValue );
                }
                else
                {
                    configSettingsCache = newValue;
                }
                configSettingsCache.setOwner( this );
                setUpdateMutableFields( true );
            }
        }
        else if ( dbValue == null && configSettingsCache == null )
        {
            configSettingsCache =
                net.sf.webcat.core.MutableDictionary
                .objectWithArchiveData( dbValue );
             configSettingsCache.setOwner( this );
             setUpdateMutableFields( true );
        }
        return configSettingsCache;
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>configSettings</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setConfigSettings( net.sf.webcat.core.MutableDictionary value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setConfigSettings("
                + value + ")" );
        }
        if ( configSettingsCache == null )
        {
            configSettingsCache = value;
            value.setHasChanged( false );
            configSettingsRawCache = value.archiveData();
            takeStoredValueForKey( configSettingsRawCache, "configSettings" );
        }
        else if ( configSettingsCache != value )  // ( configSettingsCache != null )
        {
            configSettingsCache.copyFrom( value );
            setUpdateMutableFields( true );
        }
        else  // ( configSettingsCache == non-null value )
        {
            // no nothing
        }
    }


    // ----------------------------------------------------------
    /**
     * Clear the value of this object's <code>configSettings</code>
     * property.
     */
    public void clearConfigSettings()
    {
        if (log.isDebugEnabled())
        {
            log.debug( "clearConfigSettings()" );
        }
        takeStoredValueForKey( null, "configSettings" );
        configSettingsRawCache = null;
        configSettingsCache = null;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>name</code> value.
     * @return the value of the attribute
     */
    public String name()
    {
        return (String)storedValueForKey( "name" );
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>name</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setName( String value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setName("
                + value + "): was " + name() );
        }
        takeStoredValueForKey( value, "name" );
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
        if ( configSettingsCache != null
            && configSettingsCache.hasChanged() )
        {
            configSettingsRawCache = configSettingsCache.archiveData();
            takeStoredValueForKey( configSettingsRawCache, "configSettings" );
            configSettingsCache.setHasChanged( false );
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
        configSettingsCache = null;
        configSettingsRawCache  = null;
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
     * Retrieve the entity pointed to by the <code>author</code>
     * relationship.
     * @return the entity in the relationship
     */
    public net.sf.webcat.core.User author()
    {
        return (net.sf.webcat.core.User)storedValueForKey( "author" );
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
    public void setAuthor( net.sf.webcat.core.User value )
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
        net.sf.webcat.core.User value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setAuthorRelationship("
                + value + "): was " + author() );
        }
        if ( value == null )
        {
            net.sf.webcat.core.User object = author();
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
     * Retrieve the entities pointed to by the <code>steps</code>
     * relationship.
     * @return an NSArray of the entities in the relationship
     */
    public NSArray steps()
    {
        return (NSArray)storedValueForKey( "steps" );
    }


    // ----------------------------------------------------------
    /**
     * Replace the list of entities pointed to by the
     * <code>steps</code> relationship.
     *
     * @param value The new set of entities to relate to
     */
    public void setSteps( NSMutableArray value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "setSteps("
                + value + "): was " + steps() );
        }
        takeStoredValueForKey( value, "steps" );
    }


    // ----------------------------------------------------------
    /**
     * Add a new entity to the <code>steps</code>
     * relationship (DO NOT USE--instead, use
     * <code>addToStepsRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The new entity to relate to
     */
    public void addToSteps( net.sf.webcat.grader.Step value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "addToSteps("
                + value + "): was " + steps() );
        }
        NSMutableArray array = (NSMutableArray)steps();
        willChange();
        array.addObject( value );
    }


    // ----------------------------------------------------------
    /**
     * Remove a specific entity from the <code>steps</code>
     * relationship (DO NOT USE--instead, use
     * <code>removeFromStepsRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The entity to remove from the relationship
     */
    public void removeFromSteps( net.sf.webcat.grader.Step value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "RemoveFromSteps("
                + value + "): was " + steps() );
        }
        NSMutableArray array = (NSMutableArray)steps();
        willChange();
        array.removeObject( value );
    }


    // ----------------------------------------------------------
    /**
     * Add a new entity to the <code>steps</code>
     * relationship.
     *
     * @param value The new entity to relate to
     */
    public void addToStepsRelationship( net.sf.webcat.grader.Step value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "addToStepsRelationship("
                + value + "): was " + steps() );
        }
        addObjectToBothSidesOfRelationshipWithKey(
            value, "steps" );
    }


    // ----------------------------------------------------------
    /**
     * Remove a specific entity from the <code>steps</code>
     * relationship.
     *
     * @param value The entity to remove from the relationship
     */
    public void removeFromStepsRelationship( net.sf.webcat.grader.Step value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "removeFromStepsRelationship("
                + value + "): was " + steps() );
        }
        removeObjectFromBothSidesOfRelationshipWithKey(
            value, "steps" );
    }


    // ----------------------------------------------------------
    /**
     * Create a brand new object that is a member of the
     * <code>steps</code> relationship.
     *
     * @return The new entity
     */
    public net.sf.webcat.grader.Step createStepsRelationship()
    {
        if (log.isDebugEnabled())
        {
            log.debug( "createStepsRelationship()" );
        }
        EOClassDescription eoClassDesc = EOClassDescription
            .classDescriptionForEntityName( "Step" );
        EOEnterpriseObject eoObject = eoClassDesc
            .createInstanceWithEditingContext( editingContext(), null );
        editingContext().insertObject( eoObject );
        addObjectToBothSidesOfRelationshipWithKey(
            eoObject, "steps" );
        return (net.sf.webcat.grader.Step)eoObject;
    }


    // ----------------------------------------------------------
    /**
     * Remove and then delete a specific entity that is a member of the
     * <code>steps</code> relationship.
     *
     * @param value The entity to remove from the relationship and then delete
     */
    public void deleteStepsRelationship( net.sf.webcat.grader.Step value )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "deleteStepsRelationship("
                + value + "): was " + steps() );
        }
        removeObjectFromBothSidesOfRelationshipWithKey(
            value, "steps" );
        editingContext().deleteObject( value );
    }


    // ----------------------------------------------------------
    /**
     * Remove (and then delete, if owned) all entities that are members of the
     * <code>steps</code> relationship.
     */
    public void deleteAllStepsRelationships()
    {
        if (log.isDebugEnabled())
        {
            log.debug( "deleteAllStepsRelationships(): was "
                + steps() );
        }
        Enumeration objects = steps().objectEnumerator();
        while ( objects.hasMoreElements() )
            deleteStepsRelationship(
                (net.sf.webcat.grader.Step)objects.nextElement() );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve object according to the <code>CourseAndScript</code>
     * fetch specification.
     *
     * @param context The editing context to use
     * @param scriptFileBinding fetch spec parameter
     * @param courseBinding fetch spec parameter
     * @return an NSArray of the entities retrieved
     */
    public static NSArray objectsForCourseAndScript(
            EOEditingContext context,
            net.sf.webcat.grader.ScriptFile scriptFileBinding,
            net.sf.webcat.core.Course courseBinding
        )
    {
        EOFetchSpecification spec = EOFetchSpecification
            .fetchSpecificationNamed( "courseAndScript", "StepConfig" );

        NSMutableDictionary bindings = new NSMutableDictionary();

        if ( scriptFileBinding != null )
            bindings.setObjectForKey( scriptFileBinding,
                                      "scriptFile" );
        if ( courseBinding != null )
            bindings.setObjectForKey( courseBinding,
                                      "course" );
        spec = spec.fetchSpecificationWithQualifierBindings( bindings );

        NSArray result = context.objectsWithFetchSpecification( spec );
        if (log.isDebugEnabled())
        {
            log.debug( "objectsForCourseAndScript(ec"
            
                + ", " + scriptFileBinding
                + ", " + courseBinding
                + "): " + result );
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve object according to the <code>User</code>
     * fetch specification.
     *
     * @param context The editing context to use
     * @param userBinding fetch spec parameter
     * @return an NSArray of the entities retrieved
     */
    public static NSArray objectsForUser(
            EOEditingContext context,
            net.sf.webcat.core.User userBinding
        )
    {
        EOFetchSpecification spec = EOFetchSpecification
            .fetchSpecificationNamed( "user", "StepConfig" );

        NSMutableDictionary bindings = new NSMutableDictionary();

        if ( userBinding != null )
            bindings.setObjectForKey( userBinding,
                                      "user" );
        spec = spec.fetchSpecificationWithQualifierBindings( bindings );

        NSArray result = context.objectsWithFetchSpecification( spec );
        if (log.isDebugEnabled())
        {
            log.debug( "objectsForUser(ec"
            
                + ", " + userBinding
                + "): " + result );
        }
        return result;
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( StepConfig.class );
}
