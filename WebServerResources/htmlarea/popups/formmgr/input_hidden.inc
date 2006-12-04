<SCRIPT LANGUAGE="JavaScript1.2">
//document.title = "Form Editor";
//var highlightedText = window.dialogArguments;

    function returnHidden() {
	
	var text = escape( "<INPUT " );
	text += 'TYPE="hidden"' //ignored by IE 
		if (document.hidden.hiddenname.value != "") {
	        text = text + escape( " NAME=" );
			text = text + escape( document.hidden.hiddenname.value );
	}
		else {alert("You must type in a Name"); return true;}
	
	if (document.hidden.hiddenvalue.value != "") {
	        text = text + escape( " VALUE=" );
			text = text + escape( document.hidden.hiddenvalue.value );
	}
		else {alert("You must type in a Value"); return true;}
	
	text += '>';	

	window.returnValue = text;
	window.close();
      }
	  
//function paste_link() {
//document.textbox.text.value = highlightedText;
//}
</SCRIPT>
<form method=post name=hidden>
<fieldset id=fldLayout style='left: 1.50em; top: 7.1em; width: 31.1em; height: 11.3000em;'>
<LEGEND>INPUT HIDDEN</LEGEND></fieldset>
<div id=divHiddenName style='left: 2.50em; top: 10.1em; width: 5em; height: 1.2168em;'>Name: <SPAN STYLE="font: 16px; color:red;">*</SPAN></div>
<input style='left: 7.80em; top: 10.1em; width: 23.60em; height: 2.0000em;' name='hiddenname' type=text value="" size=30>
<div id=divHiddenValue style='left: 2.50em; top: 12.9em; width: 5em; height: 1.2168em;'>Value: <SPAN STYLE="font: 16px; color:red;">*</SPAN></div>
<input style='left: 7.80em; top: 12.5em; width: 23.60em; height: 2.0000em;' name='hiddenvalue' type=text value="" size=30>
<input style='left: 33.50em; top: 10.3em; width: 6em; height: 2.2000em;' value=OK  type=button onClick='returnHidden()'>
<input style='left: 33.50em; top: 12.5em; width: 6em; height: 2.2000em;' value=Cancel type=button onClick='window.close()'>
</form>