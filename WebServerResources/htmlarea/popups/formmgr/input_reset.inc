<SCRIPT LANGUAGE="JavaScript1.2">
 //document.title = "Form Editor";
//var highlightedText = window.dialogArguments;
    function returnReset() {
	var text = escape( "<INPUT " );
	text += 'TYPE="reset"' 
			if (document.reset.resetvalue.value != "") {
	        text = text + escape( " VALUE=" );
			text = text + escape( document.reset.resetvalue.value );
			}

							
	if (document.reset.eventreset.value  !== "" && document.reset.eventresetValue.value !== "" ){
	text = text + escape( document.reset.eventreset.value);
		   text += '="'
	text = text + escape( document.reset.eventresetValue.value );
		   text += '"'
	}
	
		   text += ' DIR="' +document.reset.Dirreset.value+ '"'
		        + ' STYLE="' +document.reset.stylereset.value+ '"'
	if (document.reset.Idreset.value  !== "" ){
	text = text + escape( "Id=\"" );
	text = text + escape( document.reset.Idreset.value );
	text = text + escape( "\"" );
	}
		   text += '>';
	
	window.returnValue = text;
	window.close();
      }
	  
    //function paste_link() {
	//document.reset.text.value = highlightedText;
      //}
</SCRIPT>
<form method=post name=reset> 
<fieldset id=fldLayout style='left: 1.50em; top: 7.1em; width: 31.1em; height: 13.5000em;'>
<LEGEND>INPUT reset</LEGEND></fieldset>
<div id=divTextValue style='left: 2.50em; top: 10.3em; width: 5em; height: 1.2168em;'>Value:</div>
<input style='left: 7.80em; top: 9.9em; width: 23.60em; height: 2.0000em;' name=resetvalue type=text value="" size=30>
<div id=divTextEvents style='left: 2.50em; top: 12.6em; width: 5.0em; height: 1.2168em;'>Events:</div>
<div id=divEventText style='left: 7.8em; top: 12.4em; width: 7.0em; height: 1.2168em;'>
<select name='eventreset' style='width: 8.0em; height: 1.2168em;'>
<option value="" selected>
<option value="OnClick">OnClick
</select>
</div>
<input style='left: 16.70em; top: 12.4em; width: 14.70em; height: 2.0000em;' name=eventresetValue type=text value="" size=30>
<div id=divStyleText style='left: 2.50em; top: 14.8em; width: 5.0em; height: 1.2168em;'>Style:</div>
<input style='left: 7.80em; top: 14.8em; width: 23.6em; height: 2.0000em;' name=stylereset type=text value="" size=30>
<div id=divIdreset style='left: 2.50em; top: 17.4em; width: 9.0em; height: 1.2168em;'>ID:</div>
<input style='left: 7.8em; top: 17.4em; width: 8.00em; height: 2.000em;' name=Idreset type=text value="" size=30>

<div id=divDir style='left: 16.70em; top: 17.4em; width: 5.0em; height: 1.2168em;'>Direction:</div>
<div id=divDirText style='left: 23.4em; top: 17.4em; width: 7.0em; height: 1.2168em;'>
<select name='Dirreset'>
<option value="" selected>
<option value="ltr">Left to Right
<option value="rtl">Right To Left
</select>
</div>

<input style='left: 33.50em; top: 9.6em; width: 6em; height: 2.2000em;' value=OK  type=button onClick='returnReset()'>
<input style='left: 33.50em; top: 11.8em; width: 6em; height: 2.2000em;' value=Cancel type=button onClick='window.close()'>
</form>