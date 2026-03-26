/*==========================================================================*\
 |  Copyright (C) 2018-2021 Virginia Tech
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
import org.webcat.woextensions.ECAction;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestampFormatter;

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
    public static LISResultId forAssignmentAndUser(
        final EOEditingContext context,
        final AssignmentOffering anAssignmentOffering,
        final User aUser)
    {
        LISResultId result = null;
        final NSArray<LISResultId> ids = objectsMatchingQualifier(
            context,
            user.is(aUser).and(
            assignmentOffering.is(anAssignmentOffering)));
        if (ids != null && ids.size() > 0)
        {
            result = ids.get(0);
            if (ids.size() > 1)
            {
                // If multiple bars, clean them up!
                new ECAction() { public void action() {
                    for (int i = 1; i < ids.size(); i++)
                    {
                        ids.get(i).localInstance(ec).delete();
                    }
                    ec.saveChanges();
                }}.run();
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    public static LISResultId ensureExists(
        EOEditingContext editingContext,
        User userValue,
        AssignmentOffering assignmentOfferingValue,
        LMSInstance lms,
        String lisResultSourcedIdValue)
    {
        LISResultId result = forAssignmentAndUser(
            editingContext, assignmentOfferingValue, userValue);
        if (result == null)
        {
            result = create(editingContext, lisResultSourcedIdValue,
                assignmentOfferingValue, lms, userValue);
            // FIXME: find exception thrown if index is used and duplicate
            // is accidentally attempted, and recover from that exception
            // here
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
            // Was "unique" instead of "first", but concurrent insertions
            // need to be fixed before we can use unique, and this variant
            // will work in the meantime ... and will continue working once
            // the fix is in place
            LISResultId id = firstObjectMatchingQualifier(
                submission.editingContext(),
                user.is(submission.user()).and(
                assignmentOffering.is(submission.assignmentOffering())), null);
            String url =
                submission.assignmentOffering().lisOutcomeServiceUrl();
            log.debug("sendScoreToLTIConsumer(" + submission + "): id = "
                + id + ", url = " + url);
            if (id != null && url != null && !url.isEmpty())
            {
                LMSInstance lms = id.lmsInstance();
                double points = submission.result().finalScore();
                double score = points /
                    submission.assignmentOffering().assignment()
                    .submissionProfile().availablePoints();
                // Cap LTI score at 1.0, as required by LTI spec
                if (score > 1.0)
                {
                    score = 1.0;
                }
                else if (score < 0.0)
                {
                    score = 0.0;
                }
                // An ISO8601-formatted timestamp, like this:
                // 2017-04-16T18:54:36.736+00:00
                String timestamp = formatter.format(submission.submitTime());

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
                    Double.toString(score))
                    // Should really make this section of the message
                    // conditional on whether the LTI launch included param:
                    // ext_outcome_result_total_score_accepted=true
                    .replaceAll("%POINTS%",
                    Double.toString(points))
                    // Should really make this section of the message
                    // conditional on whether the LTI launch included param:
                    // ext_outcome_submission_submitted_at_accepted=true
                    .replaceAll("%TIMESTAMP%", timestamp)
                    // Should really make this section of the message
                    // conditional on whether the LTI launch included param:
                    // ext_outcome_data_values_accepted=...url...
                    .replaceAll("%URL%", submission.permalink());
//                log.debug("outgoing message = " + msg);
                try
                {
                    byte[] bytes =
                        Charset.forName("UTF-8").encode(msg).array();
                        // Charset.forName("UTF-16").encode(msg).array();
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

    // Produces an ISO8601-formatted timestamp, like this:
    // 2017-04-16T18:54:36.736Z
    private static NSTimestampFormatter formatter =
        new NSTimestampFormatter("%Y-%m-%dT%H:%M:%S.%F3%Z");

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
        // The following section should only be included if the LTI launch
        // request included: ext_outcome_submission_submitted_at_accepted=true
        + "      <submissionDetails>\n"
        + "        <submittedAt>\n"
        + "          %TIMESTAMP%\n"
        + "        </submittedAt>\n"
        + "      </submissionDetails>\n"
        + "      <resultRecord>\n"
        + "        <sourcedGUID>\n"
        + "          <sourcedId>%ID%</sourcedId>\n"
        + "        </sourcedGUID>\n"
        + "        <result>\n"
        + "          <resultScore>\n"
        + "            <language>en</language>\n"
        + "            <textString>%SCORE%</textString>\n"
        + "          </resultScore>\n"
        // The following section should only be included if the LTI launch
        // request included: ext_outcome_result_total_score_accepted=true
        + "          <resultTotalScore>\n"
        + "            <language>en</language>\n"
        + "            <textString>%POINTS%</textString>\n"
        + "          </resultTotalScore>\n"
        // The following section should only be included if the LTI launch
        // request included: ext_outcome_data_values_accepted=...url...
        + "          <resultData>\n"
        + "            <url>%URL%</url>\n"
        + "          </resultData>\n"
        + "        </result>\n"
        + "      </resultRecord>\n"
        + "    </replaceResultRequest>\n"
        + "  </imsx_POXBody>\n"
        + "</imsx_POXEnvelopeRequest>\n";
}
