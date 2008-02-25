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
import java.io.*;

//-------------------------------------------------------------------------
/**
 * Used to hold the data related to a file upload as part of a submission
 * that has been started (but not yet completed).
 *
 * @author stedwar2
 * @version $Id$
 */
public class SubmissionInProcess
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new object.
     */
    public SubmissionInProcess()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Returns the current uploaded file's data.
     * @return The file's data
     */
    public NSData uploadedFile()
    {
        return uploadedFile;
    }


    // ----------------------------------------------------------
    /**
     * Set the current uploaded file's data.
     * @param data The file's data
     */
    public void setUploadedFile( NSData data )
    {
        uploadedFile = data;
    }


    // ----------------------------------------------------------
    /**
     * Returns the current uploaded file's name.
     * @return The file's name
     */
    public String uploadedFileName()
    {
        return uploadedFileName;
    }


    // ----------------------------------------------------------
    /**
     * Set the current uploaded file's name.
     * @param name The file's name
     */
    public void setUploadedFileName( String name )
    {
        uploadedFileName = name;
    }


    // ----------------------------------------------------------
    /**
     * Returns the stored list of the internal contents of the
     * current uploaded file (if it is a zip or jar).
     * @return A list of its files
     */
    public NSArray uploadedFileList()
    {
        return uploadedFileList;
    }


    // ----------------------------------------------------------
    /**
     * Set the current uploaded file's list of internal contents.
     * @param list An array of files contained within this zip or jar
     */
    public void setUploadedFileList( NSArray list )
    {
        uploadedFileList = list;
    }


    // ----------------------------------------------------------
    /**
     * Determine if a submission has been started, but not yet committed.
     * @return True if a submission is in progress
     */
    public boolean submissionInProcess()
    {
        return submissionInProcess;
    }


    // ----------------------------------------------------------
    /**
     * Set or clear the flag for a submission currently in progress.
     * @param value The flag's new value
     */
    public void setSubmissionInProcess( boolean value )
    {
        submissionInProcess = value;
    }


    // ----------------------------------------------------------
    /**
     * Checks whether the state contains a valid file upload
     * (non-null, non-zero-length data, plus non-null, non-empty
     * file name).
     * @return true if the uploaded file data and name are valid
     */
    public boolean hasValidFileUpload()
    {
        if ( uploadedFileName != null )
        {
            uploadedFileName = ( new File( uploadedFileName ) ).getName();
        }
        return (    uploadedFile          != null
                 && uploadedFile.length() != 0
                 && uploadedFileName      != null
                 && !uploadedFileName.equals( "" ) );
    }


    // ----------------------------------------------------------
    /**
     * Clear info related to the uploaded file.
     */
    public void clearUpload()
    {
        uploadedFile     = null;
        uploadedFileName = null;
        uploadedFileList = null;
    }


    //~ Instance/static variables .............................................

    private NSData  uploadedFile;
    private String  uploadedFileName;
    private NSArray uploadedFileList;
    private boolean submissionInProcess = false;
}
