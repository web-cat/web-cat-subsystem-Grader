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
import com.webobjects.foundation.*;
import er.extensions.ERXConstant;
import net.sf.webcat.core.*;

import org.apache.log4j.*;

//-------------------------------------------------------------------------
/**
* Represents a standard Web-CAT page that has not yet been implemented
* (is "to be defined").
*
*  @author Stephen Edwards
*  @version $Id$
*/
public class ScriptOptionPanel
    extends WCComponent
    implements FileBrowser.FileSelectionListener
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new TBDPage object.
     * 
     * @param context The context to use
     */
    public ScriptOptionPanel( WOContext context )
    {
        super( context );
    }


    //~ KVC Attributes (must be public) .......................................

    public NSDictionary        option;
    public NSMutableDictionary optionValues;
    public Boolean             terse;
    public int                 type = 0;
    public String              property;
    public NSDictionary        choice;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void appendToResponse( WOResponse response, WOContext context )
    {
        log.debug( "option = " + option.objectForKey( "property" )
                        + ", type = " + option.objectForKey( "type" )
                        + ", type = " + type );
        // if ( type == 0 )
        {
            String typeName = (String)option.objectForKey( "type" );
            for ( int i = 0; i < types.length; i++ )
            {
                if ( types[i].equals( typeName ) )
                {
                    type = i;
                    break;
                }
            }
        }
        theSelectedChoice = null;
        log.debug( "option = " + option.objectForKey( "property" )
                   + ", type = " + option.objectForKey( "type" )
                   + ", type = " + type );
        if ( property == null )
        {
            property = (String)option.objectForKey( "property" );
        }
//        log.debug( "appendToResponse(): type = " + type + ", property = "
//                   + property );
        super.appendToResponse( response, context );
    }


    // ----------------------------------------------------------
    public boolean isBoolean()
    {
        return type == BOOLEAN_TYPE;
    }


    // ----------------------------------------------------------
    public boolean isInteger()
    {
        return type == INTEGER_TYPE;
    }


    // ----------------------------------------------------------
    public boolean isDouble()
    {
        return type == DOUBLE_TYPE;
    }


    // ----------------------------------------------------------
    public boolean isString()
    {
        return type == STRING_TYPE;
    }


    // ----------------------------------------------------------
    public boolean isListChoice()
    {
        return type == LIST_CHOICE_TYPE;
    }


    // ----------------------------------------------------------
    public boolean isRadioChoice()
    {
        return type == RADIO_CHOICE_TYPE;
    }


    // ----------------------------------------------------------
    public boolean isFile()
    {
        return type == FILE_TYPE;
    }


    // ----------------------------------------------------------
    public boolean isFileOrDir()
    {
        return type == FILE_OR_DIR_TYPE;
    }


    // ----------------------------------------------------------
    public boolean isShortText()
    {
        return type == SHORT_TEXT_TYPE;
    }


    // ----------------------------------------------------------
    public boolean isLongText()
    {
        return type == LONG_TEXT_TYPE;
    }


    // ----------------------------------------------------------
    public Object fieldSize()
    {
        Object result = option.objectForKey( "size" );
        if ( result == null )
        {
            // default sizes
            switch ( type )
            {
                case INTEGER_TYPE:
                    result = ERXConstant.integerForInt( 4 );
                    break;
                case DOUBLE_TYPE:
                    result = ERXConstant.integerForInt( 6 );
                    break;
            }
        }
        return result;
    }


    // ----------------------------------------------------------
    public Object value()
    {
        Object result = optionValues.objectForKey( property );
        if ( result == null )
        {
            result = option.objectForKey( "default" );
        }
        return result;
    }


    // ----------------------------------------------------------
    public void setValue( Object value )
    {
        Object oldValue = value();
        log.debug( "set " + property + " = " + value );
        if ( value != null
                  && ( oldValue == null ||
                       !value().toString().equals( value.toString() ) ) )
        {
            log.debug( "storing value" );
            optionValues.setObjectForKey( value, property );
        }
        else
        {
            log.debug( "removing value" );
            optionValues.removeObjectForKey( property );
        }
    }


    // ----------------------------------------------------------
    public boolean hasValue()
    {
        Object oldValue = optionValues.objectForKey( property );
        return oldValue != null;
    }


    // ----------------------------------------------------------
    public WOComponent clearValue()
    {
        setValue( null );
        return null;
    }


    // ----------------------------------------------------------
    public Object choiceLabel()
    {
        Object result = choice.objectForKey( "label" );
        if ( result == null )
        {
            result = choiceValue();
        }
        return result;
    }


    // ----------------------------------------------------------
    public Object choiceValue()
    {
        return choice.objectForKey( "value" );
    }


    // ----------------------------------------------------------
    public Object selectedChoice()
    {
        Object currentValue = value();
        if ( theSelectedChoice == null && currentValue != null )
        {
            String valueString = currentValue.toString();
            NSArray choices = (NSArray)option.objectForKey( "choices" );
            for ( int i = 0; i < choices.count(); i++ )
            {
                NSDictionary thisChoice =
                    (NSDictionary)choices.objectAtIndex( i );
                if ( valueString.equals(
                        thisChoice.objectForKey( "value" ).toString() ) )
                {
                    theSelectedChoice = thisChoice;
                    break;
                }
            }
        }
        return theSelectedChoice;
    }


    // ----------------------------------------------------------
    public void setSelectedChoice( NSDictionary theChoice )
    {
        setValue( theChoice.objectForKey( "value" ) );
    }


    // ----------------------------------------------------------
    public String fileValue()
    {
        Object value = value();
        return ( value == null ) ? "<script default>" : value.toString();
    }


    // ----------------------------------------------------------
    public WOComponent browse()
    {
        EditScriptFilesPage newPage = (EditScriptFilesPage)
            pageWithName( EditScriptFilesPage.class.getName() );
        newPage.nextPage = (WCComponent)context().page();
        newPage.isEditable = true;
        newPage.base = new java.io.File ( ScriptFile.userScriptDirName(
                        wcSession().user(), true ).toString() );
        if ( ! newPage.base.exists() )
        {
            newPage.base.mkdirs();
        }
        newPage.fileSelectionListener = this;
        newPage.allowSelectDir = type == FILE_OR_DIR_TYPE;
        newPage.allowSelectExtensions =
            (NSArray)option.objectForKey( "fileTypes" );
        return newPage;
    }


    // ----------------------------------------------------------
    public WOComponent selectFile( String filePath )
    {
        setValue( wcSession().user().authenticationDomain().subdirName()
                  + "/" + filePath );
        if ( log.isDebugEnabled() )
        {
            log.debug( "new option values:\n" + optionValues );
        }
        return context().page();
    }


    //~ Instance/static variables .............................................

    private NSDictionary theSelectedChoice = null;
    
    // private static int UNKNOWN_TYPE      = 0;
    private static final int BOOLEAN_TYPE      = 1;
    private static final int INTEGER_TYPE      = 2;
    private static final int DOUBLE_TYPE       = 3;
    private static final int STRING_TYPE       = 4;
    private static final int LIST_CHOICE_TYPE  = 5;
    private static final int RADIO_CHOICE_TYPE = 6;
    private static final int FILE_TYPE         = 7;
    private static final int FILE_OR_DIR_TYPE  = 8;
    private static final int SHORT_TEXT_TYPE   = 9;
    private static final int LONG_TEXT_TYPE   = 10;
    private static final String[] types = new String[] {
        "unknown",
        "boolean",
        "integer",
        "double",
        "string",
        "listChoice",
        "radioChoice",
        "file",
        "fileOrDir",
        "shortText",
        "longText"
    };
    static Logger log = Logger.getLogger( ScriptOptionPanel.class );
}
