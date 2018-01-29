/*==========================================================================*\
 |  $Id: EditFileCommentsPage.java,v 1.7 2011/10/25 15:32:06 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2010 Virginia Tech
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

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.webcat.core.*;
import org.webcat.grader.messaging.GraderMarkupParseError;
import org.webcat.woextensions.WCResourceManager;
import com.webobjects.appserver.WODisplayGroup;

// -------------------------------------------------------------------------
/**
 * This class presents the details for one file in a submission,
 * including all markup comments and a color-highlighted version
 * of the source code.
 *
 * @author  Stephen Edwards, Hussein Vastani
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.7 $, $Date: 2011/10/25 15:32:06 $
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
    public EditFileCommentsPage(WOContext context)
    {
        super(context);
    }


    //~ KVC attributes (must be public) .......................................

    public SubmissionFileStats fileStats;

    public WODisplayGroup      filesDisplayGroup;
    public SubmissionFileStats selectedFile;
    public SubmissionFileStats file;

    public NSArray<Byte> formats = SubmissionResult.formats;
    public byte aFormat;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Adds to the response of the page
     *
     * @param response The response being built
     * @param context  The context of the request
     */
    protected void beforeAppendToResponse(
        WOResponse response, WOContext context)
    {
        if (fileStats == null)
        {
            fileStats = prefs().submissionFileStats();
        }
        if (log.isDebugEnabled())
        {
            log.debug("beginning appendToResponse()");
            SubmissionResult result = fileStats.submissionResult();
            log.debug("result = " + result);
            if (result != null)
            {
                log.debug("result = " + result.hashCode());
                log.debug("result EC = " + result.editingContext().hashCode());
            }
        }
        initializeCodeWithComments();

        selectedFile = null;

        filesDisplayGroup.setObjectArray(
                fileStats.submissionResult().submissionFileStats());

        priorOverallComments = fileStats.submissionResult().comments();
        super.beforeAppendToResponse(response, context);
    }


    // ----------------------------------------------------------
    protected void afterAppendToResponse(
        WOResponse response, WOContext context)
    {
        super.afterAppendToResponse(response, context);
        codeWithComments = null;
        log.debug("ending appendToResponse()");
    }


    // ----------------------------------------------------------
    public WOComponent saveChanges()
    {
        try
        {
            updateTAScore(storeComments());
            fileStats.submissionResult().addCommentByLineFor(
                user(), priorOverallComments);
            if (fileStats.submissionResult().changedProperties().size() > 0)
            {
                fileStats.submissionResult().setLastUpdated(new NSTimestamp());
            }
            applyLocalChanges();
        }
        catch (Exception e)
        {
            // This is thrown by an XML parse error in storeComments(),
            // so use it to avoid updating the TA Score
            error("An error occurred while reading your comments.  "
                + "They could not be saved successfully.  The situation has "
                + "been reported to the administrator.");
        }

        return hasMessages() ? null : goToSelectedDestination();
    }


    // ----------------------------------------------------------
    public WOComponent goToSelectedDestination()
    {
        if (selectedFile == null)
        {
            return super.next();
        }
        else
        {
            prefs().setSubmissionFileStatsRelationship(selectedFile);
            WCComponent statsPage = pageWithName(EditFileCommentsPage.class);
            statsPage.nextPage = this.nextPage;
            return statsPage;
        }
    }


    // ----------------------------------------------------------
    public void updateTAScore(double ptsTakenOff)
    {
        if (ptsTakenOff == 0.0d)
        {
            // do nothing
            return;
        }

        double score = 0.0;
        SubmissionResult result = fileStats.submissionResult();
        Number taScore = result.taScoreRaw();
        Number possible = result.submission().assignmentOffering().assignment()
               .submissionProfile().taPointsRaw();
        log.debug( "updateTAScore()");
        if ( taScore != null)
        {
            //calculate the TA score
            score = taScore.doubleValue();
            log.debug("  TA score = " + score);
            // now subtract the new score, which takes care of the
            // difference in the scores
            score += ptsTakenOff;
            log.debug(" After subtracting new deductions, score now is = "
                + score);
            result.setTaScore(score);
        }
        else
        {
            if (possible != null) // there is a possible value
            {
                score = possible.doubleValue();
                score += ptsTakenOff; // adding the deductions
                result.setTaScore(score);
            }
            else
            {
                result.setTaScore(result.taScore() + ptsTakenOff);
            }
        }
        double oldDeductions = fileStats.deductions();
        log.debug("old deductions = " + oldDeductions);
        fileStats.setDeductions(oldDeductions + ptsTakenOff);
        log.debug("new deductions = " + fileStats.deductions());
        if (result.status() == Status.TO_DO)
        {
            result.setStatus(Status.UNFINISHED);
        }
    }


    // ----------------------------------------------------------
    public boolean isGrading()
    {
        return true;
    }


    /**
     * This function is called when we save the file after adding comments. It
     * will extract all the comments that were added and insert them in the
     * database.
     *
     * @return the cumulative score adjustment of all the comments in the file
     * @throws Exception if an error occurred
     */
    public double storeComments()
        throws Exception
    {
        boolean commentsModified = false;
        double taPts = 0.0;
        EOEditingContext ec = localContext();
        if (codeWithCommentsToStore == null)
        {
            return taPts;
        }
        if (codeWithCommentsToStore.length() < 50
            && codeWithCommentsToStore.trim().equals("<br />"))
        {
            // An error occurred, but it should have already been
            // trapped elsewhere
            throw new Exception(
                "storeComments(): null (<br/>) code returned from client");
        }
        try
        {
            // remove the link statement

            if (log.isDebugEnabled())
            {
                log.debug("before subst:\n---------------------------------");
                log.debug(codeWithCommentsToStore.substring(0, 200));
                log.debug("----------------------------------");
            }
            codeWithCommentsToStore = codeWithCommentsToStore
                .replaceFirst("^\\s*<![^>]*>\\s*", "")
                .replaceFirst("^\\s*<link [^>]*>\\s*", "");
            if (log.isDebugEnabled())
            {
                log.debug("after subst:\n----------------------------------");
                log.debug(codeWithCommentsToStore.substring(0, 200));
                log.debug("----------------------------------");
            }

            SAXBuilder parser = new SAXBuilder();
            // Try parsing the file/string with the parser
            Document doc =
                parser.build(new StringReader(codeWithCommentsToStore));
            Element root = doc.getRootElement();
            @SuppressWarnings("unchecked")
            List<Element> children = root.getChild("tbody").getChildren();

            // Delete existing comments by the current user from the
            // database
            for (SubmissionFileComment thisComment : fileStats.comments())
            {
                 // if its the current users comments
                 if (user() == thisComment.author())
                 {
                     log.debug("Deleting comment, line "
                         + thisComment.lineNo());
                     taPts -= thisComment.deduction();
                     ec.deleteObject(thisComment);
                     commentsModified = true;
                 }
            }
            applyLocalChanges();

            // check all children for comment box (id should have I) and
            // then extract values
            for (Element child : children)
            {
                // get the id attribute from the row
                String id = child.getAttributeValue("id");
                if (id == null)
                {
                    log.error("html tag is missing id attribute:"
                        + fileStats.markupFile().getPath());
                    continue;
                }
                if (   (id.charAt(0) == '\"')
                    && (id.charAt(id.length() - 1) == '\"'))
                {
                    // if there are quotes around the attribute value
                    id = id.substring(1, id.length() - 1);
                }
                String [] idarr = id.split(":");
                if (idarr[0].charAt(0) == 'I')
                {
                    // This is a table tag
                    int    boxnum = Integer.parseInt(
                        idarr[0].substring(1, idarr[0].length()));
                    int    rownum = Integer.parseInt(idarr[1]);
                    int    refnum = Integer.parseInt(idarr[2]);
                    String idpart = "I" + boxnum + ":" + rownum + ":" + refnum;
                    log.debug("Tagged comment block found: " + idpart);

                    Element msg = getElementById(child, idpart + ":M");
                    if (msg != null)
                    {
                        SubmissionFileComment comment =
                            new SubmissionFileComment();
                        ec.insertObject(comment);
                        applyLocalChanges();
                        comment.setAuthorRelationship(user());
                        comment.setSubmissionFileStatsRelationship(fileStats);
                        Element target =
                            getElementById(child, idpart + ":T");
                        if (target != null)
                        {
                            comment.setTo(
                                target.getAttribute("value").getValue());
                            log.debug("  to = " + comment.toNo());
                        }
                        Element category =
                            getElementById(child, idpart + ":C");
                        if (category != null)
                        {
                            comment.setCategory(category.getText());
                            log.debug("  category = " + comment.category());
                        }
                        Element pts = getElementById(child, idpart + ":P");
                        if (pts != null)
                        {
                            String ptVal = pts.getText();
                            int colonIndex = ptVal.indexOf(":");
                            if (colonIndex != -1)
                            {
                                // colon exists, so remove it
                                ptVal = ptVal.substring(
                                    colonIndex + 1, ptVal.length());
                            }
                            ptVal = ptVal.trim();

                            if (!ptVal.equals(""))
                            {
                                try
                                {
                                    double deduction =
                                        Double.parseDouble(ptVal);
                                    taPts += deduction;
                                    comment.setDeduction(deduction);
                                }
                                catch (NumberFormatException e)
                                {
                                    log.error("Double conversion failure on "
                                               + "ptVal argument '"
                                               + ptVal + "'", e);
                                }
                            }
                            else // if no points given , then 0 is by default
                            {
                                comment.setDeduction(0.0);
                            }
                            log.debug("  deduction = "
                                       + comment.deductionRaw());
                        }
                        XMLOutputter outputter = new XMLOutputter();
                        outputter.setOmitDeclaration(true);
                        String newmes = outputter.outputString(msg);
                        newmes = newmes.replaceAll(idpart,"&&&&");
                        comment.setMessage(newmes);
                        log.debug("  message = '" + comment.message() + "'");
                        comment.setLineNo(rownum);
                        log.debug("  line = " + comment.lineNo());
                        applyLocalChanges();
                        log.debug("result = " + comment);
                        commentsModified = true;
                    }
                    else
                    {
                        log.debug("Skipping blank separator TR");
                    }
                }
            }
            if (commentsModified)
            {
                fileStats.submissionResult().setLastUpdated(new NSTimestamp());
                if (fileStats.submissionResult().status() == Status.TO_DO)
                {
                    fileStats.submissionResult().setStatus(Status.UNFINISHED);
                }
            }
            applyLocalChanges();
        }
        catch (Exception e)
        {
            log.error("exception reading comments for "
                + fileStats.markupFile().getPath(), e);

            new GraderMarkupParseError(
                fileStats.submissionResult().submission(),
                GraderMarkupParseError.LOCATION_EDIT_FILE_COMMENTS,
                e,
                context(),
                fileStats.markupFile(),
                "Raw XML (value of codeWithComments):\n"
                + codeWithCommentsToStore).send();

            throw e;
        }
        // Let the raw string be garbage collected, now that we're
        // finished with it
        codeWithCommentsToStore = null;

        log.debug(" Store comments is returning = " + taPts);
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
            codeWithComments = fileStats.codeWithComments(
                user(), isGrading(), context().request());
            if (log.isDebugEnabled())
            {
                log.debug("codeWithComments = "
                    + codeWithComments.substring(0, 200));
            }
        }
        catch (Exception e)
        {
            error("An error occurred while trying to prepare the source code "
                + "view for this file.  The error has been reported to the "
                + "administrator.  Please do not try to edit the comments "
                + "in this file until the situation has been resolved.");
            codeWithComments = null;
        }
    }


    // ----------------------------------------------------------
    public void setCodeWithComments(String code)
    {
        codeWithCommentsToStore = code;
    }


    // ----------------------------------------------------------
    public String javascriptText()
    {
        StringBuffer buffer = new StringBuffer(200);
        buffer.append("<script type=\"text/javascript\" src=\"");
        buffer.append(WCResourceManager.resourceURLFor(
            "htmlarea/codearea.js", "Grader", null, context().request()));
        buffer.append("\"></script>\n");
        buffer.append("<script type=\"text/javascript\" src=\"");
        buffer.append(WCResourceManager.resourceURLFor(
            "htmlarea/htmlarea-lang-en.js", "Grader", null,
            context().request()));
        buffer.append("\"></script>\n");
        buffer.append("<script type=\"text/javascript\" src=\"");
        buffer.append(WCResourceManager.resourceURLFor(
            "htmlarea/dialog.js", "Grader", null,
            context().request()));
        buffer.append("\"></script>\n");

        buffer.append("<script type=\"text/javascript\">\n");
        buffer.append("var editor = null;\nfunction initEditor() {\n");
        buffer.append("editor = new HTMLArea(\"source\");\n");
        {
            String url = WCResourceManager.versionlessResourceURLFor(
                "htmlarea/htmlarea.js", "Grader", null, context().request());
            if (url != null)
            {
                buffer.append("editor.config.editorURL = \"");
                buffer.append(url.substring(0,
                    url.length() - "htmlarea.js".length()));
                buffer.append("\";\n");
            }
            url = WCResourceManager.versionlessResourceURLFor(
                "images/blank.gif", "Core", null, context().request());
            if (url != null)
            {
                buffer.append("editor.config.coreResourceURL = \"");
                buffer.append(url.substring(0,
                    url.length() - "images/blank.gif".length()));
                buffer.append("\";\n");
            }
        }
        buffer.append("editor.generate();\n");
        if (wcSession() != null && user() != null)
        {
            buffer.append("editor.config.userName = \"");
            buffer.append(user().name());
                buffer.append("\";\n");
        }
        buffer.append("editor.config.numComments = ");
        try
        {
            buffer.append(fileStats.comments().count());
        }
        catch (Exception e)
        {
            buffer.append(0);
            log.debug("Exception caught in javascriptText():", e);
        }
        buffer.append(";\n");
        buffer.append("editor.config.viewPoints = ");
        // Check to see if the current user is authorized to view or modify
        // the deductions
        buffer.append(isGrading());
        buffer.append(";\n}\n");
        buffer.append("dojo.addOnLoad(function() { initEditor() });\n");
        buffer.append("</script>\n");
        return buffer.toString();
    }


    // ----------------------------------------------------------
    public void setJavascriptText(String text)
    {
        // What?  This is just to satisfy WO, since we never need
        // to set this field meaningfully
    }


    // ----------------------------------------------------------
    private static Element getElementById(Element current, String id)
    {
        Element result = null;
        if (current != null)
        {
            String thisId = current.getAttributeValue("id");
            if (thisId != null &&
                 thisId.equals(id))
            {
                result = current;
            }
            else
            {
                @SuppressWarnings("unchecked")
                List<Element> children = current.getChildren();
                for (Element child : children)
                {
                    Element e = getElementById(child, id);
                    if (e != null)
                    {
                        result = e;
                        break;
                    }
                }
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    public Byte commentFormat()
    {
        Byte format = SubmissionResult.formats.get(0);
        if (fileStats.submissionResult() != null)
        {
            format = SubmissionResult.formats.get(
                fileStats.submissionResult().commentFormat());
        }
        return format;
    }


    // ----------------------------------------------------------
    public void setCommentFormat(Byte format)
    {
        if (format != null && fileStats.submissionResult() != null)
        {
            fileStats.submissionResult().setCommentFormat(format.byteValue());
        }
    }


    // ----------------------------------------------------------
    public String formatLabel()
    {
        return SubmissionResult.formatStrings.objectAtIndex(aFormat);
    }


    //~ Instance/static variables .............................................

    private String codeWithComments            = null;
    private String codeWithCommentsToStore     = null;
    private String priorOverallComments        = null;

    static Logger log = Logger.getLogger(EditFileCommentsPage.class);
}
