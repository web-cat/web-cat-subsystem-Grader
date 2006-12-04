<SCRIPT LANGUAGE="JavaScript1.2">
 //document.title = "Form Editor";
//var highlightedText = window.dialogArguments;
   function returnTextBox() {
	var text = escape( "<INPUT " );
	text += 'TYPE="text"' //ignored by IE 
		if (document.textbox.name.value != "") {
	        text = text + escape( " NAME=" );
			text = text + escape( document.textbox.name.value );
			}
		else {alert("You must type in a Name"); return true;}
			
		   text += ' VALUE="' +document.textbox.value.value+ '"'
				+ ' SIZE="' +document.textbox.size.value+ '"'
				+ ' MAXLENGTH="' +document.textbox.maxlength.value+ '"'
		
	if (document.textbox.eventText.value  !== "" && document.textbox.eventTextValue.value !== "" ){
	text = text + escape( document.textbox.eventText.value);
		   text += '="'
	text = text + escape( document.textbox.eventTextValue.value );
		   text += '"'
	}
			
		   text += ' DIR="' +document.textbox.DirText.value+ '"'
				+ ' STYLE="' +document.textbox.styleText.value+ '"'
	if (document.textbox.IdText.value  !== "" ){
	text = text + escape( "Id=\"" );
	text = text + escape( document.textbox.IdText.value );
	text = text + escape( "\"" );
	}
		   text += '>';

	window.returnValue = text;
	window.close();
      }
	  
    //function paste_link() {
	//document.textbox.text.value = highlightedText;
      //}
</SCRIPT>
<form method=post name=textbox>
<fieldset id=fldLayout style='left: 1.50em; top: 7.1em; width: 31.1em; height: 18.3000em;'>
<LEGEND>INPUT TEXT</LEGEND></fieldset>
<div id=divTextName style='left: 2.50em; top: 10.1em; width: 5em; height: 1.2168em;'>Name: <SPAN STYLE="font: 16px; color:red;">*</SPAN></div>
<input style='left: 7.80em; top: 10.1em; width: 23.60em; height: 2.0000em;' name=name type=text value="" size=30>
<div id=divTextValue style='left: 2.50em; top: 12.9em; width: 5em; height: 1.2168em;'>Value:</div>
<input style='left: 7.80em; top: 12.5em; width: 23.60em; height: 2.0000em;' name=value type=text value="" size=30>
<div id=divTextSize style='left: 2.50em; top: 15.3em; width: 5em; height: 1.2168em;'>Size:</div>
<input style='left: 7.80em; top: 14.9em; width: 8.00em; height: 2.0000em;' name=size type=text value="" size=30>
<div id=divTextMax style='left: 16.7em; top: 15.3em; width: 8.5em; height: 1.2168em;'>Max. Length:</div>
<input style='left: 23.40em; top: 14.9em; width: 8.00em; height: 2.0000em;' name=maxlength type=text value="" size=30>
<div id=divTextEvents style='left: 2.50em; top: 17.4em; width: 5.0em; height: 1.2168em;'>Events:</div>
<div id=divEventText style='left: 7.8em; top: 17.2em; width: 7.0em; height: 1.2168em;'>
<select name='eventText' style='width: 8.0em; height: 1.2168em;'>
<option value="" selected>
<option value="OnFocus">OnFocus
<option value="OnBlur">OnBlur
<option value="OnSelect">OnSelect
<option value="OnChange">OnChange
</select>
</div>
<input style='left: 16.70em; top: 17.2em; width: 14.70em; height: 2.0000em;' name=eventTextValue type=text value="" size=30>
<div id=divStyleText style='left: 2.50em; top: 19.6em; width: 5.0em; height: 1.2168em;'>Style:</div>
<input style='left: 7.80em; top: 19.6em; width: 23.6em; height: 2.0000em;' name=styleText type=text value="" size=30>

<div id=divIdText style='left: 2.50em; top: 22.2em; width: 9.0em; height: 1.2168em;'>ID:</div>
<input style='left: 7.8em; top: 22.2em; width: 8.00em; height: 2.000em;' name=IdText type=text value="" size=30>

<div id=divDir style='left: 16.70em; top: 22.2em; width: 5.0em; height: 1.2168em;'>Direction:</div>
<div id=divDirText style='left: 23.4em; top: 22.2em; width: 7.0em; height: 1.2168em;'>
<select name='DirText'>
<option value="" selected>
<option value="ltr">Left to Right
<option value="rtl">Right To Left
</select>
</div>

<input style='left: 33.50em; top: 14.3em; width: 6em; height: 2.2000em;' value=OK  type=button onClick='returnTextBox()'>
<input style='left: 33.50em; top: 16.5em; width: 6em; height: 2.2000em;' value=Cancel type=button onClick='window.close()'>
</form>