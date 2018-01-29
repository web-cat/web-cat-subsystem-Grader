/*==========================================================================*\
 |  $Id: MassRegraderPage.js,v 1.3 2009/10/23 16:48:31 aallowat Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2009 Virginia Tech
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

// --------------------------------------------------------------
/**
 * Manages the timer used to refresh the queue status pane on MassRegraderPage.
 */
dojo.declare("webcat.grader.MassRegraderWatcher", null,
{
    _gradingStatusInterval: null,
    _enqueueInterval: null,

    // ----------------------------------------------------------
    startGradingStatusMonitor: function()
    {
        if (!this.isGradingStatusMonitorRunning())
        {
            this._refreshGradingStatusMonitor();
		    this._gradingStatusInterval = setInterval(
		    dojo.hitch(this, function() {
                this._refreshGradingStatusMonitor();
		    }), 5000);
		}
    },


    // ----------------------------------------------------------
    stopGradingStatusMonitor: function()
    {
        if (this.isGradingStatusMonitorRunning())
        {
            clearInterval(this._gradingStatusInterval);
            this._gradingStatusInterval = null;
        }
    },
    

    // ----------------------------------------------------------
    isGradingStatusMonitorRunning: function()
    {
        return (this._gradingStatusInterval != null);
    },


    // ----------------------------------------------------------
    _refreshGradingStatusMonitor: function()
    {
        webcat.refreshContentPanes("queuePane");
    },


    // ----------------------------------------------------------
    startEnqueueMonitor: function()
    {
        if (!this.isEnqueueMonitorRunning())
        {
            this._refreshEnqueueMonitor();
            this._enqueueInterval = setInterval(dojo.hitch(this, function() {
                this._refreshEnqueueMonitor();
            }), 2500);
        }
    },


    // ----------------------------------------------------------
    stopEnqueueMonitor: function()
    {
        if (this.isEnqueueMonitorRunning())
        {
            clearInterval(this._enqueueInterval);
            this._enqueueInterval = null;
        }
    },
    

    // ----------------------------------------------------------
    isEnqueueMonitorRunning: function()
    {
        return (this._enqueueInterval != null);
    },


    // ----------------------------------------------------------
    _refreshEnqueueMonitor: function()
    {
        webcat.refreshContentPanes([ "qualifierErrors", "enqueueProgress" ]);
    }
});
