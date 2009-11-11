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

package net.sf.webcat.grader;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

// -------------------------------------------------------------------------
/**
 *  Represents test coverage metrics for one file/class in a submission.
 *
 *  @author Stephen H. Edwards
 *  @version $Id$
 */
public class SubmissionFileStats
    extends _SubmissionFileStats
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new SubmissionFileStats object.
     */
    public SubmissionFileStats()
    {
        super();
    }


    //~ Constants (for key names) .............................................

    // Attributes ---
    public static final String SOURCE_FILE_NAME_KEY = "sourceFileName";


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public double gradedElementsCoverage()
    {
        int num = elements();
        int numCovered = elementsCovered();
        if ( num != 0 )
        {
            return ( (double)numCovered ) / ( (double)num );
        }
        else
        {
            return 1.0;
        }
    }


    // ----------------------------------------------------------
    public double gradedElementsCoveragePercent()
    {
        return gradedElementsCoverage() * 100.0;
    }


    // ----------------------------------------------------------
    public int totalElements()
    {
        return statements() + conditionals() + methods();
    }


    // ----------------------------------------------------------
    public int totalElementsCovered()
    {
        return statementsCovered() + conditionalsCovered() + methodsCovered();
    }


    // ----------------------------------------------------------
    public double totalElementsCoverage()
    {
        int num = totalElements();
        int numCovered = totalElementsCovered();
        if ( num != 0 )
        {
            return ( (double)numCovered ) / ( (double)num );
        }
        else
        {
            return 1.0;
        }
    }


    // ----------------------------------------------------------
    public String fullyQualifiedClassName()
    {
        String pkg = pkgName();
        if ( pkg != null )
        {
            return pkg + "." + className();
        }
        else
        {
            return className();
        }
    }


    // ----------------------------------------------------------
    public String sourceFileName()
    {
        String result = sourceFileNameRaw();
        if ( result == null )
        {
            result = fullyQualifiedClassName().replace( '.', '/' ) + ".java";
            setSourceFileNameRaw( result );
        }
        return result;
    }


    // ----------------------------------------------------------
    public String markupFileName()
    {
        String result = markupFileNameRaw();
        if ( result == null )
        {
            result =  "clover/" + fullyQualifiedClassName().replace( '.', '/' )
                + ".html";
        }
        return result;
    }


    // ----------------------------------------------------------
    public java.io.File markupFile()
    {
        return new java.io.File(
            submissionResult().submission().resultDirName(),
            markupFileName() );
    }


    // ----------------------------------------------------------
    /**
     * Get the corresponding icon URL for this file's grading status.
     *
     * @return The image URL as a string
     */
    public String statusURL()
    {
        return Status.statusURL( status() );
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the number of TA/instructor remarks made on this file.
     * @return a count of manual comments entered for this file
     */
    public int staffRemarks()
    {
        NSArray comments = comments();
        return comments == null
            ? 0
            : comments.count();
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the total number of remarks on this file: auto-graded +
     * manual.
     * @return the value of the attribute
     */
    public int totalRemarks()
    {
        return remarks() + staffRemarks();
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the total number of manual point deductions on this file.
     * @return the value of the attribute
     */
    public double staffDeductions()
    {
        if (!staffDeductionsIsValid)
        {
            staffDeductions = 0.0;
            NSArray comments = comments();
            for ( int i = 0; i < comments.count(); i++ )
            {
                SubmissionFileComment thisComment =
                    (SubmissionFileComment)comments.objectAtIndex( i );
                staffDeductions += thisComment.deduction();
            }
            staffDeductionsIsValid = true;
        }
        return staffDeductions;
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the total number of automated grading point deductions
     * on this file.
     * @return the value of the attribute
     */
    public double toolDeductions()
    {
        return deductions() - staffDeductions();
    }


    // ----------------------------------------------------------
    /**
     * Add a new entity to the <code>comments</code>
     * relationship (DO NOT USE--instead, use
     * <code>addToCommentsRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The new entity to relate to
     */
    public void addToComments( net.sf.webcat.grader.SubmissionFileComment value )
    {
        staffDeductionsIsValid = false;
        super.addToComments(value);
    }


    // ----------------------------------------------------------
    /**
     * Remove a specific entity from the <code>comments</code>
     * relationship (DO NOT USE--instead, use
     * <code>removeFromCommentsRelationship()</code>.
     * This method is provided for WebObjects use.
     *
     * @param value The entity to remove from the relationship
     */
    public void removeFromComments( net.sf.webcat.grader.SubmissionFileComment value )
    {
        staffDeductionsIsValid = false;
        super.removeFromComments(value);
    }


    // ----------------------------------------------------------
    /**
     * Add a new entity to the <code>comments</code>
     * relationship.
     *
     * @param value The new entity to relate to
     */
    public void addToCommentsRelationship( net.sf.webcat.grader.SubmissionFileComment value )
    {
        staffDeductionsIsValid = false;
        super.addToCommentsRelationship(value);
    }


    // ----------------------------------------------------------
    /**
     * Remove a specific entity from the <code>comments</code>
     * relationship.
     *
     * @param value The entity to remove from the relationship
     */
    public void removeFromCommentsRelationship( net.sf.webcat.grader.SubmissionFileComment value )
    {
        staffDeductionsIsValid = false;
        super.removeFromCommentsRelationship(value);
    }


    // ----------------------------------------------------------
    /**
     * Create a brand new object that is a member of the
     * <code>comments</code> relationship.
     *
     * @return The new entity
     */
    public net.sf.webcat.grader.SubmissionFileComment createCommentsRelationship()
    {
        staffDeductionsIsValid = false;
        return super.createCommentsRelationship();
    }


    // ----------------------------------------------------------
    /**
     * Remove a specific entity that is a member of the
     * <code>comments</code> relationship.
     *
     * @param value The entity to remove from the relationship
     */
    public void deleteCommentsRelationship( net.sf.webcat.grader.SubmissionFileComment value )
    {
        staffDeductionsIsValid = false;
        super.deleteCommentsRelationship(value);
    }


    // ----------------------------------------------------------
    /**
     * Remove (and then delete, if owned) all entities that are members of the
     * <code>comments</code> relationship.
     */
    public void deleteAllCommentsRelationships()
    {
        staffDeductionsIsValid = false;
        super.deleteAllCommentsRelationships();
    }


    // ----------------------------------------------------------
    /**
     * Change the value of this object's <code>deductions</code>
     * property.
     *
     * @param value The new value for this property
     */
    public void setDeductionsRaw( Double value )
    {
        staffDeductionsIsValid = false;
        super.setDeductionsRaw(value);
    }


    // ----------------------------------------------------------
    /**
     * A convenience method that returns true if this file has been tagged with
     * the specified tag.
     *
     * @param tag the tag to search for
     * @return true if the file has been tagged with the specified tag
     */
    public boolean hasTag(String tag)
    {
        // As noted in GraderQueueProcessor, we always search for the tag with
        // surrounding spaces so that tags that are infixes of other tags do
        // not return false positives. The tags() attribute is guaranteed to
        // have leading and trailing spaces as well so that this always works.

        if (tags() == null)
        {
            return false;
        }
        else
        {
            return tags().contains(" " + tag + " ");
        }
    }


    // ----------------------------------------------------------
    public String codeWithComments(
        User user,
        boolean isGrading,
        com.webobjects.appserver.WORequest request )
        throws Exception
    {
        File file = markupFile();
        //make the html file
        StringBuffer contents = new StringBuffer( (int)file.length() );
//        contents.append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 " );
//        contents.append( "Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD" );
//        contents.append( "/xhtml1-strict.dtd\">\n" );
        contents.append(
        "<link rel=\"stylesheet\" type=\"text/css\" href=\"" );
        contents.append( WCResourceManager.resourceURLFor(
            "wc-code.css", "Grader", null, request ) );
        contents.append( "\"/>\n" );

        //get the array of file comments from the database
        NSArray comments = comments().sortedArrayUsingComparator(
            SubmissionFileComment.STANDARD_ORDERING );

        /*
        StringBuffer fileoutput = new StringBuffer( (int)file.length() );
        try
    {
        FileReader in = new FileReader( file );
        final int BUFFER_SIZE = 8192;
        char[] b = new char[BUFFER_SIZE];
        int count = in.read( b, 0, BUFFER_SIZE );
        while ( count > -1 )
        {
        fileoutput.append( b, 0, count );
        count = in.read( b, 0, BUFFER_SIZE );
        }
        java.io.FileWriter out =
        new java.io.FileWriter( new File( "C:/comments.out" ) );
        out.write( fileoutput.toString() );
        out.close();
    }
    catch ( Exception e )
    {
        log.error( "error loading file contents for " + file.getPath(),
               e );
    }
    */

        // parse the HTML text into a DOM structure
        FileInputStream inStream = null;
        try
        {
            SAXBuilder parser = new SAXBuilder();
            inStream = new FileInputStream( file );
            Document doc = parser.build( inStream );
            Element root = doc.getRootElement();
            List children = root.getChild( "TBODY" ).getChildren();
            ListIterator iterator = children.listIterator();

            int index          = 0;
            int box_number     = 1;
            int reference      = 0;
            String prefixToId  = "";
            boolean showPts    = false;
            boolean isEditable = false;
            while ( iterator.hasNext() )
            {
                Element child = (Element)iterator.next();
                // get the id attribute from the row
                String id = child.getAttributeValue( "id" );
                if (    ( id.charAt( 0 ) == '\"' )
                     && ( id.charAt( id.length() - 1 ) == '\"' ) )
                {
                    // if quotes around it
                    id = id.substring( 1, id.length() - 1 );
                }
                String [] idarr = id.split( ":" );
                if ( idarr[0].charAt( 0 ) == 'O' ) // outside
                {
                    // check to see if this is the row where the comment
                    // needs to be inserted
                    int rownum = Integer.parseInt( idarr[1] );
                    while (    ( index != comments.count() )
                        && ( ( (SubmissionFileComment)comments
                           .objectAtIndex( index ) ).lineNo()
                         == rownum ) )
                    {
                        log.debug( "index = " + index
                           + " count = " + comments.count() );
                        // make a new comment with the properties and
                        // insert it after the line
                        SubmissionFileComment thisComment =
                            (SubmissionFileComment)comments.
                        objectAtIndex( index );
                        if ( thisComment.readableByUser( user ) )
                        {
                            log.debug( "Inserting comment at line number "
                                       + thisComment.lineNo() );
                            box_number++;
                            reference  = box_number;
                            showPts    = true; // should be false, later ...
                            isEditable = false;

                            // if the comment is of the current users,
                            if ( thisComment.author() == user )
                            {
                                prefixToId = "I";
                                isEditable = isGrading;
                            }
                            else
                            {
                                // this comment is by someone else, not the
                                // current user, so make it uneditable
                                prefixToId = "F";
                                isEditable = false;
                            }

                            // Also need to check user's relationship with
                            // course and enable/disable showPts appropriately

                            String idnum = prefixToId + box_number + ":"
                                + rownum + ":" + reference;
                            // ---- first row ----
                            String firstrow = "<tr id=\"" + idnum
                                + "\"><td id=\"" + idnum + "\" colspan=\"3\">"
                                + "<div id=\"" + idnum + "\"><img id=\""
                                + idnum + "\" src=\"/images/blank.gif\" "
                                + "width=\"1\" height=\"2\"/></div></td></tr>";
                            // pass it through the XML parser before putting
                            // it in JDOM
                            Document doc1 = parser.build(
                                new StringReader( firstrow ) );
                            // ---- second row ----
                            String pts = null;
                            // basically, check here if it is a code review
                            // assignment or a TA grading page, and
                            // suppress score if it is
                            // log.debug( "deduction = "
                            //            + thisComment.deduction() );
                            if ( thisComment.deduction() != 0.0
                                 && showPts )
                            {
                                pts = "" + thisComment.deduction();
                            }
                            // log.debug( "pts = \"" + pts + "\"" );

                            // replace the current value of the
                            // contentEditable tag with the new value
                            String newmes = thisComment.message();
                            log.debug( "newmes is = " + newmes );
                            newmes = newmes.replaceAll( "&&&&", idnum );
                            newmes = newmes.replaceAll(
                                "content[e|E]ditable=\"[false|true]\"",
                                "contentEditable=\"" + isEditable + "\"" );
                            log.debug( newmes );
                            String vals = "<table id=\"" + idnum
                                + ":X\" border=\"0\" cellpadding=\"0\"><tbody "
                                + "id=\"" + idnum + ":B\"><tr id=\"" + idnum
                                + ":R\"><td id=\"" + idnum
                                + ":D\" class=\"messageBox\"><img id=\""
                                + idnum + ":I\" src=\""
                                + thisComment.categoryIcon()
                                + "\" border=\"0\"/><option id=\"" + idnum
                                + ":T\" value=\"" + thisComment.to()
                                + "\"/><b id=\"" + idnum + "\"> <span id=\""
                                + idnum + ":C\">" + thisComment.category()
                                + "</span> <span id=\"" + idnum + ":N\">["
                                + thisComment.author().name()
                                + "]"
                                + ( ( pts != null )
                                    ? " : </span><span id=\"" + idnum
                                      + ":P\" contentEditable=\"" + isEditable
                                      + "\">" + pts + "</span>"
                                    : "</span>"
                                  )
                                + "</b><br id=\"" + idnum
                                + "\"/><i id=\"" + idnum + "\">" + newmes
                                + "</i></td></tr></tbody></table>";
                            // log.debug( "vals = " + vals );

                            String secondrow = "<tr id=\"" + idnum
                                + "\"><td id=\"" + idnum + "\"><div id=\""
                                + idnum + "\"> </div></td><td id=\"" + idnum
                                + "\"><div id=\"" + idnum + "\"> </div></td>"
                                + "<td id=\"" + idnum
                                + "\" align=\"left\"><div id=\"" + idnum
                                + "\">" + vals + "</div></td></tr>";

                            // pass it through the XML parser before putting
                            // it in JDOM
                            Document doc2 =
                            parser.build( new StringReader( secondrow ) );

                            // ---- third row ----
                            String thirdrow = "<tr id=\"" + idnum
                                + "\"><td id=\"" + idnum
                                + "\" colspan=\"3\"><div id=\"" + idnum
                                + "\"><img id=\"" + idnum
                                + "\" src=\"/images/blank.gif\" "
                               + "width=\"1\" height=\"2\"/></div> </td></tr>";
                            // pass it through the XML parser before putting
                            // it in JDOM
                            Document doc3 = parser.build(
                                new StringReader( thirdrow ) );

                            int newcat = thisComment.categoryNo();

                            // check to see if it has any attributes
                            if ( !child.getAttributes().isEmpty() )
                            {
                                String classname =
                                    child.getAttributeValue( "class" );
                                if ( classname != null )
                                {
                                    if (    classname.charAt( 0 ) == '\"'
                                         && classname.charAt(
                                                classname.length() - 1 )
                                            == '\"' )
                                    {
                                        // if quotes around it
                                        classname = classname.substring(
                                            1, classname.length() - 1 );
                                    }
                                    int thisCategory = SubmissionFileComment
                                        .categoryIntFromString( classname );
                                    if ( thisCategory < newcat )
                                        newcat = thisCategory;
                                    child.removeAttribute( "class" );
                                }
                            }
                            child.setAttribute( "class",
                            SubmissionFileComment.categoryName( newcat ) );
                            // inserting the comment box
                            iterator.add( doc1.detachRootElement() );
                            iterator.add( doc2.detachRootElement() );
                            iterator.add( doc3.detachRootElement() );
                        }
                                index++;    // go to next comment
                    }   //while ends here
                }   // big if ends here
            }   // big while ends here
            // Now render the DOM tree in string form at append it
            // to contents
            XMLOutputter outputter = new XMLOutputter();
            outputter.setOmitDeclaration( true );
            contents.append( outputter.outputString( doc ) );
        }
        catch ( Exception e )
        {
            log.error( "exception parsing raw HTML file "
                       + markupFile().getPath(),
                       e );
            Application.sendAdminEmail(
                null,
                submissionResult().submission().assignmentOffering()
                    .courseOffering().instructors(),
                true,
                "Exception in SubmissionFileStats",
                "This is an automatic message from the Web-CAT server.  An "
                + "exception was caught while\nattempting to read "
                + "the raw HTML stored in the file:\n\n"
                + markupFile().getPath()
                + "\n\nThis error may be due to errors in the HTML "
                + "generated by the grading script.\n"
                + ( (Application)Application.application() )
                      .informationForExceptionInContext( e, null, null ),
                null );
            throw e;
        }
        finally
        {
            // Ensure the stream is closed, to prevent exceeding the
            // max # of file handles
            if ( inStream != null )
            {
                try
                {
                    inStream.close();
                }
                catch ( Exception e )
                {
                    // Just swallow it
                }
            }
        }

        /* try
        {
            FileReader in = new FileReader( file );
            final int BUFFER_SIZE = 8192;
            char[] b = new char[BUFFER_SIZE];
            int count = in.read( b, 0, BUFFER_SIZE );
            while ( count > -1 )
            {
                contents.append( b, 0, count );
                count = in.read( b, 0, BUFFER_SIZE );
            }
        }
        catch ( Exception e )
        {
            log.error( "error loading file contents for " + file.getPath(),
                       e );
        }
        */

        return contents.toString();
    }


// If you add instance variables to store property values you
// should add empty implementions of the Serialization methods
// to avoid unnecessary overhead (the properties will be
// serialized for you in the superclass).

//    // ----------------------------------------------------------
//    /**
//     * Serialize this object (an empty implementation, since the
//     * superclass handles this responsibility).
//     * @param out the stream to write to
//     */
//    private void writeObject( java.io.ObjectOutputStream out )
//        throws java.io.IOException
//    {
//    }
//
//
//    // ----------------------------------------------------------
//    /**
//     * Read in a serialized object (an empty implementation, since the
//     * superclass handles this responsibility).
//     * @param in the stream to read from
//     */
//    private void readObject( java.io.ObjectInputStream in )
//        throws java.io.IOException, java.lang.ClassNotFoundException
//    {
//    }


    //~ Instance/static variables .............................................
    private double staffDeductions;
    private boolean staffDeductionsIsValid;

    static Logger log = Logger.getLogger( SubmissionFileStats.class );
}
