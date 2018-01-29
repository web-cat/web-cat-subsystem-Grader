/*==========================================================================*\
|  $Id: submissionResultResource.java,v 1.2 2011/05/19 16:47:53 stedwar2 Exp $
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

package org.webcat.grader.actions;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.webcat.core.Application;
import org.webcat.core.CourseOffering;
import org.webcat.core.DirectAction;
import org.webcat.core.FilePattern;
import org.webcat.core.FileUtilities;
import org.webcat.core.Session;
import org.webcat.core.User;
import org.webcat.grader.Submission;
import org.webcat.grader.SubmissionResult;

//-------------------------------------------------------------------------
/**
* Return resources (like images) to which generated HTML reports refer. Image
* references in a rendered report use this direct action as their source URL
* since this rendered content is not actually stored in a web-accessible
* location.
*
* @author Tony Allevato
* @version $Id: submissionResultResource.java,v 1.2 2011/05/19 16:47:53 stedwar2 Exp $
*/
public class submissionResultResource
   extends DirectAction
{
   //~ Constructor ...........................................................

   // ----------------------------------------------------------
   /**
    * Creates a new object.
    * @param request The incoming request
    */
   public submissionResultResource(WORequest request)
   {
       super(request);
   }


   //~ Public Methods ........................................................

   // ----------------------------------------------------------
   public WOActionResults defaultAction()
   {
       WOResponse response = new WOResponse();

       Session session = (Session) session();

       if (session == null || session.user() == null)
       {
           response.setStatus(WOResponse.HTTP_STATUS_FORBIDDEN);
           return response;
       }

       int resultId = Integer.parseInt(
               request().stringFormValueForKey("id"));
       String path = request().stringFormValueForKey("path");

       if (path.startsWith("/"))
       {
           path = path.substring(1);
       }

       EOEditingContext ec = session.sessionContext();
       SubmissionResult result = SubmissionResult.forId(ec, resultId);

       File requestedFile =
           result.submission().fileForPublicResourceAtPath(path);

       if (requestedFile == null || !userHasPermission(session.user(), result))
       {
           response.setStatus(WOResponse.HTTP_STATUS_FORBIDDEN);
           return response;
       }

       try
       {
           NSData data = new NSData(new FileInputStream(requestedFile),
               (int) requestedFile.length());

           response.setContent(data);
           response.setHeader(FileUtilities.mimeType(requestedFile),
                   "Content-Type");
       }
       catch (IOException e)
       {
           response.setStatus(WOResponse.HTTP_STATUS_FORBIDDEN);
           log.error(e);
       }

       return response;
   }


   // -----------------------------------------------------------
   /**
    * Checks to see if the specified user has permission to access a resource
    * associated with the submission result.
    *
    * @param user the user making the request
    * @param result the SubmissionResult being viewed
    * @param path the path to the resource within the results directory
    * @return true if the user can see the resource, otherwise false
    */
   private boolean userHasPermission(User user, SubmissionResult result)
   {
       if (result.submission() == null
           || result.submission().assignmentOffering() == null
           || result.submission().assignmentOffering().courseOffering() == null)
       {
           return false;
       }

       CourseOffering co =
           result.submission().assignmentOffering().courseOffering();

       if (user.hasAdminPrivileges() || co.isStaff(user))
       {
           // Admins and course staff can access any resource associated with
           // the result, so we don't care about the path.

           return true;
       }

       Submission primarySub = result.submission();
       if (primarySub.primarySubmission() != null)
       {
           primarySub = primarySub.primarySubmission();
       }

       // Check that the user requesting the resource is one of the users
       // associated with the submission (the original submitter or a partner).

       if (primarySub.allUsers().containsObject(user))
       {
           return true;
       }

       return false;
   }


   //~ Instance/static variables .............................................

   private static Logger log = Logger.getLogger(submissionResultResource.class);
}
