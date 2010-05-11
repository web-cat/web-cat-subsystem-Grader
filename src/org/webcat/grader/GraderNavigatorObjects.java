package org.webcat.grader;

import java.util.EnumSet;
import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.INavigatorObject;
import org.webcat.core.Semester;
import org.webcat.core.CoreNavigatorObjects.SingleCourseOffering;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import er.extensions.eof.ERXQ;

//--------------------------------------------------------------------------
/**
 * This class contains wrapper objects that represent the selectable items in
 * the Web-CAT grader navigator component.
 *
 * @author Tony Allevato
 * @version $Id$
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
                result += "(unpub.)";
            }
            if (closed)
            {
                result += "(closed)";
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
