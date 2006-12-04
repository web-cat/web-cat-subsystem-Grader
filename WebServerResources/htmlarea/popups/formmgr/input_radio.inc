<SCRIPT LANGUAGE="JavaScript1.2">
//document.title = "Form Editor";
//var highlightedText = window.dialogArguments;

    function returnRadio() {
	var text = escape( "<INPUT " );
	text = text + escape( "TYPE=radio " );
			if (document.radio.radioname.value != "") {
	        text = text + escape( " NAME=" );
			text = text + escape( document.radio.radioname.value );
			}
		else {alert("You must type in a Name"); return true;}
			
		if (document.radio.radiovalue.value != "") {
	        text = text + escape( " VALUE=" );
			text = text + escape( document.radio.radiovalue.value );

	}
		else {alert("You must type in a Value"); return true;}
	
		if (document.radio.eventradio.value  !== "" && document.radio.eventradioValue.value !== "" ){
			   text += ' '	
		text = text + escape( document.radio.eventradio.value);
		   	   text += '="'
		text = text + escape( document.radio.eventradioValue.value );
		   	   text += '"'
	}
			
	if (document.radio.Idradio.value  !== "" ){
	text = text + escape( "Id=\"" );
	text = text + escape( document.radio.Idradio.value );
	text = text + escape( "\"" );
	}
	
	if (document.radio.ischeckedradio.checked) {
			text = text + escape( " CHECKED");
			}
	       text +=' STYLE="' +document.radio.styleradio.value+ '"'
		   text += '>';	
		
	if (document.radio.radiocaption.value != "") {
	text = text + escape( document.radio.radiocaption.value );
	}
	
	window.returnValue = text;
	window.close();
      }
	  
//function paste_link() {
//document.radio.text.value = highlightedText;
//}
</SCRIPT>
<form method=post name=radio>
<fieldset id=fldLayout style='left: 1.50em; top: 7.1em; width: 31.1em; height: 18.3000em;'>
<LEGEND>INPUT RADIO BUTTON</LEGEND></fieldset>
<div id=divRadiotName style='left: 2.50em; top: 10.1em; width: 5em; height: 1.2168em;'>Name: <SPAN STYLE="font: 16px; color:red;">*</SPAN></div>
<input style='left: 7.80em; top: 10.1em; width: 23.60em; height: 2.0000em;' name=radioname type=text value="" size=30>
<div id=divRadioValue style='left: 2.50em; top: 12.5em; width: 5em; height: 1.2168em;'>Value: <SPAN STYLE="font: 16px; color:red;">*</SPAN></div>
<input style='left: 7.80em; top: 12.5em; width: 23.60em; height: 2.0000em;' name=radiovalue type=text value="" size=30>
<div id=divCaptionRadio style='left: 2.50em; top: 15.3em; width: 5.0em; height: 1.2168em;'>Caption:</div>
<input style='left: 7.80em; top: 14.9em; width: 23.6em; height: 2.0000em;' name=radiocaption type=text value="" size=30>
<div id=divRadioEvents style='left: 2.50em; top: 17.4em; width: 5.0em; height: 1.2168em;'>Events:</div>
<div id=divRadioText style='left: 7.8em; top: 17.2em; width: 7.0em; height: 1.2168em;'>
<select name='eventradio' style='width: 8.0em; height: 1.2168em;'>
<option value="" selected>
<option value="OnClick">OnClick
</select>
</div>
<input style='left: 16.70em; top: 17.2em; width: 14.70em; height: 2.0000em;' name=eventradioValue type=text value="" size=30>
<div id=divStyleRadio style='left: 2.50em; top: 19.6em; width: 5.0em; height: 1.2168em;'>Style:</div>
<input style='left: 7.80em; top: 19.6em; width: 23.6em; height: 2.0000em;' name=styleradio type=text value="" size=30>
<div id=divIdradio style='left: 2.50em; top: 22.2em; width: 9.0em; height: 1.2168em;'>ID:</div>
<input style='left: 7.8em; top: 22.2em; width: 8.00em; height: 2.000em;' name=Idradio type=text value="" size=30>
<div id=divIsChecked style='left: 16.70em; top: 22.4em; width: 5em; height: 1.2168em;'>Checked:</div>
<input style='left: 21.00em; top: 22.2em; width: 1.5em; height: 2.0000em;' name=ischeckedradio type=checkbox>

<input style='left: 33.50em; top: 14.3em; width: 6em; height: 2.2000em;' value=OK  type=button onClick='returnRadio()'>
<input style='left: 33.50em; top: 16.5em; width: 6em; height: 2.2000em;' value=Cancel type=button onClick='window.close()'>
</form>
