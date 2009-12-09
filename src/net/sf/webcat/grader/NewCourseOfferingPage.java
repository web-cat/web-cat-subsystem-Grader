/*==========================================================================*\
 |  $Id$
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

package net.sf.webcat.grader;

import org.apache.log4j.Logger;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import net.sf.webcat.core.*;

//-------------------------------------------------------------------------
/**
 * Allows the user to create a new course offering.
 *
 * @author Stephen Edwards
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class NewCourseOfferingPage
    extends GraderCourseEditComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new object.
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
    public NSArray<AuthenticationDomain> institutions;
    public AuthenticationDomain institution;
    public AuthenticationDomain anInstitution;
    public Semester            semester;
    public NSArray<Semester>   semesters;
    public Semester            aSemester;
    public String              crn;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    public void appendToResponse(WOResponse response, WOContext context)
    {
        if ( semesters == null )
        {
            semesters =
                Semester.allObjectsOrderedByStartDate(localContext());
        }
        if (institutions == null)
        {
            institutions = AuthenticationDomain.authDomains();
            institution = user().authenticationDomain();
        }
        if (institution == null)
        {
            courseDisplayGroup.setQualifier(null);
        }
        else
        {
            courseDisplayGroup.setQualifier(new EOKeyValueQualifier(
                Course.iNSTITUTION_KEY,
                EOQualifier.QualifierOperatorEqual,
                institution
                ));
        }
        courseDisplayGroup.updateDisplayedObjects();
        if (coreSelections().courseOffering() != null)
        {
            coreSelections().setCourseRelationship(
                coreSelections().courseOffering().course());
        }
        super.appendToResponse(response, context);
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
            error("Please select a course.");
            return null;
        }
        if (crn == null || crn.length() == 0)
        {
            error("Please enter a CRN (unique identifier) for your course offering.");
            return null;
        }
        CourseOffering newOffering = new CourseOffering();
        localContext().insertObject(newOffering);
        newOffering.setCourseRelationship(coreSelections().course());
        newOffering.setSemesterRelationship(semester);
        newOffering.addToInstructorsRelationship(user());
        newOffering.setCrn(crn);
        setCourseOffering(newOffering);
        apply();
        return super.next();
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger(NewCourseOfferingPage.class);
}
