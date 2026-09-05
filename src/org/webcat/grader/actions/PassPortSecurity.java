/*==========================================================================*\
 |  PassPortSecurity.java
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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Cryptographic and validation utilities for the PassPort Protocol v1.
 * Handles HMAC-SHA256 calculation, timing-safe verification, timestamp
 * freshness checking, and domain/IP whitelist screening.
 *
 * @author PassPort API Generator
 */
public class PassPortSecurity
{
    //~ Constants .............................................................
    public static final String HMAC_SHA256 = "HmacSHA256";
    public static final long DEFAULT_MAX_DRIFT_SECONDS = 300L; // 5 minutes


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Computes the hex-encoded HMAC-SHA256 signature for a message given a secret.
     *
     * @param secret shared secret key
     * @param message serialized request body string
     * @return lowercase hex string of the HMAC-SHA256 signature
     * @throws GeneralSecurityException on crypto error
     */
    public static String computeHmacSha256(String secret, String message)
        throws GeneralSecurityException
    {
        if (secret == null)
        {
            throw new IllegalArgumentException("Secret key cannot be null");
        }
        if (message == null)
        {
            message = "";
        }

        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(keySpec);
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash)
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    // ----------------------------------------------------------
    /**
     * Verifies whether an incoming signature matches the expected HMAC-SHA256
     * in constant time to protect against timing attacks.
     *
     * @param secret shared secret key
     * @param message serialized request body
     * @param expectedHex signature from X-PassPort-Signature header
     * @return true if valid, false otherwise
     */
    public static boolean verifySignature(
        String secret, String message, String expectedHex)
    {
        if (secret == null || expectedHex == null)
        {
            return false;
        }

        try
        {
            String computedHex = computeHmacSha256(secret, message);
            byte[] computedBytes = computedHex.toLowerCase().getBytes(StandardCharsets.UTF_8);
            byte[] expectedBytes = expectedHex.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(computedBytes, expectedBytes);
        }
        catch (Exception e)
        {
            log.error("Error verifying HMAC-SHA256 signature: " + e.getMessage(), e);
            return false;
        }
    }


    // ----------------------------------------------------------
    /**
     * Checks whether an incoming Unix timestamp header is fresh within the allowed drift window.
     *
     * @param timestampHeader X-PassPort-Timestamp header value
     * @param maxDriftSeconds maximum permissible clock difference in seconds
     * @return true if timestamp is within [now - maxDrift, now + maxDrift], false otherwise
     */
    public static boolean isTimestampFresh(String timestampHeader, long maxDriftSeconds)
    {
        if (timestampHeader == null || timestampHeader.trim().isEmpty())
        {
            return false;
        }

        try
        {
            long ts = Long.parseLong(timestampHeader.trim());
            // If sent in milliseconds (13 digits), convert to seconds
            if (ts > 1000000000000L)
            {
                ts = ts / 1000L;
            }

            long nowSeconds = System.currentTimeMillis() / 1000L;
            long delta = Math.abs(nowSeconds - ts);
            return delta <= maxDriftSeconds;
        }
        catch (NumberFormatException e)
        {
            log.warn("Invalid X-PassPort-Timestamp header: " + timestampHeader);
            return false;
        }
    }


    // ----------------------------------------------------------
    /**
     * Validates whether a given host or IP matches the configured domain patterns.
     * Supports exact match, wildcard subdomains (*.domain.com), and regex patterns.
     *
     * @param patterns comma-separated whitelist patterns
     * @param host incoming hostname or IP
     * @return true if matching or if patterns is empty/wildcard, false otherwise
     */
    public static boolean matchesDomainPattern(String patterns, String host)
    {
        if (patterns == null || patterns.trim().isEmpty())
        {
            return true;
        }
        if (host == null || host.trim().isEmpty())
        {
            return false;
        }

        String target = host.trim().toLowerCase();
        // Remove port if present
        int colon = target.indexOf(':');
        if (colon > 0)
        {
            target = target.substring(0, colon);
        }

        String[] rules = patterns.split("[,\\s]+");
        for (String rule : rules)
        {
            rule = rule.trim().toLowerCase();
            if (rule.isEmpty())
            {
                continue;
            }
            if (rule.equals("*") || rule.equals(target))
            {
                return true;
            }
            if (rule.startsWith("*."))
            {
                String domain = rule.substring(2);
                if (target.equals(domain) || target.endsWith("." + domain))
                {
                    return true;
                }
            }
            if (rule.startsWith("^") || rule.endsWith("$"))
            {
                try
                {
                    if (Pattern.compile(rule).matcher(target).matches())
                    {
                        return true;
                    }
                }
                catch (Exception e)
                {
                    // Ignore regex syntax error
                }
            }
        }
        return false;
    }


    //~ Static fields .........................................................
    private static final Logger log = Logger.getLogger(PassPortSecurity.class);
}
