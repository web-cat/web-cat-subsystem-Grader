/*==========================================================================*\
 |  PassportClient.java
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

package org.webcat.grader;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSTimestamp;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Pattern;

// -------------------------------------------------------------------------
/**
 * Represents a registered PassPort Protocol external client broker.
 * Stores authentication credentials (client_id, client_secret) and
 * domain/pattern screening rules for incoming requests.
 *
 * @author PassPort API Generator
 */
public class PassportClient
    extends _PassportClient
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    public PassportClient()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public void willInsert()
    {
        NSTimestamp now = new NSTimestamp();
        if (created() == null)
        {
            setCreated(now);
        }
        if (lastModified() == null)
        {
            setLastModified(now);
        }
        if (clientId() == null || clientId().trim().isEmpty())
        {
            setClientId(UUID.randomUUID().toString());
        }
        if (clientSecret() == null || clientSecret().trim().isEmpty())
        {
            setClientSecret(generateSecret(32));
        }
        super.willInsert();
    }


    // ----------------------------------------------------------
    @Override
    public void willUpdate()
    {
        setLastModified(new NSTimestamp());
        super.willUpdate();
    }


    // ----------------------------------------------------------
    /**
     * Check if an incoming host or IP is allowed by this client's domain pattern.
     *
     * @param hostOrIp the incoming host or IP address
     * @return true if allowed or if no pattern is set, false otherwise
     */
    public boolean allowsOrigin(String hostOrIp)
    {
        String pattern = domainPattern();
        if (pattern == null || pattern.trim().isEmpty())
        {
            return true;
        }
        if (hostOrIp == null || hostOrIp.trim().isEmpty())
        {
            return false;
        }

        String target = hostOrIp.trim().toLowerCase();
        // Domain pattern may be comma or space separated list of patterns
        String[] rules = pattern.split("[,\\s]+");
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
                String rootDomain = rule.substring(2);
                if (target.equals(rootDomain) || target.endsWith("." + rootDomain))
                {
                    return true;
                }
            }
            // Check regex if enclosed in slashes or starts with regex prefix
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
                    // Ignore regex compile error
                }
            }
        }
        return false;
    }


    // ----------------------------------------------------------
    /**
     * Generate a cryptographically secure random hexadecimal secret.
     *
     * @param byteLength number of random bytes
     * @return hex-encoded random string
     */
    public static String generateSecret(int byteLength)
    {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(byteLength * 2);
        for (byte b : bytes)
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    // ----------------------------------------------------------
    /**
     * Look up a PassportClient by its clientId.
     *
     * @param ec editing context
     * @param clientId client ID to match
     * @return matching client or null
     */
    public static PassportClient clientForId(EOEditingContext ec, String clientId)
    {
        if (clientId == null || clientId.trim().isEmpty())
        {
            return null;
        }
        return uniqueObjectMatchingQualifier(ec, PassportClient.clientId.is(clientId.trim()));
    }


    //~ Static fields .........................................................
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
}
