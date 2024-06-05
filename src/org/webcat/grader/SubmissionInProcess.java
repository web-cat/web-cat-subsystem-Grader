/*==========================================================================*\
 |  Copyright (C) 2006-2021 Virginia Tech
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
import org.webcat.core.Disposable;
import org.webcat.core.PathMatcher;
import org.webcat.core.User;

//-------------------------------------------------------------------------
/**
 * Used to hold the data related to a file upload as part of a submission
 * that has been started (but not yet completed).
 *
 * @author  Stephen Edwards
 */
public class SubmissionInProcess
    implements Disposable
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
    public void setUploadedFile(NSData data)
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
    public void setUploadedFileName(String name)
    {
        uploadedFileName = name;
        if (uploadedFileName == null)
        {
            return;
        }

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
        pos = uploadedFileName.indexOf(";*");
        if (pos > 0)
        {
            uploadedFileName = uploadedFileName.substring(0, pos);
        }

        // If file name starts with dot, add user name as a prefix
        if (uploadedFileName.startsWith("."))
        {
            String userName = null;
            if (user != null)
            {
                userName = user.userName();
            }
            if (userName == null || userName.isEmpty())
            {
                userName = "sub";
            }
            uploadedFileName = userName + uploadedFileName;
        }

        if (uploadedFileName.isEmpty())
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
    public void setUploadedFileList(NSArray<IArchiveEntry> list)
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
        return user != null;
    }


    // ----------------------------------------------------------
    /**
     * Begin a submission for the specified user with the specified
     * submission number.
     * @param forUser The user making the submission
     * @param subNumber The number of the user's submission
     */
    public void startSubmission(
        User forUser, AssignmentOffering assignmentOffering, int subNumber)
    {
        user = forUser;
        offering = assignmentOffering;
        submitNumber = subNumber;
    }


    // ----------------------------------------------------------
    /**
     * Get the user making the submission currently in progress.
     * @return The user, or null if none is in process
     */
    public User user()
    {
        return user;
    }


    // ----------------------------------------------------------
    /**
     * Get the assignment offering for this submission.
     * @return The assignment offering, or null if none is in process
     */
    public AssignmentOffering assignmentOffering()
    {
        return offering;
    }


    // ----------------------------------------------------------
    /**
     * Get the number of the submission currently in progress.
     * @return The number, or -1 if none is in process
     */
    public int submitNumber()
    {
        return submitNumber;
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
    public void setPartners(NSArray<User> somePartners)
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
        if (uploadedFileName != null)
        {
            uploadedFileName = (new File(uploadedFileName)).getName();
        }
        return uploadedFile          != null
            && uploadedFile.length() != 0
            && uploadedFileName      != null
            && !uploadedFileName.isEmpty();
    }


    // ----------------------------------------------------------
    /**
     * Gets a list of any missing required files in the submission, based on
     * the requirements of the specified submission profile.
     *
     * @param profile the submission profile to check against
     * @return an array of any missing required files, which will be empty if
     *     all required files are present
     */
    public NSArray<String> findMissingRequiredFiles(SubmissionProfile profile)
    {
        String requiredString = profile.requiredFilePatterns();

        NSMutableArray<String> missing = new NSMutableArray<String>();

        if (requiredString != null)
        {
            String[] required = requiredString.split(",");

            NSMutableDictionary<String, Boolean> requiredItemPatterns =
                new NSMutableDictionary<String, Boolean>();

            // Initialize each required pattern as "not found" initially.

            for (String pattern : required)
            {
                requiredItemPatterns.put(pattern, false);
            }

            for (IArchiveEntry entry : uploadedFileList())
            {
                if (!entry.isDirectory())
                {
                    for (String requiredPattern : required)
                    {
                        PathMatcher pattern = new PathMatcher(requiredPattern);

                        // If some file matches the pattern, we mark it as
                        // found.

                        if (pattern.matches(entry.getName()))
                        {
                            requiredItemPatterns.put(requiredPattern, true);
                        }
                    }
                }
            }

            // Look for any patterns that were never found and add them to the
            // array that will be returned.

            for (String requiredPattern : requiredItemPatterns.allKeys())
            {
                if (requiredItemPatterns.objectForKey(requiredPattern) == false)
                {
                    missing.addObject(requiredPattern);
                }
            }
        }

        return missing;
    }


    // ----------------------------------------------------------
    public void dispose()
    {
        cancelSubmission();
    }


    // ----------------------------------------------------------
    /**
     * End the current submission in process, if there is one, and clear
     * any upload file data.
     */
    public void cancelSubmission()
    {
        user = null;
        offering = null;
        submitNumber = -1;
        partners = null;
        clearUpload();
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
    private User                   user;
    private AssignmentOffering     offering;
    private int                    submitNumber = -1;
    private NSArray<User>          partners;
}
