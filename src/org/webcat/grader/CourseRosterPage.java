/*==========================================================================*\
 |  $Id: CourseRosterPage.java,v 1.5 2014/11/07 13:55:03 stedwar2 Exp $
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

import org.apache.log4j.Logger;
import org.webcat.core.User;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EONotQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSData;
import er.extensions.appserver.ERXDisplayGroup;
import er.extensions.batching.ERXBatchingDisplayGroup;

// -------------------------------------------------------------------------
/**
 * This class displays a list of users enrolled in a selected course and
 * allows new users to be added.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.5 $, $Date: 2014/11/07 13:55:03 $
 */
public class CourseRosterPage
    extends GraderCourseEditComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public CourseRosterPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public ERXDisplayGroup<User> studentDisplayGroup;
    public ERXBatchingDisplayGroup<User> notStudentDisplayGroup;
    /** student in the worepetition */
    public User           student;
    /** index in the worepetition */
    public int            index;

    public String         filePath;
    public NSData         data;
    public boolean        manuallyAdding = false;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        if (log.isDebugEnabled())
        {
            log.debug("context = " + localContext().getClass().getName());
            log.debug("shared context = "
                + localContext().sharedEditingContext());
        }
        // Set up student list filters
        studentDisplayGroup.setObjectArray( courseOffering().students() );

        if ( firstLoad )
        {
            notStudentDisplayGroup.setQualifier( new EONotQualifier(
                new EOKeyValueQualifier(
                    User.ENROLLED_IN_KEY,
                    EOQualifier.QualifierOperatorContains,
                    courseOffering()
                ) ) );
            notStudentDisplayGroup.queryMatch().takeValueForKey(
                courseOffering().course().department().institution()
                .propertyName(),
                "authenticationDomain.propertyName" );
            firstLoad = false;
        }
//        if (manuallyAdding)
//        {
            notStudentDisplayGroup.fetch();
//        }
            super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    protected void afterAppendToResponse(WOResponse response, WOContext context)
    {
        super.afterAppendToResponse(response, context);
        oldBatchSize1  = studentDisplayGroup.numberOfObjectsPerBatch();
        oldBatchIndex1 = studentDisplayGroup.currentBatchIndex();
        oldBatchSize2  = notStudentDisplayGroup.numberOfObjectsPerBatch();
        oldBatchIndex2 = notStudentDisplayGroup.currentBatchIndex();
        if ( log.isDebugEnabled() )
        {
            log.debug( "unenrolled student filters = "
                + notStudentDisplayGroup.queryMatch() );
        }
    }


    // ----------------------------------------------------------
    public WOComponent upload()
    {
        if (  filePath != null
           && !filePath.equals( "" )
           && data != null
           && data.length() > 0 )
        {
            UploadRosterPage page = (UploadRosterPage)pageWithName(
                UploadRosterPage.class.getName() );
            page.nextPage = this;
            page.filePath = filePath;
            page.data     = data;
            return page;
        }
        else
        {
            error( "Please select a (non-empty) CSV file to upload." );
            return null;
        }
    }


    // ----------------------------------------------------------
    public WOComponent defaultAction()
    {
        log.debug( "defaultAction()" );
        if (  filePath != null
           && !filePath.equals( "" )
           && data != null
           && data.length() > 0 )
        {
            return upload();
        }
        if (    oldBatchSize1  != studentDisplayGroup.numberOfObjectsPerBatch()
             || oldBatchIndex1 != studentDisplayGroup.currentBatchIndex()
             || oldBatchSize2  != notStudentDisplayGroup.numberOfObjectsPerBatch()
             || oldBatchIndex2 != notStudentDisplayGroup.currentBatchIndex() )
        {
            return null;
        }
        else
        {
            return super.defaultAction();
        }
    }


    // ----------------------------------------------------------
    public WOComponent save()
    {
        apply();
        return super.next();
    }


    // ----------------------------------------------------------
    public WOComponent cancel()
    {
        clearMessages();
        return super.cancel();
//        cancelLocalChanges();
//        return super.next();
    }


    // ----------------------------------------------------------
    /**
     * Remove the selected student.
     * @return always null
     */
    public WOComponent removeStudent()
    {
        courseOffering().removeFromStudentsRelationship(student);
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Add the selected student.
     * @return always null
     */
    public WOComponent addStudent()
    {
        courseOffering().addToStudentsRelationship( student );
        manuallyAdding = true;
        return null;
    }


    // ----------------------------------------------------------
    public String forceManuallyAdding()
    {
        manuallyAdding = true;
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Toggle the manuallyAdding KVC property.
     * @return always null
     */
    public WOComponent toggleManuallyAdding()
    {
        manuallyAdding = !manuallyAdding;
        return null;
    }


    //~ Instance/static variables .............................................

    /** Saves the state of the student batch navigator to detect setting
     * changes. */
    protected int oldBatchSize1;
    /** Saves the state of the student batch navigator to detect setting
     * changes. */
    protected int oldBatchIndex1;
    /** Saves the state of the staff batch navigator to detect setting
     * changes. */
    protected int oldBatchSize2;
    /** Saves the state of the staff batch navigator to detect setting
     * changes. */
    protected int oldBatchIndex2;

    private boolean firstLoad = true;

    static Logger log = Logger.getLogger( CourseRosterPage.class );
}
