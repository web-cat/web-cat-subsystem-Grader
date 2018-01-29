/*==========================================================================*\
 |  $Id: BlueJSubmitterDefinitions.java,v 1.5 2013/08/11 02:09:46 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2011 Virginia Tech
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

package org.webcat.grader.actions;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.foundation.ERXValueUtilities;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.core.messaging.UnexpectedExceptionMessage;
import org.webcat.grader.*;
import org.webcat.woextensions.WCEC;

//-------------------------------------------------------------------------
/**
 * This page generates an assignment definition set published for
 * the BlueJ submitter extension.
 *
 * @author Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.5 $, $Date: 2013/08/11 02:09:46 $
 */
public class BlueJSubmitterDefinitions
    extends WOComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new BlueJSubmitterDefinitions page.
     *
     * @param context The context for this page
     */
    public BlueJSubmitterDefinitions( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public AssignmentOffering          anAssignmentOffering;
    public NSArray<AssignmentOffering> assignmentsToDisplay;
    public boolean                     groupByInstitution = false;
    public boolean                     groupByCRN         = false;
    public int                         index;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "appendToResponse()" );
        groupByInstitution = false;
        groupByCRN = false;
        includePatternsIndex = -1;
        excludePatternsIndex = -1;
        requirePatternsIndex = -1;
        currentTime = new NSTimestamp();
        EOEditingContext ec = WCEC.newEditingContext();
        try
        {
            ec.lock();
            groupByCRN = ERXValueUtilities.booleanValue(
                context().request().formValueForKey( "groupByCRN" ) )
                || ( context().request().formValueForKey( "crns" ) != null );
            assignmentsToDisplay =
                AssignmentOffering.objectsForSubmitterEngine(
                    ec,
                    context().request().formValues(),
                    currentTime,
                    groupByCRN,
                    showAll(),
                    preserveDateDifferences()
                    );
            if ( assignmentsToDisplay != null &&
                 assignmentsToDisplay.count() > 1 )
            {
                AssignmentOffering first =
                    assignmentsToDisplay.objectAtIndex( 0 );
                AssignmentOffering last =
                    assignmentsToDisplay.objectAtIndex(
                        assignmentsToDisplay.count() - 1 );
                groupByInstitution =
                    first.courseOffering().course().department().institution()
                  != last.courseOffering().course().department().institution();
            }
            response.setHeader( mimeType(), "content-type" );
            super.appendToResponse( response, context );
        }
        finally
        {
            ec.unlock();
            ec.dispose();
        }
    }


    // ----------------------------------------------------------
    /* (non-Javadoc)
     * @see com.webobjects.appserver.WOComponent#isStateless()
     */
    public boolean isStateless()
    {
        return true;
    }


    // ----------------------------------------------------------
    public boolean showAll()
    {
        return false;
    }


    // ----------------------------------------------------------
    public boolean preserveDateDifferences()
    {
        return false;
    }


    // ----------------------------------------------------------
    public boolean startNewInstitution()
    {
        boolean result = index == 0;
        if ( !result )
        {
            AssignmentOffering prev =
                assignmentsToDisplay.objectAtIndex(index - 1);
            result = prev.courseOffering().course().department().institution()
                != anAssignmentOffering.courseOffering().course().department()
                    .institution();
        }
        return result;
    }


    // ----------------------------------------------------------
    public boolean startNewCourse()
    {
        boolean result = index == 0;
        if ( !result )
        {
            AssignmentOffering prev =
                assignmentsToDisplay.objectAtIndex(index - 1);
            result = groupByCRN
                ? ( prev.courseOffering()
                    != anAssignmentOffering.courseOffering() )
                : ( prev.courseOffering().course()
                    != anAssignmentOffering.courseOffering().course() );
        }
        return result;
    }


    // ----------------------------------------------------------
    public String courseName()
    {
        String name;
        if ( groupByCRN )
        {
            name = anAssignmentOffering.courseOffering().compactName();
        }
        else
        {
            name =
                anAssignmentOffering.courseOffering().course().deptNumber();
        }
        return escapeBareName( name );
    }


    // ----------------------------------------------------------
    public String assignmentName()
    {
        String name = anAssignmentOffering.assignment().titleString();
        if ( name == null )
        {
            name = "(missing name)";
        }
        if ( !anAssignmentOffering.publish() )
        {
            name += " (not published)";
        }
        if ( currentTime.after( anAssignmentOffering.lateDeadline() ) )
        {
            name += " (closed)"; // " (by permission only)";
        }
        return escapeBareName( name );
    }


    // ----------------------------------------------------------
    public String assignmentNameParameter()
    {
        return escapeURLParameter( anAssignmentOffering.assignment().name() );
    }


    // ----------------------------------------------------------
    public String authenticator()
    {
        String base = anAssignmentOffering.courseOffering().course()
            .department().institution().propertyName();
        int pos = base.indexOf( '.' );
        if ( pos > 0 )
        {
            base = base.substring( pos + 1 );
        }
        return escapeURLParameter( base );
    }


    // ----------------------------------------------------------
    public String url()
    {
        return Application.completeURLWithRequestHandlerKey(
            context(),
            Application.application().directActionRequestHandlerKey(),
            "submit",
            null,
            useSecureSubmissionURLs(),
            0,
            !useSecureSubmissionURLs()
            );
        // unfortunately, we can't force HTTPS here, since not all
        // installations will necessarily support SSL
    }


    // ----------------------------------------------------------
    public boolean useSecureSubmissionURLs()
    {
        return true;
    }


    // ----------------------------------------------------------
    public String mimeType()
    {
        return "text/plain";
    }


    // ----------------------------------------------------------
    public String escapeBareName( String name )
    {
        return name;
    }


    // ----------------------------------------------------------
    public String escapeURLParameter( String name )
    {
        if ( name == null ) return name;
        try
        {
            return java.net.URLEncoder.encode( name, "UTF-8" );
        }
        catch ( java.io.UnsupportedEncodingException e )
        {
            new UnexpectedExceptionMessage(e, context(), null,
                "For url parameter: '" + name + "'").send();
            return name;
        }
    }


    // ----------------------------------------------------------
    public boolean isNotFirstInCourse()
    {
        if ( index == 0 ) return false;
        AssignmentOffering prev =
            assignmentsToDisplay.objectAtIndex( index - 1 );
        return prev.courseOffering().course()
            == anAssignmentOffering.courseOffering().course();
    }


    // ----------------------------------------------------------
    public boolean isNotFirstInInstitution()
    {
        if ( index == 0 ) return false;
        AssignmentOffering prev =
            assignmentsToDisplay.objectAtIndex( index - 1 );
        return prev.courseOffering().course().department().institution()
            == anAssignmentOffering.courseOffering().course().department()
                .institution();
    }


    // ----------------------------------------------------------
    public String[] includePatterns()
    {
        if ( index != includePatternsIndex )
        {
            includePatterns = null;
            SubmissionProfile profile = anAssignmentOffering.assignment()
                .submissionProfile();
            if ( profile != null )
            {
                String patterns = profile.includedFilePatterns();
                if ( patterns != null && !patterns.equals( "" ) )
                {
                    includePatterns = patterns.split( "\\s*,\\s*" );
                }
            }
            includePatternsIndex = index;
        }
        return includePatterns;
    }


    // ----------------------------------------------------------
    public String[] excludePatterns()
    {
        if ( index != excludePatternsIndex )
        {
            excludePatterns = null;
            SubmissionProfile profile = anAssignmentOffering.assignment()
                .submissionProfile();
            if ( profile != null )
            {
                String patterns = profile.excludedFilePatterns();
                if ( patterns != null && !patterns.equals( "" ) )
                {
                    excludePatterns = patterns.split( "\\s*,\\s*" );
                }
            }
            excludePatternsIndex = index;
        }
        return excludePatterns;
    }


    // ----------------------------------------------------------
    public String[] requirePatterns()
    {
        if ( index != requirePatternsIndex )
        {
            requirePatterns = null;
            SubmissionProfile profile = anAssignmentOffering.assignment()
                .submissionProfile();
            if ( profile != null )
            {
                String patterns = profile.requiredFilePatterns();
                if ( patterns != null && !patterns.equals( "" ) )
                {
                    requirePatterns = patterns.split( "\\s*,\\s*" );
                }
            }
            requirePatternsIndex = index;
        }
        return requirePatterns;
    }


    // ----------------------------------------------------------
    public String thisPattern()
    {
        return thisPattern;
    }


    // ----------------------------------------------------------
    public void setThisPattern( String value )
    {
        thisPattern = value;
    }


    //~ Instance/static variables .............................................

    private NSTimestamp currentTime;
    private String[]     includePatterns = null;
    private int          includePatternsIndex;
    private String[]     excludePatterns = null;
    private int          excludePatternsIndex;
    private String[]     requirePatterns = null;
    private int          requirePatternsIndex;
    private String       thisPattern;
    static Logger log = Logger.getLogger( BlueJSubmitterDefinitions.class );
}
