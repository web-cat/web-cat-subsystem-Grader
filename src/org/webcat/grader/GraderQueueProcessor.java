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

package org.webcat.grader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.webcat.core.Application;
import org.webcat.core.FileUtilities;
import org.webcat.core.MutableDictionary;
import org.webcat.core.WCProperties;
import org.webcat.grader.messaging.AdminReportsForSubmissionMessage;
import org.webcat.grader.messaging.GraderKilledMessage;
import org.webcat.grader.messaging.SubmissionSuspendedMessage;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;
import er.extensions.appserver.ERXApplication;
import er.extensions.eof.ERXConstant;

// -------------------------------------------------------------------------
/**
 * This is the main grader processor class that performs the
 * compile/reference execution/execute/grade cycle on a student submission
 * job.
 *
 * @author Amit Kulkarni
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class GraderQueueProcessor
    extends Thread
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor
     *
     * @param queue the queue to operate on
     */
    public GraderQueueProcessor( GraderQueue queue )
    {
        this.queue = queue;
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * The actual thread of execution
     */
    public void run()
    {
        // Find all jobs that are not paused
        NSMutableArray newJobQualifiers = new NSMutableArray();
        newJobQualifiers.addObject( new EOKeyValueQualifier(
                        EnqueuedJob.PAUSED_KEY,
                        EOQualifier.QualifierOperatorEqual,
                        ERXConstant.integerForInt( 0 ) ) );
        newJobQualifiers.addObject( new EOKeyValueQualifier(
                        EnqueuedJob.REGRADING_KEY,
                        EOQualifier.QualifierOperatorEqual,
                        ERXConstant.integerForInt( 0 ) ) );
        NSMutableArray regradingJobQualifiers = new NSMutableArray();
        regradingJobQualifiers.addObject( newJobQualifiers.objectAtIndex( 0 ) );
        regradingJobQualifiers.addObject( new EOKeyValueQualifier(
                        EnqueuedJob.REGRADING_KEY,
                        EOQualifier.QualifierOperatorEqual,
                        ERXConstant.integerForInt( 1 ) ) );
        EOFetchSpecification fetchNewJobs =
                new EOFetchSpecification(
                        EnqueuedJob.ENTITY_NAME,
                        new EOAndQualifier( newJobQualifiers ),
                        new NSArray( new Object[]{
                                new EOSortOrdering(
                                        EnqueuedJob.SUBMIT_TIME_KEY,
                                        EOSortOrdering.CompareAscending
                                    )
                            } )
                    );
        EOFetchSpecification fetchRegradingJobs =
            new EOFetchSpecification(
                    EnqueuedJob.ENTITY_NAME,
                    new EOAndQualifier( regradingJobQualifiers ),
                    new NSArray( new Object[]{
                            new EOSortOrdering(
                                    EnqueuedJob.QUEUE_TIME_KEY,
                                    EOSortOrdering.CompareAscending
                                )
                        } )
                );
        EOFetchSpecification fetchDiscardedJobs =
            new EOFetchSpecification(
                    EnqueuedJob.ENTITY_NAME,
                    new EOKeyValueQualifier(
                                    EnqueuedJob.DISCARDED_KEY,
                                    EOQualifier.QualifierOperatorEqual,
                                    ERXConstant.integerForInt( 1 ) ),
                    null
                );
        try
        {
            while ( true )
            {
                if ( editingContext != null )
                {
                    editingContext.unlock();
                    Application.releasePeerEditingContext( editingContext );
                }
                editingContext = Application.newPeerEditingContext();
                editingContext.lock();

                // Clear discarded jobs
                NSArray<EnqueuedJob> jobList = null;
                try
                {
                    jobList = EnqueuedJob.objectsWithFetchSpecification(
                        editingContext, fetchDiscardedJobs);
                }
                catch ( Exception e )
                {
                    log.info( "error fetching jobs: ", e );
                    jobList = EnqueuedJob.objectsWithFetchSpecification(
                        editingContext, fetchDiscardedJobs);
                }

                if ( jobList != null )
                {
                    // delete all the discarded jobs
                    for ( int i = 0; i < jobList.count(); i++ )
                    {
                        EnqueuedJob job = jobList.objectAtIndex( i );
                        editingContext.deleteObject( job );
                    }
                    editingContext.saveChanges();
                    log.debug( "" + jobList.count()
                        + " discarded jobs retrieved" );
                }

                // Get a job
                log.debug( "waiting for a token" );
                // We don't need the return value, since it is just null:
                queue.getJobToken();
                log.debug( "token received." );

                jobList = null;
                try
                {
                    jobList = EnqueuedJob.objectsWithFetchSpecification(
                        editingContext, fetchNewJobs);
                }
                catch ( Exception e )
                {
                    log.info( "error fetching jobs: ", e );
                    jobList = EnqueuedJob.objectsWithFetchSpecification(
                        editingContext, fetchNewJobs);
                }

                if ( log.isDebugEnabled() )
                {
                    log.debug( ""
                               + ( jobList == null
                                   ? "<null>"
                                   : "" + jobList.count() )
                               + " fresh jobs retrieved" );
                }
                if ( jobList == null || jobList.count() == 0 )
                {
                    try
                    {
                        jobList = EnqueuedJob.objectsWithFetchSpecification(
                            editingContext, fetchRegradingJobs);
                    }
                    catch ( Exception e )
                    {
                        log.info( "error fetching jobs: ", e );
                        jobList = EnqueuedJob.objectsWithFetchSpecification(
                            editingContext, fetchRegradingJobs);
                    }
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( ""
                                   + ( jobList == null
                                       ? "<null>"
                                       : "" + jobList.count() )
                                   + " regrading jobs retrieved" );
                    }
                }

                // This test is just to make sure the compiler knows it
                // isn't null, even though the try/catch above ensures it
                if ( jobList != null )
                {
                    for (EnqueuedJob job : jobList)
                    {
                        NSTimestamp startProcessing = new NSTimestamp();
                        Submission submission = job.submission();
                        if ( submission == null )
                        {
                            log.error( "null submission in enqueued job: "
                                       + "deleting" );
                            editingContext.deleteObject( job );
                        }
                        else if ( job.discarded() )
                        {
                            log.debug("discarded job: deleting");
                            editingContext.deleteObject( job );
                        }
                        else if ( submission.assignmentOffering() == null )
                        {
                            log.error( "submission with null assignment "
                                       + "offering in enqueued job: deleting" );
                            editingContext.deleteObject( job );
                        }
                        else
                        {
                            if ( submission.assignmentOffering()
                                     .gradingSuspended() )
                            {
                                log.warn( "Suspending job "
                                          + submission.dirName() );
                                job.setPaused( true );
                            }
                            else
                            {
                                log.info( "processing submission "
                                           + submission.dirName() );
                                processJobWithProtection( job );
                                NSTimestamp now = new NSTimestamp();
                                if ( job.queueTime() != null )
                                {
                                    mostRecentJobWait = now.getTime()
                                        - job.queueTime().getTime();
                                }
                                else
                                {
                                    mostRecentJobWait = now.getTime()
                                    - submission.submitTime().getTime();
                                }
                                {
                                    long processingTime = now.getTime() -
                                       startProcessing.getTime();
                                    totalWaitForJobs += processingTime;
                                    jobsCountedWithWaits++;
                                }
                            }
                        }

                        // Now save all the changes
                        {
                            // assignment offering could have changed because
                            // of a fault, so save any changes before
                            // forcing it out of editing context cache
                            try
                            {
                                editingContext.saveChanges();
                                editingContext.refaultAllObjects();
                            }
                            catch (IllegalStateException e)
                            {
                                // Database inconsistency problem
                                try
                                {
                                    editingContext.revert();
                                    editingContext.unlock();
                                    Application.releasePeerEditingContext(
                                        editingContext);
                                }
                                catch (Exception ee)
                                {
                                    log.error("Exception trying to save "
                                        + "grading results (retrying)", ee);
                                }
                                editingContext = null;
                                queue.enqueue( null );
                                break;
                            }
                        }
                        // Only process one regrading job before looking for
                        // more regular submissions.
                        if ( job.regrading() )
                        {
                            queue.enqueue( null );
                            break;
                        }
                    }
                }
            }
        }
        catch ( Exception e )
        {
            log.fatal( "Job queue processing halted.\n"
                       + "Exception processing student submission",
                       e );

            new GraderKilledMessage(e).send();

/*            Application.emailExceptionToAdmins(
                    e,
                    null,
                    "Job queue processing halted."
                );*/

            log.fatal( "Aborting: job queue processing halted." );
            ERXApplication.erxApplication().killInstance();
        }
    }


    // ----------------------------------------------------------
    /**
     * This function processes the job and performs the stages that
     * are necessary.  It guards against any exceptions while
     * processing the job.
     *
     * @param job the job to process
     */
    void processJobWithProtection( EnqueuedJob job )
    {
        try
        {
            processJob( job );
        }
        catch ( Exception e )
        {
            technicalFault( job,
                            "while processing job",
                            e,
                            null );
        }
    }


    // ----------------------------------------------------------
    /**
     * This function processes the job and performs the stages that
     * are necessary.
     *
     * @param job the job to process
     */
    void processJob( EnqueuedJob job )
    {
        // boolean status            = false;
        // String  extendedErrorInfo = null;
        double scoreAdjustments = 0.0;
        double correctnessScore = 0.0;
        double toolScore        = 0.0;

        jobCount++;
        log.info( "Processing job " + jobCount + " for: "
                  + job.submission().user().userName() );

        // Set up the working directory first
        try
        {
            prepareWorkingDirectory( job );
        }
        catch ( Exception e )
        {
            technicalFault( job,
                            "while preparing the working directory",
                            e,
                            null );
            return;
        }

        // Fetch all the steps in grading this assignment
        NSArray<Step> steps = Step.objectsWithFetchSpecification(
            editingContext,
            new EOFetchSpecification(
                "Step",
                new EOKeyValueQualifier(
                    "assignment",
                    EOQualifier.QualifierOperatorEqual,
                    job.submission().assignmentOffering()
                        .assignment()),
                new NSArray( new Object[]{
                    new EOSortOrdering(
                        "order",
                        EOSortOrdering.CompareAscending)
                    })
            ));

        // Set up the properties to pass to execution scripts
        WCProperties gradingProperties;
        File gradingPropertiesFile = job.submission().gradingPropertiesFile();

        try
        {
            gradingProperties =
                job.submission().createInitialGradingPropertiesFile();
        }
        catch (IOException e)
        {
            technicalFault( job,
                    "could not create the initial grading.properties file: "
                    + gradingPropertiesFile.getAbsolutePath() + ": "
                    + e.getMessage(),
                    null,
                    gradingPropertiesFile.getParentFile());
            return;
        }

        writeOutSavedGradingProperties(job, gradingProperties);

        for ( int stepNo = 0; stepNo < steps.count(); stepNo++ )
        {
            Step thisStep = steps.objectAtIndex( stepNo );

            executeStep( job,
                         thisStep,
                         gradingProperties,
                         gradingPropertiesFile );

            if ( faultOccurredInStep )
            {
                // technicalFault was already called by executeStep()
                // to pause the assignment and send e-mail to admins,
                // so just bail
                return;
            }

            // check the properties to update score and halt, if necessary
            if ( gradingProperties.getProperty( "score.adjustment" ) != null )
            {
                scoreAdjustments +=
                    gradingProperties.doubleForKey( "score.adjustment" );
                gradingProperties.remove( "score.adjustment" );
            }
            if ( gradingProperties.getProperty( "score.correctness" ) != null )
            {
                correctnessScore =
                    gradingProperties.doubleForKey( "score.correctness" );
//                gradingProperties.remove( "score.correctness" );
            }
            if ( gradingProperties.getProperty( "score.tools" ) != null )
            {
                toolScore = gradingProperties.doubleForKey( "score.tools" );
//                gradingProperties.remove( "score.tools" );
            }
            if ( gradingProperties.getProperty( "halt" ) != null )
            {
                if ( gradingProperties.booleanForKey( "halt" ) )
                {
                  gradingProperties.remove( "halt" );
                  log.error( "halt requested in step " + thisStep
                             + "\n\tfor job " + job );
                  job.setPaused( true );
                  return;
                }
            }
            if ( gradingProperties.getProperty( "canProceed" ) != null )
            {
                if ( !gradingProperties.booleanForKey( "canProceed" ) )
                {
                    break;
                }
            }
            if ( gradingProperties.getProperty( "halt.all" ) != null )
            {
                if ( gradingProperties.booleanForKey( "halt.all" ) )
                {
                  gradingProperties.remove( "halt.all" );
                  log.error( "halt requested for all jobs in step " + thisStep
                             + "\n\tfor job " + job );
                  job.setPaused( true );
                  AssignmentOffering assignment =
                      job.submission().assignmentOffering();
                  assignment.setGradingSuspended( true );
                  return;
                }
            }

            if ( timeoutOccurredInStep )
            {
                technicalFault( job,
                                "script time limit exceeded in stage "
                                + ( stepNo + 1 ),
                                null,
                                gradingPropertiesFile.getParentFile() );
                return;
            }
        }
        // Clean up the working directory
        FileUtilities.deleteDirectory( job.workingDirName() );

        generateFinalReport( job,
                             gradingProperties,
                             correctnessScore,
                             toolScore );
    }


    // ----------------------------------------------------------
    /**
     * Creates and cleans the working directory, if necessary, fills
     * it with the student's submission, and creates the reporting
     * directory.
     *
     * @param job the job to operate on
     * @throws Exception if it occurs during this stage
     */
    private void prepareWorkingDirectory( EnqueuedJob job )
        throws java.io.IOException
    {
        // Create the working compilation directory for the user
        File workingDir = new File( job.workingDirName() );
        if ( workingDir.exists() )
        {
            FileUtilities.deleteDirectory( workingDir );
        }
        workingDir.mkdirs();

        // Copy the user's submission to the working dir
        Submission submission = job.submission();
//        if ( submission.fileIsArchive() )
//        {
//            Grader.unZip( submission.file(), workingDir );
//        }
//        else
//        {
//            Grader.copyFile( submission.file(), workingDir );
//        }
        org.webcat.archives.ArchiveManager.getInstance()
            .unpack( workingDir, submission.file() );

        // Create the grading output directory
        File graderLD = new File( submission.resultDirName() );
        if ( graderLD.exists() )
        {
            FileUtilities.deleteDirectory( graderLD );
        }
        graderLD.mkdirs();
    }


    // ----------------------------------------------------------
    /**
     * Execute a single step in the grading process.  Communication
     * between the Grader subsystem and the script being executed for
     * this step is accomplished by using a properties file.  The set
     * of properties to communicate and the file to pass to store them
     * in (and place as the script's command line arg) are passed in
     * to this method.  This method will set the faultOccurredInStep
     * data member if an internal failure occurred in processing this
     * step.  Alternatively, it will set the timeoutOccurredInStep data
     * member if the time limit was exceeded for this step.
     *
     * @param job            the job being processed
     * @param step           the grading step to execute
     * @param properties     the cumulative properties settings to use
     * @param propertiesFile the file to record the properties in
     */
    //     * @throws IOException if one occurs
    void executeStep( EnqueuedJob  job,
                      Step         step,
                      WCProperties properties,
                      File         propertiesFile )
    {
        faultOccurredInStep = false;
        timeoutOccurredInStep = false;
        log.debug( "step " + step.order() + ": "
                   + step.gradingPlugin().mainFilePath() );

        try
        {
            step.gradingPlugin().reinitializeConfigAttributesIfNecessary();
            log.debug( "creating properties file" );
            // Re-write the properties file
            properties.addPropertiesFromDictionaryIfNotDefined(
                ( (Application)Application.application() )
                    .subsystemManager().pluginProperties()
                );
            properties.addPropertiesFromDictionaryIfNotDefined(
                step.gradingPlugin().globalConfigSettings() );
            properties.addPropertiesFromDictionaryIfNotDefined(
                step.gradingPlugin().defaultConfigSettings() );
            if ( step.config() != null )
            {
                properties.addPropertiesFromDictionary(
                    step.config().configSettings() );
            }
            properties.addPropertiesFromDictionary(
                step.configSettings() );
            properties.setProperty( "userName",
                                    job.submission().user().userName() );
            properties.setProperty( "workingDir",
                                    job.workingDirName() );
            properties.setProperty( "resultDir",
                                    job.submission().resultDirName() );
            properties.setProperty( "scriptHome",
                                    step.gradingPlugin().dirName() );
            properties.setProperty( "scriptData",
                            GradingPlugin.scriptDataRoot() );
            properties.setProperty( "timeout",
                                    Integer.toString(
                                        step.effectiveEndToEndTimeout() ) );
            properties.setProperty( "timeoutForOneRun",
                            Integer.toString(
                                step.effectiveTimeoutForOneRun() ) );
            properties.setProperty( "course",
                job.submission().assignmentOffering().courseOffering()
                .course().deptNumber() );
            {
                String crn = job.submission().assignmentOffering()
                    .courseOffering().crn();
                properties.setProperty( "CRN",
                    ( crn == null ) ? "null" : crn
                );
            }
            properties.setProperty( "assignment",
                job.submission().assignmentOffering().assignment()
                .name() );
            properties.setProperty( "dueDateTimestamp",
                Long.toString( job.submission().assignmentOffering()
                               .dueDate().getTime() ) );
            properties.setProperty( "submissionTimestamp",
                Long.toString( job.submission().submitTime().getTime() ) );
            properties.setProperty( "submissionNo",
                Integer.toString( job.submission().submitNumber() ) );
            properties.setProperty( "frameworksBaseURL",
                Application.application().frameworksBaseURL() );

            BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream( propertiesFile ) );
            properties.store(
                out, "Web-CAT grader script configuration properties" );
            out.close();

            File stdout = new File( job.submission().resultDirName(),
                                    "" + step.order() + "-stdout.txt" );
            File stderr = new File( job.submission().resultDirName(),
                                    "" + step.order() + "-stderr.txt" );

            // execute the script
            log.debug( "executing script" );
            timeoutOccurredInStep =
                step.execute(
                    propertiesFile.getPath(),
                    new File( job.workingDirName() ),
                    stdout,
                    stderr );

            if ( stderr.length() != 0 )
            {
                technicalFault( job,
                                "stderr output was produced by " + step,
                                null,
                                propertiesFile.getParentFile() );
                return;
            }
            else
            {
                stderr.delete();
            }
            if ( stdout.length() == 0 )
            {
                stdout.delete();
            }
            else
            {
                log.warn( "Script produced stdout output in "
                          + stdout.getPath() );
            }

            // Now reload the properties file
            log.debug( "re-loading properties from file" );
            BufferedInputStream in = new BufferedInputStream(
                new FileInputStream( propertiesFile ) );
            properties.clear();
            properties.load( in );
            in.close();
            // log.debug( "properties:\n" + properties );
        }
        catch ( Exception e )
        {
            technicalFault( job,
                            "in stage " + step,
                            e,
                            propertiesFile.getParentFile() );
        }
    }


    // ----------------------------------------------------------
    /**
     * Collects all the reports in the given properties file, splitting
     * them into those that are inline, those that are downloadable,
     * and those to send to the administrator.
     *
     * @param job              the job
     * @param properties       the properties describing the reports
     * @param submissionResult the result to link downloadable reports to
     * @param inlineStudentReports    the array where inline report files are
     *                         added (as InlineFile objects)
     * @param inlineStudentReports    the array where inline report files
     *                         intended for course staff are added (as
     *                         InlineFile objects)
     * @param adminReports     the Vector where admin-targeted report files
     *                         are added (as string file names)
     */
    void collectReports( EnqueuedJob      job,
                         WCProperties     properties,
                         SubmissionResult submissionResult,
                         NSMutableArray   inlineStudentReports,
                         NSMutableArray   inlineStaffReports,
                         Vector           adminReports )
    {
        File parentDir = new File( job.submission().resultDirName() );

        // First, collect all the report fragments
        int numReports = properties.intForKey( "numReports" );
        for ( int i = 1; i <= numReports; i++ )
        {
            // First, extract the attributes
            String attributeBase = "report" + i + ".";
            String fileName = properties.getProperty( attributeBase + "file" );
            String mimeType =
                properties.getProperty( attributeBase + "mimeType" );
            boolean inline =
                ( ( properties.getProperty( attributeBase + "inline" )
                    == null )
                  ? true
                  : properties.booleanForKey( attributeBase + "inline" ) );
            boolean border =
                properties.booleanForKey( attributeBase + "border" );
            String to = properties.getProperty( attributeBase + "to" );
            boolean toStudent =
                to == null
                || to.equalsIgnoreCase( "student" )
                || to.equalsIgnoreCase( "both" )
                || to.equalsIgnoreCase( "all" );
            boolean toStaff =
                to != null
                && (    to.equalsIgnoreCase( "staff" )
                     || to.equalsIgnoreCase( "instructor" )
                     || to.equalsIgnoreCase( "both" )
                     || to.equalsIgnoreCase( "all" ) );
            boolean toAdmin =
                to != null
                && (    to.equalsIgnoreCase( "admin" )
                     || to.equalsIgnoreCase( "administrator" )
                     || to.equalsIgnoreCase( "all" ) );
            // Now, populate the lists
            if ( toStudent )
            {
                if ( inline )
                {
                    inlineStudentReports.addObject(
                        new InlineFile( parentDir,
                                        fileName,
                                        mimeType,
                                        border ) );
                }
                else
                {
                    ResultFile thisFile = new ResultFile();
                    editingContext.insertObject( thisFile );
                    thisFile.setFileName( fileName );
                    thisFile.setLabel(
                        properties.getProperty( attributeBase + "label" ) );
                    thisFile.setMimeType( mimeType );
                    thisFile.setSubmissionResultRelationship(
                        submissionResult );
                }
            }
            if ( toStaff )
            {
                if ( inline )
                {
                    inlineStaffReports.addObject(
                        new InlineFile( parentDir,
                                        fileName,
                                        mimeType,
                                        border ) );
                }
                else
                {
                    // FIXME!
//                    ResultFile thisFile = new ResultFile();
//                    editingContext.insertObject( thisFile );
//                    thisFile.setFileName( fileName );
//                    thisFile.setLabel(
//                        properties.getProperty( attributeBase + "label" ) );
//                    thisFile.setMimeType( mimeType );
//                    thisFile.setSubmissionStaffResultRelationship(
//                        submissionResult );
                }
            }
            if ( toAdmin )
            {
                adminReports.add( job.submission().resultDirName() +
                                  "/" + fileName );
            }
        }

        // Second, collect all the stats markup files
        String statElementsLabel =
            properties.getProperty( "statElementsLabel" );
        if ( statElementsLabel != null )
        {
            submissionResult.setStatElementsLabel( statElementsLabel );
        }
        numReports = properties.intForKey( "numCodeMarkups" );
        for ( int i = 1; i <= numReports; i++ )
        {
            String attributeBase = "codeMarkup" + i + ".";
            SubmissionFileStats stats = new SubmissionFileStats();
            editingContext.insertObject( stats );
            stats.setClassName(
                properties.getProperty( attributeBase + "className" ) );
            stats.setPkgName(
                properties.getProperty( attributeBase + "pkgName" ) );
            stats.setSourceFileNameRaw(
                properties.getProperty( attributeBase + "sourceFileName" ) );
            stats.setMarkupFileNameRaw(
                properties.getProperty( attributeBase + "markupFileName" ) );

            // The tags are zero or more space-delimited strings that describe
            // what this file's role is (such as if it is a test case). Note
            // that we pad the tag string with a space on each end if necessary
            // so that tags can always be searched for in the database using a
            // LIKE qualifier such as "% tag %". This way tags that are infixes
            // of other tags will not be erroneously detected.

            String tags = properties.getProperty( attributeBase + "tags" );
            if (tags != null)
            {
                if (tags.length() == 0)
                {
                    tags = null;
                }
                else
                {
                    if (!tags.startsWith(" "))
                    {
                        tags = " " + tags;
                    }
                    if (!tags.endsWith(" "))
                    {
                        tags = tags + " ";
                    }
                }
            }
            stats.setTags(tags);

            String attr = properties.getProperty( attributeBase + "loc" );
            if ( attr != null )
            {
                stats.setLocRaw( ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase + "ncloc" );
            if ( attr != null )
            {
                stats.setNclocRaw( ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase + "deductions" );
            if ( attr != null )
            {
                stats.setDeductionsRaw( new Double( attr ) );
            }
            attr = properties.getProperty( attributeBase + "remarks" );
            if ( attr != null )
            {
                stats.setRemarksRaw( ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase + "conditionals" );
            if ( attr != null )
            {
                stats.setConditionalsRaw(
                    ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase
                                           + "conditionalsCovered" );
            if ( attr != null )
            {
                stats.setConditionalsCoveredRaw(
                    ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase + "statements" );
            if ( attr != null )
            {
                stats.setStatementsRaw( ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase
                                           + "statementsCovered" );
            if ( attr != null )
            {
                stats.setStatementsCoveredRaw(
                    ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase + "methods" );
            if ( attr != null )
            {
                stats.setMethodsRaw( ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase
                                           + "methodsCovered" );
            if ( attr != null )
            {
                stats.setMethodsCoveredRaw(
                    ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase + "elements" );
            if ( attr != null )
            {
                stats.setElementsRaw( ERXConstant.integerForString( attr ) );
            }
            attr = properties.getProperty( attributeBase
                                           + "elementsCovered" );
            if ( attr != null )
            {
                stats.setElementsCoveredRaw(
                    ERXConstant.integerForString( attr ) );
            }
            stats.setSubmissionResultRelationship( submissionResult );
        }
    }


    // ----------------------------------------------------------
    protected class InlineFile
        extends File
    {
        public InlineFile( File    parent,
                           String  child,
                           String  type,
                           boolean useBorder )
        {
            super( parent, child );
            mimeType = type;
            border   = useBorder;
        }

        public String mimeType = null;
        public boolean border  = false;
    }


    // ----------------------------------------------------------
    /**
     * Generates the final report, records the submission results,
     * and deletes the job.
     *
     * @param job the finished job
     */
    void generateFinalReport( EnqueuedJob  job,
                              WCProperties properties,
                              double       correctnessScore,
                              double       toolScore )
    {
        SubmissionResult submissionResult = new SubmissionResult();
//      if ( rawScore >= 0.0 )
//      {
//          submissionResult.setRawScore( rawScore );
//      }
        submissionResult.setCorrectnessScore( correctnessScore );
        submissionResult.setToolScore( toolScore );
        editingContext.insertObject( submissionResult );

        NSMutableArray inlineStudentReports = new NSMutableArray();
        NSMutableArray inlineStaffReports = new NSMutableArray();
        Vector         adminReports  = new Vector();
        collectReports( job,
                        properties,
                        submissionResult,
                        inlineStudentReports,
                        inlineStaffReports,
                        adminReports );

        generateCompositeResultFile(
            new File( job.submission().resultDirName(),
                      SubmissionResult.resultFileName() ),
            inlineStudentReports );
        generateCompositeResultFile(
            new File( job.submission().resultDirName(),
                      SubmissionResult.staffResultFileName() ),
            inlineStaffReports );

        // 2009-02-04 (AJA): create result blobs
        processSavedProperties( job, submissionResult, properties );

        editingContext.saveChanges();
        boolean wasRegraded = job.regrading();
        submissionResult.addToSubmissionsRelationship( job.submission() );
        job.setSubmissionRelationship( null );
        editingContext.deleteObject( job );
        editingContext.saveChanges();

        // The following line self-commits any changes it makes
        submissionResult.setAsMostRecentIfNecessary();

        // Send out e-mail messages to student
        //
        NSTimestamp limitTime = submissionResult.submission().submitTime()
            .timestampByAddingGregorianUnits(
                    0,  // years
                    0,  // months
                    0,  // days
                    0,  // hours
                    emailWaitMinutes,
                    0   // seconds
                );
        if ( limitTime.before( new NSTimestamp() ) ) // compare against now
        {
            String msg = "is now available";
            if ( wasRegraded )
            {
                msg += ".\nA course staff member requested that it be "
                    + "regraded";
            }
            submissionResult.submission().emailNotificationToStudent( msg );
        }

        // Send out admin reports, if any
        //
        if ( adminReports.size() > 0 )
        {
            Submission submission = submissionResult.submission();

            new AdminReportsForSubmissionMessage(submission, adminReports)
                .send();

/*            AssignmentOffering assignment = submission.assignmentOffering();
            Application.sendAdminEmail(
                null,
                submission.assignmentOffering().courseOffering()
                    .instructors(),
                true,
                "[Grader] reports: "
                + submission.user().email() + " #"
                + submission.submitNumber()
                + ( assignment == null
                    ? ""
                    : ( ", " + assignment.titleString() ) ),
                "Reports addressed to the adminstrator are attached.\n",
                adminReports );*/
        }
    }


    // ----------------------------------------------------------
    private void writeOutSavedGradingProperties(EnqueuedJob job,
                                                WCProperties gradingProperties)
    {
        MutableDictionary accumulatedValues = mostRecentAccumulatedValues(job);
        NSDictionary<String, Object> previousValues =
            previousSubmissionSavedProperties(job);

        for (String key : (NSArray<String>) accumulatedValues.allKeys())
        {
            Object value = accumulatedValues.objectForKey(key);
            gradingProperties.setObjectForKey(value, "mostRecent." + key);
        }

        for (String key : previousValues.allKeys())
        {
            Object value = previousValues.objectForKey(key);
            gradingProperties.setObjectForKey(value, "previous." + key);
        }
    }


    // ----------------------------------------------------------
    /**
     * Build a saved property dictionary from the result outcomes for the
     * specified submission result.
     *
     * @param submissionResult the submission result
     */
    private NSDictionary<String, Object> previousSubmissionSavedProperties(
            EnqueuedJob job)
    {
        NSMutableDictionary<String, Object> props =
            new NSMutableDictionary<String, Object>();

        Submission submission = job.submission().previousSubmission();
        SubmissionResult submissionResult = null;
        if (submission != null)
        {
            submissionResult = submission.result();
        }

        if (submissionResult != null)
        {
            for (ResultOutcome outcome : submissionResult.resultOutcomes())
            {
                String key = outcome.tag();
                MutableDictionary contents = outcome.contents();
                Integer index = outcome.index();

                Object value;
                if (contents != null && contents.count() == 1
                        && contents.objectForKey("value") != null)
                {
                    value = contents.objectForKey("value");
                }
                else
                {
                    value = contents;
                }

                if (index == null)
                {
                    props.setObjectForKey(value, key);
                }
                else
                {
                    NSMutableArray<Object> array =
                        (NSMutableArray<Object>) props.objectForKey(key);

                    if (array == null)
                    {
                        array = new NSMutableArray<Object>();
                        props.setObjectForKey(array, key);
                    }

                    growArrayUpToIndex(array, index);
                    array.replaceObjectAtIndex(value, index);
                }
            }
        }

        return props;
    }


    // ----------------------------------------------------------
    /**
     * Grows the specified array until an item at the specified index can be
     * set, inserting null values in the gaps.
     *
     * @param array the array
     * @param index the index to grow up to
     */
    private void growArrayUpToIndex(NSMutableArray<?> array, int index)
    {
        while (array.count() <= index)
        {
            array.addObject(NSKeyValueCoding.NullValue);
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets the most recent accumulated values for the submission chain
     * associated with this job.
     *
     * @param job the grading job
     * @return the most recent accumulated values
     */
    private MutableDictionary mostRecentAccumulatedValues(EnqueuedJob job)
    {
        MutableDictionary accumulatedValues = null;

        // Get the previous submission that has a result object so that we can
        // get the accumulated values dictionary from it.
        Submission prevSub = job.submission().previousSubmission();
        while (prevSub != null && prevSub.result() == null)
        {
            prevSub = prevSub.previousSubmission();
        }

        if (prevSub != null)
        {
            SubmissionResult prevResult = prevSub.result();

            if (prevResult != null)
            {
                accumulatedValues = new MutableDictionary(
                    prevResult.accumulatedSavedProperties());
            }
        }

        if (accumulatedValues == null)
        {
            accumulatedValues = new MutableDictionary();
        }

        return accumulatedValues;
    }


    // ----------------------------------------------------------
    /**
     * Create result outcome objects from properties in the grading properties
     * file.
     *
     * @param job
     * @param submissionResult
     * @param properties
     */
    private void processSavedProperties( EnqueuedJob job,
                                         SubmissionResult submissionResult,
                                         WCProperties properties )
    {
        // Get the previous result with any data so that we can merge in these
        // values with the accumulated values.

        MutableDictionary accumulatedValues = mostRecentAccumulatedValues(job);

        // Pull any properties that are prefixed with "saved." into
        // ResultOutcome objects
        final String SAVED_PROPERTY_PREFIX = "saved.";

        for (Object propertyAsObj : properties.keySet())
        {
            String property = (String) propertyAsObj;

            if (property.startsWith(SAVED_PROPERTY_PREFIX))
            {
                String actualName = property.substring(
                        SAVED_PROPERTY_PREFIX.length());
                Object value = properties.valueForKey(property);

                if (value != null)
                {
                    // Update the accumulated value dictionary.
                    accumulatedValues.setObjectForKey(value, actualName);

                    if (value instanceof NSArray)
                    {
                        NSArray<?> array = (NSArray<?>) value;
                        int index = 0;

                        for (Object elem : array)
                        {
                            createResultOutcome( job, submissionResult, index,
                                    actualName, elem );
                            index++;
                        }
                    }
                    else
                    {
                        createResultOutcome( job, submissionResult, null,
                                actualName, value );
                    }
                }
            }
        }

        // Save the new accumulated saved properties into this submission
        // result.
        submissionResult.setAccumulatedSavedProperties(accumulatedValues);
    }


    // ----------------------------------------------------------
    /**
     * Creates a single result outcome from the value of a property in the
     * grading properties file. If the value is a dictionary, then it is
     * stored in the outcome directly; if it is a scalar value, then it is
     * stored in the outcome contents as a one-element dictionary with the key
     * named "value".
     *
     * @param job
     * @param submissionResult
     * @param index
     * @param tag
     * @param value
     */
    private void createResultOutcome( EnqueuedJob job,
                                      SubmissionResult submissionResult,
                                      Integer index,
                                      String tag,
                                      Object value )
    {
        NSDictionary<String, Object> contents;

        if (!(value instanceof NSDictionary))
        {
            contents = new NSDictionary<String, Object>(value, "value");
        }
        else
        {
            contents = (NSDictionary<String, Object>) value;
        }

        ResultOutcome outcome = new ResultOutcome();

        outcome.setTag(tag);
        outcome.setContents(new MutableDictionary(contents));

        if (index != null)
        {
            outcome.setIndex(index);
        }

        editingContext.insertObject(outcome);

        // TODO remove this when we fix the SubmissionResult.submission
        // relationship "problem"
        outcome.setSubmissionRelationship(job.submission());

        outcome.setSubmissionResultRelationship(submissionResult);
    }


    // ----------------------------------------------------------
    /**
     * Generates a single composite file from multiple inlined report
     * fragments.
     *
     * @param destination The output file to create and fill
     * @param inlineFragments The array of InlineFiles to fill it with
     */
    void generateCompositeResultFile( File    destination,
                                      NSArray inlineFragments )
    {
        if ( inlineFragments.count() > 0 )
        {
            try
            {
                BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream( destination ) );
                final byte[] borderString =
                    "<hr size=\"1\" noshade />\n".getBytes();

                boolean lastNeedsBorder = false;
                for ( int i = 0; i < inlineFragments.count(); i++ )
                {
                    InlineFile thisFile =
                        (InlineFile)inlineFragments.objectAtIndex( i );

                    boolean isHTML = thisFile.mimeType != null &&
                        ( thisFile.mimeType.equalsIgnoreCase( "text/html" ) ||
                          thisFile.mimeType.equalsIgnoreCase( "html" ) );
                    try
                    {
                        BufferedInputStream in = new BufferedInputStream(
                            new FileInputStream( thisFile ) );
                        if ( lastNeedsBorder || thisFile.border )
                        {
                            out.write( borderString );
                        }
                        lastNeedsBorder = thisFile.border;
                        if ( !isHTML )
                        {
                            out.write( "<pre>".getBytes() );
                        }

                        FileUtilities.copyStream( in, out );
                        in.close();

                        if ( !isHTML )
                        {
                            out.write( "</pre>".getBytes() );
                        }

                    }
                    catch ( Exception ex1 )
                    {
                        log.error( "exception copying inline report "
                                   + "fragment '"
                                   + thisFile.getPath()
                                   + "'",
                                   ex1 );
                        continue;
                    }
                }
                if ( lastNeedsBorder )
                {
                    out.write( borderString );
                }
                out.flush();
                out.close();
            }
            catch ( Exception ex2 )
            {
                log.error( "exception generating final report '"
                           + destination
                           + "'",
                           ex2 );
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Handles a technical fault Suspends grading of other submissions for the
     * same assignment
     *
     * @param job the job which faulted
     */
    void technicalFault( EnqueuedJob job,
                         String      stage,
                         Exception   e,
                         File        attachmentsDir )
    {
        job.setPaused( true );
        faultOccurredInStep = true;

        SubmissionSuspendedMessage msg = new SubmissionSuspendedMessage(
                job.submission(), e, stage, attachmentsDir);

        log.info("technicalFault(): " + msg.title());
        log.info(msg.shortBody(), e);

        msg.send();

/*        Vector<String> attachments = null;
        if ( attachmentsDir != null  &&  attachmentsDir.exists() )
        {
            attachments = new Vector<String>();
            File[] fileList = attachmentsDir.listFiles();
            for ( int i = 0; i < fileList.length; i++ )
            {
                if ( !fileList[i].isDirectory() )
                {
                    attachments.addElement( fileList[i].getPath() );
                }
            }
        }

        String errorMsg = "An " + ( ( e == null ) ? "error": "exception" )
                          + " occurred " + stage;
        if ( e != null )
        {
            errorMsg += ":\n" + e;
        }
        // errorMsg += "\n\nGrading for the submissions has been halted.\n";
        errorMsg += "\n\nGrading of this submission has been suspended.\n";
        String subject =
            "[Grader] Grading error: "
            + job.submission().user().userName() + " #"
            + job.submission().submitNumber();
        log.info( "technicalFault(): " + subject );
        log.info( errorMsg, e );
        Application.sendAdminEmail( null,
                                    job.submission().assignmentOffering()
                                        .courseOffering().instructors(),
                                    true,
                                    subject,
                                    errorMsg,
                                    attachments );*/
    }


    // ----------------------------------------------------------
    /**
     * Find out how many grading jobs have been processed so far.
     *
     * @return the number of jobs process so far
     */
    public int processedJobCount()
    {
        return jobCount;
    }


    // ----------------------------------------------------------
    /**
     * Find out the processing delay for the most recently completed job.
     *
     * @return the time in milliseconds
     */
    public long mostRecentJobWait()
    {
        return mostRecentJobWait;
    }


    // ----------------------------------------------------------
    /**
     * Find out the estimated processing delay for any job.
     *
     * @return the time in milliseconds
     */
    public long estimatedJobTime()
    {
        if ( jobsCountedWithWaits > 0 )
        {
            return totalWaitForJobs / jobsCountedWithWaits;
        }
        else
        {
            return DEFAULT_JOB_WAIT;
        }
    }


    //~ Instance/static variables .............................................

    /**
     * The grace period is added to the timeout limits for the various
     * scripts.  The value comes from the application property file.
     */
    static final int gracePeriod =
        Application.configurationProperties().intForKey( "grader.gracePeriod" );

    /**
     * The grace period is added to the timeout limits for the various
     * scripts.  The value comes from the application property file.
     */
    static final int emailWaitMinutes =
        Application.configurationProperties().intForKeyWithDefault(
            "grader.mailResultNotificationAfterMinutes", 15 );

    /** The queue to receive processing tokens. */
    private GraderQueue queue;
    /** Number of jobs processed so far, to report administrative status. */
    private int  jobCount = 0;
    private int  jobsCountedWithWaits = 0;

    /** Time between submission and grading completion for more recent job. */
    private long mostRecentJobWait = 0;
    private long totalWaitForJobs = 0;
    private static final long DEFAULT_JOB_WAIT = 30000;

    // State for the current step being executed
    private boolean faultOccurredInStep;
    private boolean timeoutOccurredInStep;

    private EOEditingContext editingContext;

    static Logger log = Logger.getLogger( GraderQueueProcessor.class );
}