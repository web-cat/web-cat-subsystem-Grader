/*==========================================================================*\
 |  Copyright (C) 2011-2021 Virginia Tech
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

import java.io.File;
import org.webcat.core.EntityResourceHandler;
import org.webcat.woextensions.WCFetchSpecification;

//-------------------------------------------------------------------------
/**
 * The Web-CAT entity resource handler for accessing resources associated with
 * BatchResult entities through direct URLs.
 *
 * @author  Tony Allevato
 */
public class GradingPluginResourceHandler
    extends EntityResourceHandler<GradingPlugin>
{
    //~ Methods ...............................................................

    // ----------------------------------------------------------
    @Override
    public File pathForResource(GradingPlugin object, String relativePath)
    {
        return object.fileForPublicResourceAtPath(relativePath);
    }


    // ----------------------------------------------------------
    @Override
    public WCFetchSpecification<GradingPlugin>
        fetchSpecificationForFriendlyName(String name)
    {
        return new WCFetchSpecification<GradingPlugin>(
                GradingPlugin.ENTITY_NAME,
                GradingPlugin.name.is(name),
                GradingPlugin.lastModified.ascs());
    }
}
