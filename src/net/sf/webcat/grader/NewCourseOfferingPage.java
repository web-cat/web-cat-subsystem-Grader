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

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import net.sf.webcat.core.*;

//-------------------------------------------------------------------------
/**
 * Allows the user to create a new course offering.
 *
 *  @author Stephen Edwards
 *  @version $Id$
 */
public class NewCourseOfferingPage
    extends GraderCourseEditComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new TBDPage object.
     *
     * @param context The context to use
     */
    public NewCourseOfferingPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public Course               course;
    public WODisplayGroup       courseDisplayGroup;
    public NSArray              institutions;
    public AuthenticationDomain institution;
    public AuthenticationDomain anInstitution;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    public void appendToResponse( WOResponse response, WOContext context )
    {
        if (institutions == null)
        {
            institutions = AuthenticationDomain.authDomains();
            institution = user().authenticationDomain();
        }
        if (institution == null)
        {
            courseDisplayGroup.setQualifier( null );
        }
        else
        {
            courseDisplayGroup.setQualifier( new EOKeyValueQualifier(
                Course.iNSTITUTION_KEY,
                EOQualifier.QualifierOperatorEqual,
                institution
                ));
        }
        courseDisplayGroup.updateDisplayedObjects();
        if ( coreSelections().courseOffering() != null )
        {
            coreSelections().setCourseRelationship(
                coreSelections().courseOffering().course() );
        }
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        // When semester list changes, make sure not to take the
        // default action, which is to click "next".
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Create a new course offering object and move on to an edit page.
     * @see net.sf.webcat.core.WCComponent#next()
     */
    public WOComponent next()
    {
        if (coreSelections().course() == null)
        {
            error( "Please select a course." );
            return null;
        }
        CourseOffering newOffering = new CourseOffering();
        localContext().insertObject( newOffering );
        newOffering.setCourseRelationship( coreSelections().course() );
        // TODO: use date-based search instead of just creation ordering
        NSArray semesters = EOUtilities.objectsForEntityNamed(
                        localContext(), Semester.ENTITY_NAME );
        newOffering.setSemesterRelationship(
                        (Semester)semesters.lastObject() );
        newOffering.addToInstructorsRelationship( user() );
        // wcSession().setCourseOfferingRelationship( newOffering );
        setCourseOffering(newOffering);
        return super.next();
    }
}
