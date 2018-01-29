/*==========================================================================*\
 |  $Id: NavTestPage.java,v 1.3 2013/08/11 02:10:10 stedwar2 Exp $
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

import com.webobjects.appserver.*;
import org.apache.log4j.Logger;
import org.webcat.woextensions.WCEC;

//-------------------------------------------------------------------------
/**
* Represents a standard Web-CAT page that has not yet been implemented
* (is "to be defined").
*
*  @author  Stephen Edwards
 * @author  Latest changes by: $Author: stedwar2 $
 * @version $Revision: 1.3 $, $Date: 2013/08/11 02:10:10 $
*/
public class NavTestPage
    extends GraderAssignmentComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new NavTestPage object.
     *
     * @param context The context to use
     */
    public NavTestPage( WOContext context )
    {
        super( context );
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug("entering appendToResponse()");
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public void awake()
    {
        log.debug("entering awake()");
        super.awake();
        log.debug("leaving awake()");
    }


    // ----------------------------------------------------------
    public WOComponent refresh()
    {
    	return null;
    }


    // ----------------------------------------------------------
    public int count()
    {
    	return ++count;
    }


    // ----------------------------------------------------------
    public String testOutput;
    public WOComponent testMap()
    {
        testOutput = "beginning test.\n";
        WCEC ec1 = null;
        WCEC ec2 = null;
        try
        {
            ec1 = WCEC.newEditingContext();
            ec1.lock();

            StepConfig sc1 = StepConfig.create(ec1, false);
            sc1.configSettings().takeValueForKey("Entry1", "key1");
            log.debug("sc1 changed properties = " + sc1.changedProperties());
            ec1.saveChanges();
            log.debug("sc1 saved");
            sc1.configSettings().takeValueForKey("Entry2", "key2");
            log.debug("sc1 changed properties = " + sc1.changedProperties());
            log.debug("sc1 changes = " + sc1.changesFromCommittedSnapshot());
            log.debug("sc1 config settings = " + sc1.configSettings());
            log.debug("sc1 changes UNSAVED");
            int sc1Id = sc1.id().intValue();

            ec2 = WCEC.newEditingContext();
            ec2.lock();
            StepConfig sc2 = StepConfig.forId(ec2, sc1Id);
            log.debug("sc2 changed properties = " + sc2.changedProperties());
            log.debug("sc2 changes = " + sc2.changesFromCommittedSnapshot());
            log.debug("sc2 config settings = " + sc2.configSettings());
            sc2.configSettings().takeValueForKey("Entry3", "key3");
            log.debug("sc2 changed properties = " + sc2.changedProperties());
            log.debug("sc2 changes = " + sc2.changesFromCommittedSnapshot());
            log.debug("sc2 config settings = " + sc2.configSettings());
            ec2.saveChanges();
            log.debug("sc2 saved");

            log.debug("sc1 changed properties = " + sc1.changedProperties());
            log.debug("sc1 changes = " + sc1.changesFromCommittedSnapshot());
            log.debug("sc1 config settings = " + sc1.configSettings());

            sc2.delete();
            ec2.saveChanges();
            log.debug("sc1/sc2 deleted");
        }
        finally
        {
            if (ec1 != null)
            {
                ec1.unlock();
                ec1.dispose();
                ec1 = null;
            }
            if (ec2 != null)
            {
                ec2.unlock();
                ec2.dispose();
                ec2 = null;
            }
        }
        return null;
    }


    //~ Instance/static variables .............................................

    private int count = 0;
    static Logger log = Logger.getLogger(NavTestPage.class);
}
