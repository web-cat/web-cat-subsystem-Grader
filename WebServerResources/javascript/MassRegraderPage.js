/*==========================================================================*\
 |  $Id$
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
    _interval: null,

    // ----------------------------------------------------------
    start: function()
    {
        if (!this._interval)
        {
            this._refresh();
		    this._interval = setInterval(dojo.hitch(this, function() {
                this._refresh();
		    }), 5000);
		}
    },


    // ----------------------------------------------------------
    stop: function()
    {
        if (this._interval)
        {
            clearInterval(this._interval);
            this._interval = null;
        }
    },
    

    // ----------------------------------------------------------
    _refresh: function()
    {
        webcat.refreshContentPanes([ "queuePane", "qualifierErrors" ]);
    }
});
