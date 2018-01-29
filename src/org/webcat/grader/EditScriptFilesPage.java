/*==========================================================================*\
 |  $Id: EditScriptFilesPage.java,v 1.6 2013/10/09 00:04:21 stedwar2 Exp $
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

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.log4j.Logger;
import org.webcat.archives.ArchiveManager;
import org.webcat.core.*;
import org.webcat.ui.generators.JavascriptGenerator;

// -------------------------------------------------------------------------
/**
 * This class presents the list of scripts (grading steps) that
 * are available for selection.
 *
 * @author  Stephen Edwards
 * @author  Latest changes by: $Author: stedwar2 $
 * @version $Revision: 1.6 $, $Date: 2013/10/09 00:04:21 $
 */
public class EditScriptFilesPage
    extends GraderComponent
    implements EditFilePage.FileEditListener, FileBrowser.FileSelectionListener
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public EditScriptFilesPage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public GradingPlugin          gradingPlugin;
    public File                   base;
    public boolean                isEditable;
    public boolean                allowSelectDir;
    public NSArray<String>        allowSelectExtensions;
    public String                 folderName;
    public String                 aFolder;
    public String                 selectedParentFolderForSubFolder;
    public String                 selectedParentFolderForUpload;
    public NSMutableArray<String> folderList;
    public NSData                 uploadedFile2;
    public String                 uploadedFileName2;
    public NSData                 uploadedFile3;
    public String                 uploadedFileName3;
    public boolean                unzip = false;
    public FileBrowser.FileSelectionListener fileSelectionListener = null;
    public String                 currentSelection;
    public String                 browserId;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug("listener = " + fileSelectionListener);
        rescanFolders();
        if ((selectedParentFolderForSubFolder == null
             || selectedParentFolderForUpload == null)
            && currentSelection != null)
        {
            int pos = currentSelection.lastIndexOf('/');
            if (pos > 0)
            {
                String path = currentSelection.substring(0, pos);
                if (selectedParentFolderForSubFolder == null)
                {
                    selectedParentFolderForSubFolder = path;
                }
                if (selectedParentFolderForUpload == null)
                {
                    selectedParentFolderForUpload = path;
                }
            }
        }
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    protected void afterAppendToResponse(
        WOResponse response, WOContext context)
    {
        super.afterAppendToResponse(response, context);
        showFiles.clear();
    }


    // ----------------------------------------------------------
    public String sideStepTitle()
    {
        if (title == null)
        {
            title = isEditable ? "Edit Your " : "Browse Your ";
            if (gradingPlugin == null)
            {
                title += "Configuration ";
            }
            else
            {
                title += "Plug-in ";
            }
            if (base.isDirectory())
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
    public boolean allowSelection()
    {
        return fileSelectionListener != null;
    }


    // ----------------------------------------------------------
    public void setSelectedParentFolderForSubFolder(String value)
    {
        selectedParentFolderForSubFolder = value;
        log.debug("setSelectedParentFolderForSubFolder(\"" + value + "\")");
    }


    // ----------------------------------------------------------
    public WOActionResults createFolder()
    {
        log.debug("createFolder()");
        JavascriptGenerator page = new JavascriptGenerator();
        page.refresh("error-panel");
        if (applyLocalChanges())
        {
            if (folderName == null || folderName.length() == 0)
            {
                log.debug("createFolder(): no folder name");
                error("Please enter a folder name.");
            }
            else
            {
                String fullName =
                    selectedParentFolderForSubFolder + "/" + folderName;
                File target = new File(base.getParent(), fullName);
                log.debug("createFolder(): attempting to create " + target);
                try
                {
                    target.mkdirs();
                }
                catch (Exception e)
                {
                    error(e.getMessage());
                }
                rescanFolders();
                page.refresh(browserId, "folderControls");
                showFiles.add(fullName);
            }
        }
        else
        {
            log.debug("createFolder(): applyLocalChanges() failed");
        }
        return page;
    }


    // ----------------------------------------------------------
    public WOActionResults uploadFile()
    {
//        JavascriptGenerator page = new JavascriptGenerator();
//        page.refresh("error-panel");
        if (applyLocalChanges())
        {
            if (unzip && FileUtilities.isArchiveFile(uploadedFileName2))
            {
                File target =
                    new File(base.getParent(), selectedParentFolderForUpload);
                try
                {
                    ArchiveManager.getInstance().unpack(
                        target, uploadedFileName2, uploadedFile2.stream());
                }
                catch (java.io.IOException e)
                {
                    error(e.getMessage());
                }
//                page.refresh("folderControls");
                showFiles.add(selectedParentFolderForUpload);
                rescanFolders();
            }
            else
            {
                uploadedFileName2 = new File(uploadedFileName2).getName();
                String fullName =
                    selectedParentFolderForUpload + "/" + uploadedFileName2;
                File target = new File(base.getParent(), fullName);
                try
                {
                    FileOutputStream out = new FileOutputStream(target);
                    uploadedFile2.writeToStream(out);
                    out.close();
                }
                catch (java.io.IOException e)
                {
                    error(e.getMessage());
                }
                showFiles.add(fullName);
            }
            if (gradingPlugin != null)
            {
                gradingPlugin.initializeConfigAttributes();
                applyLocalChanges();
            }
//            page.refresh(browserId);
        }
//        return page;
        return null;
    }


    // ----------------------------------------------------------
    public boolean nextEnabled()
    {
        return !hideNextBack
            && (nextPage != null || currentTab().hasNextSibling());
    }


    // ----------------------------------------------------------
    public boolean backEnabled()
    {
        return !hideNextBack && super.backEnabled();
    }


    // ----------------------------------------------------------
    public void hideNextAndBack(boolean value)
    {
        hideNextBack = value;
    }


    // ----------------------------------------------------------
    public void saveFile(String fileName)
    {
        if (gradingPlugin != null)
        {
            gradingPlugin.initializeConfigAttributes();
            gradingPlugin.setLastModified(new NSTimestamp());
            applyLocalChanges();
        }
    }


    // ----------------------------------------------------------
    public WOComponent selectFile(String filePath)
    {
        WOComponent page = this;
        if (fileSelectionListener != null)
        {
            page = fileSelectionListener.selectFile(filePath);
        }

        if (page == this)
        {
            currentSelection = base.getParentFile().getName() + "/" + filePath;
        }
        return page;
    }


    // ----------------------------------------------------------
    public NSArray<String> focusedFiles()
    {
        return showFiles;
    }


    // ----------------------------------------------------------
    /**
     * This property is read-only, so the setter does nothing and is
     * provided only for synchronized binding pushing.
     */
    public void setFocusedFiles(NSArray<String> values)
    {
        // ignore
    }


    // ----------------------------------------------------------
    public WCComponent thisPage()
    {
        return this;
    }


    // ----------------------------------------------------------
    private void rescanFolders()
    {
        folderName = null;
        folderList = null;
        if (base == null)
        {
            if (gradingPlugin.hasSubdir())
            {
                base = new File(gradingPlugin.dirName());
            }
            else
            {
                base = new File(gradingPlugin.mainFilePath());
            }
        }
        if (folderList == null)
        {
            folderList = new NSMutableArray<String>();
            String parent = base.getParent();
            int stripLength = 0;
            if (parent != null && parent.length() > 0)
            {
                stripLength = parent.length() + 1;
            }
            addFolders(folderList, base, stripLength);
        }
    }


    // ----------------------------------------------------------
    private void addFolders(
        NSMutableArray<String> list, File folder, int stripLength)
    {
        if (!folder.isDirectory())
        {
            return;
        }
        String name = folder.getName();
        if (name.equals( "." ) || name.equals( ".." ))
        {
            return;
        }
        name = folder.getPath().substring(stripLength).replaceAll("\\\\", "/");
        list.addObject(name);
        for (File file : folder.listFiles())
        {
            addFolders(list, file, stripLength);
        }
    }


    //~ Instance/static variables .............................................

    private String title;
    private boolean hideNextBack = false;
    private NSMutableArray<String> showFiles = new NSMutableArray<String>(1);

    static Logger log = Logger.getLogger(EditScriptFilesPage.class);
}
