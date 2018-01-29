/*==========================================================================*\
 |  $Id: Step.java,v 1.5 2011/12/25 21:11:41 stedwar2 Exp $
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006-2011 Virginia Tech
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

import er.extensions.eof.ERXConstant;
import java.io.*;
import org.apache.log4j.Logger;

// -------------------------------------------------------------------------
/**
 * Represents a single step (plug-in entry) in the grading pipeline or
 * processing sequence for handling a given assignment.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.5 $, $Date: 2011/12/25 21:11:41 $
 */
public class Step
    extends _Step
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new Step object.
     */
    public Step()
    {
        super();
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Get a short (no longer than 60 characters) description of this
     * step, which currently returns {@link #toString()}.
     * @return the description
     */
    public String userPresentableDescription()
    {
        return "(" + order() + "): " + gradingPlugin();
    }


    // ----------------------------------------------------------
    public static int maxTimeout()
    {
        return maxTimeout;
    }


    // ----------------------------------------------------------
    public static int defaultTimeout()
    {
        return defaultTimeout;
    }


    // ----------------------------------------------------------
    public static boolean timeoutIsWithinLimits( int value )
    {
        return value > 0  &&  value <= maxTimeout;
    }


    // ----------------------------------------------------------
    public static boolean timeoutIsWithinLimits( Number value )
    {
        return value == null || timeoutIsWithinLimits( value.intValue() );
    }


    // ----------------------------------------------------------
    public void setTimeout( int value )
    {
        if ( !timeoutIsWithinLimits( value ) )
        {
            value = maxTimeout;
        }
        super.setTimeout( value );
    }


    // ----------------------------------------------------------
    public void setTimeoutRaw( Integer value )
    {
        if ( value != null && !timeoutIsWithinLimits( value.intValue() ) )
        {
            value = ERXConstant.integerForInt( maxTimeout );
        }
        super.setTimeoutRaw( value );
    }


    // ----------------------------------------------------------
    public int effectiveTimeoutForOneRun()
    {
        int value = timeout();
        return ( value == 0 ) ? defaultTimeout : value;
    }


    // ----------------------------------------------------------
    public int effectiveEndToEndTimeout()
    {
        int timeoutOneRun = effectiveTimeoutForOneRun();
        return timeoutOneRun * gradingPlugin().timeoutMultiplier()
            + gradingPlugin().timeoutInternalPadding();
    }


    // ----------------------------------------------------------
    /**
     * Deprecated; may be removed in the future once all bindings in WOD files
     * have been renamed. Use {@link #gradingPlugin()} instead.
     *
     * @return the grading plugin associated with this step
     */
    @Deprecated
    public GradingPlugin script()
    {
        return gradingPlugin();
    }


    // ----------------------------------------------------------
    /**
     * Execute this step with the given command line argument(s).
     *
     * @param args the arguments to pass to the script
     * @param cwd  the working directory to use
     * @param stdout the file where the script's standard output should
     *               be stored
     * @param stderr the file where the script's standard error output
     *               should be stored
     * @return true if the execution script exceeded the time limit
     * @throws IOException if one occurs
     */
    public boolean execute( String args,
                            File   cwd,
                            File   stdout,
                            File   stderr )
        throws IOException
    {
        String finalArgs = args;
        if ( finalArgs == null )
        {
            finalArgs = "";
        }
        if ( stdout != null )
        {
            finalArgs = finalArgs + " 1> " + stdout.getPath();
        }
        if ( stderr != null )
        {
            finalArgs = finalArgs + " 2> " + stderr.getPath();
        }

        ExecThread exeThread = new ExecThread( Thread.currentThread(),
                                               finalArgs,
                                               cwd);
        boolean timedOut = false;
        try
        {
            exeThread.start();
            Thread.sleep( effectiveEndToEndTimeout() * 1000 );
            timedOut = true;
            exeThread.interrupt();
        }
        catch ( InterruptedException e )
        {
            timedOut = false;
        }
        if ( exeThread.exception != null )
        {
            throw exeThread.exception;
        }
        return timedOut;
    }


    // ----------------------------------------------------------
    /**
     * An internal class used as the thread of execution when this
     * step is executed.
     */
    private class ExecThread
        extends Thread
    {
        public ExecThread(Thread parent, String argList, File dir)
        {
            super("Step.ExecThread");
            parentThread = parent;
            args         = argList;
            cwd          = dir;
        }

        public void run()
        {
            try
            {
                gradingPlugin().execute( args, cwd );
            }
            catch ( IOException e )
            {
                // Error creating process, so record it
                log.error( "Exception executing "
                           + gradingPlugin().mainFilePath(),
                           e );
                exception = e;
            }
            catch ( InterruptedException e )
            {
                // Stopped by timeout
                log.info( "Plug-in process was interrupted due to "
                          + "grace period timeout" );
            }
            catch ( Throwable t )
            {
                log.error( "Unhandled exception occurred executing plug-in:",
                    t );
            }
            parentThread.interrupt();
        }

        private Thread      parentThread;
        private String      args;
        private File        cwd;
        public  IOException exception = null;
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

    static final int maxTimeout = org.webcat.core.Application
        .configurationProperties()
        .intForKeyWithDefault( "grader.timeout.max", 600 );
    static final int defaultTimeout = org.webcat.core.Application
        .configurationProperties()
        .intForKeyWithDefault( "grader.timeout.default", 60 );

    static Logger log = Logger.getLogger( Step.class );
}
