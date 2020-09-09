/*==========================================================================*\
 |  $Id: GraderQueueProcessor.java,v 1.24 2014/11/07 13:55:03 stedwar2 Exp $
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.webcat.archives.ArchiveManager;
import org.webcat.archives.IWritableContainer;
import org.webcat.core.Application;
import org.webcat.core.FileUtilities;
import org.webcat.core.MutableDictionary;
import org.webcat.core.RepositoryEntryRef;
import org.webcat.core.Status;
import org.webcat.core.User;
import org.webcat.core.WCProperties;
import org.webcat.grader.messaging.AdminReportsForSubmissionMessage;
import org.webcat.grader.messaging.SubmissionSuspendedMessage;
import org.webcat.woextensions.ECAction;
import org.webcat.woextensions.WCEC;
import org.webcat.woextensions.WCFetchSpecification;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSTimestamp;
import er.extensions.eof.ERXConstant;
import er.extensions.eof.ERXEOGlobalIDUtilities;

// -------------------------------------------------------------------------
/**
 * This is the main grader processor class that performs the
 * compile/reference execution/execute/grade cycle on a student submission
 * job.
 *
 * @author  Amit Kulkarni
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.24 $, $Date: 2014/11/07 13:55:03 $
 */
public class GraderQueueProcessor
    extends ECAction
    implements Comparable<GraderQueueProcessor>
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Default constructor
     *
     * @param queue the queue to operate on
     */
    private GraderQueueProcessor(
        EOGlobalID jobId, long queueTime, boolean isRegrading)
    {
        this.jobId = jobId;
        this.queueTime = queueTime;
        this.isRegrading = isRegrading;
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public int compareTo(GraderQueueProcessor other)
    {
        if (this.isRegrading == other.isRegrading)
        {
            long diff = this.queueTime - other.queueTime;
            if (diff < 0)
            {
                return -1;
            }
            else if (diff == 0)
            {
                return this.jobId.toString().compareTo(other.jobId.toString());
            }
            else
            {
                return 1;
            }
        }
        else if (this.isRegrading)
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }


    // ----------------------------------------------------------
    /**
     * The actual thread of execution
     */
    public void action()
    {
        // Clear discarded jobs
        deleteJobsMatchingQualifier(EnqueuedJob.submission.isNull());
        deleteJobsMatchingQualifier(EnqueuedJob.discarded.isTrue());

        // Look for real jobs
        EnqueuedJob job = (EnqueuedJob)ERXEOGlobalIDUtilities
            .fetchObjectWithGlobalID(ec, jobId);
        if (job == null)
        {
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug(getName() + ": received job: " + job.submission());
        }

        NSTimestamp startProcessing = new NSTimestamp();
        Submission submission = job.submission();
        if (submission == null)
        {
            log.error(getName()
                + ": null submission in enqueued job: deleting");
            job.delete();
        }
        else if (job.discarded())
        {
            log.debug(getName() + ": discarded job: deleting");
            job.delete();
        }
        else if (submission.assignmentOffering() == null)
        {
            log.error(getName()
                + ": submission with null assignment "
                + "offering in enqueued job: deleting");
            job.delete();
        }
        else
        {
            if (submission.assignmentOffering()
                .gradingSuspended())
            {
                log.warn(getName() +
                    ": suspending job " + submission.dirName());
                job.setPaused(true);
                job.setProcessorRaw(null);
            }
            else
            {
                log.info(getName()
                    + ": processing submission " + submission);
                String msg = job.userPresentableDescription();
                processJobWithProtection(job);
                NSTimestamp now = new NSTimestamp();
                qstats.recordTimes(
                    msg,
                    (job.queueTime() != null)
                        ? now.getTime() - job.queueTime().getTime()
                        : now.getTime() - submission.submitTime().getTime(),
                    now.getTime() - startProcessing.getTime());
            }
        }

        // Now save all the changes

        // assignment offering could have changed because
        // of a fault, so save any changes before
        // forcing it out of editing context cache
        try
        {
            ec.saveChanges();
        }
        catch (IllegalStateException e)
        {
            // Database inconsistency problem
            log.error(getName() + ": exception trying to save "
                + "grading results for " + job.submission(), e);
            ec.revert();
            // FIXME: retry?
        }
    }


    // ----------------------------------------------------------
    private EnqueuedJob jobMatching(
        EOEditingContext context, EOQualifier qualifier)
    {
        // Repeat this here to force any objects to be refreshed
        WCFetchSpecification<EnqueuedJob> fspec =
            new WCFetchSpecification<EnqueuedJob>(
            EnqueuedJob.ENTITY_NAME, qualifier, EnqueuedJob.submitTime.ascs());
        fspec.setUsesDistinct(true);
        fspec.setFetchLimit(1);
        fspec.setRefreshesRefetchedObjects(true);
        NSArray<EnqueuedJob> objects =
            EnqueuedJob.objectsWithFetchSpecification(context, fspec);
        return (objects.size() > 0)
            ? objects.get(0)
            : null;
    }


    // ----------------------------------------------------------
    private void deleteJobsMatchingQualifier(EOQualifier qualifier)
    {
        EnqueuedJob job = null;
        try
        {
            job = jobMatching(ec, qualifier);
            while (job != null)
            {
                log.debug(getName()
                    + ": attempting to delete stale job " + job);
                job.delete();
                ec.saveChanges();
                job = jobMatching(ec, qualifier);
            }
        }
        catch (Exception e)
        {
            ec.reset();
            ec.unlock();
            if (ownsEC)
            {
                ec.dispose();
            }
            ec = WCEC.newEditingContext();
            ec.lock();
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
    void processJobWithProtection(EnqueuedJob job)
    {
        try
        {
            internalProcessJob(job);
        }
        catch (Exception e)
        {
            technicalFault(job, "while processing job", e, null);
        }
    }


    // ----------------------------------------------------------
    /**
     * This function processes the job and performs the stages that
     * are necessary.
     *
     * @param job the job to process
     */
    void internalProcessJob(EnqueuedJob job)
    {
        // boolean status            = false;
        // String  extendedErrorInfo = null;
        double scoreAdjustments = 0.0;
        double correctnessScore = 0.0;
        double toolScore        = 0.0;

        int jobNo = qstats.nextJob();
        log.info(getName() + ": Processing job " + jobNo + " for: "
            + job.submission().user().userName());

        // Set up the working directory first
        try
        {
            prepareWorkingDirectory(job);
        }
        catch (Exception e)
        {
            technicalFault(
                job, "while preparing the working directory", e, null);
            return;
        }

        // Get the steps in grading this assignment
        NSArray<Step> steps = Step.order.asc().sorted(
            job.submission().assignmentOffering().assignment().steps());


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
            technicalFault(job,
                "could not create the initial grading.properties file: "
                + gradingPropertiesFile.getAbsolutePath() + ": "
                + e.getMessage(),
                null,
                gradingPropertiesFile.getParentFile());
            return;
        }

        for (Step thisStep : steps)
        {
            @SuppressWarnings("unchecked")
            NSDictionary<String, Object> exports =
                (NSDictionary<String, Object>)thisStep.gradingPlugin()
                .configDescription().valueForKey("pipelineExports");
            if (exports != null)
            {
                gradingProperties.addPropertiesFromDictionary(exports);
            }
        }

        writeOutSavedGradingProperties(job, gradingProperties);

        for (int stepNo = 0; stepNo < steps.count(); stepNo++)
        {
            Step thisStep = steps.objectAtIndex(stepNo);

            executeStep(
                job, thisStep, gradingProperties, gradingPropertiesFile);

            if (faultOccurredInStep)
            {
                // technicalFault was already called by executeStep()
                // to pause the assignment and send e-mail to admins,
                // so just bail
                if (!Application.configurationProperties()
                    .booleanForKeyWithDefault("grader.preserveScratchFiles",
                    false))
                {
                    // duplicates the line at the end of the loop :-(
                    FileUtilities.deleteDirectory(job.workingDirName());
                }
                return;
            }

            // check the properties to update score and halt, if necessary
            if (gradingProperties.getProperty("score.adjustment") != null)
            {
                scoreAdjustments +=
                    gradingProperties.doubleForKey("score.adjustment");
                gradingProperties.remove("score.adjustment");
            }
            if (gradingProperties.getProperty("score.correctness") != null)
            {
                correctnessScore =
                    gradingProperties.doubleForKey("score.correctness");
            }
            if (gradingProperties.getProperty("score.tools") != null)
            {
                toolScore = gradingProperties.doubleForKey("score.tools");
            }
            if (gradingProperties.getProperty("halt") != null)
            {
                if (gradingProperties.booleanForKey("halt"))
                {
                  gradingProperties.remove("halt");
                  log.error(getName() + ": halt requested in step "
                      + thisStep + "\n\tfor job " + job);
                  job.setPaused(true);
                  return;
                }
            }
            if (gradingProperties.getProperty("canProceed") != null)
            {
                if (!gradingProperties.booleanForKey("canProceed"))
                {
                    break;
                }
            }
            if (gradingProperties.getProperty("halt.all") != null)
            {
                if (gradingProperties.booleanForKey("halt.all"))
                {
                  gradingProperties.remove("halt.all");
                  log.error(getName()
                      + ": halt requested for all jobs in step "
                      + thisStep + "\n\tfor job " + job);
                  job.setPaused(true);
                  AssignmentOffering assignment =
                      job.submission().assignmentOffering();
                  assignment.setGradingSuspended(true);
                  return;
                }
            }

            if (timeoutOccurredInStep)
            {
                if (!Application.configurationProperties()
                    .booleanForKeyWithDefault("grader.preserveScratchFiles",
                    false))
                {
                    FileUtilities.deleteDirectory(job.workingDirName());
                }
                technicalFault(job,
                    "script time limit exceeded in stage " + (stepNo + 1),
                    null,
                    gradingPropertiesFile.getParentFile());
                return;
            }
        }

        // Clean up the working directory.
        FileUtilities.deleteDirectory(job.workingDirName());

        generateFinalReport(job,
                            gradingProperties,
                            correctnessScore,
                            toolScore);

        log.info(getName() + ": Finished job " + jobNo);
    }


    // ----------------------------------------------------------
    /**
     * Gets the location where checked out files should be stored, creating
     * it if desired.
     *
     * @param job the grader job to associate the checkout with
     * @param clean true to clean out the checkout location and create it
     *              fresh, or false to just return the path (which may or
     *              may not exist)
     * @return the path to the checkout location
     */
    private File repositoryCheckoutLocation(EnqueuedJob job, boolean clean)
    {
        File root = new File(org.webcat.core.Application
            .configurationProperties().getProperty("grader.workarea"),
            "_GraderCheckout");
        File location = new File(root, job.id().toString());

        if (clean)
        {
            if (location.exists())
            {
                FileUtilities.deleteDirectory(location);
            }

            location.mkdirs();
        }

        return location;
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
    private void prepareWorkingDirectory(EnqueuedJob job)
        throws java.io.IOException
    {
        // Create the working compilation directory for the user
        File workingDir = new File(job.workingDirName());
        if (workingDir.exists())
        {
            FileUtilities.deleteDirectory(workingDir);
        }
        workingDir.mkdirs();

        // Copy the user's submission to the working dir
        Submission submission = job.submission();
        org.webcat.archives.ArchiveManager.getInstance()
            .unpack(workingDir, submission.file());

        // Create the grading output directory
        File graderLD = new File(submission.resultDirName());
        if (graderLD.exists())
        {
            FileUtilities.deleteDirectory(graderLD);
        }
        graderLD.mkdirs();
    }


    // ----------------------------------------------------------
    /**
     * Checks out the specified file into the temporary space used for
     * grading.
     *
     * @param fileInfo a String representing an old-style absolute path to
     *                 the old script-data area, or a dictionary containing
     *                 the repository info for a file
     * @return a File object that points to the location of the checked out
     *         file
     * @throws IOException if an I/O error occurred
     */
    private File checkOutRepositoryFiles(EnqueuedJob job, Object fileInfo)
        throws IOException
    {
        RepositoryEntryRef entryRef = null;

        if (fileInfo instanceof String)
        {
            entryRef = RepositoryEntryRef.fromOldStylePath((String) fileInfo);
        }
        else if (fileInfo instanceof NSDictionary<?, ?>)
        {
            @SuppressWarnings("unchecked")
            NSDictionary<String, Object> fileInfoDict =
                (NSDictionary<String, Object>)fileInfo;
            entryRef = RepositoryEntryRef.fromDictionary(fileInfoDict);
        }

        if (entryRef != null)
        {
            entryRef.resolve(job.editingContext());

            File checkoutLocation = repositoryCheckoutLocation(job, false);

            File repoDir = new File(entryRef.repositoryName());
            File filePath = new File(repoDir, entryRef.path());

            File containerPath =
                new File(checkoutLocation, filePath.getPath());
            if (!entryRef.isDirectory())
            {
                containerPath = containerPath.getParentFile();
            }

            containerPath.mkdirs();

            IWritableContainer container =
                ArchiveManager.getInstance().createWritableContainer(
                        containerPath, false);

            entryRef.repository().copyItemToContainer(
                    entryRef.objectId(), entryRef.name(), container);

            return filePath;
        }
        else
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Adds settings from a configuration settings dictionary to the
     * properties file for grading, checking out any required files into
     * temporary storage if necessary.
     *
     * @param job the job
     * @param config the configuration settings to add
     * @param properties the properties file to add the settings to
     * @param fileSettings a dictionary whose keys represent settings that
     *        are intended to be file paths
     * @param onlyIfNotDefined true to only add the property if it doesn't
     *        already exist
     */
    private void addConfigSettingsToProperties(
        EnqueuedJob       job,
        MutableDictionary config,
        WCProperties      properties,
        MutableDictionary fileSettings,
        boolean           onlyIfNotDefined)
        throws IOException
    {
        @SuppressWarnings("unchecked")
        NSArray<String> keys = config.allKeys();
        for (String property : keys)
        {
            if (!onlyIfNotDefined || !properties.containsKey(property))
            {
                Object value = config.objectForKey(property);

                if (fileSettings.containsKey(property))
                {
                    // Check out the file or directory, write it to a temporary
                    // location, and then write the value of the property to
                    // point to that location.

                    File location = checkOutRepositoryFiles(job, value);

                    if (location != null)
                    {
                        properties.setProperty(property, location.getPath());
                    }
                }
                else
                {
                    properties.setProperty(property, value.toString());
                }
            }
        }
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
    private void executeStep(
        EnqueuedJob  job,
        Step         step,
        WCProperties properties,
        File         propertiesFile)
    {
        faultOccurredInStep = false;
        timeoutOccurredInStep = false;
        log.debug(getName() + ": step " + step.order() + ": "
            + step.gradingPlugin().mainFilePath());

        try
        {
            step.gradingPlugin().reinitializeConfigAttributesIfNecessary();
            log.debug(getName() + ": creating properties file");

            MutableDictionary fileProps =
                step.gradingPlugin().fileConfigSettings();

            // Create a clean checkout location at the beginning of each step.
            File checkoutLocation = repositoryCheckoutLocation(job, true);

            properties.setProperty("pluginName", step.gradingPlugin().name());
            properties.setProperty("pluginData",
                checkoutLocation.getAbsolutePath());
            properties.setProperty("scriptData",  // legacy
                checkoutLocation.getAbsolutePath());

            // Re-write the properties file
            properties.addPropertiesFromDictionaryIfNotDefined(Application
                .wcApplication().subsystemManager().pluginProperties());

            addConfigSettingsToProperties(job,
                step.gradingPlugin().globalConfigSettings(),
                properties, fileProps, true);
            addConfigSettingsToProperties(job,
                step.gradingPlugin().defaultConfigSettings(),
                properties, fileProps, true);

            if (step.config() != null)
            {
                addConfigSettingsToProperties(job,
                    step.config().configSettings(),
                    properties, fileProps, false);
            }

            addConfigSettingsToProperties(
                job, step.configSettings(), properties, fileProps, false);

            /*properties.addPropertiesFromDictionaryIfNotDefined(
                step.gradingPlugin().globalConfigSettings() );
            properties.addPropertiesFromDictionaryIfNotDefined(
                step.gradingPlugin().defaultConfigSettings() );

            if ( step.config() != null )
            {
                properties.addPropertiesFromDictionary(
                    step.config().configSettings() );
            }

            properties.addPropertiesFromDictionary(
                step.configSettings() );*/

            properties.setProperty(
                "userName", job.submission().user().userName());
            properties.setProperty("user.isStaff", Boolean.toString(
                job.submission().assignmentOffering().courseOffering()
                .isStaff(job.submission().user())));
            properties.setProperty("userInstitution", job.submission().user()
                .authenticationDomain().displayableName());
            properties.setProperty(
                "workingDir", job.workingDirName());
            properties.setProperty(
                "resultDir", job.submission().resultDirName());
            properties.setProperty(
                "pluginHome", step.gradingPlugin().dirName());
            properties.setProperty(
                "scriptHome",  // legacy
                step.gradingPlugin().dirName());
            properties.setProperty("pluginResourcePrefix",
                "${pluginResource:" + step.gradingPlugin().name() + "}");
            properties.setProperty(
                "timeout", Integer.toString(step.effectiveEndToEndTimeout()));
            properties.setProperty("timeoutForOneRun",
                Integer.toString(step.effectiveTimeoutForOneRun()));
            properties.setProperty("course",
                job.submission().assignmentOffering().courseOffering()
                .course().deptNumber());
            properties.setProperty("course.number", Integer.toString(
                job.submission().assignmentOffering().courseOffering()
                .course().number()));
            properties.setProperty("institution",
                job.submission().assignmentOffering().courseOffering()
                .course().department().institution().name());
            {
                String crn = job.submission().assignmentOffering()
                    .courseOffering().crn();
                properties.setProperty("CRN",
                    (crn == null) ? "null" : crn);
            }
            properties.setProperty("semester",
                job.submission().assignmentOffering().courseOffering()
                .semester().toString());
            properties.setProperty("assignment",
                job.submission().assignmentOffering().assignment()
                .name() );

            properties.setProperty("dueDateTimestamp", Long.toString(
                job.submission().assignmentOffering().dueDate().getTime()));
            properties.setProperty("lateDeadlineTimestamp",
                Long.toString(job.submission().assignmentOffering()
                    .lateDeadline().getTime()));
            properties.setProperty("submissionTimestamp",
                Long.toString(job.submission().submitTime().getTime()));
            properties.setProperty("jobQueueTimestamp",
                Long.toString(job.queueTime().getTime()));

            properties.setProperty("jobQueuedAfterLateDeadline",
                Boolean.toString(job.queueTime().after(
                    job.submission().assignmentOffering().lateDeadline())));
            properties.setProperty("jobIsRegrading",
                Boolean.toString(job.regrading()));

            properties.setProperty("submissionNo",
                Integer.toString(job.submission().submitNumber()));
            properties.setProperty("frameworksBaseURL",
                Application.application().frameworksBaseURL());
            properties.setProperty(
                usePluginInternalThreads ? "run.parallel" : "run.sequential",
                "1");

            BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(propertiesFile));
            properties.store(
                out, "Web-CAT grader script configuration properties");
            out.close();

            File stdout = new File(job.submission().resultDirName(),
                "" + step.order() + "-stdout.txt");
            File stderr = new File(job.submission().resultDirName(),
                "" + step.order() + "-stderr.txt");

            // execute the script
            log.debug(getName() + ": executing script");
            timeoutOccurredInStep = step.execute(
                propertiesFile.getPath(),
                new File(job.workingDirName()),
                stdout,
                stderr);

            if (stderr.length() != 0)
            {
                technicalFault(job, "stderr output was produced by " + step,
                    null, propertiesFile.getParentFile());
                return;
            }
            else
            {
                stderr.delete();
            }
            if (stdout.length() == 0)
            {
                stdout.delete();
            }
            else
            {
                log.warn(getName() + ": Script produced stdout output in "
                    + stdout.getPath());
            }

            // Now reload the properties file
            log.debug(getName() + ": re-loading properties from file");
            BufferedInputStream in = new BufferedInputStream(
                new FileInputStream(propertiesFile));
            properties.clear();
            properties.load(in);
            in.close();
            // log.debug( "properties:\n" + properties );
        }
        catch (Exception e)
        {
            technicalFault(
                job, "in stage " + step, e, propertiesFile.getParentFile());
        }
        finally
        {
            // Clean up the checked out files.
            File checkoutLocation = repositoryCheckoutLocation(job, false);
            if (checkoutLocation.exists())
            {
                FileUtilities.deleteDirectory(checkoutLocation);
            }
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
    void collectReports(
        EnqueuedJob                job,
        WCProperties               properties,
        SubmissionResult           submissionResult,
        NSMutableArray<InlineFile> inlineStudentReports,
        NSMutableArray<InlineFile> inlineStaffReports,
        List<File>                 adminReports)
    {
        File parentDir = new File(job.submission().resultDirName());
        NSMutableArray<ResultFile> oldResultFiles =
            submissionResult.isNewObject()
            ? new NSMutableArray<ResultFile>()
            : submissionResult.resultFiles().mutableClone();

        // First, collect all the report fragments
        int numReports = properties.intForKey("numReports");
        for (int i = 1; i <= numReports; i++)
        {
            // First, extract the attributes
            String attributeBase = "report" + i + ".";
            String fileName = properties.getProperty(attributeBase + "file");
            if (fileName == null)
            {
                continue;
            }

            String mimeType = properties.getProperty(
                attributeBase + "mimeType", "text/plain");
            boolean inline = properties.booleanForKeyWithDefault(
                attributeBase + "inline", true);
            boolean border =
                properties.booleanForKey(attributeBase + "border");
            int styleVersion = properties.intForKeyWithDefault(
                    attributeBase + "styleVersion", 0);
            String to =
                properties.getProperty(attributeBase + "to", "student");
            boolean toStudent =
                to.equalsIgnoreCase("student")
                || to.equalsIgnoreCase("both")
                || to.equalsIgnoreCase("all");
            boolean toStaff =
                to.equalsIgnoreCase("staff")
                || to.equalsIgnoreCase("instructor")
                || to.equalsIgnoreCase("both")
                || to.equalsIgnoreCase("all");
            boolean toAdmin =
                to.equalsIgnoreCase("admin")
                || to.equalsIgnoreCase("administrator")
                || to.equalsIgnoreCase("all");
            // Now, populate the lists
            if (toStudent)
            {
                if (inline)
                {
                    int pos = properties.intForKeyWithDefault(
                        attributeBase + "position",
                        inlineStudentReports.size());
                    inlineStudentReports.add(pos, new InlineFile(
                        parentDir, fileName, mimeType, border));

                    int currentVersion =
                        submissionResult.studentReportStyleVersion();

                    if (submissionResult.studentReportStyleVersionRaw() == null
                            || styleVersion < currentVersion)
                    {
                        submissionResult.setStudentReportStyleVersion(
                            styleVersion);
                    }
                }
                else
                {
                    ResultFile thisFile = null;
                    NSArray<ResultFile> files =
                        submissionResult.isNewObject()
                        ? new NSArray<ResultFile>()
                        : ResultFile
                            .objectsMatchingQualifier(job.editingContext(),
                                ResultFile.submissionResult.is(submissionResult)
                                    .and(ResultFile.fileName.eq(fileName)));
                    if (files.size() > 0)
                    {
                        thisFile = files.get(0);
                        oldResultFiles.remove(thisFile);
                    }
                    else
                    {
                        thisFile = new ResultFile();
                        job.editingContext().insertObject(thisFile);
                        thisFile.setFileName(fileName);
                        thisFile.setSubmissionResultRelationship(
                            submissionResult);
                    }
                    thisFile.setLabel(
                        properties.getProperty(attributeBase + "label"));
                    thisFile.setMimeType(mimeType);
                }
            }
            if (toStaff)
            {
                if (inline)
                {
                    inlineStaffReports.addObject(new InlineFile(
                        parentDir, fileName, mimeType, border));

                    int currentVersion =
                        submissionResult.staffReportStyleVersion();

                    if (submissionResult.staffReportStyleVersionRaw() == null
                        || styleVersion < currentVersion)
                    {
                        submissionResult.setStaffReportStyleVersion(
                            styleVersion);
                    }
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
                    toAdmin = true;
                }
            }
            if (toAdmin)
            {
                adminReports.add(new File(
                    job.submission().resultDirName() + "/" + fileName));
            }
        }
        for (ResultFile thisFile : oldResultFiles)
        {
            thisFile.delete();
        }

        // Second, collect all the stats markup files
        NSMutableArray<SubmissionFileStats> oldStats =
            submissionResult.isNewObject()
            ? new NSMutableArray<SubmissionFileStats>()
            : submissionResult.submissionFileStats().mutableClone();
        String statElementsLabel = properties.getProperty("statElementsLabel");
        if (statElementsLabel != null)
        {
            submissionResult.setStatElementsLabel(statElementsLabel);
        }
        numReports = properties.intForKey("numCodeMarkups");
        for (int i = 1; i <= numReports; i++)
        {
            String attributeBase = "codeMarkup" + i + ".";
            SubmissionFileStats stats = null;

            String markupFileName =
                properties.getProperty(attributeBase + "markupFileName");
            String className =
                properties.getProperty(attributeBase + "className");
            String pkgName =
                properties.getProperty(attributeBase + "pkgName");

            NSArray<SubmissionFileStats> matches =
                submissionResult.isNewObject()
                ? new NSArray<SubmissionFileStats>()
                : ((markupFileName != null && !markupFileName.isEmpty())
                ? SubmissionFileStats.objectsMatchingQualifier(
                    job.editingContext(),
                    SubmissionFileStats.submissionResult.is(submissionResult)
                        .and(SubmissionFileStats.markupFileNameRaw
                            .eq(markupFileName)))
                : SubmissionFileStats.objectsMatchingQualifier(
                    job.editingContext(),
                    SubmissionFileStats.submissionResult.is(submissionResult)
                        .and(SubmissionFileStats.className
                            .eq(className))
                        .and(SubmissionFileStats.pkgName.eq(pkgName))));

            if (matches.size() > 0)
            {
                stats = matches.get(0);
                oldStats.remove(stats);
            }
            else
            {
                stats = new SubmissionFileStats();
                job.editingContext().insertObject(stats);
                stats.setSubmissionResultRelationship(submissionResult);
            }
            stats.setClassName(className);
            stats.setPkgName(pkgName);
            stats.setSourceFileNameRaw(
                properties.getProperty(attributeBase + "sourceFileName"));
            if (markupFileName != null && !markupFileName.isEmpty())
            {
                stats.setMarkupFileNameRaw(markupFileName);
            }
            else
            {
                // Force default generation of markup file name
                stats.markupFileName();
            }

            // The tags are zero or more space-delimited strings that describe
            // what this file's role is (such as if it is a test case). Note
            // that we pad the tag string with a space on each end if necessary
            // so that tags can always be searched for in the database using a
            // LIKE qualifier such as "% tag %". This way tags that are infixes
            // of other tags will not be erroneously detected.

            String tags = properties.getProperty(attributeBase + "tags");
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

            String attr = properties.getProperty(attributeBase + "loc");
            if (attr != null)
            {
                stats.setLocRaw(integerForString(attr));
            }
            attr = properties.getProperty(attributeBase + "ncloc");
            if (attr != null)
            {
                stats.setNclocRaw(integerForString(attr));
            }
            attr = properties.getProperty(attributeBase + "deductions");
            if (attr != null)
            {
                stats.setDeductionsRaw(new Double(attr));
            }
            attr = properties.getProperty(attributeBase + "remarks");
            if (attr != null)
            {
                stats.setRemarksRaw(integerForString(attr));
            }
            attr = properties.getProperty(attributeBase + "conditionals");
            if (attr != null)
            {
                stats.setConditionalsRaw(integerForString(attr));
            }
            attr = properties.getProperty(
                attributeBase + "conditionalsCovered");
            if (attr != null)
            {
                stats.setConditionalsCoveredRaw(integerForString(attr));
            }
            attr = properties.getProperty(attributeBase + "statements");
            if (attr != null)
            {
                stats.setStatementsRaw(integerForString(attr));
            }
            attr = properties.getProperty(
                attributeBase + "statementsCovered");
            if (attr != null)
            {
                stats.setStatementsCoveredRaw(integerForString(attr));
            }
            attr = properties.getProperty(attributeBase + "methods");
            if (attr != null)
            {
                stats.setMethodsRaw(integerForString(attr));
            }
            attr = properties.getProperty(
                attributeBase + "methodsCovered");
            if (attr != null)
            {
                stats.setMethodsCoveredRaw(integerForString(attr));
            }
            attr = properties.getProperty(attributeBase + "elements");
            if (attr != null)
            {
                stats.setElementsRaw(integerForString(attr));
            }
            attr = properties.getProperty(
                attributeBase + "elementsCovered");
            if (attr != null)
            {
                stats.setElementsCoveredRaw(integerForString(attr));
            }
        }
        for (SubmissionFileStats stats : oldStats)
        {
            stats.delete();
        }
    }


    // ----------------------------------------------------------
    private static Integer integerForString(String value)
    {
        try
        {
            return ERXConstant.integerForString(value);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    protected class InlineFile
        extends File
    {
        public InlineFile(
            File parent, String child, String type, boolean useBorder)
        {
            super(parent, child);
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
    void generateFinalReport(
        EnqueuedJob  job,
        WCProperties properties,
        double       correctnessScore,
        double       toolScore)
    {
        EOEditingContext editingContext = job.editingContext();
        SubmissionResult submissionResult = job.submission().result();
        if (submissionResult == null)
        {
            submissionResult = new SubmissionResult();
            job.editingContext().insertObject(submissionResult);
            submissionResult.addToSubmissionsRelationship(job.submission());
        }

        submissionResult.setCorrectnessScore(correctnessScore);
        submissionResult.setToolScore(toolScore);

        NSMutableArray<InlineFile> inlineStudentReports =
            new NSMutableArray<InlineFile>();
        NSMutableArray<InlineFile> inlineStaffReports =
            new NSMutableArray<InlineFile>();
        List<File> adminReports  = new ArrayList<File>();
        collectReports(job, properties, submissionResult,
            inlineStudentReports, inlineStaffReports, adminReports);

        generateCompositeResultFile(
            new File(job.submission().resultDirName(),
                     SubmissionResult.resultFileName()),
            inlineStudentReports);
        generateCompositeResultFile(
            new File(job.submission().resultDirName(),
                SubmissionResult.staffResultFileName()),
            inlineStaffReports);

        // 2009-02-04 (AJA): create result blobs
        processSavedProperties(job, submissionResult, properties);

        editingContext.saveChanges();
        boolean wasRegraded = job.regrading();

        if (!job.submission().assignmentOffering().assignment().usesTAScore()
            && (job.submission().assignmentOffering().assignment()
                .usesTestingScore()
                || job.submission().assignmentOffering().assignment()
                    .usesToolCheckScore()))
        {
            submissionResult.setStatus(Status.CHECK);
        }

        if (job.submission().assignmentOffering().assignment()
            .submissionProfile().allowPartners()
            && job.submission().assignmentOffering().assignment()
            .submissionProfile().autoAssignPartners())
        {
            connectPartnersFromProperty(job, properties.getProperty(
                "grader.potentialpartners"));
        }

        job.submission().setIsSubmissionForGradingIfNecessary();

        try
        {
            if (job.submission() != null)
            {
                for (Submission partneredSubmission :
                    job.submission().partneredSubmissions())
                {
                    partneredSubmission.setResultRelationship(
                        submissionResult);
                    // Force it to be marked as a partner submission as
                    // a stop-gap until we find the real problem.
                    partneredSubmission.setPartnerLink(true);

                    partneredSubmission.setIsSubmissionForGradingIfNecessary();
                }
            }
        }
        catch (Exception e)
        {
            log.error(getName() + ": Unable to link partner submissions", e);
        }

        job.setSubmissionRelationship(null);
        editingContext.deleteObject(job);
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
        if (limitTime.before(new NSTimestamp())) // compare against now
        {
            String msg = "is now available";
            if (wasRegraded)
            {
                msg += ".\nA course staff member requested that it be "
                    + "regraded";
            }
            submissionResult.emailNotificationToStudent(msg);
        }

        // Send out admin reports, if any
        //
        if (adminReports.size() > 0)
        {
            Submission submission = submissionResult.submission();

            new AdminReportsForSubmissionMessage(submission, adminReports)
                .send();
        }
    }


    // ----------------------------------------------------------
    /**
     * Parses the specified space-separated list of partner candidates and
     * pairs the submission with any of those usernames who are in the same
     * course.
     *
     * @param job the job with the primary submission
     * @param candidateString the space-separated list of partner candidates
     */
    private void connectPartnersFromProperty(
        EnqueuedJob job, String candidateString)
    {
        NSMutableSet<User> potentialPartners = new NSMutableSet<User>();

        if (candidateString != null)
        {
            String[] candidates = candidateString.split("\\s+");

            for (String candidate : candidates)
            {
                candidate = candidate.trim();

                if (candidate.length() > 0)
                {
                    User user = User.userWithDomainAndName(
                        job.editingContext(),
                        job.submission().user().authenticationDomain(),
                        candidate);

                    if (user != null)
                    {
                        potentialPartners.addObject(user);
                    }
                }
            }
        }

        // Now that we've found the set of people who can be partners, filter
        // out the ones who already have submissions (say, through an external
        // submitter) and create submissions for the ones who don't.

        for (Submission partneredSub : job.submission().partneredSubmissions())
        {
            potentialPartners.removeObject(partneredSub.user());
        }

        job.submission().partnerWith(potentialPartners.allObjects());
    }


    // ----------------------------------------------------------
    private void writeOutSavedGradingProperties(
        EnqueuedJob job, WCProperties gradingProperties)
    {
        MutableDictionary accumulatedValues =
            mostRecentAccumulatedValues(job);
        NSDictionary<String, Object> previousValues =
            previousSubmissionSavedProperties(job);

        @SuppressWarnings("unchecked")
        NSArray<String> keys = accumulatedValues.allKeys();

        for (String key : keys)
        {
            if (!key.matches("^(previous|mostRecent)\\..*\\.results$"))
            {
                Object value = accumulatedValues.objectForKey(key);
                gradingProperties.setObjectForKey(value, "mostRecent." + key);
            }
        }

        for (String key : previousValues.allKeys())
        {
            if (!key.matches("^(previous|mostRecent)\\..*\\.results$"))
            {
                Object value = previousValues.objectForKey(key);
                gradingProperties.setObjectForKey(value, "previous." + key);
            }
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
                    @SuppressWarnings("unchecked")
                    NSMutableArray<Object> array =
                        (NSMutableArray<Object>)props.objectForKey(key);

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
        while (prevSub != null && !prevSub.resultIsReady())
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
    private void processSavedProperties(
        EnqueuedJob job,
        SubmissionResult submissionResult,
        WCProperties properties)
    {
        // Get the previous result with any data so that we can merge in these
        // values with the accumulated values.

        MutableDictionary accumulatedValues =
            mostRecentAccumulatedValues(job);

        // Pull any properties that are prefixed with "saved." into
        // ResultOutcome objects
        final String SAVED_PROPERTY_PREFIX = "save.";
        final String RESULT_PROPERTY_SUFFIX = ".results";

        for (Object propertyAsObj : properties.keySet())
        {
            String property = (String) propertyAsObj;
            String actualName = null;
            if (property.startsWith(SAVED_PROPERTY_PREFIX))
            {
                actualName =
                    property.substring(SAVED_PROPERTY_PREFIX.length());
            }
            else if (property.endsWith(RESULT_PROPERTY_SUFFIX)
                && !property.startsWith("mostRecent.")
                && !property.startsWith("previous."))
            {
                actualName = property;
//                actualName = property.substring(
//                    0, property.length() - RESULT_PROPERTY_SUFFIX.length());
            }

            if (actualName != null)
            {
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
                            createResultOutcome(job, submissionResult,
                                index, actualName, elem);
                            index++;
                        }
                    }
                    else
                    {
                        createResultOutcome(
                            job, submissionResult, null, actualName, value);
                    }
                }
            }
        }

        // Save the new accumulated saved properties into this submission
        // result.
        submissionResult.setAccumulatedSavedProperties(accumulatedValues);

        String recharge =
            properties.getProperty("submission.energy.recharge", "0");
        try
        {
            int rc =
                er.extensions.foundation.ERXValueUtilities.intValue(recharge);
            if (rc > 0)
            {
                Submission sub = submissionResult.submission();
                EnergyBar bar = sub.assignmentOffering().energyBarForUser(
                    sub.user());
                if (bar != null)
                {
                    int max = bar.maxCharge();
                    if (bar.charge() < max)
                    {
                        bar.setCharge(max);
                    }
                    bar.reevaluateCharge();
                    bar.logEvent(EnergyBar.FULL_RECHARGE_BONUS, sub);
                }
            }
        }
        catch (NumberFormatException e)
        {
            // ignore, nothing to see here
        }
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
    private void createResultOutcome(
        EnqueuedJob job,
        SubmissionResult submissionResult,
        Integer index,
        String tag,
        Object value)
    {
        NSDictionary<String, Object> contents;

        if (!(value instanceof NSDictionary))
        {
            contents = new NSDictionary<String, Object>(value, "value");
        }
        else
        {
            @SuppressWarnings("unchecked")
            NSDictionary<String, Object> castContents =
                (NSDictionary<String, Object>) value;
            contents = castContents;
        }

        ResultOutcome outcome = new ResultOutcome();

        outcome.setTag(tag);
        outcome.setContents(new MutableDictionary(contents));

        if (index != null)
        {
            outcome.setIndex(index);
        }

        job.editingContext().insertObject(outcome);

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
    void generateCompositeResultFile(
        File destination, NSArray<InlineFile> inlineFragments)
    {
        if (inlineFragments.count() > 0)
        {
            try
            {
                BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(destination));
                final byte[] borderString =
                    "<hr size=\"1\" noshade />\n".getBytes();

                boolean lastNeedsBorder = false;
                for (InlineFile thisFile: inlineFragments)
                {
                    boolean isHTML = thisFile.mimeType != null &&
                        (thisFile.mimeType.equalsIgnoreCase("text/html") ||
                        thisFile.mimeType.equalsIgnoreCase("html"));
                    try
                    {
                        BufferedInputStream in = new BufferedInputStream(
                            new FileInputStream(thisFile));
                        if (lastNeedsBorder || thisFile.border)
                        {
                            out.write(borderString);
                        }
                        lastNeedsBorder = thisFile.border;
                        if (!isHTML)
                        {
                            out.write("<pre>".getBytes());
                        }

                        FileUtilities.copyStream(in, out);
                        in.close();

                        if (!isHTML)
                        {
                            out.write("</pre>".getBytes());
                        }

                    }
                    catch (Exception ex1)
                    {
                        log.error(getName()
                            + ": exception copying inline report fragment '"
                            + thisFile.getPath() + "'", ex1);
                        continue;
                    }
                }
                if (lastNeedsBorder)
                {
                    out.write(borderString);
                }
                out.flush();
                out.close();
            }
            catch (Exception ex2)
            {
                log.error(getName() + ": exception generating final report '"
                    + destination + "'", ex2);
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
    void technicalFault(
        EnqueuedJob job,
        String      stage,
        Exception   e,
        File        attachmentsDir)
    {
        job.setPaused(true);
        job.setProcessorRaw(null);
        faultOccurredInStep = true;
        if (attachmentsDir == null && job.submission() != null)
        {
            attachmentsDir = job.submission().resultDir();
        }

        try
        {
            SubmissionSuspendedMessage msg = new SubmissionSuspendedMessage(
                job.submission(), e, stage, attachmentsDir);

            log.info(getName() + ": technicalFault(): " + msg.title());
            log.info(getName() + ": " + msg.shortBody(), e);

            msg.send();
        }
        catch (Exception ee)
        {
            log.error(
                getName() + ": Exception sending message to student", ee);
            log.error(getName() + ": Cause:", e);
        }
        try
        {
            // If less than approx 2GB of space
            if (Grader.workAreaTracker().isTrackingStore()
                && Grader.workAreaTracker().usableSpace() < 2000000000)
            {
                // Clean up the working directory to keep work area from
                // clogging
                FileUtilities.deleteDirectory(job.workingDirName());
                // Normally, the working directory would be left for
                // a while, in case the admin needed to check it to
                // determine why the failure occurred
            }
        }
        catch (Exception ee)
        {
            log.error(getName() +
                ": Exception attempting to purge working directory", ee);
            log.error(getName() + ": Cause:", e);
        }

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
    public static int processedJobCount()
    {
        return qstats.jobCount();
    }


    // ----------------------------------------------------------
    /**
     * Find out the processing delay for the most recently completed job.
     *
     * @return the time in milliseconds
     */
    public static long mostRecentJobWait()
    {
        return qstats.mostRecentJobWait();
    }


    // ----------------------------------------------------------
    /**
     * Find out the estimated processing delay for any job.
     *
     * @return the time in milliseconds
     */
    public static long estimatedJobTime()
    {
        return qstats.estimatedJobTime();
    }


    // ----------------------------------------------------------
    /**
     * Indicate that a job is available for grading.
     * @param job The job.
     */
    public static void processJob(EnqueuedJob job)
    {
        if (pool == null)
        {
            pool = newFixedThreadPool();
        }
        pool.execute(new GraderQueueProcessor(
            job.permanentGlobalID(false),
            job.queueTime().getTime(),
            job.regrading()));
    }


    // ----------------------------------------------------------
    /**
     * Indicate that a job is available for grading.
     * @param submission The submission corresponding to the queued job.
     */
    public static void processSubmission(Submission submission)
    {
        if (submission.enqueuedJob() == null)
        {
            log.error("submission " + submission
                + " not requeued for grading properly!");
            submission.requeueForGrading(submission.editingContext());
            submission.editingContext().saveChanges();
        }
        processJob(submission.enqueuedJob());
    }


    // ----------------------------------------------------------
    /**
     * Indicate that multiple jobs are available for grading.
     * @param jobs An array of the available jobs.
     */
    public static void processJobs(NSArray<EnqueuedJob> jobs)
    {
        for (EnqueuedJob job : jobs)
        {
            processJob(job);
        }
    }


    // ----------------------------------------------------------
    /**
     * Indicate that multiple jobs are available for grading.
     * @param submissions An array of the submissions corresponding to the
     *                    enqueued jobs.
     */
    public static void processSubmissions(NSArray<Submission> submissions)
    {
        for (Submission sub : submissions)
        {
            processSubmission(sub);
        }
    }


    // ----------------------------------------------------------
    private static class QueueStats
    {
        // ----------------------------------------------------------
        public QueueStats()
        {
            // fields initialized in declarations
        }


        // ----------------------------------------------------------
        public synchronized int nextJob()
        {
            return ++jobCount;
        }


        // ----------------------------------------------------------
        public synchronized void recordTimes(
            String msg, long thisWait, long processingTime)
        {
            mostRecentJobWait = thisWait;
            totalWaitForJobs += processingTime;
            jobsCountedWithWaits++;

            long wait = thisWait - processingTime;
            log.info("completed job [" + msg + "], wait = "
                + ((wait + 500.0)/1000.0)
                + "s, processing time = "
                + ((processingTime + 500.0)/1000.0)
                + "s");
        }


        // ----------------------------------------------------------
        public synchronized int jobCount()
        {
            return jobCount;
        }


        // ----------------------------------------------------------
        public synchronized long mostRecentJobWait()
        {
            return mostRecentJobWait;
        }


        // ----------------------------------------------------------
        public synchronized long estimatedJobTime()
        {
            if (jobsCountedWithWaits > 0)
            {
                return totalWaitForJobs / jobsCountedWithWaits;
            }
            else
            {
                return DEFAULT_JOB_WAIT;
            }
        }


        private int  jobCount = 0;
        private int  jobsCountedWithWaits = 0;
        private long mostRecentJobWait = 0;
        private long totalWaitForJobs = 0;
        private static final long DEFAULT_JOB_WAIT = 30000;
    }


    // ----------------------------------------------------------
    private String getName()
    {
        return Thread.currentThread().getName();
    }


    // ----------------------------------------------------------
    private static int threadPoolSize()
    {
        // Calculate the number of grader threads
        int cores = Runtime.getRuntime().availableProcessors();
        if (!Application.configurationProperties()
            .booleanForKeyWithDefault("grader.multithreaded", true))
        {
            cores = 1;
        }
        log.info("Configuring for " + cores + " core"
            + (cores > 1 ? "s" : ""));
        int threads = (cores / 2) - 1;
        usePluginInternalThreads = Application.configurationProperties()
            .booleanForKeyWithDefault("grader.usePluginInternalThreads", true);
        if (usePluginInternalThreads)
        {
            threads /= 3;
        }

        if (threads <= 2)
        {
            usePluginInternalThreads = false;
            threads = (cores / 2) - 1;
            if (threads < 1)
            {
                threads = 1;
            }
        }

        int maxThreads = Application.configurationProperties()
            .intForKeyWithDefault("grader.maxGraderThreads", 0);
        if (maxThreads > 0 && maxThreads < threads)
        {
            threads = maxThreads;
        }

        log.info("Multi-threaded execution of plug-ins is "
            + (usePluginInternalThreads ? "ON" : "OFF"));
        return threads;
    }


    // ----------------------------------------------------------
    private static ThreadPoolExecutor newFixedThreadPool()
    {
        int nThreads = threadPoolSize();
        log.info("Creating pool of " + nThreads
            + " submission processing thread(s)");

        return new ThreadPoolExecutor(
            nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            new PriorityBlockingQueue<Runnable>(),
            new ThreadFactory() {
                // ----------------------------------------------------------
                public Thread newThread(Runnable r)
                {
                    return new Thread(r,
                        "GraderQueueProcessor" + ++nextProcessor);
                }
            });
    }


    //~ Instance/static variables .............................................

    /**
     * The grace period is added to the timeout limits for the various
     * scripts.  The value comes from the application property file.
     */
    static final int gracePeriod = Application.configurationProperties()
        .intForKey("grader.gracePeriod");

    /**
     * The grace period is added to the timeout limits for the various
     * scripts.  The value comes from the application property file.
     */
    static final int emailWaitMinutes =
        Application.configurationProperties().intForKeyWithDefault(
            "grader.mailResultNotificationAfterMinutes", 15);

    private static int nextProcessor = 0;
    private static boolean usePluginInternalThreads = false;
    private static final QueueStats qstats = new QueueStats();
    private static ThreadPoolExecutor pool = null;

    // State for the current step being executed
    private boolean faultOccurredInStep;
    private boolean timeoutOccurredInStep;
    private EOGlobalID jobId;
    private long queueTime;
    private boolean isRegrading;

    static Logger log = Logger.getLogger(GraderQueueProcessor.class);
}
