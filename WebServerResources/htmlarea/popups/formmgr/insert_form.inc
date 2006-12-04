<form method=post name=set>
<fieldset id=fldLayout style='left: 1.50em; top: 8.1em; width: 31.1em; height: 18.3000em;'>
<LEGEND>&lt;FORM&gt;</LEGEND>
</fieldset>
<div id=divType style='left: 2.50em; top: 9.9em; width: 5.0em; height: 1.2168em;'>Type</div>
<div id=divTypedropdown style='left: 7.80em; top: 9.8em; width: 7.0em; height: 1.2168em;'>
<select name=type onChange='javascript: updateTarget();'>
<option value="clientemail"selected>Submit form by e-mail
<option value="serverscript">Submit form by script
</select>
</div>
<div id=divMethod style='left: 19.90em; top: 9.9em; width: 5.0em; height: 1.2168em;'>Method</div>
<div id=divMethoddropdown style='left: 23.6em; top: 9.8em; width: 7.0em; height: 1.2168em;'>
<select name=method onChange='javascript: updateTarget();'>
<option value="get" selected>Get (default)
<option value="post">Post
</select>
</div>
<div id=divName style='left: 2.50em; top: 12.2em; width: 5em; height: 1.2168em;'>Name <SPAN STYLE="font: 16px; color:red;">*</SPAN></div>
<input style='left: 7.80em; top: 12.1em; width: 23.60em; height: 2.0000em;' name=name type=text value="" size=30>
<div id=divAction style='left: 2.50em; top: 14.9em; width: 5em; height: 1.2168em;'>Action</div>
<input style='left: 7.80em; top: 14.5em; width: 23.60em; height: 2.0000em;' name=action type=text value="mailto:" size=30 disabled=true>

<div id=divEnctype style='left: 2.50em; top: 17.0em; width: 5.0em; height: 1.2168em;'>Enctype</div>
<div id=divEnctypedropdown style='left: 7.8em; top: 16.9em; width: 7.0em; height: 1.2168em;'>
<select name=enctype onChange='javascript: updateTarget();'>
<option value="">application/x-www-form-urlencoded (default)
<option value="multipart/form-data">multipart/form-data (req. for INPUT TYPE=FILE)
<option value="text/plain">text/plain (for mailto: only)
<option value="image/jpeg">image/jpeg 
</select>
</div>
<div id=divEmail style='left: 2.50em; top: 19.2em; width: 5em; height: 1.2168em;'>Email <SPAN STYLE="font: 16px; color:red;">*</SPAN></div>
<input style='left: 7.80em; top: 19.2em; width: 23.60em; height: 2.0000em;' name=email type=text value="" size=30>
<input style='left: 33.50em; top: 16.3em; width: 6em; height: 2.2000em;' value=OK  type=button onClick='returnSelected()'>
<input style='left: 33.50em; top: 18.5em; width: 6em; height: 2.2000em;' value=Cancel type=button onClick='window.close()'>

<div id=divEnctype style='left: 2.50em; top: 21.6em; width: 5.0em; height: 1.2168em;'>Events</div>
<div id=divEvent style='left: 7.8em; top: 21.6em; width: 7.0em; height: 1.2168em;'>
<select name=event onChange='javascript: updateTarget();'>
<option value="" selected>
<option value="OnSubmit">OnSubmit
<option value="OnReset">OnReset
</select>
</div>
<input style='left: 15.30em; top: 21.6em; width: 16.10em; height: 2.0000em;' name=inputevent type=text value="" size=30>
<div id=divStyle style='left: 2.50em; top: 23.9em; width: 5.0em; height: 1.2168em;'>Style</div>
<input style='left: 7.80em; top: 23.8em; width: 23.60em; height: 2.0000em;' name=style type=text value="" size=30>
</form>