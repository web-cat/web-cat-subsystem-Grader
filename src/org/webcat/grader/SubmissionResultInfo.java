/*==========================================================================*\
 |  $Id: SubmissionResultInfo.java,v 1.7 2014/06/16 17:21:53 stedwar2 Exp $
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

import org.webcat.core.CourseOffering;
import org.webcat.core.User;
import org.webcat.ui.util.ComponentIDGenerator;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import er.extensions.eof.ERXQ;
import er.extensions.foundation.ERXArrayUtilities;

// -------------------------------------------------------------------------
/**
 *  Renders a descriptive table containing a submission result's basic
 *  identifying information.  An optional submission file stats object,
 *  if present, will be used to present file-specific data.
 *
 *  @author  Stephen Edwards
 *  @author  Latest changes by: $Author: stedwar2 $
 *  @version $Revision: 1.7 $, $Date: 2014/06/16 17:21:53 $
 */
public class SubmissionResultInfo
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor.
     * @param context The page's context
     */
    public SubmissionResultInfo( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public Submission          submission;
    public SubmissionFileStats submissionFileStats;
    public boolean             showFileInfo     = false;
    public boolean             allowPartnerEdit = false;
    public boolean             includeSeparator = true;
    public User                aPartner;
    public int                 rowNumber;
    public NSArray<User>       originalPartners;
    public NSArray<User>       partnersForEditing;

    public ComponentIDGenerator idFor;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        idFor = new ComponentIDGenerator(this);

        rowNumber = 0;

        if (submission != null)
        {
            // TODO: fix this with auto-migration
            submission.migratePartnerLink();

            originalPartners = submission.allUsers();
            partnersForEditing = originalPartners.mutableClone();
        }

        super.beforeAppendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public void setPartnersForEditing(NSArray<User> users)
    {
        partnersForEditing = users;
        NSArray<User> partnersWithoutPrincipal = users;
        if (!partnersForEditing.contains(submission.user()))
        {
            NSMutableArray<User> p = partnersForEditing.mutableClone();
            p.addObject(submission.user());
            partnersForEditing = p;
        }
        else
        {
            partnersForEditing = partnersForEditing.mutableClone();
            NSMutableArray<User> p = partnersWithoutPrincipal.mutableClone();
            p.removeObject(submission.user());
            partnersWithoutPrincipal = p;
        }

        // TODO: This now changes partnering for ALL submissions in a chain,
        // but there should probably be an option in the partner editing
        // dialog that allows the user to choose whether to change all, or
        // just the current one.
        for (Submission s : submission.allSubmissions())
        {
            NSArray<User> originals = s.allPartners();
            @SuppressWarnings("unchecked")
            NSArray<User> partnersToRemove = ERXArrayUtilities.arrayMinusArray(
                originals, partnersWithoutPrincipal);
            s.unpartnerFrom(partnersToRemove);

            @SuppressWarnings("unchecked")
            NSArray<User> partnersToAdd = ERXArrayUtilities.arrayMinusArray(
                partnersWithoutPrincipal, originals);
            s.partnerWith(partnersToAdd);
        }

        applyLocalChanges();

        originalPartners = partnersForEditing;
    }


    // ----------------------------------------------------------
    public EOQualifier qualifierForStudentsInCourse()
    {
        CourseOffering courseOffering =
            submission.assignmentOffering().courseOffering();
        NSArray<CourseOffering> offerings =
            CourseOffering.offeringsForSemesterAndCourse(localContext(),
                courseOffering.course(),
                courseOffering.semester());

        EOQualifier[] enrollmentQuals = new EOQualifier[offerings.count()];
        int i = 0;
        for (CourseOffering offering : offerings)
        {
            enrollmentQuals[i++] = User.enrolledIn.is(offering);
        }

        return ERXQ.or(enrollmentQuals);
    }
}
