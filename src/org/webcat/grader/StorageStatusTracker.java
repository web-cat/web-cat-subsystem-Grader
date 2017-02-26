/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2015 Virginia Tech
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

import java.io.File;
import org.apache.log4j.Logger;
import com.webobjects.foundation.NSTimestamp;

//-------------------------------------------------------------------------
/**
 * A class that tracks information about a storage location on disk,
 * for the purposes of monitoring available storage.
 *
 * @author  edwards
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class StorageStatusTracker
{
    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Create a new storage tracker.
     * @param fileName  The name of the file or directory representing the
     *                  device to track storage on.
     */
    public StorageStatusTracker(String fileName)
    {
        this(fileName, DEFAULT_MIN_SPACE);
    }


    // ----------------------------------------------------------
    /**
     * Create a new storage tracker.
     * @param fileName  The name of the file or directory representing the
     *                  device to track storage on.
     * @param minSpace  The threshold value for triggering alert status.
     */
    public StorageStatusTracker(String fileName, long minSpace)
    {
        this.minSpace = minSpace;
        if (fileName == null)
        {
            throw new IllegalArgumentException("fileName cannot be null");
        }
        else
        {
            storageRoot = new File(fileName);
            try
            {
                while (storageRoot != null
                    && storageRoot.getTotalSpace() == 0L)
                {
                    storageRoot = storageRoot.getParentFile();
                }
                if (storageRoot != null
                    && storageRoot.getTotalSpace() == 0L)
                {
                    log.error(
                        "Cannot get device space on file " + storageRoot);
                    storageRoot = null;
                }
            }
            catch (SecurityException e)
            {
                log.error("Encountered security exception trying to "
                    + "identify storage partition using " + storageRoot,
                    e);
                storageRoot = null;
            }
        }
        if (storageRoot != null)
        {
            firstRemaining = storageRoot.getUsableSpace();
            firstTime = new NSTimestamp();
            log.info("Total space on " + storageRoot + ":  "
                + storageRoot.getTotalSpace());
            log.info("Free space on " + storageRoot + ":  "
                + storageRoot.getFreeSpace());
            log.info("Usable space on " + storageRoot + ":  "
                + firstRemaining);
        }
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public boolean isTrackingStore()
    {
        return storageRoot != null;
    }


    // ----------------------------------------------------------
    public String store()
    {
        return storageRoot == null
            ? null
            : storageRoot.toString();
    }


    // ----------------------------------------------------------
    public long totalSpace()
    {
        return storageRoot == null
            ? 0L
            : storageRoot.getTotalSpace();
    }


    // ----------------------------------------------------------
    public long usableSpace()
    {
        return storageRoot == null
        ? 0L
        : storageRoot.getUsableSpace();
    }


    // ----------------------------------------------------------
    public int usedSpacePct()
    {
        return 100 - usableSpacePct();
    }


    // ----------------------------------------------------------
    public int usableSpacePct()
    {
        if (storageRoot == null)
        {
            return 0;
        }
        else
        {
            int result = (int)(
                ((double)storageRoot.getUsableSpace())
                / ((double)storageRoot.getTotalSpace())
                * 100.0 + 0.5);
            if (result > 100)
            {
                result = 100;
            }
            return result;
        }
    }


    // ----------------------------------------------------------
    public long burnRatePerHour()
    {
        if (storageRoot == null)
        {
            return 0L;
        }
        nowRemaining = storageRoot.getUsableSpace();
        nowTime = new NSTimestamp();
        long deltaSize = firstRemaining - nowRemaining;
        if (deltaSize < 0L)
        {
            deltaSize = 0L;
        }
        long deltaT = nowTime.getTime() - firstTime.getTime();
        if (deltaT <= 0L)
        {
            return 0L;
        }
        double rate = deltaSize / (deltaT/3600000.0);
        return (long)(rate + 0.5);
    }


    // ----------------------------------------------------------
    public double predictedHoursRemaining()
    {
        long rate = burnRatePerHour();
        if (rate <= 0L)
        {
            return 8760.0;  // one year :-)
        }
        return nowRemaining / (double)rate;
    }


    // ----------------------------------------------------------
    public boolean alert()
    {
        return predictedHoursRemaining() < 48.0
            || nowRemaining < minSpace;
    }


    //~ Instance/static fields ................................................

    private static final long DEFAULT_MIN_SPACE = 1000000000;  // approx 1 GB

    private File storageRoot;
    private NSTimestamp firstTime;
    private long firstRemaining;
    private NSTimestamp nowTime;
    private long nowRemaining;
    private final long minSpace;
    static Logger log = Logger.getLogger(StorageStatusTracker.class);
}
