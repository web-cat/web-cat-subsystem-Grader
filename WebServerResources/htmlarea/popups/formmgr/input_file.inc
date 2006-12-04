<SCRIPT LANGUAGE="JavaScript1.2">
 //document.title = "Form Editor";
//var highlightedText = window.dialogArguments;
    function returnFile() {
	var text = escape( "<INPUT " );
	text += 'TYPE="file"' 
		if (document.file.filename.value != "") {
	        text = text + escape( " NAME=" );
			text = text + escape( document.file.filename.value );
			}
		else {alert("You must type in a Name"); return true;}
           //text += ' VALUE="' +document.file.filevalue.value+ '"'
			text += ' SIZE="' +document.file.filesize.value+ '"'
				+ ' MAXLENGTH="' +document.file.filemaxlength.value+ '"'
			
	if (document.file.eventfile.value  !== "" && document.file.eventfileValue.value !== "" ){
	text = text + escape( document.file.eventfile.value);
		   text += '="'
	text = text + escape( document.file.eventfileValue.value );
		   text += '"'
	}
	
		   text + ' STYLE="' +document.file.stylefile.value+ '"'
	if (document.file.Idfile.value  !== "" ){
	text = text + escape( "Id=\"" );
	text = text + escape( document.file.Idfile.value );
	text = text + escape( "\"" );
	}
		   text += '>';
	
	window.returnValue = text;
	window.close();
      }
	  
    //function paste_link() {
	//document.file.text.value = highlightedText;
      //}
</SCRIPT>
<form method=post name=file> 
<fieldset id=fldLayout style='left: 1.50em; top: 7.1em; width: 31.1em; height: 15.9000em;'>
<LEGEND>INPUT PASSWORD</LEGEND></fieldset>
<div id=divTextName style='left: 2.50em; top: 10.1em; width: 5em; height: 1.2168em;'>Name: <SPAN STYLE="font: 16px; color:red;">*</SPAN></div>
<input style='left: 7.80em; top: 10.1em; width: 23.60em; height: 2.0000em;' name=filename type=text value="" size=30>
<div id=divTextSize style='left: 2.50em; top: 12.9em; width: 5em; height: 1.2168em;'>Size:</div>
<input style='left: 7.80em; top: 12.5em; width: 8.00em; height: 2.0000em;' name=filesize type=text value="" size=30>
<div id=divTextMax style='left: 16.7em; top: 12.9em; width: 8.5em; height: 1.2168em;'>Max. Length:</div>
<input style='left: 23.40em; top: 12.5em; width: 8.00em; height: 2.0000em;' name=filemaxlength type=text value="" size=30>
<div id=divTextEvents style='left: 2.50em; top: 15.0em; width: 5.0em; height: 1.2168em;'>Events:</div>
<div id=divEventText style='left: 7.8em; top: 14.8em; width: 7.0em; height: 1.2168em;'>
<select name='eventfile' style='width: 8.0em; height: 1.2168em;'>
<option value="" selected>
<option value="OnFocus">OnFocus
<option value="OnBlur">OnBlur
<option value="OnSelect">OnSelect
<option value="OnChange">OnChange
</select>
</div>
<input style='left: 16.70em; top: 14.8em; width: 14.70em; height: 2.0000em;' name=eventfileValue type=text value="" size=30>
<div id=divStyleText style='left: 2.50em; top: 17.2em; width: 5.0em; height: 1.2168em;'>Style:</div>
<input style='left: 7.80em; top: 17.2em; width: 23.6em; height: 2.0000em;' name=stylefile type=text value="" size=30>
<div id=divIdfile style='left: 2.50em; top: 19.8em; width: 9.0em; height: 1.2168em;'>ID:</div>
<input style='left: 7.8em; top: 19.8em; width: 8.00em; height: 2.000em;' name=Idfile type=text value="" size=30>

<input style='left: 33.50em; top: 11.9em; width: 6em; height: 2.2000em;' value=OK  type=button onClick='returnFile()'>
<input style='left: 33.50em; top: 14.1em; width: 6em; height: 2.2000em;' value=Cancel type=button onClick='window.close()'>
</form>