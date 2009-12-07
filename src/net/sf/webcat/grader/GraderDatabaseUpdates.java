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

import net.sf.webcat.dbupdate.UpdateSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * This class captures the SQL database schema for the database tables
 * underlying the Grader subsystem and the Grader.eomodeld.  Logging output
 * for this class uses its parent class' logger.
 *
 * @author  Stephen Edwards
 * @version $Id$
 */
public class GraderDatabaseUpdates
    extends UpdateSet
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * The default constructor uses the name "grader" as the unique
     * identifier for this subsystem and EOModel.
     */
    public GraderDatabaseUpdates()
    {
        super( "grader" );
    }


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     * Creates all tables in their baseline configuration, as needed.
     * @throws SQLException on error
     */
    public void updateIncrement0() throws SQLException
    {
        createAssignmentTable();
        createAssignmentOfferingTable();
        createEnqueuedJobTable();
        createGraderPrefsTable();
        createGradingCriteriaTable();
        createResultFileTable();
        createUploadedScriptFilesTable();
        createStepTable();
        createStepConfigTable();
        createSubmissionTable();
        createSubmissionFileCommentTable();
        createSubmissionFileStatsTable();
        createSubmissionProfileTable();
        createSubmissionResultTable();
    }


    // ----------------------------------------------------------
    /**
     * Adds columns for recording names and owners on step configurations.
     * @throws SQLException on error
     */
    public void updateIncrement1() throws SQLException
    {
        database().executeSQL(
            "alter table TSTEPCONFIG add CAUTHORID INTEGER" );
        database().executeSQL(
            "alter table TSTEPCONFIG add CNAME TINYTEXT" );
    }


    // ----------------------------------------------------------
    /**
     * This performs some simple column value maintenance to repair a
     * bug in an earlier Web-CAT version.  It resets all the
     * updateMutableFields columns to all zeroes.
     * @throws SQLException on error
     */
    public void updateIncrement2() throws SQLException
    {
        database().executeSQL(
            "UPDATE TSTEP SET CUPDATEMUTABLEFIELDS = 0" );
        database().executeSQL(
            "UPDATE TSTEPCONFIG SET CUPDATEMUTABLEFIELDS = 0" );
        database().executeSQL(
            "UPDATE TUPLOADEDSCRIPTFILES SET CUPDATEMUTABLEFIELDS = 0" );
    }


    // ----------------------------------------------------------
    /**
     * Adds assignment graphing summary fields to assignment offerings.
     * @throws SQLException on error
     */
    public void updateIncrement3() throws SQLException
    {
        database().executeSQL(
            "alter table TASSIGNMENTOFFERING add "
            + "CGRAPHSUMMARY BLOB" );
        database().executeSQL(
            "alter table TASSIGNMENTOFFERING add "
            + "CUPDATEMUTABLEFIELDS BIT NOT NULL" );
    }


    // ----------------------------------------------------------
    /**
     * Adds support for storing parameters needed to publish submissions
     * for external submission engines.
     * @throws SQLException on error
     */
    public void updateIncrement4() throws SQLException
    {
        database().executeSQL(
            "alter table TSUBMISSIONPROFILE add "
            + "CINCLUDEDFILEPATTERNS TEXT" );
        database().executeSQL(
            "alter table TSUBMISSIONPROFILE add "
            + "CEXCLUDEDFILEPATTERNS TEXT" );
        database().executeSQL(
            "alter table TSUBMISSIONPROFILE add "
            + "CREQUIREDFILEPATTERNS TEXT" );
        database().executeSQL(
            "alter table TSUBMISSIONPROFILE add "
            + "CSUBMISSIONMETHOD TINYINT NOT NULL" );
    }


    // ----------------------------------------------------------
    /**
     * Adds support for global configuration parameters for plug-ins.
     * @throws SQLException on error
     */
    public void updateIncrement5() throws SQLException
    {
        database().executeSQL(
            "alter table TUPLOADEDSCRIPTFILES add "
            + "CGLOBALCONFIGSETTINGS BLOB" );
    }


    // ----------------------------------------------------------
    /**
     * Drop the unused hasSuspendedSubs attribute from AssignmentOffering.
     * @throws SQLException on error
     */
    public void updateIncrement6() throws SQLException
    {
        database().executeSQL(
            "alter table TASSIGNMENTOFFERING drop "
            + "FHASSUSPENDEDSUBS" );
    }


    // ----------------------------------------------------------
    /**
     * Drop the unused courseOfferingId attribute from GraderPrefs, and add
     * to Submission a field indicating whether it is the "submission for
     * grading."
     * @throws SQLException on error
     */
    public void updateIncrement7() throws SQLException
    {
        database().executeSQL(
            "alter table TGRADERPREFS drop "
            + "CCOURSEOFFERINGID" );

        database().executeSQL(
            "alter table TSUBMISSION add "
            + "CISSUBMISSIONFORGRADING BIT");

        database().executeSQL(
                "alter table TSUBMISSIONRESULT modify "
                + "CISMOSTRECENT BIT");

/*        database().executeSQL(
                "alter table TSUBMISSIONRESULT add "
                + "CFINALSCORE DOUBLE");*/
    }


    // ----------------------------------------------------------
    /**
     * Creates the TRESULTOUTCOME table.
     * @throws SQLException on error
     */
    public void updateIncrement8() throws SQLException
    {
        if ( !database().hasTable( "TRESULTOUTCOME" ) )
        {
            log.info( "creating table TRESULTOUTCOME" );
            database().executeSQL(
                "CREATE TABLE TRESULTOUTCOME "
                + "(OID INTEGER NOT NULL , CRESULTID INTEGER , "
                + "CSUBMISSIONID INTEGER , CINDEX INTEGER , CTAG TINYTEXT , "
                + "CCONTENTS BLOB , CUPDATEMUTABLEFIELDS BIT NOT NULL )" );
            database().executeSQL(
                "ALTER TABLE TRESULTOUTCOME ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Adds support for global configuration parameters for plug-ins.
     * @throws SQLException on error
     */
    public void updateIncrement9() throws SQLException
    {
        database().executeSQL(
            "alter table TGRADERPREFS add "
            + "CASSIGNID INTEGER" );
    }


    // ----------------------------------------------------------
    /**
     * Adds support for tagging files during grading.
     * @throws SQLException on error
     */
    public void updateIncrement10() throws SQLException
    {
        database().executeSQL(
            "alter table TSUBMISSIONFILESTATS add "
            + "CTAGS TEXT" );
    }


    // ----------------------------------------------------------
    /**
     * Adds fields for closing assignments and tracking opinions.
     * @throws SQLException on error
     */
    public void updateIncrement11() throws SQLException
    {
        database().executeSQL(
            "alter table TASSIGNMENTOFFERING add "
            + "closedOnDate DATETIME" );
        database().executeSQL(
            "alter table TASSIGNMENT add "
            + "trackOpinions BIT NOT NULL" );
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    /**
     * Create the TASSIGNMENT table, if needed.
     * @throws SQLException on error
     */
    private void createAssignmentTable() throws SQLException
    {
        if ( !database().hasTable( "TASSIGNMENT" ) )
        {
            log.info( "creating table TASSIGNMENT" );
            database().executeSQL(
                "CREATE TABLE TASSIGNMENT "
                + "(CAUTHORID INTEGER , CFILEUPLOADMESSAGE TEXT , "
                + "CCOMPARISONGRADINGCRITERIAID INTEGER , "
                + "OID INTEGER NOT NULL, CMOODLEID INTEGER , "
                + "CASSIGNMENTNAME TINYTEXT , CRUBRICID INTEGER , "
                + "CASSIGNMENTDESCRIPTION TINYTEXT , "
                + "CGRADINGPROFILEID INTEGER , CASSIGNMENTURL TINYTEXT )" );
            database().executeSQL(
                "ALTER TABLE TASSIGNMENT ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TASSIGNMENTOFFERING table, if needed.
     * @throws SQLException on error
     */
    private void createAssignmentOfferingTable() throws SQLException
    {
        if ( !database().hasTable( "TASSIGNMENTOFFERING" ) )
        {
            log.info( "creating table TASSIGNMENTOFFERING" );
            database().executeSQL(
                "CREATE TABLE TASSIGNMENTOFFERING "
                + "(CASSIGNMENTID INTEGER , CCOURSEOFFERINGID INTEGER , "
                + "CDUEDATE DATETIME , FGRADINGSUSPENDED BIT NOT NULL, "
                + "FHASSUSPENDEDSUBS BIT NOT NULL, OID INTEGER NOT NULL, "
                + "CMOODLEID INTEGER , CPUBLISH BIT NOT NULL)" );
            database().executeSQL(
                "ALTER TABLE TASSIGNMENTOFFERING ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TENQUEUEDJOB table, if needed.
     * @throws SQLException on error
     */
    private void createEnqueuedJobTable() throws SQLException
    {
        if ( !database().hasTable( "TENQUEUEDJOB" ) )
        {
            log.info( "creating table TENQUEUEDJOB" );
            database().executeSQL(
                "CREATE TABLE TENQUEUEDJOB "
                + "(CDISCARDED BIT NOT NULL, OID INTEGER NOT NULL, "
                + "CPAUSED BIT NOT NULL, CQUEUETIME DATETIME , "
                + "CREGRADING BIT NOT NULL, CSUBMISSIONID INTEGER )" );
            database().executeSQL(
                "ALTER TABLE TENQUEUEDJOB ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TGRADERPREFS table, if needed.
     * @throws SQLException on error
     */
    private void createGraderPrefsTable() throws SQLException
    {
        if ( !database().hasTable( "TGRADERPREFS" ) )
        {
            log.info( "creating table TGRADERPREFS" );
            database().executeSQL(
                "CREATE TABLE TGRADERPREFS "
                + "(CASSIGNMENTID INTEGER , CCOMMENTHISTORY MEDIUMTEXT , "
                + "CCOURSEOFFERINGID INTEGER , OID INTEGER NOT NULL, "
                + "CSTEPID INTEGER , CSUBMISSIONFILESTATSID INTEGER , "
                + "CSUBMISSIONID INTEGER , CUSERID INTEGER )" );
            database().executeSQL(
                "ALTER TABLE TGRADERPREFS ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TGRADINGCRITERIA table, if needed.
     * @throws SQLException on error
     */
    private void createGradingCriteriaTable() throws SQLException
    {
        if ( !database().hasTable( "TGRADINGCRITERIA" ) )
        {
            log.info( "creating table TGRADINGCRITERIA" );
            database().executeSQL(
                "CREATE TABLE TGRADINGCRITERIA "
                + "(CBLANKLINEPT DOUBLE , CDEADTIMEDELTA BIGINT , "
                + "CDIFFLINESYNCING BIT NOT NULL, CEXTRALINEPT DOUBLE , "
                + "CFLOATCOMPARSIONSTYLE BIT NOT NULL, "
                + "CFLOATNEGATIVESTYLE DOUBLE , CFLOATPOSITIVESTYLE DOUBLE , "
                + "OID INTEGER NOT NULL, CIGNOREBLANKLINES BIT NOT NULL, "
                + "CIGNORECASE BIT NOT NULL, CIGNORENONPRINTING BIT NOT NULL, "
                + "CINGOREPUNCTUATION BIT NOT NULL, "
                + "CIGNOREWHITESPACE BIT NOT NULL, FPROFILENAME TINYTEXT , "
                + "CNOREMALIZEWHITESPACE BIT NOT NULL, "
                + "CPUNCTUATIONTOIGNORE MEDIUMTEXT , "
                + "CSTRINGCOMPARSIONSTYLE INTEGER , "
                + "CTOKENIZING_STYLE BIT NOT NULL, "
                + "CTRIMWHITESPACE BIT NOT NULL, CUSERID INTEGER )" );
            database().executeSQL(
                "ALTER TABLE TGRADINGCRITERIA ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TRESULTFILE table, if needed.
     * @throws SQLException on error
     */
    private void createResultFileTable() throws SQLException
    {
        if ( !database().hasTable( "TRESULTFILE" ) )
        {
            log.info( "creating table TRESULTFILE" );
            database().executeSQL(
                "CREATE TABLE TRESULTFILE "
                + "(CREPORT TINYTEXT , OID INTEGER NOT NULL, "
                + "CLABEL TINYTEXT , TYPE TINYTEXT , CRESULTID INTEGER )" );
            database().executeSQL(
                "ALTER TABLE TRESULTFILE ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TUPLOADEDSCRIPTFILES table, if needed.
     * @throws SQLException on error
     */
    private void createUploadedScriptFilesTable() throws SQLException
    {
        if ( !database().hasTable( "TUPLOADEDSCRIPTFILES" ) )
        {
            log.info( "creating table TUPLOADEDSCRIPTFILES" );
            database().executeSQL(
                "CREATE TABLE TUPLOADEDSCRIPTFILES "
                + "(CAUTHORID INTEGER , CCONFIGDESCRIPTION BLOB , "
                + "CDEFAULTCONFIGSETTINGS BLOB , OID INTEGER NOT NULL, "
                + "CISCONFIGFILE BIT NOT NULL, CISPUBLISHED BIT NOT NULL, "
                + "CLANGUAGEID INTEGER , CLASTMODIFIEDTIME DATETIME , "
                + "CMAINFILENAME TINYTEXT , CNAME TINYTEXT , "
                + "CSUBDIRNAME TINYTEXT , CUPDATEMUTABLEFIELDS BIT NOT NULL, "
                + "CUPLOADEDFILENAME TINYTEXT )" );
            database().executeSQL(
                "ALTER TABLE TUPLOADEDSCRIPTFILES ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TSTEP table, if needed.
     * @throws SQLException on error
     */
    private void createStepTable() throws SQLException
    {
        if ( !database().hasTable( "TSTEP" ) )
        {
            log.info( "creating table TSTEP" );
            database().executeSQL(
                "CREATE TABLE TSTEP "
                + "(CASSIGNMENTID INTEGER , CCONFIGSETTINGS BLOB , "
                + "OID INTEGER NOT NULL, CORDER INTEGER , CSCRIPTID INTEGER , "
                + "CSTEPCONFIGID INTEGER , CTIMEOUT INTEGER , "
                + "CUPDATEMUTABLEFIELDS BIT NOT NULL)" );
            database().executeSQL(
                "ALTER TABLE TSTEP ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TSTEPCONFIG table, if needed.
     * @throws SQLException on error
     */
    private void createStepConfigTable() throws SQLException
    {
        if ( !database().hasTable( "TSTEPCONFIG" ) )
        {
            log.info( "creating table TSTEPCONFIG" );
            database().executeSQL(
                "CREATE TABLE TSTEPCONFIG "
                + "(CCONFIGSETTINGS BLOB , OID INTEGER NOT NULL, "
                + "CUPDATEMUTABLEFIELDS BIT NOT NULL)" );
            database().executeSQL(
                "ALTER TABLE TSTEPCONFIG ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TSUBMISSION table, if needed.
     * @throws SQLException on error
     */
    private void createSubmissionTable() throws SQLException
    {
        if ( !database().hasTable( "TSUBMISSION" ) )
        {
            log.info( "creating table TSUBMISSION" );
            database().executeSQL(
                "CREATE TABLE TSUBMISSION "
                + "(CASSIGNMENTID INTEGER , CFILENAME TINYTEXT , "
                + "OID INTEGER NOT NULL, CPARTNERLINK BIT NOT NULL, "
                + "CRESULTID INTEGER , CSUBMITNUMBER INTEGER , "
                + "CSUBMITTIME DATETIME , CUSERID INTEGER )" );
            database().executeSQL(
                "ALTER TABLE TSUBMISSION ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TSUBMISSIONFILECOMMENT table, if needed.
     * @throws SQLException on error
     */
    private void createSubmissionFileCommentTable() throws SQLException
    {
        if ( !database().hasTable( "TSUBMISSIONFILECOMMENT" ) )
        {
            log.info( "creating table TSUBMISSIONFILECOMMENT" );
            database().executeSQL(
                "CREATE TABLE TSUBMISSIONFILECOMMENT "
                + "(CAUTHORID INTEGER , CCATEGORYNO TINYINT , "
                + "CDEDUCTION DOUBLE , CFILENAME TINYTEXT , "
                + "CGROUPNAME TINYTEXT , OID INTEGER NOT NULL, "
                + "CLIMITEXCEEDED BIT NOT NULL, CLINENO INTEGER , "
                + "CMESSAGE TEXT , CSUBMISSIONFILESTATSID INTEGER , "
                + "CTONO TINYINT )" );
            database().executeSQL(
                "ALTER TABLE TSUBMISSIONFILECOMMENT ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TSUBMISSIONFILESTATS table, if needed.
     * @throws SQLException on error
     */
    private void createSubmissionFileStatsTable() throws SQLException
    {
        if ( !database().hasTable( "TSUBMISSIONFILESTATS" ) )
        {
            log.info( "creating table TSUBMISSIONFILESTATS" );
            database().executeSQL(
                "CREATE TABLE TSUBMISSIONFILESTATS "
                + "(CCLASSNAME TINYTEXT , CCONDITIONALS INTEGER , "
                + "CCONDITIONALSCOVERED INTEGER , CDEDUCTIONS DOUBLE , "
                + "CELEMENTS INTEGER , CELEMENTSCOVERED INTEGER , "
                + "OID INTEGER NOT NULL, CLOC INTEGER , "
                + "CMARKUPFILENAME TINYTEXT , CMETHODS INTEGER , "
                + "CMETHODSCOVERED INTEGER , CNCCLOC INTEGER , "
                + "CPKGNAME TINYTEXT , CREMARKS INTEGER , CRESULTID INTEGER , "
                + "CSOURCEFILENAME TINYTEXT , CSTATEMENTS INTEGER , "
                + "CSTATEMENTSCOVERED INTEGER , CSTATUS TINYINT )" );
            database().executeSQL(
                "ALTER TABLE TSUBMISSIONFILESTATS ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TSUBMISSIONPROFILE table, if needed.
     * @throws SQLException on error
     */
    private void createSubmissionProfileTable() throws SQLException
    {
        if ( !database().hasTable( "TSUBMISSIONPROFILE" ) )
        {
            log.info( "creating table TSUBMISSIONPROFILE" );
            database().executeSQL(
                "CREATE TABLE TSUBMISSIONPROFILE "
                + "(CAVAILABLEPOINTS DOUBLE , CAVAILABLETIMEDELTA BIGINT , "
                + "FAWARDEARLYBONUS BIT NOT NULL, CDEADTIMEDELTA BIGINT , "
                + "FDEDUCTLATEPENALTY BIT NOT NULL, "
                + "CEARLYBONUSMAXPTS DOUBLE , CEARLYBONUSUNITPTS DOUBLE , "
                + "CEARLYBONUSUNITTIME BIGINT , OID INTEGER NOT NULL, "
                + "CLATEPENALTYMAXPTS DOUBLE , CLATEPENALTYUNITPTS DOUBLE , "
                + "CLATEPENALTYUNITTIME BIGINT , CMAXFILEUPLOADSIZE BIGINT , "
                + "CMAXSUBMITS INTEGER , FPROFILENAME TINYTEXT , "
                + "CSCOREFORMAT TINYTEXT , CTAPOINTS DOUBLE , "
                + "CTOOLPOINTS DOUBLE , CUSERID INTEGER )" );
            database().executeSQL(
                "ALTER TABLE TSUBMISSIONPROFILE ADD PRIMARY KEY (OID)" );
        }
    }


    // ----------------------------------------------------------
    /**
     * Create the TSUBMISSIONRESULT table, if needed.
     * @throws SQLException on error
     */
    private void createSubmissionResultTable() throws SQLException
    {
        if ( !database().hasTable( "TSUBMISSIONRESULT" ) )
        {
            log.info( "creating table TSUBMISSIONRESULT" );
            database().executeSQL(
                "CREATE TABLE TSUBMISSIONRESULT "
                + "(CCOMMENTFORMAT TINYINT , CCOMMENTS MEDIUMTEXT , "
                + "CCORRECTNESSSCORE DOUBLE , OID INTEGER NOT NULL, "
                + "CISMOSTRECENT BIT NOT NULL, CSTATELEMENTSLABEL TINYTEXT , "
                + "CSTATUS TINYINT , CTASCORE DOUBLE , CTOOLSCORE DOUBLE )" );
            database().executeSQL(
                "ALTER TABLE TSUBMISSIONRESULT ADD PRIMARY KEY (OID)" );
        }
    }


    //~ Instance/static variables .............................................

    static Logger log = Logger.getLogger( UpdateSet.class );
}
