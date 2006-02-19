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
import java.io.*;
import java.util.zip.*;
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


    //~ Methods ...............................................................

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
     * @throws java.io.IOException if one occurs
     * @throws InterruptedException if one occurs
     */
    public void execute( String args )
        throws java.io.IOException, InterruptedException
    {
        Runtime runtime = Runtime.getRuntime();
        String  command =
            net.sf.webcat.core.Application.cmdShell() + mainFilePath();
        Process proc = null;
        if ( args != null )
        {
            command = command + " " + args;
        }

        try
        {
            log.debug( "execute(): " + command );
            proc = runtime.exec( command );
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
        String name = name();
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
                String version = (String)dict.objectForKey( "version" );
                if ( version != null )
                {
                    name = name + " (v" + version + ")";
                }
//              log.debug( "script name = " + name );
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
            }
            catch ( Exception e )
            {
                return e.getMessage()
                    + "(error reading script's config.plist file)";
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
        try
        {
            log.debug( "saving to file " + script.mainFilePath() );
            File scriptPath = new File( script.mainFilePath() );
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
            return null;
        }

        if ( expand && ( uploadedNameLC.endsWith( ".zip" ) ||
                         uploadedNameLC.endsWith( ".jar" ) ) )
        {
            try
            {
                File zfile = new File( script.mainFilePath() );
                ZipFile zip = new ZipFile( script.mainFilePath() );
                script.setSubdirName( subdirName );
                log.debug( "unzipping to " + script.dirName() );
                Grader.unZip( zip, new File( script.dirName() ) );
                zip.close();
                zfile.delete();
            }
            catch ( java.io.IOException e )
            {
                String msg = e.getMessage();
                errors.setObjectForKey( msg, msg );
                script.setSubdirName( subdirName );
                Grader.deleteDirectory( script.dirName() );
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

    static private String scriptRoot = null;
    static private String scriptDataRoot = null;
    static Logger log = Logger.getLogger( ScriptFile.class );
}
