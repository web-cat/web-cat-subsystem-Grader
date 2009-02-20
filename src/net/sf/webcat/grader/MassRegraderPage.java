package net.sf.webcat.grader;

import net.sf.webcat.core.Course;
import net.sf.webcat.core.CourseOffering;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import er.extensions.eof.ERXQ;
import er.extensions.foundation.ERXArrayUtilities;

public class MassRegraderPage extends GraderComponent
{
    public MassRegraderPage(WOContext context)
    {
        super(context);
    }
    
    
    public NSArray<CourseOffering> courseOfferings;
    public CourseOffering selectedCourseOffering;

    public NSArray<AssignmentOffering> assignmentOfferings;
    public AssignmentOffering selectedAssignmentOffering;

    public NSArray<Submission> submissions;
    public Submission submission;
    

    public void appendToResponse(WOResponse response, WOContext context)
    {
        if (courseOfferings == null)
        {
            courseOfferings =
                ERXArrayUtilities.arrayByAddingObjectsFromArrayWithoutDuplicates(
                        user().teaching(),
                        user().adminForButNoOtherRelationships());
        }

        super.appendToResponse(response, context);
    }

    
    public WOActionResults updateAssignments()
    {
        selectedAssignmentOffering = null;
        
        EOFetchSpecification fspec = new EOFetchSpecification(
                AssignmentOffering.ENTITY_NAME,
                ERXQ.equals("courseOffering", selectedCourseOffering),
                null);

        assignmentOfferings =
            localContext().objectsWithFetchSpecification(fspec);

        return null;
    }


    public WOActionResults massRegrade()
    {
        if (selectedAssignmentOffering == null
                || selectedCourseOffering == null)
        {
            return null;
        }

        NSArray<EOSortOrdering> sortOrderings = new NSArray<EOSortOrdering>(
                new EOSortOrdering[] {
                        new EOSortOrdering("user.userName",
                                EOSortOrdering.CompareCaseInsensitiveAscending),
                        new EOSortOrdering("submitNumber",
                                EOSortOrdering.CompareAscending)
                });

        EOFetchSpecification fspec = new EOFetchSpecification(
                Submission.ENTITY_NAME,
                ERXQ.is("assignmentOffering", selectedAssignmentOffering),
                sortOrderings);

        submissions = localContext().objectsWithFetchSpecification(fspec);

        // Enqueue the whole lot of submissions for regrading.

        for (Submission sub : submissions)
        {
            sub.requeueForGrading( localContext() );
        }
        localContext().saveChanges();
        Grader.getInstance().graderQueue().enqueue( null );
        
        return null;
    }
}
