/*==========================================================================*\
 |  $Id: AssignmentSummary.java,v 1.2 2011/03/07 18:57:09 stedwar2 Exp $
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

package org.webcat.grader.graphs;

import com.webobjects.foundation.*;
import er.extensions.eof.ERXConstant;
import java.io.*;
import org.apache.log4j.Logger;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.webcat.core.*;

// -------------------------------------------------------------------------
/**
 * Represents cumulative, graphable score performance data for all
 * submissions to an assignment offering or an assignment.
 *
 * @author  Stephen Edwards
 * @author  Last changed by $Author: stedwar2 $
 * @version $Revision: 1.2 $, $Date: 2011/03/07 18:57:09 $
 */
public class AssignmentSummary
    implements org.webcat.core.MutableContainer,
               Serializable
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates an empty array.
     */
    public AssignmentSummary()
    {
        this( DEFAULT_MAX_SCORE, DEFAULT_NUMBER_OF_DIVISIONS );
    }


    // ----------------------------------------------------------
    /**
     * Creates an empty assignment summary, set up with the given maximum
     * score and number of divisions (buckets) used for maintaining
     * summary data.
     * @param maxScore the maximum score for this assignment
     * @param numberOfDivisions the number of buckets, or ranges, in which
     *        scores are placed
     */
    public AssignmentSummary( double maxScore, int numberOfDivisions )
    {
        setMaxScore( maxScore, numberOfDivisions );
    }


    //~ Public Constants ......................................................

    public static final double DEFAULT_MAX_SCORE = 100.0;
    public static final int DEFAULT_NUMBER_OF_DIVISIONS = 10;
    public static final String STUDENT_SERIES_KEY = "Students";


    //~ Public Methods ........................................................

    //----------------------------------------------------------
    /**
     * Clear all the data stored in this summary.
     */
    public void clear()
    {
        if ( numInBin == null
             || numInBin.length != numBins )
        {
            numInBin = new int[numBins];
        }
        if ( bin == null
             || bin.length != numBins )
        {
            bin = new float[numBins];
        }
        for ( int i = 0; i < numBins; i++ )
        {
            numInBin[i] = 0;
            bin[i] = 0.0f;
        }
        sum = 0.0f;
        numStudents = 0;
        if ( percentages != null && percentages.length != numBins )
        {
            percentages = null;
        }
        percentagesAreUpToDate = false;
        setHasChanged( true );
    }


    //----------------------------------------------------------
    /**
     * Calculate the percentage distribution for each division across the
     * entire score range.  The resulting array should be treated as
     * read-only.
     * @return an array with one slot per division, containing a percentage
     * (a float between 0 and 1) corresponding to the number of scores in
     * that division.
     */
    public float[] percentageDistribution()
    {
        ensurePercentagesAreCurrent();
        if ( log.isDebugEnabled() )
        {
            log.debug( "percentageDistribution():\n" + printable() );
        }
        return percentages;
    }


    //----------------------------------------------------------
    /**
     * Get the percentage distribution as a chartable data set.
     * @return a JFreeChart category dataset
     */
    public IntervalXYDataset frequencyDataset()
    {
        if ( dataset == null )
        {
            dataset = new Dataset();
        }
        if ( log.isDebugEnabled() )
        {
            log.debug( "frequencyDataset():\n" + printable() );
        }
        return dataset;
    }


    //----------------------------------------------------------
    /**
     * Generate a small, human-readable representation.
     * @return A printable summary
     */
    public String toString()
    {
        float mean = mean();
        int start = (int)mean;
        int tenth = (int)( ( mean - start ) * 10 );
        return "" + start + "." + tenth + "(" + students() + ")";
    }


    //----------------------------------------------------------
    /**
     * Calculate the mean score for this summary.
     * @return the mean (average) score
     */
    public float mean()
    {
        if ( numStudents > 0 )
            return sum / numStudents;
        else
            return 0.0f;
    }


    //----------------------------------------------------------
    /**
     * Get the total number of score entries used in generating this
     * numerical summary.  A student may make 20 submissions for an
     * assignment, but only the most recent submission is included in
     * statistical totalling.  This count includes only the most recent
     * submission for each student, so it is equivalent to the number of
     * distinct students covered by this summary.
     * @return the number of students who have made submissions
     */
    public int students()
    {
        return numStudents;
    }


    //----------------------------------------------------------
    /**
     * Get the total number of submissions that have been entered into
     * this summary.  A student may make 20 submissions for an assignment,
     * but only the most recent submission is included in statistical
     * totalling.  This count includes all submissions considered, not just
     * the most recent for each student.
     * @return the number of submissions
     */
    public int submissions()
    {
        return numSubmissions;
    }


    //----------------------------------------------------------
    /**
     * Get the number of divisions, that is, buckets or ranges, into which
     * scores in this summary are divided.
     * @return the number of score divisions
     */
    public int numberOfDivisions()
    {
        return numBins;
    }


    //----------------------------------------------------------
    /**
     * Adds a single submission from a new student, who does not already
     * have data in this summary.
     * @param score the submission score
     */
    public void addSubmission( double score )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "addSubmission(" + score + ") before:\n"
                + printable() );
        }
        float val = (float)score;
        int binNo = binFor( val );
        bin[binNo] += val;
        numInBin[binNo]++;
        sum += val;
        numStudents++;
        numSubmissions++;
        percentagesAreUpToDate = false;
        setHasChanged( true );
        if ( log.isDebugEnabled() )
        {
            log.debug( "addSubmission(" + score + ") after:\n"
                + printable() );
        }
    }


    //----------------------------------------------------------
    /**
     * Removes a single submission for a student who has not made any
     * other submissions.
     * @param score the submission score
     */
    public void removeSubmission( double score )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "removeSubmission(" + score + ") before:\n"
                + printable() );
        }
        float val = (float)score;
        int binNo = binFor( val );
        bin[binNo] -= val;
        numInBin[binNo]--;
        sum -= val;
        numStudents--;
        percentagesAreUpToDate = false;
        setHasChanged( true );
        if ( log.isDebugEnabled() )
        {
            log.debug( "removeSubmission(" + score + ") after:\n"
                + printable() );
        }
    }


    //----------------------------------------------------------
    /**
     * Replaces (updates) a student's submission score with a newer
     * score, when the student has already submitted before.
     * @param oldScore the submission score to replace
     * @param newScore the new score to replace it with
     */
    public void updateSubmission( double oldScore, double newScore )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "updateSubmission(" + oldScore + ", " + newScore
                + ") before:\n" + printable() );
        }
        removeSubmission( oldScore );
        addSubmission( newScore );
        if ( log.isDebugEnabled() )
        {
            log.debug( "updateSubmission(" + oldScore + ", " + newScore
                + ") after:\n" + printable() );
        }
    }


    //----------------------------------------------------------
    /**
     * Get the maximum score for the assignment associated with this
     * summary.  This is usually the maximum computer-graded score, including
     * correctness/testing points and static analysis tool points, but not
     * late penalties or manually graded points.
     * @return the maximum score
     */
    public float maxScore()
    {
        return maxScore;
    }


    //----------------------------------------------------------
    /**
     * Sets the maximum possible score for this assignment summary,
     * clearing out any data that may already be stored here.
     * @param maxScore the new maximum score for this summary
     */
    public void setMaxScore( double maxScore )
    {
        assert maxScore > 0.0;
        this.maxScore = (float)maxScore;
        if ( binBoundary == null
             || binBoundary.length != numBins )
        {
            binBoundary = new float[numBins];
        }
        for ( int i = 1; i < numBins; i++ )
        {
            binBoundary[i - 1] = ( i * this.maxScore ) / numBins;
        }
        binBoundary[numBins - 1] = this.maxScore;
        clear();
        if ( log.isDebugEnabled() )
        {
            log.debug( "setMaxScore(" + maxScore + ") after:\n"
                + printable() );
        }
    }


    //----------------------------------------------------------
    /**
     * Sets the maximum possible score for this assignment summary, together
     * with the number of divisions this range should be divided into,
     * clearing out any data that may already be stored here.
     * @param maxScore the new maximum score for this summary
     * @param numberOfDivisions the number of buckets, or ranges, in which
     *        scores are placed
     */
    public void setMaxScore( double maxScore, int numberOfDivisions )
    {
        assert maxScore > 0.0 && numberOfDivisions > 0;
        numBins = numberOfDivisions;
        setMaxScore( maxScore );
    }


    //----------------------------------------------------------
    /**
     * Test this object to see if it has been changed (mutated) since it
     * was last saved.
     * @return true if this dictionary has been changed
     */
    public boolean hasChanged()
    {
        log.debug( "hasChanged() = " + hasChanged );
        return hasChanged;
    }


    //----------------------------------------------------------
    /**
     * Mark this object as having changed (mutated) since it
     * was last saved.
     * @param value true if this dictionary has been changed
     */
    public void setHasChanged( boolean value )
    {
        log.debug( "setHasChanged() = " + value );
        hasChanged = value;
        if ( hasChanged )
        {
            if ( parent != null )
            {
                parent.setHasChanged( value );
            }
            if ( owner != null )
            {
                owner.mutableContainerHasChanged();
            }
        }
    }


    //----------------------------------------------------------
    /**
     * Set the enclosing container that holds this one, if any.
     * @param parent a reference to the enclosing container
     */
    public void setParent( MutableContainer parent )
    {
        this.parent = parent;
    }


    //----------------------------------------------------------
    /**
     * Set the enclosing container that holds this one, if any.
     * Also, recursively cycle through all contained mutable containers,
     * resetting their parents to this object as well.
     * @param parent a reference to the enclosing container
     */
    public void setParentRecursively( MutableContainer parent )
    {
        this.parent = parent;
    }


    //----------------------------------------------------------
    /**
     * Examine all contained objects for mutable containers, and reset
     * the parent relationships for any that are found.  Any NS containers
     * found will be converted to mutable versions.
     * @param recurse if true, force the reset to cascade recursively down
     *                the tree, rather than just affecting this node's
     *                immediate children.
     */
    public void resetChildParents( boolean recurse )
    {
        // cannot have any mutable containers as children, so there is
        // nothing to do.
    }


    //----------------------------------------------------------
    /**
     * Retrieve the enclosing container that holds this one, if any.
     * @return a reference to the enclosing container
     */
    public MutableContainer parent()
    {
        return parent;
    }


    //----------------------------------------------------------
    /**
     * Set the owner of this container.
     * @param owner the owner of this container container
     */
    public void setOwner( MutableContainerOwner owner )
    {
        this.owner = owner;
    }


    //----------------------------------------------------------
    /**
     * Replace this container's contents by copying from another (and
     * assuming parent ownership over any subcontainers).  The container
     * is free to assume the argument is of a compatible container type.
     * @param other the container to copy from
     */
    public void copyFrom( MutableContainer other )
    {
        AssignmentSummary rhs = (AssignmentSummary)other;
        setMaxScore( rhs.maxScore(), rhs.numberOfDivisions() );
        addFrom( rhs );
    }


    //----------------------------------------------------------
    /**
     * Determine whether this summary is compatible with another, meaning
     * that both summaries have the same number of divisions and the same
     * maximum score.
     * @param summary the container to check against
     * @return true if the two summaries have the same maximum score and
     * number of divisions
     */
    public boolean isCompatibleWith( AssignmentSummary summary )
    {
        return maxScore() == summary.maxScore()
            && numberOfDivisions() == summary.numberOfDivisions();
    }


    //----------------------------------------------------------
    /**
     * Add all of the entires in the given summary to this summary.  This
     * method can be used to combine multiple summaries into one aggregate
     * summary.  The maximum score and number of divisions must be the
     * same in both containers.
     * @param summary the container to add from
     */
    public void addFrom( AssignmentSummary summary )
    {
        assert isCompatibleWith( summary );
        sum += summary.sum;
        numStudents += summary.students();
        numSubmissions += summary.submissions();
        for ( int i = 0; i < numBins; i++ )
        {
            numInBin[i]    += summary.numInBin[i];
            bin[i]         += summary.bin[i];
            binBoundary[i] += summary.binBoundary[i];
        }
    }


    // ----------------------------------------------------------
    /**
     * This is the conversion method that serializes this object for
     * storage in the database.  It uses java serialization to serialize
     * this object into bytes within an NSData object.
     *
     * @return An NSData object containing the serialized bytes of this object.
     */
    public NSData archiveData()
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "archiveData():\n" + printable() );
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream(
            kOverheadAdjustment + numBins * kCountBytesFactor );
        NSData result = null;
        try
        {
            ObjectOutputStream oos = new ObjectOutputStream( bos );
            oos.writeObject( this );
            oos.flush();
            oos.close();
            result = new NSData( bos.toByteArray() );
        }
        catch ( IOException ioe )
        {
            log.error( "IOException while saving AssignmentSummary to NSData",
                       ioe );
            if ( kThrowOnError )
            {
                throw new NSForwardException( ioe );
            }
        }

        return result;
    }


    // ----------------------------------------------------------
    /**
     * This is the factory method used to recreate an object from a
     * database attribute. It uses java Serialization to turn bytes from an
     * NSData into a reconstituted Object.
     *
     * @param data This is the NSData holding the previously serialized bytes.
     * @return The un-serialized Object.
     */
    public static AssignmentSummary objectWithArchiveData( NSData data )
    {
        if ( data == null ) return new AssignmentSummary();
        ByteArrayInputStream bis = new ByteArrayInputStream( data.bytes() );
        AssignmentSummary result = null;
        Throwable exception = null;
        try
        {
            ObjectInputStream ois = new ObjectInputStream( bis );
            result = (AssignmentSummary)ois.readObject();
        }
        catch ( IOException ioe )
        {
            exception = ioe;
        }
        catch ( ClassNotFoundException cnfe )
        {
            // exception = cnfe;
            result = new AssignmentSummary();
        }

        if ( exception != null )
        {
            log.error(
                "IOException while restoring AssignmentSummary from NSData",
                exception );
            if ( kThrowOnError )
            {
                throw new NSForwardException( exception );
            }
        }

        if ( log.isDebugEnabled() && result != null )
        {
            log.debug( "objectWithArchiveData():\n" + result.printable() );
        }
        return result;
    }


    //----------------------------------------------------------
    /**
     * Calculate the bin number that contains a given score value.
     * @param score the score to use
     * @return the index of the bin where this score belongs
     */
    public int binFor( float score )
    {
        for ( int index = 0; index < numBins - 1; index++ )
        {
            if ( binBoundary[index] > score ) return index;
        }
        return numBins - 1;
    }


    //----------------------------------------------------------
    /**
     * Interpolate a position between 0 and width-1 where a given
     * score would fall within a given bin.  This method performs a simple
     * linear interpolation based on the upper and lower bounds for the bin.
     * @param score the score to use
     * @param binNo the bin in which to perform the interpolation
     * @param width the size (resolution) of the space to interpolate on
     * @return a number >= 0 and < width that is proportional to the score's
     * linearly interpolated position within the bin boundaries
     */
    public int interpolateInBin( float score, int binNo, int width )
    {
        float lowerBound = 0.0f;
        if ( binNo > 0 )
        {
            lowerBound = binBoundary[binNo - 1];
        }
        float val = ( score - lowerBound ) / binBoundary[0];
        int result = Math.round( val * width );
        if ( result >= width )
        {
            result = width - 1;
        }
        return result;
    }


    //----------------------------------------------------------
    /**
     * Generate a detailed printable view of this graph summary.
     * @return the printable view as a string
     */
    public String printable()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "AssignmentSummary, bin count = " );
        buffer.append( numBins );
        buffer.append( "\n    max score = " );
        buffer.append( maxScore );
        buffer.append( ", sum = " );
        buffer.append( sum );
        buffer.append( ", students = " );
        buffer.append( numStudents );
        buffer.append( ", subs = " );
        buffer.append( numSubmissions );
        buffer.append( ", changed = " );
        buffer.append( hasChanged );
        buffer.append( ", up-to-date = " );
        buffer.append( percentagesAreUpToDate );
        buffer.append( "\n" );
        for ( int i = 0; i < numBins; i++ )
        {
            buffer.append( "    bin " );
            buffer.append( i );
            buffer.append( ": boundary = " );
            if ( binBoundary == null )
                buffer.append( "null" );
            else
                buffer.append( binBoundary[i] );
            buffer.append( ", count = " );
            if ( numInBin == null )
                buffer.append( "null" );
            else
                buffer.append( numInBin[i] );
            buffer.append( ", sum = " );
            if ( bin == null )
                buffer.append( "null" );
            else
                buffer.append( bin[i] );
            buffer.append( ", % = " );
            if ( percentages == null )
                buffer.append( "null" );
            else
                buffer.append( percentages[i] );
            buffer.append( "\n" );
        }

        return buffer.toString();
    }


    //~ Dataset Class .........................................................

    @SuppressWarnings("unchecked")
    private class Dataset
        extends AbstractIntervalXYDataset
    {
        // ----------------------------------------------------------
        public int getSeriesCount()
        {
            return 1;
        }


        // ----------------------------------------------------------
        public Comparable getSeriesKey( int series )
        {
            return ( series == 0 )
                ? STUDENT_SERIES_KEY
                : null;
        }


        // ----------------------------------------------------------
        public int indexOf( Comparable seriesKey )
        {
            return STUDENT_SERIES_KEY.equals( seriesKey )
                ? 0
                : -1;
        }


        // ----------------------------------------------------------
        public int getItemCount( int series )
        {
            return series == 0
                ? numBins
                : -1;
        }


        // ----------------------------------------------------------
        public Number getX( int series, int item )
        {
            return new Double( getXValue( series, item ) );
        }


        // ----------------------------------------------------------
        public double getXValue( int series, int item )
        {
            if ( item == 0 )
            {
                return binBoundary[item] / 2.0;
            }
            else
            {
                return ( binBoundary[item - 1] + binBoundary[item] ) / 2.0;
            }
        }


        // ----------------------------------------------------------
        public Number getY( int series, int item )
        {
            ensurePercentagesAreCurrent();
            return ERXConstant.integerForInt( numInBin[item] );
        }


        // ----------------------------------------------------------
        public double getYValue( int series, int item )
        {
            ensurePercentagesAreCurrent();
            return numInBin[item];
        }


        // ----------------------------------------------------------
        public Number getEndX( int series, int item )
        {
            return new Float( binBoundary[item] );
        }


        // ----------------------------------------------------------
        public double getEndXValue( int series, int item )
        {
            return binBoundary[item];
        }


        // ----------------------------------------------------------
        public Number getEndY( int series, int item )
        {
            return getY( series, item );
        }


        // ----------------------------------------------------------
        public double getEndYValue( int series, int item )
        {
            return getYValue( series, item );
        }


        // ----------------------------------------------------------
        public Number getStartX( int series, int item )
        {
            return item == 0
                ? (Number)ERXConstant.ZeroInteger
                : new Float( binBoundary[item - 1] );
        }


        // ----------------------------------------------------------
        public double getStartXValue( int series, int item )
        {
            return item == 0
                ? 0.0
                : binBoundary[item - 1];
        }


        // ----------------------------------------------------------
        public Number getStartY( int series, int item )
        {
            return getY( series, item );
        }


        // ----------------------------------------------------------
        public double getStartYValue( int series, int item )
        {
            return getYValue( series, item );
        }
    }


    //~ Private Methods .......................................................

    private void ensurePercentagesAreCurrent()
    {
        if ( !percentagesAreUpToDate || percentages == null )
        {
            if ( percentages == null )
            {
                percentages = new float[numBins];
            }
            for ( int i = 0; i < numBins; i++ )
            {
                if ( numStudents > 0 )
                {
                    percentages[i] =
                        ( (float)numInBin[i] ) / ( (float)numStudents );
                }
                else
                {
                    percentages[i] = 0.0f;
                }
            }
            percentagesAreUpToDate = true;
        }
    }


    //~ Instance/static variables .............................................

    private int     numBins;
    private float   maxScore;
    private int[]   numInBin;
    private float[] bin;
    private float[] binBoundary;
    private float   sum;
    private int     numStudents;
    private int     numSubmissions;

    private transient float[] percentages;
    private transient boolean percentagesAreUpToDate;

    private           boolean               hasChanged = false;
    private           MutableContainer      parent     = null;
    private transient MutableContainerOwner owner      = null;
    private transient IntervalXYDataset     dataset;

    /**
     * This helps create the ByteArrayOutputStream with a good space estimate.
     */
    private static final int kOverheadAdjustment = 128;

    /**
     * This also helps create the ByteArrayOutputStream with a good space
     * estimate.
     */
    private static final int kCountBytesFactor = 16;

    /**
     * This determines, when an error occurs, if we should throw an
     * NSForwardException or just return null.
     */
    private static final boolean kThrowOnError = true;

    static final long serialVersionUID = 3641621406572343011L;
    static Logger log = Logger.getLogger( AssignmentSummary.class );
}
