/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2008 Virginia Tech
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

package org.webcat.grader.objectquery;

import org.webcat.core.Course;
import org.webcat.core.CourseOffering;
import org.webcat.core.QualifierInSubquery;
import org.webcat.core.QualifierUtils;
import org.webcat.core.objectquery.AbstractQueryAssistantModel;
import org.webcat.grader.Assignment;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOKeyComparisonQualifier;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EONotQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import er.extensions.eof.ERXQ;
import er.extensions.foundation.ERXArrayUtilities;

//-------------------------------------------------------------------------
/**
 * The model of the query built by a
 * {@link CourseAndAssignmentSubmissionsAssistant}.
 *
 * @author aallowat
 * @version $Id$
 */
public class CourseAndAssignmentSubmissionsModel
    extends AbstractQueryAssistantModel
{
    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    public CourseAndAssignmentSubmissionsModel()
    {
        selectedCourseModelItems = new NSMutableArray<Object>();
        selectedAssignmentModelItems = new NSMutableArray<Object>();
        includeCourseStaff = false;
        includeOnlySubmissionsForGrading = true;
    }


    // ----------------------------------------------------------
    @Override
    public EOQualifier qualifierFromValues()
    {
        if (selectedCourseModelItems.isEmpty()
                || selectedAssignmentModelItems.isEmpty())
        {
            return null;
        }
        else
        {
            NSMutableArray<EOQualifier> terms =
                new NSMutableArray<EOQualifier>();

            terms.addObject(QualifierUtils.qualifierForInCondition(
                "assignmentOffering.courseOffering", selectedCourseOfferings()));

            terms.addObject(QualifierUtils.qualifierForInCondition(
                    "assignmentOffering.assignment", selectedAssignments()));

            if (includeOnlySubmissionsForGrading)
            {
                terms.addObject(ERXQ.isTrue("submissionForGrading"));
            }

            if (!includeCourseStaff)
            {
                EOQualifier q1 =
                    QualifierUtils.qualifierForKeyPathInRelationship(
                        "user",
                        "assignmentOffering.courseOffering.instructors");

                EOQualifier q2 =
                    QualifierUtils.qualifierForKeyPathInRelationship(
                        "user",
                        "assignmentOffering.courseOffering.graders");

                terms.addObject(ERXQ.not(q1));
                terms.addObject(ERXQ.not(q2));
            }

            return new EOAndQualifier(terms);
        }
    }


    // ----------------------------------------------------------
    @Override
    public void takeValuesFromQualifier(EOQualifier qualifier)
    {
        selectedCourseModelItems = new NSMutableArray<Object>();
        selectedAssignmentModelItems = new NSMutableArray<Object>();
        includeCourseStaff = false;
        includeOnlySubmissionsForGrading = true;

        boolean foundSubsForGradingQualifier = false;
        boolean excludeGraders = false;
        boolean excludeInstructors = false;

        if (qualifier instanceof EOAndQualifier)
        {
            EOAndQualifier aq = (EOAndQualifier)qualifier;
            NSArray<EOQualifier> terms = aq.qualifiers();

            for (EOQualifier q : terms)
            {
                NSDictionary<String, Object> info =
                    QualifierUtils.infoIfInQualifier(q);

                if (info != null)
                {
                    String key = (String)info.objectForKey("key");
                    NSArray<?> values = (NSArray<?>)info.objectForKey("values");

                    if ("assignmentOffering.courseOffering".equals(key))
                    {
                        selectedCourseModelItems =
                            new NSMutableArray<Object>(values);
                    }
                    else if ("assignmentOffering.assignment".equals(key))
                    {
                        selectedAssignmentModelItems =
                            new NSMutableArray<Object>(values);
                    }
                }
                else if (q instanceof EOKeyValueQualifier)
                {
                    EOKeyValueQualifier kvq = (EOKeyValueQualifier)q;

                    if ("submissionForGrading".equals(kvq.key())
                            && EOQualifier.QualifierOperatorEqual.equals(kvq.selector())
                            && Boolean.TRUE.equals(kvq.value()))
                    {
                        foundSubsForGradingQualifier = true;
                    }
                }
                else if (q instanceof EONotQualifier)
                {
                    EOQualifier nq = ((EONotQualifier) q).qualifier();

                    if (nq instanceof QualifierInSubquery)
                    {
                        QualifierInSubquery qis = (QualifierInSubquery) nq;

                        if (qis.qualifier() instanceof EOKeyComparisonQualifier)
                        {
                            EOKeyComparisonQualifier kcq =
                                (EOKeyComparisonQualifier) qis.qualifier();

                            if ("user.id".equals(kcq.leftKey())
                                    && EOQualifier.QualifierOperatorEqual.equals(kcq.selector()))
                            {
                                if ("assignmentOffering.courseOffering.instructors.id".equals(kcq.rightKey()))
                                {
                                    excludeInstructors = true;
                                }
                                else if ("assignmentOffering.courseOffering.graders.id".equals(kcq.rightKey()))
                                {
                                    excludeGraders = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!foundSubsForGradingQualifier)
        {
            includeOnlySubmissionsForGrading = false;
        }

        if (!excludeInstructors && !excludeGraders && qualifier != null)
        {
            includeCourseStaff = true;
        }
    }


    // ----------------------------------------------------------
    public void pruneAssignmentsFromUnselectedCourses()
    {
        NSArray<Course> selectedCourses = selectedCourses();

        for (int i = 0; i < selectedAssignmentModelItems.count(); )
        {
            Object o = selectedAssignmentModelItems.objectAtIndex(i);

            if (o instanceof Assignment)
            {
                Assignment a = (Assignment) o;

                NSArray<Course> assignmentCourses = (NSArray<Course>)
                    a.valueForKeyPath(Assignment.COURSES_KEY);

                if (ERXArrayUtilities.arrayContainsAnyObjectFromArray(
                        assignmentCourses, selectedCourses))
                {
                    i++;
                }
                else
                {
                    selectedAssignmentModelItems.removeObjectAtIndex(i);
                }
            }
            else
            {
                i++;
            }
        }
    }


    // ----------------------------------------------------------
    public NSArray<Object> selectedCourseModelItems()
    {
        return selectedCourseModelItems;
    }


    // ----------------------------------------------------------
    public void setSelectedCourseModelItems(NSArray<Object> array)
    {
        selectedCourseModelItems = new NSMutableArray<Object>(array);
    }


    // ----------------------------------------------------------
    public NSArray<Object> selectedAssignmentModelItems()
    {
        return selectedAssignmentModelItems;
    }


    // ----------------------------------------------------------
    public void setSelectedAssignmentModelItems(NSArray<Object> array)
    {
        selectedAssignmentModelItems = new NSMutableArray<Object>(array);
    }


    // ----------------------------------------------------------
    public NSArray<CourseOffering> selectedCourseOfferings()
    {
        NSMutableArray<CourseOffering> array =
            new NSMutableArray<CourseOffering>();

        for (Object obj : selectedCourseModelItems)
        {
            if (obj instanceof CourseOffering)
            {
                array.addObject(obj);
            }
        }

        return array;
    }


    // ----------------------------------------------------------
    public NSArray<Course> selectedCourses()
    {
        NSMutableArray<Course> courses = new NSMutableArray<Course>();

        for (Object obj : selectedCourseModelItems)
        {
            if (obj instanceof CourseOffering)
            {
                CourseOffering co = (CourseOffering) obj;
                Course c = co.course();

                if (!courses.contains(c))
                {
                    courses.addObject(c);
                }
            }
        }

        return courses;
    }


    // ----------------------------------------------------------
    public NSArray<Assignment> selectedAssignments()
    {
        NSMutableArray<Assignment> array =
            new NSMutableArray<Assignment>();

        for (Object obj : selectedAssignmentModelItems)
        {
            if (obj instanceof Assignment)
            {
                array.addObject(obj);
            }
        }

        return array;
    }


    // ----------------------------------------------------------
    public boolean includeOnlySubmissionsForGrading()
    {
        return includeOnlySubmissionsForGrading;
    }


    // ----------------------------------------------------------
    public void setIncludeOnlySubmissionsForGrading(boolean v)
    {
        includeOnlySubmissionsForGrading = v;
    }


    // ----------------------------------------------------------
    public boolean includeCourseStaff()
    {
        return includeCourseStaff;
    }


    // ----------------------------------------------------------
    public void setIncludeCourseStaff(boolean v)
    {
        includeCourseStaff = v;
    }


    // ----------------------------------------------------------
    public String humanReadableDescription()
    {
        StringBuffer buffer = new StringBuffer();

        if (includeOnlySubmissionsForGrading)
        {
            buffer.append("Submissions for grading ");
        }
        else
        {
            buffer.append("All submissions ");
        }

        buffer.append("by students ");

        if (includeCourseStaff)
        {
            buffer.append("and staff ");
        }

        buffer.append("to the assignments (");

        NSArray<Assignment> assignments = selectedAssignments();
        if (assignments != null && assignments.count() > 0)
        {
            buffer.append(assignments.objectAtIndex(0).name());
            for (int i = 1; i < assignments.count(); i++)
            {
                buffer.append(", ");
                buffer.append(assignments.objectAtIndex(i).name());
            }
        }

        buffer.append(") in course offerings (");

        NSArray<CourseOffering> offerings = selectedCourseOfferings();
        if (offerings != null && offerings.count() > 0)
        {
            buffer.append(offerings.objectAtIndex(0).compactName());
            for (int i = 1; i < offerings.count(); i++)
            {
                buffer.append(", ");
                buffer.append(offerings.objectAtIndex(i).compactName());
            }
        }

        buffer.append(")");

        return buffer.toString();
    }


    //~ Instance/static variables .............................................

    private NSMutableArray<Object> selectedCourseModelItems;
    private NSMutableArray<Object> selectedAssignmentModelItems;
    private boolean includeOnlySubmissionsForGrading;
    private boolean includeCourseStaff;
}
