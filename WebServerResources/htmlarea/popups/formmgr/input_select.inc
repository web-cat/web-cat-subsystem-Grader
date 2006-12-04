<SCRIPT LANGUAGE="JavaScript1.2" FOR="tab12">
//function options(){
var width = prompt("How many options would you like? (1-10 is good)","5"); 
var a_line="";
var loop = 0; 

while (loop < width)
{

  a_line += "Name  <input name=\"optionnumber"+loop+"\" type=\"text\" value=\"\" size=\"20\"> Caption <BR><BR>\n"; //this was a_line = a_line + "x";
  loop++;        //this was loop=loop+1;
}

function writeoptions() {
document.write (a_line);
}
//}
</SCRIPT>
<DIV STYLE="top:30px; left:300px; width:600px;">
<SCRIPT>
writeoptions()
</SCRIPT>

</DIV>