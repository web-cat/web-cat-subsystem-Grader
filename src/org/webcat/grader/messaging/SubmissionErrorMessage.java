/*==========================================================================*\
 |  $Id: SubmissionErrorMessage.java,v 1.1 2011/12/25 21:11:41 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2011 Virginia Tech
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

package org.webcat.grader.messaging;

import java.io.File;
import java.util.List;
import org.webcat.core.User;
import org.webcat.core.messaging.Message;
import org.webcat.grader.Submission;
import com.webobjects.foundation.NSArray;

//-------------------------------------------------------------------------
/**
 * A message that is sent to course instructors about a submission problem,
 * possibly including attachments.
 *
 * @author  Stephen Edwards
 * @author  Last changed by: $Author: stedwar2 $
 * @version $Revision: 1.1 $ $Date: 2011/12/25 21:11:41 $
 */
public abstract class SubmissionErrorMessage
    extends Message
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new message associated with the specified submission
     * and set of attachments.
     *
     * @param submission The submission that caused the error.  The
     *                   submission determines the course, and thus the
     *                   instructors who will receive the message.
     * @param attachments A list of files to attach, or null if none.
     */
    public SubmissionErrorMessage(
        Submission submission, List<File> attachments)
    {
        this.submission = submission.localInstance(editingContext());
        this.attachments = attachments;
    }


    // ----------------------------------------------------------
    /**
     * Creates a new message associated with the specified submission
     * and set of attachments.
     *
     * @param submission The submission that caused the error.  The
     *                   submission determines the course, and thus the
     *                   instructors who will receive the message.
     * @param attachmentFileOrDir
     *        If non-null, this should either be a directory, which will
     *        cause all files in the dir to be attached, or a single file,
     *        in which case it alone will be attached.
     */
    public SubmissionErrorMessage(
        Submission submission, File attachmentFileOrDir)
    {
        this(submission, listFromFile(attachmentFileOrDir));
    }


    //~ public Methods ........................................................

    // ----------------------------------------------------------
    @Override
    public synchronized NSArray<User> users()
    {
        if (submission != null
            && submission.assignmentOffering() != null
            && submission.assignmentOffering().courseOffering() != null)
        {
            return submission.assignmentOffering().courseOffering()
                .instructors();
        }
        else
        {
            @SuppressWarnings("unchecked")
            NSArray<User> result = NSArray.EmptyArray;
            return result;
        }
    }


    // ----------------------------------------------------------
    @Override
    public List<File> attachments()
    {
        return attachments;
    }


    //~ private Methods .......................................................

    // ----------------------------------------------------------
    protected Submission submission()
    {
        return submission;
    }


    // ----------------------------------------------------------
    private static List<File> listFromFile(File attachmentFileOrDir)
    {
        List<File> attachments = null;

        if (attachmentFileOrDir != null && attachmentFileOrDir.exists())
        {
            attachments = new java.util.ArrayList<File>(16);

            if (attachmentFileOrDir.isDirectory())
            {
                File[] fileList = attachmentFileOrDir.listFiles();

                for (File file : fileList)
                {
                    if (!file.isDirectory())
                    {
                        attachments.add(file);
                    }
                }
            }
            else
            {
                attachments.add(attachmentFileOrDir);
            }
        }

        return attachments;
    }


    //~ Static/instance variables .............................................

    private Submission submission;
    private List<File> attachments;
}
