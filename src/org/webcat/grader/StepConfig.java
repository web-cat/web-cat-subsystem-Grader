/*==========================================================================*\
 |  $Id: StepConfig.java,v 1.3 2013/08/11 02:06:33 stedwar2 Exp $
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

import org.webcat.core.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.foundation.ERXArrayUtilities;
import org.webcat.core.MutableDictionary;

// -------------------------------------------------------------------------
/**
 * Custom settings for a single grading {@link Step}.
 *
 * @author  Stephen Edwards
 * @author  Latest changes by: $Author: stedwar2 $
 * @version $Revision: 1.3 $, $Date: 2013/08/11 02:06:33 $
 */
public class StepConfig
    extends _StepConfig
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new StepConfig object.
     */
    public StepConfig()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Retrieve this object's <code>name</code> value.
     * @return the value of the attribute
     */
    public String name()
    {
        String result = super.name();
        if ( result == null )
        {
            NSArray<Step> mySteps = steps();
            if ( mySteps.count() > 0 )
            {
                Step step = mySteps.objectAtIndex( 0 );
                result = step.assignment().titleString() + " config";
                super.setName( result );
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the entity pointed to by the <code>author</code>
     * relationship.
     * @return the entity in the relationship
     */
    public User author()
    {
        User myAuthor = super.author();
        if ( myAuthor == null )
        {
            for (Step step : steps())
            {
                myAuthor = step.assignment().author();
                if ( myAuthor != null )
                {
                    super.setAuthor( myAuthor );
                    break;
                }
            }
        }
        return myAuthor;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve all of the unique step config objects that are either
     * authored by the given user, or are associated with the given script
     * in some assignment in some offering of the given course.
     *
     * @param context The editing context to use
     * @param userBinding fetch spec parameter
     * @param scriptFileBinding fetch spec parameter
     * @param courseBinding fetch spec parameter
     * @param mine An additional step config to include--probably
     * one that wouldn't be picked up by the fetch specification, say because
     * it has been added but not yet committed.  This parameter is only
     * added to the result if it is not already in the results of the fetch.
     * @return an NSArray of the entities retrieved
     */
    public static NSArray<StepConfig>
        configsForUserAndCourseScriptIncludingMine(
            EOEditingContext context,
            org.webcat.core.User userBinding,
            org.webcat.grader.GradingPlugin scriptFileBinding,
            org.webcat.core.Course courseBinding,
            StepConfig mine
        )
    {
        // Have to use two separate queries here, since the join required
        // in the second query will overly restrict the results of the first!
        NSMutableArray<StepConfig> results =
            stepConfigsForUser( context, userBinding ).mutableClone();
        ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
            results,
            stepConfigsForCourseAndScript(
                context, courseBinding, scriptFileBinding )
            );
        if ( mine != null && !results.containsObject( mine ) )
        {
            results.addObject( mine );
        }
        return results;
    }


    // ----------------------------------------------------------
    /**
     * Create a copy of this configuration object containing all the
     * same configuration settings (but none of the relationships).
     * @param context the context in which the copy should be inserted
     * @return the new StepConfig object
     */
    public StepConfig createCopy( EOEditingContext context )
    {
        StepConfig result = new StepConfig();
        context.insertObject( result );
        // Note that calling clone this way affects the parent
        // links in any embedded mutable containers (they get transferred
        // to the clone, and no longer report changes to the original).
        result.setConfigSettings(
            (MutableDictionary)this.configSettings().clone() );
        return result;
    }


    // ----------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Override
    public NSDictionary changesFromSnapshot(NSDictionary snapshot)
    {
        NSMutableDictionary result =
            super.changesFromSnapshot(snapshot).mutableClone();
        if (result.containsKey(UPDATE_MUTABLE_FIELDS_KEY))
        {
            MutableDictionary myConfigSettings = configSettings();
            Object snapshotConfigSettings =
                snapshot.valueForKey(CONFIG_SETTINGS_KEY);
            if ((myConfigSettings == null &&
                 !MutableDictionary.NullValue.equals(snapshotConfigSettings))
                || (myConfigSettings != null &&
                    !myConfigSettings.equals(snapshotConfigSettings)))
            {
                result.takeValueForKey(myConfigSettings, CONFIG_SETTINGS_KEY);
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    @Override
    public void updateFromSnapshot(NSDictionary<String, Object> snapshot)
    {
        System.out.println(
            "updating StepConfig " + this + " from snapshot " + snapshot);
        super.updateFromSnapshot(snapshot);
    }
}
