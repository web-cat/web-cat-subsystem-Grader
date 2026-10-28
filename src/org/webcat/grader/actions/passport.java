/*==========================================================================*\
 |  passport.java
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

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webcat.core.Application;
import org.webcat.core.User;
import org.webcat.core.actions.WCDirectAction;
import org.webcat.grader.AssignmentOffering;
import org.webcat.grader.PassportClient;
import org.webcat.grader.StudentExtension;
import org.webcat.grader.Submission;
import org.webcat.grader.SubmissionResult;
import org.webcat.woextensions.ECAction;
import org.webcat.woextensions.ECActionWithResult;

// -------------------------------------------------------------------------
/**
 * Sessionless REST Direct Action controller implementing the PassPort Protocol v1.
 *
 * Handles:
 * 1. POST /passport/v1/extension - Apply deadline extension (200 OK / 401 / 404 / 409)
 * 2. DELETE /passport/v1/extension - Revert/rollback extension using request_id (200 OK / 401 / 404)
 * 3. POST /passport/v1/register - Phase 1 Dynamic Registration handshake (202 Accepted)
 *    and triggers asynchronous Phase 2 credential delivery to callback_url.
 * 4. POST|GET /passport/v1/status or /submissionStatus - Query student submission status (200 OK / 401 / 404)
 *
 * @author PassPort API Generator
 */
public class passport
    extends WCDirectAction
{
    //~ Constants .............................................................
    // HTTP Status Codes
    public static final int HTTP_STATUS_OK                 = WOResponse.HTTP_STATUS_OK;
    public static final int HTTP_STATUS_ACCEPTED           = 202;
    public static final int HTTP_STATUS_NO_CONTENT         = WOResponse.HTTP_STATUS_NO_CONTENT;
    public static final int HTTP_STATUS_MOVED_PERMANENTLY  = WOResponse.HTTP_STATUS_MOVED_PERMANENTLY;
    public static final int HTTP_STATUS_FOUND              = WOResponse.HTTP_STATUS_FOUND;
    public static final int HTTP_STATUS_BAD_REQUEST        = 400;
    public static final int HTTP_STATUS_UNAUTHORIZED       = 401;
    public static final int HTTP_STATUS_FORBIDDEN          = WOResponse.HTTP_STATUS_FORBIDDEN;
    public static final int HTTP_STATUS_NOT_FOUND          = WOResponse.HTTP_STATUS_NOT_FOUND;
    public static final int HTTP_STATUS_METHOD_NOT_ALLOWED = 405;
    public static final int HTTP_STATUS_CONFLICT           = 409;
    public static final int HTTP_STATUS_INTERNAL_ERROR     = WOResponse.HTTP_STATUS_INTERNAL_ERROR;

    public static final String DEFAULT_DOMAIN_WHITELIST =
        "*.cs.vt.edu,localhost,127.0.0.1";
    public static final String PROPERTY_DOMAIN_WHITELIST =
        "org.webcat.grader.passport.domainWhitelist";
    public static final long REGISTRY_TTL_MS = 172800000L; // 48 hours


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public passport(WORequest aRequest)
    {
        super(aRequest);
    }


    //~ Public Action Methods .................................................

    // ----------------------------------------------------------
    /**
     * Extension endpoint:
     * - POST: Records or updates student extension.
     * - DELETE: Reverts extension associated with request_id.
     */
    public WOActionResults extensionAction()
    {
        forgetSession();
        String method = request().method();
        if ("POST".equalsIgnoreCase(method))
        {
            return handleExtensionPost();
        }
        else if ("DELETE".equalsIgnoreCase(method))
        {
            return handleExtensionDelete();
        }
        else
        {
            return jsonError(HTTP_STATUS_METHOD_NOT_ALLOWED,
                "Method " + method + " not allowed on /extension");
        }
    }


    // ----------------------------------------------------------
    /**
     * Submission status query endpoint:
     * - POST / GET: Returns student's submission status for grading and most recent.
     */
    public WOActionResults submissionStatusAction()
    {
        forgetSession();
        return handleSubmissionStatus();
    }


    // ----------------------------------------------------------
    /**
     * Alias for submissionStatusAction.
     */
    public WOActionResults statusAction()
    {
        return submissionStatusAction();
    }


    // ----------------------------------------------------------
    /**
     * Phase 1 Dynamic Registration handshake (Broker -> Tool).
     * Validates domain whitelist, HTTPS enforcement, and matching callback domain.
     * Returns 202 Accepted immediately, then pushes credentials asynchronously.
     */
    public WOActionResults registerAction()
    {
        forgetSession();
        if (!"POST".equalsIgnoreCase(request().method()))
        {
            return jsonError(HTTP_STATUS_METHOD_NOT_ALLOWED,
                "Method " + request().method() + " not allowed on /register");
        }

        String raw = request().contentString();
        if (raw == null && request().content() != null)
        {
            raw = new String(request().content().bytes(), StandardCharsets.UTF_8);
        }
        if (raw == null || raw.trim().isEmpty())
        {
            return jsonError(HTTP_STATUS_BAD_REQUEST,
                "Missing JSON request body");
        }

        JSONObject body = null;
        try
        {
            body = new JSONObject(raw);
        }
        catch (JSONException e)
        {
            return jsonError(HTTP_STATUS_BAD_REQUEST,
                "Invalid JSON payload: " + e.getMessage());
        }

        String brokerBaseUrl = body.optString("broker_base_url", null);
        String callbackUrl = body.optString("callback_url", null);
        String clientName = body.optString("name", "PassPort Broker");
        String version = body.optString("passport_version", null);

        if (brokerBaseUrl == null || brokerBaseUrl.trim().isEmpty())
        {
            return jsonError(HTTP_STATUS_BAD_REQUEST,
                "Missing required field: broker_base_url");
        }
        if (callbackUrl == null || callbackUrl.trim().isEmpty())
        {
            return jsonError(HTTP_STATUS_BAD_REQUEST,
                "Missing required field: callback_url");
        }

        URI brokerUri;
        URI callbackUri;
        try
        {
            brokerUri = new URI(brokerBaseUrl.trim());
            callbackUri = new URI(callbackUrl.trim());
        }
        catch (Exception e)
        {
            return jsonError(HTTP_STATUS_BAD_REQUEST,
                "Malformed URL in registration request: " + e.getMessage());
        }

        // 1. Whitelist Check
        String whitelist = configuredDomainWhitelist();
        String brokerHost = brokerUri.getHost();
        if (!PassPortSecurity.matchesDomainPattern(whitelist, brokerHost))
        {
            log.warn("PassPort registration rejected: host '" + brokerHost
                + "' not in domain whitelist: " + whitelist);
            return jsonError(HTTP_STATUS_FORBIDDEN,
                "Broker domain is not authorized by domain whitelist");
        }

        // 2. HTTPS Enforcement (allow http only for localhost testing)
        boolean isLocal = "localhost".equalsIgnoreCase(brokerHost)
            || "127.0.0.1".equals(brokerHost);
        if (!isLocal)
        {
            if (!"https".equalsIgnoreCase(brokerUri.getScheme())
                || !"https".equalsIgnoreCase(callbackUri.getScheme()))
            {
                return jsonError(HTTP_STATUS_BAD_REQUEST,
                    "Both broker_base_url and callback_url MUST use HTTPS");
            }
        }

        // 3. Domain Matching: callback_url MUST match protocol, host, and port of broker_base_url
        if (!brokerUri.getScheme().equalsIgnoreCase(callbackUri.getScheme())
            || !brokerUri.getHost().equalsIgnoreCase(callbackUri.getHost())
            || brokerUri.getPort() != callbackUri.getPort())
        {
            return jsonError(HTTP_STATUS_BAD_REQUEST,
                "callback_url MUST have the same protocol, host, and port as broker_base_url");
        }

        // 4. Immediate Response: 202 Accepted (credentials NOT included)
        String extensionHandlerUrl = buildExtensionHandlerUrl();
        deliverCredentialsAsync(callbackUrl.trim(), brokerBaseUrl.trim(),
            clientName, extensionHandlerUrl);

        JSONObject accepted = new JSONObject();
        try
        {
            accepted.put("status", "accepted");
            accepted.put("message", "Registration request accepted for processing");
        }
        catch (JSONException e) {}

        return jsonResponse(HTTP_STATUS_ACCEPTED, accepted);
    }


    // ----------------------------------------------------------
    public WOActionResults v1Action()
    {
        forgetSession();
        String path = request().requestHandlerPath();
        String uri = request().uri();
        if ((path != null && path.contains("register"))
            || (uri != null && uri.contains("register")))
        {
            return registerAction();
        }
        if ((path != null && (path.contains("status") || path.contains("submission")))
            || (uri != null && (uri.contains("status") || uri.contains("submission"))))
        {
            return submissionStatusAction();
        }
        return extensionAction();
    }


    // ----------------------------------------------------------
    public WOActionResults defaultAction()
    {
        forgetSession();
        String path = request().requestHandlerPath();
        String uri = request().uri();
        if ((path != null && path.contains("register"))
            || (uri != null && uri.contains("register")))
        {
            return registerAction();
        }
        if ((path != null && (path.contains("status") || path.contains("submission")))
            || (uri != null && (uri.contains("status") || uri.contains("submission"))))
        {
            return submissionStatusAction();
        }
        return extensionAction();
    }


    // ----------------------------------------------------------
    @Override
    public WOActionResults performActionNamed(String actionName)
    {
        if (actionName != null)
        {
            String lower = actionName.toLowerCase();
            if (lower.contains("status") || lower.contains("submission"))
            {
                return submissionStatusAction();
            }
            if (lower.contains("extension") || lower.endsWith("extension"))
            {
                return extensionAction();
            }
            if (lower.contains("register") || lower.endsWith("register"))
            {
                return registerAction();
            }
            if ("v1".equals(lower))
            {
                return v1Action();
            }
        }
        return super.performActionNamed(actionName);
    }


    //~ Private Extension Logic ...............................................

    // ----------------------------------------------------------
    private WOActionResults handleExtensionPost()
    {
        cleanStaleRegistryEntries();

        return new ECActionWithResult<WOActionResults>() {
            @Override
            public WOActionResults action()
            {
                PassPortRequest pReq = new PassPortRequest(request(), ec);

                // 1. Authenticate & Verify
                if (!pReq.isValid())
                {
                    log.warn("PassPort extension POST unauthorized: " + pReq.validationError());
                    return jsonError(HTTP_STATUS_UNAUTHORIZED,
                        pReq.validationError());
                }

                String reqId = pReq.requestId();
                if (reqId == null || reqId.trim().isEmpty())
                {
                    return jsonError(HTTP_STATUS_BAD_REQUEST,
                        "Missing required field: request_id");
                }

                // Check idempotency: if request_id already applied, return 200 OK
                ExtensionRecord cached = EXTENSION_REGISTRY.get(reqId);
                if (cached != null)
                {
                    log.info("PassPort extension POST duplicate/retry for request_id: " + reqId);
                    JSONObject resp = new JSONObject();
                    try
                    {
                        resp.put("status", "ok");
                        resp.put("request_id", reqId);
                        resp.put("message", "Extension already recorded (idempotent response)");
                        if (cached.appliedDueDate != null)
                        {
                            resp.put("new_due_date", PassPortRequest.formatIsoDateTime(cached.appliedDueDate));
                        }
                        if (cached.appliedClosesOn != null)
                        {
                            resp.put("new_accept_until", PassPortRequest.formatIsoDateTime(cached.appliedClosesOn));
                        }
                        if (cached.appliedOpensOn != null)
                        {
                            resp.put("new_available_from", PassPortRequest.formatIsoDateTime(cached.appliedOpensOn));
                        }
                    }
                    catch (JSONException e) {}
                    return jsonResponse(HTTP_STATUS_OK, resp);
                }

                // 2. Resolve entities
                AssignmentOffering ao = pReq.assignmentOffering();
                User student = pReq.user();
                if (ao == null || student == null)
                {
                    log.warn("PassPort extension 404: ao=" + ao + ", student=" + student);
                    return jsonError(HTTP_STATUS_NOT_FOUND,
                        "User or Resource not recognized");
                }

                NSTimestamp newDue = pReq.newDueDate();
                NSTimestamp newAcceptUntil = pReq.newAcceptUntil();
                NSTimestamp newAvailableFrom = pReq.newAvailableFrom();

                if (newDue == null && newAcceptUntil == null && newAvailableFrom == null)
                {
                    return jsonError(HTTP_STATUS_BAD_REQUEST,
                        "No extension dates provided (at least one of new_due_date, new_accept_until, or new_available_from must be specified)");
                }

                // 3. Conflict Check (409 Conflict):
                // If a later due date or accept until cutoff is already active for this student, reject
                NSTimestamp currentActiveDue = ao.dueDateFor(student);
                NSTimestamp currentActiveAcceptUntil = ao.lateDeadlineFor(student);

                boolean dueConflict = (newDue != null
                    && currentActiveDue != null
                    && currentActiveDue.after(newDue));
                boolean acceptUntilConflict = (newAcceptUntil != null
                    && currentActiveAcceptUntil != null
                    && currentActiveAcceptUntil.after(newAcceptUntil));

                if (dueConflict || acceptUntilConflict)
                {
                    log.info("PassPort extension conflict for student " + student.userName()
                        + ": dueConflict=" + dueConflict + " (active=" + currentActiveDue + ", req=" + newDue + ")"
                        + ", acceptUntilConflict=" + acceptUntilConflict + " (active=" + currentActiveAcceptUntil + ", req=" + newAcceptUntil + ")");
                    JSONObject conflict = new JSONObject();
                    try
                    {
                        conflict.put("status", "conflict");
                        conflict.put("error", "A later due date or accept until cutoff is already active for this student.");
                        if (dueConflict)
                        {
                            conflict.put("active_due_date", PassPortRequest.formatIsoDateTime(currentActiveDue));
                            conflict.put("requested_due_date", PassPortRequest.formatIsoDateTime(newDue));
                        }
                        if (acceptUntilConflict)
                        {
                            conflict.put("active_accept_until", PassPortRequest.formatIsoDateTime(currentActiveAcceptUntil));
                            conflict.put("requested_accept_until", PassPortRequest.formatIsoDateTime(newAcceptUntil));
                        }
                    }
                    catch (JSONException e) {}
                    return jsonResponse(HTTP_STATUS_CONFLICT, conflict);
                }

                // 4. Create or Update StudentExtension
                StudentExtension ext = ao.extensionForUser(student);
                boolean wasNewlyCreated = false;
                NSTimestamp prevOpens = null;
                NSTimestamp prevDue = null;
                NSTimestamp prevClose = null;

                if (ext == null)
                {
                    ext = StudentExtension.create(ec, ao, student);
                    wasNewlyCreated = true;
                }
                else
                {
                    prevOpens = ext.opensOn();
                    prevDue = ext.dueDate();
                    prevClose = ext.closesOn();
                }

                if (newDue != null)
                {
                    ext.setDueDate(newDue);
                }
                if (newAcceptUntil != null)
                {
                    ext.setClosesOn(newAcceptUntil);
                }
                if (newAvailableFrom != null)
                {
                    ext.setOpensOn(newAvailableFrom);
                }
                ec.saveChanges();

                // 5. Store record in rollback/idempotency registry
                ExtensionRecord rec = new ExtensionRecord();
                rec.requestId = reqId;
                rec.studentExtensionId = (ext.id() != null) ? ext.id().intValue() : 0;
                rec.assignmentOfferingId = (ao.id() != null) ? ao.id().intValue() : 0;
                rec.userId = (student.id() != null) ? student.id().intValue() : 0;
                rec.wasNewlyCreated = wasNewlyCreated;
                rec.previousOpensOn = prevOpens;
                rec.previousDueDate = prevDue;
                rec.previousClosesOn = prevClose;
                rec.appliedOpensOn = newAvailableFrom;
                rec.appliedDueDate = newDue;
                rec.appliedClosesOn = newAcceptUntil;
                rec.recordedTime = System.currentTimeMillis();
                EXTENSION_REGISTRY.put(reqId, rec);

                log.info("PassPort extension applied: reqId=" + reqId
                    + ", student=" + student.userName() + ", ao=" + ao.id()
                    + ", newDue=" + newDue + ", newAcceptUntil=" + newAcceptUntil
                    + ", newAvailableFrom=" + newAvailableFrom);

                JSONObject success = new JSONObject();
                try
                {
                    success.put("status", "ok");
                    success.put("request_id", reqId);
                    success.put("message", "Extension successfully recorded");
                    if (newDue != null)
                    {
                        success.put("new_due_date", PassPortRequest.formatIsoDateTime(newDue));
                    }
                    if (newAcceptUntil != null)
                    {
                        success.put("new_accept_until", PassPortRequest.formatIsoDateTime(newAcceptUntil));
                    }
                    if (newAvailableFrom != null)
                    {
                        success.put("new_available_from", PassPortRequest.formatIsoDateTime(newAvailableFrom));
                    }
                }
                catch (JSONException e) {}
                return jsonResponse(HTTP_STATUS_OK, success);
            }
        }.call();
    }


    // ----------------------------------------------------------
    private WOActionResults handleExtensionDelete()
    {
        cleanStaleRegistryEntries();

        return new ECActionWithResult<WOActionResults>() {
            @Override
            public WOActionResults action()
            {
                PassPortRequest pReq = new PassPortRequest(request(), ec);

                // 1. Authenticate & Verify
                if (!pReq.isValid())
                {
                    log.warn("PassPort rollback DELETE unauthorized: " + pReq.validationError());
                    return jsonError(HTTP_STATUS_UNAUTHORIZED,
                        pReq.validationError());
                }

                String reqId = pReq.requestId();
                if (reqId == null || reqId.trim().isEmpty())
                {
                    // Check if passed via query parameter
                    reqId = request().stringFormValueForKey("request_id");
                }
                if (reqId == null || reqId.trim().isEmpty())
                {
                    reqId = request().headerForKey("X-PassPort-Request-ID");
                }
                if (reqId == null || reqId.trim().isEmpty())
                {
                    reqId = request().headerForKey("x-passport-request-id");
                }
                if (reqId == null || reqId.trim().isEmpty())
                {
                    return jsonError(HTTP_STATUS_BAD_REQUEST,
                        "Missing required field: request_id");
                }

                // 2. Rollback lookup
                ExtensionRecord rec = EXTENSION_REGISTRY.remove(reqId);
                if (rec != null)
                {
                    StudentExtension ext = StudentExtension.forId(ec, rec.studentExtensionId);
                    if (ext != null)
                    {
                        if (rec.wasNewlyCreated)
                        {
                            ec.deleteObject(ext);
                        }
                        else
                        {
                            ext.setOpensOn(rec.previousOpensOn);
                            ext.setDueDate(rec.previousDueDate);
                            ext.setClosesOn(rec.previousClosesOn);
                            if (ext.opensOn() == null && ext.dueDate() == null && ext.closesOn() == null)
                            {
                                ec.deleteObject(ext);
                            }
                        }
                        ec.saveChanges();
                        AssignmentOffering offering = AssignmentOffering.forId(ec, rec.assignmentOfferingId);
                        if (offering != null)
                        {
                            offering.updateTimes();
                            ec.saveChanges();
                        }
                        log.info("PassPort extension rollback completed from registry for reqId: " + reqId);

                        JSONObject resp = new JSONObject();
                        try
                        {
                            resp.put("status", "ok");
                            resp.put("request_id", reqId);
                            resp.put("message", "Extension successfully reverted");
                        }
                        catch (JSONException e) {}
                        return jsonResponse(HTTP_STATUS_OK, resp);
                    }
                }

                // Fallback: Resolve student and offering from payload if provided
                AssignmentOffering ao = pReq.assignmentOffering();
                User student = pReq.user();
                if (ao != null && student != null)
                {
                    StudentExtension ext = ao.extensionForUser(student);
                    if (ext != null)
                    {
                        NSTimestamp origDue = pReq.originalDueDate();
                        NSTimestamp origAcceptUntil = pReq.originalAcceptUntil();
                        NSTimestamp origAvailableFrom = pReq.originalAvailableFrom();

                        if (origDue == null && origAcceptUntil == null && origAvailableFrom == null)
                        {
                            ec.deleteObject(ext);
                        }
                        else
                        {
                            ext.setDueDate(origDue);
                            ext.setClosesOn(origAcceptUntil);
                            ext.setOpensOn(origAvailableFrom);
                            if (ext.opensOn() == null && ext.dueDate() == null && ext.closesOn() == null)
                            {
                                ec.deleteObject(ext);
                            }
                        }
                        ec.saveChanges();
                        ao.updateTimes();
                        ec.saveChanges();
                        log.info("PassPort extension rollback completed via payload match for reqId: " + reqId);

                        JSONObject resp = new JSONObject();
                        try
                        {
                            resp.put("status", "ok");
                            resp.put("request_id", reqId);
                            resp.put("message", "Extension successfully reverted");
                        }
                        catch (JSONException e) {}
                        return jsonResponse(HTTP_STATUS_OK, resp);
                    }
                }

                return jsonError(HTTP_STATUS_NOT_FOUND,
                    "No active extension found associated with request_id: " + reqId);
            }
        }.call();
    }


    // ----------------------------------------------------------
    private WOActionResults handleSubmissionStatus()
    {
        return new ECActionWithResult<WOActionResults>() {
            @Override
            public WOActionResults action()
            {
                PassPortRequest pReq = new PassPortRequest(request(), ec);

                // 1. Authenticate & Verify
                if (!pReq.isValid())
                {
                    log.warn("PassPort submission status query unauthorized: " + pReq.validationError());
                    return jsonError(HTTP_STATUS_UNAUTHORIZED,
                        pReq.validationError());
                }

                // 2. Resolve entities
                AssignmentOffering ao = pReq.assignmentOffering();
                User student = pReq.user();
                if (ao == null || student == null)
                {
                    log.warn("PassPort submission status 404: ao=" + ao + ", student=" + student);
                    return jsonError(HTTP_STATUS_NOT_FOUND,
                        "User or Resource not recognized");
                }

                // 3. Query student's submissions for this assignment offering
                NSArray<Submission> allSubs =
                    Submission.submissionsForAssignmentOfferingAndUserDescending(
                        ec, ao, student);

                // Determine submission for grading:
                NSArray<Submission> gradingCandidates =
                    Submission.submissionsForGrading(ec, ao, student);
                Submission subForGrading = (gradingCandidates != null && gradingCandidates.count() > 0)
                    ? gradingCandidates.objectAtIndex(0)
                    : null;

                if (subForGrading == null && allSubs != null && allSubs.count() > 0)
                {
                    for (Submission s : allSubs)
                    {
                        if (s.isSubmissionForGrading())
                        {
                            subForGrading = s;
                            break;
                        }
                    }
                    if (subForGrading == null)
                    {
                        for (Submission s : allSubs)
                        {
                            if (s.resultIsReady() && s.isBetterGradingChoiceThan(subForGrading))
                            {
                                subForGrading = s;
                            }
                        }
                    }
                    if (subForGrading == null)
                    {
                        subForGrading = allSubs.objectAtIndex(0);
                    }
                }

                // Determine most recent submission:
                Submission mostRecentSub = (allSubs != null && allSubs.count() > 0)
                    ? allSubs.objectAtIndex(0)
                    : null;

                JSONObject response = new JSONObject();
                try
                {
                    response.put("status", "ok");

                    if (subForGrading != null)
                    {
                        JSONObject gradingJson = formatSubmissionJson(subForGrading);
                        response.put("submission_for_grading", gradingJson);
                    }
                    else
                    {
                        response.put("submission_for_grading", JSONObject.NULL);
                    }

                    // for the most recent submission, if different from the submission for grading:
                    if (mostRecentSub != null && (subForGrading == null || !mostRecentSub.equals(subForGrading)))
                    {
                        JSONObject recentJson = formatSubmissionJson(mostRecentSub);
                        response.put("most_recent_submission", recentJson);
                    }
                }
                catch (JSONException e)
                {
                    log.error("Error creating submission status JSON response", e);
                    return jsonError(HTTP_STATUS_INTERNAL_ERROR,
                        "Internal error formatting JSON response");
                }

                return jsonResponse(HTTP_STATUS_OK, response);
            }
        }.call();
    }


    //~ Private Helper Methods ................................................

    // ----------------------------------------------------------
    /**
     * Deliver credentials asynchronously to callback_url in Phase 2 of dynamic registration.
     */
    private static void deliverCredentialsAsync(
        final String callbackUrl,
        final String brokerBaseUrl,
        final String clientName,
        final String extensionHandlerUrl)
    {
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                try
                {
                    final String[] creds = new String[2];
                    ECAction.run(new ECAction() {
                        public void action()
                        {
                            PassportClient client = PassportClient.firstObjectMatchingQualifier(
                                ec, PassportClient.brokerBaseUrl.is(brokerBaseUrl), null);
                            if (client == null)
                            {
                                String clientId = UUID.randomUUID().toString();
                                String clientSecret = PassportClient.generateSecret(32);
                                client = PassportClient.create(ec, clientId, clientSecret);
                                client.setName(clientName);
                                client.setBrokerBaseUrl(brokerBaseUrl);
                                try
                                {
                                    URI uri = new URI(brokerBaseUrl);
                                    client.setDomainPattern(uri.getHost());
                                }
                                catch (Exception e) {}
                                client.setRequestedProperties(
                                    "canvas_course_id,canvas_assignment_id,title,lti_user_id,canvas_user_id,email");
                                ec.saveChanges();
                            }
                            creds[0] = client.clientId();
                            creds[1] = client.clientSecret();
                        }
                    });

                    // Construct Phase 2 payload
                    JSONObject payload = new JSONObject();
                    payload.put("tool_name", "Web-CAT");
                    payload.put("passport_version", "1.1");

                    JSONObject endpoints = new JSONObject();
                    endpoints.put("extension_handler", extensionHandlerUrl);
                    payload.put("endpoints", endpoints);

                    JSONArray requestedProps = new JSONArray();
                    requestedProps.put("canvas_course_id");
                    requestedProps.put("canvas_assignment_id");
                    requestedProps.put("title");
                    requestedProps.put("lti_user_id");
                    requestedProps.put("canvas_user_id");
                    requestedProps.put("email");
                    payload.put("requested_properties", requestedProps);

                    JSONObject credentials = new JSONObject();
                    credentials.put("client_id", creds[0]);
                    credentials.put("client_secret", creds[1]);
                    payload.put("credentials", credentials);

                    byte[] postBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                    URL url = new URL(callbackUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    try (OutputStream os = conn.getOutputStream())
                    {
                        os.write(postBytes);
                        os.flush();
                    }

                    int responseCode = conn.getResponseCode();
                    log.info("PassPort Phase 2 delivery to " + callbackUrl
                        + " returned HTTP " + responseCode);
                    conn.disconnect();
                }
                catch (Exception e)
                {
                    log.error("Failed to deliver PassPort credentials to "
                        + callbackUrl + ": " + e.getMessage(), e);
                }
            }
        }).start();
    }


    // ----------------------------------------------------------
    private String configuredDomainWhitelist()
    {
        try
        {
            String prop = Application.wcApplication().properties()
                .getProperty(PROPERTY_DOMAIN_WHITELIST);
            if (prop != null && !prop.trim().isEmpty())
            {
                return prop.trim();
            }
        }
        catch (Exception e) {}
        return DEFAULT_DOMAIN_WHITELIST;
    }


    // ----------------------------------------------------------
    private String buildExtensionHandlerUrl()
    {
        try
        {
            String url = Application.completeURLWithRequestHandlerKey(
                context(), "wa", "passport/v1/extension", null, true, 0);
            if (url != null && !url.isEmpty())
            {
                return url;
            }
        }
        catch (Exception e) {}

        String scheme = request().isSecure() ? "https" : "http";
        String forwardedProto = request().headerForKey("x-forwarded-proto");
        if (forwardedProto != null && !forwardedProto.isEmpty())
        {
            scheme = forwardedProto;
        }
        String host = request().headerForKey("host");
        if (host == null || host.isEmpty())
        {
            host = "localhost";
        }
        return scheme + "://" + host + "/WebObjects/Web-CAT.woa/wa/passport/v1/extension";
    }


    // ----------------------------------------------------------
    /**
     * Formats submission data into a JSON object including timestamp,
     * submission number, total score, TA score, and direct URLs for
     * student view and grader view.
     */
    private JSONObject formatSubmissionJson(Submission sub)
        throws JSONException
    {
        JSONObject json = new JSONObject();

        // 1. timestamp
        if (sub.submitTime() != null)
        {
            json.put("timestamp", PassPortRequest.formatIsoDateTime(sub.submitTime()));
        }
        else
        {
            json.put("timestamp", JSONObject.NULL);
        }

        // 2. submission number
        json.put("submission_number", sub.submitNumber());
        json.put("submit_number", sub.submitNumber());

        // 3. total score
        SubmissionResult res = sub.result();
        if (res != null)
        {
            json.put("total_score", res.finalScore());
        }
        else
        {
            json.put("total_score", JSONObject.NULL);
        }

        // 4. ta score (null if none entered)
        if (res != null && res.taScoreRaw() != null)
        {
            json.put("ta_score", res.taScoreRaw());
        }
        else
        {
            json.put("ta_score", JSONObject.NULL);
        }

        // 5. direct url for the student view of feedback for the submission
        // 6. direct url to view the grading page for the submission
        Number idVal = sub.id();
        String viewUrl = buildSubmissionUrl("view", idVal);
        String editUrl = buildSubmissionUrl("edit", idVal);

        json.put("direct_url_feedback", viewUrl);
        json.put("feedback_url", viewUrl);
        json.put("view_url", viewUrl);
        json.put("student_view_url", viewUrl);

        json.put("direct_url_grading", editUrl);
        json.put("grading_url", editUrl);
        json.put("edit_url", editUrl);
        json.put("grader_view_url", editUrl);

        return json;
    }


    // ----------------------------------------------------------
    /**
     * Construct a direct URL for the submission direct action handler.
     */
    private String buildSubmissionUrl(String action, Number id)
    {
        String actionPath = "submission/" + action;
        String queryString = (id != null) ? ("id=" + id) : null;
        try
        {
            String url = Application.completeURLWithRequestHandlerKey(
                context(), "wa", actionPath, queryString, true, 0);
            if (url != null && !url.isEmpty())
            {
                return url;
            }
        }
        catch (Exception e) {}

        String scheme = request().isSecure() ? "https" : "http";
        String forwardedProto = request().headerForKey("x-forwarded-proto");
        if (forwardedProto != null && !forwardedProto.isEmpty())
        {
            scheme = forwardedProto;
        }
        String host = request().headerForKey("host");
        if (host == null || host.isEmpty())
        {
            host = "localhost";
        }
        String qs = (queryString != null) ? ("?" + queryString) : "";
        return scheme + "://" + host + "/WebObjects/Web-CAT.woa/wa/" + actionPath + qs;
    }


    // ----------------------------------------------------------
    public static WOResponse jsonResponse(int status, JSONObject obj)
    {
        WOResponse response = new WOResponse();
        response.setStatus(status);
        response.setHeader("application/json; charset=UTF-8", "Content-Type");
        response.appendContentString(obj.toString());
        return response;
    }


    // ----------------------------------------------------------
    public static WOResponse jsonError(int status, String message)
    {
        JSONObject obj = new JSONObject();
        try
        {
            obj.put("error", message);
            obj.put("status", status);
        }
        catch (JSONException e) {}
        return jsonResponse(status, obj);
    }


    // ----------------------------------------------------------
    private static void cleanStaleRegistryEntries()
    {
        long cutoff = System.currentTimeMillis() - REGISTRY_TTL_MS;
        Iterator<Map.Entry<String, ExtensionRecord>> it =
            EXTENSION_REGISTRY.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, ExtensionRecord> entry = it.next();
            if (entry.getValue().recordedTime < cutoff)
            {
                it.remove();
            }
        }
    }


    //~ Inner Classes .........................................................

    // ----------------------------------------------------------
    private static class ExtensionRecord
    {
        String requestId;
        int studentExtensionId;
        int assignmentOfferingId;
        int userId;
        boolean wasNewlyCreated;
        NSTimestamp previousOpensOn;
        NSTimestamp previousDueDate;
        NSTimestamp previousClosesOn;
        NSTimestamp appliedOpensOn;
        NSTimestamp appliedDueDate;
        NSTimestamp appliedClosesOn;
        long recordedTime;
    }


    //~ Static fields .........................................................
    private static final ConcurrentHashMap<String, ExtensionRecord> EXTENSION_REGISTRY =
        new ConcurrentHashMap<String, ExtensionRecord>();

    private static final Logger log = Logger.getLogger(passport.class);
}
