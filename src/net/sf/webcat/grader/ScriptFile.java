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
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import net.sf.webcat.FeatureDescriptor;
import net.sf.webcat.FeatureProvider;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 *  Represents an uploaded grading script.
 *
 *  @author Stephen Edwards
 *  @version $Id$
 */
public class ScriptFile
    extends _ScriptFile
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new ScriptFile object.
     */
    public ScriptFile()
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
     * Retrieve the path name for this script's entry point--its main
     * executable file.
     * @return the path to the main file
     */
    public String mainFilePath()
    {
        String name = null;
        NSDictionary config = configDescription();
        if ( config != null )
        {
            name = (String)config.objectForKey( "executable" );
        }
        if ( name == null )
        {
            name = mainFileName();
        }
        return dirName() + "/" + name;
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
        Runtime runtime = Runtime.getRuntime();
        String  command = "";
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
        Process proc = null;
        if ( args != null )
        {
            command = command + " " + args;
        }

        // Tack on the command shell prefix to the beginning, quoting the
        // whole argument sequence if necessary
        {
            String shell = net.sf.webcat.core.Application.cmdShell();
            if ( shell != null && shell.length() > 0 )
            {
                if ( shell.charAt( shell.length() - 1 ) == '"' )
                {
                    command = command.replaceAll("\"", "\\\"" );
                    command += '"';
                }
                command = shell + command;
            }
        }

        try
        {
            log.debug( "execute(): " + command );
            proc = runtime.exec( command,
                ( (Application) Application.application() )
                    .subsystemManager().envp(),
                cwd );
            proc.waitFor();
        }
        catch ( InterruptedException e )
        {
            // stopped by timeout
            if ( proc != null )
            {
                proc.destroy();
            }
            throw e;
        }
    }


    // ----------------------------------------------------------
    public String toString()
    {
        return mainFilePath();
    }


    // ----------------------------------------------------------
    public String displayableName()
    {
        Object nameObj = configDescription().valueForKey( "displayableName" );
        String name = ( nameObj == null )
            ? name()
            : nameObj.toString();
        if ( name == null )
        {
            name = uploadedFileName();
        }
        return name;
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
            if ( lastRead != null && modified.after( lastRead ) )
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
                String name = (String)dict.objectForKey( "name" );
                setName( name );
                NSArray options = (NSArray)dict.objectForKey( "options" );
//              log.debug( "options = " + options );
                if ( options != null )
                {
                    MutableDictionary defaults = new MutableDictionary();
                    for ( int i = 0; i < options.count(); i++ )
                    {
                        NSDictionary thisOption = (NSDictionary)options
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
                        }
                    }
                    setDefaultConfigSettings( defaults );
                }
                else
                {
                    setDefaultConfigSettings( null );
                }
                setLastModified(
                    new NSTimestamp( configPlist.lastModified() ) );
                if ( er.extensions.ERXValueUtilities.booleanValue(
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
    /**
     * Retrieve the configured timeout multiplier for this script file.
     * @return the timeout multiplier (scale factor)
     */
    public int timeoutMultiplier()
    {
        return er.extensions.ERXValueUtilities.intValueWithDefault(
            configDescription().valueForKey( "timeoutMultiplier" ), 1 );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the configured timeout internal padding for this script file.
     * @return the timeout internal padding (in seconds)
     */
    public int timeoutInternalPadding()
    {
        return er.extensions.ERXValueUtilities.intValueWithDefault(
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
        EOEditingContext ec = Application.newPeerEditingContext();
        try
        {
            ec.lock();
            autoInstallNewPlugins( ec, autoUpdatePlugins( ec ) );
        }
        finally
        {
            ec.unlock();
            Application.releasePeerEditingContext( ec );
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the name of the directory where a user's scripts are stored.
     * @param author the user
     * @param isData true if this is the directory for a script data/config
     *               file, or false if this is the directory where scripts
     *               themselves are stored
     * @return the directory name
     */
    public static StringBuffer userScriptDirName( User author, boolean isData )
    {
        StringBuffer dir = new StringBuffer( 50 );
        dir.append( isData ? scriptDataRoot()
                           : scriptRoot() );
        dir.append( '/' );
        dir.append( author.authenticationDomain().subdirName() );
        dir.append( '/' );
        dir.append( author.userName() );
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
     * @param author       the user uploading the script
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
    public static ScriptFile createNewScriptFile(
            EOEditingContext    ec,
            User                author,
            String              uploadedName,
            NSData              uploadedData,
            boolean             isData,
            boolean             expand,
            NSMutableDictionary errors
        )
    {
        String userScriptDir = userScriptDirName( author, isData ).toString();
        String subdirName = null;
        uploadedName = ( new File( uploadedName ) ).getName();
        String uploadedNameLC = uploadedName.toLowerCase();
        File toLookFor;
        if ( expand && ( uploadedNameLC.endsWith( ".zip" ) ||
                         uploadedNameLC.endsWith( ".jar" ) ) )
        {
            subdirName = ScriptFile.convertToSubdirName( uploadedName );
            toLookFor = new File( userScriptDir + "/" + subdirName );
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

        ScriptFile script = new ScriptFile();
        ec.insertObject( script );
        script.setUploadedFileName( uploadedName );
        script.setMainFileName( uploadedName );
        script.setLastModified( new NSTimestamp() );
        script.setAuthorRelationship( author );

        // Save the file to disk
        log.debug( "saving to file " + script.mainFilePath() );
        File scriptPath = new File( script.mainFilePath() );
        try
        {
            scriptPath.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream( scriptPath );
            uploadedData.writeToStream( out );
            out.close();
        }
        catch ( java.io.IOException e )
        {
            String msg = e.getMessage();
            errors.setObjectForKey( msg, msg );
            ec.deleteObject( script );
            scriptPath.delete();
            return null;
        }

        if ( expand && ( uploadedNameLC.endsWith( ".zip" ) ||
                         uploadedNameLC.endsWith( ".jar" ) ) )
        {
            try
            {
                //ZipFile zip = new ZipFile( script.mainFilePath() );
                script.setSubdirName( subdirName );
                log.debug( "unzipping to " + script.dirName() );
                net.sf.webcat.archives.ArchiveManager.getInstance()
                    .unpack( new File( script.dirName() ), scriptPath );
                //Grader.unZip( zip, new File( script.dirName() ) );
                //zip.close();
                scriptPath.delete();
            }
            catch ( java.io.IOException e )
            {
                String msg = e.getMessage();
                errors.setObjectForKey( msg, msg );
                script.setSubdirName( subdirName );
                net.sf.webcat.archives.FileUtilities
                    .deleteDirectory( script.dirName() );
                scriptPath.delete();
                log.warn( "error unzipping:", e );
                // throw new NSForwardException( e );
                ec.deleteObject( script );
                return null;
            }
            script.setMainFileName( null );
            String msg = script.initializeConfigAttributes();
            if ( msg != null )
            {
                errors.setObjectForKey( msg, msg );
            }
        }
        return script;
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
            scriptRoot = net.sf.webcat.core.Application
                .configurationProperties().getProperty( "grader.scriptsroot" );
            if ( scriptRoot == null )
            {
                scriptRoot = net.sf.webcat.core.Application
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
            scriptDataRoot = net.sf.webcat.core.Application
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
        ScriptFile                      scriptFile )
    {
        if ( scriptFile != null && !scriptFile.hasSubdir() )
        {
            return "Installed plug-in does not support downloads!";
        }
        
        ScriptFile newScriptFile = null;
        String subdirName = convertToSubdirName( plugin.name() );
        File newScriptPath = null;
        if ( scriptFile == null )
        {
            newScriptFile = new ScriptFile();
            installedBy.editingContext().insertObject( newScriptFile );
            newScriptFile.setLastModified( new NSTimestamp() );
            newScriptFile.setAuthorRelationship( installedBy );
            newScriptFile.setSubdirName( subdirName );
            scriptFile = newScriptFile;
        }
        else if ( !subdirName.equals( scriptFile.subdirName() ) )
        {
            newScriptPath = new File (
                userScriptDirName( installedBy, false ).toString(),
                subdirName );
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
                net.sf.webcat.archives.FileUtilities
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
        File downloadPath = newScriptPath.getParentFile();
        File archiveFile = new File( downloadPath.getAbsolutePath()
            + "/" + plugin.name() + "_" + plugin.currentVersion() + ".jar" );
        downloadPath.mkdirs();
        plugin.downloadTo( downloadPath );
        try
        {
            net.sf.webcat.archives.ArchiveManager.getInstance()
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
    private static NSArray autoUpdatePlugins( EOEditingContext ec )
    {
        NSArray pluginList = EOUtilities.objectsForEntityNamed(
            ec, ENTITY_NAME );
        if ( !Application.configurationProperties()
                 .booleanForKey( NO_AUTO_UPDATE_KEY ) )
        {
            for ( int i = 0; i < pluginList.count(); i++ )
            {
                ScriptFile plugin = (ScriptFile)pluginList.objectAtIndex( i );
                if ( plugin.descriptor().updateIsAvailable() )
                {
                    log.info( "Updating plug-in: \"" + plugin.name() + "\"" );
                    String msg = plugin.installUpdate();
                    if ( msg != null )
                    {
                        log.error( "Error updating plug-in \""
                            + plugin.name() + "\": " + msg );
                    }
                    ec.saveChanges();
                }
                else
                {
                    log.debug( "Plug-in \"" + plugin.name()
                        + "\" is up to date." );
                }
            }
        }
        return pluginList;
    }


    // ----------------------------------------------------------
    private static void autoInstallNewPlugins(
        EOEditingContext ec, NSArray pluginList )
    {
        if ( Application.configurationProperties()
                 .booleanForKey( NO_AUTO_INSTALL_KEY ) )
        {
            return;
        }
        String adminUserName = Application.configurationProperties()
            .getProperty( "AdminUsername" );
        if ( adminUserName == null )
        {
            log.error( "No definition for 'AdminUsername' config property!\n"
                + "Cannot install new plug-ins without admin user name." );
            return;
        }
        User admin = null;
        NSArray candidates = EOUtilities.objectsMatchingKeyAndValue(
            ec,
            User.ENTITY_NAME,
            User.USER_NAME_KEY,
            adminUserName );
        for ( int i = 0; i < candidates.count(); i++ )
        {
            User user = (User)candidates.objectAtIndex( i );
            if ( user.hasAdminPrivileges() )
            {
                if ( admin == null )
                {
                    admin = user;
                }
                else
                {
                    log.warn( "Duplicate admin accounts with user name \""
                        + adminUserName + "\" found.  Using first one." );
                }
            }
        }
        if ( admin == null )
        {
            log.error( "Cannot find admin account with user name \""
                + adminUserName + "\"!" );
            return;
        }
        
        Collection availablePlugins = new HashSet();
        for ( Iterator i = FeatureProvider.providers().iterator();
              i.hasNext(); )
        {
            FeatureProvider provider = (FeatureProvider)i.next();
            if ( provider != null )
            {
                availablePlugins.addAll( provider.plugins() );
            }
        }
        if ( pluginList != null )
        {
            for ( int i = 0; i < pluginList.count(); i++ )
            {
                ScriptFile s = (ScriptFile)pluginList.objectAtIndex( i );
                FeatureDescriptor fd = s.descriptor().providerVersion();
                if ( fd != null )
                {
                    availablePlugins.remove( fd );
                }
            }
        }
        for ( Iterator i = availablePlugins.iterator(); i.hasNext(); )
        {
            FeatureDescriptor plugin = (FeatureDescriptor)i.next();
            log.info( "Installing new plug-in: \"" + plugin.name() + "\"" );
            String msg = installOrUpdate( admin, plugin, false, null );
            if ( msg != null )
            {
                log.error( "Error installing new plug-in \""
                    + plugin.name() + "\": " + msg );
            }
            ec.saveChanges();
        }
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


    //~ Instance/static variables .............................................

    private PluginDescriptor descriptor;

    static private String scriptRoot = null;
    static private String scriptDataRoot = null;
    static Logger log = Logger.getLogger( ScriptFile.class );
}
