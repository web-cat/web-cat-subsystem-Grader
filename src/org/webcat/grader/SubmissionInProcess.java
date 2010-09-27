/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2008 Virginia Tech
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
import java.io.*;
import org.webcat.archives.IArchiveEntry;
import org.webcat.core.User;

//-------------------------------------------------------------------------
/**
 * Used to hold the data related to a file upload as part of a submission
 * that has been started (but not yet completed).
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
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
        if (uploadedFileName == null) return;

        // Depending on the client's browser and OS, the file name
        // might be a relative or absolute path, rather than just
        // a file name.  Try to strip off any leading directory
        // component in an OS-agnostic way.
        if (uploadedFileName.endsWith("/") || uploadedFileName.endsWith("\\"))
        {
            uploadedFileName = uploadedFileName.substring(
                0, uploadedFileName.length() - 1);
        }
        int pos = uploadedFileName.lastIndexOf('/');
        if (pos >= 0)
        {
            uploadedFileName = uploadedFileName.substring(pos + 1);
        }
        pos = uploadedFileName.lastIndexOf('\\');
        if (pos >= 0)
        {
            uploadedFileName = uploadedFileName.substring(pos + 1);
        }
        if ("".equals(uploadedFileName))
        {
            // Give it a default, if trimming the dir eliminated everything
            uploadedFileName = "file";
        }
    }


    // ----------------------------------------------------------
    /**
     * Returns the stored list of the internal contents of the
     * current uploaded file (if it is a zip or jar).
     * @return A list of its files
     */
    public NSArray<IArchiveEntry> uploadedFileList()
    {
        return uploadedFileList;
    }


    // ----------------------------------------------------------
    /**
     * Set the current uploaded file's list of internal contents.
     * @param list An array of files contained within this zip or jar
     */
    public void setUploadedFileList( NSArray<IArchiveEntry> list )
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
        return submission != null;
    }


    // ----------------------------------------------------------
    /**
     * Get the submission currently in progress.
     * @return The submission object, or null if none is in procress
     */
    public Submission submission()
    {
        return submission;
    }


    // ----------------------------------------------------------
    /**
     * Set the submission currently in progress.
     * @param submission The submission object, or null to indicate no
     * submission is in progress anymore
     */
    public void setSubmission( Submission submission )
    {
        this.submission = submission;
    }


    // ----------------------------------------------------------
    /**
     * Returns the current partners for this submission.
     * @return The partners
     */
    public NSArray<User> partners()
    {
        return partners;
    }


    // ----------------------------------------------------------
    /**
     * Set the current partners for this submission.
     * @param somePartners The partners
     */
    public void setPartners( NSArray<User> somePartners )
    {
        partners = somePartners;
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

    private NSData                 uploadedFile;
    private String                 uploadedFileName;
    private NSArray<IArchiveEntry> uploadedFileList;
    private Submission             submission;
    private NSArray<User>          partners;
}
