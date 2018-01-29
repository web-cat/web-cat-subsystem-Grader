/*==========================================================================*\
 |  $Id: GradingPlugin.java,v 1.15 2014/06/16 17:30:30 stedwar2 Exp $
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

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import er.extensions.foundation.ERXValueUtilities;
import java.io.*;
import java.util.*;
import net.sf.webcat.FeatureDescriptor;
import net.sf.webcat.FeatureProvider;
import org.webcat.core.MutableDictionary;
import org.apache.log4j.Logger;
import org.webcat.core.*;
import org.webcat.woextensions.ECAction;
import static org.webcat.woextensions.ECAction.run;

// -------------------------------------------------------------------------
/**
 *  Represents an uploaded grading plug-in.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.15 $, $Date: 2014/06/16 17:30:30 $
 */
public class GradingPlugin
    extends _GradingPlugin
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new ScriptFile object.
     */
    public GradingPlugin()
    {
        super();
    }


    //~ Constants .............................................................

    public static final String NO_AUTO_UPDATE_KEY =
        "grader.willNotAutoUpdatePlugins";
    public static final String NO_AUTO_INSTALL_KEY =
        "grader.willNotAutoInstallPlugins";
    public static final String AUTO_PUBLISH_KEY =
        "autoPublish";


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Determine whether or not this script is stored in its own
     * subdirectory, or as a single file.
     * @return true if there is a subdirectory for this script
     */
    public boolean hasSubdir()
    {
        return subdirName() != null;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where this script is stored.
     * @return the directory name
     */
    public String dirName()
    {
        StringBuffer dir = userScriptDirName( author(), isConfigFile() );
        if ( hasSubdir() )
        {
            dir.append( '/' );
            dir.append( subdirName() );
        }
        return dir.toString();
    }


    // ----------------------------------------------------------
    /**
     * Returns the directory where the plug-in's public resources are stored.
     *
     * @return the plug-in's public resources directory
     */
    public File publicResourcesDir()
    {
        return new File(dirName(), "public");
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the path name for this script's entry point--its main
     * executable file.
     * @return the path to the main file
     */
    public String mainFilePath()
    {
        String myName = null;
        @SuppressWarnings("unchecked")
        NSDictionary<String, String> config = configDescription();
        if (config != null)
        {
            myName = config.objectForKey("executable");
        }
        if (myName == null)
        {
            myName = mainFileName();
        }
        return dirName() + "/" + myName;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the path name for this script's configuration description.
     * @return the path to the config.plist file
     */
    public String configPlistFilePath()
    {
        return dirName() + "/config.plist";
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the path to the public resource with the specified relative
     * path in the public resources directory. If the path is invalid (for
     * example, it tries to navigate to a parent directory), then null is
     * returned.
     *
     * @param path the relative path to the resource
     * @return the File object that represents the file, or null if the path
     *     was invalid
     */
    public File fileForPublicResourceAtPath(String path)
    {
        if (path == null)
        {
            return publicResourcesDir();
        }
        else
        {
            File file = new File(publicResourcesDir(), path);

            try
            {
                if (file.getCanonicalPath().startsWith(
                        publicResourcesDir().getCanonicalPath()))
                {
                    return file;
                }
            }
            catch (IOException e)
            {
                log.error("An error occurred while retrieving the canonical "
                    + "path of the file " + file.toString());
            }

            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Execute this script with the given command line argument(s).
     *
     * @param args the arguments to pass to the script on the command line
     * @param cwd the working directory to use
     * @throws java.io.IOException if one occurs
     * @throws InterruptedException if one occurs
     */
    public void execute( String args, File cwd )
        throws java.io.IOException, InterruptedException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "execute(): args = '" + args + "', cwd = " + cwd );
        }
        String  command   = "";
        if ( configDescription().containsKey( "interpreter.prefix" ) )
        {
            // Look up the associated value, perform property substitution
            // on it, and add it before the main file name
            command = command
                + Application.configurationProperties().
                    substitutePropertyReferences(
                        configDescription().valueForKey( "interpreter.prefix" )
                            .toString()
                    )
                + " ";
        }
        command += mainFilePath();
        if ( args != null )
        {
            command = command + " " + args;
        }

        Application.wcApplication().executeExternalCommand( command, cwd );
    }


    // ----------------------------------------------------------
    /**
     * Get a short (no longer than 60 characters) description of this plug-in,
     * which currently returns {@link #name()}.
     * @return the description
     */
    public String userPresentableDescription()
    {
        return name();
    }


    // ----------------------------------------------------------
    public String displayableName()
    {
        Object nameObj = configDescription().valueForKey("displayableName");
        String myName = (nameObj == null)
            ? name()
            : nameObj.toString();
        if (myName == null)
        {
            myName = uploadedFileName();
        }
        return myName;
    }


    // ----------------------------------------------------------
    /**
     * If this script's config.plist file has been modified, then
     * reparse it and store its config information.
     */
    public void reinitializeConfigAttributesIfNecessary()
    {
        if ( hasSubdir() )
        {
            File configPlist = new File( configPlistFilePath() );
            NSTimestamp lastRead = lastModified();
            NSTimestamp modified =
                new NSTimestamp( configPlist.lastModified() );

            // The check against fileConfigSettings that has been added is to
            // migrate older plugins.
            if (lastRead != null && modified.after(lastRead)
                    || storedValueForKey("fileConfigSettings") == null)
            {
                // silently swallow returned message
                initializeConfigAttributes( configPlist );
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Parse this script's config.plist file and store its config information.
     * @return null on success, or an error message if parsing failed
     */
    public String initializeConfigAttributes()
    {
        if ( hasSubdir() )
        {
            return initializeConfigAttributes(
                            new File( configPlistFilePath() ) );
        }
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Parse this script's config.plist file and store its config information.
     * @param configPlist the config.plist file to parse
     * @return null on success, or an error message if parsing failed
     */
    public String initializeConfigAttributes( File configPlist )
    {
        // reset the cached descriptor, if any
        descriptor = null;
        if ( configPlist.exists() )
        {
            try
            {
                log.debug( "reloading " + configPlist.getCanonicalPath() );
            }
            catch ( IOException e )
            {
                log.error( "error attempting to load confg.plist file for "
                           + this, e );
            }
            try
            {
                MutableDictionary dict =
                    MutableDictionary.fromPropertyList( configPlist );
                setConfigDescription( dict );
//              log.debug( "script config.plist = " + dict );
                String dictName = (String)dict.objectForKey( "name" );
                setName( dictName );

                MutableDictionary fileSettings = new MutableDictionary();

                NSArray<?> globalOptions = (NSArray<?>)dict.objectForKey(
                        "globalOptions");
                if (globalOptions != null)
                {
                    addFilePropertiesToDictionary(fileSettings, globalOptions);
                }

                NSArray<?> assignmentOptions = (NSArray<?>)dict.objectForKey(
                        "assignmentOptions");
                if (assignmentOptions != null)
                {
                    addFilePropertiesToDictionary(fileSettings,
                            assignmentOptions);
                }

                NSArray<?> options = (NSArray<?>)dict.objectForKey("options");
                if (options != null)
                {
                    addFilePropertiesToDictionary(fileSettings, options);

                    MutableDictionary defaults = new MutableDictionary();
                    for ( int i = 0; i < options.count(); i++ )
                    {
                        @SuppressWarnings("unchecked")
                        NSDictionary<String, ?> thisOption =
                            (NSDictionary<String, ?>)options
                                .objectAtIndex( i );
//                      log.debug( "this option = " + thisOption );
                        if ( thisOption.objectForKey( "disable" ) == null )
                        {
                            String property = (String)thisOption
                                .objectForKey( "property" );
                            Object value = thisOption
                                .objectForKey( "default" );
                            if ( property != null && value != null )
                            {
                                defaults.setObjectForKey( value, property );
                            }

                            String type = (String) thisOption
                                .objectForKey("type");

                            if (type.equals("file"))
                            {
                                fileSettings.setObjectForKey(false, property);
                            }
                            else if (type.equals("fileOrDir"))
                            {
                                fileSettings.setObjectForKey(true, property);
                            }
                        }
                    }

                    setDefaultConfigSettings(defaults);
                }
                else
                {
                    setDefaultConfigSettings(null);
                }

                setFileConfigSettings(fileSettings);

                setLastModified(
                    new NSTimestamp( configPlist.lastModified() ) );
                if ( ERXValueUtilities.booleanValue(
                     configDescription().get( AUTO_PUBLISH_KEY ) ) )
                {
                    setIsPublished( true );
                }
            }
            catch ( Exception e )
            {
                return e.getMessage()
                    + " (error reading script's config.plist file)";
            }
        }
        else
        {
            return "This script is missing its 'config.plist' file.";
        }
        return null;
    }


    // ----------------------------------------------------------
    private void addFilePropertiesToDictionary(MutableDictionary fileProps,
            NSArray<?> options)
    {
        @SuppressWarnings("unchecked")
        NSArray<NSDictionary<String, ?>> optionsDict =
            (NSArray<NSDictionary<String, ?>>) options;
        for (NSDictionary<String, ?> option : optionsDict)
        {
            if (option.objectForKey("disable") == null)
            {
                String property = (String) option.objectForKey("property");
                String type = (String) option.objectForKey("type");

                if (type.equalsIgnoreCase("file"))
                {
                    fileProps.setObjectForKey(false, property);
                }
                else if (type.equalsIgnoreCase("fileOrDir"))
                {
                    fileProps.setObjectForKey(true, property);
                }
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the configured timeout multiplier for this script file.
     * @return the timeout multiplier (scale factor)
     */
    public int timeoutMultiplier()
    {
        return ERXValueUtilities.intValueWithDefault(
            configDescription().valueForKey( "timeoutMultiplier" ), 1 );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the configured timeout internal padding for this script file.
     * @return the timeout internal padding (in seconds)
     */
    public int timeoutInternalPadding()
    {
        return ERXValueUtilities.intValueWithDefault(
            configDescription().valueForKey( "timeoutInternalPadding" ), 0 );
    }


    // ----------------------------------------------------------
    /**
     * Get the FeatureDescriptor for this plugin.
     * @return this plug-in's descriptor
     */
    public FeatureDescriptor descriptor()
    {
        if ( descriptor == null )
        {
            descriptor = new PluginDescriptor( this );
        }
        return descriptor;
    }


    // ----------------------------------------------------------
    /**
     * Download this plug-in's latest file from its provider on-line
     * and install it for the given user, overwriting any existing
     * version.
     * @return null on success, or an error message on failure
     */
    public String installUpdate()
    {
        return installOrUpdate(
            author(), descriptor().providerVersion(), true, this );
    }


    // ----------------------------------------------------------
    /**
     * Download the specified plug-in file and install it for the given
     * user.  If the download succeeds, a new ScriptFile object will be
     * created under the specified user object's editing context.  The
     * new ScriptFile object is not returned, by can be retrieved after
     * comitting the user object's editing context and refetching.
     * @param installedBy the user
     * @param plugin the plug-in to download
     * @param overwrite true if the named plug-in already exists in the
     *        user's directory
     * @return null on success, or an error message on failure
     */
    public static String installOrUpdate(
        User                            installedBy,
        net.sf.webcat.FeatureDescriptor plugin,
        boolean                         overwrite )
    {
        return installOrUpdate( installedBy, plugin, overwrite, null );
    }


    // ----------------------------------------------------------
    /**
     * Automatically update any installed scripts if auto-updates are
     * enabled, and automatically install any new plug-ins, if
     * auto-installation is enabled.
     */
    public static void autoUpdateAndInstall()
    {
        run(new ECAction() { public void action() {
            autoInstallNewPlugins(ec, autoUpdatePlugins(ec));
        }});
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where a user's scripts are stored.
     * @param pluginAuthor the user
     * @param isData true if this is the directory for a script data/config
     *               file, or false if this is the directory where scripts
     *               themselves are stored
     * @return the directory name
     */
    public static StringBuffer userScriptDirName(
        User pluginAuthor, boolean isData)
    {
        StringBuffer dir = new StringBuffer(50);
        dir.append(isData ? scriptDataRoot() : scriptRoot());
        dir.append('/');
        dir.append(pluginAuthor.authenticationDomain().subdirName());
        dir.append('/');
        dir.append(pluginAuthor.userName());
        return dir;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where this script is stored.
     * @param fileName the script's file name
     * @return the subdirectory name based on the uploaded file name, with
     * all dots replaced by underscores
     */
    public static String convertToSubdirName( String fileName )
    {
        return fileName.replace( '.', '_' ).replace( ' ', '-' );
    }


    // ----------------------------------------------------------
    /**
     * Create a new script file object from uploaded file data.
     * @param ec           the editing context in which to add the new object
     * @param pluginAuthor       the user uploading the script
     * @param uploadedName the script's file name
     * @param uploadedData the file's data
     * @param isData       true if this is a script data/config file, or
     *                     false if this is a script itself
     * @param expand       true if zip/jar files should be expanded, or false
     *                     otherwise
     * @param errors       a dictionary in which to store any error messages
     *                     for display to the user
     * @return the new script file, if successful, or null if unsuccessful
     */
    public static GradingPlugin createNewGradingPlugin(
            EOEditingContext    ec,
            User                pluginAuthor,
            String              uploadedName,
            NSData              uploadedData,
            boolean             isData,
            boolean             expand,
            NSMutableDictionary<String, Object> errors
        )
    {
        String userScriptDir = userScriptDirName( pluginAuthor, isData ).toString();
        String newSubdirName = null;
        uploadedName = ( new File( uploadedName ) ).getName();
        String uploadedNameLC = uploadedName.toLowerCase();
        File toLookFor;
        if ( expand && ( uploadedNameLC.endsWith( ".zip" ) ||
                         uploadedNameLC.endsWith( ".jar" ) ) )
        {
            newSubdirName = GradingPlugin.convertToSubdirName( uploadedName );
            toLookFor = new File( userScriptDir + "/" + newSubdirName );
        }
        else
        {
            toLookFor = new File( userScriptDir + "/" + uploadedName );
        }
        if ( toLookFor.exists() )
        {
            String msg = "You already have an uploaded script with this "
                         + "name.  If you want to change that script's "
                         + "files, then edit its configuration.  "
                         + "Otherwise, please use a different file name "
                         + "for this new script.";
            errors.setObjectForKey( msg, msg );
            return null;
        }

        GradingPlugin gradingPlugin = new GradingPlugin();
        ec.insertObject( gradingPlugin );
        gradingPlugin.setUploadedFileName( uploadedName );
        gradingPlugin.setMainFileName( uploadedName );
        gradingPlugin.setLastModified( new NSTimestamp() );
        gradingPlugin.setAuthorRelationship( pluginAuthor );

        // Save the file to disk
        log.debug( "saving to file " + gradingPlugin.mainFilePath() );
        File pluginPath = new File( gradingPlugin.mainFilePath() );
        try
        {
            pluginPath.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream( pluginPath );
            uploadedData.writeToStream( out );
            out.close();
        }
        catch ( java.io.IOException e )
        {
            String msg = e.getMessage();
            errors.setObjectForKey( msg, msg );
            ec.deleteObject( gradingPlugin );
            pluginPath.delete();
            return null;
        }

        if ( expand && ( uploadedNameLC.endsWith( ".zip" ) ||
                         uploadedNameLC.endsWith( ".jar" ) ) )
        {
            try
            {
                //ZipFile zip = new ZipFile( script.mainFilePath() );
                gradingPlugin.setSubdirName( newSubdirName );
                log.debug( "unzipping to " + gradingPlugin.dirName() );
                org.webcat.archives.ArchiveManager.getInstance()
                    .unpack( new File( gradingPlugin.dirName() ), pluginPath );
                //Grader.unZip( zip, new File( script.dirName() ) );
                //zip.close();
                pluginPath.delete();
            }
            catch ( java.io.IOException e )
            {
                String msg = e.getMessage();
                errors.setObjectForKey( msg, msg );
                gradingPlugin.setSubdirName( newSubdirName );
                org.webcat.core.FileUtilities
                    .deleteDirectory( gradingPlugin.dirName() );
                pluginPath.delete();
                log.warn( "error unzipping:", e );
                // throw new NSForwardException( e );
                ec.deleteObject( gradingPlugin );
                return null;
            }
            gradingPlugin.setMainFileName( null );
            String msg = gradingPlugin.initializeConfigAttributes();
            if ( msg != null )
            {
                errors.setObjectForKey( msg, msg );
            }
        }
        return gradingPlugin;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where all user scripts are stored.
     * @return the directory name
     */
    public static String scriptRoot()
    {
        if ( scriptRoot == null )
        {
            scriptRoot = org.webcat.core.Application
                .configurationProperties().getProperty( "grader.scriptsroot" );
            if ( scriptRoot == null )
            {
                scriptRoot = org.webcat.core.Application
                    .configurationProperties()
                        .getProperty( "grader.submissiondir" )
                    + "/UserScripts";
            }
        }
        return scriptRoot;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where all user script config/data
     * files are stored.
     * @return the directory name
     */
    public static String scriptDataRoot()
    {
        if ( scriptDataRoot == null )
        {
            scriptDataRoot = org.webcat.core.Application
                .configurationProperties()
                .getProperty( "grader.scriptsdataroot" );
            if ( scriptDataRoot == null )
            {
                scriptDataRoot = scriptRoot() + "Data";
            }
        }
        return scriptDataRoot;
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    /**
     * Download the specified plug-in file and install it for the given
     * user.  If the download succeeds, the given ScriptFile object
     * will be updated appropriately.  If none is provided, a new ScriptFile
     * object will be created under the specified user object's editing
     * context.  The new ScriptFile object is not returned, by can be
     * retrieved after comitting the user object's editing context and
     * refetching.
     * @param installedBy the user
     * @param plugin the plug-in to download
     * @param overwrite true if the named plug-in already exists in the
     *        user's directory
     * @param scriptFile the ScriptFile object to update (or null to force
     *        creation of a new one)
     * @return null on success, or an error message on failure
     */
    private static String installOrUpdate(
        User                            installedBy,
        net.sf.webcat.FeatureDescriptor plugin,
        boolean                         overwrite,
        GradingPlugin                   scriptFile )
    {
        if ( scriptFile != null && !scriptFile.hasSubdir() )
        {
            return "Installed plug-in does not support downloads!";
        }

        GradingPlugin newScriptFile = null;
        String pluginSubdirName = convertToSubdirName( plugin.name() );
        File newScriptPath = null;
        if ( scriptFile == null )
        {
            newScriptFile = new GradingPlugin();
            installedBy.editingContext().insertObject( newScriptFile );
            newScriptFile.setLastModified( new NSTimestamp() );
            newScriptFile.setAuthorRelationship( installedBy );
            newScriptFile.setSubdirName( pluginSubdirName );
            scriptFile = newScriptFile;
        }
        else if ( !pluginSubdirName.equals( scriptFile.subdirName() ) )
        {
            newScriptPath = new File (
                userScriptDirName( installedBy, false ).toString(),
                pluginSubdirName );
            if ( newScriptPath.exists() )
            {
                return "The plug-in you are updating has changed names, but "
                    + "you already have an installed plug-in with the new "
                    + "name, so there is a conflict.  The original plug-in "
                    + "cannot be updated until the name conflict is resolved.";
            }
        }

        File pluginSubdir = new File( scriptFile.dirName() );
        if ( pluginSubdir.exists() )
        {
            log.debug(
                "directory " + pluginSubdir.getAbsolutePath() + " exists" );
            if ( overwrite )
            {
                org.webcat.core.FileUtilities
                    .deleteDirectory( pluginSubdir );
            }
            else
            {
                if ( newScriptFile != null )
                {
                    newScriptFile.editingContext()
                        .deleteObject( newScriptFile );
                }
                return "You already have an installed plug-in with this name."
                    + " If you want to change that script's files, then "
                    + " use its browse/edit action icon instead.";
            }
        }

        // Save the file to disk
        log.debug( "downloading plug-in archive" );
        if ( newScriptPath == null )
        {
            newScriptPath = new File( scriptFile.dirName() );
        }
        else
        {
            scriptFile.setSubdirName( pluginSubdirName );
        }
        File downloadPath = newScriptPath.getParentFile();
        File archiveFile = new File( downloadPath.getAbsolutePath()
            + "/" + plugin.name() + "_" + plugin.currentVersion() + ".jar" );
        downloadPath.mkdirs();
        plugin.downloadTo( downloadPath );
        try
        {
            org.webcat.archives.ArchiveManager.getInstance()
                .unpack( newScriptPath,  archiveFile );
        }
        catch ( java.io.IOException e )
        {
            if ( newScriptFile != null )
            {
                newScriptFile.editingContext()
                    .deleteObject( newScriptFile );
            }
            return e.getMessage();
        }

        archiveFile.delete();
        String msg = scriptFile.initializeConfigAttributes();
        if ( msg != null )
        {
            if ( newScriptFile != null )
            {
                newScriptFile.editingContext()
                    .deleteObject( newScriptFile );
            }
        }
        return msg;
    }


    // ----------------------------------------------------------
    private static NSArray<GradingPlugin> autoUpdatePlugins(
        EOEditingContext ec)
    {
        NSArray<GradingPlugin> pluginList = allObjects(ec);
        if (!Application.configurationProperties()
                .booleanForKey(NO_AUTO_UPDATE_KEY))
        {
            for (GradingPlugin plugin : pluginList)
            {
                try
                {
                    if (plugin.descriptor().updateIsAvailable())
                    {
                        log.info("Updating plug-in: \"" + plugin.name() + "\"");
                        String msg = plugin.installUpdate();
                        if (msg != null)
                        {
                            log.error("Error updating plug-in \""
                                + plugin.name() + "\": " + msg);
                        }
                        ec.saveChanges();
                    }
                    else
                    {
                        log.debug("Plug-in \"" + plugin.name()
                            + "\" is up to date.");
                    }
                }
                catch (IOException e)
                {
                    log.error("Error checking for updates to plug-in \""
                        + plugin.name() + "\": " + e);
                }
            }
        }
        return pluginList;
    }


    // ----------------------------------------------------------
    private static void autoInstallNewPlugins(
        EOEditingContext ec, NSArray<GradingPlugin> pluginList)
    {
        if (Application.configurationProperties()
                 .booleanForKey(NO_AUTO_INSTALL_KEY))
        {
            return;
        }
        String adminUserName = Application.configurationProperties()
            .getProperty("AdminUsername");
        if (adminUserName == null)
        {
            log.error("No definition for 'AdminUsername' config property!\n"
                + "Cannot install new plug-ins without admin user name.");
            return;
        }
        User admin = null;
        NSArray<User> candidates = User.objectsMatchingQualifier(ec,
            User.userName.eq(adminUserName));
        for (User user : candidates)
        {
            if (user.hasAdminPrivileges())
            {
                if (admin == null)
                {
                    admin = user;
                }
                else
                {
                    log.warn( "Duplicate admin accounts with user name \""
                        + adminUserName + "\" found.  Using " + admin
                        + ", ignoring " + user);
                }
            }
        }
        if (admin == null)
        {
            log.error("Cannot find admin account with user name \""
                + adminUserName + "\"!");
            return;
        }

        Collection<FeatureDescriptor> availablePlugins =
            new HashSet<FeatureDescriptor>();
        for (FeatureProvider provider : FeatureProvider.providers())
        {
            if (provider != null)
            {
                availablePlugins.addAll(provider.plugins());
            }
        }
        if (pluginList != null)
        {
            for (GradingPlugin s : pluginList)
            {
                FeatureDescriptor fd = s.descriptor().providerVersion();
                if (fd != null)
                {
                    if (availablePlugins.size() > 0
                        && !availablePlugins.remove(fd))
                    {
                        Iterator<FeatureDescriptor> available =
                            availablePlugins.iterator();
                        while (available.hasNext())
                        {
                            FeatureDescriptor candidate = available.next();
                            if (candidate.name() == null
                                || candidate.name().equals(fd.name()))
                            {
                                available.remove();
                            }
                        }
                    }
                }
            }
        }
        for (FeatureDescriptor plugin : availablePlugins)
        {
            if (plugin.getProperty("batchEntity") == null)
            {
                log.info("Installing new plug-in: \"" + plugin.name() + "\"");
                String msg = installOrUpdate(admin, plugin, true, null);
                if (msg != null)
                {
                    log.error("Error installing new plug-in \""
                        + plugin.name() + "\": " + msg);
                }
                ec.saveChanges();
            }
        }
    }


    //~ Instance/static variables .............................................

    private PluginDescriptor descriptor;

    static private String scriptRoot = null;
    static private String scriptDataRoot = null;
    static Logger log = Logger.getLogger(GradingPlugin.class);
}
