/*==========================================================================*\
 |  Copyright (C) 2018 Virginia Tech
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

package org.webcat.grader.lti;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;
import org.webcat.core.User;
import org.webcat.core.lti.LMSInstance;
import org.webcat.grader.AssignmentOffering;
import org.webcat.grader.Submission;
import com.webobjects.eocontrol.EOEditingContext;

// -------------------------------------------------------------------------
/**
 * Represents a grade cell in the LTI consumer gradebook for a given
 * student on a given assignment.
 *
 * @author Stephen Edwards
 */
public class LISResultId
    extends _LISResultId
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new LISResultId object.
     */
    public LISResultId()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public static LISResultId ensureExists(
        EOEditingContext editingContext,
        User userValue,
        AssignmentOffering assignmentOfferingValue,
        LMSInstance lms,
        String lisResultSourcedIdValue)
    {
        LISResultId result = uniqueObjectMatchingQualifier(editingContext,
            user.is(userValue).and(
            assignmentOffering.is(assignmentOfferingValue)));
        if (result == null)
        {
            result = create(editingContext, lisResultSourcedIdValue,
                assignmentOfferingValue, lms, userValue);
            editingContext.saveChanges();
        }
        else
        {
            if (!lisResultSourcedIdValue.equals(result.lisResultSourcedId()))
            {
                log.error("mismatching lis_result_sourcedid for user "
                    + userValue + " on assignment offering "
                    + assignmentOfferingValue + ": was "
                    + result.lisResultSourcedId() + ", changing to "
                    + lisResultSourcedIdValue);
                result.setLisResultSourcedId(lisResultSourcedIdValue);
                editingContext.saveChanges();
            }
            if (!lms.id().equals(result.lmsInstance().id()))
            {
                log.error("mismatching lmsInstanceId for user " + userValue
                    + " on assignment offering " + assignmentOfferingValue
                    + ": was " + result.lmsInstance() + ", changing to "
                    + lms);
                result.setLmsInstanceRelationship(lms);
                editingContext.saveChanges();
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    public static void sendScoreToLTIConsumer(Submission submission)
    {
        if (submission.assignmentOffering() == null) return;
        if (submission.isSubmissionForGrading())
        {
            LISResultId id = uniqueObjectMatchingQualifier(
                submission.editingContext(),
                user.is(submission.user()).and(
                assignmentOffering.is(submission.assignmentOffering())));
            String url =
                submission.assignmentOffering().lisOutcomeServiceUrl();
            if (id != null && url != null && !url.isEmpty())
            {
                LMSInstance lms = id.lmsInstance();
                double score = submission.result().finalScore() /
                    submission.assignmentOffering().assignment()
                    .submissionProfile().availablePoints();
//                log.debug("final score = " + submission.result().finalScore()
//                    + ", available = "
//                    + submission.assignmentOffering().assignment()
//                    .submissionProfile().availablePoints()
//                    + ", lti score = " + score);
                String msg = REPLACE_RESULT_TEMPLATE
                    .replaceAll("%MSGID%",
                    Long.toString(System.currentTimeMillis()))
                    .replaceAll("%ID%", id.lisResultSourcedId())
                    .replaceAll("%SCORE%",
                    Double.toString(score));
//                log.debug("outgoing message = " + msg);
                try
                {
                    byte[] bytes =
                        Charset.forName("UTF-16").encode(msg).array();
                    InputStream is = new ByteArrayInputStream(bytes);

                    OAuthConsumer consumer = new OAuthConsumer(null,
                        lms.consumerKey(), lms.consumerSecret(), null);
                    OAuthAccessor accessor = new OAuthAccessor(consumer);
                    OAuthMessage request = accessor.newRequestMessage(
                        "POST", url, null, is);
                    request.getHeaders().add(new OAuth.Parameter(
                        "Content-Type", "application/xml; charset=\"utf-8\""));
                    request.getHeaders().add(new OAuth.Parameter(
                        "Content-Length", Integer.toString(bytes.length)));
                    OAuthClient client = new OAuthClient(new HttpClient4());
                    OAuthMessage response = client.invoke(
                        request, ParameterStyle.AUTHORIZATION_HEADER);
//                    log.debug("response from posting grade to "
//                        + "lis_result_sourcedid " + id.lisResultSourcedId()
//                        + ":\n" + response.readBodyAsString());
                    boolean succeeded = false;
                    try
                    {
                        succeeded =
                            response.readBodyAsString().contains("success");
                    }
                    catch (Exception e)
                    {
                        log.error("exception trying to decode body of LTI "
                            + "response from consumer", e);
                    }
                    if (!succeeded)
                    {
                        log.error("failure sending grade for "
                            + "lis_result_sourcedid "
                            + id.lisResultSourcedId()
                            + "\nresult replace request =\n"
                            + msg + "\nresponse =\n"
                            + response.readBodyAsString());
                    }
                }
                catch (Exception e)
                {
                    log.error("exception sending grade for "
                        + "lis_result_sourcedid "
                        + id.lisResultSourcedId(), e);
                }
            }
        }
    }


    //~ Instance/static variables .............................................

    private static final String REPLACE_RESULT_TEMPLATE =
        "<?xml version = \"1.0\" encoding = \"UTF-8\"?>\n"
        + "<imsx_POXEnvelopeRequest xmlns=\"http://www.imsglobal.org/"
        +"services/ltiv1p1/xsd/imsoms_v1p0\">\n"
        + "  <imsx_POXHeader>\n"
        + "    <imsx_POXRequestHeaderInfo>\n"
        + "      <imsx_version>V1.0</imsx_version>\n"
        + "      <imsx_messageIdentifier>%MSGID%</imsx_messageIdentifier>\n"
        + "    </imsx_POXRequestHeaderInfo>\n"
        + "  </imsx_POXHeader>\n"
        + "  <imsx_POXBody>\n"
        + "    <replaceResultRequest>\n"
        + "      <resultRecord>\n"
        + "        <sourcedGUID>\n"
        + "          <sourcedId>%ID%</sourcedId>\n"
        + "        </sourcedGUID>\n"
        + "        <result>\n"
        + "          <resultScore>\n"
        + "            <language>en</language>\n"
        + "            <textString>%SCORE%</textString>\n"
        + "          </resultScore>\n"
        + "        </result>\n"
        + "      </resultRecord>\n"
        + "    </replaceResultRequest>\n"
        + "  </imsx_POXBody>\n"
        + "</imsx_POXEnvelopeRequest>\n";
}
