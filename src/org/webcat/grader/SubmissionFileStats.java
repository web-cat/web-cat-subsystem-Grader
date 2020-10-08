/*==========================================================================*\
 |  $Id: SubmissionFileStats.java,v 1.12 2014/06/16 17:27:25 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2012 Virginia Tech
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

import com.webobjects.foundation.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.*;
import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.webcat.core.*;
import org.webcat.grader.messaging.GraderMarkupParseError;
import org.webcat.woextensions.WCResourceManager;

// -------------------------------------------------------------------------
/**
 *  Represents test coverage metrics for one file/class in a submission.
 *
 *  @author  Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.12 $, $Date: 2014/06/16 17:27:25 $
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
            result = fullyQualifiedClassName();
            if (result != null)
            {
                result = result.replace( '.', '/' ) + ".java";
                setSourceFileNameRaw( result );
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    public boolean canMarkupFile()
    {
        return markupFileName() != null
            && !markupFileName().startsWith("public/");
    }


    // ----------------------------------------------------------
    public String markupFileName()
    {
        String result = markupFileNameRaw();
        if (result == null)
        {
            result = fullyQualifiedClassName();
            if (result != null)
            {
                result = result.replace('.', '/') + ".html";
                File dir = new File(
                    submissionResult().submission().resultDirName());
                String[] places = { "html/", "html/src/", "clover/" };
                String[] arrayOfString1;
                int j = (arrayOfString1 = places).length;
                for (int i = 0; i < j; i++)
                {
                    String place = arrayOfString1[i];

                    String name = place + result;
                    File candidate = new File(dir, name);
                    if (candidate.exists())
                    {
                        result = name;
                        break;
                    }
                }
            }
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
        NSArray<SubmissionFileComment> myComments = comments();
        return myComments == null
            ? 0
            : myComments.count();
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
     * Retrieve the total number of point deductions on this file visible
     * to the student.
     * @return the value of the attribute
     */
    public double deductionsForStudent()
    {
        if (submissionResult().status() == Status.CHECK)
        {
            return deductions();
        }
        else
        {
            return deductions() - staffDeductions();
        }
    }


    // ----------------------------------------------------------
    /**
     * Retrieve the total number of point deductions on this file visible
     * to the given user.
     * @return the value of the attribute
     */
    public double deductionsVisibleTo(User user)
    {
        if (user.hasAdminPrivileges()
            || submissionResult().submission().assignmentOffering()
                .courseOffering().isStaff(user))
        {
            return deductions();
        }
        else
        {
            return deductionsForStudent();
        }
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
            for (SubmissionFileComment thisComment : comments())
            {
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
    public void addToComments( org.webcat.grader.SubmissionFileComment value )
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
    public void removeFromComments( org.webcat.grader.SubmissionFileComment value )
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
    public void addToCommentsRelationship( org.webcat.grader.SubmissionFileComment value )
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
    public void removeFromCommentsRelationship( org.webcat.grader.SubmissionFileComment value )
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
    public org.webcat.grader.SubmissionFileComment createCommentsRelationship()
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
    public void deleteCommentsRelationship( org.webcat.grader.SubmissionFileComment value )
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
    // To fix up incorrect markup generated between 1/21/2011 and 2/9/2011.
    // SF bug 3174285.
    private static final long PERIOD_START =
        new NSTimestamp(2011, 1, 21, 0, 0, 0, TimeZone.getTimeZone("UTC"))
        .getTime();
    private static final long PERIOD_END =
        new NSTimestamp(2011, 2, 10, 0, 0, 0, TimeZone.getTimeZone("UTC"))
        .getTime();
    private void rewriteMarkupIfNecessary(File markupFile)
    {
        if (!markupFile.exists()) return;
        long lastModified = markupFile.lastModified();
        if (lastModified > PERIOD_START && lastModified < PERIOD_END)
        {
            // Attempt to correct problems in markup
            File revised = new File(markupFile.getParentFile(),
                markupFile.getName() + ".rev");
            if (markupFile.exists() && !revised.exists())
            {
                try
                {
                    BufferedReader in =
                        new BufferedReader(new FileReader(markupFile));
                    PrintWriter out = new PrintWriter(revised);

                    String line = in.readLine();
                    while (line != null)
                    {
                        line = line.replace("&#xD;", "\r");
                        out.println(line);
                        line = in.readLine();
                    }

                    in.close();
                    out.close();

                    if (markupFile.delete())
                    {
                        if (!revised.renameTo(markupFile))
                        {
                            log.error("cannot rename " + revised + " to "
                                + markupFile);
                        }
                    }
                }
                catch (Exception e)
                {
                    log.error("error patching HTML file " + markupFile, e);
                }
            }
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
        rewriteMarkupIfNecessary(file);

        //make the html file
        StringBuffer contents = new StringBuffer( (int)file.length() );

        if (isGrading)
        {
            contents.append(
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"" );
            contents.append(WCResourceManager.versionlessResourceURLFor(
                "theme/base/code.css", "Core", null, request ));
            contents.append( "\"/>\n" );
        }

        //get the array of file comments from the database
        NSArray<SubmissionFileComment> myComments = comments()
            .sortedArrayUsingComparator(
                SubmissionFileComment.STANDARD_ORDERING);

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
            @SuppressWarnings("unchecked")
            List<Element> children = root.getChild( "TBODY" ).getChildren();
            ListIterator<Element> iterator = children.listIterator();

            int index          = 0;
            int box_number     = 1;
            int reference      = 0;
            String prefixToId  = "";
            boolean showPts    = false;
            boolean isEditable = false;
            while ( iterator.hasNext() )
            {
                Element child = iterator.next();
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
                    while (index != myComments.count()
                        && myComments.objectAtIndex(index).lineNo() == rownum)
                    {
                        log.debug( "index = " + index
                           + " count = " + myComments.count() );
                        // make a new comment with the properties and
                        // insert it after the line
                        SubmissionFileComment thisComment =
                            myComments.objectAtIndex( index );
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
                                + "<img id=\""
                                + idnum + "\" src=\""
                                +  WCResourceManager.resourceURLFor(
                                    "images/blank.gif", "Core", null, null)
                                + "\" width=\"1\" height=\"2\"/>"
                                + "</td></tr>";
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
                                + WCResourceManager.resourceURLFor(
                                    thisComment.categoryIcon(),
                                    "Core", null, null)
                                + "\" border=\"0\"/><input type=\"hidden\" "
                                + "id=\"" + idnum
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
                                + "\" colspan=\"3\"><img id=\"" + idnum
                                + "\" src=\""
                                + WCResourceManager.resourceURLFor(
                                    "images/blank.gif", "Core", null, null)
                                + "\" width=\"1\" height=\"2\"/>"
                                + "</td></tr>";
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
                            child.setAttribute("class",
                                    SubmissionFileComment.categoryName(newcat)
                                    .replaceAll("\\s", "_"));
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

            new GraderMarkupParseError(
                submissionResult().editingContext(),
                submissionResult().submission(),
                GraderMarkupParseError.LOCATION_SUBMISSION_FILE_STATS,
                e, null, markupFile(), null).send();

/*            Application.sendAdminEmail(
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
                null );*/
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


    //~ Instance/static variables .............................................

    private double staffDeductions;
    private boolean staffDeductionsIsValid;

    static Logger log = Logger.getLogger(SubmissionFileStats.class);
}
