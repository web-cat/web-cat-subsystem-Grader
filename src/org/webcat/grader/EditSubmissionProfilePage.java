/*==========================================================================*\
 |  $Id: EditSubmissionProfilePage.java,v 1.3 2013/12/11 14:43:17 stedwar2 Exp $
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

import java.text.DecimalFormat;
import java.text.Format;
import com.webobjects.appserver.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class presents the list of current submission profiles that
 * are available for selection.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.3 $ $Date: 2013/12/11 14:43:17 $
 */
public class EditSubmissionProfilePage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public EditSubmissionProfilePage(WOContext context)
    {
        super(context);
    }


    //~ KVC Attributes (must be public) .......................................

    public SubmissionProfile submissionProfile;
    public Long availableTimeDelta; // null for no limit
    public Long deadTimeDelta;      // null for no late submissions
    public Long earlyBonusUnitTime;
    public Long latePenaltyUnitTime;
    public SubmissionProfile.TimeUnit unit;
    public SubmissionProfile.TimeUnit availableTimeDeltaUnit;
    public SubmissionProfile.TimeUnit deadTimeDeltaUnit;
    public SubmissionProfile.TimeUnit earlyUnitTimeUnit;
    public SubmissionProfile.TimeUnit lateUnitTimeUnit;
    public double correctnessPoints;
    public String submissionMethod;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        log.debug("starting appendToResponse()");
        if (submissionProfile == null)
        {
            if (prefs().assignment() != null)
            {
                submissionProfile =
                    prefs().assignment().submissionProfile();
            }
            else
            {
                submissionProfile = prefs().assignmentOffering()
                    .assignment().submissionProfile();
            }
            correctnessPoints = submissionProfile.availablePoints()
                - submissionProfile.taPoints()
                - submissionProfile.toolPoints();
        }
        initializeTimeFields();
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    public void initializeTimeFields()
    {
        // First, fill availableTimeDelta data members
        if (submissionProfile.availableTimeDeltaRaw() == null)
        {
            availableTimeDelta     = null;
            availableTimeDeltaUnit = SubmissionProfile.timeUnits[2];  // Days
        }
        else
        {
            long storedAvailableTimeDelta =
                submissionProfile.availableTimeDelta();
            for (int i = SubmissionProfile.timeUnits.length - 1; i >= 0; i--)
            {
                availableTimeDeltaUnit = SubmissionProfile.timeUnits[i];
                if (availableTimeDeltaUnit.isUnitFor(storedAvailableTimeDelta)
                    || i == 0)
                {
                    availableTimeDelta = availableTimeDeltaUnit
                        .unitsFromRaw(storedAvailableTimeDelta);
                    break;
                }
            }
        }
        // Next, fill deadTimeDelta data members
        if (submissionProfile.deadTimeDeltaRaw() == null)
        {
            deadTimeDelta     = 0L;
            deadTimeDeltaUnit = SubmissionProfile.timeUnits[2];  // Days
        }
        else
        {
            long storedDeadTimeDelta = submissionProfile.deadTimeDelta();
            for (int i = SubmissionProfile.timeUnits.length - 1; i >= 0; i--)
            {
                deadTimeDeltaUnit = SubmissionProfile.timeUnits[i];
                if (deadTimeDeltaUnit.isUnitFor(storedDeadTimeDelta)
                    || i == 0)
                {
                    deadTimeDelta = deadTimeDeltaUnit
                        .unitsFromRaw(storedDeadTimeDelta);
                    break;
                }
            }
        }
        // Next, fill earlyBonusTimeUnit data members
        if (submissionProfile.earlyBonusUnitTimeRaw() == null)
        {
            earlyBonusUnitTime = 0L;
            earlyUnitTimeUnit  = SubmissionProfile.timeUnits[2];  // Days
        }
        else
        {
            long storedEarlyBonusUnitTime =
                submissionProfile.earlyBonusUnitTime();
            for (int i = SubmissionProfile.timeUnits.length - 1; i >= 0; i--)
            {
                earlyUnitTimeUnit = SubmissionProfile.timeUnits[i];
                if (earlyUnitTimeUnit.isUnitFor(storedEarlyBonusUnitTime)
                    || i == 0)
                {
                    earlyBonusUnitTime = earlyUnitTimeUnit
                        .unitsFromRaw(storedEarlyBonusUnitTime);
                    break;
                }
            }
        }
        // Finally, fill latePenaltyTimeUnit data members
        if (submissionProfile.latePenaltyUnitTimeRaw() == null)
        {
            latePenaltyUnitTime = 0L;
            lateUnitTimeUnit    = SubmissionProfile.timeUnits[2];  // Days
        }
        else
        {
            long storedLatePenaltyUnitTime =
                submissionProfile.latePenaltyUnitTime();
            for (int i = SubmissionProfile.timeUnits.length - 1; i >= 0; i--)
            {
                lateUnitTimeUnit = SubmissionProfile.timeUnits[i];
                if (lateUnitTimeUnit.isUnitFor(storedLatePenaltyUnitTime)
                    || i == 0)
                {
                    latePenaltyUnitTime = lateUnitTimeUnit
                        .unitsFromRaw(storedLatePenaltyUnitTime);
                    break;
                }
            }
        }
    }


    // ----------------------------------------------------------
    public void saveTimeFields()
    {
        submissionProfile.setAvailableTimeDeltaRaw(
            availableTimeDeltaUnit.rawFromUnits(availableTimeDelta));
        submissionProfile.setDeadTimeDeltaRaw(
            deadTimeDeltaUnit.rawFromUnits(deadTimeDelta));
        submissionProfile.setEarlyBonusUnitTimeRaw(
            earlyUnitTimeUnit.rawFromUnits(earlyBonusUnitTime));
        submissionProfile.setLatePenaltyUnitTimeRaw(
            lateUnitTimeUnit.rawFromUnits(latePenaltyUnitTime));
    }


    // ----------------------------------------------------------
    public WOComponent next()
    {
        applyLocalChanges();
        return super.next();
    }


    // ----------------------------------------------------------
    public WOComponent cancel()
    {
        cancelLocalChanges();
        return super.next();
    }


    // ----------------------------------------------------------
    public boolean applyLocalChanges()
    {
        saveTimeFields();
        return super.applyLocalChanges();
    }


    // ----------------------------------------------------------
    public String maxFileUploadSize()
    {
        return SubmissionProfile.formatSizeValue(
            submissionProfile.maxFileUploadSizeRaw());
    }


    // ----------------------------------------------------------
    public String maxMaxFileUploadSize()
    {
        return SubmissionProfile.formatSizeValue(
            SubmissionProfile.maxMaxFileUploadSize());
    }


    // ----------------------------------------------------------
    public String defaultMaxFileUploadSize()
    {
        return SubmissionProfile.formatSizeValue(
            SubmissionProfile.defaultMaxFileUploadSize());
    }


    // ----------------------------------------------------------
    public void setMaxFileUploadSize(String valueAsString)
    {
        Long value = null;
        if (valueAsString != null)
        {
            try
            {
                value = new Long(
                    SubmissionProfile.parseFormattedLong(valueAsString));
            }
            catch (NumberFormatException e)
            {
                // set error message if size is out of range
                error("Unable to interpret \"" + valueAsString + "\" as a "
                    + "number for the max upload size.",
                    "formatMaxSize");
            }
        }
        clearMessage("formatMaxSize");
        if (value != null
            && value.longValue() > SubmissionProfile.maxMaxFileUploadSize())
        {
            // set error message if size is out of range
            error("The maximum upload size allowed is "
                + SubmissionProfile.maxMaxFileUploadSize()
                + ".  Contact the administrator for higher limits.",
                "tooLarge");
        }
        else
        {
            clearMessage("tooLarge");
        }
        // This will automatically restrict to the max value anyway
        submissionProfile.setMaxFileUploadSizeRaw(value);
    }


    // ----------------------------------------------------------
    /* (non-Javadoc)
     * @see com.webobjects.appserver.WOComponent#takeValuesFromRequest(com.webobjects.appserver.WORequest, com.webobjects.appserver.WOContext)
     */
    public void takeValuesFromRequest(WORequest request, WOContext context)
    {
        super.takeValuesFromRequest(request, context);
        log.debug("taking values");
        if (submissionProfile != null)
        {
            submissionProfile.setAvailablePoints(
                correctnessPoints
                + submissionProfile.taPoints()
                + submissionProfile.toolPoints());
        }
    }


    // ----------------------------------------------------------
    public String inlineJavaScript()
    {
        return INLINE_JAVASCRIPT;
    }


    // ----------------------------------------------------------
    public Format doubleFormatter()
    {
        return doubleFormatter;
    }


    //~ Instance/static variables .............................................

    private static final String INLINE_JAVASCRIPT =
        "<script type=\"text/javascript\">\n"
        + "<!-- Begin\n"
        + "function startCalc(){\n"
        + "  interval = setInterval(\"calc()\",1);\n"
        + "}\n"
        + "function calc(){\n"
        + "  document.WizardForm.total.value =\n"
        + "      ( document.WizardForm.taPoints.value * 1 )\n"
        + "      + ( document.WizardForm.toolPoints.value * 1 )\n"
        + "      + ( document.WizardForm.correctnessPoints.value * 1 );\n"
        + "}\n"
        + "function stopCalc(){\n"
        + "  clearInterval(interval);\n"
        + "}\n"
        + "// End -->\n"
        + "</script>";

    private static final Format doubleFormatter = new DecimalFormat("0.######");
    static Logger log = Logger.getLogger(EditSubmissionProfilePage.class);
}
