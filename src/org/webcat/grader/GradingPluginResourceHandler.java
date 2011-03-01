package org.webcat.grader;

import java.io.File;
import org.webcat.core.EntityResourceHandler;

//-------------------------------------------------------------------------
/**
 * The Web-CAT entity resource handler for accessing resources associated with
 * BatchResult entities through direct URLs.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class GradingPluginResourceHandler
    extends EntityResourceHandler<GradingPlugin>
{
    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public File pathForResource(GradingPlugin object, String relativePath)
    {
        return object.fileForPublicResourceAtPath(relativePath);
    }
}
