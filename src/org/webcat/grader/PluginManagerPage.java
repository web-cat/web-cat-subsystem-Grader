/*==========================================================================*\
 |  $Id: PluginManagerPage.java,v 1.6 2012/01/05 19:51:44 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2012 Virginia Tech
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
import com.webobjects.foundation.*;
import java.util.*;
import er.extensions.appserver.ERXDisplayGroup;
import er.extensions.foundation.ERXValueUtilities;
import net.sf.webcat.*;
import org.apache.log4j.Logger;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 *  The main "control panel" page for plugins in the Plug-ins
 *  tab.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.6 $, $Date: 2012/01/05 19:51:44 $
 */
public class PluginManagerPage
extends WCComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new page object.
     *
     * @param context The context to use
     */
    public PluginManagerPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public int                            index;
    public GradingPlugin                  plugin;
    public ERXDisplayGroup<GradingPlugin> publishedPluginGroup;
    public ERXDisplayGroup<GradingPlugin> unpublishedPluginGroup;
    public ERXDisplayGroup<GradingPlugin> personalPluginGroup;
    public NSArray<FeatureDescriptor>     newPlugins;
    public NSData                         uploadedData;
    public String                         uploadedName;
    public FeatureDescriptor              feature;
    public String                         providerURL;

    public static final String TERSE_DESCRIPTIONS_KEY =
        "tersePluginDescriptions";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        terse = null;
        publishedPluginGroup.fetch();
        if ( user().hasAdminPrivileges() )
        {
            unpublishedPluginGroup.fetch();
        }
        else
        {
            personalPluginGroup.queryBindings().setObjectForKey(
                user(),
                "user"
            );
            personalPluginGroup.fetch();
        }
        if ( newPlugins == null )
        {
            for (FeatureProvider fp : FeatureProvider.providers())
            {
                fp.refresh();
            }
            newPlugins = newPlugins();
        }
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    /**
     * Get the current servlet adaptor, if one is available.
     * @return the servlet adaptor, or null when none is available
     */
    public net.sf.webcat.WCServletAdaptor adaptor()
    {
        return net.sf.webcat.WCServletAdaptor.getInstance();
    }


    // ----------------------------------------------------------
    /**
     * Determine if there is a download site.
     * @return true if a download url is defined
     */
    public boolean canDownload()
    {
        return plugin.descriptor().getProperty( "provider.url" ) != null;
    }


    // ----------------------------------------------------------
    /**
     * Download the latest version of the current subsystem for updating
     * on restart.
     * @return null to refresh the current page
     */
    public WOComponent download()
    {
        String msg = plugin.installUpdate();
        possibleErrorMessage( msg );
        if ( msg == null )
        {
            if (applyLocalChanges())
            {
                confirmationMessage( "The plug-in '" + plugin.name()
                    + "' has been downloaded from its provider and "
                    + "re-installed." );
            }
        }
        else
        {
            cancelLocalChanges();
        }
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Download a new subsystem for installation on restart.
     * @return null to refresh the current page
     */
    public WOComponent downloadNew()
    {
        String msg =
            GradingPlugin.installOrUpdate( user(), feature, false );
        possibleErrorMessage( msg );
        if ( msg == null )
        {
            if (applyLocalChanges())
            {
                confirmationMessage( "New plug-in '" + feature.name()
                    + "' has been downloaded and installed." );
            }
        }
        else
        {
            cancelLocalChanges();
        }
        newPlugins = null;
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Scan the specified provider URL.
     * @return null to refresh the current page
     */
    public WOComponent scanNow()
    {
        if ( providerURL == null || providerURL.equals( "" ) )
        {
            error( "Please specify a provider URL first." );
        }
        else
        {
            try
            {
                // TODO: fix this to correctly re-load ...
                FeatureProvider.getProvider(providerURL);
            }
            catch (java.io.IOException e)
            {
                error("Cannot read feature provider information from "
                    + " specified URL: '" + providerURL + "'.");
            }
        }

        // Erase cache of new subsystems so it will be recalculated now
        newPlugins = null;

        // refresh page
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Determine whether this user has permissions to edit the current plug-in.
     * @return true if the current plug-in can be edited by the current user
     */
    public boolean canEditPlugin()
    {
        User user = user();
        return user.hasAdminPrivileges() || user == plugin.author();
    }


    // ----------------------------------------------------------
    /**
     * Edit the selected plug-in's configuration settings.
     * @return the subsystem's edit page
     */
    public WOComponent edit()
    {
        EditPluginGlobalsPage newPage =
            pageWithName(EditPluginGlobalsPage.class);
        newPage.nextPage = this;
        newPage.plugin = plugin;
        return newPage;
    }


    // ----------------------------------------------------------
    /**
     * Browse or edit the selected plug-in's files.  Administrators can
     * edit all plug-ins.  Otherwise, users can only edit plug-ins they
     * have authored, and can only browse others.
     * @return the subsystem's edit page
     */
    public WOComponent editFiles()
    {
        EditScriptFilesPage newPage = pageWithName(EditScriptFilesPage.class);
        newPage.nextPage = this;
        newPage.gradingPlugin = plugin;
        newPage.hideNextAndBack( true );
        newPage.isEditable = user().hasAdminPrivileges() ||
            user().equals( plugin.author() );
        return newPage;
    }


    // ----------------------------------------------------------
    /**
     * Toggle the
     * {@link net.sf.webcat.WCServletAdaptor#willUpdateAutomatically()}
     * attribute.
     * @return null to refresh the current page
     */
    public WOComponent toggleAutoUpdates()
    {
        boolean option = Application.configurationProperties()
            .booleanForKey( GradingPlugin.NO_AUTO_UPDATE_KEY );
        Application.configurationProperties().put(
            GradingPlugin.NO_AUTO_UPDATE_KEY, option ? "false" : "true" );
        Application.configurationProperties().attemptToSave();
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Toggle the
     * {@link net.sf.webcat.WCServletAdaptor#willUpdateAutomatically()}
     * attribute.
     * @return null to refresh the current page
     */
    public WOComponent toggleAutoInstalls()
    {
        boolean option = Application.configurationProperties()
            .booleanForKey( GradingPlugin.NO_AUTO_INSTALL_KEY );
        Application.configurationProperties().put(
            GradingPlugin.NO_AUTO_INSTALL_KEY, option ? "false" : "true" );
        Application.configurationProperties().attemptToSave();
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Upload a new plug-in.
     * @return null to refresh the page
     */
    public WOComponent upload()
    {
        if ( uploadedName == null || uploadedData == null )
        {
            error( "Please select a file to upload." );
            return null;
        }
        GradingPlugin.createNewGradingPlugin(
            localContext(),
            user(),
            uploadedName,
            uploadedData,
            false,
            true,
            messages()
        );
        applyLocalChanges();
        uploadedName = null;
        uploadedData = null;
        newPlugins = null;
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Publish/unpublish a plug-in by togglinh its isPublished attribute.
     * @return null to refresh the page
     */
    public WOComponent togglePublished()
    {
        plugin.setIsPublished( !plugin.isPublished() );
        applyLocalChanges();
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Force a fresh reload of the script's config.plist file to pick up
     * any changes (i.e., new attributes, new default values, etc.).
     * @return null, to force this page to reload in the browser when the
     *         action completes
     */
    public WOComponent reloadScriptDefinition()
    {
        String errMsg = plugin.initializeConfigAttributes();
        if ( errMsg != null )
        {
            cancelLocalChanges();
            error( errMsg );
        }
        else
        {
            if (applyLocalChanges())
            {
                confirmationMessage( "Configuration definition for plug-in '"
                    + plugin.name() + "' has been reloaded." );
            }
        }
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Toggle whether or not the user wants verbose descriptions of subsystems
     * to be shown or hidden.  The setting is stored in the user's preferences
     * under the key specified by the VERBOSE_DESCRIPTIONS_KEY, and will be
     * permanently saved the next time the session's local changes are saved.
     */
    public void toggleVerboseDescriptions()
    {
        boolean verboseDescriptions = ERXValueUtilities.booleanValue(
            user().preferences().objectForKey( TERSE_DESCRIPTIONS_KEY ) );
        verboseDescriptions = !verboseDescriptions;
        user().preferences().setObjectForKey(
            Boolean.valueOf( verboseDescriptions ), TERSE_DESCRIPTIONS_KEY );
        user().savePreferences();
    }


    // ----------------------------------------------------------
    /**
     * Look up the user's preferences and determine whether or not to show
     * verbose subsystem descriptions in this component.
     * @return true if verbose descriptions should be hidden, or false if
     * they should be shown
     */
    public Boolean terse()
    {
        if ( terse == null )
        {
            terse = ERXValueUtilities.booleanValue(
                user().preferences().objectForKey( TERSE_DESCRIPTIONS_KEY ) )
                ? Boolean.TRUE : Boolean.FALSE;
        }
        return terse;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the history URL for the current installed plug-in.
     * @return The history URL, or null if none is defined
     */
    public String pluginHistoryUrl()
    {
        String result = plugin.descriptor().getProperty( "history.url" );
        log.debug( "pluginHistoryUrl() = " + result );
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the information URL for the current installed plug-in.
     * @return The information URL, or null if none is defined
     */
    public String pluginInfoUrl()
    {
        String result = plugin.descriptor().getProperty( "info.url" );
        log.debug( "pluginInfoUrl() = " + result );
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the history URL for the current uninstalled plug-in.
     * @return The history URL, or null if none is defined
     */
    public String featureHistoryUrl()
    {
        return feature.getProperty( "history.url" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the information URL for the current uninstalled plug-in.
     * @return The information URL, or null if none is defined
     */
    public String featureInfoUrl()
    {
        return feature.getProperty( "info.url" );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the information URL for the current uninstalled plug-in.
     * @return The information URL, or null if none is defined
     */
    public String featureDisplayableName()
    {
        return feature.getProperty( "displayableName" );
    }


    // ----------------------------------------------------------
    /**
     * Calculate the current set of plug-ins that are available from
     * all registered providers, but that are not yet installed.
     * @return an array of feature descriptors for available uninstalled
     *         grader plug-ins
     */
    public NSArray<FeatureDescriptor> newPlugins()
    {
        Collection<FeatureDescriptor> availablePlugins =
            new HashSet<FeatureDescriptor>();
        for (FeatureProvider provider : FeatureProvider.providers())
        {
            if ( provider != null )
            {
                for (FeatureDescriptor aPlugin : provider.plugins())
                {
                    // Screen out batch plug-ins
                    if (aPlugin.getProperty("batchEntity") == null)
                    {
                        availablePlugins.add(aPlugin);
                    }
                }
            }
        }
        NSArray<GradingPlugin> exclude =
            publishedPluginGroup.displayedObjects();
        if (exclude != null)
        {
            for (GradingPlugin s : exclude)
            {
                FeatureDescriptor fd = s.descriptor().providerVersion();
                if (fd != null)
                {
                    availablePlugins.remove(fd);
                }
            }
        }
        exclude = unpublishedPluginGroup.displayedObjects();
        if (exclude != null)
        {
            for (GradingPlugin s : exclude)
            {
                FeatureDescriptor fd = s.descriptor().providerVersion();
                if (fd != null)
                {
                    availablePlugins.remove(fd);
                }
            }
        }
        exclude = personalPluginGroup.displayedObjects();
        if (exclude != null)
        {
            for (GradingPlugin s : exclude)
            {
                FeatureDescriptor fd = s.descriptor().providerVersion();
                if (fd != null)
                {
                    availablePlugins.remove(fd);
                }
            }
        }
        FeatureDescriptor[] descriptors =
            new FeatureDescriptor[availablePlugins.size()];
        return new NSArray<FeatureDescriptor>(
            availablePlugins.toArray(descriptors));
    }


    //~ Instance/static variables .............................................
    private Boolean terse;
    static Logger log = Logger.getLogger( PluginManagerPage.class );
}
