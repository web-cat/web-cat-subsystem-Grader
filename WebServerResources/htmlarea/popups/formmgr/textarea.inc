<SCRIPT LANGUAGE="JavaScript1.2">
 //document.title = "Form Editor";
//var highlightedText = window.dialogArguments;
   function returnTextarea() {
	var text = escape( "<TEXTAREA" );
	
		if (document.textarea.textareaname.value != "") {
	        text = text + escape( " NAME=" );
			text = text + escape( document.textarea.textareaname.value );
			}
		else {alert("You must type in a Name"); return true;}
			
		   text += ' VALUE="' +document.textarea.textareavalue.value+ '"'
				+ ' COLS="' +document.textarea.textareacols.value+ '"'
				+ ' ROWS="' +document.textarea.textarearows.value+ '"'
				+ ' WRAP="' +document.textarea.textareawrap.value+ '"'
						
	if (document.textarea.eventtextarea.value  !== "" && document.textarea.eventtextareaValue.value !== "" ){
	text = text + escape( document.textarea.eventtextarea.value);
		   text += '="'
	text = text + escape( document.textarea.eventtextareaValue.value );
		   text += '"'
	}
			
		   text += ' DIR="' +document.textarea.Dirtextarea.value+ '"'
				+ ' STYLE="' +document.textarea.styletextarea.value+ '"'
	if (document.textarea.Idtextarea.value  !== "" ){
	text = text + escape( "Id=\"" );
	text = text + escape( document.textarea.Idtextarea.value );
	text = text + escape( "\"" );
	}
		   text += '>';
		   text += '</TEXTAREA>';

	window.returnValue = text;
	window.close();
      }
	  
    //function paste_link() {
	//document.textarea.text.value = highlightedText;
      //}
</SCRIPT>
<form method=post name=textarea>
<fieldset id=fldLayout style='left: 1.50em; top: 4.7em; width: 31.1em; height: 20.7000em;'>
<LEGEND>TEXT AREA</LEGEND></fieldset>
<div id=divTextareaName style='left: 2.50em; top: 7.7em; width: 5em; height: 1.2168em;'>Name: <SPAN STYLE="font: 16px; color:red;">*</SPAN></div>
<input style='left: 7.80em; top: 7.7em; width: 23.60em; height: 2.0000em;' name=textareaname type=text value="" size=30>
<div id=divTextareaValue style='left: 2.50em; top: 10.5em; width: 5em; height: 1.2168em;'>Value:</div>
<input style='left: 7.80em; top: 10.1em; width: 23.60em; height: 2.0000em;' name=textareavalue type=text value="" size=30>
<div id=divTextareaValue style='left: 2.50em; top: 12.9em; width: 5em; height: 1.2168em;'>Cols:</div>
<input style='left: 7.80em; top: 12.5em; width: 8.0em; height: 2.0000em;' name=textareacols type=text value="" size=30>
<div id=divTextareaValue style='left: 16.70em; top: 12.9em; width: 8.55em; height: 1.2168em;'>Rows:</div>
<input style='left: 21.40em; top: 12.5em; width: 10.0em; height: 2.0000em;' name=textarearows type=text value="" size=30>

<div id=divTextareaSize style='left: 2.50em; top: 15.3em; width: 5em; height: 1.2168em;'>ID:</div>
<input style='left: 7.80em; top: 14.9em; width: 8.00em; height: 2.0000em;' name=Idtextarea type=text value="" size=30>
<div id=divTextareaMax style='left: 16.7em; top: 15.3em; width: 8.5em; height: 1.2168em;'>Wrap:</div>
<div id=divEventtextarea style='left: 21.4em; top: 14.9em; width: 10.0em; height: 1.2168em;'>
<select name='textareawrap' style='width: 10.0em; height: 1.2168em;'>
<option value="Off" selected>Off
<option value="Virtual">Virtual
<option value="Physical">Physical
<option value="Soft">Soft
<option value="Hard">Hard
</select>
</div>
<div id=divtextareaEvents style='left: 2.50em; top: 17.4em; width: 5.0em; height: 1.2168em;'>Events:</div>
<div id=divEventtextarea style='left: 7.8em; top: 17.2em; width: 7.0em; height: 1.2168em;'>
<select name='eventtextarea' style='width: 8.0em; height: 1.2168em;'>
<option value="" selected>
<option value="OnFocus">OnFocus
<option value="OnBlur">OnBlur
<option value="OnSelect">OnSelect
<option value="OnChange">OnChange
</select>
</div>
<input style='left: 16.70em; top: 17.2em; width: 14.70em; height: 2.0000em;' name=eventtextareaValue type=text value="" size=30>
<div id=divStyleText style='left: 2.50em; top: 19.6em; width: 5.0em; height: 1.2168em;'>Style:</div>
<input style='left: 7.80em; top: 19.6em; width: 23.6em; height: 2.0000em;' name=styletextarea type=text value="" size=30>

<div id=divDir style='left: 16.70em; top: 22.2em; width: 5.0em; height: 1.2168em;'>Direction:</div>
<div id=divDirText style='left: 21.4em; top: 22.2em; width: 10.0em; height: 1.2168em;'>
<select name='Dirtextarea' style='width: 10.0em; height: 1.2168em;'>
<option value="" selected>
<option value="ltr">Left to Right
<option value="rtl">Right To Left
</select>
</div>

<input style='left: 33.50em; top: 14.3em; width: 6em; height: 2.2000em;' value=OK  type=button onClick='returnTextarea()'>
<input style='left: 33.50em; top: 16.5em; width: 6em; height: 2.2000em;' value=Cancel type=button onClick='window.close()'>
</form>