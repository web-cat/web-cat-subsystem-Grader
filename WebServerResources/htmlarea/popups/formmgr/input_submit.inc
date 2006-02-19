<SCRIPT LANGUAGE="JavaScript1.2">
 //document.title = "Form Editor";
//var highlightedText = window.dialogArguments;

    function returnSubmit() {
	var text = escape( "<INPUT " );
	text += 'TYPE="submit"' 
			if (document.submit.submitname.value != "") {
	        text = text + escape( " NAME=" );
			text = text + escape( document.submit.submitname.value );
			}
					
			if (document.submit.submitvalue.value != "") {
	        text = text + escape( " VALUE=" );
			text = text + escape( document.submit.submitvalue.value );
			}
			else{
			text = text + escape( " VALUE=" );
			text += '"Submit Query"';
			}
							
	if (document.submit.eventsubmit.value  !== "" && document.submit.eventsubmitValue.value !== "" ){
	text = text + escape( document.submit.eventsubmit.value);
		   text += '="'
	text = text + escape( document.submit.eventsubmitValue.value );
		   text += '"'
	}
	
		   text += ' DIR="' +document.submit.Dirsubmit.value+ '"'
		        + ' STYLE="' +document.submit.stylesubmit.value+ '"'
	if (document.submit.Idsubmit.value  !== "" ){
	text = text + escape( "Id=\"" );
	text = text + escape( document.submit.Idsubmit.value );
	text = text + escape( "\"" );
	}
		   text += '>';
	
	window.returnValue = text;
	window.close();
      }
	  
    //function paste_link() {
	//document.submit.text.value = highlightedText;
      //}
</SCRIPT>
<form method=post name=submit> 
<fieldset id=fldLayout style='left: 1.50em; top: 7.1em; width: 31.1em; height: 15.9000em;'>
<LEGEND>INPUT SUBMIT</LEGEND></fieldset>
<div id=divTextName style='left: 2.50em; top: 10.1em; width: 5em; height: 1.2168em;'>Name:</div>
<input style='left: 7.80em; top: 10.1em; width: 23.60em; height: 2.0000em;' name=submitname type=text value="" size=30>
<div id=divTextValue style='left: 2.50em; top: 12.9em; width: 5em; height: 1.2168em;'>Value:</div>
<input style='left: 7.80em; top: 12.5em; width: 23.60em; height: 2.0000em;' name=submitvalue type=text value="" size=30>
<div id=divTextEvents style='left: 2.50em; top: 15.0em; width: 5.0em; height: 1.2168em;'>Events:</div>
<div id=divEventText style='left: 7.8em; top: 14.8em; width: 7.0em; height: 1.2168em;'>
<select name='eventsubmit' style='width: 8.0em; height: 1.2168em;'>
<option value="" selected>
<option value="OnClick">OnClick
</select>
</div>
<input style='left: 16.70em; top: 14.8em; width: 14.70em; height: 2.0000em;' name=eventsubmitValue type=text value="" size=30>
<div id=divStyleText style='left: 2.50em; top: 17.2em; width: 5.0em; height: 1.2168em;'>Style:</div>
<input style='left: 7.80em; top: 17.2em; width: 23.6em; height: 2.0000em;' name=stylesubmit type=text value="" size=30>
<div id=divIdsubmit style='left: 2.50em; top: 19.8em; width: 9.0em; height: 1.2168em;'>ID:</div>
<input style='left: 7.8em; top: 19.8em; width: 8.00em; height: 2.000em;' name=Idsubmit type=text value="" size=30>

<div id=divDir style='left: 16.70em; top: 19.8em; width: 5.0em; height: 1.2168em;'>Direction:</div>
<div id=divDirText style='left: 23.4em; top: 19.8em; width: 7.0em; height: 1.2168em;'>
<select name='Dirsubmit'>
<option value="" selected>
<option value="ltr">Left to Right
<option value="rtl">Right To Left
</select>
</div>

<input style='left: 33.50em; top: 12.1em; width: 6em; height: 2.2000em;' value=OK  type=button onClick='returnSubmit()'>
<input style='left: 33.50em; top: 14.1em; width: 6em; height: 2.2000em;' value=Cancel type=button onClick='window.close()'>
</form>