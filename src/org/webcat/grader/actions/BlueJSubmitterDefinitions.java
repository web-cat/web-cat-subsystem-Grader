/*==========================================================================*\
 |  Copyright (C) 2006-2018 Virginia Tech
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

import java.util.HashMap;
import java.util.Map;
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
    public NSTimestamp                 expires;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse(WOResponse response, WOContext context)
    {
        // System.out.println("query string = " + context.request().queryString());
        // System.out.println("handler = " + context.request().requestHandlerKey());
        // System.out.println("handler path = " + context.request().requestHandlerPath());
        // System.out.println("uri = " + context.request().uri());
        log.debug( "appendToResponse()" );
        NSDictionary<String, NSArray<Object>> formValues =
            context.request().formValues();
        if (formValues.containsKey("flush"))
        {
            flushCache();
        }
        if (formValues.containsKey("useCache"))
        {
            USE_CACHE = ERXValueUtilities.booleanValueWithDefault(
                context.request().formValueForKey("useCache"), true);
        }
        if (useCache())
        {
            NSData cachedResponse = getCachedResponse(
                context.request().requestHandlerPath(),
                context.request().queryString());
            if (cachedResponse != null)
            {
                log.debug( "appendToResponse(): returning cached response" );
                response.setHeader(mimeType(), "content-type");
                response.appendContentData(cachedResponse);
                return;
            }
        }
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
            AssignmentOffering.SubmitterOfferings offerings =
                AssignmentOffering.objectsForSubmitterEngine(
                    ec,
                    context().request().formValues(),
                    currentTime,
                    groupByCRN,
                    showAll(),
                    preserveDateDifferences()
                    );
            expires = offerings.expires;
            assignmentsToDisplay = offerings.offerings;
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
            // System.out.println("content = " + response.contentString() );
            if (useCache())
            {
                log.debug("caching response for: "
                    + context.request().requestHandlerPath()
                    + ", "
                    + context.request().queryString()
                    + ", "
                    + expires);
                cacheResponse(
                    context.request().requestHandlerPath(),
                    context.request().queryString(),
                    expires,
                    response.content());
            }
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
    public boolean includeUserAndPwd()
    {
        return !(anAssignmentOffering.courseOffering().course().department()
            .institution().authenticator()
            instanceof org.webcat.core.CasAuthenticator);
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


    // ----------------------------------------------------------
    public static void cacheResponse(
        String handler,
        String params,
        NSTimestamp expires,
        NSData response)
    {
        if (!useCache())
        {
            return;
        }
        log.debug("cacheResponse: " + handler + ", " + params + ", " + expires);
        synchronized (responseCache)
        {
            Map<String, CachedResponse> submap = responseCache.get(handler);
            if (submap == null)
            {
                submap = new HashMap<String, CachedResponse>();
                responseCache.put(handler, submap);
            }
            submap.put(params, new CachedResponse(expires, response));
        }
    }


    // ----------------------------------------------------------
    public static NSData getCachedResponse(
        String handler,
        String params)
    {
        if (!useCache())
        {
            return null;
        }
        NSTimestamp now = new NSTimestamp();
        synchronized (responseCache)
        {
            Map<String, CachedResponse> submap = responseCache.get(handler);
            if (submap != null)
            {
                CachedResponse response = submap.get(params);
                if (response != null)
                {
                    if (response.expires == null
                        || response.expires.after(now))
                    {
                        log.debug("getCachedResponse: " + handler + ", "
                            + params + ": cache hit: " + response.expires);
                        return response.response;
                    }
                    else
                    {
                        // expired, so remove it
                        log.debug("getCachedResponse: " + handler + ", " + params
                            + ": removing expired value: " + response.expires);
                        submap.remove(params);
                    }
                }
            }
        }
        log.debug("getCachedResponse: " + handler + ", " + params
            + ": no cache found");
        return null;
    }


    // ----------------------------------------------------------
    public static void flushCache()
    {
        if (!useCache())
        {
            return;
        }
        log.debug("flushCache()");
        synchronized (responseCache)
        {
            for (Map<String, CachedResponse> submap: responseCache.values())
            {
                submap.clear();
            }
        }
    }


    // ----------------------------------------------------------
    public static boolean useCache()
    {
        if (USE_CACHE == null)
        {
            USE_CACHE = Application.wcApplication().properties()
                .booleanForKeyWithDefault(
                    BlueJSubmitterDefinitions.class.getName() + ".useCache",
                    true);
        }
        return USE_CACHE.booleanValue();
    }


    // ----------------------------------------------------------
    public static class CachedResponse
    {
        public NSTimestamp expires;
        public NSData response;

        public CachedResponse(NSTimestamp expires, NSData response)
        {
            this.expires = expires;
            this.response = response;
        }
    }


    //~ Instance/static variables .............................................

    private static Map<String, Map<String, CachedResponse>> responseCache =
        new HashMap<String, Map<String, CachedResponse>>();
    private NSTimestamp currentTime;
    private String[]     includePatterns = null;
    private int          includePatternsIndex;
    private String[]     excludePatterns = null;
    private int          excludePatternsIndex;
    private String[]     requirePatterns = null;
    private int          requirePatternsIndex;
    private String       thisPattern;
    static Logger log = Logger.getLogger( BlueJSubmitterDefinitions.class );
    private static Boolean USE_CACHE = null;
}
