/*==========================================================================*\
 |  PassPortRequest.java
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2026 Virginia Tech
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

import com.webobjects.appserver.WORequest;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;
import er.extensions.eof.ERXQ;
import er.extensions.qualifiers.ERXKeyValueQualifier;
import er.extensions.qualifiers.ERXOrQualifier;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.User;
import org.webcat.core.lti.LMSIdentity;
import org.webcat.grader.Assignment;
import org.webcat.grader.AssignmentOffering;
import org.webcat.grader.PassportClient;

// -------------------------------------------------------------------------
/**
 * Wraps an incoming PassPort Protocol v1 WORequest to provide authentication,
 * HMAC-SHA256 signature verification, JSON payload parsing, and entity resolution
 * for AssignmentOffering and User.
 *
 * @author PassPort API Generator
 */
public class PassPortRequest
{
    //~ Constants (Header Names) ..............................................
    public static final String HEADER_CLIENT_ID = "X-PassPort-Client-ID";
    public static final String HEADER_SIGNATURE = "X-PassPort-Signature";
    public static final String HEADER_TIMESTAMP = "X-PassPort-Timestamp";

    // Header lowercase aliases
    public static final String HEADER_CLIENT_ID_LOWER = "x-passport-client-id";
    public static final String HEADER_SIGNATURE_LOWER = "x-passport-signature";
    public static final String HEADER_TIMESTAMP_LOWER = "x-passport-timestamp";


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public PassPortRequest(WORequest request, EOEditingContext ec)
    {
        this.request = request;
        this.ec = ec;
        initContent();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    private void initContent()
    {
        String raw = request.contentString();
        if (raw == null && request.content() != null)
        {
            raw = new String(request.content().bytes(), StandardCharsets.UTF_8);
        }
        this.rawBody = (raw != null) ? raw : "";

        if (!rawBody.trim().isEmpty())
        {
            try
            {
                this.jsonBody = new JSONObject(rawBody);
            }
            catch (JSONException e)
            {
                log.warn("Failed to parse PassPort JSON payload: " + e.getMessage());
                this.jsonBody = new JSONObject();
            }
        }
        else
        {
            this.jsonBody = new JSONObject();
        }
    }


    // ----------------------------------------------------------
    public WORequest request()
    {
        return request;
    }


    // ----------------------------------------------------------
    public String rawBody()
    {
        return rawBody;
    }


    // ----------------------------------------------------------
    public JSONObject jsonBody()
    {
        return jsonBody;
    }


    // ----------------------------------------------------------
    public String clientId()
    {
        String id = request.headerForKey(HEADER_CLIENT_ID);
        if (id == null)
        {
            id = request.headerForKey(HEADER_CLIENT_ID_LOWER);
        }
        return (id != null) ? id.trim() : null;
    }


    // ----------------------------------------------------------
    public String signature()
    {
        String sig = request.headerForKey(HEADER_SIGNATURE);
        if (sig == null)
        {
            sig = request.headerForKey(HEADER_SIGNATURE_LOWER);
        }
        return (sig != null) ? sig.trim() : null;
    }


    // ----------------------------------------------------------
    public String timestampHeader()
    {
        String ts = request.headerForKey(HEADER_TIMESTAMP);
        if (ts == null)
        {
            ts = request.headerForKey(HEADER_TIMESTAMP_LOWER);
        }
        return (ts != null) ? ts.trim() : null;
    }


    // ----------------------------------------------------------
    /**
     * Look up the PassportClient record matching the clientId header.
     */
    public PassportClient client()
    {
        if (client == null && clientId() != null)
        {
            client = PassportClient.clientForId(ec, clientId());
        }
        return client;
    }


    // ----------------------------------------------------------
    /**
     * Validates headers, timestamp freshness, client existence, client domain
     * screening, and HMAC-SHA256 signature against the raw request body.
     *
     * @return true if authentic and authorized, false otherwise
     */
    public boolean isValid()
    {
        if (clientId() == null || clientId().isEmpty())
        {
            validationError = "Missing required header: X-PassPort-Client-ID";
            return false;
        }
        if (signature() == null || signature().isEmpty())
        {
            validationError = "Missing required header: X-PassPort-Signature";
            return false;
        }
        if (timestampHeader() == null || timestampHeader().isEmpty())
        {
            validationError = "Missing required header: X-PassPort-Timestamp";
            return false;
        }

        // 1. Verify timestamp drift (reject if > 5 minutes old)
        if (!PassPortSecurity.isTimestampFresh(
            timestampHeader(), PassPortSecurity.DEFAULT_MAX_DRIFT_SECONDS))
        {
            validationError = "Request timestamp expired or clock skew exceeds allowed limit (5 minutes)";
            return false;
        }

        // 2. Resolve client
        PassportClient c = client();
        if (c == null)
        {
            validationError = "Unrecognized client ID: " + clientId();
            return false;
        }

        // 3. Domain / Host screening if configured on client
        String originHost = clientHostFromRequest();
        if (!c.allowsOrigin(originHost))
        {
            validationError = "Request origin host/IP '" + originHost
                + "' is not authorized for client ID: " + clientId();
            return false;
        }

        // 4. Verify HMAC-SHA256 signature
        String secret = c.clientSecret();
        if (secret == null || secret.isEmpty())
        {
            validationError = "Client has no shared secret configured";
            return false;
        }

        boolean sigOk = PassPortSecurity.verifySignature(secret, rawBody, signature());
        if (!sigOk)
        {
            validationError = "HMAC-SHA256 signature mismatch";
            return false;
        }

        return true;
    }


    // ----------------------------------------------------------
    public String validationError()
    {
        return validationError;
    }


    // ----------------------------------------------------------
    private String clientHostFromRequest()
    {
        String xForwardedFor = request.headerForKey("x-forwarded-for");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty())
        {
            int comma = xForwardedFor.indexOf(',');
            return (comma > 0)
                ? xForwardedFor.substring(0, comma).trim()
                : xForwardedFor.trim();
        }
        String xRealIp = request.headerForKey("x-real-ip");
        if (xRealIp != null && !xRealIp.trim().isEmpty())
        {
            return xRealIp.trim();
        }
        String host = request.headerForKey("remote_host");
        if (host == null)
        {
            host = request.headerForKey("remote_addr");
        }
        return host;
    }


    // ----------------------------------------------------------
    // Payload JSON accessors
    // ----------------------------------------------------------

    public String requestId()
    {
        return jsonBody.optString("request_id", null);
    }

    public JSONObject contextJson()
    {
        return jsonBody.optJSONObject("context");
    }

    public JSONObject userJson()
    {
        return jsonBody.optJSONObject("user");
    }

    public JSONObject resourceJson()
    {
        return jsonBody.optJSONObject("resource");
    }

    public JSONObject extensionJson()
    {
        return jsonBody.optJSONObject("extension");
    }


    // ----------------------------------------------------------
    // Entity Resolution
    // ----------------------------------------------------------

    /**
     * Resolves the target AssignmentOffering using resource identifiers
     * (lti_resource_link_id, canvas_assignment_id, broker_assignment_id, title)
     * and optional course context.
     *
     * @return resolved AssignmentOffering or null
     */
    public AssignmentOffering assignmentOffering()
    {
        if (assignmentOffering != null)
        {
            return assignmentOffering;
        }

        JSONObject res = resourceJson();
        JSONObject ctx = contextJson();
        if (res == null)
        {
            return null;
        }

        String linkId = res.optString("lti_resource_link_id", null);
        String canvasId = res.optString("canvas_assignment_id", null);
        String brokerId = res.optString("broker_assignment_id", null);

        // 1. Try lti_resource_link_id
        if (linkId != null && !linkId.isEmpty())
        {
            assignmentOffering = findOfferingByLmsId(linkId, ctx);
        }

        // 2. Try canvas_assignment_id
        if (assignmentOffering == null && canvasId != null && !canvasId.isEmpty())
        {
            assignmentOffering = findOfferingByLmsId(canvasId, ctx);
        }

        // 3. Try broker_assignment_id
        if (assignmentOffering == null && brokerId != null && !brokerId.isEmpty())
        {
            assignmentOffering = findOfferingByLmsId(brokerId, ctx);
        }

        // 4. Try matching title within course offering
        if (assignmentOffering == null)
        {
            String title = res.optString("title", null);
            if (title != null && !title.isEmpty())
            {
                assignmentOffering = findOfferingByTitle(title, ctx);
            }
        }

        return assignmentOffering;
    }


    // ----------------------------------------------------------
    private AssignmentOffering findOfferingByLmsId(String lmsId, JSONObject ctx)
    {
        NSArray<AssignmentOffering> matches =
            AssignmentOffering.objectsMatchingQualifier(
                ec, AssignmentOffering.lmsAssignmentId.is(lmsId),
                AssignmentOffering.dueDate.descs());

        if (matches != null && matches.count() > 0)
        {
            User targetUser = user();
            String courseId = (ctx != null) ? ctx.optString("lti_context_id", null) : null;
            if (courseId == null && ctx != null)
            {
                courseId = ctx.optString("canvas_course_id", null);
            }
            if (courseId == null && ctx != null)
            {
                courseId = ctx.optString("crn", null);
            }

            // 1. Try matching both course context and user association
            for (AssignmentOffering ao : matches)
            {
                CourseOffering co = ao.courseOffering();
                boolean courseMatches = (courseId == null)
                    || (co != null && (courseId.equals(co.lmsContextId())
                        || courseId.equals(co.crn())));
                if (courseMatches && isUserAssociated(ao, targetUser))
                {
                    return ao;
                }
            }

            // 2. Try matching user association across matches
            if (targetUser != null)
            {
                for (AssignmentOffering ao : matches)
                {
                    if (isUserAssociated(ao, targetUser))
                    {
                        return ao;
                    }
                }
            }

            // 3. Filter matches by course context
            if (courseId != null)
            {
                for (AssignmentOffering ao : matches)
                {
                    CourseOffering co = ao.courseOffering();
                    if (co != null && (courseId.equals(co.lmsContextId())
                        || courseId.equals(co.crn())))
                    {
                        return ao;
                    }
                }
            }

            return matches.objectAtIndex(0);
        }
        return null;
    }


    // ----------------------------------------------------------
    /**
     * Finds the AssignmentOffering matching the given title/name for the specified
     * course or course offering that the user is associated with.
     * Modeled after Grader.handleSubmission, without dueDate or maxClosesOn
     * filtering so extensions can be applied.
     *
     * @param title the assignment title or short description
     * @param ctx the context JSON containing course identifiers (lti_context_id,
     *            canvas_course_id, crn, course number)
     * @return matching AssignmentOffering, or null
     */
    private AssignmentOffering findOfferingByTitle(String title, JSONObject ctx)
    {
        if (title == null || title.trim().isEmpty())
        {
            return null;
        }

        String trimmedTitle = title.trim();

        // 1. Build course/course offering qualifier if context contains course identifiers
        EOQualifier courseQual = null;
        if (ctx != null)
        {
            String courseId = ctx.optString("lti_context_id", null);
            if (courseId == null || courseId.isEmpty())
            {
                courseId = ctx.optString("canvas_course_id", null);
            }

            String crn = ctx.optString("crn", null);

            Integer courseNo = null;
            if (ctx.has("course"))
            {
                try
                {
                    courseNo = Integer.valueOf(ctx.getInt("course"));
                }
                catch (Exception e)
                {
                    // Ignore if not an integer
                }
            }

            if (courseId != null && !courseId.isEmpty())
            {
                courseQual = AssignmentOffering.courseOffering
                    .dot(CourseOffering.lmsContextId).eq(courseId)
                    .or(AssignmentOffering.courseOffering
                    .dot(CourseOffering.crn).eq(courseId));
            }
            else if (crn != null && !crn.isEmpty())
            {
                courseQual = AssignmentOffering.courseOffering
                    .dot(CourseOffering.crn).eq(crn);
            }
            else if (courseNo != null)
            {
                courseQual = AssignmentOffering.courseOffering
                    .dot(CourseOffering.course).dot(Course.number).eq(courseNo);
            }
        }

        // 2. Query AssignmentOffering matching assignment name or short description
        ERXKeyValueQualifier matchName = AssignmentOffering.assignment
            .dot(Assignment.name).eq(trimmedTitle);
        ERXKeyValueQualifier matchDesc = AssignmentOffering.assignment
            .dot(Assignment.shortDescription).eq(trimmedTitle);
        ERXOrQualifier titleQual = matchName.or(matchDesc);

        EOQualifier qualifier = (courseQual != null)
            ? titleQual.and(courseQual)
            : titleQual;

        NSArray<EOSortOrdering> orderings = AssignmentOffering.dueDate.descs();
        NSArray<AssignmentOffering> assignments = null;
        try
        {
            assignments = AssignmentOffering.objectsMatchingQualifier(
                ec, qualifier, orderings);
        }
        catch (Exception e)
        {
            log.error("Error finding AssignmentOfferings by title: " + trimmedTitle, e);
        }

        // Fallback: if exact match found nothing, try case-insensitive match
        if (assignments == null || assignments.count() == 0)
        {
            try
            {
                EOQualifier ciTitleQual = EOQualifier.qualifierWithQualifierFormat(
                    "assignment.name caseInsensitiveLike %@ or "
                    + "assignment.shortDescription caseInsensitiveLike %@",
                    new NSArray<Object>(new Object[] { trimmedTitle, trimmedTitle }));
                EOQualifier fallbackQual = (courseQual != null)
                    ? ERXQ.and(ciTitleQual, courseQual)
                    : ciTitleQual;

                assignments = AssignmentOffering.objectsMatchingQualifier(
                    ec, fallbackQual, orderings);
            }
            catch (Exception e)
            {
                log.error("Error in fallback title matching: " + trimmedTitle, e);
            }
        }

        if (assignments == null || assignments.count() == 0)
        {
            return null;
        }

        // 3. Find offering for the course offering the target user is associated with
        User targetUser = user();
        if (targetUser != null)
        {
            for (AssignmentOffering ao : assignments)
            {
                if (isUserAssociated(ao, targetUser))
                {
                    return ao;
                }
            }
        }

        // If target user is unspecified or user association was not found but exactly
        // one offering was found, return it
        return (targetUser == null) ? assignments.objectAtIndex(0) : null;
    }


    // ----------------------------------------------------------
    /**
     * Checks if a user is associated with an AssignmentOffering's CourseOffering
     * (either as an administrator, course staff member, or enrolled student).
     *
     * @param ao the AssignmentOffering
     * @param targetUser the user to test
     * @return true if associated, false otherwise
     */
    public boolean isUserAssociated(AssignmentOffering ao, User targetUser)
    {
        if (ao == null || ao.courseOffering() == null)
        {
            return false;
        }
        if (targetUser == null)
        {
            return true;
        }
        CourseOffering co = ao.courseOffering();
        if (targetUser.hasAdminPrivileges() || co.isStaff(targetUser))
        {
            return true;
        }
        NSArray<User> students = co.students();
        return (students != null && students.contains(targetUser));
    }


    // ----------------------------------------------------------
    /**
     * Resolves the User (student) using user identifiers:
     * 1. lti_user_id via LMSIdentity
     * 2. email
     * 3. canvas_user_id / userName
     *
     * @return resolved User or null
     */
    public User user()
    {
        if (user != null)
        {
            return user;
        }

        JSONObject uJson = userJson();
        if (uJson == null)
        {
            return null;
        }

        // 1. Try lti_user_id in LMSIdentity
        String ltiUserId = uJson.optString("lti_user_id", null);
        if (ltiUserId != null && !ltiUserId.isEmpty())
        {
            LMSIdentity identity = LMSIdentity.firstObjectMatchingQualifier(
                ec, LMSIdentity.lmsUserId.is(ltiUserId), null);
            if (identity != null && identity.user() != null)
            {
                user = identity.user();
                return user;
            }
        }

        // 2. Try email
        String email = uJson.optString("email", null);
        if (email != null && !email.isEmpty())
        {
            NSArray<User> users = User.objectsMatchingQualifier(
                ec, User.email.is(email));
            if (users != null && users.count() == 1)
            {
                user = users.objectAtIndex(0);
                return user;
            }
        }

        // 3. Try userName from email prefix or canvas_user_id
        String userName = uJson.optString("userName", null);
        if (userName == null && email != null)
        {
            int at = email.indexOf('@');
            if (at > 0)
            {
                userName = email.substring(0, at);
            }
        }
        if (userName != null && !userName.isEmpty())
        {
            NSArray<User> users = User.objectsMatchingQualifier(
                ec, User.userName.is(userName));
            if (users != null && users.count() == 1)
            {
                user = users.objectAtIndex(0);
                return user;
            }
        }

        return null;
    }


    // ----------------------------------------------------------
    // Dates & Extension Fields
    // ----------------------------------------------------------

    public String passType()
    {
        JSONObject ext = extensionJson();
        return (ext != null) ? ext.optString("pass_type", null) : null;
    }

    public NSTimestamp originalDueDate()
    {
        JSONObject ext = extensionJson();
        return (ext != null)
            ? parseIsoDateTime(ext.optString("original_due_date", null))
            : null;
    }

    public NSTimestamp newDueDate()
    {
        JSONObject ext = extensionJson();
        return (ext != null)
            ? parseIsoDateTime(ext.optString("new_due_date", null))
            : null;
    }

    public NSTimestamp appliedAt()
    {
        JSONObject ext = extensionJson();
        return (ext != null)
            ? parseIsoDateTime(ext.optString("applied_at", null))
            : null;
    }


    // ----------------------------------------------------------
    /**
     * Parses an ISO-8601 date string to NSTimestamp.
     */
    public static NSTimestamp parseIsoDateTime(String isoString)
    {
        if (isoString == null || isoString.trim().isEmpty())
        {
            return null;
        }

        try
        {
            Instant instant = Instant.parse(isoString.trim());
            return new NSTimestamp(instant.toEpochMilli());
        }
        catch (Exception e)
        {
            try
            {
                TemporalAccessor ta =
                    DateTimeFormatter.ISO_DATE_TIME.parse(isoString.trim());
                Instant instant = Instant.from(ta);
                return new NSTimestamp(instant.toEpochMilli());
            }
            catch (Exception ex)
            {
                log.warn("Unable to parse date string: '" + isoString + "': " + ex.getMessage());
                return null;
            }
        }
    }


    //~ Instance fields .......................................................
    private WORequest request;
    private EOEditingContext ec;
    private String rawBody;
    private JSONObject jsonBody;
    private PassportClient client;
    private AssignmentOffering assignmentOffering;
    private User user;
    private String validationError;

    private static final Logger log = Logger.getLogger(PassPortRequest.class);
}
