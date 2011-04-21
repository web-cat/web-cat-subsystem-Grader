package org.webcat.grader;

import org.webcat.core.INavigatorObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

//--------------------------------------------------------------------------
/**
 * This class contains wrapper objects that represent the selectable items in
 * the Web-CAT grader navigator component.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class GraderNavigatorObjects
{
    // ----------------------------------------------------------
    public static class SingleAssignment
        implements INavigatorObject
    {
        // ----------------------------------------------------------
        public SingleAssignment(Assignment assignment)
        {
            this.assignment = assignment;
        }


        // ----------------------------------------------------------
        public SingleAssignment(
            Assignment assignment,
            boolean unpublished,
            boolean closed)
        {
            this.assignment = assignment;
            this.unpublished = unpublished;
            this.closed = closed;
        }


        // ----------------------------------------------------------
        public NSArray<?> representedObjects()
        {
            return new NSMutableArray<Assignment>(assignment);
        }


        // ----------------------------------------------------------
        public String toString()
        {
            String result = (assignment == null)
                ? "null"
                : assignment.titleString();
            if (unpublished)
            {
                result += " (unpub.)";
            }
            if (closed)
            {
                result += " (closed)";
            }
            return result;
        }


        // ----------------------------------------------------------
        public boolean equals(Object obj)
        {
            if (obj instanceof SingleAssignment)
            {
                SingleAssignment o = (SingleAssignment) obj;
                return assignment.equals(o.assignment);
            }
            else
            {
                return false;
            }
        }


        private Assignment assignment;
        private boolean unpublished;
        private boolean closed;
    }
}
