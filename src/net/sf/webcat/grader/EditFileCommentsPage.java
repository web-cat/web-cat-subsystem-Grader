/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006 Virginia Tech
 |
 |  This file is part of Web-CAT.
 |
 |  Web-CAT is free software; you can redistribute it and/or modify
 |  it under the terms of the GNU General Public License as published by
 |  the Free Software Foundation; either version 2 of the License, or
 |  (at your option) any later version.
 |
 |  Web-CAT is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU General Public License for more details.
 |
 |  You should have received a copy of the GNU General Public License
 |  along with Web-CAT; if not, write to the Free Software
 |  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 |
 |  Project manager: Stephen Edwards <edwards@cs.vt.edu>
 |  Virginia Tech CS Dept, 660 McBryde Hall (0106), Blacksburg, VA 24061 USA
\*==========================================================================*/

package net.sf.webcat.grader;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.io.*;
import java.util.*;
import net.sf.webcat.core.*;
import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

// -------------------------------------------------------------------------
/**
 * This class presents the details for one file in a submission,
 * including all markup comments and a color-highlighted version
 * of the source code.
 *
 * @author Stephen Edwards, Hussein Vastani
 * @version $Id$
 */
public class EditFileCommentsPage
    extends GraderComponent
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * This is the default constructor
     *
     * @param context The page's context
     */
    public EditFileCommentsPage( WOContext context )
    {
        super( context );
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    public void appendToResponse( WOResponse response, WOContext context )
    {
        if (log.isDebugEnabled())
        {
            log.debug( "beginning appendToResponse()" );
            SubmissionResult result = prefs().submission().result();
            log.debug( "result = " + result);
            if (result != null)
            {
                log.debug( "result = " + result.hashCode());
                log.debug( "result EC = " + result.editingContext().hashCode());
            }
        }
        initializeCodeWithComments();
        super.appendToResponse( response, context );
        codeWithComments = null;
        log.debug( "ending appendToResponse()" );
    }


    // ----------------------------------------------------------
    public WOComponent saveDone()
    {
        try
        {
            updateTAScore( storeComments() );
            prefs().submissionFileStats().setStatus( Status.CHECK );
        }
        catch ( Exception e )
        {
            // This is thrown by an XML parse error in storeComments(),
            // so use it to avoid updating the TA Score
            error(
                "An error occurred while reading your comments.  "
                + "They could not be saved successfully.  The situation has "
                + "been reported to the administrator." );
        }
        return hasMessages() ? null : super.next();
    }


    // ----------------------------------------------------------
    public WOComponent saveContinue()
    {
        try
        {
            updateTAScore( storeComments() );
            prefs().submissionFileStats().setStatus( Status.UNFINISHED );
        }
        catch ( Exception e )
        {
            // This is thrown by an XML parse error in storeComments(),
            // so use it to avoid updating the TA Score
            error(
                "An error occurred while reading your comments.  "
                + "They could not be saved successfully.  The situation has "
                + "been reported to the administrator." );
        }
        return null;
    }


    // ----------------------------------------------------------
    public WOComponent saveFinishLater()
    {
        try
        {
            updateTAScore( storeComments() );
            prefs().submissionFileStats().setStatus( Status.UNFINISHED );
        }
        catch ( Exception e )
        {
            // This is thrown by an XML parse error in storeComments(),
            // so use it to avoid updating the TA Score
            error(
                "An error occurred while reading your comments.  "
                + "They could not be saved successfully.  The situation has "
                + "been reported to the administrator." );
        }
        return hasMessages() ? null : super.next();
    }


    // ----------------------------------------------------------
    public double toolTestingOtherFiles()
    {
        return otherFilesDeductions() - otherFilesTaDeductions();
    }


    // ----------------------------------------------------------
    public double otherFilesTaDeductions()
    {
        return projectTADeduction()
            - prefs().submissionFileStats().staffDeductions();
    }


    //---------------------------------------------
    public double otherFilesDeductions()
    {
        return projectDeductions() - prefs().submissionFileStats().deductions();
    }


    //---------------------------------------------
    public double projectDeductions()
    {
        double total = 0.0;
        total += projectTADeduction();
        total += projectToolTestingDeduction();
        return total;
    }


    // ----------------------------------------------------------
    public double projectTADeduction()
    {
        SubmissionResult result = prefs().submission().result();
        Number taScore = result.taScoreRaw();
        projectTADeduction = 0.0;
        if ( taScore != null )
        {
            projectTADeduction += taScore.doubleValue();
            projectTADeduction -= prefs().assignmentOffering().assignment()
                .submissionProfile().taPoints();
        }
        return projectTADeduction;
    }


    // ----------------------------------------------------------
    public double projectToolTestingDeduction()
    {
        SubmissionResult result   = prefs().submission().result();
        Number correctnessScore   = result.correctnessScoreRaw();
        SubmissionProfile profile = prefs().assignmentOffering()
            .assignment().submissionProfile();

        projectToolTestingDeduction = result.toolScore();
        projectToolTestingDeduction -= profile.toolPoints();
        if ( correctnessScore != null )
        {
            double correctnessPossible = 0.0;
            correctnessPossible += profile.availablePoints();
            correctnessPossible -= profile.taPoints();
            correctnessPossible -= profile.toolPoints();
            projectToolTestingDeduction +=
                correctnessScore.doubleValue() - correctnessPossible;
        }
        return projectToolTestingDeduction;
    }


    // ----------------------------------------------------------
    public void updateTAScore( double ptsTakenOff )
    {
        double score = 0.0;
        SubmissionResult result = prefs().submission().result();
        Number taScore = result.taScoreRaw();
        Number possible = prefs().assignmentOffering().assignment()
               .submissionProfile().taPointsRaw();
        log.debug( "updateTAScore()" );
        if ( taScore != null )
        {
            //calculate the TA score
            score = taScore.doubleValue();
            log.debug( "  TA score = " + score );
            // now subtract the new score, which takes care of the
            // difference in the scores
            score += ptsTakenOff;
            log.debug( " After subtracting new deductions, score now is = "
                       + score );
            result.setTaScore( score );
        }
        else
        {
            if ( possible != null ) // there is a possible value
            {
                score = possible.doubleValue();
                score += ptsTakenOff; // adding the deductions
                result.setTaScore( score );
            }
            else
            {
                result.setTaScore(result.taScore() + ptsTakenOff);
            }
        }
        SubmissionFileStats stats = prefs().submissionFileStats();
        double oldDeductions = stats.deductions();
        log.debug( "old deductions = " + oldDeductions );
        stats.setDeductions( oldDeductions + ptsTakenOff );
        log.debug( "new deductions = " + stats.deductions() );
    }


    // ----------------------------------------------------------
    public boolean hasAdjustments()
    {
        return prefs().submission().result().scoreAdjustment() != 0.0;
    }


    // ----------------------------------------------------------
    public boolean isGrading()
    {
        return true;
    }


    // ----------------------------------------------------------
    public boolean hasBonus()
    {
        return prefs().submission().result().scoreAdjustment() > 0.0;
    }


    // ----------------------------------------------------------
    public Double fixMe()
    {
        return null;
    }


    // ----------------------------------------------------------
    // This function is called when we save the file after adding
    // comments. It will extract all the comments that were added,
    // and will insert them in the database.
    //----------------------------------------------------------
    public double storeComments()
        throws Exception
    {
        double taPts = 0.0;
        EOEditingContext ec = localContext();
        if ( codeWithCommentsToStore == null )
        {
            return taPts;
        }
        if ( codeWithCommentsToStore.length() < 50 &&
             codeWithCommentsToStore.trim().equals( "<br />" ) )
        {
            // An error occurred, but it should have already been
            // trapped elsewhere
            throw new Exception(
                "storeComments(): null (<br/>) code returned from client" );
        }
        try
        {
            // remove the link statement

            if ( log.isDebugEnabled() )
            {
                log.debug( "before subst:\n---------------------------------" );
                log.debug( codeWithCommentsToStore.substring( 0, 200 ) );
                log.debug( "----------------------------------" );
            }
            codeWithCommentsToStore = codeWithCommentsToStore
                .replaceFirst( "^\\s*<![^>]*>\\s*", "" )
                .replaceFirst( "^\\s*<link [^>]*>\\s*", "" );
//            if ( codeWithCommentsToStore.length() >= 85 )
//            {
//                if ( codeWithCommentsToStore.substring( 0, 5 ).equals( "<link" )
//                     && codeWithCommentsToStore.substring( 83, 85 ).equals( "/>" ) )
//                {
//                    codeWithCommentsToStore = codeWithCommentsToStore.substring(
//                        85, codeWithCommentsToStore.length() );
//                }
//            }
            if ( log.isDebugEnabled() )
            {
                log.debug( "after subst:\n----------------------------------" );
                log.debug( codeWithCommentsToStore.substring( 0, 200 ) );
                log.debug( "----------------------------------" );
            }

            // just for debugging purposes
            /*
            try
            {
                java.io.FileWriter out =
                    new java.io.FileWriter( new File( "C:/comments1.out" ) );
                out.write( codeWithComments );
                out.close();
            }
            catch ( Exception e )
            {
                log.error( "exception trying to save comment input", e );
            }
            */

            SAXBuilder parser     = new SAXBuilder();
            // Try parsing the file/string with the parser
            Document   doc        =
                parser.build( new StringReader( codeWithCommentsToStore ) );
            Element    root       = doc.getRootElement();
            List       children   = root.getChild( "tbody" ).getChildren();
            Iterator   iterator   = children.iterator();

            // Delete existing comments by the current user from the
            // database
            NSArray comments = prefs().submissionFileStats().comments();
            for ( int i = 0; i < comments.count(); i++ )
            {
                 SubmissionFileComment thisComment =
                     (SubmissionFileComment)comments.objectAtIndex( i );
                 // if its the current users comments
                 if ( user() == thisComment.author() )
                 {
                     log.debug( "Deleting comment, line "
                                + thisComment.lineNo() );
                     taPts -= thisComment.deduction();
                     ec.deleteObject( thisComment );
                 }
            }
            applyLocalChanges();

            // check all children for comment box (id should have I) and
            // then extract values
            while ( iterator.hasNext() )
            {
                Element child = (Element)iterator.next();
                // get the id attribute from the row
                String id = child.getAttributeValue( "id" );
                if ( id == null )
                {
                    log.error( "html tag is missing id attribute:" +
                        prefs().submissionFileStats().markupFile().getPath() );
                    continue;
                }
                if (    ( id.charAt( 0 ) == '\"' )
                     && ( id.charAt( id.length() - 1 ) == '\"' ) )
                {
                    // if there are quotes around the attribute value
                    id = id.substring( 1, id.length() - 1 );
                }
                String [] idarr = id.split( ":" );
                if ( idarr[0].charAt( 0 ) == 'I' )
                {
                    // This is a table tag
                    int    boxnum = Integer.parseInt(
                        idarr[0].substring( 1, idarr[0].length() ) );
                    int    rownum = Integer.parseInt( idarr[1] );
                    int    refnum = Integer.parseInt( idarr[2] );
                    String idpart = "I" + boxnum + ":" + rownum + ":" + refnum;
                    log.debug( "Tagged comment block found: " + idpart );

                    Element msg = getElementById( child, idpart + ":M" );
                    if ( msg != null )
                    {
                        SubmissionFileComment comment =
                            new SubmissionFileComment();
                        ec.insertObject( comment );
                        applyLocalChanges();
                        comment.setAuthorRelationship( user() );
                        comment.setSubmissionFileStatsRelationship(
                            prefs().submissionFileStats() );
                        Element target =
                            getElementById( child, idpart + ":T" );
                        if ( target != null )
                        {
                            comment.setTo(
                                target.getAttribute( "value" ).getValue() );
                            log.debug( "  to = " + comment.toNo() );
                        }
                        Element category =
                            getElementById( child, idpart + ":C" );
                        if ( category != null )
                        {
                            comment.setCategory( category.getText() );
                            log.debug( "  category = " + comment.category() );
                        }
                        Element pts = getElementById( child, idpart + ":P" );
                        if ( pts != null )
                        {
                            String ptVal = pts.getText();
                            int colonIndex = ptVal.indexOf( ":" );
                            if ( colonIndex != -1 )
                            {
                                // colon exists, so remove it
                                ptVal = ptVal.substring(
                                    colonIndex + 1, ptVal.length() );
                            }
                            ptVal = ptVal.trim();

                            if ( !ptVal.equals( "" ) )
                            {
                                try
                                {
                                    double deduction =
                                        Double.parseDouble( ptVal );
                                    taPts += deduction;
                                    comment.setDeduction( deduction );
                                }
                                catch ( NumberFormatException e )
                                {
                                    log.error( "Double conversion failure on "
                                               + "ptVal argument '"
                                               + ptVal + "'", e );
                                }
                            }
                            else // if no points given , then 0 is by default
                            {
                                comment.setDeduction( 0.0 );
                            }
                            log.debug( "  deduction = "
                                       + comment.deductionRaw() );
                        }
                        XMLOutputter outputter = new XMLOutputter();
                        outputter.setOmitDeclaration( true );
                        String newmes = outputter.outputString( msg );
                        newmes = newmes.replaceAll(idpart,"&&&&");
                        comment.setMessage( newmes );
                        log.debug( "  message = '" + comment.message() + "'" );
                        comment.setLineNo( rownum );
                        log.debug( "  line = " + comment.lineNo() );
                        applyLocalChanges();
                        log.debug( "result = " + comment );
                    }
                    else
                    {
                        log.debug( "Skipping blank separator TR" );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            log.error( "exception reading comments for "
                       + prefs().submissionFileStats().markupFile().getPath(),
                       e );
            Application.sendAdminEmail(
                null,
                prefs().submission().assignmentOffering().courseOffering()
                    .instructors(),
                true,
                "Exception in EditFileComments",
                "This is an automatic message from the Web-CAT server.  An "
                + "exception was caught while\nattempting to read "
                + "comments edited by a user in the file:\n\n"
                + prefs().submissionFileStats().markupFile().getPath()
                + "\n\nThe raw XML follows the exception details.\n"
                + ( (Application)application() )
                      .informationForExceptionInContext( e, null, context() )
                + "\n\nRaw XML (value of codeWithComments):\n"
                + codeWithCommentsToStore, null );
            throw e;
        }
        // Let the raw string be garbage collected, now that we're
        // finished with it
        codeWithCommentsToStore = null;

        log.debug( " Store comments is returning = " + taPts );
        return taPts;
    }


    // ----------------------------------------------------------
    public String codeWithComments()
    {
        return codeWithComments;
    }


    // ----------------------------------------------------------
    public void initializeCodeWithComments()
    {
        try
        {
            codeWithComments = prefs().submissionFileStats()
                .codeWithComments( user(), isGrading(), context().request() );
            if ( log.isDebugEnabled() )
            {
                log.debug( "codeWithComments = "
                                + codeWithComments.substring( 0, 200 ) );
            }
        }
        catch ( Exception e )
        {
            error(
                "An error occurred while trying to prepare the source code "
                + "view for this file.  The error has been reported to the "
                + "administrator.  Please do not try to edit the comments "
                + "in this file until the situation has been resolved." );
            codeWithComments = null;
        }
    }


    // ----------------------------------------------------------
    public void setCodeWithComments( String code )
    {
        codeWithCommentsToStore = code;
    }


    // ----------------------------------------------------------
    public String javascriptText()
    {
        StringBuffer buffer = new StringBuffer( 200 );
        buffer.append( "<script type=\"text/javascript\">\n" );
        buffer.append( "var editor = null;\nfunction initEditor() {\n" );
        buffer.append( "editor = new HTMLArea(\"source\");\n" );
        {
            String url = WCResourceManager.resourceURLFor(
                "htmlarea/htmlarea.js", "Grader", null, context().request() );
            if ( url != null )
            {
                buffer.append( "editor.config.editorURL = \"");
                buffer.append( url.substring( 0,
                    url.length() - "htmlarea.js".length() ) );
                buffer.append( "\";\n");
            }
            url = WCResourceManager.resourceURLFor(
                "images/blank.gif", "Core", null, context().request() );
            if ( url != null )
            {
                buffer.append( "editor.config.coreResourceURL = \"");
                buffer.append( url.substring( 0,
                    url.length() - "images/blank.gif".length() ) );
                buffer.append( "\";\n");
            }
        }
        buffer.append( "editor.generate();\n" );
        if ( wcSession() != null && user() != null )
        {
            buffer.append( "editor.config.userName = \"" );
            buffer.append( user().name() );
                buffer.append( "\";\n" );
        }
        buffer.append( "editor.config.numComments = " );
        try
        {
            buffer.append( prefs().submissionFileStats().comments().count() );
        }
        catch ( Exception e )
        {
            buffer.append( 0 );
            log.debug( "Exception caught in javascriptText():", e);
        }
        buffer.append( ";\n" );
        buffer.append( "editor.config.viewPoints = " );
        // Check to see if the current user is authorized to view or modify
        // the deductions
        buffer.append( isGrading() );
        buffer.append( ";\n" );
        buffer.append( "}\n</script>\n" );
        return buffer.toString();
    }


    // ----------------------------------------------------------
    public void setJavascriptText( String text )
    {
        // What?  This is just to satisfy WO, since we never need
        // to set this field meaningfully
    }


    // ----------------------------------------------------------
    private static Element getElementById( Element current, String id )
    {
        Element result = null;
        if ( current != null )
        {
            String thisId = current.getAttributeValue( "id" );
            if ( thisId != null &&
                 thisId.equals( id ) )
            {
                result = current;
            }
            else
            {
                Iterator iterator = current.getChildren().iterator();
                while ( iterator.hasNext() )
                {
                    Element e = getElementById( (Element)iterator.next(), id );
                    if ( e != null )
                    {
                        result = e;
                        break;
                    }
                }
            }
        }
        return result;
    }


    //~ Instance/static variables .............................................

    private double projectTADeduction          = 0.0;
    private double toolTestingOtherFiles       = 0.0;
    private double projectToolTestingDeduction = 0.0;
    private String codeWithComments            = null;
    private String codeWithCommentsToStore     = null;

    static Logger log = Logger.getLogger( EditFileCommentsPage.class );
}
