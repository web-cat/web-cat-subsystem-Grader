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

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import net.sf.webcat.core.*;

// -------------------------------------------------------------------------
/**
 * Custom settings for a single grading {@link Step}.
 *
 * @author stedwar2
 * @version $Id$
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
            NSArray steps = steps();
            if ( steps.count() > 0 )
            {
                Step step = (Step)steps.objectAtIndex( 0 );
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
    public net.sf.webcat.core.User author()
    {
        net.sf.webcat.core.User author = super.author();
        if ( author == null )
        {
            NSArray steps = steps();
            for ( int i = 0; i < steps.count(); i++ )
            {
                Step step = (Step)steps.objectAtIndex( i );
                author = step.assignment().author();
                if ( author != null )
                {
                    super.setAuthor( author );
                    break;
                }
            }
        }
        return author;
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
    public static NSArray configsForUserAndCourseScriptIncludingMine(
            EOEditingContext context,
            net.sf.webcat.core.User userBinding,
            net.sf.webcat.grader.ScriptFile scriptFileBinding,
            net.sf.webcat.core.Course courseBinding,
            StepConfig mine
        )
    {
        // Have to use two separate queries here, since the join required
        // in the second query will overly restrict the results of the first!
        NSMutableArray results = objectsForUser( context, userBinding )
            .mutableClone();
        er.extensions.ERXArrayUtilities.addObjectsFromArrayWithoutDuplicates(
            results, 
            objectsForCourseAndScript(
                context, scriptFileBinding, courseBinding )
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
}
