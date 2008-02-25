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
import com.webobjects.appserver.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.sf.webcat.archives.ArchiveManager;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class presents the list of scripts (grading steps) that
 * are available for selection.
 *
 * @author Stephen Edwards
 * @version $Id$
 */
public class EditScriptFilesPage
    extends GraderComponent
    implements EditFilePage.FileEditListener
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public EditScriptFilesPage( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public ScriptFile scriptFile;
    public File       base;
    public boolean    isEditable;
    public boolean    allowSelectDir;
    public NSArray    allowSelectExtensions;
    public String     folderName;
    public String     aFolder;
    public String     selectedParentFolderForSubFolder;
    public String     selectedParentFolderForUpload;
    public NSMutableArray folderList;
    public NSData     uploadedFile2;
    public String     uploadedFileName2;
    public NSData     uploadedFile3;
    public String     uploadedFileName3;
    public boolean    unzip = false;
    public FileBrowser.FileSelectionListener fileSelectionListener = null;
    public String     currentSelection;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "listener = " + fileSelectionListener );
        folderName = null;
        if ( base == null )
        {
            if ( scriptFile.hasSubdir() )
            {
                base = new File( scriptFile.dirName() );
            }
            else
            {
                base = new File( scriptFile.mainFilePath() );
            }
        }
        if ( folderList == null )
        {
            folderList = new NSMutableArray();
            String parent = base.getParent();
            int stripLength = 0;
            if ( parent != null && parent.length() > 0 )
            {
                stripLength = parent.length() + 1;
            }
            addFolders( folderList, base, stripLength );
        }
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    private void addFolders( NSMutableArray list, File file, int stripLength )
    {
        if ( !file.isDirectory() ) return;
        String name = file.getName();
        if ( name.equals( "." ) || name.equals( ".." ) ) return;
        name = file.getPath().substring( stripLength )
            .replaceAll( "\\\\", "/" );
        list.addObject( name );
        File[] files = file.listFiles();
        for ( int i = 0; i < files.length; i++ )
        {
            addFolders( list, files[i], stripLength );
        }
    }


    // ----------------------------------------------------------
    public String sideStepTitle()
    {
        if ( title == null )
        {
            title = isEditable ? "Edit Your " : "Browse Your ";
            if ( scriptFile == null )
            {
                title += "Configuration ";
            }
            else
            {
                title += "Script ";
            }
            if ( base.isDirectory() )
            {
                title += "Files";
            }
            else
            {
                title += "File";
            }
        }
        return title;
    }


    // ----------------------------------------------------------
    public String browserTitle()
    {
        return sideStepTitle();
    }


    // ----------------------------------------------------------
    public boolean allowSelection()
    {
        return fileSelectionListener != null;
    }


    // ----------------------------------------------------------
    public WOComponent createFolder()
    {
        if (!applyLocalChanges()) return null;
        if ( folderName == null || folderName.length() == 0 )
        {
            error( "Please enter a folder name." );
        }
        else
        {
            File target =
                new File( base.getParent(),
                          selectedParentFolderForSubFolder + "/" + folderName );
            try
            {
                target.mkdirs();
            }
            catch ( Exception e )
            {
                error( e.getMessage() );
            }
        }
        folderList = null;
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent uploadFile()
    {
        if (!applyLocalChanges()) return null;
        if ( unzip && WCFile.isArchiveFile( uploadedFileName2 ) )
        {
            File target =
                new File( base.getParent(), selectedParentFolderForUpload );
            // ZipInputStream zipStream =
            //    new ZipInputStream( uploadedFile2.stream() );
            try
            {
                ArchiveManager.getInstance().unpack(
                    target, uploadedFileName2, uploadedFile2.stream() );
            }
            catch ( java.io.IOException e )
            {
                error( e.getMessage() );
            }
            folderList = null;
        }
        else
        {
            uploadedFileName2 = new File( uploadedFileName2 ).getName();
            File target =
                new File( base.getParent(), selectedParentFolderForUpload
                                            + "/" + uploadedFileName2 );
            try
            {
                FileOutputStream out = new FileOutputStream( target );
                uploadedFile2.writeToStream( out );
                out.close();
            }
            catch ( java.io.IOException e )
            {
                error( e.getMessage() );
            }
        }
        if ( scriptFile != null )
        {
            scriptFile.initializeConfigAttributes();
            applyLocalChanges();
        }
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent replaceEntireFolder()
    {
        if (!applyLocalChanges()) return null;
        if ( WCFile.isArchiveFile( uploadedFileName3 ) )
        {
            net.sf.webcat.archives.FileUtilities.deleteDirectory( base );
            base.mkdirs();
            // ZipInputStream zipStream =
            //    new ZipInputStream( uploadedFile3.stream() );
            try
            {
                ArchiveManager.getInstance().unpack(
                    base, uploadedFileName3, uploadedFile3.stream() );
            }
            catch ( java.io.IOException e )
            {
                error( e.getMessage() );
            }
            if ( scriptFile != null )
            {
                scriptFile.initializeConfigAttributes();
                applyLocalChanges();
            }
            folderList = null;
        }
        else
        {
            error( "To replace this entire folder, you must upload a "
                          + "zip or a jar file." );
        }
        return null;
    }


    // ----------------------------------------------------------
    public boolean nextEnabled()
    {
        return !hideNextBack
            && ( nextPage != null || currentTab().hasNextSibling() );
    }


    // ----------------------------------------------------------
    public boolean backEnabled()
    {
        return !hideNextBack && super.backEnabled();
    }


    // ----------------------------------------------------------
    public void hideNextAndBack( boolean value )
    {
        hideNextBack = value;
    }


    // ----------------------------------------------------------
    public void saveFile( String fileName )
    {
        if ( scriptFile != null )
        {
            scriptFile.initializeConfigAttributes();
            scriptFile.setLastModified( new NSTimestamp() );
            applyLocalChanges();
        }
    }


    // ----------------------------------------------------------
    public EditFilePage.FileEditListener thisPage()
    {
        return this;
    }


    //~ Instance/static variables .............................................

    private String title;
    private boolean hideNextBack = false;
    static Logger log = Logger.getLogger( EditScriptFilesPage.class );
}
